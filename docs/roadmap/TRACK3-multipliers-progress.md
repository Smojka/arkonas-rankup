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

**Deferred to M1b:** a `/aru booster <factor> <seconds>` admin command (service method exists),
per-rank multipliers, and applying the factor to non-money currencies (tokens/points).

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

## M3 — Milestone rewards + sales  (deferred, specced)

- **Milestones**: reward commands every N rankups/prestiges (reads the stats service counts).
- **Sales/events**: scheduled temporary global discounts via the event booster from M1.
