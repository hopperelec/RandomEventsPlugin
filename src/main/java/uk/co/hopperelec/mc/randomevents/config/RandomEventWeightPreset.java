package uk.co.hopperelec.mc.randomevents.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public record RandomEventWeightPreset(
        @NotNull ImmutableMap<String, Float> eventTypes,
        @NotNull ImmutableMap<String, Map<String,Float>> subEvents,
        @NotNull ImmutableMap<String, JsonNode> grandSubEvents
) {}
