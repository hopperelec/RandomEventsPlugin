package uk.co.hopperelec.mc.randomevents.countdowns;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.mc.randomevents.TimeInSeconds;

public class BukkitSidebarCountdown extends BukkitPlayerSecondCountdown {
    @NotNull public final static String internalName = "countdown";
    @NotNull private final Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
    @NotNull private final Objective scoreboardObjective = scoreboard.registerNewObjective(internalName, Criteria.DUMMY, Component.text(DEFAULT_TEXT));

    public BukkitSidebarCountdown(@NotNull TimeInSeconds length, @NotNull JavaPlugin plugin, @NotNull Runnable action) {
        super(length, plugin, action);
        scoreboardObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
    }

    @Override
    protected void show() {
        for (Player player : players) {
            showToPlayer(player);
        }
    }

    @Override
    protected void update() {
        scoreboardObjective.getScore(internalName).setScore(getTimeRemaining().asInt());
    }

    @Override
    protected void hide() {
        for (Player player : players) {
            hideFromPlayer(player);
        }
    }

    private void showToPlayer(@NotNull Player player) {
        player.setScoreboard(scoreboard);
    }

    private void hideFromPlayer(@NotNull Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    @Override
    public void setText(@NotNull String text) {
        scoreboardObjective.displayName(Component.text(text));
    }

    @Override
    public void addPlayer(@NotNull Player player) {
        super.addPlayer(player);
        if (isOngoing()) showToPlayer(player);
    }

    @Override
    public void removePlayer(@NotNull Player player) {
        super.removePlayer(player);
        if (isOngoing()) hideFromPlayer(player);
    }
}
