package uk.co.hopperelec.mc.randomevents.specialitems.functionality;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.mc.randomevents.RandomEventsGame;
import uk.co.hopperelec.mc.randomevents.RandomEventsPlugin;

public class SpecialItemSkipCountdown extends SpecialItemFunctionality {
    public SpecialItemSkipCountdown(@NotNull RandomEventsPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean execute(@NotNull Material specialItem, @NotNull RandomEventsGame game) {
        game.skipCountdown();
        return true;
    }
}
