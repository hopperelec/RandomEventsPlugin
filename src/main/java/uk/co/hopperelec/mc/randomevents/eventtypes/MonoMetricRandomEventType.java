package uk.co.hopperelec.mc.randomevents.eventtypes;

import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.mc.randomevents.RandomEventsPlayer;
import uk.co.hopperelec.mc.randomevents.RandomEventsPlugin;

public abstract non-sealed class MonoMetricRandomEventType extends RandomEventType {
    public MonoMetricRandomEventType(@NotNull RandomEventsPlugin plugin) {
        super(plugin);
    }

    public abstract boolean execute(@NotNull RandomEventsPlayer player);

    protected abstract @NotNull String getSuccessMessage();

    public void success(@NotNull RandomEventsPlayer player) {
        player.sendMessage(getSuccessMessage());
        playSuccessSound(player);
    }
}
