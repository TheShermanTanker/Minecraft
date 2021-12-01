package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureRandomPatchConfiguration;

public class WorldGenFeatureRandomPatch extends WorldGenerator<WorldGenFeatureRandomPatchConfiguration> {
    public WorldGenFeatureRandomPatch(Codec<WorldGenFeatureRandomPatchConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeaturePlaceContext<WorldGenFeatureRandomPatchConfiguration> context) {
        WorldGenFeatureRandomPatchConfiguration randomPatchConfiguration = context.config();
        Random random = context.random();
        BlockPosition blockPos = context.origin();
        GeneratorAccessSeed worldGenLevel = context.level();
        int i = 0;
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();
        int j = randomPatchConfiguration.xzSpread() + 1;
        int k = randomPatchConfiguration.ySpread() + 1;

        for(int l = 0; l < randomPatchConfiguration.tries(); ++l) {
            mutableBlockPos.setWithOffset(blockPos, random.nextInt(j) - random.nextInt(j), random.nextInt(k) - random.nextInt(k), random.nextInt(j) - random.nextInt(j));
            if (randomPatchConfiguration.feature().get().place(worldGenLevel, context.chunkGenerator(), random, mutableBlockPos)) {
                ++i;
            }
        }

        return i > 0;
    }
}
