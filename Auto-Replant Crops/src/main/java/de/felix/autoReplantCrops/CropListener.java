package de.felix.autoReplantCrops;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;

public class CropListener implements Listener {

    private final AutoReplantCrops plugin;

    public CropListener(AutoReplantCrops plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Block block = e.getBlock();
        Player player = e.getPlayer();

        if (!plugin.getFieldManager().isInAnyField(block.getLocation())) return;

        if (!(block.getBlockData() instanceof Ageable crop)) return;

        if (crop.getAge() < crop.getMaximumAge()) return; // Not fully grown

        e.setDropItems(false); // prevent natural drops

        // Give items manually
        for (ItemStack drop : block.getDrops(player.getInventory().getItemInMainHand())) {
            HashMap<Integer, ItemStack> notFit = player.getInventory().addItem(drop);
            for (ItemStack leftover : notFit.values()) {
                // Item passt nicht -> verwerfen
            }
        }

        // Delay Replant with Animation
        Material type = block.getType();
        Location loc = block.getLocation();
        block.setType(Material.AIR);

        new BukkitRunnable() {
            ArmorStand stand;
            Location start;
            Location end;
            Vector direction;
            double traveled = 0;
            double step = 0.3; // Wie weit der Partikel pro Tick "fliegt"

            {
                ArmorStand nearest = null;
                double nearestDistance = Double.MAX_VALUE;

                // Nächstgelegenen ArmorStand zum Feld suchen
                for (Entity entity : loc.getWorld().getEntities()) {
                    if (entity instanceof ArmorStand armorStand) {
                        double dist = armorStand.getEyeLocation().distance(loc.clone().add(0.5, 0.5, 0.5));
                        if (dist < nearestDistance) {
                            nearestDistance = dist;
                            nearest = armorStand;
                        }
                    }
                }

                if (nearest != null) {
                    stand = nearest;
                    // Start: Kopf des ArmorStands (Feinjustierung Höhe z.B. +0.3)
                    start = stand.getEyeLocation().clone().add(0, 0.3, 0);
                    // Ziel: Mitte des Feldes
                    end = loc.clone().add(0.5, 0.5, 0.5);
                    // Richtung von Start zu Ende
                    direction = end.toVector().subtract(start.toVector()).normalize();
                }
            }

            @Override
            public void run() {
                if (stand == null) {
                    cancel();
                    return;
                }

                // Schrittweise vom Startpunkt in Richtung Ziel wandern
                traveled += step;
                Location current = start.clone().add(direction.clone().multiply(traveled));

                // Partikel an aktueller Position spawnen
                current.getWorld().spawnParticle(
                        Particle.HAPPY_VILLAGER,
                        current,
                        2,
                        0, 0, 0,
                        0
                );

                // Wenn Partikel nahe genug am Ziel ist, Pflanze neu setzen und Task beenden
                if (current.distance(end) <= step) {
                    block.setType(type);
                    if (block.getBlockData() instanceof Ageable replanted) {
                        replanted.setAge(0);
                        block.setBlockData(replanted);
                    }
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L); // jede Tick, um flüssige Animation zu erzeugen

    }
}