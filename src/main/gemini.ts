// Gemini as a subagent through Google's Antigravity CLI (`agy`): one fresh headless `agy -p` run per task, on the
// user's own Antigravity login (sign in once by running `agy`). Its stream-json events become short activity lines for
// the live box in the tab (the same box as Codex).
import { spawn, type ChildProcess } from 'node:child_process'
import { existsSync } from 'node:fs'
import { homedir } from 'node:os'
import { delimiter, join } from 'node:path'
import type { CodexResult } from './codex'

let exe: string | null | undefined
export function agyExecutable(): string | null {
  if (exe === undefined) {
    const name = process.platform === 'win32' ? 'agy.exe' : 'agy'
    // the installer puts it in %LOCALAPPDATA%\agy\bin without adding that to PATH
    const dirs = [...(process.env.PATH ?? '').split(delimiter), join(process.env.LOCALAPPDATA ?? join(homedir(), 'AppData', 'Local'), 'agy', 'bin'), join(homedir(), '.local', 'bin')]
    exe = dirs.map((d) => join(d, name)).find((p) => existsSync(p)) ?? null
  }
  return exe
}

export type GeminiRun = { task: string; folder: string; model?: string; effort?: string; allowEdits?: boolean; onLine: (line: string) => void }

const brief = (v: unknown) => (typeof v === 'string' ? v : JSON.stringify(v ?? '')).replace(/\s+/g, ' ').slice(0, 160)

// one step_update -> a line for the live box: tool calls when they finish, a failed tool's error
function describe(u: any): string | null {
  if (u?.step_type !== 'tool' || u.state !== 'DONE') return null
  const t = u.tool_info ?? {}
  const p = t.parameters ?? {}
  const what = p.CommandLine ?? p.command ?? p.AbsolutePath ?? p.Path ?? p.path ?? p.file_path ?? p.Query ?? p.query ?? p
  return t.error ? `${t.name ?? u.tool_name ?? 'tool'} failed: ${brief(t.error.message ?? t.error)}` : `${t.name ?? u.tool_name ?? 'tool'}: ${brief(what)}`
}

export function runGemini(run: GeminiRun, track: (p: ChildProcess) => void): Promise<CodexResult> {
  const bin = agyExecutable()
  if (!bin) return Promise.resolve({ ok: false, answer: 'The Antigravity CLI (agy) is not installed: see antigravity.google/product/antigravity-cli.' })
  // A headless agy run approves edits and commands by itself (tried 11-19: it wrote a file and ran echo in its default
  // mode), so the limits are deny rules for this run only (--settings is never saved): no commands, MCP tools or browser,
  // and no file writes unless allowEdits. Only the rule form `kind(*)` takes effect; bare tool names are ignored.
  const deny = ['command(*)', 'mcp(*)', 'browser(*)', ...(run.allowEdits ? [] : ['write_file(*)'])]
  const args = ['-p', run.task, '--output-format', 'stream-json', '--settings', JSON.stringify({ permissions: { deny } })]
  if (run.model) args.push('--model', run.model)
  if (run.effort) args.push('--effort', run.effort)

  return new Promise((resolve) => {
    const p = spawn(bin, args, { cwd: run.folder, windowsHide: true })
    track(p)
    let answer = ''
    let result: any = null
    let err = ''
    let buf = ''
    p.stderr.on('data', (c) => { err = (err + c.toString()).slice(-2000) })
    p.stdout.on('data', (chunk) => {
      buf += chunk.toString()
      let nl: number
      while ((nl = buf.indexOf('\n')) >= 0) {
        const line = buf.slice(0, nl).trim()
        buf = buf.slice(nl + 1)
        if (!line.startsWith('{')) continue
        let e: any
        try { e = JSON.parse(line) } catch { continue }
        if (e.event === 'result') { result = e.result ?? e; continue }
        if (e.event !== 'step_update') continue
        const u = e.step_update ?? {}
        // the reply streams in as text deltas; a tool step in between starts a new reply
        if (u.step_type === 'agent_response' && typeof u.text_delta === 'string') answer += u.text_delta
        if (u.step_type === 'tool' && u.state === 'ACTIVE' && answer.trim()) { run.onLine(answer.trim().slice(0, 200)); answer = '' }
        const d = describe(u)
        if (d) run.onLine(d)
      }
    })
    p.on('error', (e) => resolve({ ok: false, answer: `agy could not start: ${e.message}` }))
    p.on('close', (code) => {
      const text = String(result?.response ?? '').trim() || answer.trim()
      const ok = code === 0 && (!result || result.status === 'SUCCESS')
      resolve(ok
        ? { ok: true, answer: text || '(Gemini finished without a final message)' }
        : { ok: false, answer: result?.error?.message ?? result?.error ?? (err.trim().split('\n').slice(-3).join('\n') || text || `agy exited with code ${code}`) })
    })
    p.stdin.end()
  })
}
