package net.minecraft.world.level.levelgen.feature.stateproviders;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;

public class WorldGenFeatureStateProviderPlainFlower extends WorldGenFeatureStateProvider {
    public static final Codec<WorldGenFeatureStateProviderPlainFlower> CODEC;
    public static final WorldGenFeatureStateProviderPlainFlower INSTANCE = new WorldGenFeatureStateProviderPlainFlower();
    private static final IBlockData[] LOW_NOISE_FLOWERS = new IBlockData[]{Blocks.ORANGE_TULIP.getBlockData(), Blocks.RED_TULIP.getBlockData(), Blocks.PINK_TULIP.getBlockData(), Blocks.WHITE_TULIP.getBlockData()};
    private static final IBlockData[] HIGH_NOISE_FLOWERS = new IBlockData[]{Blocks.POPPY.getBlockData(), Blocks.AZURE_BLUET.getBlockData(), Blocks.OXEYE_DAISY.getBlockData(), Blocks.CORNFLOWER.getBlockData()};

    @Override
    protected WorldGenFeatureStateProviders<?> type() {
        return WorldGenFeatureStateProviders.PLAIN_FLOWER_PROVIDER;
    }

    @Override
    public IBlockData getState(Random random, BlockPosition pos) {
        double d = BiomeBase.BIOME_INFO_NOISE.getValue((double)pos.getX() / 200.0D, (double)pos.getZ() / 200.0D, false);
        if (d < -0.8D) {
            return SystemUtils.getRandom(LOW_NOISE_FLOWERS, random);
        } else {
            return random.nextInt(3) > 0 ? SystemUtils.getRandom(HIGH_NOISE_FLOWERS, random) : Blocks.DANDELION.getBlockData();
        }
    }

    static {
        CODEC = Codec.unit(() -> {
            return INSTANCE;
        });
    }
}
