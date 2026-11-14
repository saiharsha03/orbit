// Orbit's own state lives in its own folder, ~/.orbit (threads list, to-dos, notes, watchers, settings). Claude Code's
// files stay where Claude Code keeps them (~/.claude: conversations, login, its settings, the shared usage reading).
// NEXT keeps the conveniences of a run from source while the app is still being reworked: Ctrl+R / F5 and reloading
// on a new build.
export const NEXT = true
// ORBIT_NEXT=1 (npm run next): "Orbit Next", a copy run from source beside the Orbit in daily use, to try changes out
// with hot reload. Its own name, state folder (~/.orbit-next), Electron storage and instance lock, so the two never
// share a lock, a window or a file. Read in main only; the page has no process.env.
export const SIDE: boolean = typeof process !== 'undefined' && process.env?.ORBIT_NEXT === '1'
// Orbit is the app that was called Hive: its own name, state folder, Electron storage and instance lock. On the first
// start, Hive's state is copied over (carryOverCore.ts).
export const PRODUCT = SIDE ? 'Orbit Next' : 'Orbit'
/** the folder in the home folder for this app's state */
export const STATE_FOLDER = SIDE ? '.orbit-next' : '.orbit'
export const APP_ID = SIDE ? 'io.github.saiharsha03.orbit.next' : 'io.github.saiharsha03.orbit'
// true = this app wakes nothing by itself: no scheduled wake-up and no automatic continue after a usage limit (for a
// copy tried out beside the app in daily use). Orbit is the app in daily use, so it wakes threads; Orbit Next does not.
export const WAKEUPS_OFF: boolean = SIDE
