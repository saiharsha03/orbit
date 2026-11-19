import { describe, expect, it } from 'vitest'
import { acceptSeq, decodeLinkCode, encodeLinkCode, formatRemote, machineName, parseRemoteRef } from '../../src/shared/machineLinkCore'

const good = { relay: 'https://relay.example.com', pair: 'a'.repeat(32), token: 't'.repeat(43), key: Buffer.alloc(32, 1).toString('base64'), name: 'LAPTOP-2' }

describe('machineLinkCore', () => {
  it('round-trips a link code and refuses broken ones', () => {
    expect(decodeLinkCode(encodeLinkCode(good))).toEqual(good)
    expect(decodeLinkCode('hello')).toMatch(/not an Orbit link code/)
    expect(decodeLinkCode(encodeLinkCode({ ...good, relay: 'http://x' }))).toMatch(/https/)
    expect(decodeLinkCode(encodeLinkCode({ ...good, key: 'short' }))).toMatch(/damaged/)
    expect(decodeLinkCode(encodeLinkCode(good).slice(0, 30))).toMatch(/damaged/)
  })
  it('names and remote ids', () => {
    expect(machineName('My PC (home)')).toBe('My-PC-home')
    expect(parseRemoteRef('LAPTOP-2/new-123')).toEqual({ machine: 'LAPTOP-2', thread: 'new-123' })
    expect(parseRemoteRef('new-123')).toBeNull()
    expect(parseRemoteRef('/x')).toBeNull()
  })
  it('refuses replayed and stale messages', () => {
    expect(acceptSeq(4, 5, 1000, 1000)).toBe(true)
    expect(acceptSeq(5, 5, 1000, 1000)).toBe(false)
    expect(acceptSeq(4, 5, 0, 200_000)).toBe(false)
    expect(acceptSeq(4, '5', 1000, 1000)).toBe(false)
  })
  it('formats remote rows with routable ids', () => {
    expect(formatRemote('PC', { rows: [{ id: 'new-1', title: 'Fix', cwd: 'C:/x', status: 'idle' }] })).toContain('id=PC/new-1')
    expect(formatRemote('PC', { error: 'PC did not answer' })).toContain('did not answer')
  })
})
