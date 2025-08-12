package de.felix.autoReplantCrops;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;

public class CropGrowthManager {

    private final AutoReplantCrops plugin;
    private final CropFieldManager cropFieldManager;

    // Geschwindigkeit des Wachstums, z.B. 3.0 = 3x schneller
    private static final double SPEED_MULTIPLIER = 10.0;

    public CropGrowthManager(AutoReplantCrops plugin, CropFieldManager cropFieldManager) {
        this.plugin = plugin;
        this.cropFieldManager = cropFieldManager;
    }

    public void startGrowthTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                Collection<CropFieldManager.Field> fields = cropFieldManager.fields.values();

                if (fields.isEmpty()) {
                    plugin.getLogger().info("Keine Felder gefunden!");
                    return;
                }

                for (CropFieldManager.Field field : fields) {
                    accelerateGrowthInField(field);
                }

                // Nachricht an alle Spieler alle 20 Ticks (1 Sekunde)
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    p.sendMessage("CropGrowthManager Task läuft und aktualisiert Pflanzen...");
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }



    private void accelerateGrowthInField(CropFieldManager.Field field) {
        int radius = field.radius;
        Location center = field.center;
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int z = cz - radius; z <= cz + radius; z++) {
                Location loc = new Location(center.getWorld(), x, cy, z);
                if (!field.isInField(loc)) continue;

                Block block = loc.getBlock();
                Material type = block.getType();

                if (!isCrop(type)) continue;

                if (block.getBlockData() instanceof Ageable ageable) {
                    int maxAge = ageable.getMaximumAge();
                    int currentAge = ageable.getAge();

                    if (currentAge < maxAge) {
                        int increase = (int) SPEED_MULTIPLIER;
                        int newAge = currentAge + increase;
                        if (newAge > maxAge) newAge = maxAge;

                        // Klone das BlockData und setze das Alter darauf
                        Ageable newAgeable = (Ageable) block.getBlockData().clone();
                        newAgeable.setAge(newAge);

                        // Setze das neue BlockData mit applyPhysics = true
                        block.setBlockData(newAgeable, true);
                    }
                }
            }
        }
    }


    private boolean isCrop(Material material) {
        return material == Material.WHEAT
                || material == Material.CARROTS
                || material == Material.POTATOES
                || material == Material.BEETROOTS
                || material == Material.NETHER_WART;
    }
}
