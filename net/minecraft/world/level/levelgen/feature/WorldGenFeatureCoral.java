package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.block.BlockCoralFanWallAbstract;
import net.minecraft.world.level.block.BlockSeaPickle;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureEmptyConfiguration;

public abstract class WorldGenFeatureCoral extends WorldGenerator<WorldGenFeatureEmptyConfiguration> {
    public WorldGenFeatureCoral(Codec<WorldGenFeatureEmptyConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeaturePlaceContext<WorldGenFeatureEmptyConfiguration> context) {
        Random random = context.random();
        GeneratorAccessSeed worldGenLevel = context.level();
        BlockPosition blockPos = context.origin();
        IBlockData blockState = TagsBlock.CORAL_BLOCKS.getRandomElement(random).getBlockData();
        return this.placeFeature(worldGenLevel, random, blockPos, blockState);
    }

    protected abstract boolean placeFeature(GeneratorAccess world, Random random, BlockPosition pos, IBlockData state);

    protected boolean placeCoralBlock(GeneratorAccess world, Random random, BlockPosition pos, IBlockData state) {
        BlockPosition blockPos = pos.above();
        IBlockData blockState = world.getType(pos);
        if ((blockState.is(Blocks.WATER) || blockState.is(TagsBlock.CORALS)) && world.getType(blockPos).is(Blocks.WATER)) {
            world.setTypeAndData(pos, state, 3);
            if (random.nextFloat() < 0.25F) {
                world.setTypeAndData(blockPos, TagsBlock.CORALS.getRandomElement(random).getBlockData(), 2);
            } else if (random.nextFloat() < 0.05F) {
                world.setTypeAndData(blockPos, Blocks.SEA_PICKLE.getBlockData().set(BlockSeaPickle.PICKLES, Integer.valueOf(random.nextInt(4) + 1)), 2);
            }

            for(EnumDirection direction : EnumDirection.EnumDirectionLimit.HORIZONTAL) {
                if (random.nextFloat() < 0.2F) {
                    BlockPosition blockPos2 = pos.relative(direction);
                    if (world.getType(blockPos2).is(Blocks.WATER)) {
                        IBlockData blockState2 = TagsBlock.WALL_CORALS.getRandomElement(random).getBlockData();
                        if (blockState2.hasProperty(BlockCoralFanWallAbstract.FACING)) {
                            blockState2 = blockState2.set(BlockCoralFanWallAbstract.FACING, direction);
                        }

                        world.setTypeAndData(blockPos2, blockState2, 2);
                    }
                }
            }

            return true;
        } else {
            return false;
        }
    }
}
