package uk.co.hopperelec.mc.randomevents;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CommandAlias("randomevents")
@Description("Core command for RandomEvents")
public class RandomEventsCommands extends BaseCommand {
    @HelpCommand
    public void onHelp(@NotNull CommandHelp help) {
        help.showHelp();
    }

    @Subcommand("start")
    @Description("Start the game!")
    public void onStart(@NotNull RandomEventsGame game, @Default("30") @Name("delay") TimeInSeconds delay) {
        game.setDelay(delay);
        game.start();
    }

    @Subcommand("stop")
    @Description("Stops an ongoing game")
    public void onStop(@NotNull RandomEventsGame game, CommandSender sender) {
        if (game.isOngoing()) {
            game.stop();
        } else {
            sender.sendMessage("There isn't a game ongoing!");
        }
    }

    @Subcommand("trigger")
    @Description("Triggers an event")
    public void onTrigger(@NotNull RandomEventsGame game, @Optional @Name("type")  RandomEventType randomEventType) {
        if (randomEventType == null) {
            game.doRandomEvent();
        } else {
            game.doRandomEvent(randomEventType);
        }
    }

    private @Nullable Object getSeedFromName(CommandSender sender, String name) {
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
    @CommandAlias("randomevents drops")
    @Description("Lists the drops of the given block or entity name")
    public void onListDrops(@NotNull RandomEventsGame game, CommandSender sender, @Name("name") String name) {
        final Object seed = getSeedFromName(sender, name);
        if (seed != null)
            sender.sendMessage(game.getDropsTextFor(seed));
    }

    @Subcommand("learn")
    @Description("Learn the drops of the given block or entity name to deobfuscate them in the drops list")
    public void onLearn(@NotNull RandomEventsGame game, CommandSender sender, @Name("name") String name) {
        final Object seed = getSeedFromName(sender, name);
        if (seed != null) {
            game.learn(seed);
            sender.sendMessage("Learned!");
        }
    }

    @Subcommand("unlearn")
    @Description("Unlearn the drops of the given block or entity name to reobfuscate them in the drops list")
    public void onUnlearn(@NotNull RandomEventsGame game, CommandSender sender, @Name("name") String name) {
        final Object seed = getSeedFromName(sender, name);
        if (seed != null) {
            game.unlearn(seed);
            sender.sendMessage("Unlearned!");
        }
    }

    @Subcommand("set")
    @Description("Toggle boolean flags")
    public class RandomEventsToggleCommands extends BaseCommand {
        @Subcommand("learning")
        public void onToggleLearning(@NotNull RandomEventsGame game) {
            game.toggleRequireLearnItems();
        }
    }

    @Subcommand("set")
    @Description("Set configuration options for an upcoming or ongoing game")
    public class RandomEventsSetCommands extends BaseCommand {
        @Subcommand("delay")
        public void onSetDelay(@NotNull RandomEventsGame game, @Name("delay") TimeInSeconds delay) {
            game.setDelay(delay);
        }
        @Subcommand("learning")
        public void onSetLearning(@NotNull RandomEventsGame game, @Name("value") boolean value) {
            game.setRequireLearnItems(value);
        }
    }
}
