package net.minecraft.world.level.biome;

import com.google.common.hash.Hashing;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.QuartPos;
import net.minecraft.util.LinearCongruentialGenerator;
import net.minecraft.util.MathHelper;

public class BiomeManager {
    public static final int CHUNK_CENTER_QUART = QuartPos.fromBlock(8);
    private static final int ZOOM_BITS = 2;
    private static final int ZOOM = 4;
    private static final int ZOOM_MASK = 3;
    private final BiomeManager.Provider noiseBiomeSource;
    private final long biomeZoomSeed;

    public BiomeManager(BiomeManager.Provider storage, long seed) {
        this.noiseBiomeSource = storage;
        this.biomeZoomSeed = seed;
    }

    public static long obfuscateSeed(long seed) {
        return Hashing.sha256().hashLong(seed).asLong();
    }

    public BiomeManager withDifferentSource(BiomeManager.Provider storage) {
        return new BiomeManager(storage, this.biomeZoomSeed);
    }

    public BiomeBase getBiome(BlockPosition pos) {
        int i = pos.getX() - 2;
        int j = pos.getY() - 2;
        int k = pos.getZ() - 2;
        int l = i >> 2;
        int m = j >> 2;
        int n = k >> 2;
        double d = (double)(i & 3) / 4.0D;
        double e = (double)(j & 3) / 4.0D;
        double f = (double)(k & 3) / 4.0D;
        int o = 0;
        double g = Double.POSITIVE_INFINITY;

        for(int p = 0; p < 8; ++p) {
            boolean bl = (p & 4) == 0;
            boolean bl2 = (p & 2) == 0;
            boolean bl3 = (p & 1) == 0;
            int q = bl ? l : l + 1;
            int r = bl2 ? m : m + 1;
            int s = bl3 ? n : n + 1;
            double h = bl ? d : d - 1.0D;
            double t = bl2 ? e : e - 1.0D;
            double u = bl3 ? f : f - 1.0D;
            double v = getFiddledDistance(this.biomeZoomSeed, q, r, s, h, t, u);
            if (g > v) {
                o = p;
                g = v;
            }
        }

        int w = (o & 4) == 0 ? l : l + 1;
        int x = (o & 2) == 0 ? m : m + 1;
        int y = (o & 1) == 0 ? n : n + 1;
        return this.noiseBiomeSource.getBiome(w, x, y);
    }

    public BiomeBase getNoiseBiomeAtPosition(double x, double y, double z) {
        int i = QuartPos.fromBlock(MathHelper.floor(x));
        int j = QuartPos.fromBlock(MathHelper.floor(y));
        int k = QuartPos.fromBlock(MathHelper.floor(z));
        return this.getNoiseBiomeAtQuart(i, j, k);
    }

    public BiomeBase getNoiseBiomeAtPosition(BlockPosition pos) {
        int i = QuartPos.fromBlock(pos.getX());
        int j = QuartPos.fromBlock(pos.getY());
        int k = QuartPos.fromBlock(pos.getZ());
        return this.getNoiseBiomeAtQuart(i, j, k);
    }

    public BiomeBase getNoiseBiomeAtQuart(int biomeX, int biomeY, int biomeZ) {
        return this.noiseBiomeSource.getBiome(biomeX, biomeY, biomeZ);
    }

    private static double getFiddledDistance(long l, int i, int j, int k, double d, double e, double f) {
        long m = LinearCongruentialGenerator.next(l, (long)i);
        m = LinearCongruentialGenerator.next(m, (long)j);
        m = LinearCongruentialGenerator.next(m, (long)k);
        m = LinearCongruentialGenerator.next(m, (long)i);
        m = LinearCongruentialGenerator.next(m, (long)j);
        m = LinearCongruentialGenerator.next(m, (long)k);
        double g = getFiddle(m);
        m = LinearCongruentialGenerator.next(m, l);
        double h = getFiddle(m);
        m = LinearCongruentialGenerator.next(m, l);
        double n = getFiddle(m);
        return MathHelper.square(f + n) + MathHelper.square(e + h) + MathHelper.square(d + g);
    }

    private static double getFiddle(long l) {
        double d = (double)Math.floorMod(l >> 24, 1024) / 1024.0D;
        return (d - 0.5D) * 0.9D;
    }

    public interface Provider {
        BiomeBase getBiome(int biomeX, int biomeY, int biomeZ);
    }
}
