// a thread whose turn ended but which still waits on its background work or schedule is listed as working
import { test } from 'vitest'
import assert from 'node:assert/strict'
import { liveRows } from '../../src/renderer/src/background'
import { waitingOn } from '../../src/shared/agentStatusCore'
import { emptyStatus } from '../../src/shared/statusCore'
import type { Session } from '../../src/renderer/src/chat'

const s = (id: string, over: Partial<Session> = {}): Session => ({ id, title: id, items: [], streaming: '', busy: false, loaded: true, background: false, ...over })

test('idle or finished threads still waiting on something are working; a question still waits; nothing, no row', () => {
  const all = [
    s('idle'),
    s('finished', { waitingReason: 'Finished: done', waitingSince: 1 }),
    s('asks', { waitingReason: 'Which account?', waitingSince: 2 }),
    s('quiet'),
  ]
  const r = liveRows(all, [], new Map([['idle', '1 shell'], ['finished', '2 agents'], ['asks', '1 agent'], ['quiet', '']]))
  assert.deepEqual(r.working.map((x) => [x.id, x.note]), [['idle', 'Waiting on 1 shell'], ['finished', 'Waiting on 2 agents']])
  assert.deepEqual(r.waiting.map((x) => x.id), ['asks'])
})

test('waitingOn names running shells, monitors and agents, not the schedule', () => {
  const task = (kind: string, status = 'running') => ({ id: kind, kind, status, title: kind }) as never
  const st = { ...emptyStatus(), tasks: [task('local_bash'), task('monitor'), task('local_agent'), task('local_agent'), task('local_bash', 'completed')], crons: [{ id: 'c', schedule: '*/5', recurring: true, prompt: 'p' }] }
  assert.equal(waitingOn(st), '1 shell, 1 monitor, 2 agents') // the loop is scheduled, not running
  assert.equal(waitingOn({ ...emptyStatus(), nextWake: Date.now() + 60_000 }), '')
  assert.equal(waitingOn(emptyStatus()), '')
})

test('a goal keeps its thread under Working; a met goal is Completed until opened', () => {
  const g = (status: 'active' | 'met', metAt?: number) => ({ condition: 'tests pass', status, setAt: 1, ...(metAt ? { metAt } : {}) })
  const all = [s('going'), s('done'), s('seen'), s('busy', { busy: true })]
  const goals = new Map([['going', g('active')], ['done', g('met', 5)], ['seen', g('met', 6)], ['busy', g('active')]])
  const r = liveRows(all, [], new Map(), goals, new Set(['seen:6']))
  assert.deepEqual(r.working.map((x) => [x.id, x.goal?.status]), [['going', 'active'], ['busy', 'active']])
  assert.equal(r.working[0].note, 'Goal: tests pass')
  assert.deepEqual(r.completed.map((x) => [x.id, x.note]), [['done', 'Goal met: tests pass']])
})

test('Claude Code signals: retries blink the row, todos count, a cap needs you', () => {
  const r = liveRows([s('slow', { busy: true }), s('capped')], [], new Map(), new Map(), new Set(), new Map([
    ['slow', { retry: { attempt: 2, max: 10, error: 'overloaded' }, todos: { done: 3, total: 7 } }],
    ['capped', { cap: 'turns' }],
  ]))
  assert.deepEqual(r.working.map((x) => [x.note, x.retrying, x.todos]), [['Retrying 2/10 · overloaded', true, '3/7']])
  assert.deepEqual(r.waiting.map((x) => [x.id, x.note]), [['capped', 'Stopped at the turn cap']])
})
