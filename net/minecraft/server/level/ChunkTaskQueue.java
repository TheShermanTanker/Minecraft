package net.minecraft.server.level;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.world.level.ChunkCoordIntPair;

public class ChunkTaskQueue<T> {
    public static final int PRIORITY_LEVEL_COUNT = PlayerChunkMap.MAX_CHUNK_DISTANCE + 2;
    private final List<Long2ObjectLinkedOpenHashMap<List<Optional<T>>>> taskQueue = IntStream.range(0, PRIORITY_LEVEL_COUNT).mapToObj((i) -> {
        return new Long2ObjectLinkedOpenHashMap();
    }).collect(Collectors.toList());
    private volatile int firstQueue = PRIORITY_LEVEL_COUNT;
    private final String name;
    private final LongSet acquired = new LongOpenHashSet();
    private final int maxTasks;

    public ChunkTaskQueue(String name, int maxSize) {
        this.name = name;
        this.maxTasks = maxSize;
    }

    protected void resortChunkTasks(int fromLevel, ChunkCoordIntPair pos, int toLevel) {
        if (fromLevel < PRIORITY_LEVEL_COUNT) {
            Long2ObjectLinkedOpenHashMap<List<Optional<T>>> long2ObjectLinkedOpenHashMap = this.taskQueue.get(fromLevel);
            List<Optional<T>> list = long2ObjectLinkedOpenHashMap.remove(pos.pair());
            if (fromLevel == this.firstQueue) {
                while(this.firstQueue < PRIORITY_LEVEL_COUNT && this.taskQueue.get(this.firstQueue).isEmpty()) {
                    ++this.firstQueue;
                }
            }

            if (list != null && !list.isEmpty()) {
                this.taskQueue.get(toLevel).computeIfAbsent(pos.pair(), (l) -> {
                    return Lists.newArrayList();
                }).addAll(list);
                this.firstQueue = Math.min(this.firstQueue, toLevel);
            }

        }
    }

    protected void submit(Optional<T> element, long pos, int level) {
        this.taskQueue.get(level).computeIfAbsent(pos, (l) -> {
            return Lists.newArrayList();
        }).add(element);
        this.firstQueue = Math.min(this.firstQueue, level);
    }

    protected void release(long pos, boolean removeElement) {
        for(Long2ObjectLinkedOpenHashMap<List<Optional<T>>> long2ObjectLinkedOpenHashMap : this.taskQueue) {
            List<Optional<T>> list = long2ObjectLinkedOpenHashMap.get(pos);
            if (list != null) {
                if (removeElement) {
                    list.clear();
                } else {
                    list.removeIf((optional) -> {
                        return !optional.isPresent();
                    });
                }

                if (list.isEmpty()) {
                    long2ObjectLinkedOpenHashMap.remove(pos);
                }
            }
        }

        while(this.firstQueue < PRIORITY_LEVEL_COUNT && this.taskQueue.get(this.firstQueue).isEmpty()) {
            ++this.firstQueue;
        }

        this.acquired.remove(pos);
    }

    private Runnable acquire(long pos) {
        return () -> {
            this.acquired.add(pos);
        };
    }

    @Nullable
    public Stream<Either<T, Runnable>> pop() {
        if (this.acquired.size() >= this.maxTasks) {
            return null;
        } else if (this.firstQueue >= PRIORITY_LEVEL_COUNT) {
            return null;
        } else {
            int i = this.firstQueue;
            Long2ObjectLinkedOpenHashMap<List<Optional<T>>> long2ObjectLinkedOpenHashMap = this.taskQueue.get(i);
            long l = long2ObjectLinkedOpenHashMap.firstLongKey();

            List<Optional<T>> list;
            for(list = long2ObjectLinkedOpenHashMap.removeFirst(); this.firstQueue < PRIORITY_LEVEL_COUNT && this.taskQueue.get(this.firstQueue).isEmpty(); ++this.firstQueue) {
            }

            return list.stream().map((optional) -> {
                return optional.map(Either::left).orElseGet(() -> {
                    return Either.right(this.acquire(l));
                });
            });
        }
    }

    @Override
    public String toString() {
        return this.name + " " + this.firstQueue + "...";
    }

    @VisibleForTesting
    LongSet getAcquired() {
        return new LongOpenHashSet(this.acquired);
    }
}
