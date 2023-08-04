package uk.co.hopperelec.mc.randomevents.countdowns;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.mc.randomevents.TimeInSeconds;

import java.util.HashSet;
import java.util.Set;

public abstract class BukkitPlayerSecondCountdown extends BukkitVisibleSecondCountdown {
    @NotNull protected final Set<Player> players = new HashSet<>();

    public BukkitPlayerSecondCountdown(@NotNull TimeInSeconds length, @NotNull JavaPlugin plugin, @NotNull Runnable action) {
        super(length, plugin, action);
    }

    @Override
    public void addPlayer(@NotNull Player player) {
        players.add(player);
    }

    @Override
    public void removePlayer(@NotNull Player player) {
        players.remove(player);
    }
}
