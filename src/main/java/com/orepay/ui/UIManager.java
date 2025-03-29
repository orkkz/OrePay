package com.orepay.ui;

import com.orepay.OrePay;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Manages UI-related functionality, including notifications
 */
public class UIManager {
    
    private final OrePay plugin;
    
    public UIManager(OrePay plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Send a mining reward notification to a player using their preferred format
     * @param player The player
     * @param material The ore material
     * @param amount The reward amount
     */
    public void sendRewardNotification(Player player, Material material, double amount) {
        String notifyType = plugin.getConfigManager().getString("notifications.type", "chat").toLowerCase();
        
        switch (notifyType) {
            case "actionbar":
                sendActionBar(player, material, amount);
                break;
            case "title":
                sendTitle(player, material, amount);
                break;
            case "subtitle":
                sendSubtitle(player, material, amount);
                break;
            case "none":
                // No notification
                break;
            case "chat":
            default:
                sendChatMessage(player, material, amount);
                break;
        }
    }
    
    /**
     * Send a mining reward notification to a player via chat
     * @param player The player
     * @param material The ore material
     * @param amount The reward amount
     */
    private void sendChatMessage(Player player, Material material, double amount) {
        String message = plugin.getConfigManager().getMessage("notifications.chat")
                .replace("%amount%", String.format("%.2f", amount))
                .replace("%ore%", formatOreName(material))
                .replace("%currency%", plugin.getEconomy().currencyNamePlural());
        
        player.sendMessage(plugin.getConfigManager().getPrefix() + message);
    }
    
    /**
     * Send a mining reward notification to a player via action bar
     * @param player The player
     * @param material The ore material
     * @param amount The reward amount
     */
    private void sendActionBar(Player player, Material material, double amount) {
        String message = plugin.getConfigManager().getMessage("notifications.actionbar")
                .replace("%amount%", String.format("%.2f", amount))
                .replace("%ore%", formatOreName(material))
                .replace("%currency%", plugin.getEconomy().currencyNamePlural());
        
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
    }
    
    /**
     * Send a mining reward notification to a player via title
     * @param player The player
     * @param material The ore material
     * @param amount The reward amount
     */
    private void sendTitle(Player player, Material material, double amount) {
        String title = plugin.getConfigManager().getMessage("notifications.title")
                .replace("%amount%", String.format("%.2f", amount))
                .replace("%ore%", formatOreName(material))
                .replace("%currency%", plugin.getEconomy().currencyNamePlural());
        
        String subtitle = plugin.getConfigManager().getMessage("notifications.subtitle")
                .replace("%amount%", String.format("%.2f", amount))
                .replace("%ore%", formatOreName(material))
                .replace("%currency%", plugin.getEconomy().currencyNamePlural());
        
        int fadeIn = plugin.getConfigManager().getInt("notifications.title-fade-in", 5);
        int stay = plugin.getConfigManager().getInt("notifications.title-stay", 20);
        int fadeOut = plugin.getConfigManager().getInt("notifications.title-fade-out", 5);
        
        player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
    }
    
    /**
     * Send a mining reward notification to a player via subtitle only
     * @param player The player
     * @param material The ore material
     * @param amount The reward amount
     */
    private void sendSubtitle(Player player, Material material, double amount) {
        String subtitle = plugin.getConfigManager().getMessage("notifications.subtitle")
                .replace("%amount%", String.format("%.2f", amount))
                .replace("%ore%", formatOreName(material))
                .replace("%currency%", plugin.getEconomy().currencyNamePlural());
        
        int fadeIn = plugin.getConfigManager().getInt("notifications.title-fade-in", 5);
        int stay = plugin.getConfigManager().getInt("notifications.title-stay", 20);
        int fadeOut = plugin.getConfigManager().getInt("notifications.title-fade-out", 5);
        
        player.sendTitle("", subtitle, fadeIn, stay, fadeOut);
    }
    
    /**
     * Format ore name for display
     * @param material The ore material
     * @return Formatted ore name
     */
    private String formatOreName(Material material) {
        String oreName = material.name().toLowerCase();
        oreName = oreName.replace('_', ' ');
        
        // Capitalize first letter of each word
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        
        for (char c : oreName.toCharArray()) {
            if (c == ' ') {
                capitalizeNext = true;
                result.append(c);
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }
        
        return result.toString();
    }
}