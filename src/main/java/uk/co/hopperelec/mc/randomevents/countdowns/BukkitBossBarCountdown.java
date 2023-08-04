package uk.co.hopperelec.mc.randomevents.countdowns;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.mc.randomevents.TimeInSeconds;

public class BukkitBossBarCountdown extends BukkitVisibleSecondCountdown {
    @NotNull public final BossBar bossBar = Bukkit.createBossBar(DEFAULT_TEXT, BarColor.YELLOW, BarStyle.SOLID);

    public BukkitBossBarCountdown(@NotNull TimeInSeconds length, @NotNull JavaPlugin plugin, @NotNull Runnable action) {
        super(length, plugin, action);
        hide();
    }

    @Override
    protected void show() {
        bossBar.setVisible(true);
    }

    @Override
    protected void update() {
        bossBar.setProgress(getPercentageComplete());
    }

    @Override
    protected void hide() {
        bossBar.setVisible(false);
    }

    @Override
    public void setText(@NotNull String text) {
        bossBar.setTitle(text);
    }

    @Override
    public void addPlayer(@NotNull Player player) {
        bossBar.addPlayer(player);
    }

    @Override
    public void removePlayer(@NotNull Player player) {
        bossBar.removePlayer(player);
    }
}
