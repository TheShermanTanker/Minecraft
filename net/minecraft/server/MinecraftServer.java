package net.minecraft.server;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.datafixers.DataFixer;
import it.unimi.dsi.fastutil.longs.LongIterator;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.net.Proxy;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.SystemReport;
import net.minecraft.SystemUtils;
import net.minecraft.commands.CommandDispatcher;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.ICommandListener;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.core.RegistryMaterials;
import net.minecraft.data.worldgen.features.MiscOverworldFeatures;
import net.minecraft.gametest.framework.GameTestHarnessTicker;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.game.PacketPlayOutServerDifficulty;
import net.minecraft.network.protocol.game.PacketPlayOutUpdateTime;
import net.minecraft.network.protocol.status.ServerPing;
import net.minecraft.obfuscate.DontObfuscate;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.bossevents.BossBattleCustomData;
import net.minecraft.server.level.ChunkProviderServer;
import net.minecraft.server.level.DemoPlayerInteractManager;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.PlayerInteractManager;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.level.WorldProviderNormal;
import net.minecraft.server.level.WorldServer;
import net.minecraft.server.level.progress.WorldLoadListener;
import net.minecraft.server.level.progress.WorldLoadListenerFactory;
import net.minecraft.server.network.ITextFilter;
import net.minecraft.server.network.ServerConnection;
import net.minecraft.server.packs.repository.ResourcePackLoader;
import net.minecraft.server.packs.repository.ResourcePackRepository;
import net.minecraft.server.packs.resources.IResourceManager;
import net.minecraft.server.players.OpListEntry;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.UserCache;
import net.minecraft.server.players.WhiteList;
import net.minecraft.tags.ITagRegistry;
import net.minecraft.util.CircularTimer;
import net.minecraft.util.CryptographyException;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MinecraftEncryption;
import net.minecraft.util.ModCheck;
import net.minecraft.util.NativeModuleLister;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.GameProfilerFiller;
import net.minecraft.util.profiling.GameProfilerTick;
import net.minecraft.util.profiling.MethodProfilerResults;
import net.minecraft.util.profiling.MethodProfilerResultsEmpty;
import net.minecraft.util.profiling.MethodProfilerResultsField;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import net.minecraft.util.profiling.jfr.callback.ProfiledDuration;
import net.minecraft.util.profiling.metrics.profiling.IMetricsRecorder;
import net.minecraft.util.profiling.metrics.profiling.MetricSamplerProviderServer;
import net.minecraft.util.profiling.metrics.profiling.MetricsRecorderActive;
import net.minecraft.util.profiling.metrics.profiling.MetricsRecorderInactive;
import net.minecraft.util.profiling.metrics.storage.MetricsPersister;
import net.minecraft.util.thread.IAsyncTaskHandlerReentrant;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.village.VillageSiege;
import net.minecraft.world.entity.npc.MobSpawnerCat;
import net.minecraft.world.entity.npc.MobSpawnerTrader;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.crafting.CraftingManager;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.DataPackConfiguration;
import net.minecraft.world.level.EnumGamemode;
import net.minecraft.world.level.ForcedChunk;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.MobSpawner;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldSettings;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.border.IWorldBorderListener;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionManager;
import net.minecraft.world.level.dimension.WorldDimension;
import net.minecraft.world.level.levelgen.GeneratorSettings;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.MobSpawnerPatrol;
import net.minecraft.world.level.levelgen.MobSpawnerPhantom;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureConfigured;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureManager;
import net.minecraft.world.level.storage.Convertable;
import net.minecraft.world.level.storage.IWorldDataServer;
import net.minecraft.world.level.storage.PersistentCommandStorage;
import net.minecraft.world.level.storage.SaveData;
import net.minecraft.world.level.storage.SavedFile;
import net.minecraft.world.level.storage.SecondaryWorldData;
import net.minecraft.world.level.storage.WorldData;
import net.minecraft.world.level.storage.WorldNBTStorage;
import net.minecraft.world.level.storage.WorldPersistentData;
import net.minecraft.world.level.storage.loot.ItemModifierManager;
import net.minecraft.world.level.storage.loot.LootPredicateManager;
import net.minecraft.world.level.storage.loot.LootTableRegistry;
import net.minecraft.world.phys.Vec2F;
import net.minecraft.world.phys.Vec3D;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class MinecraftServer extends IAsyncTaskHandlerReentrant<TickTask> implements ICommandListener, AutoCloseable {
    public static final Logger LOGGER = LogManager.getLogger();
    public static final String VANILLA_BRAND = "vanilla";
    private static final float AVERAGE_TICK_TIME_SMOOTHING = 0.8F;
    private static final int TICK_STATS_SPAN = 100;
    public static final int MS_PER_TICK = 50;
    private static final int OVERLOADED_THRESHOLD = 2000;
    private static final int OVERLOADED_WARNING_INTERVAL = 15000;
    public static final String LEVEL_STORAGE_PROTOCOL = "level";
    public static final String LEVEL_STORAGE_SCHEMA = "level://";
    private static final long STATUS_EXPIRE_TIME_NS = 5000000000L;
    private static final int MAX_STATUS_PLAYER_SAMPLE = 12;
    public static final String MAP_RESOURCE_FILE = "resources.zip";
    public static final File USERID_CACHE_FILE = new File("usercache.json");
    public static final int START_CHUNK_RADIUS = 11;
    private static final int START_TICKING_CHUNK_COUNT = 441;
    private static final int AUTOSAVE_INTERVAL = 6000;
    private static final int MAX_TICK_LATENCY = 3;
    public static final int ABSOLUTE_MAX_WORLD_SIZE = 29999984;
    public static final WorldSettings DEMO_SETTINGS = new WorldSettings("Demo World", EnumGamemode.SURVIVAL, false, EnumDifficulty.NORMAL, false, new GameRules(), DataPackConfiguration.DEFAULT);
    private static final long DELAYED_TASKS_TICK_EXTENSION = 50L;
    public static final GameProfile ANONYMOUS_PLAYER_PROFILE = new GameProfile(SystemUtils.NIL_UUID, "Anonymous Player");
    public Convertable.ConversionSession storageSource;
    public final WorldNBTStorage playerDataStorage;
    private final List<Runnable> tickables = Lists.newArrayList();
    private IMetricsRecorder metricsRecorder = MetricsRecorderInactive.INSTANCE;
    private GameProfilerFiller profiler = this.metricsRecorder.getProfiler();
    private Consumer<MethodProfilerResults> onMetricsRecordingStopped = (profileResults) -> {
        this.stopRecordingMetrics();
    };
    private Consumer<Path> onMetricsRecordingFinished = (path) -> {
    };
    private boolean willStartRecordingMetrics;
    @Nullable
    private MinecraftServer.TimeProfiler debugCommandProfiler;
    private boolean debugCommandProfilerDelayStart;
    private ServerConnection connection;
    public final WorldLoadListenerFactory progressListenerFactory;
    private final ServerPing status = new ServerPing();
    private final Random random = new Random();
    public final DataFixer fixerUpper;
    private String localIp;
    private int port = -1;
    public final IRegistryCustom.Dimension registryHolder;
    public final Map<ResourceKey<World>, WorldServer> levels = Maps.newLinkedHashMap();
    private PlayerList playerList;
    private volatile boolean running = true;
    private boolean stopped;
    private int tickCount;
    protected final Proxy proxy;
    private boolean onlineMode;
    private boolean preventProxyConnections;
    private boolean pvp;
    private boolean allowFlight;
    @Nullable
    private String motd;
    private int playerIdleTimeout;
    public final long[] tickTimes = new long[100];
    @Nullable
    private KeyPair keyPair;
    @Nullable
    private String singleplayerName;
    private boolean isDemo;
    private String resourcePack = "";
    private String resourcePackHash = "";
    private volatile boolean isReady;
    private long lastOverloadWarning;
    private final MinecraftSessionService sessionService;
    @Nullable
    private final GameProfileRepository profileRepository;
    @Nullable
    private final UserCache profileCache;
    private long lastServerStatus;
    public final Thread serverThread;
    private long nextTickTime = SystemUtils.getMonotonicMillis();
    private long delayedTasksMaxNextTickTime;
    private boolean mayHaveDelayedTasks;
    private final ResourcePackRepository packRepository;
    private final ScoreboardServer scoreboard = new ScoreboardServer(this);
    @Nullable
    private PersistentCommandStorage commandStorage;
    private final BossBattleCustomData customBossEvents = new BossBattleCustomData();
    private final CustomFunctionData functionManager;
    private final CircularTimer frameTimer = new CircularTimer();
    private boolean enforceWhitelist;
    private float averageTickTime;
    public final Executor executor;
    @Nullable
    private String serverId;
    public DataPackResources resources;
    private final DefinedStructureManager structureManager;
    protected SaveData worldData;
    private volatile boolean isSaving;

    public static <S extends MinecraftServer> S spin(Function<Thread, S> serverFactory) {
        AtomicReference<S> atomicReference = new AtomicReference<>();
        Thread thread = new Thread(() -> {
            atomicReference.get().runServer();
        }, "Server thread");
        thread.setUncaughtExceptionHandler((threadx, throwable) -> {
            LOGGER.error(throwable);
        });
        if (Runtime.getRuntime().availableProcessors() > 4) {
            thread.setPriority(8);
        }

        S minecraftServer = serverFactory.apply(thread);
        atomicReference.set(minecraftServer);
        thread.start();
        return minecraftServer;
    }

    public MinecraftServer(Thread serverThread, IRegistryCustom.Dimension registryManager, Convertable.ConversionSession session, SaveData saveProperties, ResourcePackRepository dataPackManager, Proxy proxy, DataFixer dataFixer, DataPackResources serverResourceManager, @Nullable MinecraftSessionService sessionService, @Nullable GameProfileRepository gameProfileRepo, @Nullable UserCache userCache, WorldLoadListenerFactory worldGenerationProgressListenerFactory) {
        super("Server");
        this.registryHolder = registryManager;
        this.worldData = saveProperties;
        this.proxy = proxy;
        this.packRepository = dataPackManager;
        this.resources = serverResourceManager;
        this.sessionService = sessionService;
        this.profileRepository = gameProfileRepo;
        this.profileCache = userCache;
        if (userCache != null) {
            userCache.setExecutor(this);
        }

        this.connection = new ServerConnection(this);
        this.progressListenerFactory = worldGenerationProgressListenerFactory;
        this.storageSource = session;
        this.playerDataStorage = session.createPlayerStorage();
        this.fixerUpper = dataFixer;
        this.functionManager = new CustomFunctionData(this, serverResourceManager.getFunctionLibrary());
        this.structureManager = new DefinedStructureManager(serverResourceManager.getResourceManager(), session, dataFixer);
        this.serverThread = serverThread;
        this.executor = SystemUtils.backgroundExecutor();
    }

    private void initializeScoreboards(WorldPersistentData persistentStateManager) {
        persistentStateManager.computeIfAbsent(this.getScoreboard()::createData, this.getScoreboard()::createData, "scoreboard");
    }

    protected abstract boolean init() throws IOException;

    protected void loadWorld() {
        if (!JvmProfiler.INSTANCE.isRunning()) {
        }

        boolean bl = false;
        ProfiledDuration profiledDuration = JvmProfiler.INSTANCE.onWorldLoadedStarted();
        this.loadResourcesZip();
        this.worldData.setModdedInfo(this.getServerModName(), this.getModdedStatus().shouldReportAsModified());
        WorldLoadListener chunkProgressListener = this.progressListenerFactory.create(11);
        this.createLevels(chunkProgressListener);
        this.updateWorldSettings();
        this.loadSpawn(chunkProgressListener);
        if (profiledDuration != null) {
            profiledDuration.finish();
        }

        if (bl) {
            try {
                JvmProfiler.INSTANCE.stop();
            } catch (Throwable var5) {
                LOGGER.warn("Failed to stop JFR profiling", var5);
            }
        }

    }

    protected void updateWorldSettings() {
    }

    protected void createLevels(WorldLoadListener worldGenerationProgressListener) {
        IWorldDataServer serverLevelData = this.worldData.overworldData();
        GeneratorSettings worldGenSettings = this.worldData.getGeneratorSettings();
        boolean bl = worldGenSettings.isDebugWorld();
        long l = worldGenSettings.getSeed();
        long m = BiomeManager.obfuscateSeed(l);
        List<MobSpawner> list = ImmutableList.of(new MobSpawnerPhantom(), new MobSpawnerPatrol(), new MobSpawnerCat(), new VillageSiege(), new MobSpawnerTrader(serverLevelData));
        RegistryMaterials<WorldDimension> mappedRegistry = worldGenSettings.dimensions();
        WorldDimension levelStem = mappedRegistry.get(WorldDimension.OVERWORLD);
        ChunkGenerator chunkGenerator;
        DimensionManager dimensionType;
        if (levelStem == null) {
            dimensionType = this.registryHolder.<DimensionManager>registryOrThrow(IRegistry.DIMENSION_TYPE_REGISTRY).getOrThrow(DimensionManager.OVERWORLD_LOCATION);
            chunkGenerator = GeneratorSettings.makeDefaultOverworld(this.registryHolder, (new Random()).nextLong());
        } else {
            dimensionType = levelStem.type();
            chunkGenerator = levelStem.generator();
        }

        WorldServer serverLevel = new WorldServer(this, this.executor, this.storageSource, serverLevelData, World.OVERWORLD, dimensionType, worldGenerationProgressListener, chunkGenerator, bl, m, list, true);
        this.levels.put(World.OVERWORLD, serverLevel);
        WorldPersistentData dimensionDataStorage = serverLevel.getWorldPersistentData();
        this.initializeScoreboards(dimensionDataStorage);
        this.commandStorage = new PersistentCommandStorage(dimensionDataStorage);
        WorldBorder worldBorder = serverLevel.getWorldBorder();
        if (!serverLevelData.isInitialized()) {
            try {
                setInitialSpawn(serverLevel, serverLevelData, worldGenSettings.generateBonusChest(), bl);
                serverLevelData.setInitialized(true);
                if (bl) {
                    this.setupDebugLevel(this.worldData);
                }
            } catch (Throwable var26) {
                CrashReport crashReport = CrashReport.forThrowable(var26, "Exception initializing level");

                try {
                    serverLevel.fillReportDetails(crashReport);
                } catch (Throwable var25) {
                }

                throw new ReportedException(crashReport);
            }

            serverLevelData.setInitialized(true);
        }

        this.getPlayerList().addWorldborderListener(serverLevel);
        if (this.worldData.getCustomBossEvents() != null) {
            this.getBossBattleCustomData().load(this.worldData.getCustomBossEvents());
        }

        for(Entry<ResourceKey<WorldDimension>, WorldDimension> entry : mappedRegistry.entrySet()) {
            ResourceKey<WorldDimension> resourceKey = entry.getKey();
            if (resourceKey != WorldDimension.OVERWORLD) {
                ResourceKey<World> resourceKey2 = ResourceKey.create(IRegistry.DIMENSION_REGISTRY, resourceKey.location());
                DimensionManager dimensionType3 = entry.getValue().type();
                ChunkGenerator chunkGenerator3 = entry.getValue().generator();
                SecondaryWorldData derivedLevelData = new SecondaryWorldData(this.worldData, serverLevelData);
                WorldServer serverLevel2 = new WorldServer(this, this.executor, this.storageSource, derivedLevelData, resourceKey2, dimensionType3, worldGenerationProgressListener, chunkGenerator3, bl, m, ImmutableList.of(), false);
                worldBorder.addListener(new IWorldBorderListener.DelegateBorderChangeListener(serverLevel2.getWorldBorder()));
                this.levels.put(resourceKey2, serverLevel2);
            }
        }

        worldBorder.applySettings(serverLevelData.getWorldBorder());
    }

    private static void setInitialSpawn(WorldServer world, IWorldDataServer worldProperties, boolean bonusChest, boolean debugWorld) {
        if (debugWorld) {
            worldProperties.setSpawn(BlockPosition.ZERO.above(80), 0.0F);
        } else {
            ChunkGenerator chunkGenerator = world.getChunkSource().getChunkGenerator();
            ChunkCoordIntPair chunkPos = new ChunkCoordIntPair(chunkGenerator.climateSampler().findSpawnPosition());
            int i = chunkGenerator.getSpawnHeight(world);
            if (i < world.getMinBuildHeight()) {
                BlockPosition blockPos = chunkPos.getWorldPosition();
                i = world.getHeight(HeightMap.Type.WORLD_SURFACE, blockPos.getX() + 8, blockPos.getZ() + 8);
            }

            worldProperties.setSpawn(chunkPos.getWorldPosition().offset(8, i, 8), 0.0F);
            int j = 0;
            int k = 0;
            int l = 0;
            int m = -1;
            int n = 5;

            for(int o = 0; o < MathHelper.square(11); ++o) {
                if (j >= -5 && j <= 5 && k >= -5 && k <= 5) {
                    BlockPosition blockPos2 = WorldProviderNormal.getSpawnPosInChunk(world, new ChunkCoordIntPair(chunkPos.x + j, chunkPos.z + k));
                    if (blockPos2 != null) {
                        worldProperties.setSpawn(blockPos2, 0.0F);
                        break;
                    }
                }

                if (j == k || j < 0 && j == -k || j > 0 && j == 1 - k) {
                    int p = l;
                    l = -m;
                    m = p;
                }

                j += l;
                k += m;
            }

            if (bonusChest) {
                WorldGenFeatureConfigured<?, ?> configuredFeature = MiscOverworldFeatures.BONUS_CHEST;
                configuredFeature.place(world, chunkGenerator, world.random, new BlockPosition(worldProperties.getXSpawn(), worldProperties.getYSpawn(), worldProperties.getZSpawn()));
            }

        }
    }

    private void setupDebugLevel(SaveData properties) {
        properties.setDifficulty(EnumDifficulty.PEACEFUL);
        properties.setDifficultyLocked(true);
        IWorldDataServer serverLevelData = properties.overworldData();
        serverLevelData.setStorm(false);
        serverLevelData.setThundering(false);
        serverLevelData.setClearWeatherTime(1000000000);
        serverLevelData.setDayTime(6000L);
        serverLevelData.setGameType(EnumGamemode.SPECTATOR);
    }

    public void loadSpawn(WorldLoadListener worldGenerationProgressListener) {
        WorldServer serverLevel = this.overworld();
        LOGGER.info("Preparing start region for dimension {}", (Object)serverLevel.getDimensionKey().location());
        BlockPosition blockPos = serverLevel.getSpawn();
        worldGenerationProgressListener.updateSpawnPos(new ChunkCoordIntPair(blockPos));
        ChunkProviderServer serverChunkCache = serverLevel.getChunkSource();
        serverChunkCache.getLightEngine().setTaskPerBatch(500);
        this.nextTickTime = SystemUtils.getMonotonicMillis();
        serverChunkCache.addTicket(TicketType.START, new ChunkCoordIntPair(blockPos), 11, Unit.INSTANCE);

        while(serverChunkCache.getTickingGenerated() != 441) {
            this.nextTickTime = SystemUtils.getMonotonicMillis() + 10L;
            this.sleepForTick();
        }

        this.nextTickTime = SystemUtils.getMonotonicMillis() + 10L;
        this.sleepForTick();

        for(WorldServer serverLevel2 : this.levels.values()) {
            ForcedChunk forcedChunksSavedData = serverLevel2.getWorldPersistentData().get(ForcedChunk::load, "chunks");
            if (forcedChunksSavedData != null) {
                LongIterator longIterator = forcedChunksSavedData.getChunks().iterator();

                while(longIterator.hasNext()) {
                    long l = longIterator.nextLong();
                    ChunkCoordIntPair chunkPos = new ChunkCoordIntPair(l);
                    serverLevel2.getChunkSource().updateChunkForced(chunkPos, true);
                }
            }
        }

        this.nextTickTime = SystemUtils.getMonotonicMillis() + 10L;
        this.sleepForTick();
        worldGenerationProgressListener.stop();
        serverChunkCache.getLightEngine().setTaskPerBatch(5);
        this.updateSpawnFlags();
    }

    protected void loadResourcesZip() {
        File file = this.storageSource.getWorldFolder(SavedFile.MAP_RESOURCE_FILE).toFile();
        if (file.isFile()) {
            String string = this.storageSource.getLevelName();

            try {
                this.setResourcePack("level://" + URLEncoder.encode(string, StandardCharsets.UTF_8.toString()) + "/resources.zip", "");
            } catch (UnsupportedEncodingException var4) {
                LOGGER.warn("Something went wrong url encoding {}", (Object)string);
            }
        }

    }

    public EnumGamemode getGamemode() {
        return this.worldData.getGameType();
    }

    public boolean isHardcore() {
        return this.worldData.isHardcore();
    }

    public abstract int getOperatorUserPermissionLevel();

    public abstract int getFunctionCompilationLevel();

    public abstract boolean shouldRconBroadcast();

    public boolean saveChunks(boolean suppressLogs, boolean flush, boolean force) {
        boolean bl = false;

        for(WorldServer serverLevel : this.getWorlds()) {
            if (!suppressLogs) {
                LOGGER.info("Saving chunks for level '{}'/{}", serverLevel, serverLevel.getDimensionKey().location());
            }

            serverLevel.save((IProgressUpdate)null, flush, serverLevel.noSave && !force);
            bl = true;
        }

        WorldServer serverLevel2 = this.overworld();
        IWorldDataServer serverLevelData = this.worldData.overworldData();
        serverLevelData.setWorldBorder(serverLevel2.getWorldBorder().createSettings());
        this.worldData.setCustomBossEvents(this.getBossBattleCustomData().save());
        this.storageSource.saveDataTag(this.registryHolder, this.worldData, this.getPlayerList().save());
        if (flush) {
            for(WorldServer serverLevel3 : this.getWorlds()) {
                LOGGER.info("ThreadedAnvilChunkStorage ({}): All chunks are saved", (Object)serverLevel3.getChunkSource().chunkMap.getStorageName());
            }

            LOGGER.info("ThreadedAnvilChunkStorage: All dimensions are saved");
        }

        return bl;
    }

    public boolean saveEverything(boolean suppressLogs, boolean flush, boolean force) {
        boolean var4;
        try {
            this.isSaving = true;
            this.getPlayerList().savePlayers();
            var4 = this.saveChunks(suppressLogs, flush, force);
        } finally {
            this.isSaving = false;
        }

        return var4;
    }

    @Override
    public void close() {
        this.stop();
    }

    public void stop() {
        LOGGER.info("Stopping server");
        if (this.getServerConnection() != null) {
            this.getServerConnection().stop();
        }

        this.isSaving = true;
        if (this.playerList != null) {
            LOGGER.info("Saving players");
            this.playerList.savePlayers();
            this.playerList.shutdown();
        }

        LOGGER.info("Saving worlds");

        for(WorldServer serverLevel : this.getWorlds()) {
            if (serverLevel != null) {
                serverLevel.noSave = false;
            }
        }

        this.saveChunks(false, true, false);

        for(WorldServer serverLevel2 : this.getWorlds()) {
            if (serverLevel2 != null) {
                try {
                    serverLevel2.close();
                } catch (IOException var5) {
                    LOGGER.error("Exception closing the level", (Throwable)var5);
                }
            }
        }

        this.isSaving = false;
        this.resources.close();

        try {
            this.storageSource.close();
        } catch (IOException var4) {
            LOGGER.error("Failed to unlock level {}", this.storageSource.getLevelName(), var4);
        }

    }

    public String getServerIp() {
        return this.localIp;
    }

    public void setLocalIp(String serverIp) {
        this.localIp = serverIp;
    }

    public boolean isRunning() {
        return this.running;
    }

    public void safeShutdown(boolean bl) {
        this.running = false;
        if (bl) {
            try {
                this.serverThread.join();
            } catch (InterruptedException var3) {
                LOGGER.error("Error while shutting down", (Throwable)var3);
            }
        }

    }

    protected void runServer() {
        try {
            if (this.init()) {
                this.nextTickTime = SystemUtils.getMonotonicMillis();
                this.status.setMOTD(new ChatComponentText(this.motd));
                this.status.setServerInfo(new ServerPing.ServerData(SharedConstants.getCurrentVersion().getName(), SharedConstants.getCurrentVersion().getProtocolVersion()));
                this.updateStatusIcon(this.status);

                while(this.running) {
                    long l = SystemUtils.getMonotonicMillis() - this.nextTickTime;
                    if (l > 2000L && this.nextTickTime - this.lastOverloadWarning >= 15000L) {
                        long m = l / 50L;
                        LOGGER.warn("Can't keep up! Is the server overloaded? Running {}ms or {} ticks behind", l, m);
                        this.nextTickTime += m * 50L;
                        this.lastOverloadWarning = this.nextTickTime;
                    }

                    if (this.debugCommandProfilerDelayStart) {
                        this.debugCommandProfilerDelayStart = false;
                        this.debugCommandProfiler = new MinecraftServer.TimeProfiler(SystemUtils.getMonotonicNanos(), this.tickCount);
                    }

                    this.nextTickTime += 50L;
                    this.startMetricsRecordingTick();
                    this.profiler.enter("tick");
                    this.tickServer(this::canSleepForTick);
                    this.profiler.exitEnter("nextTickWait");
                    this.mayHaveDelayedTasks = true;
                    this.delayedTasksMaxNextTickTime = Math.max(SystemUtils.getMonotonicMillis() + 50L, this.nextTickTime);
                    this.sleepForTick();
                    this.profiler.exit();
                    this.endMetricsRecordingTick();
                    this.isReady = true;
                    JvmProfiler.INSTANCE.onServerTick(this.averageTickTime);
                }
            } else {
                this.onServerCrash((CrashReport)null);
            }
        } catch (Throwable var44) {
            LOGGER.error("Encountered an unexpected exception", var44);
            CrashReport crashReport;
            if (var44 instanceof ReportedException) {
                crashReport = ((ReportedException)var44).getReport();
            } else {
                crashReport = new CrashReport("Exception in server tick loop", var44);
            }

            this.fillSystemReport(crashReport.getSystemReport());
            File file = new File(new File(this.getServerDirectory(), "crash-reports"), "crash-" + (new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss")).format(new Date()) + "-server.txt");
            if (crashReport.saveToFile(file)) {
                LOGGER.error("This crash report has been saved to: {}", (Object)file.getAbsolutePath());
            } else {
                LOGGER.error("We were unable to save this crash report to disk.");
            }

            this.onServerCrash(crashReport);
        } finally {
            try {
                this.stopped = true;
                this.stop();
            } catch (Throwable var42) {
                LOGGER.error("Exception stopping the server", var42);
            } finally {
                if (this.profileCache != null) {
                    this.profileCache.clearExecutor();
                }

                this.exit();
            }

        }

    }

    private boolean canSleepForTick() {
        return this.isEntered() || SystemUtils.getMonotonicMillis() < (this.mayHaveDelayedTasks ? this.delayedTasksMaxNextTickTime : this.nextTickTime);
    }

    protected void sleepForTick() {
        this.executeAll();
        this.awaitTasks(() -> {
            return !this.canSleepForTick();
        });
    }

    @Override
    public TickTask wrapRunnable(Runnable runnable) {
        return new TickTask(this.tickCount, runnable);
    }

    @Override
    protected boolean shouldRun(TickTask tickTask) {
        return tickTask.getTick() + 3 < this.tickCount || this.canSleepForTick();
    }

    @Override
    public boolean executeNext() {
        boolean bl = this.pollTaskInternal();
        this.mayHaveDelayedTasks = bl;
        return bl;
    }

    private boolean pollTaskInternal() {
        if (super.executeNext()) {
            return true;
        } else {
            if (this.canSleepForTick()) {
                for(WorldServer serverLevel : this.getWorlds()) {
                    if (serverLevel.getChunkSource().runTasks()) {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    @Override
    public void doRunTask(TickTask tickTask) {
        this.getMethodProfiler().incrementCounter("runTask");
        super.executeTask(tickTask);
    }

    private void updateStatusIcon(ServerPing metadata) {
        Optional<File> optional = Optional.of(this.getFile("server-icon.png")).filter(File::isFile);
        if (!optional.isPresent()) {
            optional = this.storageSource.getIconFile().map(Path::toFile).filter(File::isFile);
        }

        optional.ifPresent((file) -> {
            try {
                BufferedImage bufferedImage = ImageIO.read(file);
                Validate.validState(bufferedImage.getWidth() == 64, "Must be 64 pixels wide");
                Validate.validState(bufferedImage.getHeight() == 64, "Must be 64 pixels high");
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                ImageIO.write(bufferedImage, "PNG", byteArrayOutputStream);
                byte[] bs = Base64.getEncoder().encode(byteArrayOutputStream.toByteArray());
                metadata.setFavicon("data:image/png;base64," + new String(bs, StandardCharsets.UTF_8));
            } catch (Exception var5) {
                LOGGER.error("Couldn't load server icon", (Throwable)var5);
            }

        });
    }

    public Optional<Path> getWorldScreenshotFile() {
        return this.storageSource.getIconFile();
    }

    public File getServerDirectory() {
        return new File(".");
    }

    protected void onServerCrash(CrashReport report) {
    }

    public void exit() {
    }

    public void tickServer(BooleanSupplier shouldKeepTicking) {
        long l = SystemUtils.getMonotonicNanos();
        ++this.tickCount;
        this.tickChildren(shouldKeepTicking);
        if (l - this.lastServerStatus >= 5000000000L) {
            this.lastServerStatus = l;
            this.status.setPlayerSample(new ServerPing.ServerPingPlayerSample(this.getMaxPlayers(), this.getPlayerCount()));
            if (!this.hidesOnlinePlayers()) {
                GameProfile[] gameProfiles = new GameProfile[Math.min(this.getPlayerCount(), 12)];
                int i = MathHelper.nextInt(this.random, 0, this.getPlayerCount() - gameProfiles.length);

                for(int j = 0; j < gameProfiles.length; ++j) {
                    EntityPlayer serverPlayer = this.playerList.getPlayers().get(i + j);
                    if (serverPlayer.allowsListing()) {
                        gameProfiles[j] = serverPlayer.getProfile();
                    } else {
                        gameProfiles[j] = ANONYMOUS_PLAYER_PROFILE;
                    }
                }

                Collections.shuffle(Arrays.asList(gameProfiles));
                this.status.getPlayers().setSample(gameProfiles);
            }
        }

        if (this.tickCount % 6000 == 0) {
            LOGGER.debug("Autosave started");
            this.profiler.enter("save");
            this.saveEverything(true, false, false);
            this.profiler.exit();
            LOGGER.debug("Autosave finished");
        }

        this.profiler.enter("tallying");
        long m = this.tickTimes[this.tickCount % 100] = SystemUtils.getMonotonicNanos() - l;
        this.averageTickTime = this.averageTickTime * 0.8F + (float)m / 1000000.0F * 0.19999999F;
        long n = SystemUtils.getMonotonicNanos();
        this.frameTimer.logFrameDuration(n - l);
        this.profiler.exit();
    }

    public void tickChildren(BooleanSupplier shouldKeepTicking) {
        this.profiler.enter("commandFunctions");
        this.getFunctionData().tick();
        this.profiler.exitEnter("levels");

        for(WorldServer serverLevel : this.getWorlds()) {
            this.profiler.push(() -> {
                return serverLevel + " " + serverLevel.getDimensionKey().location();
            });
            if (this.tickCount % 20 == 0) {
                this.profiler.enter("timeSync");
                this.playerList.broadcastAll(new PacketPlayOutUpdateTime(serverLevel.getTime(), serverLevel.getDayTime(), serverLevel.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)), serverLevel.getDimensionKey());
                this.profiler.exit();
            }

            this.profiler.enter("tick");

            try {
                serverLevel.doTick(shouldKeepTicking);
            } catch (Throwable var6) {
                CrashReport crashReport = CrashReport.forThrowable(var6, "Exception ticking world");
                serverLevel.fillReportDetails(crashReport);
                throw new ReportedException(crashReport);
            }

            this.profiler.exit();
            this.profiler.exit();
        }

        this.profiler.exitEnter("connection");
        this.getServerConnection().tick();
        this.profiler.exitEnter("players");
        this.playerList.tick();
        if (SharedConstants.IS_RUNNING_IN_IDE) {
            GameTestHarnessTicker.SINGLETON.tick();
        }

        this.profiler.exitEnter("server gui refresh");

        for(int i = 0; i < this.tickables.size(); ++i) {
            this.tickables.get(i).run();
        }

        this.profiler.exit();
    }

    public boolean getAllowNether() {
        return true;
    }

    public void addTickable(Runnable tickable) {
        this.tickables.add(tickable);
    }

    protected void setId(String serverId) {
        this.serverId = serverId;
    }

    public boolean isShutdown() {
        return !this.serverThread.isAlive();
    }

    public File getFile(String path) {
        return new File(this.getServerDirectory(), path);
    }

    public final WorldServer overworld() {
        return this.levels.get(World.OVERWORLD);
    }

    @Nullable
    public WorldServer getWorldServer(ResourceKey<World> key) {
        return this.levels.get(key);
    }

    public Set<ResourceKey<World>> levelKeys() {
        return this.levels.keySet();
    }

    public Iterable<WorldServer> getWorlds() {
        return this.levels.values();
    }

    public String getVersion() {
        return SharedConstants.getCurrentVersion().getName();
    }

    public int getPlayerCount() {
        return this.playerList.getPlayerCount();
    }

    public int getMaxPlayers() {
        return this.playerList.getMaxPlayers();
    }

    public String[] getPlayers() {
        return this.playerList.getPlayerNamesArray();
    }

    @DontObfuscate
    public String getServerModName() {
        return "vanilla";
    }

    public SystemReport fillSystemReport(SystemReport details) {
        details.setDetail("Server Running", () -> {
            return Boolean.toString(this.running);
        });
        if (this.playerList != null) {
            details.setDetail("Player Count", () -> {
                return this.playerList.getPlayerCount() + " / " + this.playerList.getMaxPlayers() + "; " + this.playerList.getPlayers();
            });
        }

        details.setDetail("Data Packs", () -> {
            StringBuilder stringBuilder = new StringBuilder();

            for(ResourcePackLoader pack : this.packRepository.getSelectedPacks()) {
                if (stringBuilder.length() > 0) {
                    stringBuilder.append(", ");
                }

                stringBuilder.append(pack.getId());
                if (!pack.getCompatibility().isCompatible()) {
                    stringBuilder.append(" (incompatible)");
                }
            }

            return stringBuilder.toString();
        });
        if (this.serverId != null) {
            details.setDetail("Server Id", () -> {
                return this.serverId;
            });
        }

        return this.fillServerSystemReport(details);
    }

    public abstract SystemReport fillServerSystemReport(SystemReport details);

    public ModCheck getModdedStatus() {
        return ModCheck.identify("vanilla", this::getServerModName, "Server", MinecraftServer.class);
    }

    @Override
    public void sendMessage(IChatBaseComponent message, UUID sender) {
        LOGGER.info(message.getString());
    }

    public KeyPair getKeyPair() {
        return this.keyPair;
    }

    public int getPort() {
        return this.port;
    }

    public void setPort(int serverPort) {
        this.port = serverPort;
    }

    public String getSinglePlayerName() {
        return this.singleplayerName;
    }

    public void setSingleplayerName(String singlePlayerName) {
        this.singleplayerName = singlePlayerName;
    }

    public boolean isEmbeddedServer() {
        return this.singleplayerName != null;
    }

    protected void initializeKeyPair() {
        LOGGER.info("Generating keypair");

        try {
            this.keyPair = MinecraftEncryption.generateKeyPair();
        } catch (CryptographyException var2) {
            throw new IllegalStateException("Failed to generate key pair", var2);
        }
    }

    public void setDifficulty(EnumDifficulty difficulty, boolean forceUpdate) {
        if (forceUpdate || !this.worldData.isDifficultyLocked()) {
            this.worldData.setDifficulty(this.worldData.isHardcore() ? EnumDifficulty.HARD : difficulty);
            this.updateSpawnFlags();
            this.getPlayerList().getPlayers().forEach(this::sendDifficultyUpdate);
        }
    }

    public int getScaledTrackingDistance(int initialDistance) {
        return initialDistance;
    }

    private void updateSpawnFlags() {
        for(WorldServer serverLevel : this.getWorlds()) {
            serverLevel.setSpawnFlags(this.getSpawnMonsters(), this.getSpawnAnimals());
        }

    }

    public void setDifficultyLocked(boolean locked) {
        this.worldData.setDifficultyLocked(locked);
        this.getPlayerList().getPlayers().forEach(this::sendDifficultyUpdate);
    }

    private void sendDifficultyUpdate(EntityPlayer player) {
        WorldData levelData = player.getWorldServer().getWorldData();
        player.connection.sendPacket(new PacketPlayOutServerDifficulty(levelData.getDifficulty(), levelData.isDifficultyLocked()));
    }

    public boolean getSpawnMonsters() {
        return this.worldData.getDifficulty() != EnumDifficulty.PEACEFUL;
    }

    public boolean isDemoMode() {
        return this.isDemo;
    }

    public void setDemo(boolean demo) {
        this.isDemo = demo;
    }

    public String getResourcePack() {
        return this.resourcePack;
    }

    public String getResourcePackHash() {
        return this.resourcePackHash;
    }

    public void setResourcePack(String url, String hash) {
        this.resourcePack = url;
        this.resourcePackHash = hash;
    }

    public abstract boolean isDedicatedServer();

    public abstract int getRateLimitPacketsPerSecond();

    public boolean getOnlineMode() {
        return this.onlineMode;
    }

    public void setOnlineMode(boolean onlineMode) {
        this.onlineMode = onlineMode;
    }

    public boolean getPreventProxyConnections() {
        return this.preventProxyConnections;
    }

    public void setPreventProxyConnections(boolean preventProxyConnections) {
        this.preventProxyConnections = preventProxyConnections;
    }

    public boolean getSpawnAnimals() {
        return true;
    }

    public boolean getSpawnNPCs() {
        return true;
    }

    public abstract boolean isEpollEnabled();

    public boolean getPVP() {
        return this.pvp;
    }

    public void setPVP(boolean pvpEnabled) {
        this.pvp = pvpEnabled;
    }

    public boolean getAllowFlight() {
        return this.allowFlight;
    }

    public void setAllowFlight(boolean flightEnabled) {
        this.allowFlight = flightEnabled;
    }

    public abstract boolean getEnableCommandBlock();

    public String getMotd() {
        return this.motd;
    }

    public void setMotd(String motd) {
        this.motd = motd;
    }

    public boolean isStopped() {
        return this.stopped;
    }

    public PlayerList getPlayerList() {
        return this.playerList;
    }

    public void setPlayerList(PlayerList playerManager) {
        this.playerList = playerManager;
    }

    public abstract boolean isPublished();

    public void setDefaultGameType(EnumGamemode gameMode) {
        this.worldData.setGameType(gameMode);
    }

    @Nullable
    public ServerConnection getServerConnection() {
        return this.connection;
    }

    public boolean isReady() {
        return this.isReady;
    }

    public boolean hasGui() {
        return false;
    }

    public boolean publishServer(@Nullable EnumGamemode gameMode, boolean cheatsAllowed, int port) {
        return false;
    }

    public int getTickCount() {
        return this.tickCount;
    }

    public int getSpawnProtection() {
        return 16;
    }

    public boolean isUnderSpawnProtection(WorldServer world, BlockPosition pos, EntityHuman player) {
        return false;
    }

    public boolean repliesToStatus() {
        return true;
    }

    public boolean hidesOnlinePlayers() {
        return false;
    }

    public Proxy getProxy() {
        return this.proxy;
    }

    public int getIdleTimeout() {
        return this.playerIdleTimeout;
    }

    public void setIdleTimeout(int playerIdleTimeout) {
        this.playerIdleTimeout = playerIdleTimeout;
    }

    public MinecraftSessionService getMinecraftSessionService() {
        return this.sessionService;
    }

    public GameProfileRepository getGameProfileRepository() {
        return this.profileRepository;
    }

    public UserCache getUserCache() {
        return this.profileCache;
    }

    public ServerPing getServerPing() {
        return this.status;
    }

    public void invalidatePingSample() {
        this.lastServerStatus = 0L;
    }

    public int getAbsoluteMaxWorldSize() {
        return 29999984;
    }

    @Override
    public boolean scheduleExecutables() {
        return super.scheduleExecutables() && !this.isStopped();
    }

    @Override
    public Thread getThread() {
        return this.serverThread;
    }

    public int getCompressionThreshold() {
        return 256;
    }

    public long getNextTickTime() {
        return this.nextTickTime;
    }

    public DataFixer getDataFixer() {
        return this.fixerUpper;
    }

    public int getSpawnRadius(@Nullable WorldServer world) {
        return world != null ? world.getGameRules().getInt(GameRules.RULE_SPAWN_RADIUS) : 10;
    }

    public AdvancementDataWorld getAdvancementData() {
        return this.resources.getAdvancements();
    }

    public CustomFunctionData getFunctionData() {
        return this.functionManager;
    }

    public CompletableFuture<Void> reloadResources(Collection<String> datapacks) {
        CompletableFuture<Void> completableFuture = CompletableFuture.supplyAsync(() -> {
            return datapacks.stream().map(this.packRepository::getPack).filter(Objects::nonNull).map(ResourcePackLoader::open).collect(ImmutableList.toImmutableList());
        }, this).thenCompose((immutableList) -> {
            return DataPackResources.loadResources(immutableList, this.registryHolder, this.isDedicatedServer() ? CommandDispatcher.ServerType.DEDICATED : CommandDispatcher.ServerType.INTEGRATED, this.getFunctionCompilationLevel(), this.executor, this);
        }).thenAcceptAsync((serverResourceManager) -> {
            this.resources.close();
            this.resources = serverResourceManager;
            this.packRepository.setSelected(datapacks);
            this.worldData.setDataPackConfig(getSelectedPacks(this.packRepository));
            serverResourceManager.updateGlobals();
            this.getPlayerList().savePlayers();
            this.getPlayerList().reload();
            this.functionManager.replaceLibrary(this.resources.getFunctionLibrary());
            this.structureManager.onResourceManagerReload(this.resources.getResourceManager());
        }, this);
        if (this.isMainThread()) {
            this.awaitTasks(completableFuture::isDone);
        }

        return completableFuture;
    }

    public static DataPackConfiguration configurePackRepository(ResourcePackRepository resourcePackManager, DataPackConfiguration dataPackSettings, boolean safeMode) {
        resourcePackManager.reload();
        if (safeMode) {
            resourcePackManager.setSelected(Collections.singleton("vanilla"));
            return new DataPackConfiguration(ImmutableList.of("vanilla"), ImmutableList.of());
        } else {
            Set<String> set = Sets.newLinkedHashSet();

            for(String string : dataPackSettings.getEnabled()) {
                if (resourcePackManager.isAvailable(string)) {
                    set.add(string);
                } else {
                    LOGGER.warn("Missing data pack {}", (Object)string);
                }
            }

            for(ResourcePackLoader pack : resourcePackManager.getAvailablePacks()) {
                String string2 = pack.getId();
                if (!dataPackSettings.getDisabled().contains(string2) && !set.contains(string2)) {
                    LOGGER.info("Found new data pack {}, loading it automatically", (Object)string2);
                    set.add(string2);
                }
            }

            if (set.isEmpty()) {
                LOGGER.info("No datapacks selected, forcing vanilla");
                set.add("vanilla");
            }

            resourcePackManager.setSelected(set);
            return getSelectedPacks(resourcePackManager);
        }
    }

    private static DataPackConfiguration getSelectedPacks(ResourcePackRepository dataPackManager) {
        Collection<String> collection = dataPackManager.getSelectedIds();
        List<String> list = ImmutableList.copyOf(collection);
        List<String> list2 = dataPackManager.getAvailableIds().stream().filter((string) -> {
            return !collection.contains(string);
        }).collect(ImmutableList.toImmutableList());
        return new DataPackConfiguration(list, list2);
    }

    public void kickUnlistedPlayers(CommandListenerWrapper source) {
        if (this.isEnforceWhitelist()) {
            PlayerList playerList = source.getServer().getPlayerList();
            WhiteList userWhiteList = playerList.getWhitelist();

            for(EntityPlayer serverPlayer : Lists.newArrayList(playerList.getPlayers())) {
                if (!userWhiteList.isWhitelisted(serverPlayer.getProfile())) {
                    serverPlayer.connection.disconnect(new ChatMessage("multiplayer.disconnect.not_whitelisted"));
                }
            }

        }
    }

    public ResourcePackRepository getResourcePackRepository() {
        return this.packRepository;
    }

    public CommandDispatcher getCommandDispatcher() {
        return this.resources.getCommands();
    }

    public CommandListenerWrapper getServerCommandListener() {
        WorldServer serverLevel = this.overworld();
        return new CommandListenerWrapper(this, serverLevel == null ? Vec3D.ZERO : Vec3D.atLowerCornerOf(serverLevel.getSpawn()), Vec2F.ZERO, serverLevel, 4, "Server", new ChatComponentText("Server"), this, (Entity)null);
    }

    @Override
    public boolean shouldSendSuccess() {
        return true;
    }

    @Override
    public boolean shouldSendFailure() {
        return true;
    }

    @Override
    public abstract boolean shouldBroadcastCommands();

    public CraftingManager getCraftingManager() {
        return this.resources.getRecipeManager();
    }

    public ITagRegistry getTagRegistry() {
        return this.resources.getTags();
    }

    public ScoreboardServer getScoreboard() {
        return this.scoreboard;
    }

    public PersistentCommandStorage getCommandStorage() {
        if (this.commandStorage == null) {
            throw new NullPointerException("Called before server init");
        } else {
            return this.commandStorage;
        }
    }

    public LootTableRegistry getLootTableRegistry() {
        return this.resources.getLootTables();
    }

    public LootPredicateManager getLootPredicateManager() {
        return this.resources.getPredicateManager();
    }

    public ItemModifierManager getItemModifierManager() {
        return this.resources.getItemModifierManager();
    }

    public GameRules getGameRules() {
        return this.overworld().getGameRules();
    }

    public BossBattleCustomData getBossBattleCustomData() {
        return this.customBossEvents;
    }

    public boolean isEnforceWhitelist() {
        return this.enforceWhitelist;
    }

    public void setEnforceWhitelist(boolean whitelistEnabled) {
        this.enforceWhitelist = whitelistEnabled;
    }

    public float getAverageTickTime() {
        return this.averageTickTime;
    }

    public int getProfilePermissions(GameProfile profile) {
        if (this.getPlayerList().isOp(profile)) {
            OpListEntry serverOpListEntry = this.getPlayerList().getOPs().get(profile);
            if (serverOpListEntry != null) {
                return serverOpListEntry.getLevel();
            } else if (this.isSingleplayerOwner(profile)) {
                return 4;
            } else if (this.isEmbeddedServer()) {
                return this.getPlayerList().isAllowCheatsForAllPlayers() ? 4 : 0;
            } else {
                return this.getOperatorUserPermissionLevel();
            }
        } else {
            return 0;
        }
    }

    public CircularTimer getFrameTimer() {
        return this.frameTimer;
    }

    public GameProfilerFiller getMethodProfiler() {
        return this.profiler;
    }

    public abstract boolean isSingleplayerOwner(GameProfile profile);

    public void dumpServerProperties(Path file) throws IOException {
    }

    private void saveDebugReport(Path path) {
        Path path2 = path.resolve("levels");

        try {
            for(Entry<ResourceKey<World>, WorldServer> entry : this.levels.entrySet()) {
                MinecraftKey resourceLocation = entry.getKey().location();
                Path path3 = path2.resolve(resourceLocation.getNamespace()).resolve(resourceLocation.getKey());
                Files.createDirectories(path3);
                entry.getValue().saveDebugReport(path3);
            }

            this.dumpGameRules(path.resolve("gamerules.txt"));
            this.dumpClasspath(path.resolve("classpath.txt"));
            this.dumpMiscStats(path.resolve("stats.txt"));
            this.dumpThreads(path.resolve("threads.txt"));
            this.dumpServerProperties(path.resolve("server.properties.txt"));
            this.dumpNativeModules(path.resolve("modules.txt"));
        } catch (IOException var7) {
            LOGGER.warn("Failed to save debug report", (Throwable)var7);
        }

    }

    private void dumpMiscStats(Path path) throws IOException {
        Writer writer = Files.newBufferedWriter(path);

        try {
            writer.write(String.format("pending_tasks: %d\n", this.getPendingTasksCount()));
            writer.write(String.format("average_tick_time: %f\n", this.getAverageTickTime()));
            writer.write(String.format("tick_times: %s\n", Arrays.toString(this.tickTimes)));
            writer.write(String.format("queue: %s\n", SystemUtils.backgroundExecutor()));
        } catch (Throwable var6) {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Throwable var5) {
                    var6.addSuppressed(var5);
                }
            }

            throw var6;
        }

        if (writer != null) {
            writer.close();
        }

    }

    private void dumpGameRules(Path path) throws IOException {
        Writer writer = Files.newBufferedWriter(path);

        try {
            final List<String> list = Lists.newArrayList();
            final GameRules gameRules = this.getGameRules();
            GameRules.visitGameRuleTypes(new GameRules.GameRuleVisitor() {
                @Override
                public <T extends GameRules.GameRuleValue<T>> void visit(GameRules.GameRuleKey<T> key, GameRules.GameRuleDefinition<T> type) {
                    list.add(String.format("%s=%s\n", key.getId(), gameRules.<T>get(key)));
                }
            });

            for(String string : list) {
                writer.write(string);
            }
        } catch (Throwable var8) {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Throwable var7) {
                    var8.addSuppressed(var7);
                }
            }

            throw var8;
        }

        if (writer != null) {
            writer.close();
        }

    }

    private void dumpClasspath(Path path) throws IOException {
        Writer writer = Files.newBufferedWriter(path);

        try {
            String string = System.getProperty("java.class.path");
            String string2 = System.getProperty("path.separator");

            for(String string3 : Splitter.on(string2).split(string)) {
                writer.write(string3);
                writer.write("\n");
            }
        } catch (Throwable var8) {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Throwable var7) {
                    var8.addSuppressed(var7);
                }
            }

            throw var8;
        }

        if (writer != null) {
            writer.close();
        }

    }

    private void dumpThreads(Path path) throws IOException {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(true, true);
        Arrays.sort(threadInfos, Comparator.comparing(ThreadInfo::getThreadName));
        Writer writer = Files.newBufferedWriter(path);

        try {
            for(ThreadInfo threadInfo : threadInfos) {
                writer.write(threadInfo.toString());
                writer.write(10);
            }
        } catch (Throwable var10) {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Throwable var9) {
                    var10.addSuppressed(var9);
                }
            }

            throw var10;
        }

        if (writer != null) {
            writer.close();
        }

    }

    private void dumpNativeModules(Path path) throws IOException {
        Writer writer = Files.newBufferedWriter(path);

        label49: {
            try {
                label50: {
                    List<NativeModuleLister.NativeModuleInfo> list;
                    try {
                        list = Lists.newArrayList(NativeModuleLister.listModules());
                    } catch (Throwable var7) {
                        LOGGER.warn("Failed to list native modules", var7);
                        break label50;
                    }

                    list.sort(Comparator.comparing((module) -> {
                        return module.name;
                    }));
                    Iterator throwable = list.iterator();

                    while(true) {
                        if (!throwable.hasNext()) {
                            break label49;
                        }

                        NativeModuleLister.NativeModuleInfo nativeModuleInfo = (NativeModuleLister.NativeModuleInfo)throwable.next();
                        writer.write(nativeModuleInfo.toString());
                        writer.write(10);
                    }
                }
            } catch (Throwable var8) {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (Throwable var6) {
                        var8.addSuppressed(var6);
                    }
                }

                throw var8;
            }

            if (writer != null) {
                writer.close();
            }

            return;
        }

        if (writer != null) {
            writer.close();
        }

    }

    private void startMetricsRecordingTick() {
        if (this.willStartRecordingMetrics) {
            this.metricsRecorder = MetricsRecorderActive.createStarted(new MetricSamplerProviderServer(SystemUtils.timeSource, this.isDedicatedServer()), SystemUtils.timeSource, SystemUtils.ioPool(), new MetricsPersister("server"), this.onMetricsRecordingStopped, (path) -> {
                this.executeSync(() -> {
                    this.saveDebugReport(path.resolve("server"));
                });
                this.onMetricsRecordingFinished.accept(path);
            });
            this.willStartRecordingMetrics = false;
        }

        this.profiler = GameProfilerTick.decorateFiller(this.metricsRecorder.getProfiler(), GameProfilerTick.createTickProfiler("Server"));
        this.metricsRecorder.startTick();
        this.profiler.startTick();
    }

    private void endMetricsRecordingTick() {
        this.profiler.endTick();
        this.metricsRecorder.endTick();
    }

    public boolean isRecordingMetrics() {
        return this.metricsRecorder.isRecording();
    }

    public void startRecordingMetrics(Consumer<MethodProfilerResults> resultConsumer, Consumer<Path> dumpConsumer) {
        this.onMetricsRecordingStopped = (result) -> {
            this.stopRecordingMetrics();
            resultConsumer.accept(result);
        };
        this.onMetricsRecordingFinished = dumpConsumer;
        this.willStartRecordingMetrics = true;
    }

    public void stopRecordingMetrics() {
        this.metricsRecorder = MetricsRecorderInactive.INSTANCE;
    }

    public void finishRecordingMetrics() {
        this.metricsRecorder.end();
    }

    public Path getWorldPath(SavedFile worldSavePath) {
        return this.storageSource.getWorldFolder(worldSavePath);
    }

    public boolean isSyncChunkWrites() {
        return true;
    }

    public DefinedStructureManager getDefinedStructureManager() {
        return this.structureManager;
    }

    public SaveData getSaveData() {
        return this.worldData;
    }

    public IRegistryCustom getCustomRegistry() {
        return this.registryHolder;
    }

    public ITextFilter createTextFilterForPlayer(EntityPlayer player) {
        return ITextFilter.DUMMY;
    }

    public boolean isResourcePackRequired() {
        return false;
    }

    public PlayerInteractManager createGameModeForPlayer(EntityPlayer player) {
        return (PlayerInteractManager)(this.isDemoMode() ? new DemoPlayerInteractManager(player) : new PlayerInteractManager(player));
    }

    @Nullable
    public EnumGamemode getForcedGameType() {
        return null;
    }

    public IResourceManager getResourceManager() {
        return this.resources.getResourceManager();
    }

    @Nullable
    public IChatBaseComponent getResourcePackPrompt() {
        return null;
    }

    public boolean isCurrentlySaving() {
        return this.isSaving;
    }

    public boolean isTimeProfilerRunning() {
        return this.debugCommandProfilerDelayStart || this.debugCommandProfiler != null;
    }

    public void startTimeProfiler() {
        this.debugCommandProfilerDelayStart = true;
    }

    public MethodProfilerResults stopTimeProfiler() {
        if (this.debugCommandProfiler == null) {
            return MethodProfilerResultsEmpty.EMPTY;
        } else {
            MethodProfilerResults profileResults = this.debugCommandProfiler.stop(SystemUtils.getMonotonicNanos(), this.tickCount);
            this.debugCommandProfiler = null;
            return profileResults;
        }
    }

    static class TimeProfiler {
        final long startNanos;
        final int startTick;

        TimeProfiler(long time, int tick) {
            this.startNanos = time;
            this.startTick = tick;
        }

        MethodProfilerResults stop(long endTime, int endTick) {
            return new MethodProfilerResults() {
                @Override
                public List<MethodProfilerResultsField> getTimes(String parentPath) {
                    return Collections.emptyList();
                }

                @Override
                public boolean saveResults(Path path) {
                    return false;
                }

                @Override
                public long getStartTimeNano() {
                    return TimeProfiler.this.startNanos;
                }

                @Override
                public int getStartTimeTicks() {
                    return TimeProfiler.this.startTick;
                }

                @Override
                public long getEndTimeNano() {
                    return endTime;
                }

                @Override
                public int getEndTimeTicks() {
                    return endTick;
                }

                @Override
                public String getProfilerResults() {
                    return "";
                }
            };
        }
    }
}
