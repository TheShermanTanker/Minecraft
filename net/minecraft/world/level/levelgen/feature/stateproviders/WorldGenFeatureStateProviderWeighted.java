package net.minecraft.world.level.levelgen.feature.stateproviders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.world.level.block.state.IBlockData;

public class WorldGenFeatureStateProviderWeighted extends WorldGenFeatureStateProvider {
    public static final Codec<WorldGenFeatureStateProviderWeighted> CODEC = SimpleWeightedRandomList.wrappedCodec(IBlockData.CODEC).comapFlatMap(WorldGenFeatureStateProviderWeighted::create, (weightedStateProvider) -> {
        return weightedStateProvider.weightedList;
    }).fieldOf("entries").codec();
    private final SimpleWeightedRandomList<IBlockData> weightedList;

    private static DataResult<WorldGenFeatureStateProviderWeighted> create(SimpleWeightedRandomList<IBlockData> states) {
        return states.isEmpty() ? DataResult.error("WeightedStateProvider with no states") : DataResult.success(new WorldGenFeatureStateProviderWeighted(states));
    }

    public WorldGenFeatureStateProviderWeighted(SimpleWeightedRandomList<IBlockData> states) {
        this.weightedList = states;
    }

    public WorldGenFeatureStateProviderWeighted(SimpleWeightedRandomList.Builder<IBlockData> states) {
        this(states.build());
    }

    @Override
    protected WorldGenFeatureStateProviders<?> type() {
        return WorldGenFeatureStateProviders.WEIGHTED_STATE_PROVIDER;
    }

    @Override
    public IBlockData getState(Random random, BlockPosition pos) {
        return this.weightedList.getRandomValue(random).orElseThrow(IllegalStateException::new);
    }
}
