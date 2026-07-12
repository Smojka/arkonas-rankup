# ArkonasRanks

Feature-complete rankup system for Paper 1.21.x — a full-parity reimplementation of
[Rankup3 3.15.3](https://github.com/okx-code/Rankup3) with extra features on top.

Licensed **GPL-3.0** (derived from Rankup3 by Okx; see `LICENSE.txt`).

## Requirements

- Paper 1.21.x, Java 21
- **Vault** (required) + a Vault-compatible permission plugin (**LuckPerms** recommended)
- Optional: PlaceholderAPI, mcMMO, AdvancedAchievements, Towny, SuperbVote, VotingPlugin (6.14+), TokenManager

## Build

```
./gradlew build        # shaded jar in build/libs/
./gradlew test         # full test suite (JUnit 5 + MockBukkit)
```

## Rankup3 parity

Everything documented for Rankup3 3.15.3 works identically:

- `/rankup` (+ `noconfirm`), `/ranks` (text or GUI), `/maxrankup`, `/maxprestige`, `/prestige`, `/prestiges`
- Admin: `/arkonasranks` (aliases `/aru`, `/rankup3`, `/pru`): `reload`, `forcerankup`,
  `forceprestige`, `rankdown`, `playtime get|set|add`, `placeholders`, `tree`
- All permission nodes are unchanged (`rankup.*`)
- `rankups.yml` / `rankups.toml` (TOML wins) / `prestiges.yml`, per-rank message & GUI overrides,
  prestige-keyed requirement maps, display names, console commands
- Every requirement type incl. deductible/`h` (hold) variants and all plugin hooks
- Pebble templating (`{{ rank.req('money').total | money }}`), filters `money`, `simple`,
  `percent`, `shortmoney`; legacy `{MONEY}`-style placeholders still work
- Confirmation GUI + `/ranks` GUI incl. serialized `item:` stacks, index ranges
- PlaceholderAPI expansion `rankup` with the complete placeholder set
- Autorankup, cooldowns, permission-rankup mode, LuckPerms contexts
  (`luckperms-context`, `use-luckperms-group-names`)
- Custom requirement API: `RankupRegisterEvent`, `PlayerRankupEvent`, `PlayerPrestigeEvent`
  (now under `com.arkonas.ranks.events`; not binary-compatible with `sh.okx.rankup`)
- Existing Rankup3 config files (`config.yml` v10, rankups, prestiges, locales) drop in unchanged

### Intentionally preserved quirks

- `world` requirement only checks the first listed world
- `rankups.toml` silently takes precedence over `rankups.yml`
- `ranks`, `prestige`, `ranks-gui`, `max-rankup.enabled` changes need a restart
- First prestige entry has no `rank:` and assigns two groups (`to:` + `next:`)

### Fixes over Rankup3

- Cooldowns are keyed by UUID (no stale `Player` references)
- Update checker no longer polls SpigotMC (self-distributed)

## Extras

### MiniMessage + Türkçe

`message-format: auto|legacy|minimessage` in config.yml. In `auto` (default) one message can mix
legacy `&6`, hex `&#RRGGBB` and MiniMessage `<gradient:...>` tags. Full Turkish locale:
`locale: tr`.

### Celebration effects (`effects.yml`)

Sound, particles, no-damage fireworks, title, action bar, boss bar per rankup/prestige;
`broadcast-mode: none|world|server`. Per-rank override: add a `celebration:` section to any rank
in rankups.yml/prestiges.yml. Fires for manual, `/maxrankup`, autorankup and forced rankups.

### Statistics + leaderboards (`database:` in config.yml)

SQLite by default (`data.db`), optional MySQL/MariaDB. All writes async.

- `/rankup top [rankups|prestiges]`
- Placeholders: `%rankup_top_<n>_name%`, `%rankup_top_<n>_count%`,
  `%rankup_prestige_top_<n>_name%`, `%rankup_prestige_top_<n>_count%`,
  `%rankup_player_rankups%`, `%rankup_player_prestiges%`
- Disable with `database.enabled: false` — the core runs stateless, exactly like Rankup3

### Advanced menus / GUI overhaul (`menus.yml`)

When `menus.enabled: true` in config.yml (the shipped default), the player-facing
commands open animated pop-up inventory menus instead of writing to chat, and the menu
*is* the confirmation screen:

- `/rankup` → the rankup screen (requirement icons with unicode progress bars, a pulsing
  confirm button, or a live cooldown countdown)
- `/ranks` → the paginated rank-path map (completed / current / locked)
- `/prestige`, `/prestiges` → the prestige confirm and prestige ladder screens
- `/rankup top [prestiges]` → the leaderboard (three-head podium + rankups/prestiges toggle)
- Every screen has a bottom-row nav bar (Home / Back / Close)

Appearance is configured in `menus.yml`: a modern gradient `theme` (primary/secondary hex,
state colours, dark glass border with a chasing light), the `▰▱` progress bar, UI `sounds`,
per-requirement `requirement-icons`, per-menu row counts, and an `animation` block
(border chase, progress fill, confirm pulse, cooldown countdown; `period` in ticks). All
wording lives under the `menu:` section of the locale files (en + tr shipped). A single
lazy `BukkitRunnable` drives every open menu and only rewrites the slots that change; it
cancels itself when no menus are open.

**Interaction with parity settings:** while menus are active, `ranks-gui` and
`confirmation-type` are *ignored* — the menu already is the confirmation screen (a startup
INFO note is logged if you leave them set). The console, `/rankup noconfirm` and players
who are not in any ladder still fall through to the exact Rankup3-parity behaviour
(chat output). `/aru reload` closes any open menus and re-reads `menus.yml`.

**Full parity:** set `menus.enabled: false` (or use an older config without the key — an
absent key counts as false) and the plugin behaves exactly like the Rankup3-parity core:
chat listings, the `confirmation-type` GUI/text/none flow and the `ranks-gui` all return.

## Smoke test (real server)

1. Paper 1.21.x + LuckPerms + Vault + an economy (EssentialsX) + PlaceholderAPI
2. `lp creategroup a b c d p1 p2`, drop the jar, start
3. `/rankup` without money → rankup menu with unmet requirement icons + filling bar; give
   money → READY pulse → confirm → group change, effects, DB row
4. `/ranks` → rank-path map (current rank glows, click it → rankup menu → Cancel returns);
   `/prestige` at the top rank → prestige menu → confirm → reset + prestige group
   (`lp user <p> parent info`); `/rankup top` → leaderboard podium (toggle rankups/prestiges)
5. `/aru reload` with a menu open → it closes and reopens cleanly; set `menus.enabled: false`
   → parity fallback (`/rankup` GUI/text confirm, `/ranks` chat list)
6. `/aru tree`, `/aru placeholders`, `/papi parse me %rankup_next_rank%`
