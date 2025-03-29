package com.orepay.listeners;

import com.orepay.OrePay;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MiningListener implements Listener {
    
    private final OrePay plugin;
    private final Map<UUID, Long> lastMiningTimes = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> veinCounter = new ConcurrentHashMap<>();
    
    // Vein mining detection constants
    private static final long VEIN_MINING_TIMEOUT_MS = 500; // 500ms timeout for vein mining detection
    
    public MiningListener(OrePay plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        
        // Skip if player doesn't have permission
        if (!player.hasPermission("orepay.earn")) {
            return;
        }
        
        // Check if the broken block is an ore
        if (!isOre(block.getType())) {
            return;
        }
        
        // Handle vein mining
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        // Check if this is part of a vein mining operation
        if (isPartOfVeinMining(playerId, currentTime)) {
            // Increment counter for this vein mining operation
            veinCounter.put(playerId, veinCounter.getOrDefault(playerId, 0) + 1);
        } else {
            // Reset counter for new mining operation
            veinCounter.put(playerId, 1);
        }
        
        // Update last mining time
        lastMiningTimes.put(playerId, currentTime);
        
        // Reward player
        rewardPlayer(player, block.getType());
    }
    
    /**
     * Checks if a block break is part of an ongoing vein mining operation
     * @param playerId The player's UUID
     * @param currentTime Current system time in milliseconds
     * @return true if part of vein mining, false otherwise
     */
    private boolean isPartOfVeinMining(UUID playerId, long currentTime) {
        if (!lastMiningTimes.containsKey(playerId)) {
            return false;
        }
        
        long lastMiningTime = lastMiningTimes.get(playerId);
        return (currentTime - lastMiningTime) < VEIN_MINING_TIMEOUT_MS;
    }
    
    /**
     * Rewards a player for mining an ore
     * @param player The player to reward
     * @param material The material (ore) that was mined
     */
    private void rewardPlayer(Player player, Material material) {
        double reward = plugin.getConfigManager().getRewardForOre(material);
        
        if (reward <= 0) {
            return;
        }
        
        Economy economy = plugin.getEconomy();
        if (economy != null) {
            economy.depositPlayer(player, reward);
            
            // Show message if configured
            if (plugin.getConfigManager().shouldShowRewardMessages()) {
                String message = plugin.getConfigManager().getRewardMessage()
                        .replace("{amount}", String.format("%.2f", reward))
                        .replace("{ore}", formatOreName(material.name()));
                player.sendMessage(message);
            }
        }
    }
    
    /**
     * Checks if a material is an ore
     * @param material The material to check
     * @return true if it's an ore, false otherwise
     */
    private boolean isOre(Material material) {
        return plugin.getConfigManager().isConfiguredOre(material);
    }
    
    /**
     * Formats the ore name for display
     * @param oreName The raw ore name
     * @return Formatted ore name
     */
    private String formatOreName(String oreName) {
        oreName = oreName.replace("_", " ").toLowerCase();
        StringBuilder formatted = new StringBuilder();
        boolean capitalizeNext = true;
        
        for (char c : oreName.toCharArray()) {
            if (capitalizeNext && Character.isLetter(c)) {
                formatted.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                formatted.append(c);
                if (c == ' ') {
                    capitalizeNext = true;
                }
            }
        }
        
        return formatted.toString();
    }
    
    /**
     * Cleans up expired vein mining data
     */
    public void cleanupExpiredData() {
        long currentTime = System.currentTimeMillis();
        lastMiningTimes.entrySet().removeIf(entry -> 
            (currentTime - entry.getValue()) > VEIN_MINING_TIMEOUT_MS);
        
        // Players with no recent mining activity should have vein counters cleared
        veinCounter.keySet().removeIf(playerId -> !lastMiningTimes.containsKey(playerId));
    }
}
