package uk.co.hopperelec.mc.randomevents.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uk.co.hopperelec.mc.randomevents.countdowns.BukkitCountdownLocation;

public record RandomEventsConfig(
        short teleportSearchRadius,
        @NotNull BukkitCountdownLocation countdownLocation,
        @NotNull @JsonProperty("defaults") RandomEventsGameConfig defaultGameConfig,
        @NotNull ImmutableMap<Material,ImmutableMap<String,JsonNode>> specialItems,
        @NotNull ImmutableMap<String,Sound> eventSoundEffects,
        @Nullable Sound learnDropSoundEffect,
        @NotNull ImmutableMap<String, RandomEventWeightPreset> weightPresets
) {}
