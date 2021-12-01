package net.minecraft.world.level.chunk.storage;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.DataFixer;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.thread.ThreadedMailbox;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.entity.ChunkEntities;
import net.minecraft.world.level.entity.EntityPersistentStorage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EntityStorage implements EntityPersistentStorage<Entity> {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String ENTITIES_TAG = "Entities";
    private static final String POSITION_TAG = "Position";
    public final WorldServer level;
    private final IOWorker worker;
    private final LongSet emptyChunks = new LongOpenHashSet();
    public final ThreadedMailbox<Runnable> entityDeserializerQueue;
    protected final DataFixer fixerUpper;

    public EntityStorage(WorldServer world, Path path, DataFixer dataFixer, boolean dsync, Executor executor) {
        this.level = world;
        this.fixerUpper = dataFixer;
        this.entityDeserializerQueue = ThreadedMailbox.create(executor, "entity-deserializer");
        this.worker = new IOWorker(path, dsync, "entities");
    }

    @Override
    public CompletableFuture<ChunkEntities<Entity>> loadEntities(ChunkCoordIntPair pos) {
        return this.emptyChunks.contains(pos.pair()) ? CompletableFuture.completedFuture(emptyChunk(pos)) : this.worker.loadAsync(pos).thenApplyAsync((compound) -> {
            if (compound == null) {
                this.emptyChunks.add(pos.pair());
                return emptyChunk(pos);
            } else {
                try {
                    ChunkCoordIntPair chunkPos2 = readChunkPos(compound);
                    if (!Objects.equals(pos, chunkPos2)) {
                        LOGGER.error("Chunk file at {} is in the wrong location. (Expected {}, got {})", pos, pos, chunkPos2);
                    }
                } catch (Exception var6) {
                    LOGGER.warn("Failed to parse chunk {} position info", pos, var6);
                }

                NBTTagCompound compoundTag = this.upgradeChunkTag(compound);
                NBTTagList listTag = compoundTag.getList("Entities", 10);
                List<Entity> list = EntityTypes.loadEntitiesRecursive(listTag, this.level).collect(ImmutableList.toImmutableList());
                return new ChunkEntities<>(pos, list);
            }
        }, this.entityDeserializerQueue::tell);
    }

    private static ChunkCoordIntPair readChunkPos(NBTTagCompound chunkTag) {
        int[] is = chunkTag.getIntArray("Position");
        return new ChunkCoordIntPair(is[0], is[1]);
    }

    private static void writeChunkPos(NBTTagCompound chunkTag, ChunkCoordIntPair pos) {
        chunkTag.set("Position", new NBTTagIntArray(new int[]{pos.x, pos.z}));
    }

    private static ChunkEntities<Entity> emptyChunk(ChunkCoordIntPair pos) {
        return new ChunkEntities<>(pos, ImmutableList.of());
    }

    @Override
    public void storeEntities(ChunkEntities<Entity> dataList) {
        ChunkCoordIntPair chunkPos = dataList.getPos();
        if (dataList.isEmpty()) {
            if (this.emptyChunks.add(chunkPos.pair())) {
                this.worker.store(chunkPos, (NBTTagCompound)null);
            }

        } else {
            NBTTagList listTag = new NBTTagList();
            dataList.getEntities().forEach((entity) -> {
                NBTTagCompound compoundTag = new NBTTagCompound();
                if (entity.save(compoundTag)) {
                    listTag.add(compoundTag);
                }

            });
            NBTTagCompound compoundTag = new NBTTagCompound();
            compoundTag.setInt("DataVersion", SharedConstants.getCurrentVersion().getWorldVersion());
            compoundTag.set("Entities", listTag);
            writeChunkPos(compoundTag, chunkPos);
            this.worker.store(chunkPos, compoundTag).exceptionally((ex) -> {
                LOGGER.error("Failed to store chunk {}", chunkPos, ex);
                return null;
            });
            this.emptyChunks.remove(chunkPos.pair());
        }
    }

    @Override
    public void flush(boolean sync) {
        this.worker.synchronize(sync).join();
        this.entityDeserializerQueue.runAll();
    }

    private NBTTagCompound upgradeChunkTag(NBTTagCompound chunkTag) {
        int i = getVersion(chunkTag);
        return GameProfileSerializer.update(this.fixerUpper, DataFixTypes.ENTITY_CHUNK, chunkTag, i);
    }

    public static int getVersion(NBTTagCompound chunkTag) {
        return chunkTag.hasKeyOfType("DataVersion", 99) ? chunkTag.getInt("DataVersion") : -1;
    }

    @Override
    public void close() throws IOException {
        this.worker.close();
    }
}
