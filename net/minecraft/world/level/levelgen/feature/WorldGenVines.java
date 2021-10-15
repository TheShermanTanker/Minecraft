package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.block.BlockVine;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureEmptyConfiguration;

public class WorldGenVines extends WorldGenerator<WorldGenFeatureEmptyConfiguration> {
    public WorldGenVines(Codec<WorldGenFeatureEmptyConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeaturePlaceContext<WorldGenFeatureEmptyConfiguration> context) {
        GeneratorAccessSeed worldGenLevel = context.level();
        BlockPosition blockPos = context.origin();
        context.config();
        if (!worldGenLevel.isEmpty(blockPos)) {
            return false;
        } else {
            for(EnumDirection direction : EnumDirection.values()) {
                if (direction != EnumDirection.DOWN && BlockVine.isAcceptableNeighbour(worldGenLevel, blockPos.relative(direction), direction)) {
                    worldGenLevel.setTypeAndData(blockPos, Blocks.VINE.getBlockData().set(BlockVine.getDirection(direction), Boolean.valueOf(true)), 2);
                    return true;
                }
            }

            return false;
        }
    }
}
