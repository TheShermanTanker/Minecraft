package net.minecraft.world.level.chunk.storage;

import com.mojang.datafixers.DataFixer;
import java.io.File;
import java.io.IOException;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.World;
import net.minecraft.world.level.levelgen.structure.PersistentStructureLegacy;
import net.minecraft.world.level.storage.WorldPersistentData;

public class IChunkLoader implements AutoCloseable {
    private final IOWorker worker;
    protected final DataFixer fixerUpper;
    @Nullable
    private PersistentStructureLegacy legacyStructureHandler;

    public IChunkLoader(File directory, DataFixer dataFixer, boolean dsync) {
        this.fixerUpper = dataFixer;
        this.worker = new IOWorker(directory, dsync, "chunk");
    }

    public NBTTagCompound getChunkData(ResourceKey<World> worldKey, Supplier<WorldPersistentData> persistentStateManagerFactory, NBTTagCompound nbt) {
        int i = getVersion(nbt);
        int j = 1493;
        if (i < 1493) {
            nbt = GameProfileSerializer.update(this.fixerUpper, DataFixTypes.CHUNK, nbt, i, 1493);
            if (nbt.getCompound("Level").getBoolean("hasLegacyStructureData")) {
                if (this.legacyStructureHandler == null) {
                    this.legacyStructureHandler = PersistentStructureLegacy.getLegacyStructureHandler(worldKey, persistentStateManagerFactory.get());
                }

                nbt = this.legacyStructureHandler.updateFromLegacy(nbt);
            }
        }

        nbt = GameProfileSerializer.update(this.fixerUpper, DataFixTypes.CHUNK, nbt, Math.max(1493, i));
        if (i < SharedConstants.getGameVersion().getWorldVersion()) {
            nbt.setInt("DataVersion", SharedConstants.getGameVersion().getWorldVersion());
        }

        return nbt;
    }

    public static int getVersion(NBTTagCompound nbt) {
        return nbt.hasKeyOfType("DataVersion", 99) ? nbt.getInt("DataVersion") : -1;
    }

    @Nullable
    public NBTTagCompound read(ChunkCoordIntPair chunkPos) throws IOException {
        return this.worker.load(chunkPos);
    }

    public void write(ChunkCoordIntPair chunkPos, NBTTagCompound nbt) {
        this.worker.store(chunkPos, nbt);
        if (this.legacyStructureHandler != null) {
            this.legacyStructureHandler.removeIndex(chunkPos.pair());
        }

    }

    public void flushWorker() {
        this.worker.synchronize(true).join();
    }

    @Override
    public void close() throws IOException {
        this.worker.close();
    }
}
