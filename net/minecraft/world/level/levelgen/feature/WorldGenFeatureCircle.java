package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.tags.TagsFluid;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureCircleConfiguration;

public class WorldGenFeatureCircle extends WorldGenFeatureDisk {
    public WorldGenFeatureCircle(Codec<WorldGenFeatureCircleConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeaturePlaceContext<WorldGenFeatureCircleConfiguration> context) {
        return !context.level().getFluid(context.origin()).is(TagsFluid.WATER) ? false : super.generate(context);
    }
}
