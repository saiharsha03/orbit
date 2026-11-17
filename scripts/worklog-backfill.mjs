// Write the work log summary for every day in a range, outside the app (the same code the Work log page runs).
// node scripts/worklog-backfill.mjs <from YYYY-MM-DD> <to YYYY-MM-DD> [--root <repos root>] [--agents a,b] [--force] [--dry]
// A day that already has a summary is skipped unless --force; a day with nothing in it is skipped.
import { registerHooks } from 'node:module'
import { tmpdir } from 'node:os'
import { dirname, join, resolve } from 'node:path'
import { existsSync } from 'node:fs'
import { fileURLToPath, pathToFileURL } from 'node:url'

const here = dirname(fileURLToPath(import.meta.url))
// main's TypeScript runs on Node's own type stripping: extensionless imports get .ts, electron gets a stub
registerHooks({
  resolve(spec, ctx, next) {
    if (spec === 'electron') return { url: `data:text/javascript,export const app={getPath:()=>${JSON.stringify(tmpdir())}}`, shortCircuit: true }
    if ((spec.startsWith('./') || spec.startsWith('../')) && ctx.parentURL?.startsWith('file:') && !/\.\w+$/.test(spec)) {
      const p = resolve(dirname(fileURLToPath(ctx.parentURL)), spec)
      if (existsSync(`${p}.ts`)) return { url: pathToFileURL(`${p}.ts`).href, shortCircuit: true }
    }
    return next(spec, ctx)
  },
})

const args = process.argv.slice(2)
const opt = (k, d) => { const i = args.indexOf(k); return i >= 0 ? args[i + 1] : d }
const [from, to] = args.filter((a, i) => !a.startsWith('--') && !args[i - 1]?.startsWith('--'))
if (!from || !to) { console.error('usage: node scripts/worklog-backfill.mjs <from> <to> [--root dir] [--agents a,b] [--force]'); process.exit(2) }
const cfg = { root: opt('--root', resolve(here, '..', '..')), agentsFolders: opt('--agents', '').split(',').filter(Boolean), pullSkip: [], worktreesDir: '', stacks: [], services: [] }
const force = args.includes('--force')
const appDir = resolve(here, '..') // Orbit's own repo: its GitHub owner marks the personal repos
const dry = args.includes('--dry') // list what each day holds, write nothing

const { dayLog, writeSummary } = await import(pathToFileURL(join(here, '..', 'src', 'main', 'worklog.ts')).href)
const { shiftDay } = await import(pathToFileURL(join(here, '..', 'src', 'shared', 'worklogCore.ts')).href)

for (let day = from; day <= to; day = shiftDay(day, 1)) {
  const log = await dayLog(day, cfg, appDir)
  const what = `${log.threads.length} threads, ${log.commits.length} commits, ${log.done.length} to-dos`
  if (dry) { console.log(`${day}  ${what}`); for (const t of log.threads) console.log(`    ${t.personal ? 'personal' : 'work    '}  ${t.project}: ${t.title} (${t.prompts})`); continue }
  if (log.summary && !force) { console.log(`${day}  kept (has a summary)  ${what}`); continue }
  if (!log.threads.length && !log.commits.length && !log.done.length) { console.log(`${day}  nothing that day`); continue }
  const text = await writeSummary(day, cfg, appDir)
  console.log(`${day}  ${text ? 'written' : 'NOT WRITTEN'}  ${what}`)
}
