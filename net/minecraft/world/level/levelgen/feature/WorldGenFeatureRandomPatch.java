package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.tags.TagsFluid;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.HeightMap;
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
        IBlockData blockState = randomPatchConfiguration.stateProvider.getState(random, blockPos);
        BlockPosition blockPos2;
        if (randomPatchConfiguration.project) {
            blockPos2 = worldGenLevel.getHighestBlockYAt(HeightMap.Type.WORLD_SURFACE_WG, blockPos);
        } else {
            blockPos2 = blockPos;
        }

        int i = 0;
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

        for(int j = 0; j < randomPatchConfiguration.tries; ++j) {
            mutableBlockPos.setWithOffset(blockPos2, random.nextInt(randomPatchConfiguration.xspread + 1) - random.nextInt(randomPatchConfiguration.xspread + 1), random.nextInt(randomPatchConfiguration.yspread + 1) - random.nextInt(randomPatchConfiguration.yspread + 1), random.nextInt(randomPatchConfiguration.zspread + 1) - random.nextInt(randomPatchConfiguration.zspread + 1));
            BlockPosition blockPos4 = mutableBlockPos.below();
            IBlockData blockState2 = worldGenLevel.getType(blockPos4);
            if ((worldGenLevel.isEmpty(mutableBlockPos) || randomPatchConfiguration.canReplace && worldGenLevel.getType(mutableBlockPos).getMaterial().isReplaceable()) && blockState.canPlace(worldGenLevel, mutableBlockPos) && (randomPatchConfiguration.whitelist.isEmpty() || randomPatchConfiguration.whitelist.contains(blockState2.getBlock())) && !randomPatchConfiguration.blacklist.contains(blockState2) && (!randomPatchConfiguration.needWater || worldGenLevel.getFluid(blockPos4.west()).is(TagsFluid.WATER) || worldGenLevel.getFluid(blockPos4.east()).is(TagsFluid.WATER) || worldGenLevel.getFluid(blockPos4.north()).is(TagsFluid.WATER) || worldGenLevel.getFluid(blockPos4.south()).is(TagsFluid.WATER))) {
                randomPatchConfiguration.blockPlacer.place(worldGenLevel, mutableBlockPos, blockState, random);
                ++i;
            }
        }

        return i > 0;
    }
}
