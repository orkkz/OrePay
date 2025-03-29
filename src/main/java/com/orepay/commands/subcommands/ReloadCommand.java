package com.orepay.commands.subcommands;

import com.orepay.OrePay;
import com.orepay.commands.SubCommand;
import org.bukkit.command.CommandSender;

/**
 * Command to reload the plugin configuration
 */
public class ReloadCommand implements SubCommand {
    
    private final OrePay plugin;
    
    public ReloadCommand(OrePay plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "reload";
    }
    
    @Override
    public String getDescription() {
        return "Reloads the plugin configuration";
    }
    
    @Override
    public String getUsage() {
        return "/orepay reload";
    }
    
    @Override
    public String getPermission() {
        return "orepay.command.reload";
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        long startTime = System.currentTimeMillis();
        
        // Reload config
        plugin.getConfigManager().reloadConfig();
        
        long timeTaken = System.currentTimeMillis() - startTime;
        String message = plugin.getConfigManager().getMessage("commands.reload-success")
                .replace("%time%", String.valueOf(timeTaken));
        
        sender.sendMessage(plugin.getConfigManager().getPrefix() + message);
    }
}