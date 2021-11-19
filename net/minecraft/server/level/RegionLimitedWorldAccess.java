package net.minecraft.server.level;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.core.SectionPosition;
import net.minecraft.core.particles.ParticleParam;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.util.MathHelper;
import net.minecraft.world.DifficultyDamageScaler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.TickList;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ITileEntity;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.IChunkAccess;
import net.minecraft.world.level.chunk.IChunkProvider;
import net.minecraft.world.level.dimension.DimensionManager;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.feature.StructureGenerator;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidType;
import net.minecraft.world.level.storage.WorldData;
import net.minecraft.world.phys.AxisAlignedBB;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RegionLimitedWorldAccess implements GeneratorAccessSeed {
    private static final Logger LOGGER = LogManager.getLogger();
    private final List<IChunkAccess> cache;
    private final ChunkCoordIntPair center;
    private final int size;
    private final WorldServer level;
    private final long seed;
    private final WorldData levelData;
    private final Random random;
    private final DimensionManager dimensionType;
    private final TickList<Block> blockTicks = new TickListWorldGen<>((pos) -> {
        return this.getChunk(pos).getBlockTicks();
    });
    private final TickList<FluidType> liquidTicks = new TickListWorldGen<>((pos) -> {
        return this.getChunk(pos).getLiquidTicks();
    });
    private final BiomeManager biomeManager;
    private final ChunkCoordIntPair firstPos;
    private final ChunkCoordIntPair lastPos;
    private final StructureManager structureFeatureManager;
    private final ChunkStatus generatingStatus;
    private final int writeRadiusCutoff;
    @Nullable
    private Supplier<String> currentlyGenerating;

    public RegionLimitedWorldAccess(WorldServer world, List<IChunkAccess> list, ChunkStatus chunkStatus, int i) {
        this.generatingStatus = chunkStatus;
        this.writeRadiusCutoff = i;
        int j = MathHelper.floor(Math.sqrt((double)list.size()));
        if (j * j != list.size()) {
            throw (IllegalStateException)SystemUtils.pauseInIde(new IllegalStateException("Cache size is not a square."));
        } else {
            ChunkCoordIntPair chunkPos = list.get(list.size() / 2).getPos();
            this.cache = list;
            this.center = chunkPos;
            this.size = j;
            this.level = world;
            this.seed = world.getSeed();
            this.levelData = world.getWorldData();
            this.random = world.getRandom();
            this.dimensionType = world.getDimensionManager();
            this.biomeManager = new BiomeManager(this, BiomeManager.obfuscateSeed(this.seed), world.getDimensionManager().getGenLayerZoomer());
            this.firstPos = list.get(0).getPos();
            this.lastPos = list.get(list.size() - 1).getPos();
            this.structureFeatureManager = world.getStructureManager().forWorldGenRegion(this);
        }
    }

    public ChunkCoordIntPair getCenter() {
        return this.center;
    }

    public void setCurrentlyGenerating(@Nullable Supplier<String> supplier) {
        this.currentlyGenerating = supplier;
    }

    @Override
    public IChunkAccess getChunkAt(int chunkX, int chunkZ) {
        return this.getChunkAt(chunkX, chunkZ, ChunkStatus.EMPTY);
    }

    @Nullable
    @Override
    public IChunkAccess getChunkAt(int chunkX, int chunkZ, ChunkStatus leastStatus, boolean create) {
        IChunkAccess chunkAccess;
        if (this.isChunkLoaded(chunkX, chunkZ)) {
            int i = chunkX - this.firstPos.x;
            int j = chunkZ - this.firstPos.z;
            chunkAccess = this.cache.get(i + j * this.size);
            if (chunkAccess.getChunkStatus().isOrAfter(leastStatus)) {
                return chunkAccess;
            }
        } else {
            chunkAccess = null;
        }

        if (!create) {
            return null;
        } else {
            LOGGER.error("Requested chunk : {} {}", chunkX, chunkZ);
            LOGGER.error("Region bounds : {} {} | {} {}", this.firstPos.x, this.firstPos.z, this.lastPos.x, this.lastPos.z);
            if (chunkAccess != null) {
                throw (RuntimeException)SystemUtils.pauseInIde(new RuntimeException(String.format("Chunk is not of correct status. Expecting %s, got %s | %s %s", leastStatus, chunkAccess.getChunkStatus(), chunkX, chunkZ)));
            } else {
                throw (RuntimeException)SystemUtils.pauseInIde(new RuntimeException(String.format("We are asking a region for a chunk out of bound | %s %s", chunkX, chunkZ)));
            }
        }
    }

    @Override
    public boolean isChunkLoaded(int chunkX, int chunkZ) {
        return chunkX >= this.firstPos.x && chunkX <= this.lastPos.x && chunkZ >= this.firstPos.z && chunkZ <= this.lastPos.z;
    }

    @Override
    public IBlockData getType(BlockPosition pos) {
        return this.getChunkAt(SectionPosition.blockToSectionCoord(pos.getX()), SectionPosition.blockToSectionCoord(pos.getZ())).getType(pos);
    }

    @Override
    public Fluid getFluid(BlockPosition pos) {
        return this.getChunk(pos).getFluid(pos);
    }

    @Nullable
    @Override
    public EntityHuman getNearestPlayer(double x, double y, double z, double maxDistance, Predicate<Entity> targetPredicate) {
        return null;
    }

    @Override
    public int getSkyDarken() {
        return 0;
    }

    @Override
    public BiomeManager getBiomeManager() {
        return this.biomeManager;
    }

    @Override
    public BiomeBase getUncachedNoiseBiome(int biomeX, int biomeY, int biomeZ) {
        return this.level.getUncachedNoiseBiome(biomeX, biomeY, biomeZ);
    }

    @Override
    public float getShade(EnumDirection direction, boolean shaded) {
        return 1.0F;
    }

    @Override
    public LightEngine getLightEngine() {
        return this.level.getLightEngine();
    }

    @Override
    public boolean destroyBlock(BlockPosition pos, boolean drop, @Nullable Entity breakingEntity, int maxUpdateDepth) {
        IBlockData blockState = this.getType(pos);
        if (blockState.isAir()) {
            return false;
        } else {
            if (drop) {
                TileEntity blockEntity = blockState.isTileEntity() ? this.getTileEntity(pos) : null;
                Block.dropItems(blockState, this.level, pos, blockEntity, breakingEntity, ItemStack.EMPTY);
            }

            return this.setBlock(pos, Blocks.AIR.getBlockData(), 3, maxUpdateDepth);
        }
    }

    @Nullable
    @Override
    public TileEntity getTileEntity(BlockPosition pos) {
        IChunkAccess chunkAccess = this.getChunk(pos);
        TileEntity blockEntity = chunkAccess.getTileEntity(pos);
        if (blockEntity != null) {
            return blockEntity;
        } else {
            NBTTagCompound compoundTag = chunkAccess.getBlockEntityNbt(pos);
            IBlockData blockState = chunkAccess.getType(pos);
            if (compoundTag != null) {
                if ("DUMMY".equals(compoundTag.getString("id"))) {
                    if (!blockState.isTileEntity()) {
                        return null;
                    }

                    blockEntity = ((ITileEntity)blockState.getBlock()).createTile(pos, blockState);
                } else {
                    blockEntity = TileEntity.create(pos, blockState, compoundTag);
                }

                if (blockEntity != null) {
                    chunkAccess.setTileEntity(blockEntity);
                    return blockEntity;
                }
            }

            if (blockState.isTileEntity()) {
                LOGGER.warn("Tried to access a block entity before it was created. {}", (Object)pos);
            }

            return null;
        }
    }

    @Override
    public boolean ensureCanWrite(BlockPosition blockPos) {
        int i = SectionPosition.blockToSectionCoord(blockPos.getX());
        int j = SectionPosition.blockToSectionCoord(blockPos.getZ());
        int k = Math.abs(this.center.x - i);
        int l = Math.abs(this.center.z - j);
        if (k <= this.writeRadiusCutoff && l <= this.writeRadiusCutoff) {
            return true;
        } else {
            SystemUtils.logAndPauseIfInIde("Detected setBlock in a far chunk [" + i + ", " + j + "], pos: " + blockPos + ", status: " + this.generatingStatus + (this.currentlyGenerating == null ? "" : ", currently generating: " + (String)this.currentlyGenerating.get()));
            return false;
        }
    }

    @Override
    public boolean setBlock(BlockPosition pos, IBlockData state, int flags, int maxUpdateDepth) {
        if (!this.ensureCanWrite(pos)) {
            return false;
        } else {
            IChunkAccess chunkAccess = this.getChunk(pos);
            IBlockData blockState = chunkAccess.setType(pos, state, false);
            if (blockState != null) {
                this.level.onBlockStateChange(pos, blockState, state);
            }

            if (state.isTileEntity()) {
                if (chunkAccess.getChunkStatus().getType() == ChunkStatus.Type.LEVELCHUNK) {
                    TileEntity blockEntity = ((ITileEntity)state.getBlock()).createTile(pos, state);
                    if (blockEntity != null) {
                        chunkAccess.setTileEntity(blockEntity);
                    } else {
                        chunkAccess.removeTileEntity(pos);
                    }
                } else {
                    NBTTagCompound compoundTag = new NBTTagCompound();
                    compoundTag.setInt("x", pos.getX());
                    compoundTag.setInt("y", pos.getY());
                    compoundTag.setInt("z", pos.getZ());
                    compoundTag.setString("id", "DUMMY");
                    chunkAccess.setBlockEntityNbt(compoundTag);
                }
            } else if (blockState != null && blockState.isTileEntity()) {
                chunkAccess.removeTileEntity(pos);
            }

            if (state.hasPostProcess(this, pos)) {
                this.markPosForPostprocessing(pos);
            }

            return true;
        }
    }

    private void markPosForPostprocessing(BlockPosition pos) {
        this.getChunk(pos).markPosForPostprocessing(pos);
    }

    @Override
    public boolean addEntity(Entity entity) {
        int i = SectionPosition.blockToSectionCoord(entity.getBlockX());
        int j = SectionPosition.blockToSectionCoord(entity.getBlockZ());
        this.getChunkAt(i, j).addEntity(entity);
        return true;
    }

    @Override
    public boolean removeBlock(BlockPosition pos, boolean move) {
        return this.setTypeAndData(pos, Blocks.AIR.getBlockData(), 3);
    }

    @Override
    public WorldBorder getWorldBorder() {
        return this.level.getWorldBorder();
    }

    @Override
    public boolean isClientSide() {
        return false;
    }

    @Deprecated
    @Override
    public WorldServer getLevel() {
        return this.level;
    }

    @Override
    public IRegistryCustom registryAccess() {
        return this.level.registryAccess();
    }

    @Override
    public WorldData getWorldData() {
        return this.levelData;
    }

    @Override
    public DifficultyDamageScaler getDamageScaler(BlockPosition pos) {
        if (!this.isChunkLoaded(SectionPosition.blockToSectionCoord(pos.getX()), SectionPosition.blockToSectionCoord(pos.getZ()))) {
            throw new RuntimeException("We are asking a region for a chunk out of bound");
        } else {
            return new DifficultyDamageScaler(this.level.getDifficulty(), this.level.getDayTime(), 0L, this.level.getMoonBrightness());
        }
    }

    @Nullable
    @Override
    public MinecraftServer getMinecraftServer() {
        return this.level.getMinecraftServer();
    }

    @Override
    public IChunkProvider getChunkProvider() {
        return this.level.getChunkSource();
    }

    @Override
    public long getSeed() {
        return this.seed;
    }

    @Override
    public TickList<Block> getBlockTickList() {
        return this.blockTicks;
    }

    @Override
    public TickList<FluidType> getFluidTickList() {
        return this.liquidTicks;
    }

    @Override
    public int getSeaLevel() {
        return this.level.getSeaLevel();
    }

    @Override
    public Random getRandom() {
        return this.random;
    }

    @Override
    public int getHeight(HeightMap.Type heightmap, int x, int z) {
        return this.getChunkAt(SectionPosition.blockToSectionCoord(x), SectionPosition.blockToSectionCoord(z)).getHighestBlock(heightmap, x & 15, z & 15) + 1;
    }

    @Override
    public void playSound(@Nullable EntityHuman player, BlockPosition pos, SoundEffect sound, EnumSoundCategory category, float volume, float pitch) {
    }

    @Override
    public void addParticle(ParticleParam parameters, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
    }

    @Override
    public void triggerEffect(@Nullable EntityHuman player, int eventId, BlockPosition pos, int data) {
    }

    @Override
    public void gameEvent(@Nullable Entity entity, GameEvent event, BlockPosition pos) {
    }

    @Override
    public DimensionManager getDimensionManager() {
        return this.dimensionType;
    }

    @Override
    public boolean isStateAtPosition(BlockPosition pos, Predicate<IBlockData> state) {
        return state.test(this.getType(pos));
    }

    @Override
    public boolean isFluidAtPosition(BlockPosition pos, Predicate<Fluid> state) {
        return state.test(this.getFluid(pos));
    }

    @Override
    public <T extends Entity> List<T> getEntities(EntityTypeTest<Entity, T> filter, AxisAlignedBB box, Predicate<? super T> predicate) {
        return Collections.emptyList();
    }

    @Override
    public List<Entity> getEntities(@Nullable Entity except, AxisAlignedBB box, @Nullable Predicate<? super Entity> predicate) {
        return Collections.emptyList();
    }

    @Override
    public List<EntityHuman> getPlayers() {
        return Collections.emptyList();
    }

    @Override
    public Stream<? extends StructureStart<?>> startsForFeature(SectionPosition pos, StructureGenerator<?> feature) {
        return this.structureFeatureManager.startsForFeature(pos, feature);
    }

    @Override
    public int getMinBuildHeight() {
        return this.level.getMinBuildHeight();
    }

    @Override
    public int getHeight() {
        return this.level.getHeight();
    }
}
