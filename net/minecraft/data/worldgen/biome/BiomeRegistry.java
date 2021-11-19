package net.minecraft.data.worldgen.biome;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.data.RegistryGeneration;
import net.minecraft.data.worldgen.WorldGenSurfaceComposites;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.Biomes;

public abstract class BiomeRegistry {
    private static final Int2ObjectMap<ResourceKey<BiomeBase>> TO_NAME = new Int2ObjectArrayMap<>();
    public static final BiomeBase PLAINS = register(1, Biomes.PLAINS, WorldGenBiomeSettingsDefault.plainsBiome(false));
    public static final BiomeBase THE_VOID = register(127, Biomes.THE_VOID, WorldGenBiomeSettingsDefault.theVoidBiome());

    private static BiomeBase register(int rawId, ResourceKey<BiomeBase> registryKey, BiomeBase biome) {
        TO_NAME.put(rawId, registryKey);
        return RegistryGeneration.registerMapping(RegistryGeneration.BIOME, rawId, registryKey, biome);
    }

    public static ResourceKey<BiomeBase> byId(int rawId) {
        return TO_NAME.get(rawId);
    }

    static {
        register(0, Biomes.OCEAN, WorldGenBiomeSettingsDefault.oceanBiome(false));
        register(2, Biomes.DESERT, WorldGenBiomeSettingsDefault.desertBiome(0.125F, 0.05F, true, true, true));
        register(3, Biomes.MOUNTAINS, WorldGenBiomeSettingsDefault.mountainBiome(1.0F, 0.5F, WorldGenSurfaceComposites.MOUNTAIN, false));
        register(4, Biomes.FOREST, WorldGenBiomeSettingsDefault.forestBiome(0.1F, 0.2F));
        register(5, Biomes.TAIGA, WorldGenBiomeSettingsDefault.taigaBiome(0.2F, 0.2F, false, false, true, false));
        register(6, Biomes.SWAMP, WorldGenBiomeSettingsDefault.swampBiome(-0.2F, 0.1F, false));
        register(7, Biomes.RIVER, WorldGenBiomeSettingsDefault.riverBiome(-0.5F, 0.0F, 0.5F, 4159204, false));
        register(8, Biomes.NETHER_WASTES, WorldGenBiomeSettingsDefault.netherWastesBiome());
        register(9, Biomes.THE_END, WorldGenBiomeSettingsDefault.theEndBiome());
        register(10, Biomes.FROZEN_OCEAN, WorldGenBiomeSettingsDefault.frozenOceanBiome(false));
        register(11, Biomes.FROZEN_RIVER, WorldGenBiomeSettingsDefault.riverBiome(-0.5F, 0.0F, 0.0F, 3750089, true));
        register(12, Biomes.SNOWY_TUNDRA, WorldGenBiomeSettingsDefault.tundraBiome(0.125F, 0.05F, false, false));
        register(13, Biomes.SNOWY_MOUNTAINS, WorldGenBiomeSettingsDefault.tundraBiome(0.45F, 0.3F, false, true));
        register(14, Biomes.MUSHROOM_FIELDS, WorldGenBiomeSettingsDefault.mushroomFieldsBiome(0.2F, 0.3F));
        register(15, Biomes.MUSHROOM_FIELD_SHORE, WorldGenBiomeSettingsDefault.mushroomFieldsBiome(0.0F, 0.025F));
        register(16, Biomes.BEACH, WorldGenBiomeSettingsDefault.beachBiome(0.0F, 0.025F, 0.8F, 0.4F, 4159204, false, false));
        register(17, Biomes.DESERT_HILLS, WorldGenBiomeSettingsDefault.desertBiome(0.45F, 0.3F, false, true, false));
        register(18, Biomes.WOODED_HILLS, WorldGenBiomeSettingsDefault.forestBiome(0.45F, 0.3F));
        register(19, Biomes.TAIGA_HILLS, WorldGenBiomeSettingsDefault.taigaBiome(0.45F, 0.3F, false, false, false, false));
        register(20, Biomes.MOUNTAIN_EDGE, WorldGenBiomeSettingsDefault.mountainBiome(0.8F, 0.3F, WorldGenSurfaceComposites.GRASS, true));
        register(21, Biomes.JUNGLE, WorldGenBiomeSettingsDefault.jungleBiome());
        register(22, Biomes.JUNGLE_HILLS, WorldGenBiomeSettingsDefault.jungleHillsBiome());
        register(23, Biomes.JUNGLE_EDGE, WorldGenBiomeSettingsDefault.jungleEdgeBiome());
        register(24, Biomes.DEEP_OCEAN, WorldGenBiomeSettingsDefault.oceanBiome(true));
        register(25, Biomes.STONE_SHORE, WorldGenBiomeSettingsDefault.beachBiome(0.1F, 0.8F, 0.2F, 0.3F, 4159204, false, true));
        register(26, Biomes.SNOWY_BEACH, WorldGenBiomeSettingsDefault.beachBiome(0.0F, 0.025F, 0.05F, 0.3F, 4020182, true, false));
        register(27, Biomes.BIRCH_FOREST, WorldGenBiomeSettingsDefault.birchForestBiome(0.1F, 0.2F, false));
        register(28, Biomes.BIRCH_FOREST_HILLS, WorldGenBiomeSettingsDefault.birchForestBiome(0.45F, 0.3F, false));
        register(29, Biomes.DARK_FOREST, WorldGenBiomeSettingsDefault.darkForestBiome(0.1F, 0.2F, false));
        register(30, Biomes.SNOWY_TAIGA, WorldGenBiomeSettingsDefault.taigaBiome(0.2F, 0.2F, true, false, false, true));
        register(31, Biomes.SNOWY_TAIGA_HILLS, WorldGenBiomeSettingsDefault.taigaBiome(0.45F, 0.3F, true, false, false, false));
        register(32, Biomes.GIANT_TREE_TAIGA, WorldGenBiomeSettingsDefault.giantTreeTaiga(0.2F, 0.2F, 0.3F, false));
        register(33, Biomes.GIANT_TREE_TAIGA_HILLS, WorldGenBiomeSettingsDefault.giantTreeTaiga(0.45F, 0.3F, 0.3F, false));
        register(34, Biomes.WOODED_MOUNTAINS, WorldGenBiomeSettingsDefault.mountainBiome(1.0F, 0.5F, WorldGenSurfaceComposites.GRASS, true));
        register(35, Biomes.SAVANNA, WorldGenBiomeSettingsDefault.savannaBiome(0.125F, 0.05F, 1.2F, false, false));
        register(36, Biomes.SAVANNA_PLATEAU, WorldGenBiomeSettingsDefault.savanaPlateauBiome());
        register(37, Biomes.BADLANDS, WorldGenBiomeSettingsDefault.badlandsBiome(0.1F, 0.2F, false));
        register(38, Biomes.WOODED_BADLANDS_PLATEAU, WorldGenBiomeSettingsDefault.woodedBadlandsPlateauBiome(1.5F, 0.025F));
        register(39, Biomes.BADLANDS_PLATEAU, WorldGenBiomeSettingsDefault.badlandsBiome(1.5F, 0.025F, true));
        register(40, Biomes.SMALL_END_ISLANDS, WorldGenBiomeSettingsDefault.smallEndIslandsBiome());
        register(41, Biomes.END_MIDLANDS, WorldGenBiomeSettingsDefault.endMidlandsBiome());
        register(42, Biomes.END_HIGHLANDS, WorldGenBiomeSettingsDefault.endHighlandsBiome());
        register(43, Biomes.END_BARRENS, WorldGenBiomeSettingsDefault.endBarrensBiome());
        register(44, Biomes.WARM_OCEAN, WorldGenBiomeSettingsDefault.warmOceanBiome());
        register(45, Biomes.LUKEWARM_OCEAN, WorldGenBiomeSettingsDefault.lukeWarmOceanBiome(false));
        register(46, Biomes.COLD_OCEAN, WorldGenBiomeSettingsDefault.coldOceanBiome(false));
        register(47, Biomes.DEEP_WARM_OCEAN, WorldGenBiomeSettingsDefault.deepWarmOceanBiome());
        register(48, Biomes.DEEP_LUKEWARM_OCEAN, WorldGenBiomeSettingsDefault.lukeWarmOceanBiome(true));
        register(49, Biomes.DEEP_COLD_OCEAN, WorldGenBiomeSettingsDefault.coldOceanBiome(true));
        register(50, Biomes.DEEP_FROZEN_OCEAN, WorldGenBiomeSettingsDefault.frozenOceanBiome(true));
        register(129, Biomes.SUNFLOWER_PLAINS, WorldGenBiomeSettingsDefault.plainsBiome(true));
        register(130, Biomes.DESERT_LAKES, WorldGenBiomeSettingsDefault.desertBiome(0.225F, 0.25F, false, false, false));
        register(131, Biomes.GRAVELLY_MOUNTAINS, WorldGenBiomeSettingsDefault.mountainBiome(1.0F, 0.5F, WorldGenSurfaceComposites.GRAVELLY_MOUNTAIN, false));
        register(132, Biomes.FLOWER_FOREST, WorldGenBiomeSettingsDefault.flowerForestBiome());
        register(133, Biomes.TAIGA_MOUNTAINS, WorldGenBiomeSettingsDefault.taigaBiome(0.3F, 0.4F, false, true, false, false));
        register(134, Biomes.SWAMP_HILLS, WorldGenBiomeSettingsDefault.swampBiome(-0.1F, 0.3F, true));
        register(140, Biomes.ICE_SPIKES, WorldGenBiomeSettingsDefault.tundraBiome(0.425F, 0.45000002F, true, false));
        register(149, Biomes.MODIFIED_JUNGLE, WorldGenBiomeSettingsDefault.modifiedJungleBiome());
        register(151, Biomes.MODIFIED_JUNGLE_EDGE, WorldGenBiomeSettingsDefault.modifiedJungleEdgeBiome());
        register(155, Biomes.TALL_BIRCH_FOREST, WorldGenBiomeSettingsDefault.birchForestBiome(0.2F, 0.4F, true));
        register(156, Biomes.TALL_BIRCH_HILLS, WorldGenBiomeSettingsDefault.birchForestBiome(0.55F, 0.5F, true));
        register(157, Biomes.DARK_FOREST_HILLS, WorldGenBiomeSettingsDefault.darkForestBiome(0.2F, 0.4F, true));
        register(158, Biomes.SNOWY_TAIGA_MOUNTAINS, WorldGenBiomeSettingsDefault.taigaBiome(0.3F, 0.4F, true, true, false, false));
        register(160, Biomes.GIANT_SPRUCE_TAIGA, WorldGenBiomeSettingsDefault.giantTreeTaiga(0.2F, 0.2F, 0.25F, true));
        register(161, Biomes.GIANT_SPRUCE_TAIGA_HILLS, WorldGenBiomeSettingsDefault.giantTreeTaiga(0.2F, 0.2F, 0.25F, true));
        register(162, Biomes.MODIFIED_GRAVELLY_MOUNTAINS, WorldGenBiomeSettingsDefault.mountainBiome(1.0F, 0.5F, WorldGenSurfaceComposites.GRAVELLY_MOUNTAIN, false));
        register(163, Biomes.SHATTERED_SAVANNA, WorldGenBiomeSettingsDefault.savannaBiome(0.3625F, 1.225F, 1.1F, true, true));
        register(164, Biomes.SHATTERED_SAVANNA_PLATEAU, WorldGenBiomeSettingsDefault.savannaBiome(1.05F, 1.2125001F, 1.0F, true, true));
        register(165, Biomes.ERODED_BADLANDS, WorldGenBiomeSettingsDefault.erodedBadlandsBiome());
        register(166, Biomes.MODIFIED_WOODED_BADLANDS_PLATEAU, WorldGenBiomeSettingsDefault.woodedBadlandsPlateauBiome(0.45F, 0.3F));
        register(167, Biomes.MODIFIED_BADLANDS_PLATEAU, WorldGenBiomeSettingsDefault.badlandsBiome(0.45F, 0.3F, true));
        register(168, Biomes.BAMBOO_JUNGLE, WorldGenBiomeSettingsDefault.bambooJungleBiome());
        register(169, Biomes.BAMBOO_JUNGLE_HILLS, WorldGenBiomeSettingsDefault.bambooJungleHillsBiome());
        register(170, Biomes.SOUL_SAND_VALLEY, WorldGenBiomeSettingsDefault.soulSandValleyBiome());
        register(171, Biomes.CRIMSON_FOREST, WorldGenBiomeSettingsDefault.crimsonForestBiome());
        register(172, Biomes.WARPED_FOREST, WorldGenBiomeSettingsDefault.warpedForestBiome());
        register(173, Biomes.BASALT_DELTAS, WorldGenBiomeSettingsDefault.basaltDeltasBiome());
        register(174, Biomes.DRIPSTONE_CAVES, WorldGenBiomeSettingsDefault.dripstoneCaves());
        register(175, Biomes.LUSH_CAVES, WorldGenBiomeSettingsDefault.lushCaves());
    }
}
