package net.minecraft.server.level;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.mojang.datafixers.DataFixer;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.IRegistry;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.core.SectionPosition;
import net.minecraft.core.particles.ParticleParam;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddVibrationSignalPacket;
import net.minecraft.network.protocol.game.PacketDebug;
import net.minecraft.network.protocol.game.PacketPlayOutBlockAction;
import net.minecraft.network.protocol.game.PacketPlayOutBlockBreakAnimation;
import net.minecraft.network.protocol.game.PacketPlayOutEntitySound;
import net.minecraft.network.protocol.game.PacketPlayOutEntityStatus;
import net.minecraft.network.protocol.game.PacketPlayOutExplosion;
import net.minecraft.network.protocol.game.PacketPlayOutGameStateChange;
import net.minecraft.network.protocol.game.PacketPlayOutNamedSoundEffect;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnPosition;
import net.minecraft.network.protocol.game.PacketPlayOutWorldEvent;
import net.minecraft.network.protocol.game.PacketPlayOutWorldParticles;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ScoreboardServer;
import net.minecraft.server.level.progress.WorldLoadListener;
import net.minecraft.server.players.SleepStatus;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.tags.ITagRegistry;
import net.minecraft.util.CSVWriter;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.GameProfilerFiller;
import net.minecraft.world.DifficultyDamageScaler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLightning;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumCreatureType;
import net.minecraft.world.entity.ReputationHandler;
import net.minecraft.world.entity.ai.navigation.NavigationAbstract;
import net.minecraft.world.entity.ai.village.ReputationEvent;
import net.minecraft.world.entity.ai.village.poi.VillagePlace;
import net.minecraft.world.entity.ai.village.poi.VillagePlaceType;
import net.minecraft.world.entity.animal.EntityAnimal;
import net.minecraft.world.entity.animal.EntityWaterAnimal;
import net.minecraft.world.entity.animal.horse.EntityHorseSkeleton;
import net.minecraft.world.entity.boss.EntityComplexPart;
import net.minecraft.world.entity.boss.enderdragon.EntityEnderDragon;
import net.minecraft.world.entity.npc.NPC;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.raid.PersistentRaid;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.item.crafting.CraftingManager;
import net.minecraft.world.level.BlockActionData;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.ForcedChunk;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.MobSpawner;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.NextTickListEntry;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.TickListServer;
import net.minecraft.world.level.World;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.Chunk;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkSection;
import net.minecraft.world.level.chunk.storage.EntityStorage;
import net.minecraft.world.level.dimension.DimensionManager;
import net.minecraft.world.level.dimension.end.EnderDragonBattle;
import net.minecraft.world.level.entity.EntityPersistentStorage;
import net.minecraft.world.level.entity.EntitySectionManagerPersistent;
import net.minecraft.world.level.entity.EntityTickList;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.IWorldEntityAccess;
import net.minecraft.world.level.entity.WorldCallback;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListenerRegistrar;
import net.minecraft.world.level.gameevent.vibrations.VibrationPath;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.feature.StructureGenerator;
import net.minecraft.world.level.levelgen.structure.StructureBoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureManager;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidType;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.level.portal.PortalTravelAgent;
import net.minecraft.world.level.saveddata.maps.PersistentIdCounts;
import net.minecraft.world.level.saveddata.maps.WorldMap;
import net.minecraft.world.level.storage.Convertable;
import net.minecraft.world.level.storage.IWorldDataServer;
import net.minecraft.world.level.storage.WorldPersistentData;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.Vec3D;
import net.minecraft.world.phys.shapes.OperatorBoolean;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WorldServer extends World implements GeneratorAccessSeed {
    public static final BlockPosition END_SPAWN_POINT = new BlockPosition(100, 50, 0);
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int EMPTY_TIME_NO_TICK = 300;
    public final List<EntityPlayer> players = Lists.newArrayList();
    public final ChunkProviderServer chunkSource;
    private final MinecraftServer server;
    public final IWorldDataServer serverLevelData;
    final EntityTickList entityTickList = new EntityTickList();
    public final EntitySectionManagerPersistent<Entity> entityManager;
    public boolean noSave;
    private final SleepStatus sleepStatus;
    private int emptyTime;
    private final PortalTravelAgent portalForcer;
    private final TickListServer<Block> blockTicks = new TickListServer<>(this, (block) -> {
        return block == null || block.getBlockData().isAir();
    }, IRegistry.BLOCK::getKey, this::tickBlock);
    private final TickListServer<FluidType> liquidTicks = new TickListServer<>(this, (fluid) -> {
        return fluid == null || fluid == FluidTypes.EMPTY;
    }, IRegistry.FLUID::getKey, this::tickLiquid);
    final Set<EntityInsentient> navigatingMobs = new ObjectOpenHashSet<>();
    protected final PersistentRaid raids;
    private final ObjectLinkedOpenHashSet<BlockActionData> blockEvents = new ObjectLinkedOpenHashSet<>();
    private boolean handlingTick;
    private final List<MobSpawner> customSpawners;
    @Nullable
    private final EnderDragonBattle dragonFight;
    final Int2ObjectMap<EntityComplexPart> dragonParts = new Int2ObjectOpenHashMap<>();
    private final StructureManager structureFeatureManager;
    private final boolean tickTime;

    public WorldServer(MinecraftServer server, Executor workerExecutor, Convertable.ConversionSession session, IWorldDataServer properties, ResourceKey<World> worldKey, DimensionManager dimensionType, WorldLoadListener worldGenerationProgressListener, ChunkGenerator chunkGenerator, boolean debugWorld, long seed, List<MobSpawner> spawners, boolean shouldTickTime) {
        super(properties, worldKey, dimensionType, server::getMethodProfiler, false, debugWorld, seed);
        this.tickTime = shouldTickTime;
        this.server = server;
        this.customSpawners = spawners;
        this.serverLevelData = properties;
        boolean bl = server.isSyncChunkWrites();
        DataFixer dataFixer = server.getDataFixer();
        EntityPersistentStorage<Entity> entityPersistentStorage = new EntityStorage(this, new File(session.getDimensionPath(worldKey), "entities"), dataFixer, bl, server);
        this.entityManager = new EntitySectionManagerPersistent<>(Entity.class, new WorldServer.EntityCallbacks(), entityPersistentStorage);
        this.chunkSource = new ChunkProviderServer(this, session, dataFixer, server.getDefinedStructureManager(), workerExecutor, chunkGenerator, server.getPlayerList().getViewDistance(), bl, worldGenerationProgressListener, this.entityManager::updateChunkStatus, () -> {
            return server.overworld().getWorldPersistentData();
        });
        this.portalForcer = new PortalTravelAgent(this);
        this.updateSkyBrightness();
        this.prepareWeather();
        this.getWorldBorder().setAbsoluteMaxSize(server.getAbsoluteMaxWorldSize());
        this.raids = this.getWorldPersistentData().computeIfAbsent((compoundTag) -> {
            return PersistentRaid.load(this, compoundTag);
        }, () -> {
            return new PersistentRaid(this);
        }, PersistentRaid.getFileId(this.getDimensionManager()));
        if (!server.isEmbeddedServer()) {
            properties.setGameType(server.getGamemode());
        }

        this.structureFeatureManager = new StructureManager(this, server.getSaveData().getGeneratorSettings());
        if (this.getDimensionManager().isCreateDragonBattle()) {
            this.dragonFight = new EnderDragonBattle(this, server.getSaveData().getGeneratorSettings().getSeed(), server.getSaveData().endDragonFightData());
        } else {
            this.dragonFight = null;
        }

        this.sleepStatus = new SleepStatus();
    }

    public void setWeatherParameters(int clearDuration, int rainDuration, boolean raining, boolean thundering) {
        this.serverLevelData.setClearWeatherTime(clearDuration);
        this.serverLevelData.setWeatherDuration(rainDuration);
        this.serverLevelData.setThunderDuration(rainDuration);
        this.serverLevelData.setStorm(raining);
        this.serverLevelData.setThundering(thundering);
    }

    @Override
    public BiomeBase getUncachedNoiseBiome(int biomeX, int biomeY, int biomeZ) {
        return this.getChunkSource().getChunkGenerator().getWorldChunkManager().getBiome(biomeX, biomeY, biomeZ);
    }

    public StructureManager getStructureManager() {
        return this.structureFeatureManager;
    }

    public void doTick(BooleanSupplier shouldKeepTicking) {
        GameProfilerFiller profilerFiller = this.getMethodProfiler();
        this.handlingTick = true;
        profilerFiller.enter("world border");
        this.getWorldBorder().tick();
        profilerFiller.exitEnter("weather");
        boolean bl = this.isRaining();
        if (this.getDimensionManager().hasSkyLight()) {
            if (this.getGameRules().getBoolean(GameRules.RULE_WEATHER_CYCLE)) {
                int i = this.serverLevelData.getClearWeatherTime();
                int j = this.serverLevelData.getThunderDuration();
                int k = this.serverLevelData.getWeatherDuration();
                boolean bl2 = this.levelData.isThundering();
                boolean bl3 = this.levelData.hasStorm();
                if (i > 0) {
                    --i;
                    j = bl2 ? 0 : 1;
                    k = bl3 ? 0 : 1;
                    bl2 = false;
                    bl3 = false;
                } else {
                    if (j > 0) {
                        --j;
                        if (j == 0) {
                            bl2 = !bl2;
                        }
                    } else if (bl2) {
                        j = this.random.nextInt(12000) + 3600;
                    } else {
                        j = this.random.nextInt(168000) + 12000;
                    }

                    if (k > 0) {
                        --k;
                        if (k == 0) {
                            bl3 = !bl3;
                        }
                    } else if (bl3) {
                        k = this.random.nextInt(12000) + 12000;
                    } else {
                        k = this.random.nextInt(168000) + 12000;
                    }
                }

                this.serverLevelData.setThunderDuration(j);
                this.serverLevelData.setWeatherDuration(k);
                this.serverLevelData.setClearWeatherTime(i);
                this.serverLevelData.setThundering(bl2);
                this.serverLevelData.setStorm(bl3);
            }

            this.oThunderLevel = this.thunderLevel;
            if (this.levelData.isThundering()) {
                this.thunderLevel = (float)((double)this.thunderLevel + 0.01D);
            } else {
                this.thunderLevel = (float)((double)this.thunderLevel - 0.01D);
            }

            this.thunderLevel = MathHelper.clamp(this.thunderLevel, 0.0F, 1.0F);
            this.oRainLevel = this.rainLevel;
            if (this.levelData.hasStorm()) {
                this.rainLevel = (float)((double)this.rainLevel + 0.01D);
            } else {
                this.rainLevel = (float)((double)this.rainLevel - 0.01D);
            }

            this.rainLevel = MathHelper.clamp(this.rainLevel, 0.0F, 1.0F);
        }

        if (this.oRainLevel != this.rainLevel) {
            this.server.getPlayerList().broadcastAll(new PacketPlayOutGameStateChange(PacketPlayOutGameStateChange.RAIN_LEVEL_CHANGE, this.rainLevel), this.getDimensionKey());
        }

        if (this.oThunderLevel != this.thunderLevel) {
            this.server.getPlayerList().broadcastAll(new PacketPlayOutGameStateChange(PacketPlayOutGameStateChange.THUNDER_LEVEL_CHANGE, this.thunderLevel), this.getDimensionKey());
        }

        if (bl != this.isRaining()) {
            if (bl) {
                this.server.getPlayerList().sendAll(new PacketPlayOutGameStateChange(PacketPlayOutGameStateChange.STOP_RAINING, 0.0F));
            } else {
                this.server.getPlayerList().sendAll(new PacketPlayOutGameStateChange(PacketPlayOutGameStateChange.START_RAINING, 0.0F));
            }

            this.server.getPlayerList().sendAll(new PacketPlayOutGameStateChange(PacketPlayOutGameStateChange.RAIN_LEVEL_CHANGE, this.rainLevel));
            this.server.getPlayerList().sendAll(new PacketPlayOutGameStateChange(PacketPlayOutGameStateChange.THUNDER_LEVEL_CHANGE, this.thunderLevel));
        }

        int l = this.getGameRules().getInt(GameRules.RULE_PLAYERS_SLEEPING_PERCENTAGE);
        if (this.sleepStatus.areEnoughSleeping(l) && this.sleepStatus.areEnoughDeepSleeping(l, this.players)) {
            if (this.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)) {
                long m = this.levelData.getDayTime() + 24000L;
                this.setDayTime(m - m % 24000L);
            }

            this.wakeupPlayers();
            if (this.getGameRules().getBoolean(GameRules.RULE_WEATHER_CYCLE)) {
                this.clearWeather();
            }
        }

        this.updateSkyBrightness();
        this.tickTime();
        profilerFiller.exitEnter("tickPending");
        if (!this.isDebugWorld()) {
            this.blockTicks.tick();
            this.liquidTicks.tick();
        }

        profilerFiller.exitEnter("raid");
        this.raids.tick();
        profilerFiller.exitEnter("chunkSource");
        this.getChunkSource().tick(shouldKeepTicking);
        profilerFiller.exitEnter("blockEvents");
        this.runBlockEvents();
        this.handlingTick = false;
        profilerFiller.exit();
        boolean bl4 = !this.players.isEmpty() || !this.getForceLoadedChunks().isEmpty();
        if (bl4) {
            this.resetEmptyTime();
        }

        if (bl4 || this.emptyTime++ < 300) {
            profilerFiller.enter("entities");
            if (this.dragonFight != null) {
                profilerFiller.enter("dragonFight");
                this.dragonFight.tick();
                profilerFiller.exit();
            }

            this.entityTickList.forEach((entity) -> {
                if (!entity.isRemoved()) {
                    if (this.shouldDiscardEntity(entity)) {
                        entity.die();
                    } else {
                        profilerFiller.enter("checkDespawn");
                        entity.checkDespawn();
                        profilerFiller.exit();
                        Entity entity2 = entity.getVehicle();
                        if (entity2 != null) {
                            if (!entity2.isRemoved() && entity2.hasPassenger(entity)) {
                                return;
                            }

                            entity.stopRiding();
                        }

                        profilerFiller.enter("tick");
                        this.guardEntityTick(this::entityJoinedWorld, entity);
                        profilerFiller.exit();
                    }
                }
            });
            profilerFiller.exit();
            this.tickBlockEntities();
        }

        profilerFiller.enter("entityManagement");
        this.entityManager.tick();
        profilerFiller.exit();
    }

    protected void tickTime() {
        if (this.tickTime) {
            long l = this.levelData.getTime() + 1L;
            this.serverLevelData.setTime(l);
            this.serverLevelData.getScheduledEvents().tick(this.server, l);
            if (this.levelData.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)) {
                this.setDayTime(this.levelData.getDayTime() + 1L);
            }

        }
    }

    public void setDayTime(long timeOfDay) {
        this.serverLevelData.setDayTime(timeOfDay);
    }

    public void doMobSpawning(boolean spawnMonsters, boolean spawnAnimals) {
        for(MobSpawner customSpawner : this.customSpawners) {
            customSpawner.tick(this, spawnMonsters, spawnAnimals);
        }

    }

    private boolean shouldDiscardEntity(Entity entity) {
        if (this.server.getSpawnAnimals() || !(entity instanceof EntityAnimal) && !(entity instanceof EntityWaterAnimal)) {
            return !this.server.getSpawnNPCs() && entity instanceof NPC;
        } else {
            return true;
        }
    }

    private void wakeupPlayers() {
        this.sleepStatus.removeAllSleepers();
        this.players.stream().filter(EntityLiving::isSleeping).collect(Collectors.toList()).forEach((player) -> {
            player.wakeup(false, false);
        });
    }

    public void tickChunk(Chunk chunk, int randomTickSpeed) {
        ChunkCoordIntPair chunkPos = chunk.getPos();
        boolean bl = this.isRaining();
        int i = chunkPos.getMinBlockX();
        int j = chunkPos.getMinBlockZ();
        GameProfilerFiller profilerFiller = this.getMethodProfiler();
        profilerFiller.enter("thunder");
        if (bl && this.isThundering() && this.random.nextInt(100000) == 0) {
            BlockPosition blockPos = this.findLightningTargetAround(this.getBlockRandomPos(i, 0, j, 15));
            if (this.isRainingAt(blockPos)) {
                DifficultyDamageScaler difficultyInstance = this.getDamageScaler(blockPos);
                boolean bl2 = this.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING) && this.random.nextDouble() < (double)difficultyInstance.getEffectiveDifficulty() * 0.01D && !this.getType(blockPos.below()).is(Blocks.LIGHTNING_ROD);
                if (bl2) {
                    EntityHorseSkeleton skeletonHorse = EntityTypes.SKELETON_HORSE.create(this);
                    skeletonHorse.setTrap(true);
                    skeletonHorse.setAgeRaw(0);
                    skeletonHorse.setPosition((double)blockPos.getX(), (double)blockPos.getY(), (double)blockPos.getZ());
                    this.addEntity(skeletonHorse);
                }

                EntityLightning lightningBolt = EntityTypes.LIGHTNING_BOLT.create(this);
                lightningBolt.moveTo(Vec3D.atBottomCenterOf(blockPos));
                lightningBolt.setEffect(bl2);
                this.addEntity(lightningBolt);
            }
        }

        profilerFiller.exitEnter("iceandsnow");
        if (this.random.nextInt(16) == 0) {
            BlockPosition blockPos2 = this.getHighestBlockYAt(HeightMap.Type.MOTION_BLOCKING, this.getBlockRandomPos(i, 0, j, 15));
            BlockPosition blockPos3 = blockPos2.below();
            BiomeBase biome = this.getBiome(blockPos2);
            if (biome.shouldFreeze(this, blockPos3)) {
                this.setTypeUpdate(blockPos3, Blocks.ICE.getBlockData());
            }

            if (bl) {
                if (biome.shouldSnow(this, blockPos2)) {
                    this.setTypeUpdate(blockPos2, Blocks.SNOW.getBlockData());
                }

                IBlockData blockState = this.getType(blockPos3);
                BiomeBase.Precipitation precipitation = this.getBiome(blockPos2).getPrecipitation();
                if (precipitation == BiomeBase.Precipitation.RAIN && biome.isColdEnoughToSnow(blockPos3)) {
                    precipitation = BiomeBase.Precipitation.SNOW;
                }

                blockState.getBlock().handlePrecipitation(blockState, this, blockPos3, precipitation);
            }
        }

        profilerFiller.exitEnter("tickBlocks");
        if (randomTickSpeed > 0) {
            for(ChunkSection levelChunkSection : chunk.getSections()) {
                if (levelChunkSection != Chunk.EMPTY_SECTION && levelChunkSection.isRandomlyTicking()) {
                    int k = levelChunkSection.getYPosition();

                    for(int l = 0; l < randomTickSpeed; ++l) {
                        BlockPosition blockPos4 = this.getBlockRandomPos(i, k, j, 15);
                        profilerFiller.enter("randomTick");
                        IBlockData blockState2 = levelChunkSection.getType(blockPos4.getX() - i, blockPos4.getY() - k, blockPos4.getZ() - j);
                        if (blockState2.isTicking()) {
                            blockState2.randomTick(this, blockPos4, this.random);
                        }

                        Fluid fluidState = blockState2.getFluid();
                        if (fluidState.isRandomlyTicking()) {
                            fluidState.randomTick(this, blockPos4, this.random);
                        }

                        profilerFiller.exit();
                    }
                }
            }
        }

        profilerFiller.exit();
    }

    public Optional<BlockPosition> findLightningRod(BlockPosition pos) {
        Optional<BlockPosition> optional = this.getPoiManager().findClosest((poiType) -> {
            return poiType == VillagePlaceType.LIGHTNING_ROD;
        }, (posx) -> {
            return posx.getY() == this.getLevel().getHeight(HeightMap.Type.WORLD_SURFACE, posx.getX(), posx.getZ()) - 1;
        }, pos, 128, VillagePlace.Occupancy.ANY);
        return optional.map((posx) -> {
            return posx.above(1);
        });
    }

    protected BlockPosition findLightningTargetAround(BlockPosition pos) {
        BlockPosition blockPos = this.getHighestBlockYAt(HeightMap.Type.MOTION_BLOCKING, pos);
        Optional<BlockPosition> optional = this.findLightningRod(blockPos);
        if (optional.isPresent()) {
            return optional.get();
        } else {
            AxisAlignedBB aABB = (new AxisAlignedBB(blockPos, new BlockPosition(blockPos.getX(), this.getMaxBuildHeight(), blockPos.getZ()))).inflate(3.0D);
            List<EntityLiving> list = this.getEntitiesOfClass(EntityLiving.class, aABB, (entity) -> {
                return entity != null && entity.isAlive() && this.canSeeSky(entity.getChunkCoordinates());
            });
            if (!list.isEmpty()) {
                return list.get(this.random.nextInt(list.size())).getChunkCoordinates();
            } else {
                if (blockPos.getY() == this.getMinBuildHeight() - 1) {
                    blockPos = blockPos.above(2);
                }

                return blockPos;
            }
        }
    }

    public boolean isHandlingTick() {
        return this.handlingTick;
    }

    public boolean canSleepThroughNights() {
        return this.getGameRules().getInt(GameRules.RULE_PLAYERS_SLEEPING_PERCENTAGE) <= 100;
    }

    private void announceSleepStatus() {
        if (this.canSleepThroughNights()) {
            if (!this.getMinecraftServer().isEmbeddedServer() || this.getMinecraftServer().isPublished()) {
                int i = this.getGameRules().getInt(GameRules.RULE_PLAYERS_SLEEPING_PERCENTAGE);
                IChatBaseComponent component;
                if (this.sleepStatus.areEnoughSleeping(i)) {
                    component = new ChatMessage("sleep.skipping_night");
                } else {
                    component = new ChatMessage("sleep.players_sleeping", this.sleepStatus.amountSleeping(), this.sleepStatus.sleepersNeeded(i));
                }

                for(EntityPlayer serverPlayer : this.players) {
                    serverPlayer.displayClientMessage(component, true);
                }

            }
        }
    }

    public void everyoneSleeping() {
        if (!this.players.isEmpty() && this.sleepStatus.update(this.players)) {
            this.announceSleepStatus();
        }

    }

    @Override
    public ScoreboardServer getScoreboard() {
        return this.server.getScoreboard();
    }

    private void clearWeather() {
        this.serverLevelData.setWeatherDuration(0);
        this.serverLevelData.setStorm(false);
        this.serverLevelData.setThunderDuration(0);
        this.serverLevelData.setThundering(false);
    }

    public void resetEmptyTime() {
        this.emptyTime = 0;
    }

    private void tickLiquid(NextTickListEntry<FluidType> tick) {
        Fluid fluidState = this.getFluid(tick.pos);
        if (fluidState.getType() == tick.getType()) {
            fluidState.tick(this, tick.pos);
        }

    }

    private void tickBlock(NextTickListEntry<Block> tick) {
        IBlockData blockState = this.getType(tick.pos);
        if (blockState.is(tick.getType())) {
            blockState.tick(this, tick.pos, this.random);
        }

    }

    public void entityJoinedWorld(Entity entity) {
        entity.setOldPosAndRot();
        GameProfilerFiller profilerFiller = this.getMethodProfiler();
        ++entity.tickCount;
        this.getMethodProfiler().push(() -> {
            return IRegistry.ENTITY_TYPE.getKey(entity.getEntityType()).toString();
        });
        profilerFiller.incrementCounter("tickNonPassenger");
        entity.tick();
        this.getMethodProfiler().exit();

        for(Entity entity2 : entity.getPassengers()) {
            this.tickPassenger(entity, entity2);
        }

    }

    private void tickPassenger(Entity vehicle, Entity passenger) {
        if (!passenger.isRemoved() && passenger.getVehicle() == vehicle) {
            if (passenger instanceof EntityHuman || this.entityTickList.contains(passenger)) {
                passenger.setOldPosAndRot();
                ++passenger.tickCount;
                GameProfilerFiller profilerFiller = this.getMethodProfiler();
                profilerFiller.push(() -> {
                    return IRegistry.ENTITY_TYPE.getKey(passenger.getEntityType()).toString();
                });
                profilerFiller.incrementCounter("tickPassenger");
                passenger.passengerTick();
                profilerFiller.exit();

                for(Entity entity : passenger.getPassengers()) {
                    this.tickPassenger(passenger, entity);
                }

            }
        } else {
            passenger.stopRiding();
        }
    }

    @Override
    public boolean mayInteract(EntityHuman player, BlockPosition pos) {
        return !this.server.isUnderSpawnProtection(this, pos, player) && this.getWorldBorder().isWithinBounds(pos);
    }

    public void save(@Nullable IProgressUpdate progressListener, boolean flush, boolean bl) {
        ChunkProviderServer serverChunkCache = this.getChunkSource();
        if (!bl) {
            if (progressListener != null) {
                progressListener.progressStartNoAbort(new ChatMessage("menu.savingLevel"));
            }

            this.saveLevelData();
            if (progressListener != null) {
                progressListener.progressStage(new ChatMessage("menu.savingChunks"));
            }

            serverChunkCache.save(flush);
            if (flush) {
                this.entityManager.saveAll();
            } else {
                this.entityManager.autoSave();
            }

        }
    }

    private void saveLevelData() {
        if (this.dragonFight != null) {
            this.server.getSaveData().setEndDragonFightData(this.dragonFight.saveData());
        }

        this.getChunkSource().getWorldPersistentData().save();
    }

    public <T extends Entity> List<? extends T> getEntities(EntityTypeTest<Entity, T> entityTypeTest, Predicate<? super T> predicate) {
        List<T> list = Lists.newArrayList();
        this.getEntities().get(entityTypeTest, (entity) -> {
            if (predicate.test(entity)) {
                list.add(entity);
            }

        });
        return list;
    }

    public List<? extends EntityEnderDragon> getDragons() {
        return this.getEntities(EntityTypes.ENDER_DRAGON, EntityLiving::isAlive);
    }

    public List<EntityPlayer> getPlayers(Predicate<? super EntityPlayer> predicate) {
        List<EntityPlayer> list = Lists.newArrayList();

        for(EntityPlayer serverPlayer : this.players) {
            if (predicate.test(serverPlayer)) {
                list.add(serverPlayer);
            }
        }

        return list;
    }

    @Nullable
    public EntityPlayer getRandomPlayer() {
        List<EntityPlayer> list = this.getPlayers(EntityLiving::isAlive);
        return list.isEmpty() ? null : list.get(this.random.nextInt(list.size()));
    }

    @Override
    public boolean addEntity(Entity entity) {
        return this.addEntity0(entity);
    }

    public boolean addEntitySerialized(Entity entity) {
        return this.addEntity0(entity);
    }

    public void addEntityTeleport(Entity entity) {
        this.addEntity0(entity);
    }

    public void addPlayerCommand(EntityPlayer player) {
        this.addPlayer0(player);
    }

    public void addPlayerPortal(EntityPlayer player) {
        this.addPlayer0(player);
    }

    public void addPlayerJoin(EntityPlayer player) {
        this.addPlayer0(player);
    }

    public void addPlayerRespawn(EntityPlayer player) {
        this.addPlayer0(player);
    }

    private void addPlayer0(EntityPlayer player) {
        Entity entity = this.getEntities().get(player.getUniqueID());
        if (entity != null) {
            LOGGER.warn("Force-added player with duplicate UUID {}", (Object)player.getUniqueID().toString());
            entity.decouple();
            this.removePlayerImmediately((EntityPlayer)entity, Entity.RemovalReason.DISCARDED);
        }

        this.entityManager.addNewEntity(player);
    }

    private boolean addEntity0(Entity entity) {
        if (entity.isRemoved()) {
            LOGGER.warn("Tried to add entity {} but it was marked as removed already", (Object)EntityTypes.getName(entity.getEntityType()));
            return false;
        } else {
            return this.entityManager.addNewEntity(entity);
        }
    }

    public boolean addAllEntitiesSafely(Entity entity) {
        if (entity.recursiveStream().map(Entity::getUniqueID).anyMatch(this.entityManager::isLoaded)) {
            return false;
        } else {
            this.addAllEntities(entity);
            return true;
        }
    }

    public void unloadChunk(Chunk chunk) {
        chunk.invalidateAllBlockEntities();
    }

    public void removePlayerImmediately(EntityPlayer player, Entity.RemovalReason reason) {
        player.remove(reason);
    }

    @Override
    public void destroyBlockProgress(int entityId, BlockPosition pos, int progress) {
        for(EntityPlayer serverPlayer : this.server.getPlayerList().getPlayers()) {
            if (serverPlayer != null && serverPlayer.level == this && serverPlayer.getId() != entityId) {
                double d = (double)pos.getX() - serverPlayer.locX();
                double e = (double)pos.getY() - serverPlayer.locY();
                double f = (double)pos.getZ() - serverPlayer.locZ();
                if (d * d + e * e + f * f < 1024.0D) {
                    serverPlayer.connection.sendPacket(new PacketPlayOutBlockBreakAnimation(entityId, pos, progress));
                }
            }
        }

    }

    @Override
    public void playSound(@Nullable EntityHuman player, double x, double y, double z, SoundEffect sound, SoundCategory category, float volume, float pitch) {
        this.server.getPlayerList().sendPacketNearby(player, x, y, z, volume > 1.0F ? (double)(16.0F * volume) : 16.0D, this.getDimensionKey(), new PacketPlayOutNamedSoundEffect(sound, category, x, y, z, volume, pitch));
    }

    @Override
    public void playSound(@Nullable EntityHuman player, Entity entity, SoundEffect sound, SoundCategory category, float volume, float pitch) {
        this.server.getPlayerList().sendPacketNearby(player, entity.locX(), entity.locY(), entity.locZ(), volume > 1.0F ? (double)(16.0F * volume) : 16.0D, this.getDimensionKey(), new PacketPlayOutEntitySound(sound, category, entity, volume, pitch));
    }

    @Override
    public void globalLevelEvent(int eventId, BlockPosition pos, int data) {
        this.server.getPlayerList().sendAll(new PacketPlayOutWorldEvent(eventId, pos, data, true));
    }

    @Override
    public void levelEvent(@Nullable EntityHuman player, int eventId, BlockPosition pos, int data) {
        this.server.getPlayerList().sendPacketNearby(player, (double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), 64.0D, this.getDimensionKey(), new PacketPlayOutWorldEvent(eventId, pos, data, false));
    }

    @Override
    public int getLogicalHeight() {
        return this.getDimensionManager().getLogicalHeight();
    }

    @Override
    public void gameEvent(@Nullable Entity entity, GameEvent event, BlockPosition pos) {
        this.postGameEventInRadius(entity, event, pos, event.getNotificationRadius());
    }

    @Override
    public void notify(BlockPosition pos, IBlockData oldState, IBlockData newState, int flags) {
        this.getChunkSource().flagDirty(pos);
        VoxelShape voxelShape = oldState.getCollisionShape(this, pos);
        VoxelShape voxelShape2 = newState.getCollisionShape(this, pos);
        if (VoxelShapes.joinIsNotEmpty(voxelShape, voxelShape2, OperatorBoolean.NOT_SAME)) {
            for(EntityInsentient mob : this.navigatingMobs) {
                NavigationAbstract pathNavigation = mob.getNavigation();
                if (!pathNavigation.hasDelayedRecomputation()) {
                    pathNavigation.recomputePath(pos);
                }
            }

        }
    }

    @Override
    public void broadcastEntityEffect(Entity entity, byte status) {
        this.getChunkSource().broadcastIncludingSelf(entity, new PacketPlayOutEntityStatus(entity, status));
    }

    @Override
    public ChunkProviderServer getChunkSource() {
        return this.chunkSource;
    }

    @Override
    public Explosion createExplosion(@Nullable Entity entity, @Nullable DamageSource damageSource, @Nullable ExplosionDamageCalculator behavior, double x, double y, double z, float power, boolean createFire, Explosion.Effect destructionType) {
        Explosion explosion = new Explosion(this, entity, damageSource, behavior, x, y, z, power, createFire, destructionType);
        explosion.explode();
        explosion.finalizeExplosion(false);
        if (destructionType == Explosion.Effect.NONE) {
            explosion.clearBlocks();
        }

        for(EntityPlayer serverPlayer : this.players) {
            if (serverPlayer.distanceToSqr(x, y, z) < 4096.0D) {
                serverPlayer.connection.sendPacket(new PacketPlayOutExplosion(x, y, z, power, explosion.getBlocks(), explosion.getHitPlayers().get(serverPlayer)));
            }
        }

        return explosion;
    }

    @Override
    public void playBlockAction(BlockPosition pos, Block block, int type, int data) {
        this.blockEvents.add(new BlockActionData(pos, block, type, data));
    }

    private void runBlockEvents() {
        while(!this.blockEvents.isEmpty()) {
            BlockActionData blockEventData = this.blockEvents.removeFirst();
            if (this.doBlockEvent(blockEventData)) {
                this.server.getPlayerList().sendPacketNearby((EntityHuman)null, (double)blockEventData.getPos().getX(), (double)blockEventData.getPos().getY(), (double)blockEventData.getPos().getZ(), 64.0D, this.getDimensionKey(), new PacketPlayOutBlockAction(blockEventData.getPos(), blockEventData.getBlock(), blockEventData.getParamA(), blockEventData.getParamB()));
            }
        }

    }

    private boolean doBlockEvent(BlockActionData event) {
        IBlockData blockState = this.getType(event.getPos());
        return blockState.is(event.getBlock()) ? blockState.triggerEvent(this, event.getPos(), event.getParamA(), event.getParamB()) : false;
    }

    @Override
    public TickListServer<Block> getBlockTicks() {
        return this.blockTicks;
    }

    @Override
    public TickListServer<FluidType> getLiquidTicks() {
        return this.liquidTicks;
    }

    @Nonnull
    @Override
    public MinecraftServer getMinecraftServer() {
        return this.server;
    }

    public PortalTravelAgent getTravelAgent() {
        return this.portalForcer;
    }

    public DefinedStructureManager getStructureManager() {
        return this.server.getDefinedStructureManager();
    }

    public void sendVibrationParticle(VibrationPath vibration) {
        BlockPosition blockPos = vibration.getOrigin();
        ClientboundAddVibrationSignalPacket clientboundAddVibrationSignalPacket = new ClientboundAddVibrationSignalPacket(vibration);
        this.players.forEach((player) -> {
            this.sendParticles(player, false, (double)blockPos.getX(), (double)blockPos.getY(), (double)blockPos.getZ(), clientboundAddVibrationSignalPacket);
        });
    }

    public <T extends ParticleParam> int sendParticles(T particle, double x, double y, double z, int count, double deltaX, double deltaY, double deltaZ, double speed) {
        PacketPlayOutWorldParticles clientboundLevelParticlesPacket = new PacketPlayOutWorldParticles(particle, false, x, y, z, (float)deltaX, (float)deltaY, (float)deltaZ, (float)speed, count);
        int i = 0;

        for(int j = 0; j < this.players.size(); ++j) {
            EntityPlayer serverPlayer = this.players.get(j);
            if (this.sendParticles(serverPlayer, false, x, y, z, clientboundLevelParticlesPacket)) {
                ++i;
            }
        }

        return i;
    }

    public <T extends ParticleParam> boolean sendParticles(EntityPlayer viewer, T particle, boolean force, double x, double y, double z, int count, double deltaX, double deltaY, double deltaZ, double speed) {
        Packet<?> packet = new PacketPlayOutWorldParticles(particle, force, x, y, z, (float)deltaX, (float)deltaY, (float)deltaZ, (float)speed, count);
        return this.sendParticles(viewer, force, x, y, z, packet);
    }

    private boolean sendParticles(EntityPlayer player, boolean force, double x, double y, double z, Packet<?> packet) {
        if (player.getWorldServer() != this) {
            return false;
        } else {
            BlockPosition blockPos = player.getChunkCoordinates();
            if (blockPos.closerThan(new Vec3D(x, y, z), force ? 512.0D : 32.0D)) {
                player.connection.sendPacket(packet);
                return true;
            } else {
                return false;
            }
        }
    }

    @Nullable
    @Override
    public Entity getEntity(int id) {
        return this.getEntities().get(id);
    }

    @Deprecated
    @Nullable
    public Entity getEntityOrPart(int id) {
        Entity entity = this.getEntities().get(id);
        return entity != null ? entity : this.dragonParts.get(id);
    }

    @Nullable
    public Entity getEntity(UUID uuid) {
        return this.getEntities().get(uuid);
    }

    @Nullable
    public BlockPosition findNearestMapFeature(StructureGenerator<?> feature, BlockPosition pos, int radius, boolean skipExistingChunks) {
        return !this.server.getSaveData().getGeneratorSettings().shouldGenerateMapFeatures() ? null : this.getChunkSource().getChunkGenerator().findNearestMapFeature(this, feature, pos, radius, skipExistingChunks);
    }

    @Nullable
    public BlockPosition findNearestBiome(BiomeBase biome, BlockPosition pos, int radius, int i) {
        return this.getChunkSource().getChunkGenerator().getWorldChunkManager().findBiomeHorizontal(pos.getX(), pos.getY(), pos.getZ(), radius, i, (biome2) -> {
            return biome2 == biome;
        }, this.random, true);
    }

    @Override
    public CraftingManager getCraftingManager() {
        return this.server.getCraftingManager();
    }

    @Override
    public ITagRegistry getTagManager() {
        return this.server.getTagRegistry();
    }

    @Override
    public boolean isSavingDisabled() {
        return this.noSave;
    }

    @Override
    public IRegistryCustom registryAccess() {
        return this.server.getCustomRegistry();
    }

    public WorldPersistentData getWorldPersistentData() {
        return this.getChunkSource().getWorldPersistentData();
    }

    @Nullable
    @Override
    public WorldMap getMapData(String id) {
        return this.getMinecraftServer().overworld().getWorldPersistentData().get(WorldMap::load, id);
    }

    @Override
    public void setMapData(String id, WorldMap state) {
        this.getMinecraftServer().overworld().getWorldPersistentData().set(id, state);
    }

    @Override
    public int getWorldMapCount() {
        return this.getMinecraftServer().overworld().getWorldPersistentData().computeIfAbsent(PersistentIdCounts::load, PersistentIdCounts::new, "idcounts").getFreeAuxValueForMap();
    }

    public void setDefaultSpawnPos(BlockPosition pos, float angle) {
        ChunkCoordIntPair chunkPos = new ChunkCoordIntPair(new BlockPosition(this.levelData.getXSpawn(), 0, this.levelData.getZSpawn()));
        this.levelData.setSpawn(pos, angle);
        this.getChunkSource().removeTicket(TicketType.START, chunkPos, 11, Unit.INSTANCE);
        this.getChunkSource().addTicket(TicketType.START, new ChunkCoordIntPair(pos), 11, Unit.INSTANCE);
        this.getMinecraftServer().getPlayerList().sendAll(new PacketPlayOutSpawnPosition(pos, angle));
    }

    public BlockPosition getSpawn() {
        BlockPosition blockPos = new BlockPosition(this.levelData.getXSpawn(), this.levelData.getYSpawn(), this.levelData.getZSpawn());
        if (!this.getWorldBorder().isWithinBounds(blockPos)) {
            blockPos = this.getHighestBlockYAt(HeightMap.Type.MOTION_BLOCKING, new BlockPosition(this.getWorldBorder().getCenterX(), 0.0D, this.getWorldBorder().getCenterZ()));
        }

        return blockPos;
    }

    public float getSharedSpawnAngle() {
        return this.levelData.getSpawnAngle();
    }

    public LongSet getForceLoadedChunks() {
        ForcedChunk forcedChunksSavedData = this.getWorldPersistentData().get(ForcedChunk::load, "chunks");
        return (LongSet)(forcedChunksSavedData != null ? LongSets.unmodifiable(forcedChunksSavedData.getChunks()) : LongSets.EMPTY_SET);
    }

    public boolean setForceLoaded(int x, int z, boolean forced) {
        ForcedChunk forcedChunksSavedData = this.getWorldPersistentData().computeIfAbsent(ForcedChunk::load, ForcedChunk::new, "chunks");
        ChunkCoordIntPair chunkPos = new ChunkCoordIntPair(x, z);
        long l = chunkPos.pair();
        boolean bl;
        if (forced) {
            bl = forcedChunksSavedData.getChunks().add(l);
            if (bl) {
                this.getChunk(x, z);
            }
        } else {
            bl = forcedChunksSavedData.getChunks().remove(l);
        }

        forcedChunksSavedData.setDirty(bl);
        if (bl) {
            this.getChunkSource().updateChunkForced(chunkPos, forced);
        }

        return bl;
    }

    @Override
    public List<EntityPlayer> getPlayers() {
        return this.players;
    }

    @Override
    public void onBlockStateChange(BlockPosition pos, IBlockData oldBlock, IBlockData newBlock) {
        Optional<VillagePlaceType> optional = VillagePlaceType.forState(oldBlock);
        Optional<VillagePlaceType> optional2 = VillagePlaceType.forState(newBlock);
        if (!Objects.equals(optional, optional2)) {
            BlockPosition blockPos = pos.immutableCopy();
            optional.ifPresent((poiType) -> {
                this.getMinecraftServer().execute(() -> {
                    this.getPoiManager().remove(blockPos);
                    PacketDebug.sendPoiRemovedPacket(this, blockPos);
                });
            });
            optional2.ifPresent((poiType) -> {
                this.getMinecraftServer().execute(() -> {
                    this.getPoiManager().add(blockPos, poiType);
                    PacketDebug.sendPoiAddedPacket(this, blockPos);
                });
            });
        }
    }

    public VillagePlace getPoiManager() {
        return this.getChunkSource().getPoiManager();
    }

    public boolean isVillage(BlockPosition pos) {
        return this.isCloseToVillage(pos, 1);
    }

    public boolean isVillage(SectionPosition sectionPos) {
        return this.isVillage(sectionPos.center());
    }

    public boolean isCloseToVillage(BlockPosition pos, int maxDistance) {
        if (maxDistance > 6) {
            return false;
        } else {
            return this.sectionsToVillage(SectionPosition.of(pos)) <= maxDistance;
        }
    }

    public int sectionsToVillage(SectionPosition pos) {
        return this.getPoiManager().sectionsToVillage(pos);
    }

    public PersistentRaid getPersistentRaid() {
        return this.raids;
    }

    @Nullable
    public Raid getRaidAt(BlockPosition pos) {
        return this.raids.getNearbyRaid(pos, 9216);
    }

    public boolean isRaided(BlockPosition pos) {
        return this.getRaidAt(pos) != null;
    }

    public void onReputationEvent(ReputationEvent interaction, Entity entity, ReputationHandler observer) {
        observer.onReputationEventFrom(interaction, entity);
    }

    public void saveDebugReport(Path path) throws IOException {
        PlayerChunkMap chunkMap = this.getChunkSource().chunkMap;
        Writer writer = Files.newBufferedWriter(path.resolve("stats.txt"));

        try {
            writer.write(String.format("spawning_chunks: %d\n", chunkMap.getDistanceManager().getNaturalSpawnChunkCount()));
            NaturalSpawner.SpawnState spawnState = this.getChunkSource().getLastSpawnState();
            if (spawnState != null) {
                for(Entry<EnumCreatureType> entry : spawnState.getMobCategoryCounts().object2IntEntrySet()) {
                    writer.write(String.format("spawn_count.%s: %d\n", entry.getKey().getName(), entry.getIntValue()));
                }
            }

            writer.write(String.format("entities: %s\n", this.entityManager.gatherStats()));
            writer.write(String.format("block_entity_tickers: %d\n", this.blockEntityTickers.size()));
            writer.write(String.format("block_ticks: %d\n", this.getBlockTicks().size()));
            writer.write(String.format("fluid_ticks: %d\n", this.getLiquidTicks().size()));
            writer.write("distance_manager: " + chunkMap.getDistanceManager().getDebugStatus() + "\n");
            writer.write(String.format("pending_tasks: %d\n", this.getChunkSource().getPendingTasksCount()));
        } catch (Throwable var22) {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Throwable var16) {
                    var22.addSuppressed(var16);
                }
            }

            throw var22;
        }

        if (writer != null) {
            writer.close();
        }

        CrashReport crashReport = new CrashReport("Level dump", new Exception("dummy"));
        this.fillReportDetails(crashReport);
        Writer writer2 = Files.newBufferedWriter(path.resolve("example_crash.txt"));

        try {
            writer2.write(crashReport.getFriendlyReport());
        } catch (Throwable var21) {
            if (writer2 != null) {
                try {
                    writer2.close();
                } catch (Throwable var15) {
                    var21.addSuppressed(var15);
                }
            }

            throw var21;
        }

        if (writer2 != null) {
            writer2.close();
        }

        Path path2 = path.resolve("chunks.csv");
        Writer writer3 = Files.newBufferedWriter(path2);

        try {
            chunkMap.dumpChunks(writer3);
        } catch (Throwable var20) {
            if (writer3 != null) {
                try {
                    writer3.close();
                } catch (Throwable var14) {
                    var20.addSuppressed(var14);
                }
            }

            throw var20;
        }

        if (writer3 != null) {
            writer3.close();
        }

        Path path3 = path.resolve("entity_chunks.csv");
        Writer writer4 = Files.newBufferedWriter(path3);

        try {
            this.entityManager.dumpSections(writer4);
        } catch (Throwable var19) {
            if (writer4 != null) {
                try {
                    writer4.close();
                } catch (Throwable var13) {
                    var19.addSuppressed(var13);
                }
            }

            throw var19;
        }

        if (writer4 != null) {
            writer4.close();
        }

        Path path4 = path.resolve("entities.csv");
        Writer writer5 = Files.newBufferedWriter(path4);

        try {
            dumpEntities(writer5, this.getEntities().getAll());
        } catch (Throwable var18) {
            if (writer5 != null) {
                try {
                    writer5.close();
                } catch (Throwable var12) {
                    var18.addSuppressed(var12);
                }
            }

            throw var18;
        }

        if (writer5 != null) {
            writer5.close();
        }

        Path path5 = path.resolve("block_entities.csv");
        Writer writer6 = Files.newBufferedWriter(path5);

        try {
            this.dumpBlockEntityTickers(writer6);
        } catch (Throwable var17) {
            if (writer6 != null) {
                try {
                    writer6.close();
                } catch (Throwable var11) {
                    var17.addSuppressed(var11);
                }
            }

            throw var17;
        }

        if (writer6 != null) {
            writer6.close();
        }

    }

    private static void dumpEntities(Writer writer, Iterable<Entity> entities) throws IOException {
        CSVWriter csvOutput = CSVWriter.builder().addColumn("x").addColumn("y").addColumn("z").addColumn("uuid").addColumn("type").addColumn("alive").addColumn("display_name").addColumn("custom_name").build(writer);

        for(Entity entity : entities) {
            IChatBaseComponent component = entity.getCustomName();
            IChatBaseComponent component2 = entity.getScoreboardDisplayName();
            csvOutput.writeRow(entity.locX(), entity.locY(), entity.locZ(), entity.getUniqueID(), IRegistry.ENTITY_TYPE.getKey(entity.getEntityType()), entity.isAlive(), component2.getString(), component != null ? component.getString() : null);
        }

    }

    private void dumpBlockEntityTickers(Writer writer) throws IOException {
        CSVWriter csvOutput = CSVWriter.builder().addColumn("x").addColumn("y").addColumn("z").addColumn("type").build(writer);

        for(TickingBlockEntity tickingBlockEntity : this.blockEntityTickers) {
            BlockPosition blockPos = tickingBlockEntity.getPos();
            csvOutput.writeRow(blockPos.getX(), blockPos.getY(), blockPos.getZ(), tickingBlockEntity.getType());
        }

    }

    @VisibleForTesting
    public void clearBlockEvents(StructureBoundingBox box) {
        this.blockEvents.removeIf((blockEventData) -> {
            return box.isInside(blockEventData.getPos());
        });
    }

    @Override
    public void update(BlockPosition pos, Block block) {
        if (!this.isDebugWorld()) {
            this.applyPhysics(pos, block);
        }

    }

    @Override
    public float getShade(EnumDirection direction, boolean shaded) {
        return 1.0F;
    }

    public Iterable<Entity> getAllEntities() {
        return this.getEntities().getAll();
    }

    @Override
    public String toString() {
        return "ServerLevel[" + this.serverLevelData.getName() + "]";
    }

    public boolean isFlatWorld() {
        return this.server.getSaveData().getGeneratorSettings().isFlatWorld();
    }

    @Override
    public long getSeed() {
        return this.server.getSaveData().getGeneratorSettings().getSeed();
    }

    @Nullable
    public EnderDragonBattle getDragonBattle() {
        return this.dragonFight;
    }

    @Override
    public Stream<? extends StructureStart<?>> startsForFeature(SectionPosition pos, StructureGenerator<?> feature) {
        return this.getStructureManager().startsForFeature(pos, feature);
    }

    @Override
    public WorldServer getLevel() {
        return this;
    }

    @VisibleForTesting
    public String getWatchdogStats() {
        return String.format("players: %s, entities: %s [%s], block_entities: %d [%s], block_ticks: %d, fluid_ticks: %d, chunk_source: %s", this.players.size(), this.entityManager.gatherStats(), getTypeCount(this.entityManager.getEntityGetter().getAll(), (entity) -> {
            return IRegistry.ENTITY_TYPE.getKey(entity.getEntityType()).toString();
        }), this.blockEntityTickers.size(), getTypeCount(this.blockEntityTickers, TickingBlockEntity::getType), this.getBlockTicks().size(), this.getLiquidTicks().size(), this.gatherChunkSourceStats());
    }

    private static <T> String getTypeCount(Iterable<T> items, Function<T, String> classifier) {
        try {
            Object2IntOpenHashMap<String> object2IntOpenHashMap = new Object2IntOpenHashMap<>();

            for(T object : items) {
                String string = classifier.apply(object);
                object2IntOpenHashMap.addTo(string, 1);
            }

            return object2IntOpenHashMap.object2IntEntrySet().stream().sorted(Comparator.comparing(Entry::getIntValue).reversed()).limit(5L).map((entry) -> {
                return (String)entry.getKey() + ":" + entry.getIntValue();
            }).collect(Collectors.joining(","));
        } catch (Exception var6) {
            return "";
        }
    }

    public static void makeObsidianPlatform(WorldServer world) {
        BlockPosition blockPos = END_SPAWN_POINT;
        int i = blockPos.getX();
        int j = blockPos.getY() - 2;
        int k = blockPos.getZ();
        BlockPosition.betweenClosed(i - 2, j + 1, k - 2, i + 2, j + 3, k + 2).forEach((blockPosx) -> {
            world.setTypeUpdate(blockPosx, Blocks.AIR.getBlockData());
        });
        BlockPosition.betweenClosed(i - 2, j, k - 2, i + 2, j, k + 2).forEach((blockPosx) -> {
            world.setTypeUpdate(blockPosx, Blocks.OBSIDIAN.getBlockData());
        });
    }

    @Override
    public IWorldEntityAccess<Entity> getEntities() {
        return this.entityManager.getEntityGetter();
    }

    public void addLegacyChunkEntities(Stream<Entity> entities) {
        this.entityManager.addLegacyChunkEntities(entities);
    }

    public void addWorldGenChunkEntities(Stream<Entity> entities) {
        this.entityManager.addWorldGenChunkEntities(entities);
    }

    @Override
    public void close() throws IOException {
        super.close();
        this.entityManager.close();
    }

    @Override
    public String gatherChunkSourceStats() {
        return "Chunks[S] W: " + this.chunkSource.getName() + " E: " + this.entityManager.gatherStats();
    }

    public boolean areEntitiesLoaded(long l) {
        return this.entityManager.areEntitiesLoaded(l);
    }

    public boolean isPositionTickingWithEntitiesLoaded(BlockPosition blockPos) {
        long l = ChunkCoordIntPair.asLong(blockPos);
        return this.chunkSource.isPositionTicking(l) && this.areEntitiesLoaded(l);
    }

    public boolean isPositionEntityTicking(BlockPosition blockPos) {
        return this.entityManager.isPositionTicking(blockPos);
    }

    public boolean isPositionEntityTicking(ChunkCoordIntPair chunkPos) {
        return this.entityManager.isPositionTicking(chunkPos);
    }

    final class EntityCallbacks implements WorldCallback<Entity> {
        @Override
        public void onCreated(Entity entity) {
        }

        @Override
        public void onDestroyed(Entity entity) {
            WorldServer.this.getScoreboard().entityRemoved(entity);
        }

        @Override
        public void onTickingStart(Entity entity) {
            WorldServer.this.entityTickList.add(entity);
        }

        @Override
        public void onTickingEnd(Entity entity) {
            WorldServer.this.entityTickList.remove(entity);
        }

        @Override
        public void onTrackingStart(Entity entity) {
            WorldServer.this.getChunkSource().addEntity(entity);
            if (entity instanceof EntityPlayer) {
                WorldServer.this.players.add((EntityPlayer)entity);
                WorldServer.this.everyoneSleeping();
            }

            if (entity instanceof EntityInsentient) {
                WorldServer.this.navigatingMobs.add((EntityInsentient)entity);
            }

            if (entity instanceof EntityEnderDragon) {
                for(EntityComplexPart enderDragonPart : ((EntityEnderDragon)entity).getSubEntities()) {
                    WorldServer.this.dragonParts.put(enderDragonPart.getId(), enderDragonPart);
                }
            }

        }

        @Override
        public void onTrackingEnd(Entity entity) {
            WorldServer.this.getChunkSource().removeEntity(entity);
            if (entity instanceof EntityPlayer) {
                EntityPlayer serverPlayer = (EntityPlayer)entity;
                WorldServer.this.players.remove(serverPlayer);
                WorldServer.this.everyoneSleeping();
            }

            if (entity instanceof EntityInsentient) {
                WorldServer.this.navigatingMobs.remove(entity);
            }

            if (entity instanceof EntityEnderDragon) {
                for(EntityComplexPart enderDragonPart : ((EntityEnderDragon)entity).getSubEntities()) {
                    WorldServer.this.dragonParts.remove(enderDragonPart.getId());
                }
            }

            GameEventListenerRegistrar gameEventListenerRegistrar = entity.getGameEventListenerRegistrar();
            if (gameEventListenerRegistrar != null) {
                gameEventListenerRegistrar.onListenerRemoved(entity.level);
            }

        }
    }
}
