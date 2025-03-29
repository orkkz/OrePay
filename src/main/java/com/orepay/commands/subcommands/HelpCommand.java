package com.orepay.commands.subcommands;

import com.orepay.OrePay;
import com.orepay.commands.SubCommand;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;

/**
 * Command to display help information
 */
public class HelpCommand implements SubCommand {
    
    private final OrePay plugin;
    
    public HelpCommand(OrePay plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "help";
    }
    
    @Override
    public String getDescription() {
        return "Shows help information for OrePay commands";
    }
    
    @Override
    public String getUsage() {
        return "/orepay help";
    }
    
    @Override
    public String getPermission() {
        return "orepay.command.help";
    }
    
    @Override
    public List<String> getAliases() {
        return Arrays.asList("?", "commands");
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        String prefix = plugin.getConfigManager().getPrefix();
        
        sender.sendMessage(prefix + "§6OrePay Commands:");
        
        // Only show commands the sender has permission for
        plugin.getCommandManager().getCommands().values().stream()
                .distinct() // Avoid duplicates from aliases
                .filter(cmd -> cmd.hasPermission(sender))
                .forEach(cmd -> sender.sendMessage(
                        "§e" + cmd.getUsage() + " §7- " + cmd.getDescription()
                ));
    }
}