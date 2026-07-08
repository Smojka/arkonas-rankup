# Track 2 — Modern GUI

Goal: lift the menu module from a "vanilla chest" look to a modern, resource-pack-ready GUI with
custom icons, smooth gradient progress, richer sound design and animated transitions. Additive and
opt-in: existing `menus.yml` keeps working unchanged.

Menu facts this design respects:
- Menus are raw Bukkit inventories built by `MenuItems.build(material, name, lore, glow)`.
- `RequirementIcons` mapped a requirement name → `Material` only.
- `ProgressBar` rendered a flat unicode bar (`▰▱`); the fill animation pre-builds one item per
  reveal step, so the ticker never runs Pebble.
- `MenuTheme` holds gradient/colours + four UI sounds (open/click/page/deny).

---

## M1 — Modern icons  (shipped)

`MenuIcon` — a resolved icon spec: material + optional `custom-model-data` (resource-pack models
from Oraxen / ItemsAdder / Nexo default packs) + optional `head-texture` (player name or base64)
+ `glow`. Parsed from either a material string or a config section, so simple configs are
unchanged. `MenuItems.build` gained a 6-arg overload applying CMD (reflection-guarded for pre-1.14)
and skull textures (reflection-guarded, best effort). `RequirementIcons` now stores `MenuIcon` per
requirement and exposes `iconFor(name)`; the renderer uses it. Reachable today via section-form
entries under `requirement-icons` in `menus.yml`.

Tests: CMD applied, glow, plain-string vs section parse, invalid-material fallback; pure-string
gradient/smooth bars.

## M2 — Smooth + gradient progress bars  (shipped, capability)

`ProgressBar.smoothBar(fraction)` renders eighth-block glyphs (`▏▎▍▌▋▊▉█`) so progress advances a
fraction of a cell at a time. `ProgressBar.gradientBar(fraction, from, to, empty)` emits a
MiniMessage `<gradient>` over the filled portion with a dim empty tail. Both unit-tested.

**Shipped (M2b):** `theme.progress.style: classic|smooth|gradient` wired through
`ProgressBar.styled` + `gradientPartial` (segment-indexed so the fill animation stays frame-safe)
and `MenuTheme.renderBar`; degrades gradient → classic under `message-format: legacy`.

## M3 — Transitions + sound palette  (shipped)

- **Open-reveal transition (shipped):** opt-in row-major wipe-in driven by `MenuTicker`; snapshot
  in `open()`, reveal in `tick()` as the first branch (honours the no-Pebble/no-alloc invariant),
  cancels on `refresh()`, snaps + swallows a mid-reveal click, and content animations key off a
  `revealDoneFrame`. Config `animation.open-reveal` + `open-reveal-speed` (default off).
- **Sound palette (shipped):** `MenuTheme.playSound` accepts a string or `{name,volume,pitch}`
  section (tunable UI sounds); rankup **jingle** in `CelebrationEffects` — an ascending note-block
  sequence, pitch-clamped, fired once per rankup via the MONITOR listener. effects.yml
  `rankup.jingle`.
- **Deferred:** page-turn slide transition, distinct prestige/rebirth stingers, multi-frame
  cycling (shimmer) icons.

## M4 — Resource-pack custom-texture GUI mode  (deferred, specced)

Optional fully-custom GUI backgrounds via the negative-space-font method (ItemsAdder/Oraxen/Nexo):
the inventory title carries offset unicode chars that render a custom background texture, and menu
items use namespaced custom-model-data. Config `theme.background: ":offset_-8::rankup_bg:"` and a
resource-pack section. Needs a hook to the pack plugin (Track 4) for namespaced items.

## Sequencing
`M1 → M2` shipped as capabilities. `M2b` (renderer wiring), `M3` (transitions/sounds), `M4`
(resource-pack mode) follow, each green before the next. M3/M4 lean on Track 4 dependency hooks
(Oraxen/ItemsAdder/Nexo, HeadDatabase) for namespaced content.
