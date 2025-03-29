package com.orepay;

import com.orepay.api.OrePayAPI;
import com.orepay.commands.CommandManager;
import com.orepay.config.ConfigManager;
import com.orepay.data.DatabaseManager;
import com.orepay.integration.PlaceholderManager;
import com.orepay.listeners.MiningListener;
import com.orepay.multiplier.MultiplierManager;
import com.orepay.ui.UIManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for OrePay
 */
public class OrePay extends JavaPlugin {

    private Economy economy;
    private ConfigManager configManager;
    private DatabaseManager dataManager;
    private MultiplierManager multiplierManager;
    private UIManager uiManager;
    private CommandManager commandManager;
    
    @Override
    public void onEnable() {
        // Setup configuration
        this.configManager = new ConfigManager(this);
        
        // Setup vault economy
        if (!setupEconomy()) {
            getLogger().severe("Vault dependency not found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Setup managers
        this.dataManager = new DatabaseManager(this);
        this.multiplierManager = new MultiplierManager(this);
        this.uiManager = new UIManager(this);
        this.commandManager = new CommandManager(this);
        
        // Register commands
        getCommand("orepay").setExecutor(commandManager);
        getCommand("orepay").setTabCompleter(commandManager);
        
        // Register event listeners
        getServer().getPluginManager().registerEvents(new MiningListener(this), this);
        
        // Setup API
        OrePayAPI.initialize(this);
        
        // Check for PlaceholderAPI
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            getLogger().info("PlaceholderAPI found! Registering placeholders...");
            new PlaceholderManager(this).register();
        }
        
        getLogger().info("OrePay has been enabled!");
        
        // Log loaded ore rewards
        logLoadedRewards();
    }
    
    @Override
    public void onDisable() {
        // Close database connection
        if (dataManager != null) {
            dataManager.closeConnection();
        }
        
        getLogger().info("OrePay has been disabled!");
    }
    
    /**
     * Setup Vault economy
     * @return True if successful
     */
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        
        economy = rsp.getProvider();
        return economy != null;
    }
    
    /**
     * Log loaded ore rewards for debugging
     */
    private void logLoadedRewards() {
        if (configManager.getOreRewards().isEmpty()) {
            getLogger().warning("No ore rewards loaded! Check your config.yml.");
            return;
        }
        
        getLogger().info("Loaded " + configManager.getOreRewards().size() + " ore rewards:");
        configManager.getOreRewards().forEach((material, reward) -> 
            getLogger().info("  " + material.name() + ": " + reward)
        );
    }
    
    /**
     * Get the economy provider
     * @return The economy provider
     */
    public Economy getEconomy() {
        return economy;
    }
    
    /**
     * Get the config manager
     * @return The config manager
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    /**
     * Get the data manager
     * @return The data manager
     */
    public DatabaseManager getDataManager() {
        return dataManager;
    }
    
    /**
     * Get the multiplier manager
     * @return The multiplier manager
     */
    public MultiplierManager getMultiplierManager() {
        return multiplierManager;
    }
    
    /**
     * Get the UI manager
     * @return The UI manager
     */
    public UIManager getUiManager() {
        return uiManager;
    }
    
    /**
     * Get the command manager
     * @return The command manager
     */
    public CommandManager getCommandManager() {
        return commandManager;
    }
}