package uk.co.hopperelec.mc.randomevents.eventtypes;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.mc.randomevents.RandomEventsGame;
import uk.co.hopperelec.mc.randomevents.RandomEventsPlayer;
import uk.co.hopperelec.mc.randomevents.RandomEventsPlugin;
import uk.co.hopperelec.mc.randomevents.config.RandomEventWeightPreset;

public abstract class PositionalPolyMetricRandomEventType<M> extends PolyMetricRandomEventType<M> implements PositionalRandomEventType {
    public PositionalPolyMetricRandomEventType(@NotNull RandomEventsPlugin plugin) {
        super(plugin);
    }

    public abstract void execute(@NotNull Location location, M metric);
    public void execute(@NotNull Block block, M metric) {
        execute(block.getLocation().add(0,1,0), metric);
    }
    public void execute(@NotNull Location location, M metric, short repeats) {
        for (short i = 0; i < repeats; i++) {
            execute(location, metric);
        }
    }
    public void execute(@NotNull Block block, M metric, short repeats) {
        execute(block.getLocation().add(0,1,0), metric, repeats);
    }
    public void execute(@NotNull Location location, @NotNull RandomEventWeightPreset weightPreset) {
        execute(location, getRandomMetricFor(location, weightPreset));
    }
    public void execute(@NotNull Block block, @NotNull RandomEventWeightPreset weightPreset) {
        execute(block.getLocation().add(0,1,0), weightPreset);
    }
    public void execute(@NotNull Location location, @NotNull RandomEventWeightPreset weightPreset, short repeats) {
        execute(location, getRandomMetricFor(location, weightPreset), repeats);
    }
    public void execute(@NotNull Block block, @NotNull RandomEventWeightPreset weightPreset, short repeats) {
        execute(block.getLocation().add(0,1,0), weightPreset, repeats);
    }
    public void execute(@NotNull Location location, @NotNull RandomEventsGame game, short repeats) {
        execute(location, game.weightPreset, repeats);
    }
    public void execute(@NotNull Block block, @NotNull RandomEventsGame game, short repeats) {
        execute(block.getLocation().add(0,1,0), game, repeats);
    }
    @Override
    public boolean execute(@NotNull RandomEventsPlayer player, M metric) {
        execute(player.getLocation(), metric);
        return true;
    }
}
