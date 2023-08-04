package uk.co.hopperelec.mc.randomevents;

import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.mc.randomevents.eventtypes.PolyMetricRandomEventType;

public non-sealed class PolyMetricRandomEvent<M> extends RandomEvent {
    public M metric;
    @NotNull public PolyMetricRandomEventType<M> type;

    public PolyMetricRandomEvent(RandomEventsGame game, @NotNull RandomEventsPlayer player, @NotNull PolyMetricRandomEventType<M> type, M metric) {
        super(game, player);
        this.type = type;
        this.metric = metric;
    }
}
