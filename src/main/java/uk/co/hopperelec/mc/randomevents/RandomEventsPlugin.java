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
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uk.co.hopperelec.mc.randomevents.config.RandomEventWeightPreset;
import uk.co.hopperelec.mc.randomevents.config.RandomEventsConfig;
import uk.co.hopperelec.mc.randomevents.config.RandomEventsGameConfig;
import uk.co.hopperelec.mc.randomevents.eventtypes.*;
import uk.co.hopperelec.mc.randomevents.specialitems.functionality.SpecialItemBreakBlock;
import uk.co.hopperelec.mc.randomevents.specialitems.functionality.SpecialItemFunctionality;
import uk.co.hopperelec.mc.randomevents.specialitems.functionality.SpecialItemRandomEvent;
import uk.co.hopperelec.mc.randomevents.specialitems.functionality.SpecialItemSkipCountdown;
import uk.co.hopperelec.mc.randomevents.utils.TimeInSeconds;

import javax.annotation.CheckReturnValue;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE;
import static uk.co.hopperelec.mc.randomevents.RandomEventsPlayer.getRandomEventsPlayer;

public class RandomEventsPlugin extends JavaPlugin implements Listener {
    private RandomEventsGame game;
    public RandomEventsConfig config;
    @NotNull public final BiMap<String,RandomEventType> registeredEventTypes = HashBiMap.create();
    @NotNull public final BiMap<String,SpecialItemFunctionality> registeredSpecialItemFunctionalities = HashBiMap.create();
    @NotNull public final Random random = new Random();
    public final NamespacedKey SPECIAL_ITEM_USES_KEY = new NamespacedKey(this, "special-uses");


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
        return chooseRandom(registeredEventTypes.values(), weightPreset.eventTypes(), RandomEventType::getName);
    }
    @CheckReturnValue
    public @NotNull PositionalRandomEventType chooseRandomPositionalEvent(@NotNull RandomEventWeightPreset weightPreset) {
        return chooseRandom(
                registeredEventTypes.values().stream()
                        .filter(PositionalRandomEventType.class::isInstance)
                        .map(PositionalRandomEventType.class::cast)
                        .collect(Collectors.toSet()),
                weightPreset.eventTypes(),
                PositionalRandomEventType::getName
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

    private void registerDefaults() {
        registeredEventTypes.put("block", new RandomBlockEvent(this));
        registeredEventTypes.put("item", new RandomItemEvent(this));
        registeredEventTypes.put("entity", new RandomEntityEvent(this));
        registeredEventTypes.put("effect", new RandomEffectEvent(this));
        registeredEventTypes.put("teleport", new RandomTeleportEvent(this));
        registeredEventTypes.put("structure_template", new RandomStructureTemplateEvent(this));
        registeredEventTypes.put("structure", new RandomStructureEvent(this));
        registeredEventTypes.put("world_feature", new RandomWorldFeatureEvent(this));
        registeredSpecialItemFunctionalities.put("random_event", new SpecialItemRandomEvent(this));
        registeredSpecialItemFunctionalities.put("skip_countdown", new SpecialItemSkipCountdown(this));
        registeredSpecialItemFunctionalities.put("break_block", new SpecialItemBreakBlock(this));
    }

    @Override
    public void onEnable() {
        registerDefaults();
        try {
            config = getRandomEventsConfig(getConfigFile());
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize config file; is it formatted correctly?", e);
        }
        game = new RandomEventsGame(this, new RandomEventsGameConfig(config.defaultGameConfig()));
        getServer().getPluginManager().registerEvents(this, this);
        registerCommands();
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
            event.getDrops().addAll(game.getNewDropsFor(event.getEntity().getType()));
            if (event.getEntity() instanceof InventoryHolder inventoryHolder) {
                addFromInventory(event.getDrops(), inventoryHolder);
            }
            game.learn(event.getEntity().getType());
        }
    }

    // Special item causes
    @CheckReturnValue
    public @Nullable Map<String,JsonNode> getSpecialItemFunctionalityConfig(@NotNull Material material, @NotNull String potentialCause) {
        if (!game.isOngoing() || !game.hasSpecialItems()) return null;
        final Map<String,JsonNode> functionalityConfig = config.specialItems().get(material);
        if (functionalityConfig == null) return null;
        final JsonNode requiredCause = functionalityConfig.get("cause");
        if (requiredCause == null) throw new IllegalStateException("Special item "+material+" does not have a cause");
        if (!requiredCause.asText().equals(potentialCause)) return null;
        return functionalityConfig;
    }
    @CheckReturnValue
    public @NotNull SpecialItemFunctionality getSpecialItemFunctionality(@NotNull Material material, @NotNull Map<String,JsonNode> functionalityConfig) {
        final JsonNode functionalityTypeName = functionalityConfig.get("functionality");
        if (functionalityTypeName == null) throw new IllegalStateException("Special item "+material+" does not have a functionality type");
        final SpecialItemFunctionality functionality = registeredSpecialItemFunctionalities.get(functionalityTypeName.asText());
        if (functionality == null) throw new IllegalArgumentException("Special item "+material+" has an invalid functionality type");
        return functionality;
    }
    @CheckReturnValue
    public boolean doSpecialItemProbability(@NotNull Map<String,JsonNode> functionalityConfig) {
        final JsonNode probability = functionalityConfig.get("probability");
        return probability == null || random.nextFloat() < probability.floatValue();
    }

    public boolean decrementSpecialItemUses(
            @NotNull PersistentDataContainer persistentDataContainer,
            @NotNull Map<String,JsonNode> functionalityConfig
    ) {
        final JsonNode maxUses = functionalityConfig.get("uses");
        final short remainingUses = persistentDataContainer.getOrDefault(
                SPECIAL_ITEM_USES_KEY,
                PersistentDataType.SHORT,
                maxUses == null ? 1 : maxUses.shortValue()
        );
        if (remainingUses < 0) return false;
        if (remainingUses <= 1) return true;
        persistentDataContainer.set(SPECIAL_ITEM_USES_KEY, PersistentDataType.SHORT, (short)(remainingUses-1));
        return false;
    }

    public boolean useSpecialItemStack(@NotNull ItemStack itemStack, @NotNull Player player, @NotNull Event cause, @NotNull String causeName) {
        final Material material = itemStack.getType();
        final Map<String,JsonNode> functionalityConfig = getSpecialItemFunctionalityConfig(material, causeName);
        if (functionalityConfig == null) return false;
        if (doSpecialItemProbability(functionalityConfig)) {
            if (!getSpecialItemFunctionality(material, functionalityConfig).execute(material, game, player, cause)) {
                return false;
            }
        } else {
            player.sendMessage("The odds weren't in your favour...");
        }
        final ItemMeta itemMeta = itemStack.getItemMeta();
        if (decrementSpecialItemUses(itemMeta.getPersistentDataContainer(), functionalityConfig)) {
            itemStack.setAmount(itemStack.getAmount()-1);
        } else {
            itemStack.setItemMeta(itemMeta);
        }
        return true;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDropItem(@NotNull PlayerDropItemEvent event) {
        final Material material = event.getItemDrop().getItemStack().getType();
        final Map<String,JsonNode> functionalityConfig = getSpecialItemFunctionalityConfig(material, "drop");
        if (functionalityConfig != null) {
            if (doSpecialItemProbability(functionalityConfig)) {
                if (getSpecialItemFunctionality(material, functionalityConfig).execute(material, game, event.getPlayer(), event)) {
                    if (decrementSpecialItemUses(event.getItemDrop().getPersistentDataContainer(), functionalityConfig)) {
                        event.getItemDrop().remove();
                    }
                }
            }
        }
    }
    // Could do BlockDispenseEvent, but I'm lazy and this isn't really necessary anyway

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockPlace(@NotNull BlockPlaceEvent event) {
        if (useSpecialItemStack(event.getItemInHand(), event.getPlayer(), event, "place")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(@NotNull PlayerInteractEvent event) {
        if (
                event.getAction().isRightClick() &&
                useSpecialItemStack(event.getPlayer().getInventory().getItemInMainHand(), event.getPlayer(), event, "right_click")
        ) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTimerBlockPlace(@NotNull BlockPlaceEvent event) {
        if (isInOngoingGame(event.getPlayer())) {
            final Map<String, JsonNode> functionalityConfig = getSpecialItemFunctionalityConfig(event.getBlockPlaced().getType(), "block_and_timer");
            if (functionalityConfig != null) {
                final JsonNode maxUsesNode = functionalityConfig.get("uses");
                game.addTimerBlock(event.getBlockPlaced(), maxUsesNode == null ? 1 : maxUsesNode.shortValue());
            }
        }
    }
}