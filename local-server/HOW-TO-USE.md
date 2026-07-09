# ArkonasRanks — local test server & usage guide

## 1. Dependencies

| Plugin | Role | Required? |
|---|---|---|
| **Paper 1.21.x** | Server (api-version 1.21, Java 21) | yes |
| **Vault** | Abstraction the plugin hard-depends on | **yes** (hard depend) |
| **LuckPerms** | Permission groups — *a rank IS a permission group* | **yes** in practice (Vault needs a group provider) |
| **EssentialsX** | Vault economy, for the default `money` requirements | needed only if you use money costs |
| WorldGuard, Citizens, DiscordSRV, PlayerPoints, TokenManager, VotingPlugin, mcMMO, Towny, Quests, BetonQuest, GemsEconomy, CoinsEngine, Oraxen/ItemsAdder, DecentHolograms, TAB | Optional feature hooks | no (soft-depend, auto-detected) |

Every optional hook is a **soft-depend**: absent → silently skipped, present → auto-enabled.

## 2. Quick start

```bash
cd local-server
bash setup.sh     # downloads Paper + Vault + LuckPerms + EssentialsX, builds & drops ArkonasRanks
bash run.sh       # starts the server on localhost:25565 (Ctrl-C / `stop` to quit)
```

`setup.sh` writes `eula=true` (you accept the Minecraft EULA by running it) and sets
`online-mode=false` for easy offline testing — **do not expose this server publicly.**

Then connect a **Minecraft Java 1.21.4** client to `localhost`.

## 3. First-time setup (in the server console or in-game as OP)

The default `rankups.yml` ladder is **A → B → C → D** with money costs. Wire it up:

```
op <yourname>                         # from the server console
/lp creategroup a
/lp creategroup b
/lp creategroup c
/lp creategroup d
/lp user <yourname> parent add a      # put yourself in the first rank
/eco set <yourname> 100000            # give money (EssentialsX)
```

Now in-game:

```
/ranks       # see the ladder
/rankup      # A → B (costs 1000), then again for B → C, C → D …
```

`/lp user <you> parent add a` is what makes you "rank A"; ArkonasRanks then moves you
between groups automatically on rankup (via Vault/LuckPerms).

## 4. Commands

| Command | Aliases | Permission | Purpose |
|---|---|---|---|
| `/rankup` | | `rankup.rankup` | Rank up if you meet the next rank's requirements |
| `/ranks` | | `rankup.ranks` | List the ladder |
| `/maxrankup` | | `rankup.maxrankup` | Rank up as many times as you can afford |
| `/prestige` `/prestiges` | | `rankup.prestige(s)` | Prestige (enable `prestige: true` + `prestiges.yml`) |
| `/rebirth` `/rebirths` | | `rankup.rebirth` | Rebirth tier (enable `rebirth.enabled`) |
| `/arkonasranks …` | `aru`, `rankup3`, `pru` | see below | Admin hub |

`/aru` subcommands: `reload` (`rankup.reload`) · `forcerankup <p>` / `forceprestige <p>` /
`rankdown <p>` (`rankup.force`) · `booster <factor> <dur>|clear|status` / `tree` / `placeholders`
(`rankup.admin`) · `playtime …` (`rankup.playtime`).

OP has all of these by default.

## 5. What's new (this build) & how to try each

- **Manual sale:** `/aru booster 0.5 30m` → half-price rankups for 30 min; `/aru booster status`.
- **Recurring sales:** `config.yml → boosters.schedule` (weekend windows, etc.).
- **Per-rank cost:** add `cost-multiplier: 0.8` to any rank in `rankups.yml`.
- **All-currency discounts:** the multiplier now also scales XP / item / token / vote-point /
  PlayerPoints costs, not just money.
- **Live progress:** `config.yml → progress-display` — exp-bar / boss-bar / action-bar /
  **scoreboard sidebar** (`scoreboard.enabled: true`).
- **Milestones:** `config.yml → milestones` — reward commands on cumulative rankup/prestige counts
  (needs `database.enabled: true`).
- **New requirement types** (drop the optional plugin, then use in `rankups.yml → requirements`):
  `region <id>` (WorldGuard) · `quest <name>` (Quests) · `betonquest-tag <tag>` (BetonQuest) ·
  `playerpoints <n>` (PlayerPoints) — alongside the built-ins `money`, `xp-level`, `item`,
  `permission`, `world`, `placeholder`, `group`, `mob-kills`, …
- **Economy backend:** `config.yml → economy.provider` = `auto` (default, prefers Vault) /
  `vault` / `playerpoints` / `coinsengine:<currency>` / `gemseconomy`.
- **Discord announcements:** `config.yml → discord` (needs DiscordSRV).
- **Citizens NPC rankup:** `config.yml → citizens.npcs.<npc-id>: [ rankup ]` (needs Citizens).
- **Rebirth:** `config.yml → rebirth` (`reset-prestige`, groups, requirements); `auto.rebirth: true`
  for the auto task.
- **Multi-ladder:** drop extra `ladders/<id>.yml` files; the GUI shows a ladder picker.
- **GUI:** the advanced menu module + effects live in `menus.yml` / `effects.yml`.

## 6. Config files (in `plugins/ArkonasRanks/`)

`config.yml` (global) · `rankups.yml` (the ladder) · `prestiges.yml` · `effects.yml` (celebrations,
incl. the new `rebirth:` stinger) · `menus.yml` (GUI) · `ladders/` (extra ladders) · your locale file.
Edit, then `/aru reload` (scheduled features like the scoreboard/booster-scheduler need a full restart).

## 7. Verifying it loaded

On startup the console should show `[ArkonasRanks] Enabling ArkonasRanks…` with **no stack traces**,
and `/plugins` should list ArkonasRanks in green. `/aru` prints the version. If a `money` requirement
says no economy is found, EssentialsX/Vault didn't register — check they enabled before ArkonasRanks
(Vault is a hard depend, so load order is handled).

## 8. Notes

- This build is committed locally on branch `feature/next-gen-progression` (no GitHub remote).
- The optional reflection hooks (WorldGuard/Quests/BetonQuest/PlayerPoints/GemsEconomy/Citizens/
  DiscordSRV) were unit-tested at the logic level but **need this kind of live server to validate the
  actual third-party API calls** — this is exactly what to smoke-test here.
