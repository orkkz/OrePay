package com.orepay.config;

import com.orepay.OrePay;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

public class ConfigManager {
    
    private final OrePay plugin;
    private final Map<Material, Double> oreRewards = new HashMap<>();
    private boolean showRewardMessages;
    private String rewardMessage;
    
    public ConfigManager(OrePay plugin) {
        this.plugin = plugin;
        reloadConfig();
    }
    
    /**
     * Reloads the configuration from file
     */
    public void reloadConfig() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();
        
        // Clear previous rewards
        oreRewards.clear();
        
        // Load ore rewards
        if (config.contains("ore-rewards")) {
            for (String key : config.getConfigurationSection("ore-rewards").getKeys(false)) {
                try {
                    Material material = Material.valueOf(key.toUpperCase(Locale.ENGLISH));
                    double reward = config.getDouble("ore-rewards." + key);
                    oreRewards.put(material, reward);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid material name in config: " + key);
                }
            }
        }
        
        // Load settings
        showRewardMessages = config.getBoolean("settings.show-reward-messages", true);
        rewardMessage = config.getString("settings.reward-message", "§a+{amount} coins §7(mined {ore})");
        
        // Log loaded rewards
        Logger logger = plugin.getLogger();
        logger.info("Loaded " + oreRewards.size() + " ore reward configurations");
    }
    
    /**
     * Gets the reward amount for a given ore
     * @param material The ore material
     * @return The reward amount, or 0 if no reward is configured
     */
    public double getRewardForOre(Material material) {
        return oreRewards.getOrDefault(material, 0.0);
    }
    
    /**
     * Checks if a material is configured as an ore in the config
     * @param material The material to check
     * @return true if it's a configured ore, false otherwise
     */
    public boolean isConfiguredOre(Material material) {
        return oreRewards.containsKey(material);
    }
    
    /**
     * Checks if reward messages should be shown
     * @return true if messages should be shown, false otherwise
     */
    public boolean shouldShowRewardMessages() {
        return showRewardMessages;
    }
    
    /**
     * Gets the reward message template
     * @return The reward message template
     */
    public String getRewardMessage() {
        return rewardMessage;
    }
    
    /**
     * Gets the map of all configured ore rewards
     * @return Map of ore materials to reward amounts
     */
    public Map<Material, Double> getAllOreRewards() {
        return new HashMap<>(oreRewards);
    }
}
