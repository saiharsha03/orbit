// Orbit's idle compaction says so in its note; any other compaction keeps the usual one
import { test } from 'vitest'
import assert from 'node:assert/strict'
import { applyEvent, type Session } from '../../src/renderer/src/chat'

const base = (): Session => ({ id: 't1', title: 't', items: [], streaming: '', busy: false, loaded: true, background: false })
const boundary = { type: 'system', subtype: 'compact_boundary', compact_metadata: { trigger: 'manual' } }
const lastNote = (s: Session) => (s.items.at(-1) as { kind: string; text: string }).text

test('an idle compaction gets the idle note, once', () => {
  let s = applyEvent(base(), { type: 'hive_idle_compact' })
  s = applyEvent(s, boundary)
  assert.match(lastNote(s), /^Compacted while idle, before the prompt cache expired/)
  s = applyEvent(s, boundary)
  assert.equal(lastNote(s), 'Conversation compacted to free up context')
})
