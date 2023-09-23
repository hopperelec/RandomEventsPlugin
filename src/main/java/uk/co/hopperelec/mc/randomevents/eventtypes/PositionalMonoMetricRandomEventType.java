package uk.co.hopperelec.mc.randomevents.eventtypes;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.mc.randomevents.RandomEventsPlayer;
import uk.co.hopperelec.mc.randomevents.RandomEventsPlugin;
import uk.co.hopperelec.mc.randomevents.config.RandomEventWeightPreset;

public abstract class PositionalMonoMetricRandomEventType extends MonoMetricRandomEventType implements PositionalRandomEventType {
    public PositionalMonoMetricRandomEventType(@NotNull RandomEventsPlugin plugin) {
        super(plugin);
    }

    public abstract void execute(@NotNull Location location, @NotNull RandomEventWeightPreset weightPreset);

    @Override
    public boolean execute(@NotNull RandomEventsPlayer player) {
        execute(player.getLocation(), player.game.weightPreset);
        return true;
    }
}
