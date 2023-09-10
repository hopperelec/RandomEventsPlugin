package uk.co.hopperelec.mc.randomevents.eventtypes;

import org.bukkit.Material;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.mc.randomevents.RandomEventsPlayer;
import uk.co.hopperelec.mc.randomevents.RandomEventsPlugin;

public class RandomBlockEvent extends PolyMetricRandomEventType<Material> {
    public RandomBlockEvent(@NotNull RandomEventsPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean execute(@NotNull RandomEventsPlayer player, @NotNull Material material) {
        player.setBlock(material);
        return true;
    }

    @Override
    protected boolean isValidMetric(@NotNull Material material) {
        return material.isBlock() && !material.isLegacy();
    }
    @Override
    protected boolean isEnabledIn(@NotNull Material material, @NotNull World world) {
        return material.isEnabledByFeature(world);
    }

    @Override
    protected @NotNull Material @NotNull[] getAllMetrics() {
        return Material.values();
    }

    @Override
    public @NotNull String getSuccessMessage(@NotNull Material material) {
        return "Placed "+formatId(material);
    }
}
