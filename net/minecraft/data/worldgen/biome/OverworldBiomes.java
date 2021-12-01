package net.minecraft.data.worldgen.biome;

import javax.annotation.Nullable;
import net.minecraft.data.worldgen.WorldGenBiomeSettings;
import net.minecraft.data.worldgen.placement.AquaticPlacements;
import net.minecraft.data.worldgen.placement.MiscOverworldPlacements;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.sounds.SoundTrack;
import net.minecraft.sounds.SoundTracks;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumCreatureType;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.BiomeFog;
import net.minecraft.world.level.biome.BiomeSettingsGeneration;
import net.minecraft.world.level.biome.BiomeSettingsMobs;
import net.minecraft.world.level.biome.CaveSoundSettings;
import net.minecraft.world.level.levelgen.WorldGenStage;

public class OverworldBiomes {
    protected static final int NORMAL_WATER_COLOR = 4159204;
    protected static final int NORMAL_WATER_FOG_COLOR = 329011;
    private static final int OVERWORLD_FOG_COLOR = 12638463;
    @Nullable
    private static final SoundTrack NORMAL_MUSIC = null;

    protected static int calculateSkyColor(float temperature) {
        float f = temperature / 3.0F;
        f = MathHelper.clamp(f, -1.0F, 1.0F);
        return MathHelper.hsvToRgb(0.62222224F - f * 0.05F, 0.5F + f * 0.1F, 1.0F);
    }

    private static BiomeBase biome(BiomeBase.Precipitation precipitation, BiomeBase.Geography category, float temperature, float downfall, BiomeSettingsMobs.Builder spawnSettings, BiomeSettingsGeneration.Builder generationSettings, @Nullable SoundTrack music) {
        return biome(precipitation, category, temperature, downfall, 4159204, 329011, spawnSettings, generationSettings, music);
    }

    private static BiomeBase biome(BiomeBase.Precipitation precipitation, BiomeBase.Geography category, float temperature, float downfall, int waterColor, int waterFogColor, BiomeSettingsMobs.Builder spawnSettings, BiomeSettingsGeneration.Builder generationSettings, @Nullable SoundTrack music) {
        return (new BiomeBase.BiomeBuilder()).precipitation(precipitation).biomeCategory(category).temperature(temperature).downfall(downfall).specialEffects((new BiomeFog.Builder()).waterColor(waterColor).waterFogColor(waterFogColor).fogColor(12638463).skyColor(calculateSkyColor(temperature)).ambientMoodSound(CaveSoundSettings.LEGACY_CAVE_SETTINGS).backgroundMusic(music).build()).mobSpawnSettings(spawnSettings.build()).generationSettings(generationSettings.build()).build();
    }

    private static void globalOverworldGeneration(BiomeSettingsGeneration.Builder generationSettings) {
        WorldGenBiomeSettings.addDefaultCarversAndLakes(generationSettings);
        WorldGenBiomeSettings.addDefaultCrystalFormations(generationSettings);
        WorldGenBiomeSettings.addDefaultMonsterRoom(generationSettings);
        WorldGenBiomeSettings.addDefaultUndergroundVariety(generationSettings);
        WorldGenBiomeSettings.addDefaultSprings(generationSettings);
        WorldGenBiomeSettings.addSurfaceFreezing(generationSettings);
    }

    public static BiomeBase oldGrowthTaiga(boolean spruce) {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        WorldGenBiomeSettings.farmAnimals(builder);
        builder.addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.WOLF, 8, 4, 4));
        builder.addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.RABBIT, 4, 2, 3));
        builder.addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.FOX, 8, 2, 4));
        if (spruce) {
            WorldGenBiomeSettings.commonSpawns(builder);
        } else {
            WorldGenBiomeSettings.caveSpawns(builder);
            WorldGenBiomeSettings.monsters(builder, 100, 25, 100, false);
        }

        BiomeSettingsGeneration.Builder builder2 = new BiomeSettingsGeneration.Builder();
        globalOverworldGeneration(builder2);
        WorldGenBiomeSettings.addMossyStoneBlock(builder2);
        WorldGenBiomeSettings.addFerns(builder2);
        WorldGenBiomeSettings.addDefaultOres(builder2);
        WorldGenBiomeSettings.addDefaultSoftDisks(builder2);
        builder2.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, spruce ? VegetationPlacements.TREES_OLD_GROWTH_SPRUCE_TAIGA : VegetationPlacements.TREES_OLD_GROWTH_PINE_TAIGA);
        WorldGenBiomeSettings.addDefaultFlowers(builder2);
        WorldGenBiomeSettings.addGiantTaigaVegetation(builder2);
        WorldGenBiomeSettings.addDefaultMushrooms(builder2);
        WorldGenBiomeSettings.addDefaultExtraVegetation(builder2);
        WorldGenBiomeSettings.addCommonBerryBushes(builder2);
        return biome(BiomeBase.Precipitation.RAIN, BiomeBase.Geography.TAIGA, spruce ? 0.25F : 0.3F, 0.8F, builder, builder2, NORMAL_MUSIC);
    }

    public static BiomeBase sparseJungle() {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        WorldGenBiomeSettings.baseJungleSpawns(builder);
        return baseJungle(0.8F, false, true, false, builder);
    }

    public static BiomeBase jungle() {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        WorldGenBiomeSettings.baseJungleSpawns(builder);
        builder.addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.PARROT, 40, 1, 2)).addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.OCELOT, 2, 1, 3)).addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.PANDA, 1, 1, 2));
        return baseJungle(0.9F, false, false, true, builder);
    }

    public static BiomeBase bambooJungle() {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        WorldGenBiomeSettings.baseJungleSpawns(builder);
        builder.addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.PARROT, 40, 1, 2)).addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.PANDA, 80, 1, 2)).addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.OCELOT, 2, 1, 1));
        return baseJungle(0.9F, true, false, true, builder);
    }

    private static BiomeBase baseJungle(float depth, boolean bamboo, boolean sparse, boolean unmodified, BiomeSettingsMobs.Builder spawnSettings) {
        BiomeSettingsGeneration.Builder builder = new BiomeSettingsGeneration.Builder();
        globalOverworldGeneration(builder);
        WorldGenBiomeSettings.addDefaultOres(builder);
        WorldGenBiomeSettings.addDefaultSoftDisks(builder);
        if (bamboo) {
            WorldGenBiomeSettings.addBambooVegetation(builder);
        } else {
            if (unmodified) {
                WorldGenBiomeSettings.addLightBambooVegetation(builder);
            }

            if (sparse) {
                WorldGenBiomeSettings.addSparseJungleTrees(builder);
            } else {
                WorldGenBiomeSettings.addJungleTrees(builder);
            }
        }

        WorldGenBiomeSettings.addWarmFlowers(builder);
        WorldGenBiomeSettings.addJungleGrass(builder);
        WorldGenBiomeSettings.addDefaultMushrooms(builder);
        WorldGenBiomeSettings.addDefaultExtraVegetation(builder);
        WorldGenBiomeSettings.addJungleVines(builder);
        if (sparse) {
            WorldGenBiomeSettings.addSparseJungleMelons(builder);
        } else {
            WorldGenBiomeSettings.addJungleMelons(builder);
        }

        return biome(BiomeBase.Precipitation.RAIN, BiomeBase.Geography.JUNGLE, 0.95F, depth, spawnSettings, builder, NORMAL_MUSIC);
    }

    public static BiomeBase windsweptHills(boolean forest) {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        WorldGenBiomeSettings.farmAnimals(builder);
        builder.addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.LLAMA, 5, 4, 6));
        WorldGenBiomeSettings.commonSpawns(builder);
        BiomeSettingsGeneration.Builder builder2 = new BiomeSettingsGeneration.Builder();
        globalOverworldGeneration(builder2);
        WorldGenBiomeSettings.addDefaultOres(builder2);
        WorldGenBiomeSettings.addDefaultSoftDisks(builder2);
        if (forest) {
            WorldGenBiomeSettings.addMountainForestTrees(builder2);
        } else {
            WorldGenBiomeSettings.addMountainTrees(builder2);
        }

        WorldGenBiomeSettings.addDefaultFlowers(builder2);
        WorldGenBiomeSettings.addDefaultGrass(builder2);
        WorldGenBiomeSettings.addDefaultMushrooms(builder2);
        WorldGenBiomeSettings.addDefaultExtraVegetation(builder2);
        WorldGenBiomeSettings.addExtraEmeralds(builder2);
        WorldGenBiomeSettings.addInfestedStone(builder2);
        return biome(BiomeBase.Precipitation.RAIN, BiomeBase.Geography.EXTREME_HILLS, 0.2F, 0.3F, builder, builder2, NORMAL_MUSIC);
    }

    public static BiomeBase desert() {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        WorldGenBiomeSettings.desertSpawns(builder);
        BiomeSettingsGeneration.Builder builder2 = new BiomeSettingsGeneration.Builder();
        WorldGenBiomeSettings.addFossilDecoration(builder2);
        globalOverworldGeneration(builder2);
        WorldGenBiomeSettings.addDefaultOres(builder2);
        WorldGenBiomeSettings.addDefaultSoftDisks(builder2);
        WorldGenBiomeSettings.addDefaultFlowers(builder2);
        WorldGenBiomeSettings.addDefaultGrass(builder2);
        WorldGenBiomeSettings.addDesertVegetation(builder2);
        WorldGenBiomeSettings.addDefaultMushrooms(builder2);
        WorldGenBiomeSettings.addDesertExtraVegetation(builder2);
        WorldGenBiomeSettings.addDesertExtraDecoration(builder2);
        return biome(BiomeBase.Precipitation.NONE, BiomeBase.Geography.DESERT, 2.0F, 0.0F, builder, builder2, NORMAL_MUSIC);
    }

    public static BiomeBase plains(boolean sunflower, boolean snowy, boolean iceSpikes) {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        BiomeSettingsGeneration.Builder builder2 = new BiomeSettingsGeneration.Builder();
        globalOverworldGeneration(builder2);
        if (snowy) {
            builder.creatureGenerationProbability(0.07F);
            WorldGenBiomeSettings.snowySpawns(builder);
            if (iceSpikes) {
                builder2.addFeature(WorldGenStage.Decoration.SURFACE_STRUCTURES, MiscOverworldPlacements.ICE_SPIKE);
                builder2.addFeature(WorldGenStage.Decoration.SURFACE_STRUCTURES, MiscOverworldPlacements.ICE_PATCH);
            }
        } else {
            WorldGenBiomeSettings.plainsSpawns(builder);
            WorldGenBiomeSettings.addPlainGrass(builder2);
            if (sunflower) {
                builder2.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_SUNFLOWER);
            }
        }

        WorldGenBiomeSettings.addDefaultOres(builder2);
        WorldGenBiomeSettings.addDefaultSoftDisks(builder2);
        if (snowy) {
            WorldGenBiomeSettings.addSnowyTrees(builder2);
            WorldGenBiomeSettings.addDefaultFlowers(builder2);
            WorldGenBiomeSettings.addDefaultGrass(builder2);
        } else {
            WorldGenBiomeSettings.addPlainVegetation(builder2);
        }

        WorldGenBiomeSettings.addDefaultMushrooms(builder2);
        if (sunflower) {
            builder2.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_SUGAR_CANE);
            builder2.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.PATCH_PUMPKIN);
        } else {
            WorldGenBiomeSettings.addDefaultExtraVegetation(builder2);
        }

        float f = snowy ? 0.0F : 0.8F;
        return biome(snowy ? BiomeBase.Precipitation.SNOW : BiomeBase.Precipitation.RAIN, snowy ? BiomeBase.Geography.ICY : BiomeBase.Geography.PLAINS, f, snowy ? 0.5F : 0.4F, builder, builder2, NORMAL_MUSIC);
    }

    public static BiomeBase mushroomFields() {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        WorldGenBiomeSettings.mooshroomSpawns(builder);
        BiomeSettingsGeneration.Builder builder2 = new BiomeSettingsGeneration.Builder();
        globalOverworldGeneration(builder2);
        WorldGenBiomeSettings.addDefaultOres(builder2);
        WorldGenBiomeSettings.addDefaultSoftDisks(builder2);
        WorldGenBiomeSettings.addMushroomFieldVegetation(builder2);
        WorldGenBiomeSettings.addDefaultExtraVegetation(builder2);
        return biome(BiomeBase.Precipitation.RAIN, BiomeBase.Geography.MUSHROOM, 0.9F, 1.0F, builder, builder2, NORMAL_MUSIC);
    }

    public static BiomeBase savanna(boolean windswept, boolean plateau) {
        BiomeSettingsGeneration.Builder builder = new BiomeSettingsGeneration.Builder();
        globalOverworldGeneration(builder);
        if (!windswept) {
            WorldGenBiomeSettings.addSavannaGrass(builder);
        }

        WorldGenBiomeSettings.addDefaultOres(builder);
        WorldGenBiomeSettings.addDefaultSoftDisks(builder);
        if (windswept) {
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
        BiomeSettingsMobs.Builder builder2 = new BiomeSettingsMobs.Builder();
        WorldGenBiomeSettings.farmAnimals(builder2);
        builder2.addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.HORSE, 1, 2, 6)).addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.DONKEY, 1, 1, 1));
        WorldGenBiomeSettings.commonSpawns(builder2);
        if (plateau) {
            builder2.addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.LLAMA, 8, 4, 4));
        }

        return biome(BiomeBase.Precipitation.NONE, BiomeBase.Geography.SAVANNA, 2.0F, 0.0F, builder2, builder, NORMAL_MUSIC);
    }

    public static BiomeBase badlands(boolean plateau) {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        WorldGenBiomeSettings.commonSpawns(builder);
        BiomeSettingsGeneration.Builder builder2 = new BiomeSettingsGeneration.Builder();
        globalOverworldGeneration(builder2);
        WorldGenBiomeSettings.addDefaultOres(builder2);
        WorldGenBiomeSettings.addExtraGold(builder2);
        WorldGenBiomeSettings.addDefaultSoftDisks(builder2);
        if (plateau) {
            WorldGenBiomeSettings.addBadlandsTrees(builder2);
        }

        WorldGenBiomeSettings.addBadlandGrass(builder2);
        WorldGenBiomeSettings.addDefaultMushrooms(builder2);
        WorldGenBiomeSettings.addBadlandExtraVegetation(builder2);
        return (new BiomeBase.BiomeBuilder()).precipitation(BiomeBase.Precipitation.NONE).biomeCategory(BiomeBase.Geography.MESA).temperature(2.0F).downfall(0.0F).specialEffects((new BiomeFog.Builder()).waterColor(4159204).waterFogColor(329011).fogColor(12638463).skyColor(calculateSkyColor(2.0F)).foliageColorOverride(10387789).grassColorOverride(9470285).ambientMoodSound(CaveSoundSettings.LEGACY_CAVE_SETTINGS).build()).mobSpawnSettings(builder.build()).generationSettings(builder2.build()).build();
    }

    private static BiomeBase baseOcean(BiomeSettingsMobs.Builder spawnSettings, int waterColor, int waterFogColor, BiomeSettingsGeneration.Builder builder) {
        return biome(BiomeBase.Precipitation.RAIN, BiomeBase.Geography.OCEAN, 0.5F, 0.5F, waterColor, waterFogColor, spawnSettings, builder, NORMAL_MUSIC);
    }

    private static BiomeSettingsGeneration.Builder baseOceanGeneration() {
        BiomeSettingsGeneration.Builder builder = new BiomeSettingsGeneration.Builder();
        globalOverworldGeneration(builder);
        WorldGenBiomeSettings.addDefaultOres(builder);
        WorldGenBiomeSettings.addDefaultSoftDisks(builder);
        WorldGenBiomeSettings.addWaterTrees(builder);
        WorldGenBiomeSettings.addDefaultFlowers(builder);
        WorldGenBiomeSettings.addDefaultGrass(builder);
        WorldGenBiomeSettings.addDefaultMushrooms(builder);
        WorldGenBiomeSettings.addDefaultExtraVegetation(builder);
        return builder;
    }

    public static BiomeBase coldOcean(boolean deep) {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        WorldGenBiomeSettings.oceanSpawns(builder, 3, 4, 15);
        builder.addSpawn(EnumCreatureType.WATER_AMBIENT, new BiomeSettingsMobs.SpawnerData(EntityTypes.SALMON, 15, 1, 5));
        BiomeSettingsGeneration.Builder builder2 = baseOceanGeneration();
        builder2.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, deep ? AquaticPlacements.SEAGRASS_DEEP_COLD : AquaticPlacements.SEAGRASS_COLD);
        WorldGenBiomeSettings.addDefaultSeagrass(builder2);
        WorldGenBiomeSettings.addColdOceanExtraVegetation(builder2);
        return baseOcean(builder, 4020182, 329011, builder2);
    }

    public static BiomeBase ocean(boolean deep) {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        WorldGenBiomeSettings.oceanSpawns(builder, 1, 4, 10);
        builder.addSpawn(EnumCreatureType.WATER_CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.DOLPHIN, 1, 1, 2));
        BiomeSettingsGeneration.Builder builder2 = baseOceanGeneration();
        builder2.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, deep ? AquaticPlacements.SEAGRASS_DEEP : AquaticPlacements.SEAGRASS_NORMAL);
        WorldGenBiomeSettings.addDefaultSeagrass(builder2);
        WorldGenBiomeSettings.addColdOceanExtraVegetation(builder2);
        return baseOcean(builder, 4159204, 329011, builder2);
    }

    public static BiomeBase lukeWarmOcean(boolean deep) {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        if (deep) {
            WorldGenBiomeSettings.oceanSpawns(builder, 8, 4, 8);
        } else {
            WorldGenBiomeSettings.oceanSpawns(builder, 10, 2, 15);
        }

        builder.addSpawn(EnumCreatureType.WATER_AMBIENT, new BiomeSettingsMobs.SpawnerData(EntityTypes.PUFFERFISH, 5, 1, 3)).addSpawn(EnumCreatureType.WATER_AMBIENT, new BiomeSettingsMobs.SpawnerData(EntityTypes.TROPICAL_FISH, 25, 8, 8)).addSpawn(EnumCreatureType.WATER_CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.DOLPHIN, 2, 1, 2));
        BiomeSettingsGeneration.Builder builder2 = baseOceanGeneration();
        builder2.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, deep ? AquaticPlacements.SEAGRASS_DEEP_WARM : AquaticPlacements.SEAGRASS_WARM);
        if (deep) {
            WorldGenBiomeSettings.addDefaultSeagrass(builder2);
        }

        WorldGenBiomeSettings.addLukeWarmKelp(builder2);
        return baseOcean(builder, 4566514, 267827, builder2);
    }

    public static BiomeBase warmOcean() {
        BiomeSettingsMobs.Builder builder = (new BiomeSettingsMobs.Builder()).addSpawn(EnumCreatureType.WATER_AMBIENT, new BiomeSettingsMobs.SpawnerData(EntityTypes.PUFFERFISH, 15, 1, 3));
        WorldGenBiomeSettings.warmOceanSpawns(builder, 10, 4);
        BiomeSettingsGeneration.Builder builder2 = baseOceanGeneration().addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, AquaticPlacements.WARM_OCEAN_VEGETATION).addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, AquaticPlacements.SEAGRASS_WARM).addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, AquaticPlacements.SEA_PICKLE);
        return baseOcean(builder, 4445678, 270131, builder2);
    }

    public static BiomeBase frozenOcean(boolean monument) {
        BiomeSettingsMobs.Builder builder = (new BiomeSettingsMobs.Builder()).addSpawn(EnumCreatureType.WATER_CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.SQUID, 1, 1, 4)).addSpawn(EnumCreatureType.WATER_AMBIENT, new BiomeSettingsMobs.SpawnerData(EntityTypes.SALMON, 15, 1, 5)).addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.POLAR_BEAR, 1, 1, 2));
        WorldGenBiomeSettings.commonSpawns(builder);
        builder.addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.DROWNED, 5, 1, 1));
        float f = monument ? 0.5F : 0.0F;
        BiomeSettingsGeneration.Builder builder2 = new BiomeSettingsGeneration.Builder();
        WorldGenBiomeSettings.addIcebergs(builder2);
        globalOverworldGeneration(builder2);
        WorldGenBiomeSettings.addBlueIce(builder2);
        WorldGenBiomeSettings.addDefaultOres(builder2);
        WorldGenBiomeSettings.addDefaultSoftDisks(builder2);
        WorldGenBiomeSettings.addWaterTrees(builder2);
        WorldGenBiomeSettings.addDefaultFlowers(builder2);
        WorldGenBiomeSettings.addDefaultGrass(builder2);
        WorldGenBiomeSettings.addDefaultMushrooms(builder2);
        WorldGenBiomeSettings.addDefaultExtraVegetation(builder2);
        return (new BiomeBase.BiomeBuilder()).precipitation(monument ? BiomeBase.Precipitation.RAIN : BiomeBase.Precipitation.SNOW).biomeCategory(BiomeBase.Geography.OCEAN).temperature(f).temperatureAdjustment(BiomeBase.TemperatureModifier.FROZEN).downfall(0.5F).specialEffects((new BiomeFog.Builder()).waterColor(3750089).waterFogColor(329011).fogColor(12638463).skyColor(calculateSkyColor(f)).ambientMoodSound(CaveSoundSettings.LEGACY_CAVE_SETTINGS).build()).mobSpawnSettings(builder.build()).generationSettings(builder2.build()).build();
    }

    public static BiomeBase forest(boolean birch, boolean oldGrowth, boolean flower) {
        BiomeSettingsGeneration.Builder builder = new BiomeSettingsGeneration.Builder();
        globalOverworldGeneration(builder);
        if (flower) {
            builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.FLOWER_FOREST_FLOWERS);
        } else {
            WorldGenBiomeSettings.addForestFlowers(builder);
        }

        WorldGenBiomeSettings.addDefaultOres(builder);
        WorldGenBiomeSettings.addDefaultSoftDisks(builder);
        if (flower) {
            builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.TREES_FLOWER_FOREST);
            builder.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.FLOWER_FLOWER_FOREST);
            WorldGenBiomeSettings.addDefaultGrass(builder);
        } else {
            if (birch) {
                if (oldGrowth) {
                    WorldGenBiomeSettings.addTallBirchTrees(builder);
                } else {
                    WorldGenBiomeSettings.addBirchTrees(builder);
                }
            } else {
                WorldGenBiomeSettings.addOtherBirchTrees(builder);
            }

            WorldGenBiomeSettings.addDefaultFlowers(builder);
            WorldGenBiomeSettings.addForestGrass(builder);
        }

        WorldGenBiomeSettings.addDefaultMushrooms(builder);
        WorldGenBiomeSettings.addDefaultExtraVegetation(builder);
        BiomeSettingsMobs.Builder builder2 = new BiomeSettingsMobs.Builder();
        WorldGenBiomeSettings.farmAnimals(builder2);
        WorldGenBiomeSettings.commonSpawns(builder2);
        if (flower) {
            builder2.addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.RABBIT, 4, 2, 3));
        } else if (!birch) {
            builder2.addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.WOLF, 5, 4, 4));
        }

        float f = birch ? 0.6F : 0.7F;
        return biome(BiomeBase.Precipitation.RAIN, BiomeBase.Geography.FOREST, f, birch ? 0.6F : 0.8F, builder2, builder, NORMAL_MUSIC);
    }

    public static BiomeBase taiga(boolean cold) {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        WorldGenBiomeSettings.farmAnimals(builder);
        builder.addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.WOLF, 8, 4, 4)).addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.RABBIT, 4, 2, 3)).addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.FOX, 8, 2, 4));
        WorldGenBiomeSettings.commonSpawns(builder);
        float f = cold ? -0.5F : 0.25F;
        BiomeSettingsGeneration.Builder builder2 = new BiomeSettingsGeneration.Builder();
        globalOverworldGeneration(builder2);
        WorldGenBiomeSettings.addFerns(builder2);
        WorldGenBiomeSettings.addDefaultOres(builder2);
        WorldGenBiomeSettings.addDefaultSoftDisks(builder2);
        WorldGenBiomeSettings.addTaigaTrees(builder2);
        WorldGenBiomeSettings.addDefaultFlowers(builder2);
        WorldGenBiomeSettings.addTaigaGrass(builder2);
        WorldGenBiomeSettings.addDefaultExtraVegetation(builder2);
        if (cold) {
            WorldGenBiomeSettings.addRareBerryBushes(builder2);
        } else {
            WorldGenBiomeSettings.addCommonBerryBushes(builder2);
        }

        return biome(cold ? BiomeBase.Precipitation.SNOW : BiomeBase.Precipitation.RAIN, BiomeBase.Geography.TAIGA, f, cold ? 0.4F : 0.8F, cold ? 4020182 : 4159204, 329011, builder, builder2, NORMAL_MUSIC);
    }

    public static BiomeBase darkForest() {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        WorldGenBiomeSettings.farmAnimals(builder);
        WorldGenBiomeSettings.commonSpawns(builder);
        BiomeSettingsGeneration.Builder builder2 = new BiomeSettingsGeneration.Builder();
        globalOverworldGeneration(builder2);
        builder2.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, VegetationPlacements.DARK_FOREST_VEGETATION);
        WorldGenBiomeSettings.addForestFlowers(builder2);
        WorldGenBiomeSettings.addDefaultOres(builder2);
        WorldGenBiomeSettings.addDefaultSoftDisks(builder2);
        WorldGenBiomeSettings.addDefaultFlowers(builder2);
        WorldGenBiomeSettings.addForestGrass(builder2);
        WorldGenBiomeSettings.addDefaultMushrooms(builder2);
        WorldGenBiomeSettings.addDefaultExtraVegetation(builder2);
        return (new BiomeBase.BiomeBuilder()).precipitation(BiomeBase.Precipitation.RAIN).biomeCategory(BiomeBase.Geography.FOREST).temperature(0.7F).downfall(0.8F).specialEffects((new BiomeFog.Builder()).waterColor(4159204).waterFogColor(329011).fogColor(12638463).skyColor(calculateSkyColor(0.7F)).grassColorModifier(BiomeFog.GrassColor.DARK_FOREST).ambientMoodSound(CaveSoundSettings.LEGACY_CAVE_SETTINGS).build()).mobSpawnSettings(builder.build()).generationSettings(builder2.build()).build();
    }

    public static BiomeBase swamp() {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        WorldGenBiomeSettings.farmAnimals(builder);
        WorldGenBiomeSettings.commonSpawns(builder);
        builder.addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.SLIME, 1, 1, 1));
        BiomeSettingsGeneration.Builder builder2 = new BiomeSettingsGeneration.Builder();
        WorldGenBiomeSettings.addFossilDecoration(builder2);
        globalOverworldGeneration(builder2);
        WorldGenBiomeSettings.addDefaultOres(builder2);
        WorldGenBiomeSettings.addSwampClayDisk(builder2);
        WorldGenBiomeSettings.addSwampVegetation(builder2);
        WorldGenBiomeSettings.addDefaultMushrooms(builder2);
        WorldGenBiomeSettings.addSwampExtraVegetation(builder2);
        builder2.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, AquaticPlacements.SEAGRASS_SWAMP);
        return (new BiomeBase.BiomeBuilder()).precipitation(BiomeBase.Precipitation.RAIN).biomeCategory(BiomeBase.Geography.SWAMP).temperature(0.8F).downfall(0.9F).specialEffects((new BiomeFog.Builder()).waterColor(6388580).waterFogColor(2302743).fogColor(12638463).skyColor(calculateSkyColor(0.8F)).foliageColorOverride(6975545).grassColorModifier(BiomeFog.GrassColor.SWAMP).ambientMoodSound(CaveSoundSettings.LEGACY_CAVE_SETTINGS).build()).mobSpawnSettings(builder.build()).generationSettings(builder2.build()).build();
    }

    public static BiomeBase river(boolean frozen) {
        BiomeSettingsMobs.Builder builder = (new BiomeSettingsMobs.Builder()).addSpawn(EnumCreatureType.WATER_CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.SQUID, 2, 1, 4)).addSpawn(EnumCreatureType.WATER_AMBIENT, new BiomeSettingsMobs.SpawnerData(EntityTypes.SALMON, 5, 1, 5));
        WorldGenBiomeSettings.commonSpawns(builder);
        builder.addSpawn(EnumCreatureType.MONSTER, new BiomeSettingsMobs.SpawnerData(EntityTypes.DROWNED, frozen ? 1 : 100, 1, 1));
        BiomeSettingsGeneration.Builder builder2 = new BiomeSettingsGeneration.Builder();
        globalOverworldGeneration(builder2);
        WorldGenBiomeSettings.addDefaultOres(builder2);
        WorldGenBiomeSettings.addDefaultSoftDisks(builder2);
        WorldGenBiomeSettings.addWaterTrees(builder2);
        WorldGenBiomeSettings.addDefaultFlowers(builder2);
        WorldGenBiomeSettings.addDefaultGrass(builder2);
        WorldGenBiomeSettings.addDefaultMushrooms(builder2);
        WorldGenBiomeSettings.addDefaultExtraVegetation(builder2);
        if (!frozen) {
            builder2.addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, AquaticPlacements.SEAGRASS_RIVER);
        }

        float f = frozen ? 0.0F : 0.5F;
        return biome(frozen ? BiomeBase.Precipitation.SNOW : BiomeBase.Precipitation.RAIN, BiomeBase.Geography.RIVER, f, 0.5F, frozen ? 3750089 : 4159204, 329011, builder, builder2, NORMAL_MUSIC);
    }

    public static BiomeBase beach(boolean snowy, boolean stony) {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        boolean bl = !stony && !snowy;
        if (bl) {
            builder.addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.TURTLE, 5, 2, 5));
        }

        WorldGenBiomeSettings.commonSpawns(builder);
        BiomeSettingsGeneration.Builder builder2 = new BiomeSettingsGeneration.Builder();
        globalOverworldGeneration(builder2);
        WorldGenBiomeSettings.addDefaultOres(builder2);
        WorldGenBiomeSettings.addDefaultSoftDisks(builder2);
        WorldGenBiomeSettings.addDefaultFlowers(builder2);
        WorldGenBiomeSettings.addDefaultGrass(builder2);
        WorldGenBiomeSettings.addDefaultMushrooms(builder2);
        WorldGenBiomeSettings.addDefaultExtraVegetation(builder2);
        float f;
        if (snowy) {
            f = 0.05F;
        } else if (stony) {
            f = 0.2F;
        } else {
            f = 0.8F;
        }

        return biome(snowy ? BiomeBase.Precipitation.SNOW : BiomeBase.Precipitation.RAIN, BiomeBase.Geography.BEACH, f, bl ? 0.4F : 0.3F, snowy ? 4020182 : 4159204, 329011, builder, builder2, NORMAL_MUSIC);
    }

    public static BiomeBase theVoid() {
        BiomeSettingsGeneration.Builder builder = new BiomeSettingsGeneration.Builder();
        builder.addFeature(WorldGenStage.Decoration.TOP_LAYER_MODIFICATION, MiscOverworldPlacements.VOID_START_PLATFORM);
        return biome(BiomeBase.Precipitation.NONE, BiomeBase.Geography.NONE, 0.5F, 0.5F, new BiomeSettingsMobs.Builder(), builder, NORMAL_MUSIC);
    }

    public static BiomeBase meadow() {
        BiomeSettingsGeneration.Builder builder = new BiomeSettingsGeneration.Builder();
        BiomeSettingsMobs.Builder builder2 = new BiomeSettingsMobs.Builder();
        builder2.addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.DONKEY, 1, 1, 2)).addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.RABBIT, 2, 2, 6)).addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.SHEEP, 2, 2, 4));
        WorldGenBiomeSettings.commonSpawns(builder2);
        globalOverworldGeneration(builder);
        WorldGenBiomeSettings.addPlainGrass(builder);
        WorldGenBiomeSettings.addDefaultOres(builder);
        WorldGenBiomeSettings.addDefaultSoftDisks(builder);
        WorldGenBiomeSettings.addMeadowVegetation(builder);
        WorldGenBiomeSettings.addExtraEmeralds(builder);
        WorldGenBiomeSettings.addInfestedStone(builder);
        SoundTrack music = SoundTracks.createGameMusic(SoundEffects.MUSIC_BIOME_MEADOW);
        return biome(BiomeBase.Precipitation.RAIN, BiomeBase.Geography.MOUNTAIN, 0.5F, 0.8F, 937679, 329011, builder2, builder, music);
    }

    public static BiomeBase frozenPeaks() {
        BiomeSettingsGeneration.Builder builder = new BiomeSettingsGeneration.Builder();
        BiomeSettingsMobs.Builder builder2 = new BiomeSettingsMobs.Builder();
        builder2.addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.GOAT, 5, 1, 3));
        WorldGenBiomeSettings.commonSpawns(builder2);
        globalOverworldGeneration(builder);
        WorldGenBiomeSettings.addFrozenSprings(builder);
        WorldGenBiomeSettings.addDefaultOres(builder);
        WorldGenBiomeSettings.addDefaultSoftDisks(builder);
        WorldGenBiomeSettings.addExtraEmeralds(builder);
        WorldGenBiomeSettings.addInfestedStone(builder);
        SoundTrack music = SoundTracks.createGameMusic(SoundEffects.MUSIC_BIOME_FROZEN_PEAKS);
        return biome(BiomeBase.Precipitation.SNOW, BiomeBase.Geography.MOUNTAIN, -0.7F, 0.9F, builder2, builder, music);
    }

    public static BiomeBase jaggedPeaks() {
        BiomeSettingsGeneration.Builder builder = new BiomeSettingsGeneration.Builder();
        BiomeSettingsMobs.Builder builder2 = new BiomeSettingsMobs.Builder();
        builder2.addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.GOAT, 5, 1, 3));
        WorldGenBiomeSettings.commonSpawns(builder2);
        globalOverworldGeneration(builder);
        WorldGenBiomeSettings.addFrozenSprings(builder);
        WorldGenBiomeSettings.addDefaultOres(builder);
        WorldGenBiomeSettings.addDefaultSoftDisks(builder);
        WorldGenBiomeSettings.addExtraEmeralds(builder);
        WorldGenBiomeSettings.addInfestedStone(builder);
        SoundTrack music = SoundTracks.createGameMusic(SoundEffects.MUSIC_BIOME_JAGGED_PEAKS);
        return biome(BiomeBase.Precipitation.SNOW, BiomeBase.Geography.MOUNTAIN, -0.7F, 0.9F, builder2, builder, music);
    }

    public static BiomeBase stonyPeaks() {
        BiomeSettingsGeneration.Builder builder = new BiomeSettingsGeneration.Builder();
        BiomeSettingsMobs.Builder builder2 = new BiomeSettingsMobs.Builder();
        WorldGenBiomeSettings.commonSpawns(builder2);
        globalOverworldGeneration(builder);
        WorldGenBiomeSettings.addDefaultOres(builder);
        WorldGenBiomeSettings.addDefaultSoftDisks(builder);
        WorldGenBiomeSettings.addExtraEmeralds(builder);
        WorldGenBiomeSettings.addInfestedStone(builder);
        SoundTrack music = SoundTracks.createGameMusic(SoundEffects.MUSIC_BIOME_STONY_PEAKS);
        return biome(BiomeBase.Precipitation.RAIN, BiomeBase.Geography.MOUNTAIN, 1.0F, 0.3F, builder2, builder, music);
    }

    public static BiomeBase snowySlopes() {
        BiomeSettingsGeneration.Builder builder = new BiomeSettingsGeneration.Builder();
        BiomeSettingsMobs.Builder builder2 = new BiomeSettingsMobs.Builder();
        builder2.addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.RABBIT, 4, 2, 3)).addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.GOAT, 5, 1, 3));
        WorldGenBiomeSettings.commonSpawns(builder2);
        globalOverworldGeneration(builder);
        WorldGenBiomeSettings.addFrozenSprings(builder);
        WorldGenBiomeSettings.addDefaultOres(builder);
        WorldGenBiomeSettings.addDefaultSoftDisks(builder);
        WorldGenBiomeSettings.addDefaultExtraVegetation(builder);
        WorldGenBiomeSettings.addExtraEmeralds(builder);
        WorldGenBiomeSettings.addInfestedStone(builder);
        SoundTrack music = SoundTracks.createGameMusic(SoundEffects.MUSIC_BIOME_SNOWY_SLOPES);
        return biome(BiomeBase.Precipitation.SNOW, BiomeBase.Geography.MOUNTAIN, -0.3F, 0.9F, builder2, builder, music);
    }

    public static BiomeBase grove() {
        BiomeSettingsGeneration.Builder builder = new BiomeSettingsGeneration.Builder();
        BiomeSettingsMobs.Builder builder2 = new BiomeSettingsMobs.Builder();
        WorldGenBiomeSettings.farmAnimals(builder2);
        builder2.addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.WOLF, 8, 4, 4)).addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.RABBIT, 4, 2, 3)).addSpawn(EnumCreatureType.CREATURE, new BiomeSettingsMobs.SpawnerData(EntityTypes.FOX, 8, 2, 4));
        WorldGenBiomeSettings.commonSpawns(builder2);
        globalOverworldGeneration(builder);
        WorldGenBiomeSettings.addFrozenSprings(builder);
        WorldGenBiomeSettings.addDefaultOres(builder);
        WorldGenBiomeSettings.addDefaultSoftDisks(builder);
        WorldGenBiomeSettings.addGroveTrees(builder);
        WorldGenBiomeSettings.addDefaultExtraVegetation(builder);
        WorldGenBiomeSettings.addExtraEmeralds(builder);
        WorldGenBiomeSettings.addInfestedStone(builder);
        SoundTrack music = SoundTracks.createGameMusic(SoundEffects.MUSIC_BIOME_GROVE);
        return biome(BiomeBase.Precipitation.SNOW, BiomeBase.Geography.FOREST, -0.2F, 0.8F, builder2, builder, music);
    }

    public static BiomeBase lushCaves() {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        builder.addSpawn(EnumCreatureType.AXOLOTLS, new BiomeSettingsMobs.SpawnerData(EntityTypes.AXOLOTL, 10, 4, 6));
        builder.addSpawn(EnumCreatureType.WATER_AMBIENT, new BiomeSettingsMobs.SpawnerData(EntityTypes.TROPICAL_FISH, 25, 8, 8));
        WorldGenBiomeSettings.commonSpawns(builder);
        BiomeSettingsGeneration.Builder builder2 = new BiomeSettingsGeneration.Builder();
        globalOverworldGeneration(builder2);
        WorldGenBiomeSettings.addPlainGrass(builder2);
        WorldGenBiomeSettings.addDefaultOres(builder2);
        WorldGenBiomeSettings.addLushCavesSpecialOres(builder2);
        WorldGenBiomeSettings.addDefaultSoftDisks(builder2);
        WorldGenBiomeSettings.addLushCavesVegetationFeatures(builder2);
        SoundTrack music = SoundTracks.createGameMusic(SoundEffects.MUSIC_BIOME_LUSH_CAVES);
        return biome(BiomeBase.Precipitation.RAIN, BiomeBase.Geography.UNDERGROUND, 0.5F, 0.5F, builder, builder2, music);
    }

    public static BiomeBase dripstoneCaves() {
        BiomeSettingsMobs.Builder builder = new BiomeSettingsMobs.Builder();
        WorldGenBiomeSettings.dripstoneCavesSpawns(builder);
        BiomeSettingsGeneration.Builder builder2 = new BiomeSettingsGeneration.Builder();
        globalOverworldGeneration(builder2);
        WorldGenBiomeSettings.addPlainGrass(builder2);
        WorldGenBiomeSettings.addDefaultOres(builder2, true);
        WorldGenBiomeSettings.addDefaultSoftDisks(builder2);
        WorldGenBiomeSettings.addPlainVegetation(builder2);
        WorldGenBiomeSettings.addDefaultMushrooms(builder2);
        WorldGenBiomeSettings.addDefaultExtraVegetation(builder2);
        WorldGenBiomeSettings.addDripstone(builder2);
        SoundTrack music = SoundTracks.createGameMusic(SoundEffects.MUSIC_BIOME_DRIPSTONE_CAVES);
        return biome(BiomeBase.Precipitation.RAIN, BiomeBase.Geography.UNDERGROUND, 0.8F, 0.4F, builder, builder2, music);
    }
}
