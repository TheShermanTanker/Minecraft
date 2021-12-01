package net.minecraft.world.level;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.SectionPosition;
import net.minecraft.server.level.RegionLimitedWorldAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.IStructureAccess;
import net.minecraft.world.level.levelgen.GeneratorSettings;
import net.minecraft.world.level.levelgen.feature.StructureGenerator;
import net.minecraft.world.level.levelgen.structure.StructureCheck;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;

public class StructureManager {
    private final GeneratorAccess level;
    private final GeneratorSettings worldGenSettings;
    private final StructureCheck structureCheck;

    public StructureManager(GeneratorAccess world, GeneratorSettings options, StructureCheck locator) {
        this.level = world;
        this.worldGenSettings = options;
        this.structureCheck = locator;
    }

    public StructureManager forWorldGenRegion(RegionLimitedWorldAccess region) {
        if (region.getLevel() != this.level) {
            throw new IllegalStateException("Using invalid feature manager (source level: " + region.getLevel() + ", region: " + region);
        } else {
            return new StructureManager(region, this.worldGenSettings, this.structureCheck);
        }
    }

    public List<? extends StructureStart<?>> startsForFeature(SectionPosition sectionPos, StructureGenerator<?> feature) {
        LongSet longSet = this.level.getChunkAt(sectionPos.x(), sectionPos.z(), ChunkStatus.STRUCTURE_REFERENCES).getReferencesForFeature(feature);
        Builder<StructureStart<?>> builder = ImmutableList.builder();

        for(long l : longSet) {
            SectionPosition sectionPos2 = SectionPosition.of(new ChunkCoordIntPair(l), this.level.getMinSection());
            StructureStart<?> structureStart = this.getStartForFeature(sectionPos2, feature, this.level.getChunkAt(sectionPos2.x(), sectionPos2.z(), ChunkStatus.STRUCTURE_STARTS));
            if (structureStart != null && structureStart.isValid()) {
                builder.add(structureStart);
            }
        }

        return builder.build();
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

    public StructureStart<?> getStructureAt(BlockPosition pos, StructureGenerator<?> structure) {
        for(StructureStart<?> structureStart : this.startsForFeature(SectionPosition.of(pos), structure)) {
            if (structureStart.getBoundingBox().isInside(pos)) {
                return structureStart;
            }
        }

        return StructureStart.INVALID_START;
    }

    public StructureStart<?> getStructureWithPieceAt(BlockPosition pos, StructureGenerator<?> structure) {
        for(StructureStart<?> structureStart : this.startsForFeature(SectionPosition.of(pos), structure)) {
            for(StructurePiece structurePiece : structureStart.getPieces()) {
                if (structurePiece.getBoundingBox().isInside(pos)) {
                    return structureStart;
                }
            }
        }

        return StructureStart.INVALID_START;
    }

    public boolean hasAnyStructureAt(BlockPosition pos) {
        SectionPosition sectionPos = SectionPosition.of(pos);
        return this.level.getChunkAt(sectionPos.x(), sectionPos.z(), ChunkStatus.STRUCTURE_REFERENCES).hasAnyStructureReferences();
    }

    public StructureCheckResult checkStructurePresence(ChunkCoordIntPair chunkPos, StructureGenerator<?> structure, boolean skipExistingChunk) {
        return this.structureCheck.checkStart(chunkPos, structure, skipExistingChunk);
    }

    public void addReference(StructureStart<?> structureStart) {
        structureStart.addReference();
        this.structureCheck.incrementReference(structureStart.getChunkPos(), structureStart.getFeature());
    }
}
