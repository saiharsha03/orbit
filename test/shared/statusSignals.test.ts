import { test } from 'vitest'
import assert from 'node:assert/strict'
import { emptyStatus, reduceStatus } from '../../src/shared/statusCore'

test('api_retry, TodoWrite, permission_denied and caps are kept, and an answer clears the retry', () => {
  let st = reduceStatus(emptyStatus(), { type: 'system', subtype: 'api_retry', attempt: 2, max_retries: 10, error: 'overloaded' }, 1)
  assert.deepEqual(st.retry, { attempt: 2, max: 10, error: 'overloaded', at: 1 })
  st = reduceStatus(st, { type: 'assistant', message: { model: 'claude-opus-5-5', content: [{ type: 'tool_use', name: 'TodoWrite', input: { todos: [{ status: 'completed', content: 'a' }, { status: 'in_progress', content: 'b', activeForm: 'Doing b' }, { status: 'pending', content: 'c' }] } }] } }, 2)
  assert.equal(st.retry, null)
  assert.deepEqual(st.todos, { done: 1, total: 3, now: 'Doing b' })
  st = reduceStatus(st, { type: 'system', subtype: 'permission_denied', tool_name: 'mcp__orbit__codex' }, 3)
  assert.deepEqual(st.denied, { tool: 'codex', at: 3 })
  st = reduceStatus(st, { type: 'result', subtype: 'error_max_turns' }, 4)
  assert.equal(st.cap, 'turns')
})
