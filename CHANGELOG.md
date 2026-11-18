# Changelog

All notable changes to Orbit (called Hive until 2.0) are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project uses
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [5.0.1] - 2026-11-18

### Removed

- The Running card in a thread's side bar. Background tasks still show, with Stop, on the Running page (Ctrl Shift R).

## [5.0.0] - 2026-11-17

### Changed

- Phone app redesigned on every screen: a new home with Orbie's greeting, Needs you / Working / Background counts
  and answer cards; four tabs (Home, Threads, To-dos, Scheduled) with More from the home header; one warm accent,
  Inter and Space Grotesk, real icons. Phone app 5.0.0.
- Desktop: a side bar in each thread with its goal, to-dos, running tasks (with Stop) and recap; a bigger thread
  title with folder, branch and model under it and a state pill; a session meter and New thread in the title bar;
  Inter as the UI font.
- Companion: a foot with the session meter and the next wake-up, Deny / Allow on permission questions, and a
  second count when folded.

### Fixed

- Phone: a sent message no longer shows twice, vanishes and comes back, or matches an older message with the same
  words; nothing shows through the tab bar or the message box.

## [4.8.0] - 2026-11-17

### Added

- Claude can change your to-dos with a new update_todo tool: tick one off, move it to Need to talk or Holding, set
  or clear its due date, or reword it. It finds the item by its text.

## [4.7.0] - 2026-11-17

### Added

- Work log on the phone (More, Work log), read-only: the day's summary, threads, commits and to-dos done, with
  Previous / Next and All / Work / Personal. Phone app 4.7.0.

### Changed

- The companion folds to just Orbie with a number on a see-through window: amber when something needs you, red when
  something failed, grey for the threads working. Nothing opens it by itself any more; a click on Orbie opens the
  list, and a click anywhere else folds it. Orbie's lines still show in a bubble above it.
- A thread's tab icon spins while the thread is working.

## [4.6.1] - 2026-11-17

### Changed

- The work log's summaries write themselves: from 10 AM each day, every one of the last 7 days that has no summary,
  or only one written before that day ended, gets one. A morning Orbit was closed is caught up the next time it runs.
  A summary you edited is never written over.

## [4.6.0] - 2026-11-17

### Added

- Work log, a new section in the title bar: one day at a time, the threads you typed in (when, how many prompts, and
  what you asked), your commits in every repo under the repos root, the to-dos you finished and the notes you changed.
- Each day is split into work and personal (All / Work / Personal). A repo is personal when its GitHub owner is
  Orbit's own; a thread run from the repos root is labelled once by Haiku from its title and first prompts.
- A summary per day in short bullet points under Work and Personal, written by Sonnet on request and editable. It is
  kept in `~/.orbit/hive/worklog/YYYY-MM-DD.md`.
- `scripts/worklog-backfill.mjs <from> <to>` writes the summaries for past days.

## [4.5.1] - 2026-11-16

### Changed

- No phone notifications while you are active in Orbit (its window focused, with input in the last 3 minutes), as
  Claude Code's own push notifications hold back while you are active in it.
- Answering or replying to a thread on the computer (the window or the companion) clears that thread's notifications
  from the phone.
- The phone app now carries the same version number as Orbit: phone app 4.5.1.

## [4.5.0] - 2026-11-16

### Added

- Goals: a thread working toward its /goal carries a goal badge (a pulsing target) and the goal on its row, and stays
  under Working between turns. When the goal is met the thread moves to a new Completed section (a green check) until
  you open it, the phone and the desktop are told, and Orbie throws confetti.
- The companion lists the goals: the ones being worked on (and for how long) and the ones completed. Orbie wears a
  little flag while a goal is on.
- Claude Code signals Orbit kept to itself: an API call being retried (overloaded, a network error) blinks the row
  amber with "Retrying 2/10" and makes Orbie sweat; the thread's todo list shows as "3/7"; a turn stopped at its turn
  or budget cap goes under Needs you.
- Orbie hears more, and says it while you are away: a goal set or met, a usage limit pausing a thread or crossing 90%,
  a pull request's checks going red or green or the PR merging, a watcher's alert, a wake-up firing, a dev stack
  service crashing, a script failing.

## [4.1.2] - 2026-11-16

### Fixed

- A thread with only a wake-up or a loop scheduled is no longer listed as working: it is on the Scheduled page. It shows
  as working only while something of it actually runs (a shell, a monitor, a subagent, a workflow, a Codex run).

## [4.1.1] - 2026-11-16

### Fixed

- A thread whose turn has ended but which still waits on something shows as working, saying what: background shells,
  monitors, subagents, workflows or Codex runs ("Waiting on 1 shell"). Shells and monitors used to leave it looking
  idle. Same in the companion and the phone app.

## [4.1.0] - 2026-11-16

### Added

- Orbie's greetings, written by Haiku from what is going on: good morning (the first hello of the day, with what
  finished or failed overnight, what waits for you, today's wake-ups and the usage left), welcome back (after 20+
  minutes locked or asleep) and the evening wrap (the first time you are around after 6 PM). What was still running
  when Orbit closed is told the next morning.
- Orbie's faces are drawn by Haiku for each line it says (winks, star eyes, a nightcap, a yawn...). Only plain shapes
  inside Orbie, in its own colours, are drawn.

## [4.0.0] - 2026-11-16

### Added

- The companion: a small always-on-top window with what needs you while you work in other apps. It lists waiting
  threads (answer their question or permission with one tap, or reply), failed turns, background tasks with Stop, the
  threads' pull requests and checks, deploys, working threads, plan limits and wake-ups. Ctrl+Alt+O or the tray menu.
  Inspired by [coucou](https://github.com/louis-cfm/coucou) by louis-cfm.
- Orbie, the companion's face: moods for idle, working, waiting, failed, finished and late at night; a moon per working
  thread, weather from the plan usage, a star per wake-up, a rocket on the pad while a deploy is checked.
- A rocket launches up the side of the screen when a deploy lands (poke Orbie for a test launch).
- Orbie talks: while you are away from Orbit, Haiku turns finished, failed and waiting threads and landed deploys into
  one short line in a speech bubble. Mute it from the companion.
- A failed turn is listed (companion, phone inbox) for every thread, not only background ones, with or without a phone.

### Changed

- The recap is written in the background 15 minutes after a live thread's last turn (was 30, and only while the thread
  was on screen), and shows only once it is written: no more "Writing a recap…".

## [3.0.7] - 2026-11-16

### Fixed

- The phone app is built only when it changed: a release whose tag has no change under mobile/ carries no APK.
- The phone's update card offers the newest phone app at its own version (from the APK's name), not the desktop
  release tag, so it no longer offers the same app again after every desktop release. Phone app 3.0.7.

## [3.0.6] - 2026-11-16

### Fixed

- The chat follows Claude's newest output. Sending a message jumps to the bottom at once, and while the thread follows,
  anything that grows it (new text, a tool's output, a card opening) keeps the bottom in view. Scrolling up stops
  following; scrolling back down or sending starts it again.

## [3.0.5] - 2026-11-16

### Fixed

- A thread is named once. Claude names it after its first reply and no longer renames it when the work moves on, and the
  automatic namer (after your fifth message) skips a thread that already has a name, including after Orbit restarts.

## [3.0.4] - 2026-11-16

### Changed

- The line a stopped turn leaves in the chat (Stop or Esc, Send now, the phone, Stop everything, team paused) is red,
  so it reads as a warning.

## [3.0.3] - 2026-11-16

### Changed

- A stopped turn leaves a line in the chat where it stopped, saying why: stopped by you (Stop or Esc), to send your
  message now, from the phone, by Stop everything, or because the team was paused.

### Fixed

- A message sent with Interrupt no longer shows the stopped turn as a failure.

## [3.0.2] - 2026-11-16

### Added

- The phone app offers its own update when a newer Orbit release has a phone APK (checked on open, at most daily).
- A usage guard switch above the limits, shown when a usage-guard hook is set up.

### Fixed

- A thread whose subagents are still running stays marked working, also after the window reloads.
- Closing a thread that still has subagents running asks first, also from the background list; it used to stop
  them without a word.
- The usage guard switch's knob sat outside its track.

## [3.0.1] - 2026-11-14

### Added

- Weekly dream, a watcher Orbit comes with: Sundays 09:00 (or at the next start) it reads the week's Claude Code
  sessions with Haiku and Sonnet helpers and keeps the auto-memory and skills accurate, short and deduplicated, and
  reports cleanup it would suggest. Made once; deleting it keeps it gone.

### Changed

- Thinking rows start with a small brain.
- Your messages: the read time and ticks inside the bubble, the sent time under it.

## [3.0.0] - 2026-11-14

A new look across the app and the phone: calm greys, colour only for state, no coloured bars or glows
(`design/v3-design-language.md`).

### Changed

- The title bar is a menu of words: Threads, Scheduled, Files, To do, Notes, Board, More and Settings. Scripts, Deploy,
  Pull, Allow IP and Terminal are in the status bar, which also shows the Orbit, SDK and Claude Code versions, an update
  in the warning colour.
- The thread list: one line per thread, with an icon for what it is, a ring for its context use, its state on the icon
  (working, needs your input, failed) and its prompt cache (time left, green to red, or cold). What needs you sits in
  one card on top. The usage limits are stacked at the foot, each in its own colour.
- The thread header: icons for Rewind, Advisor (a sage), Background, Split and Export, each opening a small card under
  it; context use and the prompt-cache countdown as pills; every PR the thread opened as a pill with a circle for its
  checks (the second-review gate left out).
- Tabs as VS Code's; your messages show the time and one tick when sent, two when Claude has read them; Rewind sits
  beside the bubble.
- Failed turns are cards; a usage limit reads as the Claude app shows it, with Try again, Switch to Opus and Open
  usage settings. "Waiting" reads "Needs your input".
- Every page, panel, dialog and the phone app in the same look.

### Added

- A recap ("the goal is X; next: Y") at the end of a thread idle for 30 minutes, written by a separate Sonnet call.
- `npm run next`: Orbit Next, run from source beside Orbit with hot reload.

### Fixed

- Esc stops a turn from anywhere in the thread, and closes a shown picture.
- A command moved to the background shows how it ended and its real output.
- Split opened the same new thread in both panes.
- A thread back from snooze stands out for a day, then goes back among the others.
- Commands only the terminal Claude Code runs (/design-login) open in the thread's terminal again, and their answer
  shows below them.

## [2.1.2] - 2026-11-14

### Fixed

- The phone's usage alert only watches the plan windows (5-hour and 7-day). A single model's own weekly limit, like
  Fable's, no longer sets it off.

## [2.1.1] - 2026-11-14

### Added

- Phone: attach photos and files with + in a thread. Photos reach Claude as images (shrunk to 1568 px); other files
  are saved under `phone-files` in Orbit's state folder and their path goes in the message. Up to 1 MB per message.
- Phone: the conversation in the chat apps' shape: your messages in bubbles, Claude's replies in a serif, tool runs
  folded to one line, a rounded message box whose round button sends, or stops Claude while it works.

### Fixed

- Phone: a message sent while Claude was working did not show until Claude took it in, so it looked lost. It now
  shows at once as SENDING, then NOT READ YET while it waits in the queue.
- The phone's usage alert repeated every five minutes: the reset time jitters by milliseconds between readings, so
  each reading looked like a new window. It is now said once per window.

### Removed

- Phone: the Go ahead / Wait / Show diff / Stop row under the conversation (Stop stays in the header) and the Mic
  buttons.

## [2.1.0] - 2026-11-14

### Added

- Session recap: coming back to a thread that moved on while you were away (three or more replies, idle now) shows
  Claude Code's one-line recap under the "Since you looked" line. It is written by a throwaway fork of the thread,
  the same way `/btw` answers, so nothing goes into the conversation.
- Idle compaction: a conversation of 200K tokens or more that has sat idle for 55 minutes is compacted just before
  its one-hour prompt cache expires, so the next message sends a summary instead of the whole conversation uncached.
  The thread notes "Compacted while idle". Watcher runs are left alone.

### Fixed

- "Since you looked" counted the replies you watched arrive live after switching back to a thread (it showed
  "13 replies" for a thread that had been on screen the whole time). It now counts only what came in while you were
  away, and is hidden when nothing did.
- The thread header showed only the latest pull request, and missed PRs opened earlier than the part of a long
  thread that was loaded. It now links every PR the thread opened, read from its whole transcript.

## [2.0.3] - 2026-11-14

### Fixed

- A permission request (Claude asking to run Bash, Edit and other tools) now shows its Allow / Allow for this thread /
  Deny card under the call in the thread. Before, only a "Waiting for you" line appeared, with nothing to click.
- A message's Copy button no longer covers the end of its first line: the text wraps around it.
- When a model's safeguards flag a request and Claude Code re-runs it on the fallback model (cyber: Opus 4.8, bio:
  Opus 5), the refused answer is removed from the thread and a note says what was flagged and that the thread now
  stays on the fallback model. Before, the refused answer stayed and only "Model changed" showed.
- The session e2e helpers open the folded model and effort summary before using its pickers.

### Changed

- Fast mode follows your usage credits, which is the only thing it is billed from on a subscription. While the usage
  reading says they are off or used up, the fast control reads "fast: no usage credits" and cannot be turned on, and a
  thread that has fast mode on is switched off. Hovering the control shows how much credit was used this month
  against the limit.

## [2.0.2] - 2026-11-13

### Fixed

- The thread header no longer lets the goal line cover the title at narrower widths: the title truncates, the goal,
  loop and wake-up lines truncate on their own, and when space is tight they move below the title.
- The collapsed Thinking row keeps underscores in names.

### Added

- A test-mode tour script, `scripts/tour-2.0.mjs`, that drives every 2.0 feature with screenshots.

## [2.0.1] - 2026-11-13

### Fixed

- The status line shows the phone link again once a relay is set up: Phone connected, Phone offline or Phone not
  paired, with a coloured dot. 2.0.0 showed it only when the phone was not paired.
- Old shortcuts named Hive that start this checkout are renamed Orbit, and the taskbar pin takes Orbit's icon and app
  id, so the running window groups with it.
- A watcher's end-of-run check allows a second of slack on file times.
- Test fixtures use neutral repo and person names.

## [2.0.0] - 2026-11-13 — Orbit

### Renamed

- **Hive is now Orbit.** Product name, app id, installer names, tray and taskbar identity.
- Orbit's state lives in `~/.orbit`. On the first start, `~/.hive` and Electron's storage (`%APPDATA%\Hive`) are copied
  to Orbit's places. The old folders are left untouched, and the phone keeps its pairing.
- The in-process tools are `mcp__orbit__*` now (were `mcp__hive__*`). Old transcripts that used the old names still
  read.
- The repo is `saiharsha03/orbit`; the update feed and releases follow it. The phone APK on a release is
  `Orbit-Phone-<version>.apk`.
- New Satellite logo, drawn in the theme's colours: app icon, tray, title bar, Android launcher and notification
  icons.

### New layout

- An icon rail down the left: Threads, Running, Scheduled, Files, and a **More** menu for every other section.
  Terminal and Settings sit at the foot.
- A running bar above the message box: what the thread has going now, one line, a row per task when opened.
- Header lines for a thread's goal, its loops, and what is coming up (loops, wake-ups, a pending automatic continue).
- The plan's 5-hour and 7-day limits as small gauges on the rail.
- One layout with toggles (rail, inspector, drawer, Focus) and ten themes. Colour across the app; a thread says why it
  is marked waiting.

### Claude Code parity

- **Plan approval card** when Claude finishes a plan: approve into accept edits or manual, or keep planning with
  feedback. The phone sees it as a question.
- **Shift+Tab** cycles the permission mode (Manual, Accept edits, Plan).
- Thinking rows in the conversation; **Alt+T** opens them by default. **Alt+O** turns fast mode on or off.
- A context meter with **Compact**.
- `/mcp`, `/agents`, `/permissions`, `/hooks`, `/memory`, `/resume`, `/branch` and `/add-dir` open in the app.
- **Ctrl+R** searches earlier prompts, **Ctrl+S** puts the draft aside and brings it back, **Alt+P** opens the model
  picker, **Tab** takes Claude Code's suggested next prompt.
- Comments on diff lines go to the thread as one message.
- **Worktree toggle** under the prompt box of a new thread (off by default, remembered per repo), a **Branch prefix**
  setting, and `.worktreeinclude`: the git-ignored files it names are copied into every worktree Orbit makes.

### Pull requests

- PR references in Claude's replies link to GitHub; a pull request a thread opened shows on its header.
- Changes shows the branch's pull request and the state of its checks (passing, failing, pending or none).
- **Ship** asks the thread to ship by the project's own rules; with a PR open it becomes **Update PR**.
- **Merge** with a confirm, **Merge when ready** (GitHub auto-merge), **Fix failing checks**, and **Auto-fix** (asks the
  thread once per head commit for failing checks, and once for new review comments).
- **Archive this thread**, and **Auto-archive** after the PR merges or closes. The worktree and branch are kept.

### Agent threads

- Another command-line agent (Codex CLI, OpenCode, any command) runs in a terminal tab of its own. Add it in
  Settings > Agents. Its state is a guess from the terminal and shows in the tab, the rail and the running bar.
  Restart and Stop are on the tab.

### Jira board

- **Board**: your Jira tickets as one column per status, done work left out, stage and type colours. A thread starts
  from a card. Read-only; set `HIVE_TRACKER_SITE`, `HIVE_TRACKER_EMAIL` and a token (see `.env.example`).

### Phone

- **Changes** screen for a thread: branch, pull request card, files, and one file's diff.
- **Merge** from the phone, with a confirm. A second tap before the screen redraws sends nothing.
- The app is named Orbit. Version 2.0.0 (code 10).

### Fixed

- A sign-in failure on a turn is retried once and said once.
- A thread says why it is marked waiting.
- Escape closes a file or picture a thread opened.
- **Send now** on a message Claude has not read yet, or on a queued one, stops the running tool call and sends it.
- A reference such as `and/or #3` is no longer read as a repo.
- Wake-ups and the Agent SDK auto-update are on again.

### Removed

- The office animation in the team view.
- The four old layouts (Default, Workbench, Thread strip & dock, Focus) and their per-layout themes. Saved choices are
  folded into the one layout.

## [1.1.5] - 2026-11-12

### Changed

- Closing a thread that is still working asks first: keep it working in the background, or close and stop it. An idle
  thread closes as before.

## [1.1.4] - 2026-11-10

### Added

- **Phone 1.2**: at the top of a thread its context and what it waits on, then Work (reviews apart, each task opening to
  its type, model, prompt and current step), Rewind and the PR gate; the advisor beside mode, model and effort; the
  computer's Claude Code and SDK versions in Settings. The app uses the phone's own font.

### Changed

- A hook's note after a tool call folds into that run of steps.

## [1.1.3] - 2026-11-10

### Fixed

- A limit's reset time says the day: today, tomorrow, a weekday or the date.

## [1.1.2] - 2026-11-10

### Added

- **Activity**: Reviews as their own section (Codex runs and subagents asked to review), one line for what the thread is
  waiting on (a review, subagents, the advisor), colour by kind, and each task opens to its type, model, prompt and
  current step. **Rewind** is its own tab.
- The time Claude wrote a reply shows under it.
- Quit Hive is in the palette.

### Fixed

- Interrupt and Stop no longer wait for a long shell command: after a few seconds the command is ended (Windows).
- The tray icon is drawn from the .ico.

### Changed

- The signed phone app is built and attached to a release by CI.

## [1.1.1] - 2026-11-10

### Changed

- Hive keeps its own state in `~/.hive`, not inside Claude Code's folder.
- Title bar, tabs, status line and the older pages follow the new frame.

### Added

- **Threads cut off when Hive closed carry on at the next start**: one message from Hive, a notice, a phone notification.
- **Phone 1.1**: a thread's mode, model and effort, Queue and Interrupt, Notes, To do, Watchers; messages as bubbles.
- The status line shows a per-model weekly limit when the plan reports one.

## [1.1.0] - 2026-11-10

### Changed

- **A new frame, laid out like a code editor**: an icon bar down the left with one icon per section. Files, Threads and
  Git open a side bar beside the tabs; Codex, Environments, Allow IP, Deploy, Notes, To do, Watchers, Running,
  Scheduled, Pull, Scripts, Dependabot, Usage, Quarry and Accounts are whole pages. The frame adapts to the window.
- **The conversation reads as a chat**: your messages in a bubble at the right, Claude's at the left, in the order
  Claude saw them. Thread groups (Pinned, Today, Yesterday, ...) fold from their heading.
- The default look takes the code editor's colours and type (Segoe UI, Consolas); Ctrl+= and Ctrl+- change the size.
- A snoozed thread stays snoozed when opened; a message brings it back.

### Added

- **Terminals of your own** in the bottom panel (new, kill, a list), beside each thread's terminal and environment logs.
- **Codex** as a section: a task box that runs `codex exec` once, with what it did and its answer.
- **Permission modes per thread** from the message box (bypass, auto, accept edits, ask, plan), with a card to allow or
  deny a tool in the modes that ask. Queue, Interrupt and Stop are always shown there; the advisor has its own picker.
- **Advisor calls** show as rows in the conversation. A question card takes number keys and Enter.
- **Activity** lists Subagents, Shells, Monitors, Wake-ups, Loops and Rewind points; a **PR gate** tab shows a
  workspace's review checklist for the commit at HEAD.
- The message box names the default model and effort and sets them; a small call names a thread after five messages.
- The status line shows every plan limit with when it resets (a per-model week too), and the Claude Code and SDK
  versions; newer ones are installed by themselves.
- Allow IP shows the command's output and can keep an address as already allowed.

### Fixed

- Watcher and Dependabot runs no longer wait on "the window is not ready" while something loads inside the page.

## [1.0.0] - 2026-11-10

### Added

- **Advisor per thread**: the model menu in the message box ends with an Advisor group (Off, Default with the
  Settings > General value, and the same models), switched live for that thread; the message box line says
  "advisor <model>" while the thread has one and nothing otherwise, and the Status page shows it.

- **Quarry inside Hive.** When the Quarry SQL app is installed (found from where it records itself, or named in
  Settings > General; "off" turns it off), Hive gets a Quarry page with Quarry's own UI, and threads get
  `quarry_connections`, `quarry_schema`, `quarry_query` and `quarry_explain`. Quarry enforces its own rules (one
  statement per call, read-only reads, no writes on production or read-only connections); a write runs only after you
  confirm that statement in the thread. Without Quarry nothing of it shows.

- **A new window.** The left rail opens on what needs you: each waiting question with its answer buttons right
  there, each failed thread with its reason and Retry. Below it, working and recent threads as plain one-line rows.
  Threads open as tabs and side-by-side panes; the right inspector has Activity (a timeline of subagents, shells,
  monitors and wake-ups with Output, Stop, Message and Open), Changes, Files and Status. Running, Scheduled,
  Deploys, Status and usage, Watchers and teams, Workspace and Settings are pages of their own.
- **Ten themes** in Settings > Appearance and the command palette: Quiet (the default), Graphite, Classic (the
  earlier Hive look), Midnight, Forest, Slate, Mocha, Light, Paper and High contrast. Ctrl+Alt+T moves to the next
  one. The editor keeps VS Code's own colours in every theme.
- **Fonts.** Inter and Cascadia Mono by default, with a choice of UI and code font in Settings > Appearance.
- **Phone app.** A "Needs me" inbox, answering a question from its notification, Stop everything, a Scheduled
  list with Run now, a usage guard and quiet hours. A relay already running must be updated for the notification
  buttons (`POST /v1/send`).
- **Advisor model** (Settings > General, empty = off): Claude Code's server-side advisor for new threads; a thread can
  pick its own, live; `/status` shows which one a thread runs with.
- **Workflows** switch (Settings > General: on, off, or Claude Code's default for your plan).
- A workflow run shows as Workflow with its script's name in Activity, on the Running page and on the phone, with Stop.
  A Workflow call in auto mode is never refused by Hive's permission handler.
- **Switch account without a restart.** Quiet threads move to the new login at once, working ones when their work
  ends; the dialog says how many follow.
- **Several accounts signed in at once.** Every login is saved as a profile (Add account is the browser sign-in once);
  switching to a saved one takes a moment, with no browser. Only the login's own keys are swapped (MCP logins and the
  CLI's other state stay), with a backup of the last five switches and a rollback if a write fails.
- Stop everything (palette and phone), optionally pausing wake-ups and automatic continues until resumed; run a
  wake-up now; quiet hours shared with the phone (desktop notifications go silent then, except failures).
- **Idle threads free their Claude process** after Settings > General "Close idle threads after" (default 30
  minutes, 0 = never); the tab stays and the next message resumes the session. A thread with anything still going
  (a turn, background work, a Codex run, a loop or wake-up, a question, a queued or unread message) never closes.
- **Defaults per kind of thread:** a background model and effort for watcher runs, Dependabot reviews and `/btw`, and a
  team model and effort for teammates (empty = the main default).
- **`/status` and `/usage`** show a live card for the thread: account, plan, model, effort, permission mode, session,
  context, MCP servers, plan limits with their reset times, and the thread's own token totals. The `/` menu also
  lists the CLI-only commands; picking one runs it in the thread's terminal.
- **Wake-ups and automatic continues** are listed in the command palette; picking one cancels it.
- **Allow my IP remembers every address it allowed**, so going between home and office no longer asks again; an
  address can be forgotten from the command palette, and Settings can make allowed addresses expire.
- Hive writes `~/.claude/usage/latest.json` from its plan-limit readings, so a usage-guard hook that reads it keeps
  working while only Hive is open.
- Threads in a repo or worktree under the workspace root share the root's Claude Code auto-memory.
- The Dependabot reviewer runs at most `HIVE_DEPENDABOT_MAX_REVIEWS` reviews at once (default 3).
- A teammate's brief carries the newest role memory within a size budget (default 6 KB per file) and names the file.
- Closing or archiving a thread that would delete worktrees asks first and lists them.
- A watcher's name in the sidebar, the title-bar list or Settings opens that watcher.
- Phone: drafts are kept per thread; every action says when it failed; the thread you start opens at once.

- **Seen by Claude.** A message shows "seen" once Claude has taken it in; one steered into a running turn says
  "not read yet" until Claude reaches it (its next step, when the running tool call ends).

- **Tool rows open.** Click a tool call to see the whole command and its output (a FAILED one too), with Copy.
- **Desktop notifications.** A Windows toast and a taskbar flash when a thread waits for you, asks a question,
  finishes, fails or hits the usage limit while Hive is not focused; clicking it opens that thread. Settings has
  the switch (on by default).
- **Drafts and tabs survive a restart.** Unsent message text and the open thread tabs come back.
- **Model change note.** A line in the conversation when the answering model changes.
- **Export.** Copy or save a conversation as Markdown from the chat header.
- **Files in the message box.** Attach or drop any file (its path goes in as `@path`), and type `@` for a file
  picker.
- **Phone app.** Claude's replies render as Markdown, message text can be selected, the thread list has search,
  pull to refresh and a time on each row.
- **Command palette finds files.** From two typed characters, files under the current folder whose name or path
  match are listed under Files (Enter opens one as a tab); matches rank label-starts-with first, then word-start,
  then anywhere.
- **Keyboard shortcuts.** Ctrl+N new thread, Ctrl+W close the tab, Ctrl+Tab / Ctrl+Shift+Tab next / previous tab,
  Ctrl+1 to 9 the Nth tab of the focused pane. Not while the terminal or the editor has the focus (except Ctrl+Tab).
- **Find in conversation.** Ctrl+F in a chat opens a find bar at the top right of the thread with n of m,
  previous / next and close.
- **Sidebar arrow keys.** Up / Down (and Home / End) move between thread rows.

- **Teams.** Ask a thread for a team: a Lead plans the job on a shared board, picks teammates from a role library,
  each teammate runs as its own thread. Team view with an animated office, chat with a teammate, pause and resume,
  and a memory per role kept across teams.
- **Wake-ups that survive restarts.** A thread can schedule its own follow-up; Hive keeps it on disk and wakes the
  thread after a restart or sleep.
- **Rewind a turn.** Put back the files Claude changed since one of your messages and fork the conversation from
  before it, or rewind the conversation only.
- **Handoff offer.** A thread that has grown large (200k tokens of context by default, `HIVE_HANDOFF_AT`) or has
  just compacted is offered a fresh thread: Claude writes a handoff file and a new thread in the same folder starts
  by reading it, taking the worktrees with it.
- **Switch account** in the status bar (Claude Code's own browser sign-in, then the app restarts on the new login).
- **`agent_status` tool.** What a thread's subagents, shells, workflows and Codex runs are doing right now.
- **Auto-update.** The installed app checks GitHub Releases at start and every 6 hours, downloads in the background
  and offers Restart to update in the status bar; the portable build shows the new version. Check for updates is
  in the command palette.
- **Phone app, Teams.** The Android app gets a Teams screen (state words, members, latest posts, Pause / Resume).
- **Deploy watch command.** `HIVE_DEPLOY_WATCH_CMD` runs your own landed check after the push and streams its
  lines into the deploy wizard.
- **Interactive CLI commands in the terminal.** Claude Code commands only the interactive CLI runs
  (`/design-login`, `/login`, `/mcp`, ...) open in the thread's terminal.
- **Claude in Chrome setting.** Threads Hive starts can use your Chrome through the Claude extension
  (`claude --chrome`); on by default, switchable in Settings.

- **Threads.** Each tab is a live Claude Code session through the Claude Agent SDK: one long-running query per
  thread in streaming-input mode, so every message continues the same conversation and reuses the prompt cache.
  Replies stream in, tool calls show as one-line rows, file edits show as `+12 −3 path` rows that open a diff.
  Messages typed while Claude works are queued and can be edited, removed or sent now.
- **Thread management.** Working and waiting states, pin, snooze, close and archive; background threads that run in
  auto permission mode; threads can start, message and wait for each other through Hive's own tools.
- **Composer.** A `/` command menu, model and effort pickers, `ultrathink` highlighting, image paste and drop,
  a one-click clipboard suggestion, and a live status strip for `/goal`, loops, workflows and subagents.
- **Session history and search.** Every past Claude Code session in the sidebar; hybrid search over them with BM25
  (MiniSearch) and local MiniLM embeddings fused by reciprocal rank, with the model running in a worker thread.
- **In-process MCP tools** for the agent: show a file in the preview panel, add a to-do, search past sessions,
  organize the thread, open a git worktree, drive the dev stack, run Codex as a subagent, talk to other threads,
  and create and manage watchers.
- **A worktree per thread.** The agent works in a git worktree made for its branch or ticket; closing or archiving
  the thread saves uncommitted work as a WIP commit and removes the worktree, keeping the branch.
- **Git panel.** Repos under the workspace and their worktrees: stage, unstage, commit (hooks always run), branch,
  log and push on click, with split or unified diffs. No discard, reset or force actions.
- **Files and preview.** A file tree with VS Code style git colours; a preview panel for images, HTML, PDF, video and
  text, served through a sandboxed custom protocol.
- **Terminal drawer.** A real shell per thread (node-pty) that survives switching tabs; closing it ends the whole
  process tree.
- **Watchers.** Scheduled background agents, each a folder with its own long-lived thread, prompt, memory, SQLite
  database, reports and findings; fixed intervals, daily and weekly report slots, or an adaptive interval that
  follows the weekly usage limit.
- **Ops panels.** Pull latest across repos, a dev stack runner (docker compose services and local processes),
  a deploy wizard driven by your own scripts, a scripts panel for recurring commands, and a daily Dependabot
  reviewer that only reports.
- **To-dos** that you and Claude can add to, with due dates and statuses.
- **Settings tab** for every feature, stored in one settings file; credential-looking keys stay in `.env`.
- **Status bar** with MCP connections, plan and 5-hour / 7-day usage, context size and prompt-cache countdown.
- **Hooks and plugins** switches for Claude Code settings, with a backup before every write.
- **Engineering.** Vitest suite (unit tests for the pure cores, integration tests against real git, pty and
  Python), ESLint with typescript-eslint and react-hooks, GitHub Actions CI on Windows and Ubuntu, and an
  electron-builder Windows installer and portable build published from version tags.

### Changed

- Background, team and watcher threads switch off the learning and explanatory output styles.
- `/btw` runs on the background model.
- Every watcher run header starts with a fixed mark, so a watcher's session stays read-only whatever it is called
  (old transcripts are still recognised). Watcher names cannot hold `[ ]` or line breaks; intervals have a 5-minute floor.
- More credential-looking keys stay out of the settings store (access keys, `*_PASS`, `*_KEY`, DSNs, webhook and
  database URLs); one effort list everywhere, with `xhigh`.
- Phone: the pairing key is kept in the phone's keystore behind its screen lock (the app needs one); pairing needs an
  `https://` relay; nothing of the app goes into a backup; a release build must be signed with your own keystore
  (`HIVE_KEYSTORE`, `HIVE_KEYSTORE_PASSWORD`, `HIVE_KEY_ALIAS`), never the debug key; the desk token goes in the
  Authorization header.
- e2e: every thread runs on Haiku at low effort without Chrome, nothing is checked against GitHub, sessions are listed
  from a seeded folder (`HIVE_PROJECTS_DIR`), and a teardown deletes the run's folder and transcripts. It still uses
  your real Claude login.
- CI: actions pinned to commit SHAs, checks on Ubuntu and the build on Windows; a manual release builds the tag given;
  Dependabot watches npm, Actions and the phone's Gradle build.

- Spinners and blinking dots stop under `prefers-reduced-motion`, not only the office view.

- **Usage in the status bar** reads the limits from the running threads within seconds of each reply, shows only
  the windows your plan has, and a thread that hits a limit continues by itself after the reset.

### Notes

First public release.
