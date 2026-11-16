import { test } from 'vitest'
import assert from 'node:assert/strict'
import { MAX_SHAPES, parseFaceSvg } from '../../src/shared/orbieFaceCore'

test('keeps simple shapes, colours by name, numbers inside the view', () => {
  const s = parseFaceSvg('<circle cx="15" cy="18.5" r="2" fill="ink"/><path d="M23 18.5q2-1.6 4 0" stroke="pink" stroke-width="9"/><ellipse cx="999" cy="20" rx="1" ry="1"/><ellipse cx="20" cy="25" rx="1" ry="1" fill="#ff0000"/>')
  assert.deepEqual(s[0], { tag: 'circle', attrs: { cx: 15, cy: 18.5, r: 2, fill: 'var(--hi)' } })
  assert.equal(s[1].attrs.d, 'M23 18.5q2-1.6 4 0') // a relative move keeps its negative numbers
  assert.equal(s[1].attrs.stroke, '#f7768e')
  assert.equal(s[1].attrs.fill, 'none')
  assert.equal(s[1].attrs.strokeWidth, 3)
  assert.equal(s.length, 3) // the shape off Orbie is dropped, not dragged to the edge
  assert.equal(s[2].attrs.fill, 'var(--hi)') // a colour not by name falls back to ink
})

test('drops anything that is not a plain shape', () => {
  const s = parseFaceSvg('<script>alert(1)</script><image href="x.png"/><path d="M0 0 url(evil)"/><circle cx="1" cy="1" r="1" onclick="x()" style="fill:red"/><text x="1">hi</text><foreignObject/>')
  assert.equal(s.length, 1)
  assert.deepEqual(Object.keys(s[0].attrs).sort(), ['cx', 'cy', 'fill', 'r'])
  assert.equal(parseFaceSvg('<circle cx="1" cy="1" r="1"/>'.repeat(30)).length, MAX_SHAPES)
})

test('never a new planet: shapes bigger than a face part are dropped', () => {
  assert.equal(parseFaceSvg('<circle cx="20" cy="20" r="12" fill="gold"/><ellipse cx="20" cy="20" rx="12" ry="3"/><circle cx="15" cy="18" r="2"/>').length, 1)
})
