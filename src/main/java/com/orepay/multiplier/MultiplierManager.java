package com.orepay.multiplier;

import com.orepay.OrePay;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages mining reward multipliers
 */
public class MultiplierManager {
    
    private final OrePay plugin;
    private final Map<UUID, Double> temporaryMultipliers = new HashMap<>();
    private final Map<UUID, Long> temporaryMultiplierExpiration = new HashMap<>();
    private final Pattern multiplierPermissionPattern = Pattern.compile("orepay\\.multiplier\\.(\\d+(?:\\.\\d+)?)");
    
    public MultiplierManager(OrePay plugin) {
        this.plugin = plugin;
        startExpirationTask();
    }
    
    /**
     * Get the total multiplier for a player
     * @param player The player
     * @return The total multiplier
     */
    public double getMultiplier(Player player) {
        double baseMultiplier = plugin.getConfigManager().getDouble("multipliers.base", 1.0);
        
        // Check if multipliers are enabled
        if (!plugin.getConfigManager().getBoolean("multipliers.enabled", true)) {
            return baseMultiplier;
        }
        
        // Apply permission-based multipliers
        double permissionMultiplier = getPermissionMultiplier(player);
        
        // Apply temporary multipliers
        double temporaryMultiplier = getTemporaryMultiplier(player.getUniqueId());
        
        // Apply world-specific multipliers
        double worldMultiplier = getWorldMultiplier(player);
        
        // Get final multiplier based on stacking type
        String stackType = plugin.getConfigManager().getString("multipliers.stack-type", "add").toLowerCase();
        
        if (stackType.equals("multiply")) {
            // Multiply all multipliers together
            return baseMultiplier * permissionMultiplier * temporaryMultiplier * worldMultiplier;
        } else {
            // Add all multipliers (subtract base value first to avoid counting it multiple times)
            double permissionBonus = permissionMultiplier - 1.0;
            double temporaryBonus = temporaryMultiplier - 1.0;
            double worldBonus = worldMultiplier - 1.0;
            
            return baseMultiplier + permissionBonus + temporaryBonus + worldBonus;
        }
    }
    
    /**
     * Get the permission-based multiplier for a player
     * @param player The player
     * @return The permission-based multiplier
     */
    private double getPermissionMultiplier(Player player) {
        if (!plugin.getConfigManager().getBoolean("multipliers.permission.enabled", true)) {
            return 1.0;
        }
        
        double highestMultiplier = 1.0;
        
        // Check for permission-based multipliers
        for (PermissionAttachmentInfo permission : player.getEffectivePermissions()) {
            String permName = permission.getPermission();
            Matcher matcher = multiplierPermissionPattern.matcher(permName);
            
            if (matcher.matches() && permission.getValue()) {
                try {
                    double multiplier = Double.parseDouble(matcher.group(1));
                    highestMultiplier = Math.max(highestMultiplier, multiplier);
                } catch (NumberFormatException ignored) {
                    // Invalid multiplier format, ignore
                }
            }
        }
        
        return highestMultiplier;
    }
    
    /**
     * Set a temporary multiplier for a player
     * @param playerUUID The player UUID
     * @param multiplier The multiplier value
     * @param durationSeconds The duration in seconds (0 for permanent)
     */
    public void setTemporaryMultiplier(UUID playerUUID, double multiplier, int durationSeconds) {
        temporaryMultipliers.put(playerUUID, multiplier);
        
        if (durationSeconds > 0) {
            long expirationTime = System.currentTimeMillis() + (durationSeconds * 1000L);
            temporaryMultiplierExpiration.put(playerUUID, expirationTime);
        } else {
            // Remove expiration if duration is 0 (permanent)
            temporaryMultiplierExpiration.remove(playerUUID);
        }
    }
    
    /**
     * Remove a temporary multiplier from a player
     * @param playerUUID The player UUID
     */
    public void removeTemporaryMultiplier(UUID playerUUID) {
        temporaryMultipliers.remove(playerUUID);
        temporaryMultiplierExpiration.remove(playerUUID);
    }
    
    /**
     * Get the temporary multiplier for a player
     * @param playerUUID The player UUID
     * @return The temporary multiplier
     */
    public double getTemporaryMultiplier(UUID playerUUID) {
        if (!plugin.getConfigManager().getBoolean("multipliers.temporary.enabled", true)) {
            return 1.0;
        }
        
        return temporaryMultipliers.getOrDefault(playerUUID, 1.0);
    }
    
    /**
     * Get the world-specific multiplier for a player
     * @param player The player
     * @return The world multiplier
     */
    private double getWorldMultiplier(Player player) {
        if (!plugin.getConfigManager().getBoolean("multipliers.world.enabled", true)) {
            return 1.0;
        }
        
        String worldName = player.getWorld().getName();
        String path = "multipliers.world.worlds." + worldName;
        
        if (plugin.getConfigManager().contains(path)) {
            return plugin.getConfigManager().getDouble(path, 1.0);
        }
        
        return 1.0;
    }
    
    /**
     * Start the task that checks for expired multipliers
     */
    private void startExpirationTask() {
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long currentTime = System.currentTimeMillis();
            
            // Create a copy of the keys to avoid ConcurrentModificationException
            temporaryMultiplierExpiration.entrySet().stream()
                    .filter(entry -> currentTime > entry.getValue())
                    .map(Map.Entry::getKey)
                    .toList() // Create a copy of the keys to avoid ConcurrentModificationException
                    .forEach(uuid -> {
                        temporaryMultipliers.remove(uuid);
                        temporaryMultiplierExpiration.remove(uuid);
                    });
        }, 20L, 20L); // Check every second
    }
    
    /**
     * Get the remaining duration of a temporary multiplier in seconds
     * @param playerUUID The player UUID
     * @return The remaining duration in seconds, or -1 if permanent or not set
     */
    public int getTemporaryMultiplierRemainingDuration(UUID playerUUID) {
        if (!temporaryMultiplierExpiration.containsKey(playerUUID)) {
            if (temporaryMultipliers.containsKey(playerUUID)) {
                return -1; // Permanent multiplier
            }
            return 0; // No multiplier
        }
        
        long expirationTime = temporaryMultiplierExpiration.get(playerUUID);
        long currentTime = System.currentTimeMillis();
        
        if (expirationTime <= currentTime) {
            return 0; // Expired
        }
        
        return (int) ((expirationTime - currentTime) / 1000);
    }
}