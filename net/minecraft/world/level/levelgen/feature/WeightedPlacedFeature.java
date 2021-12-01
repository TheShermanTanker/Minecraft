package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Random;
import java.util.function.Supplier;
import net.minecraft.core.BlockPosition;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class WeightedPlacedFeature {
    public static final Codec<WeightedPlacedFeature> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(PlacedFeature.CODEC.fieldOf("feature").flatXmap(ExtraCodecs.nonNullSupplierCheck(), ExtraCodecs.nonNullSupplierCheck()).forGetter((weightedPlacedFeature) -> {
            return weightedPlacedFeature.feature;
        }), Codec.floatRange(0.0F, 1.0F).fieldOf("chance").forGetter((weightedPlacedFeature) -> {
            return weightedPlacedFeature.chance;
        })).apply(instance, WeightedPlacedFeature::new);
    });
    public final Supplier<PlacedFeature> feature;
    public final float chance;

    public WeightedPlacedFeature(PlacedFeature feature, float chance) {
        this(() -> {
            return feature;
        }, chance);
    }

    private WeightedPlacedFeature(Supplier<PlacedFeature> feature, float chance) {
        this.feature = feature;
        this.chance = chance;
    }

    public boolean place(GeneratorAccessSeed world, ChunkGenerator chunkGenerator, Random random, BlockPosition pos) {
        return this.feature.get().place(world, chunkGenerator, random, pos);
    }
}
