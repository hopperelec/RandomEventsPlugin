package uk.co.hopperelec.mc.randomevents.eventtypes;

import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.mc.randomevents.RandomEventsPlayer;
import uk.co.hopperelec.mc.randomevents.RandomEventsPlugin;

import javax.annotation.CheckReturnValue;

public abstract non-sealed class MonoMetricRandomEventType extends RandomEventType {
    public MonoMetricRandomEventType(@NotNull RandomEventsPlugin plugin) {
        super(plugin);
    }

    public abstract boolean execute(@NotNull RandomEventsPlayer player);

    @CheckReturnValue
    protected abstract @NotNull String getSuccessMessage();

    public void success(@NotNull RandomEventsPlayer player) {
        player.sendMessage(getSuccessMessage());
        playSuccessSound(player);
    }
}
