package de.felix.autoReplantCrops;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class AutoReplantCrops extends JavaPlugin {

    private static AutoReplantCrops instance;
    private CropFieldManager fieldManager;
    private CropGrowthManager cropGrowthManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        fieldManager = new CropFieldManager(this);

        // Lade Felder verzögert, damit Welten fertig geladen sind
        Bukkit.getScheduler().runTaskLater(this, () -> {
            fieldManager.loadFields();
            instance.getLogger().info("Felder wurden erfolgreich nach Welten-Initialisierung geladen.");
        }, 1L); // 1 Tick warten

        // HIER: Feld setzen, nicht lokale Variable!
        this.cropGrowthManager = new CropGrowthManager(this, fieldManager);
        this.cropGrowthManager.startGrowthTask();

        // Events registrieren
        getServer().getPluginManager().registerEvents(new CropListener(this), this);

        // Befehle registrieren
        CommandHandler commandHandler = new CommandHandler(this, fieldManager);
        getCommand("createfield").setExecutor(commandHandler);
        getCommand("reloadfields").setExecutor(commandHandler);
        getCommand("deletefield").setExecutor(commandHandler);

    }

    public static AutoReplantCrops getInstance() {
        return instance;
    }

    public CropFieldManager getFieldManager() {
        return fieldManager;
    }

    public CropGrowthManager getCropGrowthManager() {
        return cropGrowthManager;
    }
}
