package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureDecoratorConfiguration;

public class CaveDecoratorConfiguration implements WorldGenFeatureDecoratorConfiguration {
    public static final Codec<CaveDecoratorConfiguration> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(CaveSurface.CODEC.fieldOf("surface").forGetter((caveDecoratorConfiguration) -> {
            return caveDecoratorConfiguration.surface;
        }), Codec.INT.fieldOf("floor_to_ceiling_search_range").forGetter((caveDecoratorConfiguration) -> {
            return caveDecoratorConfiguration.floorToCeilingSearchRange;
        })).apply(instance, CaveDecoratorConfiguration::new);
    });
    public final CaveSurface surface;
    public final int floorToCeilingSearchRange;

    public CaveDecoratorConfiguration(CaveSurface surface, int searchRange) {
        this.surface = surface;
        this.floorToCeilingSearchRange = searchRange;
    }
}
