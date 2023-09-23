package uk.co.hopperelec.mc.randomevents.eventtypes;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.mc.randomevents.RandomEventsPlugin;
import uk.co.hopperelec.mc.randomevents.utils.KeyedResource;
import uk.co.hopperelec.mc.randomevents.utils.NMSUtils;

public abstract class KeyedRandomEventType<R> extends PositionalPolyMetricRandomEventType<KeyedResource<R>> {
    private final ResourceKey<Registry<R>> registryKey;
    
    public KeyedRandomEventType(@NotNull RandomEventsPlugin plugin, ResourceKey<Registry<R>> registryKey) {
        super(plugin);
        this.registryKey = registryKey;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected KeyedResource<R>[] getAllMetrics(@NotNull World world) {
        return NMSUtils.getNMSWorld(world).registryAccess().registry(registryKey).orElseThrow().entrySet().stream().map(
                entry -> new KeyedResource<>(entry.getKey().location(), entry.getValue())
        ).toArray(KeyedResource[]::new);
    }

    @Override
    protected @NotNull String getMetricKey(@NotNull KeyedResource<R> resource) {
        return resource.key().toString();
    }

    @Override
    protected @NotNull String getSuccessMessage(@NotNull KeyedResource<R> resource) {
        return "Placed a "+resource.key()+" (sometimes they spawn in strange places- look above and below you!)";
    }
}
