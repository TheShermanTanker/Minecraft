package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureRadiusConfiguration;

public class WorldGenFeatureNetherrackReplaceBlobs extends WorldGenerator<WorldGenFeatureRadiusConfiguration> {
    public WorldGenFeatureNetherrackReplaceBlobs(Codec<WorldGenFeatureRadiusConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeaturePlaceContext<WorldGenFeatureRadiusConfiguration> context) {
        WorldGenFeatureRadiusConfiguration replaceSphereConfiguration = context.config();
        GeneratorAccessSeed worldGenLevel = context.level();
        Random random = context.random();
        Block block = replaceSphereConfiguration.targetState.getBlock();
        BlockPosition blockPos = findTarget(worldGenLevel, context.origin().mutable().clamp(EnumDirection.EnumAxis.Y, worldGenLevel.getMinBuildHeight() + 1, worldGenLevel.getMaxBuildHeight() - 1), block);
        if (blockPos == null) {
            return false;
        } else {
            int i = replaceSphereConfiguration.radius().sample(random);
            int j = replaceSphereConfiguration.radius().sample(random);
            int k = replaceSphereConfiguration.radius().sample(random);
            int l = Math.max(i, Math.max(j, k));
            boolean bl = false;

            for(BlockPosition blockPos2 : BlockPosition.withinManhattan(blockPos, i, j, k)) {
                if (blockPos2.distManhattan(blockPos) > l) {
                    break;
                }

                IBlockData blockState = worldGenLevel.getType(blockPos2);
                if (blockState.is(block)) {
                    this.setBlock(worldGenLevel, blockPos2, replaceSphereConfiguration.replaceState);
                    bl = true;
                }
            }

            return bl;
        }
    }

    @Nullable
    private static BlockPosition findTarget(GeneratorAccess world, BlockPosition.MutableBlockPosition mutablePos, Block target) {
        while(mutablePos.getY() > world.getMinBuildHeight() + 1) {
            IBlockData blockState = world.getType(mutablePos);
            if (blockState.is(target)) {
                return mutablePos;
            }

            mutablePos.move(EnumDirection.DOWN);
        }

        return null;
    }
}
