package de.felix.autoReplantCrops;


import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;

public class Field {
    public final Location center;
    public final int radius;
    public String headTexture;
    public ArmorStand headStand;

    public Field(Location center, int radius) {
        this.center = center;
        this.radius = radius;
    }

    public boolean isInField(Location loc) {
        return loc.getWorld().equals(center.getWorld()) &&
                loc.distanceSquared(center) <= (radius * radius);
    }
}
