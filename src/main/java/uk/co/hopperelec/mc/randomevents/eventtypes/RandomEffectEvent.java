package uk.co.hopperelec.mc.randomevents.eventtypes;

import com.fasterxml.jackson.databind.JsonNode;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.mc.randomevents.RandomEventsPlayer;
import uk.co.hopperelec.mc.randomevents.RandomEventsPlugin;

import javax.annotation.CheckReturnValue;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class RandomEffectEvent extends PolyMetricRandomEventType<PotionEffect> {
    public RandomEffectEvent(@NotNull RandomEventsPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean execute(@NotNull RandomEventsPlayer player, @NotNull PotionEffect potionEffect) {
        player.setPotionEffect(potionEffect);
        return true;
    }

    @CheckReturnValue
    private @NotNull PotionEffect generateBasePotionEffect(@NotNull PotionEffectType potionEffectType) {
        return new PotionEffect(potionEffectType, Integer.MAX_VALUE, 1);
    }

    @Override
    protected @NotNull PotionEffect[] getAllMetrics() {
        return Arrays.stream(PotionEffectType.values()).map(this::generateBasePotionEffect).toArray(PotionEffect[]::new);
    }

    @Override
    protected @NotNull String getMetricKey(@NotNull PotionEffect potionEffect) {
        return potionEffect.getType().toString();
    }

    @Override
    public PotionEffect getRandomMetricFor(@NotNull RandomEventsPlayer player) {
        final JsonNode effectAmplifiers = player.game.weightPreset.grandSubEvents().get("effect_amplifiers");
        final int amplifier;
        if (effectAmplifiers == null) {
            amplifier = plugin.random.nextInt(5)+1;
        } else {
            final Map<Integer, Float> weights = new HashMap<>();
            effectAmplifiers.fields().forEachRemaining(field -> weights.put(Integer.parseInt(field.getKey()), field.getValue().floatValue()));
            amplifier = plugin.chooseRandom(weights);
        }
        return super.getRandomMetricFor(player).withAmplifier(amplifier);
    }

    @Override
    public @NotNull String getSuccessMessage(@NotNull PotionEffect potionEffect) {
        return "Effected you with "+formatId(potionEffect.getType().getName())+" "+potionEffect.getAmplifier();
    }
}
