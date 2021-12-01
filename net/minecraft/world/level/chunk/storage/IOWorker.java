package net.minecraft.world.level.chunk.storage;

import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Either;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.StreamTagVisitor;
import net.minecraft.util.Unit;
import net.minecraft.util.thread.PairedQueue;
import net.minecraft.util.thread.ThreadedMailbox;
import net.minecraft.world.level.ChunkCoordIntPair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class IOWorker implements ChunkScanAccess, AutoCloseable {
    private static final Logger LOGGER = LogManager.getLogger();
    private final AtomicBoolean shutdownRequested = new AtomicBoolean();
    private final ThreadedMailbox<PairedQueue.IntRunnable> mailbox;
    private final RegionFileCache storage;
    private final Map<ChunkCoordIntPair, IOWorker.PendingStore> pendingWrites = Maps.newLinkedHashMap();

    protected IOWorker(Path directory, boolean dsync, String name) {
        this.storage = new RegionFileCache(directory, dsync);
        this.mailbox = new ThreadedMailbox<>(new PairedQueue.FixedPriorityQueue(IOWorker.Priority.values().length), SystemUtils.ioPool(), "IOWorker-" + name);
    }

    public CompletableFuture<Void> store(ChunkCoordIntPair pos, @Nullable NBTTagCompound nbt) {
        return this.submitTask(() -> {
            IOWorker.PendingStore pendingStore = this.pendingWrites.computeIfAbsent(pos, (chunkPos) -> {
                return new IOWorker.PendingStore(nbt);
            });
            pendingStore.data = nbt;
            return Either.left(pendingStore.result);
        }).thenCompose(Function.identity());
    }

    @Nullable
    public NBTTagCompound load(ChunkCoordIntPair pos) throws IOException {
        CompletableFuture<NBTTagCompound> completableFuture = this.loadAsync(pos);

        try {
            return completableFuture.join();
        } catch (CompletionException var4) {
            if (var4.getCause() instanceof IOException) {
                throw (IOException)var4.getCause();
            } else {
                throw var4;
            }
        }
    }

    protected CompletableFuture<NBTTagCompound> loadAsync(ChunkCoordIntPair pos) {
        return this.submitTask(() -> {
            IOWorker.PendingStore pendingStore = this.pendingWrites.get(pos);
            if (pendingStore != null) {
                return Either.left(pendingStore.data);
            } else {
                try {
                    NBTTagCompound compoundTag = this.storage.read(pos);
                    return Either.left(compoundTag);
                } catch (Exception var4) {
                    LOGGER.warn("Failed to read chunk {}", pos, var4);
                    return Either.right(var4);
                }
            }
        });
    }

    public CompletableFuture<Void> synchronize(boolean sync) {
        CompletableFuture<Void> completableFuture = this.submitTask(() -> {
            return Either.left(CompletableFuture.allOf(this.pendingWrites.values().stream().map((pendingStore) -> {
                return pendingStore.result;
            }).toArray((i) -> {
                return new CompletableFuture[i];
            })));
        }).thenCompose(Function.identity());
        return sync ? completableFuture.thenCompose((void_) -> {
            return this.submitTask(() -> {
                try {
                    this.storage.flush();
                    return Either.left((Void)null);
                } catch (Exception var2) {
                    LOGGER.warn("Failed to synchronize chunks", (Throwable)var2);
                    return Either.right(var2);
                }
            });
        }) : completableFuture.thenCompose((void_) -> {
            return this.submitTask(() -> {
                return Either.left((Void)null);
            });
        });
    }

    @Override
    public CompletableFuture<Void> scanChunk(ChunkCoordIntPair pos, StreamTagVisitor scanner) {
        return this.submitTask(() -> {
            try {
                IOWorker.PendingStore pendingStore = this.pendingWrites.get(pos);
                if (pendingStore != null) {
                    if (pendingStore.data != null) {
                        pendingStore.data.acceptAsRoot(scanner);
                    }
                } else {
                    this.storage.scanChunk(pos, scanner);
                }

                return Either.left((Void)null);
            } catch (Exception var4) {
                LOGGER.warn("Failed to bulk scan chunk {}", pos, var4);
                return Either.right(var4);
            }
        });
    }

    private <T> CompletableFuture<T> submitTask(Supplier<Either<T, Exception>> task) {
        return this.mailbox.askEither((processorHandle) -> {
            return new PairedQueue.IntRunnable(IOWorker.Priority.FOREGROUND.ordinal(), () -> {
                if (!this.shutdownRequested.get()) {
                    processorHandle.tell(task.get());
                }

                this.tellStorePending();
            });
        });
    }

    private void storePendingChunk() {
        if (!this.pendingWrites.isEmpty()) {
            Iterator<Entry<ChunkCoordIntPair, IOWorker.PendingStore>> iterator = this.pendingWrites.entrySet().iterator();
            Entry<ChunkCoordIntPair, IOWorker.PendingStore> entry = iterator.next();
            iterator.remove();
            this.runStore(entry.getKey(), entry.getValue());
            this.tellStorePending();
        }
    }

    private void tellStorePending() {
        this.mailbox.tell(new PairedQueue.IntRunnable(IOWorker.Priority.BACKGROUND.ordinal(), this::storePendingChunk));
    }

    private void runStore(ChunkCoordIntPair pos, IOWorker.PendingStore result) {
        try {
            this.storage.write(pos, result.data);
            result.result.complete((Void)null);
        } catch (Exception var4) {
            LOGGER.error("Failed to store chunk {}", pos, var4);
            result.result.completeExceptionally(var4);
        }

    }

    @Override
    public void close() throws IOException {
        if (this.shutdownRequested.compareAndSet(false, true)) {
            this.mailbox.ask((processorHandle) -> {
                return new PairedQueue.IntRunnable(IOWorker.Priority.SHUTDOWN.ordinal(), () -> {
                    processorHandle.tell(Unit.INSTANCE);
                });
            }).join();
            this.mailbox.close();

            try {
                this.storage.close();
            } catch (Exception var2) {
                LOGGER.error("Failed to close storage", (Throwable)var2);
            }

        }
    }

    static class PendingStore {
        @Nullable
        NBTTagCompound data;
        final CompletableFuture<Void> result = new CompletableFuture<>();

        public PendingStore(@Nullable NBTTagCompound nbt) {
            this.data = nbt;
        }
    }

    static enum Priority {
        FOREGROUND,
        BACKGROUND,
        SHUTDOWN;
    }
}
