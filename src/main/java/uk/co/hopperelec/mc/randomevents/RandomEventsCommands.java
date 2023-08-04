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

import javax.annotation.CheckReturnValue;

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

    @CheckReturnValue
    private @Nullable Object getSeedFromName(@NotNull RandomEventsGame game, @NotNull CommandSender sender, @Nullable String name) {
        if (name == null) {
            if (sender instanceof Player player) {
                final Object seed = game.getSeedFor(player.getInventory().getItemInMainHand());
                if (seed == null) {
                    sender.sendMessage("The item in your hand does not drop items!");
                }
                return seed;
            }
            sender.sendMessage("Please specify the name of a material or entity");
            return null;
        }
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

    @Subcommand("listdrops")
    @Description("Lists the drops of the block or entity name given or in your hand")
    public void onListDrops(@NotNull RandomEventsGame game, @NotNull CommandSender sender, @Optional @Name("name") String name) {
        final Object seed = getSeedFromName(game, sender, name);
        if (seed != null) {
            if (game.isLearned(seed) || !game.doesRequireLearning()) {
                sender.sendMessage(game.getDropsTextFor(seed));
            } else {
                sender.sendMessage("You have not learned the drops of this item yet");
            }
        }
    }

    @Subcommand("learn")
    @Description("Learn the drops of the given block or entity name to deobfuscate them in the drops list")
    public void onLearn(@NotNull RandomEventsGame game, @NotNull CommandSender sender, @Optional @Name("name") String name) {
        final Object seed = getSeedFromName(game, sender, name);
        if (seed != null) {
            game.learn(seed);
            sender.sendMessage("Learned!");
        }
    }

    @Subcommand("unlearn")
    @Description("Unlearn the drops of the given block or entity name to reobfuscate them in the drops list")
    public void onUnlearn(@NotNull RandomEventsGame game, @NotNull CommandSender sender, @Optional @Name("name") String name) {
        final Object seed = getSeedFromName(game, sender, name);
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
        @Subcommand("timeremaining")
        public void onSetTimeRemaining(@NotNull RandomEventsGame game, @NotNull CommandSender sender, @NotNull @Name("timeremaining") TimeInSeconds time) {
            if (time.moreThan(game.getCountdownLength())) {
                sender.sendMessage("Can not set time remaining to higher than the countdown's length!");
            } else {
                game.setTimeTillNextEvent(time);
            }
        }
    }
}
