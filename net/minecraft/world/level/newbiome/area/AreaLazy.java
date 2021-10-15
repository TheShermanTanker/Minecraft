package net.minecraft.world.level.newbiome.area;

import it.unimi.dsi.fastutil.longs.Long2IntLinkedOpenHashMap;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.newbiome.layer.traits.AreaTransformer8;

public final class AreaLazy implements Area {
    private final AreaTransformer8 transformer;
    private final Long2IntLinkedOpenHashMap cache;
    private final int maxCache;

    public AreaLazy(Long2IntLinkedOpenHashMap cache, int cacheCapacity, AreaTransformer8 operator) {
        this.cache = cache;
        this.maxCache = cacheCapacity;
        this.transformer = operator;
    }

    @Override
    public int get(int x, int z) {
        long l = ChunkCoordIntPair.pair(x, z);
        synchronized(this.cache) {
            int i = this.cache.get(l);
            if (i != Integer.MIN_VALUE) {
                return i;
            } else {
                int j = this.transformer.apply(x, z);
                this.cache.put(l, j);
                if (this.cache.size() > this.maxCache) {
                    for(int k = 0; k < this.maxCache / 16; ++k) {
                        this.cache.removeFirstInt();
                    }
                }

                return j;
            }
        }
    }

    public int getMaxCache() {
        return this.maxCache;
    }
}
