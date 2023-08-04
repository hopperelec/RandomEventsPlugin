package uk.co.hopperelec.mc.randomevents;

import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.mc.randomevents.eventtypes.MonoMetricRandomEventType;

public non-sealed class MonoMetricRandomEvent extends RandomEvent {
    @NotNull public MonoMetricRandomEventType type;

    public MonoMetricRandomEvent(RandomEventsGame game, @NotNull RandomEventsPlayer player, @NotNull MonoMetricRandomEventType type) {
        super(game, player);
        this.type = type;
    }
}
