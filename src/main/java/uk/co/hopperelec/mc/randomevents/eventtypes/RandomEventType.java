package uk.co.hopperelec.mc.randomevents.eventtypes;

import net.kyori.adventure.sound.Sound;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.mc.randomevents.RandomEventsPlayer;
import uk.co.hopperelec.mc.randomevents.RandomEventsPlugin;

public abstract sealed class RandomEventType permits MonoMetricRandomEventType,PolyMetricRandomEventType {
    @NotNull
    protected final RandomEventsPlugin plugin;

    public RandomEventType(@NotNull RandomEventsPlugin plugin) {
        this.plugin = plugin;
    }

    protected void playSuccessSound(@NotNull RandomEventsPlayer player) {
        final Sound sound = plugin.config.eventSoundEffects().get(getName());
        if (sound != null) player.playSound(sound);
    }

    public @NotNull String getName() {
        return plugin.registeredEventTypes.inverse().get(this);
    }
}