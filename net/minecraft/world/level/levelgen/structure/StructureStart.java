package net.minecraft.world.level.levelgen.structure;

import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.StructureGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public final class StructureStart<C extends WorldGenFeatureConfiguration> {
    public static final String INVALID_START_ID = "INVALID";
    public static final StructureStart<?> INVALID_START = new StructureStart((StructureGenerator<C>)null, new ChunkCoordIntPair(0, 0), 0, new PiecesContainer(List.of()));
    private final StructureGenerator<C> feature;
    private final PiecesContainer pieceContainer;
    private final ChunkCoordIntPair chunkPos;
    private int references;
    @Nullable
    private volatile StructureBoundingBox cachedBoundingBox;

    public StructureStart(StructureGenerator<C> feature, ChunkCoordIntPair pos, int references, PiecesContainer children) {
        this.feature = feature;
        this.chunkPos = pos;
        this.references = references;
        this.pieceContainer = children;
    }

    public StructureBoundingBox getBoundingBox() {
        StructureBoundingBox boundingBox = this.cachedBoundingBox;
        if (boundingBox == null) {
            boundingBox = this.feature.adjustBoundingBox(this.pieceContainer.calculateBoundingBox());
            this.cachedBoundingBox = boundingBox;
        }

        return boundingBox;
    }

    public void placeInChunk(GeneratorAccessSeed world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, StructureBoundingBox chunkBox, ChunkCoordIntPair chunkPos) {
        List<StructurePiece> list = this.pieceContainer.pieces();
        if (!list.isEmpty()) {
            StructureBoundingBox boundingBox = (list.get(0)).boundingBox;
            BlockPosition blockPos = boundingBox.getCenter();
            BlockPosition blockPos2 = new BlockPosition(blockPos.getX(), boundingBox.minY(), blockPos.getZ());

            for(StructurePiece structurePiece : list) {
                if (structurePiece.getBoundingBox().intersects(chunkBox)) {
                    structurePiece.postProcess(world, structureAccessor, chunkGenerator, random, chunkBox, chunkPos, blockPos2);
                }
            }

            this.feature.getPostPlacementProcessor().afterPlace(world, structureAccessor, chunkGenerator, random, chunkBox, chunkPos, this.pieceContainer);
        }
    }

    public NBTTagCompound createTag(StructurePieceSerializationContext context, ChunkCoordIntPair chunkPos) {
        NBTTagCompound compoundTag = new NBTTagCompound();
        if (this.isValid()) {
            compoundTag.setString("id", IRegistry.STRUCTURE_FEATURE.getKey(this.getFeature()).toString());
            compoundTag.setInt("ChunkX", chunkPos.x);
            compoundTag.setInt("ChunkZ", chunkPos.z);
            compoundTag.setInt("references", this.references);
            compoundTag.set("Children", this.pieceContainer.save(context));
            return compoundTag;
        } else {
            compoundTag.setString("id", "INVALID");
            return compoundTag;
        }
    }

    public boolean isValid() {
        return !this.pieceContainer.isEmpty();
    }

    public ChunkCoordIntPair getChunkPos() {
        return this.chunkPos;
    }

    public boolean canBeReferenced() {
        return this.references < this.getMaxReferences();
    }

    public void addReference() {
        ++this.references;
    }

    public int getReferences() {
        return this.references;
    }

    protected int getMaxReferences() {
        return 1;
    }

    public StructureGenerator<?> getFeature() {
        return this.feature;
    }

    public List<StructurePiece> getPieces() {
        return this.pieceContainer.pieces();
    }
}
