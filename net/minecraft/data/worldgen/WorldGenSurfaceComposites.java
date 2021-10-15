package net.minecraft.data.worldgen;

import net.minecraft.data.RegistryGeneration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.surfacebuilders.WorldGenSurface;
import net.minecraft.world.level.levelgen.surfacebuilders.WorldGenSurfaceComposite;
import net.minecraft.world.level.levelgen.surfacebuilders.WorldGenSurfaceConfiguration;
import net.minecraft.world.level.levelgen.surfacebuilders.WorldGenSurfaceConfigurationBase;

public class WorldGenSurfaceComposites {
    public static final WorldGenSurfaceComposite<WorldGenSurfaceConfigurationBase> BADLANDS = register("badlands", WorldGenSurface.BADLANDS.configured(WorldGenSurface.CONFIG_BADLANDS));
    public static final WorldGenSurfaceComposite<WorldGenSurfaceConfigurationBase> BASALT_DELTAS = register("basalt_deltas", WorldGenSurface.BASALT_DELTAS.configured(WorldGenSurface.CONFIG_BASALT_DELTAS));
    public static final WorldGenSurfaceComposite<WorldGenSurfaceConfigurationBase> CRIMSON_FOREST = register("crimson_forest", WorldGenSurface.NETHER_FOREST.configured(WorldGenSurface.CONFIG_CRIMSON_FOREST));
    public static final WorldGenSurfaceComposite<WorldGenSurfaceConfigurationBase> DESERT = register("desert", WorldGenSurface.DEFAULT.configured(WorldGenSurface.CONFIG_DESERT));
    public static final WorldGenSurfaceComposite<WorldGenSurfaceConfigurationBase> END = register("end", WorldGenSurface.DEFAULT.configured(WorldGenSurface.CONFIG_THEEND));
    public static final WorldGenSurfaceComposite<WorldGenSurfaceConfigurationBase> ERODED_BADLANDS = register("eroded_badlands", WorldGenSurface.ERODED_BADLANDS.configured(WorldGenSurface.CONFIG_BADLANDS));
    public static final WorldGenSurfaceComposite<WorldGenSurfaceConfigurationBase> FROZEN_OCEAN = register("frozen_ocean", WorldGenSurface.FROZEN_OCEAN.configured(WorldGenSurface.CONFIG_GRASS));
    public static final WorldGenSurfaceComposite<WorldGenSurfaceConfigurationBase> FULL_SAND = register("full_sand", WorldGenSurface.DEFAULT.configured(WorldGenSurface.CONFIG_FULL_SAND));
    public static final WorldGenSurfaceComposite<WorldGenSurfaceConfigurationBase> GIANT_TREE_TAIGA = register("giant_tree_taiga", WorldGenSurface.GIANT_TREE_TAIGA.configured(WorldGenSurface.CONFIG_GRASS));
    public static final WorldGenSurfaceComposite<WorldGenSurfaceConfigurationBase> GRASS = register("grass", WorldGenSurface.DEFAULT.configured(WorldGenSurface.CONFIG_GRASS));
    public static final WorldGenSurfaceComposite<WorldGenSurfaceConfigurationBase> GRAVELLY_MOUNTAIN = register("gravelly_mountain", WorldGenSurface.GRAVELLY_MOUNTAIN.configured(WorldGenSurface.CONFIG_GRASS));
    public static final WorldGenSurfaceComposite<WorldGenSurfaceConfigurationBase> ICE_SPIKES = register("ice_spikes", WorldGenSurface.DEFAULT.configured(new WorldGenSurfaceConfigurationBase(Blocks.SNOW_BLOCK.getBlockData(), Blocks.DIRT.getBlockData(), Blocks.GRAVEL.getBlockData())));
    public static final WorldGenSurfaceComposite<WorldGenSurfaceConfigurationBase> MOUNTAIN = register("mountain", WorldGenSurface.MOUNTAIN.configured(WorldGenSurface.CONFIG_GRASS));
    public static final WorldGenSurfaceComposite<WorldGenSurfaceConfigurationBase> MYCELIUM = register("mycelium", WorldGenSurface.DEFAULT.configured(WorldGenSurface.CONFIG_MYCELIUM));
    public static final WorldGenSurfaceComposite<WorldGenSurfaceConfigurationBase> NETHER = register("nether", WorldGenSurface.NETHER.configured(WorldGenSurface.CONFIG_HELL));
    public static final WorldGenSurfaceComposite<WorldGenSurfaceConfigurationBase> NOPE = register("nope", WorldGenSurface.NOPE.configured(WorldGenSurface.CONFIG_STONE));
    public static final WorldGenSurfaceComposite<WorldGenSurfaceConfigurationBase> OCEAN_SAND = register("ocean_sand", WorldGenSurface.DEFAULT.configured(WorldGenSurface.CONFIG_OCEAN_SAND));
    public static final WorldGenSurfaceComposite<WorldGenSurfaceConfigurationBase> SHATTERED_SAVANNA = register("shattered_savanna", WorldGenSurface.SHATTERED_SAVANNA.configured(WorldGenSurface.CONFIG_GRASS));
    public static final WorldGenSurfaceComposite<WorldGenSurfaceConfigurationBase> SOUL_SAND_VALLEY = register("soul_sand_valley", WorldGenSurface.SOUL_SAND_VALLEY.configured(WorldGenSurface.CONFIG_SOUL_SAND_VALLEY));
    public static final WorldGenSurfaceComposite<WorldGenSurfaceConfigurationBase> STONE = register("stone", WorldGenSurface.DEFAULT.configured(WorldGenSurface.CONFIG_STONE));
    public static final WorldGenSurfaceComposite<WorldGenSurfaceConfigurationBase> SWAMP = register("swamp", WorldGenSurface.SWAMP.configured(WorldGenSurface.CONFIG_GRASS));
    public static final WorldGenSurfaceComposite<WorldGenSurfaceConfigurationBase> WARPED_FOREST = register("warped_forest", WorldGenSurface.NETHER_FOREST.configured(WorldGenSurface.CONFIG_WARPED_FOREST));
    public static final WorldGenSurfaceComposite<WorldGenSurfaceConfigurationBase> WOODED_BADLANDS = register("wooded_badlands", WorldGenSurface.WOODED_BADLANDS.configured(WorldGenSurface.CONFIG_BADLANDS));

    private static <SC extends WorldGenSurfaceConfiguration> WorldGenSurfaceComposite<SC> register(String id, WorldGenSurfaceComposite<SC> configuredSurfaceBuilder) {
        return RegistryGeneration.register(RegistryGeneration.CONFIGURED_SURFACE_BUILDER, id, configuredSurfaceBuilder);
    }
}
