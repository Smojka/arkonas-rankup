# Track 1 — Core Progression

Goal: lift ArkonasRanks from single-ladder Rankup3 parity to a superior progression
engine. Four features, shipped as back-compat, opt-in milestones. Existing configs keep
working unchanged; every new capability is off by default.

Architecture facts this design must respect:
- A **rank is a permission group**. Current rank = highest group the player is in
  (`RankList.getByPlayer` scans the tree reversed, `Rank.isIn` = group membership).
- Ranks form a singly-linked `RankTree<Rank>` built by matching `next` fields
  (`RankList.findNext`). One root node per tree.
- Requirements are authored as strings (`"money 1000"`), split name/value in
  `RequirementRegistry.getRequirements`, value read via `Double.parseDouble`
  (`Requirement.getValueDouble`). Requirements are static, parsed once at load.
- `plugin.getRankups()` → single `Rankups`; `plugin.getPrestiges()` → single `Prestiges`.
- `AutoRankup` already auto-rankups **and** auto-prestiges (loops online players with
  `rankup.auto`).

---

## M1 — Formula-scaled costs  (smallest, safest, first)

Problem: admins hand-write a `money` requirement for every rank (100+ ranks = 100 lines).
Competitors (EZRanksPro `%cost%`, PrisonRanksX cost-increase POWER/EXTRA/custom) generate
cost from a formula.

Design — **expand at the serialized-string layer, before the tree is built**. Zero change
to the requirement engine.

Config (`config.yml`, opt-in):
```yaml
cost-formula:
  enabled: false
  requirement: money      # which requirement name to generate (money, xp-level, tokenmanager-tokens ...)
  expression: "1000 * 1.15 ^ {index}"   # {index}=0-based ladder position, {n}=ladder length, {prev}=previous rank cost
  round: 2                # decimal places, -1 = no rounding
  override: false         # false = only fill ranks with no explicit `<requirement>` line; true = replace
```

New code:
- `formula/MathExpression` — tiny shunting-yard evaluator. Tokens: numbers, `+ - * / % ^`,
  unary minus, parentheses, named variables, functions `min max floor ceil round pow sqrt abs`.
  Pure, no Bukkit → fully unit-testable.
- `formula/CostFormula` — holds parsed config, `String costFor(int index, int n, double prev)`.
- `formula/CostFormulaExpander` — given ordered `List<RankSerialized>`, computes ladder order
  by following `next` links from the root, then for each rank injects
  `"<requirement> <value>"` into its requirements list (skip if explicit present and
  `override:false`). Operates on serialized strings → unit-testable without a server.

Wire-in: `ArkonasRanksPlugin` rank-loading path calls the expander on the `List<RankSerialized>`
before `new Rankups(...)`. Same for each ladder (M3) and prestige layers (M4).

Tests: evaluator (precedence, `^` right-assoc, unary minus, funcs, div-by-zero, unknown var);
expander (linear/power presets, override on/off, explicit-wins, orphan rank, single rank,
rounding). Placeholder `%rankup_next_cost%` already covered by existing money placeholders.

---

## M2 — Auto chains

`AutoRankup` already does rankup + prestige. Add:
- **auto-max rankup**: when `rankup.auto` + `auto.max: true`, rank up repeatedly in one pass
  until requirements fail (bounded loop, guard against infinite via max-iterations).
- **auto-rebirth**: once M4 lands, extend the same loop to the rebirth layer.
- Config `auto: { rankup: true, prestige: true, rebirth: true, max: false, interval: <ticks> }`
  superseding the flat `autorankup-interval` (kept as alias for back-compat).
Tests: MockBukkit — player with met requirements auto-advances; max stops at unaffordable rank;
respects `rankup.auto` perm; disabled flags skip.

---

## M3 — Multi-ladder / tracks

Let a server run parallel ladders (e.g. `mining`, `combat`) a player progresses on
independently. The current single ladder becomes the `default` ladder — unchanged on disk.

Model:
- `ladder/LadderRegistry` — `Map<String, Rankups>` keyed by ladder id, plus `default`.
- Each extra ladder = its own file `ladders/<id>.yml` (+ optional `.toml`), same schema as
  `rankups.yml`, with an optional per-ladder `cost-formula` and `commands`.
- `plugin.getRankups()` keeps returning the default ladder (back-compat). Add
  `plugin.getLadder(id)`, `plugin.getLadders()`.
- Commands gain an optional trailing `<ladder>` arg: `/rankup [ladder]`, `/ranks [ladder]`,
  `/maxrankup [ladder]`. No arg = default. Tab-complete ladder ids.
- `RankupHelper` methods parameterized by ladder (overloads keep the no-ladder default).
- Leaderboards keyed by `(ladder, type)`; placeholders gain `%rankup_<ladder>_...%` forms;
  bare forms resolve against default.

Back-compat: no `ladders/` dir → behaves exactly as today. Menu module: hub lists ladders when
>1 exists, else opens the single ladder directly.

Tests: two-ladder fixture — independent current-rank per ladder, rankup on one doesn't move the
other, unknown ladder arg errors cleanly, default-only config unchanged.

---

## M4 — Infinite layers (prestige → rebirth → N)

Generalize the two-tier rankup/prestige into ordered **layers**. Layer 0 = rankup. Layer k>0
requires completion of layer k-1's top and optionally resets lower layers.

Pragmatic path (protect parity + 35 tests): keep `Prestige`/`Prestiges` as concrete layer 1.
Add `Rebirth`/`Rebirths` as concrete layer 2 mirroring the prestige classes
(`rebirths.yml`, `/rebirth`, `/rebirths`, `rankup.rebirth`), with reset semantics
(`reset-lower: true` transfers player back to first rank of layer 0 and, if configured, resets
prestige). Then extract the shared shape into a `ProgressionLayer` abstraction so further layers
(`ascension`…) are config-only.

Reset semantics config:
```yaml
rebirth:
  enabled: false
  reset-ranks: true       # send player back to first rankup group
  reset-prestige: false   # also reset prestige layer
  cost-formula: { ... }   # optional
```
Tests: rebirth requires top prestige; reset moves groups correctly; auto-rebirth; placeholders
`%rankup_current_rebirth%` etc.; disabled → no `/rebirth`.

---

## Sequencing & commits
`M1 → M2 → M3 → M4`, one milestone per commit (matches existing `M14/M15/M16` history), each
green (`./gradlew test`) before the next. M1/M2 are additive and low-risk; M3 is the structural
core; M4 builds on M3's layer plumbing.
