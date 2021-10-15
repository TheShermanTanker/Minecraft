package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureEmptyConfiguration;

public class WorldGenLightStone1 extends WorldGenerator<WorldGenFeatureEmptyConfiguration> {
    public WorldGenLightStone1(Codec<WorldGenFeatureEmptyConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeaturePlaceContext<WorldGenFeatureEmptyConfiguration> context) {
        GeneratorAccessSeed worldGenLevel = context.level();
        BlockPosition blockPos = context.origin();
        Random random = context.random();
        if (!worldGenLevel.isEmpty(blockPos)) {
            return false;
        } else {
            IBlockData blockState = worldGenLevel.getType(blockPos.above());
            if (!blockState.is(Blocks.NETHERRACK) && !blockState.is(Blocks.BASALT) && !blockState.is(Blocks.BLACKSTONE)) {
                return false;
            } else {
                worldGenLevel.setTypeAndData(blockPos, Blocks.GLOWSTONE.getBlockData(), 2);

                for(int i = 0; i < 1500; ++i) {
                    BlockPosition blockPos2 = blockPos.offset(random.nextInt(8) - random.nextInt(8), -random.nextInt(12), random.nextInt(8) - random.nextInt(8));
                    if (worldGenLevel.getType(blockPos2).isAir()) {
                        int j = 0;

                        for(EnumDirection direction : EnumDirection.values()) {
                            if (worldGenLevel.getType(blockPos2.relative(direction)).is(Blocks.GLOWSTONE)) {
                                ++j;
                            }

                            if (j > 1) {
                                break;
                            }
                        }

                        if (j == 1) {
                            worldGenLevel.setTypeAndData(blockPos2, Blocks.GLOWSTONE.getBlockData(), 2);
                        }
                    }
                }

                return true;
            }
        }
    }
}
