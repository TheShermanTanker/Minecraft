package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureEmptyConfiguration;
import net.minecraft.world.level.material.Material;

public class WorldGenFeatureBlueIce extends WorldGenerator<WorldGenFeatureEmptyConfiguration> {
    public WorldGenFeatureBlueIce(Codec<WorldGenFeatureEmptyConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeaturePlaceContext<WorldGenFeatureEmptyConfiguration> context) {
        BlockPosition blockPos = context.origin();
        GeneratorAccessSeed worldGenLevel = context.level();
        Random random = context.random();
        if (blockPos.getY() > worldGenLevel.getSeaLevel() - 1) {
            return false;
        } else if (!worldGenLevel.getType(blockPos).is(Blocks.WATER) && !worldGenLevel.getType(blockPos.below()).is(Blocks.WATER)) {
            return false;
        } else {
            boolean bl = false;

            for(EnumDirection direction : EnumDirection.values()) {
                if (direction != EnumDirection.DOWN && worldGenLevel.getType(blockPos.relative(direction)).is(Blocks.PACKED_ICE)) {
                    bl = true;
                    break;
                }
            }

            if (!bl) {
                return false;
            } else {
                worldGenLevel.setTypeAndData(blockPos, Blocks.BLUE_ICE.getBlockData(), 2);

                for(int i = 0; i < 200; ++i) {
                    int j = random.nextInt(5) - random.nextInt(6);
                    int k = 3;
                    if (j < 2) {
                        k += j / 2;
                    }

                    if (k >= 1) {
                        BlockPosition blockPos2 = blockPos.offset(random.nextInt(k) - random.nextInt(k), j, random.nextInt(k) - random.nextInt(k));
                        IBlockData blockState = worldGenLevel.getType(blockPos2);
                        if (blockState.getMaterial() == Material.AIR || blockState.is(Blocks.WATER) || blockState.is(Blocks.PACKED_ICE) || blockState.is(Blocks.ICE)) {
                            for(EnumDirection direction2 : EnumDirection.values()) {
                                IBlockData blockState2 = worldGenLevel.getType(blockPos2.relative(direction2));
                                if (blockState2.is(Blocks.BLUE_ICE)) {
                                    worldGenLevel.setTypeAndData(blockPos2, Blocks.BLUE_ICE.getBlockData(), 2);
                                    break;
                                }
                            }
                        }
                    }
                }

                return true;
            }
        }
    }
}
