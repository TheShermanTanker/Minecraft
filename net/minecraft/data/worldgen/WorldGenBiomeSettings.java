package net.minecraft.data.worldgen;

import net.minecraft.data.worldgen.placement.AquaticPlacements;
import net.minecraft.data.worldgen.placement.CavePlacements;
import net.minecraft.data.worldgen.placement.MiscOverworldPlacements;
import net.minecraft.data.worldgen.placement.OrePlacements;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumCreatureType;
import net.minecraft.world.level.biome.BiomeSettingsGeneration;
import net.minecraft.world.level.biome.BiomeSettingsMobs;
import net.minecraft.world.level.levelgen.WorldGenStage;

public class WorldGenBiomeSettings {
    public static void addDefaultCarversAndLakes(BiomeSettingsGeneration.Builder builder) {
        builder.addCarver(WorldGenStage.Features.AIR, WorldGenCarvers.CAVE);
        builder.addCarver(WorldGenStage.Features.AIR, WorldGenCarvers.CAVE_EXTRA_UNDERGROUND);
        builder.addCarver(WorldGenStage.Features.AIR, WorldGenCarvers.CANYON);
        builder.addFeature(WorldGenStage.Decoration.LAKES, MiscOverworldPlacements.LAKE_LAVA_UNDERGROUND);
        builder.addFeature(WorldGenStage.Decoration.LAKES, MiscOverworldPlacements.LAKE_LAVA_SURFACE);
    }

    public static void addDefaultMonsterRoom(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_STRUCTURES, CavePlacements.MONSTER_ROOM);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_STRUCTURES, CavePlacements.MONSTER_ROOM_DEEP);
    }

    public static void addDefaultUndergroundVariety(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_ORES, OrePlacements.ORE_DIRT);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_ORES, OrePlacements.ORE_GRAVEL);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_ORES, OrePlacements.ORE_GRANITE_UPPER);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_ORES, OrePlacements.ORE_GRANITE_LOWER);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_ORES, OrePlacements.ORE_DIORITE_UPPER);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_ORES, OrePlacements.ORE_DIORITE_LOWER);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_ORES, OrePlacements.ORE_ANDESITE_UPPER);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_ORES, OrePlacements.ORE_ANDESITE_LOWER);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_ORES, OrePlacements.ORE_TUFF);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, CavePlacements.GLOW_LICHEN);
    }

    public static void addDripstone(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.LOCAL_MODIFICATIONS, CavePlacements.LARGE_DRIPSTONE);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, CavePlacements.DRIPSTONE_CLUSTER);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, CavePlacements.POINTED_DRIPSTONE);
    }

    public static void addDefaultOres(BiomeSettingsGeneration.Builder builder) {
        addDefaultOres(builder, false);
    }

    public static void addDefaultOres(BiomeSettingsGeneration.Builder builder, boolean largeCopperOreBlob) {
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_ORES, OrePlacements.ORE_COAL_UPPER);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_ORES, OrePlacements.ORE_COAL_LOWER);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_ORES, OrePlacements.ORE_IRON_UPPER);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_ORES, OrePlacements.ORE_IRON_MIDDLE);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_ORES, OrePlacements.ORE_IRON_SMALL);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_ORES, OrePlacements.ORE_GOLD);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_ORES, OrePlacements.ORE_GOLD_LOWER);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_ORES, OrePlacements.ORE_REDSTONE);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_ORES, OrePlacements.ORE_REDSTONE_LOWER);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_ORES, OrePlacements.ORE_DIAMOND);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_ORES, OrePlacements.ORE_DIAMOND_LARGE);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_ORES, OrePlacements.ORE_DIAMOND_BURIED);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_ORES, OrePlacements.ORE_LAPIS);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_ORES, OrePlacements.ORE_LAPIS_BURIED);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_ORES, largeCopperOreBlob ? OrePlacements.ORE_COPPER_LARGE : OrePlacements.ORE_COPPER);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_ORES, CavePlacements.UNDERWATER_MAGMA);
    }

    public static void addExtraGold(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_ORES, OrePlacements.ORE_GOLD_EXTRA);
    }

    public static void addExtraEmeralds(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_ORES, OrePlacements.ORE_EMERALD);
    }

    public static void addInfestedStone(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, OrePlacements.ORE_INFESTED);
    }

    public static void addDefaultSoftDisks(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_ORES, MiscOverworldPlacements.DISK_SAND);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_ORES, MiscOverworldPlacements.DISK_CLAY);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_ORES, MiscOverworldPlacements.DISK_GRAVEL);
    }

    public static void addSwampClayDisk(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_ORES, MiscOverworldPlacements.DISK_CLAY);
    }

    public static void addMossyStoneBlock(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.LOCAL_MODIFICATIONS, MiscOverworldPlacements.FOREST_ROCK);
    }

    public static void addFerns(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_LARGE_FERN);
    }

    public static void addRareBerryBushes(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_BERRY_RARE);
    }

    public static void addCommonBerryBushes(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_BERRY_COMMON);
    }

    public static void addLightBambooVegetation(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.BAMBOO_LIGHT);
    }

    public static void addBambooVegetation(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.BAMBOO);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.BAMBOO_VEGETATION);
    }

    public static void addTaigaTrees(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.TREES_TAIGA);
    }

    public static void addGroveTrees(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.TREES_GROVE);
    }

    public static void addWaterTrees(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.TREES_WATER);
    }

    public static void addBirchTrees(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.TREES_BIRCH);
    }

    public static void addOtherBirchTrees(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.TREES_BIRCH_AND_OAK);
    }

    public static void addTallBirchTrees(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.BIRCH_TALL);
    }

    public static void addSavannaTrees(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.TREES_SAVANNA);
    }

    public static void addShatteredSavannaTrees(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.TREES_WINDSWEPT_SAVANNA);
    }

    public static void addLushCavesVegetationFeatures(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, CavePlacements.LUSH_CAVES_CEILING_VEGETATION);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, CavePlacements.CAVE_VINES);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, CavePlacements.LUSH_CAVES_CLAY);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, CavePlacements.LUSH_CAVES_VEGETATION);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, CavePlacements.ROOTED_AZALEA_TREE);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, CavePlacements.SPORE_BLOSSOM);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, CavePlacements.CLASSIC_VINES);
    }

    public static void addLushCavesSpecialOres(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_ORES, OrePlacements.ORE_CLAY);
    }

    public static void addMountainTrees(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.TREES_WINDSWEPT_HILLS);
    }

    public static void addMountainForestTrees(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.TREES_WINDSWEPT_FOREST);
    }

    public static void addJungleTrees(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.TREES_JUNGLE);
    }

    public static void addSparseJungleTrees(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.TREES_SPARSE_JUNGLE);
    }

    public static void addBadlandsTrees(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.TREES_BADLANDS);
    }

    public static void addSnowyTrees(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.TREES_SNOWY);
    }

    public static void addJungleGrass(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_GRASS_JUNGLE);
    }

    public static void addSavannaGrass(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_TALL_GRASS);
    }

    public static void addShatteredSavannaGrass(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_GRASS_NORMAL);
    }

    public static void addSavannaExtraGrass(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_GRASS_SAVANNA);
    }

    public static void addBadlandGrass(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_GRASS_BADLANDS);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_DEAD_BUSH_BADLANDS);
    }

    public static void addForestFlowers(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.FOREST_FLOWERS);
    }

    public static void addForestGrass(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_GRASS_FOREST);
    }

    public static void addSwampVegetation(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.TREES_SWAMP);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.FLOWER_SWAMP);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_GRASS_NORMAL);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_DEAD_BUSH);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_WATERLILY);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.BROWN_MUSHROOM_SWAMP);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.RED_MUSHROOM_SWAMP);
    }

    public static void addMushroomFieldVegetation(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.MUSHROOM_ISLAND_VEGETATION);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.BROWN_MUSHROOM_TAIGA);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.RED_MUSHROOM_TAIGA);
    }

    public static void addPlainVegetation(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.TREES_PLAINS);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.FLOWER_PLAINS);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_GRASS_PLAIN);
    }

    public static void addDesertVegetation(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_DEAD_BUSH_2);
    }

    public static void addGiantTaigaVegetation(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_GRASS_TAIGA);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_DEAD_BUSH);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.BROWN_MUSHROOM_OLD_GROWTH);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.RED_MUSHROOM_OLD_GROWTH);
    }

    public static void addDefaultFlowers(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.FLOWER_DEFAULT);
    }

    public static void addMeadowVegetation(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_GRASS_PLAIN);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.FLOWER_MEADOW);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.TREES_MEADOW);
    }

    public static void addWarmFlowers(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.FLOWER_WARM);
    }

    public static void addDefaultGrass(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_GRASS_BADLANDS);
    }

    public static void addTaigaGrass(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_GRASS_TAIGA_2);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.BROWN_MUSHROOM_TAIGA);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.RED_MUSHROOM_TAIGA);
    }

    public static void addPlainGrass(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_TALL_GRASS_2);
    }

    public static void addDefaultMushrooms(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.BROWN_MUSHROOM_NORMAL);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.RED_MUSHROOM_NORMAL);
    }

    public static void addDefaultExtraVegetation(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_SUGAR_CANE);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_PUMPKIN);
    }

    public static void addBadlandExtraVegetation(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_SUGAR_CANE_BADLANDS);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_PUMPKIN);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_CACTUS_DECORATED);
    }

    public static void addJungleMelons(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_MELON);
    }

    public static void addSparseJungleMelons(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_MELON_SPARSE);
    }

    public static void addJungleVines(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.VINES);
    }

    public static void addDesertExtraVegetation(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_SUGAR_CANE_DESERT);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_PUMPKIN);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_CACTUS_DESERT);
    }

    public static void addSwampExtraVegetation(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_SUGAR_CANE_SWAMP);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_PUMPKIN);
    }

    public static void addDesertExtraDecoration(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.SURFACE_STRUCTURES, MiscOverworldPlacements.DESERT_WELL);
    }

    public static void addFossilDecoration(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_STRUCTURES, CavePlacements.FOSSIL_UPPER);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_STRUCTURES, CavePlacements.FOSSIL_LOWER);
    }

    public static void addColdOceanExtraVegetation(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, AquaticPlacements.KELP_COLD);
    }

    public static void addDefaultSeagrass(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, AquaticPlacements.SEAGRASS_SIMPLE);
    }

    public static void addLukeWarmKelp(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, AquaticPlacements.KELP_WARM);
    }

    public static void addDefaultSprings(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.FLUID_SPRINGS, MiscOverworldPlacements.SPRING_WATER);
        builder.addFeature(WorldGenStage.Decoration.FLUID_SPRINGS, MiscOverworldPlacements.SPRING_LAVA);
    }

    public static void addFrozenSprings(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.FLUID_SPRINGS, MiscOverworldPlacements.SPRING_LAVA_FROZEN);
    }

    public static void addIcebergs(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.LOCAL_MODIFICATIONS, MiscOverworldPlacements.ICEBERG_PACKED);
        builder.addFeature(WorldGenStage.Decoration.LOCAL_MODIFICATIONS, MiscOverworldPlacements.ICEBERG_BLUE);
    }

    public static void addBlueIce(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.SURFACE_STRUCTURES, MiscOverworldPlacements.BLUE_ICE);
    }

    public static void addSurfaceFreezing(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.TOP_LAYER_MODIFICATION, MiscOverworldPlacements.FREEZE_TOP_LAYER);
    }

    public static void addNetherDefaultOres(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, OrePlacements.ORE_GRAVEL_NETHER);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, OrePlacements.ORE_BLACKSTONE);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, OrePlacements.ORE_GOLD_NETHER);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, OrePlacements.ORE_QUARTZ_NETHER);
        addAncientDebris(builder);
    }

    public static void addAncientDebris(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, OrePlacements.ORE_ANCIENT_DEBRIS_LARGE);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, OrePlacements.ORE_ANCIENT_DEBRIS_SMALL);
    }

    public static void addDefaultCrystalFormations(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.LOCAL_MODIFICATIONS, CavePlacements.AMETHYST_GEODE);
    }

    public static void farmAnimals(BiomeSettingsMobs.Builder builder) {
        builder.addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.SHEEP, 12, 4, 4));
        builder.addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.PIG, 10, 4, 4));
        builder.addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.CHICKEN, 10, 4, 4));
        builder.addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.COW, 8, 4, 4));
    }

    public static void caveSpawns(BiomeSettingsMobs.Builder builder) {
        builder.addSpawn(EnumCreatureType.AMBIENT, new BiomeSettingsMobs.SpawnerData(EntityTypes.BAT, 10, 8, 8));
        builder.addSpawn(EnumCreatureType.UNDERGROUND_WATER_CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.GLOW_SQUID, 10, 4, 6));
    }

    public static void commonSpawns(BiomeSettingsMobs.Builder builder) {
        caveSpawns(builder);
        monsters(builder, 95, 5, 100, false);
    }

    public static void oceanSpawns(BiomeSettingsMobs.Builder builder, int squidWeight, int squidMaxGroupSize, int codWeight) {
        builder.addSpawn(EnumCreatureType.WATER_CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.SQUID, squidWeight, 1, squidMaxGroupSize));
        builder.addSpawn(EnumCreatureType.WATER_AMBIENT, new BiomeSettingsMobs.SpawnerData(EntityTypes.COD, codWeight, 3, 6));
        commonSpawns(builder);
        builder.addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.DROWNED, 5, 1, 1));
    }

    public static void warmOceanSpawns(BiomeSettingsMobs.Builder builder, int squidWeight, int squidMinGroupSize) {
        builder.addSpawn(EnumCreatureType.WATER_CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.SQUID, squidWeight, squidMinGroupSize, 4));
        builder.addSpawn(EnumCreatureType.WATER_AMBIENT, new BiomeSettingsMobs.SpawnerData(EntityTypes.TROPICAL_FISH, 25, 8, 8));
        builder.addSpawn(EnumCreatureType.WATER_CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.DOLPHIN, 2, 1, 2));
        builder.addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.DROWNED, 5, 1, 1));
        commonSpawns(builder);
    }

    public static void plainsSpawns(BiomeSettingsMobs.Builder builder) {
        farmAnimals(builder);
        builder.addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.HORSE, 5, 2, 6));
        builder.addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.DONKEY, 1, 1, 3));
        commonSpawns(builder);
    }

    public static void snowySpawns(BiomeSettingsMobs.Builder builder) {
        builder.addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.RABBIT, 10, 2, 3));
        builder.addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.POLAR_BEAR, 1, 1, 2));
        caveSpawns(builder);
        monsters(builder, 95, 5, 20, false);
        builder.addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.STRAY, 80, 4, 4));
    }

    public static void desertSpawns(BiomeSettingsMobs.Builder builder) {
        builder.addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.RABBIT, 4, 2, 3));
        caveSpawns(builder);
        monsters(builder, 19, 1, 100, false);
        builder.addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.HUSK, 80, 4, 4));
    }

    public static void dripstoneCavesSpawns(BiomeSettingsMobs.Builder builder) {
        caveSpawns(builder);
        int i = 95;
        monsters(builder, 95, 5, 100, false);
        builder.addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.DROWNED, 95, 4, 4));
    }

    public static void monsters(BiomeSettingsMobs.Builder builder, int zombieWeight, int zombieVillagerWeight, int skeletonWeight, boolean drowned) {
        builder.addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.SPIDER, 100, 4, 4));
        builder.addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(drowned ? EntityTypes.DROWNED : EntityTypes.ZOMBIE, zombieWeight, 4, 4));
        builder.addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.ZOMBIE_VILLAGER, zombieVillagerWeight, 1, 1));
        builder.addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.SKELETON, skeletonWeight, 4, 4));
        builder.addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.CREEPER, 100, 4, 4));
        builder.addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.SLIME, 100, 4, 4));
        builder.addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.ENDERMAN, 10, 1, 4));
        builder.addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.WITCH, 5, 1, 1));
    }

    public static void mooshroomSpawns(BiomeSettingsMobs.Builder builder) {
        builder.addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.MOOSHROOM, 8, 4, 8));
        caveSpawns(builder);
    }

    public static void baseJungleSpawns(BiomeSettingsMobs.Builder builder) {
        farmAnimals(builder);
        builder.addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.CHICKEN, 10, 4, 4));
        commonSpawns(builder);
    }

    public static void endSpawns(BiomeSettingsMobs.Builder builder) {
        builder.addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.ENDERMAN, 10, 4, 4));
    }
}
