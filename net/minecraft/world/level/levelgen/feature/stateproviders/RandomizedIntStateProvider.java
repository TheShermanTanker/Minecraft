package net.minecraft.world.level.levelgen.feature.stateproviders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collection;
import java.util.Optional;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockStateInteger;
import net.minecraft.world.level.block.state.properties.IBlockState;

public class RandomizedIntStateProvider extends WorldGenFeatureStateProvider {
    public static final Codec<RandomizedIntStateProvider> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(WorldGenFeatureStateProvider.CODEC.fieldOf("source").forGetter((randomizedIntStateProvider) -> {
            return randomizedIntStateProvider.source;
        }), Codec.STRING.fieldOf("property").forGetter((randomizedIntStateProvider) -> {
            return randomizedIntStateProvider.propertyName;
        }), IntProvider.CODEC.fieldOf("values").forGetter((randomizedIntStateProvider) -> {
            return randomizedIntStateProvider.values;
        })).apply(instance, RandomizedIntStateProvider::new);
    });
    private final WorldGenFeatureStateProvider source;
    private final String propertyName;
    @Nullable
    private BlockStateInteger property;
    private final IntProvider values;

    public RandomizedIntStateProvider(WorldGenFeatureStateProvider source, BlockStateInteger property, IntProvider values) {
        this.source = source;
        this.property = property;
        this.propertyName = property.getName();
        this.values = values;
        Collection<Integer> collection = property.getValues();

        for(int i = values.getMinValue(); i <= values.getMaxValue(); ++i) {
            if (!collection.contains(i)) {
                throw new IllegalArgumentException("Property value out of range: " + property.getName() + ": " + i);
            }
        }

    }

    public RandomizedIntStateProvider(WorldGenFeatureStateProvider source, String propertyName, IntProvider values) {
        this.source = source;
        this.propertyName = propertyName;
        this.values = values;
    }

    @Override
    protected WorldGenFeatureStateProviders<?> type() {
        return WorldGenFeatureStateProviders.RANDOMIZED_INT_STATE_PROVIDER;
    }

    @Override
    public IBlockData getState(Random random, BlockPosition pos) {
        IBlockData blockState = this.source.getState(random, pos);
        if (this.property == null || !blockState.hasProperty(this.property)) {
            this.property = findProperty(blockState, this.propertyName);
        }

        return blockState.set(this.property, Integer.valueOf(this.values.sample(random)));
    }

    private static BlockStateInteger findProperty(IBlockData state, String propertyName) {
        Collection<IBlockState<?>> collection = state.getProperties();
        Optional<BlockStateInteger> optional = collection.stream().filter((property) -> {
            return property.getName().equals(propertyName);
        }).filter((property) -> {
            return property instanceof BlockStateInteger;
        }).map((property) -> {
            return (BlockStateInteger)property;
        }).findAny();
        return optional.orElseThrow(() -> {
            return new IllegalArgumentException("Illegal property: " + propertyName);
        });
    }
}
