package net.minecraft.server.level;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Either;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.SystemUtils;
import net.minecraft.util.Unit;
import net.minecraft.util.thread.Mailbox;
import net.minecraft.util.thread.PairedQueue;
import net.minecraft.util.thread.ThreadedMailbox;
import net.minecraft.world.level.ChunkCoordIntPair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ChunkTaskQueueSorter implements PlayerChunk.LevelChangeListener, AutoCloseable {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Map<Mailbox<?>, ChunkTaskQueue<? extends Function<Mailbox<Unit>, ?>>> queues;
    private final Set<Mailbox<?>> sleeping;
    private final ThreadedMailbox<PairedQueue.IntRunnable> mailbox;

    public ChunkTaskQueueSorter(List<Mailbox<?>> actors, Executor executor, int maxQueues) {
        this.queues = actors.stream().collect(Collectors.toMap(Function.identity(), (actor) -> {
            return new ChunkTaskQueue<>(actor.name() + "_queue", maxQueues);
        }));
        this.sleeping = Sets.newHashSet(actors);
        this.mailbox = new ThreadedMailbox<>(new PairedQueue.FixedPriorityQueue(4), executor, "sorter");
    }

    public static <T> ChunkTaskQueueSorter.Message<T> message(Function<Mailbox<Unit>, T> taskFunction, long pos, IntSupplier lastLevelUpdatedToProvider) {
        return new ChunkTaskQueueSorter.Message<>(taskFunction, pos, lastLevelUpdatedToProvider);
    }

    public static ChunkTaskQueueSorter.Message<Runnable> message(Runnable task, long pos, IntSupplier lastLevelUpdatedToProvider) {
        return new ChunkTaskQueueSorter.Message<>((yield) -> {
            return () -> {
                task.run();
                yield.tell(Unit.INSTANCE);
            };
        }, pos, lastLevelUpdatedToProvider);
    }

    public static ChunkTaskQueueSorter.Message<Runnable> message(PlayerChunk holder, Runnable task) {
        return message(task, holder.getPos().pair(), holder::getQueueLevel);
    }

    public static <T> ChunkTaskQueueSorter.Message<T> message(PlayerChunk holder, Function<Mailbox<Unit>, T> taskFunction) {
        return message(taskFunction, holder.getPos().pair(), holder::getQueueLevel);
    }

    public static ChunkTaskQueueSorter.Release release(Runnable task, long pos, boolean removeTask) {
        return new ChunkTaskQueueSorter.Release(task, pos, removeTask);
    }

    public <T> Mailbox<ChunkTaskQueueSorter.Message<T>> getProcessor(Mailbox<T> executor, boolean addBlocker) {
        return this.mailbox.ask((yield) -> {
            return new PairedQueue.IntRunnable(0, () -> {
                this.getQueue(executor);
                yield.tell(Mailbox.of("chunk priority sorter around " + executor.name(), (message) -> {
                    this.submit(executor, message.task, message.pos, message.level, addBlocker);
                }));
            });
        }).join();
    }

    public Mailbox<ChunkTaskQueueSorter.Release> getReleaseProcessor(Mailbox<Runnable> executor) {
        return this.mailbox.ask((yield) -> {
            return new PairedQueue.IntRunnable(0, () -> {
                yield.tell(Mailbox.of("chunk priority sorter around " + executor.name(), (release) -> {
                    this.release(executor, release.pos, release.task, release.clearQueue);
                }));
            });
        }).join();
    }

    @Override
    public void onLevelChange(ChunkCoordIntPair pos, IntSupplier levelGetter, int targetLevel, IntConsumer levelSetter) {
        this.mailbox.tell(new PairedQueue.IntRunnable(0, () -> {
            int j = levelGetter.getAsInt();
            this.queues.values().forEach((queue) -> {
                queue.resortChunkTasks(j, pos, targetLevel);
            });
            levelSetter.accept(targetLevel);
        }));
    }

    private <T> void release(Mailbox<T> actor, long chunkPos, Runnable callback, boolean clearTask) {
        this.mailbox.tell(new PairedQueue.IntRunnable(1, () -> {
            ChunkTaskQueue<Function<Mailbox<Unit>, T>> chunkTaskPriorityQueue = this.getQueue(actor);
            chunkTaskPriorityQueue.release(chunkPos, clearTask);
            if (this.sleeping.remove(actor)) {
                this.pollTask(chunkTaskPriorityQueue, actor);
            }

            callback.run();
        }));
    }

    private <T> void submit(Mailbox<T> actor, Function<Mailbox<Unit>, T> task, long chunkPos, IntSupplier lastLevelUpdatedToProvider, boolean addBlocker) {
        this.mailbox.tell(new PairedQueue.IntRunnable(2, () -> {
            ChunkTaskQueue<Function<Mailbox<Unit>, T>> chunkTaskPriorityQueue = this.getQueue(actor);
            int i = lastLevelUpdatedToProvider.getAsInt();
            chunkTaskPriorityQueue.submit(Optional.of(task), chunkPos, i);
            if (addBlocker) {
                chunkTaskPriorityQueue.submit(Optional.empty(), chunkPos, i);
            }

            if (this.sleeping.remove(actor)) {
                this.pollTask(chunkTaskPriorityQueue, actor);
            }

        }));
    }

    private <T> void pollTask(ChunkTaskQueue<Function<Mailbox<Unit>, T>> queue, Mailbox<T> actor) {
        this.mailbox.tell(new PairedQueue.IntRunnable(3, () -> {
            Stream<Either<Function<Mailbox<Unit>, T>, Runnable>> stream = queue.pop();
            if (stream == null) {
                this.sleeping.add(actor);
            } else {
                SystemUtils.sequence(stream.map((executeOrAddBlocking) -> {
                    return executeOrAddBlocking.map(actor::ask, (addBlocking) -> {
                        addBlocking.run();
                        return CompletableFuture.completedFuture(Unit.INSTANCE);
                    });
                }).collect(Collectors.toList())).thenAccept((list) -> {
                    this.pollTask(queue, actor);
                });
            }

        }));
    }

    private <T> ChunkTaskQueue<Function<Mailbox<Unit>, T>> getQueue(Mailbox<T> actor) {
        ChunkTaskQueue<? extends Function<Mailbox<Unit>, ?>> chunkTaskPriorityQueue = this.queues.get(actor);
        if (chunkTaskPriorityQueue == null) {
            throw (IllegalArgumentException)SystemUtils.pauseInIde(new IllegalArgumentException("No queue for: " + actor));
        } else {
            return chunkTaskPriorityQueue;
        }
    }

    @VisibleForTesting
    public String getDebugStatus() {
        return (String)this.queues.entrySet().stream().map((entry) -> {
            return entry.getKey().name() + "=[" + (String)entry.getValue().getAcquired().stream().map((long_) -> {
                return long_ + ":" + new ChunkCoordIntPair(long_);
            }).collect(Collectors.joining(",")) + "]";
        }).collect(Collectors.joining(",")) + ", s=" + this.sleeping.size();
    }

    @Override
    public void close() {
        this.queues.keySet().forEach(Mailbox::close);
    }

    public static final class Message<T> {
        final Function<Mailbox<Unit>, T> task;
        final long pos;
        final IntSupplier level;

        Message(Function<Mailbox<Unit>, T> taskFunction, long pos, IntSupplier lastLevelUpdatedToProvider) {
            this.task = taskFunction;
            this.pos = pos;
            this.level = lastLevelUpdatedToProvider;
        }
    }

    public static final class Release {
        final Runnable task;
        final long pos;
        final boolean clearQueue;

        Release(Runnable callback, long pos, boolean removeTask) {
            this.task = callback;
            this.pos = pos;
            this.clearQueue = removeTask;
        }
    }
}
