package net.minecraft.world.level.levelgen.carver;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import java.util.BitSet;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.IRegistry;
import net.minecraft.tags.TagsFluid;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.chunk.IChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.BaseStoneSource;
import net.minecraft.world.level.levelgen.SingleBaseStoneSource;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidType;
import net.minecraft.world.level.material.FluidTypes;
import org.apache.commons.lang3.mutable.MutableBoolean;

public abstract class WorldGenCarverAbstract<C extends WorldGenCarverConfiguration> {
    public static final WorldGenCarverAbstract<CaveCarverConfiguration> CAVE = register("cave", new WorldGenCaves(CaveCarverConfiguration.CODEC));
    public static final WorldGenCarverAbstract<CaveCarverConfiguration> NETHER_CAVE = register("nether_cave", new WorldGenCavesHell(CaveCarverConfiguration.CODEC));
    public static final WorldGenCarverAbstract<CanyonCarverConfiguration> CANYON = register("canyon", new WorldGenCanyon(CanyonCarverConfiguration.CODEC));
    public static final WorldGenCarverAbstract<CanyonCarverConfiguration> UNDERWATER_CANYON = register("underwater_canyon", new WorldGenCanyonOcean(CanyonCarverConfiguration.CODEC));
    public static final WorldGenCarverAbstract<CaveCarverConfiguration> UNDERWATER_CAVE = register("underwater_cave", new WorldGenCavesOcean(CaveCarverConfiguration.CODEC));
    protected static final BaseStoneSource STONE_SOURCE = new SingleBaseStoneSource(Blocks.STONE.getBlockData());
    protected static final IBlockData AIR = Blocks.AIR.getBlockData();
    protected static final IBlockData CAVE_AIR = Blocks.CAVE_AIR.getBlockData();
    protected static final Fluid WATER = FluidTypes.WATER.defaultFluidState();
    protected static final Fluid LAVA = FluidTypes.LAVA.defaultFluidState();
    protected Set<Block> replaceableBlocks = ImmutableSet.of(Blocks.STONE, Blocks.GRANITE, Blocks.DIORITE, Blocks.ANDESITE, Blocks.DIRT, Blocks.COARSE_DIRT, Blocks.PODZOL, Blocks.GRASS_BLOCK, Blocks.TERRACOTTA, Blocks.WHITE_TERRACOTTA, Blocks.ORANGE_TERRACOTTA, Blocks.MAGENTA_TERRACOTTA, Blocks.LIGHT_BLUE_TERRACOTTA, Blocks.YELLOW_TERRACOTTA, Blocks.LIME_TERRACOTTA, Blocks.PINK_TERRACOTTA, Blocks.GRAY_TERRACOTTA, Blocks.LIGHT_GRAY_TERRACOTTA, Blocks.CYAN_TERRACOTTA, Blocks.PURPLE_TERRACOTTA, Blocks.BLUE_TERRACOTTA, Blocks.BROWN_TERRACOTTA, Blocks.GREEN_TERRACOTTA, Blocks.RED_TERRACOTTA, Blocks.BLACK_TERRACOTTA, Blocks.SANDSTONE, Blocks.RED_SANDSTONE, Blocks.MYCELIUM, Blocks.SNOW, Blocks.PACKED_ICE, Blocks.DEEPSLATE, Blocks.TUFF, Blocks.GRANITE, Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE, Blocks.RAW_IRON_BLOCK, Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE, Blocks.RAW_COPPER_BLOCK);
    protected Set<FluidType> liquids = ImmutableSet.of(FluidTypes.WATER);
    private final Codec<WorldGenCarverWrapper<C>> configuredCodec;

    private static <C extends WorldGenCarverConfiguration, F extends WorldGenCarverAbstract<C>> F register(String name, F carver) {
        return IRegistry.register(IRegistry.CARVER, name, carver);
    }

    public WorldGenCarverAbstract(Codec<C> configCodec) {
        this.configuredCodec = configCodec.fieldOf("config").xmap(this::configured, WorldGenCarverWrapper::config).codec();
    }

    public WorldGenCarverWrapper<C> configured(C config) {
        return new WorldGenCarverWrapper<>(this, config);
    }

    public Codec<WorldGenCarverWrapper<C>> configuredCodec() {
        return this.configuredCodec;
    }

    public int getRange() {
        return 4;
    }

    protected boolean carveEllipsoid(CarvingContext context, C config, IChunkAccess chunkAccess, Function<BlockPosition, BiomeBase> posToBiome, long seed, Aquifer sampler, double x, double y, double z, double horizontalScale, double verticalScale, BitSet carvingMask, WorldGenCarverAbstract.CarveSkipChecker skipPredicate) {
        ChunkCoordIntPair chunkPos = chunkAccess.getPos();
        int i = chunkPos.x;
        int j = chunkPos.z;
        Random random = new Random(seed + (long)i + (long)j);
        double d = (double)chunkPos.getMiddleBlockX();
        double e = (double)chunkPos.getMiddleBlockZ();
        double f = 16.0D + horizontalScale * 2.0D;
        if (!(Math.abs(x - d) > f) && !(Math.abs(z - e) > f)) {
            int k = chunkPos.getMinBlockX();
            int l = chunkPos.getMinBlockZ();
            int m = Math.max(MathHelper.floor(x - horizontalScale) - k - 1, 0);
            int n = Math.min(MathHelper.floor(x + horizontalScale) - k, 15);
            int o = Math.max(MathHelper.floor(y - verticalScale) - 1, context.getMinGenY() + 1);
            int p = Math.min(MathHelper.floor(y + verticalScale) + 1, context.getMinGenY() + context.getGenDepth() - 8);
            int q = Math.max(MathHelper.floor(z - horizontalScale) - l - 1, 0);
            int r = Math.min(MathHelper.floor(z + horizontalScale) - l, 15);
            if (!config.aquifersEnabled && this.hasDisallowedLiquid(chunkAccess, m, n, o, p, q, r)) {
                return false;
            } else {
                boolean bl = false;
                BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();
                BlockPosition.MutableBlockPosition mutableBlockPos2 = new BlockPosition.MutableBlockPosition();

                for(int s = m; s <= n; ++s) {
                    int t = chunkPos.getBlockX(s);
                    double g = ((double)t + 0.5D - x) / horizontalScale;

                    for(int u = q; u <= r; ++u) {
                        int v = chunkPos.getBlockZ(u);
                        double h = ((double)v + 0.5D - z) / horizontalScale;
                        if (!(g * g + h * h >= 1.0D)) {
                            MutableBoolean mutableBoolean = new MutableBoolean(false);

                            for(int w = p; w > o; --w) {
                                double aa = ((double)w - 0.5D - y) / verticalScale;
                                if (!skipPredicate.shouldSkip(context, g, aa, h, w)) {
                                    int ab = w - context.getMinGenY();
                                    int ac = s | u << 4 | ab << 8;
                                    if (!carvingMask.get(ac) || isDebugEnabled(config)) {
                                        carvingMask.set(ac);
                                        mutableBlockPos.set(t, w, v);
                                        bl |= this.carveBlock(context, config, chunkAccess, posToBiome, carvingMask, random, mutableBlockPos, mutableBlockPos2, sampler, mutableBoolean);
                                    }
                                }
                            }
                        }
                    }
                }

                return bl;
            }
        } else {
            return false;
        }
    }

    protected boolean carveBlock(CarvingContext context, C config, IChunkAccess chunk, Function<BlockPosition, BiomeBase> posToBiome, BitSet carvingMask, Random random, BlockPosition.MutableBlockPosition pos, BlockPosition.MutableBlockPosition downPos, Aquifer sampler, MutableBoolean foundSurface) {
        IBlockData blockState = chunk.getType(pos);
        IBlockData blockState2 = chunk.getType(downPos.setWithOffset(pos, EnumDirection.UP));
        if (blockState.is(Blocks.GRASS_BLOCK) || blockState.is(Blocks.MYCELIUM)) {
            foundSurface.setTrue();
        }

        if (!this.canReplaceBlock(blockState, blockState2) && !isDebugEnabled(config)) {
            return false;
        } else {
            IBlockData blockState3 = this.getCarveState(context, config, pos, sampler);
            if (blockState3 == null) {
                return false;
            } else {
                chunk.setType(pos, blockState3, false);
                if (foundSurface.isTrue()) {
                    downPos.setWithOffset(pos, EnumDirection.DOWN);
                    if (chunk.getType(downPos).is(Blocks.DIRT)) {
                        chunk.setType(downPos, posToBiome.apply(pos).getGenerationSettings().getSurfaceBuilderConfig().getTopMaterial(), false);
                    }
                }

                return true;
            }
        }
    }

    @Nullable
    private IBlockData getCarveState(CarvingContext context, C config, BlockPosition pos, Aquifer sampler) {
        if (pos.getY() <= config.lavaLevel.resolveY(context)) {
            return LAVA.getBlockData();
        } else if (!config.aquifersEnabled) {
            return isDebugEnabled(config) ? getDebugState(config, AIR) : AIR;
        } else {
            IBlockData blockState = sampler.computeState(STONE_SOURCE, pos.getX(), pos.getY(), pos.getZ(), 0.0D);
            if (blockState == Blocks.STONE.getBlockData()) {
                return isDebugEnabled(config) ? config.debugSettings.getBarrierState() : null;
            } else {
                return isDebugEnabled(config) ? getDebugState(config, blockState) : blockState;
            }
        }
    }

    private static IBlockData getDebugState(WorldGenCarverConfiguration config, IBlockData state) {
        if (state.is(Blocks.AIR)) {
            return config.debugSettings.getAirState();
        } else if (state.is(Blocks.WATER)) {
            IBlockData blockState = config.debugSettings.getWaterState();
            return blockState.hasProperty(BlockProperties.WATERLOGGED) ? blockState.set(BlockProperties.WATERLOGGED, Boolean.valueOf(true)) : blockState;
        } else {
            return state.is(Blocks.LAVA) ? config.debugSettings.getLavaState() : state;
        }
    }

    public abstract boolean carve(CarvingContext context, C config, IChunkAccess chunk, Function<BlockPosition, BiomeBase> posToBiome, Random random, Aquifer aquifer, ChunkCoordIntPair pos, BitSet carvingMask);

    public abstract boolean isStartChunk(C config, Random random);

    protected boolean canReplaceBlock(IBlockData state) {
        return this.replaceableBlocks.contains(state.getBlock());
    }

    protected boolean canReplaceBlock(IBlockData state, IBlockData stateAbove) {
        return this.canReplaceBlock(state) || (state.is(Blocks.SAND) || state.is(Blocks.GRAVEL)) && !stateAbove.getFluid().is(TagsFluid.WATER);
    }

    protected boolean hasDisallowedLiquid(IChunkAccess chunk, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        ChunkCoordIntPair chunkPos = chunk.getPos();
        int i = chunkPos.getMinBlockX();
        int j = chunkPos.getMinBlockZ();
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

        for(int k = minX; k <= maxX; ++k) {
            for(int l = minZ; l <= maxZ; ++l) {
                for(int m = minY - 1; m <= maxY + 1; ++m) {
                    mutableBlockPos.set(i + k, m, j + l);
                    if (this.liquids.contains(chunk.getFluid(mutableBlockPos).getType())) {
                        return true;
                    }

                    if (m != maxY + 1 && !isEdge(k, l, minX, maxX, minZ, maxZ)) {
                        m = maxY;
                    }
                }
            }
        }

        return false;
    }

    private static boolean isEdge(int x, int z, int minX, int maxX, int minZ, int maxZ) {
        return x == minX || x == maxX || z == minZ || z == maxZ;
    }

    protected static boolean canReach(ChunkCoordIntPair pos, double x, double z, int branchIndex, int branchCount, float baseWidth) {
        double d = (double)pos.getMiddleBlockX();
        double e = (double)pos.getMiddleBlockZ();
        double f = x - d;
        double g = z - e;
        double h = (double)(branchCount - branchIndex);
        double i = (double)(baseWidth + 2.0F + 16.0F);
        return f * f + g * g - h * h <= i * i;
    }

    private static boolean isDebugEnabled(WorldGenCarverConfiguration config) {
        return config.debugSettings.isDebugMode();
    }

    public interface CarveSkipChecker {
        boolean shouldSkip(CarvingContext context, double scaledRelativeX, double scaledRelativeY, double scaledRelativeZ, int y);
    }
}
