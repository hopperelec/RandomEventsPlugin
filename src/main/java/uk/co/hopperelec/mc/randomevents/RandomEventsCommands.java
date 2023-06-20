package uk.co.hopperelec.mc.randomevents;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

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

    @Subcommand("listdrops")
    @CommandAlias("randomevents drops")
    @Description("Lists the drops of the given block or entity name")
    public void onListDrops(@NotNull RandomEventsGame game, CommandSender sender, @Name("name") String name) {
        name = name.toUpperCase();
        Object seed = Material.getMaterial(name);
        if (seed == null) {
            try {
                seed = EntityType.valueOf(name);
            } catch (IllegalArgumentException e) {
                sender.sendMessage("Could not find a material or entity by the given name");
                return;
            }
        } else if (!((Material) seed).isBlock()) {
            sender.sendMessage("Given material is not a block");
            return;
        }
        sender.sendMessage(game.getDropsTextFor(seed));
    }

    @Subcommand("set")
    @Description("Set configuration options for an upcoming or ongoing game")
    public class RandomEventsSetCommands extends BaseCommand {
        @Subcommand("delay")
        public void onSetDelay(@NotNull RandomEventsGame game, @Name("delay") TimeInSeconds delay) {
            game.setDelay(delay);
        }
    }
}
