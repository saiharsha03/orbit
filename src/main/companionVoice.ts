// Orbie's voice: while you are away from Orbit, what happens in it (a thread finished or failed, a question or a
// permission waits, a deploy landed) comes back as one short line in Orbie's speech bubble. Haiku writes the line on
// its own, outside every thread: no tools, no project settings, nothing saved. Events that come close together are said
// in one line. If Haiku cannot be reached the plain facts are said instead.
import { query } from '@anthropic-ai/claude-agent-sdk'
import { tmpdir } from 'node:os'
import { claudeExecutable } from './engine'
import { TEST_MODE } from './testMode'

export type VoiceEvent = { kind: 'done' | 'fail' | 'ask' | 'deploy'; title: string; text: string }
type Sink = { away: () => boolean; say: (line: string, kind: VoiceEvent['kind']) => void }

const GATHER_MS = 4_000 // events this close together are said in one line
const MIN_GAP_MS = 20_000 // at most one line in this long; later ones wait and are said together
const ORBIE = [
  'You are Orbie, a small round planet with a ring who lives in the corner of a developer\'s screen and watches their Claude Code threads.',
  'Write ONE short line (at most 16 words) telling them what just happened, from the events given.',
  'Be warm and a little playful (one emoji at most, no exclamation spam), but every fact must come from the events: the thread names, what finished, what failed and why, what is waiting.',
  'If something needs them (a question, a permission, a failure), say that first and plainly. Reply with the line only.',
].join(' ')

let sink: Sink | null = null
let queued: VoiceEvent[] = []
let timer: ReturnType<typeof setTimeout> | null = null
let lastSaid = 0
let speaking = false

export function initVoice(s: Sink): void { sink = s }

// something happened; said only while you are away from Orbit (its window not focused)
export function voiceEvent(e: VoiceEvent): void {
  if (TEST_MODE || !sink || !sink.away()) return
  queued.push({ ...e, text: e.text.replace(/\s+/g, ' ').slice(0, 300) })
  if (queued.length > 8) queued = queued.slice(-8)
  schedule()
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
  let line = ''
  try { line = await haiku(es.map((e) => `- ${e.kind}: "${e.title}"${e.text ? `: ${e.text}` : ''}`).join('\n')) } catch { /* the plain line below */ }
  speaking = false
  if (sink.away()) sink.say(line || plain(es), worst(es))
  if (queued.length) schedule()
}

async function haiku(events: string): Promise<string> {
  const q = query({
    prompt: `Events:\n${events}`,
    options: {
      cwd: tmpdir(), pathToClaudeCodeExecutable: claudeExecutable(), model: 'haiku', systemPrompt: ORBIE,
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
  return text.trim().replace(/^"|"$/g, '').slice(0, 160)
}
