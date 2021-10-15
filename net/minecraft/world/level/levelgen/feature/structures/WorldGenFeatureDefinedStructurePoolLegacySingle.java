package net.minecraft.world.level.levelgen.feature.structures;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Supplier;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.level.block.EnumBlockRotation;
import net.minecraft.world.level.levelgen.structure.StructureBoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureInfo;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureProcessorBlockIgnore;
import net.minecraft.world.level.levelgen.structure.templatesystem.ProcessorList;

public class WorldGenFeatureDefinedStructurePoolLegacySingle extends WorldGenFeatureDefinedStructurePoolSingle {
    public static final Codec<WorldGenFeatureDefinedStructurePoolLegacySingle> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(templateCodec(), processorsCodec(), projectionCodec()).apply(instance, WorldGenFeatureDefinedStructurePoolLegacySingle::new);
    });

    protected WorldGenFeatureDefinedStructurePoolLegacySingle(Either<MinecraftKey, DefinedStructure> location, Supplier<ProcessorList> processors, WorldGenFeatureDefinedStructurePoolTemplate.Matching projection) {
        super(location, processors, projection);
    }

    @Override
    protected DefinedStructureInfo getSettings(EnumBlockRotation rotation, StructureBoundingBox box, boolean keepJigsaws) {
        DefinedStructureInfo structurePlaceSettings = super.getSettings(rotation, box, keepJigsaws);
        structurePlaceSettings.popProcessor(DefinedStructureProcessorBlockIgnore.STRUCTURE_BLOCK);
        structurePlaceSettings.addProcessor(DefinedStructureProcessorBlockIgnore.STRUCTURE_AND_AIR);
        return structurePlaceSettings;
    }

    @Override
    public WorldGenFeatureDefinedStructurePools<?> getType() {
        return WorldGenFeatureDefinedStructurePools.LEGACY;
    }

    @Override
    public String toString() {
        return "LegacySingle[" + this.template + "]";
    }
}
