package net.minecraft.world.level.levelgen.feature.stateproviders;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.IBlockData;

public abstract class WorldGenFeatureStateProvider {
    public static final Codec<WorldGenFeatureStateProvider> CODEC = IRegistry.BLOCKSTATE_PROVIDER_TYPES.byNameCodec().dispatch(WorldGenFeatureStateProvider::type, WorldGenFeatureStateProviders::codec);

    public static WorldGenFeatureStateProviderSimpl simple(IBlockData state) {
        return new WorldGenFeatureStateProviderSimpl(state);
    }

    public static WorldGenFeatureStateProviderSimpl simple(Block block) {
        return new WorldGenFeatureStateProviderSimpl(block.getBlockData());
    }

    protected abstract WorldGenFeatureStateProviders<?> type();

    public abstract IBlockData getState(Random random, BlockPosition pos);
}
