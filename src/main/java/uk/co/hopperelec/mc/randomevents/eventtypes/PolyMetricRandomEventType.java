package uk.co.hopperelec.mc.randomevents.eventtypes;

import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.mc.randomevents.RandomEventsPlayer;
import uk.co.hopperelec.mc.randomevents.RandomEventsPlugin;

import javax.annotation.CheckReturnValue;
import java.util.*;
import java.util.stream.Collectors;

public abstract non-sealed class PolyMetricRandomEventType<M> extends RandomEventType {
    public PolyMetricRandomEventType(@NotNull RandomEventsPlugin plugin) {
        super(plugin);
    }

    public abstract boolean execute(@NotNull RandomEventsPlayer player, M metric);

    @CheckReturnValue
    protected boolean isValidMetric(M metric) { return true; }
    @CheckReturnValue
    protected boolean isEnabledIn(M metric, @NotNull World world) { return true; }
    @CheckReturnValue
    protected boolean isValidMetric(M metric, @NotNull World world) {
        return isValidMetric(metric) && isEnabledIn(metric, world);
    }
    @CheckReturnValue
    public boolean isValidMetric(M metric, @NotNull RandomEventsPlayer player) {
        return isValidMetric(metric, player.getWorld());
    }

    @CheckReturnValue
    protected abstract @NotNull M[] getAllMetrics();
    @CheckReturnValue
    protected @NotNull M[] getAllMetrics(@NotNull World world) { return getAllMetrics(); }

    @CheckReturnValue
    protected @NotNull String getMetricKey(@NotNull M metric) { return metric.toString(); }

    @CheckReturnValue
    protected @NotNull Set<M> getValidMetrics(@NotNull RandomEventsPlayer player) {
        return Arrays.stream(getAllMetrics(player.getWorld()))
                .filter(metric -> isValidMetric(metric, player))
                .collect(Collectors.toUnmodifiableSet());
    }

    @CheckReturnValue
    public M getRandomMetricFor(@NotNull RandomEventsPlayer player) {
        final Set<M> validMetrics = getValidMetrics(player);
        final Map<String,Float> weightsByName = player.game.weightPreset.subEvents().get(getName());
        if (weightsByName == null) {
            final Iterator<M> iterator = validMetrics.iterator();
            final int randomIndex = plugin.random.nextInt(validMetrics.size()-1);
            for (int i = 0; i < randomIndex; i++) iterator.next();
            return iterator.next();
        }
        return plugin.chooseRandom(validMetrics, weightsByName, this::getMetricKey);
    }

    @CheckReturnValue
    protected abstract @NotNull String getSuccessMessage(M metric);

    @CheckReturnValue
    protected @NotNull String formatId(@NotNull Object id) {
        return Arrays.stream(id.toString().split("_"))
                .map(word -> word.charAt(0)+word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    public void success(@NotNull RandomEventsPlayer player, M metric) {
        player.sendMessage(getSuccessMessage(metric));
        playSuccessSound(player);
    }
}
