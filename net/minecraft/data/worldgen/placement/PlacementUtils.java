package net.minecraft.data.worldgen.placement;

import java.util.Random;
import net.minecraft.SystemUtils;
import net.minecraft.core.IRegistry;
import net.minecraft.data.RegistryGeneration;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviderConstant;
import net.minecraft.util.valueproviders.WeightedListInt;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.HeightmapPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;

public class PlacementUtils {
    public static final PlacementModifier HEIGHTMAP = HeightmapPlacement.onHeightmap(HeightMap.Type.MOTION_BLOCKING);
    public static final PlacementModifier HEIGHTMAP_TOP_SOLID = HeightmapPlacement.onHeightmap(HeightMap.Type.OCEAN_FLOOR_WG);
    public static final PlacementModifier HEIGHTMAP_WORLD_SURFACE = HeightmapPlacement.onHeightmap(HeightMap.Type.WORLD_SURFACE_WG);
    public static final PlacementModifier HEIGHTMAP_OCEAN_FLOOR = HeightmapPlacement.onHeightmap(HeightMap.Type.OCEAN_FLOOR);
    public static final PlacementModifier FULL_RANGE = HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.top());
    public static final PlacementModifier RANGE_10_10 = HeightRangePlacement.uniform(VerticalAnchor.aboveBottom(10), VerticalAnchor.belowTop(10));
    public static final PlacementModifier RANGE_8_8 = HeightRangePlacement.uniform(VerticalAnchor.aboveBottom(8), VerticalAnchor.belowTop(8));
    public static final PlacementModifier RANGE_4_4 = HeightRangePlacement.uniform(VerticalAnchor.aboveBottom(4), VerticalAnchor.belowTop(4));
    public static final PlacementModifier RANGE_BOTTOM_TO_MAX_TERRAIN_HEIGHT = HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(256));

    public static PlacedFeature bootstrap() {
        PlacedFeature[] placedFeatures = new PlacedFeature[]{AquaticPlacements.KELP_COLD, CavePlacements.CAVE_VINES, EndPlacements.CHORUS_PLANT, MiscOverworldPlacements.BLUE_ICE, NetherPlacements.BASALT_BLOBS, OrePlacements.ORE_ANCIENT_DEBRIS_LARGE, TreePlacements.ACACIA_CHECKED, VegetationPlacements.BAMBOO_VEGETATION, VillagePlacements.PILE_HAY_VILLAGE};
        return SystemUtils.getRandom(placedFeatures, new Random());
    }

    public static PlacedFeature register(String id, PlacedFeature feature) {
        return IRegistry.register(RegistryGeneration.PLACED_FEATURE, id, feature);
    }

    public static PlacementModifier countExtra(int count, float extraChance, int extraCount) {
        float f = 1.0F / extraChance;
        if (Math.abs(f - (float)((int)f)) > 1.0E-5F) {
            throw new IllegalStateException("Chance data cannot be represented as list weight");
        } else {
            SimpleWeightedRandomList<IntProvider> simpleWeightedRandomList = SimpleWeightedRandomList.<IntProvider>builder().add(IntProviderConstant.of(count), (int)f - 1).add(IntProviderConstant.of(count + extraCount), 1).build();
            return CountPlacement.of(new WeightedListInt(simpleWeightedRandomList));
        }
    }
}
