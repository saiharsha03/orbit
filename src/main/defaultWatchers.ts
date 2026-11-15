// The watchers Orbit comes with. Each is created once, the first time Orbit starts with watchers on; one the user
// deletes or renames is not made again (the ids seeded are kept in the watchers folder). Their prompts say nothing
// about any one person or project: they find the user's folders from Claude Code's own layout.
import type { WatcherInput } from '../shared/watchersCore'

export const DREAM: WatcherInput & { id: string } = {
  id: 'weekly-dream',
  name: 'Weekly dream',
  goal: 'Once a week, go through the Claude Code sessions since the last run and consolidate what was learned: keep the auto-memory accurate, short and deduplicated, fold lessons into the skills, and report cleanup candidates.',
  schedule: { every: 'off', daily: 'off', weekly: 'Sun 09:00' },
  prompt: `# Weekly dream: consolidate memory and skills

You run once a week (Sunday 09:00; if Orbit was closed then, at its next start). You are the "dreaming" step of
Anthropic's agent memory (https://platform.claude.com/docs/en/managed-agents/dreams): read what happened since the
last run, then rewrite memory so it stays true, short and useful: merge duplicates, fix what changed, drop what is
wrong or done, add what is new.

## 1. What to read
- The window: from the date in your memory folder's \`last_run.txt\` to now (no file: the last 7 days). Write the new
  date there at the very end, only if the run finished.
- Sessions: Claude Code transcripts \`~/.claude/projects/*/*.jsonl\` changed in the window (skip watcher runs and
  sidechain files). They can be large: never read one whole into your own context.
- Fan out with subagents (Agent tool): model "haiku" reads sessions (one or a few each) and returns the goal, the
  decisions and corrections the user gave, facts about projects, tools and states (merged, released, done), mistakes
  and their fixes, open items. Model "sonnet" makes the judgement calls below. A handful in parallel at a time.

## 2. Memory
The auto-memory folders: \`~/.claude/projects/*/memory/\`, each with a \`MEMORY.md\` index that is loaded at session
start and cut off when long (keep it short: one line per memory, newest and open work first). Keep the format the
memory files already use (frontmatter with name, description and type; one fact per file; links between them).
- Add durable new facts and feedback (not one-off details, not what the code or git history already records).
- Update what changed; check a state (merged, released, deployed) against evidence (git, gh, a file) before writing
  it, or mark it unverified.
- Merge duplicates; delete memories that are wrong or fully superseded, keeping useful history in the one that stays.
- Never write secrets, tokens or passwords.

## 3. Skills
\`~/.claude/skills/*\` and each project's \`.claude/skills/*\`: where sessions show a skill gave wrong steps, missed a
case or was corrected, fold the lesson into its SKILL.md with a small, specific edit in its own format. Propose new
skills in the report instead of creating them.

## 4. Report only
Hooks (settings files and their scripts), CLAUDE.md files and project folders (merged worktrees and branches, stale
files): do not change them. List what misfired or could be cleaned up, each with a reason.

## 5. Report
Write \`reports/<YYYY-MM-DD>.md\` in your folder: the window, sessions read, memory files added / changed / merged /
deleted (one line why each), skill edits, and the proposals. Add an Orbit to-do (add_todo) for each proposal that
needs the user's decision. Final reply: 5 to 8 short lines.

## Never
Delete or move code, repos, branches or worktrees; push; write to any external system or production; send messages;
print secrets. When unsure whether something still holds, keep it and mark it.
`,
}

export const DEFAULT_WATCHERS = [DREAM]
