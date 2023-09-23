package uk.co.hopperelec.mc.randomevents.eventtypes;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Damageable;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.mc.randomevents.RandomEventsPlayer;
import uk.co.hopperelec.mc.randomevents.RandomEventsPlugin;

import javax.annotation.CheckReturnValue;

public class RandomItemEvent extends PositionalPolyMetricRandomEventType<Material> {
    public RandomItemEvent(@NotNull RandomEventsPlugin plugin) {
        super(plugin);
    }

    @CheckReturnValue
    private @NotNull ItemStack getRandomItemStack(@NotNull Material material) {
        final ItemStack itemStack = new ItemStack(material);
        if (itemStack.getItemMeta() instanceof Damageable damageable) {
            damageable.setHealth(plugin.random.nextInt(material.getMaxDurability())+1);
        }
        return itemStack;
    }

    @Override
    public boolean execute(@NotNull RandomEventsPlayer player, @NotNull Material material) {
        final ItemStack itemStack = getRandomItemStack(material);
        player.giveItem(itemStack);
        return true;
    }

    @Override
    public void execute(@NotNull Location location, @NotNull Material material) {
        location.getWorld().dropItemNaturally(location, getRandomItemStack(material));
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
    protected @NotNull Material[] getAllMetrics(@NotNull World world) {
        return Material.values();
    }

    @Override
    public @NotNull String getSuccessMessage(@NotNull Material material) {
        return "Given you "+formatId(material);
    }
}
