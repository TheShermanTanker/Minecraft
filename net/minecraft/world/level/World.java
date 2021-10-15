package net.minecraft.world.level;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportSystemDetails;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.IRegistry;
import net.minecraft.core.SectionPosition;
import net.minecraft.core.particles.ParticleParam;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.PlayerChunk;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.tags.ITagRegistry;
import net.minecraft.util.MathHelper;
import net.minecraft.util.profiling.GameProfilerFiller;
import net.minecraft.world.DifficultyDamageScaler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.EntityComplexPart;
import net.minecraft.world.entity.boss.enderdragon.EntityEnderDragon;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingManager;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockFireAbstract;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.Chunk;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.IChunkAccess;
import net.minecraft.world.level.dimension.DimensionManager;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.IWorldEntityAccess;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.level.saveddata.maps.WorldMap;
import net.minecraft.world.level.storage.WorldData;
import net.minecraft.world.level.storage.WorldDataMutable;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.scores.Scoreboard;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class World implements GeneratorAccess, AutoCloseable {
    protected static final Logger LOGGER = LogManager.getLogger();
    public static final Codec<ResourceKey<World>> RESOURCE_KEY_CODEC = MinecraftKey.CODEC.xmap(ResourceKey.elementKey(IRegistry.DIMENSION_REGISTRY), ResourceKey::location);
    public static final ResourceKey<World> OVERWORLD = ResourceKey.create(IRegistry.DIMENSION_REGISTRY, new MinecraftKey("overworld"));
    public static final ResourceKey<World> NETHER = ResourceKey.create(IRegistry.DIMENSION_REGISTRY, new MinecraftKey("the_nether"));
    public static final ResourceKey<World> END = ResourceKey.create(IRegistry.DIMENSION_REGISTRY, new MinecraftKey("the_end"));
    public static final int MAX_LEVEL_SIZE = 30000000;
    public static final int LONG_PARTICLE_CLIP_RANGE = 512;
    public static final int SHORT_PARTICLE_CLIP_RANGE = 32;
    private static final EnumDirection[] DIRECTIONS = EnumDirection.values();
    public static final int MAX_BRIGHTNESS = 15;
    public static final int TICKS_PER_DAY = 24000;
    public static final int MAX_ENTITY_SPAWN_Y = 20000000;
    public static final int MIN_ENTITY_SPAWN_Y = -20000000;
    protected final List<TickingBlockEntity> blockEntityTickers = Lists.newArrayList();
    private final List<TickingBlockEntity> pendingBlockEntityTickers = Lists.newArrayList();
    private boolean tickingBlockEntities;
    public final Thread thread;
    private final boolean isDebug;
    private int skyDarken;
    protected int randValue = (new Random()).nextInt();
    protected final int addend = 1013904223;
    protected float oRainLevel;
    public float rainLevel;
    protected float oThunderLevel;
    public float thunderLevel;
    public final Random random = new Random();
    private final DimensionManager dimensionType;
    public final WorldDataMutable levelData;
    private final Supplier<GameProfilerFiller> profiler;
    public final boolean isClientSide;
    private final WorldBorder worldBorder;
    private final BiomeManager biomeManager;
    private final ResourceKey<World> dimension;

    protected World(WorldDataMutable properties, ResourceKey<World> registryRef, DimensionManager dimensionType, Supplier<GameProfilerFiller> profiler, boolean isClient, boolean debugWorld, long seed) {
        this.profiler = profiler;
        this.levelData = properties;
        this.dimensionType = dimensionType;
        this.dimension = registryRef;
        this.isClientSide = isClient;
        if (dimensionType.getCoordinateScale() != 1.0D) {
            this.worldBorder = new WorldBorder() {
                @Override
                public double getCenterX() {
                    return super.getCenterX() / dimensionType.getCoordinateScale();
                }

                @Override
                public double getCenterZ() {
                    return super.getCenterZ() / dimensionType.getCoordinateScale();
                }
            };
        } else {
            this.worldBorder = new WorldBorder();
        }

        this.thread = Thread.currentThread();
        this.biomeManager = new BiomeManager(this, seed, dimensionType.getGenLayerZoomer());
        this.isDebug = debugWorld;
    }

    @Override
    public boolean isClientSide() {
        return this.isClientSide;
    }

    @Nullable
    @Override
    public MinecraftServer getMinecraftServer() {
        return null;
    }

    public boolean isValidLocation(BlockPosition pos) {
        return !this.isOutsideWorld(pos) && isInWorldBoundsHorizontal(pos);
    }

    public static boolean isInSpawnableBounds(BlockPosition pos) {
        return !isOutsideSpawnableHeight(pos.getY()) && isInWorldBoundsHorizontal(pos);
    }

    private static boolean isInWorldBoundsHorizontal(BlockPosition pos) {
        return pos.getX() >= -30000000 && pos.getZ() >= -30000000 && pos.getX() < 30000000 && pos.getZ() < 30000000;
    }

    private static boolean isOutsideSpawnableHeight(int y) {
        return y < -20000000 || y >= 20000000;
    }

    public Chunk getChunkAtWorldCoords(BlockPosition pos) {
        return this.getChunk(SectionPosition.blockToSectionCoord(pos.getX()), SectionPosition.blockToSectionCoord(pos.getZ()));
    }

    @Override
    public Chunk getChunk(int i, int j) {
        return (Chunk)this.getChunkAt(i, j, ChunkStatus.FULL);
    }

    @Nullable
    @Override
    public IChunkAccess getChunkAt(int chunkX, int chunkZ, ChunkStatus leastStatus, boolean create) {
        IChunkAccess chunkAccess = this.getChunkProvider().getChunkAt(chunkX, chunkZ, leastStatus, create);
        if (chunkAccess == null && create) {
            throw new IllegalStateException("Should always be able to create a chunk!");
        } else {
            return chunkAccess;
        }
    }

    @Override
    public boolean setTypeAndData(BlockPosition pos, IBlockData state, int flags) {
        return this.setBlock(pos, state, flags, 512);
    }

    @Override
    public boolean setBlock(BlockPosition pos, IBlockData state, int flags, int maxUpdateDepth) {
        if (this.isOutsideWorld(pos)) {
            return false;
        } else if (!this.isClientSide && this.isDebugWorld()) {
            return false;
        } else {
            Chunk levelChunk = this.getChunkAtWorldCoords(pos);
            Block block = state.getBlock();
            IBlockData blockState = levelChunk.setType(pos, state, (flags & 64) != 0);
            if (blockState == null) {
                return false;
            } else {
                IBlockData blockState2 = this.getType(pos);
                if ((flags & 128) == 0 && blockState2 != blockState && (blockState2.getLightBlock(this, pos) != blockState.getLightBlock(this, pos) || blockState2.getLightEmission() != blockState.getLightEmission() || blockState2.useShapeForLightOcclusion() || blockState.useShapeForLightOcclusion())) {
                    this.getMethodProfiler().enter("queueCheckLight");
                    this.getChunkProvider().getLightEngine().checkBlock(pos);
                    this.getMethodProfiler().exit();
                }

                if (blockState2 == state) {
                    if (blockState != blockState2) {
                        this.setBlocksDirty(pos, blockState, blockState2);
                    }

                    if ((flags & 2) != 0 && (!this.isClientSide || (flags & 4) == 0) && (this.isClientSide || levelChunk.getState() != null && levelChunk.getState().isAtLeast(PlayerChunk.State.TICKING))) {
                        this.notify(pos, blockState, state, flags);
                    }

                    if ((flags & 1) != 0) {
                        this.update(pos, blockState.getBlock());
                        if (!this.isClientSide && state.isComplexRedstone()) {
                            this.updateAdjacentComparators(pos, block);
                        }
                    }

                    if ((flags & 16) == 0 && maxUpdateDepth > 0) {
                        int i = flags & -34;
                        blockState.updateIndirectNeighbourShapes(this, pos, i, maxUpdateDepth - 1);
                        state.updateNeighbourShapes(this, pos, i, maxUpdateDepth - 1);
                        state.updateIndirectNeighbourShapes(this, pos, i, maxUpdateDepth - 1);
                    }

                    this.onBlockStateChange(pos, blockState, blockState2);
                }

                return true;
            }
        }
    }

    public void onBlockStateChange(BlockPosition pos, IBlockData oldBlock, IBlockData newBlock) {
    }

    @Override
    public boolean removeBlock(BlockPosition pos, boolean move) {
        Fluid fluidState = this.getFluid(pos);
        return this.setTypeAndData(pos, fluidState.getBlockData(), 3 | (move ? 64 : 0));
    }

    @Override
    public boolean destroyBlock(BlockPosition pos, boolean drop, @Nullable Entity breakingEntity, int maxUpdateDepth) {
        IBlockData blockState = this.getType(pos);
        if (blockState.isAir()) {
            return false;
        } else {
            Fluid fluidState = this.getFluid(pos);
            if (!(blockState.getBlock() instanceof BlockFireAbstract)) {
                this.triggerEffect(2001, pos, Block.getCombinedId(blockState));
            }

            if (drop) {
                TileEntity blockEntity = blockState.isTileEntity() ? this.getTileEntity(pos) : null;
                Block.dropItems(blockState, this, pos, blockEntity, breakingEntity, ItemStack.EMPTY);
            }

            boolean bl = this.setBlock(pos, fluidState.getBlockData(), 3, maxUpdateDepth);
            if (bl) {
                this.gameEvent(breakingEntity, GameEvent.BLOCK_DESTROY, pos);
            }

            return bl;
        }
    }

    public void addDestroyBlockEffect(BlockPosition pos, IBlockData state) {
    }

    public boolean setTypeUpdate(BlockPosition pos, IBlockData state) {
        return this.setTypeAndData(pos, state, 3);
    }

    public abstract void notify(BlockPosition pos, IBlockData oldState, IBlockData newState, int flags);

    public void setBlocksDirty(BlockPosition pos, IBlockData old, IBlockData updated) {
    }

    public void applyPhysics(BlockPosition pos, Block block) {
        this.neighborChanged(pos.west(), block, pos);
        this.neighborChanged(pos.east(), block, pos);
        this.neighborChanged(pos.below(), block, pos);
        this.neighborChanged(pos.above(), block, pos);
        this.neighborChanged(pos.north(), block, pos);
        this.neighborChanged(pos.south(), block, pos);
    }

    public void updateNeighborsAtExceptFromFacing(BlockPosition pos, Block sourceBlock, EnumDirection direction) {
        if (direction != EnumDirection.WEST) {
            this.neighborChanged(pos.west(), sourceBlock, pos);
        }

        if (direction != EnumDirection.EAST) {
            this.neighborChanged(pos.east(), sourceBlock, pos);
        }

        if (direction != EnumDirection.DOWN) {
            this.neighborChanged(pos.below(), sourceBlock, pos);
        }

        if (direction != EnumDirection.UP) {
            this.neighborChanged(pos.above(), sourceBlock, pos);
        }

        if (direction != EnumDirection.NORTH) {
            this.neighborChanged(pos.north(), sourceBlock, pos);
        }

        if (direction != EnumDirection.SOUTH) {
            this.neighborChanged(pos.south(), sourceBlock, pos);
        }

    }

    public void neighborChanged(BlockPosition pos, Block sourceBlock, BlockPosition neighborPos) {
        if (!this.isClientSide) {
            IBlockData blockState = this.getType(pos);

            try {
                blockState.doPhysics(this, pos, sourceBlock, neighborPos, false);
            } catch (Throwable var8) {
                CrashReport crashReport = CrashReport.forThrowable(var8, "Exception while updating neighbours");
                CrashReportSystemDetails crashReportCategory = crashReport.addCategory("Block being updated");
                crashReportCategory.setDetail("Source block type", () -> {
                    try {
                        return String.format("ID #%s (%s // %s)", IRegistry.BLOCK.getKey(sourceBlock), sourceBlock.getDescriptionId(), sourceBlock.getClass().getCanonicalName());
                    } catch (Throwable var2) {
                        return "ID #" + IRegistry.BLOCK.getKey(sourceBlock);
                    }
                });
                CrashReportSystemDetails.populateBlockDetails(crashReportCategory, this, pos, blockState);
                throw new ReportedException(crashReport);
            }
        }
    }

    @Override
    public int getHeight(HeightMap.Type heightmap, int x, int z) {
        int j;
        if (x >= -30000000 && z >= -30000000 && x < 30000000 && z < 30000000) {
            if (this.isChunkLoaded(SectionPosition.blockToSectionCoord(x), SectionPosition.blockToSectionCoord(z))) {
                j = this.getChunk(SectionPosition.blockToSectionCoord(x), SectionPosition.blockToSectionCoord(z)).getHighestBlock(heightmap, x & 15, z & 15) + 1;
            } else {
                j = this.getMinBuildHeight();
            }
        } else {
            j = this.getSeaLevel() + 1;
        }

        return j;
    }

    @Override
    public LightEngine getLightEngine() {
        return this.getChunkProvider().getLightEngine();
    }

    @Override
    public IBlockData getType(BlockPosition pos) {
        if (this.isOutsideWorld(pos)) {
            return Blocks.VOID_AIR.getBlockData();
        } else {
            Chunk levelChunk = this.getChunk(SectionPosition.blockToSectionCoord(pos.getX()), SectionPosition.blockToSectionCoord(pos.getZ()));
            return levelChunk.getType(pos);
        }
    }

    @Override
    public Fluid getFluid(BlockPosition pos) {
        if (this.isOutsideWorld(pos)) {
            return FluidTypes.EMPTY.defaultFluidState();
        } else {
            Chunk levelChunk = this.getChunkAtWorldCoords(pos);
            return levelChunk.getFluid(pos);
        }
    }

    public boolean isDay() {
        return !this.getDimensionManager().isFixedTime() && this.skyDarken < 4;
    }

    public boolean isNight() {
        return !this.getDimensionManager().isFixedTime() && !this.isDay();
    }

    @Override
    public void playSound(@Nullable EntityHuman player, BlockPosition pos, SoundEffect sound, SoundCategory category, float volume, float pitch) {
        this.playSound(player, (double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D, sound, category, volume, pitch);
    }

    public abstract void playSound(@Nullable EntityHuman player, double x, double y, double z, SoundEffect sound, SoundCategory category, float volume, float pitch);

    public abstract void playSound(@Nullable EntityHuman player, Entity entity, SoundEffect sound, SoundCategory category, float volume, float pitch);

    public void playLocalSound(double x, double y, double z, SoundEffect sound, SoundCategory category, float volume, float pitch, boolean useDistance) {
    }

    @Override
    public void addParticle(ParticleParam parameters, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
    }

    public void addParticle(ParticleParam parameters, boolean alwaysSpawn, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
    }

    public void addAlwaysVisibleParticle(ParticleParam parameters, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
    }

    public void addAlwaysVisibleParticle(ParticleParam parameters, boolean alwaysSpawn, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
    }

    public float getSunAngle(float tickDelta) {
        float f = this.getTimeOfDay(tickDelta);
        return f * ((float)Math.PI * 2F);
    }

    public void addBlockEntityTicker(TickingBlockEntity ticker) {
        (this.tickingBlockEntities ? this.pendingBlockEntityTickers : this.blockEntityTickers).add(ticker);
    }

    protected void tickBlockEntities() {
        GameProfilerFiller profilerFiller = this.getMethodProfiler();
        profilerFiller.enter("blockEntities");
        this.tickingBlockEntities = true;
        if (!this.pendingBlockEntityTickers.isEmpty()) {
            this.blockEntityTickers.addAll(this.pendingBlockEntityTickers);
            this.pendingBlockEntityTickers.clear();
        }

        Iterator<TickingBlockEntity> iterator = this.blockEntityTickers.iterator();

        while(iterator.hasNext()) {
            TickingBlockEntity tickingBlockEntity = iterator.next();
            if (tickingBlockEntity.isRemoved()) {
                iterator.remove();
            } else {
                tickingBlockEntity.tick();
            }
        }

        this.tickingBlockEntities = false;
        profilerFiller.exit();
    }

    public <T extends Entity> void guardEntityTick(Consumer<T> tickConsumer, T entity) {
        try {
            tickConsumer.accept(entity);
        } catch (Throwable var6) {
            CrashReport crashReport = CrashReport.forThrowable(var6, "Ticking entity");
            CrashReportSystemDetails crashReportCategory = crashReport.addCategory("Entity being ticked");
            entity.appendEntityCrashDetails(crashReportCategory);
            throw new ReportedException(crashReport);
        }
    }

    public Explosion explode(@Nullable Entity entity, double x, double y, double z, float power, Explosion.Effect destructionType) {
        return this.createExplosion(entity, (DamageSource)null, (ExplosionDamageCalculator)null, x, y, z, power, false, destructionType);
    }

    public Explosion createExplosion(@Nullable Entity entity, double x, double y, double z, float power, boolean createFire, Explosion.Effect destructionType) {
        return this.createExplosion(entity, (DamageSource)null, (ExplosionDamageCalculator)null, x, y, z, power, createFire, destructionType);
    }

    public Explosion createExplosion(@Nullable Entity entity, @Nullable DamageSource damageSource, @Nullable ExplosionDamageCalculator behavior, double x, double y, double z, float power, boolean createFire, Explosion.Effect destructionType) {
        Explosion explosion = new Explosion(this, entity, damageSource, behavior, x, y, z, power, createFire, destructionType);
        explosion.explode();
        explosion.finalizeExplosion(true);
        return explosion;
    }

    public abstract String gatherChunkSourceStats();

    @Nullable
    @Override
    public TileEntity getTileEntity(BlockPosition pos) {
        if (this.isOutsideWorld(pos)) {
            return null;
        } else {
            return !this.isClientSide && Thread.currentThread() != this.thread ? null : this.getChunkAtWorldCoords(pos).getBlockEntity(pos, Chunk.EnumTileEntityState.IMMEDIATE);
        }
    }

    public void setTileEntity(TileEntity blockEntity) {
        BlockPosition blockPos = blockEntity.getPosition();
        if (!this.isOutsideWorld(blockPos)) {
            this.getChunkAtWorldCoords(blockPos).addAndRegisterBlockEntity(blockEntity);
        }
    }

    public void removeTileEntity(BlockPosition pos) {
        if (!this.isOutsideWorld(pos)) {
            this.getChunkAtWorldCoords(pos).removeTileEntity(pos);
        }
    }

    public boolean isLoaded(BlockPosition pos) {
        return this.isOutsideWorld(pos) ? false : this.getChunkProvider().isLoaded(SectionPosition.blockToSectionCoord(pos.getX()), SectionPosition.blockToSectionCoord(pos.getZ()));
    }

    public boolean loadedAndEntityCanStandOnFace(BlockPosition pos, Entity entity, EnumDirection direction) {
        if (this.isOutsideWorld(pos)) {
            return false;
        } else {
            IChunkAccess chunkAccess = this.getChunkAt(SectionPosition.blockToSectionCoord(pos.getX()), SectionPosition.blockToSectionCoord(pos.getZ()), ChunkStatus.FULL, false);
            return chunkAccess == null ? false : chunkAccess.getType(pos).entityCanStandOnFace(this, pos, entity, direction);
        }
    }

    public boolean loadedAndEntityCanStandOn(BlockPosition pos, Entity entity) {
        return this.loadedAndEntityCanStandOnFace(pos, entity, EnumDirection.UP);
    }

    public void updateSkyBrightness() {
        double d = 1.0D - (double)(this.getRainLevel(1.0F) * 5.0F) / 16.0D;
        double e = 1.0D - (double)(this.getThunderLevel(1.0F) * 5.0F) / 16.0D;
        double f = 0.5D + 2.0D * MathHelper.clamp((double)MathHelper.cos(this.getTimeOfDay(1.0F) * ((float)Math.PI * 2F)), -0.25D, 0.25D);
        this.skyDarken = (int)((1.0D - f * d * e) * 11.0D);
    }

    public void setSpawnFlags(boolean spawnMonsters, boolean spawnAnimals) {
        this.getChunkProvider().setSpawnSettings(spawnMonsters, spawnAnimals);
    }

    protected void prepareWeather() {
        if (this.levelData.hasStorm()) {
            this.rainLevel = 1.0F;
            if (this.levelData.isThundering()) {
                this.thunderLevel = 1.0F;
            }
        }

    }

    @Override
    public void close() throws IOException {
        this.getChunkProvider().close();
    }

    @Nullable
    @Override
    public IBlockAccess getChunkForCollisions(int chunkX, int chunkZ) {
        return this.getChunkAt(chunkX, chunkZ, ChunkStatus.FULL, false);
    }

    @Override
    public List<Entity> getEntities(@Nullable Entity except, AxisAlignedBB box, Predicate<? super Entity> predicate) {
        this.getMethodProfiler().incrementCounter("getEntities");
        List<Entity> list = Lists.newArrayList();
        this.getEntities().get(box, (entity2) -> {
            if (entity2 != except && predicate.test(entity2)) {
                list.add(entity2);
            }

            if (entity2 instanceof EntityEnderDragon) {
                for(EntityComplexPart enderDragonPart : ((EntityEnderDragon)entity2).getSubEntities()) {
                    if (entity2 != except && predicate.test(enderDragonPart)) {
                        list.add(enderDragonPart);
                    }
                }
            }

        });
        return list;
    }

    @Override
    public <T extends Entity> List<T> getEntities(EntityTypeTest<Entity, T> filter, AxisAlignedBB box, Predicate<? super T> predicate) {
        this.getMethodProfiler().incrementCounter("getEntities");
        List<T> list = Lists.newArrayList();
        this.getEntities().get(filter, box, (entity) -> {
            if (predicate.test(entity)) {
                list.add(entity);
            }

            if (entity instanceof EntityEnderDragon) {
                for(EntityComplexPart enderDragonPart : ((EntityEnderDragon)entity).getSubEntities()) {
                    T entity2 = filter.tryCast(enderDragonPart);
                    if (entity2 != null && predicate.test(entity2)) {
                        list.add(entity2);
                    }
                }
            }

        });
        return list;
    }

    @Nullable
    public abstract Entity getEntity(int id);

    public void blockEntityChanged(BlockPosition pos) {
        if (this.isLoaded(pos)) {
            this.getChunkAtWorldCoords(pos).markDirty();
        }

    }

    @Override
    public int getSeaLevel() {
        return 63;
    }

    public int getBlockPower(BlockPosition pos) {
        int i = 0;
        i = Math.max(i, this.getDirectSignal(pos.below(), EnumDirection.DOWN));
        if (i >= 15) {
            return i;
        } else {
            i = Math.max(i, this.getDirectSignal(pos.above(), EnumDirection.UP));
            if (i >= 15) {
                return i;
            } else {
                i = Math.max(i, this.getDirectSignal(pos.north(), EnumDirection.NORTH));
                if (i >= 15) {
                    return i;
                } else {
                    i = Math.max(i, this.getDirectSignal(pos.south(), EnumDirection.SOUTH));
                    if (i >= 15) {
                        return i;
                    } else {
                        i = Math.max(i, this.getDirectSignal(pos.west(), EnumDirection.WEST));
                        if (i >= 15) {
                            return i;
                        } else {
                            i = Math.max(i, this.getDirectSignal(pos.east(), EnumDirection.EAST));
                            return i >= 15 ? i : i;
                        }
                    }
                }
            }
        }
    }

    public boolean isBlockFacePowered(BlockPosition pos, EnumDirection direction) {
        return this.getBlockFacePower(pos, direction) > 0;
    }

    public int getBlockFacePower(BlockPosition pos, EnumDirection direction) {
        IBlockData blockState = this.getType(pos);
        int i = blockState.getSignal(this, pos, direction);
        return blockState.isOccluding(this, pos) ? Math.max(i, this.getBlockPower(pos)) : i;
    }

    public boolean isBlockIndirectlyPowered(BlockPosition pos) {
        if (this.getBlockFacePower(pos.below(), EnumDirection.DOWN) > 0) {
            return true;
        } else if (this.getBlockFacePower(pos.above(), EnumDirection.UP) > 0) {
            return true;
        } else if (this.getBlockFacePower(pos.north(), EnumDirection.NORTH) > 0) {
            return true;
        } else if (this.getBlockFacePower(pos.south(), EnumDirection.SOUTH) > 0) {
            return true;
        } else if (this.getBlockFacePower(pos.west(), EnumDirection.WEST) > 0) {
            return true;
        } else {
            return this.getBlockFacePower(pos.east(), EnumDirection.EAST) > 0;
        }
    }

    public int getBestNeighborSignal(BlockPosition pos) {
        int i = 0;

        for(EnumDirection direction : DIRECTIONS) {
            int j = this.getBlockFacePower(pos.relative(direction), direction);
            if (j >= 15) {
                return 15;
            }

            if (j > i) {
                i = j;
            }
        }

        return i;
    }

    public void disconnect() {
    }

    public long getTime() {
        return this.levelData.getTime();
    }

    public long getDayTime() {
        return this.levelData.getDayTime();
    }

    public boolean mayInteract(EntityHuman player, BlockPosition pos) {
        return true;
    }

    public void broadcastEntityEffect(Entity entity, byte status) {
    }

    public void playBlockAction(BlockPosition pos, Block block, int type, int data) {
        this.getType(pos).triggerEvent(this, pos, type, data);
    }

    @Override
    public WorldData getWorldData() {
        return this.levelData;
    }

    public GameRules getGameRules() {
        return this.levelData.getGameRules();
    }

    public float getThunderLevel(float delta) {
        return MathHelper.lerp(delta, this.oThunderLevel, this.thunderLevel) * this.getRainLevel(delta);
    }

    public void setThunderLevel(float thunderGradient) {
        float f = MathHelper.clamp(thunderGradient, 0.0F, 1.0F);
        this.oThunderLevel = f;
        this.thunderLevel = f;
    }

    public float getRainLevel(float delta) {
        return MathHelper.lerp(delta, this.oRainLevel, this.rainLevel);
    }

    public void setRainLevel(float rainGradient) {
        float f = MathHelper.clamp(rainGradient, 0.0F, 1.0F);
        this.oRainLevel = f;
        this.rainLevel = f;
    }

    public boolean isThundering() {
        if (this.getDimensionManager().hasSkyLight() && !this.getDimensionManager().hasCeiling()) {
            return (double)this.getThunderLevel(1.0F) > 0.9D;
        } else {
            return false;
        }
    }

    public boolean isRaining() {
        return (double)this.getRainLevel(1.0F) > 0.2D;
    }

    public boolean isRainingAt(BlockPosition pos) {
        if (!this.isRaining()) {
            return false;
        } else if (!this.canSeeSky(pos)) {
            return false;
        } else if (this.getHighestBlockYAt(HeightMap.Type.MOTION_BLOCKING, pos).getY() > pos.getY()) {
            return false;
        } else {
            BiomeBase biome = this.getBiome(pos);
            return biome.getPrecipitation() == BiomeBase.Precipitation.RAIN && biome.getAdjustedTemperature(pos) >= 0.15F;
        }
    }

    public boolean isHumidAt(BlockPosition pos) {
        BiomeBase biome = this.getBiome(pos);
        return biome.isHumid();
    }

    @Nullable
    public abstract WorldMap getMapData(String id);

    public abstract void setMapData(String id, WorldMap state);

    public abstract int getWorldMapCount();

    public void globalLevelEvent(int eventId, BlockPosition pos, int data) {
    }

    public CrashReportSystemDetails fillReportDetails(CrashReport report) {
        CrashReportSystemDetails crashReportCategory = report.addCategory("Affected level", 1);
        crashReportCategory.setDetail("All players", () -> {
            return this.getPlayers().size() + " total; " + this.getPlayers();
        });
        crashReportCategory.setDetail("Chunk stats", this.getChunkProvider()::getName);
        crashReportCategory.setDetail("Level dimension", () -> {
            return this.getDimensionKey().location().toString();
        });

        try {
            this.levelData.fillCrashReportCategory(crashReportCategory, this);
        } catch (Throwable var4) {
            crashReportCategory.setDetailError("Level Data Unobtainable", var4);
        }

        return crashReportCategory;
    }

    public abstract void destroyBlockProgress(int entityId, BlockPosition pos, int progress);

    public void createFireworks(double x, double y, double z, double velocityX, double velocityY, double velocityZ, @Nullable NBTTagCompound nbt) {
    }

    public abstract Scoreboard getScoreboard();

    public void updateAdjacentComparators(BlockPosition pos, Block block) {
        for(EnumDirection direction : EnumDirection.EnumDirectionLimit.HORIZONTAL) {
            BlockPosition blockPos = pos.relative(direction);
            if (this.isLoaded(blockPos)) {
                IBlockData blockState = this.getType(blockPos);
                if (blockState.is(Blocks.COMPARATOR)) {
                    blockState.doPhysics(this, blockPos, block, pos, false);
                } else if (blockState.isOccluding(this, blockPos)) {
                    blockPos = blockPos.relative(direction);
                    blockState = this.getType(blockPos);
                    if (blockState.is(Blocks.COMPARATOR)) {
                        blockState.doPhysics(this, blockPos, block, pos, false);
                    }
                }
            }
        }

    }

    @Override
    public DifficultyDamageScaler getDamageScaler(BlockPosition pos) {
        long l = 0L;
        float f = 0.0F;
        if (this.isLoaded(pos)) {
            f = this.getMoonBrightness();
            l = this.getChunkAtWorldCoords(pos).getInhabitedTime();
        }

        return new DifficultyDamageScaler(this.getDifficulty(), this.getDayTime(), l, f);
    }

    @Override
    public int getSkyDarken() {
        return this.skyDarken;
    }

    public void setSkyFlashTime(int lightningTicksLeft) {
    }

    @Override
    public WorldBorder getWorldBorder() {
        return this.worldBorder;
    }

    public void sendPacketToServer(Packet<?> packet) {
        throw new UnsupportedOperationException("Can't send packets to server unless you're on the client.");
    }

    @Override
    public DimensionManager getDimensionManager() {
        return this.dimensionType;
    }

    public ResourceKey<World> getDimensionKey() {
        return this.dimension;
    }

    @Override
    public Random getRandom() {
        return this.random;
    }

    @Override
    public boolean isStateAtPosition(BlockPosition pos, Predicate<IBlockData> state) {
        return state.test(this.getType(pos));
    }

    @Override
    public boolean isFluidAtPosition(BlockPosition pos, Predicate<Fluid> state) {
        return state.test(this.getFluid(pos));
    }

    public abstract CraftingManager getCraftingManager();

    public abstract ITagRegistry getTagManager();

    public BlockPosition getBlockRandomPos(int x, int y, int z, int i) {
        this.randValue = this.randValue * 3 + 1013904223;
        int j = this.randValue >> 2;
        return new BlockPosition(x + (j & 15), y + (j >> 16 & i), z + (j >> 8 & 15));
    }

    public boolean isSavingDisabled() {
        return false;
    }

    public GameProfilerFiller getMethodProfiler() {
        return this.profiler.get();
    }

    public Supplier<GameProfilerFiller> getMethodProfilerSupplier() {
        return this.profiler;
    }

    @Override
    public BiomeManager getBiomeManager() {
        return this.biomeManager;
    }

    public final boolean isDebugWorld() {
        return this.isDebug;
    }

    public abstract IWorldEntityAccess<Entity> getEntities();

    protected void postGameEventInRadius(@Nullable Entity entity, GameEvent gameEvent, BlockPosition pos, int range) {
        int i = SectionPosition.blockToSectionCoord(pos.getX() - range);
        int j = SectionPosition.blockToSectionCoord(pos.getZ() - range);
        int k = SectionPosition.blockToSectionCoord(pos.getX() + range);
        int l = SectionPosition.blockToSectionCoord(pos.getZ() + range);
        int m = SectionPosition.blockToSectionCoord(pos.getY() - range);
        int n = SectionPosition.blockToSectionCoord(pos.getY() + range);

        for(int o = i; o <= k; ++o) {
            for(int p = j; p <= l; ++p) {
                IChunkAccess chunkAccess = this.getChunkProvider().getChunkNow(o, p);
                if (chunkAccess != null) {
                    for(int q = m; q <= n; ++q) {
                        chunkAccess.getEventDispatcher(q).post(gameEvent, entity, pos);
                    }
                }
            }
        }

    }
}
