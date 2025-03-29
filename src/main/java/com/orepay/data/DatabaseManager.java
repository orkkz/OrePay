package com.orepay.data;

import com.orepay.OrePay;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Manages database interactions for the plugin
 */
public class DatabaseManager {

    private final OrePay plugin;
    private final YamlDataManager yamlDataManager;
    private Connection connection;
    private boolean useDatabase;

    public DatabaseManager(OrePay plugin) {
        this.plugin = plugin;
        this.yamlDataManager = new YamlDataManager(plugin);
        this.useDatabase = plugin.getConfigManager().getBoolean("storage.use-database", false);
        
        if (useDatabase) {
            setupDatabase();
        }
    }

    /**
     * Setup the database connection and tables
     */
    private void setupDatabase() {
        String databaseType = plugin.getConfigManager().getString("storage.database.type", "sqlite").toLowerCase();
        String host, database, username, password;
        int port;

        try {
            switch (databaseType) {
                case "mysql":
                    host = plugin.getConfigManager().getString("storage.database.mysql.host", "localhost");
                    port = plugin.getConfigManager().getInt("storage.database.mysql.port", 3306);
                    database = plugin.getConfigManager().getString("storage.database.mysql.database", "orepay");
                    username = plugin.getConfigManager().getString("storage.database.mysql.username", "root");
                    password = plugin.getConfigManager().getString("storage.database.mysql.password", "");

                    Class.forName("com.mysql.jdbc.Driver");
                    String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false";
                    connection = DriverManager.getConnection(url, username, password);
                    break;

                case "sqlite":
                default:
                    Class.forName("org.sqlite.JDBC");
                    connection = DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder() + "/orepay.db");
                    break;
            }

            // Create tables if they don't exist
            createTables();
            plugin.getLogger().info("Database connection established!");
        } catch (ClassNotFoundException | SQLException e) {
            plugin.getLogger().severe("Error setting up database connection: " + e.getMessage());
            plugin.getLogger().warning("Falling back to YAML storage...");
            useDatabase = false;
        }
    }

    /**
     * Create the necessary database tables
     */
    private void createTables() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            // Player settings table
            statement.execute("CREATE TABLE IF NOT EXISTS orepay_settings ("
                    + "uuid VARCHAR(36) PRIMARY KEY, "
                    + "rewards_enabled BOOLEAN DEFAULT TRUE"
                    + ")");

            // Mining statistics table
            statement.execute("CREATE TABLE IF NOT EXISTS orepay_statistics ("
                    + "id INTEGER PRIMARY KEY " + (useDatabase && plugin.getConfigManager().getString("storage.database.type", "sqlite").equalsIgnoreCase("mysql") ? "AUTO_INCREMENT" : "AUTOINCREMENT") + ", "
                    + "uuid VARCHAR(36) NOT NULL, "
                    + "ore VARCHAR(50) NOT NULL, "
                    + "times_mined INTEGER DEFAULT 0, "
                    + "amount_earned DOUBLE DEFAULT 0.0, "
                    + "UNIQUE(uuid, ore)"
                    + ")");
        }
    }

    /**
     * Close the database connection
     */
    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                plugin.getLogger().severe("Error closing database connection: " + e.getMessage());
            }
        }
    }

    /**
     * Check if rewards are enabled for a player
     * @param player The player to check
     * @return CompletableFuture with true if rewards are enabled, false otherwise
     */
    public CompletableFuture<Boolean> areRewardsEnabled(Player player) {
        return areRewardsEnabled(player.getUniqueId());
    }

    /**
     * Check if rewards are enabled for a player
     * @param uuid The player UUID
     * @return CompletableFuture with true if rewards are enabled, false otherwise
     */
    public CompletableFuture<Boolean> areRewardsEnabled(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> areRewardsEnabledSync(uuid));
    }
    
    /**
     * Check if rewards are enabled for a player (synchronous)
     * @param player The player to check
     * @return True if rewards are enabled, false otherwise
     */
    public boolean areRewardsEnabledSync(Player player) {
        return areRewardsEnabledSync(player.getUniqueId());
    }
    
    /**
     * Check if rewards are enabled for a player (synchronous)
     * @param uuid The player UUID
     * @return True if rewards are enabled, false otherwise
     */
    public boolean areRewardsEnabledSync(UUID uuid) {
        if (!useDatabase) {
            return yamlDataManager.areRewardsEnabled(uuid);
        }
        
        try {
            String sql = "SELECT rewards_enabled FROM orepay_settings WHERE uuid = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, uuid.toString());
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getBoolean("rewards_enabled");
                    } else {
                        // Player not found, insert default value
                        insertDefaultSettings(uuid);
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error checking rewards enabled: " + e.getMessage());
            return true; // Default to enabled
        }
    }
    
    /**
     * Set whether rewards are enabled for a player
     * @param player The player
     * @param enabled Whether rewards should be enabled
     */
    public void setRewardsEnabled(Player player, boolean enabled) {
        setRewardsEnabled(player.getUniqueId(), enabled);
    }
    
    /**
     * Set whether rewards are enabled for a player
     * @param uuid The player UUID
     * @param enabled Whether rewards should be enabled
     */
    public void setRewardsEnabled(UUID uuid, boolean enabled) {
        if (!useDatabase) {
            yamlDataManager.setRewardsEnabled(uuid, enabled);
            return;
        }
        
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String sql = "INSERT INTO orepay_settings (uuid, rewards_enabled) VALUES (?, ?) "
                        + "ON CONFLICT(uuid) DO UPDATE SET rewards_enabled = ?";
                
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, uuid.toString());
                    statement.setBoolean(2, enabled);
                    statement.setBoolean(3, enabled);
                    statement.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error setting rewards enabled: " + e.getMessage());
            }
        });
    }

    /**
     * Insert default settings for a player
     * @param uuid The player UUID
     * @throws SQLException If an error occurs
     */
    private void insertDefaultSettings(UUID uuid) throws SQLException {
        String sql = "INSERT INTO orepay_settings (uuid, rewards_enabled) VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setBoolean(2, true);
            statement.executeUpdate();
        }
    }

    /**
     * Record a mining statistic
     * @param player The player
     * @param material The ore material
     * @param amount The amount earned
     */
    public void recordMiningStatistic(Player player, Material material, double amount) {
        if (!plugin.getConfigManager().getBoolean("statistics.enabled", true)) {
            return;
        }
        
        if (!useDatabase) {
            yamlDataManager.recordMiningStatistic(player.getUniqueId(), material.name(), amount);
            return;
        }
        
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String sql = "INSERT INTO orepay_statistics (uuid, ore, times_mined, amount_earned) VALUES (?, ?, 1, ?) "
                        + "ON CONFLICT(uuid, ore) DO UPDATE SET "
                        + "times_mined = times_mined + 1, "
                        + "amount_earned = amount_earned + ?";
                
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, player.getUniqueId().toString());
                    statement.setString(2, material.name());
                    statement.setDouble(3, amount);
                    statement.setDouble(4, amount);
                    statement.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error recording mining statistic: " + e.getMessage());
            }
        });
    }

    /**
     * Get all statistics for a player
     * @param player The player
     * @return CompletableFuture with map of ore names to StatisticEntry objects
     */
    public CompletableFuture<Map<String, StatisticEntry>> getPlayerStatistics(Player player) {
        return getPlayerStatistics(player.getUniqueId());
    }

    /**
     * Get all statistics for a player
     * @param uuid The player UUID
     * @return CompletableFuture with map of ore names to StatisticEntry objects
     */
    public CompletableFuture<Map<String, StatisticEntry>> getPlayerStatistics(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            if (!useDatabase) {
                return yamlDataManager.getPlayerStatistics(uuid);
            }
            
            Map<String, StatisticEntry> statistics = new HashMap<>();
            
            try {
                String sql = "SELECT ore, times_mined, amount_earned FROM orepay_statistics WHERE uuid = ?";
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, uuid.toString());
                    
                    try (ResultSet resultSet = statement.executeQuery()) {
                        while (resultSet.next()) {
                            String ore = resultSet.getString("ore");
                            int timesMined = resultSet.getInt("times_mined");
                            double amountEarned = resultSet.getDouble("amount_earned");
                            
                            statistics.put(ore, new StatisticEntry(timesMined, amountEarned));
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error getting player statistics: " + e.getMessage());
            }
            
            return statistics;
        });
    }
    
    /**
     * Get the total amount earned by a player
     * @param player The player
     * @return CompletableFuture with the total amount earned
     */
    public CompletableFuture<Double> getTotalEarned(Player player) {
        return getTotalEarned(player.getUniqueId());
    }
    
    /**
     * Get the total amount earned by a player
     * @param uuid The player UUID
     * @return CompletableFuture with the total amount earned
     */
    public CompletableFuture<Double> getTotalEarned(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> getTotalEarnedSync(uuid));
    }
    
    /**
     * Get the total amount earned by a player (synchronous)
     * @param uuid The player UUID
     * @return The total amount earned
     */
    public double getTotalEarnedSync(UUID uuid) {
        if (!useDatabase) {
            return yamlDataManager.getTotalEarned(uuid);
        }
        
        try {
            String sql = "SELECT SUM(amount_earned) AS total FROM orepay_statistics WHERE uuid = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, uuid.toString());
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getDouble("total");
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error getting total earned: " + e.getMessage());
        }
        
        return 0.0;
    }
    
    /**
     * Get the total number of ores mined by a player
     * @param player The player
     * @return CompletableFuture with the total number of ores mined
     */
    public CompletableFuture<Integer> getTotalMined(Player player) {
        return getTotalMined(player.getUniqueId());
    }
    
    /**
     * Get the total number of ores mined by a player
     * @param uuid The player UUID
     * @return CompletableFuture with the total number of ores mined
     */
    public CompletableFuture<Integer> getTotalMined(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> getTotalMinedSync(uuid));
    }
    
    /**
     * Get the total number of ores mined by a player (synchronous)
     * @param uuid The player UUID
     * @return The total number of ores mined
     */
    public int getTotalMinedSync(UUID uuid) {
        if (!useDatabase) {
            return yamlDataManager.getTotalMined(uuid);
        }
        
        try {
            String sql = "SELECT SUM(times_mined) AS total FROM orepay_statistics WHERE uuid = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, uuid.toString());
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getInt("total");
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error getting total mined: " + e.getMessage());
        }
        
        return 0;
    }

    /**
     * Get the most mined ore by a player
     * @param player The player
     * @return CompletableFuture with the name of the most mined ore
     */
    public CompletableFuture<String> getMostMinedOre(Player player) {
        return getMostMinedOre(player.getUniqueId());
    }
    
    /**
     * Get the most mined ore by a player
     * @param uuid The player UUID
     * @return CompletableFuture with the name of the most mined ore
     */
    public CompletableFuture<String> getMostMinedOre(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> getMostMinedOreSync(uuid));
    }
    
    /**
     * Get the most mined ore by a player (synchronous)
     * @param uuid The player UUID
     * @return The name of the most mined ore
     */
    public String getMostMinedOreSync(UUID uuid) {
        if (!useDatabase) {
            return yamlDataManager.getMostMinedOre(uuid);
        }
        
        try {
            String sql = "SELECT ore FROM orepay_statistics WHERE uuid = ? ORDER BY times_mined DESC LIMIT 1";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, uuid.toString());
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getString("ore");
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error getting most mined ore: " + e.getMessage());
        }
        
        return "None";
    }
    
    /**
     * Get the number of times a player has mined a specific ore
     * @param uuid The player UUID
     * @param oreName The ore name
     * @return The number of times the ore was mined
     */
    public int getOreMinedCountSync(UUID uuid, String oreName) {
        if (!useDatabase) {
            return yamlDataManager.getOreMinedCount(uuid, oreName);
        }
        
        try {
            String sql = "SELECT times_mined FROM orepay_statistics WHERE uuid = ? AND ore = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, uuid.toString());
                statement.setString(2, oreName);
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getInt("times_mined");
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error getting ore mined count: " + e.getMessage());
        }
        
        return 0;
    }
    
    /**
     * Get the amount earned from a specific ore by a player
     * @param uuid The player UUID
     * @param oreName The ore name
     * @return The amount earned
     */
    public double getOreEarnedAmountSync(UUID uuid, String oreName) {
        if (!useDatabase) {
            return yamlDataManager.getOreEarnedAmount(uuid, oreName);
        }
        
        try {
            String sql = "SELECT amount_earned FROM orepay_statistics WHERE uuid = ? AND ore = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, uuid.toString());
                statement.setString(2, oreName);
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getDouble("amount_earned");
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error getting ore earned amount: " + e.getMessage());
        }
        
        return 0.0;
    }

    /**
     * Class to hold mining statistic data
     */
    public static class StatisticEntry {
        private final int timesMined;
        private final double amountEarned;
        
        public StatisticEntry(int timesMined, double amountEarned) {
            this.timesMined = timesMined;
            this.amountEarned = amountEarned;
        }
        
        public int getTimesMined() {
            return timesMined;
        }
        
        public double getAmountEarned() {
            return amountEarned;
        }
    }
}