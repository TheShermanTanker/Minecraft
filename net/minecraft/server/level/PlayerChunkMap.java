package net.minecraft.server.level;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableList.Builder;
import com.google.gson.JsonElement;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportSystemDetails;
import net.minecraft.ReportedException;
import net.minecraft.SystemUtils;
import net.minecraft.core.IRegistry;
import net.minecraft.core.SectionPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.PacketDebug;
import net.minecraft.network.protocol.game.PacketPlayOutAttachEntity;
import net.minecraft.network.protocol.game.PacketPlayOutMount;
import net.minecraft.network.protocol.game.PacketPlayOutViewCentre;
import net.minecraft.server.level.progress.WorldLoadListener;
import net.minecraft.server.network.PlayerConnectionServer;
import net.minecraft.util.CSVWriter;
import net.minecraft.util.MathHelper;
import net.minecraft.util.profiling.GameProfilerFiller;
import net.minecraft.util.thread.IAsyncTaskHandler;
import net.minecraft.util.thread.Mailbox;
import net.minecraft.util.thread.ThreadedMailbox;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.village.poi.VillagePlace;
import net.minecraft.world.entity.boss.EntityComplexPart;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.chunk.Chunk;
import net.minecraft.world.level.chunk.ChunkConverter;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.IChunkAccess;
import net.minecraft.world.level.chunk.ILightAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.ProtoChunkExtension;
import net.minecraft.world.level.chunk.storage.ChunkRegionLoader;
import net.minecraft.world.level.chunk.storage.IChunkLoader;
import net.minecraft.world.level.entity.ChunkStatusUpdateListener;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureManager;
import net.minecraft.world.level.storage.Convertable;
import net.minecraft.world.level.storage.WorldPersistentData;
import net.minecraft.world.phys.Vec3D;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PlayerChunkMap extends IChunkLoader implements PlayerChunk.PlayerProvider {
    private static final byte CHUNK_TYPE_REPLACEABLE = -1;
    private static final byte CHUNK_TYPE_UNKNOWN = 0;
    private static final byte CHUNK_TYPE_FULL = 1;
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int CHUNK_SAVED_PER_TICK = 200;
    private static final int CHUNK_SAVED_EAGERLY_PER_TICK = 20;
    private static final int MIN_VIEW_DISTANCE = 3;
    public static final int MAX_VIEW_DISTANCE = 33;
    public static final int MAX_CHUNK_DISTANCE = 33 + ChunkStatus.maxDistance();
    public static final int FORCED_TICKET_LEVEL = 31;
    public final Long2ObjectLinkedOpenHashMap<PlayerChunk> updatingChunkMap = new Long2ObjectLinkedOpenHashMap<>();
    public volatile Long2ObjectLinkedOpenHashMap<PlayerChunk> visibleChunkMap = this.updatingChunkMap.clone();
    private final Long2ObjectLinkedOpenHashMap<PlayerChunk> pendingUnloads = new Long2ObjectLinkedOpenHashMap<>();
    public final LongSet entitiesInLevel = new LongOpenHashSet();
    public final WorldServer level;
    private final LightEngineThreaded lightEngine;
    private final IAsyncTaskHandler<Runnable> mainThreadExecutor;
    public ChunkGenerator generator;
    public final Supplier<WorldPersistentData> overworldDataStorage;
    private final VillagePlace poiManager;
    public final LongSet toDrop = new LongOpenHashSet();
    private boolean modified;
    private final ChunkTaskQueueSorter queueSorter;
    private final Mailbox<ChunkTaskQueueSorter.Message<Runnable>> worldgenMailbox;
    public final Mailbox<ChunkTaskQueueSorter.Message<Runnable>> mainThreadMailbox;
    public final WorldLoadListener progressListener;
    private final ChunkStatusUpdateListener chunkStatusListener;
    public final PlayerChunkMap.DistanceManager distanceManager;
    private final AtomicInteger tickingGenerated = new AtomicInteger();
    public final DefinedStructureManager structureManager;
    private final String storageName;
    private final PlayerMap playerMap = new PlayerMap();
    public final Int2ObjectMap<PlayerChunkMap.EntityTracker> entityMap = new Int2ObjectOpenHashMap<>();
    private final Long2ByteMap chunkTypeCache = new Long2ByteOpenHashMap();
    private final Queue<Runnable> unloadQueue = Queues.newConcurrentLinkedQueue();
    int viewDistance;

    public PlayerChunkMap(WorldServer world, Convertable.ConversionSession session, DataFixer dataFixer, DefinedStructureManager structureManager, Executor executor, IAsyncTaskHandler<Runnable> mainThreadExecutor, ILightAccess chunkProvider, ChunkGenerator chunkGenerator, WorldLoadListener worldGenerationProgressListener, ChunkStatusUpdateListener chunkStatusChangeListener, Supplier<WorldPersistentData> persistentStateManagerFactory, int viewDistance, boolean dsync) {
        super(session.getDimensionPath(world.getDimensionKey()).resolve("region"), dataFixer, dsync);
        this.structureManager = structureManager;
        Path path = session.getDimensionPath(world.getDimensionKey());
        this.storageName = path.getFileName().toString();
        this.level = world;
        this.generator = chunkGenerator;
        this.mainThreadExecutor = mainThreadExecutor;
        ThreadedMailbox<Runnable> processorMailbox = ThreadedMailbox.create(executor, "worldgen");
        Mailbox<Runnable> processorHandle = Mailbox.of("main", mainThreadExecutor::tell);
        this.progressListener = worldGenerationProgressListener;
        this.chunkStatusListener = chunkStatusChangeListener;
        ThreadedMailbox<Runnable> processorMailbox2 = ThreadedMailbox.create(executor, "light");
        this.queueSorter = new ChunkTaskQueueSorter(ImmutableList.of(processorMailbox, processorHandle, processorMailbox2), executor, Integer.MAX_VALUE);
        this.worldgenMailbox = this.queueSorter.getProcessor(processorMailbox, false);
        this.mainThreadMailbox = this.queueSorter.getProcessor(processorHandle, false);
        this.lightEngine = new LightEngineThreaded(chunkProvider, this, this.level.getDimensionManager().hasSkyLight(), processorMailbox2, this.queueSorter.getProcessor(processorMailbox2, false));
        this.distanceManager = new PlayerChunkMap.DistanceManager(executor, mainThreadExecutor);
        this.overworldDataStorage = persistentStateManagerFactory;
        this.poiManager = new VillagePlace(path.resolve("poi"), dataFixer, dsync, world);
        this.setViewDistance(viewDistance);
    }

    protected ChunkGenerator generator() {
        return this.generator;
    }

    public void debugReloadGenerator() {
        DataResult<JsonElement> dataResult = ChunkGenerator.CODEC.encodeStart(JsonOps.INSTANCE, this.generator);
        DataResult<ChunkGenerator> dataResult2 = dataResult.flatMap((jsonElement) -> {
            return ChunkGenerator.CODEC.parse(JsonOps.INSTANCE, jsonElement);
        });
        dataResult2.result().ifPresent((chunkGenerator) -> {
            this.generator = chunkGenerator;
        });
    }

    private static double euclideanDistanceSquared(ChunkCoordIntPair pos, Entity entity) {
        double d = (double)SectionPosition.sectionToBlockCoord(pos.x, 8);
        double e = (double)SectionPosition.sectionToBlockCoord(pos.z, 8);
        double f = d - entity.locX();
        double g = e - entity.locZ();
        return f * f + g * g;
    }

    private static boolean isChunkInEuclideanRange(ChunkCoordIntPair chunkPos, EntityPlayer player, boolean usePlayerWatchedSection, int distance) {
        int i;
        int j;
        if (usePlayerWatchedSection) {
            SectionPosition sectionPos = player.getLastSectionPos();
            i = sectionPos.x();
            j = sectionPos.z();
        } else {
            i = SectionPosition.blockToSectionCoord(player.getBlockX());
            j = SectionPosition.blockToSectionCoord(player.getBlockZ());
        }

        return isChunkInEuclideanRange(chunkPos, i, j, distance);
    }

    private static boolean isChunkInEuclideanRange(ChunkCoordIntPair chunkPos, int sectionX, int sectionZ, int distance) {
        int i = chunkPos.x;
        int j = chunkPos.z;
        return isChunkInEuclideanRange(i, j, sectionX, sectionZ, distance);
    }

    public static boolean isChunkInEuclideanRange(int x1, int z1, int x2, int z2, int distance) {
        int i = x1 - x2;
        int j = z1 - z2;
        int k = distance * distance + distance;
        int l = i * i + j * j;
        return l <= k;
    }

    private static boolean isChunkOnEuclideanBorder(ChunkCoordIntPair chunkPos, EntityPlayer player, boolean usePlayerWatchedSection, int distance) {
        int i;
        int j;
        if (usePlayerWatchedSection) {
            SectionPosition sectionPos = player.getLastSectionPos();
            i = sectionPos.x();
            j = sectionPos.z();
        } else {
            i = SectionPosition.blockToSectionCoord(player.getBlockX());
            j = SectionPosition.blockToSectionCoord(player.getBlockZ());
        }

        return isChunkOnEuclideanBorder(chunkPos.x, chunkPos.z, i, j, distance);
    }

    private static boolean isChunkOnEuclideanBorder(int x1, int z1, int x2, int z2, int distance) {
        if (!isChunkInEuclideanRange(x1, z1, x2, z2, distance)) {
            return false;
        } else if (!isChunkInEuclideanRange(x1 + 1, z1, x2, z2, distance)) {
            return true;
        } else if (!isChunkInEuclideanRange(x1, z1 + 1, x2, z2, distance)) {
            return true;
        } else if (!isChunkInEuclideanRange(x1 - 1, z1, x2, z2, distance)) {
            return true;
        } else {
            return !isChunkInEuclideanRange(x1, z1 - 1, x2, z2, distance);
        }
    }

    protected LightEngineThreaded getLightEngine() {
        return this.lightEngine;
    }

    @Nullable
    public PlayerChunk getUpdatingChunk(long pos) {
        return this.updatingChunkMap.get(pos);
    }

    @Nullable
    public PlayerChunk getVisibleChunk(long pos) {
        return this.visibleChunkMap.get(pos);
    }

    protected IntSupplier getChunkQueueLevel(long pos) {
        return () -> {
            PlayerChunk chunkHolder = this.getVisibleChunk(pos);
            return chunkHolder == null ? ChunkTaskQueue.PRIORITY_LEVEL_COUNT - 1 : Math.min(chunkHolder.getQueueLevel(), ChunkTaskQueue.PRIORITY_LEVEL_COUNT - 1);
        };
    }

    public String getChunkDebugData(ChunkCoordIntPair chunkPos) {
        PlayerChunk chunkHolder = this.getVisibleChunk(chunkPos.pair());
        if (chunkHolder == null) {
            return "null";
        } else {
            String string = chunkHolder.getTicketLevel() + "\n";
            ChunkStatus chunkStatus = chunkHolder.getLastAvailableStatus();
            IChunkAccess chunkAccess = chunkHolder.getLastAvailable();
            if (chunkStatus != null) {
                string = string + "St: \u00a7" + chunkStatus.getIndex() + chunkStatus + "\u00a7r\n";
            }

            if (chunkAccess != null) {
                string = string + "Ch: \u00a7" + chunkAccess.getChunkStatus().getIndex() + chunkAccess.getChunkStatus() + "\u00a7r\n";
            }

            PlayerChunk.State fullChunkStatus = chunkHolder.getFullStatus();
            string = string + "\u00a7" + fullChunkStatus.ordinal() + fullChunkStatus;
            return string + "\u00a7r";
        }
    }

    private CompletableFuture<Either<List<IChunkAccess>, PlayerChunk.Failure>> getChunkRangeFuture(ChunkCoordIntPair centerChunk, int margin, IntFunction<ChunkStatus> distanceToStatus) {
        List<CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>>> list = new ArrayList<>();
        List<PlayerChunk> list2 = new ArrayList<>();
        int i = centerChunk.x;
        int j = centerChunk.z;

        for(int k = -margin; k <= margin; ++k) {
            for(int l = -margin; l <= margin; ++l) {
                int m = Math.max(Math.abs(l), Math.abs(k));
                final ChunkCoordIntPair chunkPos = new ChunkCoordIntPair(i + l, j + k);
                long n = chunkPos.pair();
                PlayerChunk chunkHolder = this.getUpdatingChunk(n);
                if (chunkHolder == null) {
                    return CompletableFuture.completedFuture(Either.right(new PlayerChunk.Failure() {
                        @Override
                        public String toString() {
                            return "Unloaded " + chunkPos;
                        }
                    }));
                }

                ChunkStatus chunkStatus = distanceToStatus.apply(m);
                CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>> completableFuture = chunkHolder.getOrScheduleFuture(chunkStatus, this);
                list2.add(chunkHolder);
                list.add(completableFuture);
            }
        }

        CompletableFuture<List<Either<IChunkAccess, PlayerChunk.Failure>>> completableFuture2 = SystemUtils.sequence(list);
        CompletableFuture<Either<List<IChunkAccess>, PlayerChunk.Failure>> completableFuture3 = completableFuture2.thenApply((listx) -> {
            List<IChunkAccess> list2 = Lists.newArrayList();
            int l = 0;

            for(final Either<IChunkAccess, PlayerChunk.Failure> either : listx) {
                Optional<IChunkAccess> optional = either.left();
                if (!optional.isPresent()) {
                    final int m = l;
                    return Either.right(new PlayerChunk.Failure() {
                        @Override
                        public String toString() {
                            return "Unloaded " + new ChunkCoordIntPair(i + m % (margin * 2 + 1), j + m / (margin * 2 + 1)) + " " + either.right().get();
                        }
                    });
                }

                list2.add(optional.get());
                ++l;
            }

            return Either.left(list2);
        });

        for(PlayerChunk chunkHolder2 : list2) {
            chunkHolder2.addSaveDependency("getChunkRangeFuture " + centerChunk + " " + margin, completableFuture3);
        }

        return completableFuture3;
    }

    public CompletableFuture<Either<Chunk, PlayerChunk.Failure>> prepareEntityTickingChunk(ChunkCoordIntPair pos) {
        return this.getChunkRangeFuture(pos, 2, (i) -> {
            return ChunkStatus.FULL;
        }).thenApplyAsync((either) -> {
            return either.mapLeft((list) -> {
                return (Chunk)list.get(list.size() / 2);
            });
        }, this.mainThreadExecutor);
    }

    @Nullable
    PlayerChunk updateChunkScheduling(long pos, int level, @Nullable PlayerChunk holder, int i) {
        if (i > MAX_CHUNK_DISTANCE && level > MAX_CHUNK_DISTANCE) {
            return holder;
        } else {
            if (holder != null) {
                holder.setTicketLevel(level);
            }

            if (holder != null) {
                if (level > MAX_CHUNK_DISTANCE) {
                    this.toDrop.add(pos);
                } else {
                    this.toDrop.remove(pos);
                }
            }

            if (level <= MAX_CHUNK_DISTANCE && holder == null) {
                holder = this.pendingUnloads.remove(pos);
                if (holder != null) {
                    holder.setTicketLevel(level);
                } else {
                    holder = new PlayerChunk(new ChunkCoordIntPair(pos), level, this.level, this.lightEngine, this.queueSorter, this);
                }

                this.updatingChunkMap.put(pos, holder);
                this.modified = true;
            }

            return holder;
        }
    }

    @Override
    public void close() throws IOException {
        try {
            this.queueSorter.close();
            this.poiManager.close();
        } finally {
            super.close();
        }

    }

    protected void save(boolean flush) {
        if (flush) {
            List<PlayerChunk> list = this.visibleChunkMap.values().stream().filter(PlayerChunk::hasBeenLoaded).peek(PlayerChunk::refreshAccessibility).collect(Collectors.toList());
            MutableBoolean mutableBoolean = new MutableBoolean();

            do {
                mutableBoolean.setFalse();
                list.stream().map((chunkHolder) -> {
                    CompletableFuture<IChunkAccess> completableFuture;
                    do {
                        completableFuture = chunkHolder.getChunkSave();
                        this.mainThreadExecutor.awaitTasks(completableFuture::isDone);
                    } while(completableFuture != chunkHolder.getChunkSave());

                    return completableFuture.join();
                }).filter((chunk) -> {
                    return chunk instanceof ProtoChunkExtension || chunk instanceof Chunk;
                }).filter(this::saveChunk).forEach((chunkAccess) -> {
                    mutableBoolean.setTrue();
                });
            } while(mutableBoolean.isTrue());

            this.processUnloads(() -> {
                return true;
            });
            this.flushWorker();
        } else {
            this.visibleChunkMap.values().forEach(this::saveChunkIfNeeded);
        }

    }

    protected void unloadChunks(BooleanSupplier shouldKeepTicking) {
        GameProfilerFiller profilerFiller = this.level.getMethodProfiler();
        profilerFiller.enter("poi");
        this.poiManager.tick(shouldKeepTicking);
        profilerFiller.exitEnter("chunk_unload");
        if (!this.level.isSavingDisabled()) {
            this.processUnloads(shouldKeepTicking);
        }

        profilerFiller.exit();
    }

    private void processUnloads(BooleanSupplier shouldKeepTicking) {
        LongIterator longIterator = this.toDrop.iterator();

        for(int i = 0; longIterator.hasNext() && (shouldKeepTicking.getAsBoolean() || i < 200 || this.toDrop.size() > 2000); longIterator.remove()) {
            long l = longIterator.nextLong();
            PlayerChunk chunkHolder = this.updatingChunkMap.remove(l);
            if (chunkHolder != null) {
                this.pendingUnloads.put(l, chunkHolder);
                this.modified = true;
                ++i;
                this.scheduleUnload(l, chunkHolder);
            }
        }

        int j = Math.max(0, this.unloadQueue.size() - 2000);

        Runnable runnable;
        while((shouldKeepTicking.getAsBoolean() || j > 0) && (runnable = this.unloadQueue.poll()) != null) {
            --j;
            runnable.run();
        }

        int k = 0;
        ObjectIterator<PlayerChunk> objectIterator = this.visibleChunkMap.values().iterator();

        while(k < 20 && shouldKeepTicking.getAsBoolean() && objectIterator.hasNext()) {
            if (this.saveChunkIfNeeded(objectIterator.next())) {
                ++k;
            }
        }

    }

    private void scheduleUnload(long pos, PlayerChunk holder) {
        CompletableFuture<IChunkAccess> completableFuture = holder.getChunkSave();
        completableFuture.thenAcceptAsync((chunk) -> {
            CompletableFuture<IChunkAccess> completableFuture2 = holder.getChunkSave();
            if (completableFuture2 != completableFuture) {
                this.scheduleUnload(pos, holder);
            } else {
                if (this.pendingUnloads.remove(pos, holder) && chunk != null) {
                    if (chunk instanceof Chunk) {
                        ((Chunk)chunk).setLoaded(false);
                    }

                    this.saveChunk(chunk);
                    if (this.entitiesInLevel.remove(pos) && chunk instanceof Chunk) {
                        Chunk levelChunk = (Chunk)chunk;
                        this.level.unloadChunk(levelChunk);
                    }

                    this.lightEngine.updateChunkStatus(chunk.getPos());
                    this.lightEngine.queueUpdate();
                    this.progressListener.onStatusChange(chunk.getPos(), (ChunkStatus)null);
                }

            }
        }, this.unloadQueue::add).whenComplete((void_, throwable) -> {
            if (throwable != null) {
                LOGGER.error("Failed to save chunk {}", holder.getPos(), throwable);
            }

        });
    }

    protected boolean promoteChunkMap() {
        if (!this.modified) {
            return false;
        } else {
            this.visibleChunkMap = this.updatingChunkMap.clone();
            this.modified = false;
            return true;
        }
    }

    public CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>> schedule(PlayerChunk holder, ChunkStatus requiredStatus) {
        ChunkCoordIntPair chunkPos = holder.getPos();
        if (requiredStatus == ChunkStatus.EMPTY) {
            return this.scheduleChunkLoad(chunkPos);
        } else {
            if (requiredStatus == ChunkStatus.LIGHT) {
                this.distanceManager.addTicket(TicketType.LIGHT, chunkPos, 33 + ChunkStatus.getDistance(ChunkStatus.LIGHT), chunkPos);
            }

            Optional<IChunkAccess> optional = holder.getOrScheduleFuture(requiredStatus.getParent(), this).getNow(PlayerChunk.UNLOADED_CHUNK).left();
            if (optional.isPresent() && optional.get().getChunkStatus().isOrAfter(requiredStatus)) {
                CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>> completableFuture = requiredStatus.load(this.level, this.structureManager, this.lightEngine, (chunkAccess) -> {
                    return this.protoChunkToFullChunk(holder);
                }, optional.get());
                this.progressListener.onStatusChange(chunkPos, requiredStatus);
                return completableFuture;
            } else {
                return this.scheduleChunkGeneration(holder, requiredStatus);
            }
        }
    }

    private CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>> scheduleChunkLoad(ChunkCoordIntPair pos) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                this.level.getMethodProfiler().incrementCounter("chunkLoad");
                NBTTagCompound compoundTag = this.readChunkData(pos);
                if (compoundTag != null) {
                    boolean bl = compoundTag.hasKeyOfType("Status", 8);
                    if (bl) {
                        IChunkAccess chunkAccess = ChunkRegionLoader.read(this.level, this.poiManager, pos, compoundTag);
                        this.markPosition(pos, chunkAccess.getChunkStatus().getType());
                        return Either.left(chunkAccess);
                    }

                    LOGGER.error("Chunk file at {} is missing level data, skipping", (Object)pos);
                }
            } catch (ReportedException var5) {
                Throwable throwable = var5.getCause();
                if (!(throwable instanceof IOException)) {
                    this.markPositionReplaceable(pos);
                    throw var5;
                }

                LOGGER.error("Couldn't load chunk {}", pos, throwable);
            } catch (Exception var6) {
                LOGGER.error("Couldn't load chunk {}", pos, var6);
            }

            this.markPositionReplaceable(pos);
            return Either.left(new ProtoChunk(pos, ChunkConverter.EMPTY, this.level, this.level.registryAccess().registryOrThrow(IRegistry.BIOME_REGISTRY), (BlendingData)null));
        }, this.mainThreadExecutor);
    }

    private void markPositionReplaceable(ChunkCoordIntPair pos) {
        this.chunkTypeCache.put(pos.pair(), (byte)-1);
    }

    private byte markPosition(ChunkCoordIntPair pos, ChunkStatus.Type type) {
        return this.chunkTypeCache.put(pos.pair(), (byte)(type == ChunkStatus.Type.PROTOCHUNK ? -1 : 1));
    }

    private CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>> scheduleChunkGeneration(PlayerChunk holder, ChunkStatus requiredStatus) {
        ChunkCoordIntPair chunkPos = holder.getPos();
        CompletableFuture<Either<List<IChunkAccess>, PlayerChunk.Failure>> completableFuture = this.getChunkRangeFuture(chunkPos, requiredStatus.getRange(), (i) -> {
            return this.getDependencyStatus(requiredStatus, i);
        });
        this.level.getMethodProfiler().incrementCounter(() -> {
            return "chunkGenerate " + requiredStatus.getName();
        });
        Executor executor = (runnable) -> {
            this.worldgenMailbox.tell(ChunkTaskQueueSorter.message(holder, runnable));
        };
        return completableFuture.thenComposeAsync((either) -> {
            return either.map((list) -> {
                try {
                    CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>> completableFuture = requiredStatus.generate(executor, this.level, this.generator, this.structureManager, this.lightEngine, (chunkAccess) -> {
                        return this.protoChunkToFullChunk(holder);
                    }, list, false);
                    this.progressListener.onStatusChange(chunkPos, requiredStatus);
                    return completableFuture;
                } catch (Exception var9) {
                    var9.getStackTrace();
                    CrashReport crashReport = CrashReport.forThrowable(var9, "Exception generating new chunk");
                    CrashReportSystemDetails crashReportCategory = crashReport.addCategory("Chunk to be generated");
                    crashReportCategory.setDetail("Location", String.format("%d,%d", chunkPos.x, chunkPos.z));
                    crashReportCategory.setDetail("Position hash", ChunkCoordIntPair.pair(chunkPos.x, chunkPos.z));
                    crashReportCategory.setDetail("Generator", this.generator);
                    this.mainThreadExecutor.execute(() -> {
                        throw new ReportedException(crashReport);
                    });
                    throw new ReportedException(crashReport);
                }
            }, (chunkLoadingFailure) -> {
                this.releaseLightTicket(chunkPos);
                return CompletableFuture.completedFuture(Either.right(chunkLoadingFailure));
            });
        }, executor);
    }

    protected void releaseLightTicket(ChunkCoordIntPair pos) {
        this.mainThreadExecutor.tell(SystemUtils.name(() -> {
            this.distanceManager.removeTicket(TicketType.LIGHT, pos, 33 + ChunkStatus.getDistance(ChunkStatus.LIGHT), pos);
        }, () -> {
            return "release light ticket " + pos;
        }));
    }

    private ChunkStatus getDependencyStatus(ChunkStatus centerChunkTargetStatus, int distance) {
        ChunkStatus chunkStatus;
        if (distance == 0) {
            chunkStatus = centerChunkTargetStatus.getParent();
        } else {
            chunkStatus = ChunkStatus.getStatusAroundFullChunk(ChunkStatus.getDistance(centerChunkTargetStatus) + distance);
        }

        return chunkStatus;
    }

    private static void postLoadProtoChunk(WorldServer world, List<NBTTagCompound> nbt) {
        if (!nbt.isEmpty()) {
            world.addWorldGenChunkEntities(EntityTypes.loadEntitiesRecursive(nbt, world));
        }

    }

    private CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>> protoChunkToFullChunk(PlayerChunk chunkHolder) {
        CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>> completableFuture = chunkHolder.getStatusFutureUnchecked(ChunkStatus.FULL.getParent());
        return completableFuture.thenApplyAsync((either) -> {
            ChunkStatus chunkStatus = PlayerChunk.getChunkStatus(chunkHolder.getTicketLevel());
            return !chunkStatus.isOrAfter(ChunkStatus.FULL) ? PlayerChunk.UNLOADED_CHUNK : either.mapLeft((chunk) -> {
                ChunkCoordIntPair chunkPos = chunkHolder.getPos();
                ProtoChunk protoChunk = (ProtoChunk)chunk;
                Chunk levelChunk;
                if (protoChunk instanceof ProtoChunkExtension) {
                    levelChunk = ((ProtoChunkExtension)protoChunk).getWrapped();
                } else {
                    levelChunk = new Chunk(this.level, protoChunk, (levelChunk) -> {
                        postLoadProtoChunk(this.level, protoChunk.getEntities());
                    });
                    chunkHolder.replaceProtoChunk(new ProtoChunkExtension(levelChunk, false));
                }

                levelChunk.setFullStatus(() -> {
                    return PlayerChunk.getChunkState(chunkHolder.getTicketLevel());
                });
                levelChunk.addEntities();
                if (this.entitiesInLevel.add(chunkPos.pair())) {
                    levelChunk.setLoaded(true);
                    levelChunk.registerAllBlockEntitiesAfterLevelLoad();
                    levelChunk.registerTickContainerInLevel(this.level);
                }

                return levelChunk;
            });
        }, (runnable) -> {
            this.mainThreadMailbox.tell(ChunkTaskQueueSorter.message(runnable, chunkHolder.getPos().pair(), chunkHolder::getTicketLevel));
        });
    }

    public CompletableFuture<Either<Chunk, PlayerChunk.Failure>> prepareTickingChunk(PlayerChunk holder) {
        ChunkCoordIntPair chunkPos = holder.getPos();
        CompletableFuture<Either<List<IChunkAccess>, PlayerChunk.Failure>> completableFuture = this.getChunkRangeFuture(chunkPos, 1, (i) -> {
            return ChunkStatus.FULL;
        });
        CompletableFuture<Either<Chunk, PlayerChunk.Failure>> completableFuture2 = completableFuture.thenApplyAsync((either) -> {
            return either.flatMap((list) -> {
                Chunk levelChunk = (Chunk)list.get(list.size() / 2);
                levelChunk.postProcessGeneration();
                this.level.startTickingChunk(levelChunk);
                return Either.left(levelChunk);
            });
        }, (runnable) -> {
            this.mainThreadMailbox.tell(ChunkTaskQueueSorter.message(holder, runnable));
        });
        completableFuture2.thenAcceptAsync((either) -> {
            either.ifLeft((levelChunk) -> {
                this.tickingGenerated.getAndIncrement();
                MutableObject<ClientboundLevelChunkWithLightPacket> mutableObject = new MutableObject<>();
                this.getPlayers(chunkPos, false).forEach((player) -> {
                    this.playerLoadedChunk(player, mutableObject, levelChunk);
                });
            });
        }, (runnable) -> {
            this.mainThreadMailbox.tell(ChunkTaskQueueSorter.message(holder, runnable));
        });
        return completableFuture2;
    }

    public CompletableFuture<Either<Chunk, PlayerChunk.Failure>> prepareAccessibleChunk(PlayerChunk chunkHolder) {
        return this.getChunkRangeFuture(chunkHolder.getPos(), 1, ChunkStatus::getStatusAroundFullChunk).thenApplyAsync((either) -> {
            return either.mapLeft((list) -> {
                return (Chunk)list.get(list.size() / 2);
            });
        }, (runnable) -> {
            this.mainThreadMailbox.tell(ChunkTaskQueueSorter.message(chunkHolder, runnable));
        });
    }

    public int getTickingGenerated() {
        return this.tickingGenerated.get();
    }

    private boolean saveChunkIfNeeded(PlayerChunk chunkHolder) {
        if (!chunkHolder.hasBeenLoaded()) {
            return false;
        } else {
            IChunkAccess chunkAccess = chunkHolder.getChunkSave().getNow((IChunkAccess)null);
            if (!(chunkAccess instanceof ProtoChunkExtension) && !(chunkAccess instanceof Chunk)) {
                return false;
            } else {
                boolean bl = this.saveChunk(chunkAccess);
                chunkHolder.refreshAccessibility();
                return bl;
            }
        }
    }

    public boolean saveChunk(IChunkAccess chunk) {
        this.poiManager.flush(chunk.getPos());
        if (!chunk.isNeedsSaving()) {
            return false;
        } else {
            chunk.setNeedsSaving(false);
            ChunkCoordIntPair chunkPos = chunk.getPos();

            try {
                ChunkStatus chunkStatus = chunk.getChunkStatus();
                if (chunkStatus.getType() != ChunkStatus.Type.LEVELCHUNK) {
                    if (this.isExistingChunkFull(chunkPos)) {
                        return false;
                    }

                    if (chunkStatus == ChunkStatus.EMPTY && chunk.getAllStarts().values().stream().noneMatch(StructureStart::isValid)) {
                        return false;
                    }
                }

                this.level.getMethodProfiler().incrementCounter("chunkSave");
                NBTTagCompound compoundTag = ChunkRegionLoader.saveChunk(this.level, chunk);
                this.write(chunkPos, compoundTag);
                this.markPosition(chunkPos, chunkStatus.getType());
                return true;
            } catch (Exception var5) {
                LOGGER.error("Failed to save chunk {},{}", chunkPos.x, chunkPos.z, var5);
                return false;
            }
        }
    }

    private boolean isExistingChunkFull(ChunkCoordIntPair pos) {
        byte b = this.chunkTypeCache.get(pos.pair());
        if (b != 0) {
            return b == 1;
        } else {
            NBTTagCompound compoundTag;
            try {
                compoundTag = this.readChunkData(pos);
                if (compoundTag == null) {
                    this.markPositionReplaceable(pos);
                    return false;
                }
            } catch (Exception var5) {
                LOGGER.error("Failed to read chunk {}", pos, var5);
                this.markPositionReplaceable(pos);
                return false;
            }

            ChunkStatus.Type chunkType = ChunkRegionLoader.getChunkTypeFromTag(compoundTag);
            return this.markPosition(pos, chunkType) == 1;
        }
    }

    public void setViewDistance(int watchDistance) {
        int i = MathHelper.clamp(watchDistance + 1, 3, 33);
        if (i != this.viewDistance) {
            int j = this.viewDistance;
            this.viewDistance = i;
            this.distanceManager.updatePlayerTickets(this.viewDistance);

            for(PlayerChunk chunkHolder : this.updatingChunkMap.values()) {
                ChunkCoordIntPair chunkPos = chunkHolder.getPos();
                MutableObject<ClientboundLevelChunkWithLightPacket> mutableObject = new MutableObject<>();
                this.getPlayers(chunkPos, false).forEach((player) -> {
                    boolean bl = isChunkInEuclideanRange(chunkPos, player, true, j);
                    boolean bl2 = isChunkInEuclideanRange(chunkPos, player, true, this.viewDistance);
                    this.updateChunkTracking(player, chunkPos, mutableObject, bl, bl2);
                });
            }
        }

    }

    protected void updateChunkTracking(EntityPlayer player, ChunkCoordIntPair pos, MutableObject<ClientboundLevelChunkWithLightPacket> mutableObject, boolean oldWithinViewDistance, boolean newWithinViewDistance) {
        if (player.level == this.level) {
            if (newWithinViewDistance && !oldWithinViewDistance) {
                PlayerChunk chunkHolder = this.getVisibleChunk(pos.pair());
                if (chunkHolder != null) {
                    Chunk levelChunk = chunkHolder.getChunk();
                    if (levelChunk != null) {
                        this.playerLoadedChunk(player, mutableObject, levelChunk);
                    }

                    PacketDebug.sendPoiPacketsForChunk(this.level, pos);
                }
            }

            if (!newWithinViewDistance && oldWithinViewDistance) {
                player.untrackChunk(pos);
            }

        }
    }

    public int size() {
        return this.visibleChunkMap.size();
    }

    public ChunkMapDistance getDistanceManager() {
        return this.distanceManager;
    }

    protected Iterable<PlayerChunk> getChunks() {
        return Iterables.unmodifiableIterable(this.visibleChunkMap.values());
    }

    void dumpChunks(Writer writer) throws IOException {
        CSVWriter csvOutput = CSVWriter.builder().addColumn("x").addColumn("z").addColumn("level").addColumn("in_memory").addColumn("status").addColumn("full_status").addColumn("accessible_ready").addColumn("ticking_ready").addColumn("entity_ticking_ready").addColumn("ticket").addColumn("spawning").addColumn("block_entity_count").addColumn("ticking_ticket").addColumn("ticking_level").addColumn("block_ticks").addColumn("fluid_ticks").build(writer);
        TickingTracker tickingTracker = this.distanceManager.tickingTracker();

        for(Entry<PlayerChunk> entry : this.visibleChunkMap.long2ObjectEntrySet()) {
            long l = entry.getLongKey();
            ChunkCoordIntPair chunkPos = new ChunkCoordIntPair(l);
            PlayerChunk chunkHolder = entry.getValue();
            Optional<IChunkAccess> optional = Optional.ofNullable(chunkHolder.getLastAvailable());
            Optional<Chunk> optional2 = optional.flatMap((chunk) -> {
                return chunk instanceof Chunk ? Optional.of((Chunk)chunk) : Optional.empty();
            });
            csvOutput.writeRow(chunkPos.x, chunkPos.z, chunkHolder.getTicketLevel(), optional.isPresent(), optional.map(IChunkAccess::getChunkStatus).orElse((ChunkStatus)null), optional2.map(Chunk::getState).orElse((PlayerChunk.State)null), printFuture(chunkHolder.getFullChunkFuture()), printFuture(chunkHolder.getTickingChunkFuture()), printFuture(chunkHolder.getEntityTickingChunkFuture()), this.distanceManager.getTicketDebugString(l), this.anyPlayerCloseEnoughForSpawning(chunkPos), optional2.map((levelChunk) -> {
                return levelChunk.getTileEntities().size();
            }).orElse(0), tickingTracker.getTicketDebugString(l), tickingTracker.getLevel(l), optional2.map((levelChunk) -> {
                return levelChunk.getBlockTicks().count();
            }).orElse(0), optional2.map((levelChunk) -> {
                return levelChunk.getFluidTicks().count();
            }).orElse(0));
        }

    }

    private static String printFuture(CompletableFuture<Either<Chunk, PlayerChunk.Failure>> completableFuture) {
        try {
            Either<Chunk, PlayerChunk.Failure> either = completableFuture.getNow((Either<Chunk, PlayerChunk.Failure>)null);
            return either != null ? either.map((levelChunk) -> {
                return "done";
            }, (chunkLoadingFailure) -> {
                return "unloaded";
            }) : "not completed";
        } catch (CompletionException var2) {
            return "failed " + var2.getCause().getMessage();
        } catch (CancellationException var3) {
            return "cancelled";
        }
    }

    @Nullable
    public NBTTagCompound readChunkData(ChunkCoordIntPair pos) throws IOException {
        NBTTagCompound compoundTag = this.read(pos);
        return compoundTag == null ? null : this.upgradeChunkTag(this.level.getDimensionKey(), this.overworldDataStorage, compoundTag, this.generator.getTypeNameForDataFixer());
    }

    boolean anyPlayerCloseEnoughForSpawning(ChunkCoordIntPair pos) {
        long l = pos.pair();
        if (!this.distanceManager.hasPlayersNearby(l)) {
            return false;
        } else {
            for(EntityPlayer serverPlayer : this.playerMap.getPlayers(l)) {
                if (this.playerIsCloseEnoughForSpawning(serverPlayer, pos)) {
                    return true;
                }
            }

            return false;
        }
    }

    public List<EntityPlayer> getPlayersCloseForSpawning(ChunkCoordIntPair pos) {
        long l = pos.pair();
        if (!this.distanceManager.hasPlayersNearby(l)) {
            return List.of();
        } else {
            Builder<EntityPlayer> builder = ImmutableList.builder();

            for(EntityPlayer serverPlayer : this.playerMap.getPlayers(l)) {
                if (this.playerIsCloseEnoughForSpawning(serverPlayer, pos)) {
                    builder.add(serverPlayer);
                }
            }

            return builder.build();
        }
    }

    private boolean playerIsCloseEnoughForSpawning(EntityPlayer player, ChunkCoordIntPair pos) {
        if (player.isSpectator()) {
            return false;
        } else {
            double d = euclideanDistanceSquared(pos, player);
            return d < 16384.0D;
        }
    }

    private boolean skipPlayer(EntityPlayer player) {
        return player.isSpectator() && !this.level.getGameRules().getBoolean(GameRules.RULE_SPECTATORSGENERATECHUNKS);
    }

    void updatePlayerStatus(EntityPlayer player, boolean added) {
        boolean bl = this.skipPlayer(player);
        boolean bl2 = this.playerMap.ignoredOrUnknown(player);
        int i = SectionPosition.blockToSectionCoord(player.getBlockX());
        int j = SectionPosition.blockToSectionCoord(player.getBlockZ());
        if (added) {
            this.playerMap.addPlayer(ChunkCoordIntPair.pair(i, j), player, bl);
            this.updatePlayerPos(player);
            if (!bl) {
                this.distanceManager.addPlayer(SectionPosition.of(player), player);
            }
        } else {
            SectionPosition sectionPos = player.getLastSectionPos();
            this.playerMap.removePlayer(sectionPos.chunk().pair(), player);
            if (!bl2) {
                this.distanceManager.removePlayer(sectionPos, player);
            }
        }

        for(int k = i - this.viewDistance; k <= i + this.viewDistance; ++k) {
            for(int l = j - this.viewDistance; l <= j + this.viewDistance; ++l) {
                if (isChunkInEuclideanRange(k, l, i, j, this.viewDistance)) {
                    ChunkCoordIntPair chunkPos = new ChunkCoordIntPair(k, l);
                    this.updateChunkTracking(player, chunkPos, new MutableObject<>(), !added, added);
                }
            }
        }

    }

    private SectionPosition updatePlayerPos(EntityPlayer player) {
        SectionPosition sectionPos = SectionPosition.of(player);
        player.setLastSectionPos(sectionPos);
        player.connection.sendPacket(new PacketPlayOutViewCentre(sectionPos.x(), sectionPos.z()));
        return sectionPos;
    }

    public void movePlayer(EntityPlayer player) {
        for(PlayerChunkMap.EntityTracker trackedEntity : this.entityMap.values()) {
            if (trackedEntity.entity == player) {
                trackedEntity.track(this.level.getPlayers());
            } else {
                trackedEntity.updatePlayer(player);
            }
        }

        int i = SectionPosition.blockToSectionCoord(player.getBlockX());
        int j = SectionPosition.blockToSectionCoord(player.getBlockZ());
        SectionPosition sectionPos = player.getLastSectionPos();
        SectionPosition sectionPos2 = SectionPosition.of(player);
        long l = sectionPos.chunk().pair();
        long m = sectionPos2.chunk().pair();
        boolean bl = this.playerMap.ignored(player);
        boolean bl2 = this.skipPlayer(player);
        boolean bl3 = sectionPos.asLong() != sectionPos2.asLong();
        if (bl3 || bl != bl2) {
            this.updatePlayerPos(player);
            if (!bl) {
                this.distanceManager.removePlayer(sectionPos, player);
            }

            if (!bl2) {
                this.distanceManager.addPlayer(sectionPos2, player);
            }

            if (!bl && bl2) {
                this.playerMap.ignorePlayer(player);
            }

            if (bl && !bl2) {
                this.playerMap.unIgnorePlayer(player);
            }

            if (l != m) {
                this.playerMap.updatePlayer(l, m, player);
            }
        }

        int k = sectionPos.x();
        int n = sectionPos.z();
        if (Math.abs(k - i) <= this.viewDistance * 2 && Math.abs(n - j) <= this.viewDistance * 2) {
            int o = Math.min(i, k) - this.viewDistance;
            int p = Math.min(j, n) - this.viewDistance;
            int q = Math.max(i, k) + this.viewDistance;
            int r = Math.max(j, n) + this.viewDistance;

            for(int s = o; s <= q; ++s) {
                for(int t = p; t <= r; ++t) {
                    ChunkCoordIntPair chunkPos = new ChunkCoordIntPair(s, t);
                    boolean bl4 = isChunkInEuclideanRange(chunkPos, k, n, this.viewDistance);
                    boolean bl5 = isChunkInEuclideanRange(chunkPos, i, j, this.viewDistance);
                    this.updateChunkTracking(player, chunkPos, new MutableObject<>(), bl4, bl5);
                }
            }
        } else {
            for(int u = k - this.viewDistance; u <= k + this.viewDistance; ++u) {
                for(int v = n - this.viewDistance; v <= n + this.viewDistance; ++v) {
                    if (isChunkInEuclideanRange(u, v, k, n, this.viewDistance)) {
                        ChunkCoordIntPair chunkPos2 = new ChunkCoordIntPair(u, v);
                        boolean bl6 = true;
                        boolean bl7 = false;
                        this.updateChunkTracking(player, chunkPos2, new MutableObject<>(), true, false);
                    }
                }
            }

            for(int w = i - this.viewDistance; w <= i + this.viewDistance; ++w) {
                for(int x = j - this.viewDistance; x <= j + this.viewDistance; ++x) {
                    if (isChunkInEuclideanRange(w, x, i, j, this.viewDistance)) {
                        ChunkCoordIntPair chunkPos3 = new ChunkCoordIntPair(w, x);
                        boolean bl8 = false;
                        boolean bl9 = true;
                        this.updateChunkTracking(player, chunkPos3, new MutableObject<>(), false, true);
                    }
                }
            }
        }

    }

    @Override
    public List<EntityPlayer> getPlayers(ChunkCoordIntPair chunkPos, boolean onlyOnWatchDistanceEdge) {
        Set<EntityPlayer> set = this.playerMap.getPlayers(chunkPos.pair());
        Builder<EntityPlayer> builder = ImmutableList.builder();

        for(EntityPlayer serverPlayer : set) {
            if (onlyOnWatchDistanceEdge && isChunkOnEuclideanBorder(chunkPos, serverPlayer, true, this.viewDistance) || !onlyOnWatchDistanceEdge && isChunkInEuclideanRange(chunkPos, serverPlayer, true, this.viewDistance)) {
                builder.add(serverPlayer);
            }
        }

        return builder.build();
    }

    public void addEntity(Entity entity) {
        if (!(entity instanceof EntityComplexPart)) {
            EntityTypes<?> entityType = entity.getEntityType();
            int i = entityType.getChunkRange() * 16;
            if (i != 0) {
                int j = entityType.getUpdateInterval();
                if (this.entityMap.containsKey(entity.getId())) {
                    throw (IllegalStateException)SystemUtils.pauseInIde(new IllegalStateException("Entity is already tracked!"));
                } else {
                    PlayerChunkMap.EntityTracker trackedEntity = new PlayerChunkMap.EntityTracker(entity, i, j, entityType.isDeltaTracking());
                    this.entityMap.put(entity.getId(), trackedEntity);
                    trackedEntity.track(this.level.getPlayers());
                    if (entity instanceof EntityPlayer) {
                        EntityPlayer serverPlayer = (EntityPlayer)entity;
                        this.updatePlayerStatus(serverPlayer, true);

                        for(PlayerChunkMap.EntityTracker trackedEntity2 : this.entityMap.values()) {
                            if (trackedEntity2.entity != serverPlayer) {
                                trackedEntity2.updatePlayer(serverPlayer);
                            }
                        }
                    }

                }
            }
        }
    }

    protected void removeEntity(Entity entity) {
        if (entity instanceof EntityPlayer) {
            EntityPlayer serverPlayer = (EntityPlayer)entity;
            this.updatePlayerStatus(serverPlayer, false);

            for(PlayerChunkMap.EntityTracker trackedEntity : this.entityMap.values()) {
                trackedEntity.clear(serverPlayer);
            }
        }

        PlayerChunkMap.EntityTracker trackedEntity2 = this.entityMap.remove(entity.getId());
        if (trackedEntity2 != null) {
            trackedEntity2.broadcastRemoved();
        }

    }

    protected void tick() {
        List<EntityPlayer> list = Lists.newArrayList();
        List<EntityPlayer> list2 = this.level.getPlayers();

        for(PlayerChunkMap.EntityTracker trackedEntity : this.entityMap.values()) {
            SectionPosition sectionPos = trackedEntity.lastSectionPos;
            SectionPosition sectionPos2 = SectionPosition.of(trackedEntity.entity);
            boolean bl = !Objects.equals(sectionPos, sectionPos2);
            if (bl) {
                trackedEntity.track(list2);
                Entity entity = trackedEntity.entity;
                if (entity instanceof EntityPlayer) {
                    list.add((EntityPlayer)entity);
                }

                trackedEntity.lastSectionPos = sectionPos2;
            }

            if (bl || this.distanceManager.inEntityTickingRange(sectionPos2.chunk().pair())) {
                trackedEntity.serverEntity.sendChanges();
            }
        }

        if (!list.isEmpty()) {
            for(PlayerChunkMap.EntityTracker trackedEntity2 : this.entityMap.values()) {
                trackedEntity2.track(list);
            }
        }

    }

    public void broadcast(Entity entity, Packet<?> packet) {
        PlayerChunkMap.EntityTracker trackedEntity = this.entityMap.get(entity.getId());
        if (trackedEntity != null) {
            trackedEntity.broadcast(packet);
        }

    }

    protected void broadcastIncludingSelf(Entity entity, Packet<?> packet) {
        PlayerChunkMap.EntityTracker trackedEntity = this.entityMap.get(entity.getId());
        if (trackedEntity != null) {
            trackedEntity.broadcastIncludingSelf(packet);
        }

    }

    private void playerLoadedChunk(EntityPlayer player, MutableObject<ClientboundLevelChunkWithLightPacket> cachedDataPacket, Chunk chunk) {
        if (cachedDataPacket.getValue() == null) {
            cachedDataPacket.setValue(new ClientboundLevelChunkWithLightPacket(chunk, this.lightEngine, (BitSet)null, (BitSet)null, true));
        }

        player.trackChunk(chunk.getPos(), cachedDataPacket.getValue());
        PacketDebug.sendPoiPacketsForChunk(this.level, chunk.getPos());
        List<Entity> list = Lists.newArrayList();
        List<Entity> list2 = Lists.newArrayList();

        for(PlayerChunkMap.EntityTracker trackedEntity : this.entityMap.values()) {
            Entity entity = trackedEntity.entity;
            if (entity != player && entity.chunkPosition().equals(chunk.getPos())) {
                trackedEntity.updatePlayer(player);
                if (entity instanceof EntityInsentient && ((EntityInsentient)entity).getLeashHolder() != null) {
                    list.add(entity);
                }

                if (!entity.getPassengers().isEmpty()) {
                    list2.add(entity);
                }
            }
        }

        if (!list.isEmpty()) {
            for(Entity entity2 : list) {
                player.connection.sendPacket(new PacketPlayOutAttachEntity(entity2, ((EntityInsentient)entity2).getLeashHolder()));
            }
        }

        if (!list2.isEmpty()) {
            for(Entity entity3 : list2) {
                player.connection.sendPacket(new PacketPlayOutMount(entity3));
            }
        }

    }

    public VillagePlace getPoiManager() {
        return this.poiManager;
    }

    public String getStorageName() {
        return this.storageName;
    }

    void onFullChunkStatusChange(ChunkCoordIntPair chunkPos, PlayerChunk.State levelType) {
        this.chunkStatusListener.onChunkStatusChange(chunkPos, levelType);
    }

    class DistanceManager extends ChunkMapDistance {
        protected DistanceManager(Executor mainThreadExecutor, Executor executor) {
            super(mainThreadExecutor, executor);
        }

        @Override
        protected boolean isChunkToRemove(long pos) {
            return PlayerChunkMap.this.toDrop.contains(pos);
        }

        @Nullable
        @Override
        protected PlayerChunk getChunk(long pos) {
            return PlayerChunkMap.this.getUpdatingChunk(pos);
        }

        @Nullable
        @Override
        protected PlayerChunk updateChunkScheduling(long pos, int level, @Nullable PlayerChunk holder, int i) {
            return PlayerChunkMap.this.updateChunkScheduling(pos, level, holder, i);
        }
    }

    public class EntityTracker {
        final EntityTrackerEntry serverEntity;
        final Entity entity;
        private final int range;
        SectionPosition lastSectionPos;
        public final Set<PlayerConnectionServer> seenBy = Sets.newIdentityHashSet();

        public EntityTracker(Entity entity, int maxDistance, int tickInterval, boolean alwaysUpdateVelocity) {
            this.serverEntity = new EntityTrackerEntry(PlayerChunkMap.this.level, entity, tickInterval, alwaysUpdateVelocity, this::broadcast);
            this.entity = entity;
            this.range = maxDistance;
            this.lastSectionPos = SectionPosition.of(entity);
        }

        @Override
        public boolean equals(Object object) {
            if (object instanceof PlayerChunkMap.EntityTracker) {
                return ((PlayerChunkMap.EntityTracker)object).entity.getId() == this.entity.getId();
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return this.entity.getId();
        }

        public void broadcast(Packet<?> packet) {
            for(PlayerConnectionServer serverPlayerConnection : this.seenBy) {
                serverPlayerConnection.sendPacket(packet);
            }

        }

        public void broadcastIncludingSelf(Packet<?> packet) {
            this.broadcast(packet);
            if (this.entity instanceof EntityPlayer) {
                ((EntityPlayer)this.entity).connection.sendPacket(packet);
            }

        }

        public void broadcastRemoved() {
            for(PlayerConnectionServer serverPlayerConnection : this.seenBy) {
                this.serverEntity.removePairing(serverPlayerConnection.getPlayer());
            }

        }

        public void clear(EntityPlayer player) {
            if (this.seenBy.remove(player.connection)) {
                this.serverEntity.removePairing(player);
            }

        }

        public void updatePlayer(EntityPlayer player) {
            if (player != this.entity) {
                Vec3D vec3 = player.getPositionVector().subtract(this.serverEntity.sentPos());
                double d = (double)Math.min(this.getEffectiveRange(), (PlayerChunkMap.this.viewDistance - 1) * 16);
                double e = vec3.x * vec3.x + vec3.z * vec3.z;
                double f = d * d;
                boolean bl = e <= f && this.entity.broadcastToPlayer(player);
                if (bl) {
                    if (this.seenBy.add(player.connection)) {
                        this.serverEntity.addPairing(player);
                    }
                } else if (this.seenBy.remove(player.connection)) {
                    this.serverEntity.removePairing(player);
                }

            }
        }

        private int scaledRange(int initialDistance) {
            return PlayerChunkMap.this.level.getMinecraftServer().getScaledTrackingDistance(initialDistance);
        }

        private int getEffectiveRange() {
            int i = this.range;

            for(Entity entity : this.entity.getAllPassengers()) {
                int j = entity.getEntityType().getChunkRange() * 16;
                if (j > i) {
                    i = j;
                }
            }

            return this.scaledRange(i);
        }

        public void track(List<EntityPlayer> players) {
            for(EntityPlayer serverPlayer : players) {
                this.updatePlayer(serverPlayer);
            }

        }
    }
}
