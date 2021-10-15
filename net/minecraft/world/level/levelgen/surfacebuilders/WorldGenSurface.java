package net.minecraft.world.level.levelgen.surfacebuilders;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.IRegistry;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.IChunkAccess;

public abstract class WorldGenSurface<C extends WorldGenSurfaceConfiguration> {
    private static final IBlockData DIRT = Blocks.DIRT.getBlockData();
    private static final IBlockData GRASS_BLOCK = Blocks.GRASS_BLOCK.getBlockData();
    private static final IBlockData PODZOL = Blocks.PODZOL.getBlockData();
    private static final IBlockData GRAVEL = Blocks.GRAVEL.getBlockData();
    private static final IBlockData STONE = Blocks.STONE.getBlockData();
    private static final IBlockData COARSE_DIRT = Blocks.COARSE_DIRT.getBlockData();
    private static final IBlockData SAND = Blocks.SAND.getBlockData();
    private static final IBlockData RED_SAND = Blocks.RED_SAND.getBlockData();
    private static final IBlockData WHITE_TERRACOTTA = Blocks.WHITE_TERRACOTTA.getBlockData();
    private static final IBlockData MYCELIUM = Blocks.MYCELIUM.getBlockData();
    private static final IBlockData SOUL_SAND = Blocks.SOUL_SAND.getBlockData();
    private static final IBlockData NETHERRACK = Blocks.NETHERRACK.getBlockData();
    private static final IBlockData ENDSTONE = Blocks.END_STONE.getBlockData();
    private static final IBlockData CRIMSON_NYLIUM = Blocks.CRIMSON_NYLIUM.getBlockData();
    private static final IBlockData WARPED_NYLIUM = Blocks.WARPED_NYLIUM.getBlockData();
    private static final IBlockData NETHER_WART_BLOCK = Blocks.NETHER_WART_BLOCK.getBlockData();
    private static final IBlockData WARPED_WART_BLOCK = Blocks.WARPED_WART_BLOCK.getBlockData();
    private static final IBlockData BLACKSTONE = Blocks.BLACKSTONE.getBlockData();
    private static final IBlockData BASALT = Blocks.BASALT.getBlockData();
    private static final IBlockData MAGMA = Blocks.MAGMA_BLOCK.getBlockData();
    public static final WorldGenSurfaceConfigurationBase CONFIG_PODZOL = new WorldGenSurfaceConfigurationBase(PODZOL, DIRT, GRAVEL);
    public static final WorldGenSurfaceConfigurationBase CONFIG_GRAVEL = new WorldGenSurfaceConfigurationBase(GRAVEL, GRAVEL, GRAVEL);
    public static final WorldGenSurfaceConfigurationBase CONFIG_GRASS = new WorldGenSurfaceConfigurationBase(GRASS_BLOCK, DIRT, GRAVEL);
    public static final WorldGenSurfaceConfigurationBase CONFIG_STONE = new WorldGenSurfaceConfigurationBase(STONE, STONE, GRAVEL);
    public static final WorldGenSurfaceConfigurationBase CONFIG_COARSE_DIRT = new WorldGenSurfaceConfigurationBase(COARSE_DIRT, DIRT, GRAVEL);
    public static final WorldGenSurfaceConfigurationBase CONFIG_DESERT = new WorldGenSurfaceConfigurationBase(SAND, SAND, GRAVEL);
    public static final WorldGenSurfaceConfigurationBase CONFIG_OCEAN_SAND = new WorldGenSurfaceConfigurationBase(GRASS_BLOCK, DIRT, SAND);
    public static final WorldGenSurfaceConfigurationBase CONFIG_FULL_SAND = new WorldGenSurfaceConfigurationBase(SAND, SAND, SAND);
    public static final WorldGenSurfaceConfigurationBase CONFIG_BADLANDS = new WorldGenSurfaceConfigurationBase(RED_SAND, WHITE_TERRACOTTA, GRAVEL);
    public static final WorldGenSurfaceConfigurationBase CONFIG_MYCELIUM = new WorldGenSurfaceConfigurationBase(MYCELIUM, DIRT, GRAVEL);
    public static final WorldGenSurfaceConfigurationBase CONFIG_HELL = new WorldGenSurfaceConfigurationBase(NETHERRACK, NETHERRACK, NETHERRACK);
    public static final WorldGenSurfaceConfigurationBase CONFIG_SOUL_SAND_VALLEY = new WorldGenSurfaceConfigurationBase(SOUL_SAND, SOUL_SAND, SOUL_SAND);
    public static final WorldGenSurfaceConfigurationBase CONFIG_THEEND = new WorldGenSurfaceConfigurationBase(ENDSTONE, ENDSTONE, ENDSTONE);
    public static final WorldGenSurfaceConfigurationBase CONFIG_CRIMSON_FOREST = new WorldGenSurfaceConfigurationBase(CRIMSON_NYLIUM, NETHERRACK, NETHER_WART_BLOCK);
    public static final WorldGenSurfaceConfigurationBase CONFIG_WARPED_FOREST = new WorldGenSurfaceConfigurationBase(WARPED_NYLIUM, NETHERRACK, WARPED_WART_BLOCK);
    public static final WorldGenSurfaceConfigurationBase CONFIG_BASALT_DELTAS = new WorldGenSurfaceConfigurationBase(BLACKSTONE, BASALT, MAGMA);
    public static final WorldGenSurface<WorldGenSurfaceConfigurationBase> DEFAULT = register("default", new WorldGenSurfaceDefaultBlock(WorldGenSurfaceConfigurationBase.CODEC));
    public static final WorldGenSurface<WorldGenSurfaceConfigurationBase> MOUNTAIN = register("mountain", new WorldGenSurfaceExtremeHills(WorldGenSurfaceConfigurationBase.CODEC));
    public static final WorldGenSurface<WorldGenSurfaceConfigurationBase> SHATTERED_SAVANNA = register("shattered_savanna", new WorldGenSurfaceSavannaMutated(WorldGenSurfaceConfigurationBase.CODEC));
    public static final WorldGenSurface<WorldGenSurfaceConfigurationBase> GRAVELLY_MOUNTAIN = register("gravelly_mountain", new WorldGenSurfaceExtremeHillMutated(WorldGenSurfaceConfigurationBase.CODEC));
    public static final WorldGenSurface<WorldGenSurfaceConfigurationBase> GIANT_TREE_TAIGA = register("giant_tree_taiga", new WorldGenSurfaceTaigaMega(WorldGenSurfaceConfigurationBase.CODEC));
    public static final WorldGenSurface<WorldGenSurfaceConfigurationBase> SWAMP = register("swamp", new WorldGenSurfaceSwamp(WorldGenSurfaceConfigurationBase.CODEC));
    public static final WorldGenSurface<WorldGenSurfaceConfigurationBase> BADLANDS = register("badlands", new WorldGenSurfaceMesa(WorldGenSurfaceConfigurationBase.CODEC));
    public static final WorldGenSurface<WorldGenSurfaceConfigurationBase> WOODED_BADLANDS = register("wooded_badlands", new WorldGenSurfaceMesaForest(WorldGenSurfaceConfigurationBase.CODEC));
    public static final WorldGenSurface<WorldGenSurfaceConfigurationBase> ERODED_BADLANDS = register("eroded_badlands", new WorldGenSurfaceMesaBryce(WorldGenSurfaceConfigurationBase.CODEC));
    public static final WorldGenSurface<WorldGenSurfaceConfigurationBase> FROZEN_OCEAN = register("frozen_ocean", new WorldGenSurfaceFrozenOcean(WorldGenSurfaceConfigurationBase.CODEC));
    public static final WorldGenSurface<WorldGenSurfaceConfigurationBase> NETHER = register("nether", new WorldGenSurfaceNether(WorldGenSurfaceConfigurationBase.CODEC));
    public static final WorldGenSurface<WorldGenSurfaceConfigurationBase> NETHER_FOREST = register("nether_forest", new WorldGenSurfaceNetherForest(WorldGenSurfaceConfigurationBase.CODEC));
    public static final WorldGenSurface<WorldGenSurfaceConfigurationBase> SOUL_SAND_VALLEY = register("soul_sand_valley", new WorldGenSurfaceSoulSandValley(WorldGenSurfaceConfigurationBase.CODEC));
    public static final WorldGenSurface<WorldGenSurfaceConfigurationBase> BASALT_DELTAS = register("basalt_deltas", new WorldGenSurfaceBasaltDeltas(WorldGenSurfaceConfigurationBase.CODEC));
    public static final WorldGenSurface<WorldGenSurfaceConfigurationBase> NOPE = register("nope", new WorldGenSurfaceEmpty(WorldGenSurfaceConfigurationBase.CODEC));
    private final Codec<WorldGenSurfaceComposite<C>> configuredCodec;

    private static <C extends WorldGenSurfaceConfiguration, F extends WorldGenSurface<C>> F register(String id, F surfaceBuilder) {
        return IRegistry.register(IRegistry.SURFACE_BUILDER, id, surfaceBuilder);
    }

    public WorldGenSurface(Codec<C> codec) {
        this.configuredCodec = codec.fieldOf("config").xmap(this::configured, WorldGenSurfaceComposite::config).codec();
    }

    public Codec<WorldGenSurfaceComposite<C>> configuredCodec() {
        return this.configuredCodec;
    }

    public WorldGenSurfaceComposite<C> configured(C config) {
        return new WorldGenSurfaceComposite<>(this, config);
    }

    public abstract void apply(Random random, IChunkAccess chunk, BiomeBase biome, int x, int z, int height, double noise, IBlockData defaultBlock, IBlockData defaultFluid, int seaLevel, int i, long l, C surfaceBuilderConfiguration);

    public void initNoise(long seed) {
    }
}
