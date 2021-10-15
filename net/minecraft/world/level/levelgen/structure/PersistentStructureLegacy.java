package net.minecraft.world.level.levelgen.structure;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.World;
import net.minecraft.world.level.levelgen.feature.StructureGenerator;
import net.minecraft.world.level.storage.WorldPersistentData;

public class PersistentStructureLegacy {
    private static final Map<String, String> CURRENT_TO_LEGACY_MAP = SystemUtils.make(Maps.newHashMap(), (hashMap) -> {
        hashMap.put("Village", "Village");
        hashMap.put("Mineshaft", "Mineshaft");
        hashMap.put("Mansion", "Mansion");
        hashMap.put("Igloo", "Temple");
        hashMap.put("Desert_Pyramid", "Temple");
        hashMap.put("Jungle_Pyramid", "Temple");
        hashMap.put("Swamp_Hut", "Temple");
        hashMap.put("Stronghold", "Stronghold");
        hashMap.put("Monument", "Monument");
        hashMap.put("Fortress", "Fortress");
        hashMap.put("EndCity", "EndCity");
    });
    private static final Map<String, String> LEGACY_TO_CURRENT_MAP = SystemUtils.make(Maps.newHashMap(), (hashMap) -> {
        hashMap.put("Iglu", "Igloo");
        hashMap.put("TeDP", "Desert_Pyramid");
        hashMap.put("TeJP", "Jungle_Pyramid");
        hashMap.put("TeSH", "Swamp_Hut");
    });
    private final boolean hasLegacyData;
    private final Map<String, Long2ObjectMap<NBTTagCompound>> dataMap = Maps.newHashMap();
    private final Map<String, PersistentIndexed> indexMap = Maps.newHashMap();
    private final List<String> legacyKeys;
    private final List<String> currentKeys;

    public PersistentStructureLegacy(@Nullable WorldPersistentData dimensionDataStorage, List<String> list, List<String> list2) {
        this.legacyKeys = list;
        this.currentKeys = list2;
        this.populateCaches(dimensionDataStorage);
        boolean bl = false;

        for(String string : this.currentKeys) {
            bl |= this.dataMap.get(string) != null;
        }

        this.hasLegacyData = bl;
    }

    public void removeIndex(long l) {
        for(String string : this.legacyKeys) {
            PersistentIndexed structureFeatureIndexSavedData = this.indexMap.get(string);
            if (structureFeatureIndexSavedData != null && structureFeatureIndexSavedData.hasUnhandledIndex(l)) {
                structureFeatureIndexSavedData.removeIndex(l);
                structureFeatureIndexSavedData.setDirty();
            }
        }

    }

    public NBTTagCompound updateFromLegacy(NBTTagCompound nbt) {
        NBTTagCompound compoundTag = nbt.getCompound("Level");
        ChunkCoordIntPair chunkPos = new ChunkCoordIntPair(compoundTag.getInt("xPos"), compoundTag.getInt("zPos"));
        if (this.isUnhandledStructureStart(chunkPos.x, chunkPos.z)) {
            nbt = this.updateStructureStart(nbt, chunkPos);
        }

        NBTTagCompound compoundTag2 = compoundTag.getCompound("Structures");
        NBTTagCompound compoundTag3 = compoundTag2.getCompound("References");

        for(String string : this.currentKeys) {
            StructureGenerator<?> structureFeature = StructureGenerator.STRUCTURES_REGISTRY.get(string.toLowerCase(Locale.ROOT));
            if (!compoundTag3.hasKeyOfType(string, 12) && structureFeature != null) {
                int i = 8;
                LongList longList = new LongArrayList();

                for(int j = chunkPos.x - 8; j <= chunkPos.x + 8; ++j) {
                    for(int k = chunkPos.z - 8; k <= chunkPos.z + 8; ++k) {
                        if (this.hasLegacyStart(j, k, string)) {
                            longList.add(ChunkCoordIntPair.pair(j, k));
                        }
                    }
                }

                compoundTag3.putLongArray(string, (List<Long>)longList);
            }
        }

        compoundTag2.set("References", compoundTag3);
        compoundTag.set("Structures", compoundTag2);
        nbt.set("Level", compoundTag);
        return nbt;
    }

    private boolean hasLegacyStart(int chunkX, int chunkZ, String id) {
        if (!this.hasLegacyData) {
            return false;
        } else {
            return this.dataMap.get(id) != null && this.indexMap.get(CURRENT_TO_LEGACY_MAP.get(id)).hasStartIndex(ChunkCoordIntPair.pair(chunkX, chunkZ));
        }
    }

    private boolean isUnhandledStructureStart(int chunkX, int chunkZ) {
        if (!this.hasLegacyData) {
            return false;
        } else {
            for(String string : this.currentKeys) {
                if (this.dataMap.get(string) != null && this.indexMap.get(CURRENT_TO_LEGACY_MAP.get(string)).hasUnhandledIndex(ChunkCoordIntPair.pair(chunkX, chunkZ))) {
                    return true;
                }
            }

            return false;
        }
    }

    private NBTTagCompound updateStructureStart(NBTTagCompound nbt, ChunkCoordIntPair pos) {
        NBTTagCompound compoundTag = nbt.getCompound("Level");
        NBTTagCompound compoundTag2 = compoundTag.getCompound("Structures");
        NBTTagCompound compoundTag3 = compoundTag2.getCompound("Starts");

        for(String string : this.currentKeys) {
            Long2ObjectMap<NBTTagCompound> long2ObjectMap = this.dataMap.get(string);
            if (long2ObjectMap != null) {
                long l = pos.pair();
                if (this.indexMap.get(CURRENT_TO_LEGACY_MAP.get(string)).hasUnhandledIndex(l)) {
                    NBTTagCompound compoundTag4 = long2ObjectMap.get(l);
                    if (compoundTag4 != null) {
                        compoundTag3.set(string, compoundTag4);
                    }
                }
            }
        }

        compoundTag2.set("Starts", compoundTag3);
        compoundTag.set("Structures", compoundTag2);
        nbt.set("Level", compoundTag);
        return nbt;
    }

    private void populateCaches(@Nullable WorldPersistentData dimensionDataStorage) {
        if (dimensionDataStorage != null) {
            for(String string : this.legacyKeys) {
                NBTTagCompound compoundTag = new NBTTagCompound();

                try {
                    compoundTag = dimensionDataStorage.readTagFromDisk(string, 1493).getCompound("data").getCompound("Features");
                    if (compoundTag.isEmpty()) {
                        continue;
                    }
                } catch (IOException var13) {
                }

                for(String string2 : compoundTag.getKeys()) {
                    NBTTagCompound compoundTag2 = compoundTag.getCompound(string2);
                    long l = ChunkCoordIntPair.pair(compoundTag2.getInt("ChunkX"), compoundTag2.getInt("ChunkZ"));
                    NBTTagList listTag = compoundTag2.getList("Children", 10);
                    if (!listTag.isEmpty()) {
                        String string3 = listTag.getCompound(0).getString("id");
                        String string4 = LEGACY_TO_CURRENT_MAP.get(string3);
                        if (string4 != null) {
                            compoundTag2.setString("id", string4);
                        }
                    }

                    String string5 = compoundTag2.getString("id");
                    this.dataMap.computeIfAbsent(string5, (stringx) -> {
                        return new Long2ObjectOpenHashMap();
                    }).put(l, compoundTag2);
                }

                String string6 = string + "_index";
                PersistentIndexed structureFeatureIndexSavedData = dimensionDataStorage.computeIfAbsent(PersistentIndexed::load, PersistentIndexed::new, string6);
                if (!structureFeatureIndexSavedData.getAll().isEmpty()) {
                    this.indexMap.put(string, structureFeatureIndexSavedData);
                } else {
                    PersistentIndexed structureFeatureIndexSavedData2 = new PersistentIndexed();
                    this.indexMap.put(string, structureFeatureIndexSavedData2);

                    for(String string7 : compoundTag.getKeys()) {
                        NBTTagCompound compoundTag3 = compoundTag.getCompound(string7);
                        structureFeatureIndexSavedData2.addIndex(ChunkCoordIntPair.pair(compoundTag3.getInt("ChunkX"), compoundTag3.getInt("ChunkZ")));
                    }

                    structureFeatureIndexSavedData2.setDirty();
                }
            }

        }
    }

    public static PersistentStructureLegacy getLegacyStructureHandler(ResourceKey<World> world, @Nullable WorldPersistentData dimensionDataStorage) {
        if (world == World.OVERWORLD) {
            return new PersistentStructureLegacy(dimensionDataStorage, ImmutableList.of("Monument", "Stronghold", "Village", "Mineshaft", "Temple", "Mansion"), ImmutableList.of("Village", "Mineshaft", "Mansion", "Igloo", "Desert_Pyramid", "Jungle_Pyramid", "Swamp_Hut", "Stronghold", "Monument"));
        } else if (world == World.NETHER) {
            List<String> list = ImmutableList.of("Fortress");
            return new PersistentStructureLegacy(dimensionDataStorage, list, list);
        } else if (world == World.END) {
            List<String> list2 = ImmutableList.of("EndCity");
            return new PersistentStructureLegacy(dimensionDataStorage, list2, list2);
        } else {
            throw new RuntimeException(String.format("Unknown dimension type : %s", world));
        }
    }
}
