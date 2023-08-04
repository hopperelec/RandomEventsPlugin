package uk.co.hopperelec.mc.randomevents.countdowns;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uk.co.hopperelec.mc.randomevents.TimeInSeconds;

public abstract class BukkitVisibleSecondCountdown extends BukkitSecondCountdown {
    @NotNull public final static String DEFAULT_TEXT = "Next event:";

    public BukkitVisibleSecondCountdown(@NotNull TimeInSeconds length, @NotNull JavaPlugin plugin, @NotNull Runnable action) {
        super(length, plugin, action);
    }

    @Override
    public void tick() {
        super.tick();
        update();
    }

    @Override
    public void start() {
        super.start();
        show();
    }

    @Override
    public void pause() {
        super.pause();
        hide();
    }

    public void setExtraText(@Nullable String extraText) {
        setText(extraText == null ? DEFAULT_TEXT : DEFAULT_TEXT+" "+extraText);
    }

    protected void show() {}
    protected abstract void update();
    protected void hide() {}
    protected void setText(@NotNull String text) {}
    public abstract void addPlayer(@NotNull Player player);
    public abstract void removePlayer(@NotNull Player player);
}
