package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.block.BlockDirtSnow;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureEmptyConfiguration;

public class WorldGenFeatureIceSnow extends WorldGenerator<WorldGenFeatureEmptyConfiguration> {
    public WorldGenFeatureIceSnow(Codec<WorldGenFeatureEmptyConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeaturePlaceContext<WorldGenFeatureEmptyConfiguration> context) {
        GeneratorAccessSeed worldGenLevel = context.level();
        BlockPosition blockPos = context.origin();
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();
        BlockPosition.MutableBlockPosition mutableBlockPos2 = new BlockPosition.MutableBlockPosition();

        for(int i = 0; i < 16; ++i) {
            for(int j = 0; j < 16; ++j) {
                int k = blockPos.getX() + i;
                int l = blockPos.getZ() + j;
                int m = worldGenLevel.getHeight(HeightMap.Type.MOTION_BLOCKING, k, l);
                mutableBlockPos.set(k, m, l);
                mutableBlockPos2.set(mutableBlockPos).move(EnumDirection.DOWN, 1);
                BiomeBase biome = worldGenLevel.getBiome(mutableBlockPos);
                if (biome.shouldFreeze(worldGenLevel, mutableBlockPos2, false)) {
                    worldGenLevel.setTypeAndData(mutableBlockPos2, Blocks.ICE.getBlockData(), 2);
                }

                if (biome.shouldSnow(worldGenLevel, mutableBlockPos)) {
                    worldGenLevel.setTypeAndData(mutableBlockPos, Blocks.SNOW.getBlockData(), 2);
                    IBlockData blockState = worldGenLevel.getType(mutableBlockPos2);
                    if (blockState.hasProperty(BlockDirtSnow.SNOWY)) {
                        worldGenLevel.setTypeAndData(mutableBlockPos2, blockState.set(BlockDirtSnow.SNOWY, Boolean.valueOf(true)), 2);
                    }
                }
            }
        }

        return true;
    }
}
