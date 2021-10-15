package net.minecraft.world.level.storage;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.level.saveddata.PersistentBase;

public class PersistentCommandStorage {
    private static final String ID_PREFIX = "command_storage_";
    private final Map<String, PersistentCommandStorage.Container> namespaces = Maps.newHashMap();
    private final WorldPersistentData storage;

    public PersistentCommandStorage(WorldPersistentData stateManager) {
        this.storage = stateManager;
    }

    private PersistentCommandStorage.Container newStorage(String namespace) {
        PersistentCommandStorage.Container container = new PersistentCommandStorage.Container();
        this.namespaces.put(namespace, container);
        return container;
    }

    public NBTTagCompound get(MinecraftKey id) {
        String string = id.getNamespace();
        PersistentCommandStorage.Container container = this.storage.get((compoundTag) -> {
            return this.newStorage(string).load(compoundTag);
        }, createId(string));
        return container != null ? container.get(id.getKey()) : new NBTTagCompound();
    }

    public void set(MinecraftKey id, NBTTagCompound nbt) {
        String string = id.getNamespace();
        this.storage.computeIfAbsent((compoundTag) -> {
            return this.newStorage(string).load(compoundTag);
        }, () -> {
            return this.newStorage(string);
        }, createId(string)).put(id.getKey(), nbt);
    }

    public Stream<MinecraftKey> keys() {
        return this.namespaces.entrySet().stream().flatMap((entry) -> {
            return entry.getValue().getKeys(entry.getKey());
        });
    }

    private static String createId(String namespace) {
        return "command_storage_" + namespace;
    }

    static class Container extends PersistentBase {
        private static final String TAG_CONTENTS = "contents";
        private final Map<String, NBTTagCompound> storage = Maps.newHashMap();

        PersistentCommandStorage.Container load(NBTTagCompound nbt) {
            NBTTagCompound compoundTag = nbt.getCompound("contents");

            for(String string : compoundTag.getKeys()) {
                this.storage.put(string, compoundTag.getCompound(string));
            }

            return this;
        }

        @Override
        public NBTTagCompound save(NBTTagCompound nbt) {
            NBTTagCompound compoundTag = new NBTTagCompound();
            this.storage.forEach((string, compoundTag2) -> {
                compoundTag.set(string, compoundTag2.c());
            });
            nbt.set("contents", compoundTag);
            return nbt;
        }

        public NBTTagCompound get(String name) {
            NBTTagCompound compoundTag = this.storage.get(name);
            return compoundTag != null ? compoundTag : new NBTTagCompound();
        }

        public void put(String name, NBTTagCompound nbt) {
            if (nbt.isEmpty()) {
                this.storage.remove(name);
            } else {
                this.storage.put(name, nbt);
            }

            this.setDirty();
        }

        public Stream<MinecraftKey> getKeys(String namespace) {
            return this.storage.keySet().stream().map((string2) -> {
                return new MinecraftKey(namespace, string2);
            });
        }
    }
}
