package net.minecraft.data.worldgen;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import java.util.function.BiConsumer;
import net.minecraft.data.RegistryGeneration;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.StructureGenerator;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureRuinedPortal;
import net.minecraft.world.level.levelgen.feature.WorldGenMineshaft;
import net.minecraft.world.level.levelgen.feature.configurations.RangeConfiguration;
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
    private static final StructureFeature<WorldGenFeatureVillageConfiguration, ? extends StructureGenerator<WorldGenFeatureVillageConfiguration>> PILLAGER_OUTPOST = register("pillager_outpost", StructureGenerator.PILLAGER_OUTPOST.configured(new WorldGenFeatureVillageConfiguration(() -> {
        return WorldGenFeaturePillagerOutpostPieces.START;
    }, 7)));
    private static final StructureFeature<WorldGenMineshaftConfiguration, ? extends StructureGenerator<WorldGenMineshaftConfiguration>> MINESHAFT = register("mineshaft", StructureGenerator.MINESHAFT.configured(new WorldGenMineshaftConfiguration(0.004F, WorldGenMineshaft.Type.NORMAL)));
    private static final StructureFeature<WorldGenMineshaftConfiguration, ? extends StructureGenerator<WorldGenMineshaftConfiguration>> MINESHAFT_MESA = register("mineshaft_mesa", StructureGenerator.MINESHAFT.configured(new WorldGenMineshaftConfiguration(0.004F, WorldGenMineshaft.Type.MESA)));
    private static final StructureFeature<WorldGenFeatureEmptyConfiguration, ? extends StructureGenerator<WorldGenFeatureEmptyConfiguration>> WOODLAND_MANSION = register("mansion", StructureGenerator.WOODLAND_MANSION.configured(WorldGenFeatureEmptyConfiguration.INSTANCE));
    private static final StructureFeature<WorldGenFeatureEmptyConfiguration, ? extends StructureGenerator<WorldGenFeatureEmptyConfiguration>> JUNGLE_TEMPLE = register("jungle_pyramid", StructureGenerator.JUNGLE_TEMPLE.configured(WorldGenFeatureEmptyConfiguration.INSTANCE));
    private static final StructureFeature<WorldGenFeatureEmptyConfiguration, ? extends StructureGenerator<WorldGenFeatureEmptyConfiguration>> DESERT_PYRAMID = register("desert_pyramid", StructureGenerator.DESERT_PYRAMID.configured(WorldGenFeatureEmptyConfiguration.INSTANCE));
    private static final StructureFeature<WorldGenFeatureEmptyConfiguration, ? extends StructureGenerator<WorldGenFeatureEmptyConfiguration>> IGLOO = register("igloo", StructureGenerator.IGLOO.configured(WorldGenFeatureEmptyConfiguration.INSTANCE));
    private static final StructureFeature<WorldGenFeatureShipwreckConfiguration, ? extends StructureGenerator<WorldGenFeatureShipwreckConfiguration>> SHIPWRECK = register("shipwreck", StructureGenerator.SHIPWRECK.configured(new WorldGenFeatureShipwreckConfiguration(false)));
    private static final StructureFeature<WorldGenFeatureShipwreckConfiguration, ? extends StructureGenerator<WorldGenFeatureShipwreckConfiguration>> SHIPWRECK_BEACHED = register("shipwreck_beached", StructureGenerator.SHIPWRECK.configured(new WorldGenFeatureShipwreckConfiguration(true)));
    private static final StructureFeature<WorldGenFeatureEmptyConfiguration, ? extends StructureGenerator<WorldGenFeatureEmptyConfiguration>> SWAMP_HUT = register("swamp_hut", StructureGenerator.SWAMP_HUT.configured(WorldGenFeatureEmptyConfiguration.INSTANCE));
    public static final StructureFeature<WorldGenFeatureEmptyConfiguration, ? extends StructureGenerator<WorldGenFeatureEmptyConfiguration>> STRONGHOLD = register("stronghold", StructureGenerator.STRONGHOLD.configured(WorldGenFeatureEmptyConfiguration.INSTANCE));
    private static final StructureFeature<WorldGenFeatureEmptyConfiguration, ? extends StructureGenerator<WorldGenFeatureEmptyConfiguration>> OCEAN_MONUMENT = register("monument", StructureGenerator.OCEAN_MONUMENT.configured(WorldGenFeatureEmptyConfiguration.INSTANCE));
    private static final StructureFeature<WorldGenFeatureOceanRuinConfiguration, ? extends StructureGenerator<WorldGenFeatureOceanRuinConfiguration>> OCEAN_RUIN_COLD = register("ocean_ruin_cold", StructureGenerator.OCEAN_RUIN.configured(new WorldGenFeatureOceanRuinConfiguration(WorldGenFeatureOceanRuin.Temperature.COLD, 0.3F, 0.9F)));
    private static final StructureFeature<WorldGenFeatureOceanRuinConfiguration, ? extends StructureGenerator<WorldGenFeatureOceanRuinConfiguration>> OCEAN_RUIN_WARM = register("ocean_ruin_warm", StructureGenerator.OCEAN_RUIN.configured(new WorldGenFeatureOceanRuinConfiguration(WorldGenFeatureOceanRuin.Temperature.WARM, 0.3F, 0.9F)));
    private static final StructureFeature<WorldGenFeatureEmptyConfiguration, ? extends StructureGenerator<WorldGenFeatureEmptyConfiguration>> NETHER_BRIDGE = register("fortress", StructureGenerator.NETHER_BRIDGE.configured(WorldGenFeatureEmptyConfiguration.INSTANCE));
    private static final StructureFeature<RangeConfiguration, ? extends StructureGenerator<RangeConfiguration>> NETHER_FOSSIL = register("nether_fossil", StructureGenerator.NETHER_FOSSIL.configured(new RangeConfiguration(UniformHeight.of(VerticalAnchor.absolute(32), VerticalAnchor.belowTop(2)))));
    private static final StructureFeature<WorldGenFeatureEmptyConfiguration, ? extends StructureGenerator<WorldGenFeatureEmptyConfiguration>> END_CITY = register("end_city", StructureGenerator.END_CITY.configured(WorldGenFeatureEmptyConfiguration.INSTANCE));
    private static final StructureFeature<WorldGenFeatureConfigurationChance, ? extends StructureGenerator<WorldGenFeatureConfigurationChance>> BURIED_TREASURE = register("buried_treasure", StructureGenerator.BURIED_TREASURE.configured(new WorldGenFeatureConfigurationChance(0.01F)));
    private static final StructureFeature<WorldGenFeatureVillageConfiguration, ? extends StructureGenerator<WorldGenFeatureVillageConfiguration>> BASTION_REMNANT = register("bastion_remnant", StructureGenerator.BASTION_REMNANT.configured(new WorldGenFeatureVillageConfiguration(() -> {
        return WorldGenFeatureBastionPieces.START;
    }, 6)));
    private static final StructureFeature<WorldGenFeatureVillageConfiguration, ? extends StructureGenerator<WorldGenFeatureVillageConfiguration>> VILLAGE_PLAINS = register("village_plains", StructureGenerator.VILLAGE.configured(new WorldGenFeatureVillageConfiguration(() -> {
        return WorldGenFeatureVillagePlain.START;
    }, 6)));
    private static final StructureFeature<WorldGenFeatureVillageConfiguration, ? extends StructureGenerator<WorldGenFeatureVillageConfiguration>> VILLAGE_DESERT = register("village_desert", StructureGenerator.VILLAGE.configured(new WorldGenFeatureVillageConfiguration(() -> {
        return WorldGenFeatureDesertVillage.START;
    }, 6)));
    private static final StructureFeature<WorldGenFeatureVillageConfiguration, ? extends StructureGenerator<WorldGenFeatureVillageConfiguration>> VILLAGE_SAVANNA = register("village_savanna", StructureGenerator.VILLAGE.configured(new WorldGenFeatureVillageConfiguration(() -> {
        return WorldGenFeatureVillageSavanna.START;
    }, 6)));
    private static final StructureFeature<WorldGenFeatureVillageConfiguration, ? extends StructureGenerator<WorldGenFeatureVillageConfiguration>> VILLAGE_SNOWY = register("village_snowy", StructureGenerator.VILLAGE.configured(new WorldGenFeatureVillageConfiguration(() -> {
        return WorldGenFeatureVillageSnowy.START;
    }, 6)));
    private static final StructureFeature<WorldGenFeatureVillageConfiguration, ? extends StructureGenerator<WorldGenFeatureVillageConfiguration>> VILLAGE_TAIGA = register("village_taiga", StructureGenerator.VILLAGE.configured(new WorldGenFeatureVillageConfiguration(() -> {
        return WorldGenFeatureVillageTaiga.START;
    }, 6)));
    private static final StructureFeature<WorldGenFeatureRuinedPortalConfiguration, ? extends StructureGenerator<WorldGenFeatureRuinedPortalConfiguration>> RUINED_PORTAL_STANDARD = register("ruined_portal", StructureGenerator.RUINED_PORTAL.configured(new WorldGenFeatureRuinedPortalConfiguration(WorldGenFeatureRuinedPortal.Type.STANDARD)));
    private static final StructureFeature<WorldGenFeatureRuinedPortalConfiguration, ? extends StructureGenerator<WorldGenFeatureRuinedPortalConfiguration>> RUINED_PORTAL_DESERT = register("ruined_portal_desert", StructureGenerator.RUINED_PORTAL.configured(new WorldGenFeatureRuinedPortalConfiguration(WorldGenFeatureRuinedPortal.Type.DESERT)));
    private static final StructureFeature<WorldGenFeatureRuinedPortalConfiguration, ? extends StructureGenerator<WorldGenFeatureRuinedPortalConfiguration>> RUINED_PORTAL_JUNGLE = register("ruined_portal_jungle", StructureGenerator.RUINED_PORTAL.configured(new WorldGenFeatureRuinedPortalConfiguration(WorldGenFeatureRuinedPortal.Type.JUNGLE)));
    private static final StructureFeature<WorldGenFeatureRuinedPortalConfiguration, ? extends StructureGenerator<WorldGenFeatureRuinedPortalConfiguration>> RUINED_PORTAL_SWAMP = register("ruined_portal_swamp", StructureGenerator.RUINED_PORTAL.configured(new WorldGenFeatureRuinedPortalConfiguration(WorldGenFeatureRuinedPortal.Type.SWAMP)));
    private static final StructureFeature<WorldGenFeatureRuinedPortalConfiguration, ? extends StructureGenerator<WorldGenFeatureRuinedPortalConfiguration>> RUINED_PORTAL_MOUNTAIN = register("ruined_portal_mountain", StructureGenerator.RUINED_PORTAL.configured(new WorldGenFeatureRuinedPortalConfiguration(WorldGenFeatureRuinedPortal.Type.MOUNTAIN)));
    private static final StructureFeature<WorldGenFeatureRuinedPortalConfiguration, ? extends StructureGenerator<WorldGenFeatureRuinedPortalConfiguration>> RUINED_PORTAL_OCEAN = register("ruined_portal_ocean", StructureGenerator.RUINED_PORTAL.configured(new WorldGenFeatureRuinedPortalConfiguration(WorldGenFeatureRuinedPortal.Type.OCEAN)));
    private static final StructureFeature<WorldGenFeatureRuinedPortalConfiguration, ? extends StructureGenerator<WorldGenFeatureRuinedPortalConfiguration>> RUINED_PORTAL_NETHER = register("ruined_portal_nether", StructureGenerator.RUINED_PORTAL.configured(new WorldGenFeatureRuinedPortalConfiguration(WorldGenFeatureRuinedPortal.Type.NETHER)));

    public static StructureFeature<?, ?> bootstrap() {
        return MINESHAFT;
    }

    private static <FC extends WorldGenFeatureConfiguration, F extends StructureGenerator<FC>> StructureFeature<FC, F> register(String id, StructureFeature<FC, F> configuredStructureFeature) {
        return RegistryGeneration.register(RegistryGeneration.CONFIGURED_STRUCTURE_FEATURE, id, configuredStructureFeature);
    }

    private static void register(BiConsumer<StructureFeature<?, ?>, ResourceKey<BiomeBase>> registrar, StructureFeature<?, ?> feature, Set<ResourceKey<BiomeBase>> biomes) {
        biomes.forEach((biome) -> {
            registrar.accept(feature, biome);
        });
    }

    private static void register(BiConsumer<StructureFeature<?, ?>, ResourceKey<BiomeBase>> registrar, StructureFeature<?, ?> feature, ResourceKey<BiomeBase> biome) {
        registrar.accept(feature, biome);
    }

    public static void registerStructures(BiConsumer<StructureFeature<?, ?>, ResourceKey<BiomeBase>> registrar) {
        Set<ResourceKey<BiomeBase>> set = ImmutableSet.<ResourceKey<BiomeBase>>builder().add(Biomes.DEEP_FROZEN_OCEAN).add(Biomes.DEEP_COLD_OCEAN).add(Biomes.DEEP_OCEAN).add(Biomes.DEEP_LUKEWARM_OCEAN).build();
        Set<ResourceKey<BiomeBase>> set2 = ImmutableSet.<ResourceKey<BiomeBase>>builder().add(Biomes.FROZEN_OCEAN).add(Biomes.OCEAN).add(Biomes.COLD_OCEAN).add(Biomes.LUKEWARM_OCEAN).add(Biomes.WARM_OCEAN).addAll(set).build();
        Set<ResourceKey<BiomeBase>> set3 = ImmutableSet.<ResourceKey<BiomeBase>>builder().add(Biomes.BEACH).add(Biomes.SNOWY_BEACH).build();
        Set<ResourceKey<BiomeBase>> set4 = ImmutableSet.<ResourceKey<BiomeBase>>builder().add(Biomes.RIVER).add(Biomes.FROZEN_RIVER).build();
        Set<ResourceKey<BiomeBase>> set5 = ImmutableSet.<ResourceKey<BiomeBase>>builder().add(Biomes.MEADOW).add(Biomes.FROZEN_PEAKS).add(Biomes.JAGGED_PEAKS).add(Biomes.STONY_PEAKS).add(Biomes.SNOWY_SLOPES).build();
        Set<ResourceKey<BiomeBase>> set6 = ImmutableSet.<ResourceKey<BiomeBase>>builder().add(Biomes.BADLANDS).add(Biomes.ERODED_BADLANDS).add(Biomes.WOODED_BADLANDS).build();
        Set<ResourceKey<BiomeBase>> set7 = ImmutableSet.<ResourceKey<BiomeBase>>builder().add(Biomes.WINDSWEPT_HILLS).add(Biomes.WINDSWEPT_FOREST).add(Biomes.WINDSWEPT_GRAVELLY_HILLS).build();
        Set<ResourceKey<BiomeBase>> set8 = ImmutableSet.<ResourceKey<BiomeBase>>builder().add(Biomes.TAIGA).add(Biomes.SNOWY_TAIGA).add(Biomes.OLD_GROWTH_PINE_TAIGA).add(Biomes.OLD_GROWTH_SPRUCE_TAIGA).build();
        Set<ResourceKey<BiomeBase>> set9 = ImmutableSet.<ResourceKey<BiomeBase>>builder().add(Biomes.BAMBOO_JUNGLE).add(Biomes.JUNGLE).add(Biomes.SPARSE_JUNGLE).build();
        Set<ResourceKey<BiomeBase>> set10 = ImmutableSet.<ResourceKey<BiomeBase>>builder().add(Biomes.FOREST).add(Biomes.FLOWER_FOREST).add(Biomes.BIRCH_FOREST).add(Biomes.OLD_GROWTH_BIRCH_FOREST).add(Biomes.DARK_FOREST).add(Biomes.GROVE).build();
        Set<ResourceKey<BiomeBase>> set11 = ImmutableSet.<ResourceKey<BiomeBase>>builder().add(Biomes.NETHER_WASTES).add(Biomes.BASALT_DELTAS).add(Biomes.SOUL_SAND_VALLEY).add(Biomes.CRIMSON_FOREST).add(Biomes.WARPED_FOREST).build();
        register(registrar, BURIED_TREASURE, set3);
        register(registrar, DESERT_PYRAMID, Biomes.DESERT);
        register(registrar, IGLOO, Biomes.SNOWY_TAIGA);
        register(registrar, IGLOO, Biomes.SNOWY_PLAINS);
        register(registrar, IGLOO, Biomes.SNOWY_SLOPES);
        register(registrar, JUNGLE_TEMPLE, Biomes.BAMBOO_JUNGLE);
        register(registrar, JUNGLE_TEMPLE, Biomes.JUNGLE);
        register(registrar, MINESHAFT, set2);
        register(registrar, MINESHAFT, set4);
        register(registrar, MINESHAFT, set3);
        register(registrar, MINESHAFT, Biomes.STONY_SHORE);
        register(registrar, MINESHAFT, set5);
        register(registrar, MINESHAFT, set7);
        register(registrar, MINESHAFT, set8);
        register(registrar, MINESHAFT, set9);
        register(registrar, MINESHAFT, set10);
        register(registrar, MINESHAFT, Biomes.MUSHROOM_FIELDS);
        register(registrar, MINESHAFT, Biomes.ICE_SPIKES);
        register(registrar, MINESHAFT, Biomes.WINDSWEPT_SAVANNA);
        register(registrar, MINESHAFT, Biomes.DESERT);
        register(registrar, MINESHAFT, Biomes.SAVANNA);
        register(registrar, MINESHAFT, Biomes.SNOWY_PLAINS);
        register(registrar, MINESHAFT, Biomes.PLAINS);
        register(registrar, MINESHAFT, Biomes.SUNFLOWER_PLAINS);
        register(registrar, MINESHAFT, Biomes.SWAMP);
        register(registrar, MINESHAFT, Biomes.SAVANNA_PLATEAU);
        register(registrar, MINESHAFT, Biomes.DRIPSTONE_CAVES);
        register(registrar, MINESHAFT, Biomes.LUSH_CAVES);
        register(registrar, MINESHAFT_MESA, set6);
        register(registrar, OCEAN_MONUMENT, set);
        register(registrar, OCEAN_RUIN_COLD, Biomes.FROZEN_OCEAN);
        register(registrar, OCEAN_RUIN_COLD, Biomes.COLD_OCEAN);
        register(registrar, OCEAN_RUIN_COLD, Biomes.OCEAN);
        register(registrar, OCEAN_RUIN_COLD, Biomes.DEEP_FROZEN_OCEAN);
        register(registrar, OCEAN_RUIN_COLD, Biomes.DEEP_COLD_OCEAN);
        register(registrar, OCEAN_RUIN_COLD, Biomes.DEEP_OCEAN);
        register(registrar, OCEAN_RUIN_WARM, Biomes.LUKEWARM_OCEAN);
        register(registrar, OCEAN_RUIN_WARM, Biomes.WARM_OCEAN);
        register(registrar, OCEAN_RUIN_WARM, Biomes.DEEP_LUKEWARM_OCEAN);
        register(registrar, PILLAGER_OUTPOST, Biomes.DESERT);
        register(registrar, PILLAGER_OUTPOST, Biomes.PLAINS);
        register(registrar, PILLAGER_OUTPOST, Biomes.SAVANNA);
        register(registrar, PILLAGER_OUTPOST, Biomes.SNOWY_PLAINS);
        register(registrar, PILLAGER_OUTPOST, Biomes.TAIGA);
        register(registrar, PILLAGER_OUTPOST, set5);
        register(registrar, PILLAGER_OUTPOST, Biomes.GROVE);
        register(registrar, RUINED_PORTAL_DESERT, Biomes.DESERT);
        register(registrar, RUINED_PORTAL_JUNGLE, set9);
        register(registrar, RUINED_PORTAL_OCEAN, set2);
        register(registrar, RUINED_PORTAL_SWAMP, Biomes.SWAMP);
        register(registrar, RUINED_PORTAL_MOUNTAIN, set6);
        register(registrar, RUINED_PORTAL_MOUNTAIN, set7);
        register(registrar, RUINED_PORTAL_MOUNTAIN, Biomes.SAVANNA_PLATEAU);
        register(registrar, RUINED_PORTAL_MOUNTAIN, Biomes.WINDSWEPT_SAVANNA);
        register(registrar, RUINED_PORTAL_MOUNTAIN, Biomes.STONY_SHORE);
        register(registrar, RUINED_PORTAL_MOUNTAIN, set5);
        register(registrar, RUINED_PORTAL_STANDARD, Biomes.MUSHROOM_FIELDS);
        register(registrar, RUINED_PORTAL_STANDARD, Biomes.ICE_SPIKES);
        register(registrar, RUINED_PORTAL_STANDARD, set3);
        register(registrar, RUINED_PORTAL_STANDARD, set4);
        register(registrar, RUINED_PORTAL_STANDARD, set8);
        register(registrar, RUINED_PORTAL_STANDARD, set10);
        register(registrar, RUINED_PORTAL_STANDARD, Biomes.DRIPSTONE_CAVES);
        register(registrar, RUINED_PORTAL_STANDARD, Biomes.LUSH_CAVES);
        register(registrar, RUINED_PORTAL_STANDARD, Biomes.SAVANNA);
        register(registrar, RUINED_PORTAL_STANDARD, Biomes.SNOWY_PLAINS);
        register(registrar, RUINED_PORTAL_STANDARD, Biomes.PLAINS);
        register(registrar, RUINED_PORTAL_STANDARD, Biomes.SUNFLOWER_PLAINS);
        register(registrar, SHIPWRECK_BEACHED, set3);
        register(registrar, SHIPWRECK, set2);
        register(registrar, SWAMP_HUT, Biomes.SWAMP);
        register(registrar, VILLAGE_DESERT, Biomes.DESERT);
        register(registrar, VILLAGE_PLAINS, Biomes.PLAINS);
        register(registrar, VILLAGE_PLAINS, Biomes.MEADOW);
        register(registrar, VILLAGE_SAVANNA, Biomes.SAVANNA);
        register(registrar, VILLAGE_SNOWY, Biomes.SNOWY_PLAINS);
        register(registrar, VILLAGE_TAIGA, Biomes.TAIGA);
        register(registrar, WOODLAND_MANSION, Biomes.DARK_FOREST);
        register(registrar, NETHER_BRIDGE, set11);
        register(registrar, NETHER_FOSSIL, Biomes.SOUL_SAND_VALLEY);
        register(registrar, BASTION_REMNANT, Biomes.CRIMSON_FOREST);
        register(registrar, BASTION_REMNANT, Biomes.NETHER_WASTES);
        register(registrar, BASTION_REMNANT, Biomes.SOUL_SAND_VALLEY);
        register(registrar, BASTION_REMNANT, Biomes.WARPED_FOREST);
        register(registrar, RUINED_PORTAL_NETHER, set11);
        register(registrar, END_CITY, Biomes.END_HIGHLANDS);
        register(registrar, END_CITY, Biomes.END_MIDLANDS);
    }
}
