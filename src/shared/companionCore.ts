// The companion: a small always-on-top window beside the work, with what needs you across Orbit (threads waiting for
// you, failed turns), what is running, deploys being watched and the wake-ups coming up. Pure, so the test can check it.
import type { PhoneThread } from './threadsCore'
import { phoneInbox, type PhoneAsk, type PhoneFail, type PhoneNeed, type PhoneWakeup } from './phoneCore'

export type CompanionThread = { id: string; title: string; folder: string; note: string; updated: number }
export type CompanionState = { needs: PhoneNeed[]; working: CompanionThread[]; coming: PhoneWakeup[] }

export function companionState(threads: PhoneThread[], asks: Record<string, PhoneAsk[]>, fails: PhoneFail[], wakeups: PhoneWakeup[], now: number): CompanionState {
  const { needs, coming } = phoneInbox(threads, asks, fails, wakeups, now, 3)
  const working = threads.filter((t) => t.state === 'working').map((t) => ({ id: t.id, title: t.title, folder: t.folder, note: t.note, updated: t.updated }))
  return { needs, working, coming }
}

// what the collapsed bar says: the count that matters most first
export function companionSummary(s: CompanionState, deploysRunning: number): string {
  const waiting = s.needs.filter((n) => n.kind === 'waiting').length
  const failed = s.needs.length - waiting
  const parts = [
    waiting ? `${waiting} need${waiting > 1 ? '' : 's'} you` : '',
    failed ? `${failed} failed` : '',
    s.working.length ? `${s.working.length} working` : '',
    deploysRunning ? `${deploysRunning} deploy${deploysRunning > 1 ? 's' : ''}` : '',
  ].filter(Boolean)
  return parts.join(' · ') || 'All quiet'
}

// a need is new when its thread and time were not seen yet: the collapsed window opens for it
export const needKey = (n: PhoneNeed) => `${n.kind}:${n.thread}:${n.at}`

// where the window sits: its right edge and bottom kept (it grows upward and leftward), inside the screen's work area
export type Area = { x: number; y: number; width: number; height: number }
export function companionBounds(w: number, h: number, anchor: { right: number; bottom: number } | null, area: Area, margin = 16): { x: number; y: number; width: number; height: number } {
  const width = Math.min(Math.max(1, Math.round(w)), area.width)
  const height = Math.min(Math.max(1, Math.round(h)), area.height)
  const right = anchor ? anchor.right : area.x + area.width - margin
  const bottom = anchor ? anchor.bottom : area.y + area.height - margin
  const x = Math.min(Math.max(area.x, right - width), area.x + area.width - width)
  const y = Math.min(Math.max(area.y, bottom - height), area.y + area.height - height)
  return { x, y, width, height }
}
