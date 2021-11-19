package net.minecraft.world.level.levelgen;

import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviderConstant;
import net.minecraft.util.valueproviders.IntProviderUniform;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenDecoratorFrequencyConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureChanceDecoratorRangeConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureEmptyConfiguration2;
import net.minecraft.world.level.levelgen.heightproviders.TrapezoidHeight;
import net.minecraft.world.level.levelgen.heightproviders.UniformHeight;
import net.minecraft.world.level.levelgen.placement.WorldGenDecorator;
import net.minecraft.world.level.levelgen.placement.WorldGenDecoratorConfigured;
import net.minecraft.world.level.levelgen.placement.WorldGenDecoratorDungeonConfiguration;

public interface IDecoratable<R> {
    R decorated(WorldGenDecoratorConfigured<?> decorator);

    default R rarity(int chance) {
        return this.decorated(WorldGenDecorator.CHANCE.configured(new WorldGenDecoratorDungeonConfiguration(chance)));
    }

    default R count(IntProvider count) {
        return this.decorated(WorldGenDecorator.COUNT.configured(new WorldGenDecoratorFrequencyConfiguration(count)));
    }

    default R count(int count) {
        return this.count(IntProviderConstant.of(count));
    }

    default R countRandom(int maxCount) {
        return this.count(IntProviderUniform.of(0, maxCount));
    }

    default R rangeUniform(VerticalAnchor min, VerticalAnchor max) {
        return this.range(new WorldGenFeatureChanceDecoratorRangeConfiguration(UniformHeight.of(min, max)));
    }

    default R rangeTriangle(VerticalAnchor min, VerticalAnchor max) {
        return this.range(new WorldGenFeatureChanceDecoratorRangeConfiguration(TrapezoidHeight.of(min, max)));
    }

    default R range(WorldGenFeatureChanceDecoratorRangeConfiguration config) {
        return this.decorated(WorldGenDecorator.RANGE.configured(config));
    }

    default R squared() {
        return this.decorated(WorldGenDecorator.SQUARE.configured(WorldGenFeatureEmptyConfiguration2.INSTANCE));
    }
}
