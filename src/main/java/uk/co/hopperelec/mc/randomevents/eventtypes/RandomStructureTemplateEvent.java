package uk.co.hopperelec.mc.randomevents.eventtypes;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import uk.co.hopperelec.mc.randomevents.RandomEventsPlayer;
import uk.co.hopperelec.mc.randomevents.RandomEventsPlugin;
import uk.co.hopperelec.mc.randomevents.utils.NMSUtils;

import java.util.HashSet;
import java.util.Set;

public class RandomStructureTemplateEvent extends PositionalPolyMetricRandomEventType<RandomStructureTemplateEvent.StructureTemplateDetails> {
    public RandomStructureTemplateEvent(@NotNull RandomEventsPlugin plugin) {
        super(plugin);
    }

    public record StructureTemplateDetails(
            @NotNull ResourceLocation key,
            @NotNull StructureTemplate template,
            float integrity
    ) {}

    @Override
    public void execute(@NotNull Location location, @NotNull StructureTemplateDetails details) {
        final BlockPos blockPos = BlockPos.containing(location.x(), location.y(), location.z());
        details.template.placeInWorld(NMSUtils.getNMSWorld(location), blockPos, blockPos,
                new StructurePlaceSettings()
                        .addProcessor(new BlockRotProcessor(details.integrity))
                        .addProcessor(JigsawReplacementProcessor.INSTANCE)
                        .addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR)
                        .setMirror(plugin.getRandomFrom(Mirror.values()))
                        .setRotation(plugin.getRandomFrom(Rotation.values())),
                RandomSource.create(), 2
        );
    }

    @Override
    protected @NotNull StructureTemplateDetails @NotNull[] getAllMetrics(@NotNull World world) {
        final StructureTemplateManager structureTemplateManager = NMSUtils.getNMSWorld(world).getStructureManager();
        final Set<StructureTemplateDetails> metrics = new HashSet<>();
        for (ResourceLocation key : structureTemplateManager.listTemplates().toList()) {
            structureTemplateManager.get(key).ifPresent(template -> metrics.add(new StructureTemplateDetails(key, template, 1)));
        }
        return metrics.toArray(StructureTemplateDetails[]::new);
    }

    @Override
    protected @NotNull String getMetricKey(@NotNull StructureTemplateDetails details) {
        return details.key.toString();
    }

    @Override
    public StructureTemplateDetails getRandomMetricFor(@NotNull RandomEventsPlayer player) {
        final StructureTemplateDetails metric = super.getRandomMetricFor(player);
        return new StructureTemplateDetails(
                metric.key,
                metric.template,
                plugin.random.nextFloat()+Float.MIN_NORMAL
        );
    }

    @Override
    protected @NotNull String getSuccessMessage(@NotNull StructureTemplateDetails details) {
        return "Placed a "+details.key.getPath()+" with "+Math.round(details.integrity*1000)/10f+"% integrity";
    }
}
