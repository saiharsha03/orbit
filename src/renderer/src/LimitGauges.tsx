// The plan's usage limits stacked at the foot of the side bar: one line each, its name, a bar in its own colour, the
// share used and when it resets. Nearly out or ahead of the clock turns it the warning colour. A click opens the Usage page.
import { useEffect, useState } from 'react'
import { gaugeOf, type UsageRow } from '../../shared/limitsCore'

export function LimitRows({ onOpen }: { onOpen: () => void }) {
  const [rows, setRows] = useState<UsageRow[]>([])
  const [now, setNow] = useState(() => Date.now())
  useEffect(() => {
    let alive = true
    const load = () => void window.api.getStatus().then((s) => { if (alive) { setRows(s?.usage ?? []); setNow(Date.now()) } }).catch(() => {})
    load()
    const t = setInterval(load, 15_000)
    return () => { alive = false; clearInterval(t) }
  }, [])
  if (!rows.length) return null
  return (
    <button type="button" className="limrows" onClick={onOpen} title="Usage limits: click for the Usage page" data-testid="side-limits">
      {rows.map((u) => gaugeOf(u, now)).map((g) => (
        <span key={g.key} className={`limrow${g.ahead ? ' ahead' : ''}`} data-k={g.key === 'five_hour' || g.key === 'seven_day' ? g.key : 'model'} title={g.says}>
          <span className="limrow-n">{g.short}</span>
          <span className="limrow-g"><i style={{ width: `${g.used}%` }} /></span>
          <b>{Math.round(g.used)}%</b>
          <em>{g.resets}</em>
        </span>
      ))}
    </button>
  )
}
