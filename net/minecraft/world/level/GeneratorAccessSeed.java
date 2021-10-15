package net.minecraft.world.level;

import java.util.stream.Stream;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.SectionPosition;
import net.minecraft.world.level.levelgen.feature.StructureGenerator;
import net.minecraft.world.level.levelgen.structure.StructureStart;

public interface GeneratorAccessSeed extends WorldAccess {
    long getSeed();

    Stream<? extends StructureStart<?>> startsForFeature(SectionPosition pos, StructureGenerator<?> feature);

    default boolean ensureCanWrite(BlockPosition blockPos) {
        return true;
    }
}
