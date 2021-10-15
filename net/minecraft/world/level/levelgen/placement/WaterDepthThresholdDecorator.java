package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import java.util.Random;
import java.util.stream.Stream;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.levelgen.HeightMap;

public class WaterDepthThresholdDecorator extends WorldGenDecorator<WaterDepthThresholdConfiguration> {
    public WaterDepthThresholdDecorator(Codec<WaterDepthThresholdConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public Stream<BlockPosition> getPositions(WorldGenDecoratorContext context, Random random, WaterDepthThresholdConfiguration config, BlockPosition pos) {
        int i = context.getHeight(HeightMap.Type.OCEAN_FLOOR, pos.getX(), pos.getZ());
        int j = context.getHeight(HeightMap.Type.WORLD_SURFACE, pos.getX(), pos.getZ());
        return j - i > config.maxWaterDepth ? Stream.of() : Stream.of(pos);
    }
}
