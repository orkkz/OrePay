package com.orepay.api.events;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Event that fires when a player mines an ore
 * Can be cancelled to prevent rewards
 */
public class OreMinedEvent extends Event implements Cancellable {
    
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final Material material;
    private double reward;
    private boolean cancelled;
    
    public OreMinedEvent(Player player, Material material, double reward) {
        this.player = player;
        this.material = material;
        this.reward = reward;
        this.cancelled = false;
    }
    
    /**
     * Get the player who mined the ore
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
     * Get the base reward amount (before multipliers)
     * @return The reward amount
     */
    public double getReward() {
        return reward;
    }
    
    /**
     * Set the base reward amount (before multipliers)
     * @param reward The new reward amount
     */
    public void setReward(double reward) {
        this.reward = reward;
    }
    
    @Override
    public boolean isCancelled() {
        return cancelled;
    }
    
    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
    
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
    
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}