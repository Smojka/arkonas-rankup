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

**Deferred to M2b:** a `theme.progress.style: blocks|smooth|gradient` toggle wired into
`RequirementItemRenderer` (the renderer pre-builds animation frames with `partialBar`, so smooth/
gradient need frame-aware plumbing and a MiniMessage lore path for the bar line).

## M3 — Transitions + sound palette  (deferred, specced)

- **Transitions**: an open-reveal (cascade/wipe fill of border + tiles) and a page-turn slide,
  driven by the existing `MenuTicker` frame clock. Add `theme.transitions.{open,page}` toggles.
- **Sound palette**: expand `MenuTheme` sounds beyond open/click/page/deny — per-screen open
  sounds, a rankup jingle (a short note sequence with pitch ramp), distinct prestige/rebirth
  stingers, and a confirm-hold pitch ramp. Config `theme.sounds.*` + `theme.jingle`.
- **Multi-frame cycling icons**: allow an icon to define frames the ticker cycles (shimmer/glint).

## M4 — Resource-pack custom-texture GUI mode  (deferred, specced)

Optional fully-custom GUI backgrounds via the negative-space-font method (ItemsAdder/Oraxen/Nexo):
the inventory title carries offset unicode chars that render a custom background texture, and menu
items use namespaced custom-model-data. Config `theme.background: ":offset_-8::rankup_bg:"` and a
resource-pack section. Needs a hook to the pack plugin (Track 4) for namespaced items.

## Sequencing
`M1 → M2` shipped as capabilities. `M2b` (renderer wiring), `M3` (transitions/sounds), `M4`
(resource-pack mode) follow, each green before the next. M3/M4 lean on Track 4 dependency hooks
(Oraxen/ItemsAdder/Nexo, HeadDatabase) for namespaced content.
