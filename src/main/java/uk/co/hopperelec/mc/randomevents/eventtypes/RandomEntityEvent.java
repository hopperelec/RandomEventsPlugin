package uk.co.hopperelec.mc.randomevents.eventtypes;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang.ArrayUtils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.mc.randomevents.RandomEventsPlayer;
import uk.co.hopperelec.mc.randomevents.RandomEventsPlugin;

import javax.annotation.CheckReturnValue;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class RandomEntityEvent extends PositionalPolyMetricRandomEventType<EntityType[]> {
    public RandomEntityEvent(@NotNull RandomEventsPlugin plugin) {
        super(plugin);
    }

    @Override
    public void execute(@NotNull Location location, @NotNull EntityType @NotNull[] entityTypes) {
        Entity topEntity = location.getWorld().spawnEntity(location, entityTypes[0]);
        for (int i = 1; i < entityTypes.length; i++) {
            final Entity nextEntity = location.getWorld().spawnEntity(location, entityTypes[i]);
            if (topEntity.addPassenger(nextEntity)) {
                topEntity = nextEntity;
            }
        }
    }

    @Override
    protected boolean isValidMetric(@NotNull EntityType @NotNull[] entityTypes) {
        return Arrays.stream(entityTypes).allMatch(EntityType::isSpawnable);
    }

    @Override
    protected boolean isEnabledIn(@NotNull EntityType @NotNull[] entityTypes, @NotNull World world) {
        return Arrays.stream(entityTypes).allMatch(entityType -> entityType.isEnabledByFeature(world));
    }

    @Override
    protected @NotNull EntityType @NotNull[] @NotNull[] getAllMetrics(@NotNull World world) {
        return Arrays.stream(EntityType.values()).map(entityType -> new EntityType[]{entityType}).toArray(EntityType[][]::new);
    }

    @CheckReturnValue
    protected @NotNull String getMetricKey(@NotNull EntityType @NotNull[] entityTypes) {
        return entityTypes[0].name();
    }

    @Override
    public EntityType[] getRandomMetricFor(@NotNull RandomEventsPlayer player) {
        final EntityType[] metric = super.getRandomMetricFor(player);
        final JsonNode passengerProbability = player.game.weightPreset.grandSubEvents().get("entity_passenger");
        if (passengerProbability != null && plugin.random.nextFloat() < passengerProbability.floatValue()) {
            return (EntityType[]) ArrayUtils.addAll(metric, getRandomMetricFor(player));
        }
        return metric;
    }

    @Override
    public @NotNull String getSuccessMessage(@NotNull EntityType[] entityTypes) {
        final List<String> entityNames = Arrays.stream(entityTypes)
                .map(this::formatId)
                .collect(Collectors.toList());
        Collections.reverse(entityNames);
        return "Spawned a "+ String.join(" riding a ", entityNames);
    }
}
