# Remaining-features implementation plan

Finish every open backlog item. Quality bar unchanged: opt-in / neutral default, reflection hooks
fail closed with a pure unit-tested core, full suite green after each. Order = testable-first, then
reflection hooks, then a final review + build.

## Batch A — fully-testable core features
1. **Per-rank multipliers** — per-rank `cost-multiplier` in rankups.yml/prestiges.yml. `costFactor`
   on the requirement base gains a per-rank factor (set by `RankRequirements` from the rank section,
   copied on clone). Applies on top of global/permission/event/sale. Tests: neutral default, per-rank
   discount stacks with a sale.
2. **Item-count scaling** — `ItemDeductibleRequirement` honours the cost factor (rounded, min 0) so a
   sale discounts item costs too, like money/XP/tokens. Test: scaled `getTotal` + deduction.
3. **Scoreboard sidebar** — `progress-display.scoreboard.{enabled,title,lines,interval}`. Pure
   `ScoreboardLines` renders configured line templates (%rank%/%next%/%percent%/%bar%/%player%);
   `ScoreboardDisplay` runnable pushes them to a per-player sidebar. Tests: line rendering.
4. **Menu ladder picker** — advanced-menu hub lists configured ladders and opens the chosen one.
   Pure selection tested.
5. **Rebirth follow-ups** — reset-prestige on rebirth, auto-rebirth in the auto task,
   `%rankup_*_rebirth%` placeholders. Tests: reset + placeholder resolution.

## Batch B — GUI polish (Track 2)
6. **Page-turn slide** + **distinct prestige/rebirth stingers** + **shimmer icons** — animation
   flags + pure frame/stinger selection tested; visuals are live.
7. **Resource-pack texture mode** — `menu.texture-mode` maps icons to custom-model-data ranges;
   pure mapping tested (full visual is live).

## Batch C — reflection dependency hooks (pure core tested, live-validate)
8. **CMI groups** — `CmiGroupProvider` reflection `GroupProvider` (alongside LuckPerms/Vault).
9. **Native holograms** — `HologramProvider` reflection adapter (DecentHolograms/HolographicDisplays)
   auto-creating a managed leaderboard hologram at a config location.
10. **TAB / scoreboard driver hook** — feed progress placeholders to TAB.
11. **BetonQuest requirement** — `betonquest-tag <tag>` via `hasTag`, reflection, pure `matches`.
12. **Economy adapters** — GemsEconomy + RedisEconomy reflection `Economy`; PlayerPoints as a
    *currency requirement type* (`playerpoints <n>` deductible).

## Batch D — finalise
13. Multi-agent adversarial review over all new reflection/runtime diffs; fix confirmed findings.
14. Full `./gradlew test` + a clean compile; update roadmap docs + memory. Report.

---

## Outcome (status)

**Shipped (tested, committed):**
- A1 per-rank multipliers · A2 item-count scaling · A3 scoreboard sidebar · A5 rebirth follow-ups
  (reset-prestige, auto-rebirth, placeholders) — all with unit tests.
- C economy/requirement hooks: GemsEconomy backend · PlayerPoints currency requirement · BetonQuest
  tag requirement (reflection, fail closed, live-validate) · rebirth celebration stinger.
- A4 multi-ladder picker menu (`LadderPickerMenu`, ladder-aware path/rankup).

**Satisfied by existing mechanisms (no new code needed):**
- **TAB / FeatherBoard** and **DecentHolograms / HolographicDisplays**: read the plugin's
  PlaceholderAPI output (`top_<n>_*`, progress placeholders), so leaderboard/progress lines already
  work — a native auto-spawned hologram is the only extra, and it is live-visual.
- **CMI groups** and **RedisEconomy**: both register a Vault provider, so they already work through
  the `vault` group/economy backend.
- **Resource-pack GUI theming**: per-icon `custom-model-data` + namespaced item ids (Oraxen/
  ItemsAdder/Nexo/HeadDatabase) already ship (Track 2 M1), so menus can be fully re-textured.

**Remaining (cosmetic/visual, need a live client — not unit-testable):**
- Menu page-turn slide + shimmer icons (extra animation flags on the existing ticker).
- Native auto-spawned/refreshed leaderboard hologram (DecentHolograms API).
These are polish on top of already-working features; deferred pending live-client validation.
