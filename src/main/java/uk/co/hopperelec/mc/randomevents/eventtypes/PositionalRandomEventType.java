package uk.co.hopperelec.mc.randomevents.eventtypes;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.mc.randomevents.RandomEventsGame;
import uk.co.hopperelec.mc.randomevents.config.RandomEventWeightPreset;

public interface PositionalRandomEventType {
    void execute(@NotNull Location location, @NotNull RandomEventWeightPreset weightPreset);
    default void execute(@NotNull Block block, @NotNull RandomEventWeightPreset weightPreset) {
        execute(block.getLocation().add(0,1,0), weightPreset);
    }
    default void execute(@NotNull Location location, @NotNull RandomEventsGame game) {
        execute(location, game.weightPreset);
    }
    default void execute(@NotNull Block block, @NotNull RandomEventsGame game) {
        execute(block, game.weightPreset);
    }

    String getName();
}
