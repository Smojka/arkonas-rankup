# ArkonasRanks

A rankup, prestige and rebirth system for Paper 1.21.

Players climb a ladder of permission groups by meeting requirements: money, XP, playtime, mined
blocks, mcMMO levels, WorldGuard regions, quests, votes, tokens, or anything a PlaceholderAPI
placeholder can express. When they reach the top they can prestige, and when they max that out they
can be reborn. The rest of the plugin is built on top of that loop. Menus draw the requirements as
icons, effects fire on the group change, the database counts the rankups, and multipliers scale the
costs.

It is a full reimplementation of [Rankup3 3.15.3](https://github.com/okx-code/Rankup3), so existing
Rankup3 configs drop in unchanged. Licensed GPL-3.0 (see `LICENSE.txt`).

---

## Contents

- [Install](#install)
- [Quick start](#quick-start)
- [How it works](#how-it-works)
- [Commands](#commands)
- [Permissions](#permissions)
- [Defining ranks (`rankups.yml`)](#defining-ranks-rankupsyml)
- [Prestige (`prestiges.yml`)](#prestige-prestigesyml)
- [Rebirth](#rebirth)
- [Multiple ladders](#multiple-ladders)
- [Cost formulas](#cost-formulas)
- [Requirements](#requirements)
- [`config.yml` reference](#configyml-reference)
- [Menus](#menus)
- [Celebration effects (`effects.yml`)](#celebration-effects-effectsyml)
- [Live progress display](#live-progress-display)
- [Discounts, boosters and sales](#discounts-boosters-and-sales)
- [Milestones](#milestones)
- [Statistics and leaderboards](#statistics-and-leaderboards)
- [Integrations](#integrations)
- [PlaceholderAPI](#placeholderapi)
- [Developer API](#developer-api)
- [Upgrading](#upgrading)
- [Building](#building)

---

## Install

Required: Paper 1.21.x, Java 21, [Vault](https://www.spigotmc.org/resources/vault.34315/), and a
Vault-compatible permission plugin (LuckPerms recommended). An economy plugin is only needed if you
use the `money` requirement.

Drop the jar in `plugins/` and start the server. Everything else is optional.

> LuckPerms users should read [the inheritance gotcha](#the-luckperms-inheritance-gotcha) before
> configuring their groups. It is the most common reason `/rankup` reports that a player is not in
> any ladder.

## Quick start

Create the rank groups and let the shipped `rankups.yml` do the rest:

```
lp creategroup a
lp creategroup b
lp creategroup c
lp creategroup d
lp user Notch parent add a
```

The shipped ladder is `a → b → c → d`, and it costs money. Give yourself some, run `/rankup`, and
the rankup screen should open with a filling progress bar. Edit
`plugins/ArkonasRanks/rankups.yml` to make it yours, then run `/aru reload`.

## How it works

A rank is a permission group. `rankups.yml` links groups into a chain with `rank` and `next`, so
`a → b → c → d`. The plugin works out which rank a player is on by checking which of those groups
they belong to, and ranking up means moving them into the next one. You do not need commands to do
the move; the plugin does it.

Each rank lists what a player needs before they can leave it. Some requirements are deducted on
rankup, like money, items and tokens. Others are only checked, like playtime or standing in a
region.

At the top rank a player can `/prestige`. They go back to the first rank and gain a prestige group,
and requirements can scale per prestige, so the second lap costs more. Above prestige there is an
optional third tier, rebirth.

Ladders are independent of each other. Drop more ladder files in `ladders/` and a player progresses
through each one separately, so a mining track and a combat track can run side by side.

---

## Commands

Commands with a config gate are only registered when that option is on. When it is off the command
still exists but does nothing. Turning one on needs a server restart, because `/aru reload` re-reads
the config files without re-registering commands.

### Players

| Command | Permission | Needs | What it does |
| --- | --- | --- | --- |
| `/rankup` | `rankup.rankup` | - | Rank up. Opens the rankup menu, or uses `confirmation-type` when menus are off. |
| `/rankup <ladder>` | `rankup.rankup` | a file in `ladders/` | Rank up on a named ladder. Skips the menu and the confirmation. |
| `/rankup noconfirm [player]` | `rankup.noconfirm.other` for the `[player]` form | `enable-noconfirm` (default on) | Rank up with no confirmation. |
| `/rankup top [prestige]` | `rankup.top` | `database.enabled` | Top-10 leaderboard. |
| `/ranks` | `rankup.ranks` | `ranks` (default on) | The rank path, as a menu, a GUI or a chat list. |
| `/maxrankup [ladder]` | `rankup.maxrankup` | `max-rankup.enabled` (default off) | Rank up repeatedly until you cannot afford the next one. |
| `/prestige` | `rankup.prestige` | `prestige` (default off) | Prestige. |
| `/prestiges` | `rankup.prestiges` | `prestige` and `prestiges` | List the prestiges. |
| `/maxprestige` | `rankup.maxprestige` | `prestige` | Prestige as many times as possible in one pass. |
| `/rebirth` | `rankup.rebirth` | `rebirth.enabled` (default off) | Be reborn. |
| `/rebirths` | `rankup.rebirth` | `rebirth.enabled` | List the rebirth tiers. |

### Admin: `/arkonasranks`

Aliases: `/aru`, `/rankup3`, `/pru`.

| Command | Permission | What it does |
| --- | --- | --- |
| `/aru` | none | Version, and the help lines you have permission for. |
| `/aru reload` | `rankup.reload` | Re-read the config files. |
| `/aru forcerankup <player>` | `rankup.force` | Rank a player up, ignoring requirements. |
| `/aru forceprestige <player>` | `rankup.force` | Prestige a player, ignoring requirements. |
| `/aru rankdown <player>` | `rankup.force` | Move a player down one rank. |
| `/aru playtime get [player]` | `rankup.playtime.get` | Read the playtime statistic, in minutes. |
| `/aru playtime set <player> <minutes>` | `rankup.playtime` | Overwrite it. |
| `/aru playtime add <player> <minutes>` | `rankup.playtime` | Add to it. A negative value subtracts, and it never goes below zero. |
| `/aru booster <factor> <duration>` | `rankup.admin` | Start a server-wide sale. `/aru booster 0.5 30m` is half price for half an hour. Durations take `s`, `m`, `h` or `d`, and a bare number means seconds. |
| `/aru booster clear` | `rankup.admin` | End it. `stop` and `off` also work. |
| `/aru booster status` | `rankup.admin` | What is running, and for how much longer. |
| `/aru tree` | `rankup.admin` | Dump the rank chain. Not tab-completed. |
| `/aru placeholders [status]` | `rankup.admin` | Evaluate the common placeholders for yourself. Not tab-completed. |

## Permissions

| Node | Default | Grants |
| --- | --- | --- |
| `rankup.rankup` | everyone | `/rankup` |
| `rankup.ranks` | everyone | `/ranks` |
| `rankup.prestige` | everyone | `/prestige` |
| `rankup.prestiges` | everyone | `/prestiges` |
| `rankup.maxrankup` | everyone | `/maxrankup` |
| `rankup.maxprestige` | everyone | `/maxprestige` |
| `rankup.rebirth` | everyone | `/rebirth`, `/rebirths` |
| `rankup.top` | everyone | `/rankup top` |
| `rankup.auto` | everyone | Included in the auto-rankup sweep |
| `rankup.admin` | op | `/aru booster`, `/aru tree`, `/aru placeholders`, plus everything below |
| `rankup.reload` | op | `/aru reload` |
| `rankup.force` | op | `/aru forcerankup`, `/aru forceprestige`, `/aru rankdown` |
| `rankup.playtime` | op | `/aru playtime set` and `add` |
| `rankup.playtime.get` | op | `/aru playtime get` |
| `rankup.noconfirm.other` | op | `/rankup noconfirm <someone else>` |
| `rankup.notify` | op | Update notice on join |
| `rankup.checkversion` | op | Update notice from `/aru` |

Two more nodes are used but not declared, because you name them yourself:

- `rankup.rank.<RANK>`. With `permission-rankup: true`, this is how a player's rank is read.
- `rankup.multiplier.<tier>`. Your own discount tiers, described under
  [discounts](#discounts-boosters-and-sales).

`rankup.noconfirm` is declared in `plugin.yml` but nothing reads it. `/rankup noconfirm` for
yourself is gated only by `enable-noconfirm`.

---

## Defining ranks (`rankups.yml`)

Each top-level key is one rank. The key itself is only a label. What matters is `rank` and `next`.

```yaml
# the section name is arbitrary
Aexample:
  rank: 'A'                 # the permission group the player must be in
  next: 'B'                 # the group they rank up into
  display-name: '&aStone'   # optional, used by the *_name placeholders and the menus
  requirements:
    - 'money 1000'
    - 'xp-level 2'
  commands:                 # optional, run by the console after the group change
    - 'say {{player}} reached {{next.rank}}!'
  cost-multiplier: 1.0      # optional, a per-rank price factor
```

| Key | Meaning |
| --- | --- |
| `rank` | The group the player must hold to use this entry. |
| `next` | The group they move to. Required. A section with a blank `next` is logged and skipped. |
| `display-name` | Pretty name for the placeholders and the menus. It has no fallback: leave it out and `%rankup_next_rank_name%` renders the literal text `null`. Set it on every rank, or on none. |
| `requirements` | A list of `"<name> <value>"` lines, or a per-prestige map (below). |
| `commands` | Console commands. `{{player}}`, `{{rank.rank}}` (the old rank) and `{{next.rank}}` (the new one) are substituted, and PlaceholderAPI works. |
| `cost-multiplier` | Scales every cost on this rank. Default `1.0`. |
| `rankup:` | Per-rank message overrides: any locale path under `rankup.`, such as `requirements-not-met`, `success-public` or `list.current`. |
| `celebration:` | Per-rank effects override, described under [effects](#per-rank-celebration-override). |

### Requirements that scale with prestige

Write `requirements` as a map instead of a list. Each key is a prestige, and `default` is mandatory:

```yaml
Aexample:
  rank: 'A'
  next: 'B'
  requirements:
    default:            # required
      - 'money 1000'
    P1:                 # for players whose next prestige is P1
      - 'money 2000'
    P2:
      - 'money 4000'
```

This needs `prestige: true`. Ranks written this way are skipped by the
[cost formula](#cost-formulas).

### TOML

`rankups.toml` is loaded instead of `rankups.yml` if it exists, silently, with no warning. It
supports the same keys. There is no `prestiges.toml`; prestiges are YAML only.

## Prestige (`prestiges.yml`)

Only loaded when `prestige: true`. At the top rank a player prestiges: they are sent back down the
ladder and gain a prestige group.

```yaml
first:
  from: 'D'          # the rank they must be at
  to: 'A'            # the rank they are sent back to
  next: 'P1'         # the prestige group they gain
  requirements:
    - 'money 10000'

P1example:
  from: 'D'
  to: 'A'
  rank: 'P1'         # the prestige group they already hold
  next: 'P2'
  requirements:
    - 'money 20000'
```

The first entry has no `rank:`, which covers the player who has never prestiged. Prestiges accept
`commands:`, `celebration:` and a `prestige:` message-override block. They ignore `display-name`
and `cost-multiplier`. Here `requirements` must be a list, and the per-prestige map form is
silently ignored.

## Rebirth

An optional tier above prestige, off by default. Configured entirely in `config.yml`:

```yaml
rebirth:
  enabled: false
  requires: top-rank       # or top-prestige
  groups:                  # ordered; one is granted per rebirth
    - rebirth1
    - rebirth2
    - rebirth3
  reset-ranks: true        # send them back to the first rank
  reset-prestige: false    # also strip every prestige group
  requirements:
    - 'money 1000000'
  commands: []             # {player} and {rebirth} are replaced
  messages:
    success: '&aYou have been reborn as &e{rebirth}&a!'
    requirements-not-met: '&cYou do not meet the requirements to be reborn.'
    not-at-top: '&cYou must reach the top of your progression before you can be reborn.'
    maxed: '&cYou have reached the maximum rebirth.'
```

Enabling it with an empty `groups` list logs a warning and disables rebirth. Note that rebirth
messages and commands use `{player}` and `{rebirth}`, not the `{{...}}` template engine that rank
commands use.

## Multiple ladders

`rankups.yml` is the ladder called `default`. Add more by dropping one file per ladder into a
`ladders/` folder:

```
plugins/ArkonasRanks/
├── rankups.yml          # ladder id: "default"
└── ladders/
    ├── mining.yml       # ladder id: "mining"
    └── combat.yml       # ladder id: "combat"
```

The file name is the ladder id, and each file uses the same format as `rankups.yml`. Players
progress through each ladder independently, since a rank is just a permission group and each ladder
only looks at its own.

Use `/rankup mining` and `/maxrankup mining`. Plain `/rankup`, the menus and prestige all act on the
default ladder. With more than one ladder loaded, `/ranks` opens a picker so the other tracks can be
browsed too, and auto-rankup advances every ladder. A ladder that fails to load is logged and
skipped without stopping the server. The id `default` is reserved.

## Cost formulas

Rather than hand-writing a price on forty ranks, generate them:

```yaml
cost-formula:
  enabled: false
  requirement: money              # which requirement to generate
  expression: "1000 * 1.15 ^ {index}"
  round: 2                        # decimal places; -1 to disable rounding
  override: false                 # true = replace an explicit value of the same name
```

Variables: `{index}` (the 0-based position in the ladder), `{n}` (the rank count) and `{prev}` (the
previous rank's generated cost, `0` for the first). Operators are `+ - * / % ^`, and the functions
are `min`, `max`, `pow`, `floor`, `ceil`, `round`, `abs` and `sqrt`.

With `override: false` a rank that already declares the requirement keeps its explicit value, but
that value still feeds `{prev}`, so a hand-tuned rank does not break the curve after it. Ranks using
the per-prestige map form are skipped. Each ladder can carry its own formula under
`ladders.<id>.cost-formula`.

---

## Requirements

A requirement is one string: a name, a space, then a value.

```yaml
requirements:
  - 'money 5000'
  - 'mcmmo mining 250'
  - 'region rankup_zone spawn'
```

Names are case-insensitive. Some take a sub-value (`<name> <sub> <amount>`), and some take a
space-separated list, where any one entry matching is enough.

Requirements that cost something come in two flavours. The plain name is taken away on rankup, so
`money 500` charges 500. The same name with an `h` suffix is only checked, so `moneyh 500` means
have 500 and keep it. This applies to `money`, `xp-level`, `item`, `tokenmanager-tokens`,
`votingplugin-points` and `playerpoints`.

### Every requirement

| Requirement | Syntax | Needs | Deducts |
| --- | --- | --- | --- |
| `money` / `moneyh` | `money 1500` | an economy | yes / no |
| `xp-level` / `xp-levelh` | `xp-level 30` | - | yes / no |
| `item` / `itemh` | `item diamond 5`, `item wool:2 16` | - | yes / no |
| `playtime-minutes` | `playtime-minutes 600` | - | no |
| `advancement` | `advancement story/mine_stone` | - | no |
| `permission` | `permission essentials.fly vip.perk` | - | no |
| `group` | `group vip donor` | - | no |
| `world` | `world world_mine survival` | - | no |
| `block-break` | `block-break stone 5000` | - | no |
| `craft-item` | `craft-item bread 64` | - | no |
| `use-item` | `use-item diamond_pickaxe 100` | - | no |
| `mob-kills` | `mob-kills zombie 100` | - | no |
| `total-mob-kills` | `total-mob-kills 1000` | - | no |
| `player-kills` | `player-kills 25` | - | no |
| `placeholder` | `placeholder %some_papi% >= 100` | PlaceholderAPI | no |
| `mcmmo` | `mcmmo mining 50` | mcMMO | no |
| `mcmmo-power-level` | `mcmmo-power-level 500` | mcMMO | no |
| `region` | `region mine_a mine_b` | WorldGuard | no |
| `quest` | `quest The Long Road` | Quests (PikaMug) | no |
| `betonquest-tag` | `betonquest-tag default.tutorial_done` | BetonQuest | no |
| `playerpoints` / `playerpointsh` | `playerpoints 500` | PlayerPoints | yes / no |
| `tokenmanager-tokens` / `…h` | `tokenmanager-tokens 250` | TokenManager | yes / no |
| `votingplugin-points` / `…h` | `votingplugin-points 100` | VotingPlugin | yes / no |
| `votingplugin-votes` | `votingplugin-votes 50` | VotingPlugin | no |
| `superbvote-votes` | `superbvote-votes 50` | SuperbVote | no |
| `advancedachievements-achievement` | `advancedachievements-achievement Mine_1000` | AdvancedAchievements | no |
| `advancedachievements-total` | `advancedachievements-total 20` | AdvancedAchievements | no |
| `towny-resident` | `towny-resident true` | Towny | no |
| `towny-mayor` | `towny-mayor true` | Towny | no |
| `towny-king` | `towny-king true` | Towny | no |
| `towny-mayor-residents` | `towny-mayor-residents 10` | Towny | no |
| `towny-king-residents` | `towny-king-residents 50` | Towny | no |
| `towny-king-towns` | `towny-king-towns 5` | Towny | no |

A few details:

- `item` takes an optional durability. `item wool:2 16` matches only items with durability 2. Only
  the main inventory counts, so armour and offhand do not.
- `advancement` is matched as a pattern. `*` is a wildcard, and a leading `-` inverts it, so the
  requirement passes when the advancement has *not* been completed.
- The Towny booleans compare against a value, so `towny-mayor false` means the player must not be a
  mayor.
- `placeholder` supports `=` and `!=` on strings, and `>` `>=` `<` `<=` `==` on numbers. Only `>=`
  draws a real progress bar. The other operators render as pass or fail.
- `quest` does not split on spaces, because quest names contain them. It matches the id or the
  display name.

A requirement whose plugin is not installed is never registered, and a rank that uses it fails to
load with `Unknown requirement: <name>`. It does not quietly pass.

Once the server is running, requirements fail closed. If a hook plugin throws, or is only half
loaded, the requirement reads as unmet and logs once, instead of breaking `/rankup`, the menus or a
placeholder.

---

## `config.yml` reference

Every option, with its shipped default.

### Core

```yaml
version: 11               # do not edit; the plugin keeps this current
locale: en                # en, tr, pt_br, ru, zh_cn, fr, it, es, nl
message-format: auto      # auto | legacy | minimessage

ranks: true               # register /ranks
prestiges: true           # register /prestiges (also needs prestige: true)
prestige: false           # register /prestige, /prestiges and /maxprestige; load prestiges.yml
ranks-gui: false          # /ranks opens the old chest GUI instead of writing to chat
notify-update: true       # tell rankup.notify players about updates on join
```

`message-format: auto` accepts legacy `&6` codes, `&#RRGGBB` hex and MiniMessage tags mixed in a
single message. `legacy` allows only the first two, and `minimessage` only the last.

### Confirmation and cooldown

```yaml
confirmation-type: 'gui'  # gui | text | none (ignored while the menus are on)
enable-noconfirm: true    # allow /rankup noconfirm
cooldown: 1               # seconds between successful rankups; 0 disables it
text:
  timeout: 10             # seconds to re-type /rankup, for confirmation-type: text

max-rankup:
  enabled: false            # register /maxrankup (needs a restart)
  individual-messages: true # announce every rank passed, or only the last
```

The cooldown is a `/rankup` spam guard. Auto-rankup bypasses it entirely.

### Auto-rankup

```yaml
autorankup-interval: 0    # minutes between sweeps; 0 = off. 0.5 = every 30 seconds

auto:
  rankup: true            # rank players up (advances every ladder)
  prestige: true          # prestige them if no ladder advanced
  max: false              # rank up repeatedly in one sweep, not just once
  rebirth: false          # be reborn if nothing else advanced
```

Only players with `rankup.auto` are swept.

### Permissions and groups

```yaml
permission-rankup: false        # read ranks from rankup.rank.<RANK> instead of groups
luckperms-context: ''           # e.g. 'server=survival world=world_nether'
use-luckperms-group-names: false
```

With `permission-rankup: true` the plugin stops moving players between groups. You have to do that
yourself in each rank's `commands`. The other two options are covered under
[the LuckPerms gotcha](#the-luckperms-inheritance-gotcha).

### Economy

```yaml
economy:
  provider: auto    # auto | vault | playerpoints | coinsengine:<currency> | gemseconomy
```

`auto` prefers Vault and then tries the rest. CoinsEngine can never be auto-detected, so name its
currency explicitly: `coinsengine:gold`. An unknown or unavailable id logs a warning and falls back
to auto-detect.

### Database and leaderboards

```yaml
database:
  enabled: true
  type: sqlite            # sqlite | mysql | mariadb
  host: localhost
  port: 3306
  database: arkonasranks
  username: root
  password: ''
  pool-size: 4            # MySQL only; SQLite is forced to 1
```

SQLite writes to `plugins/ArkonasRanks/data.db`, and all writes are async. This block also powers
[milestones](#milestones), so turning the database off turns those off with it. If the database
fails to start the plugin still runs, and statistics switch off.

### Placeholder formatting

```yaml
placeholders:
  money-format: "#,##0.##"
  percent-format: "0.##"
  simple-format: "#.##"
  not-in-ladder: "None"
  no-prestige: "None"
  highest-rank: "None"
  leaderboard-empty-name: ''
  leaderboard-empty-count: '0'
  status:
    complete: "Complete"
    current: "Current"
    incomplete: "Incomplete"
  last-rank-display-name: "last rank"

shorten: ['K', 'M', 'B', 'T', 'Q', 'Qu', 'S']   # 1000, 1000², 1000³ …
```

The sections not shown here (`menus`, `cost-formula`, `ladders`, `rebirth`, `multipliers`,
`boosters`, `progress-display`, `milestones`, `discord` and `citizens`) each get their own section
below.

### What needs a restart

| Needs a restart | Picked up by `/aru reload` |
| --- | --- |
| `prestige`, `ranks`, `ranks-gui` | `rankups.yml`, `prestiges.yml`, the ladders |
| `max-rankup.enabled`, `rebirth.enabled` | `menus.yml`, `effects.yml` |
| `menus.enabled` | `progress-display`, `boosters.schedule`, `multipliers` |

---

## Menus

With `menus.enabled: true`, the commands `/rankup`, `/ranks`, `/prestige`, `/prestiges` and
`/rankup top` open animated inventory screens instead of writing to chat. The menu becomes the
confirmation screen, so `ranks-gui` and `confirmation-type` are ignored while it is on.

Confirming in a menu routes back through the same rankup code as `/rankup`, so requirements and
cooldowns are re-checked and the menu is not a way around them.

- Rankup and Prestige: one icon per requirement, each with a progress bar, a cost and a ✔ or ✖. A
  pulsing Confirm button appears once everything is met, or a live countdown while on cooldown.
- Rank path: the ladder as a paginated map. Completed ranks are green, your own head sits on the
  current one (click it to open the rankup screen), and locked ranks are red with their costs.
- Ladder picker: only appears when you run more than one ladder.
- Prestige list: the same map, over the prestige ladder.
- Leaderboard: a three-head podium plus positions 4 to 10, and a toggle between rankups and
  prestiges. Fetched off the main thread.
- Hub: reached with the `☰ Menu` button in the nav bar of every screen.

The console, `/rankup noconfirm`, `/rankup <ladder>` and players who are in no ladder all fall
through to the old chat behaviour.

> `menus.enabled` counts as `false` when the key is absent. A config written before the menus
> existed keeps the old behaviour, and when the plugin adds the key on upgrade it writes it as
> `false`, so a jar update does not move your server onto a different UI. Switch it on when you want
> it.

### `menus.yml`

Layout, theme and icons live here. The wording lives in the locale files, under `menu:`.

```yaml
animation:
  enabled: true
  period: 2                 # frame period in ticks; 2 is roughly 10fps
  border-chase: true        # a light chasing around the border
  progress-fill: true       # bars fill in from empty
  confirm-pulse: true       # the confirm button pulses
  cooldown-countdown: true  # live per-second countdown
  open-reveal: false        # opt-in wipe-in when a menu opens
  open-reveal-speed: 6

theme:
  primary: '#00E5FF'        # title gradient start
  secondary: '#7C4DFF'      # title gradient end
  success: '#3DDC84'        # met requirements
  danger: '#FF5370'         # unmet requirements, Close
  muted: '#8A8F98'          # Back, page counter
  border:
    base: BLACK_STAINED_GLASS_PANE
    light: [CYAN_STAINED_GLASS_PANE, LIGHT_BLUE_STAINED_GLASS_PANE, PURPLE_STAINED_GLASS_PANE]
    lights: 3
  progress:
    filled-char: '▰'
    empty-char: '▱'
    length: 10
    style: classic          # classic | smooth | gradient
    gradient:
      from: '#3DDC84'
      to: '#00E5FF'
      empty: '#8A8F98'
  sounds:
    open: block.note_block.pling
    click: ui.button.click
    page: item.book.page_turn
    deny: block.note_block.bass

menus:
  hub: { rows: 5, head-slot: 13, path-slot: 20, rankup-slot: 22, prestige-slot: 24, leaderboard-slot: 40 }
  path: { rows: 6 }
  rankup: { rows: 5, info-material: WRITABLE_BOOK }
  prestige: { rows: 5, info-material: WRITABLE_BOOK }
  prestige-list: { rows: 6 }
  leaderboard: { rows: 6 }
```

One shared ticker animates every open menu. It starts when the first menu opens and cancels itself
when the last one closes, so an idle server does no work for it.

The `smooth` bar style uses eighth-block glyphs, so the leading cell advances a fraction at a time.
It ignores `filled-char` and `empty-char`. The `gradient` style emits MiniMessage and falls back to
`classic` under `message-format: legacy`, so raw tags are never shown.

Any sound can also be written as a section with its own `volume` and `pitch`.

### Requirement icons

```yaml
requirement-icons:
  default: PAPER
  money: GOLD_INGOT
  xp-level: EXPERIENCE_BOTTLE
  playtime-minutes: CLOCK
  block-break: DIAMOND_PICKAXE

  # a custom item from another plugin
  mcmmo: oraxen:mcmmo_icon
  region: itemsadder:mypack:region_beacon
  votingplugin-votes: hdb:1234

  # the long form, for resource-pack models, heads and a forced glint
  playerpoints:
    material: nexo:points_gem
    custom-model-data: 1001
    head-texture: <player-name-or-base64>
    glow: true
```

An icon is resolved by exact name, then by the name with a trailing `h` stripped (so `moneyh` shares
`money`'s icon), then by `default`.

Namespaced ids come from Oraxen (`oraxen:id`), Nexo (`nexo:id`), ItemsAdder
(`itemsadder:pack:name`) or HeadDatabase (`hdb:id`), whichever of them is actually running. None is
a hard dependency. If the plugin is missing, the icon falls back to a plain item. Namespaced ids are
honoured in `requirement-icons` only.

---

## Celebration effects (`effects.yml`)

A rankup can fire a sound, an ascending note jingle, particles, a firework, a title, an action-bar
line and a temporary boss bar. You configure it once, and it fires for every path a player can
advance through: `/rankup`, `/maxrankup`, auto-rankup, forced rankups, the menus, prestige and
rebirth.

```yaml
enabled: true

rankup:
  sound:
    name: entity.player.levelup
    volume: 1.0
    pitch: 1.0
  jingle:                     # opt-in ascending note-block run
    enabled: false
    sound: block.note_block.pling
    notes: 5
    start-pitch: 0.8
    pitch-step: 0.15          # pitch is clamped to the note-block range, 0.5-2.0
    interval-ticks: 3
  particle:
    type: TOTEM_OF_UNDYING
    count: 40
    offset: 0.5
  firework:
    enabled: false
    power: 1                  # clamped to 0-2
    colors: ['00FF88', '00CCFF']
    type: BALL_LARGE          # BALL | BALL_LARGE | STAR | BURST | CREEPER
  title:
    title: '&6&lRANKUP!'
    subtitle: '&e%rank% &7» &a%next%'
    fade-in: 10
    stay: 60
    fade-out: 20
  actionbar: ''
  bossbar:
    enabled: false
    text: '&a%player% is now %next%!'
    color: GREEN
    overlay: PROGRESS
    seconds: 5
  broadcast-mode: none        # none | world | server

prestige:
  # …the same keys…

rebirth:
  # …the same keys…
```

All three sections take exactly the same keys. The shipped defaults just differ in taste.
Placeholders are `%player%`, `%rank%` (the old rank) and `%next%` (the new one).

`broadcast-mode` decides who else sees the title and the boss bar: nobody (`none`), everyone in the
same `world`, or the whole `server`. The action bar and the sound always go to the ranking player
alone. Particles and fireworks are world objects, so bystanders see them either way.

Celebration fireworks cannot hurt anyone. Each one is tagged as it spawns, and any damage it would
deal is cancelled. Fireworks from players or other plugins are untouched.

### Per-rank celebration override

Any rank in `rankups.yml`, and any prestige in `prestiges.yml`, can carry its own `celebration:`
block taking the same keys. It works in TOML ladders too.

```yaml
Cexample:
  rank: 'C'
  next: 'D'
  requirements:
    - 'money 5000'
  celebration:
    firework:
      enabled: true
      power: 2
      colors: ['FF0000', 'FFAA00']
      type: BURST
    title:
      title: '&c&lD RANK!'
      subtitle: '&7%player% broke into &c%next%'
    broadcast-mode: server
```

> The override replaces the whole section. It does not merge with `effects.yml` key by key.
> Anything you leave out falls back to the code default, not to your global value. The rank above
> plays no sound and no particles, because the override never mentions them. If you want them,
> repeat them.

Rebirth always uses the global `rebirth:` section. There is no per-tier override for it.

---

## Live progress display

An always-on readout of how close a player is to their next rank, on up to four surfaces at once.
Off by default.

```yaml
progress-display:
  enabled: false
  interval-ticks: 20          # refresh period; 20 = once a second
  expbar: false               # mirror progress onto the vanilla XP bar
  bossbar:
    enabled: false
    text: '&aNext: %next% &7- &e%percent%%'
    color: GREEN              # PINK BLUE RED GREEN YELLOW PURPLE WHITE
    overlay: PROGRESS         # PROGRESS NOTCHED_6 NOTCHED_10 NOTCHED_12 NOTCHED_20
  actionbar:
    enabled: false
    text: '&7Progress to %next%: &e%percent%%'
  scoreboard:
    enabled: false
    title: '&6&lRankUp'
    lines:
      - '&fRank: &e%rank%'
      - '&fNext: &e%next%'
      - '&a%bar% &7%percent%%'
```

Placeholders are `%percent%`, `%rank%`, `%next%` and `%player%` everywhere, plus `%bar%` (a
ten-character `■`/`□` bar) in the scoreboard lines only.

Progress is the average completion across the current rank's requirements. A player at the top of
the ladder, or in no ladder at all, reads as full.

The exp-bar mirror overwrites the vanilla XP bar's fill. The level number is left alone, so the
`xp-level` requirement still works. The player's real fill is saved the first time it is overridden,
and restored when they quit, when the plugin stops, or on `/aru reload`, so the mirrored value never
ends up baked into their save. It does mean the XP bar is unusable for anything else while the
mirror is on.

`/aru reload` rebuilds the whole display, so toggling it does not need a restart.

---

## Discounts, boosters and sales

Three factors multiply into one price: a global multiplier, a permission-based one, and a temporary
event.

```yaml
multipliers:
  global: 1.0                 # below 1 discounts everything, above 1 surcharges it
  permissions:
    rankup.multiplier.vip: 0.9
    rankup.multiplier.mvp: 0.8
```

Of the nodes a player holds, the lowest factor wins, so someone with both VIP and MVP pays the MVP
price. A rank's own `cost-multiplier` folds in on top.

Run a sale by hand:

```
/aru booster 0.5 30m       # half-price rankups for half an hour
/aru booster status
/aru booster clear
```

Or schedule them:

```yaml
boosters:
  schedule:
    - days: [SATURDAY, SUNDAY]     # or [ALL]
      start: '18:00'               # server-local
      end: '20:00'
      factor: 0.5
```

Overlapping windows use the best factor, and a manual `/aru booster` always overrides the schedule.
A malformed window is skipped without breaking the rest.

Every currency-style requirement respects all of this: `money`, `xp-level`, `item`,
`tokenmanager-tokens`, `votingplugin-points`, `playerpoints` and their `h` variants. The discount is
shown in the menus and applied when the player is charged.

## Milestones

Reward players on their cumulative rankup and prestige counts. Needs `database.enabled`.

```yaml
milestones:
  enabled: false
  rankup:
    every: 25                 # every 25th rankup; 0 = off
    every-commands:
      - 'broadcast &e%player% &7reached &a%count% &7rankups!'
    at:
      100:                    # a specific count; takes precedence over 'every'
        - 'give %player% diamond 16'
        - 'broadcast &6%player% &7hit &b100 rankups&7!'
  prestige:
    every: 0
    every-commands: []
    at: {}
```

Commands run from the console. The placeholders are `%player%`, `%count%` and `%type%`.

## Statistics and leaderboards

Every rankup and prestige is recorded. SQLite by default, MySQL or MariaDB optional, always written
off the main thread.

- `/rankup top [prestiges]`
- `%rankup_top_<n>_name%` and `%rankup_top_<n>_count%`, plus the `prestige_top` equivalents
- `%rankup_player_rankups%` and `%rankup_player_prestiges%`

The leaderboard placeholders read from an async cache, so they are safe to put in holograms, TAB or
a scoreboard. That cache holds the top 10, so `top_11` and beyond always resolve to the empty
fallback. For holograms the usual chain is ArkonasRanks, then PlaceholderAPI, then ajLeaderboards,
then DecentHolograms. There is no built-in hologram engine.

Set `database.enabled: false` and the plugin runs completely stateless.

---

## Integrations

ArkonasRanks has one hard dependency, Vault. Everything else is optional and fails soft: when a
plugin is missing, its feature is not available, and nothing throws.

| Plugin | What you get | How to turn it on |
| --- | --- | --- |
| Vault (required) | Rank groups, and the default `money` economy | Automatic |
| LuckPerms | Inheritance-aware group checks, context-scoped ranks | `use-luckperms-group-names: true`, and [read this](#the-luckperms-inheritance-gotcha) |
| PlaceholderAPI | The `%rankup_*%` expansion, and the `placeholder` requirement | Just install it |
| PlayerPoints | A `money` backend, and points requirements | `economy.provider: playerpoints` |
| CoinsEngine | A `money` backend on a named currency | `economy.provider: coinsengine:gold` |
| GemsEconomy | A `money` backend | `economy.provider: gemseconomy` |
| TokenManager | Token requirements (deductible) | Just install it |
| mcMMO | Skill and power-level requirements | Just install it |
| Towny | Six town and nation requirements | Just install it |
| WorldGuard | `region`, so a player must stand somewhere to rank up | Just install it |
| Quests (PikaMug) | The `quest` requirement | Just install it |
| BetonQuest | The `betonquest-tag` requirement | Just install it |
| VotingPlugin | Vote and vote-point requirements | Just install it |
| SuperbVote | The `superbvote-votes` requirement | Just install it |
| AdvancedAchievements | Achievement requirements | Just install it |
| DiscordSRV | Announce rankups and prestiges to Discord | `discord.enabled: true` |
| Citizens | Right-click an NPC to rank up | `citizens.enabled: true` |
| Oraxen, Nexo, ItemsAdder, HeadDatabase | Custom items as menu icons | Use a namespaced icon id |

### The LuckPerms inheritance gotcha

By default, ArkonasRanks only sees the groups a player is a *direct* member of. It asks Vault, and
Vault's group list does not expand inheritance.

So if you set your ranks up by making the LuckPerms `default` group inherit rank `a`, which is a
natural thing to do, then nobody is directly in `a`. The plugin reads every player as being in no
ladder at all, and `/rankup` refuses to work even with money in the bank. This is a configuration
problem rather than a bug in the rankup code.

There are two ways out. Either give each player their rank group directly:

```
lp user Notch parent add a
```

Or switch on the inheritance-aware provider, which walks the whole inheritance tree:

```yaml
use-luckperms-group-names: true
```

The second is what you want on a LuckPerms server. It needs a restart.

### Discord

```yaml
discord:
  enabled: false
  channel: global               # a DiscordSRV *game channel name*, not a channel id
  rankup-message: '**%player%** ranked up: %from% -> %to%'
  prestige-message: '**%player%** prestiged: %from% -> %to%'
```

Announces every path: manual, menu, `/maxrankup`, auto and forced. Leave a message blank to skip
that event type. A failed send never disturbs the rankup itself.

### Citizens

```yaml
citizens:
  enabled: false
  default-commands: []          # for NPCs not listed below; empty means ignore them
  npcs:
    0: maxrankup
    1:
      - 'rankup'
      - '[console] broadcast &e%player% used the rankup NPC!'
```

The keys are Citizens NPC ids. Each line runs as the clicking player unless you prefix it with
`[console]`. Clicks that another plugin has already cancelled (by a protection region, for example)
are ignored.

---

## PlaceholderAPI

Expansion identifier: `rankup`.

### Ranks

| Placeholder | Returns |
| --- | --- |
| `%rankup_current_rank%` | Their rank group, or `None` when they are in no ladder |
| `%rankup_current_rank_name%` | Its `display-name` |
| `%rankup_next_rank%` | The next group, or `None` at the top |
| `%rankup_next_rank_name%` | Its `display-name` |
| `%rankup_status_<rank>%` | `Complete`, `Current` or `Incomplete` for that rank |

### Money and progress

| Placeholder | Returns |
| --- | --- |
| `%rankup_money%`, `%rankup_money_formatted%` | The cost of the next rankup |
| `%rankup_money_left%`, `%rankup_money_left_formatted%` | Cost minus balance, never below zero |
| `%rankup_percent_left%`, `%rankup_percent_done%` | …and the `_formatted` variant of each |

> These show the list price, not the discounted one. Multipliers, per-rank factors and boosters are
> applied when the player is charged, and in the menus, but this family reads the raw configured
> value. If you run VIP discounts, use `%rankup_requirement_money_left%` instead. That one is
> multiplier-aware.

### Any requirement

`%rankup_requirement_<name>%`, plus the suffixes `_left`, `_done`, `_percent_left` and
`_percent_done`. Sub-requirements are written `name#sub`, as in
`%rankup_requirement_mcmmo#mining_left%`. `%rankup_rank_requirement_<rank>_<name>_…%` does the same
for a rank you name explicitly.

### Prestige, rebirth and stats

| Placeholder | Returns |
| --- | --- |
| `%rankup_current_prestige%`, `%rankup_next_prestige%` | …and the `_name` variant of each |
| `%rankup_prestige_money%`, `%rankup_prestige_percent_done_formatted%` | Prestige cost and progress |
| `%rankup_current_rebirth%`, `%rankup_next_rebirth%`, `%rankup_rebirth_count%` | Rebirth |
| `%rankup_top_<n>_name%`, `%rankup_top_<n>_count%` | The rankup leaderboard |
| `%rankup_prestige_top_<n>_name%`, `%rankup_prestige_top_<n>_count%` | The prestige leaderboard |
| `%rankup_player_rankups%`, `%rankup_player_prestiges%` | This player's own totals |

---

## Developer API

The events live in `com.arkonas.ranks.events`. Add ArkonasRanks to your `softdepend` and register a
normal listener.

### Custom requirements

`RankupRegisterEvent` fires on enable and on every reload, right before the ladders are parsed. That
makes it the only moment a new requirement can be added in time to be recognised.

```java
public class FishCaughtRequirement extends ProgressiveRequirement {

  public FishCaughtRequirement(ArkonasRanksPlugin plugin) {
    super(plugin, "fish-caught");        // written in rankups.yml as: fish-caught 100
  }

  protected FishCaughtRequirement(FishCaughtRequirement clone) {
    super(clone);
  }

  @Override
  public double getProgress(Player player) {
    return player.getStatistic(Statistic.FISH_CAUGHT);
  }

  @Override
  public Requirement clone() {           // the plugin clones a prototype per rank
    return new FishCaughtRequirement(this);
  }
}

@EventHandler
public void onRegister(RankupRegisterEvent event) {
  event.addRequirement(new FishCaughtRequirement(event.getPlugin()));
}
```

Extend `Requirement` for a plain yes-or-no check, or `ProgressiveRequirement` when there is a number
to count towards. The progressive base class gives you the progress bar, the percentage placeholders
and the fail-closed error handling. Implement `DeductibleRequirement` if the requirement should be
charged on rankup, and override `getTotal` using the protected `costFactor(player)` if it should
respect the sale multipliers.

### Rankup and prestige events

```java
@EventHandler
public void onRankup(PlayerRankupEvent event) {
  String from = event.getRank().getRank().getRank();
  String to = event.getRank().getNext().getRank().getRank();
}
```

`PlayerRankupEvent` and `PlayerPrestigeEvent` fire after the group change and after the rank's
commands have run, so a listener can react to a rankup but cannot veto one. Rebirth fires no event.

---

## Upgrading

Drop the new jar in and start the server. On load, the plugin merges any option it has added into
your existing `config.yml`, `effects.yml` and `menus.yml`, comments and all, without touching a
value you have set and without deleting keys it does not recognise. It writes a one-off `.backup` of
each file the first time it does so, and it reports what it added:

```
[ArkonasRanks] Updated config.yml: added 73 missing option(s) across [economy, auto, cost-formula,
rebirth, multipliers, boosters, progress-display, milestones, discord, citizens].
Your existing values were kept; the previous file is saved as config.yml.backup
```

New options arrive switched off, so an update does not change how your server already behaves. The
one exception would have been `menus.enabled`, which ships as `true` on a fresh install: when it is
added to an existing config it is written as `false` instead, and the plugin says so in the log.
Switch it on when you are ready for it.

Rankup3 configs drop in as they are. These quirks are preserved on purpose:

- `rankups.toml` silently wins over `rankups.yml`.
- The first prestige entry has no `rank:` and grants two groups.
- Changing `ranks`, `prestige`, `ranks-gui`, `max-rankup.enabled`, `rebirth.enabled` or
  `menus.enabled` needs a restart rather than a reload.

## Building

```
./gradlew build     # shaded jar in build/libs/
./gradlew test      # JUnit 5 + MockBukkit
```

Java 21 is required to build.
