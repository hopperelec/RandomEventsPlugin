package uk.co.hopperelec.mc.randomevents.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import net.kyori.adventure.sound.Sound;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uk.co.hopperelec.mc.randomevents.countdowns.BukkitCountdownLocation;

public record RandomEventsConfig(
        short teleportSearchRadius,
        @NotNull BukkitCountdownLocation countdownLocation,
        @NotNull @JsonProperty("defaults") RandomEventsGameConfig defaultGameConfig,
        @NotNull ImmutableMap<String,Sound> eventSoundEffects,
        @Nullable Sound learnDropSoundEffect,
        @NotNull ImmutableMap<String, RandomEventWeightPreset> weightPresets
) {}
