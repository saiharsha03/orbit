// The usage limits of the other two agents Orbit can hand work to, for the side bar under Claude's: Codex (its app
// server's account/rateLimits/read) and Gemini through Antigravity (`agy -p /usage`, which answers without a model
// call). Each is read at most every 5 minutes; an agent that is not installed or not signed in has no rows.
import { spawn } from 'node:child_process'
import { codexExecutable } from './codex'
import { agyExecutable } from './gemini'
import { codexRows, geminiRows } from '../shared/providerUsageCore'
import { TEST_MODE } from './testMode'
import type { UsageRow } from '../shared/limitsCore'

export type ProviderUsage = { codex: UsageRow[] | null; gemini: UsageRow[] | null }

const EVERY_MS = 5 * 60_000
let cache: { at: number; value: ProviderUsage } | null = null
let reading: Promise<ProviderUsage> | null = null

export function providerUsage(): Promise<ProviderUsage> {
  if (TEST_MODE) return Promise.resolve({ codex: null, gemini: null }) // a test run never starts the real agents
  if (cache && Date.now() - cache.at < EVERY_MS) return Promise.resolve(cache.value)
  reading ??= Promise.all([readCodex().catch(() => null), readGemini().catch(() => null)])
    .then(([codex, gemini]) => { const value = { codex, gemini }; cache = { at: Date.now(), value }; return value })
    .finally(() => { reading = null })
  return reading
}

// run a program and collect its stdout; it is ended after ms
function collect(bin: string, args: string[], input: string, ms: number, done?: (out: string) => boolean): Promise<string> {
  return new Promise((resolve) => {
    const p = spawn(bin, args, { windowsHide: true, env: { ...process.env, MSYS_NO_PATHCONV: '1' } })
    let out = ''
    const end = () => { clearTimeout(t); p.kill(); resolve(out) }
    const t = setTimeout(end, ms)
    p.stdout.on('data', (c) => { out += c.toString(); if (done?.(out)) end() })
    p.on('error', end)
    p.on('close', () => { clearTimeout(t); resolve(out) })
    // a server that answers over stdin quits when it closes: keep it open until the answer is in (done), then kill it
    if (done) p.stdin.write(input)
    else p.stdin.end(input)
  })
}

async function readCodex(): Promise<UsageRow[] | null> {
  const bin = codexExecutable()
  if (!bin) return null
  const req = [
    { jsonrpc: '2.0', id: 1, method: 'initialize', params: { clientInfo: { name: 'orbit', version: '1' } } },
    { jsonrpc: '2.0', method: 'initialized', params: {} },
    { jsonrpc: '2.0', id: 2, method: 'account/rateLimits/read', params: {} },
  ].map((m) => JSON.stringify(m)).join('\n') + '\n'
  // the app server keeps running on an open stdin: stop it once the answer is in
  const out = await collect(bin, ['app-server'], req, 20_000, (o) => /"id":2,/.test(o))
  const line = out.split('\n').find((l) => l.includes('"id":2,'))
  return line ? codexRows(JSON.parse(line).result) : null
}

async function readGemini(): Promise<UsageRow[] | null> {
  const bin = agyExecutable()
  if (!bin) return null
  const out = await collect(bin, ['-p', '/usage', '--output-format', 'json'], '', 30_000)
  const start = out.indexOf('{')
  return start >= 0 ? geminiRows(JSON.parse(out.slice(start))) : null
}
