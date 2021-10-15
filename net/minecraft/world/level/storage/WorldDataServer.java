package net.minecraft.world.level.storage;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.CrashReportSystemDetails;
import net.minecraft.SharedConstants;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.core.MinecraftSerializableUUID;
import net.minecraft.nbt.DynamicOpsNBT;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.resources.RegistryWriteOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.level.DataPackConfiguration;
import net.minecraft.world.level.EnumGamemode;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.IWorldHeightAccess;
import net.minecraft.world.level.WorldSettings;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.GeneratorSettings;
import net.minecraft.world.level.timers.CustomFunctionCallbackTimerQueue;
import net.minecraft.world.level.timers.CustomFunctionCallbackTimers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WorldDataServer implements IWorldDataServer, SaveData {
    private static final Logger LOGGER = LogManager.getLogger();
    protected static final String WORLD_GEN_SETTINGS = "WorldGenSettings";
    public WorldSettings settings;
    private final GeneratorSettings worldGenSettings;
    private final Lifecycle worldGenSettingsLifecycle;
    private int xSpawn;
    private int ySpawn;
    private int zSpawn;
    private float spawnAngle;
    private long gameTime;
    private long dayTime;
    @Nullable
    private final DataFixer fixerUpper;
    private final int playerDataVersion;
    private boolean upgradedPlayerTag;
    @Nullable
    private NBTTagCompound loadedPlayerTag;
    private final int version;
    private int clearWeatherTime;
    private boolean raining;
    private int rainTime;
    private boolean thundering;
    private int thunderTime;
    private boolean initialized;
    private boolean difficultyLocked;
    private WorldBorder.Settings worldBorder;
    private NBTTagCompound endDragonFightData;
    @Nullable
    private NBTTagCompound customBossEvents;
    private int wanderingTraderSpawnDelay;
    private int wanderingTraderSpawnChance;
    @Nullable
    private UUID wanderingTraderId;
    private final Set<String> knownServerBrands;
    private boolean wasModded;
    private final CustomFunctionCallbackTimerQueue<MinecraftServer> scheduledEvents;

    private WorldDataServer(@Nullable DataFixer dataFixer, int dataVersion, @Nullable NBTTagCompound playerData, boolean modded, int spawnX, int spawnY, int spawnZ, float spawnAngle, long time, long timeOfDay, int version, int clearWeatherTime, int rainTime, boolean raining, int thunderTime, boolean thundering, boolean initialized, boolean difficultyLocked, WorldBorder.Settings worldBorder, int wanderingTraderSpawnDelay, int wanderingTraderSpawnChance, @Nullable UUID wanderingTraderId, Set<String> serverBrands, CustomFunctionCallbackTimerQueue<MinecraftServer> scheduledEvents, @Nullable NBTTagCompound customBossEvents, NBTTagCompound dragonFight, WorldSettings levelInfo, GeneratorSettings generatorOptions, Lifecycle lifecycle) {
        this.fixerUpper = dataFixer;
        this.wasModded = modded;
        this.xSpawn = spawnX;
        this.ySpawn = spawnY;
        this.zSpawn = spawnZ;
        this.spawnAngle = spawnAngle;
        this.gameTime = time;
        this.dayTime = timeOfDay;
        this.version = version;
        this.clearWeatherTime = clearWeatherTime;
        this.rainTime = rainTime;
        this.raining = raining;
        this.thunderTime = thunderTime;
        this.thundering = thundering;
        this.initialized = initialized;
        this.difficultyLocked = difficultyLocked;
        this.worldBorder = worldBorder;
        this.wanderingTraderSpawnDelay = wanderingTraderSpawnDelay;
        this.wanderingTraderSpawnChance = wanderingTraderSpawnChance;
        this.wanderingTraderId = wanderingTraderId;
        this.knownServerBrands = serverBrands;
        this.loadedPlayerTag = playerData;
        this.playerDataVersion = dataVersion;
        this.scheduledEvents = scheduledEvents;
        this.customBossEvents = customBossEvents;
        this.endDragonFightData = dragonFight;
        this.settings = levelInfo;
        this.worldGenSettings = generatorOptions;
        this.worldGenSettingsLifecycle = lifecycle;
    }

    public WorldDataServer(WorldSettings levelInfo, GeneratorSettings generatorOptions, Lifecycle lifecycle) {
        this((DataFixer)null, SharedConstants.getGameVersion().getWorldVersion(), (NBTTagCompound)null, false, 0, 0, 0, 0.0F, 0L, 0L, 19133, 0, 0, false, 0, false, false, false, WorldBorder.DEFAULT_SETTINGS, 0, 0, (UUID)null, Sets.newLinkedHashSet(), new CustomFunctionCallbackTimerQueue<>(CustomFunctionCallbackTimers.SERVER_CALLBACKS), (NBTTagCompound)null, new NBTTagCompound(), levelInfo.copy(), generatorOptions, lifecycle);
    }

    public static WorldDataServer parse(Dynamic<NBTBase> dynamic, DataFixer dataFixer, int dataVersion, @Nullable NBTTagCompound playerData, WorldSettings levelInfo, LevelVersion saveVersionInfo, GeneratorSettings generatorOptions, Lifecycle lifecycle) {
        long l = dynamic.get("Time").asLong(0L);
        NBTTagCompound compoundTag = dynamic.get("DragonFight").result().map(Dynamic::getValue).orElseGet(() -> {
            return dynamic.get("DimensionData").get("1").get("DragonFight").orElseEmptyMap().getValue();
        });
        return new WorldDataServer(dataFixer, dataVersion, playerData, dynamic.get("WasModded").asBoolean(false), dynamic.get("SpawnX").asInt(0), dynamic.get("SpawnY").asInt(0), dynamic.get("SpawnZ").asInt(0), dynamic.get("SpawnAngle").asFloat(0.0F), l, dynamic.get("DayTime").asLong(l), saveVersionInfo.levelDataVersion(), dynamic.get("clearWeatherTime").asInt(0), dynamic.get("rainTime").asInt(0), dynamic.get("raining").asBoolean(false), dynamic.get("thunderTime").asInt(0), dynamic.get("thundering").asBoolean(false), dynamic.get("initialized").asBoolean(true), dynamic.get("DifficultyLocked").asBoolean(false), WorldBorder.Settings.read(dynamic, WorldBorder.DEFAULT_SETTINGS), dynamic.get("WanderingTraderSpawnDelay").asInt(0), dynamic.get("WanderingTraderSpawnChance").asInt(0), dynamic.get("WanderingTraderId").read(MinecraftSerializableUUID.CODEC).result().orElse((UUID)null), dynamic.get("ServerBrands").asStream().flatMap((dynamicx) -> {
            return SystemUtils.toStream(dynamicx.asString().result());
        }).collect(Collectors.toCollection(Sets::newLinkedHashSet)), new CustomFunctionCallbackTimerQueue<>(CustomFunctionCallbackTimers.SERVER_CALLBACKS, dynamic.get("ScheduledEvents").asStream()), (NBTTagCompound)dynamic.get("CustomBossEvents").orElseEmptyMap().getValue(), compoundTag, levelInfo, generatorOptions, lifecycle);
    }

    @Override
    public NBTTagCompound createTag(IRegistryCustom registryManager, @Nullable NBTTagCompound playerNbt) {
        this.updatePlayerTag();
        if (playerNbt == null) {
            playerNbt = this.loadedPlayerTag;
        }

        NBTTagCompound compoundTag = new NBTTagCompound();
        this.setTagData(registryManager, compoundTag, playerNbt);
        return compoundTag;
    }

    private void setTagData(IRegistryCustom registryManager, NBTTagCompound levelTag, @Nullable NBTTagCompound playerTag) {
        NBTTagList listTag = new NBTTagList();
        this.knownServerBrands.stream().map(NBTTagString::valueOf).forEach(listTag::add);
        levelTag.set("ServerBrands", listTag);
        levelTag.setBoolean("WasModded", this.wasModded);
        NBTTagCompound compoundTag = new NBTTagCompound();
        compoundTag.setString("Name", SharedConstants.getGameVersion().getName());
        compoundTag.setInt("Id", SharedConstants.getGameVersion().getWorldVersion());
        compoundTag.setBoolean("Snapshot", !SharedConstants.getGameVersion().isStable());
        levelTag.set("Version", compoundTag);
        levelTag.setInt("DataVersion", SharedConstants.getGameVersion().getWorldVersion());
        RegistryWriteOps<NBTBase> registryWriteOps = RegistryWriteOps.create(DynamicOpsNBT.INSTANCE, registryManager);
        GeneratorSettings.CODEC.encodeStart(registryWriteOps, this.worldGenSettings).resultOrPartial(SystemUtils.prefix("WorldGenSettings: ", LOGGER::error)).ifPresent((tag) -> {
            levelTag.set("WorldGenSettings", tag);
        });
        levelTag.setInt("GameType", this.settings.getGameType().getId());
        levelTag.setInt("SpawnX", this.xSpawn);
        levelTag.setInt("SpawnY", this.ySpawn);
        levelTag.setInt("SpawnZ", this.zSpawn);
        levelTag.setFloat("SpawnAngle", this.spawnAngle);
        levelTag.setLong("Time", this.gameTime);
        levelTag.setLong("DayTime", this.dayTime);
        levelTag.setLong("LastPlayed", SystemUtils.getTimeMillis());
        levelTag.setString("LevelName", this.settings.getLevelName());
        levelTag.setInt("version", 19133);
        levelTag.setInt("clearWeatherTime", this.clearWeatherTime);
        levelTag.setInt("rainTime", this.rainTime);
        levelTag.setBoolean("raining", this.raining);
        levelTag.setInt("thunderTime", this.thunderTime);
        levelTag.setBoolean("thundering", this.thundering);
        levelTag.setBoolean("hardcore", this.settings.isHardcore());
        levelTag.setBoolean("allowCommands", this.settings.allowCommands());
        levelTag.setBoolean("initialized", this.initialized);
        this.worldBorder.write(levelTag);
        levelTag.setByte("Difficulty", (byte)this.settings.getDifficulty().getId());
        levelTag.setBoolean("DifficultyLocked", this.difficultyLocked);
        levelTag.set("GameRules", this.settings.getGameRules().createTag());
        levelTag.set("DragonFight", this.endDragonFightData);
        if (playerTag != null) {
            levelTag.set("Player", playerTag);
        }

        DataPackConfiguration.CODEC.encodeStart(DynamicOpsNBT.INSTANCE, this.settings.getDataPackConfig()).result().ifPresent((tag) -> {
            levelTag.set("DataPacks", tag);
        });
        if (this.customBossEvents != null) {
            levelTag.set("CustomBossEvents", this.customBossEvents);
        }

        levelTag.set("ScheduledEvents", this.scheduledEvents.store());
        levelTag.setInt("WanderingTraderSpawnDelay", this.wanderingTraderSpawnDelay);
        levelTag.setInt("WanderingTraderSpawnChance", this.wanderingTraderSpawnChance);
        if (this.wanderingTraderId != null) {
            levelTag.putUUID("WanderingTraderId", this.wanderingTraderId);
        }

    }

    @Override
    public int getXSpawn() {
        return this.xSpawn;
    }

    @Override
    public int getYSpawn() {
        return this.ySpawn;
    }

    @Override
    public int getZSpawn() {
        return this.zSpawn;
    }

    @Override
    public float getSpawnAngle() {
        return this.spawnAngle;
    }

    @Override
    public long getTime() {
        return this.gameTime;
    }

    @Override
    public long getDayTime() {
        return this.dayTime;
    }

    private void updatePlayerTag() {
        if (!this.upgradedPlayerTag && this.loadedPlayerTag != null) {
            if (this.playerDataVersion < SharedConstants.getGameVersion().getWorldVersion()) {
                if (this.fixerUpper == null) {
                    throw (NullPointerException)SystemUtils.pauseInIde(new NullPointerException("Fixer Upper not set inside LevelData, and the player tag is not upgraded."));
                }

                this.loadedPlayerTag = GameProfileSerializer.update(this.fixerUpper, DataFixTypes.PLAYER, this.loadedPlayerTag, this.playerDataVersion);
            }

            this.upgradedPlayerTag = true;
        }
    }

    @Override
    public NBTTagCompound getLoadedPlayerTag() {
        this.updatePlayerTag();
        return this.loadedPlayerTag;
    }

    @Override
    public void setXSpawn(int spawnX) {
        this.xSpawn = spawnX;
    }

    @Override
    public void setYSpawn(int spawnY) {
        this.ySpawn = spawnY;
    }

    @Override
    public void setZSpawn(int spawnZ) {
        this.zSpawn = spawnZ;
    }

    @Override
    public void setSpawnAngle(float angle) {
        this.spawnAngle = angle;
    }

    @Override
    public void setTime(long time) {
        this.gameTime = time;
    }

    @Override
    public void setDayTime(long timeOfDay) {
        this.dayTime = timeOfDay;
    }

    @Override
    public void setSpawn(BlockPosition pos, float angle) {
        this.xSpawn = pos.getX();
        this.ySpawn = pos.getY();
        this.zSpawn = pos.getZ();
        this.spawnAngle = angle;
    }

    @Override
    public String getName() {
        return this.settings.getLevelName();
    }

    @Override
    public int getVersion() {
        return this.version;
    }

    @Override
    public int getClearWeatherTime() {
        return this.clearWeatherTime;
    }

    @Override
    public void setClearWeatherTime(int clearWeatherTime) {
        this.clearWeatherTime = clearWeatherTime;
    }

    @Override
    public boolean isThundering() {
        return this.thundering;
    }

    @Override
    public void setThundering(boolean thundering) {
        this.thundering = thundering;
    }

    @Override
    public int getThunderDuration() {
        return this.thunderTime;
    }

    @Override
    public void setThunderDuration(int thunderTime) {
        this.thunderTime = thunderTime;
    }

    @Override
    public boolean hasStorm() {
        return this.raining;
    }

    @Override
    public void setStorm(boolean raining) {
        this.raining = raining;
    }

    @Override
    public int getWeatherDuration() {
        return this.rainTime;
    }

    @Override
    public void setWeatherDuration(int rainTime) {
        this.rainTime = rainTime;
    }

    @Override
    public EnumGamemode getGameType() {
        return this.settings.getGameType();
    }

    @Override
    public void setGameType(EnumGamemode gameMode) {
        this.settings = this.settings.withGameType(gameMode);
    }

    @Override
    public boolean isHardcore() {
        return this.settings.isHardcore();
    }

    @Override
    public boolean getAllowCommands() {
        return this.settings.allowCommands();
    }

    @Override
    public boolean isInitialized() {
        return this.initialized;
    }

    @Override
    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    @Override
    public GameRules getGameRules() {
        return this.settings.getGameRules();
    }

    @Override
    public WorldBorder.Settings getWorldBorder() {
        return this.worldBorder;
    }

    @Override
    public void setWorldBorder(WorldBorder.Settings properties) {
        this.worldBorder = properties;
    }

    @Override
    public EnumDifficulty getDifficulty() {
        return this.settings.getDifficulty();
    }

    @Override
    public void setDifficulty(EnumDifficulty difficulty) {
        this.settings = this.settings.withDifficulty(difficulty);
    }

    @Override
    public boolean isDifficultyLocked() {
        return this.difficultyLocked;
    }

    @Override
    public void setDifficultyLocked(boolean locked) {
        this.difficultyLocked = locked;
    }

    @Override
    public CustomFunctionCallbackTimerQueue<MinecraftServer> getScheduledEvents() {
        return this.scheduledEvents;
    }

    @Override
    public void fillCrashReportCategory(CrashReportSystemDetails reportSection, IWorldHeightAccess world) {
        IWorldDataServer.super.fillCrashReportCategory(reportSection, world);
        SaveData.super.fillCrashReportCategory(reportSection);
    }

    @Override
    public GeneratorSettings getGeneratorSettings() {
        return this.worldGenSettings;
    }

    @Override
    public Lifecycle worldGenSettingsLifecycle() {
        return this.worldGenSettingsLifecycle;
    }

    @Override
    public NBTTagCompound endDragonFightData() {
        return this.endDragonFightData;
    }

    @Override
    public void setEndDragonFightData(NBTTagCompound nbt) {
        this.endDragonFightData = nbt;
    }

    @Override
    public DataPackConfiguration getDataPackConfig() {
        return this.settings.getDataPackConfig();
    }

    @Override
    public void setDataPackConfig(DataPackConfiguration dataPackSettings) {
        this.settings = this.settings.withDataPackConfig(dataPackSettings);
    }

    @Nullable
    @Override
    public NBTTagCompound getCustomBossEvents() {
        return this.customBossEvents;
    }

    @Override
    public void setCustomBossEvents(@Nullable NBTTagCompound nbt) {
        this.customBossEvents = nbt;
    }

    @Override
    public int getWanderingTraderSpawnDelay() {
        return this.wanderingTraderSpawnDelay;
    }

    @Override
    public void setWanderingTraderSpawnDelay(int wanderingTraderSpawnDelay) {
        this.wanderingTraderSpawnDelay = wanderingTraderSpawnDelay;
    }

    @Override
    public int getWanderingTraderSpawnChance() {
        return this.wanderingTraderSpawnChance;
    }

    @Override
    public void setWanderingTraderSpawnChance(int wanderingTraderSpawnChance) {
        this.wanderingTraderSpawnChance = wanderingTraderSpawnChance;
    }

    @Nullable
    @Override
    public UUID getWanderingTraderId() {
        return this.wanderingTraderId;
    }

    @Override
    public void setWanderingTraderId(UUID uuid) {
        this.wanderingTraderId = uuid;
    }

    @Override
    public void setModdedInfo(String brand, boolean modded) {
        this.knownServerBrands.add(brand);
        this.wasModded |= modded;
    }

    @Override
    public boolean wasModded() {
        return this.wasModded;
    }

    @Override
    public Set<String> getKnownServerBrands() {
        return ImmutableSet.copyOf(this.knownServerBrands);
    }

    @Override
    public IWorldDataServer overworldData() {
        return this;
    }

    @Override
    public WorldSettings getLevelSettings() {
        return this.settings.copy();
    }
}
