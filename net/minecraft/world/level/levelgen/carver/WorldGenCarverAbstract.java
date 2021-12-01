package net.minecraft.world.level.levelgen.carver;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.IRegistry;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.IChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidType;
import net.minecraft.world.level.material.FluidTypes;
import org.apache.commons.lang3.mutable.MutableBoolean;

public abstract class WorldGenCarverAbstract<C extends WorldGenCarverConfiguration> {
    public static final WorldGenCarverAbstract<CaveCarverConfiguration> CAVE = register("cave", new WorldGenCaves(CaveCarverConfiguration.CODEC));
    public static final WorldGenCarverAbstract<CaveCarverConfiguration> NETHER_CAVE = register("nether_cave", new WorldGenCavesHell(CaveCarverConfiguration.CODEC));
    public static final WorldGenCarverAbstract<CanyonCarverConfiguration> CANYON = register("canyon", new WorldGenCanyon(CanyonCarverConfiguration.CODEC));
    protected static final IBlockData AIR = Blocks.AIR.getBlockData();
    protected static final IBlockData CAVE_AIR = Blocks.CAVE_AIR.getBlockData();
    protected static final Fluid WATER = FluidTypes.WATER.defaultFluidState();
    protected static final Fluid LAVA = FluidTypes.LAVA.defaultFluidState();
    protected Set<Block> replaceableBlocks = ImmutableSet.of(Blocks.WATER, Blocks.STONE, Blocks.GRANITE, Blocks.DIORITE, Blocks.ANDESITE, Blocks.DIRT, Blocks.COARSE_DIRT, Blocks.PODZOL, Blocks.GRASS_BLOCK, Blocks.TERRACOTTA, Blocks.WHITE_TERRACOTTA, Blocks.ORANGE_TERRACOTTA, Blocks.MAGENTA_TERRACOTTA, Blocks.LIGHT_BLUE_TERRACOTTA, Blocks.YELLOW_TERRACOTTA, Blocks.LIME_TERRACOTTA, Blocks.PINK_TERRACOTTA, Blocks.GRAY_TERRACOTTA, Blocks.LIGHT_GRAY_TERRACOTTA, Blocks.CYAN_TERRACOTTA, Blocks.PURPLE_TERRACOTTA, Blocks.BLUE_TERRACOTTA, Blocks.BROWN_TERRACOTTA, Blocks.GREEN_TERRACOTTA, Blocks.RED_TERRACOTTA, Blocks.BLACK_TERRACOTTA, Blocks.SANDSTONE, Blocks.RED_SANDSTONE, Blocks.MYCELIUM, Blocks.SNOW, Blocks.PACKED_ICE, Blocks.DEEPSLATE, Blocks.CALCITE, Blocks.SAND, Blocks.RED_SAND, Blocks.GRAVEL, Blocks.TUFF, Blocks.GRANITE, Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE, Blocks.RAW_IRON_BLOCK, Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE, Blocks.RAW_COPPER_BLOCK);
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

    protected boolean carveEllipsoid(CarvingContext context, C config, IChunkAccess chunk, Function<BlockPosition, BiomeBase> posToBiome, Aquifer aquiferSampler, double d, double e, double f, double g, double h, CarvingMask mask, WorldGenCarverAbstract.CarveSkipChecker skipPredicate) {
        ChunkCoordIntPair chunkPos = chunk.getPos();
        double i = (double)chunkPos.getMiddleBlockX();
        double j = (double)chunkPos.getMiddleBlockZ();
        double k = 16.0D + g * 2.0D;
        if (!(Math.abs(d - i) > k) && !(Math.abs(f - j) > k)) {
            int l = chunkPos.getMinBlockX();
            int m = chunkPos.getMinBlockZ();
            int n = Math.max(MathHelper.floor(d - g) - l - 1, 0);
            int o = Math.min(MathHelper.floor(d + g) - l, 15);
            int p = Math.max(MathHelper.floor(e - h) - 1, context.getMinGenY() + 1);
            int q = chunk.isUpgrading() ? 0 : 7;
            int r = Math.min(MathHelper.floor(e + h) + 1, context.getMinGenY() + context.getGenDepth() - 1 - q);
            int s = Math.max(MathHelper.floor(f - g) - m - 1, 0);
            int t = Math.min(MathHelper.floor(f + g) - m, 15);
            boolean bl = false;
            BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();
            BlockPosition.MutableBlockPosition mutableBlockPos2 = new BlockPosition.MutableBlockPosition();

            for(int u = n; u <= o; ++u) {
                int v = chunkPos.getBlockX(u);
                double w = ((double)v + 0.5D - d) / g;

                for(int x = s; x <= t; ++x) {
                    int y = chunkPos.getBlockZ(x);
                    double z = ((double)y + 0.5D - f) / g;
                    if (!(w * w + z * z >= 1.0D)) {
                        MutableBoolean mutableBoolean = new MutableBoolean(false);

                        for(int aa = r; aa > p; --aa) {
                            double ab = ((double)aa - 0.5D - e) / h;
                            if (!skipPredicate.shouldSkip(context, w, ab, z, aa) && (!mask.get(u, aa, x) || isDebugEnabled(config))) {
                                mask.set(u, aa, x);
                                mutableBlockPos.set(v, aa, y);
                                bl |= this.carveBlock(context, config, chunk, posToBiome, mask, mutableBlockPos, mutableBlockPos2, aquiferSampler, mutableBoolean);
                            }
                        }
                    }
                }
            }

            return bl;
        } else {
            return false;
        }
    }

    protected boolean carveBlock(CarvingContext context, C config, IChunkAccess chunk, Function<BlockPosition, BiomeBase> posToBiome, CarvingMask mask, BlockPosition.MutableBlockPosition mutableBlockPos, BlockPosition.MutableBlockPosition mutableBlockPos2, Aquifer aquiferSampler, MutableBoolean mutableBoolean) {
        IBlockData blockState = chunk.getType(mutableBlockPos);
        if (blockState.is(Blocks.GRASS_BLOCK) || blockState.is(Blocks.MYCELIUM)) {
            mutableBoolean.setTrue();
        }

        if (!this.canReplaceBlock(blockState) && !isDebugEnabled(config)) {
            return false;
        } else {
            IBlockData blockState2 = this.getCarveState(context, config, mutableBlockPos, aquiferSampler);
            if (blockState2 == null) {
                return false;
            } else {
                chunk.setType(mutableBlockPos, blockState2, false);
                if (aquiferSampler.shouldScheduleFluidUpdate() && !blockState2.getFluid().isEmpty()) {
                    chunk.markPosForPostprocessing(mutableBlockPos);
                }

                if (mutableBoolean.isTrue()) {
                    mutableBlockPos2.setWithOffset(mutableBlockPos, EnumDirection.DOWN);
                    if (chunk.getType(mutableBlockPos2).is(Blocks.DIRT)) {
                        context.topMaterial(posToBiome, chunk, mutableBlockPos2, !blockState2.getFluid().isEmpty()).ifPresent((state) -> {
                            chunk.setType(mutableBlockPos2, state, false);
                            if (!state.getFluid().isEmpty()) {
                                chunk.markPosForPostprocessing(mutableBlockPos2);
                            }

                        });
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
        } else {
            IBlockData blockState = sampler.computeSubstance(pos.getX(), pos.getY(), pos.getZ(), 0.0D, 0.0D);
            if (blockState == null) {
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

    public abstract boolean carve(CarvingContext context, C config, IChunkAccess chunk, Function<BlockPosition, BiomeBase> posToBiome, Random random, Aquifer aquiferSampler, ChunkCoordIntPair pos, CarvingMask mask);

    public abstract boolean isStartChunk(C config, Random random);

    protected boolean canReplaceBlock(IBlockData state) {
        return this.replaceableBlocks.contains(state.getBlock());
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
