package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureStructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.ProcessorList;

public class FossilFeatureConfiguration implements WorldGenFeatureConfiguration {
    public static final Codec<FossilFeatureConfiguration> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(MinecraftKey.CODEC.listOf().fieldOf("fossil_structures").forGetter((fossilFeatureConfiguration) -> {
            return fossilFeatureConfiguration.fossilStructures;
        }), MinecraftKey.CODEC.listOf().fieldOf("overlay_structures").forGetter((fossilFeatureConfiguration) -> {
            return fossilFeatureConfiguration.overlayStructures;
        }), DefinedStructureStructureProcessorType.LIST_CODEC.fieldOf("fossil_processors").forGetter((fossilFeatureConfiguration) -> {
            return fossilFeatureConfiguration.fossilProcessors;
        }), DefinedStructureStructureProcessorType.LIST_CODEC.fieldOf("overlay_processors").forGetter((fossilFeatureConfiguration) -> {
            return fossilFeatureConfiguration.overlayProcessors;
        }), Codec.intRange(0, 7).fieldOf("max_empty_corners_allowed").forGetter((fossilFeatureConfiguration) -> {
            return fossilFeatureConfiguration.maxEmptyCornersAllowed;
        })).apply(instance, FossilFeatureConfiguration::new);
    });
    public final List<MinecraftKey> fossilStructures;
    public final List<MinecraftKey> overlayStructures;
    public final Supplier<ProcessorList> fossilProcessors;
    public final Supplier<ProcessorList> overlayProcessors;
    public final int maxEmptyCornersAllowed;

    public FossilFeatureConfiguration(List<MinecraftKey> fossilStructures, List<MinecraftKey> overlayStructures, Supplier<ProcessorList> fossilProcessors, Supplier<ProcessorList> overlayProcessors, int maxEmptyCorners) {
        if (fossilStructures.isEmpty()) {
            throw new IllegalArgumentException("Fossil structure lists need at least one entry");
        } else if (fossilStructures.size() != overlayStructures.size()) {
            throw new IllegalArgumentException("Fossil structure lists must be equal lengths");
        } else {
            this.fossilStructures = fossilStructures;
            this.overlayStructures = overlayStructures;
            this.fossilProcessors = fossilProcessors;
            this.overlayProcessors = overlayProcessors;
            this.maxEmptyCornersAllowed = maxEmptyCorners;
        }
    }

    public FossilFeatureConfiguration(List<MinecraftKey> fossilStructures, List<MinecraftKey> overlayStructures, ProcessorList fossilProcessors, ProcessorList overlayProcessors, int maxEmptyCorners) {
        this(fossilStructures, overlayStructures, () -> {
            return fossilProcessors;
        }, () -> {
            return overlayProcessors;
        }, maxEmptyCorners);
    }
}
