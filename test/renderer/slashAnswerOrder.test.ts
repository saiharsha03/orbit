import { test } from 'vitest'
import assert from 'node:assert/strict'
import { applyEvent } from '../../src/renderer/src/chat'

// "/design-login" sent, Claude Code answers it itself (a synthetic message): the answer goes below the command, which is read
test('a slash command answered by Claude Code itself stays above its answer', () => {
  const s0 = { items: [{ kind: 'user', text: '/design-login', images: [], uuid: 'u1' }] } as any
  const s1 = applyEvent(s0, { type: 'assistant', uuid: 'a1', message: { model: '<synthetic>', content: [{ type: 'text', text: "/design-login isn't available in this environment." }] } } as any)
  assert.deepEqual(s1.items.map((i: any) => i.kind), ['user', 'assistant'])
  assert.equal((s1.items[0] as { seen?: boolean }).seen, true)
})
