// A face Haiku draws for Orbie: a few simple SVG shapes (eyes, mouth, and small extras around it) on Orbie's 40 x 40
// view (the planet is a circle of radius 12 at 20,20; its eyes sit near 15,18.5 and 25,18.5, its mouth near 20,25).
// Only what is read here is drawn: basic shapes, numbers kept inside the view, colours from Orbie's own set, at most
// MAX_SHAPES. Anything else (scripts, links, styles, text, events) is dropped.
export type FaceShape = { tag: 'path' | 'circle' | 'ellipse' | 'line' | 'polyline' | 'rect'; attrs: Record<string, string | number> }

export const MAX_SHAPES = 12
// the colour words Haiku may use, and what each is drawn with (the theme's tokens)
export const FACE_COLORS: Record<string, string> = {
  ink: 'var(--hi)', dim: 'var(--dim)', none: 'none', amber: 'var(--signal)', red: 'var(--fail)',
  blue: '#7aa2f7', pink: '#f7768e', green: '#9ece6a', violet: '#bb9af7', gold: '#e0af68', white: '#ffffff',
}
const NUM: Record<FaceShape['tag'], string[]> = {
  path: [], circle: ['cx', 'cy', 'r'], ellipse: ['cx', 'cy', 'rx', 'ry'], line: ['x1', 'y1', 'x2', 'y2'], polyline: [], rect: ['x', 'y', 'width', 'height', 'rx'],
}
const round = (v: number) => Math.round(v * 100) / 100
// a position or size: inside the view (a shape whose centre is outside is dropped below, not moved to the edge)
const clampN = (v: number) => round(Math.min(40, Math.max(0, v)))
// a number inside path data or points: relative moves are negative, so only bounded to the view's size (the face is
// clipped to the view where it is drawn)
const boundN = (v: number) => round(Math.min(40, Math.max(-40, v)))
const outside = (v: unknown) => typeof v === 'number' && (v < 0 || v > 40)
// the face, not a new planet: no shape bigger than this (a circle's r, an ellipse's rx or ry, half a rect's side)
export const MAX_PART = 6
const tooBig = (tag: FaceShape['tag'], a: Record<string, string | number>) =>
  (tag === 'circle' && Number(a.r) > MAX_PART) || (tag === 'ellipse' && Math.max(Number(a.rx), Number(a.ry)) > MAX_PART)
  || (tag === 'rect' && Math.max(Number(a.width), Number(a.height)) > MAX_PART * 2)

// one path's data, kept only when it is plain commands and numbers (no exponent tricks), each number clamped
function cleanPath(d: string): string | null {
  if (!/^[MmLlHhVvCcSsQqTtAaZz0-9.,\s-]+$/.test(d) || d.length > 400) return null
  return d.replace(/-?\d*\.?\d+/g, (n) => String(boundN(Number(n))))
}
function cleanPoints(p: string): string | null {
  if (!/^[0-9.,\s-]+$/.test(p) || p.length > 300) return null
  return p.replace(/-?\d*\.?\d+/g, (n) => String(boundN(Number(n))))
}

// the shapes in Haiku's answer, cleaned; [] when there are none it can draw
export function parseFaceSvg(text: string): FaceShape[] {
  const out: FaceShape[] = []
  for (const m of text.matchAll(/<(path|circle|ellipse|line|polyline|rect)\b([^<>]*?)\/?>/gi)) {
    const tag = m[1].toLowerCase() as FaceShape['tag']
    const raw: Record<string, string> = {}
    for (const a of m[2].matchAll(/([a-zA-Z-]+)\s*=\s*"([^"]*)"/g)) raw[a[1].toLowerCase()] = a[2].trim()
    const attrs: Record<string, string | number> = {}
    for (const k of NUM[tag]) { const n = Number(raw[k]); if (raw[k] !== undefined && Number.isFinite(n)) attrs[k] = n }
    if (['cx', 'cy', 'x', 'y'].some((k) => outside(attrs[k]))) continue // off Orbie: dropped, not dragged to the edge
    for (const k of NUM[tag]) if (typeof attrs[k] === 'number') attrs[k] = clampN(attrs[k] as number)
    if (tag === 'path') { const d = raw.d ? cleanPath(raw.d) : null; if (!d) continue; attrs.d = d }
    if (tag === 'polyline') { const p = raw.points ? cleanPoints(raw.points) : null; if (!p) continue; attrs.points = p }
    if (tag === 'circle' && attrs.r === undefined) continue
    if (tag === 'ellipse' && (attrs.rx === undefined || attrs.ry === undefined)) continue
    if (tag === 'rect' && (attrs.width === undefined || attrs.height === undefined)) continue
    // colours by name only; a shape with neither fill nor stroke given is filled with ink
    const fill = FACE_COLORS[(raw.fill ?? '').toLowerCase()]
    const stroke = FACE_COLORS[(raw.stroke ?? '').toLowerCase()]
    attrs.fill = fill ?? (stroke ? 'none' : FACE_COLORS.ink)
    if (stroke) {
      attrs.stroke = stroke
      const w = Number(raw['stroke-width'])
      attrs.strokeWidth = Number.isFinite(w) ? Math.min(3, Math.max(0.4, w)) : 1.4
      attrs.strokeLinecap = 'round'
      attrs.strokeLinejoin = 'round'
    }
    if (tooBig(tag, attrs)) continue
    out.push({ tag, attrs })
    if (out.length >= MAX_SHAPES) break
  }
  return out
}
