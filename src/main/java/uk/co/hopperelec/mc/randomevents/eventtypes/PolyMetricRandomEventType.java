package uk.co.hopperelec.mc.randomevents.eventtypes;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.mc.randomevents.RandomEventsGame;
import uk.co.hopperelec.mc.randomevents.RandomEventsPlayer;
import uk.co.hopperelec.mc.randomevents.RandomEventsPlugin;
import uk.co.hopperelec.mc.randomevents.config.RandomEventWeightPreset;

import javax.annotation.CheckReturnValue;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract non-sealed class PolyMetricRandomEventType<M> extends RandomEventType {
    public PolyMetricRandomEventType(@NotNull RandomEventsPlugin plugin) {
        super(plugin);
    }

    public abstract boolean execute(@NotNull RandomEventsPlayer player, M metric);
    public short execute(@NotNull RandomEventsPlayer player, M metric, short repeats) {
        for (short i = 0; i < repeats; i++) {
            if (!execute(player, metric)) {
                return i;
            }
        }
        return repeats;
    }

    @CheckReturnValue
    protected boolean isValidMetric(M metric) { return true; }
    @CheckReturnValue
    protected boolean isEnabledIn(M metric, @NotNull World world) { return true; }
    @CheckReturnValue
    protected boolean isValidMetric(M metric, @NotNull World world) {
        return isValidMetric(metric) && isEnabledIn(metric, world);
    }
    @CheckReturnValue
    public boolean isValidMetric(M metric, @NotNull Location location) {
        return isValidMetric(metric, location.getWorld());
    }
    @CheckReturnValue
    public boolean isValidMetric(M metric, @NotNull Block block) {
        return isValidMetric(metric, block.getLocation());
    }
    @CheckReturnValue
    public boolean isValidMetric(M metric, @NotNull RandomEventsPlayer player) {
        return isValidMetric(metric, player.getLocation());
    }

    @CheckReturnValue
    protected abstract @NotNull M[] getAllMetrics(@NotNull World world);

    @CheckReturnValue
    protected @NotNull String getMetricKey(@NotNull M metric) { return metric.toString(); }

    @CheckReturnValue
    protected @NotNull Set<M> getValidMetrics(@NotNull Location location) {
        return Arrays.stream(getAllMetrics(location.getWorld()))
                .filter(metric -> isValidMetric(metric, location))
                .collect(Collectors.toUnmodifiableSet());
    }

    @CheckReturnValue
    public M getRandomMetricFor(@NotNull Location location, @NotNull RandomEventWeightPreset weightPreset) {
        final Set<M> validMetrics = getValidMetrics(location);
        final Map<String,Float> weightsByName = weightPreset.subEvents().get(getName());
        if (weightsByName == null) {
            final Iterator<M> iterator = validMetrics.iterator();
            final int randomIndex = plugin.random.nextInt(validMetrics.size()-1);
            for (int i = 0; i < randomIndex; i++) iterator.next();
            return iterator.next();
        }
        return plugin.chooseRandom(validMetrics, weightsByName, this::getMetricKey);
    }
    @CheckReturnValue
    public M getRandomMetricFor(@NotNull Location location, @NotNull RandomEventsGame game) {
        return getRandomMetricFor(location, game.weightPreset);
    }
    @CheckReturnValue
    public M getRandomMetricFor(@NotNull Block block, @NotNull RandomEventWeightPreset weightPreset) {
        return getRandomMetricFor(block.getLocation(), weightPreset);
    }
    @CheckReturnValue
    public M getRandomMetricFor(@NotNull Block block, @NotNull RandomEventsGame game) {
        return getRandomMetricFor(block, game.weightPreset);
    }
    @CheckReturnValue
    public M getRandomMetricFor(@NotNull RandomEventsPlayer player) {
        return getRandomMetricFor(player.getLocation(), player.game);
    }

    @CheckReturnValue
    protected abstract @NotNull String getSuccessMessage(M metric);

    @CheckReturnValue
    protected @NotNull String formatId(@NotNull Object id) {
        return Arrays.stream(id.toString().split("_"))
                .map(word -> word.charAt(0)+word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    protected void success(@NotNull RandomEventsPlayer player, @NotNull String message) {
        player.sendMessage(message);
        playSuccessSound(player);
    }
    public void success(@NotNull RandomEventsPlayer player, M metric) {
        success(player, getSuccessMessage(metric));
    }
    public void success(@NotNull RandomEventsPlayer player, M metric, short repeats) {
        final String message = getSuccessMessage(metric);
        success(player, repeats > 1 ? message : message+" x"+repeats);
    }
}
