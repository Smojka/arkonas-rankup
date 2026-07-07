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

- `/rankup` (+ `noconfirm`), `/ranks` (text or GUI), `/maxrankup`, `/prestige`, `/prestiges`
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

## Smoke test (real server)

1. Paper 1.21.x + LuckPerms + Vault + an economy (EssentialsX) + PlaceholderAPI
2. `lp creategroup a b c d p1 p2`, drop the jar, start
3. `/rankup` without money → requirements-not-met; with money → GUI confirm → group change,
   effects, DB row
4. `/prestige` at the top rank → reset + prestige group (`lp user <p> parent info`)
5. `/aru tree`, `/aru placeholders`, `/papi parse me %rankup_next_rank%`, `/rankup top`
