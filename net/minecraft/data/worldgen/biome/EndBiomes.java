package net.minecraft.data.worldgen.biome;

import net.minecraft.data.worldgen.WorldGenBiomeSettings;
import net.minecraft.data.worldgen.placement.EndPlacements;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.BiomeFog;
import net.minecraft.world.level.biome.BiomeSettingsGeneration;
import net.minecraft.world.level.biome.BiomeSettingsMobs;
import net.minecraft.world.level.biome.CaveSoundSettings;
import net.minecraft.world.level.levelgen.WorldGenStage;

public class EndBiomes {
    private static BiomeBase baseEndBiome(BiomeSettingsGeneration.Builder builder) {
        BiomeSettingsMobs.Builder builder2 = new BiomeSettingsMobs.Builder();
        WorldGenBiomeSettings.endSpawns(builder2);
        return (new BiomeBase.BiomeBuilder()).precipitation(BiomeBase.Precipitation.NONE).biomeCategory(BiomeBase.Geography.THEEND).temperature(0.5F).downfall(0.5F).specialEffects((new BiomeFog.Builder()).waterColor(4159204).waterFogColor(329011).fogColor(10518688).skyColor(0).ambientMoodSound(CaveSoundSettings.LEGACY_CAVE_SETTINGS).build()).mobSpawnSettings(builder2.build()).generationSettings(builder.build()).build();
    }

    public static BiomeBase endBarrens() {
        BiomeSettingsGeneration.Builder builder = new BiomeSettingsGeneration.Builder();
        return baseEndBiome(builder);
    }

    public static BiomeBase theEnd() {
        BiomeSettingsGeneration.Builder builder = (new BiomeSettingsGeneration.Builder()).addFeature(WorldGenStage.Decoration.SURFACE_STRUCTURES, EndPlacements.END_SPIKE);
        return baseEndBiome(builder);
    }

    public static BiomeBase endMidlands() {
        BiomeSettingsGeneration.Builder builder = new BiomeSettingsGeneration.Builder();
        return baseEndBiome(builder);
    }

    public static BiomeBase endHighlands() {
        BiomeSettingsGeneration.Builder builder = (new BiomeSettingsGeneration.Builder()).addFeature(WorldGenStage.Decoration.SURFACE_STRUCTURES, EndPlacements.END_GATEWAY_RETURN).addFeature(WorldGenStage.Decoration.VEGETAL_DECORATION, EndPlacements.CHORUS_PLANT);
        return baseEndBiome(builder);
    }

    public static BiomeBase smallEndIslands() {
        BiomeSettingsGeneration.Builder builder = (new BiomeSettingsGeneration.Builder()).addFeature(WorldGenStage.Decoration.RAW_GENERATION, EndPlacements.END_ISLAND_DECORATED);
        return baseEndBiome(builder);
    }
}
