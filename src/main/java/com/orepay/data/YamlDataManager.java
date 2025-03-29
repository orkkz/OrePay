package com.orepay.data;

import com.orepay.OrePay;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages data storage in YAML files
 */
public class YamlDataManager {
    
    private final OrePay plugin;
    private final File statisticsFile;
    private final File settingsFile;
    private FileConfiguration statisticsConfig;
    private FileConfiguration settingsConfig;
    
    public YamlDataManager(OrePay plugin) {
        this.plugin = plugin;
        
        this.statisticsFile = new File(plugin.getDataFolder(), "statistics.yml");
        this.settingsFile = new File(plugin.getDataFolder(), "settings.yml");
        
        // Create files if they don't exist
        createFiles();
        
        // Load the configurations
        this.statisticsConfig = YamlConfiguration.loadConfiguration(statisticsFile);
        this.settingsConfig = YamlConfiguration.loadConfiguration(settingsFile);
    }
    
    /**
     * Create data files if they don't exist
     */
    private void createFiles() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        if (!statisticsFile.exists()) {
            try {
                statisticsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Error creating statistics.yml: " + e.getMessage());
            }
        }
        
        if (!settingsFile.exists()) {
            try {
                settingsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Error creating settings.yml: " + e.getMessage());
            }
        }
    }
    
    /**
     * Save the statistics configuration
     */
    private void saveStatistics() {
        try {
            statisticsConfig.save(statisticsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Error saving statistics.yml: " + e.getMessage());
        }
    }
    
    /**
     * Save the settings configuration
     */
    private void saveSettings() {
        try {
            settingsConfig.save(settingsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Error saving settings.yml: " + e.getMessage());
        }
    }
    
    /**
     * Record a mining statistic
     * @param uuid The player UUID
     * @param oreName The ore name
     * @param amount The amount earned
     */
    public void recordMiningStatistic(UUID uuid, String oreName, double amount) {
        String playerPath = "players." + uuid.toString() + ".ores." + oreName;
        
        // Update times mined
        int timesMined = statisticsConfig.getInt(playerPath + ".times-mined", 0) + 1;
        statisticsConfig.set(playerPath + ".times-mined", timesMined);
        
        // Update amount earned
        double amountEarned = statisticsConfig.getDouble(playerPath + ".amount-earned", 0.0) + amount;
        statisticsConfig.set(playerPath + ".amount-earned", amountEarned);
        
        // Save the changes
        saveStatistics();
    }
    
    /**
     * Get all statistics for a player
     * @param uuid The player UUID
     * @return Map of ore names to StatisticEntry objects
     */
    public Map<String, DatabaseManager.StatisticEntry> getPlayerStatistics(UUID uuid) {
        Map<String, DatabaseManager.StatisticEntry> statistics = new HashMap<>();
        
        String playerPath = "players." + uuid.toString() + ".ores";
        ConfigurationSection oresSection = statisticsConfig.getConfigurationSection(playerPath);
        
        if (oresSection != null) {
            for (String oreName : oresSection.getKeys(false)) {
                int timesMined = statisticsConfig.getInt(playerPath + "." + oreName + ".times-mined", 0);
                double amountEarned = statisticsConfig.getDouble(playerPath + "." + oreName + ".amount-earned", 0.0);
                
                statistics.put(oreName, new DatabaseManager.StatisticEntry(timesMined, amountEarned));
            }
        }
        
        return statistics;
    }
    
    /**
     * Get the total amount earned by a player
     * @param uuid The player UUID
     * @return The total amount earned
     */
    public double getTotalEarned(UUID uuid) {
        Map<String, DatabaseManager.StatisticEntry> statistics = getPlayerStatistics(uuid);
        return statistics.values().stream().mapToDouble(DatabaseManager.StatisticEntry::getAmountEarned).sum();
    }
    
    /**
     * Get the total number of ores mined by a player
     * @param uuid The player UUID
     * @return The total number of ores mined
     */
    public int getTotalMined(UUID uuid) {
        Map<String, DatabaseManager.StatisticEntry> statistics = getPlayerStatistics(uuid);
        return statistics.values().stream().mapToInt(DatabaseManager.StatisticEntry::getTimesMined).sum();
    }
    
    /**
     * Get the most mined ore by a player
     * @param uuid The player UUID
     * @return The name of the most mined ore
     */
    public String getMostMinedOre(UUID uuid) {
        Map<String, DatabaseManager.StatisticEntry> statistics = getPlayerStatistics(uuid);
        
        String mostMinedOre = "None";
        int highestCount = 0;
        
        for (Map.Entry<String, DatabaseManager.StatisticEntry> entry : statistics.entrySet()) {
            if (entry.getValue().getTimesMined() > highestCount) {
                highestCount = entry.getValue().getTimesMined();
                mostMinedOre = entry.getKey();
            }
        }
        
        return mostMinedOre;
    }
    
    /**
     * Get the number of times a player has mined a specific ore
     * @param uuid The player UUID
     * @param oreName The ore name
     * @return The number of times the ore was mined
     */
    public int getOreMinedCount(UUID uuid, String oreName) {
        String path = "players." + uuid.toString() + ".ores." + oreName + ".times-mined";
        return statisticsConfig.getInt(path, 0);
    }
    
    /**
     * Get the amount earned from a specific ore by a player
     * @param uuid The player UUID
     * @param oreName The ore name
     * @return The amount earned
     */
    public double getOreEarnedAmount(UUID uuid, String oreName) {
        String path = "players." + uuid.toString() + ".ores." + oreName + ".amount-earned";
        return statisticsConfig.getDouble(path, 0.0);
    }
    
    /**
     * Check if rewards are enabled for a player
     * @param uuid The player UUID
     * @return True if rewards are enabled, false otherwise
     */
    public boolean areRewardsEnabled(UUID uuid) {
        String path = "players." + uuid.toString() + ".rewards-enabled";
        
        // If not set, default to true
        if (!settingsConfig.contains(path)) {
            settingsConfig.set(path, true);
            saveSettings();
            return true;
        }
        
        return settingsConfig.getBoolean(path);
    }
    
    /**
     * Set whether rewards are enabled for a player
     * @param uuid The player UUID
     * @param enabled Whether rewards should be enabled
     */
    public void setRewardsEnabled(UUID uuid, boolean enabled) {
        String path = "players." + uuid.toString() + ".rewards-enabled";
        settingsConfig.set(path, enabled);
        saveSettings();
    }
}