package net.minecraft.world.level.storage;

import com.google.common.collect.Maps;
import com.mojang.datafixers.DataFixer;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PushbackInputStream;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.NBTCompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.PersistentBase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WorldPersistentData {
    private static final Logger LOGGER = LogManager.getLogger();
    public final Map<String, PersistentBase> cache = Maps.newHashMap();
    private final DataFixer fixerUpper;
    private final File dataFolder;

    public WorldPersistentData(File directory, DataFixer dataFixer) {
        this.fixerUpper = dataFixer;
        this.dataFolder = directory;
    }

    private File getDataFile(String id) {
        return new File(this.dataFolder, id + ".dat");
    }

    public <T extends PersistentBase> T computeIfAbsent(Function<NBTTagCompound, T> function, Supplier<T> supplier, String string) {
        T savedData = this.get(function, string);
        if (savedData != null) {
            return savedData;
        } else {
            T savedData2 = supplier.get();
            this.set(string, savedData2);
            return savedData2;
        }
    }

    @Nullable
    public <T extends PersistentBase> T get(Function<NBTTagCompound, T> function, String id) {
        PersistentBase savedData = this.cache.get(id);
        if (savedData == null && !this.cache.containsKey(id)) {
            savedData = this.readSavedData(function, id);
            this.cache.put(id, savedData);
        }

        return (T)savedData;
    }

    @Nullable
    private <T extends PersistentBase> T readSavedData(Function<NBTTagCompound, T> function, String id) {
        try {
            File file = this.getDataFile(id);
            if (file.exists()) {
                NBTTagCompound compoundTag = this.readTagFromDisk(id, SharedConstants.getGameVersion().getWorldVersion());
                return function.apply(compoundTag.getCompound("data"));
            }
        } catch (Exception var5) {
            LOGGER.error("Error loading saved data: {}", id, var5);
        }

        return (T)null;
    }

    public void set(String string, PersistentBase savedData) {
        this.cache.put(string, savedData);
    }

    public NBTTagCompound readTagFromDisk(String id, int dataVersion) throws IOException {
        File file = this.getDataFile(id);
        FileInputStream fileInputStream = new FileInputStream(file);

        NBTTagCompound var8;
        try {
            PushbackInputStream pushbackInputStream = new PushbackInputStream(fileInputStream, 2);

            try {
                NBTTagCompound compoundTag;
                if (this.isGzip(pushbackInputStream)) {
                    compoundTag = NBTCompressedStreamTools.readCompressed(pushbackInputStream);
                } else {
                    DataInputStream dataInputStream = new DataInputStream(pushbackInputStream);

                    try {
                        compoundTag = NBTCompressedStreamTools.read(dataInputStream);
                    } catch (Throwable var13) {
                        try {
                            dataInputStream.close();
                        } catch (Throwable var12) {
                            var13.addSuppressed(var12);
                        }

                        throw var13;
                    }

                    dataInputStream.close();
                }

                int i = compoundTag.hasKeyOfType("DataVersion", 99) ? compoundTag.getInt("DataVersion") : 1343;
                var8 = GameProfileSerializer.update(this.fixerUpper, DataFixTypes.SAVED_DATA, compoundTag, i, dataVersion);
            } catch (Throwable var14) {
                try {
                    pushbackInputStream.close();
                } catch (Throwable var11) {
                    var14.addSuppressed(var11);
                }

                throw var14;
            }

            pushbackInputStream.close();
        } catch (Throwable var15) {
            try {
                fileInputStream.close();
            } catch (Throwable var10) {
                var15.addSuppressed(var10);
            }

            throw var15;
        }

        fileInputStream.close();
        return var8;
    }

    private boolean isGzip(PushbackInputStream pushbackInputStream) throws IOException {
        byte[] bs = new byte[2];
        boolean bl = false;
        int i = pushbackInputStream.read(bs, 0, 2);
        if (i == 2) {
            int j = (bs[1] & 255) << 8 | bs[0] & 255;
            if (j == 35615) {
                bl = true;
            }
        }

        if (i != 0) {
            pushbackInputStream.unread(bs, 0, i);
        }

        return bl;
    }

    public void save() {
        this.cache.forEach((string, savedData) -> {
            if (savedData != null) {
                savedData.save(this.getDataFile(string));
            }

        });
    }
}
