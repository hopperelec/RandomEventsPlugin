package uk.co.hopperelec.mc.randomevents;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uk.co.hopperelec.mc.randomevents.eventtypes.RandomEventType;

@CommandAlias("randomevents")
@Description("Core command for RandomEvents")
public class RandomEventsCommands extends BaseCommand {
    @HelpCommand
    public void onHelp(@NotNull CommandHelp help) {
        help.showHelp();
    }

    @Subcommand("start")
    @Description("Start the game!")
    public void onStart(@NotNull RandomEventsGame game, @Optional TimeInSeconds countdownLength) {
        if (countdownLength != null) game.setCountdownLength(countdownLength);
        game.start();
    }

    @Subcommand("stop")
    @Description("Stops an ongoing game")
    public void onStop(@NotNull RandomEventsGame game, @NotNull CommandSender sender) {
        if (game.isOngoing()) {
            game.stop();
        } else {
            sender.sendMessage("There isn't a game ongoing!");
        }
    }

    @Subcommand("trigger")
    @Description("Triggers an event")
    @CommandCompletion("@randomeventtypes")
    public void onTrigger(@NotNull RandomEventsGame game, @Optional @Name("type") RandomEventType randomEventType) {
        if (randomEventType == null) {
            game.doRandomEvent();
        } else {
            game.doRandomEvent(randomEventType);
        }
    }

    @Subcommand("triggerplayer")
    @Description("Triggers an event on a given player")
    @CommandCompletion("@players @randomeventtypes")
    public void onTriggerPlayer(@NotNull RandomEventsGame game, @Name("player") RandomEventsPlayer player, @Optional @Name("type") RandomEventType randomEventType) {
        if (randomEventType == null) {
            game.doRandomEvent(player);
        } else {
            game.doRandomEvent(player, randomEventType);
        }
    }

    private @Nullable Object getSeedFromName(@NotNull CommandSender sender, @NotNull String name) {
        name = name.toUpperCase();
        final Material material = Material.getMaterial(name);
        if (material != null) {
            if (material.isBlock()) {
                return material;
            }
            sender.sendMessage("Given material is not a block");
        } else {
            try {
                return EntityType.valueOf(name);
            } catch (IllegalArgumentException e) {
                sender.sendMessage("Could not find a material or entity by the given name");
            }
        }
        return null;
    }

    private void listDrops(@NotNull RandomEventsGame game, @NotNull CommandSender sender, @Nullable Object seed) {
        if (seed == null) {
            sender.sendMessage("The item in your hand does not drop items!");
        } else if (game.isLearned(seed)) {
            sender.sendMessage(game.getDropsTextFor(seed));
        } else {
            sender.sendMessage("You have not learned the drops of this item yet");
        }
    }

    @Subcommand("listdrops")
    @Description("Lists the drops of the block or entity name given or in your hand")
    public void onListDrops(@NotNull RandomEventsGame game, @NotNull CommandSender sender, @Optional @Name("name") String name) {
        if (name == null) {
            if (sender instanceof Player player) {
                listDrops(game, sender, game.getSeedFor(player.getInventory().getItemInMainHand()));
            } else {
                sender.sendMessage("listdrops can only be used without a name argument if you are a player");
            }
        } else {
            listDrops(game, sender, getSeedFromName(sender, name));
        }
    }

    @Subcommand("learn")
    @Description("Learn the drops of the given block or entity name to deobfuscate them in the drops list")
    public void onLearn(@NotNull RandomEventsGame game, @NotNull CommandSender sender, @NotNull @Name("name") String name) {
        final Object seed = getSeedFromName(sender, name);
        if (seed != null) {
            game.learn(seed);
            sender.sendMessage("Learned!");
        }
    }

    @Subcommand("unlearn")
    @Description("Unlearn the drops of the given block or entity name to reobfuscate them in the drops list")
    public void onUnlearn(@NotNull RandomEventsGame game, @NotNull CommandSender sender, @NotNull @Name("name") String name) {
        final Object seed = getSeedFromName(sender, name);
        if (seed != null) {
            game.unlearn(seed);
            sender.sendMessage("Unlearned!");
        }
    }

    @Subcommand("seed")
    @Description("View the seed used to generate the random block and entity drops")
    public void onSeed(@NotNull RandomEventsGame game, @NotNull CommandSender sender) {
        sender.sendMessage(Long.toString(game.getLootSeed()));
    }

    @Subcommand("set")
    @Description("Toggle boolean flags")
    public class RandomEventsToggleCommands extends BaseCommand {
        @Subcommand("learning")
        public void onToggleLearning(@NotNull RandomEventsGame game) {
            game.toggleRequireLearning();
        }
    }

    @Subcommand("set")
    @Description("Set configuration options for an upcoming or ongoing game")
    public class RandomEventsSetCommands extends BaseCommand {
        @Subcommand("countdownLength")
        public void onSetCountdownLength(@NotNull RandomEventsGame game, @NotNull @Name("countdownLength") TimeInSeconds countdownLength) {
            game.setCountdownLength(countdownLength);
        }
        @Subcommand("learning")
        public void onSetLearning(@NotNull RandomEventsGame game, @Name("value") boolean value) {
            game.setRequireLearning(value);
        }
        @Subcommand("seed")
        public void onSetSeed(@NotNull RandomEventsGame game, @Name("seed") long seed) {
            game.setLootSeed(seed);
        }
    }
}
