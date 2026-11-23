// The plan's usage limits stacked at the foot of the side bar: one line each, its name, a bar in its own colour, the
// share used and when it resets. Nearly out or ahead of the clock turns it the warning colour. A click on the rows opens
// the Usage page; with more than Claude's, a click on an agent's name folds its rows to one line of shares.
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
  const [folded, setFolded] = useState<string[]>(() => { try { return JSON.parse(localStorage.getItem(FOLD_KEY) ?? '[]') } catch { return [] } })
  const fold = (next: string[]) => { setFolded(next); try { localStorage.setItem(FOLD_KEY, JSON.stringify(next)) } catch { /* kept for this run only */ } }
  if (!rows.length && !others.length) return null
  const groups = [...(rows.length ? [{ name: 'Claude', rows }] : []), ...others]
  // names and folding only when there is more than Claude's
  const many = groups.length > 1
  const allFolded = groups.every((g) => folded.includes(g.name))
  return (
    <div className="limfoot">
      <UsageGuardSwitch />
      {many && (
        <div className="limhead">
          <span>Usage</span>
          <button type="button" onClick={() => fold(allFolded ? [] : groups.map((g) => g.name))} data-testid="limits-fold-all">{allFolded ? 'Open all' : 'Fold all'}</button>
        </div>
      )}
      <div className="limrows" data-testid="side-limits">
        {groups.map((grp) => {
          const gauges = grp.rows.map((u) => gaugeOf(u, now))
          const shut = many && folded.includes(grp.name)
          return (
            <div key={grp.name} className={`limgroup${shut ? ' shut' : ''}`} data-testid={`limits-${grp.name.toLowerCase()}`}>
              {many && (
                <button type="button" className="limgroup-n" aria-expanded={!shut} onClick={() => fold(shut ? folded.filter((n) => n !== grp.name) : [...folded, grp.name])}
                  title={shut ? `Open ${grp.name}'s limits` : `Fold ${grp.name}'s limits to one line`} data-testid={`limits-fold-${grp.name.toLowerCase()}`}>
                  <span className="limgroup-t">{grp.name}</span>
                  {/* folded: each limit's share on the name line, in its colour; one ahead of the clock in the warning colour */}
                  {shut && gauges.map((g) => (
                    <span key={g.key} className={`limsum${g.ahead ? ' ahead' : ''}`} data-k={kindOf(g.key)} title={g.says}>{g.short} <b>{Math.round(g.used)}%</b></span>
                  ))}
                  <i className="limgroup-c" aria-hidden>{shut ? '▸' : '▾'}</i>
                </button>
              )}
              {!shut && (
                <button type="button" className="limgroup-rows" onClick={onOpen} title="Usage limits: click for the Usage page">
                  {gauges.map((g) => (
                    <span key={g.key} className={`limrow${g.ahead ? ' ahead' : ''}`} data-k={kindOf(g.key)} title={g.says}>
                      <span className="limrow-n">{g.short}</span>
                      <span className="limrow-g"><i style={{ width: `${g.used}%` }} /></span>
                      <b>{Math.round(g.used)}%</b>
                      <em>{g.resets}</em>
                    </span>
                  ))}
                </button>
              )}
            </div>
          )
        })}
      </div>
    </div>
  )
}

// which agents' limits are folded to one line, kept across restarts
const FOLD_KEY = 'orbit.limits.folded'
const kindOf = (key: string) => /five_hour$/.test(key) ? 'five_hour' : /seven_day$/.test(key) ? 'seven_day' : 'model'

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
