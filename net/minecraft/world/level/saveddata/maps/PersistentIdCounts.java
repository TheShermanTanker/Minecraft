package net.minecraft.world.level.saveddata.maps;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.level.saveddata.PersistentBase;

public class PersistentIdCounts extends PersistentBase {
    public static final String FILE_NAME = "idcounts";
    private final Object2IntMap<String> usedAuxIds = new Object2IntOpenHashMap<>();

    public PersistentIdCounts() {
        this.usedAuxIds.defaultReturnValue(-1);
    }

    public static PersistentIdCounts load(NBTTagCompound nbt) {
        PersistentIdCounts mapIndex = new PersistentIdCounts();

        for(String string : nbt.getKeys()) {
            if (nbt.hasKeyOfType(string, 99)) {
                mapIndex.usedAuxIds.put(string, nbt.getInt(string));
            }
        }

        return mapIndex;
    }

    @Override
    public NBTTagCompound save(NBTTagCompound nbt) {
        for(Entry<String> entry : this.usedAuxIds.object2IntEntrySet()) {
            nbt.setInt(entry.getKey(), entry.getIntValue());
        }

        return nbt;
    }

    public int getFreeAuxValueForMap() {
        int i = this.usedAuxIds.getInt("map") + 1;
        this.usedAuxIds.put("map", i);
        this.setDirty();
        return i;
    }
}
