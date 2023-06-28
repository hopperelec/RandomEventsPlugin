package uk.co.hopperelec.mc.randomevents;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SpawnEggMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;
import static uk.co.hopperelec.mc.randomevents.RandomEventsPlayer.getRandomEventsPlayer;

public class RandomEventsGame {
    private int bossBarTaskId = -1;
    @NotNull private final RandomEventsPlugin plugin;
    @NotNull private final BossBar bossBar = Bukkit.createBossBar("Next event:", BarColor.YELLOW, BarStyle.SOLID);
    @NotNull private final TimeInSeconds countdown = new TimeInSeconds();
    @NotNull private final TimeInSeconds delay = new TimeInSeconds();
    @NotNull private final Random eventRandomizer = new Random();
    public long lootSeed = new Random().nextLong();
    public final static short TELEPORT_SEARCH_RADIUS = 32;
    @NotNull public final static TextComponent LORE_PREFIX = Component.text("Drops: ", YELLOW, BOLD).decoration(ITALIC, false);
    @NotNull public final static TextComponent UNKNOWN_DROP_TEXT = LORE_PREFIX.append(Component.text("Unknown").decoration(OBFUSCATED, true));
    @NotNull private final static Set<RandomEventsPlayer> players = new HashSet<>();
    @NotNull private final Set<Object> learnedDropSeeds = new HashSet<>();
    @NotNull private final Map<Object,Set<ItemStack>> itemsWithLore = new HashMap<>();
    private boolean requireLearnItems = false;

    public RandomEventsGame(@NotNull RandomEventsPlugin plugin) {
        this.plugin = plugin;
        bossBar.setVisible(false);
    }

    public void start() {
        if (isOngoing()) {
            plugin.getServer().getScheduler().cancelTask(bossBarTaskId);
        }
        bossBar.setVisible(true);
        countdown.set(delay);
        bossBarTaskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, this::progressCountdown, 20L, 20L);
        addLoreToPlayers();
    }

    public void stop() {
        if (!isOngoing()) {
            throw new IllegalStateException("Tried to stop a game which isn't ongoing");
        }
        bossBar.setVisible(false);
        countdown.set(0);
        plugin.getServer().getScheduler().cancelTask(bossBarTaskId);
        bossBarTaskId = -1;
        removeLoreFromPlayers();
        clearPotionEffects();
    }

    public void progressCountdown() {
        if (countdown.asInt() <= 1) {
            countdown.set(delay);
            doRandomEvent();
        } else {
            countdown.decrement();
        }
        bossBar.setProgress(countdown.dividedBy(delay));
    }


    public void doRandomEvent() {
        for (RandomEventsPlayer player : players) {
            doRandomEvent(player);
        }
    }
    public void doRandomEvent(@NotNull RandomEventType randomEventType) {
        for (RandomEventsPlayer player : players) {
            doRandomEvent(player, randomEventType);
        }
    }
    public void doRandomEvent(@NotNull Player player) {
        doRandomEvent(getRandomEventsPlayer(player));
    }
    public void doRandomEvent(@NotNull RandomEventsPlayer player) {
        doRandomEvent(player, getRandomFrom(RandomEventType.values(), eventRandomizer));
    }
    public void doRandomEvent(@NotNull Player player, @NotNull RandomEventType randomEventType) {
        doRandomEvent(getRandomEventsPlayer(player), randomEventType);
    }
    public void doRandomEvent(@NotNull RandomEventsPlayer player, @NotNull RandomEventType randomEventType) {
        final RandomEvent randomEvent = new RandomEvent(this, player, randomEventType);
        randomEvent.callEvent();
        if (!randomEvent.isCancelled()) {
            doRandomEvent(randomEvent);
        }
    }
    public void doRandomEvent(@NotNull RandomEvent randomEvent) {
        switch (randomEvent.type) {
            case BLOCK -> {
                final Material material = getRandomWhich(Material.values(), Material::isBlock, eventRandomizer);
                randomEvent.player.setBlock(material);
                randomEvent.player.sendMessage("Placed a "+material);
            }
            case ITEM -> {
                final Material material = getRandomWhich(Material.values(), Material::isItem, eventRandomizer);
                final ItemStack itemStack = new ItemStack(material);
                if (itemStack.getItemMeta() instanceof Damageable damageable) {
                    damageable.setHealth(eventRandomizer.nextInt(material.getMaxDurability())+1);
                }
                addLoreTo(itemStack);
                randomEvent.player.giveItem(itemStack);
                randomEvent.player.sendMessage("Given you "+material);
            }
            case ENTITY -> {
                final EntityType entityType = getRandomWhich(EntityType.values(), e -> e.isSpawnable() && e.isEnabledByFeature(randomEvent.player.getWorld()), eventRandomizer);
                randomEvent.player.spawnEntity(entityType);
                randomEvent.player.sendMessage("Spawned a "+entityType);
            }
            case TELEPORT -> {
                final Location location = getRandomSafeLocationNear(randomEvent.player.getLocation());
                if (location == null) {
                    randomEvent.player.sendMessage("Tried to teleport you, but couldn't find a suitable location close enough!");
                } else {
                    randomEvent.player.teleport(location);
                    randomEvent.player.sendMessage("Teleported!");
                }
            }
            case EFFECT -> {
                final PotionEffectType potionEffectType = getRandomFrom(PotionEffectType.values(), eventRandomizer);
                final int amplifier = eventRandomizer.nextInt(5)+1;
                final PotionEffect potionEffect = new PotionEffect(potionEffectType, delay.asInt()*20, amplifier);
                randomEvent.player.setPotionEffect(potionEffect);
                randomEvent.player.sendMessage("Effected you with "+potionEffectType.getName()+" "+amplifier);
            }
        }
    }

    public @Nullable Location getRandomSafeLocationNear(@NotNull Location location) {
        final int minX = location.getBlockX()-TELEPORT_SEARCH_RADIUS;
        final int maxX = location.getBlockX()+TELEPORT_SEARCH_RADIUS;
        final int minZ = location.getBlockZ()-TELEPORT_SEARCH_RADIUS;
        final int maxZ = location.getBlockZ()+TELEPORT_SEARCH_RADIUS;
        final int minY = location.getWorld().getMinHeight();
        final List<Vector> safeLocations = new ArrayList<>();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                final int maxY = location.getWorld().getHighestBlockYAt(x, z);
                if (maxY != minY-1) {
                    safeLocations.add(new Vector(x, maxY+1, z));
                    int y = minY;
                    while (y < maxY) {
                        if (!location.getWorld().getBlockAt(x, y, z).isSolid() && location.getWorld().getBlockAt(x, y-1, z).isSolid()) {
                            safeLocations.add(new Vector(x, y, z));
                            y++;
                        }
                        y++;
                    }
                }
            }
        }
        if (safeLocations.size() != 0) {
            final Vector safeLocation = safeLocations.get(eventRandomizer.nextInt(safeLocations.size()));
            return location.set(safeLocation.getX(), safeLocation.getY(), safeLocation.getZ());
        }
        return null;
    }


    private <T> T getRandomFrom(@NotNull T @NotNull [] array, @NotNull Random random) {
        return array[random.nextInt(array.length)];
    }
    private <T> T getRandomWhich(T[] array, @NotNull Predicate<T> which, @NotNull Random random) {
        while (true) {
            final T option = getRandomFrom(array, random);
            if (which.test(option)) {
                return option;
            }
        }
    }


    private Material getNewDropFor(@NotNull Object seed) {
        return getRandomWhich(Material.values(), Material::isItem, new Random(lootSeed + seed.hashCode()));
    }
    public List<ItemStack> getNewDropsFor(Object seed) {
        final List<ItemStack> newDroppedItems = new ArrayList<>();
        newDroppedItems.add(new ItemStack(getNewDropFor(seed)));
        return newDroppedItems;
    }

    public @NotNull Component getDropsTextForItems(@NotNull List<ItemStack> newDroppedItems) {
        Component loreToAdd = LORE_PREFIX.append(Component.translatable(newDroppedItems.remove(0).translationKey(), BLUE).decoration(BOLD, false));
        for (ItemStack newDroppedItem : newDroppedItems) {
            loreToAdd = loreToAdd.append(Component.text(", ", DARK_GRAY));
            loreToAdd = loreToAdd.append(Component.translatable(newDroppedItem.translationKey(), BLUE));
            if (newDroppedItem.getAmount() != 1) {
                loreToAdd = loreToAdd.append(Component.text(newDroppedItem.getAmount(), WHITE));
            }
        }
        return loreToAdd;
    }
    public @NotNull Component getDropsTextFor(@NotNull Object seed) {
        if (isLearned(seed)) return getDropsTextForItems(getNewDropsFor(seed));
        return UNKNOWN_DROP_TEXT;
    }

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

    public boolean hasLore(@Nullable ItemStack itemStack) {
        if (itemStack == null) return false;
        final ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) return false;
        return itemMeta.getPersistentDataContainer().has(plugin.ITEM_LORE_HASH_KEY);
    }

    public void addLoreTo(@Nullable ItemStack itemStack) {
        if (itemStack == null || hasLore(itemStack)) return;
        final Object seed = getSeedFor(itemStack);
        if (seed == null) return;
        final Component loreToAdd = getDropsTextFor(seed);

        final List<Component> currentLore = itemStack.lore();
        if (currentLore == null) {
            itemStack.lore(List.of(loreToAdd));
        } else {
            currentLore.add(loreToAdd);
            itemStack.lore(currentLore);
        }

        final ItemMeta itemMeta = itemStack.hasItemMeta() ? itemStack.getItemMeta() : Bukkit.getItemFactory().getItemMeta(itemStack.getType());
        itemMeta.getPersistentDataContainer().set(plugin.ITEM_LORE_HASH_KEY, PersistentDataType.INTEGER, loreToAdd.hashCode());
        itemStack.setItemMeta(itemMeta);

        itemsWithLore.putIfAbsent(seed, new HashSet<>());
        itemsWithLore.get(seed).add(itemStack);
    }
    public void addLoreTo(@NotNull Inventory inventory) {
        for (ItemStack itemStack : inventory.getContents()) {
            addLoreTo(itemStack);
        }
    }
    public void addLoreTo(@NotNull InventoryHolder inventoryHolder) {
        addLoreTo(inventoryHolder.getInventory());
    }
    public void addLoreToPlayers() {
        for (RandomEventsPlayer player : players) {
            addLoreTo(player);
        }
    }

    public void removeLoreFrom(@Nullable ItemStack itemStack) {
        if (itemStack == null) return;
        final ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) return;
        final Integer loreHash = itemMeta.getPersistentDataContainer().get(plugin.ITEM_LORE_HASH_KEY, PersistentDataType.INTEGER);
        if (loreHash != null) {
            final List<Component> lore = itemMeta.lore();
            if (lore != null) {
                if (lore.size() > 1) {
                    lore.removeIf(component -> component.hashCode() == loreHash);
                    itemMeta.lore(lore);
                } else {
                    itemMeta.lore(null);
                }
            }
            itemMeta.getPersistentDataContainer().remove(plugin.ITEM_LORE_HASH_KEY);
            itemStack.setItemMeta(itemMeta);
        }
        for (Set<ItemStack> items : itemsWithLore.values()) {
            items.remove(itemStack);
        }
    }
    public void removeLoreFrom(@NotNull Inventory inventory) {
        for (ItemStack itemStack : inventory.getContents()) {
            removeLoreFrom(itemStack);
        }
    }
    public void removeLoreFrom(@NotNull InventoryHolder inventoryHolder) {
        removeLoreFrom(inventoryHolder.getInventory());
    }
    public void removeLoreFromPlayers() {
        for (RandomEventsPlayer player : players) {
            removeLoreFrom(player);
        }
    }

    public void handleLoreFor(@Nullable ItemStack itemStack) {
        if (itemStack != null) {
            if (isOngoing()) {
                addLoreTo(itemStack);
            } else {
                removeLoreFrom(itemStack);
            }
        }
    }
    public void handleLoreFor(@NotNull Inventory inventory) {
        if (isOngoing()) {
            addLoreTo(inventory);
        } else {
            removeLoreFrom(inventory);
        }
    }
    public void handleLoreFor(@NotNull InventoryHolder inventoryHolder) {
        if (isOngoing()) {
            addLoreTo(inventoryHolder);
        } else {
            removeLoreFrom(inventoryHolder);
        }
    }

    public void resetLoreFor(@NotNull ItemStack itemStack) {
        removeLoreFrom(itemStack);
        if (isOngoing()) addLoreTo(itemStack);
    }
    public void resetLore() {
        for (Set<ItemStack> items : itemsWithLore.values()) {
            for (ItemStack itemStack : items) {
                resetLoreFor(itemStack);
            }
        }
    }


    public boolean isLearned(@NotNull Object seed) {
        return learnedDropSeeds.contains(seed);
    }

    public void learn(@NotNull Object seed) {
        learnedDropSeeds.add(seed);
        if (itemsWithLore.containsKey(seed)) {
            for (ItemStack itemStack : itemsWithLore.get(seed)) {
                resetLoreFor(itemStack);
            }
        }
    }

    public void unlearn(@NotNull Object seed) {
        learnedDropSeeds.remove(seed);
        for (ItemStack itemStack : itemsWithLore.get(seed)) {
            resetLoreFor(itemStack);
        }
    }

    public boolean doesRequireLearnItems() {
        return requireLearnItems;
    }
    public void setRequireLearnItems(boolean requireLearnItems) {
        if (requireLearnItems != this.requireLearnItems)
            toggleRequireLearnItems();
    }
    public void toggleRequireLearnItems() {
        requireLearnItems = !requireLearnItems;
        if (isOngoing()) resetLore();
    }


    public void clearPotionEffects() {
        for (RandomEventsPlayer player : players) {
            player.clearPotionEffect();
        }
    }

    public boolean hasPlayer(@NotNull Player player) {
        return hasPlayer(getRandomEventsPlayer(player));
    }
    public boolean hasPlayer(@NotNull RandomEventsPlayer player) {
        return players.contains(player);
    }

    public @NotNull Set<RandomEventsPlayer> getPlayers() {
        return players;
    }

    public void joinPlayer(@NotNull Player player) {
        bossBar.addPlayer(player);
        players.add(getRandomEventsPlayer(player));
        handleLoreFor(player);
    }

    public void removePlayer(@NotNull Player player) {
        bossBar.removePlayer(player);
        players.remove(getRandomEventsPlayer(player));
        removeLoreFrom(player);
    }

    public boolean isOngoing() {
        return bossBarTaskId != -1;
    }

    public @NotNull TimeInSeconds getDelay() {
        return delay;
    }
    public void setDelay(@NotNull TimeInSeconds newDelay) {
        delay.set(newDelay);
        if (countdown.moreThan(newDelay)) {
            countdown.set(newDelay);
        }
    }

    public @NotNull TimeInSeconds getCountdown() {
        return countdown;
    }
    public void setCountdown(@NotNull TimeInSeconds newCountdown) {
        if (newCountdown.moreThan(delay)) {
            throw new IllegalArgumentException("Can not set countdown to higher than delay!");
        }
        countdown.set(newCountdown);
    }
}
