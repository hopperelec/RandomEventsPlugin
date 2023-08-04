package uk.co.hopperelec.mc.randomevents.countdowns;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.mc.randomevents.TimeInSeconds;

public class BukkitSecondCountdown {
    @NotNull private final TimeInSeconds timeRemaining = new TimeInSeconds();
    private final TimeInSeconds length;
    @NotNull private final JavaPlugin plugin;
    private int scheduledTaskId = -1;
    public final Runnable action;

    public BukkitSecondCountdown(@NotNull TimeInSeconds length, @NotNull JavaPlugin plugin, @NotNull Runnable action) {
        this.length = length;
        this.plugin = plugin;
        this.action = action;
    }

    public void start() {
        scheduledTaskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, this::tick, 20L, 20L);
    }

    public void pause() {
        plugin.getServer().getScheduler().cancelTask(scheduledTaskId);
        scheduledTaskId = -1;
    }

    public void reset() {
        timeRemaining.set(length);
    }

    public boolean isOngoing() {
        return scheduledTaskId != -1;
    }

    public void tick() {
        if (timeRemaining.asInt() <= 1) {
            reset();
            action.run();
        }
        timeRemaining.decrement();
    }

    public @NotNull TimeInSeconds getTimeRemaining() {
        return new TimeInSeconds(timeRemaining);
    }
    public void setTimeRemaining(@NotNull TimeInSeconds newTimeRemaining) {
        if (newTimeRemaining.moreThan(length)) {
            throw new IllegalArgumentException("Can not set time remaining to higher than the countdown's length!");
        }
        timeRemaining.set(newTimeRemaining);
    }

    public @NotNull TimeInSeconds getLength() {
        return new TimeInSeconds(length);
    }
    public void setLength(@NotNull TimeInSeconds newLength) {
        length.set(newLength);
        if (timeRemaining.moreThan(newLength)) {
            timeRemaining.set(newLength);
        }
    }

    public double getPercentageComplete() {
        return timeRemaining.dividedBy(length);
    }
}
