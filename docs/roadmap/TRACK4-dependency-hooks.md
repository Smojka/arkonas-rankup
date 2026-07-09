# Track 4 — Dependency hooks batch

Goal (the "vary-dependency" / flexibility aim): hook the plugins servers actually run, all as
**soft-depends with graceful fallback** — a missing plugin never breaks startup.

Current hooks: Vault (hard), LuckPerms, PlaceholderAPI, mcMMO, AdvancedAchievements, Towny,
SuperbVote, VotingPlugin, TokenManager.

## Shipped

### Namespaced item provider seam  (M1, shipped)
`NamespacedItems` registry + `NamespacedItemProvider` — menu icons resolve `ns:key` ids to items
from Oraxen/ItemsAdder/Nexo/HeadDatabase. Tested (registry + MenuIcon fallback). Concrete
per-plugin providers register into it (below).

### Economy / currency abstraction  (M2, shipped)
`EconomyRegistry` (tested selection + auto-detect + graceful fallback) + `ConfigurableEconomyProvider`
(now the default provider) selecting from `economy.provider` (default `auto`, prefers Vault).
Vault unchanged out of the box; **PlayerPoints** and **CoinsEngine** ship as reflection adapters
(no compile dep; degrade to Vault on any mismatch — validate on a live server). CMI/EssentialsX and
most economies already work through Vault. Still deferred: GemsEconomy, RedisEconomy, PlayerPoints
as a *currency requirement type* (vs the money backend).

### Discord announcements  (M3, shipped)
`DiscordAnnouncer` (tested render + dispatch + config parse) fires on the plugin's own
rankup/prestige events at MONITOR and delivers through a `DiscordSender` adapter. `DiscordSrvSender`
is the DiscordSRV reflection adapter (handles resolved up front; the JDA `sendMessage` overload is
found by shape so it survives JDA package moves; no-op when DiscordSRV is absent — validate live).
Config `discord.*` (channel + `%player%/%from%/%to%/%type%` templates), opt-in, softdepend.

### Leaderboard holograms via placeholders  (M4, shipped)
Rather than bind to one hologram plugin's API, the leaderboard-position placeholders
(`top_<n>_name/count`, `prestige_top_<n>_name/count`) are the substrate **DecentHolograms,
HolographicDisplays and TAB all read through PlaceholderAPI**. Resolution extracted to a pure,
unit-tested `LeaderboardPlaceholder` with configurable empty-slot fallbacks
(`placeholders.leaderboard-empty-name/-count`). A *native* auto-spawned/refreshed hologram (managed
by the plugin at a configured location) remains a live-server extra.

## Priority hooks still to add

### Permissions / groups  (M2)
- **CMI** groups (alongside the existing LuckPerms + generic Vault providers).

### Custom items / GUI textures  (M4)  — feeds Track 2 M1/M4
- **Oraxen / ItemsAdder / Nexo**: resolve namespaced item ids (`oraxen:rank_icon`) to ItemStacks
  for menu icons; **HeadDatabase** (`hdb:<id>`) for textured heads. An `ItemProvider` the
  `MenuIcon` builder consults when the material string is namespaced.

### NPC rankup  (M5, shipped)
**Citizens**: right-click an NPC to run configured commands as the clicker. Wired by reflection —
no Citizens compile dependency — by registering a handler for Citizens' own `NPCRightClickEvent`
through `PluginManager#registerEvent` (so player-type NPCs, which don't fire vanilla interact
events, are covered). Config `citizens.npcs.<id>` maps an NPC id to a command or list of commands
(NPC id -> command list is strictly more flexible than a fixed rankup/menu action); `[console]`
prefix runs from console, `%player%` is substituted. `NpcRankupSettings` + `NpcCommand` are pure
and unit-tested (11 methods: list/scalar/default/disabled parse, console/player routing, slash
strip); the reflection listener (`CitizensHook`) is the live-validate part. Opt-in, softdepend.

### Requirement sources  (M6, partly shipped)
- **WorldGuard** (`region <id>` / `region <id1> <id2>` = any-of): shipped. `WorldGuardRegionRequirement`
  registered when WorldGuard is enabled; the region query is the reflection `WorldGuardRegions`
  adapter (no compile dep, empty-set on failure so it fails closed), the membership test
  (`matches`) is pure + unit-tested (5). Softdepend WorldGuard.
- **Jobs Reborn** (`jobs-level <job> <n>`): not needed as a dedicated type — the existing
  `PlaceholderRequirement` already gates on `%jobs_...%` (and mcMMO/votes/playtime/stats), so any
  PlaceholderAPI-exposed value works out of the box.
- **Quests** (PikaMug) `quest <id or name>`: shipped. `QuestRequirement` registered when Quests is
  enabled; completion lookup is the reflection `QuestsCompletion` adapter (getQuester/
  getCompletedQuests resolved up front, per-element id getter discovered on first use since
  completed quests are Strings or Quest objects across versions; fails closed). Whole value = one
  quest id (names may contain spaces), matched case-insensitively; pure `matches` unit-tested (5).
  Softdepend Quests.
- Still open: **BetonQuest** (tag-based, a different model — `hasTag(tag)` rather than completed
  quests).

### Native holograms  (M7)  — extends M4
- **DecentHolograms / HolographicDisplays** API adapter to auto-create + refresh a managed
  leaderboard hologram at a configured location (the placeholder route above already powers
  user-authored holograms).

### Scoreboard / nametag  (M8)  — feeds Track 3 M2
- **TAB**, **FeatherBoard**: expose progress placeholders / drive sidebar lines.

## Design rules
- Every hook is `softdepend` in plugin.yml + a runtime `isPluginEnabled` guard; absent → skip.
- Each provider behind a small interface with a null-object default, mirroring the existing
  `GroupProvider` / `EconomyProvider` pattern, so the core never imports a hook directly.
- One provider per commit, each with a MockBukkit or pure test where feasible (the abstraction/
  resolution logic is testable even when the third-party plugin isn't on the test classpath).
