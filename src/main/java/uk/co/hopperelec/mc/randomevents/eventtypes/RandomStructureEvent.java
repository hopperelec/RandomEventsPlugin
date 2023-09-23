package uk.co.hopperelec.mc.randomevents.eventtypes;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.mc.randomevents.RandomEventsPlugin;
import uk.co.hopperelec.mc.randomevents.utils.KeyedResource;
import uk.co.hopperelec.mc.randomevents.utils.NMSUtils;

public class RandomStructureEvent extends KeyedRandomEventType<Structure> {
    public RandomStructureEvent(@NotNull RandomEventsPlugin plugin) {
        super(plugin, Registries.STRUCTURE);
    }

    @Override
    public void execute(@NotNull Location location, KeyedResource<Structure> structure) {
        final ServerLevel nmsWorld = NMSUtils.getNMSWorld(location);
        final StructureStart structureStart = structure.resource().generate(
                nmsWorld.registryAccess(),
                nmsWorld.chunkSource.getGenerator(),
                nmsWorld.chunkSource.getGenerator().getBiomeSource(),
                nmsWorld.chunkSource.randomState(),
                nmsWorld.getStructureManager(),
                nmsWorld.getSeed(),
                NMSUtils.getChunkPos(location.getBlockX(), location.getBlockZ()),
                0,
                nmsWorld,
                holder -> true
        );
        final BoundingBox bBox = structureStart.getBoundingBox();
        ChunkPos.rangeClosed(NMSUtils.getChunkPos(bBox.minX(), bBox.minZ()), NMSUtils.getChunkPos(bBox.maxX(), bBox.maxZ())).forEach(chunkPos -> {
            structureStart.placeInChunk(
                    nmsWorld,
                    nmsWorld.structureManager(),
                    nmsWorld.getChunkSource().getGenerator(),
                    nmsWorld.getRandom(),
                    new BoundingBox(
                            chunkPos.getMinBlockX(),
                            nmsWorld.getMinBuildHeight(),
                            chunkPos.getMinBlockZ(),
                            chunkPos.getMaxBlockX(),
                            nmsWorld.getMaxBuildHeight(),
                            chunkPos.getMaxBlockZ()
                    ), chunkPos
            );
        });
    }
}
