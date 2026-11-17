// The work log's types and its local-day arithmetic (shared by main and the window).
export type LogThread = { id: string; title: string; project: string; cwd: string; path: string; started: number; updated: number; personal: boolean; prompts: number; first: number; last: number; asked: string[] }
export type LogCommit = { repo: string; hash: string; at: number; subject: string; personal: boolean }
export type DayLog = {
  day: string
  threads: LogThread[]
  commits: LogCommit[]
  done: { text: string; at: number }[]
  notes: { title: string; at: number }[]
  summary: string
  personalNames: string[] // the personal repos' names (their GitHub owner is Orbit's own)
}

export const isDay = (d: string): boolean => /^\d{4}-\d{2}-\d{2}$/.test(d) && !Number.isNaN(new Date(`${d}T00:00:00`).getTime())

const pad = (n: number) => String(n).padStart(2, '0')
/** the local day of a time, YYYY-MM-DD */
export const dayOf = (ms: number): string => { const d = new Date(ms); return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}` }

/** local midnight to the next local midnight (a daylight-saving day is 23 or 25 hours) */
export function dayRange(day: string): { start: number; end: number } {
  const [y, m, d] = day.split('-').map(Number)
  return { start: new Date(y, m - 1, d).getTime(), end: new Date(y, m - 1, d + 1).getTime() }
}

/** the day n days after (negative: before) */
export function shiftDay(day: string, n: number): string {
  const [y, m, d] = day.split('-').map(Number)
  return dayOf(new Date(y, m - 1, d + n).getTime())
}
