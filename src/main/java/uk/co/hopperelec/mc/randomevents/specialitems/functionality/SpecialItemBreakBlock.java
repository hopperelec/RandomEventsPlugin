package uk.co.hopperelec.mc.randomevents.specialitems.functionality;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uk.co.hopperelec.mc.randomevents.RandomEventsPlayer;
import uk.co.hopperelec.mc.randomevents.RandomEventsPlugin;

public class SpecialItemBreakBlock extends SpecialItemFunctionality {
    public SpecialItemBreakBlock(@NotNull RandomEventsPlugin plugin) {
        super(plugin);
    }

    public boolean execute(@NotNull RandomEventsPlayer player, @Nullable Block block) {
        if (block == null || block.getType() == Material.AIR) return false;
        return new BlockBreakEvent(block, player.spigotPlayer).callEvent() && block.breakNaturally();
    }

    @Override
    public boolean execute(@NotNull Material specialItem, @NotNull RandomEventsPlayer player, @NotNull Event cause) {
        if (cause instanceof PlayerInteractEvent event) return execute(player, event.getClickedBlock());
        if (cause instanceof BlockPlaceEvent event) return execute(player, event.getBlockAgainst());
        if (cause instanceof PlayerDropItemEvent event) return execute(player, event.getItemDrop().getLocation().getBlock().getRelative(BlockFace.DOWN));
        throw new UnsupportedOperationException("Special item functionality 'break_block' triggered by a cause for which a block can not be identified");
    }
}
