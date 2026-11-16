// Orbie, the companion's face: a small round planet with a ring that shows how Orbit is doing at a glance.
// mood (the face): idle floats and blinks; work looks around; wait hops with a "?"; fail is dizzy with X eyes; happy
//   jumps (a thread finished, a deploy landed); sleepy (idle late at night) has heavy eyes and a "z".
// moons: one per working thread, round the ring. weather: the plan usage (clear, cloud, rain, storm).
// rocket: a deploy on the pad while it is checked, gone up once it landed. stars: one per wake-up coming (up to 3).
// drawn: while Orbie says something, the face Haiku drew for it takes the place of the mood's eyes and mouth.
// Drawn in SVG, moved by CSS (companion.css).
import { useId } from 'react'
import type { FaceShape } from '../../shared/orbieFaceCore'
export type Mood = 'idle' | 'work' | 'wait' | 'fail' | 'happy' | 'sleepy'
export type Weather = 'clear' | 'cloudy' | 'rain' | 'storm'
export type Rocket = 'none' | 'pad' | 'launch'

export const MAX_MOONS = 6
export const weatherOf = (pct: number): Weather => (pct >= 90 ? 'storm' : pct >= 75 ? 'rain' : pct >= 50 ? 'cloudy' : 'clear')

// one turn of the ring per mood (companion.css): the moons are spread by starting part of a turn in
const MOON_SECONDS: Record<string, number> = { work: 2.2, fail: 1.2 }
const STARS = [{ x: 4, y: 6 }, { x: 36, y: 34 }, { x: 2, y: 33 }]

// one shape of a drawn face: only the tags and attributes orbieFaceCore.ts let through
function DrawnShape({ s }: { s: FaceShape }) {
  const Tag = s.tag
  return <Tag {...(s.attrs as Record<string, string | number>)} />
}

export function Orbie({ mood, moons = 0, weather = 'clear', rocket = 'none', stars = 0, size = 42, drawn }: { mood: Mood; moons?: number; weather?: Weather; rocket?: Rocket; stars?: number; size?: number; drawn?: FaceShape[] }) {
  const clip = `ob-clip${useId().replace(/:/g, '')}`
  const n = Math.min(MAX_MOONS, Math.max(0, moons))
  // the moons share one path, spread evenly by starting each part of the way round
  const turn = MOON_SECONDS[mood] ?? 9
  const phase = (i: number) => ({ animationDelay: `${(-turn * i) / Math.max(1, n)}s` })
  const moon = (side: 'far' | 'near') => Array.from({ length: n }, (_, i) => <circle key={`${side}${i}`} className={`ob-moon ob-${side}`} cx="20" cy="23" r="1.6" style={phase(i)} />)
  return (
    <svg className={`orbie m-${mood} w-${weather}`} width={size} height={size} viewBox="0 0 40 40" aria-label={`Orbie: ${mood}, ${n} working, ${weather}`}>
      {/* a drawn face stays inside Orbie's square */}
      <defs><clipPath id={clip}><rect x="0" y="0" width="40" height="40" /></clipPath></defs>
      {STARS.slice(0, Math.min(3, stars)).map((s, i) => <path key={i} className="ob-star" style={{ animationDelay: `${i * 0.7}s` }} d={`M${s.x} ${s.y - 1.6}L${s.x + 0.5} ${s.y - 0.5}L${s.x + 1.6} ${s.y}L${s.x + 0.5} ${s.y + 0.5}L${s.x} ${s.y + 1.6}L${s.x - 0.5} ${s.y + 0.5}L${s.x - 1.6} ${s.y}L${s.x - 0.5} ${s.y - 0.5}Z`} />)}
      <g className="ob-body">
        {/* the ring's back half, behind the planet */}
        <ellipse className="ob-ring ob-back" cx="20" cy="23" rx="18" ry="5" />
        {moon('far')}
        <circle className="ob-planet" cx="20" cy="20" r="12" />
        {!drawn?.length && <>
        <ellipse className="ob-cheek" cx="13.5" cy="24" rx="2" ry="1.2" />
        <ellipse className="ob-cheek" cx="26.5" cy="24" rx="2" ry="1.2" />
        <g className="ob-eyes">
          {mood === 'fail' ? (
            <g className="ob-x">
              <path d="M13.5 16.5l3 3m0-3l-3 3M23.5 16.5l3 3m0-3l-3 3" />
            </g>
          ) : mood === 'happy' ? (
            <path className="ob-smile-eyes" d="M13.5 19q1.5-2.2 3 0M23.5 19q1.5-2.2 3 0" />
          ) : mood === 'sleepy' ? (
            <path className="ob-smile-eyes" d="M13.3 18.5q1.7 1.4 3.4 0M23.3 18.5q1.7 1.4 3.4 0" />
          ) : (
            <g className="ob-open">
              <ellipse className="ob-eye" cx="15" cy="18.5" rx="1.7" ry="2.2" />
              <ellipse className="ob-eye" cx="25" cy="18.5" rx="1.7" ry="2.2" />
            </g>
          )}
        </g>
        <path className="ob-mouth" d={mood === 'fail' ? 'M17.5 26q2.5-1.6 5 0' : mood === 'wait' || mood === 'sleepy' ? 'M19 25.5a1 1 0 0 0 2 0a1 1 0 0 0-2 0' : 'M17.5 24.5q2.5 2 5 0'} />
        </>}
        {/* the ring's front half, over the planet */}
        <path className="ob-ring ob-front" d="M2 23a18 5 0 0 0 36 0" />
        {moon('near')}
        {/* the face Haiku drew for what Orbie is saying (orbieFaceCore.ts cleaned it), in place of the mood's own */}
        {!!drawn?.length && <g className="ob-drawn" clipPath={`url(#${clip})`}>{drawn.map((s, i) => <DrawnShape key={i} s={s} />)}</g>}
      </g>
      {weather !== 'clear' && (
        <g className="ob-weather">
          {(weather === 'rain' || weather === 'storm') && (
            <g className="ob-rain">
              <path style={{ animationDelay: '0s' }} d="M6 11.5v2.2" />
              <path style={{ animationDelay: '.3s' }} d="M9 12v2.2" />
              <path style={{ animationDelay: '.15s' }} d="M12 11.5v2.2" />
            </g>
          )}
          {weather === 'storm' && <path className="ob-bolt" d="M10 11l-2 3.4h2.2l-1.4 3.2l3.6-4.4h-2.2l1.4-2.2z" />}
          <path className="ob-cloud" d="M4.5 10.5h9a2.6 2.6 0 0 0-.4-5.2a3.6 3.6 0 0 0-6.8-.6a2.9 2.9 0 0 0-1.8 5.8z" />
        </g>
      )}
      {rocket !== 'none' && (
        <g className={`ob-rocket r-${rocket}`}>
          <path className="ob-flame" d="M35 37.5l1 2.4l1-2.4z" />
          <path className="ob-hull" d="M36 28.5q2 2.2 2 6v3h-4v-3q0-3.8 2-6z" />
          <path className="ob-fin" d="M34 35.5l-1.4 2.2h1.4zM38 35.5l1.4 2.2h-1.4z" />
        </g>
      )}
      {mood === 'wait' && <text className="ob-q" x="33" y="9">?</text>}
      {mood === 'sleepy' && <text className="ob-z" x="31" y="10">z</text>}
    </svg>
  )
}
