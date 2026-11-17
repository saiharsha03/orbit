// Work log: what you did on one day, put together from what is already on this machine.
// - threads: every Claude Code session with a prompt of yours that day (its transcript is read line by line for the
//   prompts' times; a past day's answer is kept in memory by file size)
// - commits: your commits that day in every repo under the repos root (git log --all, author = the repo's user.email)
// - to-dos finished and notes changed that day
// The day's summary is a markdown file in ~/.orbit/hive/worklog/YYYY-MM-DD.md: Sonnet writes it on request and you can
// edit it like any note.
import { query } from '@anthropic-ai/claude-agent-sdk'
import { execFile } from 'node:child_process'
import { createReadStream, existsSync, mkdirSync, readdirSync, statSync } from 'node:fs'
import { readFile, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { basename, join } from 'node:path'
import { createInterface } from 'node:readline'
import { claudeExecutable } from './engine'
import { listSessions } from './sessions'
import { listTodos } from './todos'
import { listNotes } from './notes'
import { dataDir, TEST_MODE, blocked } from './testMode'
import type { DevConfig } from '../shared/devCore'
import { dayRange, isDay, type DayLog, type LogCommit, type LogThread } from '../shared/worklogCore'

export const worklogDir = () => join(dataDir(), 'hive', 'worklog')
const summaryFile = (day: string) => {
  if (!isDay(day)) throw new Error(`Not a day: ${day}`)
  return join(worklogDir(), `${day}.md`)
}

// a user message you typed (not a tool result, a command wrapper or an interruption): its text, else null
function promptText(e: any): string | null {
  if (e.type !== 'user' || e.isMeta || e.isSidechain) return null
  const c = e.message?.content
  const text = typeof c === 'string' ? c : Array.isArray(c) ? c.find((p: any) => p.type === 'text')?.text : null
  if (!text || text.startsWith('<') || text.startsWith('[Request interrupted') || text.startsWith('Caveat:')) return null
  return text.replace(/\s+/g, ' ').trim()
}

type Prompts = { times: number[]; texts: string[] }
const promptCache = new Map<string, { size: number; p: Prompts }>()

// the prompts in one transcript between start and end
async function promptsIn(path: string, size: number, start: number, end: number): Promise<Prompts> {
  const key = `${path}|${start}`
  const hit = promptCache.get(key)
  if (hit && hit.size === size) return hit.p
  const p: Prompts = { times: [], texts: [] }
  const rl = createInterface({ input: createReadStream(path, 'utf8'), crlfDelay: Infinity })
  for await (const line of rl) {
    if (!line.includes('"type":"user"') || line.includes('"tool_use_id"')) continue
    let e: any
    try { e = JSON.parse(line) } catch { continue }
    const t = Date.parse(e.timestamp ?? '')
    if (!(t >= start && t < end)) continue
    const text = promptText(e)
    if (!text) continue
    p.times.push(t)
    p.texts.push(text.length > 240 ? `${text.slice(0, 240)}…` : text)
  }
  promptCache.set(key, { size, p })
  return p
}

async function threadsOn(start: number, end: number): Promise<LogThread[]> {
  const all = await listSessions()
  const cands = all.filter((s) => !s.watcherRun && !s.dependabot && s.started < end && s.updated >= start)
  const out: LogThread[] = []
  for (let i = 0; i < cands.length; i += 8) {
    await Promise.all(cands.slice(i, i + 8).map(async (s) => {
      let size: number
      try { size = statSync(s.path).size } catch { return }
      const p = await promptsIn(s.path, size, start, end).catch(() => null)
      if (!p || !p.times.length) return
      out.push({ id: s.id, title: s.title, project: /\.hive-worktrees[\\/]([^\\/]+)/.exec(s.cwd)?.[1] ?? s.project, cwd: s.cwd, path: s.path, started: s.started, updated: s.updated, personal: false, prompts: p.times.length, first: Math.min(...p.times), last: Math.max(...p.times), asked: p.texts })
    }))
  }
  return out.sort((a, b) => a.first - b.first)
}

const git = (cwd: string, args: string[]): Promise<string> =>
  new Promise((resolve) => execFile('git', args, { cwd, timeout: 30_000, maxBuffer: 16 * 1024 * 1024, windowsHide: true, env: { ...process.env, GIT_TERMINAL_PROMPT: '0' } },
    (err, stdout) => resolve(err ? '' : String(stdout))))

// the git repos: each folder under the root with a .git, and each under the agents folders. A repo is personal when
// its GitHub owner is the owner of Orbit's own repo (appDir); names = its folder and GitHub repo names
type Repo = { dir: string; personal: boolean }
const ownerOf = (url: string): { owner: string; name: string } | null => {
  const m = /github\.com[:/]([^/]+)\/([^/\s]+?)(?:\.git)?\s*$/i.exec(url.trim())
  return m ? { owner: m[1].toLowerCase(), name: m[2].toLowerCase() } : null
}
let repoCache: { key: string; at: number; list: Repo[]; names: string[] } | null = null
async function repos(cfg: DevConfig | null, appDir: string): Promise<{ list: Repo[]; names: string[] }> {
  if (!cfg?.root) return { list: [], names: [] }
  const key = `${cfg.root}|${cfg.agentsFolders.join(',')}|${appDir}`
  if (repoCache && repoCache.key === key && Date.now() - repoCache.at < 10 * 60_000) return repoCache
  const dirs: string[] = []
  const scan = (dir: string) => {
    let names: string[]
    try { names = readdirSync(dir) } catch { return }
    for (const n of names) {
      if (n.startsWith('.')) continue
      const p = join(dir, n)
      if (existsSync(join(p, '.git'))) dirs.push(p)
    }
  }
  scan(cfg.root)
  for (const a of cfg.agentsFolders) scan(join(cfg.root, a))
  const mine = ownerOf(await git(appDir, ['remote', 'get-url', 'origin']))?.owner
  const names = new Set<string>()
  const list = await Promise.all(dirs.map(async (dir) => {
    const o = ownerOf(await git(dir, ['remote', 'get-url', 'origin']))
    const personal = !!mine && o?.owner === mine
    if (personal) { names.add(basename(dir).toLowerCase()); if (o) names.add(o.name) }
    return { dir, personal }
  }))
  repoCache = { key, at: Date.now(), list, names: [...names] }
  return repoCache
}

// Work or personal, per thread: a thread that ran in a repo (or an Orbit worktree of one) takes the repo's side.
// Any other (one run from the repos root) is labelled once by Haiku from its title and first prompts, and the label is
// kept in worklog/kinds.json; without Haiku, the title naming a personal repo decides.
const kindsFile = () => join(worklogDir(), 'kinds.json')
async function readKinds(): Promise<Record<string, boolean>> {
  try { return JSON.parse(await readFile(kindsFile(), 'utf8')) } catch { return {} }
}
const low = (p: string) => p.replace(/\\/g, '/').toLowerCase()

async function classify(threads: LogThread[], rs: { list: Repo[]; names: string[] }): Promise<LogThread[]> {
  const byName = new Map(rs.list.map((r) => [basename(r.dir).toLowerCase(), r.personal]))
  const fromRepo = (t: LogThread): boolean | undefined => {
    const hit = rs.list.find((r) => low(t.cwd) === low(r.dir) || low(t.cwd).startsWith(`${low(r.dir)}/`))
    return hit ? hit.personal : byName.get(t.project.toLowerCase())
  }
  const kinds = await readKinds()
  const ask = threads.filter((t) => fromRepo(t) === undefined && kinds[t.id] === undefined)
  if (ask.length && !TEST_MODE) {
    const work = rs.list.filter((r) => !r.personal).map((r) => basename(r.dir))
    const prompt = [
      `PERSONAL repos (the developer's own apps and side projects): ${rs.names.join(', ') || 'none'}`,
      `WORK repos (their job): ${work.join(', ') || 'none'}`,
      'THREADS:',
      ...ask.map((t, i) => `${i + 1}. "${t.title}" | ${t.asked.slice(0, 3).map((a) => a.slice(0, 160)).join(' / ')}`),
    ].join('\n')
    const text = await askClaude('haiku', [
      'Label each Claude Code thread as work (the developer\'s job: its products, customers, data, teammates) or personal',
      '(their own apps and tools, the personal repos). Answer one line per thread, "<number>: work" or "<number>: personal", nothing else.',
    ].join(' '), prompt, 60_000).catch(() => '')
    let got = 0
    for (const m of text.matchAll(/^\s*(\d+)\s*[:.)-]\s*(work|personal)/gim)) {
      const t = ask[Number(m[1]) - 1]
      if (t) { kinds[t.id] = m[2].toLowerCase() === 'personal'; got++ }
    }
    if (got) { mkdirSync(worklogDir(), { recursive: true }); await writeFile(kindsFile(), JSON.stringify(kinds), 'utf8') }
  }
  const named = (t: LogThread) => { const w = `${t.project} ${t.title}`.toLowerCase().split(/[^a-z0-9-]+/); return rs.names.some((n) => w.includes(n)) }
  return threads.map((t) => ({ ...t, personal: fromRepo(t) ?? kinds[t.id] ?? named(t) }))
}

// one answer from a model with no tools, no settings, nothing saved as a session
async function askClaude(model: string, system: string, prompt: string, timeout: number): Promise<string> {
  const q = query({
    prompt,
    options: {
      cwd: tmpdir(), pathToClaudeCodeExecutable: claudeExecutable(), model, systemPrompt: system,
      settingSources: [], persistSession: false, maxTurns: 1, allowedTools: [],
      canUseTool: async () => ({ behavior: 'deny' as const, message: 'Answered without tools.' }),
    },
  })
  let text = ''
  const stop = setTimeout(() => q.close(), timeout)
  try {
    for await (const m of q) {
      if (m.type === 'assistant') for (const b of (m.message.content as { type: string; text?: string }[])) if (b.type === 'text') text += b.text ?? ''
      if (m.type === 'result') break
    }
  } finally {
    clearTimeout(stop)
    q.close()
  }
  return text.trim()
}

async function commitsOn(list: Repo[], start: number, end: number): Promise<LogCommit[]> {
  const seen = new Set<string>()
  const out: LogCommit[] = []
  for (let i = 0; i < list.length; i += 6) {
    await Promise.all(list.slice(i, i + 6).map(async ({ dir, personal }) => {
      const email = (await git(dir, ['config', 'user.email'])).trim()
      if (!email) return
      const log = await git(dir, ['log', '--all', '--no-merges', `--since=@${Math.floor(start / 1000)}`, `--until=@${Math.floor(end / 1000)}`,
        `--author=${email}`, '--format=%H%x1f%ct%x1f%s'])
      for (const line of log.split('\n')) {
        const [hash, ct, subject] = line.split('\x1f')
        if (!hash || !subject || seen.has(hash)) continue
        seen.add(hash)
        out.push({ repo: basename(dir), hash: hash.slice(0, 7), at: Number(ct) * 1000, subject, personal })
      }
    }))
  }
  return out.sort((a, b) => a.at - b.at)
}

// appDir: Orbit's own repo, whose GitHub owner marks the personal repos
export async function dayLog(day: string, cfg: DevConfig | null, appDir: string): Promise<DayLog> {
  const { start, end } = dayRange(day)
  const rs = await repos(cfg, appDir)
  const [threads, commits, todos, notes, summary] = await Promise.all([
    threadsOn(start, end).then((ts) => classify(ts, rs)),
    commitsOn(rs.list, start, end),
    listTodos().catch(() => []),
    listNotes().catch(() => []),
    readFile(summaryFile(day), 'utf8').catch(() => ''),
  ])
  return {
    day, threads, commits, summary, personalNames: rs.names,
    done: todos.filter((t) => t.status === 'done' && t.doneAt && t.doneAt >= start && t.doneAt < end).map((t) => ({ text: t.text, at: t.doneAt! })),
    notes: notes.filter((n) => n.updated >= start && n.updated < end).map((n) => ({ title: n.title, at: n.updated })),
  }
}

export async function saveSummary(day: string, text: string): Promise<void> {
  mkdirSync(worklogDir(), { recursive: true })
  await writeFile(summaryFile(day), text, 'utf8')
}

const SUMMARY_RULES = [
  'You write a developer\'s end-of-day work log from what their tools recorded: the Claude Code threads they worked in',
  '(title, project, the prompts they typed), their git commits, the to-dos they finished and the notes they changed.',
  'Split it in two: "## Work" (their job) then "## Personal" (their own apps and side projects, the PERSONAL REPOS',
  'given). Each item is marked [work] or [personal] from its folder; a thread marked [work] whose title or prompts are',
  'clearly about a personal repo goes under Personal. Leave out a heading with nothing under it. Under each heading',
  'short bullet points only: one "- " line per piece of work, at most 8 per heading, each at most 20 words, starting',
  'with the project in bold (e.g. "- **Orbit**: added the work log page, released 4.6.0"). Say what was done and where',
  'it stands (shipped, PR open, open, blocked), no detail beyond that. Use only the facts given, never invent; merge',
  'threads that were the same piece of work; skip chit-chat and trivial threads. Plain words, past tense, no em dashes,',
  'no preamble, no closing line. Loose ends go last in their section as one "- **Still open**: a, b, c" bullet.',
].join(' ')

function facts(log: DayLog): string {
  const t = (ms: number) => new Date(ms).toTimeString().slice(0, 5)
  const tag = (personal: boolean) => (personal ? '[personal]' : '[work]')
  const parts: string[] = [`Day: ${log.day}`, `PERSONAL REPOS: ${log.personalNames.join(', ') || 'none known'}`]
  for (const th of log.threads) {
    parts.push(`\nTHREAD ${tag(th.personal)} "${th.title}" in ${th.project || '?'} (${th.prompts} prompts, ${t(th.first)}-${t(th.last)})`)
    for (const a of th.asked.slice(0, 12)) parts.push(`- asked: ${a}`)
  }
  if (log.commits.length) parts.push('\nCOMMITS', ...log.commits.map((c) => `- ${tag(c.personal)} ${c.repo}: ${c.subject}`))
  if (log.done.length) parts.push('\nTO-DOS DONE', ...log.done.map((d) => `- ${d.text}`))
  if (log.notes.length) parts.push('\nNOTES CHANGED', ...log.notes.map((n) => `- ${n.title}`))
  return parts.join('\n').slice(0, 60_000)
}

// Sonnet writes the day's summary from its facts (no tools, nothing saved as a session); the file is overwritten
export async function writeSummary(day: string, cfg: DevConfig | null, appDir: string): Promise<string> {
  if (TEST_MODE) return blocked('the work log summary (it asks Claude)')
  const log = await dayLog(day, cfg, appDir)
  if (!log.threads.length && !log.commits.length && !log.done.length) return ''
  const text = await askClaude('sonnet', SUMMARY_RULES, facts(log), 120_000)
  if (text) await saveSummary(day, `${text}\n`)
  return text
}
