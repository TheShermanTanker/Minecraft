package net.minecraft.world.level.chunk.storage;

import net.minecraft.core.IRegistry;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.IWorldHeightAccess;
import net.minecraft.world.level.biome.WorldChunkManager;
import net.minecraft.world.level.chunk.BiomeStorage;
import net.minecraft.world.level.chunk.NibbleArray;
import net.minecraft.world.level.chunk.OldNibbleArray;

public class OldChunkLoader {
    private static final int DATALAYER_BITS = 7;
    private static final IWorldHeightAccess OLD_LEVEL_HEIGHT = new IWorldHeightAccess() {
        @Override
        public int getMinBuildHeight() {
            return 0;
        }

        @Override
        public int getHeight() {
            return 128;
        }
    };

    public static OldChunkLoader.OldChunk load(NBTTagCompound nbt) {
        int i = nbt.getInt("xPos");
        int j = nbt.getInt("zPos");
        OldChunkLoader.OldChunk oldLevelChunk = new OldChunkLoader.OldChunk(i, j);
        oldLevelChunk.blocks = nbt.getByteArray("Blocks");
        oldLevelChunk.data = new OldNibbleArray(nbt.getByteArray("Data"), 7);
        oldLevelChunk.skyLight = new OldNibbleArray(nbt.getByteArray("SkyLight"), 7);
        oldLevelChunk.blockLight = new OldNibbleArray(nbt.getByteArray("BlockLight"), 7);
        oldLevelChunk.heightmap = nbt.getByteArray("HeightMap");
        oldLevelChunk.terrainPopulated = nbt.getBoolean("TerrainPopulated");
        oldLevelChunk.entities = nbt.getList("Entities", 10);
        oldLevelChunk.blockEntities = nbt.getList("TileEntities", 10);
        oldLevelChunk.blockTicks = nbt.getList("TileTicks", 10);

        try {
            oldLevelChunk.lastUpdated = nbt.getLong("LastUpdate");
        } catch (ClassCastException var5) {
            oldLevelChunk.lastUpdated = (long)nbt.getInt("LastUpdate");
        }

        return oldLevelChunk;
    }

    public static void convertToAnvilFormat(IRegistryCustom.Dimension registryHolder, OldChunkLoader.OldChunk alphaChunk, NBTTagCompound nbt, WorldChunkManager biomeSource) {
        nbt.setInt("xPos", alphaChunk.x);
        nbt.setInt("zPos", alphaChunk.z);
        nbt.setLong("LastUpdate", alphaChunk.lastUpdated);
        int[] is = new int[alphaChunk.heightmap.length];

        for(int i = 0; i < alphaChunk.heightmap.length; ++i) {
            is[i] = alphaChunk.heightmap[i];
        }

        nbt.setIntArray("HeightMap", is);
        nbt.setBoolean("TerrainPopulated", alphaChunk.terrainPopulated);
        NBTTagList listTag = new NBTTagList();

        for(int j = 0; j < 8; ++j) {
            boolean bl = true;

            for(int k = 0; k < 16 && bl; ++k) {
                for(int l = 0; l < 16 && bl; ++l) {
                    for(int m = 0; m < 16; ++m) {
                        int n = k << 11 | m << 7 | l + (j << 4);
                        int o = alphaChunk.blocks[n];
                        if (o != 0) {
                            bl = false;
                            break;
                        }
                    }
                }
            }

            if (!bl) {
                byte[] bs = new byte[4096];
                NibbleArray dataLayer = new NibbleArray();
                NibbleArray dataLayer2 = new NibbleArray();
                NibbleArray dataLayer3 = new NibbleArray();

                for(int p = 0; p < 16; ++p) {
                    for(int q = 0; q < 16; ++q) {
                        for(int r = 0; r < 16; ++r) {
                            int s = p << 11 | r << 7 | q + (j << 4);
                            int t = alphaChunk.blocks[s];
                            bs[q << 8 | r << 4 | p] = (byte)(t & 255);
                            dataLayer.set(p, q, r, alphaChunk.data.get(p, q + (j << 4), r));
                            dataLayer2.set(p, q, r, alphaChunk.skyLight.get(p, q + (j << 4), r));
                            dataLayer3.set(p, q, r, alphaChunk.blockLight.get(p, q + (j << 4), r));
                        }
                    }
                }

                NBTTagCompound compoundTag = new NBTTagCompound();
                compoundTag.setByte("Y", (byte)(j & 255));
                compoundTag.setByteArray("Blocks", bs);
                compoundTag.setByteArray("Data", dataLayer.asBytes());
                compoundTag.setByteArray("SkyLight", dataLayer2.asBytes());
                compoundTag.setByteArray("BlockLight", dataLayer3.asBytes());
                listTag.add(compoundTag);
            }
        }

        nbt.set("Sections", listTag);
        nbt.setIntArray("Biomes", (new BiomeStorage(registryHolder.registryOrThrow(IRegistry.BIOME_REGISTRY), OLD_LEVEL_HEIGHT, new ChunkCoordIntPair(alphaChunk.x, alphaChunk.z), biomeSource)).writeBiomes());
        nbt.set("Entities", alphaChunk.entities);
        nbt.set("TileEntities", alphaChunk.blockEntities);
        if (alphaChunk.blockTicks != null) {
            nbt.set("TileTicks", alphaChunk.blockTicks);
        }

        nbt.setBoolean("convertedFromAlphaFormat", true);
    }

    public static class OldChunk {
        public long lastUpdated;
        public boolean terrainPopulated;
        public byte[] heightmap;
        public OldNibbleArray blockLight;
        public OldNibbleArray skyLight;
        public OldNibbleArray data;
        public byte[] blocks;
        public NBTTagList entities;
        public NBTTagList blockEntities;
        public NBTTagList blockTicks;
        public final int x;
        public final int z;

        public OldChunk(int x, int z) {
            this.x = x;
            this.z = z;
        }
    }
}
