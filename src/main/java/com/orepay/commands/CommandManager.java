package com.orepay.commands;

import com.orepay.OrePay;
import com.orepay.commands.subcommands.HelpCommand;
import com.orepay.commands.subcommands.ReloadCommand;
import com.orepay.commands.subcommands.StatsCommand;
import com.orepay.commands.subcommands.ToggleCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages all plugin commands
 */
public class CommandManager implements CommandExecutor, TabCompleter {
    
    private final OrePay plugin;
    private final Map<String, SubCommand> commands = new HashMap<>();
    
    public CommandManager(OrePay plugin) {
        this.plugin = plugin;
        registerCommands();
    }
    
    /**
     * Register all subcommands
     */
    private void registerCommands() {
        registerCommand(new HelpCommand(plugin));
        registerCommand(new ReloadCommand(plugin));
        registerCommand(new StatsCommand(plugin));
        registerCommand(new ToggleCommand(plugin));
    }
    
    /**
     * Register a single subcommand
     * @param command The subcommand to register
     */
    private void registerCommand(SubCommand command) {
        commands.put(command.getName().toLowerCase(), command);
        for (String alias : command.getAliases()) {
            commands.put(alias.toLowerCase(), command);
        }
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            // Default to help command
            commands.get("help").execute(sender, args);
            return true;
        }
        
        String subCommandName = args[0].toLowerCase();
        SubCommand subCommand = commands.get(subCommandName);
        
        if (subCommand == null) {
            // Command not found, show help
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cUnknown command. Use /orepay help for a list of commands.");
            return true;
        }
        
        // Check permission
        if (!subCommand.hasPermission(sender)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("commands.no-permission"));
            return true;
        }
        
        // Execute the command
        String[] newArgs = new String[args.length - 1];
        System.arraycopy(args, 1, newArgs, 0, args.length - 1);
        
        subCommand.execute(sender, newArgs);
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Main commands
            String search = args[0].toLowerCase();
            for (SubCommand subCommand : commands.values()) {
                // Add unique commands only
                if (!completions.contains(subCommand.getName()) && 
                        subCommand.getName().toLowerCase().startsWith(search) && 
                        subCommand.hasPermission(sender)) {
                    completions.add(subCommand.getName());
                }
            }
        } else if (args.length > 1) {
            // Subcommand completions
            String subCommandName = args[0].toLowerCase();
            SubCommand subCommand = commands.get(subCommandName);
            
            if (subCommand != null && subCommand.hasPermission(sender)) {
                String[] newArgs = new String[args.length - 1];
                System.arraycopy(args, 1, newArgs, 0, args.length - 1);
                
                List<String> subCompletions = subCommand.getTabCompletions(sender, newArgs);
                if (subCompletions != null) {
                    completions.addAll(subCompletions);
                }
            }
        }
        
        return completions;
    }
    
    /**
     * Get all registered subcommands
     * @return Map of command names to SubCommand objects
     */
    public Map<String, SubCommand> getCommands() {
        return new HashMap<>(commands);
    }
}