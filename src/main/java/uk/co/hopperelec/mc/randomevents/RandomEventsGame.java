package uk.co.hopperelec.mc.randomevents;

import com.fasterxml.jackson.databind.JsonNode;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SpawnEggMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uk.co.hopperelec.mc.randomevents.config.RandomEventScope;
import uk.co.hopperelec.mc.randomevents.config.RandomEventWeightPreset;
import uk.co.hopperelec.mc.randomevents.config.RandomEventsGameConfig;
import uk.co.hopperelec.mc.randomevents.countdowns.*;
import uk.co.hopperelec.mc.randomevents.eventtypes.*;
import uk.co.hopperelec.mc.randomevents.utils.TimeInSeconds;

import javax.annotation.CheckReturnValue;
import java.util.*;

import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;
import static uk.co.hopperelec.mc.randomevents.RandomEventsPlayer.getRandomEventsPlayer;

public class RandomEventsGame {
    @NotNull private final RandomEventsPlugin plugin;
    private final RandomEventsGameConfig config;
    private final BukkitSecondCountdown countdown;
    public RandomEventWeightPreset weightPreset;
    @NotNull public final static TextComponent LORE_PREFIX = Component.text("Drops: ", YELLOW, BOLD).decoration(ITALIC, false);
    @NotNull public final static TextComponent UNKNOWN_DROP_TEXT = LORE_PREFIX.append(Component.text("Unknown").decoration(OBFUSCATED, true));
    @NotNull private final Map<Block,Short> timerBlocks = new HashMap<>();
    @NotNull private final Set<RandomEventsPlayer> players = new HashSet<>();
    @NotNull private final Set<Object> learnedDropSeeds = new HashSet<>();

    public RandomEventsGame(@NotNull RandomEventsPlugin plugin, @NotNull RandomEventsGameConfig config) {
        if (config.lootSeed == -1) config.lootSeed = new Random().nextLong();
        this.plugin = plugin;
        this.config = config;
        if (!setWeightPreset(config.weightPresetName)) throw new IllegalArgumentException("Tried to set a game config which contains the name of a weight preset which does not exist!");
        this.countdown = switch(plugin.config.countdownLocation()) {
            case NONE -> new BukkitSecondCountdown(config.countdownLength, plugin, this::onEventTimerEnd);
            case BOSSBAR -> new BukkitBossBarCountdown(config.countdownLength, plugin, this::onEventTimerEnd);
            case SIDEBAR -> new BukkitSidebarCountdown(config.countdownLength, plugin, this::onEventTimerEnd);
            case ACTION_BAR -> new BukkitActionBarCountdown(config.countdownLength, plugin, this::onEventTimerEnd);
        };
    }

    public void start() {
        resetCountdown();
        countdown.start();
    }

    public void resetCountdown() {
        countdown.reset();
    }
    public void skipCountdown() {
        countdown.skip();
    }

    public void stop() {
        if (!isOngoing()) {
            throw new IllegalStateException("Tried to stop a game which isn't ongoing");
        }
        countdown.pause();
        clearPotionEffects();
    }

    @CheckReturnValue
    public boolean isOngoing() {
        return countdown.isOngoing();
    }

    public void onEventTimerEnd() {
        clearPotionEffects();
        doRandomEvent();
        useTimerBlocks();
    }


    public void addTimerBlock(@NotNull Block block, short remainingUses) {
        timerBlocks.put(block, remainingUses);
    }
    public @NotNull Map<Block,Map<String, JsonNode>> getTimerBlocks() {
        final Map<Block,Map<String, JsonNode>> blocks = new HashMap<>();
        for (Block block : timerBlocks.keySet()) {
            final Map<String, JsonNode> functionalityConfig = plugin.getSpecialItemFunctionalityConfig(block.getType(), "block_and_timer");
            if (functionalityConfig == null) {
                timerBlocks.remove(block);
            } else {
                blocks.put(block, functionalityConfig);
            }
        }
        return blocks;
    }

    public void useTimerBlocks() {
        for (Map.Entry<Block,Map<String, JsonNode>> timerBlock : getTimerBlocks().entrySet()) {
            useTimerBlock(timerBlock.getKey(), timerBlock.getValue());
        }
    }
    public void useTimerBlock(@NotNull Block block, Map<String, JsonNode> functionalityConfig) {
        if (runTimerBlock(block, functionalityConfig)) {
            block.setType(Material.AIR);
            timerBlocks.remove(block);
        }
    }
    private boolean runTimerBlock(@NotNull Block block, Map<String, JsonNode> functionalityConfig) {
        final Material material = block.getType();
        if (plugin.doSpecialItemProbability(functionalityConfig)) {
            if (!plugin.getSpecialItemFunctionality(material, functionalityConfig).execute(block, this)) { // TODO: If the functionality is random_event, should share scope with players
                return false;
            }
        }
        final short remainingUses = timerBlocks.get(block);
        if (remainingUses < 0) return false;
        if (remainingUses <= 1) return true;
        timerBlocks.put(block, (short)(remainingUses-1));
        return false;
    }


    public void doRandomEvent() {
        if (config.shareScope == RandomEventScope.NONE) {
            for (RandomEventsPlayer player : players) {
                doRandomEvent(player);
            }
        } else {
            final RandomEventType randomEventType = plugin.chooseRandomEvent(weightPreset);
            for (RandomEventsPlayer player : players) {
                doRandomEvent(player, randomEventType);
            }
        }
    }
    public void doRandomEvent(@NotNull RandomEventType randomEventType) {
        if (randomEventType instanceof PolyMetricRandomEventType<?> pRET) {
            doRandomEvent(pRET);
        } else {
            for (RandomEventsPlayer player : players) {
                doRandomEvent(player, randomEventType);
            }
        }
    }
    public <M> void doRandomEvent(@NotNull PolyMetricRandomEventType<M> randomEventType) {
        if (config.shareScope == RandomEventScope.ALL) {
            final List<M> metrics = new ArrayList<>();
            for (RandomEventsPlayer player : players) {
                M metric = null;
                for (M pastMetric : metrics) {
                    if (randomEventType.isValidMetric(pastMetric, player)) {
                        metric = pastMetric;
                        break;
                    }
                }
                if (metric == null) {
                    metric = randomEventType.getRandomMetricFor(player);
                    metrics.add(metric);
                }
                doRandomEvent(player, randomEventType, metric);
            }
        } else {
            for (RandomEventsPlayer player : players) {
                doRandomEvent(player, randomEventType);
            }
        }
    }
    public void doRandomEvent(@NotNull RandomEventsPlayer player) {
        doRandomEvent(player, plugin.chooseRandomEvent(weightPreset));
    }
    public void doRandomEvent(@NotNull Block block) {
        doRandomEvent(block, plugin.chooseRandomPositionalEvent(weightPreset));
    }
    public void doRandomEvent(@NotNull RandomEventsPlayer player, @NotNull RandomEventType randomEventType) {
        if (randomEventType instanceof MonoMetricRandomEventType mRET) {
            doRandomEvent(player, mRET);
        } else if (randomEventType instanceof PolyMetricRandomEventType<?> pRET) {
            doRandomEvent(player, pRET);
        } else {
            throw new IllegalStateException("Sealed class RandomEventType should only be MonoMetric or PolyMetric but is somehow neither");
        }
    }
    public void doRandomEvent(@NotNull Block block, @NotNull PositionalRandomEventType randomEventType) {
        if (randomEventType instanceof PositionalMonoMetricRandomEventType pmRET) {
            pmRET.execute(block, this);
        } else if (randomEventType instanceof PositionalPolyMetricRandomEventType<?> ppRET) {
            doRandomEvent(block, ppRET);
        } else {
            throw new IllegalStateException("Sealed class RandomEventType should only be MonoMetric or PolyMetric but is somehow neither");
        }
    }
    public void doRandomEvent(@NotNull RandomEventsPlayer player, @NotNull MonoMetricRandomEventType randomEventType) {
        final MonoMetricRandomEvent event = new MonoMetricRandomEvent(this, player, randomEventType);
        if (event.callEvent()) {
            if (event.type.execute(player)) {
                event.type.success(player);
            }
        }
    }
    public <M> void doRandomEvent(@NotNull RandomEventsPlayer player, @NotNull PolyMetricRandomEventType<M> randomEventType) {
        doRandomEvent(player, randomEventType, randomEventType.getRandomMetricFor(player));
    }
    public <M> void doRandomEvent(@NotNull Block block, @NotNull PositionalPolyMetricRandomEventType<M> randomEventType) {
        doRandomEvent(block, randomEventType, randomEventType.getRandomMetricFor(block, this));
    }
    public <M> void doRandomEvent(@NotNull RandomEventsPlayer player, @NotNull PolyMetricRandomEventType<M> randomEventType, short repeats) {
        doRandomEvent(player, randomEventType, randomEventType.getRandomMetricFor(player), repeats);
    }
    public <M> void doRandomEvent(@NotNull RandomEventsPlayer player, @NotNull PolyMetricRandomEventType<M> type, M metric) {
        doRandomEvent(player, type, metric, (short) 1);
    }
    public <M> void doRandomEvent(@NotNull Block block, @NotNull PositionalPolyMetricRandomEventType<M> type, M metric) {
        type.execute(block, metric, (short)1);
    }
    public <M> void doRandomEvent(@NotNull RandomEventsPlayer player, @NotNull PolyMetricRandomEventType<M> type, M metric, short repeats) {
        final PolyMetricRandomEvent<M> event = new PolyMetricRandomEvent<>(this, player, type, metric, repeats);
        if (event.callEvent()) {
            final short successfulRepeats = event.type.execute(player, metric, repeats);
            if (successfulRepeats != 0) {
                event.type.success(player, metric, successfulRepeats);
            }
        }
    }


    @CheckReturnValue
    private @NotNull Material getNewDropFor(@NotNull Object seed) {
        final Random random = new Random(config.lootSeed + seed.hashCode());
        while (true) {
            final Material newDrop = Material.values()[random.nextInt(Material.values().length)];
            if (newDrop.isItem()) return newDrop;
        }
    }
    @CheckReturnValue
    public List<ItemStack> getNewDropsFor(Object seed) {
        final List<ItemStack> newDroppedItems = new ArrayList<>();
        newDroppedItems.add(new ItemStack(getNewDropFor(seed)));
        return newDroppedItems;
    }

    @CheckReturnValue
    public @NotNull Component getDropsTextForItems(@NotNull List<ItemStack> newDroppedItems) {
        Component component = LORE_PREFIX.append(Component.translatable(newDroppedItems.remove(0).translationKey(), BLUE).decoration(BOLD, false));
        for (ItemStack newDroppedItem : newDroppedItems) {
            component = component.append(Component.text(", ", DARK_GRAY));
            component = component.append(Component.translatable(newDroppedItem.translationKey(), BLUE));
            if (newDroppedItem.getAmount() != 1) {
                component = component.append(Component.text(newDroppedItem.getAmount(), WHITE));
            }
        }
        return component;
    }
    @CheckReturnValue
    public @NotNull Component getDropsTextFor(@NotNull Object seed) {
        if (isLearned(seed)) return getDropsTextForItems(getNewDropsFor(seed));
        return UNKNOWN_DROP_TEXT;
    }

    @CheckReturnValue
    public @Nullable Object getSeedFor(@Nullable ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return null;
        }
        if (itemStack.getType().isBlock()) {
            return itemStack.getType();
        }
        if (itemStack.getItemMeta() instanceof SpawnEggMeta) {
            return EntityType.valueOf(itemStack.getType().name().replace("_SPAWN_EGG", ""));
        }
        return null;
    }


    @CheckReturnValue
    public boolean isLearned(@NotNull Object seed) {
        return learnedDropSeeds.contains(seed);
    }

    public void learn(@NotNull Object seed) {
        if (learnedDropSeeds.add(seed)) {
            if (plugin.config.learnDropSoundEffect() != null) {
                for (RandomEventsPlayer player : players) {
                    player.playSound(plugin.config.learnDropSoundEffect());
                }
            }
        }
    }

    public void unlearn(@NotNull Object seed) {
       learnedDropSeeds.remove(seed);
    }


    @CheckReturnValue
    public boolean hasPlayer(@NotNull Player player) {
        return hasPlayer(getRandomEventsPlayer(player, this));
    }
    @CheckReturnValue
    public boolean hasPlayer(@NotNull RandomEventsPlayer player) {
        return players.contains(player);
    }

    @CheckReturnValue
    public @NotNull Set<RandomEventsPlayer> getPlayers() {
        return players;
    }

    public void joinPlayer(@NotNull Player player) {
        if (countdown instanceof BukkitVisibleSecondCountdown visibleCountdown) {
            visibleCountdown.addPlayer(player);
        }
        players.add(getRandomEventsPlayer(player, this));
    }

    public void removePlayer(@NotNull Player player) {
        if (countdown instanceof BukkitVisibleSecondCountdown visibleCountdown) {
            visibleCountdown.removePlayer(player);
        }
        players.remove(getRandomEventsPlayer(player, this));
    }

    public void clearPotionEffects() {
        for (RandomEventsPlayer player : players) {
            player.clearPotionEffect();
        }
    }


    @CheckReturnValue
    public @NotNull TimeInSeconds getCountdownLength() {
        return countdown.getLength();
    }
    public void setCountdownLength(@NotNull TimeInSeconds newLength) {
        config.countdownLength = newLength;
        countdown.setLength(newLength);
    }

    @CheckReturnValue
    public @NotNull TimeInSeconds getTimeTillNextEvent() {
        return countdown.getTimeRemaining();
    }
    public void setTimeTillNextEvent(@NotNull TimeInSeconds newTimeRemaining) {
        countdown.setTimeRemaining(newTimeRemaining);
    }

    @CheckReturnValue
    public long getLootSeed() {
        return config.lootSeed;
    }
    public void setLootSeed(long seed) {
        config.lootSeed = seed;
        // resetLore();
    }

    @CheckReturnValue
    public @NotNull RandomEventScope getShareScope() {
        return config.shareScope;
    }
    public void setShareScope(@NotNull RandomEventScope scope) {
        config.shareScope = scope;
    }

    public boolean setWeightPreset(String name) {
        final RandomEventWeightPreset weightPreset = plugin.config.weightPresets().get(name);
        if (weightPreset == null) return false;
        config.weightPresetName = name;
        this.weightPreset = weightPreset;
        return true;
    }

    public boolean hasSpecialItems() {
        return config.enableSpecialItems;
    }
    public void setHasSpecialItems(boolean hasSpecialItems) {
        config.enableSpecialItems = hasSpecialItems;
    }
    public void toggleSpecialItems() {
        config.enableSpecialItems = !config.enableSpecialItems;
    }
}
