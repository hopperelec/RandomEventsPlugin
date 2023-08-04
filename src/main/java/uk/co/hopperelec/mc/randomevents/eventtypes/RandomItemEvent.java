package uk.co.hopperelec.mc.randomevents.eventtypes;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Damageable;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uk.co.hopperelec.mc.randomevents.RandomEventsPlayer;
import uk.co.hopperelec.mc.randomevents.RandomEventsPlugin;

public class RandomItemEvent extends PolyMetricRandomEventType<Material> {
    public RandomItemEvent(@NotNull RandomEventsPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean execute(@NotNull RandomEventsPlayer player, @NotNull Material material) {
        final ItemStack itemStack = new ItemStack(material);
        if (itemStack.getItemMeta() instanceof Damageable damageable) {
            damageable.setHealth(plugin.random.nextInt(material.getMaxDurability())+1);
        }
        player.game.handleLoreFor(itemStack);
        player.giveItem(itemStack);
        return true;
    }

    @Override
    protected boolean isValidMetric(@NotNull Material material) {
        return material.isItem() && !material.isLegacy();
    }

    @Override
    protected boolean isEnabledIn(@NotNull Material material, @NotNull World world) {
        return material.isEnabledByFeature(world);
    }

    @Override
    protected @NotNull Material[] getAllMetrics() {
        return Material.values();
    }

    @Override
    protected @Nullable Material getMetricByName(@NotNull String name) {
        return Material.getMaterial(name);
    }

    @Override
    public @NotNull String getSuccessMessage(@NotNull Material material) {
        return "Given you "+formatId(material);
    }
}
