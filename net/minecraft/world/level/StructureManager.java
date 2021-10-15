package net.minecraft.world.level;

import com.mojang.datafixers.DataFixUtils;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.SectionPosition;
import net.minecraft.server.level.RegionLimitedWorldAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.IStructureAccess;
import net.minecraft.world.level.levelgen.GeneratorSettings;
import net.minecraft.world.level.levelgen.feature.StructureGenerator;
import net.minecraft.world.level.levelgen.structure.StructureStart;

public class StructureManager {
    private final GeneratorAccess level;
    private final GeneratorSettings worldGenSettings;

    public StructureManager(GeneratorAccess world, GeneratorSettings options) {
        this.level = world;
        this.worldGenSettings = options;
    }

    public StructureManager forWorldGenRegion(RegionLimitedWorldAccess region) {
        if (region.getLevel() != this.level) {
            throw new IllegalStateException("Using invalid feature manager (source level: " + region.getLevel() + ", region: " + region);
        } else {
            return new StructureManager(region, this.worldGenSettings);
        }
    }

    public Stream<? extends StructureStart<?>> startsForFeature(SectionPosition pos, StructureGenerator<?> feature) {
        return this.level.getChunkAt(pos.x(), pos.z(), ChunkStatus.STRUCTURE_REFERENCES).getReferencesForFeature(feature).stream().map((long_) -> {
            return SectionPosition.of(new ChunkCoordIntPair(long_), this.level.getMinSection());
        }).map((posx) -> {
            return this.getStartForFeature(posx, feature, this.level.getChunkAt(posx.x(), posx.z(), ChunkStatus.STRUCTURE_STARTS));
        }).filter((structureStart) -> {
            return structureStart != null && structureStart.isValid();
        });
    }

    @Nullable
    public StructureStart<?> getStartForFeature(SectionPosition pos, StructureGenerator<?> feature, IStructureAccess holder) {
        return holder.getStartForFeature(feature);
    }

    public void setStartForFeature(SectionPosition pos, StructureGenerator<?> feature, StructureStart<?> structureStart, IStructureAccess holder) {
        holder.setStartForFeature(feature, structureStart);
    }

    public void addReferenceForFeature(SectionPosition pos, StructureGenerator<?> feature, long reference, IStructureAccess holder) {
        holder.addReferenceForFeature(feature, reference);
    }

    public boolean shouldGenerateFeatures() {
        return this.worldGenSettings.shouldGenerateMapFeatures();
    }

    public StructureStart<?> getStructureAt(BlockPosition pos, boolean matchChildren, StructureGenerator<?> feature) {
        return DataFixUtils.orElse(this.startsForFeature(SectionPosition.of(pos), feature).filter((structureStart) -> {
            return matchChildren ? structureStart.getPieces().stream().anyMatch((piece) -> {
                return piece.getBoundingBox().isInside(pos);
            }) : structureStart.getBoundingBox().isInside(pos);
        }).findFirst(), StructureStart.INVALID_START);
    }
}
