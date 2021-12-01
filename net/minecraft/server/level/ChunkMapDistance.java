package net.minecraft.server.level;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntMaps;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;
import net.minecraft.core.SectionPosition;
import net.minecraft.util.ArraySetSorted;
import net.minecraft.util.thread.Mailbox;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.chunk.Chunk;
import net.minecraft.world.level.chunk.ChunkStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class ChunkMapDistance {
    static final Logger LOGGER = LogManager.getLogger();
    private static final int ENTITY_TICKING_RANGE = 2;
    static final int PLAYER_TICKET_LEVEL = 33 + ChunkStatus.getDistance(ChunkStatus.FULL) - 2;
    private static final int INITIAL_TICKET_LIST_CAPACITY = 4;
    private static final int ENTITY_TICKING_LEVEL_THRESHOLD = 32;
    private static final int BLOCK_TICKING_LEVEL_THRESHOLD = 33;
    final Long2ObjectMap<ObjectSet<EntityPlayer>> playersPerChunk = new Long2ObjectOpenHashMap<>();
    public final Long2ObjectOpenHashMap<ArraySetSorted<Ticket<?>>> tickets = new Long2ObjectOpenHashMap<>();
    private final ChunkMapDistance.ChunkTicketTracker ticketTracker = new ChunkMapDistance.ChunkTicketTracker();
    private final ChunkMapDistance.FixedPlayerDistanceChunkTracker naturalSpawnChunkCounter = new ChunkMapDistance.FixedPlayerDistanceChunkTracker(8);
    private final TickingTracker tickingTicketsTracker = new TickingTracker();
    private final ChunkMapDistance.PlayerTicketTracker playerTicketManager = new ChunkMapDistance.PlayerTicketTracker(33);
    final Set<PlayerChunk> chunksToUpdateFutures = Sets.newHashSet();
    final ChunkTaskQueueSorter ticketThrottler;
    final Mailbox<ChunkTaskQueueSorter.Message<Runnable>> ticketThrottlerInput;
    final Mailbox<ChunkTaskQueueSorter.Release> ticketThrottlerReleaser;
    final LongSet ticketsToRelease = new LongOpenHashSet();
    final Executor mainThreadExecutor;
    private long ticketTickCounter;
    private int simulationDistance = 10;

    protected ChunkMapDistance(Executor workerExecutor, Executor mainThreadExecutor) {
        Mailbox<Runnable> processorHandle = Mailbox.of("player ticket throttler", mainThreadExecutor::execute);
        ChunkTaskQueueSorter chunkTaskPriorityQueueSorter = new ChunkTaskQueueSorter(ImmutableList.of(processorHandle), workerExecutor, 4);
        this.ticketThrottler = chunkTaskPriorityQueueSorter;
        this.ticketThrottlerInput = chunkTaskPriorityQueueSorter.getProcessor(processorHandle, true);
        this.ticketThrottlerReleaser = chunkTaskPriorityQueueSorter.getReleaseProcessor(processorHandle);
        this.mainThreadExecutor = mainThreadExecutor;
    }

    protected void purgeTickets() {
        ++this.ticketTickCounter;
        ObjectIterator<Entry<ArraySetSorted<Ticket<?>>>> objectIterator = this.tickets.long2ObjectEntrySet().fastIterator();

        while(objectIterator.hasNext()) {
            Entry<ArraySetSorted<Ticket<?>>> entry = objectIterator.next();
            Iterator<Ticket<?>> iterator = entry.getValue().iterator();
            boolean bl = false;

            while(iterator.hasNext()) {
                Ticket<?> ticket = iterator.next();
                if (ticket.timedOut(this.ticketTickCounter)) {
                    iterator.remove();
                    bl = true;
                    this.tickingTicketsTracker.removeTicket(entry.getLongKey(), ticket);
                }
            }

            if (bl) {
                this.ticketTracker.update(entry.getLongKey(), getLowestTicketLevel(entry.getValue()), false);
            }

            if (entry.getValue().isEmpty()) {
                objectIterator.remove();
            }
        }

    }

    private static int getLowestTicketLevel(ArraySetSorted<Ticket<?>> tickets) {
        return !tickets.isEmpty() ? tickets.first().getTicketLevel() : PlayerChunkMap.MAX_CHUNK_DISTANCE + 1;
    }

    protected abstract boolean isChunkToRemove(long pos);

    @Nullable
    protected abstract PlayerChunk getChunk(long pos);

    @Nullable
    protected abstract PlayerChunk updateChunkScheduling(long pos, int level, @Nullable PlayerChunk holder, int i);

    public boolean runAllUpdates(PlayerChunkMap chunkStorage) {
        this.naturalSpawnChunkCounter.runAllUpdates();
        this.tickingTicketsTracker.runAllUpdates();
        this.playerTicketManager.runAllUpdates();
        int i = Integer.MAX_VALUE - this.ticketTracker.runDistanceUpdates(Integer.MAX_VALUE);
        boolean bl = i != 0;
        if (bl) {
        }

        if (!this.chunksToUpdateFutures.isEmpty()) {
            this.chunksToUpdateFutures.forEach((holder) -> {
                holder.updateFutures(chunkStorage, this.mainThreadExecutor);
            });
            this.chunksToUpdateFutures.clear();
            return true;
        } else {
            if (!this.ticketsToRelease.isEmpty()) {
                LongIterator longIterator = this.ticketsToRelease.iterator();

                while(longIterator.hasNext()) {
                    long l = longIterator.nextLong();
                    if (this.getTickets(l).stream().anyMatch((ticket) -> {
                        return ticket.getTicketType() == TicketType.PLAYER;
                    })) {
                        PlayerChunk chunkHolder = chunkStorage.getUpdatingChunk(l);
                        if (chunkHolder == null) {
                            throw new IllegalStateException();
                        }

                        CompletableFuture<Either<Chunk, PlayerChunk.Failure>> completableFuture = chunkHolder.getEntityTickingChunkFuture();
                        completableFuture.thenAccept((either) -> {
                            this.mainThreadExecutor.execute(() -> {
                                this.ticketThrottlerReleaser.tell(ChunkTaskQueueSorter.release(() -> {
                                }, l, false));
                            });
                        });
                    }
                }

                this.ticketsToRelease.clear();
            }

            return bl;
        }
    }

    void addTicket(long position, Ticket<?> ticket) {
        ArraySetSorted<Ticket<?>> sortedArraySet = this.getTickets(position);
        int i = getLowestTicketLevel(sortedArraySet);
        Ticket<?> ticket2 = sortedArraySet.addOrGet(ticket);
        ticket2.setCreatedTick(this.ticketTickCounter);
        if (ticket.getTicketLevel() < i) {
            this.ticketTracker.update(position, ticket.getTicketLevel(), true);
        }

    }

    void removeTicket(long pos, Ticket<?> ticket) {
        ArraySetSorted<Ticket<?>> sortedArraySet = this.getTickets(pos);
        if (sortedArraySet.remove(ticket)) {
        }

        if (sortedArraySet.isEmpty()) {
            this.tickets.remove(pos);
        }

        this.ticketTracker.update(pos, getLowestTicketLevel(sortedArraySet), false);
    }

    public <T> void addTicket(TicketType<T> type, ChunkCoordIntPair pos, int level, T argument) {
        this.addTicket(pos.pair(), new Ticket<>(type, level, argument));
    }

    public <T> void removeTicket(TicketType<T> type, ChunkCoordIntPair pos, int level, T argument) {
        Ticket<T> ticket = new Ticket<>(type, level, argument);
        this.removeTicket(pos.pair(), ticket);
    }

    public <T> void addRegionTicket(TicketType<T> type, ChunkCoordIntPair pos, int radius, T argument) {
        Ticket<T> ticket = new Ticket<>(type, 33 - radius, argument);
        long l = pos.pair();
        this.addTicket(l, ticket);
        this.tickingTicketsTracker.addTicket(l, ticket);
    }

    public <T> void removeRegionTicket(TicketType<T> type, ChunkCoordIntPair pos, int radius, T argument) {
        Ticket<T> ticket = new Ticket<>(type, 33 - radius, argument);
        long l = pos.pair();
        this.removeTicket(l, ticket);
        this.tickingTicketsTracker.removeTicket(l, ticket);
    }

    private ArraySetSorted<Ticket<?>> getTickets(long position) {
        return this.tickets.computeIfAbsent(position, (l) -> {
            return ArraySetSorted.create(4);
        });
    }

    protected void updateChunkForced(ChunkCoordIntPair pos, boolean forced) {
        Ticket<ChunkCoordIntPair> ticket = new Ticket<>(TicketType.FORCED, 31, pos);
        long l = pos.pair();
        if (forced) {
            this.addTicket(l, ticket);
            this.tickingTicketsTracker.addTicket(l, ticket);
        } else {
            this.removeTicket(l, ticket);
            this.tickingTicketsTracker.removeTicket(l, ticket);
        }

    }

    public void addPlayer(SectionPosition pos, EntityPlayer player) {
        ChunkCoordIntPair chunkPos = pos.chunk();
        long l = chunkPos.pair();
        this.playersPerChunk.computeIfAbsent(l, (lx) -> {
            return new ObjectOpenHashSet();
        }).add(player);
        this.naturalSpawnChunkCounter.update(l, 0, true);
        this.playerTicketManager.update(l, 0, true);
        this.tickingTicketsTracker.addTicket(TicketType.PLAYER, chunkPos, this.getPlayerTicketLevel(), chunkPos);
    }

    public void removePlayer(SectionPosition pos, EntityPlayer player) {
        ChunkCoordIntPair chunkPos = pos.chunk();
        long l = chunkPos.pair();
        ObjectSet<EntityPlayer> objectSet = this.playersPerChunk.get(l);
        objectSet.remove(player);
        if (objectSet.isEmpty()) {
            this.playersPerChunk.remove(l);
            this.naturalSpawnChunkCounter.update(l, Integer.MAX_VALUE, false);
            this.playerTicketManager.update(l, Integer.MAX_VALUE, false);
            this.tickingTicketsTracker.removeTicket(TicketType.PLAYER, chunkPos, this.getPlayerTicketLevel(), chunkPos);
        }

    }

    private int getPlayerTicketLevel() {
        return Math.max(0, 31 - this.simulationDistance);
    }

    public boolean inEntityTickingRange(long chunkPos) {
        return this.tickingTicketsTracker.getLevel(chunkPos) < 32;
    }

    public boolean inBlockTickingRange(long chunkPos) {
        return this.tickingTicketsTracker.getLevel(chunkPos) < 33;
    }

    protected String getTicketDebugString(long pos) {
        ArraySetSorted<Ticket<?>> sortedArraySet = this.tickets.get(pos);
        return sortedArraySet != null && !sortedArraySet.isEmpty() ? sortedArraySet.first().toString() : "no_ticket";
    }

    protected void updatePlayerTickets(int viewDistance) {
        this.playerTicketManager.updateViewDistance(viewDistance);
    }

    public void updateSimulationDistance(int simulationDistance) {
        if (simulationDistance != this.simulationDistance) {
            this.simulationDistance = simulationDistance;
            this.tickingTicketsTracker.replacePlayerTicketsLevel(this.getPlayerTicketLevel());
        }

    }

    public int getNaturalSpawnChunkCount() {
        this.naturalSpawnChunkCounter.runAllUpdates();
        return this.naturalSpawnChunkCounter.chunks.size();
    }

    public boolean hasPlayersNearby(long chunkPos) {
        this.naturalSpawnChunkCounter.runAllUpdates();
        return this.naturalSpawnChunkCounter.chunks.containsKey(chunkPos);
    }

    public String getDebugStatus() {
        return this.ticketThrottler.getDebugStatus();
    }

    private void dumpTickets(String path) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(new File(path));

            try {
                for(Entry<ArraySetSorted<Ticket<?>>> entry : this.tickets.long2ObjectEntrySet()) {
                    ChunkCoordIntPair chunkPos = new ChunkCoordIntPair(entry.getLongKey());

                    for(Ticket<?> ticket : entry.getValue()) {
                        fileOutputStream.write((chunkPos.x + "\t" + chunkPos.z + "\t" + ticket.getTicketType() + "\t" + ticket.getTicketLevel() + "\t\n").getBytes(StandardCharsets.UTF_8));
                    }
                }
            } catch (Throwable var9) {
                try {
                    fileOutputStream.close();
                } catch (Throwable var8) {
                    var9.addSuppressed(var8);
                }

                throw var9;
            }

            fileOutputStream.close();
        } catch (IOException var10) {
            LOGGER.error(var10);
        }

    }

    @VisibleForTesting
    TickingTracker tickingTracker() {
        return this.tickingTicketsTracker;
    }

    class ChunkTicketTracker extends ChunkMap {
        public ChunkTicketTracker() {
            super(PlayerChunkMap.MAX_CHUNK_DISTANCE + 2, 16, 256);
        }

        @Override
        protected int getLevelFromSource(long id) {
            ArraySetSorted<Ticket<?>> sortedArraySet = ChunkMapDistance.this.tickets.get(id);
            if (sortedArraySet == null) {
                return Integer.MAX_VALUE;
            } else {
                return sortedArraySet.isEmpty() ? Integer.MAX_VALUE : sortedArraySet.first().getTicketLevel();
            }
        }

        @Override
        protected int getLevel(long id) {
            if (!ChunkMapDistance.this.isChunkToRemove(id)) {
                PlayerChunk chunkHolder = ChunkMapDistance.this.getChunk(id);
                if (chunkHolder != null) {
                    return chunkHolder.getTicketLevel();
                }
            }

            return PlayerChunkMap.MAX_CHUNK_DISTANCE + 1;
        }

        @Override
        protected void setLevel(long id, int level) {
            PlayerChunk chunkHolder = ChunkMapDistance.this.getChunk(id);
            int i = chunkHolder == null ? PlayerChunkMap.MAX_CHUNK_DISTANCE + 1 : chunkHolder.getTicketLevel();
            if (i != level) {
                chunkHolder = ChunkMapDistance.this.updateChunkScheduling(id, level, chunkHolder, i);
                if (chunkHolder != null) {
                    ChunkMapDistance.this.chunksToUpdateFutures.add(chunkHolder);
                }

            }
        }

        public int runDistanceUpdates(int distance) {
            return this.runUpdates(distance);
        }
    }

    class FixedPlayerDistanceChunkTracker extends ChunkMap {
        protected final Long2ByteMap chunks = new Long2ByteOpenHashMap();
        protected final int maxDistance;

        protected FixedPlayerDistanceChunkTracker(int maxDistance) {
            super(maxDistance + 2, 16, 256);
            this.maxDistance = maxDistance;
            this.chunks.defaultReturnValue((byte)(maxDistance + 2));
        }

        @Override
        protected int getLevel(long id) {
            return this.chunks.get(id);
        }

        @Override
        protected void setLevel(long id, int level) {
            byte b;
            if (level > this.maxDistance) {
                b = this.chunks.remove(id);
            } else {
                b = this.chunks.put(id, (byte)level);
            }

            this.onLevelChange(id, b, level);
        }

        protected void onLevelChange(long pos, int oldDistance, int distance) {
        }

        @Override
        protected int getLevelFromSource(long id) {
            return this.havePlayer(id) ? 0 : Integer.MAX_VALUE;
        }

        private boolean havePlayer(long chunkPos) {
            ObjectSet<EntityPlayer> objectSet = ChunkMapDistance.this.playersPerChunk.get(chunkPos);
            return objectSet != null && !objectSet.isEmpty();
        }

        public void runAllUpdates() {
            this.runUpdates(Integer.MAX_VALUE);
        }

        private void dumpChunks(String path) {
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(new File(path));

                try {
                    for(it.unimi.dsi.fastutil.longs.Long2ByteMap.Entry entry : this.chunks.long2ByteEntrySet()) {
                        ChunkCoordIntPair chunkPos = new ChunkCoordIntPair(entry.getLongKey());
                        String string = Byte.toString(entry.getByteValue());
                        fileOutputStream.write((chunkPos.x + "\t" + chunkPos.z + "\t" + string + "\n").getBytes(StandardCharsets.UTF_8));
                    }
                } catch (Throwable var8) {
                    try {
                        fileOutputStream.close();
                    } catch (Throwable var7) {
                        var8.addSuppressed(var7);
                    }

                    throw var8;
                }

                fileOutputStream.close();
            } catch (IOException var9) {
                ChunkMapDistance.LOGGER.error(var9);
            }

        }
    }

    class PlayerTicketTracker extends ChunkMapDistance.FixedPlayerDistanceChunkTracker {
        private int viewDistance;
        private final Long2IntMap queueLevels = Long2IntMaps.synchronize(new Long2IntOpenHashMap());
        private final LongSet toUpdate = new LongOpenHashSet();

        protected PlayerTicketTracker(int i) {
            super(i);
            this.viewDistance = 0;
            this.queueLevels.defaultReturnValue(i + 2);
        }

        @Override
        protected void onLevelChange(long pos, int oldDistance, int distance) {
            this.toUpdate.add(pos);
        }

        public void updateViewDistance(int watchDistance) {
            for(it.unimi.dsi.fastutil.longs.Long2ByteMap.Entry entry : this.chunks.long2ByteEntrySet()) {
                byte b = entry.getByteValue();
                long l = entry.getLongKey();
                this.onLevelChange(l, b, this.haveTicketFor(b), b <= watchDistance - 2);
            }

            this.viewDistance = watchDistance;
        }

        private void onLevelChange(long pos, int distance, boolean oldWithinViewDistance, boolean withinViewDistance) {
            if (oldWithinViewDistance != withinViewDistance) {
                Ticket<?> ticket = new Ticket<>(TicketType.PLAYER, ChunkMapDistance.PLAYER_TICKET_LEVEL, new ChunkCoordIntPair(pos));
                if (withinViewDistance) {
                    ChunkMapDistance.this.ticketThrottlerInput.tell(ChunkTaskQueueSorter.message(() -> {
                        ChunkMapDistance.this.mainThreadExecutor.execute(() -> {
                            if (this.haveTicketFor(this.getLevel(pos))) {
                                ChunkMapDistance.this.addTicket(pos, ticket);
                                ChunkMapDistance.this.ticketsToRelease.add(pos);
                            } else {
                                ChunkMapDistance.this.ticketThrottlerReleaser.tell(ChunkTaskQueueSorter.release(() -> {
                                }, pos, false));
                            }

                        });
                    }, pos, () -> {
                        return distance;
                    }));
                } else {
                    ChunkMapDistance.this.ticketThrottlerReleaser.tell(ChunkTaskQueueSorter.release(() -> {
                        ChunkMapDistance.this.mainThreadExecutor.execute(() -> {
                            ChunkMapDistance.this.removeTicket(pos, ticket);
                        });
                    }, pos, true));
                }
            }

        }

        @Override
        public void runAllUpdates() {
            super.runAllUpdates();
            if (!this.toUpdate.isEmpty()) {
                LongIterator longIterator = this.toUpdate.iterator();

                while(longIterator.hasNext()) {
                    long l = longIterator.nextLong();
                    int i = this.queueLevels.get(l);
                    int j = this.getLevel(l);
                    if (i != j) {
                        ChunkMapDistance.this.ticketThrottler.onLevelChange(new ChunkCoordIntPair(l), () -> {
                            return this.queueLevels.get(l);
                        }, j, (ix) -> {
                            if (ix >= this.queueLevels.defaultReturnValue()) {
                                this.queueLevels.remove(l);
                            } else {
                                this.queueLevels.put(l, ix);
                            }

                        });
                        this.onLevelChange(l, j, this.haveTicketFor(i), this.haveTicketFor(j));
                    }
                }

                this.toUpdate.clear();
            }

        }

        private boolean haveTicketFor(int distance) {
            return distance <= this.viewDistance - 2;
        }
    }
}
