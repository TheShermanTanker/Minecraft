package net.minecraft.world.level.chunk;

import java.util.Arrays;
import javax.annotation.Nullable;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.IWorldHeightAccess;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.WorldChunkManager;
import net.minecraft.world.level.dimension.DimensionManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BiomeStorage implements BiomeManager.Provider {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int WIDTH_BITS = MathHelper.ceillog2(16) - 2;
    private static final int HORIZONTAL_MASK = (1 << WIDTH_BITS) - 1;
    public static final int MAX_SIZE = 1 << WIDTH_BITS + WIDTH_BITS + DimensionManager.BITS_FOR_Y - 2;
    public final Registry<BiomeBase> biomeRegistry;
    private final BiomeBase[] biomes;
    private final int quartMinY;
    private final int quartHeight;

    protected BiomeStorage(Registry<BiomeBase> biomes, IWorldHeightAccess world, BiomeBase[] data) {
        this.biomeRegistry = biomes;
        this.biomes = data;
        this.quartMinY = QuartPos.fromBlock(world.getMinBuildHeight());
        this.quartHeight = QuartPos.fromBlock(world.getHeight()) - 1;
    }

    public BiomeStorage(Registry<BiomeBase> biomes, IWorldHeightAccess world, int[] ids) {
        this(biomes, world, new BiomeBase[ids.length]);
        int i = -1;

        for(int j = 0; j < this.biomes.length; ++j) {
            int k = ids[j];
            BiomeBase biome = biomes.fromId(k);
            if (biome == null) {
                if (i == -1) {
                    i = j;
                }

                this.biomes[j] = biomes.fromId(0);
            } else {
                this.biomes[j] = biome;
            }
        }

        if (i != -1) {
            LOGGER.warn("Invalid biome data received, starting from {}: {}", i, Arrays.toString(ids));
        }

    }

    public BiomeStorage(Registry<BiomeBase> biomes, IWorldHeightAccess world, ChunkCoordIntPair chunkPos, WorldChunkManager biomeSource) {
        this(biomes, world, chunkPos, biomeSource, (int[])null);
    }

    public BiomeStorage(Registry<BiomeBase> biomes, IWorldHeightAccess world, ChunkCoordIntPair chunkPos, WorldChunkManager biomeSource, @Nullable int[] is) {
        this(biomes, world, new BiomeBase[(1 << WIDTH_BITS + WIDTH_BITS) * ceilDiv(world.getHeight(), 4)]);
        int i = QuartPos.fromBlock(chunkPos.getMinBlockX());
        int j = this.quartMinY;
        int k = QuartPos.fromBlock(chunkPos.getMinBlockZ());

        for(int l = 0; l < this.biomes.length; ++l) {
            if (is != null && l < is.length) {
                this.biomes[l] = biomes.fromId(is[l]);
            }

            if (this.biomes[l] == null) {
                this.biomes[l] = generateBiomeForIndex(biomeSource, i, j, k, l);
            }
        }

    }

    private static int ceilDiv(int i, int j) {
        return (i + j - 1) / j;
    }

    private static BiomeBase generateBiomeForIndex(WorldChunkManager biomeSource, int i, int j, int k, int l) {
        int m = l & HORIZONTAL_MASK;
        int n = l >> WIDTH_BITS + WIDTH_BITS;
        int o = l >> WIDTH_BITS & HORIZONTAL_MASK;
        return biomeSource.getBiome(i + m, j + n, k + o);
    }

    public int[] writeBiomes() {
        int[] is = new int[this.biomes.length];

        for(int i = 0; i < this.biomes.length; ++i) {
            is[i] = this.biomeRegistry.getId(this.biomes[i]);
        }

        return is;
    }

    @Override
    public BiomeBase getBiome(int biomeX, int biomeY, int biomeZ) {
        int i = biomeX & HORIZONTAL_MASK;
        int j = MathHelper.clamp(biomeY - this.quartMinY, 0, this.quartHeight);
        int k = biomeZ & HORIZONTAL_MASK;
        return this.biomes[j << WIDTH_BITS + WIDTH_BITS | k << WIDTH_BITS | i];
    }
}
