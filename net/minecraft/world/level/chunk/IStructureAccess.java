package net.minecraft.world.level.chunk;

import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.world.level.levelgen.feature.StructureGenerator;
import net.minecraft.world.level.levelgen.structure.StructureStart;

public interface IStructureAccess {
    @Nullable
    StructureStart<?> getStartForFeature(StructureGenerator<?> structure);

    void setStartForFeature(StructureGenerator<?> structure, StructureStart<?> start);

    LongSet getReferencesForFeature(StructureGenerator<?> structure);

    void addReferenceForFeature(StructureGenerator<?> structure, long reference);

    Map<StructureGenerator<?>, LongSet> getAllReferences();

    void setAllReferences(Map<StructureGenerator<?>, LongSet> structureReferences);
}
