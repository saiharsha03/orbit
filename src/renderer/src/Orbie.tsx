// Orbie, the companion's face: a small round planet with a ring that shows how Orbit is doing.
// idle: floats and blinks. work: eyes dart, the ring turns. wait: hops with a "?". fail: dizzy, X eyes. happy: a jump
// and closed smiling eyes (a thread finished, a deploy landed). Drawn in SVG, moved by CSS (companion.css).
export type Mood = 'idle' | 'work' | 'wait' | 'fail' | 'happy'

export function Orbie({ mood, size = 30 }: { mood: Mood; size?: number }) {
  return (
    <svg className={`orbie m-${mood}`} width={size} height={size} viewBox="0 0 40 40" aria-label={`Orbie: ${mood}`}>
      <g className="ob-body">
        {/* the ring's back half, behind the planet */}
        <ellipse className="ob-ring ob-back" cx="20" cy="23" rx="18" ry="5" />
        <circle className="ob-moon ob-far" cx="20" cy="23" r="1.6" />
        <circle className="ob-planet" cx="20" cy="20" r="12" />
        <ellipse className="ob-cheek" cx="13.5" cy="24" rx="2" ry="1.2" />
        <ellipse className="ob-cheek" cx="26.5" cy="24" rx="2" ry="1.2" />
        <g className="ob-eyes">
          {mood === 'fail' ? (
            <g className="ob-x">
              <path d="M13.5 16.5l3 3m0-3l-3 3M23.5 16.5l3 3m0-3l-3 3" />
            </g>
          ) : mood === 'happy' ? (
            <path className="ob-smile-eyes" d="M13.5 19q1.5-2.2 3 0M23.5 19q1.5-2.2 3 0" />
          ) : (
            <g className="ob-open">
              <ellipse className="ob-eye" cx="15" cy="18.5" rx="1.7" ry="2.2" />
              <ellipse className="ob-eye" cx="25" cy="18.5" rx="1.7" ry="2.2" />
            </g>
          )}
        </g>
        <path className="ob-mouth" d={mood === 'fail' ? 'M17.5 26q2.5-1.6 5 0' : mood === 'wait' ? 'M19 25.5a1 1 0 0 0 2 0a1 1 0 0 0-2 0' : 'M17.5 24.5q2.5 2 5 0'} />
        {/* the ring's front half, over the planet */}
        <path className="ob-ring ob-front" d="M2 23a18 5 0 0 0 36 0" />
        <circle className="ob-moon ob-near" cx="20" cy="23" r="1.6" />
      </g>
      {mood === 'wait' && <text className="ob-q" x="33" y="9">?</text>}
    </svg>
  )
}
