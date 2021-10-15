package net.minecraft.world.level.levelgen.feature.structures;

import com.mojang.serialization.Codec;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BaseBlockPosition;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.EnumBlockRotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.StructureBoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureManager;

public class WorldGenFeatureDefinedStructurePoolEmpty extends WorldGenFeatureDefinedStructurePoolStructure {
    public static final Codec<WorldGenFeatureDefinedStructurePoolEmpty> CODEC;
    public static final WorldGenFeatureDefinedStructurePoolEmpty INSTANCE = new WorldGenFeatureDefinedStructurePoolEmpty();

    private WorldGenFeatureDefinedStructurePoolEmpty() {
        super(WorldGenFeatureDefinedStructurePoolTemplate.Matching.TERRAIN_MATCHING);
    }

    @Override
    public BaseBlockPosition getSize(DefinedStructureManager structureManager, EnumBlockRotation rotation) {
        return BaseBlockPosition.ZERO;
    }

    @Override
    public List<DefinedStructure.BlockInfo> getShuffledJigsawBlocks(DefinedStructureManager structureManager, BlockPosition pos, EnumBlockRotation rotation, Random random) {
        return Collections.emptyList();
    }

    @Override
    public StructureBoundingBox getBoundingBox(DefinedStructureManager structureManager, BlockPosition pos, EnumBlockRotation rotation) {
        throw new IllegalStateException("Invalid call to EmtyPoolElement.getBoundingBox, filter me!");
    }

    @Override
    public boolean place(DefinedStructureManager structureManager, GeneratorAccessSeed world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, BlockPosition pos, BlockPosition blockPos, EnumBlockRotation rotation, StructureBoundingBox box, Random random, boolean keepJigsaws) {
        return true;
    }

    @Override
    public WorldGenFeatureDefinedStructurePools<?> getType() {
        return WorldGenFeatureDefinedStructurePools.EMPTY;
    }

    @Override
    public String toString() {
        return "Empty";
    }

    static {
        CODEC = Codec.unit(() -> {
            return INSTANCE;
        });
    }
}
