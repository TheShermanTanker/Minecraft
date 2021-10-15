package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureFillConfiguration;

public class WorldGenFeatureFill extends WorldGenerator<WorldGenFeatureFillConfiguration> {
    public WorldGenFeatureFill(Codec<WorldGenFeatureFillConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeaturePlaceContext<WorldGenFeatureFillConfiguration> context) {
        BlockPosition blockPos = context.origin();
        WorldGenFeatureFillConfiguration layerConfiguration = context.config();
        GeneratorAccessSeed worldGenLevel = context.level();
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

        for(int i = 0; i < 16; ++i) {
            for(int j = 0; j < 16; ++j) {
                int k = blockPos.getX() + i;
                int l = blockPos.getZ() + j;
                int m = worldGenLevel.getMinBuildHeight() + layerConfiguration.height;
                mutableBlockPos.set(k, m, l);
                if (worldGenLevel.getType(mutableBlockPos).isAir()) {
                    worldGenLevel.setTypeAndData(mutableBlockPos, layerConfiguration.state, 2);
                }
            }
        }

        return true;
    }
}
