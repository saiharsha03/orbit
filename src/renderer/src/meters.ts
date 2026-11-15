// A thread's two numbers, for the thread header and each side bar row: how much of the context window it uses, and
// until when its prompt cache stays warm (its last reply plus the cache's 5 minutes or hour, from the transcript).
import { useEffect, useState } from 'react'
import { contextPct } from '../../shared/contextCore'

export const mmss = (ms: number) => { const s = Math.ceil(ms / 1000); return `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')}` }

// the context window of the last running thread asked: a thread that is not running only has its transcript's tokens
let lastMax = 200_000

export type Meters = { total: number | null; max: number; pct: number | null; until: number | null }
/** read once, and every 15 s while the thread runs (live: it has a process to ask for its exact context) */
export function useThreadMeters(path: string | undefined, tabId: string | undefined, live: boolean): Meters {
  const [m, setM] = useState<Meters>({ total: null, max: lastMax, pct: null, until: null })
  useEffect(() => {
    let alive = true
    const load = async () => {
      const [st, c] = await Promise.all([path ? window.api.sessionStats(path).catch(() => null) : null, live && tabId ? window.api.chatContext(tabId).catch(() => null) : null])
      if (c?.max) lastMax = c.max
      const total = c?.total ?? st?.contextTokens ?? null
      const max = c?.max ?? lastMax
      if (alive) setM({ total, max, pct: total === null ? null : contextPct(total, max), until: st?.lastReplyAt ? st.lastReplyAt + st.cacheTtlMs : null })
    }
    void load()
    const t = live ? setInterval(load, 15_000) : undefined
    return () => { alive = false; clearInterval(t) }
  }, [path, tabId, live])
  return m
}

/** the cache in a word: "warm" while a turn runs, the time left, or "cold"; null when not known */
export function cacheWord(until: number | null, now: number, busy: boolean): { word: string; warm: boolean; title: string } | null {
  if (until === null) return null
  if (busy) return { word: 'warm', warm: true, title: 'Prompt cache: this turn keeps it warm' }
  const left = until - now
  return left > 0
    ? { word: mmss(left), warm: true, title: `Prompt cache warm for ${mmss(left)} more. After that the next message re-reads the whole context` }
    : { word: 'cold', warm: false, title: 'Prompt cache cold: the next message re-reads the whole context' }
}
