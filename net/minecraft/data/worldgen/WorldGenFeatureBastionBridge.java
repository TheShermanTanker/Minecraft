package net.minecraft.data.worldgen;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.level.levelgen.feature.structures.WorldGenFeatureDefinedStructurePoolStructure;
import net.minecraft.world.level.levelgen.feature.structures.WorldGenFeatureDefinedStructurePoolTemplate;

public class WorldGenFeatureBastionBridge {
    public static void bootstrap() {
    }

    static {
        WorldGenFeaturePieces.register(new WorldGenFeatureDefinedStructurePoolTemplate(new MinecraftKey("bastion/bridge/starting_pieces"), new MinecraftKey("empty"), ImmutableList.of(Pair.of(WorldGenFeatureDefinedStructurePoolStructure.single("bastion/bridge/starting_pieces/entrance", WorldGenProcessorLists.ENTRANCE_REPLACEMENT), 1), Pair.of(WorldGenFeatureDefinedStructurePoolStructure.single("bastion/bridge/starting_pieces/entrance_face", WorldGenProcessorLists.BASTION_GENERIC_DEGRADATION), 1)), WorldGenFeatureDefinedStructurePoolTemplate.Matching.RIGID));
        WorldGenFeaturePieces.register(new WorldGenFeatureDefinedStructurePoolTemplate(new MinecraftKey("bastion/bridge/bridge_pieces"), new MinecraftKey("empty"), ImmutableList.of(Pair.of(WorldGenFeatureDefinedStructurePoolStructure.single("bastion/bridge/bridge_pieces/bridge", WorldGenProcessorLists.BRIDGE), 1)), WorldGenFeatureDefinedStructurePoolTemplate.Matching.RIGID));
        WorldGenFeaturePieces.register(new WorldGenFeatureDefinedStructurePoolTemplate(new MinecraftKey("bastion/bridge/legs"), new MinecraftKey("empty"), ImmutableList.of(Pair.of(WorldGenFeatureDefinedStructurePoolStructure.single("bastion/bridge/legs/leg_0", WorldGenProcessorLists.BASTION_GENERIC_DEGRADATION), 1), Pair.of(WorldGenFeatureDefinedStructurePoolStructure.single("bastion/bridge/legs/leg_1", WorldGenProcessorLists.BASTION_GENERIC_DEGRADATION), 1)), WorldGenFeatureDefinedStructurePoolTemplate.Matching.RIGID));
        WorldGenFeaturePieces.register(new WorldGenFeatureDefinedStructurePoolTemplate(new MinecraftKey("bastion/bridge/walls"), new MinecraftKey("empty"), ImmutableList.of(Pair.of(WorldGenFeatureDefinedStructurePoolStructure.single("bastion/bridge/walls/wall_base_0", WorldGenProcessorLists.RAMPART_DEGRADATION), 1), Pair.of(WorldGenFeatureDefinedStructurePoolStructure.single("bastion/bridge/walls/wall_base_1", WorldGenProcessorLists.RAMPART_DEGRADATION), 1)), WorldGenFeatureDefinedStructurePoolTemplate.Matching.RIGID));
        WorldGenFeaturePieces.register(new WorldGenFeatureDefinedStructurePoolTemplate(new MinecraftKey("bastion/bridge/ramparts"), new MinecraftKey("empty"), ImmutableList.of(Pair.of(WorldGenFeatureDefinedStructurePoolStructure.single("bastion/bridge/ramparts/rampart_0", WorldGenProcessorLists.RAMPART_DEGRADATION), 1), Pair.of(WorldGenFeatureDefinedStructurePoolStructure.single("bastion/bridge/ramparts/rampart_1", WorldGenProcessorLists.RAMPART_DEGRADATION), 1)), WorldGenFeatureDefinedStructurePoolTemplate.Matching.RIGID));
        WorldGenFeaturePieces.register(new WorldGenFeatureDefinedStructurePoolTemplate(new MinecraftKey("bastion/bridge/rampart_plates"), new MinecraftKey("empty"), ImmutableList.of(Pair.of(WorldGenFeatureDefinedStructurePoolStructure.single("bastion/bridge/rampart_plates/plate_0", WorldGenProcessorLists.RAMPART_DEGRADATION), 1)), WorldGenFeatureDefinedStructurePoolTemplate.Matching.RIGID));
        WorldGenFeaturePieces.register(new WorldGenFeatureDefinedStructurePoolTemplate(new MinecraftKey("bastion/bridge/connectors"), new MinecraftKey("empty"), ImmutableList.of(Pair.of(WorldGenFeatureDefinedStructurePoolStructure.single("bastion/bridge/connectors/back_bridge_top", WorldGenProcessorLists.BASTION_GENERIC_DEGRADATION), 1), Pair.of(WorldGenFeatureDefinedStructurePoolStructure.single("bastion/bridge/connectors/back_bridge_bottom", WorldGenProcessorLists.BASTION_GENERIC_DEGRADATION), 1)), WorldGenFeatureDefinedStructurePoolTemplate.Matching.RIGID));
    }
}
