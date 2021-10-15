package net.minecraft.world.level.newbiome.context;

import it.unimi.dsi.fastutil.longs.Long2IntLinkedOpenHashMap;
import net.minecraft.util.LinearCongruentialGenerator;
import net.minecraft.world.level.levelgen.SimpleRandomSource;
import net.minecraft.world.level.levelgen.synth.NoiseGeneratorPerlin;
import net.minecraft.world.level.newbiome.area.AreaLazy;
import net.minecraft.world.level.newbiome.layer.traits.AreaTransformer8;

public class WorldGenContextArea implements AreaContextTransformed<AreaLazy> {
    private static final int MAX_CACHE = 1024;
    private final Long2IntLinkedOpenHashMap cache;
    private final int maxCache;
    private final NoiseGeneratorPerlin biomeNoise;
    private final long seed;
    private long rval;

    public WorldGenContextArea(int cacheCapacity, long seed, long salt) {
        this.seed = mixSeed(seed, salt);
        this.biomeNoise = new NoiseGeneratorPerlin(new SimpleRandomSource(seed));
        this.cache = new Long2IntLinkedOpenHashMap(16, 0.25F);
        this.cache.defaultReturnValue(Integer.MIN_VALUE);
        this.maxCache = cacheCapacity;
    }

    @Override
    public AreaLazy createResult(AreaTransformer8 pixelTransformer) {
        return new AreaLazy(this.cache, this.maxCache, pixelTransformer);
    }

    @Override
    public AreaLazy createResult(AreaTransformer8 operator, AreaLazy parent) {
        return new AreaLazy(this.cache, Math.min(1024, parent.getMaxCache() * 4), operator);
    }

    @Override
    public AreaLazy createResult(AreaTransformer8 operator, AreaLazy firstParent, AreaLazy secondParent) {
        return new AreaLazy(this.cache, Math.min(1024, Math.max(firstParent.getMaxCache(), secondParent.getMaxCache()) * 4), operator);
    }

    @Override
    public void initRandom(long x, long y) {
        long l = this.seed;
        l = LinearCongruentialGenerator.next(l, x);
        l = LinearCongruentialGenerator.next(l, y);
        l = LinearCongruentialGenerator.next(l, x);
        l = LinearCongruentialGenerator.next(l, y);
        this.rval = l;
    }

    @Override
    public int nextRandom(int bound) {
        int i = Math.floorMod(this.rval >> 24, bound);
        this.rval = LinearCongruentialGenerator.next(this.rval, this.seed);
        return i;
    }

    @Override
    public NoiseGeneratorPerlin getBiomeNoise() {
        return this.biomeNoise;
    }

    private static long mixSeed(long seed, long salt) {
        long l = LinearCongruentialGenerator.next(salt, salt);
        l = LinearCongruentialGenerator.next(l, salt);
        l = LinearCongruentialGenerator.next(l, salt);
        long m = LinearCongruentialGenerator.next(seed, l);
        m = LinearCongruentialGenerator.next(m, l);
        return LinearCongruentialGenerator.next(m, l);
    }
}
