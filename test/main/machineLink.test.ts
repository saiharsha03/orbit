// Two Orbits linked through a real relay on this computer: B pastes A's code, then lists, messages and waits for A's
// threads; A messages back. A copy with a made-up code gets nowhere.
import { spawn, type ChildProcess } from 'node:child_process'
import { mkdtempSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { randomBytes } from 'node:crypto'
import { afterAll, beforeAll, describe, expect, it, vi } from 'vitest'
import { encodeLinkCode } from '../../src/shared/machineLinkCore'

const PORT = 18000 + Math.floor(Math.random() * 1000)
const RELAY = `http://127.0.0.1:${PORT}`
const ADMIN = randomBytes(24).toString('hex')
let relay: ChildProcess
type Mod = typeof import('../../src/main/machineLink')

async function instance(env: Record<string, string>, onSend: (...a: unknown[]) => void): Promise<Mod> {
  vi.resetModules()
  process.env.HIVE_DATA_DIR = mkdtempSync(join(tmpdir(), 'orbit-link-'))
  const m: Mod = await import('../../src/main/machineLink')
  m.initMachines(env, {
    list: () => [{ id: 'new-1', title: 'Fix login', cwd: 'C:/repo', status: 'idle' }],
    send: async (thread, text, mode, from) => { onSend(thread, text, mode, from); return `Sent to thread "${thread}".` },
    wait: async (thread) => `Thread "${thread}" is idle.\nIts last reply:\ndone`,
    changed: () => {},
  })
  return m
}
const until = async (f: () => boolean, ms = 10_000) => {
  const end = Date.now() + ms
  while (!f()) { if (Date.now() > end) throw new Error('timeout'); await new Promise((r) => setTimeout(r, 50)) }
}

describe('linked machines through a real relay', () => {
  beforeAll(async () => {
    relay = spawn(process.execPath, ['relay/relay.mjs'], { env: { ...process.env, HIVE_RELAY_ADMIN_TOKEN: ADMIN, HIVE_RELAY_PORT: String(PORT), HIVE_RELAY_STATE: join(mkdtempSync(join(tmpdir(), 'relay-')), 's.json') }, stdio: 'ignore' })
    const end = Date.now() + 10_000
    while (Date.now() < end) { try { if ((await fetch(`${RELAY}/healthz`)).ok) return } catch { /* starting */ } await new Promise((r) => setTimeout(r, 100)) }
    throw new Error('relay did not start')
  }, 20_000)
  afterAll(() => { relay?.kill() })

  it('lists, messages and waits both ways; a made-up code is refused', async () => {
    const gotA: unknown[][] = []
    const gotB: unknown[][] = []
    const a = await instance({ HIVE_MOBILE_RELAY: RELAY, HIVE_MOBILE_ADMIN_TOKEN: ADMIN }, (...x) => gotA.push(x))
    const code = await a.createLinkCode()
    const b = await instance({}, (...x) => gotB.push(x))
    const name = b.joinLink(code)
    await until(() => b.machineStatus().links[0]?.connected && a.machineStatus().links[0]?.peerOnline === true)

    expect((await b.listRemote())[0]).toContain(`id=${name}/new-1`)
    expect(await b.messageRemote(name, 'new-1', 'hello', 'queue', { id: 'new-9', title: 'Plan' })).toContain('Sent to thread "new-1"')
    const fromB = gotA[0][3] as { id: string; title: string }
    expect(gotA[0].slice(0, 3)).toEqual(['new-1', 'hello', 'queue'])
    expect(fromB.title).toContain('Plan on ')
    expect(await b.waitRemote(name, 'new-1', 1000)).toContain('done')

    // A answers B's thread by the id its message came from
    const [machine, thread] = fromB.id.split('/')
    expect(await a.messageRemote(machine, thread, 'got it', 'queue', { id: 'new-1', title: 'Fix login' })).toContain('Sent')
    expect(gotB[0].slice(0, 2)).toEqual(['new-9', 'got it'])

    // someone else's copy with its own key and token for the same pair: the relay refuses it
    const c = await instance({}, () => {})
    const pair = JSON.parse(Buffer.from(code.slice('orbit-link:'.length), 'base64url').toString()).pair
    c.joinLink(encodeLinkCode({ relay: RELAY, pair, token: randomBytes(32).toString('base64url'), key: randomBytes(32).toString('base64'), name: 'intruder' }))
    await new Promise((r) => setTimeout(r, 1500))
    expect(c.machineStatus().links[0].connected).toBe(false)
    await expect(c.listRemote()).resolves.toEqual([expect.stringContaining('not connected')])

    // Remove on B ends it on A too
    await b.removeLink(name)
    await until(() => a.machineStatus().links.length === 0)
  }, 30_000)
})
