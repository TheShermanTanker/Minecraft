package net.minecraft.world.level.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.Supplier;

public class WorldChunkManagerCheckerBoard extends WorldChunkManager {
    public static final Codec<WorldChunkManagerCheckerBoard> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(BiomeBase.LIST_CODEC.fieldOf("biomes").forGetter((checkerboardColumnBiomeSource) -> {
            return checkerboardColumnBiomeSource.allowedBiomes;
        }), Codec.intRange(0, 62).fieldOf("scale").orElse(2).forGetter((checkerboardColumnBiomeSource) -> {
            return checkerboardColumnBiomeSource.size;
        })).apply(instance, WorldChunkManagerCheckerBoard::new);
    });
    private final List<Supplier<BiomeBase>> allowedBiomes;
    private final int bitShift;
    private final int size;

    public WorldChunkManagerCheckerBoard(List<Supplier<BiomeBase>> biomeArray, int size) {
        super(biomeArray.stream());
        this.allowedBiomes = biomeArray;
        this.bitShift = size + 2;
        this.size = size;
    }

    @Override
    protected Codec<? extends WorldChunkManager> codec() {
        return CODEC;
    }

    @Override
    public WorldChunkManager withSeed(long seed) {
        return this;
    }

    @Override
    public BiomeBase getNoiseBiome(int x, int y, int z, Climate.Sampler noise) {
        return this.allowedBiomes.get(Math.floorMod((x >> this.bitShift) + (z >> this.bitShift), this.allowedBiomes.size())).get();
    }
}
