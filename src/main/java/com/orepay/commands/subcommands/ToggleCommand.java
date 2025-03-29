package com.orepay.commands.subcommands;

import com.orepay.OrePay;
import com.orepay.commands.SubCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Command to toggle ore rewards
 */
public class ToggleCommand implements SubCommand {
    
    private final OrePay plugin;
    
    public ToggleCommand(OrePay plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "toggle";
    }
    
    @Override
    public String getDescription() {
        return "Toggle ore rewards for yourself or another player";
    }
    
    @Override
    public String getUsage() {
        return "/orepay toggle [player] [on|off]";
    }
    
    @Override
    public String getPermission() {
        return "orepay.command.toggle";
    }
    
    @Override
    public List<String> getAliases() {
        return List.of("switch");
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        String prefix = plugin.getConfigManager().getPrefix();
        
        if (args.length > 0 && !sender.hasPermission("orepay.command.toggle.others")) {
            sender.sendMessage(prefix + plugin.getConfigManager().getMessage("commands.no-permission"));
            return;
        }
        
        final Player target;
        final boolean setValue;
        
        if (args.length > 0 && !args[0].equalsIgnoreCase("on") && !args[0].equalsIgnoreCase("off")) {
            // Toggle for another player
            target = Bukkit.getPlayer(args[0]);
            
            if (target == null) {
                sender.sendMessage(prefix + plugin.getConfigManager().getMessage("commands.player-not-found"));
                return;
            }
            
            // Check for forced on/off value
            if (args.length > 1) {
                if (args[1].equalsIgnoreCase("on")) {
                    setValue = true;
                } else if (args[1].equalsIgnoreCase("off")) {
                    setValue = false;
                } else {
                    sender.sendMessage(prefix + plugin.getConfigManager().getMessage("commands.invalid-toggle"));
                    return;
                }
            } else {
                // Toggle the current value
                setValue = !plugin.getDataManager().areRewardsEnabledSync(target);
            }
        } else {
            // Toggle for self
            if (!(sender instanceof Player)) {
                sender.sendMessage(prefix + plugin.getConfigManager().getMessage("commands.player-only"));
                return;
            }
            
            target = (Player) sender;
            
            // Check for forced on/off value
            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("on")) {
                    setValue = true;
                } else if (args[0].equalsIgnoreCase("off")) {
                    setValue = false;
                } else {
                    sender.sendMessage(prefix + plugin.getConfigManager().getMessage("commands.invalid-toggle"));
                    return;
                }
            } else {
                // Toggle the current value
                setValue = !plugin.getDataManager().areRewardsEnabledSync(target);
            }
        }
        
        // Update the setting
        plugin.getDataManager().setRewardsEnabled(target, setValue);
        
        // Send message to the target
        String targetMessage = plugin.getConfigManager().getMessage(setValue ? "commands.toggle-on" : "commands.toggle-off");
        target.sendMessage(prefix + targetMessage);
        
        // Send message to the sender if different from target
        if (sender != target) {
            String senderMessage = plugin.getConfigManager().getMessage(setValue ? "commands.toggle-on-other" : "commands.toggle-off-other")
                    .replace("%player%", target.getName());
            sender.sendMessage(prefix + senderMessage);
        }
    }
    
    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            
            // Add on/off options
            List<String> options = Arrays.asList("on", "off");
            options.forEach(option -> {
                if (option.startsWith(args[0].toLowerCase())) {
                    completions.add(option);
                }
            });
            
            // Add player names if has permission
            if (sender.hasPermission("orepay.command.toggle.others")) {
                Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                        .forEach(completions::add);
            }
            
            return completions;
        } else if (args.length == 2 && sender.hasPermission("orepay.command.toggle.others")) {
            // Check if first arg is a player name
            if (Bukkit.getPlayer(args[0]) != null) {
                return Arrays.asList("on", "off").stream()
                        .filter(option -> option.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        
        return new ArrayList<>();
    }
}