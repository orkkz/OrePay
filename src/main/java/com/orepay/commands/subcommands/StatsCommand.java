package com.orepay.commands.subcommands;

import com.orepay.OrePay;
import com.orepay.commands.SubCommand;
import com.orepay.data.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Command to view mining statistics
 */
public class StatsCommand implements SubCommand {
    
    private final OrePay plugin;
    
    public StatsCommand(OrePay plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "stats";
    }
    
    @Override
    public String getDescription() {
        return "View mining statistics for yourself or another player";
    }
    
    @Override
    public String getUsage() {
        return "/orepay stats [player]";
    }
    
    @Override
    public String getPermission() {
        return "orepay.command.stats";
    }
    
    @Override
    public List<String> getAliases() {
        return List.of("statistics");
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        String prefix = plugin.getConfigManager().getPrefix();
        
        if (args.length > 0 && !sender.hasPermission("orepay.command.stats.others")) {
            sender.sendMessage(prefix + plugin.getConfigManager().getMessage("commands.no-permission"));
            return;
        }
        
        final Player target;
        
        if (args.length > 0) {
            // Looking up stats for another player
            target = Bukkit.getPlayer(args[0]);
            
            if (target == null) {
                // Try to lookup by UUID for offline players
                try {
                    UUID uuid = UUID.fromString(args[0]);
                    String playerName = args[0]; // Default to UUID string if name not found
                    
                    // Fetch stats for offline player
                    fetchAndDisplayStats(sender, uuid, playerName);
                    return;
                } catch (IllegalArgumentException e) {
                    // Not a valid UUID, must be an invalid player name
                    sender.sendMessage(prefix + plugin.getConfigManager().getMessage("commands.player-not-found"));
                    return;
                }
            }
        } else {
            // Stats for self
            if (!(sender instanceof Player)) {
                sender.sendMessage(prefix + plugin.getConfigManager().getMessage("commands.player-only"));
                return;
            }
            
            target = (Player) sender;
        }
        
        // Display stats for online player
        fetchAndDisplayStats(sender, target.getUniqueId(), target.getName());
    }
    
    private void fetchAndDisplayStats(CommandSender sender, UUID playerUUID, String playerName) {
        String prefix = plugin.getConfigManager().getPrefix();
        
        plugin.getDataManager().getPlayerStatistics(playerUUID).thenAccept(stats -> {
            if (stats.isEmpty()) {
                sender.sendMessage(prefix + plugin.getConfigManager().getMessage("commands.no-stats"));
                return;
            }
            
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                double totalEarned = stats.values().stream().mapToDouble(DatabaseManager.StatisticEntry::getAmountEarned).sum();
                int totalMined = stats.values().stream().mapToInt(DatabaseManager.StatisticEntry::getTimesMined).sum();
                
                // Find most mined ore
                Map.Entry<String, DatabaseManager.StatisticEntry> mostMinedEntry = null;
                for (Map.Entry<String, DatabaseManager.StatisticEntry> entry : stats.entrySet()) {
                    if (mostMinedEntry == null || entry.getValue().getTimesMined() > mostMinedEntry.getValue().getTimesMined()) {
                        mostMinedEntry = entry;
                    }
                }
                
                // Display stats
                List<String> messages = plugin.getConfigManager().getStringList("messages.stats-header");
                for (String message : messages) {
                    sender.sendMessage(message
                            .replace("%player%", playerName)
                            .replace("%total_earned%", String.format("%.2f", totalEarned))
                            .replace("%total_mined%", String.valueOf(totalMined))
                            .replace("%currency%", plugin.getEconomy().currencyNamePlural())
                    );
                }
                
                // Display per-ore stats
                for (Map.Entry<String, DatabaseManager.StatisticEntry> entry : stats.entrySet()) {
                    String oreName = entry.getKey();
                    DatabaseManager.StatisticEntry stat = entry.getValue();
                    
                    try {
                        Material material = Material.valueOf(oreName);
                        oreName = material.name().toLowerCase().replace('_', ' ');
                    } catch (IllegalArgumentException e) {
                        // Not a valid material, just use the name as is
                    }
                    
                    String statLine = plugin.getConfigManager().getMessage("messages.stats-line")
                            .replace("%ore%", oreName)
                            .replace("%amount%", String.valueOf(stat.getTimesMined()))
                            .replace("%earned%", String.format("%.2f", stat.getAmountEarned()))
                            .replace("%currency%", plugin.getEconomy().currencyNamePlural());
                    
                    sender.sendMessage(statLine);
                }
            });
        });
    }
    
    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        if (args.length == 1 && sender.hasPermission("orepay.command.stats.others")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        return new ArrayList<>();
    }
}