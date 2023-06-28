package uk.co.hopperelec.mc.randomevents;

import co.aikar.commands.PaperCommandManager;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockState;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RandomEventsPlugin extends JavaPlugin implements Listener {
    private final RandomEventsGame game = new RandomEventsGame(this);
    private boolean processingEvent = false;
    public final NamespacedKey ITEM_LORE_HASH_KEY = new NamespacedKey(this, "lore-hash");

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        final PaperCommandManager manager = new PaperCommandManager(this);
        manager.enableUnstableAPI("help");
        manager.getCommandContexts().registerContext(TimeInSeconds.class, c ->
                new TimeInSeconds((int) manager.getCommandContexts().getResolver(int.class).getContext(c))
        );
        manager.getCommandContexts().registerIssuerOnlyContext(RandomEventsGame.class, c -> game);
        manager.registerCommand(new RandomEventsCommands());
    }

    @Override
    public void onDisable() {
        game.removeLoreFromPlayers();
    }

    @EventHandler
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        game.joinPlayer(event.getPlayer());
    }

    private List<Item> dropItems(@NotNull List<ItemStack> items, @NotNull Location location) {
        return items.stream().map(
                itemStack -> location.getWorld().dropItemNaturally(location, itemStack)
        ).toList();
    }

    private boolean isInOngoingGame(@NotNull HumanEntity player) {
        return game.isOngoing() && game.hasPlayer((Player) player);
    }

    private void addFromInventory(@NotNull List<ItemStack> list, @NotNull Inventory inventory) {
        for (ItemStack itemStack : inventory.getContents()) {
            if (itemStack != null && itemStack.getType() != Material.AIR) {
                list.add(itemStack);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockDropItem(@NotNull BlockDropItemEvent event) {
        if (!isInOngoingGame(event.getPlayer()) || event.getPlayer().getGameMode() == GameMode.CREATIVE || processingEvent) return;

        event.setCancelled(true);
        final List<ItemStack> newDroppedItems = game.getNewDropsFor(event.getBlockState().getType());
        final BlockState blockState = event.getBlockState();
        if (blockState instanceof ShulkerBox) {
            addFromInventory(newDroppedItems, ((ShulkerBox)blockState).getInventory());
        } else if (blockState instanceof InventoryHolder) {
            final Material type = blockState.getType();
            boolean removedBlockItem = false;
            for (Item item : event.getItems()) {
                final ItemStack itemStack = item.getItemStack();
                if (!removedBlockItem && itemStack.getType() == type) {
                    removedBlockItem = true;
                    if (itemStack.getAmount() <= 1) continue;
                    itemStack.setAmount(itemStack.getAmount()-1);
                }
                newDroppedItems.add(itemStack);
            }
        }

        game.learn(event.getBlockState().getType());
        processingEvent = true;
        new BlockDropItemEvent(
                event.getBlock(),
                event.getBlockState(),
                event.getPlayer(),
                dropItems(newDroppedItems, event.getBlock().getLocation())
        ).callEvent();
        processingEvent = false;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityDeath(@NotNull EntityDeathEvent event) {
        if (game.isOngoing()) {
            event.getDrops().clear();
            event.getDrops().addAll(game.getNewDropsFor(event.getEntity()));
            if (event.getEntity() instanceof InventoryHolder) {
                addFromInventory(event.getDrops(), ((InventoryHolder)event.getEntity()).getInventory());
            }
            game.learn(event.getEntity().getType());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryOpen(@NotNull InventoryOpenEvent event) {
        game.handleLoreFor(event.getInventory());
    }
    @EventHandler
    public void onInventoryClose(@NotNull InventoryCloseEvent event) {
        if (event.getInventory().getViewers().stream().noneMatch(this::isInOngoingGame)) {
            game.removeLoreFrom(event.getInventory());
        }
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        // Excessive, but more readable and can be used to fix bugged items
        game.handleLoreFor(event.getCursor());
    }
    @EventHandler
    public void onPrepareItemCraft(@NotNull PrepareItemCraftEvent event) {
        if (isInOngoingGame(event.getView().getPlayer())) {
            game.addLoreTo(event.getInventory().getResult());
        }
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemSpawn(@NotNull ItemSpawnEvent event) {
        if (game.isOngoing()) {
            game.removeLoreFrom(event.getEntity().getItemStack());
        }
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityPickupItem(@NotNull EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player) {
            game.handleLoreFor(event.getItem().getItemStack());
        }
    }
    @EventHandler
    public void onInventoryMoveItem(@NotNull InventoryMoveItemEvent event) {
        if (game.isOngoing()) {
            // For some reason, lore not only added/removed to/from item moved, but also entire stack it originated from
            // I don't believe this can lead to any remnants, though, so it's only a minor visual issue
            if (event.getDestination().getViewers().stream().anyMatch(this::isInOngoingGame)) {
                game.addLoreTo(event.getItem());
            } else {
                game.removeLoreFrom(event.getItem());
            }
        }
    }
    // Current known issues: Campfires, jukeboxes, /give, items given by other plugins
}