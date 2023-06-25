package uk.co.hopperelec.mc.randomevents;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class RandomEvent extends Event implements Cancellable {
    @NotNull private static final HandlerList HANDLER_LIST = new HandlerList();
    private boolean cancelled = false;
    public final RandomEventsGame game;
    @NotNull public final RandomEventsPlayer player;
    @NotNull public RandomEventType type;

    public RandomEvent(RandomEventsGame game, @NotNull RandomEventsPlayer player, @NotNull RandomEventType type) {
        this.game = game;
        this.player = player;
        this.type = type;
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
