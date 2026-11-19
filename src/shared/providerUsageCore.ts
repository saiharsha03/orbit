// Codex's and Antigravity's usage answers turned into the side bar's rows (keys "codex:five_hour", "gemini:seven_day"),
// so they draw like Claude's. Pure, for tests.
import type { UsageRow } from './limitsCore'

const iso = (s: unknown) => (typeof s === 'string' && s ? s : null)

// Codex app server, account/rateLimits/read: primary is the 5-hour window, secondary the weekly one (by their length)
export function codexRows(r: any): UsageRow[] | null {
  const rl = r?.rateLimits
  if (!rl) return null
  const rows: UsageRow[] = []
  for (const w of [rl.primary, rl.secondary]) {
    if (!w || typeof w.usedPercent !== 'number') continue
    const key = (w.windowDurationMins ?? 0) <= 300 ? 'codex:five_hour' : 'codex:seven_day'
    rows.push({ key, label: key.endsWith('five_hour') ? 'Codex 5-hour' : 'Codex weekly', pct: w.usedPercent, resetsAt: typeof w.resetsAt === 'number' ? new Date(w.resetsAt * 1000).toISOString() : null })
  }
  return rows.length ? rows : null
}

// agy -p /usage --output-format json: the Gemini group's 5-hour and weekly buckets (remaining, so used = 1 - that)
export function geminiRows(r: any): UsageRow[] | null {
  const group = r?.command?.data?.groups?.find((g: any) => /^gemini models$/i.test(String(g?.name ?? ''))) ?? r?.command?.data?.groups?.[0]
  const rows: UsageRow[] = []
  for (const b of group?.buckets ?? []) {
    if (typeof b?.remaining_fraction !== 'number') continue
    const key = b.window === '5h' ? 'gemini:five_hour' : b.window === 'weekly' ? 'gemini:seven_day' : null
    if (key) rows.push({ key, label: key.endsWith('five_hour') ? 'Gemini 5-hour' : 'Gemini weekly', pct: (1 - b.remaining_fraction) * 100, resetsAt: iso(b.reset_time) })
  }
  return rows.length ? rows.sort((a, b) => a.key.localeCompare(b.key)) : null
}
