package uk.co.hopperelec.mc.randomevents.countdowns;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uk.co.hopperelec.mc.randomevents.utils.TimeInSeconds;

public class BukkitActionBarCountdown extends BukkitPlayerSecondCountdown {
    @Nullable private String extraText;

    public BukkitActionBarCountdown(@NotNull TimeInSeconds length, @NotNull JavaPlugin plugin, @NotNull Runnable action) {
        super(length, plugin, action);
    }

    @Override
    protected void update() {
        for (Player player : players) {
            player.sendActionBar(Component.text(DEFAULT_TEXT+" "+getTimeRemaining().asInt()+" ("+extraText+")"));
        }
    }

    @Override
    public void setExtraText(@Nullable String extraText) {
        this.extraText = extraText;
    }
}
