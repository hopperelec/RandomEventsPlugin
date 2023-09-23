package uk.co.hopperelec.mc.randomevents.eventtypes;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_20_R1.util.CraftLocation;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.mc.randomevents.RandomEventsPlugin;
import uk.co.hopperelec.mc.randomevents.utils.KeyedResource;
import uk.co.hopperelec.mc.randomevents.utils.NMSUtils;

public class RandomWorldFeatureEvent extends KeyedRandomEventType<ConfiguredFeature<?,?>> {
    public RandomWorldFeatureEvent(@NotNull RandomEventsPlugin plugin) {
        super(plugin, Registries.CONFIGURED_FEATURE);
    }

    @Override
    public void execute(@NotNull Location location, KeyedResource<ConfiguredFeature<?,?>> worldFeature) {
        final ServerLevel nmsWorld = NMSUtils.getNMSWorld(location);
        worldFeature.resource().place(
                nmsWorld,
                nmsWorld.chunkSource.getGenerator(),
                nmsWorld.random,
                CraftLocation.toBlockPosition(location)
        );
    }
}
