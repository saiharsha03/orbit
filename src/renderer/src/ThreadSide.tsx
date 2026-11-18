// The bar at the right of a thread: its goal, the to-do list Claude keeps (TodoWrite) with how far it is, and the
// recap written after the thread sat idle. A card shows only when the thread has that thing; with none there is no bar.
// It is drawn only in a pane wide enough for it (conversation.css), where it stands in for the goal on the header and the recap in the conversation.
import { useThreadStatus } from './statusStore'
import { useNow } from './ThreadStatus'
import { untilText } from '../../shared/cronCore'

export type SideTodo = { text: string; state: 'done' | 'now' | 'next' }

/** the list of the thread's last TodoWrite call; none when there is no list or it was emptied */
export function sideTodos(items: Item[]): SideTodo[] {
  let list: unknown = null
  for (let i = items.length - 1; i >= 0; i--) { const it = items[i]; if (it.kind === 'tool' && it.name === 'TodoWrite') { list = it.input?.todos; break } }
  if (!Array.isArray(list)) return []
  return list.flatMap((t: { content?: unknown; activeForm?: unknown; status?: unknown }) => {
    const text = String(t?.content ?? t?.activeForm ?? '').trim()
    return text ? [{ text, state: t.status === 'completed' ? 'done' as const : t.status === 'in_progress' ? 'now' as const : 'next' as const }] : []
  })
}

/** whether the thread has anything the bar would show */
export function useThreadSide(tabId: string, items: Item[], recap: string) {
  const status = useThreadStatus(tabId)
  // read on every render, not kept: the list grows in place while a turn runs (chat.ts), so it cannot key a memo
  const todos = sideTodos(items)
  return { goal: status.goal, todos, recap, any: !!status.goal || todos.length > 0 || !!recap }
}

export function ThreadSide({ side, onStopGoal, onDismissGoal }: {
  side: ReturnType<typeof useThreadSide>; onStopGoal: () => void; onDismissGoal: () => void
}) {
  const { goal, todos, recap } = side
  useNow(goal?.status === 'active')
  const done = todos.filter((t) => t.state === 'done').length
  return (
    <aside className="tside" aria-label="Thread details" data-testid="thread-side">
      {goal && (
        <section className={`tcard tcard-goal${goal.status === 'met' ? ' met' : ''}`} data-testid="side-goal">
          <div className="tcard-h">
            <i className={`codicon codicon-${goal.status === 'met' ? 'pass' : 'target'}`} aria-hidden />
            <b>Goal</b>
            <span className="tcard-n">{goal.status === 'met' ? 'met' : untilText(Date.now() - goal.setAt).replace(/ \d+s$/, '')}</span>
          </div>
          <p>{goal.condition}</p>
          <div className="tcard-acts">
            {goal.status === 'active'
              ? <button type="button" className="tbtn" onClick={onStopGoal} data-testid="side-goal-stop">Stop</button>
              : <button type="button" className="tbtn" onClick={onDismissGoal} data-testid="side-goal-dismiss">Dismiss</button>}
          </div>
        </section>
      )}
      {todos.length > 0 && (
        <section className="tcard" data-testid="side-todos">
          <div className="tcard-h"><b>To-dos</b><span className="tcard-n">{done} of {todos.length}</span></div>
          <div className="tbar" role="progressbar" aria-valuemin={0} aria-valuemax={todos.length} aria-valuenow={done}><i style={{ width: `${(done / todos.length) * 100}%` }} /></div>
          <ul className="ttodos">
            {todos.map((t, i) => <li key={i} className={t.state}>{t.text}</li>)}
          </ul>
        </section>
      )}
      {recap && (
        <section className="tcard" data-testid="side-recap">
          <div className="tcard-h"><b>Recap</b></div>
          <p className="trecap">{recap}</p>
        </section>
      )}
    </aside>
  )
}
