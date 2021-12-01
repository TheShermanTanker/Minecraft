package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockFalling;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureCircleConfiguration;

public class WorldGenFeatureDisk extends WorldGenerator<WorldGenFeatureCircleConfiguration> {
    public WorldGenFeatureDisk(Codec<WorldGenFeatureCircleConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeaturePlaceContext<WorldGenFeatureCircleConfiguration> context) {
        WorldGenFeatureCircleConfiguration diskConfiguration = context.config();
        BlockPosition blockPos = context.origin();
        GeneratorAccessSeed worldGenLevel = context.level();
        boolean bl = false;
        int i = blockPos.getY();
        int j = i + diskConfiguration.halfHeight();
        int k = i - diskConfiguration.halfHeight() - 1;
        boolean bl2 = diskConfiguration.state().getBlock() instanceof BlockFalling;
        int l = diskConfiguration.radius().sample(context.random());

        for(int m = blockPos.getX() - l; m <= blockPos.getX() + l; ++m) {
            for(int n = blockPos.getZ() - l; n <= blockPos.getZ() + l; ++n) {
                int o = m - blockPos.getX();
                int p = n - blockPos.getZ();
                if (o * o + p * p <= l * l) {
                    boolean bl3 = false;

                    for(int q = j; q >= k; --q) {
                        BlockPosition blockPos2 = new BlockPosition(m, q, n);
                        IBlockData blockState = worldGenLevel.getType(blockPos2);
                        Block block = blockState.getBlock();
                        boolean bl4 = false;
                        if (q > k) {
                            for(IBlockData blockState2 : diskConfiguration.targets()) {
                                if (blockState2.is(block)) {
                                    worldGenLevel.setTypeAndData(blockPos2, diskConfiguration.state(), 2);
                                    this.markAboveForPostProcessing(worldGenLevel, blockPos2);
                                    bl = true;
                                    bl4 = true;
                                    break;
                                }
                            }
                        }

                        if (bl2 && bl3 && blockState.isAir()) {
                            IBlockData blockState3 = diskConfiguration.state().is(Blocks.RED_SAND) ? Blocks.RED_SANDSTONE.getBlockData() : Blocks.SANDSTONE.getBlockData();
                            worldGenLevel.setTypeAndData(new BlockPosition(m, q + 1, n), blockState3, 2);
                        }

                        bl3 = bl4;
                    }
                }
            }
        }

        return bl;
    }
}
