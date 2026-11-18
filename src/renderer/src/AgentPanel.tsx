// One subagent at the right of its thread, opened by clicking it where the thread lists its background work: what it
// has been doing (its own transcript: what it said and each tool call, read again every 2s while it runs), Stop, and a
// box to message it. Claude passes a message on with SendMessage: the SDK gives Orbit no way into a subagent itself.
import { useEffect, useRef, useState, useSyncExternalStore } from 'react'
import Markdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import { useThreadStatus } from './statusStore'
import { toolLine } from './chat'
import { passedOn, remember, sentOf } from './RunPanels'
import { taskHeading } from '../../shared/taskCore'

// which subagent each thread has open (tab -> task id), for the app's life
const openBy = new Map<string, string>()
const subs = new Set<() => void>()
const changed = () => subs.forEach((f) => f())
export function openAgentPanel(tabId: string, taskId: string) { openBy.set(tabId, taskId); changed() }
export function closeAgentPanel(tabId: string) { openBy.delete(tabId); changed() }
export function useOpenAgent(tabId: string): string | null {
  return useSyncExternalStore((f) => { subs.add(f); return () => { subs.delete(f) } }, () => openBy.get(tabId) ?? null)
}

const ENDED = { running: 'Running', completed: 'Done', failed: 'Failed', stopped: 'Stopped' } as const

// items: the thread's own, to see whether Claude's SendMessage carried a message on
export function AgentPanel({ tabId, taskId, busy, items, onSend }: {
  tabId: string; taskId: string; busy: boolean; items: Item[]
  onSend: (text: string, images: never[], mode?: 'steer' | 'queue') => void
}) {
  const t = useThreadStatus(tabId).tasks.find((x) => x.id === taskId)
  const running = t?.status === 'running'
  const [log, setLog] = useState<Item[]>([])
  const [text, setText] = useState('')
  const [, bump] = useState(0)
  const end = useRef<HTMLDivElement>(null)
  const stuck = useRef(true) // following the bottom until scrolled up

  // read once on open, then every 2s while it runs; once more when it ends, for its last words
  useEffect(() => {
    let live = true
    const read = () => window.api.chatAgentLog(tabId, taskId).then((v) => { if (live) setLog(v) }).catch(() => {})
    void read()
    const timer = running ? setInterval(read, 2000) : undefined
    return () => { live = false; if (timer) clearInterval(timer) }
  }, [tabId, taskId, running])
  useEffect(() => { if (stuck.current) end.current?.scrollIntoView({ block: 'end' }) }, [log])

  const sent = sentOf(tabId, taskId)
  const go = () => {
    const v = text.trim()
    if (!v || !t) return
    onSend(`[To subagent "${t.name ? `${t.name}: ` : ''}${t.title}" (task ${t.id})] ${v}`, [], busy ? 'steer' : undefined)
    remember(tabId, taskId, v)
    setText('')
    bump((n) => n + 1)
  }

  return (
    <aside className="tside tagent" aria-label="Subagent" data-testid="agent-panel">
      <div className="tagent-h">
        <div className="tagent-t">
          <b title={t ? taskHeading(t) : taskId}>{t ? taskHeading(t) : 'Subagent'}</b>
          <span className={`tagent-st${running ? ' on' : ''}`}>{t ? ENDED[t.status] : 'Ended'}{t?.name ? `, ${t.name}` : ''}</span>
        </div>
        {running && <button type="button" className="tbtn" onClick={() => void window.api.chatStopTask(tabId, taskId)} data-testid="agent-panel-stop">Stop</button>}
        <button type="button" className="tbtn tagent-x" aria-label="Close" onClick={() => closeAgentPanel(tabId)} data-testid="agent-panel-close">×</button>
      </div>
      <div className="tagent-log" onScroll={(e) => { const el = e.currentTarget; stuck.current = el.scrollHeight - el.scrollTop - el.clientHeight < 40 }}>
        {!log.length && <p className="tagent-empty">{running ? 'Starting…' : 'Nothing written yet.'}</p>}
        {log.map((it, i) => <LogRow key={i} it={it} first={i === 0} />)}
        {sent.map((m, i) => (
          <div key={`s${i}`} className="tagent-you"><span>You</span>{m.text}
            <em>{passedOn(items, m.text) ? 'passed on' : 'with Claude, passing on'}</em></div>
        ))}
        <div ref={end} />
      </div>
      {running && (
        <input className="tagent-in" value={text} placeholder="Message this subagent" onChange={(e) => setText(e.target.value)}
          onKeyDown={(e) => { if (e.key === 'Enter') { e.preventDefault(); go() } }} aria-label="Message this subagent" data-testid="agent-panel-input" />
      )}
    </aside>
  )
}

function LogRow({ it, first }: { it: Item; first: boolean }) {
  if (it.kind === 'user') {
    // the first is the brief Claude gave it; later ones are tool results or messages passed on
    if (!it.text.trim()) return null
    return <details className="tagent-brief" open={!first}><summary>{first ? 'Brief from Claude' : 'Message'}</summary><p>{it.text}</p></details>
  }
  if (it.kind === 'assistant') return it.text.trim() ? <div className="tagent-say md"><Markdown remarkPlugins={[remarkGfm]}>{it.text}</Markdown></div> : null
  if (it.kind === 'tool') {
    const l = toolLine(it.name, it.input)
    return <div className={`tagent-tool${it.status === 'error' ? ' err' : ''}`} title={l.what}><b>{l.verb}</b> {l.what}</div>
  }
  return null
}
