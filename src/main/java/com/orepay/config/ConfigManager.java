package com.orepay.config;

import com.orepay.OrePay;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manages the plugin configuration
 */
public class ConfigManager {
    
    private final OrePay plugin;
    private FileConfiguration config;
    private final Map<Material, Double> oreRewards = new HashMap<>();
    
    public ConfigManager(OrePay plugin) {
        this.plugin = plugin;
        reloadConfig();
    }
    
    /**
     * Reload the configuration
     */
    public void reloadConfig() {
        // Save default config if it doesn't exist
        plugin.saveDefaultConfig();
        
        // Reload the config
        plugin.reloadConfig();
        config = plugin.getConfig();
        
        // Load ore rewards
        loadOreRewards();
    }
    
    /**
     * Load ore rewards from the configuration
     */
    private void loadOreRewards() {
        oreRewards.clear();
        
        ConfigurationSection oresSection = config.getConfigurationSection("rewards");
        if (oresSection == null) {
            plugin.getLogger().warning("No ore rewards found in config.yml");
            return;
        }
        
        for (String key : oresSection.getKeys(false)) {
            try {
                Material material = Material.valueOf(key.toUpperCase());
                double reward = oresSection.getDouble(key);
                
                if (reward > 0) {
                    oreRewards.put(material, reward);
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid material name in config.yml: " + key);
            }
        }
        
        plugin.getLogger().info("Loaded " + oreRewards.size() + " ore rewards");
    }
    
    /**
     * Get a map of all ore rewards
     * @return Map of material to reward amount
     */
    public Map<Material, Double> getOreRewards() {
        return Collections.unmodifiableMap(oreRewards);
    }
    
    /**
     * Get the reward amount for an ore
     * @param material The ore material
     * @return The reward amount (0 if not found)
     */
    public double getRewardForOre(Material material) {
        return oreRewards.getOrDefault(material, 0.0);
    }
    
    /**
     * Get a value from the config
     * @param path The config path
     * @return The value, or null if not found
     */
    public Object get(String path) {
        return config.get(path);
    }
    
    /**
     * Check if a path exists in the config
     * @param path The config path
     * @return True if the path exists
     */
    public boolean contains(String path) {
        return config.contains(path);
    }
    
    /**
     * Get a string from the config
     * @param path The config path
     * @param defaultValue The default value
     * @return The string value, or defaultValue if not found
     */
    public String getString(String path, String defaultValue) {
        return config.getString(path, defaultValue);
    }
    
    /**
     * Get an integer from the config
     * @param path The config path
     * @param defaultValue The default value
     * @return The integer value, or defaultValue if not found
     */
    public int getInt(String path, int defaultValue) {
        return config.getInt(path, defaultValue);
    }
    
    /**
     * Get a double from the config
     * @param path The config path
     * @param defaultValue The default value
     * @return The double value, or defaultValue if not found
     */
    public double getDouble(String path, double defaultValue) {
        return config.getDouble(path, defaultValue);
    }
    
    /**
     * Get a boolean from the config
     * @param path The config path
     * @param defaultValue The default value
     * @return The boolean value, or defaultValue if not found
     */
    public boolean getBoolean(String path, boolean defaultValue) {
        return config.getBoolean(path, defaultValue);
    }
    
    /**
     * Get a long from the config
     * @param path The config path
     * @param defaultValue The default value
     * @return The long value, or defaultValue if not found
     */
    public long getLong(String path, long defaultValue) {
        return config.getLong(path, defaultValue);
    }
    
    /**
     * Get a list of strings from the config
     * @param path The config path
     * @return The string list, or empty list if not found
     */
    public List<String> getStringList(String path) {
        return config.getStringList(path);
    }
    
    /**
     * Get a message from the config with colors translated
     * @param path The config path
     * @return The message with colors, or a default error message if not found
     */
    public String getMessage(String path) {
        String message = config.getString(path);
        
        if (message == null) {
            plugin.getLogger().warning("Missing message in config: " + path);
            return ChatColor.RED + "Missing message: " + path;
        }
        
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    /**
     * Get the plugin prefix
     * @return The plugin prefix with colors
     */
    public String getPrefix() {
        return ChatColor.translateAlternateColorCodes('&', config.getString("prefix", "&7[&6OrePay&7] "));
    }
}