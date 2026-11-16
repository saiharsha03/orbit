// Background sessions: pure logic for what a live session is doing (working / waiting for the user) and the sidebar rows built from it.
import { applyEvent, toolLine, type Session } from './chat'
import { isWatcherTab } from '../../shared/readOnlyCore'
import { nowLine } from './conversationCore'

export type LiveState = 'working' | 'waiting' | null
// kind 'deploy' / 'dev' / 'script' are the Deploy wizard's, Dev stack's and Scripts' items (not Claude sessions)
// goal: the thread's /goal, while it works toward it (active) or once it is met (the Completed rows)
// retrying: its API calls are being retried (the row blinks); todos: its TodoWrite progress ("3/7")
export type LiveRow = { id: string; kind?: 'deploy' | 'script'; sessionId?: string; title: string; state: 'working' | 'waiting'; note: string; since: number; goal?: { condition: string; status: 'active' | 'met'; setAt: number; metAt?: number }; retrying?: boolean; todos?: string }
// what Claude Code says about a thread beyond working or not (statusCore): retries, its todo list, a cap it stopped at
export type LiveSignals = { retry?: { attempt: number; max: number; error: string } | null; todos?: { done: number; total: number } | null; cap?: string | null }
// the goal a tab's Completed row is for, so opening the thread acknowledges that one (a later goal shows again)
export const goalKey = (id: string, metAt: number | undefined) => `${id}:${metAt ?? 0}`

// waiting (Claude asked, or a background turn ended) wins over working; the visible active tab never carries waiting
export const stateOf = (s: Session): LiveState => (s.waitingSince ? 'waiting' : s.busy ? 'working' : null)

// opening or activating a session shows its tab and clears waiting
export const activateSession = (all: Session[], id: string): Session[] =>
  all.map((s) => (s.id === id ? { ...s, background: false, waitingReason: undefined, waitingSince: undefined } : s))

// the live session for a saved-session id: a tab with that id, or a new tab whose Claude session got that id
export const findSession = (all: Session[], id: string) => all.find((s) => s.id === id || s.sessionId === id)

// Claude's own "waiting" call; ignored while that tab is the one on screen
export function markWaiting(s: Session, reason: string | undefined, now: number, shown: boolean): Session {
  if (shown) return s
  return { ...s, waitingReason: reason?.trim() || 'Needs your input', waitingSince: now }
}

// What Claude said it needs ("waiting" with a reason), kept even when the thread is the one on screen, so the thread
// itself says why it is marked waiting. It stands until the user's next message.
export const withNeeds = (s: Session, reason: string | undefined): Session => ({ ...s, needs: { text: reason?.trim() || 'Needs your input', at: s.items.length } })
export const needsNote = (s: Pick<Session, 'needs' | 'items'>): string =>
  s.needs && !s.items.slice(s.needs.at).some((it) => it.kind === 'user') ? s.needs.text : ''

const firstLine = (t: string) => (t.split('\n').map((l) => l.replace(/^[#>*\-\s`]+/, '').trim()).find(Boolean) ?? '')

// an event for one session; a background turn that ends while the tab is not on screen leaves it waiting for the user
export function onSessionEvent(s: Session, ev: any, activeId: string | null, now: number): Session {
  const next = applyEvent(s, ev)
  if (!s.busy || next.busy || !s.background || activeId === s.id || next.waitingSince) return next
  if (ev.type !== 'result' && ev.type !== 'hive_error') return next
  const last = next.items.at(-1)
  if (last?.kind === 'error') return { ...next, waitingReason: `Failed: ${firstLine(last.text)}`, waitingSince: now }
  // a Watch run only waits for the user when it says so (organize_thread waiting), not every time a scheduled run ends
  if (isWatcherTab(s.id)) return next
  const reply = [...next.items].reverse().find((i) => i.kind === 'assistant')
  const line = reply?.kind === 'assistant' ? firstLine(reply.text) : ''
  return { ...next, waitingReason: line ? `Finished: ${line}` : 'Finished', waitingSince: now }
}

// the latest running tool, as "Ran npm test"
export function activity(s: Session): string {
  const t = [...s.items].reverse().find((i) => i.kind === 'tool' && i.status === 'running')
  if (t?.kind !== 'tool') return 'Working'
  const { verb, what } = toolLine(t.name, t.input)
  return nowLine(verb, what, t.input)
}

// waitingOn: per tab, what it is still waiting on in words (agentStatusCore.ts waitingOn: shells, monitors, subagents,
// workflows, Codex, loops, a wake-up). A thread whose turn ended but which still waits on any of them is working, not
// idle or "Finished"; a question for the user wins.
// goals: per tab, its /goal (statusCore). A thread working toward one carries it on its row; one whose goal is met
// and that has stopped is Completed until you open it (acked holds the goalKey of those opened).
export type LiveGoal = NonNullable<LiveRow['goal']>
export function liveRows(all: Session[], extra: LiveRow[] = [], waitingOn: Map<string, string> = new Map(), goals: Map<string, LiveGoal> = new Map(), acked: Set<string> = new Set(), signals: Map<string, LiveSignals> = new Map()): { waiting: LiveRow[]; working: LiveRow[]; completed: LiveRow[] } {
  const rows: LiveRow[] = [...extra]
  const completed: LiveRow[] = []
  for (const s of all) {
    let state = stateOf(s)
    const on = waitingOn.get(s.id) ?? ''
    const g = goals.get(s.id)
    const finished = (s.waitingReason ?? '').startsWith('Finished')
    const onWork = !!on && !s.busy && (!state || finished)
    if (onWork) state = 'working'
    // a goal still being worked toward keeps the thread under Working even between turns
    const onGoal = g?.status === 'active' && !s.busy && (!state || finished)
    if (onGoal) state = 'working'
    const sig = signals.get(s.id)
    // stopped at a cap (max turns, max budget): it needs a nudge to go on
    if (!s.busy && sig?.cap && (!state || finished)) { rows.push({ id: s.id, sessionId: s.sessionId, title: s.title, state: 'waiting', note: `Stopped at the ${sig.cap === 'budget' ? 'budget' : 'turn'} cap`, since: s.waitingSince ?? Date.now() }); continue }
    if (!state || (finished && g?.status === 'met')) {
      if (g?.status === 'met' && !acked.has(goalKey(s.id, g.metAt))) completed.push({ id: s.id, sessionId: s.sessionId, title: s.title, state: 'working', note: `Goal met: ${g.condition}`, since: g.metAt ?? 0, goal: g })
      continue
    }
    const retry = state === 'working' ? sig?.retry : null
    const note = retry ? `Retrying ${retry.attempt}/${retry.max} · ${retry.error}` : onWork ? `Waiting on ${on}` : onGoal ? `Goal: ${g!.condition}` : state === 'waiting' ? s.waitingReason ?? '' : activity(s)
    const todos = state === 'working' && sig?.todos && sig.todos.total ? `${sig.todos.done}/${sig.todos.total}` : undefined
    rows.push({ id: s.id, sessionId: s.sessionId, title: s.title, state, note, since: s.waitingSince ?? 0, ...(g ? { goal: g } : {}), ...(retry ? { retrying: true } : {}), ...(todos ? { todos } : {}) })
  }
  return {
    waiting: rows.filter((r) => r.state === 'waiting').sort((a, b) => a.since - b.since),
    working: rows.filter((r) => r.state === 'working'),
    completed: completed.sort((a, b) => b.since - a.since),
  }
}
