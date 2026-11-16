// a stopped turn's note (Stop, Esc, Send now, Stop everything) is a warning, drawn red
import { test } from 'vitest'
import assert from 'node:assert/strict'
import { applyEvent, type Session } from '../../src/renderer/src/chat'

const base = (): Session => ({ id: 't1', title: 't', items: [], streaming: '', busy: true, loaded: true, background: false })

test('a stopped turn ends with a warn note saying why', () => {
  const s = applyEvent(base(), { type: 'result', subtype: 'success', is_error: false, hive_stopped: true, hive_stopped_why: 'Stopped to send your message' })
  assert.deepEqual(s.items.at(-1), { kind: 'note', warn: true, text: 'Stopped to send your message' })
})

