import { test } from 'vitest'
import assert from 'node:assert/strict'
import { reconcile, splitWith, type PaneState } from '../../src/renderer/src/panes'

// Split from a thread: the new thread is first placed in the focused pane (reconcile sees a thread with no tab), then
// split moves it to a new pane. It must end up in the new pane only, with the old thread back in front where it was.
test('split moves the new thread beside, it is not shown in both panes', () => {
  const one: PaneState = { panes: [{ id: 'p1', tabs: ['t1'], active: 't1' }], focus: 'p1' }
  const placed = reconcile(one, ['t1', 't2'])
  const s = splitWith(placed, 't2', 'p1')!
  assert.deepEqual(s.panes.map((p) => [p.tabs, p.active]), [[['t1'], 't1'], [['t2'], 't2']])
})
