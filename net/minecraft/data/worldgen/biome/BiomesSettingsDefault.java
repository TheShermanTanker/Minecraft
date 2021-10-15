package net.minecraft.data.worldgen.biome;

import net.minecraft.core.particles.Particles;
import net.minecraft.data.worldgen.BiomeDecoratorGroups;
import net.minecraft.data.worldgen.BiomeSettings;
import net.minecraft.data.worldgen.StructureFeatures;
import net.minecraft.data.worldgen.WorldGenCarvers;
import net.minecraft.data.worldgen.WorldGenSurfaceComposites;
import net.minecraft.sounds.Musics;
import net.minecraft.sounds.SoundEffects;
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

public class BiomesSettingsDefault {
    private static int calculateSkyColor(float temperature) {
        float f = temperature / 3.0F;
        f = MathHelper.clamp(f, -1.0F, 1.0F);
        return MathHelper.hsvToRgb(0.62222224F - f * 0.05F, 0.5F + f * 0.1F, 1.0F);
    }

    public static BiomeBase giantTreeTaiga(float depth, float scale, float temperature, boolean spruce) {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        BiomeSettings.farmAnimals(builder);
        builder.addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.WOLF, 8, 4, 4));
        builder.addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.RABBIT, 4, 2, 3));
        builder.addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.FOX, 8, 2, 4));
        if (spruce) {
            BiomeSettings.commonSpawns(builder);
        } else {
            BiomeSettings.caveSpawns(builder);
            BiomeSettings.monsters(builder, 100, 25, 100);
        }

        BiomeSettingsGeneration.Builder builder2 = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(WorldGenSurfaceComposites.GIANT_TREE_TAIGA);
        BiomeSettings.addDefaultOverworldLandStructures(builder2);
        builder2.addStructureStart(StructureFeatures.RUINED_PORTAL_STANDARD);
        BiomeSettings.addDefaultCarvers(builder2);
        BiomeSettings.addDefaultLakes(builder2);
        BiomeSettings.addDefaultCrystalFormations(builder2);
        BiomeSettings.addDefaultMonsterRoom(builder2);
        BiomeSettings.addMossyStoneBlock(builder2);
        BiomeSettings.addFerns(builder2);
        BiomeSettings.addDefaultUndergroundVariety(builder2);
        BiomeSettings.addDefaultOres(builder2);
        BiomeSettings.addDefaultSoftDisks(builder2);
        builder2.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, spruce ? BiomeDecoratorGroups.TREES_GIANT_SPRUCE : BiomeDecoratorGroups.TREES_GIANT);
        BiomeSettings.addDefaultFlowers(builder2);
        BiomeSettings.addGiantTaigaVegetation(builder2);
        BiomeSettings.addDefaultMushrooms(builder2);
        BiomeSettings.addDefaultExtraVegetation(builder2);
        BiomeSettings.addDefaultSprings(builder2);
        BiomeSettings.addSparseBerryBushes(builder2);
        BiomeSettings.addSurfaceFreezing(builder2);
        return (new BiomeBase.BiomeBuilder()).precipitation(BiomeBase.Precipitation.RAIN).biomeCategory(BiomeBase.Geography.TAIGA).depth(depth).scale(scale).temperature(temperature).downfall(0.8F).specialEffects((new BiomeFog.Builder()).waterColor(4159204).waterFogColor(329011).fogColor(12638463).skyColor(calculateSkyColor(temperature)).ambientMoodSound(CaveSoundSettings.LEGACY_CAVE_SETTINGS).build()).mobSpawnSettings(builder.build()).generationSettings(builder2.build()).build();
    }

    public static BiomeBase birchForestBiome(float depth, float scale, boolean tallTrees) {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        BiomeSettings.farmAnimals(builder);
        BiomeSettings.commonSpawns(builder);
        BiomeSettingsGeneration.Builder builder2 = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(WorldGenSurfaceComposites.GRASS);
        BiomeSettings.addDefaultOverworldLandStructures(builder2);
        builder2.addStructureStart(StructureFeatures.RUINED_PORTAL_STANDARD);
        BiomeSettings.addDefaultCarvers(builder2);
        BiomeSettings.addDefaultLakes(builder2);
        BiomeSettings.addDefaultCrystalFormations(builder2);
        BiomeSettings.addDefaultMonsterRoom(builder2);
        BiomeSettings.addForestFlowers(builder2);
        BiomeSettings.addDefaultUndergroundVariety(builder2);
        BiomeSettings.addDefaultOres(builder2);
        BiomeSettings.addDefaultSoftDisks(builder2);
        if (tallTrees) {
            BiomeSettings.addTallBirchTrees(builder2);
        } else {
            BiomeSettings.addBirchTrees(builder2);
        }

        BiomeSettings.addDefaultFlowers(builder2);
        BiomeSettings.addForestGrass(builder2);
        BiomeSettings.addDefaultMushrooms(builder2);
        BiomeSettings.addDefaultExtraVegetation(builder2);
        BiomeSettings.addDefaultSprings(builder2);
        BiomeSettings.addSurfaceFreezing(builder2);
        return (new BiomeBase.BiomeBuilder()).precipitation(BiomeBase.Precipitation.RAIN).biomeCategory(BiomeBase.Geography.FOREST).depth(depth).scale(scale).temperature(0.6F).downfall(0.6F).specialEffects((new BiomeFog.Builder()).waterColor(4159204).waterFogColor(329011).fogColor(12638463).skyColor(calculateSkyColor(0.6F)).ambientMoodSound(CaveSoundSettings.LEGACY_CAVE_SETTINGS).build()).mobSpawnSettings(builder.build()).generationSettings(builder2.build()).build();
    }

    public static BiomeBase jungleBiome() {
        return jungleBiome(0.1F, 0.2F, 40, 2, 3);
    }

    public static BiomeBase jungleEdgeBiome() {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        BiomeSettings.baseJungleSpawns(builder);
        return baseJungleBiome(0.1F, 0.2F, 0.8F, false, true, false, builder);
    }

    public static BiomeBase modifiedJungleEdgeBiome() {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        BiomeSettings.baseJungleSpawns(builder);
        return baseJungleBiome(0.2F, 0.4F, 0.8F, false, true, true, builder);
    }

    public static BiomeBase modifiedJungleBiome() {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        BiomeSettings.baseJungleSpawns(builder);
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
        BiomeSettings.baseJungleSpawns(builder);
        builder.addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.PARROT, parrotWeight, 1, parrotMaxGroupSize)).addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.OCELOT, 2, 1, ocelotMaxGroupSize)).addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.PANDA, 1, 1, 2));
        builder.setPlayerCanSpawn();
        return baseJungleBiome(depth, scale, 0.9F, false, false, false, builder);
    }

    private static BiomeBase bambooJungleBiome(float depth, float scale, int parrotWeight, int parrotMaxGroupSize) {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        BiomeSettings.baseJungleSpawns(builder);
        builder.addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.PARROT, parrotWeight, 1, parrotMaxGroupSize)).addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.PANDA, 80, 1, 2)).addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.OCELOT, 2, 1, 1));
        return baseJungleBiome(depth, scale, 0.9F, true, false, false, builder);
    }

    private static BiomeBase baseJungleBiome(float depth, float scale, float downfall, boolean bamboo, boolean edge, boolean modified, BiomeSettingsMobs.Builder builder) {
        BiomeSettingsGeneration.Builder builder2 = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(WorldGenSurfaceComposites.GRASS);
        if (!edge && !modified) {
            builder2.addStructureStart(StructureFeatures.JUNGLE_TEMPLE);
        }

        BiomeSettings.addDefaultOverworldLandStructures(builder2);
        builder2.addStructureStart(StructureFeatures.RUINED_PORTAL_JUNGLE);
        BiomeSettings.addDefaultCarvers(builder2);
        BiomeSettings.addDefaultLakes(builder2);
        BiomeSettings.addDefaultCrystalFormations(builder2);
        BiomeSettings.addDefaultMonsterRoom(builder2);
        BiomeSettings.addDefaultUndergroundVariety(builder2);
        BiomeSettings.addDefaultOres(builder2);
        BiomeSettings.addDefaultSoftDisks(builder2);
        if (bamboo) {
            BiomeSettings.addBambooVegetation(builder2);
        } else {
            if (!edge && !modified) {
                BiomeSettings.addLightBambooVegetation(builder2);
            }

            if (edge) {
                BiomeSettings.addJungleEdgeTrees(builder2);
            } else {
                BiomeSettings.addJungleTrees(builder2);
            }
        }

        BiomeSettings.addWarmFlowers(builder2);
        BiomeSettings.addJungleGrass(builder2);
        BiomeSettings.addDefaultMushrooms(builder2);
        BiomeSettings.addDefaultExtraVegetation(builder2);
        BiomeSettings.addDefaultSprings(builder2);
        BiomeSettings.addJungleExtraVegetation(builder2);
        BiomeSettings.addSurfaceFreezing(builder2);
        return (new BiomeBase.BiomeBuilder()).precipitation(BiomeBase.Precipitation.RAIN).biomeCategory(BiomeBase.Geography.JUNGLE).depth(depth).scale(scale).temperature(0.95F).downfall(downfall).specialEffects((new BiomeFog.Builder()).waterColor(4159204).waterFogColor(329011).fogColor(12638463).skyColor(calculateSkyColor(0.95F)).ambientMoodSound(CaveSoundSettings.LEGACY_CAVE_SETTINGS).build()).mobSpawnSettings(builder.build()).generationSettings(builder2.build()).build();
    }

    public static BiomeBase mountainBiome(float depth, float scale, WorldGenSurfaceComposite<WorldGenSurfaceConfigurationBase> surfaceBuilder, boolean extraTrees) {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        BiomeSettings.farmAnimals(builder);
        builder.addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.LLAMA, 5, 4, 6));
        builder.addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.GOAT, 10, 4, 6));
        BiomeSettings.commonSpawns(builder);
        BiomeSettingsGeneration.Builder builder2 = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(surfaceBuilder);
        BiomeSettings.addDefaultOverworldLandStructures(builder2);
        builder2.addStructureStart(StructureFeatures.RUINED_PORTAL_MOUNTAIN);
        BiomeSettings.addDefaultCarvers(builder2);
        BiomeSettings.addDefaultLakes(builder2);
        BiomeSettings.addDefaultCrystalFormations(builder2);
        BiomeSettings.addDefaultMonsterRoom(builder2);
        BiomeSettings.addDefaultUndergroundVariety(builder2);
        BiomeSettings.addDefaultOres(builder2);
        BiomeSettings.addDefaultSoftDisks(builder2);
        if (extraTrees) {
            BiomeSettings.addMountainEdgeTrees(builder2);
        } else {
            BiomeSettings.addMountainTrees(builder2);
        }

        BiomeSettings.addDefaultFlowers(builder2);
        BiomeSettings.addDefaultGrass(builder2);
        BiomeSettings.addDefaultMushrooms(builder2);
        BiomeSettings.addDefaultExtraVegetation(builder2);
        BiomeSettings.addDefaultSprings(builder2);
        BiomeSettings.addExtraEmeralds(builder2);
        BiomeSettings.addInfestedStone(builder2);
        BiomeSettings.addSurfaceFreezing(builder2);
        return (new BiomeBase.BiomeBuilder()).precipitation(BiomeBase.Precipitation.RAIN).biomeCategory(BiomeBase.Geography.EXTREME_HILLS).depth(depth).scale(scale).temperature(0.2F).downfall(0.3F).specialEffects((new BiomeFog.Builder()).waterColor(4159204).waterFogColor(329011).fogColor(12638463).skyColor(calculateSkyColor(0.2F)).ambientMoodSound(CaveSoundSettings.LEGACY_CAVE_SETTINGS).build()).mobSpawnSettings(builder.build()).generationSettings(builder2.build()).build();
    }

    public static BiomeBase desertBiome(float depth, float scale, boolean villages, boolean pyramids, boolean fossils) {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        BiomeSettings.desertSpawns(builder);
        BiomeSettingsGeneration.Builder builder2 = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(WorldGenSurfaceComposites.DESERT);
        if (villages) {
            builder2.addStructureStart(StructureFeatures.VILLAGE_DESERT);
            builder2.addStructureStart(StructureFeatures.PILLAGER_OUTPOST);
        }

        if (pyramids) {
            builder2.addStructureStart(StructureFeatures.DESERT_PYRAMID);
        }

        if (fossils) {
            BiomeSettings.addFossilDecoration(builder2);
        }

        BiomeSettings.addDefaultOverworldLandStructures(builder2);
        builder2.addStructureStart(StructureFeatures.RUINED_PORTAL_DESERT);
        BiomeSettings.addDefaultCarvers(builder2);
        BiomeSettings.addDesertLakes(builder2);
        BiomeSettings.addDefaultCrystalFormations(builder2);
        BiomeSettings.addDefaultMonsterRoom(builder2);
        BiomeSettings.addDefaultUndergroundVariety(builder2);
        BiomeSettings.addDefaultOres(builder2);
        BiomeSettings.addDefaultSoftDisks(builder2);
        BiomeSettings.addDefaultFlowers(builder2);
        BiomeSettings.addDefaultGrass(builder2);
        BiomeSettings.addDesertVegetation(builder2);
        BiomeSettings.addDefaultMushrooms(builder2);
        BiomeSettings.addDesertExtraVegetation(builder2);
        BiomeSettings.addDefaultSprings(builder2);
        BiomeSettings.addDesertExtraDecoration(builder2);
        BiomeSettings.addSurfaceFreezing(builder2);
        return (new BiomeBase.BiomeBuilder()).precipitation(BiomeBase.Precipitation.NONE).biomeCategory(BiomeBase.Geography.DESERT).depth(depth).scale(scale).temperature(2.0F).downfall(0.0F).specialEffects((new BiomeFog.Builder()).waterColor(4159204).waterFogColor(329011).fogColor(12638463).skyColor(calculateSkyColor(2.0F)).ambientMoodSound(CaveSoundSettings.LEGACY_CAVE_SETTINGS).build()).mobSpawnSettings(builder.build()).generationSettings(builder2.build()).build();
    }

    public static BiomeBase plainsBiome(boolean sunflower) {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        BiomeSettings.plainsSpawns(builder);
        if (!sunflower) {
            builder.setPlayerCanSpawn();
        }

        BiomeSettingsGeneration.Builder builder2 = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(WorldGenSurfaceComposites.GRASS);
        if (!sunflower) {
            builder2.addStructureStart(StructureFeatures.VILLAGE_PLAINS).addStructureStart(StructureFeatures.PILLAGER_OUTPOST);
        }

        BiomeSettings.addDefaultOverworldLandStructures(builder2);
        builder2.addStructureStart(StructureFeatures.RUINED_PORTAL_STANDARD);
        BiomeSettings.addDefaultCarvers(builder2);
        BiomeSettings.addDefaultLakes(builder2);
        BiomeSettings.addDefaultCrystalFormations(builder2);
        BiomeSettings.addDefaultMonsterRoom(builder2);
        BiomeSettings.addPlainGrass(builder2);
        if (sunflower) {
            builder2.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, BiomeDecoratorGroups.PATCH_SUNFLOWER);
        }

        BiomeSettings.addDefaultUndergroundVariety(builder2);
        BiomeSettings.addDefaultOres(builder2);
        BiomeSettings.addDefaultSoftDisks(builder2);
        BiomeSettings.addPlainVegetation(builder2);
        if (sunflower) {
            builder2.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, BiomeDecoratorGroups.PATCH_SUGAR_CANE);
        }

        BiomeSettings.addDefaultMushrooms(builder2);
        if (sunflower) {
            builder2.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, BiomeDecoratorGroups.PATCH_PUMPKIN);
        } else {
            BiomeSettings.addDefaultExtraVegetation(builder2);
        }

        BiomeSettings.addDefaultSprings(builder2);
        BiomeSettings.addSurfaceFreezing(builder2);
        return (new BiomeBase.BiomeBuilder()).precipitation(BiomeBase.Precipitation.RAIN).biomeCategory(BiomeBase.Geography.PLAINS).depth(0.125F).scale(0.05F).temperature(0.8F).downfall(0.4F).specialEffects((new BiomeFog.Builder()).waterColor(4159204).waterFogColor(329011).fogColor(12638463).skyColor(calculateSkyColor(0.8F)).ambientMoodSound(CaveSoundSettings.LEGACY_CAVE_SETTINGS).build()).mobSpawnSettings(builder.build()).generationSettings(builder2.build()).build();
    }

    private static BiomeBase baseEndBiome(BiomeSettingsGeneration.Builder builder) {
        BiomeSettingsMobs.Builder builder2 = new BiomeSettingsMobs.Builder();
        BiomeSettings.endSpawns(builder2);
        return (new BiomeBase.BiomeBuilder()).precipitation(BiomeBase.Precipitation.NONE).biomeCategory(BiomeBase.Geography.THEEND).depth(0.1F).scale(0.2F).temperature(0.5F).downfall(0.5F).specialEffects((new BiomeFog.Builder()).waterColor(4159204).waterFogColor(329011).fogColor(10518688).skyColor(0).ambientMoodSound(CaveSoundSettings.LEGACY_CAVE_SETTINGS).build()).mobSpawnSettings(builder2.build()).generationSettings(builder.build()).build();
    }

    public static BiomeBase endBarrensBiome() {
        BiomeSettingsGeneration.Builder builder = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(WorldGenSurfaceComposites.END);
        return baseEndBiome(builder);
    }

    public static BiomeBase theEndBiome() {
        BiomeSettingsGeneration.Builder builder = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(WorldGenSurfaceComposites.END).addFeature(WorldGenStage.Decoration.SURFACE_STRUCTURES, BiomeDecoratorGroups.END_SPIKE);
        return baseEndBiome(builder);
    }

    public static BiomeBase endMidlandsBiome() {
        BiomeSettingsGeneration.Builder builder = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(WorldGenSurfaceComposites.END).addStructureStart(StructureFeatures.END_CITY);
        return baseEndBiome(builder);
    }

    public static BiomeBase endHighlandsBiome() {
        BiomeSettingsGeneration.Builder builder = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(WorldGenSurfaceComposites.END).addStructureStart(StructureFeatures.END_CITY).addFeature(WorldGenStage.Decoration.SURFACE_STRUCTURES, BiomeDecoratorGroups.END_GATEWAY).addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, BiomeDecoratorGroups.CHORUS_PLANT);
        return baseEndBiome(builder);
    }

    public static BiomeBase smallEndIslandsBiome() {
        BiomeSettingsGeneration.Builder builder = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(WorldGenSurfaceComposites.END).addFeature(WorldGenStage.Decoration.RAW_GENERATION, BiomeDecoratorGroups.END_ISLAND_DECORATED);
        return baseEndBiome(builder);
    }

    public static BiomeBase mushroomFieldsBiome(float depth, float scale) {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        BiomeSettings.mooshroomSpawns(builder);
        BiomeSettingsGeneration.Builder builder2 = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(WorldGenSurfaceComposites.MYCELIUM);
        BiomeSettings.addDefaultOverworldLandStructures(builder2);
        builder2.addStructureStart(StructureFeatures.RUINED_PORTAL_STANDARD);
        BiomeSettings.addDefaultCarvers(builder2);
        BiomeSettings.addDefaultLakes(builder2);
        BiomeSettings.addDefaultCrystalFormations(builder2);
        BiomeSettings.addDefaultMonsterRoom(builder2);
        BiomeSettings.addDefaultUndergroundVariety(builder2);
        BiomeSettings.addDefaultOres(builder2);
        BiomeSettings.addDefaultSoftDisks(builder2);
        BiomeSettings.addMushroomFieldVegetation(builder2);
        BiomeSettings.addDefaultMushrooms(builder2);
        BiomeSettings.addDefaultExtraVegetation(builder2);
        BiomeSettings.addDefaultSprings(builder2);
        BiomeSettings.addSurfaceFreezing(builder2);
        return (new BiomeBase.BiomeBuilder()).precipitation(BiomeBase.Precipitation.RAIN).biomeCategory(BiomeBase.Geography.MUSHROOM).depth(depth).scale(scale).temperature(0.9F).downfall(1.0F).specialEffects((new BiomeFog.Builder()).waterColor(4159204).waterFogColor(329011).fogColor(12638463).skyColor(calculateSkyColor(0.9F)).ambientMoodSound(CaveSoundSettings.LEGACY_CAVE_SETTINGS).build()).mobSpawnSettings(builder.build()).generationSettings(builder2.build()).build();
    }

    private static BiomeBase baseSavannaBiome(float depth, float scale, float temperature, boolean plateau, boolean shattered, BiomeSettingsMobs.Builder spawnSettings) {
        BiomeSettingsGeneration.Builder builder = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(shattered ? WorldGenSurfaceComposites.SHATTERED_SAVANNA : WorldGenSurfaceComposites.GRASS);
        if (!plateau && !shattered) {
            builder.addStructureStart(StructureFeatures.VILLAGE_SAVANNA).addStructureStart(StructureFeatures.PILLAGER_OUTPOST);
        }

        BiomeSettings.addDefaultOverworldLandStructures(builder);
        builder.addStructureStart(plateau ? StructureFeatures.RUINED_PORTAL_MOUNTAIN : StructureFeatures.RUINED_PORTAL_STANDARD);
        BiomeSettings.addDefaultCarvers(builder);
        BiomeSettings.addDefaultLakes(builder);
        BiomeSettings.addDefaultCrystalFormations(builder);
        BiomeSettings.addDefaultMonsterRoom(builder);
        if (!shattered) {
            BiomeSettings.addSavannaGrass(builder);
        }

        BiomeSettings.addDefaultUndergroundVariety(builder);
        BiomeSettings.addDefaultOres(builder);
        BiomeSettings.addDefaultSoftDisks(builder);
        if (shattered) {
            BiomeSettings.addShatteredSavannaTrees(builder);
            BiomeSettings.addDefaultFlowers(builder);
            BiomeSettings.addShatteredSavannaGrass(builder);
        } else {
            BiomeSettings.addSavannaTrees(builder);
            BiomeSettings.addWarmFlowers(builder);
            BiomeSettings.addSavannaExtraGrass(builder);
        }

        BiomeSettings.addDefaultMushrooms(builder);
        BiomeSettings.addDefaultExtraVegetation(builder);
        BiomeSettings.addDefaultSprings(builder);
        BiomeSettings.addSurfaceFreezing(builder);
        return (new BiomeBase.BiomeBuilder()).precipitation(BiomeBase.Precipitation.NONE).biomeCategory(BiomeBase.Geography.SAVANNA).depth(depth).scale(scale).temperature(temperature).downfall(0.0F).specialEffects((new BiomeFog.Builder()).waterColor(4159204).waterFogColor(329011).fogColor(12638463).skyColor(calculateSkyColor(temperature)).ambientMoodSound(CaveSoundSettings.LEGACY_CAVE_SETTINGS).build()).mobSpawnSettings(spawnSettings.build()).generationSettings(builder.build()).build();
    }

    public static BiomeBase savannaBiome(float depth, float scale, float temperature, boolean mountain, boolean shattered) {
        BiomeSettingsMobs.Builder builder = savannaMobs();
        return baseSavannaBiome(depth, scale, temperature, mountain, shattered, builder);
    }

    private static BiomeSettingsMobs.Builder savannaMobs() {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        BiomeSettings.farmAnimals(builder);
        builder.addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.HORSE, 1, 2, 6)).addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.DONKEY, 1, 1, 1));
        BiomeSettings.commonSpawns(builder);
        return builder;
    }

    public static BiomeBase savanaPlateauBiome() {
        BiomeSettingsMobs.Builder builder = savannaMobs();
        builder.addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.LLAMA, 8, 4, 4));
        return baseSavannaBiome(1.5F, 0.025F, 1.0F, true, false, builder);
    }

    private static BiomeBase baseBadlandsBiome(WorldGenSurfaceComposite<WorldGenSurfaceConfigurationBase> configuredSurfaceBuilder, float depth, float scale, boolean plateau, boolean wooded) {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        BiomeSettings.commonSpawns(builder);
        BiomeSettingsGeneration.Builder builder2 = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(configuredSurfaceBuilder);
        BiomeSettings.addDefaultOverworldLandMesaStructures(builder2);
        builder2.addStructureStart(plateau ? StructureFeatures.RUINED_PORTAL_MOUNTAIN : StructureFeatures.RUINED_PORTAL_STANDARD);
        BiomeSettings.addDefaultCarvers(builder2);
        BiomeSettings.addDefaultLakes(builder2);
        BiomeSettings.addDefaultCrystalFormations(builder2);
        BiomeSettings.addDefaultMonsterRoom(builder2);
        BiomeSettings.addDefaultUndergroundVariety(builder2);
        BiomeSettings.addDefaultOres(builder2);
        BiomeSettings.addExtraGold(builder2);
        BiomeSettings.addDefaultSoftDisks(builder2);
        if (wooded) {
            BiomeSettings.addBadlandsTrees(builder2);
        }

        BiomeSettings.addBadlandGrass(builder2);
        BiomeSettings.addDefaultMushrooms(builder2);
        BiomeSettings.addBadlandExtraVegetation(builder2);
        BiomeSettings.addDefaultSprings(builder2);
        BiomeSettings.addSurfaceFreezing(builder2);
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
        StructureFeature<?, ?> configuredStructureFeature = warm ? StructureFeatures.OCEAN_RUIN_WARM : StructureFeatures.OCEAN_RUIN_COLD;
        if (bl) {
            if (deep) {
                builder.addStructureStart(StructureFeatures.OCEAN_MONUMENT);
            }

            BiomeSettings.addDefaultOverworldOceanStructures(builder);
            builder.addStructureStart(configuredStructureFeature);
        } else {
            builder.addStructureStart(configuredStructureFeature);
            if (deep) {
                builder.addStructureStart(StructureFeatures.OCEAN_MONUMENT);
            }

            BiomeSettings.addDefaultOverworldOceanStructures(builder);
        }

        builder.addStructureStart(StructureFeatures.RUINED_PORTAL_OCEAN);
        BiomeSettings.addOceanCarvers(builder);
        BiomeSettings.addDefaultLakes(builder);
        BiomeSettings.addDefaultCrystalFormations(builder);
        BiomeSettings.addDefaultMonsterRoom(builder);
        BiomeSettings.addDefaultUndergroundVariety(builder, true);
        BiomeSettings.addDefaultOres(builder);
        BiomeSettings.addDefaultSoftDisks(builder);
        BiomeSettings.addWaterTrees(builder);
        BiomeSettings.addDefaultFlowers(builder);
        BiomeSettings.addDefaultGrass(builder);
        BiomeSettings.addDefaultMushrooms(builder);
        BiomeSettings.addDefaultExtraVegetation(builder);
        BiomeSettings.addDefaultSprings(builder);
        return builder;
    }

    public static BiomeBase coldOceanBiome(boolean deep) {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        BiomeSettings.oceanSpawns(builder, 3, 4, 15);
        builder.addSpawn(EnumCreatureType.WATER_AMBIENT, new BiomeSettingsMobs.SpawnerData(EntityTypes.SALMON, 15, 1, 5));
        boolean bl = !deep;
        BiomeSettingsGeneration.Builder builder2 = baseOceanGeneration(WorldGenSurfaceComposites.GRASS, deep, false, bl);
        builder2.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, deep ? BiomeDecoratorGroups.SEAGRASS_DEEP_COLD : BiomeDecoratorGroups.SEAGRASS_COLD);
        BiomeSettings.addDefaultSeagrass(builder2);
        BiomeSettings.addColdOceanExtraVegetation(builder2);
        BiomeSettings.addSurfaceFreezing(builder2);
        return baseOceanBiome(builder, 4020182, 329011, deep, builder2);
    }

    public static BiomeBase oceanBiome(boolean deep) {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        BiomeSettings.oceanSpawns(builder, 1, 4, 10);
        builder.addSpawn(EnumCreatureType.WATER_CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.DOLPHIN, 1, 1, 2));
        BiomeSettingsGeneration.Builder builder2 = baseOceanGeneration(WorldGenSurfaceComposites.GRASS, deep, false, true);
        builder2.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, deep ? BiomeDecoratorGroups.SEAGRASS_DEEP : BiomeDecoratorGroups.SEAGRASS_NORMAL);
        BiomeSettings.addDefaultSeagrass(builder2);
        BiomeSettings.addColdOceanExtraVegetation(builder2);
        BiomeSettings.addSurfaceFreezing(builder2);
        return baseOceanBiome(builder, 4159204, 329011, deep, builder2);
    }

    public static BiomeBase lukeWarmOceanBiome(boolean deep) {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        if (deep) {
            BiomeSettings.oceanSpawns(builder, 8, 4, 8);
        } else {
            BiomeSettings.oceanSpawns(builder, 10, 2, 15);
        }

        builder.addSpawn(EnumCreatureType.WATER_AMBIENT, new BiomeSettingsMobs.SpawnerData(EntityTypes.PUFFERFISH, 5, 1, 3)).addSpawn(EnumCreatureType.WATER_AMBIENT, new BiomeSettingsMobs.SpawnerData(EntityTypes.TROPICAL_FISH, 25, 8, 8)).addSpawn(EnumCreatureType.WATER_CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.DOLPHIN, 2, 1, 2));
        BiomeSettingsGeneration.Builder builder2 = baseOceanGeneration(WorldGenSurfaceComposites.OCEAN_SAND, deep, true, false);
        builder2.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, deep ? BiomeDecoratorGroups.SEAGRASS_DEEP_WARM : BiomeDecoratorGroups.SEAGRASS_WARM);
        if (deep) {
            BiomeSettings.addDefaultSeagrass(builder2);
        }

        BiomeSettings.addLukeWarmKelp(builder2);
        BiomeSettings.addSurfaceFreezing(builder2);
        return baseOceanBiome(builder, 4566514, 267827, deep, builder2);
    }

    public static BiomeBase warmOceanBiome() {
        BiomeSettingsMobs.Builder builder = (new BiomeSettingsMobs.Builder()).addSpawn(EnumCreatureType.WATER_AMBIENT, new BiomeSettingsMobs.SpawnerData(EntityTypes.PUFFERFISH, 15, 1, 3));
        BiomeSettings.warmOceanSpawns(builder, 10, 4);
        BiomeSettingsGeneration.Builder builder2 = baseOceanGeneration(WorldGenSurfaceComposites.FULL_SAND, false, true, false).addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, BiomeDecoratorGroups.WARM_OCEAN_VEGETATION).addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, BiomeDecoratorGroups.SEAGRASS_WARM).addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, BiomeDecoratorGroups.SEA_PICKLE);
        BiomeSettings.addSurfaceFreezing(builder2);
        return baseOceanBiome(builder, 4445678, 270131, false, builder2);
    }

    public static BiomeBase deepWarmOceanBiome() {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        BiomeSettings.warmOceanSpawns(builder, 5, 1);
        builder.addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.DROWNED, 5, 1, 1));
        BiomeSettingsGeneration.Builder builder2 = baseOceanGeneration(WorldGenSurfaceComposites.FULL_SAND, true, true, false).addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, BiomeDecoratorGroups.SEAGRASS_DEEP_WARM);
        BiomeSettings.addDefaultSeagrass(builder2);
        BiomeSettings.addSurfaceFreezing(builder2);
        return baseOceanBiome(builder, 4445678, 270131, true, builder2);
    }

    public static BiomeBase frozenOceanBiome(boolean monument) {
        BiomeSettingsMobs.Builder builder = (new BiomeSettingsMobs.Builder()).addSpawn(EnumCreatureType.WATER_CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.SQUID, 1, 1, 4)).addSpawn(EnumCreatureType.WATER_AMBIENT, new BiomeSettingsMobs.SpawnerData(EntityTypes.SALMON, 15, 1, 5)).addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.POLAR_BEAR, 1, 1, 2));
        BiomeSettings.commonSpawns(builder);
        builder.addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.DROWNED, 5, 1, 1));
        float f = monument ? 0.5F : 0.0F;
        BiomeSettingsGeneration.Builder builder2 = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(WorldGenSurfaceComposites.FROZEN_OCEAN);
        builder2.addStructureStart(StructureFeatures.OCEAN_RUIN_COLD);
        if (monument) {
            builder2.addStructureStart(StructureFeatures.OCEAN_MONUMENT);
        }

        BiomeSettings.addDefaultOverworldOceanStructures(builder2);
        builder2.addStructureStart(StructureFeatures.RUINED_PORTAL_OCEAN);
        BiomeSettings.addOceanCarvers(builder2);
        BiomeSettings.addDefaultLakes(builder2);
        BiomeSettings.addIcebergs(builder2);
        BiomeSettings.addDefaultCrystalFormations(builder2);
        BiomeSettings.addDefaultMonsterRoom(builder2);
        BiomeSettings.addBlueIce(builder2);
        BiomeSettings.addDefaultUndergroundVariety(builder2, true);
        BiomeSettings.addDefaultOres(builder2);
        BiomeSettings.addDefaultSoftDisks(builder2);
        BiomeSettings.addWaterTrees(builder2);
        BiomeSettings.addDefaultFlowers(builder2);
        BiomeSettings.addDefaultGrass(builder2);
        BiomeSettings.addDefaultMushrooms(builder2);
        BiomeSettings.addDefaultExtraVegetation(builder2);
        BiomeSettings.addDefaultSprings(builder2);
        BiomeSettings.addSurfaceFreezing(builder2);
        return (new BiomeBase.BiomeBuilder()).precipitation(monument ? BiomeBase.Precipitation.RAIN : BiomeBase.Precipitation.SNOW).biomeCategory(BiomeBase.Geography.OCEAN).depth(monument ? -1.8F : -1.0F).scale(0.1F).temperature(f).temperatureAdjustment(BiomeBase.TemperatureModifier.FROZEN).downfall(0.5F).specialEffects((new BiomeFog.Builder()).waterColor(3750089).waterFogColor(329011).fogColor(12638463).skyColor(calculateSkyColor(f)).ambientMoodSound(CaveSoundSettings.LEGACY_CAVE_SETTINGS).build()).mobSpawnSettings(builder.build()).generationSettings(builder2.build()).build();
    }

    private static BiomeBase baseForestBiome(float depth, float scale, boolean flower, BiomeSettingsMobs.Builder spawnSettings) {
        BiomeSettingsGeneration.Builder builder = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(WorldGenSurfaceComposites.GRASS);
        BiomeSettings.addDefaultOverworldLandStructures(builder);
        builder.addStructureStart(StructureFeatures.RUINED_PORTAL_STANDARD);
        BiomeSettings.addDefaultCarvers(builder);
        BiomeSettings.addDefaultLakes(builder);
        BiomeSettings.addDefaultCrystalFormations(builder);
        BiomeSettings.addDefaultMonsterRoom(builder);
        if (flower) {
            builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, BiomeDecoratorGroups.FOREST_FLOWER_VEGETATION_COMMON);
        } else {
            BiomeSettings.addForestFlowers(builder);
        }

        BiomeSettings.addDefaultUndergroundVariety(builder);
        BiomeSettings.addDefaultOres(builder);
        BiomeSettings.addDefaultSoftDisks(builder);
        if (flower) {
            builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, BiomeDecoratorGroups.FOREST_FLOWER_TREES);
            builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, BiomeDecoratorGroups.FLOWER_FOREST);
            BiomeSettings.addDefaultGrass(builder);
        } else {
            BiomeSettings.addOtherBirchTrees(builder);
            BiomeSettings.addDefaultFlowers(builder);
            BiomeSettings.addForestGrass(builder);
        }

        BiomeSettings.addDefaultMushrooms(builder);
        BiomeSettings.addDefaultExtraVegetation(builder);
        BiomeSettings.addDefaultSprings(builder);
        BiomeSettings.addSurfaceFreezing(builder);
        return (new BiomeBase.BiomeBuilder()).precipitation(BiomeBase.Precipitation.RAIN).biomeCategory(BiomeBase.Geography.FOREST).depth(depth).scale(scale).temperature(0.7F).downfall(0.8F).specialEffects((new BiomeFog.Builder()).waterColor(4159204).waterFogColor(329011).fogColor(12638463).skyColor(calculateSkyColor(0.7F)).ambientMoodSound(CaveSoundSettings.LEGACY_CAVE_SETTINGS).build()).mobSpawnSettings(spawnSettings.build()).generationSettings(builder.build()).build();
    }

    private static BiomeSettingsMobs.Builder defaultSpawns() {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        BiomeSettings.farmAnimals(builder);
        BiomeSettings.commonSpawns(builder);
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
        BiomeSettings.farmAnimals(builder);
        builder.addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.WOLF, 8, 4, 4)).addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.RABBIT, 4, 2, 3)).addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.FOX, 8, 2, 4));
        if (!snowy && !mountains) {
            builder.setPlayerCanSpawn();
        }

        BiomeSettings.commonSpawns(builder);
        float f = snowy ? -0.5F : 0.25F;
        BiomeSettingsGeneration.Builder builder2 = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(WorldGenSurfaceComposites.GRASS);
        if (villages) {
            builder2.addStructureStart(StructureFeatures.VILLAGE_TAIGA);
            builder2.addStructureStart(StructureFeatures.PILLAGER_OUTPOST);
        }

        if (igloos) {
            builder2.addStructureStart(StructureFeatures.IGLOO);
        }

        BiomeSettings.addDefaultOverworldLandStructures(builder2);
        builder2.addStructureStart(mountains ? StructureFeatures.RUINED_PORTAL_MOUNTAIN : StructureFeatures.RUINED_PORTAL_STANDARD);
        BiomeSettings.addDefaultCarvers(builder2);
        BiomeSettings.addDefaultLakes(builder2);
        BiomeSettings.addDefaultCrystalFormations(builder2);
        BiomeSettings.addDefaultMonsterRoom(builder2);
        BiomeSettings.addFerns(builder2);
        BiomeSettings.addDefaultUndergroundVariety(builder2);
        BiomeSettings.addDefaultOres(builder2);
        BiomeSettings.addDefaultSoftDisks(builder2);
        BiomeSettings.addTaigaTrees(builder2);
        BiomeSettings.addDefaultFlowers(builder2);
        BiomeSettings.addTaigaGrass(builder2);
        BiomeSettings.addDefaultMushrooms(builder2);
        BiomeSettings.addDefaultExtraVegetation(builder2);
        BiomeSettings.addDefaultSprings(builder2);
        if (snowy) {
            BiomeSettings.addBerryBushes(builder2);
        } else {
            BiomeSettings.addSparseBerryBushes(builder2);
        }

        BiomeSettings.addSurfaceFreezing(builder2);
        return (new BiomeBase.BiomeBuilder()).precipitation(snowy ? BiomeBase.Precipitation.SNOW : BiomeBase.Precipitation.RAIN).biomeCategory(BiomeBase.Geography.TAIGA).depth(depth).scale(scale).temperature(f).downfall(snowy ? 0.4F : 0.8F).specialEffects((new BiomeFog.Builder()).waterColor(snowy ? 4020182 : 4159204).waterFogColor(329011).fogColor(12638463).skyColor(calculateSkyColor(f)).ambientMoodSound(CaveSoundSettings.LEGACY_CAVE_SETTINGS).build()).mobSpawnSettings(builder.build()).generationSettings(builder2.build()).build();
    }

    public static BiomeBase darkForestBiome(float depth, float scale, boolean hills) {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        BiomeSettings.farmAnimals(builder);
        BiomeSettings.commonSpawns(builder);
        BiomeSettingsGeneration.Builder builder2 = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(WorldGenSurfaceComposites.GRASS);
        builder2.addStructureStart(StructureFeatures.WOODLAND_MANSION);
        BiomeSettings.addDefaultOverworldLandStructures(builder2);
        builder2.addStructureStart(StructureFeatures.RUINED_PORTAL_STANDARD);
        BiomeSettings.addDefaultCarvers(builder2);
        BiomeSettings.addDefaultLakes(builder2);
        BiomeSettings.addDefaultCrystalFormations(builder2);
        BiomeSettings.addDefaultMonsterRoom(builder2);
        builder2.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, hills ? BiomeDecoratorGroups.DARK_FOREST_VEGETATION_RED : BiomeDecoratorGroups.DARK_FOREST_VEGETATION_BROWN);
        BiomeSettings.addForestFlowers(builder2);
        BiomeSettings.addDefaultUndergroundVariety(builder2);
        BiomeSettings.addDefaultOres(builder2);
        BiomeSettings.addDefaultSoftDisks(builder2);
        BiomeSettings.addDefaultFlowers(builder2);
        BiomeSettings.addForestGrass(builder2);
        BiomeSettings.addDefaultMushrooms(builder2);
        BiomeSettings.addDefaultExtraVegetation(builder2);
        BiomeSettings.addDefaultSprings(builder2);
        BiomeSettings.addSurfaceFreezing(builder2);
        return (new BiomeBase.BiomeBuilder()).precipitation(BiomeBase.Precipitation.RAIN).biomeCategory(BiomeBase.Geography.FOREST).depth(depth).scale(scale).temperature(0.7F).downfall(0.8F).specialEffects((new BiomeFog.Builder()).waterColor(4159204).waterFogColor(329011).fogColor(12638463).skyColor(calculateSkyColor(0.7F)).grassColorModifier(BiomeFog.GrassColor.DARK_FOREST).ambientMoodSound(CaveSoundSettings.LEGACY_CAVE_SETTINGS).build()).mobSpawnSettings(builder.build()).generationSettings(builder2.build()).build();
    }

    public static BiomeBase swampBiome(float depth, float scale, boolean hills) {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        BiomeSettings.farmAnimals(builder);
        BiomeSettings.commonSpawns(builder);
        builder.addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.SLIME, 1, 1, 1));
        BiomeSettingsGeneration.Builder builder2 = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(WorldGenSurfaceComposites.SWAMP);
        if (!hills) {
            builder2.addStructureStart(StructureFeatures.SWAMP_HUT);
        }

        builder2.addStructureStart(StructureFeatures.MINESHAFT);
        builder2.addStructureStart(StructureFeatures.RUINED_PORTAL_SWAMP);
        BiomeSettings.addDefaultCarvers(builder2);
        if (!hills) {
            BiomeSettings.addFossilDecoration(builder2);
        }

        BiomeSettings.addDefaultLakes(builder2);
        BiomeSettings.addDefaultCrystalFormations(builder2);
        BiomeSettings.addDefaultMonsterRoom(builder2);
        BiomeSettings.addDefaultUndergroundVariety(builder2);
        BiomeSettings.addDefaultOres(builder2);
        BiomeSettings.addSwampClayDisk(builder2);
        BiomeSettings.addSwampVegetation(builder2);
        BiomeSettings.addDefaultMushrooms(builder2);
        BiomeSettings.addSwampExtraVegetation(builder2);
        BiomeSettings.addDefaultSprings(builder2);
        if (hills) {
            BiomeSettings.addFossilDecoration(builder2);
        } else {
            builder2.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, BiomeDecoratorGroups.SEAGRASS_SWAMP);
        }

        BiomeSettings.addSurfaceFreezing(builder2);
        return (new BiomeBase.BiomeBuilder()).precipitation(BiomeBase.Precipitation.RAIN).biomeCategory(BiomeBase.Geography.SWAMP).depth(depth).scale(scale).temperature(0.8F).downfall(0.9F).specialEffects((new BiomeFog.Builder()).waterColor(6388580).waterFogColor(2302743).fogColor(12638463).skyColor(calculateSkyColor(0.8F)).foliageColorOverride(6975545).grassColorModifier(BiomeFog.GrassColor.SWAMP).ambientMoodSound(CaveSoundSettings.LEGACY_CAVE_SETTINGS).build()).mobSpawnSettings(builder.build()).generationSettings(builder2.build()).build();
    }

    public static BiomeBase tundraBiome(float depth, float scale, boolean iceSpikes, boolean mountains) {
        BiomeSettingsMobs.Builder builder = (new BiomeSettingsMobs.Builder()).creatureGenerationProbability(0.07F);
        BiomeSettings.snowySpawns(builder);
        BiomeSettingsGeneration.Builder builder2 = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(iceSpikes ? WorldGenSurfaceComposites.ICE_SPIKES : WorldGenSurfaceComposites.GRASS);
        if (!iceSpikes && !mountains) {
            builder2.addStructureStart(StructureFeatures.VILLAGE_SNOWY).addStructureStart(StructureFeatures.IGLOO);
        }

        BiomeSettings.addDefaultOverworldLandStructures(builder2);
        if (!iceSpikes && !mountains) {
            builder2.addStructureStart(StructureFeatures.PILLAGER_OUTPOST);
        }

        builder2.addStructureStart(mountains ? StructureFeatures.RUINED_PORTAL_MOUNTAIN : StructureFeatures.RUINED_PORTAL_STANDARD);
        BiomeSettings.addDefaultCarvers(builder2);
        BiomeSettings.addDefaultLakes(builder2);
        BiomeSettings.addDefaultCrystalFormations(builder2);
        BiomeSettings.addDefaultMonsterRoom(builder2);
        if (iceSpikes) {
            builder2.addFeature(WorldGenStage.Decoration.SURFACE_STRUCTURES, BiomeDecoratorGroups.ICE_SPIKE);
            builder2.addFeature(WorldGenStage.Decoration.SURFACE_STRUCTURES, BiomeDecoratorGroups.ICE_PATCH);
        }

        BiomeSettings.addDefaultUndergroundVariety(builder2);
        BiomeSettings.addDefaultOres(builder2);
        BiomeSettings.addDefaultSoftDisks(builder2);
        BiomeSettings.addSnowyTrees(builder2);
        BiomeSettings.addDefaultFlowers(builder2);
        BiomeSettings.addDefaultGrass(builder2);
        BiomeSettings.addDefaultMushrooms(builder2);
        BiomeSettings.addDefaultExtraVegetation(builder2);
        BiomeSettings.addDefaultSprings(builder2);
        BiomeSettings.addSurfaceFreezing(builder2);
        return (new BiomeBase.BiomeBuilder()).precipitation(BiomeBase.Precipitation.SNOW).biomeCategory(BiomeBase.Geography.ICY).depth(depth).scale(scale).temperature(0.0F).downfall(0.5F).specialEffects((new BiomeFog.Builder()).waterColor(4159204).waterFogColor(329011).fogColor(12638463).skyColor(calculateSkyColor(0.0F)).ambientMoodSound(CaveSoundSettings.LEGACY_CAVE_SETTINGS).build()).mobSpawnSettings(builder.build()).generationSettings(builder2.build()).build();
    }

    public static BiomeBase riverBiome(float depth, float scale, float temperature, int waterColor, boolean frozen) {
        BiomeSettingsMobs.Builder builder = (new BiomeSettingsMobs.Builder()).addSpawn(EnumCreatureType.WATER_CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.SQUID, 2, 1, 4)).addSpawn(EnumCreatureType.WATER_AMBIENT, new BiomeSettingsMobs.SpawnerData(EntityTypes.SALMON, 5, 1, 5));
        BiomeSettings.commonSpawns(builder);
        builder.addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.DROWNED, frozen ? 1 : 100, 1, 1));
        BiomeSettingsGeneration.Builder builder2 = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(WorldGenSurfaceComposites.GRASS);
        builder2.addStructureStart(StructureFeatures.MINESHAFT);
        builder2.addStructureStart(StructureFeatures.RUINED_PORTAL_STANDARD);
        BiomeSettings.addDefaultCarvers(builder2);
        BiomeSettings.addDefaultLakes(builder2);
        BiomeSettings.addDefaultCrystalFormations(builder2);
        BiomeSettings.addDefaultMonsterRoom(builder2);
        BiomeSettings.addDefaultUndergroundVariety(builder2);
        BiomeSettings.addDefaultOres(builder2);
        BiomeSettings.addDefaultSoftDisks(builder2);
        BiomeSettings.addWaterTrees(builder2);
        BiomeSettings.addDefaultFlowers(builder2);
        BiomeSettings.addDefaultGrass(builder2);
        BiomeSettings.addDefaultMushrooms(builder2);
        BiomeSettings.addDefaultExtraVegetation(builder2);
        BiomeSettings.addDefaultSprings(builder2);
        if (!frozen) {
            builder2.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, BiomeDecoratorGroups.SEAGRASS_RIVER);
        }

        BiomeSettings.addSurfaceFreezing(builder2);
        return (new BiomeBase.BiomeBuilder()).precipitation(frozen ? BiomeBase.Precipitation.SNOW : BiomeBase.Precipitation.RAIN).biomeCategory(BiomeBase.Geography.RIVER).depth(depth).scale(scale).temperature(temperature).downfall(0.5F).specialEffects((new BiomeFog.Builder()).waterColor(waterColor).waterFogColor(329011).fogColor(12638463).skyColor(calculateSkyColor(temperature)).ambientMoodSound(CaveSoundSettings.LEGACY_CAVE_SETTINGS).build()).mobSpawnSettings(builder.build()).generationSettings(builder2.build()).build();
    }

    public static BiomeBase beachBiome(float depth, float scale, float temperature, float downfall, int waterColor, boolean snowy, boolean stony) {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        if (!stony && !snowy) {
            builder.addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.TURTLE, 5, 2, 5));
        }

        BiomeSettings.commonSpawns(builder);
        BiomeSettingsGeneration.Builder builder2 = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(stony ? WorldGenSurfaceComposites.STONE : WorldGenSurfaceComposites.DESERT);
        if (stony) {
            BiomeSettings.addDefaultOverworldLandStructures(builder2);
        } else {
            builder2.addStructureStart(StructureFeatures.MINESHAFT);
            builder2.addStructureStart(StructureFeatures.BURIED_TREASURE);
            builder2.addStructureStart(StructureFeatures.SHIPWRECH_BEACHED);
        }

        builder2.addStructureStart(stony ? StructureFeatures.RUINED_PORTAL_MOUNTAIN : StructureFeatures.RUINED_PORTAL_STANDARD);
        BiomeSettings.addDefaultCarvers(builder2);
        BiomeSettings.addDefaultLakes(builder2);
        BiomeSettings.addDefaultCrystalFormations(builder2);
        BiomeSettings.addDefaultMonsterRoom(builder2);
        BiomeSettings.addDefaultUndergroundVariety(builder2);
        BiomeSettings.addDefaultOres(builder2);
        BiomeSettings.addDefaultSoftDisks(builder2);
        BiomeSettings.addDefaultFlowers(builder2);
        BiomeSettings.addDefaultGrass(builder2);
        BiomeSettings.addDefaultMushrooms(builder2);
        BiomeSettings.addDefaultExtraVegetation(builder2);
        BiomeSettings.addDefaultSprings(builder2);
        BiomeSettings.addSurfaceFreezing(builder2);
        return (new BiomeBase.BiomeBuilder()).precipitation(snowy ? BiomeBase.Precipitation.SNOW : BiomeBase.Precipitation.RAIN).biomeCategory(stony ? BiomeBase.Geography.NONE : BiomeBase.Geography.BEACH).depth(depth).scale(scale).temperature(temperature).downfall(downfall).specialEffects((new BiomeFog.Builder()).waterColor(waterColor).waterFogColor(329011).fogColor(12638463).skyColor(calculateSkyColor(temperature)).ambientMoodSound(CaveSoundSettings.LEGACY_CAVE_SETTINGS).build()).mobSpawnSettings(builder.build()).generationSettings(builder2.build()).build();
    }

    public static BiomeBase theVoidBiome() {
        BiomeSettingsGeneration.Builder builder = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(WorldGenSurfaceComposites.NOPE);
        builder.addFeature(WorldGenStage.Decoration.TOP_LAYER_MODIFICATION, BiomeDecoratorGroups.VOID_START_PLATFORM);
        return (new BiomeBase.BiomeBuilder()).precipitation(BiomeBase.Precipitation.NONE).biomeCategory(BiomeBase.Geography.NONE).depth(0.1F).scale(0.2F).temperature(0.5F).downfall(0.5F).specialEffects((new BiomeFog.Builder()).waterColor(4159204).waterFogColor(329011).fogColor(12638463).skyColor(calculateSkyColor(0.5F)).ambientMoodSound(CaveSoundSettings.LEGACY_CAVE_SETTINGS).build()).mobSpawnSettings(BiomeSettingsMobs.EMPTY).generationSettings(builder.build()).build();
    }

    public static BiomeBase netherWastesBiome() {
        BiomeSettingsMobs mobSpawnSettings = (new BiomeSettingsMobs.Builder()).addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.GHAST, 50, 4, 4)).addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.ZOMBIFIED_PIGLIN, 100, 4, 4)).addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.MAGMA_CUBE, 2, 4, 4)).addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.ENDERMAN, 1, 4, 4)).addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.PIGLIN, 15, 4, 4)).addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.STRIDER, 60, 1, 2)).build();
        BiomeSettingsGeneration.Builder builder = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(WorldGenSurfaceComposites.NETHER).addStructureStart(StructureFeatures.RUINED_PORTAL_NETHER).addStructureStart(StructureFeatures.NETHER_BRIDGE).addStructureStart(StructureFeatures.BASTION_REMNANT).addCarver(WorldGenStage.Features.AIR, WorldGenCarvers.NETHER_CAVE).addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, BiomeDecoratorGroups.SPRING_LAVA);
        BiomeSettings.addDefaultMushrooms(builder);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, BiomeDecoratorGroups.SPRING_OPEN).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, BiomeDecoratorGroups.PATCH_FIRE).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, BiomeDecoratorGroups.PATCH_SOUL_FIRE).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, BiomeDecoratorGroups.GLOWSTONE_EXTRA).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, BiomeDecoratorGroups.GLOWSTONE).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, BiomeDecoratorGroups.BROWN_MUSHROOM_NETHER).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, BiomeDecoratorGroups.RED_MUSHROOM_NETHER).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, BiomeDecoratorGroups.ORE_MAGMA).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, BiomeDecoratorGroups.SPRING_CLOSED);
        BiomeSettings.addNetherDefaultOres(builder);
        return (new BiomeBase.BiomeBuilder()).precipitation(BiomeBase.Precipitation.NONE).biomeCategory(BiomeBase.Geography.NETHER).depth(0.1F).scale(0.2F).temperature(2.0F).downfall(0.0F).specialEffects((new BiomeFog.Builder()).waterColor(4159204).waterFogColor(329011).fogColor(3344392).skyColor(calculateSkyColor(2.0F)).ambientLoopSound(SoundEffects.AMBIENT_NETHER_WASTES_LOOP).ambientMoodSound(new CaveSoundSettings(SoundEffects.AMBIENT_NETHER_WASTES_MOOD, 6000, 8, 2.0D)).ambientAdditionsSound(new CaveSound(SoundEffects.AMBIENT_NETHER_WASTES_ADDITIONS, 0.0111D)).backgroundMusic(Musics.createGameMusic(SoundEffects.MUSIC_BIOME_NETHER_WASTES)).build()).mobSpawnSettings(mobSpawnSettings).generationSettings(builder.build()).build();
    }

    public static BiomeBase soulSandValleyBiome() {
        double d = 0.7D;
        double e = 0.15D;
        BiomeSettingsMobs mobSpawnSettings = (new BiomeSettingsMobs.Builder()).addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.SKELETON, 20, 5, 5)).addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.GHAST, 50, 4, 4)).addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.ENDERMAN, 1, 4, 4)).addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.STRIDER, 60, 1, 2)).addMobCharge(EntityTypes.SKELETON, 0.7D, 0.15D).addMobCharge(EntityTypes.GHAST, 0.7D, 0.15D).addMobCharge(EntityTypes.ENDERMAN, 0.7D, 0.15D).addMobCharge(EntityTypes.STRIDER, 0.7D, 0.15D).build();
        BiomeSettingsGeneration.Builder builder = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(WorldGenSurfaceComposites.SOUL_SAND_VALLEY).addStructureStart(StructureFeatures.NETHER_BRIDGE).addStructureStart(StructureFeatures.NETHER_FOSSIL).addStructureStart(StructureFeatures.RUINED_PORTAL_NETHER).addStructureStart(StructureFeatures.BASTION_REMNANT).addCarver(WorldGenStage.Features.AIR, WorldGenCarvers.NETHER_CAVE).addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, BiomeDecoratorGroups.SPRING_LAVA).addFeature(WorldGenStage.Decoration.LOCAL_MODIFICATIONS, BiomeDecoratorGroups.BASALT_PILLAR).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, BiomeDecoratorGroups.SPRING_OPEN).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, BiomeDecoratorGroups.GLOWSTONE_EXTRA).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, BiomeDecoratorGroups.GLOWSTONE).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, BiomeDecoratorGroups.PATCH_CRIMSON_ROOTS).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, BiomeDecoratorGroups.PATCH_FIRE).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, BiomeDecoratorGroups.PATCH_SOUL_FIRE).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, BiomeDecoratorGroups.ORE_MAGMA).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, BiomeDecoratorGroups.SPRING_CLOSED).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, BiomeDecoratorGroups.ORE_SOUL_SAND);
        BiomeSettings.addNetherDefaultOres(builder);
        return (new BiomeBase.BiomeBuilder()).precipitation(BiomeBase.Precipitation.NONE).biomeCategory(BiomeBase.Geography.NETHER).depth(0.1F).scale(0.2F).temperature(2.0F).downfall(0.0F).specialEffects((new BiomeFog.Builder()).waterColor(4159204).waterFogColor(329011).fogColor(1787717).skyColor(calculateSkyColor(2.0F)).ambientParticle(new BiomeParticles(Particles.ASH, 0.00625F)).ambientLoopSound(SoundEffects.AMBIENT_SOUL_SAND_VALLEY_LOOP).ambientMoodSound(new CaveSoundSettings(SoundEffects.AMBIENT_SOUL_SAND_VALLEY_MOOD, 6000, 8, 2.0D)).ambientAdditionsSound(new CaveSound(SoundEffects.AMBIENT_SOUL_SAND_VALLEY_ADDITIONS, 0.0111D)).backgroundMusic(Musics.createGameMusic(SoundEffects.MUSIC_BIOME_SOUL_SAND_VALLEY)).build()).mobSpawnSettings(mobSpawnSettings).generationSettings(builder.build()).build();
    }

    public static BiomeBase basaltDeltasBiome() {
        BiomeSettingsMobs mobSpawnSettings = (new BiomeSettingsMobs.Builder()).addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.GHAST, 40, 1, 1)).addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.MAGMA_CUBE, 100, 2, 5)).addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.STRIDER, 60, 1, 2)).build();
        BiomeSettingsGeneration.Builder builder = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(WorldGenSurfaceComposites.BASALT_DELTAS).addStructureStart(StructureFeatures.RUINED_PORTAL_NETHER).addCarver(WorldGenStage.Features.AIR, WorldGenCarvers.NETHER_CAVE).addStructureStart(StructureFeatures.NETHER_BRIDGE).addFeature(WorldGenStage.Decoration.SURFACE_STRUCTURES, BiomeDecoratorGroups.DELTA).addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, BiomeDecoratorGroups.SPRING_LAVA_DOUBLE).addFeature(WorldGenStage.Decoration.SURFACE_STRUCTURES, BiomeDecoratorGroups.SMALL_BASALT_COLUMNS).addFeature(WorldGenStage.Decoration.SURFACE_STRUCTURES, BiomeDecoratorGroups.LARGE_BASALT_COLUMNS).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, BiomeDecoratorGroups.BASALT_BLOBS).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, BiomeDecoratorGroups.BLACKSTONE_BLOBS).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, BiomeDecoratorGroups.SPRING_DELTA).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, BiomeDecoratorGroups.PATCH_FIRE).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, BiomeDecoratorGroups.PATCH_SOUL_FIRE).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, BiomeDecoratorGroups.GLOWSTONE_EXTRA).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, BiomeDecoratorGroups.GLOWSTONE).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, BiomeDecoratorGroups.BROWN_MUSHROOM_NETHER).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, BiomeDecoratorGroups.RED_MUSHROOM_NETHER).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, BiomeDecoratorGroups.ORE_MAGMA).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, BiomeDecoratorGroups.SPRING_CLOSED_DOUBLE).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, BiomeDecoratorGroups.ORE_GOLD_DELTAS).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, BiomeDecoratorGroups.ORE_QUARTZ_DELTAS);
        BiomeSettings.addAncientDebris(builder);
        return (new BiomeBase.BiomeBuilder()).precipitation(BiomeBase.Precipitation.NONE).biomeCategory(BiomeBase.Geography.NETHER).depth(0.1F).scale(0.2F).temperature(2.0F).downfall(0.0F).specialEffects((new BiomeFog.Builder()).waterColor(4159204).waterFogColor(4341314).fogColor(6840176).skyColor(calculateSkyColor(2.0F)).ambientParticle(new BiomeParticles(Particles.WHITE_ASH, 0.118093334F)).ambientLoopSound(SoundEffects.AMBIENT_BASALT_DELTAS_LOOP).ambientMoodSound(new CaveSoundSettings(SoundEffects.AMBIENT_BASALT_DELTAS_MOOD, 6000, 8, 2.0D)).ambientAdditionsSound(new CaveSound(SoundEffects.AMBIENT_BASALT_DELTAS_ADDITIONS, 0.0111D)).backgroundMusic(Musics.createGameMusic(SoundEffects.MUSIC_BIOME_BASALT_DELTAS)).build()).mobSpawnSettings(mobSpawnSettings).generationSettings(builder.build()).build();
    }

    public static BiomeBase crimsonForestBiome() {
        BiomeSettingsMobs mobSpawnSettings = (new BiomeSettingsMobs.Builder()).addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.ZOMBIFIED_PIGLIN, 1, 2, 4)).addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.HOGLIN, 9, 3, 4)).addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.PIGLIN, 5, 3, 4)).addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.STRIDER, 60, 1, 2)).build();
        BiomeSettingsGeneration.Builder builder = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(WorldGenSurfaceComposites.CRIMSON_FOREST).addStructureStart(StructureFeatures.RUINED_PORTAL_NETHER).addCarver(WorldGenStage.Features.AIR, WorldGenCarvers.NETHER_CAVE).addStructureStart(StructureFeatures.NETHER_BRIDGE).addStructureStart(StructureFeatures.BASTION_REMNANT).addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, BiomeDecoratorGroups.SPRING_LAVA);
        BiomeSettings.addDefaultMushrooms(builder);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, BiomeDecoratorGroups.SPRING_OPEN).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, BiomeDecoratorGroups.PATCH_FIRE).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, BiomeDecoratorGroups.GLOWSTONE_EXTRA).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, BiomeDecoratorGroups.GLOWSTONE).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, BiomeDecoratorGroups.ORE_MAGMA).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, BiomeDecoratorGroups.SPRING_CLOSED).addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, BiomeDecoratorGroups.WEEPING_VINES).addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, BiomeDecoratorGroups.CRIMSON_FUNGI).addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, BiomeDecoratorGroups.CRIMSON_FOREST_VEGETATION);
        BiomeSettings.addNetherDefaultOres(builder);
        return (new BiomeBase.BiomeBuilder()).precipitation(BiomeBase.Precipitation.NONE).biomeCategory(BiomeBase.Geography.NETHER).depth(0.1F).scale(0.2F).temperature(2.0F).downfall(0.0F).specialEffects((new BiomeFog.Builder()).waterColor(4159204).waterFogColor(329011).fogColor(3343107).skyColor(calculateSkyColor(2.0F)).ambientParticle(new BiomeParticles(Particles.CRIMSON_SPORE, 0.025F)).ambientLoopSound(SoundEffects.AMBIENT_CRIMSON_FOREST_LOOP).ambientMoodSound(new CaveSoundSettings(SoundEffects.AMBIENT_CRIMSON_FOREST_MOOD, 6000, 8, 2.0D)).ambientAdditionsSound(new CaveSound(SoundEffects.AMBIENT_CRIMSON_FOREST_ADDITIONS, 0.0111D)).backgroundMusic(Musics.createGameMusic(SoundEffects.MUSIC_BIOME_CRIMSON_FOREST)).build()).mobSpawnSettings(mobSpawnSettings).generationSettings(builder.build()).build();
    }

    public static BiomeBase warpedForestBiome() {
        BiomeSettingsMobs mobSpawnSettings = (new BiomeSettingsMobs.Builder()).addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.ENDERMAN, 1, 4, 4)).addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.STRIDER, 60, 1, 2)).addMobCharge(EntityTypes.ENDERMAN, 1.0D, 0.12D).build();
        BiomeSettingsGeneration.Builder builder = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(WorldGenSurfaceComposites.WARPED_FOREST).addStructureStart(StructureFeatures.NETHER_BRIDGE).addStructureStart(StructureFeatures.BASTION_REMNANT).addStructureStart(StructureFeatures.RUINED_PORTAL_NETHER).addCarver(WorldGenStage.Features.AIR, WorldGenCarvers.NETHER_CAVE).addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, BiomeDecoratorGroups.SPRING_LAVA);
        BiomeSettings.addDefaultMushrooms(builder);
        builder.addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, BiomeDecoratorGroups.SPRING_OPEN).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, BiomeDecoratorGroups.PATCH_FIRE).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, BiomeDecoratorGroups.PATCH_SOUL_FIRE).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, BiomeDecoratorGroups.GLOWSTONE_EXTRA).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, BiomeDecoratorGroups.GLOWSTONE).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, BiomeDecoratorGroups.ORE_MAGMA).addFeature(WorldGenStage.Decoration.UNDERGROUND_DECORATION, BiomeDecoratorGroups.SPRING_CLOSED).addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, BiomeDecoratorGroups.WARPED_FUNGI).addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, BiomeDecoratorGroups.WARPED_FOREST_VEGETATION).addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, BiomeDecoratorGroups.NETHER_SPROUTS).addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, BiomeDecoratorGroups.TWISTING_VINES);
        BiomeSettings.addNetherDefaultOres(builder);
        return (new BiomeBase.BiomeBuilder()).precipitation(BiomeBase.Precipitation.NONE).biomeCategory(BiomeBase.Geography.NETHER).depth(0.1F).scale(0.2F).temperature(2.0F).downfall(0.0F).specialEffects((new BiomeFog.Builder()).waterColor(4159204).waterFogColor(329011).fogColor(1705242).skyColor(calculateSkyColor(2.0F)).ambientParticle(new BiomeParticles(Particles.WARPED_SPORE, 0.01428F)).ambientLoopSound(SoundEffects.AMBIENT_WARPED_FOREST_LOOP).ambientMoodSound(new CaveSoundSettings(SoundEffects.AMBIENT_WARPED_FOREST_MOOD, 6000, 8, 2.0D)).ambientAdditionsSound(new CaveSound(SoundEffects.AMBIENT_WARPED_FOREST_ADDITIONS, 0.0111D)).backgroundMusic(Musics.createGameMusic(SoundEffects.MUSIC_BIOME_WARPED_FOREST)).build()).mobSpawnSettings(mobSpawnSettings).generationSettings(builder.build()).build();
    }

    public static BiomeBase lushCaves() {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        BiomeSettings.commonSpawns(builder);
        BiomeSettingsGeneration.Builder builder2 = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(WorldGenSurfaceComposites.GRASS);
        BiomeSettings.addDefaultOverworldLandStructures(builder2);
        builder2.addStructureStart(StructureFeatures.RUINED_PORTAL_STANDARD);
        BiomeSettings.addDefaultCarvers(builder2);
        BiomeSettings.addDefaultLakes(builder2);
        BiomeSettings.addDefaultCrystalFormations(builder2);
        BiomeSettings.addDefaultMonsterRoom(builder2);
        BiomeSettings.addPlainGrass(builder2);
        BiomeSettings.addDefaultUndergroundVariety(builder2);
        BiomeSettings.addDefaultOres(builder2);
        BiomeSettings.addLushCavesSpecialOres(builder2);
        BiomeSettings.addDefaultSoftDisks(builder2);
        BiomeSettings.addLushCavesVegetationFeatures(builder2);
        return (new BiomeBase.BiomeBuilder()).precipitation(BiomeBase.Precipitation.RAIN).biomeCategory(BiomeBase.Geography.UNDERGROUND).depth(0.1F).scale(0.2F).temperature(0.5F).downfall(0.5F).specialEffects((new BiomeFog.Builder()).waterColor(4159204).waterFogColor(329011).fogColor(12638463).skyColor(calculateSkyColor(0.5F)).build()).mobSpawnSettings(builder.build()).generationSettings(builder2.build()).build();
    }

    public static BiomeBase dripstoneCaves() {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        BiomeSettings.commonSpawns(builder);
        BiomeSettingsGeneration.Builder builder2 = (new BiomeSettingsGeneration.Builder()).surfaceBuilder(WorldGenSurfaceComposites.GRASS);
        BiomeSettings.addDefaultOverworldLandStructures(builder2);
        builder2.addStructureStart(StructureFeatures.RUINED_PORTAL_STANDARD);
        BiomeSettings.addDefaultCarvers(builder2);
        BiomeSettings.addDefaultLakes(builder2);
        BiomeSettings.addDefaultCrystalFormations(builder2);
        BiomeSettings.addDefaultMonsterRoom(builder2);
        BiomeSettings.addPlainGrass(builder2);
        BiomeSettings.addDefaultUndergroundVariety(builder2);
        BiomeSettings.addDefaultOres(builder2);
        BiomeSettings.addDefaultSoftDisks(builder2);
        BiomeSettings.addPlainVegetation(builder2);
        BiomeSettings.addDefaultMushrooms(builder2);
        BiomeSettings.addDefaultExtraVegetation(builder2);
        BiomeSettings.addDefaultSprings(builder2);
        BiomeSettings.addSurfaceFreezing(builder2);
        BiomeSettings.addDripstone(builder2);
        return (new BiomeBase.BiomeBuilder()).precipitation(BiomeBase.Precipitation.RAIN).biomeCategory(BiomeBase.Geography.UNDERGROUND).depth(0.125F).scale(0.05F).temperature(0.8F).downfall(0.4F).specialEffects((new BiomeFog.Builder()).waterColor(4159204).waterFogColor(329011).fogColor(12638463).skyColor(calculateSkyColor(0.8F)).ambientMoodSound(CaveSoundSettings.LEGACY_CAVE_SETTINGS).build()).mobSpawnSettings(builder.build()).generationSettings(builder2.build()).build();
    }
}
