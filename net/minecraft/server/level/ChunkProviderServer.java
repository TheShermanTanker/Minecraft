package net.minecraft.server.level;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Either;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.SectionPosition;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.progress.WorldLoadListener;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.util.profiling.GameProfilerFiller;
import net.minecraft.util.thread.IAsyncTaskHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.village.poi.VillagePlace;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.EnumSkyBlock;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.LocalMobCapCalculator;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.World;
import net.minecraft.world.level.chunk.Chunk;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.IChunkAccess;
import net.minecraft.world.level.chunk.IChunkProvider;
import net.minecraft.world.level.chunk.storage.ChunkScanAccess;
import net.minecraft.world.level.entity.ChunkStatusUpdateListener;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureManager;
import net.minecraft.world.level.storage.Convertable;
import net.minecraft.world.level.storage.WorldData;
import net.minecraft.world.level.storage.WorldPersistentData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ChunkProviderServer extends IChunkProvider {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final List<ChunkStatus> CHUNK_STATUSES = ChunkStatus.getStatusList();
    private final ChunkMapDistance distanceManager;
    final WorldServer level;
    public final Thread mainThread;
    final LightEngineThreaded lightEngine;
    public final ChunkProviderServer.MainThreadExecutor mainThreadProcessor;
    public final PlayerChunkMap chunkMap;
    private final WorldPersistentData dataStorage;
    private long lastInhabitedUpdate;
    public boolean spawnEnemies = true;
    public boolean spawnFriendlies = true;
    private static final int CACHE_SIZE = 4;
    private final long[] lastChunkPos = new long[4];
    private final ChunkStatus[] lastChunkStatus = new ChunkStatus[4];
    private final IChunkAccess[] lastChunk = new IChunkAccess[4];
    @Nullable
    @VisibleForDebug
    private NaturalSpawner.SpawnState lastSpawnState;

    public ChunkProviderServer(WorldServer world, Convertable.ConversionSession session, DataFixer dataFixer, DefinedStructureManager structureManager, Executor workerExecutor, ChunkGenerator chunkGenerator, int viewDistance, int simulationDistance, boolean dsync, WorldLoadListener worldGenerationProgressListener, ChunkStatusUpdateListener chunkStatusChangeListener, Supplier<WorldPersistentData> persistentStateManagerFactory) {
        this.level = world;
        this.mainThreadProcessor = new ChunkProviderServer.MainThreadExecutor(world);
        this.mainThread = Thread.currentThread();
        File file = session.getDimensionPath(world.getDimensionKey()).resolve("data").toFile();
        file.mkdirs();
        this.dataStorage = new WorldPersistentData(file, dataFixer);
        this.chunkMap = new PlayerChunkMap(world, session, dataFixer, structureManager, workerExecutor, this.mainThreadProcessor, this, chunkGenerator, worldGenerationProgressListener, chunkStatusChangeListener, persistentStateManagerFactory, viewDistance, dsync);
        this.lightEngine = this.chunkMap.getLightEngine();
        this.distanceManager = this.chunkMap.getDistanceManager();
        this.distanceManager.updateSimulationDistance(simulationDistance);
        this.clearCache();
    }

    @Override
    public LightEngineThreaded getLightEngine() {
        return this.lightEngine;
    }

    @Nullable
    private PlayerChunk getChunk(long pos) {
        return this.chunkMap.getVisibleChunk(pos);
    }

    public int getTickingGenerated() {
        return this.chunkMap.getTickingGenerated();
    }

    private void storeInCache(long pos, IChunkAccess chunk, ChunkStatus status) {
        for(int i = 3; i > 0; --i) {
            this.lastChunkPos[i] = this.lastChunkPos[i - 1];
            this.lastChunkStatus[i] = this.lastChunkStatus[i - 1];
            this.lastChunk[i] = this.lastChunk[i - 1];
        }

        this.lastChunkPos[0] = pos;
        this.lastChunkStatus[0] = status;
        this.lastChunk[0] = chunk;
    }

    @Nullable
    @Override
    public IChunkAccess getChunkAt(int x, int z, ChunkStatus leastStatus, boolean create) {
        if (Thread.currentThread() != this.mainThread) {
            return CompletableFuture.supplyAsync(() -> {
                return this.getChunkAt(x, z, leastStatus, create);
            }, this.mainThreadProcessor).join();
        } else {
            GameProfilerFiller profilerFiller = this.level.getMethodProfiler();
            profilerFiller.incrementCounter("getChunk");
            long l = ChunkCoordIntPair.pair(x, z);

            for(int i = 0; i < 4; ++i) {
                if (l == this.lastChunkPos[i] && leastStatus == this.lastChunkStatus[i]) {
                    IChunkAccess chunkAccess = this.lastChunk[i];
                    if (chunkAccess != null || !create) {
                        return chunkAccess;
                    }
                }
            }

            profilerFiller.incrementCounter("getChunkCacheMiss");
            CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>> completableFuture = this.getChunkFutureMainThread(x, z, leastStatus, create);
            this.mainThreadProcessor.awaitTasks(completableFuture::isDone);
            IChunkAccess chunkAccess2 = completableFuture.join().map((chunkAccess) -> {
                return chunkAccess;
            }, (chunkLoadingFailure) -> {
                if (create) {
                    throw (IllegalStateException)SystemUtils.pauseInIde(new IllegalStateException("Chunk not there when requested: " + chunkLoadingFailure));
                } else {
                    return null;
                }
            });
            this.storeInCache(l, chunkAccess2, leastStatus);
            return chunkAccess2;
        }
    }

    @Nullable
    @Override
    public Chunk getChunkNow(int chunkX, int chunkZ) {
        if (Thread.currentThread() != this.mainThread) {
            return null;
        } else {
            this.level.getMethodProfiler().incrementCounter("getChunkNow");
            long l = ChunkCoordIntPair.pair(chunkX, chunkZ);

            for(int i = 0; i < 4; ++i) {
                if (l == this.lastChunkPos[i] && this.lastChunkStatus[i] == ChunkStatus.FULL) {
                    IChunkAccess chunkAccess = this.lastChunk[i];
                    return chunkAccess instanceof Chunk ? (Chunk)chunkAccess : null;
                }
            }

            PlayerChunk chunkHolder = this.getChunk(l);
            if (chunkHolder == null) {
                return null;
            } else {
                Either<IChunkAccess, PlayerChunk.Failure> either = chunkHolder.getFutureIfPresent(ChunkStatus.FULL).getNow((Either<IChunkAccess, PlayerChunk.Failure>)null);
                if (either == null) {
                    return null;
                } else {
                    IChunkAccess chunkAccess2 = either.left().orElse((IChunkAccess)null);
                    if (chunkAccess2 != null) {
                        this.storeInCache(l, chunkAccess2, ChunkStatus.FULL);
                        if (chunkAccess2 instanceof Chunk) {
                            return (Chunk)chunkAccess2;
                        }
                    }

                    return null;
                }
            }
        }
    }

    private void clearCache() {
        Arrays.fill(this.lastChunkPos, ChunkCoordIntPair.INVALID_CHUNK_POS);
        Arrays.fill(this.lastChunkStatus, (Object)null);
        Arrays.fill(this.lastChunk, (Object)null);
    }

    public CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>> getChunkFuture(int chunkX, int chunkZ, ChunkStatus leastStatus, boolean create) {
        boolean bl = Thread.currentThread() == this.mainThread;
        CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>> completableFuture;
        if (bl) {
            completableFuture = this.getChunkFutureMainThread(chunkX, chunkZ, leastStatus, create);
            this.mainThreadProcessor.awaitTasks(completableFuture::isDone);
        } else {
            completableFuture = CompletableFuture.supplyAsync(() -> {
                return this.getChunkFutureMainThread(chunkX, chunkZ, leastStatus, create);
            }, this.mainThreadProcessor).thenCompose((completableFuture) -> {
                return completableFuture;
            });
        }

        return completableFuture;
    }

    private CompletableFuture<Either<IChunkAccess, PlayerChunk.Failure>> getChunkFutureMainThread(int chunkX, int chunkZ, ChunkStatus leastStatus, boolean create) {
        ChunkCoordIntPair chunkPos = new ChunkCoordIntPair(chunkX, chunkZ);
        long l = chunkPos.pair();
        int i = 33 + ChunkStatus.getDistance(leastStatus);
        PlayerChunk chunkHolder = this.getChunk(l);
        if (create) {
            this.distanceManager.addTicket(TicketType.UNKNOWN, chunkPos, i, chunkPos);
            if (this.chunkAbsent(chunkHolder, i)) {
                GameProfilerFiller profilerFiller = this.level.getMethodProfiler();
                profilerFiller.enter("chunkLoad");
                this.tickDistanceManager();
                chunkHolder = this.getChunk(l);
                profilerFiller.exit();
                if (this.chunkAbsent(chunkHolder, i)) {
                    throw (IllegalStateException)SystemUtils.pauseInIde(new IllegalStateException("No chunk holder after ticket has been added"));
                }
            }
        }

        return this.chunkAbsent(chunkHolder, i) ? PlayerChunk.UNLOADED_CHUNK_FUTURE : chunkHolder.getOrScheduleFuture(leastStatus, this.chunkMap);
    }

    private boolean chunkAbsent(@Nullable PlayerChunk holder, int maxLevel) {
        return holder == null || holder.getTicketLevel() > maxLevel;
    }

    @Override
    public boolean isLoaded(int x, int z) {
        PlayerChunk chunkHolder = this.getChunk((new ChunkCoordIntPair(x, z)).pair());
        int i = 33 + ChunkStatus.getDistance(ChunkStatus.FULL);
        return !this.chunkAbsent(chunkHolder, i);
    }

    @Override
    public IBlockAccess getChunkForLighting(int chunkX, int chunkZ) {
        long l = ChunkCoordIntPair.pair(chunkX, chunkZ);
        PlayerChunk chunkHolder = this.getChunk(l);
        if (chunkHolder == null) {
            return null;
        } else {
            int i = CHUNK_STATUSES.size() - 1;

            while(true) {
                ChunkStatus chunkStatus = CHUNK_STATUSES.get(i);
                Optional<IChunkAccess> optional = chunkHolder.getStatusFutureUnchecked(chunkStatus).getNow(PlayerChunk.UNLOADED_CHUNK).left();
                if (optional.isPresent()) {
                    return optional.get();
                }

                if (chunkStatus == ChunkStatus.LIGHT.getParent()) {
                    return null;
                }

                --i;
            }
        }
    }

    @Override
    public World getLevel() {
        return this.level;
    }

    public boolean runTasks() {
        return this.mainThreadProcessor.executeNext();
    }

    public boolean tickDistanceManager() {
        boolean bl = this.distanceManager.runAllUpdates(this.chunkMap);
        boolean bl2 = this.chunkMap.promoteChunkMap();
        if (!bl && !bl2) {
            return false;
        } else {
            this.clearCache();
            return true;
        }
    }

    public boolean isPositionTicking(long pos) {
        PlayerChunk chunkHolder = this.getChunk(pos);
        if (chunkHolder == null) {
            return false;
        } else if (!this.level.shouldTickBlocksAt(pos)) {
            return false;
        } else {
            Either<Chunk, PlayerChunk.Failure> either = chunkHolder.getTickingChunkFuture().getNow((Either<Chunk, PlayerChunk.Failure>)null);
            return either != null && either.left().isPresent();
        }
    }

    public void save(boolean flush) {
        this.tickDistanceManager();
        this.chunkMap.save(flush);
    }

    @Override
    public void close() throws IOException {
        this.save(true);
        this.lightEngine.close();
        this.chunkMap.close();
    }

    @Override
    public void tick(BooleanSupplier booleanSupplier) {
        this.level.getMethodProfiler().enter("purge");
        this.distanceManager.purgeTickets();
        this.tickDistanceManager();
        this.level.getMethodProfiler().exitEnter("chunks");
        this.tickChunks();
        this.level.getMethodProfiler().exitEnter("unload");
        this.chunkMap.unloadChunks(booleanSupplier);
        this.level.getMethodProfiler().exit();
        this.clearCache();
    }

    private void tickChunks() {
        long l = this.level.getTime();
        long m = l - this.lastInhabitedUpdate;
        this.lastInhabitedUpdate = l;
        boolean bl = this.level.isDebugWorld();
        if (bl) {
            this.chunkMap.tick();
        } else {
            WorldData levelData = this.level.getWorldData();
            GameProfilerFiller profilerFiller = this.level.getMethodProfiler();
            profilerFiller.enter("pollingChunks");
            int i = this.level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);
            boolean bl2 = levelData.getTime() % 400L == 0L;
            profilerFiller.enter("naturalSpawnCount");
            int j = this.distanceManager.getNaturalSpawnChunkCount();
            NaturalSpawner.SpawnState spawnState = NaturalSpawner.createState(j, this.level.getAllEntities(), this::getFullChunk, new LocalMobCapCalculator(this.chunkMap));
            this.lastSpawnState = spawnState;
            profilerFiller.exitEnter("filteringLoadedChunks");
            List<ServerChunkCache$ChunkAndHolder> list = Lists.newArrayListWithCapacity(j);

            for(PlayerChunk chunkHolder : this.chunkMap.getChunks()) {
                Chunk levelChunk = chunkHolder.getChunk();
                if (levelChunk != null) {
                    list.add(new ServerChunkCache$ChunkAndHolder(levelChunk, chunkHolder));
                }
            }

            profilerFiller.exitEnter("spawnAndTick");
            boolean bl3 = this.level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING);
            Collections.shuffle(list);

            for(ServerChunkCache$ChunkAndHolder chunkAndHolder : list) {
                Chunk levelChunk2 = chunkAndHolder.chunk;
                ChunkCoordIntPair chunkPos = levelChunk2.getPos();
                if (this.level.isPositionEntityTicking(chunkPos) && this.chunkMap.anyPlayerCloseEnoughForSpawning(chunkPos)) {
                    levelChunk2.incrementInhabitedTime(m);
                    if (bl3 && (this.spawnEnemies || this.spawnFriendlies) && this.level.getWorldBorder().isInBounds(chunkPos)) {
                        NaturalSpawner.spawnForChunk(this.level, levelChunk2, spawnState, this.spawnFriendlies, this.spawnEnemies, bl2);
                    }

                    if (this.level.shouldTickBlocksAt(chunkPos.pair())) {
                        this.level.tickChunk(levelChunk2, i);
                    }
                }
            }

            profilerFiller.exitEnter("customSpawners");
            if (bl3) {
                this.level.doMobSpawning(this.spawnEnemies, this.spawnFriendlies);
            }

            profilerFiller.exitEnter("broadcast");
            list.forEach((chunk) -> {
                chunk.holder.broadcastChanges(chunk.chunk);
            });
            profilerFiller.exit();
            profilerFiller.exit();
            this.chunkMap.tick();
        }
    }

    private void getFullChunk(long pos, Consumer<Chunk> chunkConsumer) {
        PlayerChunk chunkHolder = this.getChunk(pos);
        if (chunkHolder != null) {
            chunkHolder.getFullChunkFuture().getNow(PlayerChunk.UNLOADED_LEVEL_CHUNK).left().ifPresent(chunkConsumer);
        }

    }

    @Override
    public String getName() {
        return Integer.toString(this.getLoadedChunksCount());
    }

    @VisibleForTesting
    public int getPendingTasksCount() {
        return this.mainThreadProcessor.getPendingTasksCount();
    }

    public ChunkGenerator getChunkGenerator() {
        return this.chunkMap.generator();
    }

    @Override
    public int getLoadedChunksCount() {
        return this.chunkMap.size();
    }

    public void flagDirty(BlockPosition pos) {
        int i = SectionPosition.blockToSectionCoord(pos.getX());
        int j = SectionPosition.blockToSectionCoord(pos.getZ());
        PlayerChunk chunkHolder = this.getChunk(ChunkCoordIntPair.pair(i, j));
        if (chunkHolder != null) {
            chunkHolder.blockChanged(pos);
        }

    }

    @Override
    public void onLightUpdate(EnumSkyBlock type, SectionPosition pos) {
        this.mainThreadProcessor.execute(() -> {
            PlayerChunk chunkHolder = this.getChunk(pos.chunk().pair());
            if (chunkHolder != null) {
                chunkHolder.sectionLightChanged(type, pos.y());
            }

        });
    }

    public <T> void addTicket(TicketType<T> ticketType, ChunkCoordIntPair pos, int radius, T argument) {
        this.distanceManager.addRegionTicket(ticketType, pos, radius, argument);
    }

    public <T> void removeTicket(TicketType<T> ticketType, ChunkCoordIntPair pos, int radius, T argument) {
        this.distanceManager.removeRegionTicket(ticketType, pos, radius, argument);
    }

    @Override
    public void updateChunkForced(ChunkCoordIntPair pos, boolean forced) {
        this.distanceManager.updateChunkForced(pos, forced);
    }

    public void movePlayer(EntityPlayer player) {
        if (player.isRemoved()) {
            LOGGER.info("Skipping update from removed player '{}'", (Object)player);
        } else {
            this.chunkMap.movePlayer(player);
        }

    }

    public void removeEntity(Entity entity) {
        this.chunkMap.removeEntity(entity);
    }

    public void addEntity(Entity entity) {
        this.chunkMap.addEntity(entity);
    }

    public void broadcastIncludingSelf(Entity entity, Packet<?> packet) {
        this.chunkMap.broadcastIncludingSelf(entity, packet);
    }

    public void broadcast(Entity entity, Packet<?> packet) {
        this.chunkMap.broadcast(entity, packet);
    }

    public void setViewDistance(int watchDistance) {
        this.chunkMap.setViewDistance(watchDistance);
    }

    public void setSimulationDistance(int simulationDistance) {
        this.distanceManager.updateSimulationDistance(simulationDistance);
    }

    @Override
    public void setSpawnSettings(boolean spawnMonsters, boolean spawnAnimals) {
        this.spawnEnemies = spawnMonsters;
        this.spawnFriendlies = spawnAnimals;
    }

    public String getChunkDebugData(ChunkCoordIntPair pos) {
        return this.chunkMap.getChunkDebugData(pos);
    }

    public WorldPersistentData getWorldPersistentData() {
        return this.dataStorage;
    }

    public VillagePlace getPoiManager() {
        return this.chunkMap.getPoiManager();
    }

    public ChunkScanAccess chunkScanner() {
        return this.chunkMap.chunkScanner();
    }

    @Nullable
    @VisibleForDebug
    public NaturalSpawner.SpawnState getLastSpawnState() {
        return this.lastSpawnState;
    }

    public final class MainThreadExecutor extends IAsyncTaskHandler<Runnable> {
        MainThreadExecutor(World world) {
            super("Chunk source main thread executor for " + world.getDimensionKey().location());
        }

        @Override
        protected Runnable postToMainThread(Runnable runnable) {
            return runnable;
        }

        @Override
        protected boolean canExecute(Runnable task) {
            return true;
        }

        @Override
        protected boolean scheduleExecutables() {
            return true;
        }

        @Override
        protected Thread getThread() {
            return ChunkProviderServer.this.mainThread;
        }

        @Override
        protected void executeTask(Runnable task) {
            ChunkProviderServer.this.level.getMethodProfiler().incrementCounter("runTask");
            super.executeTask(task);
        }

        @Override
        protected boolean executeNext() {
            if (ChunkProviderServer.this.tickDistanceManager()) {
                return true;
            } else {
                ChunkProviderServer.this.lightEngine.queueUpdate();
                return super.executeNext();
            }
        }
    }
}
