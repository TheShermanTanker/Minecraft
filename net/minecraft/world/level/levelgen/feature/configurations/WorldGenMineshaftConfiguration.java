package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.WorldGenMineshaft;

public class WorldGenMineshaftConfiguration implements WorldGenFeatureConfiguration {
    public static final Codec<WorldGenMineshaftConfiguration> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Codec.floatRange(0.0F, 1.0F).fieldOf("probability").forGetter((mineshaftConfiguration) -> {
            return mineshaftConfiguration.probability;
        }), WorldGenMineshaft.Type.CODEC.fieldOf("type").forGetter((mineshaftConfiguration) -> {
            return mineshaftConfiguration.type;
        })).apply(instance, WorldGenMineshaftConfiguration::new);
    });
    public final float probability;
    public final WorldGenMineshaft.Type type;

    public WorldGenMineshaftConfiguration(float probability, WorldGenMineshaft.Type type) {
        this.probability = probability;
        this.type = type;
    }
}
