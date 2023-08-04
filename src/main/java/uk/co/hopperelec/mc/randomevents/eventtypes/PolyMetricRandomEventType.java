package uk.co.hopperelec.mc.randomevents.eventtypes;

import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uk.co.hopperelec.mc.randomevents.RandomEventsPlayer;
import uk.co.hopperelec.mc.randomevents.RandomEventsPlugin;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract non-sealed class PolyMetricRandomEventType<M> extends RandomEventType {
    public PolyMetricRandomEventType(@NotNull RandomEventsPlugin plugin) {
        super(plugin);
    }

    public abstract boolean execute(@NotNull RandomEventsPlayer player, M metric);

    protected boolean isValidMetric(M metric) { return true; }
    protected boolean isEnabledIn(M metric, @NotNull World world) { return true; }
    protected boolean isValidMetric(M metric, @NotNull World world) {
        return isValidMetric(metric) && isEnabledIn(metric, world);
    }
    public boolean isValidMetric(M metric, @NotNull RandomEventsPlayer player) {
        return isValidMetric(metric, player.getWorld());
    }

    protected abstract @NotNull M[] getAllMetrics();
    protected @NotNull M[] getAllMetrics(@NotNull World world) { return getAllMetrics(); }

    protected abstract @Nullable M getMetricByName(@NotNull String name);
    protected @Nullable M getMetricByName(@NotNull String name, @NotNull World world) { return getMetricByName(name); }

    protected @NotNull Set<M> getValidMetrics(M[] metrics, @NotNull RandomEventsPlayer player) {
        return Arrays.stream(metrics)
                .filter(metric -> isValidMetric(metric, player))
                .collect(Collectors.toUnmodifiableSet());
    }

    protected Map<M,Float> getWeights(@NotNull RandomEventsPlayer player) {
        final Map<String,Float> weightsByName = player.game.weightPreset.subEvents().get(getName());
        if (weightsByName == null) return Map.of();
        final Map<M,Float> weights = new HashMap<>();
        for (Map.Entry<String,Float> weightByName : weightsByName.entrySet()) {
            final M metric = getMetricByName(weightByName.getKey(), player.getWorld());
            if (metric != null) weights.put(metric,weightByName.getValue());
        }
        return weights;
    }

    public M getRandomMetricFor(@NotNull RandomEventsPlayer player) {
        return plugin.chooseRandom(getValidMetrics(getAllMetrics(player.getWorld()), player), getWeights(player));
    }

    protected abstract @NotNull String getSuccessMessage(M metric);
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
