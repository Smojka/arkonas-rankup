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

## Priority hooks still to add

### Permissions / groups  (M2)
- **CMI** groups (alongside the existing LuckPerms + generic Vault providers).

### Holograms  (M3)
- **DecentHolograms** and **HolographicDisplays**: a leaderboard hologram (top rankups/prestiges)
  and an at-spawn progress hologram. `HologramProvider` interface, one impl per plugin.

### Custom items / GUI textures  (M4)  — feeds Track 2 M1/M4
- **Oraxen / ItemsAdder / Nexo**: resolve namespaced item ids (`oraxen:rank_icon`) to ItemStacks
  for menu icons; **HeadDatabase** (`hdb:<id>`) for textured heads. An `ItemProvider` the
  `MenuIcon` builder consults when the material string is namespaced.

### NPC rankup  (M5)
- **Citizens**: right-click an NPC to rank up / open the menu (trait or listener).

### Requirement sources  (M6)
- **Jobs Reborn** (`jobs-level <job> <n>`), **WorldGuard** (`region <id>`), **Quests**/**BetonQuest**.

### Announcements  (M7)
- **DiscordSRV**: broadcast rankups/prestiges/rebirths to a Discord channel.

### Scoreboard / nametag  (M8)  — feeds Track 3 M2
- **TAB**, **FeatherBoard**: expose progress placeholders / drive sidebar lines.

## Design rules
- Every hook is `softdepend` in plugin.yml + a runtime `isPluginEnabled` guard; absent → skip.
- Each provider behind a small interface with a null-object default, mirroring the existing
  `GroupProvider` / `EconomyProvider` pattern, so the core never imports a hook directly.
- One provider per commit, each with a MockBukkit or pure test where feasible (the abstraction/
  resolution logic is testable even when the third-party plugin isn't on the test classpath).
