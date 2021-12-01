package net.minecraft.gametest.framework;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.serialization.Lifecycle;
import java.net.Proxy;
import java.util.Collection;
import java.util.List;
import java.util.function.BooleanSupplier;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.SystemReport;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.server.DataPackResources;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.server.level.progress.WorldLoadListenerLogger;
import net.minecraft.server.packs.repository.ResourcePackRepository;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.UserCache;
import net.minecraft.util.datafix.DataConverterRegistry;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.level.DataPackConfiguration;
import net.minecraft.world.level.EnumGamemode;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.WorldSettings;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.block.EnumBlockRotation;
import net.minecraft.world.level.dimension.DimensionManager;
import net.minecraft.world.level.levelgen.ChunkProviderFlat;
import net.minecraft.world.level.levelgen.GeneratorSettings;
import net.minecraft.world.level.levelgen.flat.GeneratorSettingsFlat;
import net.minecraft.world.level.storage.Convertable;
import net.minecraft.world.level.storage.WorldDataServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GameTestServer extends MinecraftServer {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int PROGRESS_REPORT_INTERVAL = 20;
    private final List<GameTestHarnessBatch> testBatches;
    private final BlockPosition spawnPos;
    private static final GameRules TEST_GAME_RULES = SystemUtils.make(new GameRules(), (gameRules) -> {
        gameRules.get(GameRules.RULE_DOMOBSPAWNING).set(false, (MinecraftServer)null);
        gameRules.get(GameRules.RULE_WEATHER_CYCLE).set(false, (MinecraftServer)null);
    });
    private static final WorldSettings TEST_SETTINGS = new WorldSettings("Test Level", EnumGamemode.CREATIVE, false, EnumDifficulty.NORMAL, true, TEST_GAME_RULES, DataPackConfiguration.DEFAULT);
    @Nullable
    private GameTestHarnessCollector testTracker;

    public GameTestServer(Thread serverThread, Convertable.ConversionSession session, ResourcePackRepository dataPackManager, DataPackResources serverResourceManager, Collection<GameTestHarnessBatch> batches, BlockPosition pos, IRegistryCustom.Dimension registryManager) {
        this(serverThread, session, dataPackManager, serverResourceManager, batches, pos, registryManager, registryManager.registryOrThrow(IRegistry.BIOME_REGISTRY), registryManager.registryOrThrow(IRegistry.DIMENSION_TYPE_REGISTRY));
    }

    private GameTestServer(Thread serverThread, Convertable.ConversionSession session, ResourcePackRepository dataPackManager, DataPackResources serverResourceManager, Collection<GameTestHarnessBatch> batches, BlockPosition pos, IRegistryCustom.Dimension registryManager, IRegistry<BiomeBase> biomeRegistry, IRegistry<DimensionManager> dimensionTypeRegistry) {
        super(serverThread, registryManager, session, new WorldDataServer(TEST_SETTINGS, new GeneratorSettings(0L, false, false, GeneratorSettings.withOverworld(dimensionTypeRegistry, DimensionManager.defaultDimensions(registryManager, 0L), new ChunkProviderFlat(GeneratorSettingsFlat.getDefault(biomeRegistry)))), Lifecycle.stable()), dataPackManager, Proxy.NO_PROXY, DataConverterRegistry.getDataFixer(), serverResourceManager, (MinecraftSessionService)null, (GameProfileRepository)null, (UserCache)null, WorldLoadListenerLogger::new);
        this.testBatches = Lists.newArrayList(batches);
        this.spawnPos = pos;
        if (batches.isEmpty()) {
            throw new IllegalArgumentException("No test batches were given!");
        }
    }

    @Override
    public boolean init() {
        this.setPlayerList(new PlayerList(this, this.registryHolder, this.playerDataStorage, 1) {
        });
        this.loadWorld();
        WorldServer serverLevel = this.overworld();
        serverLevel.setDefaultSpawnPos(this.spawnPos, 0.0F);
        int i = 20000000;
        serverLevel.setWeatherParameters(20000000, 20000000, false, false);
        return true;
    }

    @Override
    public void tickServer(BooleanSupplier shouldKeepTicking) {
        super.tickServer(shouldKeepTicking);
        WorldServer serverLevel = this.overworld();
        if (!this.haveTestsStarted()) {
            this.startTests(serverLevel);
        }

        if (serverLevel.getTime() % 20L == 0L) {
            LOGGER.info(this.testTracker.getProgressBar());
        }

        if (this.testTracker.isDone()) {
            this.safeShutdown(false);
            LOGGER.info(this.testTracker.getProgressBar());
            GlobalTestReporter.finish();
            LOGGER.info("========= {} GAME TESTS COMPLETE ======================", (int)this.testTracker.getTotalCount());
            if (this.testTracker.hasFailedRequired()) {
                LOGGER.info("{} required tests failed :(", (int)this.testTracker.getFailedRequiredCount());
                this.testTracker.getFailedRequired().forEach((test) -> {
                    LOGGER.info("   - {}", (Object)test.getTestName());
                });
            } else {
                LOGGER.info("All {} required tests passed :)", (int)this.testTracker.getTotalCount());
            }

            if (this.testTracker.hasFailedOptional()) {
                LOGGER.info("{} optional tests failed", (int)this.testTracker.getFailedOptionalCount());
                this.testTracker.getFailedOptional().forEach((test) -> {
                    LOGGER.info("   - {}", (Object)test.getTestName());
                });
            }

            LOGGER.info("====================================================");
        }

    }

    @Override
    public SystemReport fillServerSystemReport(SystemReport details) {
        details.setDetail("Type", "Game test server");
        return details;
    }

    @Override
    public void exit() {
        super.exit();
        System.exit(this.testTracker.getFailedRequiredCount());
    }

    @Override
    public void onServerCrash(CrashReport report) {
        System.exit(1);
    }

    private void startTests(WorldServer world) {
        Collection<GameTestHarnessInfo> collection = GameTestHarnessRunner.runTestBatches(this.testBatches, new BlockPosition(0, -60, 0), EnumBlockRotation.NONE, world, GameTestHarnessTicker.SINGLETON, 8);
        this.testTracker = new GameTestHarnessCollector(collection);
        LOGGER.info("{} tests are now running!", (int)this.testTracker.getTotalCount());
    }

    private boolean haveTestsStarted() {
        return this.testTracker != null;
    }

    @Override
    public boolean isHardcore() {
        return false;
    }

    @Override
    public int getOperatorUserPermissionLevel() {
        return 0;
    }

    @Override
    public int getFunctionCompilationLevel() {
        return 4;
    }

    @Override
    public boolean shouldRconBroadcast() {
        return false;
    }

    @Override
    public boolean isDedicatedServer() {
        return false;
    }

    @Override
    public int getRateLimitPacketsPerSecond() {
        return 0;
    }

    @Override
    public boolean isEpollEnabled() {
        return false;
    }

    @Override
    public boolean getEnableCommandBlock() {
        return true;
    }

    @Override
    public boolean isPublished() {
        return false;
    }

    @Override
    public boolean shouldBroadcastCommands() {
        return false;
    }

    @Override
    public boolean isSingleplayerOwner(GameProfile profile) {
        return false;
    }
}
