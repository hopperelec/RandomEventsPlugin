package uk.co.hopperelec.mc.randomevents;

import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.MessageKeys;
import co.aikar.commands.PaperCommandManager;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import net.kyori.adventure.sound.Sound;
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
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.mc.randomevents.config.RandomEventWeightPreset;
import uk.co.hopperelec.mc.randomevents.config.RandomEventsConfig;
import uk.co.hopperelec.mc.randomevents.config.RandomEventsGameConfig;
import uk.co.hopperelec.mc.randomevents.eventtypes.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE;
import static uk.co.hopperelec.mc.randomevents.RandomEventsPlayer.getRandomEventsPlayer;

public class RandomEventsPlugin extends JavaPlugin implements Listener {
    private RandomEventsGame game;
    public RandomEventsConfig config;
    @NotNull public final BiMap<String,RandomEventType> registeredEventTypes = HashBiMap.create();
    @NotNull public final Random random = new Random();
    private boolean processingEvent = false;
    public final NamespacedKey ITEM_LORE_HASH_KEY = new NamespacedKey(this, "lore-hash");


    public <T> T getRandomFrom(@NotNull T @NotNull [] array) {
        return array[random.nextInt(array.length)];
    }

    public <M> M chooseRandom(@NotNull Collection<M> possibleValues, @NotNull Map<M,Float> weights) {
        double totalWeight = 0;
        for (M possibleValue : possibleValues) {
            final Float weight = weights.get(possibleValue);
            totalWeight += weight == null ? 1 : weight;
        }
        final double randomValue = random.nextDouble(totalWeight);
        double acc = 0;
        for (M possibleValue : possibleValues) {
            final Float weight = weights.get(possibleValue);
            acc += weight == null ? 1 : weight;
            if (acc > randomValue) return possibleValue;
        }
        return null;
    }

    public @NotNull RandomEventType chooseRandomEvent(@NotNull RandomEventWeightPreset weightPreset) {
        final Map<RandomEventType,Float> weights = new HashMap<>();
        for (Map.Entry<String,Float> weightByName : weightPreset.eventTypes().entrySet()) {
            weights.put(registeredEventTypes.get(weightByName.getKey()), weightByName.getValue());
        }
        return chooseRandom(registeredEventTypes.values(), weights);
    }


    private @NotNull File getConfigFile() {
        saveDefaultConfig();
        return new File(getDataFolder(), "config.yml");
    }

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
    // Current known issues: Campfires, jukeboxes, /give, items given by other plugins
}