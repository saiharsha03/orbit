import { describe, expect, it } from 'vitest'
import { codexRows, geminiRows } from '../../src/shared/providerUsageCore'
import { gaugeOf, windowMs } from '../../src/shared/limitsCore'

describe('other agents\' usage rows', () => {
  it('reads Codex windows by their length', () => {
    const rows = codexRows({ rateLimits: { primary: { usedPercent: 12, windowDurationMins: 300, resetsAt: 1795140201 }, secondary: { usedPercent: 40, windowDurationMins: 10080, resetsAt: 1795727001 } } })
    expect(rows?.map((r) => [r.key, r.pct])).toEqual([['codex:five_hour', 12], ['codex:seven_day', 40]])
    expect(codexRows({})).toBeNull()
  })
  it('reads the Gemini group from agy /usage as used, not remaining', () => {
    const rows = geminiRows({ command: { data: { groups: [{ name: 'Claude and GPT models', buckets: [{ window: '5h', remaining_fraction: 0 }] }, { name: 'Gemini Models', buckets: [
      { window: 'weekly', remaining_fraction: 0.75, reset_time: '2026-11-26T20:52:02Z' }, { window: '5h', remaining_fraction: 0.5, reset_time: '2026-11-20T01:52:02Z' }] }] } } })
    expect(rows?.map((r) => [r.key, Math.round(r.pct)])).toEqual([['gemini:five_hour', 50], ['gemini:seven_day', 25]])
    expect(geminiRows({ command: { data: { groups: [] } } })).toBeNull()
  })
  it('draws them like Claude\'s windows', () => {
    expect(windowMs('codex:five_hour')).toBe(5 * 3_600_000)
    expect(gaugeOf({ key: 'gemini:seven_day', label: 'Gemini weekly', pct: 10, resetsAt: null }, 0).short).toBe('Weekly')
  })
})
