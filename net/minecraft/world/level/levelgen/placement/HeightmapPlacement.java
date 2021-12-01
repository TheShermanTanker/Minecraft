package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Random;
import java.util.stream.Stream;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.levelgen.HeightMap;

public class HeightmapPlacement extends PlacementModifier {
    public static final Codec<HeightmapPlacement> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(HeightMap.Type.CODEC.fieldOf("heightmap").forGetter((heightmapPlacement) -> {
            return heightmapPlacement.heightmap;
        })).apply(instance, HeightmapPlacement::new);
    });
    private final HeightMap.Type heightmap;

    private HeightmapPlacement(HeightMap.Type heightmap) {
        this.heightmap = heightmap;
    }

    public static HeightmapPlacement onHeightmap(HeightMap.Type heightmap) {
        return new HeightmapPlacement(heightmap);
    }

    @Override
    public Stream<BlockPosition> getPositions(PlacementContext context, Random random, BlockPosition pos) {
        int i = pos.getX();
        int j = pos.getZ();
        int k = context.getHeight(this.heightmap, i, j);
        return k > context.getMinBuildHeight() ? Stream.of(new BlockPosition(i, k, j)) : Stream.of();
    }

    @Override
    public PlacementModifierType<?> type() {
        return PlacementModifierType.HEIGHTMAP;
    }
}
