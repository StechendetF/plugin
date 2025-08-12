package de.felix.autoReplantCrops;

import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.*;

public class CropFieldManager {

    private final AutoReplantCrops plugin;
    final Map<String, Field> fields = new HashMap<>();
    private int nextId = 1;

    public CropFieldManager(AutoReplantCrops plugin) {
        this.plugin = plugin;
    }

    public void loadFields() {
        FileConfiguration config = plugin.getConfig();
        if (!config.contains("fields")) {
            plugin.getLogger().info("Keine Felder in der config gefunden.");
            return;
        }

        for (String key : config.getConfigurationSection("fields").getKeys(false)) {
            String path = "fields." + key;
            World world = Bukkit.getWorld(config.getString(path + ".world"));
            if (world == null) {
                plugin.getLogger().warning("Welt nicht gefunden für Feld " + key);
                continue;
            }

            Location center = new Location(
                    world,
                    config.getDouble(path + ".x"),
                    config.getDouble(path + ".y"),
                    config.getDouble(path + ".z")
            );
            int radius = config.getInt(path + ".radius");

            Field field = new Field(center, radius);
            fields.put(key, field);
            plugin.getLogger().info("Feld geladen: " + key + " in Welt " + world.getName() + " mit Radius " + radius);

            try {
                int id = Integer.parseInt(key.replace("field_", ""));
                if (id >= nextId) nextId = id + 1;
            } catch (NumberFormatException ignored) {}

            String base64 = config.getString(path + ".headTexture");
            if (base64 == null || base64.isEmpty()) base64 = getDefaultHeadTexture();
            spawnHead(field, base64);
        }
    }

    public boolean handleCreateFieldCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Nur Spieler können diesen Befehl ausführen.");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Nutzung: /createfield <range>");
            return true;
        }

        try {
            int range = Integer.parseInt(args[0]);

            Location base = player.getLocation();
            Location center = new Location(
                    base.getWorld(),
                    base.getBlockX(),
                    base.getBlockY(),
                    base.getBlockZ()
            );
            Field field = new Field(center, range);
            String fieldId = "field_" + nextId++;

            fields.put(fieldId, field);

            String path = "fields." + fieldId;
            plugin.getConfig().set(path + ".world", field.center.getWorld().getName());
            plugin.getConfig().set(path + ".x", field.center.getX());
            plugin.getConfig().set(path + ".y", field.center.getY());
            plugin.getConfig().set(path + ".z", field.center.getZ());
            plugin.getConfig().set(path + ".radius", field.radius);
            plugin.getConfig().set(path + ".headTexture", getDefaultHeadTexture());
            plugin.saveConfig();

            spawnHead(field, getDefaultHeadTexture());

            player.sendMessage(ChatColor.GREEN + "Feld mit Radius " + range + " gespeichert und Kopf gesetzt.");
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Ungültige Zahl.");
        }
        return true;
    }

    private void spawnHead(Field field, String base64) {
        Location headLoc = new Location(
                field.center.getWorld(),
                field.center.getBlockX() + 0.5,
                field.center.getBlockY() + 2.0,
                field.center.getBlockZ() + 0.5
        );

        for (ArmorStand nearby : headLoc.getWorld().getEntitiesByClass(ArmorStand.class)) {
            if (nearby.getLocation().distanceSquared(headLoc) <= 1.0
                    && !nearby.isVisible()
                    && nearby.isSmall()
                    && nearby.isMarker()
                    && nearby.getHelmet() != null) {
                nearby.remove();
            }
        }

        ArmorStand stand = headLoc.getWorld().spawn(headLoc, ArmorStand.class);
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setMarker(true);
        stand.setSmall(true);
        stand.setHelmet(getCustomSkull(base64));

        new BukkitRunnable() {
            double yaw = 0;
            @Override
            public void run() {
                if (!stand.isValid()) {
                    cancel();
                    return;
                }
                yaw += 2.5;
                if (yaw >= 360) yaw = 0;
                stand.setHeadPose(new EulerAngle(0, Math.toRadians(yaw), 0));
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private ItemStack getCustomSkull(String base64) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        String url = extractTextureUrl(base64);
        if (url == null) return head;

        PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
        try {
            profile.getTextures().setSkin(URI.create(url).toURL());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        meta.setOwnerProfile(profile);
        head.setItemMeta(meta);
        return head;
    }

    private String extractTextureUrl(String base64) {
        if (base64 == null || base64.isEmpty()) {
            plugin.getLogger().warning("Kopf-Textur ist null oder leer!");
            return null;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(base64);
            String json = new String(decoded);
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            return obj.getAsJsonObject("textures")
                    .getAsJsonObject("SKIN")
                    .get("url").getAsString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getDefaultHeadTexture() {
        return "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzBkOTY5Y2Q4YzhiMjkxNmIyNmExOTcyNTNlM2FkZmU5ODUzNzIwNDk0ZjIyYmUxOWEwODNiZjE4NGY5YzJiYyJ9fX0=";
    }

    public boolean isInAnyField(Location location) {
        return fields.values().stream().anyMatch(f -> f.isInField(location));
    }

    // Methoden außerhalb der inneren Klasse, auf Felder und Plugin zugreifend:

    public String getLastFieldId() {
        if (fields.isEmpty()) return null;

        return fields.keySet().stream()
                .filter(id -> id.startsWith("field_"))
                .max((a, b) -> {
                    try {
                        int aId = Integer.parseInt(a.replace("field_", ""));
                        int bId = Integer.parseInt(b.replace("field_", ""));
                        return Integer.compare(aId, bId);
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                }).orElse(null);
    }

    public boolean deleteField(String fieldId) {
        if (!fields.containsKey(fieldId)) return false;

        fields.remove(fieldId);
        plugin.getConfig().set("fields." + fieldId, null);
        plugin.saveConfig();
        return true;
    }

    // ----- Feldklasse -----
    public static class Field {
        public final Location center;
        public final int radius;

        public Field(Location center, int radius) {
            this.center = center;
            this.radius = radius;
        }

        public boolean isInField(Location loc) {
            if (!loc.getWorld().equals(center.getWorld())) return false;
            double dx = Math.abs(loc.getBlockX() - center.getBlockX());
            double dz = Math.abs(loc.getBlockZ() - center.getBlockZ());
            return dx <= radius && dz <= radius;
        }
    }
}
