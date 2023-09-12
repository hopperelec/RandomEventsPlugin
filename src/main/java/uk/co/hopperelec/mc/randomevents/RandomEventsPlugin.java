package uk.co.hopperelec.mc.randomevents;

import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.MessageKeys;
import co.aikar.commands.PaperCommandManager;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import com.destroystokyo.paper.event.block.BlockDestroyEvent;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import io.papermc.paper.event.block.BlockBreakBlockEvent;
import net.kyori.adventure.sound.Sound;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.mc.randomevents.config.RandomEventWeightPreset;
import uk.co.hopperelec.mc.randomevents.config.RandomEventsConfig;
import uk.co.hopperelec.mc.randomevents.config.RandomEventsGameConfig;
import uk.co.hopperelec.mc.randomevents.eventtypes.*;

import javax.annotation.CheckReturnValue;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;

import static com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE;
import static uk.co.hopperelec.mc.randomevents.RandomEventsPlayer.getRandomEventsPlayer;

public class RandomEventsPlugin extends JavaPlugin implements Listener {
    private RandomEventsGame game;
    public RandomEventsConfig config;
    @NotNull public final BiMap<String,RandomEventType> registeredEventTypes = HashBiMap.create();
    @NotNull public final Random random = new Random();
    public final NamespacedKey ITEM_LORE_HASH_KEY = new NamespacedKey(this, "lore-hash");


    @CheckReturnValue
    public <T> T getRandomFrom(@NotNull T @NotNull [] array) {
        return array[random.nextInt(array.length)];
    }

    @CheckReturnValue
    public <M> M chooseRandom(@NotNull Map<M,Float> weights) {
        double totalWeight = weights.values().stream().mapToDouble(Float::floatValue).sum();
        final double randomValue = random.nextDouble(totalWeight);
        double acc = 0;
        for (Map.Entry<M,Float> possibleValue : weights.entrySet()) {
            acc += possibleValue.getValue();
            if (acc > randomValue) return possibleValue.getKey();
        }
        return null;
    }
    @CheckReturnValue
    public <M> M chooseRandom(@NotNull Set<M> possibleValues, @NotNull Map<String,Float> weightsByKey, Function<M,String> mapper) {
        final Map<M,Float> weights = new HashMap<>();
        for (M value : possibleValues) {
            final String key = mapper.apply(value).toUpperCase();
            weights.put(value,
                    weightsByKey.entrySet().stream()
                        .filter(weightByKey -> weightByKey.getKey().toUpperCase().equals(key))
                        .map(Map.Entry::getValue)
                        .findAny().orElse(1f)
            );
        }
        return chooseRandom(weights);
    }

    @CheckReturnValue
    public @NotNull RandomEventType chooseRandomEvent(@NotNull RandomEventWeightPreset weightPreset) {
        return chooseRandom(
                new HashSet<>(registeredEventTypes.values()),
                weightPreset.eventTypes(),
                eventName -> registeredEventTypes.inverse().get(eventName)
        );
    }


    @CheckReturnValue
    private @NotNull File getConfigFile() {
        saveDefaultConfig();
        return new File(getDataFolder(), "config.yml");
    }

    @CheckReturnValue
    private @NotNull RandomEventsConfig getRandomEventsConfig(File configFile) throws IOException {
        final ObjectMapper configMapper = YAMLMapper.builder()
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .build()
                .setPropertyNamingStrategy(SNAKE_CASE)
                .registerModule(new GuavaModule());
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Sound.class, new JsonDeserializer<>() {
            @Override
            public Sound deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                final JsonNode node = p.getCodec().readTree(p);
                return Sound.sound(
                        org.bukkit.Sound.valueOf(node.get("sound").asText()),
                        Sound.Source.PLAYER,
                        node.get("volume").floatValue(),
                        node.get("pitch").floatValue()
                );
            }
        });
        module.addKeyDeserializer(PotionEffectType.class, new KeyDeserializer() {
            @Override
            public Object deserializeKey(String key, DeserializationContext ctxt) {
                return PotionEffectType.getByName(key);
            }
        });
        module.addDeserializer(TimeInSeconds.class, new JsonDeserializer<>() {
            @Override
            public TimeInSeconds deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                return new TimeInSeconds(p.getIntValue());
            }
        });
        configMapper.registerModule(module);
        return configMapper.readValue(configFile, RandomEventsConfig.class);
    }

    private void registerCommands() {
        final PaperCommandManager manager = new PaperCommandManager(this);
        manager.enableUnstableAPI("help");
        manager.getCommandContexts().registerContext(TimeInSeconds.class, c ->
                new TimeInSeconds((int) manager.getCommandContexts().getResolver(int.class).getContext(c))
        );
        manager.getCommandContexts().registerContext(RandomEventType.class, c -> {
                final RandomEventType match = registeredEventTypes.get(c.popFirstArg().toLowerCase());
                if (match == null) throw new InvalidCommandArgument(MessageKeys.PLEASE_SPECIFY_ONE_OF, "{valid}", String.join(", ",registeredEventTypes.keySet()));
                return match;
            }
        );
        manager.getCommandContexts().registerContext(RandomEventsPlayer.class, c ->
                getRandomEventsPlayer(((OnlinePlayer) manager.getCommandContexts().getResolver(OnlinePlayer.class).getContext(c)).player, game)
        );
        manager.getCommandContexts().registerIssuerOnlyContext(RandomEventsGame.class, c -> game);
        manager.getCommandCompletions().registerCompletion("randomeventtypes", c -> registeredEventTypes.keySet());
        manager.registerCommand(new RandomEventsCommands());
    }

    private void registerDefaultEventTypes() {
        registeredEventTypes.put("block", new RandomBlockEvent(this));
        registeredEventTypes.put("item", new RandomItemEvent(this));
        registeredEventTypes.put("entity", new RandomEntityEvent(this));
        registeredEventTypes.put("effect", new RandomEffectEvent(this));
        registeredEventTypes.put("teleport", new RandomTeleportEvent(this));
        registeredEventTypes.put("structure", new RandomStructureEvent(this));
    }

    @Override
    public void onEnable() {
        registerDefaultEventTypes();
        try {
            config = getRandomEventsConfig(getConfigFile());
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize config file; is it formatted correctly?", e);
        }
        game = new RandomEventsGame(this, new RandomEventsGameConfig(config.defaultGameConfig()));
        getServer().getPluginManager().registerEvents(this, this);
        registerCommands();
    }

    @Override
    public void onDisable() {
        game.removeLoreFromPlayers();
    }


    @EventHandler
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        game.joinPlayer(event.getPlayer());
    }

    @CheckReturnValue
    private boolean isInOngoingGame(@NotNull HumanEntity player) {
        return game.isOngoing() && game.hasPlayer((Player) player);
    }

    private void addFromInventory(@NotNull List<ItemStack> list, @NotNull InventoryHolder inventoryHolder) {
        for (ItemStack itemStack : inventoryHolder.getInventory().getContents()) {
            if (itemStack != null && itemStack.getType() != Material.AIR) {
                list.add(itemStack);
            }
        }
    }


    // Random block drops
    private boolean doTileDrops(@NotNull World world) {
        return world.getGameRuleValue(GameRule.DO_TILE_DROPS) == Boolean.TRUE;
    }
    private boolean dropItemsFor(@NotNull World world) {
        return game.isOngoing() && doTileDrops(world);
    }
    private boolean dropItemsFor(@NotNull Block block) {
        return dropItemsFor(block.getWorld());
    }

    private @NotNull List<ItemStack> getDroppedItemStacks(@NotNull Block block) {
        final List<ItemStack> newDroppedItems = game.getNewDropsFor(block.getType());
        if (block instanceof InventoryHolder inventoryHolder) addFromInventory(newDroppedItems, inventoryHolder);
        game.learn(block.getType());
        return newDroppedItems;
    }

    private @NotNull List<Item> dropItems(@NotNull Block block) {
        return getDroppedItemStacks(block).stream().map(
                itemStack -> block.getWorld().dropItemNaturally(block.getLocation(), itemStack)
        ).toList();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(@NotNull BlockBreakEvent event) {
        if (!isInOngoingGame(event.getPlayer())) return;
        if (!event.isDropItems()) return;
        if (!doTileDrops(event.getBlock().getWorld())) return;
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE) return;
        if (event.getBlock().getBlockData().requiresCorrectToolForDrops() && !event.getBlock().isValidTool(event.getPlayer().getInventory().getItemInMainHand())) return;

        event.setDropItems(false);
        new BlockDropItemEvent(
                event.getBlock(), event.getBlock().getState(), event.getPlayer(), dropItems(event.getBlock())
        ).callEvent();
    }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockDestroy(@NotNull BlockDestroyEvent event) {
        if (dropItemsFor(event.getBlock()) && event.willDrop()) {
            event.setWillDrop(false);
            // Needs to be delayed, because if a cancelled BlockBreakEvent triggers this then the drop items would be otherwise removed
            getServer().getScheduler().scheduleSyncDelayedTask(this, () -> dropItems(event.getBlock()));
        }
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLeafDecay(@NotNull LeavesDecayEvent event) {
        if (dropItemsFor(event.getBlock())) {
            dropItems(event.getBlock());
            event.setCancelled(true);
            event.getBlock().setType(Material.AIR);
        }
    }
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBreakBlock(@NotNull BlockBreakBlockEvent event) {
        if (dropItemsFor(event.getBlock())) {
            event.getDrops().clear();
            event.getDrops().addAll(getDroppedItemStacks(event.getBlock()));
        }
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(@NotNull BlockExplodeEvent event) {
        if (dropItemsFor(event.getBlock())) {
            for (Block block : event.blockList()) {
                if (random.nextFloat() < event.getYield()) {
                    dropItems(block);
                }
            }
            event.setYield(0);
        }
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(@NotNull EntityExplodeEvent event) {
        if (dropItemsFor(event.getEntity().getWorld())) {
            for (Block block : event.blockList()) {
                if (random.nextFloat() < event.getYield()) {
                    dropItems(block);
                }
            }
            event.setYield(0);
        }
    }


    // Random entity drops
    // Current known issues: Campfires, jukeboxes, /give, items given by other plugins
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityDeath(@NotNull EntityDeathEvent event) {
        if (game.isOngoing()) {
            event.getDrops().clear();
            event.getDrops().addAll(game.getNewDropsFor(event.getEntity()));
            if (event.getEntity() instanceof InventoryHolder inventoryHolder) {
                addFromInventory(event.getDrops(), inventoryHolder);
            }
            game.learn(event.getEntity().getType());
        }
    }


    // Item lore maintenance
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
        game.resetLoreFor(event.getCursor());
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
}