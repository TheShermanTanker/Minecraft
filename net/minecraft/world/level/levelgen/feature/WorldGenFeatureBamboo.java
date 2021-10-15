package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.block.BlockBamboo;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockPropertyBambooSize;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureConfigurationChance;

public class WorldGenFeatureBamboo extends WorldGenerator<WorldGenFeatureConfigurationChance> {
    private static final IBlockData BAMBOO_TRUNK = Blocks.BAMBOO.getBlockData().set(BlockBamboo.AGE, Integer.valueOf(1)).set(BlockBamboo.LEAVES, BlockPropertyBambooSize.NONE).set(BlockBamboo.STAGE, Integer.valueOf(0));
    private static final IBlockData BAMBOO_FINAL_LARGE = BAMBOO_TRUNK.set(BlockBamboo.LEAVES, BlockPropertyBambooSize.LARGE).set(BlockBamboo.STAGE, Integer.valueOf(1));
    private static final IBlockData BAMBOO_TOP_LARGE = BAMBOO_TRUNK.set(BlockBamboo.LEAVES, BlockPropertyBambooSize.LARGE);
    private static final IBlockData BAMBOO_TOP_SMALL = BAMBOO_TRUNK.set(BlockBamboo.LEAVES, BlockPropertyBambooSize.SMALL);

    public WorldGenFeatureBamboo(Codec<WorldGenFeatureConfigurationChance> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeaturePlaceContext<WorldGenFeatureConfigurationChance> context) {
        int i = 0;
        BlockPosition blockPos = context.origin();
        GeneratorAccessSeed worldGenLevel = context.level();
        Random random = context.random();
        WorldGenFeatureConfigurationChance probabilityFeatureConfiguration = context.config();
        BlockPosition.MutableBlockPosition mutableBlockPos = blockPos.mutable();
        BlockPosition.MutableBlockPosition mutableBlockPos2 = blockPos.mutable();
        if (worldGenLevel.isEmpty(mutableBlockPos)) {
            if (Blocks.BAMBOO.getBlockData().canPlace(worldGenLevel, mutableBlockPos)) {
                int j = random.nextInt(12) + 5;
                if (random.nextFloat() < probabilityFeatureConfiguration.probability) {
                    int k = random.nextInt(4) + 1;

                    for(int l = blockPos.getX() - k; l <= blockPos.getX() + k; ++l) {
                        for(int m = blockPos.getZ() - k; m <= blockPos.getZ() + k; ++m) {
                            int n = l - blockPos.getX();
                            int o = m - blockPos.getZ();
                            if (n * n + o * o <= k * k) {
                                mutableBlockPos2.set(l, worldGenLevel.getHeight(HeightMap.Type.WORLD_SURFACE, l, m) - 1, m);
                                if (isDirt(worldGenLevel.getType(mutableBlockPos2))) {
                                    worldGenLevel.setTypeAndData(mutableBlockPos2, Blocks.PODZOL.getBlockData(), 2);
                                }
                            }
                        }
                    }
                }

                for(int p = 0; p < j && worldGenLevel.isEmpty(mutableBlockPos); ++p) {
                    worldGenLevel.setTypeAndData(mutableBlockPos, BAMBOO_TRUNK, 2);
                    mutableBlockPos.move(EnumDirection.UP, 1);
                }

                if (mutableBlockPos.getY() - blockPos.getY() >= 3) {
                    worldGenLevel.setTypeAndData(mutableBlockPos, BAMBOO_FINAL_LARGE, 2);
                    worldGenLevel.setTypeAndData(mutableBlockPos.move(EnumDirection.DOWN, 1), BAMBOO_TOP_LARGE, 2);
                    worldGenLevel.setTypeAndData(mutableBlockPos.move(EnumDirection.DOWN, 1), BAMBOO_TOP_SMALL, 2);
                }
            }

            ++i;
        }

        return i > 0;
    }
}
