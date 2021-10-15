package net.minecraft.world.level.levelgen.feature;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureBasaltColumnsConfiguration;

public class WorldGenFeatureBasaltColumns extends WorldGenerator<WorldGenFeatureBasaltColumnsConfiguration> {
    private static final ImmutableList<Block> CANNOT_PLACE_ON = ImmutableList.of(Blocks.LAVA, Blocks.BEDROCK, Blocks.MAGMA_BLOCK, Blocks.SOUL_SAND, Blocks.NETHER_BRICKS, Blocks.NETHER_BRICK_FENCE, Blocks.NETHER_BRICK_STAIRS, Blocks.NETHER_WART, Blocks.CHEST, Blocks.SPAWNER);
    private static final int CLUSTERED_REACH = 5;
    private static final int CLUSTERED_SIZE = 50;
    private static final int UNCLUSTERED_REACH = 8;
    private static final int UNCLUSTERED_SIZE = 15;

    public WorldGenFeatureBasaltColumns(Codec<WorldGenFeatureBasaltColumnsConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeaturePlaceContext<WorldGenFeatureBasaltColumnsConfiguration> context) {
        int i = context.chunkGenerator().getSeaLevel();
        BlockPosition blockPos = context.origin();
        GeneratorAccessSeed worldGenLevel = context.level();
        Random random = context.random();
        WorldGenFeatureBasaltColumnsConfiguration columnFeatureConfiguration = context.config();
        if (!canPlaceAt(worldGenLevel, i, blockPos.mutable())) {
            return false;
        } else {
            int j = columnFeatureConfiguration.height().sample(random);
            boolean bl = random.nextFloat() < 0.9F;
            int k = Math.min(j, bl ? 5 : 8);
            int l = bl ? 50 : 15;
            boolean bl2 = false;

            for(BlockPosition blockPos2 : BlockPosition.randomBetweenClosed(random, l, blockPos.getX() - k, blockPos.getY(), blockPos.getZ() - k, blockPos.getX() + k, blockPos.getY(), blockPos.getZ() + k)) {
                int m = j - blockPos2.distManhattan(blockPos);
                if (m >= 0) {
                    bl2 |= this.placeColumn(worldGenLevel, i, blockPos2, m, columnFeatureConfiguration.reach().sample(random));
                }
            }

            return bl2;
        }
    }

    private boolean placeColumn(GeneratorAccess world, int seaLevel, BlockPosition pos, int height, int reach) {
        boolean bl = false;

        for(BlockPosition blockPos : BlockPosition.betweenClosed(pos.getX() - reach, pos.getY(), pos.getZ() - reach, pos.getX() + reach, pos.getY(), pos.getZ() + reach)) {
            int i = blockPos.distManhattan(pos);
            BlockPosition blockPos2 = isAirOrLavaOcean(world, seaLevel, blockPos) ? findSurface(world, seaLevel, blockPos.mutable(), i) : findAir(world, blockPos.mutable(), i);
            if (blockPos2 != null) {
                int j = height - i / 2;

                for(BlockPosition.MutableBlockPosition mutableBlockPos = blockPos2.mutable(); j >= 0; --j) {
                    if (isAirOrLavaOcean(world, seaLevel, mutableBlockPos)) {
                        this.setBlock(world, mutableBlockPos, Blocks.BASALT.getBlockData());
                        mutableBlockPos.move(EnumDirection.UP);
                        bl = true;
                    } else {
                        if (!world.getType(mutableBlockPos).is(Blocks.BASALT)) {
                            break;
                        }

                        mutableBlockPos.move(EnumDirection.UP);
                    }
                }
            }
        }

        return bl;
    }

    @Nullable
    private static BlockPosition findSurface(GeneratorAccess world, int seaLevel, BlockPosition.MutableBlockPosition mutablePos, int distance) {
        while(mutablePos.getY() > world.getMinBuildHeight() + 1 && distance > 0) {
            --distance;
            if (canPlaceAt(world, seaLevel, mutablePos)) {
                return mutablePos;
            }

            mutablePos.move(EnumDirection.DOWN);
        }

        return null;
    }

    private static boolean canPlaceAt(GeneratorAccess world, int seaLevel, BlockPosition.MutableBlockPosition mutablePos) {
        if (!isAirOrLavaOcean(world, seaLevel, mutablePos)) {
            return false;
        } else {
            IBlockData blockState = world.getType(mutablePos.move(EnumDirection.DOWN));
            mutablePos.move(EnumDirection.UP);
            return !blockState.isAir() && !CANNOT_PLACE_ON.contains(blockState.getBlock());
        }
    }

    @Nullable
    private static BlockPosition findAir(GeneratorAccess world, BlockPosition.MutableBlockPosition mutablePos, int distance) {
        while(mutablePos.getY() < world.getMaxBuildHeight() && distance > 0) {
            --distance;
            IBlockData blockState = world.getType(mutablePos);
            if (CANNOT_PLACE_ON.contains(blockState.getBlock())) {
                return null;
            }

            if (blockState.isAir()) {
                return mutablePos;
            }

            mutablePos.move(EnumDirection.UP);
        }

        return null;
    }

    private static boolean isAirOrLavaOcean(GeneratorAccess world, int seaLevel, BlockPosition pos) {
        IBlockData blockState = world.getType(pos);
        return blockState.isAir() || blockState.is(Blocks.LAVA) && pos.getY() <= seaLevel;
    }
}
