// The plan's usage limits stacked at the foot of the side bar: one line each, its name, a bar in its own colour, the
// share used and when it resets. Nearly out or ahead of the clock turns it the warning colour. A click opens the Usage page.
import { useEffect, useState } from 'react'
import { gaugeOf, type UsageRow } from '../../shared/limitsCore'

export function LimitRows({ onOpen }: { onOpen: () => void }) {
  const [rows, setRows] = useState<UsageRow[]>([])
  // Codex's and Gemini's own limits, under Claude's (main reads them at most every 5 minutes)
  const [others, setOthers] = useState<{ name: string; rows: UsageRow[] }[]>([])
  const [now, setNow] = useState(() => Date.now())
  useEffect(() => {
    let alive = true
    const load = () => void window.api.getStatus().then((s) => { if (alive) { setRows(s?.usage ?? []); setNow(Date.now()) } }).catch(() => {})
    const loadOthers = () => void window.api.usageProviders().then((u) => {
      if (alive) setOthers([{ name: 'Codex', rows: u?.codex ?? [] }, { name: 'Gemini', rows: u?.gemini ?? [] }].filter((g) => g.rows.length))
    }).catch(() => {})
    load()
    loadOthers()
    const t = setInterval(load, 15_000)
    const t2 = setInterval(loadOthers, 60_000)
    return () => { alive = false; clearInterval(t); clearInterval(t2) }
  }, [])
  if (!rows.length && !others.length) return null
  const groups = [...(rows.length ? [{ name: 'Claude', rows }] : []), ...others]
  return (
    <div className="limfoot">
      <UsageGuardSwitch />
    <button type="button" className="limrows" onClick={onOpen} title="Usage limits: click for the Usage page" data-testid="side-limits">
      {groups.map((grp) => (
        <span key={grp.name} className="limgroup" data-testid={`limits-${grp.name.toLowerCase()}`}>
          {/* a name over each agent's rows only when there is more than Claude's */}
          {groups.length > 1 && <span className="limgroup-n">{grp.name}</span>}
          {grp.rows.map((u) => gaugeOf(u, now)).map((g) => (
            <span key={g.key} className={`limrow${g.ahead ? ' ahead' : ''}`} data-k={/five_hour$/.test(g.key) ? 'five_hour' : /seven_day$/.test(g.key) ? 'seven_day' : 'model'} title={g.says}>
              <span className="limrow-n">{g.short}</span>
              <span className="limrow-g"><i style={{ width: `${g.used}%` }} /></span>
              <b>{Math.round(g.used)}%</b>
              <em>{g.resets}</em>
            </span>
          ))}
        </span>
      ))}
    </button>
    </div>
  )
}

// the usage guard (a Claude Code hook that warns and then stops work near the limits), on or off from here; shown only
// when the user's settings run one
function UsageGuardSwitch() {
  const [g, setG] = useState<{ available: boolean; on: boolean } | null>(null)
  useEffect(() => { void window.api.usageGuardGet().then(setG).catch(() => {}) }, [])
  if (!g?.available) return null
  const flip = () => void window.api.usageGuardSet(!g.on).then(setG).catch(() => {})
  return (
    <div className="guard-row">
      <span>Usage guard</span>
      <button type="button" role="switch" aria-checked={g.on} className={g.on ? 'guard-sw on' : 'guard-sw'} onClick={flip} data-testid="usage-guard-switch"
        title={g.on ? 'On: Claude Code warns and then stops work near the usage limits. Click to turn it off' : 'Off: work goes on up to the limits. Click to turn it on'}>
        <i />
      </button>
    </div>
  )
}
