package net.minecraft.world.level.storage;

import com.mojang.datafixers.DataFixer;
import java.io.File;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.NBTCompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.player.EntityHuman;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WorldNBTStorage {
    private static final Logger LOGGER = LogManager.getLogger();
    private final File playerDir;
    protected final DataFixer fixerUpper;

    public WorldNBTStorage(Convertable.ConversionSession session, DataFixer dataFixer) {
        this.fixerUpper = dataFixer;
        this.playerDir = session.getWorldFolder(SavedFile.PLAYER_DATA_DIR).toFile();
        this.playerDir.mkdirs();
    }

    public void save(EntityHuman player) {
        try {
            NBTTagCompound compoundTag = player.save(new NBTTagCompound());
            File file = File.createTempFile(player.getUniqueIDString() + "-", ".dat", this.playerDir);
            NBTCompressedStreamTools.writeCompressed(compoundTag, file);
            File file2 = new File(this.playerDir, player.getUniqueIDString() + ".dat");
            File file3 = new File(this.playerDir, player.getUniqueIDString() + ".dat_old");
            SystemUtils.safeReplaceFile(file2, file, file3);
        } catch (Exception var6) {
            LOGGER.warn("Failed to save player data for {}", (Object)player.getDisplayName().getString());
        }

    }

    @Nullable
    public NBTTagCompound load(EntityHuman player) {
        NBTTagCompound compoundTag = null;

        try {
            File file = new File(this.playerDir, player.getUniqueIDString() + ".dat");
            if (file.exists() && file.isFile()) {
                compoundTag = NBTCompressedStreamTools.readCompressed(file);
            }
        } catch (Exception var4) {
            LOGGER.warn("Failed to load player data for {}", (Object)player.getDisplayName().getString());
        }

        if (compoundTag != null) {
            int i = compoundTag.hasKeyOfType("DataVersion", 3) ? compoundTag.getInt("DataVersion") : -1;
            player.load(GameProfileSerializer.update(this.fixerUpper, DataFixTypes.PLAYER, compoundTag, i));
        }

        return compoundTag;
    }

    public String[] getSeenPlayers() {
        String[] strings = this.playerDir.list();
        if (strings == null) {
            strings = new String[0];
        }

        for(int i = 0; i < strings.length; ++i) {
            if (strings[i].endsWith(".dat")) {
                strings[i] = strings[i].substring(0, strings[i].length() - 4);
            }
        }

        return strings;
    }
}
