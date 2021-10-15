package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.VegetationPatchConfiguration;

public class WorldGenFeatureWaterloggedVegetationPatch extends WorldGenFeatureVegetationPatch {
    public WorldGenFeatureWaterloggedVegetationPatch(Codec<VegetationPatchConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    protected Set<BlockPosition> placeGroundPatch(GeneratorAccessSeed world, VegetationPatchConfiguration config, Random random, BlockPosition pos, Predicate<IBlockData> replaceable, int radiusX, int radiusZ) {
        Set<BlockPosition> set = super.placeGroundPatch(world, config, random, pos, replaceable, radiusX, radiusZ);
        Set<BlockPosition> set2 = new HashSet<>();
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

        for(BlockPosition blockPos : set) {
            if (!isExposed(world, set, blockPos, mutableBlockPos)) {
                set2.add(blockPos);
            }
        }

        for(BlockPosition blockPos2 : set2) {
            world.setTypeAndData(blockPos2, Blocks.WATER.getBlockData(), 2);
        }

        return set2;
    }

    private static boolean isExposed(GeneratorAccessSeed world, Set<BlockPosition> positions, BlockPosition pos, BlockPosition.MutableBlockPosition mutablePos) {
        return isExposedDirection(world, pos, mutablePos, EnumDirection.NORTH) || isExposedDirection(world, pos, mutablePos, EnumDirection.EAST) || isExposedDirection(world, pos, mutablePos, EnumDirection.SOUTH) || isExposedDirection(world, pos, mutablePos, EnumDirection.WEST) || isExposedDirection(world, pos, mutablePos, EnumDirection.DOWN);
    }

    private static boolean isExposedDirection(GeneratorAccessSeed world, BlockPosition pos, BlockPosition.MutableBlockPosition mutablePos, EnumDirection direction) {
        mutablePos.setWithOffset(pos, direction);
        return !world.getType(mutablePos).isFaceSturdy(world, mutablePos, direction.opposite());
    }

    @Override
    protected boolean placeVegetation(GeneratorAccessSeed world, VegetationPatchConfiguration config, ChunkGenerator generator, Random random, BlockPosition pos) {
        if (super.placeVegetation(world, config, generator, random, pos.below())) {
            IBlockData blockState = world.getType(pos);
            if (blockState.hasProperty(BlockProperties.WATERLOGGED) && !blockState.get(BlockProperties.WATERLOGGED)) {
                world.setTypeAndData(pos, blockState.set(BlockProperties.WATERLOGGED, Boolean.valueOf(true)), 2);
            }

            return true;
        } else {
            return false;
        }
    }
}
