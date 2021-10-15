package net.minecraft.world.level.levelgen.feature;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureDeltaConfiguration;

public class WorldGenFeatureDelta extends WorldGenerator<WorldGenFeatureDeltaConfiguration> {
    private static final ImmutableList<Block> CANNOT_REPLACE = ImmutableList.of(Blocks.BEDROCK, Blocks.NETHER_BRICKS, Blocks.NETHER_BRICK_FENCE, Blocks.NETHER_BRICK_STAIRS, Blocks.NETHER_WART, Blocks.CHEST, Blocks.SPAWNER);
    private static final EnumDirection[] DIRECTIONS = EnumDirection.values();
    private static final double RIM_SPAWN_CHANCE = 0.9D;

    public WorldGenFeatureDelta(Codec<WorldGenFeatureDeltaConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeaturePlaceContext<WorldGenFeatureDeltaConfiguration> context) {
        boolean bl = false;
        Random random = context.random();
        GeneratorAccessSeed worldGenLevel = context.level();
        WorldGenFeatureDeltaConfiguration deltaFeatureConfiguration = context.config();
        BlockPosition blockPos = context.origin();
        boolean bl2 = random.nextDouble() < 0.9D;
        int i = bl2 ? deltaFeatureConfiguration.rimSize().sample(random) : 0;
        int j = bl2 ? deltaFeatureConfiguration.rimSize().sample(random) : 0;
        boolean bl3 = bl2 && i != 0 && j != 0;
        int k = deltaFeatureConfiguration.size().sample(random);
        int l = deltaFeatureConfiguration.size().sample(random);
        int m = Math.max(k, l);

        for(BlockPosition blockPos2 : BlockPosition.withinManhattan(blockPos, k, 0, l)) {
            if (blockPos2.distManhattan(blockPos) > m) {
                break;
            }

            if (isClear(worldGenLevel, blockPos2, deltaFeatureConfiguration)) {
                if (bl3) {
                    bl = true;
                    this.setBlock(worldGenLevel, blockPos2, deltaFeatureConfiguration.rim());
                }

                BlockPosition blockPos3 = blockPos2.offset(i, 0, j);
                if (isClear(worldGenLevel, blockPos3, deltaFeatureConfiguration)) {
                    bl = true;
                    this.setBlock(worldGenLevel, blockPos3, deltaFeatureConfiguration.contents());
                }
            }
        }

        return bl;
    }

    private static boolean isClear(GeneratorAccess world, BlockPosition pos, WorldGenFeatureDeltaConfiguration config) {
        IBlockData blockState = world.getType(pos);
        if (blockState.is(config.contents().getBlock())) {
            return false;
        } else if (CANNOT_REPLACE.contains(blockState.getBlock())) {
            return false;
        } else {
            for(EnumDirection direction : DIRECTIONS) {
                boolean bl = world.getType(pos.relative(direction)).isAir();
                if (bl && direction != EnumDirection.UP || !bl && direction == EnumDirection.UP) {
                    return false;
                }
            }

            return true;
        }
    }
}
