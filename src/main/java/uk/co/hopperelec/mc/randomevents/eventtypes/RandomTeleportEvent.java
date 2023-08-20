package uk.co.hopperelec.mc.randomevents.eventtypes;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uk.co.hopperelec.mc.randomevents.RandomEventsPlayer;
import uk.co.hopperelec.mc.randomevents.RandomEventsPlugin;

import javax.annotation.CheckReturnValue;
import java.util.ArrayList;
import java.util.List;

public class RandomTeleportEvent extends MonoMetricRandomEventType {
    public RandomTeleportEvent(@NotNull RandomEventsPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean execute(@NotNull RandomEventsPlayer player) {
        final Location location = getRandomSafeLocationNear(player.getLocation());
        if (location == null) {
            player.sendMessage("Tried to teleport you, but couldn't find a suitable location close enough!");
            return false;
        }
        player.teleport(location);
        return true;
    }

    @Override
    public @NotNull String getSuccessMessage() {
        return "Teleported!";
    }

    @CheckReturnValue
    private boolean isSafeLocation(@NotNull World world, int x, int y, int z) {
        return world.getBlockAt(x, y, z).isSolid() && world.getBlockAt(x, y-1, z).isSolid();
    }

    @CheckReturnValue
    private @NotNull List<Vector> getSafeLocationsNear(@NotNull Location location) {
        final int minX = location.getBlockX()-plugin.config.teleportSearchRadius();
        final int maxX = location.getBlockX()+plugin.config.teleportSearchRadius();
        final int minZ = location.getBlockZ()-plugin.config.teleportSearchRadius();
        final int maxZ = location.getBlockZ()+plugin.config.teleportSearchRadius();
        final int minY = location.getWorld().getMinHeight();
        final List<Vector> safeLocations = new ArrayList<>();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                final int maxY = location.getWorld().getHighestBlockYAt(x, z);
                if (maxY != minY-1) {
                    safeLocations.add(new Vector(x, maxY+1, z));
                    int y = minY;
                    while (y < maxY) {
                        if (isSafeLocation(location.getWorld(), x, y, z)) {
                            safeLocations.add(new Vector(x, y, z));
                            y++;
                        }
                        y++;
                    }
                }
            }
        }
        return safeLocations;
    }

    @CheckReturnValue
    private @Nullable Location getRandomSafeLocationNear(@NotNull Location location) {
        final List<Vector> safeLocations = getSafeLocationsNear(location);
        if (safeLocations.isEmpty()) return null;
        final Vector safeLocation = safeLocations.get(plugin.random.nextInt(safeLocations.size()));
        return location.set(safeLocation.getX(), safeLocation.getY(), safeLocation.getZ());
    }
}
