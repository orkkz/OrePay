package com.orepay.commands;

import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

/**
 * Base interface for all subcommands
 */
public interface SubCommand {
    
    /**
     * Get the name of the subcommand
     * @return The name
     */
    String getName();
    
    /**
     * Get the description of the subcommand
     * @return The description
     */
    String getDescription();
    
    /**
     * Get the usage of the subcommand
     * @return The usage
     */
    String getUsage();
    
    /**
     * Get the permission required to use this subcommand
     * @return The permission
     */
    String getPermission();
    
    /**
     * Get aliases for this subcommand
     * @return List of aliases
     */
    default List<String> getAliases() {
        return Collections.emptyList();
    }
    
    /**
     * Check if the sender has permission to use this subcommand
     * @param sender The command sender
     * @return True if the sender has permission
     */
    default boolean hasPermission(CommandSender sender) {
        return sender.hasPermission(getPermission());
    }
    
    /**
     * Execute the subcommand
     * @param sender The command sender
     * @param args The command arguments
     */
    void execute(CommandSender sender, String[] args);
    
    /**
     * Get tab completions for this subcommand
     * @param sender The command sender
     * @param args The command arguments
     * @return List of tab completions
     */
    default List<String> getTabCompletions(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}