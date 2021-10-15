package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureDecoratorConfiguration;

public class WorldGenDecoratorDecpratedConfiguration implements WorldGenFeatureDecoratorConfiguration {
    public static final Codec<WorldGenDecoratorDecpratedConfiguration> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(WorldGenDecoratorConfigured.CODEC.fieldOf("outer").forGetter(WorldGenDecoratorDecpratedConfiguration::outer), WorldGenDecoratorConfigured.CODEC.fieldOf("inner").forGetter(WorldGenDecoratorDecpratedConfiguration::inner)).apply(instance, WorldGenDecoratorDecpratedConfiguration::new);
    });
    private final WorldGenDecoratorConfigured<?> outer;
    private final WorldGenDecoratorConfigured<?> inner;

    public WorldGenDecoratorDecpratedConfiguration(WorldGenDecoratorConfigured<?> outer, WorldGenDecoratorConfigured<?> inner) {
        this.outer = outer;
        this.inner = inner;
    }

    public WorldGenDecoratorConfigured<?> outer() {
        return this.outer;
    }

    public WorldGenDecoratorConfigured<?> inner() {
        return this.inner;
    }
}
