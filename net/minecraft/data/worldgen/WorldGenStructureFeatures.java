package net.minecraft.data.worldgen;

import net.minecraft.data.RegistryGeneration;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.StructureGenerator;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureRuinedPortal;
import net.minecraft.world.level.levelgen.feature.WorldGenMineshaft;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureChanceDecoratorRangeConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureConfigurationChance;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureEmptyConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureOceanRuinConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureRuinedPortalConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureShipwreckConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureVillageConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenMineshaftConfiguration;
import net.minecraft.world.level.levelgen.heightproviders.UniformHeight;
import net.minecraft.world.level.levelgen.structure.WorldGenFeatureOceanRuin;

public class WorldGenStructureFeatures {
    public static final StructureFeature<WorldGenFeatureVillageConfiguration, ? extends StructureGenerator<WorldGenFeatureVillageConfiguration>> PILLAGER_OUTPOST = register("pillager_outpost", StructureGenerator.PILLAGER_OUTPOST.configured(new WorldGenFeatureVillageConfiguration(() -> {
        return WorldGenFeaturePillagerOutpostPieces.START;
    }, 7)));
    public static final StructureFeature<WorldGenMineshaftConfiguration, ? extends StructureGenerator<WorldGenMineshaftConfiguration>> MINESHAFT = register("mineshaft", StructureGenerator.MINESHAFT.configured(new WorldGenMineshaftConfiguration(0.004F, WorldGenMineshaft.Type.NORMAL)));
    public static final StructureFeature<WorldGenMineshaftConfiguration, ? extends StructureGenerator<WorldGenMineshaftConfiguration>> MINESHAFT_MESA = register("mineshaft_mesa", StructureGenerator.MINESHAFT.configured(new WorldGenMineshaftConfiguration(0.004F, WorldGenMineshaft.Type.MESA)));
    public static final StructureFeature<WorldGenFeatureEmptyConfiguration, ? extends StructureGenerator<WorldGenFeatureEmptyConfiguration>> WOODLAND_MANSION = register("mansion", StructureGenerator.WOODLAND_MANSION.configured(WorldGenFeatureEmptyConfiguration.INSTANCE));
    public static final StructureFeature<WorldGenFeatureEmptyConfiguration, ? extends StructureGenerator<WorldGenFeatureEmptyConfiguration>> JUNGLE_TEMPLE = register("jungle_pyramid", StructureGenerator.JUNGLE_TEMPLE.configured(WorldGenFeatureEmptyConfiguration.INSTANCE));
    public static final StructureFeature<WorldGenFeatureEmptyConfiguration, ? extends StructureGenerator<WorldGenFeatureEmptyConfiguration>> DESERT_PYRAMID = register("desert_pyramid", StructureGenerator.DESERT_PYRAMID.configured(WorldGenFeatureEmptyConfiguration.INSTANCE));
    public static final StructureFeature<WorldGenFeatureEmptyConfiguration, ? extends StructureGenerator<WorldGenFeatureEmptyConfiguration>> IGLOO = register("igloo", StructureGenerator.IGLOO.configured(WorldGenFeatureEmptyConfiguration.INSTANCE));
    public static final StructureFeature<WorldGenFeatureShipwreckConfiguration, ? extends StructureGenerator<WorldGenFeatureShipwreckConfiguration>> SHIPWRECK = register("shipwreck", StructureGenerator.SHIPWRECK.configured(new WorldGenFeatureShipwreckConfiguration(false)));
    public static final StructureFeature<WorldGenFeatureShipwreckConfiguration, ? extends StructureGenerator<WorldGenFeatureShipwreckConfiguration>> SHIPWRECH_BEACHED = register("shipwreck_beached", StructureGenerator.SHIPWRECK.configured(new WorldGenFeatureShipwreckConfiguration(true)));
    public static final StructureFeature<WorldGenFeatureEmptyConfiguration, ? extends StructureGenerator<WorldGenFeatureEmptyConfiguration>> SWAMP_HUT = register("swamp_hut", StructureGenerator.SWAMP_HUT.configured(WorldGenFeatureEmptyConfiguration.INSTANCE));
    public static final StructureFeature<WorldGenFeatureEmptyConfiguration, ? extends StructureGenerator<WorldGenFeatureEmptyConfiguration>> STRONGHOLD = register("stronghold", StructureGenerator.STRONGHOLD.configured(WorldGenFeatureEmptyConfiguration.INSTANCE));
    public static final StructureFeature<WorldGenFeatureEmptyConfiguration, ? extends StructureGenerator<WorldGenFeatureEmptyConfiguration>> OCEAN_MONUMENT = register("monument", StructureGenerator.OCEAN_MONUMENT.configured(WorldGenFeatureEmptyConfiguration.INSTANCE));
    public static final StructureFeature<WorldGenFeatureOceanRuinConfiguration, ? extends StructureGenerator<WorldGenFeatureOceanRuinConfiguration>> OCEAN_RUIN_COLD = register("ocean_ruin_cold", StructureGenerator.OCEAN_RUIN.configured(new WorldGenFeatureOceanRuinConfiguration(WorldGenFeatureOceanRuin.Temperature.COLD, 0.3F, 0.9F)));
    public static final StructureFeature<WorldGenFeatureOceanRuinConfiguration, ? extends StructureGenerator<WorldGenFeatureOceanRuinConfiguration>> OCEAN_RUIN_WARM = register("ocean_ruin_warm", StructureGenerator.OCEAN_RUIN.configured(new WorldGenFeatureOceanRuinConfiguration(WorldGenFeatureOceanRuin.Temperature.WARM, 0.3F, 0.9F)));
    public static final StructureFeature<WorldGenFeatureEmptyConfiguration, ? extends StructureGenerator<WorldGenFeatureEmptyConfiguration>> NETHER_BRIDGE = register("fortress", StructureGenerator.NETHER_BRIDGE.configured(WorldGenFeatureEmptyConfiguration.INSTANCE));
    public static final StructureFeature<WorldGenFeatureChanceDecoratorRangeConfiguration, ? extends StructureGenerator<WorldGenFeatureChanceDecoratorRangeConfiguration>> NETHER_FOSSIL = register("nether_fossil", StructureGenerator.NETHER_FOSSIL.configured(new WorldGenFeatureChanceDecoratorRangeConfiguration(UniformHeight.of(VerticalAnchor.absolute(32), VerticalAnchor.belowTop(2)))));
    public static final StructureFeature<WorldGenFeatureEmptyConfiguration, ? extends StructureGenerator<WorldGenFeatureEmptyConfiguration>> END_CITY = register("end_city", StructureGenerator.END_CITY.configured(WorldGenFeatureEmptyConfiguration.INSTANCE));
    public static final StructureFeature<WorldGenFeatureConfigurationChance, ? extends StructureGenerator<WorldGenFeatureConfigurationChance>> BURIED_TREASURE = register("buried_treasure", StructureGenerator.BURIED_TREASURE.configured(new WorldGenFeatureConfigurationChance(0.01F)));
    public static final StructureFeature<WorldGenFeatureVillageConfiguration, ? extends StructureGenerator<WorldGenFeatureVillageConfiguration>> BASTION_REMNANT = register("bastion_remnant", StructureGenerator.BASTION_REMNANT.configured(new WorldGenFeatureVillageConfiguration(() -> {
        return WorldGenFeatureBastionPieces.START;
    }, 6)));
    public static final StructureFeature<WorldGenFeatureVillageConfiguration, ? extends StructureGenerator<WorldGenFeatureVillageConfiguration>> VILLAGE_PLAINS = register("village_plains", StructureGenerator.VILLAGE.configured(new WorldGenFeatureVillageConfiguration(() -> {
        return WorldGenFeatureVillagePlain.START;
    }, 6)));
    public static final StructureFeature<WorldGenFeatureVillageConfiguration, ? extends StructureGenerator<WorldGenFeatureVillageConfiguration>> VILLAGE_DESERT = register("village_desert", StructureGenerator.VILLAGE.configured(new WorldGenFeatureVillageConfiguration(() -> {
        return WorldGenFeatureDesertVillage.START;
    }, 6)));
    public static final StructureFeature<WorldGenFeatureVillageConfiguration, ? extends StructureGenerator<WorldGenFeatureVillageConfiguration>> VILLAGE_SAVANNA = register("village_savanna", StructureGenerator.VILLAGE.configured(new WorldGenFeatureVillageConfiguration(() -> {
        return WorldGenFeatureVillageSavanna.START;
    }, 6)));
    public static final StructureFeature<WorldGenFeatureVillageConfiguration, ? extends StructureGenerator<WorldGenFeatureVillageConfiguration>> VILLAGE_SNOWY = register("village_snowy", StructureGenerator.VILLAGE.configured(new WorldGenFeatureVillageConfiguration(() -> {
        return WorldGenFeatureVillageSnowy.START;
    }, 6)));
    public static final StructureFeature<WorldGenFeatureVillageConfiguration, ? extends StructureGenerator<WorldGenFeatureVillageConfiguration>> VILLAGE_TAIGA = register("village_taiga", StructureGenerator.VILLAGE.configured(new WorldGenFeatureVillageConfiguration(() -> {
        return WorldGenFeatureVillageTaiga.START;
    }, 6)));
    public static final StructureFeature<WorldGenFeatureRuinedPortalConfiguration, ? extends StructureGenerator<WorldGenFeatureRuinedPortalConfiguration>> RUINED_PORTAL_STANDARD = register("ruined_portal", StructureGenerator.RUINED_PORTAL.configured(new WorldGenFeatureRuinedPortalConfiguration(WorldGenFeatureRuinedPortal.Type.STANDARD)));
    public static final StructureFeature<WorldGenFeatureRuinedPortalConfiguration, ? extends StructureGenerator<WorldGenFeatureRuinedPortalConfiguration>> RUINED_PORTAL_DESERT = register("ruined_portal_desert", StructureGenerator.RUINED_PORTAL.configured(new WorldGenFeatureRuinedPortalConfiguration(WorldGenFeatureRuinedPortal.Type.DESERT)));
    public static final StructureFeature<WorldGenFeatureRuinedPortalConfiguration, ? extends StructureGenerator<WorldGenFeatureRuinedPortalConfiguration>> RUINED_PORTAL_JUNGLE = register("ruined_portal_jungle", StructureGenerator.RUINED_PORTAL.configured(new WorldGenFeatureRuinedPortalConfiguration(WorldGenFeatureRuinedPortal.Type.JUNGLE)));
    public static final StructureFeature<WorldGenFeatureRuinedPortalConfiguration, ? extends StructureGenerator<WorldGenFeatureRuinedPortalConfiguration>> RUINED_PORTAL_SWAMP = register("ruined_portal_swamp", StructureGenerator.RUINED_PORTAL.configured(new WorldGenFeatureRuinedPortalConfiguration(WorldGenFeatureRuinedPortal.Type.SWAMP)));
    public static final StructureFeature<WorldGenFeatureRuinedPortalConfiguration, ? extends StructureGenerator<WorldGenFeatureRuinedPortalConfiguration>> RUINED_PORTAL_MOUNTAIN = register("ruined_portal_mountain", StructureGenerator.RUINED_PORTAL.configured(new WorldGenFeatureRuinedPortalConfiguration(WorldGenFeatureRuinedPortal.Type.MOUNTAIN)));
    public static final StructureFeature<WorldGenFeatureRuinedPortalConfiguration, ? extends StructureGenerator<WorldGenFeatureRuinedPortalConfiguration>> RUINED_PORTAL_OCEAN = register("ruined_portal_ocean", StructureGenerator.RUINED_PORTAL.configured(new WorldGenFeatureRuinedPortalConfiguration(WorldGenFeatureRuinedPortal.Type.OCEAN)));
    public static final StructureFeature<WorldGenFeatureRuinedPortalConfiguration, ? extends StructureGenerator<WorldGenFeatureRuinedPortalConfiguration>> RUINED_PORTAL_NETHER = register("ruined_portal_nether", StructureGenerator.RUINED_PORTAL.configured(new WorldGenFeatureRuinedPortalConfiguration(WorldGenFeatureRuinedPortal.Type.NETHER)));

    private static <FC extends WorldGenFeatureConfiguration, F extends StructureGenerator<FC>> StructureFeature<FC, F> register(String id, StructureFeature<FC, F> configuredStructureFeature) {
        return RegistryGeneration.register(RegistryGeneration.CONFIGURED_STRUCTURE_FEATURE, id, configuredStructureFeature);
    }
}
