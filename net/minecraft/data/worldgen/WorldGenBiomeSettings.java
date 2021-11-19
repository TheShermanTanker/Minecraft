package net.minecraft.data.worldgen;

import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumCreatureType;
import net.minecraft.world.level.biome.BiomeSettingsGeneration;
import net.minecraft.world.level.biome.BiomeSettingsMobs;
import net.minecraft.world.level.levelgen.WorldGenStage;

public class WorldGenBiomeSettings {
    public static void addDefaultOverworldLandMesaStructures(BiomeSettingsGeneration.Builder builder) {
        builder.addStructureStart(WorldGenStructureFeatures.MINESHAFT_MESA);
        builder.addStructureStart(WorldGenStructureFeatures.STRONGHOLD);
    }

    public static void addDefaultOverworldLandStructures(BiomeSettingsGeneration.Builder builder) {
        builder.addStructureStart(WorldGenStructureFeatures.MINESHAFT);
        builder.addStructureStart(WorldGenStructureFeatures.STRONGHOLD);
    }

    public static void addDefaultOverworldOceanStructures(BiomeSettingsGeneration.Builder builder) {
        builder.addStructureStart(WorldGenStructureFeatures.MINESHAFT);
        builder.addStructureStart(WorldGenStructureFeatures.SHIPWRECK);
    }

    public static void addDefaultCarvers(BiomeSettingsGeneration.Builder builder) {
        builder.addCarver(WorldGenStage.Features.AIR, WorldGenCarvers.CAVE);
        builder.addCarver(WorldGenStage.Features.AIR, WorldGenCarvers.CANYON);
    }

    public static void addOceanCarvers(BiomeSettingsGeneration.Builder builder) {
        builder.addCarver(WorldGenStage.Features.AIR, WorldGenCarvers.OCEAN_CAVE);
        builder.addCarver(WorldGenStage.Features.AIR, WorldGenCarvers.CANYON);
        builder.addCarver(WorldGenStage.Features.LIQUID, WorldGenCarvers.UNDERWATER_CANYON);
        builder.addCarver(WorldGenStage.Features.LIQUID, WorldGenCarvers.UNDERWATER_CAVE);
    }

    public static void addDefaultLakes(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.LAKES, WorldGenBiomeDecoratorGroups.LAKE_WATER);
        builder.addFeature(WorldGenStage.Decoration.LAKES, WorldGenBiomeDecoratorGroups.LAKE_LAVA);
    }

    public static void addDesertLakes(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.LAKES, WorldGenBiomeDecoratorGroups.LAKE_LAVA);
    }

    public static void addDefaultMonsterRoom(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_STRUCTURES, WorldGenBiomeDecoratorGroups.MONSTER_ROOM);
    }

    public static void addDefaultUndergroundVariety(BiomeSettingsGeneration.Builder builder) {
        addDefaultUndergroundVariety(builder, false);
    }

    public static void addDefaultUndergroundVariety(BiomeSettingsGeneration.Builder builder, boolean bl) {
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_ORES, WorldGenBiomeDecoratorGroups.ORE_DIRT);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_ORES, WorldGenBiomeDecoratorGroups.ORE_GRAVEL);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_ORES, WorldGenBiomeDecoratorGroups.ORE_GRANITE);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_ORES, WorldGenBiomeDecoratorGroups.ORE_DIORITE);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_ORES, WorldGenBiomeDecoratorGroups.ORE_ANDESITE);
        if (!bl) {
            builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.GLOW_LICHEN);
        }

        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_ORES, WorldGenBiomeDecoratorGroups.ORE_TUFF);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_ORES, WorldGenBiomeDecoratorGroups.ORE_DEEPSLATE);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, WorldGenBiomeDecoratorGroups.RARE_DRIPSTONE_CLUSTER_FEATURE);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, WorldGenBiomeDecoratorGroups.RARE_SMALL_DRIPSTONE_FEATURE);
    }

    public static void addDripstone(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.LOCAL_MODIFICATIONS, WorldGenBiomeDecoratorGroups.LARGE_DRIPSTONE_FEATURE);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, WorldGenBiomeDecoratorGroups.DRIPSTONE_CLUSTER_FEATURE);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, WorldGenBiomeDecoratorGroups.SMALL_DRIPSTONE_FEATURE);
    }

    public static void addDefaultOres(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_ORES, WorldGenBiomeDecoratorGroups.ORE_COAL);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_ORES, WorldGenBiomeDecoratorGroups.ORE_IRON);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_ORES, WorldGenBiomeDecoratorGroups.ORE_GOLD);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_ORES, WorldGenBiomeDecoratorGroups.ORE_REDSTONE);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_ORES, WorldGenBiomeDecoratorGroups.ORE_DIAMOND);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_ORES, WorldGenBiomeDecoratorGroups.ORE_LAPIS);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_ORES, WorldGenBiomeDecoratorGroups.ORE_COPPER);
    }

    public static void addExtraGold(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_ORES, WorldGenBiomeDecoratorGroups.ORE_GOLD_EXTRA);
    }

    public static void addExtraEmeralds(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_ORES, WorldGenBiomeDecoratorGroups.ORE_EMERALD);
    }

    public static void addInfestedStone(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, WorldGenBiomeDecoratorGroups.ORE_INFESTED);
    }

    public static void addDefaultSoftDisks(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_ORES, WorldGenBiomeDecoratorGroups.DISK_SAND);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_ORES, WorldGenBiomeDecoratorGroups.DISK_CLAY);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_ORES, WorldGenBiomeDecoratorGroups.DISK_GRAVEL);
    }

    public static void addSwampClayDisk(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_ORES, WorldGenBiomeDecoratorGroups.DISK_CLAY);
    }

    public static void addMossyStoneBlock(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.LOCAL_MODIFICATIONS, WorldGenBiomeDecoratorGroups.FOREST_ROCK);
    }

    public static void addFerns(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.PATCH_LARGE_FERN);
    }

    public static void addBerryBushes(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.PATCH_BERRY_DECORATED);
    }

    public static void addSparseBerryBushes(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.PATCH_BERRY_SPARSE);
    }

    public static void addLightBambooVegetation(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.BAMBOO_LIGHT);
    }

    public static void addBambooVegetation(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.BAMBOO);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.BAMBOO_VEGETATION);
    }

    public static void addTaigaTrees(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.TAIGA_VEGETATION);
    }

    public static void addWaterTrees(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.TREES_WATER);
    }

    public static void addBirchTrees(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.TREES_BIRCH);
    }

    public static void addOtherBirchTrees(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.BIRCH_OTHER);
    }

    public static void addTallBirchTrees(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.BIRCH_TALL);
    }

    public static void addSavannaTrees(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.TREES_SAVANNA);
    }

    public static void addShatteredSavannaTrees(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.TREES_SHATTERED_SAVANNA);
    }

    public static void addLushCavesVegetationFeatures(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.LUSH_CAVES_CEILING_VEGETATION);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.CAVE_VINES);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.LUSH_CAVES_CLAY);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.LUSH_CAVES_VEGETATION);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.ROOTED_AZALEA_TREES);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.SPORE_BLOSSOM_FEATURE);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.CLASSIC_VINES_CAVE_FEATURE);
    }

    public static void addLushCavesSpecialOres(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_ORES, WorldGenBiomeDecoratorGroups.ORE_CLAY);
    }

    public static void addMountainTrees(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.TREES_MOUNTAIN);
    }

    public static void addMountainEdgeTrees(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.TREES_MOUNTAIN_EDGE);
    }

    public static void addJungleTrees(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.TREES_JUNGLE);
    }

    public static void addJungleEdgeTrees(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.TREES_JUNGLE_EDGE);
    }

    public static void addBadlandsTrees(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.TREES_BADLANDS);
    }

    public static void addSnowyTrees(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.TREES_SNOWY);
    }

    public static void addJungleGrass(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.PATCH_GRASS_JUNGLE);
    }

    public static void addSavannaGrass(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.PATCH_TALL_GRASS);
    }

    public static void addShatteredSavannaGrass(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.PATCH_GRASS_NORMAL);
    }

    public static void addSavannaExtraGrass(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.PATCH_GRASS_SAVANNA);
    }

    public static void addBadlandGrass(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.PATCH_GRASS_BADLANDS);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.PATCH_DEAD_BUSH_BADLANDS);
    }

    public static void addForestFlowers(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.FOREST_FLOWER_VEGETATION);
    }

    public static void addForestGrass(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.PATCH_GRASS_FOREST);
    }

    public static void addSwampVegetation(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.TREES_SWAMP);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.FLOWER_SWAMP);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.PATCH_GRASS_NORMAL);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.PATCH_DEAD_BUSH);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.PATCH_WATERLILLY);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.BROWN_MUSHROOM_SWAMP);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.RED_MUSHROOM_SWAMP);
    }

    public static void addMushroomFieldVegetation(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.MUSHROOM_FIELD_VEGETATION);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.BROWN_MUSHROOM_TAIGA);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.RED_MUSHROOM_TAIGA);
    }

    public static void addPlainVegetation(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.PLAIN_VEGETATION);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.FLOWER_PLAIN_DECORATED);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.PATCH_GRASS_PLAIN);
    }

    public static void addDesertVegetation(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.PATCH_DEAD_BUSH_2);
    }

    public static void addGiantTaigaVegetation(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.PATCH_GRASS_TAIGA);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.PATCH_DEAD_BUSH);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.BROWN_MUSHROOM_GIANT);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.RED_MUSHROOM_GIANT);
    }

    public static void addDefaultFlowers(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.FLOWER_DEFAULT);
    }

    public static void addWarmFlowers(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.FLOWER_WARM);
    }

    public static void addDefaultGrass(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.PATCH_GRASS_BADLANDS);
    }

    public static void addTaigaGrass(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.PATCH_GRASS_TAIGA_2);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.BROWN_MUSHROOM_TAIGA);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.RED_MUSHROOM_TAIGA);
    }

    public static void addPlainGrass(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.PATCH_TALL_GRASS_2);
    }

    public static void addDefaultMushrooms(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.BROWN_MUSHROOM_NORMAL);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.RED_MUSHROOM_NORMAL);
    }

    public static void addDefaultExtraVegetation(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.PATCH_SUGAR_CANE);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.PATCH_PUMPKIN);
    }

    public static void addBadlandExtraVegetation(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.PATCH_SUGAR_CANE_BADLANDS);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.PATCH_PUMPKIN);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.PATCH_CACTUS_DECORATED);
    }

    public static void addJungleExtraVegetation(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.PATCH_MELON);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.VINES);
    }

    public static void addDesertExtraVegetation(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.PATCH_SUGAR_CANE_DESERT);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.PATCH_PUMPKIN);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.PATCH_CACTUS_DESERT);
    }

    public static void addSwampExtraVegetation(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.PATCH_SUGAR_CANE_SWAMP);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.PATCH_PUMPKIN);
    }

    public static void addDesertExtraDecoration(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.SURFACE_STRUCTURES, WorldGenBiomeDecoratorGroups.WELL);
    }

    public static void addFossilDecoration(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_STRUCTURES, WorldGenBiomeDecoratorGroups.FOSSIL);
    }

    public static void addColdOceanExtraVegetation(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.KELP_COLD);
    }

    public static void addDefaultSeagrass(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.SEAGRASS_SIMPLE);
    }

    public static void addLukeWarmKelp(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.KELP_WARM);
    }

    public static void addDefaultSprings(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.SPRING_WATER);
        builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.SPRING_LAVA);
    }

    public static void addIcebergs(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.LOCAL_MODIFICATIONS, WorldGenBiomeDecoratorGroups.ICEBERG_PACKED);
        builder.addFeature(WorldGenStage.Decoration.LOCAL_MODIFICATIONS, WorldGenBiomeDecoratorGroups.ICEBERG_BLUE);
    }

    public static void addBlueIce(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.SURFACE_STRUCTURES, WorldGenBiomeDecoratorGroups.BLUE_ICE);
    }

    public static void addSurfaceFreezing(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.TOP_LAYER_MODIFICATION, WorldGenBiomeDecoratorGroups.FREEZE_TOP_LAYER);
    }

    public static void addNetherDefaultOres(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, WorldGenBiomeDecoratorGroups.ORE_GRAVEL_NETHER);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, WorldGenBiomeDecoratorGroups.ORE_BLACKSTONE);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, WorldGenBiomeDecoratorGroups.ORE_GOLD_NETHER);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, WorldGenBiomeDecoratorGroups.ORE_QUARTZ_NETHER);
        addAncientDebris(builder);
    }

    public static void addAncientDebris(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, WorldGenBiomeDecoratorGroups.ORE_DEBRIS_LARGE);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, WorldGenBiomeDecoratorGroups.ORE_DEBRIS_SMALL);
    }

    public static void addDefaultCrystalFormations(BiomeSettingsGeneration.Builder builder) {
        builder.addFeature(WorldGenStage.Decoration.LOCAL_MODIFICATIONS, WorldGenBiomeDecoratorGroups.AMETHYST_GEODE);
    }

    public static void farmAnimals(BiomeSettingsMobs.Builder builder) {
        builder.addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.SHEEP, 12, 4, 4));
        builder.addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.PIG, 10, 4, 4));
        builder.addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.CHICKEN, 10, 4, 4));
        builder.addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.COW, 8, 4, 4));
    }

    public static void caveSpawns(BiomeSettingsMobs.Builder builder) {
        builder.addSpawn(EnumCreatureType.AMBIENT, new BiomeSettingsMobs.SpawnerData(EntityTypes.BAT, 10, 8, 8));
        caveWaterSpawns(builder);
    }

    public static void commonSpawns(BiomeSettingsMobs.Builder builder) {
        caveSpawns(builder);
        monsters(builder, 95, 5, 100);
    }

    public static void caveWaterSpawns(BiomeSettingsMobs.Builder builder) {
        builder.addSpawn(EnumCreatureType.UNDERGROUND_WATER_CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.GLOW_SQUID, 10, 4, 6));
        builder.addSpawn(EnumCreatureType.UNDERGROUND_WATER_CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.AXOLOTL, 10, 4, 6));
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
        monsters(builder, 95, 5, 20);
        builder.addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.STRAY, 80, 4, 4));
    }

    public static void desertSpawns(BiomeSettingsMobs.Builder builder) {
        builder.addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.RABBIT, 4, 2, 3));
        caveSpawns(builder);
        monsters(builder, 19, 1, 100);
        builder.addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.HUSK, 80, 4, 4));
    }

    public static void monsters(BiomeSettingsMobs.Builder builder, int zombieWeight, int zombieVillagerWeight, int skeletonWeight) {
        builder.addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.SPIDER, 100, 4, 4));
        builder.addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.ZOMBIE, zombieWeight, 4, 4));
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
