package com.arkonas.ranks.data;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Rankup/prestige history and leaderboards. All JDBC happens on a dedicated
 * single-thread executor; the leaderboard caches are refreshed periodically so
 * PlaceholderAPI reads never block the main thread.
 */
public class StatsService implements AutoCloseable {

  private final Logger logger;
  private final HikariDataSource dataSource;
  private final ExecutorService executor =
      Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "ArkonasRanks-Stats");
        thread.setDaemon(true);
        return thread;
      });

  private volatile List<LeaderboardEntry> topRankups = List.of();
  private volatile List<LeaderboardEntry> topPrestiges = List.of();
  private final Map<UUID, int[]> countsCache = new ConcurrentHashMap<>();
  private volatile MilestoneHook milestoneHook;

  public StatsService(Logger logger, File dataFolder, ConfigurationSection database)
      throws SQLException {
    this.logger = logger;
    HikariConfig config = new HikariConfig();
    String type = database == null ? "sqlite" : database.getString("type", "sqlite");
    if ("mysql".equalsIgnoreCase(type) || "mariadb".equalsIgnoreCase(type)) {
      config.setJdbcUrl("jdbc:mariadb://" + database.getString("host", "localhost")
          + ":" + database.getInt("port", 3306)
          + "/" + database.getString("database", "arkonasranks"));
      config.setUsername(database.getString("username", "root"));
      config.setPassword(database.getString("password", ""));
      config.setMaximumPoolSize(database.getInt("pool-size", 4));
    } else {
      config.setJdbcUrl("jdbc:sqlite:" + new File(dataFolder, "data.db").getAbsolutePath());
      // sqlite: single connection avoids file-lock contention
      config.setMaximumPoolSize(1);
    }
    config.setPoolName("ArkonasRanks-Hikari");
    dataSource = new HikariDataSource(config);
    createTables();
    refreshLeaderboards();
  }

  private void createTables() throws SQLException {
    try (Connection connection = dataSource.getConnection()) {
      connection.createStatement().executeUpdate("""
          CREATE TABLE IF NOT EXISTS ar_players (
            uuid VARCHAR(36) PRIMARY KEY,
            name VARCHAR(16) NOT NULL,
            rankup_count INT NOT NULL DEFAULT 0,
            prestige_count INT NOT NULL DEFAULT 0,
            last_rankup_ts BIGINT NOT NULL DEFAULT 0
          )""");
      connection.createStatement().executeUpdate("""
          CREATE TABLE IF NOT EXISTS ar_history (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            uuid VARCHAR(36) NOT NULL,
            type VARCHAR(16) NOT NULL,
            from_rank VARCHAR(64),
            to_rank VARCHAR(64),
            ts BIGINT NOT NULL
          )""".replace("INTEGER PRIMARY KEY AUTOINCREMENT",
              dataSource.getJdbcUrl().startsWith("jdbc:sqlite")
                  ? "INTEGER PRIMARY KEY AUTOINCREMENT"
                  : "BIGINT PRIMARY KEY AUTO_INCREMENT"));
    }
  }

  public void record(RankupRecord record) {
    executor.execute(() -> {
      try (Connection connection = dataSource.getConnection()) {
        try (PreparedStatement history = connection.prepareStatement(
            "INSERT INTO ar_history (uuid, type, from_rank, to_rank, ts) VALUES (?, ?, ?, ?, ?)")) {
          history.setString(1, record.uuid().toString());
          history.setString(2, record.type().name());
          history.setString(3, record.fromRank());
          history.setString(4, record.toRank());
          history.setLong(5, record.timestamp());
          history.executeUpdate();
        }
        boolean prestige = record.type() == RankupRecord.Type.PRESTIGE
            || record.type() == RankupRecord.Type.FORCE_PRESTIGE;
        String upsert = dataSource.getJdbcUrl().startsWith("jdbc:sqlite")
            ? "INSERT INTO ar_players (uuid, name, rankup_count, prestige_count, last_rankup_ts) "
              + "VALUES (?, ?, ?, ?, ?) ON CONFLICT(uuid) DO UPDATE SET name = excluded.name, "
              + "rankup_count = rankup_count + excluded.rankup_count, "
              + "prestige_count = prestige_count + excluded.prestige_count, "
              + "last_rankup_ts = excluded.last_rankup_ts"
            : "INSERT INTO ar_players (uuid, name, rankup_count, prestige_count, last_rankup_ts) "
              + "VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE name = VALUES(name), "
              + "rankup_count = rankup_count + VALUES(rankup_count), "
              + "prestige_count = prestige_count + VALUES(prestige_count), "
              + "last_rankup_ts = VALUES(last_rankup_ts)";
        try (PreparedStatement players = connection.prepareStatement(upsert)) {
          players.setString(1, record.uuid().toString());
          players.setString(2, record.name());
          players.setInt(3, prestige ? 0 : 1);
          players.setInt(4, prestige ? 1 : 0);
          players.setLong(5, record.timestamp());
          players.executeUpdate();
        }
        countsCache.remove(record.uuid());

        // notify the milestone hook with the just-written totals (same stats thread, so no race)
        MilestoneHook hook = this.milestoneHook;
        if (hook != null) {
          try (PreparedStatement counts = connection.prepareStatement(
              "SELECT rankup_count, prestige_count FROM ar_players WHERE uuid = ?")) {
            counts.setString(1, record.uuid().toString());
            ResultSet result = counts.executeQuery();
            if (result.next()) {
              hook.onRecord(record, result.getInt(1), result.getInt(2));
            }
          }
        }
      } catch (SQLException e) {
        logger.log(Level.SEVERE, "Failed to record rankup stats", e);
      }
      // outside the try-with-resources: with a single-connection sqlite pool the
      // leaderboard query must not run while the write connection is still open
      refreshLeaderboardsNow();
    });
  }

  /** Registers a hook notified after each recorded rankup/prestige with the fresh totals. */
  public void setMilestoneHook(MilestoneHook hook) {
    this.milestoneHook = hook;
  }

  /** Asynchronously fetches the top list and hands it back on the stats thread. */
  public void top(boolean prestiges, int limit, Consumer<List<LeaderboardEntry>> callback) {
    executor.execute(() -> callback.accept(queryTop(prestiges, limit)));
  }

  /** Cached snapshot for PlaceholderAPI; never blocks. */
  public List<LeaderboardEntry> cachedTop(boolean prestiges) {
    return prestiges ? topPrestiges : topRankups;
  }

  /** Cached per-player counts for PlaceholderAPI; loads asynchronously on miss. */
  public int[] cachedCounts(UUID uuid) {
    int[] counts = countsCache.get(uuid);
    if (counts == null) {
      countsCache.put(uuid, new int[]{0, 0});
      executor.execute(() -> {
        try (Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(
                "SELECT rankup_count, prestige_count FROM ar_players WHERE uuid = ?")) {
          statement.setString(1, uuid.toString());
          ResultSet result = statement.executeQuery();
          if (result.next()) {
            countsCache.put(uuid, new int[]{result.getInt(1), result.getInt(2)});
          }
        } catch (SQLException e) {
          logger.log(Level.WARNING, "Failed to load player stats", e);
        }
      });
      return new int[]{0, 0};
    }
    return counts;
  }

  public void refreshLeaderboards() {
    executor.execute(this::refreshLeaderboardsNow);
  }

  private void refreshLeaderboardsNow() {
    topRankups = queryTop(false, 10);
    topPrestiges = queryTop(true, 10);
  }

  private List<LeaderboardEntry> queryTop(boolean prestiges, int limit) {
    String column = prestiges ? "prestige_count" : "rankup_count";
    List<LeaderboardEntry> entries = new ArrayList<>();
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(
            "SELECT uuid, name, " + column + " FROM ar_players WHERE " + column
                + " > 0 ORDER BY " + column + " DESC LIMIT ?")) {
      statement.setInt(1, limit);
      ResultSet result = statement.executeQuery();
      while (result.next()) {
        entries.add(new LeaderboardEntry(
            UUID.fromString(result.getString(1)), result.getString(2), result.getInt(3)));
      }
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Failed to query leaderboard", e);
    }
    return entries;
  }

  /** Blocks until previously submitted writes finish. Test helper. */
  public void flush() {
    try {
      executor.submit(() -> { }).get(10, TimeUnit.SECONDS);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void close() {
    executor.shutdown();
    try {
      if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
        logger.warning("Stats executor did not drain in time");
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    dataSource.close();
  }
}
