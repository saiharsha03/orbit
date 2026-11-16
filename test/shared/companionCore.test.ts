import { test } from 'vitest'
import assert from 'node:assert/strict'
import { companionBounds, companionState, companionSummary } from '../../src/shared/companionCore'
import type { PhoneThread } from '../../src/shared/threadsCore'

const t = (id: string, state: PhoneThread['state'], note = ''): PhoneThread => ({ id, title: id, folder: 'f', state, note, updated: 1, section: 'recent' as PhoneThread['section'] })

test('needs, working and the summary', () => {
  const ask = { id: 'a1', questions: [{ question: 'Ship?', header: 'Ship', multiSelect: false, options: [{ label: 'Yes', description: '' }, { label: 'No', description: '' }] }] }
  const s = companionState([t('w', 'waiting'), t('b', 'working', 'tests'), t('f', 'idle')], { w: [ask] }, [{ thread: 'f', title: 'f', note: 'boom', at: 5 }], [], 0)
  assert.deepEqual(s.needs.map((n) => [n.kind, n.thread, n.ask?.options]), [['waiting', 'w', ['Yes', 'No']], ['failed', 'f', undefined]])
  assert.deepEqual(s.working.map((w) => w.id), ['b'])
  assert.equal(companionSummary(s, 1), '1 needs you · 1 failed · 1 working · 1 deploy')
  assert.equal(companionSummary({ needs: [], working: [], coming: [] }, 0), 'All quiet')
})

test('keeps its right edge and bottom, inside the work area', () => {
  const area = { x: 0, y: 0, width: 1000, height: 800 }
  assert.deepEqual(companionBounds(300, 44, null, area), { x: 684, y: 740, width: 300, height: 44 })
  assert.deepEqual(companionBounds(380, 400, { right: 900, bottom: 500 }, area), { x: 520, y: 100, width: 380, height: 400 })
  assert.deepEqual(companionBounds(300, 300, { right: 100, bottom: 100 }, area), { x: 0, y: 0, width: 300, height: 300 })
})
