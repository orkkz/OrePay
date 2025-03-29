package com.orepay.integration;

import com.orepay.OrePay;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * PlaceholderAPI integration for OrePay
 */
public class PlaceholderManager extends PlaceholderExpansion {
    
    private final OrePay plugin;
    
    public PlaceholderManager(OrePay plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getIdentifier() {
        return "orepay";
    }
    
    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }
    
    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }
    
    @Override
    public boolean persist() {
        return true;
    }
    
    @Override
    public String onRequest(OfflinePlayer player, String identifier) {
        if (player == null) {
            return "";
        }
        
        // Process the identifiers for online players
        if (player.isOnline()) {
            return processOnlinePlayerPlaceholder(player.getPlayer(), identifier);
        }
        
        // Handle placeholders for offline players
        switch (identifier.toLowerCase()) {
            case "total_earned":
                return String.format("%.2f", plugin.getDataManager().getTotalEarnedSync(player.getUniqueId()));
                
            case "total_mined":
                return String.valueOf(plugin.getDataManager().getTotalMinedSync(player.getUniqueId()));
                
            case "most_mined_ore":
                return plugin.getDataManager().getMostMinedOreSync(player.getUniqueId());
                
            case "enabled":
                return Boolean.toString(plugin.getDataManager().areRewardsEnabledSync(player.getUniqueId()));
        }
        
        // Check if it's an ore-specific placeholder
        if (identifier.startsWith("mined_")) {
            String oreName = identifier.substring(6).toUpperCase();
            return String.valueOf(plugin.getDataManager().getOreMinedCountSync(player.getUniqueId(), oreName));
        }
        
        if (identifier.startsWith("earned_")) {
            String oreName = identifier.substring(7).toUpperCase();
            return String.format("%.2f", plugin.getDataManager().getOreEarnedAmountSync(player.getUniqueId(), oreName));
        }
        
        return null; // Placeholder not found
    }
    
    /**
     * Process placeholders for online players
     * @param player The online player
     * @param identifier The placeholder identifier
     * @return The placeholder value
     */
    private String processOnlinePlayerPlaceholder(Player player, String identifier) {
        switch (identifier.toLowerCase()) {
            case "multiplier":
                return String.format("%.2f", plugin.getMultiplierManager().getMultiplier(player));
                
            case "total_earned":
                return String.format("%.2f", plugin.getDataManager().getTotalEarnedSync(player.getUniqueId()));
                
            case "total_mined":
                return String.valueOf(plugin.getDataManager().getTotalMinedSync(player.getUniqueId()));
                
            case "most_mined_ore":
                return plugin.getDataManager().getMostMinedOreSync(player.getUniqueId());
                
            case "enabled":
                return Boolean.toString(plugin.getDataManager().areRewardsEnabledSync(player));
        }
        
        // Check if it's an ore-specific placeholder
        if (identifier.startsWith("mined_")) {
            String oreName = identifier.substring(6).toUpperCase();
            return String.valueOf(plugin.getDataManager().getOreMinedCountSync(player.getUniqueId(), oreName));
        }
        
        if (identifier.startsWith("earned_")) {
            String oreName = identifier.substring(7).toUpperCase();
            return String.format("%.2f", plugin.getDataManager().getOreEarnedAmountSync(player.getUniqueId(), oreName));
        }
        
        return null; // Placeholder not found
    }
}