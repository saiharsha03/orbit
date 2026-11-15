# Orbit v3 design language

What the v3 redesign settled on, from the owner's feedback on 2026-11-14. Every page, panel and the phone app follow it.
The reference implementation is the thread sidebar (`Rail.tsx`), the thread header (`SessionView.tsx` `.phead`), the
error card (`ErrorCard`, `.errcard`), the header cards (`.hpop`) and the status bar (`StatusLine.tsx`).

## Principles
- **Nothing that "screams AI".** No coloured left bars, no accent washes behind rows, no glows, no moving lines, no
  filled bright-blue buttons, no gradient text. Calm and near-monochrome, like shadcn/ui, Linear, VS Code.
- **Greys carry the layout; colour carries state only.** Green = good/passing/warm, amber = needs you / getting low /
  running, red = failed / nearly out, blue-grey = cold/idle. A colour never decorates.
- **Words over icons in navigation** (the title bar is a menu bar of words). **Icons over words in dense toolbars**
  (the thread header), each with its name on hover.
- **Small rounded shapes**: rows and buttons 6–8px, cards 9–12px, pills 5–6px, dots round. The app's global
  `border-radius: 0 !important` reset means each rounded element needs `.app <selector> { border-radius: Npx !important; }`.

## Pieces
- **Section label**: 11.5px, weight 500, `var(--faint)`, count on the right in the same grey. No uppercase, no bar.
- **Row** (list item): one line, ~30px tall, 5px 8px padding, radius 7px. Hover = `color-mix(in srgb, var(--hi) 6%, transparent)`,
  selected = 10%. A leading icon (codicon, 12–14px, `var(--dim)`) says what the row is; state rides on the icon as a
  small badge (spinner = working, amber dot = needs you, red dot = failed). Secondary facts go in the hover title, not
  a second line.
- **Pill** (a fact beside a title: context %, cache, PR, version): `inline-flex`, height 22–26px, padding 0 8px,
  12px weight 500 tabular numbers, `box-shadow: inset 0 0 0 1px var(--rule2)`, radius 6px. A state colour tints the
  text/border, never fills it (except a selected toggle: solid fill in its colour).
- **Card** (an error, a confirm, a small dialog, a header popover): border 1px `var(--rule2)`, radius 12px, a header
  strip a shade lighter (`color-mix(in srgb, var(--hi) 4–6%, var(--bg))`) with an icon + title (13.5px), the body text
  in `var(--dim)` 13.5px/1.5, actions right-aligned at the bottom: secondary first as `btn btn-quiet`, the main action
  last as `btn btn-strong` (light fill, dark text). See `.errcard`, `.hpop`.
- **Buttons**: `btn-quiet` (lift2 fill, hi text) and `btn-strong` (hi fill, bg text), 30px tall, radius 8px. Outlined
  quiet button for "New …" (`box-shadow: inset 0 0 0 1px var(--rule2)`, transparent). No accent-blue filled buttons.
- **Tabs** (VS Code style): the selected tab takes the content's darker background and joins it; others sit on the
  strip; no accent top line. A small icon before the title.
- **Meters**: thin bars 4–6px, radius 2–3px, in the item's own colour (limits: 5-hour blue `#7aa2f7`, 7-day violet
  `#bb9af7`, model amber `#e0af68`); nearly out turns the number red, not the bar.
- **Read receipts** (chat bubbles): time + one tick sent, two ticks read (accent); actions like Rewind sit outside the
  bubble on hover.
- **Empty states**: one plain grey sentence saying what will appear and how; a quiet button if there is one action.

## Don'ts (all came up in review)
- Coloured inset left borders (`box-shadow: inset 3px 0 0 …`) on rows, headings, titles, cards.
- Uppercase "WAITING"-style state words where a dot or a pill would do; use "Needs your input" when words are needed.
- Two stacked labels that collide (e.g. "Seen by" over "Rewind").
- Text on a long command line in a header; raw IDs where names exist.
- Popups that are full-width modals for two buttons: use a small card under the button.
