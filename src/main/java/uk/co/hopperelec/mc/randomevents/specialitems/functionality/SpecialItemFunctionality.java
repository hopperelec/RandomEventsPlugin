package uk.co.hopperelec.mc.randomevents.specialitems.functionality;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.mc.randomevents.RandomEventsGame;
import uk.co.hopperelec.mc.randomevents.RandomEventsPlayer;
import uk.co.hopperelec.mc.randomevents.RandomEventsPlugin;

import javax.annotation.CheckReturnValue;

public abstract class SpecialItemFunctionality {
    @NotNull protected final RandomEventsPlugin plugin;

    public SpecialItemFunctionality(@NotNull RandomEventsPlugin plugin) {
        this.plugin = plugin;
    }

    protected boolean execute(@NotNull Material specialItem, @NotNull RandomEventsGame game) {
        throw new UnsupportedOperationException("Special item functionality for "+specialItem+" triggered by unsupported cause");
    }
    public boolean execute(@NotNull Block block, @NotNull RandomEventsGame game) {
        return execute(block.getType(), game);
    }
    public boolean execute(@NotNull Material specialItem, @NotNull RandomEventsPlayer player, @NotNull Event cause) {
        return execute(specialItem, player.game);
    }
    public boolean execute(@NotNull Material specialItem, @NotNull RandomEventsGame game, @NotNull Player player, @NotNull Event cause) {
        return execute(specialItem, RandomEventsPlayer.getRandomEventsPlayer(player, game), cause);
    }

    @CheckReturnValue
    public @NotNull String getName() {
        return plugin.registeredSpecialItemFunctionalities.inverse().get(this);
    }
}
