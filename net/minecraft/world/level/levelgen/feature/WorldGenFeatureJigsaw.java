package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.BlockPosition;
import net.minecraft.data.worldgen.WorldGenFeaturePieces;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureVillageConfiguration;
import net.minecraft.world.level.levelgen.feature.structures.WorldGenFeatureDefinedStructureJigsawPlacement;
import net.minecraft.world.level.levelgen.structure.NoiseAffectingStructureFeature;
import net.minecraft.world.level.levelgen.structure.WorldGenFeaturePillagerOutpostPoolPiece;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGeneratorSupplier;

public class WorldGenFeatureJigsaw extends NoiseAffectingStructureFeature<WorldGenFeatureVillageConfiguration> {
    public WorldGenFeatureJigsaw(Codec<WorldGenFeatureVillageConfiguration> codec, int structureStartY, boolean modifyBoundingBox, boolean surface, Predicate<PieceGeneratorSupplier.Context<WorldGenFeatureVillageConfiguration>> predicate) {
        super(codec, (context) -> {
            if (!predicate.test(context)) {
                return Optional.empty();
            } else {
                BlockPosition blockPos = new BlockPosition(context.chunkPos().getMinBlockX(), structureStartY, context.chunkPos().getMinBlockZ());
                WorldGenFeaturePieces.bootstrap();
                return WorldGenFeatureDefinedStructureJigsawPlacement.addPieces(context, WorldGenFeaturePillagerOutpostPoolPiece::new, blockPos, modifyBoundingBox, surface);
            }
        });
    }
}
