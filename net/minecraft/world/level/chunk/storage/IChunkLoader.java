package net.minecraft.world.level.chunk.storage;

import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Codec;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.World;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.PersistentStructureLegacy;
import net.minecraft.world.level.storage.WorldPersistentData;

public class IChunkLoader implements AutoCloseable {
    public static final int LAST_MONOLYTH_STRUCTURE_DATA_VERSION = 1493;
    private final IOWorker worker;
    protected final DataFixer fixerUpper;
    @Nullable
    private PersistentStructureLegacy legacyStructureHandler;

    public IChunkLoader(Path directory, DataFixer dataFixer, boolean dsync) {
        this.fixerUpper = dataFixer;
        this.worker = new IOWorker(directory, dsync, "chunk");
    }

    public NBTTagCompound upgradeChunkTag(ResourceKey<World> worldKey, Supplier<WorldPersistentData> persistentStateManagerFactory, NBTTagCompound nbt, Optional<ResourceKey<Codec<? extends ChunkGenerator>>> generatorCodecKey) {
        int i = getVersion(nbt);
        if (i < 1493) {
            nbt = GameProfileSerializer.update(this.fixerUpper, DataFixTypes.CHUNK, nbt, i, 1493);
            if (nbt.getCompound("Level").getBoolean("hasLegacyStructureData")) {
                if (this.legacyStructureHandler == null) {
                    this.legacyStructureHandler = PersistentStructureLegacy.getLegacyStructureHandler(worldKey, persistentStateManagerFactory.get());
                }

                nbt = this.legacyStructureHandler.updateFromLegacy(nbt);
            }
        }

        injectDatafixingContext(nbt, worldKey, generatorCodecKey);
        nbt = GameProfileSerializer.update(this.fixerUpper, DataFixTypes.CHUNK, nbt, Math.max(1493, i));
        if (i < SharedConstants.getCurrentVersion().getWorldVersion()) {
            nbt.setInt("DataVersion", SharedConstants.getCurrentVersion().getWorldVersion());
        }

        nbt.remove("__context");
        return nbt;
    }

    public static void injectDatafixingContext(NBTTagCompound nbt, ResourceKey<World> worldKey, Optional<ResourceKey<Codec<? extends ChunkGenerator>>> generatorCodecKey) {
        NBTTagCompound compoundTag = new NBTTagCompound();
        compoundTag.setString("dimension", worldKey.location().toString());
        generatorCodecKey.ifPresent((key) -> {
            compoundTag.setString("generator", key.location().toString());
        });
        nbt.set("__context", compoundTag);
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

    public ChunkScanAccess chunkScanner() {
        return this.worker;
    }
}
