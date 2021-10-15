package net.minecraft.server;

import com.google.common.collect.ImmutableSet;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Lifecycle;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.net.Proxy;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.minecraft.CrashReport;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.SharedConstants;
import net.minecraft.SystemUtils;
import net.minecraft.commands.CommandDispatcher;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.nbt.DynamicOpsNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.obfuscate.DontObfuscate;
import net.minecraft.resources.RegistryReadOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import net.minecraft.server.dedicated.DedicatedServerSettings;
import net.minecraft.server.level.progress.WorldLoadListenerLogger;
import net.minecraft.server.packs.EnumResourcePackType;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.ResourcePackRepository;
import net.minecraft.server.packs.repository.ResourcePackSourceFolder;
import net.minecraft.server.packs.repository.ResourcePackSourceVanilla;
import net.minecraft.server.players.UserCache;
import net.minecraft.util.MathHelper;
import net.minecraft.util.datafix.DataConverterRegistry;
import net.minecraft.util.worldupdate.WorldUpgrader;
import net.minecraft.world.level.DataPackConfiguration;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldSettings;
import net.minecraft.world.level.levelgen.GeneratorSettings;
import net.minecraft.world.level.storage.Convertable;
import net.minecraft.world.level.storage.SaveData;
import net.minecraft.world.level.storage.SavedFile;
import net.minecraft.world.level.storage.WorldDataServer;
import net.minecraft.world.level.storage.WorldInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {
    private static final Logger LOGGER = LogManager.getLogger();

    @DontObfuscate
    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        OptionParser optionParser = new OptionParser();
        OptionSpec<Void> optionSpec = optionParser.accepts("nogui");
        OptionSpec<Void> optionSpec2 = optionParser.accepts("initSettings", "Initializes 'server.properties' and 'eula.txt', then quits");
        OptionSpec<Void> optionSpec3 = optionParser.accepts("demo");
        OptionSpec<Void> optionSpec4 = optionParser.accepts("bonusChest");
        OptionSpec<Void> optionSpec5 = optionParser.accepts("forceUpgrade");
        OptionSpec<Void> optionSpec6 = optionParser.accepts("eraseCache");
        OptionSpec<Void> optionSpec7 = optionParser.accepts("safeMode", "Loads level with vanilla datapack only");
        OptionSpec<Void> optionSpec8 = optionParser.accepts("help").forHelp();
        OptionSpec<String> optionSpec9 = optionParser.accepts("singleplayer").withRequiredArg();
        OptionSpec<String> optionSpec10 = optionParser.accepts("universe").withRequiredArg().defaultsTo(".");
        OptionSpec<String> optionSpec11 = optionParser.accepts("world").withRequiredArg();
        OptionSpec<Integer> optionSpec12 = optionParser.accepts("port").withRequiredArg().ofType(Integer.class).defaultsTo(-1);
        OptionSpec<String> optionSpec13 = optionParser.accepts("serverId").withRequiredArg();
        OptionSpec<String> optionSpec14 = optionParser.nonOptions();

        try {
            OptionSet optionSet = optionParser.parse(args);
            if (optionSet.has(optionSpec8)) {
                optionParser.printHelpOn(System.err);
                return;
            }

            CrashReport.preload();
            DispenserRegistry.init();
            DispenserRegistry.validate();
            SystemUtils.startTimerHackThread();
            IRegistryCustom.Dimension registryHolder = IRegistryCustom.builtin();
            Path path = Paths.get("server.properties");
            DedicatedServerSettings dedicatedServerSettings = new DedicatedServerSettings(path);
            dedicatedServerSettings.save();
            Path path2 = Paths.get("eula.txt");
            EULA eula = new EULA(path2);
            if (optionSet.has(optionSpec2)) {
                LOGGER.info("Initialized '{}' and '{}'", path.toAbsolutePath(), path2.toAbsolutePath());
                return;
            }

            if (!eula.hasAgreedToEULA()) {
                LOGGER.info("You need to agree to the EULA in order to run the server. Go to eula.txt for more info.");
                return;
            }

            File file = new File(optionSet.valueOf(optionSpec10));
            YggdrasilAuthenticationService yggdrasilAuthenticationService = new YggdrasilAuthenticationService(Proxy.NO_PROXY);
            MinecraftSessionService minecraftSessionService = yggdrasilAuthenticationService.createMinecraftSessionService();
            GameProfileRepository gameProfileRepository = yggdrasilAuthenticationService.createProfileRepository();
            UserCache gameProfileCache = new UserCache(gameProfileRepository, new File(file, MinecraftServer.USERID_CACHE_FILE.getName()));
            String string = Optional.ofNullable(optionSet.valueOf(optionSpec11)).orElse(dedicatedServerSettings.getProperties().levelName);
            Convertable levelStorageSource = Convertable.createDefault(file.toPath());
            Convertable.ConversionSession levelStorageAccess = levelStorageSource.createAccess(string);
            MinecraftServer.convertWorld(levelStorageAccess);
            WorldInfo levelSummary = levelStorageAccess.getSummary();
            if (levelSummary != null && levelSummary.isIncompatibleWorldHeight()) {
                LOGGER.info("Loading of worlds with extended height is disabled.");
                return;
            }

            DataPackConfiguration dataPackConfig = levelStorageAccess.getDataPacks();
            boolean bl = optionSet.has(optionSpec7);
            if (bl) {
                LOGGER.warn("Safe mode active, only vanilla datapack will be loaded");
            }

            ResourcePackRepository packRepository = new ResourcePackRepository(EnumResourcePackType.SERVER_DATA, new ResourcePackSourceVanilla(), new ResourcePackSourceFolder(levelStorageAccess.getWorldFolder(SavedFile.DATAPACK_DIR).toFile(), PackSource.WORLD));
            DataPackConfiguration dataPackConfig2 = MinecraftServer.configurePackRepository(packRepository, dataPackConfig == null ? DataPackConfiguration.DEFAULT : dataPackConfig, bl);
            CompletableFuture<DataPackResources> completableFuture = DataPackResources.loadResources(packRepository.openAllSelected(), registryHolder, CommandDispatcher.ServerType.DEDICATED, dedicatedServerSettings.getProperties().functionPermissionLevel, SystemUtils.backgroundExecutor(), Runnable::run);

            DataPackResources serverResources;
            try {
                serverResources = completableFuture.get();
            } catch (Exception var42) {
                LOGGER.warn("Failed to load datapacks, can't proceed with server load. You can either fix your datapacks or reset to vanilla with --safeMode", (Throwable)var42);
                packRepository.close();
                return;
            }

            serverResources.updateGlobals();
            RegistryReadOps<NBTBase> registryReadOps = RegistryReadOps.createAndLoad(DynamicOpsNBT.INSTANCE, serverResources.getResourceManager(), registryHolder);
            dedicatedServerSettings.getProperties().getWorldGenSettings(registryHolder);
            SaveData worldData = levelStorageAccess.getDataTag(registryReadOps, dataPackConfig2);
            if (worldData == null) {
                WorldSettings levelSettings;
                GeneratorSettings worldGenSettings;
                if (optionSet.has(optionSpec3)) {
                    levelSettings = MinecraftServer.DEMO_SETTINGS;
                    worldGenSettings = GeneratorSettings.demoSettings(registryHolder);
                } else {
                    DedicatedServerProperties dedicatedServerProperties = dedicatedServerSettings.getProperties();
                    levelSettings = new WorldSettings(dedicatedServerProperties.levelName, dedicatedServerProperties.gamemode, dedicatedServerProperties.hardcore, dedicatedServerProperties.difficulty, false, new GameRules(), dataPackConfig2);
                    worldGenSettings = optionSet.has(optionSpec4) ? dedicatedServerProperties.getWorldGenSettings(registryHolder).withBonusChest() : dedicatedServerProperties.getWorldGenSettings(registryHolder);
                }

                worldData = new WorldDataServer(levelSettings, worldGenSettings, Lifecycle.stable());
            }

            if (optionSet.has(optionSpec5)) {
                convertWorld(levelStorageAccess, DataConverterRegistry.getDataFixer(), optionSet.has(optionSpec6), () -> {
                    return true;
                }, worldData.getGeneratorSettings().levels());
            }

            levelStorageAccess.saveDataTag(registryHolder, worldData);
            SaveData worldData2 = worldData;
            final DedicatedServer dedicatedServer = MinecraftServer.spin((serverThread) -> {
                DedicatedServer dedicatedServer = new DedicatedServer(serverThread, registryHolder, levelStorageAccess, packRepository, serverResources, worldData2, dedicatedServerSettings, DataConverterRegistry.getDataFixer(), minecraftSessionService, gameProfileRepository, gameProfileCache, WorldLoadListenerLogger::new);
                dedicatedServer.setSingleplayerName(optionSet.valueOf(optionSpec9));
                dedicatedServer.setPort(optionSet.valueOf(optionSpec12));
                dedicatedServer.setDemo(optionSet.has(optionSpec3));
                dedicatedServer.setId(optionSet.valueOf(optionSpec13));
                boolean bl = !optionSet.has(optionSpec) && !optionSet.valuesOf(optionSpec14).contains("nogui");
                if (bl && !GraphicsEnvironment.isHeadless()) {
                    dedicatedServer.showGui();
                }

                return dedicatedServer;
            });
            Thread thread = new Thread("Server Shutdown Thread") {
                @Override
                public void run() {
                    dedicatedServer.safeShutdown(true);
                }
            };
            thread.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER));
            Runtime.getRuntime().addShutdownHook(thread);
        } catch (Exception var43) {
            LOGGER.fatal("Failed to start the minecraft server", (Throwable)var43);
        }

    }

    public static void convertWorld(Convertable.ConversionSession session, DataFixer dataFixer, boolean eraseCache, BooleanSupplier booleanSupplier, ImmutableSet<ResourceKey<World>> worlds) {
        LOGGER.info("Forcing world upgrade!");
        WorldUpgrader worldUpgrader = new WorldUpgrader(session, dataFixer, worlds, eraseCache);
        IChatBaseComponent component = null;

        while(!worldUpgrader.isFinished()) {
            IChatBaseComponent component2 = worldUpgrader.getStatus();
            if (component != component2) {
                component = component2;
                LOGGER.info(worldUpgrader.getStatus().getString());
            }

            int i = worldUpgrader.getTotalChunks();
            if (i > 0) {
                int j = worldUpgrader.getConverted() + worldUpgrader.getSkipped();
                LOGGER.info("{}% completed ({} / {} chunks)...", MathHelper.floor((float)j / (float)i * 100.0F), j, i);
            }

            if (!booleanSupplier.getAsBoolean()) {
                worldUpgrader.cancel();
            } else {
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException var10) {
                }
            }
        }

    }
}
