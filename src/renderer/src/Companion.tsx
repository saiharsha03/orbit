// The companion window (?view=companion): one bar that says what needs you, opening into the list. Answer a question,
// reply to a waiting thread, open a thread or a deploy in Orbit, without bringing Orbit forward first.
import { useEffect, useLayoutEffect, useRef, useState } from 'react'
import { companionSummary, needKey, type CompanionState } from '../../shared/companionCore'
import type { PhoneNeed } from '../../shared/phoneCore'
import './companion.css'

const EMPTY: CompanionState = { needs: [], working: [], coming: [] }
const WIDTH_OPEN = 380
const WIDTH_SHUT = 300
const MAX_H = 560
const ago = (t: number) => { const m = Math.round((Date.now() - t) / 60_000); return m < 1 ? 'now' : m < 60 ? `${m}m` : m < 1440 ? `${Math.round(m / 60)}h` : `${Math.round(m / 1440)}d` }
const at = (t: number) => new Date(t).toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' })

function Need({ n, refresh }: { n: PhoneNeed; refresh: () => void }) {
  const [text, setText] = useState('')
  const [busy, setBusy] = useState(false)
  const [err, setErr] = useState('')
  const run = async (f: () => Promise<unknown>) => { setBusy(true); setErr(''); try { const r = await f(); if (r === false) setErr('That question is no longer open') } catch (e) { setErr(String((e as Error).message ?? e)) } finally { setBusy(false); refresh() } }
  const fail = n.kind === 'failed'
  return (
    <div className="cp-need">
      <div className="cp-row cp-click" onClick={() => void window.api.companionOpen(n.thread)} title="Open in Orbit">
        <span className={`cp-dot ${fail ? 'fail' : 'wait'}`} />
        <span className="cp-title">{n.title}</span>
        <span className="cp-time">{ago(n.at)}</span>
      </div>
      {n.note && <div className="cp-note">{n.ask && n.note === n.ask.question ? null : n.note}</div>}
      {n.ask && (
        <div className="cp-ask">
          <div className="cp-q">{n.ask.question}</div>
          <div className="cp-opts">
            {n.ask.options.map((o) => <button key={o} className="btn btn-quiet cp-opt" disabled={busy} onClick={() => void run(() => window.api.companionAnswer(n.thread, n.ask!.id, n.ask!.question, o))}>{o}</button>)}
          </div>
        </div>
      )}
      {!fail && !n.ask && (
        <form className="cp-reply" onSubmit={(e) => { e.preventDefault(); const t = text.trim(); if (t) void run(async () => { await window.api.companionReply(n.thread, t); setText('') }) }}>
          <input value={text} onChange={(e) => setText(e.target.value)} placeholder="Reply…" disabled={busy} />
        </form>
      )}
      {fail && (
        <div className="cp-acts">
          <button className="btn btn-quiet cp-sm" onClick={() => void run(() => window.api.companionDismiss(n.thread))}>Dismiss</button>
          <button className="btn btn-strong cp-sm" onClick={() => void window.api.companionOpen(n.thread)}>Open</button>
        </div>
      )}
      {err && <div className="cp-err">{err}</div>}
    </div>
  )
}

export function Companion() {
  const [s, setS] = useState<CompanionState>(EMPTY)
  const [deploys, setDeploys] = useState<DeployWatcher[]>([])
  const [open, setOpen] = useState(false)
  const seen = useRef<Set<string> | null>(null)
  const root = useRef<HTMLDivElement>(null)

  const refresh = () => { void window.api.companionState().then(setS).catch(() => {}) }
  useEffect(() => {
    refresh()
    const t = setInterval(refresh, 3_000)
    void window.api.deployWatchers().then(setDeploys).catch(() => {})
    const off = window.api.onDeployWatch(setDeploys)
    return () => { clearInterval(t); off() }
  }, [])

  // something new needs you (a question, a failure, a deploy that ended, or one already there at start): the bar opens
  const keyList = [...s.needs.map(needKey), ...deploys.filter((d) => d.landed || d.stuck).map((d) => `deploy:${d.id}:${d.landed}`)].join('|')
  useEffect(() => {
    const keys = keyList ? keyList.split('|') : []
    if (!seen.current) seen.current = new Set()
    if (keys.some((k) => !seen.current!.has(k))) setOpen(true)
    for (const k of keys) seen.current.add(k)
  }, [keyList])

  // the window is as tall as what it shows
  useLayoutEffect(() => {
    const el = root.current
    if (!el) return
    const fit = () => void window.api.companionSize(open ? WIDTH_OPEN : WIDTH_SHUT, Math.min(MAX_H, Math.ceil(el.getBoundingClientRect().height)))
    fit()
    const ro = new ResizeObserver(fit)
    ro.observe(el)
    return () => ro.disconnect()
  }, [open])

  const running = deploys.filter((d) => !d.landed && !d.stuck)
  const failed = s.needs.some((n) => n.kind === 'failed') || deploys.some((d) => d.stuck)
  const waiting = s.needs.some((n) => n.kind === 'waiting')
  const dot = failed ? 'fail' : waiting ? 'wait' : s.working.length || running.length ? 'work' : 'idle'
  const summary = companionSummary(s, running.length)

  return (
    <div className={`cp ${open ? 'open' : ''}`} ref={root}>
      <div className="cp-bar">
        <button className="cp-head" onClick={() => setOpen(!open)} title={open ? 'Fold' : 'Open'}>
          {dot === 'work' ? <i className="codicon codicon-loading sm cp-spin" /> : <span className={`cp-dot ${dot}`} />}
          <span className="cp-sum">{summary}</span>
        </button>
        <span className="cp-drag" />
        <button className="cp-ib" onClick={() => void window.api.companionOpen('')} title="Open Orbit"><i className="codicon codicon-link-external sm" /></button>
        <button className="cp-ib" onClick={() => setOpen(!open)} title={open ? 'Fold' : 'Open'}><i className={`codicon codicon-chevron-${open ? 'down' : 'up'} sm`} /></button>
        <button className="cp-ib" onClick={() => void window.api.companionHide()} title="Hide the companion (Ctrl+Alt+O or the tray brings it back)"><i className="codicon codicon-close sm" /></button>
      </div>
      {open && (
        <div className="cp-body">
          {!s.needs.length && !deploys.length && !s.working.length && !s.coming.length && (
            <div className="cp-empty">Nothing needs you. Threads that wait for you or fail, and deploys being watched, show up here.</div>
          )}
          {!!s.needs.length && <div className="cp-sec">Needs you <span>{s.needs.length}</span></div>}
          {s.needs.map((n) => <Need key={needKey(n)} n={n} refresh={refresh} />)}
          {!!deploys.length && <div className="cp-sec">Deploys <span>{deploys.length}</span></div>}
          {deploys.map((d) => (
            <div key={d.id} className="cp-row" title={d.note}>
              {d.stuck ? <span className="cp-dot fail" /> : d.landed ? <i className="codicon codicon-check sm cp-ok" /> : <i className="codicon codicon-loading sm cp-spin" />}
              <span className="cp-title">{d.repo}{d.target ? ` → ${d.target}` : ''}</span>
              <span className="cp-time cp-dnote">{d.stuck ? 'not live' : d.landed ? 'landed' : d.note || 'checking'}</span>
              {(d.landed || d.stuck) && <button className="cp-ib" onClick={() => void window.api.deployDismiss(d.id).then(() => window.api.deployWatchers().then(setDeploys))} title="Dismiss"><i className="codicon codicon-close sm" /></button>}
            </div>
          ))}
          {!!s.working.length && <div className="cp-sec">Working <span>{s.working.length}</span></div>}
          {s.working.map((t) => (
            <div key={t.id} className="cp-row cp-click" onClick={() => void window.api.companionOpen(t.id)} title={t.note || `${t.title} (${t.folder})`}>
              <i className="codicon codicon-loading sm cp-spin" />
              <span className="cp-title">{t.title}</span>
              {t.note && <span className="cp-time cp-dnote">{t.note}</span>}
            </div>
          ))}
          {!!s.coming.length && <div className="cp-sec">Coming up</div>}
          {s.coming.map((w) => (
            <div key={`${w.kind}:${w.id}`} className="cp-row cp-click" onClick={() => void window.api.companionOpen(w.open)} title={w.prompt}>
              <i className="codicon codicon-clock sm cp-faint" />
              <span className="cp-title">{w.title}</span>
              <span className="cp-time">{at(w.at)}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
