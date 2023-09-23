package uk.co.hopperelec.mc.randomevents;

import net.kyori.adventure.sound.Sound;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.CheckReturnValue;
import java.util.HashSet;
import java.util.Set;

public class RandomEventsPlayer implements InventoryHolder {
    @NotNull public final Player spigotPlayer;
    @NotNull public final RandomEventsGame game;
    @Nullable private PotionEffectType lastPotionEffect;
    @NotNull private static final Set<RandomEventsPlayer> allPlayers = new HashSet<>();

    private RandomEventsPlayer(@NotNull Player spigotPlayer, @NotNull RandomEventsGame game) {
        this.spigotPlayer = spigotPlayer;
        this.game = game;
    }

    @CheckReturnValue
    public static @NotNull RandomEventsPlayer getRandomEventsPlayer(@NotNull Player spigotPlayer, @NotNull RandomEventsGame game) {
        for (RandomEventsPlayer randomEventsPlayer : allPlayers) {
            if (randomEventsPlayer.spigotPlayer.equals(spigotPlayer)) {
                return randomEventsPlayer;
            }
        }
        final RandomEventsPlayer randomEventsPlayer = new RandomEventsPlayer(spigotPlayer, game);
        allPlayers.add(randomEventsPlayer);
        return randomEventsPlayer;
    }

    public void clearPotionEffect() {
        if (lastPotionEffect != null) {
            spigotPlayer.removePotionEffect(lastPotionEffect);
            lastPotionEffect = null;
        }
    }

    public void setPotionEffect(@NotNull PotionEffect potionEffect) {
        clearPotionEffect();
        lastPotionEffect = potionEffect.getType();
        spigotPlayer.addPotionEffect(potionEffect);
    }

    public void playSound(@NotNull Sound sound) {
        spigotPlayer.playSound(sound);
    }

    public void sendMessage(@NotNull String message) {
        spigotPlayer.sendMessage(message);
    }

    public void setBlock(@NotNull Material type) {
        getLocation().getBlock().setType(type);
    }

    public void giveItem(@NotNull ItemStack itemStack) {
        getInventory().addItem(itemStack);
    }

    public Entity spawnEntity(@NotNull EntityType entityType) {
        return getWorld().spawnEntity(getLocation(), entityType);
    }

    public void teleport(@NotNull Location location) {
        spigotPlayer.teleport(location);
    }

    @Override
    @CheckReturnValue
    public @NotNull Inventory getInventory() {
        return spigotPlayer.getInventory();
    }

    @CheckReturnValue
    public @NotNull Location getLocation() {
        return spigotPlayer.getLocation();
    }

    @CheckReturnValue
    public @NotNull World getWorld() {
        return spigotPlayer.getWorld();
    }
}
