# Track 3 — Multipliers/boosters + live progress

## M1 — Cost multipliers  (shipped)

`MultiplierService.costFactor(player)` = `global * bestPermissionFactor * activeEventBooster`.
Applied to money requirements via the existing (previously always-1) `DeductibleRequirement`
multiplier seam: `MoneyRequirement.getTotal` scales the affordability threshold and
`MoneyDeductibleRequirement.apply` scales the deduction, so menus, placeholders and the actual
charge all agree. Factor &lt; 1 = discount, &gt; 1 = surcharge. Default (no config) = 1.0, so
parity is untouched. Config `multipliers.{global, permissions}`; a mutable event booster
(`setEventMultiplier`) is ready for a booster command.

Tests: disabled neutral, global, best-permission-wins, event active/expired, end-to-end discounted
rankup cost + deduction.

**M1b — `/aru booster` command (shipped):** `booster <factor> <duration> | clear | status`
(rankup.admin) starts a temporary server-wide cost booster/sale, e.g. `booster 0.5 30m` = half-price
rankups for 30 minutes. Pure `BoosterCommands` (duration units s/m/h/d, factor validation, apply via
`MultiplierService.setEventMultiplier`, human-readable status) is unit-tested (8); `InfoCommand` is a
thin adapter passing the clock in. Tab-complete + help added.

**M1c — non-money currency boosts (shipped):** the cost multiplier now applies to XP-level, token
(TokenManager) and vote-point (VotingPlugin) requirements too, not just money — so a VIP discount or
a sale halves whatever currency a rank costs. `costFactor` moved to the `Requirement` base; each
currency's `getTotal` scales (menus/placeholders/affordability agree) and each deductible's
`apply(player)` deducts the scaled amount. Also fixed a latent bug where the VotingPlugin points
deductible ignored the multiplier entirely. XP path is unit-tested end-to-end (MockBukkit);
token/vote mirror the money path.

**Still deferred:** per-rank multipliers, and scaling item-count requirements (fractional items).

## M2 — Live progress display  (deferred, specced)

Always-on progress feedback, not just on rankup:
- **Exp-bar mirror**: reflect rankup % on the vanilla XP bar (opt-in; restore on disable), updated
  on a short interval (PrisonRanksX uses 10 ticks).
- **Persistent boss bar**: a rankup progress bar with configurable colour/style, toggle per player.
- **Action bar**: periodic "next rank: X — 62%" line.
- **Scoreboard sidebar / TAB**: rank + progress lines (TAB hook in Track 4).
Config `progress-display.{expbar,bossbar,actionbar,scoreboard}` with intervals. A single
`BukkitRunnable` (like `MenuTicker`) drives updates; reuse the `ProgressBar` renderers from Track 2
for the bossbar/actionbar text.

## M3 — Milestone rewards  (shipped)

Reward commands when a player's cumulative rankup/prestige count hits a milestone (specific `at`
count, precedence over an `every N` cadence). `StatsService` gained a `MilestoneHook` invoked on
the stats thread right after the write, so the count is never raced; `MilestoneService` selects
(pure `commandsForRecord`) and dispatches on the main thread. Config `milestones.*`, opt-in, needs
the database. Tests: tier selection + record routing + config parse + fresh-count integration.

**M3b — recurring sales (shipped):** config `boosters.schedule` lists weekly windows
(`days` + `start`/`end` HH:mm server-local + `factor`); a `SaleScheduler` runnable reflects the
active window into the event slot each minute (auto-expires when the window closes). A manual
`/aru booster` always wins while active (tracked via an `eventManual` flag on `MultiplierService`);
the scheduler resumes when it expires. Pure `SaleSchedule` (parse/coverage/overlap-lowest/malformed-
skip) + `SaleScheduler.apply` precedence are unit-tested (10). Overlapping windows use the best
(lowest) factor. Deferred: per-rank multipliers, non-money currency boosts.
