package uk.co.hopperelec.mc.randomevents.utils;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.jetbrains.annotations.NotNull;

import static net.minecraft.core.SectionPos.blockToSectionCoord;

public class NMSUtils {
    public static @NotNull ServerLevel getNMSWorld(@NotNull World world) {
        return ((CraftWorld) world).getHandle();
    }
    public static @NotNull ServerLevel getNMSWorld(@NotNull Location location) {
        return getNMSWorld(location.getWorld());
    }

    public static ChunkPos getChunkPos(int x, int z) {
        return new ChunkPos(blockToSectionCoord(x), blockToSectionCoord(z));
    }
}
