// Work log: one day at a time. The summary (Sonnet writes it, you can edit it), then the threads you typed in that day,
// your commits, the to-dos you finished and the notes you changed. Days move with the arrows; Today comes back.
import { Fragment, useEffect, useState } from 'react'
import { openSession } from '../actions'
import { MdBody } from '../MdBody'
import { dayOf, shiftDay, type DayLog } from '../../../shared/worklogCore'
import './worklog.css'

const time = (ms: number) => new Date(ms).toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' })
const longDay = (day: string) => new Date(`${day}T12:00:00`).toLocaleDateString([], { weekday: 'long', month: 'long', day: 'numeric', year: 'numeric' })

type Side = 'all' | 'work' | 'personal'
const SIDES: { id: Side; label: string }[] = [{ id: 'all', label: 'All' }, { id: 'work', label: 'Work' }, { id: 'personal', label: 'Personal' }]
const plural = (n: number, one: string) => `${one}${n === 1 ? '' : 's'}`
// the summary's part for one side: its "## Work" or "## Personal" section, the whole text for All
function summaryPart(text: string, side: Side): string {
  if (side === 'all') return text
  const parts = text.split(/^(?=## )/m)
  const hit = parts.find((p) => p.toLowerCase().startsWith(`## ${side}`))
  return hit ? hit.replace(/^## [^\n]*\n/, '').trim() : ''
}
const keep = (side: Side) => (x: { personal: boolean }) => side === 'all' || x.personal === (side === 'personal')

export function WorkLogPage() {
  const today = dayOf(Date.now())
  const [day, setDay] = useState(today)
  const [log, setLog] = useState<DayLog | null>(null)
  const [error, setError] = useState('')
  const [writing, setWriting] = useState(false)
  const [edit, setEdit] = useState<string | null>(null)
  const [open, setOpen] = useState<string | null>(null)
  const [side, setSide] = useState<Side>('all')

  const load = (d = day) => {
    setError('')
    return window.api.workDay(d).then((l) => { if (l.day === d) setLog(l) }).catch((e) => setError(String(e?.message ?? e)))
  }
  useEffect(() => { setLog(null); setEdit(null); void load(day) }, [day]) // eslint-disable-line react-hooks/exhaustive-deps

  const summarize = async () => {
    setWriting(true)
    try { await window.api.writeWorkSummary(day); await load() } catch (e: any) { setError(String(e?.message ?? e)) } finally { setWriting(false) }
  }
  const save = async () => {
    if (edit === null) return
    try { await window.api.saveWorkSummary(day, edit); setEdit(null); await load() } catch (e: any) { setError(String(e?.message ?? e)) }
  }

  const threads = log ? log.threads.filter(keep(side)) : []
  const commits = log ? log.commits.filter(keep(side)) : []
  const repos = [...new Set(commits.map((c) => c.repo))]
  const prompts = threads.reduce((n, t) => n + t.prompts, 0)
  const kind = (personal: boolean) => <td className={`wl-kind${personal ? ' p' : ''}`}>{personal ? 'PERSONAL' : 'WORK'}</td>
  return (
    <div className="wl" data-testid="worklog">
      <div className="wl-nav">
        <button className="dlink" onClick={() => setDay(shiftDay(day, -1))} title="The day before" data-testid="wl-prev">Previous</button>
        <span className="wl-day" data-testid="wl-day">{longDay(day)}</span>
        <button className="dlink" onClick={() => setDay(shiftDay(day, 1))} disabled={day >= today} title="The day after" data-testid="wl-next">Next</button>
        {day !== today && <button className="dlink" onClick={() => setDay(today)} data-testid="wl-today">Today</button>}
        <input type="date" value={day} max={today} onChange={(e) => e.target.value && setDay(e.target.value)} aria-label="Pick a day" />
      </div>
      {error && <div className="derr">{error}</div>}
      {!log ? <div className="dnote">Reading the day...</div> : (
        <>
          <div className="wl-sides" role="tablist" aria-label="Work or personal">
            {SIDES.map((x) => <button key={x.id} className={`dlink${side === x.id ? ' won' : ''}`} onClick={() => setSide(x.id)} aria-pressed={side === x.id} data-testid={`wl-side-${x.id}`}>{x.label}</button>)}
          </div>
          <div className="wl-facts">
            <span><b>{threads.length}</b> {plural(threads.length, 'thread')}</span>
            <span><b>{prompts}</b> {plural(prompts, 'prompt')}</span>
            <span><b>{commits.length}</b> {plural(commits.length, 'commit')}{repos.length ? ` in ${repos.length} ${plural(repos.length, 'repo')}` : ''}</span>
            <span><b>{log.done.length}</b> {plural(log.done.length, 'to-do')} done</span>
          </div>

          <section className="wl-sec">
            <div className="wl-h">
              <h2>Summary</h2>
              {edit === null ? (
                <>
                  {log.summary && <button className="dlink" onClick={() => setEdit(log.summary)}>Edit</button>}
                  <button className="dbtn" onClick={summarize} disabled={writing || (!log.threads.length && !log.commits.length && !log.done.length)} data-testid="wl-write">
                    {writing ? 'Writing...' : log.summary ? 'Write again' : 'Write summary'}
                  </button>
                </>
              ) : (
                <>
                  <button className="dlink" onClick={() => setEdit(null)}>Cancel</button>
                  <button className="dbtn primary" onClick={save}>Save</button>
                </>
              )}
            </div>
            {edit !== null
              ? <textarea className="wl-edit" value={edit} onChange={(e) => setEdit(e.target.value)} spellCheck={false} />
              : summaryPart(log.summary, side) ? <div className="wl-sum"><MdBody text={summaryPart(log.summary, side)} copy={false} /></div>
                : <div className="dnote">{log.summary ? `Nothing ${side} in the summary.` : 'No summary for this day yet.'}</div>}
          </section>

          <section className="wl-sec">
            <div className="wl-h"><h2>Threads</h2></div>
            {!threads.length ? <div className="dnote">No prompts typed this day.</div> : (
              <table className="wl-t">
                <thead><tr><th>When</th>{side === 'all' && <th>Kind</th>}<th>Thread</th><th>Project</th><th className="num">Prompts</th></tr></thead>
                <tbody>
                  {threads.map((t) => (
                    <Fragment key={t.id}>
                      <tr className="wl-row" onClick={() => setOpen(open === t.id ? null : t.id)} title="Show what you asked">
                        <td className="wl-when">{time(t.first)}{t.last - t.first > 60_000 ? ` to ${time(t.last)}` : ''}</td>
                        {side === 'all' && kind(t.personal)}
                        <td className="wl-title">{t.title}</td>
                        <td>{t.project}</td>
                        <td className="num">{t.prompts}</td>
                      </tr>
                      {open === t.id && (
                        <tr className="wl-asked">
                          <td colSpan={side === 'all' ? 2 : 1} />
                          <td colSpan={3}>
                            <ol>{t.asked.map((a, i) => <li key={i}>{a}</li>)}</ol>
                            <button className="dlink" onClick={() => openSession({ id: t.id, title: t.title, project: t.project, cwd: t.cwd, updated: t.updated, started: t.started, path: t.path })}>Open thread</button>
                          </td>
                        </tr>
                      )}
                    </Fragment>
                  ))}
                </tbody>
              </table>
            )}
          </section>

          <section className="wl-sec">
            <div className="wl-h"><h2>Commits</h2></div>
            {!commits.length ? <div className="dnote">No commits of yours this day.</div> : (
              <table className="wl-t">
                <thead><tr><th>When</th>{side === 'all' && <th>Kind</th>}<th>Repo</th><th>Commit</th></tr></thead>
                <tbody>
                  {commits.map((c) => (
                    <tr key={c.hash}><td className="wl-when">{time(c.at)}</td>{side === 'all' && kind(c.personal)}<td>{c.repo}</td><td><span className="wl-hash">{c.hash}</span> {c.subject}</td></tr>
                  ))}
                </tbody>
              </table>
            )}
          </section>

          {(log.done.length > 0 || log.notes.length > 0) && (
            <section className="wl-sec wl-two">
              <div>
                <div className="wl-h"><h2>To-dos done</h2></div>
                {log.done.length ? <ul>{log.done.map((d, i) => <li key={i}>{d.text}</li>)}</ul> : <div className="dnote">None.</div>}
              </div>
              <div>
                <div className="wl-h"><h2>Notes changed</h2></div>
                {log.notes.length ? <ul>{log.notes.map((n, i) => <li key={i}>{n.title}</li>)}</ul> : <div className="dnote">None.</div>}
              </div>
            </section>
          )}
        </>
      )}
    </div>
  )
}
