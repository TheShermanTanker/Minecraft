package net.minecraft.world.level.biome;

import net.minecraft.util.LinearCongruentialGenerator;

public enum GenLayerZoomVoronoi implements GenLayerZoomer {
    INSTANCE;

    private static final int ZOOM_BITS = 2;
    private static final int ZOOM = 4;
    private static final int ZOOM_MASK = 3;

    @Override
    public BiomeBase getBiome(long seed, int x, int y, int z, BiomeManager.Provider storage) {
        int i = x - 2;
        int j = y - 2;
        int k = z - 2;
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
            double v = getFiddledDistance(seed, q, r, s, h, t, u);
            if (g > v) {
                o = p;
                g = v;
            }
        }

        int w = (o & 4) == 0 ? l : l + 1;
        int aa = (o & 2) == 0 ? m : m + 1;
        int ab = (o & 1) == 0 ? n : n + 1;
        return storage.getBiome(w, aa, ab);
    }

    private static double getFiddledDistance(long seed, int x, int y, int z, double xFraction, double yFraction, double zFraction) {
        long l = LinearCongruentialGenerator.next(seed, (long)x);
        l = LinearCongruentialGenerator.next(l, (long)y);
        l = LinearCongruentialGenerator.next(l, (long)z);
        l = LinearCongruentialGenerator.next(l, (long)x);
        l = LinearCongruentialGenerator.next(l, (long)y);
        l = LinearCongruentialGenerator.next(l, (long)z);
        double d = getFiddle(l);
        l = LinearCongruentialGenerator.next(l, seed);
        double e = getFiddle(l);
        l = LinearCongruentialGenerator.next(l, seed);
        double f = getFiddle(l);
        return sqr(zFraction + f) + sqr(yFraction + e) + sqr(xFraction + d);
    }

    private static double getFiddle(long seed) {
        double d = (double)Math.floorMod(seed >> 24, 1024) / 1024.0D;
        return (d - 0.5D) * 0.9D;
    }

    private static double sqr(double d) {
        return d * d;
    }
}
