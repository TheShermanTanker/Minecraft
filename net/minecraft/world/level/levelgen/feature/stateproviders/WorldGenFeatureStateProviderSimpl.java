package net.minecraft.world.level.levelgen.feature.stateproviders;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.block.state.IBlockData;

public class WorldGenFeatureStateProviderSimpl extends WorldGenFeatureStateProvider {
    public static final Codec<WorldGenFeatureStateProviderSimpl> CODEC = IBlockData.CODEC.fieldOf("state").xmap(WorldGenFeatureStateProviderSimpl::new, (simpleStateProvider) -> {
        return simpleStateProvider.state;
    }).codec();
    private final IBlockData state;

    public WorldGenFeatureStateProviderSimpl(IBlockData state) {
        this.state = state;
    }

    @Override
    protected WorldGenFeatureStateProviders<?> type() {
        return WorldGenFeatureStateProviders.SIMPLE_STATE_PROVIDER;
    }

    @Override
    public IBlockData getState(Random random, BlockPosition pos) {
        return this.state;
    }
}
