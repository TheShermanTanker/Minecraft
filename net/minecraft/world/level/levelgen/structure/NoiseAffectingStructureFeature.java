package net.minecraft.world.level.levelgen.structure;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.levelgen.feature.StructureGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGeneratorSupplier;

public abstract class NoiseAffectingStructureFeature<C extends WorldGenFeatureConfiguration> extends StructureGenerator<C> {
    public NoiseAffectingStructureFeature(Codec<C> configCodec, PieceGeneratorSupplier<C> piecesGenerator) {
        super(configCodec, piecesGenerator);
    }

    public NoiseAffectingStructureFeature(Codec<C> configCodec, PieceGeneratorSupplier<C> piecesGenerator, PostPlacementProcessor postPlacementProcessor) {
        super(configCodec, piecesGenerator, postPlacementProcessor);
    }

    @Override
    public StructureBoundingBox adjustBoundingBox(StructureBoundingBox box) {
        return super.adjustBoundingBox(box).inflatedBy(12);
    }
}
