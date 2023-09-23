package uk.co.hopperelec.mc.randomevents;

import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.mc.randomevents.eventtypes.PolyMetricRandomEventType;

public non-sealed class PolyMetricRandomEvent<M> extends RandomEvent {
    @NotNull public PolyMetricRandomEventType<M> type;
    public M metric;
    public short repeats;

    public PolyMetricRandomEvent(RandomEventsGame game, @NotNull RandomEventsPlayer player, @NotNull PolyMetricRandomEventType<M> type, M metric, short repeats) {
        super(game, player);
        this.type = type;
        this.metric = metric;
        this.repeats = repeats;
    }
    public PolyMetricRandomEvent(RandomEventsGame game, @NotNull RandomEventsPlayer player, @NotNull PolyMetricRandomEventType<M> type, M metric) {
        this(game, player, type, metric, (short) 1);
    }
}
