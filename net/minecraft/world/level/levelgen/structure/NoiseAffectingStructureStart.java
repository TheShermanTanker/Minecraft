package net.minecraft.world.level.levelgen.structure;

import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.levelgen.feature.StructureGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureConfiguration;

public abstract class NoiseAffectingStructureStart<C extends WorldGenFeatureConfiguration> extends StructureStart<C> {
    public NoiseAffectingStructureStart(StructureGenerator<C> feature, ChunkCoordIntPair pos, int references, long seed) {
        super(feature, pos, references, seed);
    }

    @Override
    protected StructureBoundingBox createBoundingBox() {
        return super.createBoundingBox().inflate(12);
    }
}
