// Settings > General > Linked machines: the links to Orbit on other computers, Make a link code (shown once, to paste
// into the other Orbit), Paste a link code, and Remove. The code holds the link's key: it is shown only until closed.
import { useEffect, useState } from 'react'

export function MachineLinks() {
  const [st, setSt] = useState<MachinesStatus | null>(null)
  const [code, setCode] = useState<string | null>(null)
  const [paste, setPaste] = useState('')
  const [msg, setMsg] = useState('')
  const [busy, setBusy] = useState(false)
  useEffect(() => { void window.api.machinesStatus().then(setSt); return window.api.onMachinesStatus(setSt) }, [])

  const run = async (f: () => Promise<unknown>) => {
    setBusy(true); setMsg('')
    try { await f() } catch (e) { setMsg(String((e as Error)?.message ?? e).replace(/^Error invoking remote method '[^']+': (Error: )?/, '')) }
    setBusy(false)
  }
  const make = () => run(async () => setCode(await window.api.machinesCreate()))
  const join = () => run(async () => { const name = await window.api.machinesJoin(paste); setPaste(''); setMsg(`Linked to ${name}.`) })
  const remove = (name: string) => run(() => window.api.machinesRemove(name))
  const copy = () => { if (code) void navigator.clipboard.writeText(code).then(() => setMsg('Copied. Paste it into Orbit on the other computer.')) }

  const state = (l: MachinesStatus['links'][number]) => !l.connected ? `not connected to the relay${l.error ? ` (${l.error})` : ''}`
    : l.peerOnline === false ? 'its Orbit is offline' : l.peerOnline ? 'connected' : 'waiting to hear from it'
  return (
    <div className="phonepair" data-testid="machine-links">
      <div className="dnote">This computer is <b>{st?.me ?? '...'}</b> to the machines it is linked with.</div>
      {st?.links.map((l) => (
        <div className="drow" key={l.name} data-testid="machine-link">
          <span><b>{l.name}</b>: {state(l)}</span>
          <button className="dlink" disabled={busy} onClick={() => void remove(l.name)}>Remove</button>
        </div>
      ))}
      <div className="drow">
        <button className="dbtn primary" disabled={busy || !st?.canCreate} onClick={() => void make()} data-testid="machine-make">Make a link code</button>
      </div>
      {code && (
        <div className="phoneqr">
          <textarea className="sin wide" readOnly value={code} rows={3} style={{ fontFamily: 'monospace' }} data-testid="machine-code" />
          <div className="dnote">Paste this into Orbit on the other computer (Settings &gt; General &gt; Linked machines). It holds the key to the link: send it only to yourself, close it once linked, and remove the link if anyone else saw it.</div>
          <div className="drow">
            <button className="dlink" onClick={copy}>Copy</button>
            <button className="dlink" onClick={() => setCode(null)} data-testid="machine-code-close">Close the code</button>
          </div>
        </div>
      )}
      <div className="drow">
        <input className="sin wide" spellCheck={false} value={paste} onChange={(e) => setPaste(e.target.value)} placeholder="orbit-link:..." style={{ flex: 1 }} data-testid="machine-paste" />
        <button className="dbtn" disabled={busy || !paste.trim()} onClick={() => void join()} data-testid="machine-join">Link</button>
      </div>
      {msg && <div className="dnote" data-testid="machine-msg">{msg}</div>}
    </div>
  )
}
