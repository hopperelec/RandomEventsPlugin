package uk.co.hopperelec.mc.randomevents.specialitems.functionality;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.mc.randomevents.RandomEventsGame;
import uk.co.hopperelec.mc.randomevents.RandomEventsPlayer;
import uk.co.hopperelec.mc.randomevents.RandomEventsPlugin;
import uk.co.hopperelec.mc.randomevents.eventtypes.PolyMetricRandomEventType;
import uk.co.hopperelec.mc.randomevents.eventtypes.PositionalPolyMetricRandomEventType;
import uk.co.hopperelec.mc.randomevents.eventtypes.PositionalRandomEventType;
import uk.co.hopperelec.mc.randomevents.eventtypes.RandomEventType;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class SpecialItemRandomEvent extends SpecialItemFunctionality {
    public SpecialItemRandomEvent(@NotNull RandomEventsPlugin plugin) {
        super(plugin);
    }

    private void execute(@NotNull Material specialItem, @NotNull Consumer<Short> ifNotGivenType, @NotNull BiConsumer<RandomEventType,Short> ifGivenType) {
        final ImmutableMap<String, JsonNode> specialItemConfig = plugin.config.specialItems().get(specialItem);
        if (specialItemConfig == null) {
            ifNotGivenType.accept((short) 1);
        } else {
            final JsonNode eventTypeName = specialItemConfig.get("event_type");
            // TODO: Add support for random ranges for repeats/event (e.g: "num_repeats: 5-10")
            final JsonNode numEventsNode = specialItemConfig.get("num_events");
            final short numEvents = numEventsNode == null ? 1 : numEventsNode.shortValue();
            final JsonNode numRepeatsNode = specialItemConfig.get("num_repeats");
            final short repeats = numRepeatsNode == null ? 1 : numRepeatsNode.shortValue();
            final JsonNode eventTickDelayNode = specialItemConfig.get("event_tick_delay");

            final Runnable action;
            if (eventTypeName == null) {
                action = () -> ifNotGivenType.accept(repeats);
            } else {
                final RandomEventType eventType = plugin.registeredEventTypes.get(eventTypeName.asText());
                if (eventType == null) {
                    action = () -> ifNotGivenType.accept(repeats);
                } else {
                    action = () -> ifGivenType.accept(eventType, repeats);
                }
            }

            if (eventTickDelayNode == null) {
                for (short i = 0; i < numEvents; i++) {
                    action.run();
                }
            } else {
                final AtomicInteger i = new AtomicInteger();
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        action.run();
                        if (i.getAndIncrement() >= numEvents) {
                            cancel();
                        }
                    }
                }.runTaskTimer(plugin, 0, eventTickDelayNode.asLong());
            }
        }
    }

    @Override
    public boolean execute(@NotNull Block block, @NotNull RandomEventsGame game) {
        execute(block.getType(), repeats -> {
            final PositionalRandomEventType randomEventType = plugin.chooseRandomPositionalEvent(game.weightPreset);
            if (randomEventType instanceof PositionalPolyMetricRandomEventType<?> ppRET) {
                ppRET.execute(block, game, repeats);
            } else {
                randomEventType.execute(block, game);
            }
        }, (randomEventType,repeats) -> {
            if (randomEventType instanceof PositionalRandomEventType positionalRandomEventType) {
                if (positionalRandomEventType instanceof PositionalPolyMetricRandomEventType<?> ppRET) {
                    ppRET.execute(block, game, repeats);
                } else {
                    positionalRandomEventType.execute(block, game);
                }
            } else {
                throw new IllegalArgumentException("Special item functionality for "+block.getType()+" tries to use a non-positional event type on a block");
            }
        });
        return true;
    }

    @Override
    public boolean execute(@NotNull Material specialItem, @NotNull RandomEventsPlayer player, @NotNull Event cause) {
        execute(specialItem, repeats -> {
            player.game.doRandomEvent(player);
        }, (randomEventType,repeats) -> {
            if (randomEventType instanceof PolyMetricRandomEventType<?> pRET) {
                player.game.doRandomEvent(player, pRET, repeats);
            } else {
                player.game.doRandomEvent(player, randomEventType);
            }
        });
        return true;
    }
}
