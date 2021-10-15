package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import java.util.Random;
import java.util.stream.Stream;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.world.level.levelgen.IDecoratable;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureDecoratorConfiguration;

public class WorldGenDecoratorConfigured<DC extends WorldGenFeatureDecoratorConfiguration> implements IDecoratable<WorldGenDecoratorConfigured<?>> {
    public static final Codec<WorldGenDecoratorConfigured<?>> CODEC = IRegistry.DECORATOR.dispatch("type", (configuredDecorator) -> {
        return configuredDecorator.decorator;
    }, WorldGenDecorator::configuredCodec);
    private final WorldGenDecorator<DC> decorator;
    private final DC config;

    public WorldGenDecoratorConfigured(WorldGenDecorator<DC> decorator, DC config) {
        this.decorator = decorator;
        this.config = config;
    }

    public Stream<BlockPosition> getPositions(WorldGenDecoratorContext context, Random random, BlockPosition pos) {
        return this.decorator.getPositions(context, random, this.config, pos);
    }

    @Override
    public String toString() {
        return String.format("[%s %s]", IRegistry.DECORATOR.getKey(this.decorator), this.config);
    }

    @Override
    public WorldGenDecoratorConfigured<?> decorated(WorldGenDecoratorConfigured<?> configuredDecorator) {
        return new WorldGenDecoratorConfigured<>(WorldGenDecorator.DECORATED, new WorldGenDecoratorDecpratedConfiguration(configuredDecorator, this));
    }

    public DC config() {
        return this.config;
    }
}
