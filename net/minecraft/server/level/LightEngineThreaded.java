package net.minecraft.server.level;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntSupplier;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.SectionPosition;
import net.minecraft.util.thread.Mailbox;
import net.minecraft.util.thread.ThreadedMailbox;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.EnumSkyBlock;
import net.minecraft.world.level.chunk.ChunkSection;
import net.minecraft.world.level.chunk.IChunkAccess;
import net.minecraft.world.level.chunk.ILightAccess;
import net.minecraft.world.level.chunk.NibbleArray;
import net.minecraft.world.level.lighting.LightEngine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LightEngineThreaded extends LightEngine implements AutoCloseable {
    private static final Logger LOGGER = LogManager.getLogger();
    private final ThreadedMailbox<Runnable> taskMailbox;
    private final ObjectList<Pair<LightEngineThreaded.Update, Runnable>> lightTasks = new ObjectArrayList<>();
    private final PlayerChunkMap chunkMap;
    private final Mailbox<ChunkTaskQueueSorter.Message<Runnable>> sorterMailbox;
    private volatile int taskPerBatch = 5;
    private final AtomicBoolean scheduled = new AtomicBoolean();

    public LightEngineThreaded(ILightAccess chunkProvider, PlayerChunkMap chunkStorage, boolean hasBlockLight, ThreadedMailbox<Runnable> processor, Mailbox<ChunkTaskQueueSorter.Message<Runnable>> executor) {
        super(chunkProvider, true, hasBlockLight);
        this.chunkMap = chunkStorage;
        this.sorterMailbox = executor;
        this.taskMailbox = processor;
    }

    @Override
    public void close() {
    }

    @Override
    public int runUpdates(int i, boolean doSkylight, boolean skipEdgeLightPropagation) {
        throw (UnsupportedOperationException)SystemUtils.pauseInIde(new UnsupportedOperationException("Ran automatically on a different thread!"));
    }

    @Override
    public void onBlockEmissionIncrease(BlockPosition pos, int level) {
        throw (UnsupportedOperationException)SystemUtils.pauseInIde(new UnsupportedOperationException("Ran automatically on a different thread!"));
    }

    @Override
    public void checkBlock(BlockPosition pos) {
        BlockPosition blockPos = pos.immutableCopy();
        this.addTask(SectionPosition.blockToSectionCoord(pos.getX()), SectionPosition.blockToSectionCoord(pos.getZ()), LightEngineThreaded.Update.POST_UPDATE, SystemUtils.name(() -> {
            super.checkBlock(blockPos);
        }, () -> {
            return "checkBlock " + blockPos;
        }));
    }

    protected void updateChunkStatus(ChunkCoordIntPair pos) {
        this.addTask(pos.x, pos.z, () -> {
            return 0;
        }, LightEngineThreaded.Update.PRE_UPDATE, SystemUtils.name(() -> {
            super.retainData(pos, false);
            super.enableLightSources(pos, false);

            for(int i = this.getMinLightSection(); i < this.getMaxLightSection(); ++i) {
                super.queueSectionData(EnumSkyBlock.BLOCK, SectionPosition.of(pos, i), (NibbleArray)null, true);
                super.queueSectionData(EnumSkyBlock.SKY, SectionPosition.of(pos, i), (NibbleArray)null, true);
            }

            for(int j = this.levelHeightAccessor.getMinSection(); j < this.levelHeightAccessor.getMaxSection(); ++j) {
                super.updateSectionStatus(SectionPosition.of(pos, j), true);
            }

        }, () -> {
            return "updateChunkStatus " + pos + " true";
        }));
    }

    @Override
    public void updateSectionStatus(SectionPosition pos, boolean notReady) {
        this.addTask(pos.x(), pos.z(), () -> {
            return 0;
        }, LightEngineThreaded.Update.PRE_UPDATE, SystemUtils.name(() -> {
            super.updateSectionStatus(pos, notReady);
        }, () -> {
            return "updateSectionStatus " + pos + " " + notReady;
        }));
    }

    @Override
    public void enableLightSources(ChunkCoordIntPair pos, boolean retainData) {
        this.addTask(pos.x, pos.z, LightEngineThreaded.Update.PRE_UPDATE, SystemUtils.name(() -> {
            super.enableLightSources(pos, retainData);
        }, () -> {
            return "enableLight " + pos + " " + retainData;
        }));
    }

    @Override
    public void queueSectionData(EnumSkyBlock lightType, SectionPosition pos, @Nullable NibbleArray nibbles, boolean nonEdge) {
        this.addTask(pos.x(), pos.z(), () -> {
            return 0;
        }, LightEngineThreaded.Update.PRE_UPDATE, SystemUtils.name(() -> {
            super.queueSectionData(lightType, pos, nibbles, nonEdge);
        }, () -> {
            return "queueData " + pos;
        }));
    }

    private void addTask(int x, int z, LightEngineThreaded.Update stage, Runnable task) {
        this.addTask(x, z, this.chunkMap.getChunkQueueLevel(ChunkCoordIntPair.pair(x, z)), stage, task);
    }

    private void addTask(int x, int z, IntSupplier completedLevelSupplier, LightEngineThreaded.Update stage, Runnable task) {
        this.sorterMailbox.tell(ChunkTaskQueueSorter.message(() -> {
            this.lightTasks.add(Pair.of(stage, task));
            if (this.lightTasks.size() >= this.taskPerBatch) {
                this.runUpdate();
            }

        }, ChunkCoordIntPair.pair(x, z), completedLevelSupplier));
    }

    @Override
    public void retainData(ChunkCoordIntPair pos, boolean retainData) {
        this.addTask(pos.x, pos.z, () -> {
            return 0;
        }, LightEngineThreaded.Update.PRE_UPDATE, SystemUtils.name(() -> {
            super.retainData(pos, retainData);
        }, () -> {
            return "retainData " + pos;
        }));
    }

    public CompletableFuture<IChunkAccess> lightChunk(IChunkAccess chunk, boolean excludeBlocks) {
        ChunkCoordIntPair chunkPos = chunk.getPos();
        chunk.setLightCorrect(false);
        this.addTask(chunkPos.x, chunkPos.z, LightEngineThreaded.Update.PRE_UPDATE, SystemUtils.name(() -> {
            ChunkSection[] levelChunkSections = chunk.getSections();

            for(int i = 0; i < chunk.getSectionsCount(); ++i) {
                ChunkSection levelChunkSection = levelChunkSections[i];
                if (!levelChunkSection.hasOnlyAir()) {
                    int j = this.levelHeightAccessor.getSectionYFromSectionIndex(i);
                    super.updateSectionStatus(SectionPosition.of(chunkPos, j), false);
                }
            }

            super.enableLightSources(chunkPos, true);
            if (!excludeBlocks) {
                chunk.getLights().forEach((pos) -> {
                    super.onBlockEmissionIncrease(pos, chunk.getLightEmission(pos));
                });
            }

        }, () -> {
            return "lightChunk " + chunkPos + " " + excludeBlocks;
        }));
        return CompletableFuture.supplyAsync(() -> {
            chunk.setLightCorrect(true);
            super.retainData(chunkPos, false);
            this.chunkMap.releaseLightTicket(chunkPos);
            return chunk;
        }, (runnable) -> {
            this.addTask(chunkPos.x, chunkPos.z, LightEngineThreaded.Update.POST_UPDATE, runnable);
        });
    }

    public void queueUpdate() {
        if ((!this.lightTasks.isEmpty() || super.hasLightWork()) && this.scheduled.compareAndSet(false, true)) {
            this.taskMailbox.tell(() -> {
                this.runUpdate();
                this.scheduled.set(false);
            });
        }

    }

    private void runUpdate() {
        int i = Math.min(this.lightTasks.size(), this.taskPerBatch);
        ObjectListIterator<Pair<LightEngineThreaded.Update, Runnable>> objectListIterator = this.lightTasks.iterator();

        int j;
        for(j = 0; objectListIterator.hasNext() && j < i; ++j) {
            Pair<LightEngineThreaded.Update, Runnable> pair = objectListIterator.next();
            if (pair.getFirst() == LightEngineThreaded.Update.PRE_UPDATE) {
                pair.getSecond().run();
            }
        }

        objectListIterator.back(j);
        super.runUpdates(Integer.MAX_VALUE, true, true);

        for(int var5 = 0; objectListIterator.hasNext() && var5 < i; ++var5) {
            Pair<LightEngineThreaded.Update, Runnable> pair2 = objectListIterator.next();
            if (pair2.getFirst() == LightEngineThreaded.Update.POST_UPDATE) {
                pair2.getSecond().run();
            }

            objectListIterator.remove();
        }

    }

    public void setTaskPerBatch(int taskBatchSize) {
        this.taskPerBatch = taskBatchSize;
    }

    static enum Update {
        PRE_UPDATE,
        POST_UPDATE;
    }
}
