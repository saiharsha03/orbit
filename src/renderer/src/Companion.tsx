// The companion window (?view=companion): folded it is only Orbie with a number (what needs you, else what is working);
// a click on Orbie opens the list and nothing opens it by itself. Answer a question,
// reply to a waiting thread, open a thread or a deploy in Orbit, without bringing Orbit forward first.
import { useEffect, useLayoutEffect, useRef, useState } from 'react'
import { companionSummary, needKey, type CompanionView } from '../../shared/companionCore'
import type { PhoneNeed } from '../../shared/phoneCore'
import { Orbie, weatherOf, type Mood, type Rocket } from './Orbie'
import type { FaceShape } from '../../shared/orbieFaceCore'
import './companion.css'

const EMPTY: CompanionView = { needs: [], working: [], coming: [], running: [], prs: [], usage: [], goals: [], extra: {} }
const WIDTH_OPEN = 380
const WIDTH_SHUT = 76 // Orbie and its number
const WIDTH_SAY = 300 // folded, while Orbie says something
const MAX_H = 560
const ago = (t: number) => { const m = Math.round((Date.now() - t) / 60_000); return m < 1 ? 'now' : m < 60 ? `${m}m` : m < 1440 ? `${Math.round(m / 60)}h` : `${Math.round(m / 1440)}d` }
const at = (t: number) => new Date(t).toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' })

function Need({ n, refresh }: { n: PhoneNeed; refresh: () => void }) {
  const [text, setText] = useState('')
  const [busy, setBusy] = useState(false)
  const [err, setErr] = useState('')
  const run = async (f: () => Promise<unknown>) => { setBusy(true); setErr(''); try { const r = await f(); if (r === false) setErr('That question is no longer open') } catch (e) { setErr(String((e as Error).message ?? e)) } finally { setBusy(false); refresh() } }
  const fail = n.kind === 'failed'
  // a permission question (Allow / Allow for this thread / Deny): Deny first, the filled Allow last
  const perm = !!n.ask && n.ask.options.includes('Allow') && n.ask.options.includes('Deny')
  const opts = n.ask ? (perm ? [...n.ask.options].sort((a, b) => Number(a === 'Allow') - Number(b === 'Allow') || Number(b === 'Deny') - Number(a === 'Deny')) : n.ask.options) : []
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
          <div className={perm ? 'cp-opts cp-perm' : 'cp-opts'}>
            {opts.map((o) => <button key={o} className={`btn btn-quiet cp-opt${perm && o === 'Allow' ? ' cp-allow' : ''}`} disabled={busy} onClick={() => void run(() => window.api.companionAnswer(n.thread, n.ask!.id, n.ask!.question, o))}>{o}</button>)}
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
  const [s, setS] = useState<CompanionView>(EMPTY)
  const [deploys, setDeploys] = useState<DeployWatcher[]>([])
  const [open, setOpen] = useState(false)
  const root = useRef<HTMLDivElement>(null)

  // Orbie's speech bubble: a line from main (companionVoice.ts) while you are away from Orbit, shown for a while
  // with it, the face Haiku drew for the line (Orbie wears it while the bubble shows); a greeting stays up longer
  const [said, setSaid] = useState<{ line: string; kind: string; at: number; face: FaceShape[] } | null>(null)
  const [voice, setVoice] = useState(true)
  useEffect(() => {
    void window.api.companionVoice().then(setVoice)
    return window.api.onCompanionSay((line, kind, face) => setSaid({ line, kind, at: Date.now(), face: Array.isArray(face) ? face : [] }))
  }, [])
  useEffect(() => { if (!said) return; const t = setTimeout(() => setSaid(null), said.kind === 'greet' ? 40_000 : 15_000); return () => clearTimeout(t) }, [said])

  // a click anywhere else (the window loses focus) folds it back to Orbie
  useEffect(() => {
    const fold = () => setOpen(false)
    window.addEventListener('blur', fold)
    return () => window.removeEventListener('blur', fold)
  }, [])

  const refresh = () => { void window.api.companionState().then(setS).catch(() => {}) }
  useEffect(() => {
    refresh()
    const t = setInterval(refresh, 3_000)
    void window.api.deployWatchers().then(setDeploys).catch(() => {})
    const off = window.api.onDeployWatch(setDeploys)
    return () => { clearInterval(t); off() }
  }, [])

  // the window is as tall as what it shows
  useLayoutEffect(() => {
    const el = root.current
    if (!el) return
    const fit = () => void window.api.companionSize(open ? WIDTH_OPEN : said ? WIDTH_SAY : WIDTH_SHUT, Math.min(MAX_H, Math.ceil(el.getBoundingClientRect().height)))
    fit()
    const ro = new ResizeObserver(fit)
    ro.observe(el)
    return () => ro.disconnect()
  }, [open, said])

  const running = deploys.filter((d) => !d.landed && !d.stuck)
  const failed = s.needs.some((n) => n.kind === 'failed') || deploys.some((d) => d.stuck)
  const waiting = s.needs.some((n) => n.kind === 'waiting')
  const busy = s.working.length + s.running.length + running.length
  // happy for a few seconds when work ends without a failure: a thread finished, a deploy landed (or you poke it)
  const [happy, setHappy] = useState(0)
  const was = useRef({ busy: 0, landed: 0 })
  const landed = deploys.filter((d) => d.landed).length
  // a deploy that landed since the last look: Orbie jumps and a rocket goes up the side of the screen
  const launched = useRef<Set<string> | null>(null)
  useEffect(() => {
    const done = deploys.filter((d) => d.landed)
    if (!launched.current) { launched.current = new Set(done.map((d) => d.id)); return } // landed before the window opened
    for (const d of done) if (!launched.current.has(d.id)) { launched.current.add(d.id); void window.api.companionLaunch(`${d.repo}${d.target ? ` → ${d.target}` : ''} landed`) }
  }, [deploys])
  useEffect(() => {
    if ((busy < was.current.busy && !failed) || landed > was.current.landed) setHappy(Date.now())
    was.current = { busy, landed }
  }, [busy, landed, failed])
  useEffect(() => { if (!happy) return; const t = setTimeout(() => setHappy(0), 2_400); return () => clearTimeout(t) }, [happy])
  // a goal met since the last look: a party (confetti and a jump)
  const [party, setParty] = useState(0)
  const metSeen = useRef<Set<string> | null>(null)
  const metKeys = s.goals.filter((g) => g.status === 'met').map((g) => `${g.thread}:${g.metAt ?? 0}`).join('|')
  useEffect(() => {
    const keys = metKeys ? metKeys.split('|') : []
    if (!metSeen.current) { metSeen.current = new Set(keys); return }
    if (keys.some((k) => !metSeen.current!.has(k))) { setParty(Date.now()); setHappy(Date.now()) }
    for (const k of keys) metSeen.current.add(k)
  }, [metKeys])
  useEffect(() => { if (!party) return; const t = setTimeout(() => setParty(0), 4_000); return () => clearTimeout(t) }, [party])
  // a thread whose API calls are being retried (overloaded, a network error): Orbie sweats
  const retrying = Object.values(s.extra).some((x) => x.retry)
  const goalsActive = s.goals.filter((g) => g.status === 'active').length
  const hour = new Date().getHours()
  const mood: Mood = failed ? 'fail' : waiting ? 'wait' : party || happy ? 'happy' : retrying ? 'retry' : busy || goalsActive ? 'work' : hour >= 23 || hour < 7 ? 'sleepy' : 'idle'
  const weather = weatherOf(Math.max(0, ...s.usage.map((u) => u.pct)))
  const rocket: Rocket = running.length ? 'pad' : happy && landed ? 'launch' : 'none'
  const summary = companionSummary({ ...s, goalsActive }, running.length)
  const limits = s.usage.slice(0, 3)
  const session = limits.find((u) => u.key === 'five_hour')
  const nextUp = s.coming[0]
  // the folded number: what needs you (red when one failed), else the threads working (quiet)
  const count = s.needs.length || s.working.length
  const face = <Orbie mood={mood} moons={s.working.length} weather={weather} rocket={rocket} stars={s.coming.length} drawn={said?.face} party={!!party} goal={goalsActive > 0} />

  return (
    <div className={`cp ${open ? 'open' : ''}`} ref={root}>
      {said && (
        <div className={`cp-say k-${said.kind}`} onClick={() => { setSaid(null); void window.api.companionOpen('') }} title="Open Orbit">
          <span>{said.line}</span>
        </div>
      )}
      {!open && (
        <div className="cp-mini">
          <button className="cp-mini-b" onClick={() => setOpen(true)} title={`${summary}. Click to open`} data-testid="companion-orbie">
            {face}
            {count > 0 && <span className={`cp-count ${s.needs.length ? (failed ? 'fail' : 'wait') : 'busy'}`} data-testid="companion-count">{count}</span>}
            {s.needs.length > 0 && s.working.length > 0 && <span className="cp-count cp-count2 busy" data-testid="companion-working">{s.working.length}</span>}
          </button>
        </div>
      )}
      {open && <div className="cp-bar">
        <button className="cp-head" onClick={() => setOpen(false)} title="Fold">
          <span className="cp-orbie">{face}</span>
          <span className="cp-sum">{summary}</span>
        </button>
        <span className="cp-drag" />
        <button className="cp-ib" onClick={() => void window.api.companionVoice(!voice).then(setVoice)} title={voice ? 'Orbie tells you what happened while you are away from Orbit (Haiku writes the line). Click to mute' : 'Orbie is muted. Click to let it talk'}><i className={`codicon codicon-${voice ? 'comment' : 'comment-unresolved'} sm ${voice ? '' : 'cp-faint'}`} /></button>
        <button className="cp-ib" onClick={() => void window.api.companionOpen('')} title="Open Orbit"><i className="codicon codicon-link-external sm" /></button>
        <button className="cp-ib" onClick={() => setOpen(!open)} title={open ? 'Fold' : 'Open'}><i className={`codicon codicon-chevron-${open ? 'down' : 'up'} sm`} /></button>
        <button className="cp-ib" onClick={() => void window.api.companionHide()} title="Hide the companion (Ctrl+Alt+O or the tray brings it back)"><i className="codicon codicon-close sm" /></button>
      </div>}
      {open && (
        <div className="cp-body">
          {!s.needs.length && !deploys.length && !s.working.length && !s.running.length && !s.prs.length && !s.coming.length && (
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
          {!!s.goals.length && <div className="cp-sec">Goals <span>{s.goals.length}</span></div>}
          {s.goals.map((g) => (
            <div key={g.thread} className="cp-row cp-click" onClick={() => void window.api.companionOpen(g.thread)} title={`${g.title}: ${g.condition}`}>
              {g.status === 'met' ? <i className="codicon codicon-pass-filled sm cp-good" /> : <i className="codicon codicon-target sm cp-goal" />}
              <span className="cp-title">{g.condition}</span>
              <span className={`cp-time ${g.status === 'met' ? 'cp-good' : ''}`}>{g.status === 'met' ? `completed ${at(g.metAt ?? Date.now())}` : ago(g.setAt)}</span>
            </div>
          ))}
          {!!s.running.length && <div className="cp-sec">Background tasks <span>{s.running.length}</span></div>}
          {s.running.map((r) => (
            <div key={`${r.thread}:${r.id}`} className="cp-row cp-click" onClick={() => void window.api.companionOpen(r.thread)} title={[r.threadTitle, r.title, r.now].filter(Boolean).join(' · ')}>
              <i className="codicon codicon-loading sm cp-spin" />
              <span className="cp-title">{r.title}</span>
              <span className="cp-time cp-dnote">{r.kind}</span>
              <button className="cp-ib" onClick={(e) => { e.stopPropagation(); void window.api.companionStopTask(r.thread, r.id).then(refresh) }} title="Stop"><i className="codicon codicon-debug-stop sm" /></button>
            </div>
          ))}
          {!!s.prs.length && <div className="cp-sec">Pull requests <span>{s.prs.length}</span></div>}
          {s.prs.map((p) => (
            <div key={p.url} className="cp-row cp-click" onClick={() => void window.api.companionOpen(p.thread)} title={`${p.threadTitle} · ${p.url}`}>
              {p.state === 'failing' ? <span className="cp-dot fail" /> : p.state === 'pending' ? <i className="codicon codicon-loading sm cp-spin" /> : p.state === 'passing' ? <i className="codicon codicon-check sm cp-ok" /> : <i className="codicon codicon-git-pull-request sm cp-faint" />}
              <span className="cp-title">{p.repo.split('/').pop()} #{p.num}</span>
              <span className={`cp-time ${p.state === 'failing' ? 'cp-bad' : ''}`}>{p.state === 'none' ? '' : p.state}</span>
            </div>
          ))}
          {!!s.working.length && <div className="cp-sec">Working <span>{s.working.length}</span></div>}
          {s.working.map((t) => (
            <div key={t.id} className="cp-row cp-click" onClick={() => void window.api.companionOpen(t.id)} title={t.note || `${t.title} (${t.folder})`}>
              <i className="codicon codicon-loading sm cp-spin" />
              <span className="cp-title">{t.title}</span>
              {s.extra[t.id]?.retry ? <span className="cp-time cp-warn" title={s.extra[t.id]!.retry!.error}>retrying {s.extra[t.id]!.retry!.attempt}/{s.extra[t.id]!.retry!.max}</span>
                : s.extra[t.id]?.todos ? <span className="cp-time" title={s.extra[t.id]!.todos!.now}>{s.extra[t.id]!.todos!.done}/{s.extra[t.id]!.todos!.total}</span>
                : t.note && <span className="cp-time cp-dnote">{t.note}</span>}
            </div>
          ))}
          {limits.some((u) => u !== session) && <div className="cp-sec">Plan limits</div>}
          {limits.map((u, i) => u !== session && (
            <div key={u.key} className="cp-row cp-lim" title={u.resetsAt ? `Resets ${new Date(u.resetsAt).toLocaleString()}` : ''}>
              <span className="cp-title">{u.label}</span>
              <span className="cp-meter"><span style={{ width: `${Math.min(100, Math.max(0, u.pct))}%` }} className={['five', 'seven', 'model'][i]} /></span>
              <span className={`cp-time ${u.pct >= 90 ? 'cp-bad' : ''}`}>{Math.round(u.pct)}%</span>
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
      {open && (session || nextUp) && (
        <div className="cp-foot" data-testid="companion-foot">
          {session && <>
            <span className="cp-meter"><span style={{ width: `${Math.min(100, Math.max(0, session.pct))}%` }} /></span>
            <span className={session.pct >= 90 ? 'cp-bad' : ''}>{Math.round(session.pct)}%{session.resetsAt ? ` · resets ${at(Date.parse(session.resetsAt))}` : ''}</span>
          </>}
          {nextUp && <span className="cp-next" title={nextUp.title}>Next wake-up {at(nextUp.at)}</span>}
        </div>
      )}
    </div>
  )
}
