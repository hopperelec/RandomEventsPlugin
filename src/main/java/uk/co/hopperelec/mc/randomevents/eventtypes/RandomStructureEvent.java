package uk.co.hopperelec.mc.randomevents.eventtypes;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uk.co.hopperelec.mc.randomevents.RandomEventsPlayer;
import uk.co.hopperelec.mc.randomevents.RandomEventsPlugin;

import javax.annotation.CheckReturnValue;
import java.util.HashSet;
import java.util.Set;

public class RandomStructureEvent extends PolyMetricRandomEventType<RandomStructureEvent.StructureTemplateDetails> {
    public RandomStructureEvent(@NotNull RandomEventsPlugin plugin) {
        super(plugin);
    }

    public record StructureTemplateDetails(
            @NotNull StructureTemplate template,
            @NotNull ResourceLocation key,
            float integrity
    ) {}

    @CheckReturnValue
    private @NotNull ServerLevel getNMSWorld(@NotNull World world) {
        return ((CraftWorld) world).getHandle();
    }

    @Override
    public boolean execute(@NotNull RandomEventsPlayer player, @NotNull StructureTemplateDetails details) {
        final Location loc = player.getLocation();
        final BlockPos blockPos = BlockPos.containing(loc.x(), loc.y(), loc.z());
        details.template.placeInWorld(getNMSWorld(player.getWorld()), blockPos, blockPos,
                new StructurePlaceSettings()
                        .addProcessor(new BlockRotProcessor(details.integrity))
                        .addProcessor(JigsawReplacementProcessor.INSTANCE)
                        .addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR)
                        .setMirror(plugin.getRandomFrom(Mirror.values()))
                        .setRotation(plugin.getRandomFrom(Rotation.values())),
                RandomSource.create(), 2
        );
        return true;
    }

    @Override
    protected StructureTemplateDetails[] getAllMetrics() { return new StructureTemplateDetails[0]; }

    @Override
    protected @NotNull StructureTemplateDetails @NotNull[] getAllMetrics(@NotNull World world) {
        final StructureTemplateManager structureTemplateManager = getNMSWorld(world).getStructureManager();
        final Set<StructureTemplateDetails> metrics = new HashSet<>();
        for (ResourceLocation key : structureTemplateManager.listTemplates().toList()) {
            structureTemplateManager.get(key).ifPresent(template -> metrics.add(new StructureTemplateDetails(template, key, 1)));
        }
        return metrics.toArray(StructureTemplateDetails[]::new);
    }

    @Override
    protected @Nullable StructureTemplateDetails getMetricByName(@NotNull String name) { return null; }
    @Override
    protected @Nullable StructureTemplateDetails getMetricByName(@NotNull String name, @NotNull World world) {
        final ResourceLocation key = new ResourceLocation(name);
        return getNMSWorld(world).getStructureManager().get(key)
                .map(structureTemplate -> new StructureTemplateDetails(structureTemplate, key, 1))
                .orElse(null);
    }

    @Override
    public StructureTemplateDetails getRandomMetricFor(@NotNull RandomEventsPlayer player) {
        final StructureTemplateDetails metric = super.getRandomMetricFor(player);
        return new StructureTemplateDetails(
                metric.template,
                metric.key,
                plugin.random.nextFloat()+Float.MIN_NORMAL
        );
    }

    @Override
    protected @NotNull String getSuccessMessage(@NotNull StructureTemplateDetails details) {
        return "Placed a "+details.key.getPath()+" with "+Math.round(details.integrity*1000)/10f+"% integrity";
    }
}
