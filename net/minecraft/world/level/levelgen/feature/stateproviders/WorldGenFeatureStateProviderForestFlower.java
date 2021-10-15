package net.minecraft.world.level.levelgen.feature.stateproviders;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;

public class WorldGenFeatureStateProviderForestFlower extends WorldGenFeatureStateProvider {
    public static final Codec<WorldGenFeatureStateProviderForestFlower> CODEC;
    private static final IBlockData[] FLOWERS = new IBlockData[]{Blocks.DANDELION.getBlockData(), Blocks.POPPY.getBlockData(), Blocks.ALLIUM.getBlockData(), Blocks.AZURE_BLUET.getBlockData(), Blocks.RED_TULIP.getBlockData(), Blocks.ORANGE_TULIP.getBlockData(), Blocks.WHITE_TULIP.getBlockData(), Blocks.PINK_TULIP.getBlockData(), Blocks.OXEYE_DAISY.getBlockData(), Blocks.CORNFLOWER.getBlockData(), Blocks.LILY_OF_THE_VALLEY.getBlockData()};
    public static final WorldGenFeatureStateProviderForestFlower INSTANCE = new WorldGenFeatureStateProviderForestFlower();

    @Override
    protected WorldGenFeatureStateProviders<?> type() {
        return WorldGenFeatureStateProviders.FOREST_FLOWER_PROVIDER;
    }

    @Override
    public IBlockData getState(Random random, BlockPosition pos) {
        double d = MathHelper.clamp((1.0D + BiomeBase.BIOME_INFO_NOISE.getValue((double)pos.getX() / 48.0D, (double)pos.getZ() / 48.0D, false)) / 2.0D, 0.0D, 0.9999D);
        return FLOWERS[(int)(d * (double)FLOWERS.length)];
    }

    static {
        CODEC = Codec.unit(() -> {
            return INSTANCE;
        });
    }
}
