package net.minecraft.world.level.lighting;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import javax.annotation.Nullable;
import net.minecraft.world.level.chunk.NibbleArray;

public abstract class LightEngineStorageArray<M extends LightEngineStorageArray<M>> {
    private static final int CACHE_SIZE = 2;
    private final long[] lastSectionKeys = new long[2];
    private final NibbleArray[] lastSections = new NibbleArray[2];
    private boolean cacheEnabled;
    protected final Long2ObjectOpenHashMap<NibbleArray> map;

    protected LightEngineStorageArray(Long2ObjectOpenHashMap<NibbleArray> arrays) {
        this.map = arrays;
        this.clearCache();
        this.cacheEnabled = true;
    }

    public abstract M copy();

    public void copyDataLayer(long pos) {
        this.map.put(pos, this.map.get(pos).copy());
        this.clearCache();
    }

    public boolean hasLayer(long chunkPos) {
        return this.map.containsKey(chunkPos);
    }

    @Nullable
    public NibbleArray getLayer(long chunkPos) {
        if (this.cacheEnabled) {
            for(int i = 0; i < 2; ++i) {
                if (chunkPos == this.lastSectionKeys[i]) {
                    return this.lastSections[i];
                }
            }
        }

        NibbleArray dataLayer = this.map.get(chunkPos);
        if (dataLayer == null) {
            return null;
        } else {
            if (this.cacheEnabled) {
                for(int j = 1; j > 0; --j) {
                    this.lastSectionKeys[j] = this.lastSectionKeys[j - 1];
                    this.lastSections[j] = this.lastSections[j - 1];
                }

                this.lastSectionKeys[0] = chunkPos;
                this.lastSections[0] = dataLayer;
            }

            return dataLayer;
        }
    }

    @Nullable
    public NibbleArray removeLayer(long chunkPos) {
        return this.map.remove(chunkPos);
    }

    public void setLayer(long pos, NibbleArray data) {
        this.map.put(pos, data);
    }

    public void clearCache() {
        for(int i = 0; i < 2; ++i) {
            this.lastSectionKeys[i] = Long.MAX_VALUE;
            this.lastSections[i] = null;
        }

    }

    public void disableCache() {
        this.cacheEnabled = false;
    }
}
