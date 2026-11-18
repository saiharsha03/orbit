// A running subagent opens at the right when clicked: its own steps, Stop, and a box whose message Claude passes on.
// One real background subagent on Haiku that sleeps, so there is time to look at it.
import { expect, test } from '@playwright/test'
import { close, launch, shot, TAG, type Launched } from './harness'
import { Orbit } from './hive'

test.describe.serial('subagent panel', () => {
  let l: Launched
  let h: Orbit
  test.beforeAll(async () => { l = await launch(); h = new Orbit(l.page) })
  test.afterAll(async () => { await close(l) })

  test('clicking a running subagent opens its panel with what it is doing and a message box', async () => {
    test.setTimeout(300_000)
    await h.newSession() // the run's settings put every thread on Haiku at low effort
    await h.send(`${TAG}: Use the Agent tool once, with run_in_background set to true, subagent_type general-purpose, description "slow sleeper", `
      + 'and this prompt: "Use the Bash tool to run exactly: node -e \\"setTimeout(() => {}, 90000)\\" (it waits 90 seconds on purpose). Then reply with the word finished." '
      + 'Do not wait for it. Reply with the word started.')
    // the bar above the message box lists it; open the bar, then click the subagent's row
    await expect(h.id('running-bar')).toBeVisible({ timeout: 120_000 })
    const row = l.page.locator('[data-testid^="running-row-"]').first()
    if (!(await row.isVisible())) await h.id('running-count').first().click()
    await expect(row).toBeVisible({ timeout: 60_000 })
    await row.click()
    const panel = h.id('agent-panel')
    await expect(panel).toBeVisible()
    await expect(panel).toContainText('Running')
    // the whole panel is on screen, its Stop and × too
    await expect(h.id('agent-panel-stop')).toBeInViewport({ ratio: 1 })
    await expect(h.id('agent-panel-close')).toBeInViewport({ ratio: 1 })
    // its own transcript: the brief Claude gave it, then its Bash call
    await expect(panel).toContainText('Brief from Claude', { timeout: 60_000 })
    await expect(panel.locator('.tagent-tool')).not.toHaveCount(0, { timeout: 60_000 })
    await h.id('agent-panel-input').fill('hello from the panel')
    await h.id('agent-panel-input').press('Enter')
    await expect(panel).toContainText('hello from the panel')
    await shot(l.page, 'subagent-panel')
    // Stop ends it, and the panel says so; × closes it
    await h.id('agent-panel-stop').click()
    await expect(panel).toContainText(/Stopped|Done|Failed/, { timeout: 60_000 })
    await h.id('agent-panel-close').click()
    await expect(panel).toHaveCount(0)
  })
})
