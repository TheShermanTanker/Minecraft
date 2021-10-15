package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.IntProvider;

public class WorldGenFeatureBasaltColumnsConfiguration implements WorldGenFeatureConfiguration {
    public static final Codec<WorldGenFeatureBasaltColumnsConfiguration> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(IntProvider.codec(0, 3).fieldOf("reach").forGetter((columnFeatureConfiguration) -> {
            return columnFeatureConfiguration.reach;
        }), IntProvider.codec(1, 10).fieldOf("height").forGetter((columnFeatureConfiguration) -> {
            return columnFeatureConfiguration.height;
        })).apply(instance, WorldGenFeatureBasaltColumnsConfiguration::new);
    });
    private final IntProvider reach;
    private final IntProvider height;

    public WorldGenFeatureBasaltColumnsConfiguration(IntProvider reach, IntProvider height) {
        this.reach = reach;
        this.height = height;
    }

    public IntProvider reach() {
        return this.reach;
    }

    public IntProvider height() {
        return this.height;
    }
}
