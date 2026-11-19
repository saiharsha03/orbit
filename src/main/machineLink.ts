// Linked machines: this Orbit and an Orbit on another computer, so threads on either can list, message and wait for
// threads on the other. Same relay and sealing as the phone (mobileLink.ts): the computer that makes the link registers
// a new pair with the relay (it needs the relay's admin token) and takes the desk role; the one that pastes the code
// takes the phone role. Every message is sealed with the link's own key and carries a counter and a time (replays and
// held-back messages are dropped). Links live in ~/.orbit/hive-machines.json; a key never leaves this computer except
// inside the link code shown once.
import { createHash, randomBytes, randomUUID } from 'node:crypto'
import { readFileSync } from 'node:fs'
import { hostname } from 'node:os'
import { join } from 'node:path'
import { open, seal, type Dir } from './mobileLink'
import { writeFileSafe } from './safeWrite'
import { blocked, dataDir, TEST_MODE } from './testMode'
import { acceptSeq, relayOk, decodeLinkCode, encodeLinkCode, formatRemote, machineName, type RemoteRow } from '../shared/machineLinkCore'

type Link = { pair: string; relay: string; token: string; key: string; host: boolean; name: string; seqOut: number; seqIn: number; at: number }
export type MachineStatus = { name: string; host: boolean; connected: boolean; peerOnline: boolean | null; error: string; at: number }
// what this computer does when a linked machine asks (chat.ts): its live threads, a message into one, a wait for one
export type MachineDeps = {
  list: () => RemoteRow[]
  send: (thread: string, text: string, mode: 'queue' | 'steer', from: { id: string; title: string }) => Promise<string>
  wait: (thread: string, timeoutMs: number) => Promise<string>
  changed: () => void
}

type Conn = { ws: WebSocket | null; connected: boolean; peerOnline: boolean | null; error: string; retry?: ReturnType<typeof setTimeout> }
type Pending = { resolve: (v: unknown) => void; reject: (e: Error) => void; timer: ReturnType<typeof setTimeout> }
type HeaderSocket = new (url: string, init: { headers: Record<string, string> }) => WebSocket

// fixed at init, so a test can run two of these side by side with their own folders
let path = ''
const file = () => path
let relay = ''
let admin = ''
let deps: MachineDeps | null = null
let links: Link[] = []
const conns = new Map<string, Conn>()
const pending = new Map<string, Pending>()
// the link's secret goes in the Authorization header; a runtime whose WebSocket drops headers falls back to the address
let headerAuth = true
const sha = (s: string) => createHash('sha256').update(s).digest('hex')
const myName = () => machineName(hostname())
const store = () => writeFileSafe(file(), JSON.stringify(links))
const load = (): Link[] => { try { const j = JSON.parse(readFileSync(file(), 'utf8')); return Array.isArray(j) ? j.filter((l) => l?.pair && l?.key) : [] } catch { return [] } }
const outDir = (l: Link): Dir => (l.host ? 'd2p' : 'p2d')
const inDir = (l: Link): Dir => (l.host ? 'p2d' : 'd2p')

export function initMachines(env: Record<string, string>, d: MachineDeps): void {
  relay = (env.HIVE_MOBILE_RELAY ?? '').trim().replace(/\/+$/, '')
  admin = (env.HIVE_MOBILE_ADMIN_TOKEN ?? '').trim()
  deps = d
  path = join(dataDir(), 'hive-machines.json')
  links = load()
  if (TEST_MODE) return
  for (const pair of [...conns.keys()]) if (!links.some((l) => l.pair === pair)) drop(pair)
  for (const l of links) connect(l)
}

export const machineStatus = (): { canCreate: boolean; me: string; links: MachineStatus[] } => ({
  canCreate: !!relay && !!admin,
  me: myName(),
  links: links.map((l) => { const c = conns.get(l.pair); return { name: l.name, host: l.host, connected: !!c?.connected, peerOnline: c?.peerOnline ?? null, error: c?.error ?? '', at: l.at } }),
})

// ---- making and joining a link ----
export async function createLinkCode(): Promise<string> {
  if (TEST_MODE) throw new Error(blocked('linking machines'))
  if (!relay || !admin) throw new Error('Set the relay address and its admin token first (Settings > General > Phone app).')
  if (!relayOk(relay)) throw new Error('Linking machines needs an https:// relay address.')
  const pair = randomBytes(16).toString('hex')
  const deskToken = randomBytes(32).toString('base64url')
  const otherToken = randomBytes(32).toString('base64url')
  const key = randomBytes(32).toString('base64')
  const r = await fetch(`${relay}/v1/pair`, {
    method: 'POST', headers: { authorization: `Bearer ${admin}`, 'content-type': 'application/json' },
    body: JSON.stringify({ pair, desk: sha(deskToken), phone: sha(otherToken) }), signal: AbortSignal.timeout(15_000),
  })
  if (!r.ok) throw new Error(`The relay refused the link (${r.status}). Check the relay address and admin token.`)
  const l: Link = { pair, relay, token: deskToken, key, host: true, name: 'waiting-for-other-machine', seqOut: 0, seqIn: 0, at: Date.now() }
  links.push(l)
  store()
  connect(l)
  deps?.changed()
  return encodeLinkCode({ relay, pair, token: otherToken, key, name: myName() })
}

export function joinLink(code: string): string {
  if (TEST_MODE) throw new Error(blocked('linking machines'))
  const c = decodeLinkCode(code)
  if (typeof c === 'string') throw new Error(c)
  if (links.some((l) => l.pair === c.pair)) throw new Error('This computer already has that link.')
  const l: Link = { pair: c.pair, relay: c.relay, token: c.token, key: c.key, host: false, name: uniqueName(c.name), seqOut: 0, seqIn: 0, at: Date.now() }
  links.push(l)
  store()
  connect(l)
  deps?.changed()
  return l.name
}

export async function removeLink(name: string): Promise<void> {
  const l = links.find((x) => x.name === name)
  if (!l) return
  sendTo(l, { op: 'bye' })
  await forget(l)
}

async function forget(l: Link): Promise<void> {
  links = links.filter((x) => x !== l)
  store()
  drop(l.pair)
  // the side that made the pair takes it off the relay, so the token and key stop working there too
  if (l.host && admin) await fetch(`${l.relay}/v1/pair/${l.pair}`, { method: 'DELETE', headers: { authorization: `Bearer ${admin}` }, signal: AbortSignal.timeout(15_000) }).catch(() => {})
  deps?.changed()
}

const uniqueName = (n: string, self?: Link) => {
  let name = n
  for (let i = 2; links.some((l) => l !== self && l.name === name); i++) name = `${n}-${i}`
  return name
}

// ---- the connections ----
function drop(pair: string): void {
  const c = conns.get(pair)
  if (!c) return
  clearTimeout(c.retry)
  const ws = c.ws
  c.ws = null
  ws?.close()
  conns.delete(pair)
}

function connect(l: Link): void {
  drop(l.pair)
  const c: Conn = { ws: null, connected: false, peerOnline: null, error: '' }
  conns.set(l.pair, c)
  try {
    const url = `${l.relay.replace(/^http/, 'ws')}/v1/ws?pair=${l.pair}&role=${l.host ? 'desk' : 'phone'}`
    const byHeader = headerAuth
    const sock = byHeader
      ? new (WebSocket as unknown as HeaderSocket)(url, { headers: { authorization: `Bearer ${l.token}` } })
      : new WebSocket(`${url}&token=${encodeURIComponent(l.token)}`)
    c.ws = sock
    let opened = false
    sock.onopen = () => { opened = true; c.connected = true; c.error = ''; sendTo(l, { op: 'hello', name: myName() }); deps?.changed() }
    sock.onmessage = (e) => { void onMessage(l, c, String(e.data)) }
    sock.onclose = (e) => {
      if (c.ws !== sock) return
      c.connected = false
      // refused before it opened although the relay answers: this runtime's WebSocket may have dropped the header
      if (byHeader && !opened) void fetch(`${l.relay}/healthz`, { signal: AbortSignal.timeout(10_000) }).then((r) => { if (r.ok) headerAuth = false }).catch(() => {})
      c.error = e.code === 4401 ? 'The relay says this link was removed' : ''
      deps?.changed()
      if (e.code !== 4401 && links.includes(l)) c.retry = setTimeout(() => connect(l), 15_000)
    }
    sock.onerror = () => { c.error = 'Cannot reach the relay' }
  } catch (e) {
    c.error = String((e as Error).message ?? e)
    c.retry = setTimeout(() => connect(l), 30_000)
  }
}

function sendTo(l: Link, msg: Record<string, unknown>): boolean {
  const ws = conns.get(l.pair)?.ws
  if (!ws || ws.readyState !== WebSocket.OPEN) return false
  l.seqOut += 1
  const blob = seal(Buffer.from(l.key, 'base64'), l.pair, outDir(l), { ...msg, seq: l.seqOut, at: Date.now() })
  store()
  ws.send(JSON.stringify({ blob }))
  return true
}

async function onMessage(l: Link, c: Conn, raw: string): Promise<void> {
  let env: Record<string, unknown>
  try { env = JSON.parse(raw) } catch { return }
  if (env.desk === 'online' || env.desk === 'offline') { c.peerOnline = env.desk === 'online'; deps?.changed(); return } // the relay, about the other side
  if (typeof env.blob !== 'string') return
  const m = open(Buffer.from(l.key, 'base64'), l.pair, inDir(l), env.blob)
  if (!m || !acceptSeq(l.seqIn, m.seq, m.at, Date.now())) return
  l.seqIn = m.seq as number
  c.peerOnline = true
  store()
  const op = m.op
  if (op === 'hello') {
    const n = uniqueName(machineName(String(m.name ?? '')), l)
    if (n !== l.name) { l.name = n; store() }
    deps?.changed()
    return
  }
  if (op === 'bye') { await forget(l); return }
  if (op === 'res') {
    const p = pending.get(String(m.rid))
    if (!p) return
    pending.delete(String(m.rid))
    clearTimeout(p.timer)
    if (m.ok) p.resolve(m.body)
    else p.reject(new Error(String(m.error ?? 'failed')))
    return
  }
  if (op !== 'req' || !deps) return
  const rid = String(m.rid ?? '')
  const a = (m.args ?? {}) as Record<string, unknown>
  try {
    let body: unknown
    if (m.call === 'list') body = deps.list()
    else if (m.call === 'send') {
      const from = (a.from ?? {}) as Record<string, unknown>
      body = await deps.send(String(a.thread ?? ''), String(a.text ?? '').slice(0, 100_000), a.mode === 'steer' ? 'steer' : 'queue',
        { id: `${l.name}/${String(from.id ?? '?')}`, title: `${String(from.title ?? 'thread')} on ${l.name}` })
    } else if (m.call === 'wait') body = await deps.wait(String(a.thread ?? ''), Math.min(Math.max(Number(a.timeoutMs) || 0, 0), 600_000))
    else throw new Error(`unknown call ${String(m.call)}`)
    sendTo(l, { op: 'res', rid, ok: true, body })
  } catch (e) {
    sendTo(l, { op: 'res', rid, ok: false, error: String((e as Error)?.message ?? e) })
  }
}

function call(l: Link, name: string, args: Record<string, unknown>, timeoutMs: number): Promise<unknown> {
  return new Promise((resolve, reject) => {
    const rid = randomUUID()
    const timer = setTimeout(() => { pending.delete(rid); reject(new Error(`${l.name} did not answer (is Orbit running there?)`)) }, timeoutMs)
    pending.set(rid, { resolve, reject, timer })
    if (!sendTo(l, { op: 'req', rid, call: name, args })) { clearTimeout(timer); pending.delete(rid); reject(new Error(`not connected to the relay for ${l.name}`)) }
  })
}

// ---- what chat.ts calls ----
export const isLinkedMachine = (name: string): boolean => links.some((l) => l.name === name)

export async function listRemote(): Promise<string[]> {
  return Promise.all(links.map(async (l) => {
    try { return formatRemote(l.name, { rows: (await call(l, 'list', {}, 10_000)) as RemoteRow[] }) } catch (e) { return formatRemote(l.name, { error: (e as Error).message }) }
  }))
}

export async function messageRemote(machine: string, thread: string, text: string, mode: 'queue' | 'steer', from: { id: string; title: string }): Promise<string> {
  const l = links.find((x) => x.name === machine)
  if (!l) throw new Error(`No linked machine "${machine}".`)
  return `${machine}: ${String(await call(l, 'send', { thread, text, mode, from }, 30_000))}`
}

export async function waitRemote(machine: string, thread: string, timeoutMs: number): Promise<string> {
  const l = links.find((x) => x.name === machine)
  if (!l) throw new Error(`No linked machine "${machine}".`)
  return `${machine}: ${String(await call(l, 'wait', { thread, timeoutMs }, timeoutMs + 30_000))}`
}
