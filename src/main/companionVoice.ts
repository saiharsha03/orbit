// Orbie's voice. Two kinds of line, both written by Haiku on its own, outside every thread (no tools, no project
// settings, nothing saved), each with a face for Orbie to pull while it shows:
// - news, while you are away from Orbit: a thread finished or failed, a question or a permission waits, a deploy landed.
//   Events close together are said in one line. If Haiku cannot be reached the plain facts are said instead.
// - greetings: good morning, welcome back, the evening wrap (index.ts decides when, from the facts it gathers).
// What happened while you were away is kept (awayLog) so a greeting can tell it.
import { query } from '@anthropic-ai/claude-agent-sdk'
import { tmpdir } from 'node:os'
import { claudeExecutable } from './engine'
import { TEST_MODE } from './testMode'
import { FACE_COLORS, MAX_PART, MAX_SHAPES, parseFaceSvg, type FaceShape } from '../shared/orbieFaceCore'

export type VoiceEvent = { kind: 'done' | 'fail' | 'ask' | 'deploy'; title: string; text: string }
export type SayKind = VoiceEvent['kind'] | 'greet'
// the face Haiku drew for the line (orbieFaceCore.ts cleans it); empty when it drew none Orbit can use
export type Face = FaceShape[]
type Sink = { away: () => boolean; say: (line: string, kind: SayKind, face: Face) => void }

const GATHER_MS = 4_000 // events this close together are said in one line
const MIN_GAP_MS = 20_000 // at most one line in this long; later ones wait and are said together
const WHO = 'You are Orbie, a small round planet with a ring who lives in the corner of a developer\'s screen and watches their Claude Code threads (Orbit is their app).'
const FACE_RULE = [
  'Also draw the face Orbie pulls while saying it, to fit the mood: Orbie is a planet, a circle of radius 12 at 20,20 on a 40 x 40 SVG view.',
  'The planet and its ring are already drawn: never draw them. Draw only its two eyes (near 15,18.5 and 25,18.5, always both) and its mouth (near 20,25),',
  `plus, if it fits, a tiny extra or two within the view (a hat above y 8, hearts, sparkles, a tear, sweat drop, blush); no shape bigger than radius ${MAX_PART}.`,
  'Make every face different and specific to this moment; never default to a plain smile. A few to show the range (invent your own, do not copy):',
  'heart eyes: <path d="M13 18q0-2 2-1q2-1 2 1l-2 2.5z" fill="pink"/> <path d="M23 18q0-2 2-1q2-1 2 1l-2 2.5z" fill="pink"/> <path d="M16 24q4 4 8 0z" fill="ink"/>;',
  'sleepy yawn: <path d="M13 19h4M23 19h4" stroke="ink" stroke-width="1.4"/> <ellipse cx="20" cy="25.5" rx="2" ry="2.6" fill="ink"/>;',
  'cool shades: <rect x="11.5" y="16.5" width="7" height="3.5" rx="1.5" fill="ink"/> <rect x="21.5" y="16.5" width="7" height="3.5" rx="1.5" fill="ink"/> <path d="M18.5 18h3" stroke="ink" stroke-width="1"/> <path d="M17 25q3 1.5 6-.5" stroke="ink" stroke-width="1.4"/>.',
  `Use only <path>, <circle>, <ellipse>, <line>, <polyline>, <rect> with numeric attributes, at most ${MAX_SHAPES} shapes,`,
  `fill and stroke only by name: ${Object.keys(FACE_COLORS).join(', ')} (ink is the face colour); stroke-width 0.8 to 2.`,
  'Be expressive and silly: winks, heart eyes, tongue out, swirly dizzy eyes, star eyes, a big grin, a yawn.',
  'Answer in exactly two lines. Line 1: DRAW: followed by the shapes. Line 2: SAY: followed by what Orbie says.',
].join(' ')
const NEWS = [WHO,
  'Write ONE short line (at most 16 words) telling them what just happened, from the events given.',
  'Be warm and a little playful (one emoji at most, no exclamation spam), but every fact must come from the events: the thread names, what finished, what failed and why, what is waiting.',
  'If something needs them (a question, a permission, a failure), say that first and plainly. No pet names or nicknames.', FACE_RULE].join(' ')
const GREET = [WHO,
  'Greet them for the moment given (good morning, welcome back, evening wrap) in at most 28 words: playful, a pun is welcome, one or two emoji.',
  'Never call them anything: no pet names or nicknames (sunshine, buddy, friend, champ, ring-bearer...), just "Good morning!" and on.',
  'Weave in the facts that matter (what finished or failed while they were away, what is waiting for them, what is still running, usage left) using only the facts given; never invent any.',
  'Something that needs them comes first. On a Friday afternoon or evening a weekend nudge is fine.', FACE_RULE].join(' ')

let sink: Sink | null = null
let queued: VoiceEvent[] = []
let timer: ReturnType<typeof setTimeout> | null = null
let lastSaid = 0
let speaking = false
// what happened, kept for the next greeting (the last 30 events, with when)
const awayLog: (VoiceEvent & { at: number })[] = []

export function initVoice(s: Sink): void { sink = s }

// something happened: kept for the greetings, and said now while you are away from Orbit (its window not focused)
export function voiceEvent(e: VoiceEvent): void {
  if (TEST_MODE) return
  const ev = { ...e, text: e.text.replace(/\s+/g, ' ').slice(0, 300) }
  awayLog.push({ ...ev, at: Date.now() })
  if (awayLog.length > 30) awayLog.splice(0, awayLog.length - 30)
  if (!sink || !sink.away()) return
  queued.push(ev)
  if (queued.length > 8) queued = queued.slice(-8)
  schedule()
}

// what happened since then, one line each, for a greeting's facts
export const happenedSince = (since: number): string[] =>
  awayLog.filter((e) => e.at >= since).map((e) => `${e.kind === 'fail' ? 'failed' : e.kind === 'ask' ? 'needs you' : e.kind === 'deploy' ? 'deploy landed' : 'finished'}: ${e.title}${e.text ? ` (${e.text.slice(0, 100)})` : ''}`)

// a greeting (moment: "good morning", "welcome back after 2 hours away", ...), said whether or not Orbit is focused
export async function greet(moment: string, facts: string[]): Promise<void> {
  if (TEST_MODE || !sink) return
  let out: { line: string; face: Face } = { line: '', face: [] }
  try { out = parseFace(await haiku(GREET, `Moment: ${moment}\nFacts:\n${facts.map((f) => `- ${f}`).join('\n') || '- nothing new'}`)) } catch { /* no greeting rather than a dull one */ }
  if (out.line) { lastSaid = Date.now(); sink.say(out.line, 'greet', out.face) }
}

function schedule(): void {
  if (timer) return
  const wait = Math.max(GATHER_MS, lastSaid + MIN_GAP_MS - Date.now())
  timer = setTimeout(() => { timer = null; void speak() }, wait)
}

const plain = (es: VoiceEvent[]) => es.map((e) => `${e.kind === 'fail' ? 'Failed' : e.kind === 'ask' ? 'Needs you' : e.kind === 'deploy' ? 'Landed' : 'Done'}: ${e.title}`).join(' · ')
const worst = (es: VoiceEvent[]): VoiceEvent['kind'] => (['fail', 'ask', 'deploy', 'done'] as const).find((k) => es.some((e) => e.kind === k)) ?? 'done'

async function speak(): Promise<void> {
  if (speaking || !queued.length || !sink) return
  const es = queued
  queued = []
  speaking = true
  lastSaid = Date.now()
  let out: { line: string; face: Face } = { line: '', face: [] }
  try { out = parseFace(await haiku(NEWS, `Events:\n${es.map((e) => `- ${e.kind}: "${e.title}"${e.text ? `: ${e.text}` : ''}`).join('\n')}`)) } catch { /* the plain line below */ }
  speaking = false
  if (sink.away()) sink.say(out.line || plain(es), worst(es), out.face)
  if (queued.length) schedule()
}

// "DRAW: <shapes>" then "SAY: the line" -> the cleaned face and the line (an answer without SAY: is all line)
export function parseFace(text: string): { line: string; face: Face } {
  const draw = /DRAW:\s*([\s\S]*?)(?=\n\s*SAY:|$)/i.exec(text)
  const say = /SAY:\s*([\s\S]*)$/i.exec(text)
  const face = draw ? parseFaceSvg(draw[1]) : []
  const line = (say ? say[1] : draw ? '' : text).trim().replace(/^"|"$/g, '').slice(0, 220)
  return { line, face }
}

async function haiku(system: string, prompt: string): Promise<string> {
  const q = query({
    prompt,
    options: {
      cwd: tmpdir(), pathToClaudeCodeExecutable: claudeExecutable(), model: 'haiku', systemPrompt: system,
      settingSources: [], persistSession: false, maxTurns: 1, allowedTools: [],
      canUseTool: async () => ({ behavior: 'deny' as const, message: 'Orbie only talks.' }),
    },
  })
  let text = ''
  const stop = setTimeout(() => q.close(), 30_000)
  try {
    for await (const m of q) {
      if (m.type === 'assistant') for (const b of (m.message.content as { type: string; text?: string }[])) if (b.type === 'text') text += b.text ?? ''
      if (m.type === 'result') break
    }
  } finally { clearTimeout(stop); q.close() }
  return text.trim()
}
