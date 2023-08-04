package uk.co.hopperelec.mc.randomevents.eventtypes;

import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uk.co.hopperelec.mc.randomevents.RandomEventsPlayer;
import uk.co.hopperelec.mc.randomevents.RandomEventsPlugin;

public class RandomEntityEvent extends PolyMetricRandomEventType<EntityType> {
    public RandomEntityEvent(@NotNull RandomEventsPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean execute(@NotNull RandomEventsPlayer player, @NotNull EntityType entityType) {
        player.spawnEntity(entityType);
        return true;
    }

    @Override
    protected boolean isValidMetric(@NotNull EntityType entityType) {
        return entityType.isSpawnable();
    }

    @Override
    protected boolean isEnabledIn(@NotNull EntityType entityType, @NotNull World world) {
        return entityType.isEnabledByFeature(world);
    }

    @Override
    protected @NotNull EntityType[] getAllMetrics() {
        return EntityType.values();
    }

    @Override
    protected @Nullable EntityType getMetricByName(@NotNull String name) {
        return EntityType.valueOf(name);
    }

    @Override
    public @NotNull String getSuccessMessage(@NotNull EntityType entityType) {
        return "Spawned a "+formatId(entityType);
    }
}
