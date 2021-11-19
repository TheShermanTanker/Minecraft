package net.minecraft.data.worldgen.biome;

import net.minecraft.core.particles.Particles;
import net.minecraft.data.worldgen.WorldGenBiomeDecoratorGroups;
import net.minecraft.data.worldgen.WorldGenBiomeSettings;
import net.minecraft.data.worldgen.WorldGenCarvers;
import net.minecraft.data.worldgen.WorldGenStructureFeatures;
import net.minecraft.data.worldgen.WorldGenSurfaceComposites;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.sounds.SoundTracks;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumCreatureType;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.BiomeFog;
import net.minecraft.world.level.biome.BiomeParticles;
import net.minecraft.world.level.biome.BiomeSettingsGeneration;
import net.minecraft.world.level.biome.BiomeSettingsMobs;
import net.minecraft.world.level.biome.CaveSound;
import net.minecraft.world.level.biome.CaveSoundSettings;
import net.minecraft.world.level.levelgen.WorldGenStage;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.surfacebuilders.WorldGenSurfaceComposite;
import net.minecraft.world.level.levelgen.surfacebuilders.WorldGenSurfaceConfigurationBase;

public class WorldGenBiomeSettingsDefault {
    private static int calculateSkyColor(float temperature) {
        float f = temperature / 3.0F;
        f = MathHelper.clamp(f, -1.0F, 1.0F);
        return MathHelper.hsvToRgb(0.62222224F - f * 0.05F, 0.5F + f * 0.1F, 1.0F);
    }

    public static BiomeBase giantTreeTaiga(float depth, float scale, float temperature, boolean spruce) {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        WorldGenBiomeSettings.farmAnimals(builder);
        builder.addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.WOLF, 8, 4, 4));
        builder.addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.RABBIT, 4, 2, 3));
        builder.addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.FOX, 8, 2, 4));
        if (spruce) {
            WorldGenBiomeSettings.commonSpawns(builder);
        } else {
            WorldGenBiomeSettings.caveSpawns(builder);
            WorldGenBiomeSettings.monsters(builder, 100, 25, 100);
        }

        BiomeSettingsGeneration.Builder builder2 = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(WorldGenSurfaceComposites.GIANT_TREE_TAIGA);
        WorldGenBiomeSettings.addDefaultOverworldLandStructures(builder2);
        builder2.addStructureStart(WorldGenStructureFeatures.RUINED_PORTAL_STANDARD);
        WorldGenBiomeSettings.addDefaultCarvers(builder2);
        WorldGenBiomeSettings.addDefaultLakes(builder2);
        WorldGenBiomeSettings.addDefaultCrystalFormations(builder2);
        WorldGenBiomeSettings.addDefaultMonsterRoom(builder2);
        WorldGenBiomeSettings.addMossyStoneBlock(builder2);
        WorldGenBiomeSettings.addFerns(builder2);
        WorldGenBiomeSettings.addDefaultUndergroundVariety(builder2);
        WorldGenBiomeSettings.addDefaultOres(builder2);
        WorldGenBiomeSettings.addDefaultSoftDisks(builder2);
        builder2.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, spruce ? WorldGenBiomeDecoratorGroups.TREES_GIANT_SPRUCE : WorldGenBiomeDecoratorGroups.TREES_GIANT);
        WorldGenBiomeSettings.addDefaultFlowers(builder2);
        WorldGenBiomeSettings.addGiantTaigaVegetation(builder2);
        WorldGenBiomeSettings.addDefaultMushrooms(builder2);
        WorldGenBiomeSettings.addDefaultExtraVegetation(builder2);
        WorldGenBiomeSettings.addDefaultSprings(builder2);
        WorldGenBiomeSettings.addSparseBerryBushes(builder2);
        WorldGenBiomeSettings.addSurfaceFreezing(builder2);
        return (new BiomeBase.BiomeBuilder()).precipitation(BiomeBase.Precipitation.RAIN).biomeCategory(BiomeBase.Geography.TAIGA).depth(depth).scale(scale).temperature(temperature).downfall(0.8F).specialEffects((new BiomeFog.Builder()).waterColor(4159204).waterFogColor(329011).fogColor(12638463).skyColor(calculateSkyColor(temperature)).ambientMoodSound(CaveSoundSettings.LEGACY_CAVE_SETTINGS).build()).mobSpawnSettings(builder.build()).generationSettings(builder2.build()).build();
    }

    public static BiomeBase birchForestBiome(float depth, float scale, boolean tallTrees) {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        WorldGenBiomeSettings.farmAnimals(builder);
        WorldGenBiomeSettings.commonSpawns(builder);
        BiomeSettingsGeneration.Builder builder2 = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(WorldGenSurfaceComposites.GRASS);
        WorldGenBiomeSettings.addDefaultOverworldLandStructures(builder2);
        builder2.addStructureStart(WorldGenStructureFeatures.RUINED_PORTAL_STANDARD);
        WorldGenBiomeSettings.addDefaultCarvers(builder2);
        WorldGenBiomeSettings.addDefaultLakes(builder2);
        WorldGenBiomeSettings.addDefaultCrystalFormations(builder2);
        WorldGenBiomeSettings.addDefaultMonsterRoom(builder2);
        WorldGenBiomeSettings.addForestFlowers(builder2);
        WorldGenBiomeSettings.addDefaultUndergroundVariety(builder2);
        WorldGenBiomeSettings.addDefaultOres(builder2);
        WorldGenBiomeSettings.addDefaultSoftDisks(builder2);
        if (tallTrees) {
            WorldGenBiomeSettings.addTallBirchTrees(builder2);
        } else {
            WorldGenBiomeSettings.addBirchTrees(builder2);
        }

        WorldGenBiomeSettings.addDefaultFlowers(builder2);
        WorldGenBiomeSettings.addForestGrass(builder2);
        WorldGenBiomeSettings.addDefaultMushrooms(builder2);
        WorldGenBiomeSettings.addDefaultExtraVegetation(builder2);
        WorldGenBiomeSettings.addDefaultSprings(builder2);
        WorldGenBiomeSettings.addSurfaceFreezing(builder2);
        return (new BiomeBase.BiomeBuilder()).precipitation(BiomeBase.Precipitation.RAIN).biomeCategory(BiomeBase.Geography.FOREST).depth(depth).scale(scale).temperature(0.6F).downfall(0.6F).specialEffects((new BiomeFog.Builder()).waterColor(4159204).waterFogColor(329011).fogColor(12638463).skyColor(calculateSkyColor(0.6F)).ambientMoodSound(CaveSoundSettings.LEGACY_CAVE_SETTINGS).build()).mobSpawnSettings(builder.build()).generationSettings(builder2.build()).build();
    }

    public static BiomeBase jungleBiome() {
        return jungleBiome(0.1F, 0.2F, 40, 2, 3);
    }

    public static BiomeBase jungleEdgeBiome() {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        WorldGenBiomeSettings.baseJungleSpawns(builder);
        return baseJungleBiome(0.1F, 0.2F, 0.8F, false, true, false, builder);
    }

    public static BiomeBase modifiedJungleEdgeBiome() {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        WorldGenBiomeSettings.baseJungleSpawns(builder);
        return baseJungleBiome(0.2F, 0.4F, 0.8F, false, true, true, builder);
    }

    public static BiomeBase modifiedJungleBiome() {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        WorldGenBiomeSettings.baseJungleSpawns(builder);
        builder.addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.PARROT, 10, 1, 1)).addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.OCELOT, 2, 1, 1));
        return baseJungleBiome(0.2F, 0.4F, 0.9F, false, false, true, builder);
    }

    public static BiomeBase jungleHillsBiome() {
        return jungleBiome(0.45F, 0.3F, 10, 1, 1);
    }

    public static BiomeBase bambooJungleBiome() {
        return bambooJungleBiome(0.1F, 0.2F, 40, 2);
    }

    public static BiomeBase bambooJungleHillsBiome() {
        return bambooJungleBiome(0.45F, 0.3F, 10, 1);
    }

    private static BiomeBase jungleBiome(float depth, float scale, int parrotWeight, int parrotMaxGroupSize, int ocelotMaxGroupSize) {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        WorldGenBiomeSettings.baseJungleSpawns(builder);
        builder.addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.PARROT, parrotWeight, 1, parrotMaxGroupSize)).addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.OCELOT, 2, 1, ocelotMaxGroupSize)).addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.PANDA, 1, 1, 2));
        builder.setPlayerCanSpawn();
        return baseJungleBiome(depth, scale, 0.9F, false, false, false, builder);
    }

    private static BiomeBase bambooJungleBiome(float depth, float scale, int parrotWeight, int parrotMaxGroupSize) {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        WorldGenBiomeSettings.baseJungleSpawns(builder);
        builder.addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.PARROT, parrotWeight, 1, parrotMaxGroupSize)).addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.PANDA, 80, 1, 2)).addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.OCELOT, 2, 1, 1));
        return baseJungleBiome(depth, scale, 0.9F, true, false, false, builder);
    }

    private static BiomeBase baseJungleBiome(float depth, float scale, float downfall, boolean bamboo, boolean edge, boolean modified, BiomeSettingsMobs.Builder builder) {
        BiomeSettingsGeneration.Builder builder2 = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(WorldGenSurfaceComposites.GRASS);
        if (!edge && !modified) {
            builder2.addStructureStart(WorldGenStructureFeatures.JUNGLE_TEMPLE);
        }

        WorldGenBiomeSettings.addDefaultOverworldLandStructures(builder2);
        builder2.addStructureStart(WorldGenStructureFeatures.RUINED_PORTAL_JUNGLE);
        WorldGenBiomeSettings.addDefaultCarvers(builder2);
        WorldGenBiomeSettings.addDefaultLakes(builder2);
        WorldGenBiomeSettings.addDefaultCrystalFormations(builder2);
        WorldGenBiomeSettings.addDefaultMonsterRoom(builder2);
        WorldGenBiomeSettings.addDefaultUndergroundVariety(builder2);
        WorldGenBiomeSettings.addDefaultOres(builder2);
        WorldGenBiomeSettings.addDefaultSoftDisks(builder2);
        if (bamboo) {
            WorldGenBiomeSettings.addBambooVegetation(builder2);
        } else {
            if (!edge && !modified) {
                WorldGenBiomeSettings.addLightBambooVegetation(builder2);
            }

            if (edge) {
                WorldGenBiomeSettings.addJungleEdgeTrees(builder2);
            } else {
                WorldGenBiomeSettings.addJungleTrees(builder2);
            }
        }

        WorldGenBiomeSettings.addWarmFlowers(builder2);
        WorldGenBiomeSettings.addJungleGrass(builder2);
        WorldGenBiomeSettings.addDefaultMushrooms(builder2);
        WorldGenBiomeSettings.addDefaultExtraVegetation(builder2);
        WorldGenBiomeSettings.addDefaultSprings(builder2);
        WorldGenBiomeSettings.addJungleExtraVegetation(builder2);
        WorldGenBiomeSettings.addSurfaceFreezing(builder2);
        return (new BiomeBase.BiomeBuilder()).precipitation(BiomeBase.Precipitation.RAIN).biomeCategory(BiomeBase.Geography.JUNGLE).depth(depth).scale(scale).temperature(0.95F).downfall(downfall).specialEffects((new BiomeFog.Builder()).waterColor(4159204).waterFogColor(329011).fogColor(12638463).skyColor(calculateSkyColor(0.95F)).ambientMoodSound(CaveSoundSettings.LEGACY_CAVE_SETTINGS).build()).mobSpawnSettings(builder.build()).generationSettings(builder2.build()).build();
    }

    public static BiomeBase mountainBiome(float depth, float scale, WorldGenSurfaceComposite<WorldGenSurfaceConfigurationBase> surfaceBuilder, boolean extraTrees) {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        WorldGenBiomeSettings.farmAnimals(builder);
        builder.addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.LLAMA, 5, 4, 6));
        builder.addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.GOAT, 10, 4, 6));
        WorldGenBiomeSettings.commonSpawns(builder);
        BiomeSettingsGeneration.Builder builder2 = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(surfaceBuilder);
        WorldGenBiomeSettings.addDefaultOverworldLandStructures(builder2);
        builder2.addStructureStart(WorldGenStructureFeatures.RUINED_PORTAL_MOUNTAIN);
        WorldGenBiomeSettings.addDefaultCarvers(builder2);
        WorldGenBiomeSettings.addDefaultLakes(builder2);
        WorldGenBiomeSettings.addDefaultCrystalFormations(builder2);
        WorldGenBiomeSettings.addDefaultMonsterRoom(builder2);
        WorldGenBiomeSettings.addDefaultUndergroundVariety(builder2);
        WorldGenBiomeSettings.addDefaultOres(builder2);
        WorldGenBiomeSettings.addDefaultSoftDisks(builder2);
        if (extraTrees) {
            WorldGenBiomeSettings.addMountainEdgeTrees(builder2);
        } else {
            WorldGenBiomeSettings.addMountainTrees(builder2);
        }

        WorldGenBiomeSettings.addDefaultFlowers(builder2);
        WorldGenBiomeSettings.addDefaultGrass(builder2);
        WorldGenBiomeSettings.addDefaultMushrooms(builder2);
        WorldGenBiomeSettings.addDefaultExtraVegetation(builder2);
        WorldGenBiomeSettings.addDefaultSprings(builder2);
        WorldGenBiomeSettings.addExtraEmeralds(builder2);
        WorldGenBiomeSettings.addInfestedStone(builder2);
        WorldGenBiomeSettings.addSurfaceFreezing(builder2);
        return (new BiomeBase.BiomeBuilder()).precipitation(BiomeBase.Precipitation.RAIN).biomeCategory(BiomeBase.Geography.EXTREME_HILLS).depth(depth).scale(scale).temperature(0.2F).downfall(0.3F).specialEffects((new BiomeFog.Builder()).waterColor(4159204).waterFogColor(329011).fogColor(12638463).skyColor(calculateSkyColor(0.2F)).ambientMoodSound(CaveSoundSettings.LEGACY_CAVE_SETTINGS).build()).mobSpawnSettings(builder.build()).generationSettings(builder2.build()).build();
    }

    public static BiomeBase desertBiome(float depth, float scale, boolean villages, boolean pyramids, boolean fossils) {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        WorldGenBiomeSettings.desertSpawns(builder);
        BiomeSettingsGeneration.Builder builder2 = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(WorldGenSurfaceComposites.DESERT);
        if (villages) {
            builder2.addStructureStart(WorldGenStructureFeatures.VILLAGE_DESERT);
            builder2.addStructureStart(WorldGenStructureFeatures.PILLAGER_OUTPOST);
        }

        if (pyramids) {
            builder2.addStructureStart(WorldGenStructureFeatures.DESERT_PYRAMID);
        }

        if (fossils) {
            WorldGenBiomeSettings.addFossilDecoration(builder2);
        }

        WorldGenBiomeSettings.addDefaultOverworldLandStructures(builder2);
        builder2.addStructureStart(WorldGenStructureFeatures.RUINED_PORTAL_DESERT);
        WorldGenBiomeSettings.addDefaultCarvers(builder2);
        WorldGenBiomeSettings.addDesertLakes(builder2);
        WorldGenBiomeSettings.addDefaultCrystalFormations(builder2);
        WorldGenBiomeSettings.addDefaultMonsterRoom(builder2);
        WorldGenBiomeSettings.addDefaultUndergroundVariety(builder2);
        WorldGenBiomeSettings.addDefaultOres(builder2);
        WorldGenBiomeSettings.addDefaultSoftDisks(builder2);
        WorldGenBiomeSettings.addDefaultFlowers(builder2);
        WorldGenBiomeSettings.addDefaultGrass(builder2);
        WorldGenBiomeSettings.addDesertVegetation(builder2);
        WorldGenBiomeSettings.addDefaultMushrooms(builder2);
        WorldGenBiomeSettings.addDesertExtraVegetation(builder2);
        WorldGenBiomeSettings.addDefaultSprings(builder2);
        WorldGenBiomeSettings.addDesertExtraDecoration(builder2);
        WorldGenBiomeSettings.addSurfaceFreezing(builder2);
        return (new BiomeBase.BiomeBuilder()).precipitation(BiomeBase.Precipitation.NONE).biomeCategory(BiomeBase.Geography.DESERT).depth(depth).scale(scale).temperature(2.0F).downfall(0.0F).specialEffects((new BiomeFog.Builder()).waterColor(4159204).waterFogColor(329011).fogColor(12638463).skyColor(calculateSkyColor(2.0F)).ambientMoodSound(CaveSoundSettings.LEGACY_CAVE_SETTINGS).build()).mobSpawnSettings(builder.build()).generationSettings(builder2.build()).build();
    }

    public static BiomeBase plainsBiome(boolean sunflower) {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        WorldGenBiomeSettings.plainsSpawns(builder);
        if (!sunflower) {
            builder.setPlayerCanSpawn();
        }

        BiomeSettingsGeneration.Builder builder2 = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(WorldGenSurfaceComposites.GRASS);
        if (!sunflower) {
            builder2.addStructureStart(WorldGenStructureFeatures.VILLAGE_PLAINS).addStructureStart(WorldGenStructureFeatures.PILLAGER_OUTPOST);
        }

        WorldGenBiomeSettings.addDefaultOverworldLandStructures(builder2);
        builder2.addStructureStart(WorldGenStructureFeatures.RUINED_PORTAL_STANDARD);
        WorldGenBiomeSettings.addDefaultCarvers(builder2);
        WorldGenBiomeSettings.addDefaultLakes(builder2);
        WorldGenBiomeSettings.addDefaultCrystalFormations(builder2);
        WorldGenBiomeSettings.addDefaultMonsterRoom(builder2);
        WorldGenBiomeSettings.addPlainGrass(builder2);
        if (sunflower) {
            builder2.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.PATCH_SUNFLOWER);
        }

        WorldGenBiomeSettings.addDefaultUndergroundVariety(builder2);
        WorldGenBiomeSettings.addDefaultOres(builder2);
        WorldGenBiomeSettings.addDefaultSoftDisks(builder2);
        WorldGenBiomeSettings.addPlainVegetation(builder2);
        if (sunflower) {
            builder2.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.PATCH_SUGAR_CANE);
        }

        WorldGenBiomeSettings.addDefaultMushrooms(builder2);
        if (sunflower) {
            builder2.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.PATCH_PUMPKIN);
        } else {
            WorldGenBiomeSettings.addDefaultExtraVegetation(builder2);
        }

        WorldGenBiomeSettings.addDefaultSprings(builder2);
        WorldGenBiomeSettings.addSurfaceFreezing(builder2);
        return (new BiomeBase.BiomeBuilder()).precipitation(BiomeBase.Precipitation.RAIN).biomeCategory(BiomeBase.Geography.PLAINS).depth(0.125F).scale(0.05F).temperature(0.8F).downfall(0.4F).specialEffects((new BiomeFog.Builder()).waterColor(4159204).waterFogColor(329011).fogColor(12638463).skyColor(calculateSkyColor(0.8F)).ambientMoodSound(CaveSoundSettings.LEGACY_CAVE_SETTINGS).build()).mobSpawnSettings(builder.build()).generationSettings(builder2.build()).build();
    }

    private static BiomeBase baseEndBiome(BiomeSettingsGeneration.Builder builder) {
        BiomeSettingsMobs.Builder builder2 = new BiomeSettingsMobs.Builder();
        WorldGenBiomeSettings.endSpawns(builder2);
        return (new BiomeBase.BiomeBuilder()).precipitation(BiomeBase.Precipitation.NONE).biomeCategory(BiomeBase.Geography.THEEND).depth(0.1F).scale(0.2F).temperature(0.5F).downfall(0.5F).specialEffects((new BiomeFog.Builder()).waterColor(4159204).waterFogColor(329011).fogColor(10518688).skyColor(0).ambientMoodSound(CaveSoundSettings.LEGACY_CAVE_SETTINGS).build()).mobSpawnSettings(builder2.build()).generationSettings(builder.build()).build();
    }

    public static BiomeBase endBarrensBiome() {
        BiomeSettingsGeneration.Builder builder = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(WorldGenSurfaceComposites.END);
        return baseEndBiome(builder);
    }

    public static BiomeBase theEndBiome() {
        BiomeSettingsGeneration.Builder builder = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(WorldGenSurfaceComposites.END).addFeature(WorldGenStage.Decoration.SURFACE_STRUCTURES, WorldGenBiomeDecoratorGroups.END_SPIKE);
        return baseEndBiome(builder);
    }

    public static BiomeBase endMidlandsBiome() {
        BiomeSettingsGeneration.Builder builder = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(WorldGenSurfaceComposites.END).addStructureStart(WorldGenStructureFeatures.END_CITY);
        return baseEndBiome(builder);
    }

    public static BiomeBase endHighlandsBiome() {
        BiomeSettingsGeneration.Builder builder = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(WorldGenSurfaceComposites.END).addStructureStart(WorldGenStructureFeatures.END_CITY).addFeature(WorldGenStage.Decoration.SURFACE_STRUCTURES, WorldGenBiomeDecoratorGroups.END_GATEWAY).addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.CHORUS_PLANT);
        return baseEndBiome(builder);
    }

    public static BiomeBase smallEndIslandsBiome() {
        BiomeSettingsGeneration.Builder builder = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(WorldGenSurfaceComposites.END).addFeature(WorldGenStage.Decoration.RAW_GENERATION, WorldGenBiomeDecoratorGroups.END_ISLAND_DECORATED);
        return baseEndBiome(builder);
    }

    public static BiomeBase mushroomFieldsBiome(float depth, float scale) {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        WorldGenBiomeSettings.mooshroomSpawns(builder);
        BiomeSettingsGeneration.Builder builder2 = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(WorldGenSurfaceComposites.MYCELIUM);
        WorldGenBiomeSettings.addDefaultOverworldLandStructures(builder2);
        builder2.addStructureStart(WorldGenStructureFeatures.RUINED_PORTAL_STANDARD);
        WorldGenBiomeSettings.addDefaultCarvers(builder2);
        WorldGenBiomeSettings.addDefaultLakes(builder2);
        WorldGenBiomeSettings.addDefaultCrystalFormations(builder2);
        WorldGenBiomeSettings.addDefaultMonsterRoom(builder2);
        WorldGenBiomeSettings.addDefaultUndergroundVariety(builder2);
        WorldGenBiomeSettings.addDefaultOres(builder2);
        WorldGenBiomeSettings.addDefaultSoftDisks(builder2);
        WorldGenBiomeSettings.addMushroomFieldVegetation(builder2);
        WorldGenBiomeSettings.addDefaultMushrooms(builder2);
        WorldGenBiomeSettings.addDefaultExtraVegetation(builder2);
        WorldGenBiomeSettings.addDefaultSprings(builder2);
        WorldGenBiomeSettings.addSurfaceFreezing(builder2);
        return (new BiomeBase.BiomeBuilder()).precipitation(BiomeBase.Precipitation.RAIN).biomeCategory(BiomeBase.Geography.MUSHROOM).depth(depth).scale(scale).temperature(0.9F).downfall(1.0F).specialEffects((new BiomeFog.Builder()).waterColor(4159204).waterFogColor(329011).fogColor(12638463).skyColor(calculateSkyColor(0.9F)).ambientMoodSound(CaveSoundSettings.LEGACY_CAVE_SETTINGS).build()).mobSpawnSettings(builder.build()).generationSettings(builder2.build()).build();
    }

    private static BiomeBase baseSavannaBiome(float depth, float scale, float temperature, boolean plateau, boolean shattered, BiomeSettingsMobs.Builder spawnSettings) {
        BiomeSettingsGeneration.Builder builder = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(shattered ? WorldGenSurfaceComposites.SHATTERED_SAVANNA : WorldGenSurfaceComposites.GRASS);
        if (!plateau && !shattered) {
            builder.addStructureStart(WorldGenStructureFeatures.VILLAGE_SAVANNA).addStructureStart(WorldGenStructureFeatures.PILLAGER_OUTPOST);
        }

        WorldGenBiomeSettings.addDefaultOverworldLandStructures(builder);
        builder.addStructureStart(plateau ? WorldGenStructureFeatures.RUINED_PORTAL_MOUNTAIN : WorldGenStructureFeatures.RUINED_PORTAL_STANDARD);
        WorldGenBiomeSettings.addDefaultCarvers(builder);
        WorldGenBiomeSettings.addDefaultLakes(builder);
        WorldGenBiomeSettings.addDefaultCrystalFormations(builder);
        WorldGenBiomeSettings.addDefaultMonsterRoom(builder);
        if (!shattered) {
            WorldGenBiomeSettings.addSavannaGrass(builder);
        }

        WorldGenBiomeSettings.addDefaultUndergroundVariety(builder);
        WorldGenBiomeSettings.addDefaultOres(builder);
        WorldGenBiomeSettings.addDefaultSoftDisks(builder);
        if (shattered) {
            WorldGenBiomeSettings.addShatteredSavannaTrees(builder);
            WorldGenBiomeSettings.addDefaultFlowers(builder);
            WorldGenBiomeSettings.addShatteredSavannaGrass(builder);
        } else {
            WorldGenBiomeSettings.addSavannaTrees(builder);
            WorldGenBiomeSettings.addWarmFlowers(builder);
            WorldGenBiomeSettings.addSavannaExtraGrass(builder);
        }

        WorldGenBiomeSettings.addDefaultMushrooms(builder);
        WorldGenBiomeSettings.addDefaultExtraVegetation(builder);
        WorldGenBiomeSettings.addDefaultSprings(builder);
        WorldGenBiomeSettings.addSurfaceFreezing(builder);
        return (new BiomeBase.BiomeBuilder()).precipitation(BiomeBase.Precipitation.NONE).biomeCategory(BiomeBase.Geography.SAVANNA).depth(depth).scale(scale).temperature(temperature).downfall(0.0F).specialEffects((new BiomeFog.Builder()).waterColor(4159204).waterFogColor(329011).fogColor(12638463).skyColor(calculateSkyColor(temperature)).ambientMoodSound(CaveSoundSettings.LEGACY_CAVE_SETTINGS).build()).mobSpawnSettings(spawnSettings.build()).generationSettings(builder.build()).build();
    }

    public static BiomeBase savannaBiome(float depth, float scale, float temperature, boolean mountain, boolean shattered) {
        BiomeSettingsMobs.Builder builder = savannaMobs();
        return baseSavannaBiome(depth, scale, temperature, mountain, shattered, builder);
    }

    private static BiomeSettingsMobs.Builder savannaMobs() {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        WorldGenBiomeSettings.farmAnimals(builder);
        builder.addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.HORSE, 1, 2, 6)).addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.DONKEY, 1, 1, 1));
        WorldGenBiomeSettings.commonSpawns(builder);
        return builder;
    }

    public static BiomeBase savanaPlateauBiome() {
        BiomeSettingsMobs.Builder builder = savannaMobs();
        builder.addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.LLAMA, 8, 4, 4));
        return baseSavannaBiome(1.5F, 0.025F, 1.0F, true, false, builder);
    }

    private static BiomeBase baseBadlandsBiome(WorldGenSurfaceComposite<WorldGenSurfaceConfigurationBase> configuredSurfaceBuilder, float depth, float scale, boolean plateau, boolean wooded) {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        WorldGenBiomeSettings.commonSpawns(builder);
        BiomeSettingsGeneration.Builder builder2 = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(configuredSurfaceBuilder);
        WorldGenBiomeSettings.addDefaultOverworldLandMesaStructures(builder2);
        builder2.addStructureStart(plateau ? WorldGenStructureFeatures.RUINED_PORTAL_MOUNTAIN : WorldGenStructureFeatures.RUINED_PORTAL_STANDARD);
        WorldGenBiomeSettings.addDefaultCarvers(builder2);
        WorldGenBiomeSettings.addDefaultLakes(builder2);
        WorldGenBiomeSettings.addDefaultCrystalFormations(builder2);
        WorldGenBiomeSettings.addDefaultMonsterRoom(builder2);
        WorldGenBiomeSettings.addDefaultUndergroundVariety(builder2);
        WorldGenBiomeSettings.addDefaultOres(builder2);
        WorldGenBiomeSettings.addExtraGold(builder2);
        WorldGenBiomeSettings.addDefaultSoftDisks(builder2);
        if (wooded) {
            WorldGenBiomeSettings.addBadlandsTrees(builder2);
        }

        WorldGenBiomeSettings.addBadlandGrass(builder2);
        WorldGenBiomeSettings.addDefaultMushrooms(builder2);
        WorldGenBiomeSettings.addBadlandExtraVegetation(builder2);
        WorldGenBiomeSettings.addDefaultSprings(builder2);
        WorldGenBiomeSettings.addSurfaceFreezing(builder2);
        return (new BiomeBase.BiomeBuilder()).precipitation(BiomeBase.Precipitation.NONE).biomeCategory(BiomeBase.Geography.MESA).depth(depth).scale(scale).temperature(2.0F).downfall(0.0F).specialEffects((new BiomeFog.Builder()).waterColor(4159204).waterFogColor(329011).fogColor(12638463).skyColor(calculateSkyColor(2.0F)).foliageColorOverride(10387789).grassColorOverride(9470285).ambientMoodSound(CaveSoundSettings.LEGACY_CAVE_SETTINGS).build()).mobSpawnSettings(builder.build()).generationSettings(builder2.build()).build();
    }

    public static BiomeBase badlandsBiome(float depth, float scale, boolean plateau) {
        return baseBadlandsBiome(WorldGenSurfaceComposites.BADLANDS, depth, scale, plateau, false);
    }

    public static BiomeBase woodedBadlandsPlateauBiome(float depth, float scale) {
        return baseBadlandsBiome(WorldGenSurfaceComposites.WOODED_BADLANDS, depth, scale, true, true);
    }

    public static BiomeBase erodedBadlandsBiome() {
        return baseBadlandsBiome(WorldGenSurfaceComposites.ERODED_BADLANDS, 0.1F, 0.2F, true, false);
    }

    private static BiomeBase baseOceanBiome(BiomeSettingsMobs.Builder spawnSettings, int waterColor, int waterFogColor, boolean deep, BiomeSettingsGeneration.Builder builder) {
        return (new BiomeBase.BiomeBuilder()).precipitation(BiomeBase.Precipitation.RAIN).biomeCategory(BiomeBase.Geography.OCEAN).depth(deep ? -1.8F : -1.0F).scale(0.1F).temperature(0.5F).downfall(0.5F).specialEffects((new BiomeFog.Builder()).waterColor(waterColor).waterFogColor(waterFogColor).fogColor(12638463).skyColor(calculateSkyColor(0.5F)).ambientMoodSound(CaveSoundSettings.LEGACY_CAVE_SETTINGS).build()).mobSpawnSettings(spawnSettings.build()).generationSettings(builder.build()).build();
    }

    private static BiomeSettingsGeneration.Builder baseOceanGeneration(WorldGenSurfaceComposite<WorldGenSurfaceConfigurationBase> configuredSurfaceBuilder, boolean deep, boolean warm, boolean bl) {
        BiomeSettingsGeneration.Builder builder = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(configuredSurfaceBuilder);
        StructureFeature<?, ?> configuredStructureFeature = warm ? WorldGenStructureFeatures.OCEAN_RUIN_WARM : WorldGenStructureFeatures.OCEAN_RUIN_COLD;
        if (bl) {
            if (deep) {
                builder.addStructureStart(WorldGenStructureFeatures.OCEAN_MONUMENT);
            }

            WorldGenBiomeSettings.addDefaultOverworldOceanStructures(builder);
            builder.addStructureStart(configuredStructureFeature);
        } else {
            builder.addStructureStart(configuredStructureFeature);
            if (deep) {
                builder.addStructureStart(WorldGenStructureFeatures.OCEAN_MONUMENT);
            }

            WorldGenBiomeSettings.addDefaultOverworldOceanStructures(builder);
        }

        builder.addStructureStart(WorldGenStructureFeatures.RUINED_PORTAL_OCEAN);
        WorldGenBiomeSettings.addOceanCarvers(builder);
        WorldGenBiomeSettings.addDefaultLakes(builder);
        WorldGenBiomeSettings.addDefaultCrystalFormations(builder);
        WorldGenBiomeSettings.addDefaultMonsterRoom(builder);
        WorldGenBiomeSettings.addDefaultUndergroundVariety(builder, true);
        WorldGenBiomeSettings.addDefaultOres(builder);
        WorldGenBiomeSettings.addDefaultSoftDisks(builder);
        WorldGenBiomeSettings.addWaterTrees(builder);
        WorldGenBiomeSettings.addDefaultFlowers(builder);
        WorldGenBiomeSettings.addDefaultGrass(builder);
        WorldGenBiomeSettings.addDefaultMushrooms(builder);
        WorldGenBiomeSettings.addDefaultExtraVegetation(builder);
        WorldGenBiomeSettings.addDefaultSprings(builder);
        return builder;
    }

    public static BiomeBase coldOceanBiome(boolean deep) {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        WorldGenBiomeSettings.oceanSpawns(builder, 3, 4, 15);
        builder.addSpawn(EnumCreatureType.WATER_AMBIENT, new BiomeSettingsMobs.SpawnerData(EntityTypes.SALMON, 15, 1, 5));
        boolean bl = !deep;
        BiomeSettingsGeneration.Builder builder2 = baseOceanGeneration(WorldGenSurfaceComposites.GRASS, deep, false, bl);
        builder2.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, deep ? WorldGenBiomeDecoratorGroups.SEAGRASS_DEEP_COLD : WorldGenBiomeDecoratorGroups.SEAGRASS_COLD);
        WorldGenBiomeSettings.addDefaultSeagrass(builder2);
        WorldGenBiomeSettings.addColdOceanExtraVegetation(builder2);
        WorldGenBiomeSettings.addSurfaceFreezing(builder2);
        return baseOceanBiome(builder, 4020182, 329011, deep, builder2);
    }

    public static BiomeBase oceanBiome(boolean deep) {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        WorldGenBiomeSettings.oceanSpawns(builder, 1, 4, 10);
        builder.addSpawn(EnumCreatureType.WATER_CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.DOLPHIN, 1, 1, 2));
        BiomeSettingsGeneration.Builder builder2 = baseOceanGeneration(WorldGenSurfaceComposites.GRASS, deep, false, true);
        builder2.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, deep ? WorldGenBiomeDecoratorGroups.SEAGRASS_DEEP : WorldGenBiomeDecoratorGroups.SEAGRASS_NORMAL);
        WorldGenBiomeSettings.addDefaultSeagrass(builder2);
        WorldGenBiomeSettings.addColdOceanExtraVegetation(builder2);
        WorldGenBiomeSettings.addSurfaceFreezing(builder2);
        return baseOceanBiome(builder, 4159204, 329011, deep, builder2);
    }

    public static BiomeBase lukeWarmOceanBiome(boolean deep) {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        if (deep) {
            WorldGenBiomeSettings.oceanSpawns(builder, 8, 4, 8);
        } else {
            WorldGenBiomeSettings.oceanSpawns(builder, 10, 2, 15);
        }

        builder.addSpawn(EnumCreatureType.WATER_AMBIENT, new BiomeSettingsMobs.SpawnerData(EntityTypes.PUFFERFISH, 5, 1, 3)).addSpawn(EnumCreatureType.WATER_AMBIENT, new BiomeSettingsMobs.SpawnerData(EntityTypes.TROPICAL_FISH, 25, 8, 8)).addSpawn(EnumCreatureType.WATER_CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.DOLPHIN, 2, 1, 2));
        BiomeSettingsGeneration.Builder builder2 = baseOceanGeneration(WorldGenSurfaceComposites.OCEAN_SAND, deep, true, false);
        builder2.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, deep ? WorldGenBiomeDecoratorGroups.SEAGRASS_DEEP_WARM : WorldGenBiomeDecoratorGroups.SEAGRASS_WARM);
        if (deep) {
            WorldGenBiomeSettings.addDefaultSeagrass(builder2);
        }

        WorldGenBiomeSettings.addLukeWarmKelp(builder2);
        WorldGenBiomeSettings.addSurfaceFreezing(builder2);
        return baseOceanBiome(builder, 4566514, 267827, deep, builder2);
    }

    public static BiomeBase warmOceanBiome() {
        BiomeSettingsMobs.Builder builder = (new BiomeSettingsMobs.Builder()).addSpawn(EnumCreatureType.WATER_AMBIENT, new BiomeSettingsMobs.SpawnerData(EntityTypes.PUFFERFISH, 15, 1, 3));
        WorldGenBiomeSettings.warmOceanSpawns(builder, 10, 4);
        BiomeSettingsGeneration.Builder builder2 = baseOceanGeneration(WorldGenSurfaceComposites.FULL_SAND, false, true, false).addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.WARM_OCEAN_VEGETATION).addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.SEAGRASS_WARM).addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.SEA_PICKLE);
        WorldGenBiomeSettings.addSurfaceFreezing(builder2);
        return baseOceanBiome(builder, 4445678, 270131, false, builder2);
    }

    public static BiomeBase deepWarmOceanBiome() {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        WorldGenBiomeSettings.warmOceanSpawns(builder, 5, 1);
        builder.addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.DROWNED, 5, 1, 1));
        BiomeSettingsGeneration.Builder builder2 = baseOceanGeneration(WorldGenSurfaceComposites.FULL_SAND, true, true, false).addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.SEAGRASS_DEEP_WARM);
        WorldGenBiomeSettings.addDefaultSeagrass(builder2);
        WorldGenBiomeSettings.addSurfaceFreezing(builder2);
        return baseOceanBiome(builder, 4445678, 270131, true, builder2);
    }

    public static BiomeBase frozenOceanBiome(boolean monument) {
        BiomeSettingsMobs.Builder builder = (new BiomeSettingsMobs.Builder()).addSpawn(EnumCreatureType.WATER_CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.SQUID, 1, 1, 4)).addSpawn(EnumCreatureType.WATER_AMBIENT, new BiomeSettingsMobs.SpawnerData(EntityTypes.SALMON, 15, 1, 5)).addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.POLAR_BEAR, 1, 1, 2));
        WorldGenBiomeSettings.commonSpawns(builder);
        builder.addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.DROWNED, 5, 1, 1));
        float f = monument ? 0.5F : 0.0F;
        BiomeSettingsGeneration.Builder builder2 = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(WorldGenSurfaceComposites.FROZEN_OCEAN);
        builder2.addStructureStart(WorldGenStructureFeatures.OCEAN_RUIN_COLD);
        if (monument) {
            builder2.addStructureStart(WorldGenStructureFeatures.OCEAN_MONUMENT);
        }

        WorldGenBiomeSettings.addDefaultOverworldOceanStructures(builder2);
        builder2.addStructureStart(WorldGenStructureFeatures.RUINED_PORTAL_OCEAN);
        WorldGenBiomeSettings.addOceanCarvers(builder2);
        WorldGenBiomeSettings.addDefaultLakes(builder2);
        WorldGenBiomeSettings.addIcebergs(builder2);
        WorldGenBiomeSettings.addDefaultCrystalFormations(builder2);
        WorldGenBiomeSettings.addDefaultMonsterRoom(builder2);
        WorldGenBiomeSettings.addBlueIce(builder2);
        WorldGenBiomeSettings.addDefaultUndergroundVariety(builder2, true);
        WorldGenBiomeSettings.addDefaultOres(builder2);
        WorldGenBiomeSettings.addDefaultSoftDisks(builder2);
        WorldGenBiomeSettings.addWaterTrees(builder2);
        WorldGenBiomeSettings.addDefaultFlowers(builder2);
        WorldGenBiomeSettings.addDefaultGrass(builder2);
        WorldGenBiomeSettings.addDefaultMushrooms(builder2);
        WorldGenBiomeSettings.addDefaultExtraVegetation(builder2);
        WorldGenBiomeSettings.addDefaultSprings(builder2);
        WorldGenBiomeSettings.addSurfaceFreezing(builder2);
        return (new BiomeBase.BiomeBuilder()).precipitation(monument ? BiomeBase.Precipitation.RAIN : BiomeBase.Precipitation.SNOW).biomeCategory(BiomeBase.Geography.OCEAN).depth(monument ? -1.8F : -1.0F).scale(0.1F).temperature(f).temperatureAdjustment(BiomeBase.TemperatureModifier.FROZEN).downfall(0.5F).specialEffects((new BiomeFog.Builder()).waterColor(3750089).waterFogColor(329011).fogColor(12638463).skyColor(calculateSkyColor(f)).ambientMoodSound(CaveSoundSettings.LEGACY_CAVE_SETTINGS).build()).mobSpawnSettings(builder.build()).generationSettings(builder2.build()).build();
    }

    private static BiomeBase baseForestBiome(float depth, float scale, boolean flower, BiomeSettingsMobs.Builder spawnSettings) {
        BiomeSettingsGeneration.Builder builder = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(WorldGenSurfaceComposites.GRASS);
        WorldGenBiomeSettings.addDefaultOverworldLandStructures(builder);
        builder.addStructureStart(WorldGenStructureFeatures.RUINED_PORTAL_STANDARD);
        WorldGenBiomeSettings.addDefaultCarvers(builder);
        WorldGenBiomeSettings.addDefaultLakes(builder);
        WorldGenBiomeSettings.addDefaultCrystalFormations(builder);
        WorldGenBiomeSettings.addDefaultMonsterRoom(builder);
        if (flower) {
            builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.FOREST_FLOWER_VEGETATION_COMMON);
        } else {
            WorldGenBiomeSettings.addForestFlowers(builder);
        }

        WorldGenBiomeSettings.addDefaultUndergroundVariety(builder);
        WorldGenBiomeSettings.addDefaultOres(builder);
        WorldGenBiomeSettings.addDefaultSoftDisks(builder);
        if (flower) {
            builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.FOREST_FLOWER_TREES);
            builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.FLOWER_FOREST);
            WorldGenBiomeSettings.addDefaultGrass(builder);
        } else {
            WorldGenBiomeSettings.addOtherBirchTrees(builder);
            WorldGenBiomeSettings.addDefaultFlowers(builder);
            WorldGenBiomeSettings.addForestGrass(builder);
        }

        WorldGenBiomeSettings.addDefaultMushrooms(builder);
        WorldGenBiomeSettings.addDefaultExtraVegetation(builder);
        WorldGenBiomeSettings.addDefaultSprings(builder);
        WorldGenBiomeSettings.addSurfaceFreezing(builder);
        return (new BiomeBase.BiomeBuilder()).precipitation(BiomeBase.Precipitation.RAIN).biomeCategory(BiomeBase.Geography.FOREST).depth(depth).scale(scale).temperature(0.7F).downfall(0.8F).specialEffects((new BiomeFog.Builder()).waterColor(4159204).waterFogColor(329011).fogColor(12638463).skyColor(calculateSkyColor(0.7F)).ambientMoodSound(CaveSoundSettings.LEGACY_CAVE_SETTINGS).build()).mobSpawnSettings(spawnSettings.build()).generationSettings(builder.build()).build();
    }

    private static BiomeSettingsMobs.Builder defaultSpawns() {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        WorldGenBiomeSettings.farmAnimals(builder);
        WorldGenBiomeSettings.commonSpawns(builder);
        return builder;
    }

    public static BiomeBase forestBiome(float depth, float scale) {
        BiomeSettingsMobs.Builder builder = defaultSpawns().addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.WOLF, 5, 4, 4)).setPlayerCanSpawn();
        return baseForestBiome(depth, scale, false, builder);
    }

    public static BiomeBase flowerForestBiome() {
        BiomeSettingsMobs.Builder builder = defaultSpawns().addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.RABBIT, 4, 2, 3));
        return baseForestBiome(0.1F, 0.4F, true, builder);
    }

    public static BiomeBase taigaBiome(float depth, float scale, boolean snowy, boolean mountains, boolean villages, boolean igloos) {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        WorldGenBiomeSettings.farmAnimals(builder);
        builder.addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.WOLF, 8, 4, 4)).addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.RABBIT, 4, 2, 3)).addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.FOX, 8, 2, 4));
        if (!snowy && !mountains) {
            builder.setPlayerCanSpawn();
        }

        WorldGenBiomeSettings.commonSpawns(builder);
        float f = snowy ? -0.5F : 0.25F;
        BiomeSettingsGeneration.Builder builder2 = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(WorldGenSurfaceComposites.GRASS);
        if (villages) {
            builder2.addStructureStart(WorldGenStructureFeatures.VILLAGE_TAIGA);
            builder2.addStructureStart(WorldGenStructureFeatures.PILLAGER_OUTPOST);
        }

        if (igloos) {
            builder2.addStructureStart(WorldGenStructureFeatures.IGLOO);
        }

        WorldGenBiomeSettings.addDefaultOverworldLandStructures(builder2);
        builder2.addStructureStart(mountains ? WorldGenStructureFeatures.RUINED_PORTAL_MOUNTAIN : WorldGenStructureFeatures.RUINED_PORTAL_STANDARD);
        WorldGenBiomeSettings.addDefaultCarvers(builder2);
        WorldGenBiomeSettings.addDefaultLakes(builder2);
        WorldGenBiomeSettings.addDefaultCrystalFormations(builder2);
        WorldGenBiomeSettings.addDefaultMonsterRoom(builder2);
        WorldGenBiomeSettings.addFerns(builder2);
        WorldGenBiomeSettings.addDefaultUndergroundVariety(builder2);
        WorldGenBiomeSettings.addDefaultOres(builder2);
        WorldGenBiomeSettings.addDefaultSoftDisks(builder2);
        WorldGenBiomeSettings.addTaigaTrees(builder2);
        WorldGenBiomeSettings.addDefaultFlowers(builder2);
        WorldGenBiomeSettings.addTaigaGrass(builder2);
        WorldGenBiomeSettings.addDefaultMushrooms(builder2);
        WorldGenBiomeSettings.addDefaultExtraVegetation(builder2);
        WorldGenBiomeSettings.addDefaultSprings(builder2);
        if (snowy) {
            WorldGenBiomeSettings.addBerryBushes(builder2);
        } else {
            WorldGenBiomeSettings.addSparseBerryBushes(builder2);
        }

        WorldGenBiomeSettings.addSurfaceFreezing(builder2);
        return (new BiomeBase.BiomeBuilder()).precipitation(snowy ? BiomeBase.Precipitation.SNOW : BiomeBase.Precipitation.RAIN).biomeCategory(BiomeBase.Geography.TAIGA).depth(depth).scale(scale).temperature(f).downfall(snowy ? 0.4F : 0.8F).specialEffects((new BiomeFog.Builder()).waterColor(snowy ? 4020182 : 4159204).waterFogColor(329011).fogColor(12638463).skyColor(calculateSkyColor(f)).ambientMoodSound(CaveSoundSettings.LEGACY_CAVE_SETTINGS).build()).mobSpawnSettings(builder.build()).generationSettings(builder2.build()).build();
    }

    public static BiomeBase darkForestBiome(float depth, float scale, boolean hills) {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        WorldGenBiomeSettings.farmAnimals(builder);
        WorldGenBiomeSettings.commonSpawns(builder);
        BiomeSettingsGeneration.Builder builder2 = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(WorldGenSurfaceComposites.GRASS);
        builder2.addStructureStart(WorldGenStructureFeatures.WOODLAND_MANSION);
        WorldGenBiomeSettings.addDefaultOverworldLandStructures(builder2);
        builder2.addStructureStart(WorldGenStructureFeatures.RUINED_PORTAL_STANDARD);
        WorldGenBiomeSettings.addDefaultCarvers(builder2);
        WorldGenBiomeSettings.addDefaultLakes(builder2);
        WorldGenBiomeSettings.addDefaultCrystalFormations(builder2);
        WorldGenBiomeSettings.addDefaultMonsterRoom(builder2);
        builder2.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, hills ? WorldGenBiomeDecoratorGroups.DARK_FOREST_VEGETATION_RED : WorldGenBiomeDecoratorGroups.DARK_FOREST_VEGETATION_BROWN);
        WorldGenBiomeSettings.addForestFlowers(builder2);
        WorldGenBiomeSettings.addDefaultUndergroundVariety(builder2);
        WorldGenBiomeSettings.addDefaultOres(builder2);
        WorldGenBiomeSettings.addDefaultSoftDisks(builder2);
        WorldGenBiomeSettings.addDefaultFlowers(builder2);
        WorldGenBiomeSettings.addForestGrass(builder2);
        WorldGenBiomeSettings.addDefaultMushrooms(builder2);
        WorldGenBiomeSettings.addDefaultExtraVegetation(builder2);
        WorldGenBiomeSettings.addDefaultSprings(builder2);
        WorldGenBiomeSettings.addSurfaceFreezing(builder2);
        return (new BiomeBase.BiomeBuilder()).precipitation(BiomeBase.Precipitation.RAIN).biomeCategory(BiomeBase.Geography.FOREST).depth(depth).scale(scale).temperature(0.7F).downfall(0.8F).specialEffects((new BiomeFog.Builder()).waterColor(4159204).waterFogColor(329011).fogColor(12638463).skyColor(calculateSkyColor(0.7F)).grassColorModifier(BiomeFog.GrassColor.DARK_FOREST).ambientMoodSound(CaveSoundSettings.LEGACY_CAVE_SETTINGS).build()).mobSpawnSettings(builder.build()).generationSettings(builder2.build()).build();
    }

    public static BiomeBase swampBiome(float depth, float scale, boolean hills) {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        WorldGenBiomeSettings.farmAnimals(builder);
        WorldGenBiomeSettings.commonSpawns(builder);
        builder.addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.SLIME, 1, 1, 1));
        BiomeSettingsGeneration.Builder builder2 = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(WorldGenSurfaceComposites.SWAMP);
        if (!hills) {
            builder2.addStructureStart(WorldGenStructureFeatures.SWAMP_HUT);
        }

        builder2.addStructureStart(WorldGenStructureFeatures.MINESHAFT);
        builder2.addStructureStart(WorldGenStructureFeatures.RUINED_PORTAL_SWAMP);
        WorldGenBiomeSettings.addDefaultCarvers(builder2);
        if (!hills) {
            WorldGenBiomeSettings.addFossilDecoration(builder2);
        }

        WorldGenBiomeSettings.addDefaultLakes(builder2);
        WorldGenBiomeSettings.addDefaultCrystalFormations(builder2);
        WorldGenBiomeSettings.addDefaultMonsterRoom(builder2);
        WorldGenBiomeSettings.addDefaultUndergroundVariety(builder2);
        WorldGenBiomeSettings.addDefaultOres(builder2);
        WorldGenBiomeSettings.addSwampClayDisk(builder2);
        WorldGenBiomeSettings.addSwampVegetation(builder2);
        WorldGenBiomeSettings.addDefaultMushrooms(builder2);
        WorldGenBiomeSettings.addSwampExtraVegetation(builder2);
        WorldGenBiomeSettings.addDefaultSprings(builder2);
        if (hills) {
            WorldGenBiomeSettings.addFossilDecoration(builder2);
        } else {
            builder2.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.SEAGRASS_SWAMP);
        }

        WorldGenBiomeSettings.addSurfaceFreezing(builder2);
        return (new BiomeBase.BiomeBuilder()).precipitation(BiomeBase.Precipitation.RAIN).biomeCategory(BiomeBase.Geography.SWAMP).depth(depth).scale(scale).temperature(0.8F).downfall(0.9F).specialEffects((new BiomeFog.Builder()).waterColor(6388580).waterFogColor(2302743).fogColor(12638463).skyColor(calculateSkyColor(0.8F)).foliageColorOverride(6975545).grassColorModifier(BiomeFog.GrassColor.SWAMP).ambientMoodSound(CaveSoundSettings.LEGACY_CAVE_SETTINGS).build()).mobSpawnSettings(builder.build()).generationSettings(builder2.build()).build();
    }

    public static BiomeBase tundraBiome(float depth, float scale, boolean iceSpikes, boolean mountains) {
        BiomeSettingsMobs.Builder builder = (new BiomeSettingsMobs.Builder()).creatureGenerationProbability(0.07F);
        WorldGenBiomeSettings.snowySpawns(builder);
        BiomeSettingsGeneration.Builder builder2 = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(iceSpikes ? WorldGenSurfaceComposites.ICE_SPIKES : WorldGenSurfaceComposites.GRASS);
        if (!iceSpikes && !mountains) {
            builder2.addStructureStart(WorldGenStructureFeatures.VILLAGE_SNOWY).addStructureStart(WorldGenStructureFeatures.IGLOO);
        }

        WorldGenBiomeSettings.addDefaultOverworldLandStructures(builder2);
        if (!iceSpikes && !mountains) {
            builder2.addStructureStart(WorldGenStructureFeatures.PILLAGER_OUTPOST);
        }

        builder2.addStructureStart(mountains ? WorldGenStructureFeatures.RUINED_PORTAL_MOUNTAIN : WorldGenStructureFeatures.RUINED_PORTAL_STANDARD);
        WorldGenBiomeSettings.addDefaultCarvers(builder2);
        WorldGenBiomeSettings.addDefaultLakes(builder2);
        WorldGenBiomeSettings.addDefaultCrystalFormations(builder2);
        WorldGenBiomeSettings.addDefaultMonsterRoom(builder2);
        if (iceSpikes) {
            builder2.addFeature(WorldGenStage.Decoration.SURFACE_STRUCTURES, WorldGenBiomeDecoratorGroups.ICE_SPIKE);
            builder2.addFeature(WorldGenStage.Decoration.SURFACE_STRUCTURES, WorldGenBiomeDecoratorGroups.ICE_PATCH);
        }

        WorldGenBiomeSettings.addDefaultUndergroundVariety(builder2);
        WorldGenBiomeSettings.addDefaultOres(builder2);
        WorldGenBiomeSettings.addDefaultSoftDisks(builder2);
        WorldGenBiomeSettings.addSnowyTrees(builder2);
        WorldGenBiomeSettings.addDefaultFlowers(builder2);
        WorldGenBiomeSettings.addDefaultGrass(builder2);
        WorldGenBiomeSettings.addDefaultMushrooms(builder2);
        WorldGenBiomeSettings.addDefaultExtraVegetation(builder2);
        WorldGenBiomeSettings.addDefaultSprings(builder2);
        WorldGenBiomeSettings.addSurfaceFreezing(builder2);
        return (new BiomeBase.BiomeBuilder()).precipitation(BiomeBase.Precipitation.SNOW).biomeCategory(BiomeBase.Geography.ICY).depth(depth).scale(scale).temperature(0.0F).downfall(0.5F).specialEffects((new BiomeFog.Builder()).waterColor(4159204).waterFogColor(329011).fogColor(12638463).skyColor(calculateSkyColor(0.0F)).ambientMoodSound(CaveSoundSettings.LEGACY_CAVE_SETTINGS).build()).mobSpawnSettings(builder.build()).generationSettings(builder2.build()).build();
    }

    public static BiomeBase riverBiome(float depth, float scale, float temperature, int waterColor, boolean frozen) {
        BiomeSettingsMobs.Builder builder = (new BiomeSettingsMobs.Builder()).addSpawn(EnumCreatureType.WATER_CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.SQUID, 2, 1, 4)).addSpawn(EnumCreatureType.WATER_AMBIENT, new BiomeSettingsMobs.SpawnerData(EntityTypes.SALMON, 5, 1, 5));
        WorldGenBiomeSettings.commonSpawns(builder);
        builder.addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.DROWNED, frozen ? 1 : 100, 1, 1));
        BiomeSettingsGeneration.Builder builder2 = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(WorldGenSurfaceComposites.GRASS);
        builder2.addStructureStart(WorldGenStructureFeatures.MINESHAFT);
        builder2.addStructureStart(WorldGenStructureFeatures.RUINED_PORTAL_STANDARD);
        WorldGenBiomeSettings.addDefaultCarvers(builder2);
        WorldGenBiomeSettings.addDefaultLakes(builder2);
        WorldGenBiomeSettings.addDefaultCrystalFormations(builder2);
        WorldGenBiomeSettings.addDefaultMonsterRoom(builder2);
        WorldGenBiomeSettings.addDefaultUndergroundVariety(builder2);
        WorldGenBiomeSettings.addDefaultOres(builder2);
        WorldGenBiomeSettings.addDefaultSoftDisks(builder2);
        WorldGenBiomeSettings.addWaterTrees(builder2);
        WorldGenBiomeSettings.addDefaultFlowers(builder2);
        WorldGenBiomeSettings.addDefaultGrass(builder2);
        WorldGenBiomeSettings.addDefaultMushrooms(builder2);
        WorldGenBiomeSettings.addDefaultExtraVegetation(builder2);
        WorldGenBiomeSettings.addDefaultSprings(builder2);
        if (!frozen) {
            builder2.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.SEAGRASS_RIVER);
        }

        WorldGenBiomeSettings.addSurfaceFreezing(builder2);
        return (new BiomeBase.BiomeBuilder()).precipitation(frozen ? BiomeBase.Precipitation.SNOW : BiomeBase.Precipitation.RAIN).biomeCategory(BiomeBase.Geography.RIVER).depth(depth).scale(scale).temperature(temperature).downfall(0.5F).specialEffects((new BiomeFog.Builder()).waterColor(waterColor).waterFogColor(329011).fogColor(12638463).skyColor(calculateSkyColor(temperature)).ambientMoodSound(CaveSoundSettings.LEGACY_CAVE_SETTINGS).build()).mobSpawnSettings(builder.build()).generationSettings(builder2.build()).build();
    }

    public static BiomeBase beachBiome(float depth, float scale, float temperature, float downfall, int waterColor, boolean snowy, boolean stony) {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        if (!stony && !snowy) {
            builder.addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.TURTLE, 5, 2, 5));
        }

        WorldGenBiomeSettings.commonSpawns(builder);
        BiomeSettingsGeneration.Builder builder2 = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(stony ? WorldGenSurfaceComposites.STONE : WorldGenSurfaceComposites.DESERT);
        if (stony) {
            WorldGenBiomeSettings.addDefaultOverworldLandStructures(builder2);
        } else {
            builder2.addStructureStart(WorldGenStructureFeatures.MINESHAFT);
            builder2.addStructureStart(WorldGenStructureFeatures.BURIED_TREASURE);
            builder2.addStructureStart(WorldGenStructureFeatures.SHIPWRECH_BEACHED);
        }

        builder2.addStructureStart(stony ? WorldGenStructureFeatures.RUINED_PORTAL_MOUNTAIN : WorldGenStructureFeatures.RUINED_PORTAL_STANDARD);
        WorldGenBiomeSettings.addDefaultCarvers(builder2);
        WorldGenBiomeSettings.addDefaultLakes(builder2);
        WorldGenBiomeSettings.addDefaultCrystalFormations(builder2);
        WorldGenBiomeSettings.addDefaultMonsterRoom(builder2);
        WorldGenBiomeSettings.addDefaultUndergroundVariety(builder2);
        WorldGenBiomeSettings.addDefaultOres(builder2);
        WorldGenBiomeSettings.addDefaultSoftDisks(builder2);
        WorldGenBiomeSettings.addDefaultFlowers(builder2);
        WorldGenBiomeSettings.addDefaultGrass(builder2);
        WorldGenBiomeSettings.addDefaultMushrooms(builder2);
        WorldGenBiomeSettings.addDefaultExtraVegetation(builder2);
        WorldGenBiomeSettings.addDefaultSprings(builder2);
        WorldGenBiomeSettings.addSurfaceFreezing(builder2);
        return (new BiomeBase.BiomeBuilder()).precipitation(snowy ? BiomeBase.Precipitation.SNOW : BiomeBase.Precipitation.RAIN).biomeCategory(stony ? BiomeBase.Geography.NONE : BiomeBase.Geography.BEACH).depth(depth).scale(scale).temperature(temperature).downfall(downfall).specialEffects((new BiomeFog.Builder()).waterColor(waterColor).waterFogColor(329011).fogColor(12638463).skyColor(calculateSkyColor(temperature)).ambientMoodSound(CaveSoundSettings.LEGACY_CAVE_SETTINGS).build()).mobSpawnSettings(builder.build()).generationSettings(builder2.build()).build();
    }

    public static BiomeBase theVoidBiome() {
        BiomeSettingsGeneration.Builder builder = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(WorldGenSurfaceComposites.NOPE);
        builder.addFeature(WorldGenStage.Decoration.TOP_LAYER_MODIFICATION, WorldGenBiomeDecoratorGroups.VOID_START_PLATFORM);
        return (new BiomeBase.BiomeBuilder()).precipitation(BiomeBase.Precipitation.NONE).biomeCategory(BiomeBase.Geography.NONE).depth(0.1F).scale(0.2F).temperature(0.5F).downfall(0.5F).specialEffects((new BiomeFog.Builder()).waterColor(4159204).waterFogColor(329011).fogColor(12638463).skyColor(calculateSkyColor(0.5F)).ambientMoodSound(CaveSoundSettings.LEGACY_CAVE_SETTINGS).build()).mobSpawnSettings(BiomeSettingsMobs.EMPTY).generationSettings(builder.build()).build();
    }

    public static BiomeBase netherWastesBiome() {
        BiomeSettingsMobs mobSpawnSettings = (new BiomeSettingsMobs.Builder()).addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.GHAST, 50, 4, 4)).addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.ZOMBIFIED_PIGLIN, 100, 4, 4)).addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.MAGMA_CUBE, 2, 4, 4)).addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.ENDERMAN, 1, 4, 4)).addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.PIGLIN, 15, 4, 4)).addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.STRIDER, 60, 1, 2)).build();
        BiomeSettingsGeneration.Builder builder = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(WorldGenSurfaceComposites.NETHER).addStructureStart(WorldGenStructureFeatures.RUINED_PORTAL_NETHER).addStructureStart(WorldGenStructureFeatures.NETHER_BRIDGE).addStructureStart(WorldGenStructureFeatures.BASTION_REMNANT).addCarver(WorldGenStage.Features.AIR, WorldGenCarvers.NETHER_CAVE).addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.SPRING_LAVA);
        WorldGenBiomeSettings.addDefaultMushrooms(builder);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, WorldGenBiomeDecoratorGroups.SPRING_OPEN).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, WorldGenBiomeDecoratorGroups.PATCH_FIRE).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, WorldGenBiomeDecoratorGroups.PATCH_SOUL_FIRE).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, WorldGenBiomeDecoratorGroups.GLOWSTONE_EXTRA).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, WorldGenBiomeDecoratorGroups.GLOWSTONE).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, WorldGenBiomeDecoratorGroups.BROWN_MUSHROOM_NETHER).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, WorldGenBiomeDecoratorGroups.RED_MUSHROOM_NETHER).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, WorldGenBiomeDecoratorGroups.ORE_MAGMA).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, WorldGenBiomeDecoratorGroups.SPRING_CLOSED);
        WorldGenBiomeSettings.addNetherDefaultOres(builder);
        return (new BiomeBase.BiomeBuilder()).precipitation(BiomeBase.Precipitation.NONE).biomeCategory(BiomeBase.Geography.NETHER).depth(0.1F).scale(0.2F).temperature(2.0F).downfall(0.0F).specialEffects((new BiomeFog.Builder()).waterColor(4159204).waterFogColor(329011).fogColor(3344392).skyColor(calculateSkyColor(2.0F)).ambientLoopSound(SoundEffects.AMBIENT_NETHER_WASTES_LOOP).ambientMoodSound(new CaveSoundSettings(SoundEffects.AMBIENT_NETHER_WASTES_MOOD, 6000, 8, 2.0D)).ambientAdditionsSound(new CaveSound(SoundEffects.AMBIENT_NETHER_WASTES_ADDITIONS, 0.0111D)).backgroundMusic(SoundTracks.createGameMusic(SoundEffects.MUSIC_BIOME_NETHER_WASTES)).build()).mobSpawnSettings(mobSpawnSettings).generationSettings(builder.build()).build();
    }

    public static BiomeBase soulSandValleyBiome() {
        double d = 0.7D;
        double e = 0.15D;
        BiomeSettingsMobs mobSpawnSettings = (new BiomeSettingsMobs.Builder()).addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.SKELETON, 20, 5, 5)).addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.GHAST, 50, 4, 4)).addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.ENDERMAN, 1, 4, 4)).addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.STRIDER, 60, 1, 2)).addMobCharge(EntityTypes.SKELETON, 0.7D, 0.15D).addMobCharge(EntityTypes.GHAST, 0.7D, 0.15D).addMobCharge(EntityTypes.ENDERMAN, 0.7D, 0.15D).addMobCharge(EntityTypes.STRIDER, 0.7D, 0.15D).build();
        BiomeSettingsGeneration.Builder builder = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(WorldGenSurfaceComposites.SOUL_SAND_VALLEY).addStructureStart(WorldGenStructureFeatures.NETHER_BRIDGE).addStructureStart(WorldGenStructureFeatures.NETHER_FOSSIL).addStructureStart(WorldGenStructureFeatures.RUINED_PORTAL_NETHER).addStructureStart(WorldGenStructureFeatures.BASTION_REMNANT).addCarver(WorldGenStage.Features.AIR, WorldGenCarvers.NETHER_CAVE).addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.SPRING_LAVA).addFeature(WorldGenStage.Decoration.LOCAL_MODIFICATIONS, WorldGenBiomeDecoratorGroups.BASALT_PILLAR).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, WorldGenBiomeDecoratorGroups.SPRING_OPEN).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, WorldGenBiomeDecoratorGroups.GLOWSTONE_EXTRA).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, WorldGenBiomeDecoratorGroups.GLOWSTONE).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, WorldGenBiomeDecoratorGroups.PATCH_CRIMSON_ROOTS).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, WorldGenBiomeDecoratorGroups.PATCH_FIRE).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, WorldGenBiomeDecoratorGroups.PATCH_SOUL_FIRE).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, WorldGenBiomeDecoratorGroups.ORE_MAGMA).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, WorldGenBiomeDecoratorGroups.SPRING_CLOSED).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, WorldGenBiomeDecoratorGroups.ORE_SOUL_SAND);
        WorldGenBiomeSettings.addNetherDefaultOres(builder);
        return (new BiomeBase.BiomeBuilder()).precipitation(BiomeBase.Precipitation.NONE).biomeCategory(BiomeBase.Geography.NETHER).depth(0.1F).scale(0.2F).temperature(2.0F).downfall(0.0F).specialEffects((new BiomeFog.Builder()).waterColor(4159204).waterFogColor(329011).fogColor(1787717).skyColor(calculateSkyColor(2.0F)).ambientParticle(new BiomeParticles(Particles.ASH, 0.00625F)).ambientLoopSound(SoundEffects.AMBIENT_SOUL_SAND_VALLEY_LOOP).ambientMoodSound(new CaveSoundSettings(SoundEffects.AMBIENT_SOUL_SAND_VALLEY_MOOD, 6000, 8, 2.0D)).ambientAdditionsSound(new CaveSound(SoundEffects.AMBIENT_SOUL_SAND_VALLEY_ADDITIONS, 0.0111D)).backgroundMusic(SoundTracks.createGameMusic(SoundEffects.MUSIC_BIOME_SOUL_SAND_VALLEY)).build()).mobSpawnSettings(mobSpawnSettings).generationSettings(builder.build()).build();
    }

    public static BiomeBase basaltDeltasBiome() {
        BiomeSettingsMobs mobSpawnSettings = (new BiomeSettingsMobs.Builder()).addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.GHAST, 40, 1, 1)).addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.MAGMA_CUBE, 100, 2, 5)).addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.STRIDER, 60, 1, 2)).build();
        BiomeSettingsGeneration.Builder builder = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(WorldGenSurfaceComposites.BASALT_DELTAS).addStructureStart(WorldGenStructureFeatures.RUINED_PORTAL_NETHER).addCarver(WorldGenStage.Features.AIR, WorldGenCarvers.NETHER_CAVE).addStructureStart(WorldGenStructureFeatures.NETHER_BRIDGE).addFeature(WorldGenStage.Decoration.SURFACE_STRUCTURES, WorldGenBiomeDecoratorGroups.DELTA).addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.SPRING_LAVA_DOUBLE).addFeature(WorldGenStage.Decoration.SURFACE_STRUCTURES, WorldGenBiomeDecoratorGroups.SMALL_BASALT_COLUMNS).addFeature(WorldGenStage.Decoration.SURFACE_STRUCTURES, WorldGenBiomeDecoratorGroups.LARGE_BASALT_COLUMNS).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, WorldGenBiomeDecoratorGroups.BASALT_BLOBS).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, WorldGenBiomeDecoratorGroups.BLACKSTONE_BLOBS).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, WorldGenBiomeDecoratorGroups.SPRING_DELTA).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, WorldGenBiomeDecoratorGroups.PATCH_FIRE).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, WorldGenBiomeDecoratorGroups.PATCH_SOUL_FIRE).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, WorldGenBiomeDecoratorGroups.GLOWSTONE_EXTRA).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, WorldGenBiomeDecoratorGroups.GLOWSTONE).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, WorldGenBiomeDecoratorGroups.BROWN_MUSHROOM_NETHER).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, WorldGenBiomeDecoratorGroups.RED_MUSHROOM_NETHER).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, WorldGenBiomeDecoratorGroups.ORE_MAGMA).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, WorldGenBiomeDecoratorGroups.SPRING_CLOSED_DOUBLE).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, WorldGenBiomeDecoratorGroups.ORE_GOLD_DELTAS).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, WorldGenBiomeDecoratorGroups.ORE_QUARTZ_DELTAS);
        WorldGenBiomeSettings.addAncientDebris(builder);
        return (new BiomeBase.BiomeBuilder()).precipitation(BiomeBase.Precipitation.NONE).biomeCategory(BiomeBase.Geography.NETHER).depth(0.1F).scale(0.2F).temperature(2.0F).downfall(0.0F).specialEffects((new BiomeFog.Builder()).waterColor(4159204).waterFogColor(4341314).fogColor(6840176).skyColor(calculateSkyColor(2.0F)).ambientParticle(new BiomeParticles(Particles.WHITE_ASH, 0.118093334F)).ambientLoopSound(SoundEffects.AMBIENT_BASALT_DELTAS_LOOP).ambientMoodSound(new CaveSoundSettings(SoundEffects.AMBIENT_BASALT_DELTAS_MOOD, 6000, 8, 2.0D)).ambientAdditionsSound(new CaveSound(SoundEffects.AMBIENT_BASALT_DELTAS_ADDITIONS, 0.0111D)).backgroundMusic(SoundTracks.createGameMusic(SoundEffects.MUSIC_BIOME_BASALT_DELTAS)).build()).mobSpawnSettings(mobSpawnSettings).generationSettings(builder.build()).build();
    }

    public static BiomeBase crimsonForestBiome() {
        BiomeSettingsMobs mobSpawnSettings = (new BiomeSettingsMobs.Builder()).addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.ZOMBIFIED_PIGLIN, 1, 2, 4)).addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.HOGLIN, 9, 3, 4)).addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.PIGLIN, 5, 3, 4)).addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.STRIDER, 60, 1, 2)).build();
        BiomeSettingsGeneration.Builder builder = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(WorldGenSurfaceComposites.CRIMSON_FOREST).addStructureStart(WorldGenStructureFeatures.RUINED_PORTAL_NETHER).addCarver(WorldGenStage.Features.AIR, WorldGenCarvers.NETHER_CAVE).addStructureStart(WorldGenStructureFeatures.NETHER_BRIDGE).addStructureStart(WorldGenStructureFeatures.BASTION_REMNANT).addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.SPRING_LAVA);
        WorldGenBiomeSettings.addDefaultMushrooms(builder);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, WorldGenBiomeDecoratorGroups.SPRING_OPEN).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, WorldGenBiomeDecoratorGroups.PATCH_FIRE).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, WorldGenBiomeDecoratorGroups.GLOWSTONE_EXTRA).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, WorldGenBiomeDecoratorGroups.GLOWSTONE).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, WorldGenBiomeDecoratorGroups.ORE_MAGMA).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, WorldGenBiomeDecoratorGroups.SPRING_CLOSED).addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.WEEPING_VINES).addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.CRIMSON_FUNGI).addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.CRIMSON_FOREST_VEGETATION);
        WorldGenBiomeSettings.addNetherDefaultOres(builder);
        return (new BiomeBase.BiomeBuilder()).precipitation(BiomeBase.Precipitation.NONE).biomeCategory(BiomeBase.Geography.NETHER).depth(0.1F).scale(0.2F).temperature(2.0F).downfall(0.0F).specialEffects((new BiomeFog.Builder()).waterColor(4159204).waterFogColor(329011).fogColor(3343107).skyColor(calculateSkyColor(2.0F)).ambientParticle(new BiomeParticles(Particles.CRIMSON_SPORE, 0.025F)).ambientLoopSound(SoundEffects.AMBIENT_CRIMSON_FOREST_LOOP).ambientMoodSound(new CaveSoundSettings(SoundEffects.AMBIENT_CRIMSON_FOREST_MOOD, 6000, 8, 2.0D)).ambientAdditionsSound(new CaveSound(SoundEffects.AMBIENT_CRIMSON_FOREST_ADDITIONS, 0.0111D)).backgroundMusic(SoundTracks.createGameMusic(SoundEffects.MUSIC_BIOME_CRIMSON_FOREST)).build()).mobSpawnSettings(mobSpawnSettings).generationSettings(builder.build()).build();
    }

    public static BiomeBase warpedForestBiome() {
        BiomeSettingsMobs mobSpawnSettings = (new BiomeSettingsMobs.Builder()).addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.ENDERMAN, 1, 4, 4)).addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.STRIDER, 60, 1, 2)).addMobCharge(EntityTypes.ENDERMAN, 1.0D, 0.12D).build();
        BiomeSettingsGeneration.Builder builder = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(WorldGenSurfaceComposites.WARPED_FOREST).addStructureStart(WorldGenStructureFeatures.NETHER_BRIDGE).addStructureStart(WorldGenStructureFeatures.BASTION_REMNANT).addStructureStart(WorldGenStructureFeatures.RUINED_PORTAL_NETHER).addCarver(WorldGenStage.Features.AIR, WorldGenCarvers.NETHER_CAVE).addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.SPRING_LAVA);
        WorldGenBiomeSettings.addDefaultMushrooms(builder);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, WorldGenBiomeDecoratorGroups.SPRING_OPEN).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, WorldGenBiomeDecoratorGroups.PATCH_FIRE).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, WorldGenBiomeDecoratorGroups.PATCH_SOUL_FIRE).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, WorldGenBiomeDecoratorGroups.GLOWSTONE_EXTRA).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, WorldGenBiomeDecoratorGroups.GLOWSTONE).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, WorldGenBiomeDecoratorGroups.ORE_MAGMA).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, WorldGenBiomeDecoratorGroups.SPRING_CLOSED).addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.WARPED_FUNGI).addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.WARPED_FOREST_VEGETATION).addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.NETHER_SPROUTS).addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, WorldGenBiomeDecoratorGroups.TWISTING_VINES);
        WorldGenBiomeSettings.addNetherDefaultOres(builder);
        return (new BiomeBase.BiomeBuilder()).precipitation(BiomeBase.Precipitation.NONE).biomeCategory(BiomeBase.Geography.NETHER).depth(0.1F).scale(0.2F).temperature(2.0F).downfall(0.0F).specialEffects((new BiomeFog.Builder()).waterColor(4159204).waterFogColor(329011).fogColor(1705242).skyColor(calculateSkyColor(2.0F)).ambientParticle(new BiomeParticles(Particles.WARPED_SPORE, 0.01428F)).ambientLoopSound(SoundEffects.AMBIENT_WARPED_FOREST_LOOP).ambientMoodSound(new CaveSoundSettings(SoundEffects.AMBIENT_WARPED_FOREST_MOOD, 6000, 8, 2.0D)).ambientAdditionsSound(new CaveSound(SoundEffects.AMBIENT_WARPED_FOREST_ADDITIONS, 0.0111D)).backgroundMusic(SoundTracks.createGameMusic(SoundEffects.MUSIC_BIOME_WARPED_FOREST)).build()).mobSpawnSettings(mobSpawnSettings).generationSettings(builder.build()).build();
    }

    public static BiomeBase lushCaves() {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        WorldGenBiomeSettings.commonSpawns(builder);
        BiomeSettingsGeneration.Builder builder2 = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(WorldGenSurfaceComposites.GRASS);
        WorldGenBiomeSettings.addDefaultOverworldLandStructures(builder2);
        builder2.addStructureStart(WorldGenStructureFeatures.RUINED_PORTAL_STANDARD);
        WorldGenBiomeSettings.addDefaultCarvers(builder2);
        WorldGenBiomeSettings.addDefaultLakes(builder2);
        WorldGenBiomeSettings.addDefaultCrystalFormations(builder2);
        WorldGenBiomeSettings.addDefaultMonsterRoom(builder2);
        WorldGenBiomeSettings.addPlainGrass(builder2);
        WorldGenBiomeSettings.addDefaultUndergroundVariety(builder2);
        WorldGenBiomeSettings.addDefaultOres(builder2);
        WorldGenBiomeSettings.addLushCavesSpecialOres(builder2);
        WorldGenBiomeSettings.addDefaultSoftDisks(builder2);
        WorldGenBiomeSettings.addLushCavesVegetationFeatures(builder2);
        return (new BiomeBase.BiomeBuilder()).precipitation(BiomeBase.Precipitation.RAIN).biomeCategory(BiomeBase.Geography.UNDERGROUND).depth(0.1F).scale(0.2F).temperature(0.5F).downfall(0.5F).specialEffects((new BiomeFog.Builder()).waterColor(4159204).waterFogColor(329011).fogColor(12638463).skyColor(calculateSkyColor(0.5F)).build()).mobSpawnSettings(builder.build()).generationSettings(builder2.build()).build();
    }

    public static BiomeBase dripstoneCaves() {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        WorldGenBiomeSettings.commonSpawns(builder);
        BiomeSettingsGeneration.Builder builder2 = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(WorldGenSurfaceComposites.GRASS);
        WorldGenBiomeSettings.addDefaultOverworldLandStructures(builder2);
        builder2.addStructureStart(WorldGenStructureFeatures.RUINED_PORTAL_STANDARD);
        WorldGenBiomeSettings.addDefaultCarvers(builder2);
        WorldGenBiomeSettings.addDefaultLakes(builder2);
        WorldGenBiomeSettings.addDefaultCrystalFormations(builder2);
        WorldGenBiomeSettings.addDefaultMonsterRoom(builder2);
        WorldGenBiomeSettings.addPlainGrass(builder2);
        WorldGenBiomeSettings.addDefaultUndergroundVariety(builder2);
        WorldGenBiomeSettings.addDefaultOres(builder2);
        WorldGenBiomeSettings.addDefaultSoftDisks(builder2);
        WorldGenBiomeSettings.addPlainVegetation(builder2);
        WorldGenBiomeSettings.addDefaultMushrooms(builder2);
        WorldGenBiomeSettings.addDefaultExtraVegetation(builder2);
        WorldGenBiomeSettings.addDefaultSprings(builder2);
        WorldGenBiomeSettings.addSurfaceFreezing(builder2);
        WorldGenBiomeSettings.addDripstone(builder2);
        return (new BiomeBase.BiomeBuilder()).precipitation(BiomeBase.Precipitation.RAIN).biomeCategory(BiomeBase.Geography.UNDERGROUND).depth(0.125F).scale(0.05F).temperature(0.8F).downfall(0.4F).specialEffects((new BiomeFog.Builder()).waterColor(4159204).waterFogColor(329011).fogColor(12638463).skyColor(calculateSkyColor(0.8F)).ambientMoodSound(CaveSoundSettings.LEGACY_CAVE_SETTINGS).build()).mobSpawnSettings(builder.build()).generationSettings(builder2.build()).build();
    }
}
