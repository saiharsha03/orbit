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

test('waitingOn names shells, monitors, agents, loops and a wake-up', () => {
  const task = (kind: string, status = 'running') => ({ id: kind, kind, status, title: kind }) as never
  const st = { ...emptyStatus(), tasks: [task('local_bash'), task('monitor'), task('local_agent'), task('local_agent'), task('local_bash', 'completed')], crons: [{ id: 'c', schedule: '*/5', recurring: true, prompt: 'p' }] }
  assert.equal(waitingOn(st), '1 shell, 1 monitor, 2 agents, 1 loop')
  assert.equal(waitingOn(emptyStatus()), '')
})
