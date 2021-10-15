package net.minecraft.world.level.levelgen;

import java.util.Random;

public class SeededRandom extends Random implements RandomSource {
    private int count;

    public SeededRandom() {
    }

    public SeededRandom(long seed) {
        super(seed);
    }

    public int getCount() {
        return this.count;
    }

    @Override
    public int next(int i) {
        ++this.count;
        return super.next(i);
    }

    public long setBaseChunkSeed(int chunkX, int chunkZ) {
        long l = (long)chunkX * 341873128712L + (long)chunkZ * 132897987541L;
        this.setSeed(l);
        return l;
    }

    public long setDecorationSeed(long worldSeed, int blockX, int blockZ) {
        this.setSeed(worldSeed);
        long l = this.nextLong() | 1L;
        long m = this.nextLong() | 1L;
        long n = (long)blockX * l + (long)blockZ * m ^ worldSeed;
        this.setSeed(n);
        return n;
    }

    public long setFeatureSeed(long populationSeed, int index, int step) {
        long l = populationSeed + (long)index + (long)(10000 * step);
        this.setSeed(l);
        return l;
    }

    public long setLargeFeatureSeed(long worldSeed, int chunkX, int chunkZ) {
        this.setSeed(worldSeed);
        long l = this.nextLong();
        long m = this.nextLong();
        long n = (long)chunkX * l ^ (long)chunkZ * m ^ worldSeed;
        this.setSeed(n);
        return n;
    }

    public long setBaseStoneSeed(long worldSeed, int x, int y, int z) {
        this.setSeed(worldSeed);
        long l = this.nextLong();
        long m = this.nextLong();
        long n = this.nextLong();
        long o = (long)x * l ^ (long)y * m ^ (long)z * n ^ worldSeed;
        this.setSeed(o);
        return o;
    }

    public long setLargeFeatureWithSalt(long worldSeed, int regionX, int regionZ, int salt) {
        long l = (long)regionX * 341873128712L + (long)regionZ * 132897987541L + worldSeed + (long)salt;
        this.setSeed(l);
        return l;
    }

    public static Random seedSlimeChunk(int chunkX, int chunkZ, long worldSeed, long scrambler) {
        return new Random(worldSeed + (long)(chunkX * chunkX * 4987142) + (long)(chunkX * 5947611) + (long)(chunkZ * chunkZ) * 4392871L + (long)(chunkZ * 389711) ^ scrambler);
    }
}
