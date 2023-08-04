package uk.co.hopperelec.mc.randomevents;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public abstract sealed class RandomEvent extends Event implements Cancellable permits MonoMetricRandomEvent,PolyMetricRandomEvent {
    @NotNull private static final HandlerList HANDLER_LIST = new HandlerList();
    private boolean cancelled = false;
    public final RandomEventsGame game;
    @NotNull public final RandomEventsPlayer player;

    public RandomEvent(RandomEventsGame game, @NotNull RandomEventsPlayer player) {
        this.game = game;
        this.player = player;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }
}
