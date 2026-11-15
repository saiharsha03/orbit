// A thread's two numbers, for the thread header and each side bar row: how much of the context window it uses, and
// until when its prompt cache stays warm (its last reply plus the cache's 5 minutes or hour, from the transcript).
import { useEffect, useState } from 'react'
import { contextPct } from '../../shared/contextCore'
import { getPref, setPref } from './prefs'

export const mmss = (ms: number) => { const s = Math.ceil(ms / 1000); return `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')}` }

// each model's context window, as a running thread on it last said (kept across restarts): a thread that is not running
// only has its transcript's tokens and model, so its share is known once a thread on that model has run (else tokens)
const windows: Record<string, number> = getPref<Record<string, number>>('context.windows', {})

/** every finished turn says each model's context window (its result's modelUsage): kept for the threads not running */
export function learnWindows(usage: Record<string, { contextWindow?: number }> | undefined): void {
  let changed = false
  for (const [model, u] of Object.entries(usage ?? {})) if (u.contextWindow && windows[model] !== u.contextWindow) { windows[model] = u.contextWindow; changed = true }
  if (changed) setPref('context.windows', windows)
}

// before a turn on a model has said its window: the 5.x Opus, Sonnet, Fable and Mythos models have a 1M one, Haiku 200k
const guessWindow = (model: string): number | null => (/haiku/i.test(model) ? 200_000 : /(opus|sonnet|fable|mythos)-5/i.test(model) ? 1_000_000 : null)

export type Meters = { total: number | null; max: number | null; pct: number | null; until: number | null; ttl: number }
/** read once, and every 15 s while the thread runs (live: it has a process to ask for its exact context) */
export function useThreadMeters(path: string | undefined, tabId: string | undefined, live: boolean): Meters {
  const [m, setM] = useState<Meters>({ total: null, max: null, pct: null, until: null, ttl: 300_000 })
  useEffect(() => {
    let alive = true
    const load = async () => {
      const [st, c] = await Promise.all([path ? window.api.sessionStats(path).catch(() => null) : null, live && tabId ? window.api.chatContext(tabId).catch(() => null) : null])
      if (c?.max && st?.model && windows[st.model] !== c.max) { windows[st.model] = c.max; setPref('context.windows', windows) }
      const total = c?.total ?? st?.contextTokens ?? null
      const max = c?.max ?? (st?.model ? windows[st.model] ?? guessWindow(st.model) : null)
      if (alive) setM({ total, max, pct: total === null || !max ? null : contextPct(total, max), until: st?.lastReplyAt ? st.lastReplyAt + st.cacheTtlMs : null, ttl: st?.cacheTtlMs ?? 300_000 })
    }
    void load()
    const t = live ? setInterval(load, 15_000) : undefined
    return () => { alive = false; clearInterval(t) }
  }, [path, tabId, live])
  return m
}

/** the cache in a word: "warm" while a turn runs, the time left, or "cold"; null when not known */
// level: what is left of the cache's window: good (over a third), low (under a third), last (under a tenth), cold
export type CacheLevel = 'good' | 'low' | 'last' | 'cold'
export function cacheWord(until: number | null, now: number, busy: boolean, ttl = 300_000): { word: string; level: CacheLevel; title: string } | null {
  if (until === null) return null
  if (busy) return { word: 'warm', level: 'good', title: 'Prompt cache: this turn keeps it warm' }
  const left = until - now
  if (left <= 0) return { word: 'cold', level: 'cold', title: 'Prompt cache cold: the next message re-reads the whole context' }
  const level: CacheLevel = left < ttl / 10 ? 'last' : left < ttl / 3 ? 'low' : 'good'
  return { word: mmss(left), level, title: `Prompt cache warm for ${mmss(left)} more. After that the next message re-reads the whole context` }
}
