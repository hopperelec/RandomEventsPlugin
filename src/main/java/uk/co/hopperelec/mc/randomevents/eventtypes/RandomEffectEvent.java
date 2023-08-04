package uk.co.hopperelec.mc.randomevents.eventtypes;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uk.co.hopperelec.mc.randomevents.RandomEventsPlayer;
import uk.co.hopperelec.mc.randomevents.RandomEventsPlugin;

import java.util.Arrays;

public class RandomEffectEvent extends PolyMetricRandomEventType<PotionEffect> {
    public RandomEffectEvent(@NotNull RandomEventsPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean execute(@NotNull RandomEventsPlayer player, @NotNull PotionEffect potionEffect) {
        player.setPotionEffect(potionEffect);
        return true;
    }

    private @NotNull PotionEffect generateBasePotionEffect(@NotNull PotionEffectType potionEffectType) {
        return new PotionEffect(potionEffectType, Integer.MAX_VALUE, 1);
    }

    @Override
    protected @NotNull PotionEffect[] getAllMetrics() {
        return Arrays.stream(PotionEffectType.values()).map(this::generateBasePotionEffect).toArray(PotionEffect[]::new);
    }

    @Override
    protected @Nullable PotionEffect getMetricByName(@NotNull String name) {
        final PotionEffectType potionEffectType = PotionEffectType.getByName(name);
        if (potionEffectType == null) return null;
        return generateBasePotionEffect(potionEffectType);
    }

    @Override
    public PotionEffect getRandomMetricFor(@NotNull RandomEventsPlayer player) {
        return super.getRandomMetricFor(player).withAmplifier(plugin.random.nextInt(5)+1); // TODO: Configurable amplifier
    }

    @Override
    public @NotNull String getSuccessMessage(@NotNull PotionEffect potionEffect) {
        return "Effected you with "+formatId(potionEffect.getType().getName())+" "+potionEffect.getAmplifier();
    }
}
