package uk.co.hopperelec.mc.randomevents.utils;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record KeyedResource<R> (
        @NotNull ResourceLocation key,
        @NotNull R resource
) {}
