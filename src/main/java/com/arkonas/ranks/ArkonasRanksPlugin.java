package com.arkonas.ranks;

import com.electronwill.nightconfig.toml.TomlFormat;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryView;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;
import com.arkonas.ranks.commands.InfoCommand;
import com.arkonas.ranks.commands.MaxRankupCommand;
import com.arkonas.ranks.commands.PrestigeCommand;
import com.arkonas.ranks.commands.PrestigesCommand;
import com.arkonas.ranks.commands.RanksCommand;
import com.arkonas.ranks.commands.RankupCommand;
import com.arkonas.ranks.economy.Economy;
import com.arkonas.ranks.economy.EconomyProvider;
import com.arkonas.ranks.economy.VaultEconomyProvider;
import com.arkonas.ranks.events.RankupRegisterEvent;
import com.arkonas.ranks.gui.Gui;
import com.arkonas.ranks.gui.GuiListener;
import com.arkonas.ranks.hook.GroupProvider;
import com.arkonas.ranks.hook.PermissionManager;
import com.arkonas.ranks.hook.VaultPermissionManager;
import com.arkonas.ranks.messages.Message;
import com.arkonas.ranks.messages.MessageBuilder;
import com.arkonas.ranks.messages.pebble.PebbleMessageBuilder;
import com.arkonas.ranks.placeholders.Placeholders;
import com.arkonas.ranks.prestige.Prestige;
import com.arkonas.ranks.prestige.Prestiges;
import com.arkonas.ranks.ranks.Rank;
import com.arkonas.ranks.ranks.RankList;
import com.arkonas.ranks.ranks.Rankups;
import com.arkonas.ranks.ranksgui.RanksGuiCommand;
import com.arkonas.ranks.ranksgui.RanksGuiListener;
import com.arkonas.ranks.requirements.Requirement;
import com.arkonas.ranks.requirements.RequirementRegistry;
import com.arkonas.ranks.requirements.requirement.AdvancementRequirement;
import com.arkonas.ranks.requirements.requirement.BlockBreakRequirement;
import com.arkonas.ranks.requirements.requirement.CraftItemRequirement;
import com.arkonas.ranks.requirements.requirement.GroupRequirement;
import com.arkonas.ranks.requirements.requirement.ItemDeductibleRequirement;
import com.arkonas.ranks.requirements.requirement.ItemRequirement;
import com.arkonas.ranks.requirements.requirement.MobKillsRequirement;
import com.arkonas.ranks.requirements.requirement.MoneyDeductibleRequirement;
import com.arkonas.ranks.requirements.requirement.MoneyRequirement;
import com.arkonas.ranks.requirements.requirement.PermissionRequirement;
import com.arkonas.ranks.requirements.requirement.PlaceholderRequirement;
import com.arkonas.ranks.requirements.requirement.PlayerKillsRequirement;
import com.arkonas.ranks.requirements.requirement.PlaytimeMinutesRequirement;
import com.arkonas.ranks.requirements.requirement.TotalMobKillsRequirement;
import com.arkonas.ranks.requirements.requirement.UseItemRequirement;
import com.arkonas.ranks.requirements.requirement.WorldRequirement;
import com.arkonas.ranks.requirements.requirement.XpLevelDeductibleRequirement;
import com.arkonas.ranks.requirements.requirement.XpLevelRequirement;
import com.arkonas.ranks.requirements.requirement.advancedachievements.AdvancedAchievementsAchievementRequirement;
import com.arkonas.ranks.requirements.requirement.advancedachievements.AdvancedAchievementsTotalRequirement;
import com.arkonas.ranks.requirements.requirement.mcmmo.McMMOPowerLevelRequirement;
import com.arkonas.ranks.requirements.requirement.mcmmo.McMMOSkillRequirement;
import com.arkonas.ranks.requirements.requirement.superbvote.SuperbVoteVotesRequirement;
import com.arkonas.ranks.requirements.requirement.tokenmanager.TokensDeductibleRequirement;
import com.arkonas.ranks.requirements.requirement.tokenmanager.TokensRequirement;
import com.arkonas.ranks.requirements.requirement.towny.TownyKingNumberResidentsRequirement;
import com.arkonas.ranks.requirements.requirement.towny.TownyKingNumberTownsRequirement;
import com.arkonas.ranks.requirements.requirement.towny.TownyKingRequirement;
import com.arkonas.ranks.requirements.requirement.towny.TownyMayorNumberResidentsRequirement;
import com.arkonas.ranks.requirements.requirement.towny.TownyMayorRequirement;
import com.arkonas.ranks.requirements.requirement.towny.TownyResidentRequirement;
import com.arkonas.ranks.requirements.requirement.votingplugin.VotingPluginPointsDeductibleRequirement;
import com.arkonas.ranks.requirements.requirement.votingplugin.VotingPluginPointsRequirement;
import com.arkonas.ranks.requirements.requirement.votingplugin.VotingPluginVotesRequirement;
import com.arkonas.ranks.formula.CostFormula;
import com.arkonas.ranks.formula.CostFormulaExpander;
import com.arkonas.ranks.serialization.RankSerialized;
import com.arkonas.ranks.serialization.ShadowDeserializer;
import com.arkonas.ranks.serialization.YamlDeserializer;
import com.arkonas.ranks.util.UpdateNotifier;
import com.arkonas.ranks.util.VersionChecker;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArkonasRanksPlugin extends JavaPlugin {

  public static final int CONFIG_VERSION = 10;

  @Getter
  private GroupProvider permissions;
  @Getter
  private Economy economy;
  /**
   * The registry for listing the requirements to /rankup.
   */
  @Getter
  private RequirementRegistry requirements;
  @Getter
  private FileConfiguration messages;
  @Getter
  private FileConfiguration config;
  @Getter
  private Rankups rankups;
  @Getter
  private com.arkonas.ranks.ladder.LadderRegistry ladders;
  @Getter
  private Prestiges prestiges;
  @Getter
  private com.arkonas.ranks.rebirth.RebirthManager rebirth;
  @Getter
  private com.arkonas.ranks.multiplier.MultiplierService multipliers;
  @Getter
  private com.arkonas.ranks.progress.ProgressDisplay progressDisplay;
  @Getter
  private com.arkonas.ranks.milestone.MilestoneService milestones;
  @Getter
  private com.arkonas.ranks.discord.DiscordAnnouncer discordAnnouncer;
  @Getter
  private Placeholders placeholders;
  @Getter
  private RankupHelper helper;
  @Getter
  private com.arkonas.ranks.text.ComponentRenderer componentRenderer =
      com.arkonas.ranks.text.ComponentRenderer.of("auto");
  @Getter
  private com.arkonas.ranks.effects.EffectsListener effectsListener;
  @Getter
  private com.arkonas.ranks.data.StatsService stats;
  @Getter
  private com.arkonas.ranks.menu.MenuModule menuModule;
  protected AutoRankup autoRankup = new AutoRankup(this);
  private String errorMessage;
  private PermissionManager permissionManager = new VaultPermissionManager(this);
  private EconomyProvider economyProvider =
      new com.arkonas.ranks.economy.ConfigurableEconomyProvider(this);

  public ArkonasRanksPlugin() {
    super();
  }

  protected ArkonasRanksPlugin(PermissionManager permissionManager, EconomyProvider economyProvider) {
    super();
    this.permissionManager = permissionManager;
    this.economyProvider = economyProvider;
  }

  protected ArkonasRanksPlugin(JavaPluginLoader loader, PluginDescriptionFile description, File dataFolder, File file, PermissionManager permissionManager, EconomyProvider economyProvider) {
    super(loader, description, dataFolder, file);
    this.permissionManager = permissionManager;
    this.economyProvider = economyProvider;
  }

  @Override
  public void onEnable() {
    UpdateNotifier notifier = new UpdateNotifier(new VersionChecker(this));

    reload(true);

    if (System.getProperty("RANKUP_TEST") == null) {
      Metrics metrics = new Metrics(this);
      metrics.addCustomChart(new Metrics.SimplePie("confirmation",
          () -> config.getString("confirmation-type", "unknown")));
      metrics.addCustomChart(new Metrics.AdvancedPie("requirements", () -> {
        Map<String, Integer> map = new HashMap<>();
        addAllRequirements(map, rankups);
        if (prestiges != null) {
          addAllRequirements(map, prestiges);
        }
        return map;
      }));
      metrics.addCustomChart(new Metrics.SimplePie("prestige",
          () -> config.getBoolean("prestige") ? "enabled" : "disabled"));
      metrics.addCustomChart(new Metrics.SimplePie("permission-rankup",
          () -> config.getBoolean("permission-rankup") ? "enabled" : "disabled"));
      metrics.addCustomChart(new Metrics.SimplePie("notify-update",
          () -> config.getBoolean("notify-update") ? "enabled" : "disabled"));
    }

    // The advanced menu module wraps the player-facing commands with pop-up
    // inventory screens. It is fully additive: when menus.enabled is absent or
    // false the plugin behaves exactly like the Rankup3-parity core.
    boolean menus = config.getBoolean("menus.enabled");
    if (menus) {
      menuModule = new com.arkonas.ranks.menu.MenuModule(this);
      if (config.getBoolean("ranks-gui")
          || "gui".equalsIgnoreCase(config.getString("confirmation-type", ""))) {
        getLogger().info("menus.enabled is true: ranks-gui and confirmation-type are ignored "
            + "while the menu module is active (the menus are the confirmation screen).");
      }
    }

    if (config.getBoolean("ranks")) {
      if (menus) {
        // menus override ranks-gui; the console still gets the parity chat list
        getCommand("ranks").setExecutor(
            new com.arkonas.ranks.menu.commands.MenuRanksCommand(this, menuModule, new RanksCommand(this)));
      } else if (config.getBoolean("ranks-gui")) {
        RanksGuiListener listener = new RanksGuiListener();
        getCommand("ranks").setExecutor(new RanksGuiCommand(this, listener));
        getServer().getPluginManager().registerEvents(listener, this);
      } else {
        getCommand("ranks").setExecutor(new RanksCommand(this));
      }
    }
    if (config.getBoolean("prestige")) {
      PrestigeCommand prestigeParity = new PrestigeCommand(this);
      getCommand("prestige").setExecutor(menus
          ? new com.arkonas.ranks.menu.commands.MenuPrestigeCommand(this, menuModule, prestigeParity)
          : prestigeParity);
      if (config.getBoolean("prestiges")) {
        PrestigesCommand prestigesParity = new PrestigesCommand(this);
        getCommand("prestiges").setExecutor(menus
            ? new com.arkonas.ranks.menu.commands.MenuPrestigesCommand(this, menuModule, prestigesParity)
            : prestigesParity);
      }
    }
    if (config.getBoolean("max-rankup.enabled")) {
      getCommand("maxrankup").setExecutor(new MaxRankupCommand(this));
      getCommand("maxrankup").setTabCompleter(new com.arkonas.ranks.commands.LadderTabCompleter(this));
    }

    if (config.getBoolean("rebirth.enabled")) {
      getCommand("rebirth").setExecutor(new com.arkonas.ranks.commands.RebirthCommand(this));
      getCommand("rebirths").setExecutor(new com.arkonas.ranks.commands.RebirthsCommand(this));
    }

    RankupCommand rankupParity = new RankupCommand(this);
    getCommand("rankup").setExecutor(menus
        ? new com.arkonas.ranks.menu.commands.MenuRankupCommand(this, menuModule, rankupParity)
        : rankupParity);
    getCommand("rankup").setTabCompleter(
        new com.arkonas.ranks.commands.LadderTabCompleter(this, "noconfirm", "top", "gui"));
    getCommand("arkonasranks").setExecutor(new InfoCommand(this, notifier));
    effectsListener = new com.arkonas.ranks.effects.EffectsListener(this);
    getServer().getPluginManager().registerEvents(effectsListener, this);
    if (config.getBoolean("database.enabled", true)) {
      try {
        stats = new com.arkonas.ranks.data.StatsService(getLogger(), getDataFolder(),
            config.getConfigurationSection("database"));
        getServer().getPluginManager().registerEvents(
            new com.arkonas.ranks.data.StatsListener(stats), this);
        // milestone rewards ride on the stats counts (needs the database enabled)
        milestones = com.arkonas.ranks.milestone.MilestoneService.fromConfig(
            this, config.getConfigurationSection("milestones"));
        if (milestones.isEnabled()) {
          stats.setMilestoneHook(milestones);
        }
      } catch (Exception e) {
        getLogger().log(java.util.logging.Level.SEVERE,
            "Could not initialise the statistics database; /rankup top and"
                + " leaderboard placeholders are disabled", e);
        stats = null;
      }
    }
    // Discord announcements (opt-in). Resolve the DiscordSRV adapter only when configured on,
    // so servers without the dependency never pay the reflection lookup.
    com.arkonas.ranks.discord.DiscordAnnouncer announcer =
        com.arkonas.ranks.discord.DiscordAnnouncer.fromConfig(
            config.getConfigurationSection("discord"),
            com.arkonas.ranks.discord.DiscordSrvSender.tryCreate(getLogger()));
    if (announcer.isEnabled()) {
      discordAnnouncer = announcer;
      getServer().getPluginManager().registerEvents(
          new com.arkonas.ranks.discord.DiscordListener(announcer), this);
    } else if (config.getBoolean("discord.enabled", false)) {
      getLogger().info("Discord announcements are enabled in config but DiscordSRV was not found;"
          + " skipping the hook.");
    }

    // Citizens NPC rankup (opt-in). Registered by reflection, so no Citizens compile dependency.
    com.arkonas.ranks.citizens.NpcRankupSettings npcSettings =
        com.arkonas.ranks.citizens.NpcRankupSettings.fromConfig(
            config.getConfigurationSection("citizens"));
    if (npcSettings.isEnabled()) {
      if (com.arkonas.ranks.citizens.CitizensHook.register(this, npcSettings)) {
        getLogger().info("Citizens NPC rankup hook enabled.");
      } else {
        getLogger().info("Citizens NPC rankup is enabled in config but Citizens was not found;"
            + " skipping the hook.");
      }
    }

    getServer().getPluginManager().registerEvents(new GuiListener(this), this);
    if (menuModule != null) {
      // the parity GuiListener stays registered for when menus are disabled
      getServer().getPluginManager().registerEvents(menuModule.getListener(), this);
    }
    getServer().getPluginManager().registerEvents(
        new JoinUpdateNotifier(notifier, () -> getConfig().getBoolean("notify-update"), "rankup.notify"), this);

    placeholders = new Placeholders(this);
    placeholders.register();

    // live progress display (opt-in). Toggling it needs a restart, like other scheduled features.
    if (progressDisplay != null) {
      getServer().getPluginManager().registerEvents(progressDisplay, this);
      long interval = progressDisplay.intervalTicks(config.getConfigurationSection("progress-display"));
      progressDisplay.runTaskTimer(this, interval, interval);
    }
  }


  @Override
  public void onDisable() {
    closeInventories();
    if (menuModule != null) {
      menuModule.closeAll();
    }
    if (placeholders != null) {
      placeholders.unregister();
    }
    if (stats != null) {
      stats.close();
      stats = null;
    }
  }

  public void reload(boolean init) {
    errorMessage = null;

    config = loadConfig("config.yml");

    if (config.getBoolean("permission-rankup")) {
      permissions = permissionManager.permissionOnlyProvider();
    } else {
      permissions = permissionManager.findPermissionProvider();
      if (permissions == null) {
        errorMessage = "No permission plugin found";
      }
    }

    setupEconomy();

    closeInventories();
    loadConfigs(init);

    long time = (long) (config.getDouble("autorankup-interval") * 60 * 20);
    if (time > 0) {
      try {
        if (!autoRankup.isCancelled()) {
          autoRankup.cancel();
        }
      } catch (IllegalStateException ignored) {
      }
      autoRankup = new AutoRankup(this);
      autoRankup.runTaskTimer(this, time, time);
    }

    if (config.getInt("version") < CONFIG_VERSION) {
      getLogger().severe("You are using an outdated config!");
      getLogger().severe("This means that some things might not work!");
      getLogger().severe("To update, please rename ALL your config files (or the folder they are in),");
      getLogger().severe("and run /aru reload to generate a new config file.");
      getLogger().severe("If that does not work, restart your server.");
      getLogger().severe("You may then copy in your config values manually from the old config.");
    }

    componentRenderer = com.arkonas.ranks.text.ComponentRenderer.of(config.getString("message-format", "auto"));
    if (effectsListener != null) {
      effectsListener.reload();
    }
    if (menuModule != null) {
      // close open menus and re-read menus.yml (the ladder may have changed)
      menuModule.reload();
    }

    helper = new RankupHelper(this);
  }

  public MessageBuilder newMessageBuilder(String message) {
    return new PebbleMessageBuilder(this, message);
  }

  public boolean error() {
    return error(null);
  }

  /**
   * Notify the player of an error if there is one
   *
   * @return true if there was an error and action was taken
   */
  public boolean error(CommandSender sender) {
    if (errorMessage == null) {
      return false;
    }

    if (sender instanceof Player) {
      sender.sendMessage(
          ChatColor.RED + "Could not load Rankup, check console for more information.");
    } else {
      getLogger().severe("Failed to load Rankup");
    }
    for (String line : errorMessage.split("\n")) {
      getLogger().severe(line);
    }
    getLogger().severe("More information can be found in the console log at startup");
    return true;
  }

  private void addAllRequirements(Map<String, Integer> map, RankList<? extends Rank> ranks) {
    for (Rank rank : ranks.getTree()) {
      for (Requirement requirement : rank.getRequirements().getRequirements(null)) {
        String name = requirement.getName();
        map.put(name, map.getOrDefault(name, 0) + 1);
      }
    }
  }

  /**
   * Closes all rankup inventories on disable so players cannot grab items from the inventory on a
   * plugin reload.
   */
  private void closeInventories() {
    for (Player player : Bukkit.getOnlinePlayers()) {
      InventoryView view = player.getOpenInventory();
      if (view.getType() == InventoryType.CHEST
          && view.getTopInventory().getHolder() instanceof Gui) {
        player.closeInventory();
      }
    }
  }

  private void loadConfigs(boolean init) {
    saveLocales();

    String locale = config.getString("locale", "en");
    File localeFile = new File(new File(getDataFolder(), "locale"), locale + ".yml");
    messages = YamlConfiguration.loadConfiguration(localeFile);

    if (init) {
      Bukkit.getScheduler().runTask(this, () -> {
        refreshRanks();
        error();
      });
    } else {
      refreshRanks();
    }
  }

  public void refreshRanks() {
    try {
      registerRequirements();
      Bukkit.getPluginManager().callEvent(new RankupRegisterEvent(this));

      if (config.getBoolean("prestige")) {
        prestiges = new Prestiges(this, loadConfig("prestiges.yml"));
//        prestiges.getOrderedList();
      } else {
        prestiges = null;
      }

      List<RankSerialized> rankupConfig = loadRankupConfig("rankups");
      CostFormula costFormula = CostFormula.fromConfig(getConfig().getConfigurationSection("cost-formula"));
      rankupConfig = CostFormulaExpander.expand(costFormula, rankupConfig);
      rankups = new Rankups(this, rankupConfig);
      // check rankups are not in an infinite loop
//      rankups.getOrderedList();

      ladders = new com.arkonas.ranks.ladder.LadderRegistry();
      ladders.put(com.arkonas.ranks.ladder.LadderRegistry.DEFAULT, rankups);
      loadExtraLadders();

      rebirth = com.arkonas.ranks.rebirth.RebirthManager.fromConfig(
          this, getConfig().getConfigurationSection("rebirth"));

      multipliers = com.arkonas.ranks.multiplier.MultiplierService.fromConfig(
          getConfig().getConfigurationSection("multipliers"));

      progressDisplay = com.arkonas.ranks.progress.ProgressDisplay.fromConfig(
          this, getConfig().getConfigurationSection("progress-display"));


    } catch (RuntimeException e) {
      this.errorMessage = e.getClass().getName() + ": " + e.getMessage();
      e.printStackTrace();
    }
  }

  private void saveLocales() {
    saveLocale("en");
    saveLocale("tr");
    saveLocale("pt_br");
    saveLocale("ru");
    saveLocale("zh_cn");
    saveLocale("fr");
    saveLocale("it");
    saveLocale("es");
    saveLocale("nl");
  }

  private void saveLocale(String locale) {
    String name = "locale/" + locale + ".yml";
    File file = new File(getDataFolder(), name);
    if (!file.exists()) {
      saveResource(name, false);
    }
  }

  private List<RankSerialized> loadRankupConfig(String name) {
    File ymlFile = new File(getDataFolder(), name + ".yml");
    File tomlFile = new File(getDataFolder(), name + ".toml");
    if (tomlFile.exists()) {
      try {
        return ShadowDeserializer.deserialize(TomlFormat.instance().createParser().parse(new FileReader(tomlFile)));
      } catch (FileNotFoundException ignored) {
      }
    }
    if (!ymlFile.exists()) {
      saveResource(ymlFile.getName(), false);
    }
    return YamlDeserializer.deserialize(YamlConfiguration.loadConfiguration(ymlFile));
  }

  /**
   * Loads any additional rankup ladders from files in the {@code ladders/} folder into the ladder
   * registry. Each file (yaml or toml, toml winning on a name clash) becomes a ladder keyed by its
   * file name; the reserved id {@code default} is skipped. A per-ladder cost formula may be set at
   * {@code ladders.<id>.cost-formula} in config.yml, otherwise the global {@code cost-formula}
   * applies. A single malformed ladder is logged and skipped, never aborting startup.
   */
  private void loadExtraLadders() {
    File dir = new File(getDataFolder(), "ladders");
    if (!dir.isDirectory()) {
      return;
    }
    File[] files = dir.listFiles((d, fileName) -> {
      String lower = fileName.toLowerCase();
      return lower.endsWith(".yml") || lower.endsWith(".toml");
    });
    if (files == null || files.length == 0) {
      return;
    }

    java.util.Map<String, File> ymlById = new java.util.HashMap<>();
    java.util.Map<String, File> tomlById = new java.util.HashMap<>();
    for (File file : files) {
      String name = file.getName();
      String lower = name.toLowerCase();
      String id = lower.replaceAll("\\.(yml|toml)$", "");
      if (id.equals(com.arkonas.ranks.ladder.LadderRegistry.DEFAULT)) {
        getLogger().warning("Ignoring ladders/" + name
            + ": the id 'default' is reserved for rankups.yml");
        continue;
      }
      (lower.endsWith(".toml") ? tomlById : ymlById).put(id, file);
    }

    java.util.Set<String> ids = new java.util.TreeSet<>();
    ids.addAll(ymlById.keySet());
    ids.addAll(tomlById.keySet());
    for (String id : ids) {
      File file = tomlById.getOrDefault(id, ymlById.get(id));
      try {
        List<RankSerialized> cfg = loadLadderFile(file);
        org.bukkit.configuration.ConfigurationSection ladderFormula =
            getConfig().getConfigurationSection("ladders." + id + ".cost-formula");
        CostFormula formula = CostFormula.fromConfig(ladderFormula != null ? ladderFormula
            : getConfig().getConfigurationSection("cost-formula"));
        cfg = CostFormulaExpander.expand(formula, cfg);
        ladders.put(id, new Rankups(this, cfg));
        getLogger().info("Loaded rankup ladder '" + id + "' (" + cfg.size() + " ranks).");
      } catch (Exception e) {
        getLogger().log(java.util.logging.Level.SEVERE,
            "Failed to load ladder file " + file.getName() + "; skipping it", e);
      }
    }
  }

  private List<RankSerialized> loadLadderFile(File file) throws FileNotFoundException {
    if (file.getName().toLowerCase().endsWith(".toml")) {
      return ShadowDeserializer.deserialize(
          TomlFormat.instance().createParser().parse(new FileReader(file)));
    }
    return YamlDeserializer.deserialize(YamlConfiguration.loadConfiguration(file));
  }

  private FileConfiguration loadConfig(String name) {
    File file = new File(getDataFolder(), name);
    if (!file.exists()) {
      saveResource(name, false);
    }
    return YamlConfiguration.loadConfiguration(file);
  }

  private void registerRequirements() {
    requirements = new RequirementRegistry();
    requirements.addRequirements(
        new XpLevelRequirement(this, "xp-levelh"),
        new XpLevelDeductibleRequirement(this, "xp-level"),
        new PlaytimeMinutesRequirement(this),
        new AdvancementRequirement(this),
        new GroupRequirement(this),
        new PermissionRequirement(this),
        new PlaceholderRequirement(this),
        new WorldRequirement(this),
        new BlockBreakRequirement(this),
        new PlayerKillsRequirement(this),
        new MobKillsRequirement(this),
        new ItemRequirement(this, "itemh"),
        new ItemDeductibleRequirement(this, "item"),
        new UseItemRequirement(this),
        new TotalMobKillsRequirement(this),
        new CraftItemRequirement(this));
    if (economy != null) {
      requirements.addRequirements(
          new MoneyRequirement(this, "moneyh"),
          new MoneyDeductibleRequirement(this, "money"));
    }

    PluginManager pluginManager = Bukkit.getPluginManager();
    if (pluginManager.isPluginEnabled("mcMMO")) {
      requirements.addRequirements(
          new McMMOSkillRequirement(this),
          new McMMOPowerLevelRequirement(this));
    }
    if (pluginManager.isPluginEnabled("AdvancedAchievements")) {
      requirements.addRequirements(
          new AdvancedAchievementsAchievementRequirement(this),
          new AdvancedAchievementsTotalRequirement(this));
    }
    if (pluginManager.isPluginEnabled("VotingPlugin")) {
      requirements.addRequirements(
          new VotingPluginVotesRequirement(this),
          new VotingPluginPointsRequirement(this, "votingplugin-pointsh"),
          new VotingPluginPointsDeductibleRequirement(this, "votingplugin-points"));
    }
    if (Bukkit.getPluginManager().isPluginEnabled("Towny")) {
      requirements.addRequirements(
          new TownyResidentRequirement(this),
          new TownyMayorRequirement(this),
          new TownyMayorNumberResidentsRequirement(this),
          new TownyKingRequirement(this),
          new TownyKingNumberResidentsRequirement(this),
          new TownyKingNumberTownsRequirement(this));
    }
    if (Bukkit.getPluginManager().isPluginEnabled("TokenManager")) {
      requirements.addRequirements(
          new TokensRequirement(this, "tokenmanager-tokensh"),
          new TokensDeductibleRequirement(this, "tokenmanager-tokens"));
    }
    if (Bukkit.getPluginManager().isPluginEnabled("SuperbVote")) {
      requirements.addRequirements(new SuperbVoteVotesRequirement(this));
    }
  }

  private void setupEconomy() {
    economy = economyProvider.getEconomy();
  }

  public ConfigurationSection getSection(Rank rank, String path) {
    ConfigurationSection rankSection = rank.getSection();
    if (rankSection == null || !rankSection.isConfigurationSection(path)) {
      return this.messages.getConfigurationSection(path);
    }
    return rankSection.getConfigurationSection(path);
  }

  public MessageBuilder getMessage(Rank rank, Message message) {
    ConfigurationSection messages = rank.getSection();
    if (messages == null || !messages.isSet(message.getName())) {
      messages = this.messages;
    }
    return newMessageBuilder(messages.getString(message.getName()));
  }

  public MessageBuilder getMessage(Message message) {
    return newMessageBuilder(messages.getString(message.getName()));
  }

  public MessageBuilder getMessage(CommandSender player, Message message, Rank oldRank, Rank rank) {
    Rank actualOldRank;
    if (oldRank instanceof Prestige && oldRank.getRank() == null) {
      actualOldRank = rankups.getByName(((Prestige) oldRank).getFrom()).getRank();
    } else {
      actualOldRank = oldRank;
    }

    return getMessage(oldRank, message)
        .replacePlayer(player)
        .replaceRank(rank)
        .replaceOldRank(actualOldRank);
  }

  public void sendHeaderFooter(CommandSender sender, Rank rank, Message type) {
    MessageBuilder builder;
    if (rank == null) {
      builder = getMessage(type)
          .failIfEmpty()
          .replacePlayer(sender);
    } else {
      builder = getMessage(rank, type)
          .failIfEmpty()
          .replacePlayer(sender)
          .replaceRank(rank);
    }
    builder.send(sender);
  }
}
