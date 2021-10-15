package net.minecraft.server.dedicated;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.datafixers.DataFixer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.InetAddress;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.SharedConstants;
import net.minecraft.SystemReport;
import net.minecraft.SystemUtils;
import net.minecraft.ThreadNamedUncaughtExceptionHandler;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.server.DataPackResources;
import net.minecraft.server.IMinecraftServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerCommand;
import net.minecraft.server.gui.ServerGUI;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.server.level.progress.WorldLoadListenerFactory;
import net.minecraft.server.network.ITextFilter;
import net.minecraft.server.network.TextFilter;
import net.minecraft.server.packs.repository.ResourcePackRepository;
import net.minecraft.server.players.NameReferencingFileConverter;
import net.minecraft.server.players.UserCache;
import net.minecraft.server.rcon.RemoteControlCommandListener;
import net.minecraft.server.rcon.thread.RemoteControlListener;
import net.minecraft.server.rcon.thread.RemoteStatusListener;
import net.minecraft.util.MathHelper;
import net.minecraft.util.monitoring.jmx.MinecraftServerBeans;
import net.minecraft.world.MojangStatisticsGenerator;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.EnumGamemode;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.TileEntitySkull;
import net.minecraft.world.level.storage.Convertable;
import net.minecraft.world.level.storage.SaveData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DedicatedServer extends MinecraftServer implements IMinecraftServer {
    static final Logger LOGGER = LogManager.getLogger();
    private static final int CONVERSION_RETRY_DELAY_MS = 5000;
    private static final int CONVERSION_RETRIES = 2;
    private static final Pattern SHA1 = Pattern.compile("^[a-fA-F0-9]{40}$");
    private final List<ServerCommand> consoleInput = Collections.synchronizedList(Lists.newArrayList());
    private RemoteStatusListener queryThreadGs4;
    public final RemoteControlCommandListener rconConsoleSource;
    private RemoteControlListener rconThread;
    public DedicatedServerSettings settings;
    @Nullable
    private ServerGUI gui;
    @Nullable
    private final TextFilter textFilterClient;
    @Nullable
    private final IChatBaseComponent resourcePackPrompt;

    public DedicatedServer(Thread serverThread, IRegistryCustom.Dimension registryManager, Convertable.ConversionSession session, ResourcePackRepository dataPackManager, DataPackResources serverResourceManager, SaveData saveProperties, DedicatedServerSettings propertiesLoader, DataFixer dataFixer, MinecraftSessionService sessionService, GameProfileRepository gameProfileRepo, UserCache userCache, WorldLoadListenerFactory worldGenerationProgressListenerFactory) {
        super(serverThread, registryManager, session, saveProperties, dataPackManager, Proxy.NO_PROXY, dataFixer, serverResourceManager, sessionService, gameProfileRepo, userCache, worldGenerationProgressListenerFactory);
        this.settings = propertiesLoader;
        this.rconConsoleSource = new RemoteControlCommandListener(this);
        this.textFilterClient = TextFilter.createFromConfig(propertiesLoader.getProperties().textFilteringConfig);
        this.resourcePackPrompt = parseResourcePackPrompt(propertiesLoader);
    }

    @Override
    public boolean init() throws IOException {
        Thread thread = new Thread("Server console handler") {
            @Override
            public void run() {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

                String string;
                try {
                    while(!DedicatedServer.this.isStopped() && DedicatedServer.this.isRunning() && (string = bufferedReader.readLine()) != null) {
                        DedicatedServer.this.issueCommand(string, DedicatedServer.this.getServerCommandListener());
                    }
                } catch (IOException var4) {
                    DedicatedServer.LOGGER.error("Exception handling console input", (Throwable)var4);
                }

            }
        };
        thread.setDaemon(true);
        thread.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER));
        thread.start();
        LOGGER.info("Starting minecraft server version {}", (Object)SharedConstants.getGameVersion().getName());
        if (Runtime.getRuntime().maxMemory() / 1024L / 1024L < 512L) {
            LOGGER.warn("To start the server with more ram, launch it as \"java -Xmx1024M -Xms1024M -jar minecraft_server.jar\"");
        }

        LOGGER.info("Loading properties");
        DedicatedServerProperties dedicatedServerProperties = this.settings.getProperties();
        if (this.isEmbeddedServer()) {
            this.setLocalIp("127.0.0.1");
        } else {
            this.setOnlineMode(dedicatedServerProperties.onlineMode);
            this.setPreventProxyConnections(dedicatedServerProperties.preventProxyConnections);
            this.setLocalIp(dedicatedServerProperties.serverIp);
        }

        this.setPVP(dedicatedServerProperties.pvp);
        this.setAllowFlight(dedicatedServerProperties.allowFlight);
        this.setResourcePack(dedicatedServerProperties.resourcePack, this.getPackHash());
        this.setMotd(dedicatedServerProperties.motd);
        super.setIdleTimeout(dedicatedServerProperties.playerIdleTimeout.get());
        this.setEnforceWhitelist(dedicatedServerProperties.enforceWhitelist);
        this.worldData.setGameType(dedicatedServerProperties.gamemode);
        LOGGER.info("Default game type: {}", (Object)dedicatedServerProperties.gamemode);
        InetAddress inetAddress = null;
        if (!this.getServerIp().isEmpty()) {
            inetAddress = InetAddress.getByName(this.getServerIp());
        }

        if (this.getPort() < 0) {
            this.setPort(dedicatedServerProperties.serverPort);
        }

        this.initializeKeyPair();
        LOGGER.info("Starting Minecraft server on {}:{}", this.getServerIp().isEmpty() ? "*" : this.getServerIp(), this.getPort());

        try {
            this.getServerConnection().startTcpServerListener(inetAddress, this.getPort());
        } catch (IOException var10) {
            LOGGER.warn("**** FAILED TO BIND TO PORT!");
            LOGGER.warn("The exception was: {}", (Object)var10.toString());
            LOGGER.warn("Perhaps a server is already running on that port?");
            return false;
        }

        if (!this.getOnlineMode()) {
            LOGGER.warn("**** SERVER IS RUNNING IN OFFLINE/INSECURE MODE!");
            LOGGER.warn("The server will make no attempt to authenticate usernames. Beware.");
            LOGGER.warn("While this makes the game possible to play without internet access, it also opens up the ability for hackers to connect with any username they choose.");
            LOGGER.warn("To change this, set \"online-mode\" to \"true\" in the server.properties file.");
        }

        if (this.convertNames()) {
            this.getUserCache().save();
        }

        if (!NameReferencingFileConverter.serverReadyAfterUserconversion(this)) {
            return false;
        } else {
            this.setPlayerList(new DedicatedPlayerList(this, this.registryHolder, this.playerDataStorage));
            long l = SystemUtils.getMonotonicNanos();
            TileEntitySkull.setProfileCache(this.getUserCache());
            TileEntitySkull.setSessionService(this.getMinecraftSessionService());
            TileEntitySkull.setMainThreadExecutor(this);
            UserCache.setUsesAuthentication(this.getOnlineMode());
            LOGGER.info("Preparing level \"{}\"", (Object)this.getWorld());
            this.loadWorld();
            long m = SystemUtils.getMonotonicNanos() - l;
            String string = String.format(Locale.ROOT, "%.3fs", (double)m / 1.0E9D);
            LOGGER.info("Done ({})! For help, type \"help\"", (Object)string);
            if (dedicatedServerProperties.announcePlayerAchievements != null) {
                this.getGameRules().get(GameRules.RULE_ANNOUNCE_ADVANCEMENTS).set(dedicatedServerProperties.announcePlayerAchievements, this);
            }

            if (dedicatedServerProperties.enableQuery) {
                LOGGER.info("Starting GS4 status listener");
                this.queryThreadGs4 = RemoteStatusListener.create(this);
            }

            if (dedicatedServerProperties.enableRcon) {
                LOGGER.info("Starting remote control listener");
                this.rconThread = RemoteControlListener.create(this);
            }

            if (this.getMaxTickTime() > 0L) {
                Thread thread2 = new Thread(new ThreadWatchdog(this));
                thread2.setUncaughtExceptionHandler(new ThreadNamedUncaughtExceptionHandler(LOGGER));
                thread2.setName("Server Watchdog");
                thread2.setDaemon(true);
                thread2.start();
            }

            Items.AIR.fillItemCategory(CreativeModeTab.TAB_SEARCH, NonNullList.create());
            if (dedicatedServerProperties.enableJmxMonitoring) {
                MinecraftServerBeans.registerJmxMonitoring(this);
                LOGGER.info("JMX monitoring enabled");
            }

            return true;
        }
    }

    @Override
    public boolean getSpawnAnimals() {
        return this.getDedicatedServerProperties().spawnAnimals && super.getSpawnAnimals();
    }

    @Override
    public boolean getSpawnMonsters() {
        return this.settings.getProperties().spawnMonsters && super.getSpawnMonsters();
    }

    @Override
    public boolean getSpawnNPCs() {
        return this.settings.getProperties().spawnNpcs && super.getSpawnNPCs();
    }

    public String getPackHash() {
        DedicatedServerProperties dedicatedServerProperties = this.settings.getProperties();
        String string;
        if (!dedicatedServerProperties.resourcePackSha1.isEmpty()) {
            string = dedicatedServerProperties.resourcePackSha1;
            if (!Strings.isNullOrEmpty(dedicatedServerProperties.resourcePackHash)) {
                LOGGER.warn("resource-pack-hash is deprecated and found along side resource-pack-sha1. resource-pack-hash will be ignored.");
            }
        } else if (!Strings.isNullOrEmpty(dedicatedServerProperties.resourcePackHash)) {
            LOGGER.warn("resource-pack-hash is deprecated. Please use resource-pack-sha1 instead.");
            string = dedicatedServerProperties.resourcePackHash;
        } else {
            string = "";
        }

        if (!string.isEmpty() && !SHA1.matcher(string).matches()) {
            LOGGER.warn("Invalid sha1 for ressource-pack-sha1");
        }

        if (!dedicatedServerProperties.resourcePack.isEmpty() && string.isEmpty()) {
            LOGGER.warn("You specified a resource pack without providing a sha1 hash. Pack will be updated on the client only if you change the name of the pack.");
        }

        return string;
    }

    @Override
    public DedicatedServerProperties getDedicatedServerProperties() {
        return this.settings.getProperties();
    }

    @Override
    public void updateWorldSettings() {
        this.setDifficulty(this.getDedicatedServerProperties().difficulty, true);
    }

    @Override
    public boolean isHardcore() {
        return this.getDedicatedServerProperties().hardcore;
    }

    @Override
    public SystemReport fillServerSystemReport(SystemReport details) {
        details.setDetail("Is Modded", () -> {
            return this.getModded().orElse("Unknown (can't tell)");
        });
        details.setDetail("Type", () -> {
            return "Dedicated Server (map_server.txt)";
        });
        return details;
    }

    @Override
    public void dumpServerProperties(Path file) throws IOException {
        DedicatedServerProperties dedicatedServerProperties = this.getDedicatedServerProperties();
        Writer writer = Files.newBufferedWriter(file);

        try {
            writer.write(String.format("sync-chunk-writes=%s%n", dedicatedServerProperties.syncChunkWrites));
            writer.write(String.format("gamemode=%s%n", dedicatedServerProperties.gamemode));
            writer.write(String.format("spawn-monsters=%s%n", dedicatedServerProperties.spawnMonsters));
            writer.write(String.format("entity-broadcast-range-percentage=%d%n", dedicatedServerProperties.entityBroadcastRangePercentage));
            writer.write(String.format("max-world-size=%d%n", dedicatedServerProperties.maxWorldSize));
            writer.write(String.format("spawn-npcs=%s%n", dedicatedServerProperties.spawnNpcs));
            writer.write(String.format("view-distance=%d%n", dedicatedServerProperties.viewDistance));
            writer.write(String.format("spawn-animals=%s%n", dedicatedServerProperties.spawnAnimals));
            writer.write(String.format("generate-structures=%s%n", dedicatedServerProperties.getWorldGenSettings(this.registryHolder).shouldGenerateMapFeatures()));
            writer.write(String.format("use-native=%s%n", dedicatedServerProperties.useNativeTransport));
            writer.write(String.format("rate-limit=%d%n", dedicatedServerProperties.rateLimitPacketsPerSecond));
        } catch (Throwable var7) {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Throwable var6) {
                    var7.addSuppressed(var6);
                }
            }

            throw var7;
        }

        if (writer != null) {
            writer.close();
        }

    }

    @Override
    public Optional<String> getModded() {
        String string = this.getServerModName();
        return !"vanilla".equals(string) ? Optional.of("Definitely; Server brand changed to '" + string + "'") : Optional.empty();
    }

    @Override
    public void exit() {
        if (this.textFilterClient != null) {
            this.textFilterClient.close();
        }

        if (this.gui != null) {
            this.gui.close();
        }

        if (this.rconThread != null) {
            this.rconThread.stop();
        }

        if (this.queryThreadGs4 != null) {
            this.queryThreadGs4.stop();
        }

    }

    @Override
    public void tickChildren(BooleanSupplier shouldKeepTicking) {
        super.tickChildren(shouldKeepTicking);
        this.handleCommandQueue();
    }

    @Override
    public boolean getAllowNether() {
        return this.getDedicatedServerProperties().allowNether;
    }

    @Override
    public void populateSnooper(MojangStatisticsGenerator snooper) {
        snooper.setDynamicData("whitelist_enabled", this.getPlayerList().getHasWhitelist());
        snooper.setDynamicData("whitelist_count", this.getPlayerList().getWhitelisted().length);
        super.populateSnooper(snooper);
    }

    @Override
    public boolean isSnooperEnabled() {
        return this.getDedicatedServerProperties().snooperEnabled;
    }

    public void issueCommand(String command, CommandListenerWrapper commandSource) {
        this.consoleInput.add(new ServerCommand(command, commandSource));
    }

    public void handleCommandQueue() {
        while(!this.consoleInput.isEmpty()) {
            ServerCommand consoleInput = this.consoleInput.remove(0);
            this.getCommandDispatcher().performCommand(consoleInput.source, consoleInput.msg);
        }

    }

    @Override
    public boolean isDedicatedServer() {
        return true;
    }

    @Override
    public int getRateLimitPacketsPerSecond() {
        return this.getDedicatedServerProperties().rateLimitPacketsPerSecond;
    }

    @Override
    public boolean isEpollEnabled() {
        return this.getDedicatedServerProperties().useNativeTransport;
    }

    @Override
    public DedicatedPlayerList getPlayerList() {
        return (DedicatedPlayerList)super.getPlayerList();
    }

    @Override
    public boolean isPublished() {
        return true;
    }

    @Override
    public String getServerIp() {
        return this.getServerIp();
    }

    @Override
    public int getServerPort() {
        return this.getPort();
    }

    @Override
    public String getServerName() {
        return this.getMotd();
    }

    public void showGui() {
        if (this.gui == null) {
            this.gui = ServerGUI.showFrameFor(this);
        }

    }

    @Override
    public boolean hasGui() {
        return this.gui != null;
    }

    @Override
    public boolean getEnableCommandBlock() {
        return this.getDedicatedServerProperties().enableCommandBlock;
    }

    @Override
    public int getSpawnProtection() {
        return this.getDedicatedServerProperties().spawnProtection;
    }

    @Override
    public boolean isUnderSpawnProtection(WorldServer world, BlockPosition pos, EntityHuman player) {
        if (world.getDimensionKey() != World.OVERWORLD) {
            return false;
        } else if (this.getPlayerList().getOPs().isEmpty()) {
            return false;
        } else if (this.getPlayerList().isOp(player.getProfile())) {
            return false;
        } else if (this.getSpawnProtection() <= 0) {
            return false;
        } else {
            BlockPosition blockPos = world.getSpawn();
            int i = MathHelper.abs(pos.getX() - blockPos.getX());
            int j = MathHelper.abs(pos.getZ() - blockPos.getZ());
            int k = Math.max(i, j);
            return k <= this.getSpawnProtection();
        }
    }

    @Override
    public boolean repliesToStatus() {
        return this.getDedicatedServerProperties().enableStatus;
    }

    @Override
    public int getOperatorUserPermissionLevel() {
        return this.getDedicatedServerProperties().opPermissionLevel;
    }

    @Override
    public int getFunctionCompilationLevel() {
        return this.getDedicatedServerProperties().functionPermissionLevel;
    }

    @Override
    public void setIdleTimeout(int playerIdleTimeout) {
        super.setIdleTimeout(playerIdleTimeout);
        this.settings.setProperty((dedicatedServerProperties) -> {
            return dedicatedServerProperties.playerIdleTimeout.set(this.getCustomRegistry(), playerIdleTimeout);
        });
    }

    @Override
    public boolean shouldRconBroadcast() {
        return this.getDedicatedServerProperties().broadcastRconToOps;
    }

    @Override
    public boolean shouldBroadcastCommands() {
        return this.getDedicatedServerProperties().broadcastConsoleToOps;
    }

    @Override
    public int getAbsoluteMaxWorldSize() {
        return this.getDedicatedServerProperties().maxWorldSize;
    }

    @Override
    public int getCompressionThreshold() {
        return this.getDedicatedServerProperties().networkCompressionThreshold;
    }

    protected boolean convertNames() {
        boolean bl = false;

        for(int i = 0; !bl && i <= 2; ++i) {
            if (i > 0) {
                LOGGER.warn("Encountered a problem while converting the user banlist, retrying in a few seconds");
                this.waitForRetry();
            }

            bl = NameReferencingFileConverter.convertUserBanlist(this);
        }

        boolean bl2 = false;

        for(int var7 = 0; !bl2 && var7 <= 2; ++var7) {
            if (var7 > 0) {
                LOGGER.warn("Encountered a problem while converting the ip banlist, retrying in a few seconds");
                this.waitForRetry();
            }

            bl2 = NameReferencingFileConverter.convertIpBanlist(this);
        }

        boolean bl3 = false;

        for(int var8 = 0; !bl3 && var8 <= 2; ++var8) {
            if (var8 > 0) {
                LOGGER.warn("Encountered a problem while converting the op list, retrying in a few seconds");
                this.waitForRetry();
            }

            bl3 = NameReferencingFileConverter.convertOpsList(this);
        }

        boolean bl4 = false;

        for(int var9 = 0; !bl4 && var9 <= 2; ++var9) {
            if (var9 > 0) {
                LOGGER.warn("Encountered a problem while converting the whitelist, retrying in a few seconds");
                this.waitForRetry();
            }

            bl4 = NameReferencingFileConverter.convertWhiteList(this);
        }

        boolean bl5 = false;

        for(int var10 = 0; !bl5 && var10 <= 2; ++var10) {
            if (var10 > 0) {
                LOGGER.warn("Encountered a problem while converting the player save files, retrying in a few seconds");
                this.waitForRetry();
            }

            bl5 = NameReferencingFileConverter.convertPlayers(this);
        }

        return bl || bl2 || bl3 || bl4 || bl5;
    }

    private void waitForRetry() {
        try {
            Thread.sleep(5000L);
        } catch (InterruptedException var2) {
        }
    }

    public long getMaxTickTime() {
        return this.getDedicatedServerProperties().maxTickTime;
    }

    @Override
    public String getPlugins() {
        return "";
    }

    @Override
    public String executeRemoteCommand(String command) {
        this.rconConsoleSource.clearMessages();
        this.executeSync(() -> {
            this.getCommandDispatcher().performCommand(this.rconConsoleSource.getWrapper(), command);
        });
        return this.rconConsoleSource.getMessages();
    }

    public void setHasWhitelist(boolean useWhitelist) {
        this.settings.setProperty((dedicatedServerProperties) -> {
            return dedicatedServerProperties.whiteList.set(this.getCustomRegistry(), useWhitelist);
        });
    }

    @Override
    public void stop() {
        super.stop();
        SystemUtils.shutdownExecutors();
    }

    @Override
    public boolean isSingleplayerOwner(GameProfile profile) {
        return false;
    }

    @Override
    public int getScaledTrackingDistance(int initialDistance) {
        return this.getDedicatedServerProperties().entityBroadcastRangePercentage * initialDistance / 100;
    }

    @Override
    public String getWorld() {
        return this.storageSource.getLevelName();
    }

    @Override
    public boolean isSyncChunkWrites() {
        return this.settings.getProperties().syncChunkWrites;
    }

    @Override
    public ITextFilter createTextFilterForPlayer(EntityPlayer player) {
        return this.textFilterClient != null ? this.textFilterClient.createContext(player.getProfile()) : ITextFilter.DUMMY;
    }

    @Override
    public boolean isResourcePackRequired() {
        return this.settings.getProperties().requireResourcePack;
    }

    @Nullable
    @Override
    public EnumGamemode getForcedGameType() {
        return this.settings.getProperties().forceGameMode ? this.worldData.getGameType() : null;
    }

    @Nullable
    private static IChatBaseComponent parseResourcePackPrompt(DedicatedServerSettings propertiesLoader) {
        String string = propertiesLoader.getProperties().resourcePackPrompt;
        if (!Strings.isNullOrEmpty(string)) {
            try {
                return IChatBaseComponent.ChatSerializer.fromJson(string);
            } catch (Exception var3) {
                LOGGER.warn("Failed to parse resource pack prompt '{}'", string, var3);
            }
        }

        return null;
    }

    @Nullable
    @Override
    public IChatBaseComponent getResourcePackPrompt() {
        return this.resourcePackPrompt;
    }
}
