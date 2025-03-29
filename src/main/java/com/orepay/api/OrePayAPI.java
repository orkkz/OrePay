package com.orepay.api;

import com.orepay.OrePay;
import com.orepay.api.events.OreMinedEvent;
import com.orepay.api.events.PlayerRewardedEvent;
import com.orepay.data.DatabaseManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Public API for OrePay plugin
 * Allows other plugins to interact with OrePay
 */
public class OrePayAPI {
    
    private static OrePay plugin;
    
    /**
     * Initialize the API with the plugin instance
     * @param plugin The OrePay plugin instance
     */
    public static void initialize(OrePay plugin) {
        OrePayAPI.plugin = plugin;
    }
    
    /**
     * Get the reward amount for a specific ore
     * @param material The ore material
     * @return The reward amount
     */
    public static double getRewardForOre(Material material) {
        ensureInitialized();
        return plugin.getConfigManager().getRewardForOre(material);
    }
    
    /**
     * Get the multiplier for a player
     * @param player The player
     * @return The player's current multiplier
     */
    public static double getPlayerMultiplier(Player player) {
        ensureInitialized();
        return plugin.getMultiplierManager().getMultiplier(player);
    }
    
    /**
     * Check if rewards are enabled for a player
     * @param player The player to check
     * @return CompletableFuture with true if rewards are enabled, false otherwise
     */
    public static CompletableFuture<Boolean> areRewardsEnabled(Player player) {
        ensureInitialized();
        return plugin.getDataManager().areRewardsEnabled(player);
    }
    
    /**
     * Set whether rewards are enabled for a player
     * @param player The player
     * @param enabled Whether rewards should be enabled
     */
    public static void setRewardsEnabled(Player player, boolean enabled) {
        ensureInitialized();
        plugin.getDataManager().setRewardsEnabled(player, enabled);
    }
    
    /**
     * Get all statistics for a player
     * @param player The player
     * @return CompletableFuture with map of ore names to StatisticEntry objects
     */
    public static CompletableFuture<Map<String, DatabaseManager.StatisticEntry>> getPlayerStatistics(Player player) {
        ensureInitialized();
        return plugin.getDataManager().getPlayerStatistics(player);
    }
    
    /**
     * Get the total amount earned by a player
     * @param player The player
     * @return CompletableFuture with the total amount earned
     */
    public static CompletableFuture<Double> getTotalEarned(Player player) {
        ensureInitialized();
        return plugin.getDataManager().getTotalEarned(player);
    }
    
    /**
     * Get the total number of ores mined by a player
     * @param player The player
     * @return CompletableFuture with the total number of ores mined
     */
    public static CompletableFuture<Integer> getTotalMined(Player player) {
        ensureInitialized();
        return plugin.getDataManager().getTotalMined(player);
    }
    
    /**
     * Get the most commonly mined ore by a player
     * @param player The player
     * @return CompletableFuture with the name of the most mined ore
     */
    public static CompletableFuture<String> getMostMinedOre(Player player) {
        ensureInitialized();
        return plugin.getDataManager().getMostMinedOre(player);
    }
    
    /**
     * Manually reward a player for mining an ore
     * This will trigger the PlayerRewardedEvent
     * @param player The player to reward
     * @param material The ore material
     * @param amount The amount to reward (before multipliers)
     * @return The actual amount rewarded (after multipliers)
     */
    public static double rewardPlayer(Player player, Material material, double amount) {
        ensureInitialized();
        
        // Fire OreMinedEvent first (cancellable)
        OreMinedEvent minedEvent = new OreMinedEvent(player, material, amount);
        plugin.getServer().getPluginManager().callEvent(minedEvent);
        
        if (minedEvent.isCancelled()) {
            return 0;
        }
        
        // Apply multiplier
        double multiplier = plugin.getMultiplierManager().getMultiplier(player);
        double finalAmount = minedEvent.getReward() * multiplier;
        
        // Give reward
        plugin.getEconomy().depositPlayer(player, finalAmount);
        
        // Record statistic
        plugin.getDataManager().recordMiningStatistic(player, material, finalAmount);
        
        // Fire PlayerRewardedEvent
        PlayerRewardedEvent rewardedEvent = new PlayerRewardedEvent(player, material, finalAmount, multiplier);
        plugin.getServer().getPluginManager().callEvent(rewardedEvent);
        
        return finalAmount;
    }
    
    /**
     * Check if the API is initialized
     * @throws IllegalStateException if not initialized
     */
    private static void ensureInitialized() {
        if (plugin == null) {
            throw new IllegalStateException("OrePayAPI has not been initialized!");
        }
    }
}