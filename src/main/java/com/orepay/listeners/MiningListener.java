package com.orepay.listeners;

import com.orepay.OrePay;
import com.orepay.api.OrePayAPI;
import com.orepay.api.events.OreMinedEvent;
import com.orepay.api.events.PlayerRewardedEvent;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Listener for mining events
 */
public class MiningListener implements Listener {
    
    private final OrePay plugin;
    
    // Maps to track vein mining
    private final Map<UUID, Long> lastMiningTime = new HashMap<>();
    private final Map<UUID, Material> lastMinedOre = new HashMap<>();
    
    public MiningListener(OrePay plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material material = block.getType();
        
        // Check if this is an ore
        if (!isOre(material)) {
            return;
        }
        
        // Check if the player has permission to receive rewards
        if (!player.hasPermission("orepay.earn")) {
            return;
        }
        
        // Check if the player has rewards enabled
        if (!plugin.getDataManager().areRewardsEnabledSync(player)) {
            return;
        }
        
        // Get the reward amount
        double rewardAmount = plugin.getConfigManager().getRewardForOre(material);
        
        // Skip if reward is 0 or negative
        if (rewardAmount <= 0) {
            return;
        }
        
        // Check for vein mining
        if (isVeinMining(player, material)) {
            // Apply vein mining multiplier if enabled
            if (plugin.getConfigManager().getBoolean("vein-mining.enable-multiplier", true)) {
                double veinMultiplier = plugin.getConfigManager().getDouble("vein-mining.multiplier", 0.5);
                rewardAmount *= veinMultiplier;
            }
        }
        
        // Update vein mining tracking
        updateVeinMiningStatus(player, material);
        
        // Fire OreMinedEvent (cancellable)
        OreMinedEvent minedEvent = new OreMinedEvent(player, material, rewardAmount);
        plugin.getServer().getPluginManager().callEvent(minedEvent);
        
        if (minedEvent.isCancelled()) {
            return;
        }
        
        // Apply multiplier
        double multiplier = plugin.getMultiplierManager().getMultiplier(player);
        double finalAmount = minedEvent.getReward() * multiplier;
        
        // If amount is too small due to multipliers, skip
        if (finalAmount < plugin.getConfigManager().getDouble("minimum-payout", 0.01)) {
            return;
        }
        
        // Give reward
        plugin.getEconomy().depositPlayer(player, finalAmount);
        
        // Record statistic
        plugin.getDataManager().recordMiningStatistic(player, material, finalAmount);
        
        // Send notification
        plugin.getUiManager().sendRewardNotification(player, material, finalAmount);
        
        // Fire PlayerRewardedEvent
        PlayerRewardedEvent rewardedEvent = new PlayerRewardedEvent(player, material, finalAmount, multiplier);
        plugin.getServer().getPluginManager().callEvent(rewardedEvent);
    }
    
    /**
     * Check if a material is an ore with a configured reward
     * @param material The material to check
     * @return True if the material is a rewarded ore
     */
    private boolean isOre(Material material) {
        return plugin.getConfigManager().getRewardForOre(material) > 0;
    }
    
    /**
     * Check if the player is vein mining
     * @param player The player
     * @param material The material being mined
     * @return True if the player is vein mining
     */
    private boolean isVeinMining(Player player, Material material) {
        // Check if vein mining detection is enabled
        if (!plugin.getConfigManager().getBoolean("vein-mining.detection-enabled", true)) {
            return false;
        }
        
        UUID uuid = player.getUniqueId();
        
        // Check if player has mined recently
        if (!lastMiningTime.containsKey(uuid) || !lastMinedOre.containsKey(uuid)) {
            return false;
        }
        
        // Check if the ore is the same as last time
        if (lastMinedOre.get(uuid) != material) {
            return false;
        }
        
        // Check if within timeout period
        long timeout = plugin.getConfigManager().getLong("vein-mining.timeout-ticks", 15L);
        long currentTime = plugin.getServer().getCurrentTick();
        long lastTime = lastMiningTime.get(uuid);
        
        return (currentTime - lastTime) <= timeout;
    }
    
    /**
     * Update vein mining status for a player
     * @param player The player
     * @param material The material being mined
     */
    private void updateVeinMiningStatus(Player player, Material material) {
        UUID uuid = player.getUniqueId();
        long currentTime = plugin.getServer().getCurrentTick();
        
        lastMiningTime.put(uuid, currentTime);
        lastMinedOre.put(uuid, material);
    }
}