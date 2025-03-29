package com.orepay.api.events;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Event that fires after a player has been rewarded for mining an ore
 * This event cannot be cancelled
 */
public class PlayerRewardedEvent extends Event {
    
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final Material material;
    private final double amount;
    private final double multiplier;
    
    public PlayerRewardedEvent(Player player, Material material, double amount, double multiplier) {
        this.player = player;
        this.material = material;
        this.amount = amount;
        this.multiplier = multiplier;
    }
    
    /**
     * Get the player who was rewarded
     * @return The player
     */
    public Player getPlayer() {
        return player;
    }
    
    /**
     * Get the ore material that was mined
     * @return The material
     */
    public Material getMaterial() {
        return material;
    }
    
    /**
     * Get the final reward amount (after multipliers)
     * @return The reward amount
     */
    public double getAmount() {
        return amount;
    }
    
    /**
     * Get the multiplier that was applied
     * @return The multiplier
     */
    public double getMultiplier() {
        return multiplier;
    }
    
    /**
     * Get the base reward amount (before multiplier)
     * @return The base reward amount
     */
    public double getBaseAmount() {
        return amount / multiplier;
    }
    
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
    
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}