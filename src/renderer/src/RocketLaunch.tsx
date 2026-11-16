// The launch (?view=rocket&label=...): a see-through strip up the side of the screen that clicks pass through.
// 3, 2, 1, then the rocket shakes on ignition, lifts off from just above the taskbar in a cloud of smoke, trails sparkles
// and is gone off the top; the label says what landed. Main closes the window after.
import './companion.css'

const COUNT = ['3', '2', '1', 'Liftoff!']

export function RocketLaunch() {
  const label = new URLSearchParams(location.search).get('label') ?? ''
  return (
    <div className="rl">
      <div className="rl-count">{COUNT.map((c, i) => <span key={c} style={{ animationDelay: `${i * 0.38}s` }}>{c}</span>)}</div>
      {label && <div className="rl-label">✓ {label}</div>}
      <div className="rl-smoke">{Array.from({ length: 9 }, (_, i) => <span key={i} style={{ animationDelay: `${1.25 + i * 0.12}s`, left: `${30 + ((i * 41) % 52)}px` }} />)}</div>
      <div className="rl-ship">
        <svg className="rl-rocket" width="56" height="96" viewBox="0 0 28 48">
          <path className="rl-flame" d="M10 34q4 16 8 0z" />
          <path className="rl-fin" d="M7 24l-5 9v4l5-3zM21 24l5 9v4l-5-3z" />
          <path className="rl-hull" d="M14 1q8 8 8 22v12H6V23Q6 9 14 1z" />
          <circle className="rl-window" cx="14" cy="16" r="3.2" />
          <path className="rl-nozzle" d="M9 35h10l-1.5 2.5h-7z" />
        </svg>
        <div className="rl-trail">{Array.from({ length: 6 }, (_, i) => <span key={i} style={{ animationDelay: `${i * 0.11}s`, left: `${18 + ((i * 13) % 20)}px` }}>✦</span>)}</div>
      </div>
    </div>
  )
}
