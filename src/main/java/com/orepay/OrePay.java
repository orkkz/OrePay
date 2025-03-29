package com.orepay;

import com.orepay.config.ConfigManager;
import com.orepay.listeners.MiningListener;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class OrePay extends JavaPlugin {
    
    private static final Logger LOGGER = Logger.getLogger("OrePay");
    private Economy economy = null;
    private ConfigManager configManager;
    
    @Override
    public void onEnable() {
        // Save default config if it doesn't exist
        saveDefaultConfig();
        
        // Initialize config manager
        configManager = new ConfigManager(this);
        
        // Setup Vault economy
        if (!setupEconomy()) {
            LOGGER.severe("Vault economy dependency not found! Disabling OrePay...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Register event listeners
        getServer().getPluginManager().registerEvents(new MiningListener(this), this);
        
        // Log successful enabling
        LOGGER.info("OrePay has been enabled successfully!");
    }
    
    @Override
    public void onDisable() {
        LOGGER.info("OrePay has been disabled!");
    }
    
    /**
     * Setup the Vault economy integration
     * @return true if setup was successful, false otherwise
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
     * Get the Vault economy instance
     * @return Economy instance
     */
    public Economy getEconomy() {
        return economy;
    }
    
    /**
     * Get the config manager
     * @return ConfigManager instance
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    /**
     * Reloads the plugin configuration
     */
    public void reloadPluginConfig() {
        reloadConfig();
        configManager.reloadConfig();
        LOGGER.info("OrePay configuration has been reloaded.");
    }
}
