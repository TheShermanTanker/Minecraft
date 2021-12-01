package net.minecraft.world.level.biome;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.IRegistry;
import net.minecraft.resources.RegistryLookupCodec;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.SeededRandom;
import net.minecraft.world.level.levelgen.synth.NoiseGenerator3Handler;

public class WorldChunkManagerTheEnd extends WorldChunkManager {
    public static final Codec<WorldChunkManagerTheEnd> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(RegistryLookupCodec.create(IRegistry.BIOME_REGISTRY).forGetter((theEndBiomeSource) -> {
            return theEndBiomeSource.biomes;
        }), Codec.LONG.fieldOf("seed").stable().forGetter((theEndBiomeSource) -> {
            return theEndBiomeSource.seed;
        })).apply(instance, instance.stable(WorldChunkManagerTheEnd::new));
    });
    private static final float ISLAND_THRESHOLD = -0.9F;
    public static final int ISLAND_CHUNK_DISTANCE = 64;
    private static final long ISLAND_CHUNK_DISTANCE_SQR = 4096L;
    private final NoiseGenerator3Handler islandNoise;
    private final IRegistry<BiomeBase> biomes;
    private final long seed;
    private final BiomeBase end;
    private final BiomeBase highlands;
    private final BiomeBase midlands;
    private final BiomeBase islands;
    private final BiomeBase barrens;

    public WorldChunkManagerTheEnd(IRegistry<BiomeBase> biomeRegistry, long seed) {
        this(biomeRegistry, seed, biomeRegistry.getOrThrow(Biomes.THE_END), biomeRegistry.getOrThrow(Biomes.END_HIGHLANDS), biomeRegistry.getOrThrow(Biomes.END_MIDLANDS), biomeRegistry.getOrThrow(Biomes.SMALL_END_ISLANDS), biomeRegistry.getOrThrow(Biomes.END_BARRENS));
    }

    private WorldChunkManagerTheEnd(IRegistry<BiomeBase> biomeRegistry, long seed, BiomeBase centerBiome, BiomeBase highlandsBiome, BiomeBase midlandsBiome, BiomeBase smallIslandsBiome, BiomeBase barrensBiome) {
        super(ImmutableList.of(centerBiome, highlandsBiome, midlandsBiome, smallIslandsBiome, barrensBiome));
        this.biomes = biomeRegistry;
        this.seed = seed;
        this.end = centerBiome;
        this.highlands = highlandsBiome;
        this.midlands = midlandsBiome;
        this.islands = smallIslandsBiome;
        this.barrens = barrensBiome;
        SeededRandom worldgenRandom = new SeededRandom(new LegacyRandomSource(seed));
        worldgenRandom.consumeCount(17292);
        this.islandNoise = new NoiseGenerator3Handler(worldgenRandom);
    }

    @Override
    protected Codec<? extends WorldChunkManager> codec() {
        return CODEC;
    }

    @Override
    public WorldChunkManager withSeed(long seed) {
        return new WorldChunkManagerTheEnd(this.biomes, seed, this.end, this.highlands, this.midlands, this.islands, this.barrens);
    }

    @Override
    public BiomeBase getNoiseBiome(int x, int y, int z, Climate.Sampler noise) {
        int i = x >> 2;
        int j = z >> 2;
        if ((long)i * (long)i + (long)j * (long)j <= 4096L) {
            return this.end;
        } else {
            float f = getHeightValue(this.islandNoise, i * 2 + 1, j * 2 + 1);
            if (f > 40.0F) {
                return this.highlands;
            } else if (f >= 0.0F) {
                return this.midlands;
            } else {
                return f < -20.0F ? this.islands : this.barrens;
            }
        }
    }

    public boolean stable(long seed) {
        return this.seed == seed;
    }

    public static float getHeightValue(NoiseGenerator3Handler simplexNoise, int i, int j) {
        int k = i / 2;
        int l = j / 2;
        int m = i % 2;
        int n = j % 2;
        float f = 100.0F - MathHelper.sqrt((float)(i * i + j * j)) * 8.0F;
        f = MathHelper.clamp(f, -100.0F, 80.0F);

        for(int o = -12; o <= 12; ++o) {
            for(int p = -12; p <= 12; ++p) {
                long q = (long)(k + o);
                long r = (long)(l + p);
                if (q * q + r * r > 4096L && simplexNoise.getValue((double)q, (double)r) < (double)-0.9F) {
                    float g = (MathHelper.abs((float)q) * 3439.0F + MathHelper.abs((float)r) * 147.0F) % 13.0F + 9.0F;
                    float h = (float)(m - o * 2);
                    float s = (float)(n - p * 2);
                    float t = 100.0F - MathHelper.sqrt(h * h + s * s) * g;
                    t = MathHelper.clamp(t, -100.0F, 80.0F);
                    f = Math.max(f, t);
                }
            }
        }

        return f;
    }
}
