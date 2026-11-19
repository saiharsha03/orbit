// Linked machines: two Orbits on two computers whose threads can talk (list_threads, message_thread, wait_for_thread).
// One computer makes a link code (relay address, pair id, the other side's token, the sealing key, its own name); the
// other pastes it once. Without the code a copy of Orbit cannot reach you: the relay refuses its token and it has no key.
// Pure: the code, remote thread ids ("<machine>/<thread id>") and the replay check.

export type LinkCode = { relay: string; pair: string; token: string; key: string; name: string }

const PREFIX = 'orbit-link:'

// https, or plain http to a relay on this computer (a local test; it never leaves the machine)
export const relayOk = (u: string): boolean => /^https:\/\//.test(u) || /^http:\/\/(127\.0\.0\.1|localhost)(:\d+)?$/.test(u)

export const machineName = (s: string): string => s.replace(/[^A-Za-z0-9._-]+/g, '-').replace(/^-+|-+$/g, '').slice(0, 40) || 'machine'

export const encodeLinkCode = (c: LinkCode): string => PREFIX + Buffer.from(JSON.stringify({ v: 1, ...c })).toString('base64url')

// the code, or an error saying what is wrong with it
export function decodeLinkCode(text: string): LinkCode | string {
  const s = text.trim()
  if (!s.startsWith(PREFIX)) return 'That is not an Orbit link code (it starts with "orbit-link:").'
  let j: Record<string, unknown>
  try { j = JSON.parse(Buffer.from(s.slice(PREFIX.length), 'base64url').toString('utf8')) } catch { return 'The link code is cut off or damaged; copy it again.' }
  const str = (k: string) => (typeof j[k] === 'string' ? (j[k] as string) : '')
  const c = { relay: str('relay').replace(/\/+$/, ''), pair: str('pair'), token: str('token'), key: str('key'), name: machineName(str('name')) }
  if (j.v !== 1) return 'This link code is from a newer Orbit; update this one.'
  if (!relayOk(c.relay)) return 'The link code needs an https:// relay address.'
  if (!/^[a-f0-9]{32}$/.test(c.pair) || c.token.length < 32 || Buffer.from(c.key, 'base64').length !== 32) return 'The link code is cut off or damaged; copy it again.'
  return c
}

// "<machine>/<thread id>": a thread on a linked machine (local ids never hold a slash)
export function parseRemoteRef(ref: string): { machine: string; thread: string } | null {
  const i = ref.indexOf('/')
  if (i <= 0 || i === ref.length - 1) return null
  return { machine: ref.slice(0, i), thread: ref.slice(i + 1) }
}

// A message is taken only if its counter is above the last one taken and it was sealed within 2 minutes of this clock,
// so the relay can neither replay one nor hold one back and deliver it later.
export const FRESH_MS = 120_000
export const acceptSeq = (seqIn: number, seq: unknown, at: unknown, now: number): boolean =>
  typeof seq === 'number' && Number.isInteger(seq) && seq > seqIn && typeof at === 'number' && Math.abs(now - at) <= FRESH_MS

// rows another machine sent, shown under its name with ids that route back to it
export type RemoteRow = { id: string; title: string; cwd: string; status: string; reason?: string; background?: boolean }
export function formatRemote(machine: string, r: { rows: RemoteRow[] } | { error: string }): string {
  if ('error' in r) return `On ${machine} (linked machine): ${r.error}`
  if (!r.rows.length) return `On ${machine} (linked machine): no live threads.`
  return [`On ${machine} (linked machine):`, ...r.rows.map((x) => [
    `- "${x.title}" id=${machine}/${x.id}`,
    `  status: ${x.status}${x.reason ? ` (${x.reason})` : ''}; cwd: ${x.cwd}`,
  ].join('\n'))].join('\n')
}
