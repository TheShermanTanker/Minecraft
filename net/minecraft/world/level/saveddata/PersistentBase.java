package net.minecraft.world.level.saveddata;

import java.io.File;
import java.io.IOException;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.NBTCompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class PersistentBase {
    private static final Logger LOGGER = LogManager.getLogger();
    private boolean dirty;

    public abstract NBTTagCompound save(NBTTagCompound nbt);

    public void setDirty() {
        this.setDirty(true);
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public boolean isDirty() {
        return this.dirty;
    }

    public void save(File file) {
        if (this.isDirty()) {
            NBTTagCompound compoundTag = new NBTTagCompound();
            compoundTag.set("data", this.save(new NBTTagCompound()));
            compoundTag.setInt("DataVersion", SharedConstants.getGameVersion().getWorldVersion());

            try {
                NBTCompressedStreamTools.writeCompressed(compoundTag, file);
            } catch (IOException var4) {
                LOGGER.error("Could not save data {}", this, var4);
            }

            this.setDirty(false);
        }
    }
}
