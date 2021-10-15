package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureRandomPatchConfiguration;

public class WorldGenFeatureFlower extends WorldGenFlowers<WorldGenFeatureRandomPatchConfiguration> {
    public WorldGenFeatureFlower(Codec<WorldGenFeatureRandomPatchConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean isValid(GeneratorAccess world, BlockPosition pos, WorldGenFeatureRandomPatchConfiguration config) {
        return !config.blacklist.contains(world.getType(pos));
    }

    @Override
    public int getCount(WorldGenFeatureRandomPatchConfiguration config) {
        return config.tries;
    }

    @Override
    public BlockPosition getPos(Random random, BlockPosition pos, WorldGenFeatureRandomPatchConfiguration config) {
        return pos.offset(random.nextInt(config.xspread) - random.nextInt(config.xspread), random.nextInt(config.yspread) - random.nextInt(config.yspread), random.nextInt(config.zspread) - random.nextInt(config.zspread));
    }

    @Override
    public IBlockData getRandomFlower(Random random, BlockPosition pos, WorldGenFeatureRandomPatchConfiguration config) {
        return config.stateProvider.getState(random, pos);
    }
}
