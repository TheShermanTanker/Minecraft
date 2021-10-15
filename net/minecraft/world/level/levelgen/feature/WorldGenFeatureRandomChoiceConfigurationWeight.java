package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Random;
import java.util.function.Supplier;
import net.minecraft.core.BlockPosition;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.chunk.ChunkGenerator;

public class WorldGenFeatureRandomChoiceConfigurationWeight {
    public static final Codec<WorldGenFeatureRandomChoiceConfigurationWeight> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(WorldGenFeatureConfigured.CODEC.fieldOf("feature").flatXmap(ExtraCodecs.nonNullSupplierCheck(), ExtraCodecs.nonNullSupplierCheck()).forGetter((weightedConfiguredFeature) -> {
            return weightedConfiguredFeature.feature;
        }), Codec.floatRange(0.0F, 1.0F).fieldOf("chance").forGetter((weightedConfiguredFeature) -> {
            return weightedConfiguredFeature.chance;
        })).apply(instance, WorldGenFeatureRandomChoiceConfigurationWeight::new);
    });
    public final Supplier<WorldGenFeatureConfigured<?, ?>> feature;
    public final float chance;

    public WorldGenFeatureRandomChoiceConfigurationWeight(WorldGenFeatureConfigured<?, ?> feature, float chance) {
        this(() -> {
            return feature;
        }, chance);
    }

    private WorldGenFeatureRandomChoiceConfigurationWeight(Supplier<WorldGenFeatureConfigured<?, ?>> feature, float chance) {
        this.feature = feature;
        this.chance = chance;
    }

    public boolean place(GeneratorAccessSeed world, ChunkGenerator chunkGenerator, Random random, BlockPosition pos) {
        return this.feature.get().place(world, chunkGenerator, random, pos);
    }
}
