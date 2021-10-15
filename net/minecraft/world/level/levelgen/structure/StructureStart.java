package net.minecraft.world.level.levelgen.structure;

import com.google.common.collect.Lists;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.IWorldHeightAccess;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.SeededRandom;
import net.minecraft.world.level.levelgen.feature.StructureGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenMineshaftConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class StructureStart<C extends WorldGenFeatureConfiguration> implements StructurePieceAccessor {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final String INVALID_START_ID = "INVALID";
    public static final StructureStart<?> INVALID_START = new StructureStart<WorldGenMineshaftConfiguration>((StructureGenerator)null, new ChunkCoordIntPair(0, 0), 0, 0L) {
        @Override
        public void generatePieces(IRegistryCustom registryManager, ChunkGenerator chunkGenerator, DefinedStructureManager manager, ChunkCoordIntPair pos, BiomeBase biome, WorldGenMineshaftConfiguration config, IWorldHeightAccess world) {
        }

        @Override
        public boolean isValid() {
            return false;
        }
    };
    private final StructureGenerator<C> feature;
    protected final List<StructurePiece> pieces = Lists.newArrayList();
    private final ChunkCoordIntPair chunkPos;
    private int references;
    protected final SeededRandom random;
    @Nullable
    private StructureBoundingBox cachedBoundingBox;

    public StructureStart(StructureGenerator<C> feature, ChunkCoordIntPair pos, int references, long seed) {
        this.feature = feature;
        this.chunkPos = pos;
        this.references = references;
        this.random = new SeededRandom();
        this.random.setLargeFeatureSeed(seed, pos.x, pos.z);
    }

    public abstract void generatePieces(IRegistryCustom registryManager, ChunkGenerator chunkGenerator, DefinedStructureManager manager, ChunkCoordIntPair pos, BiomeBase biome, C config, IWorldHeightAccess world);

    public final StructureBoundingBox getBoundingBox() {
        if (this.cachedBoundingBox == null) {
            this.cachedBoundingBox = this.createBoundingBox();
        }

        return this.cachedBoundingBox;
    }

    protected StructureBoundingBox createBoundingBox() {
        synchronized(this.pieces) {
            return StructureBoundingBox.encapsulatingBoxes(this.pieces.stream().map(StructurePiece::getBoundingBox)::iterator).orElseThrow(() -> {
                return new IllegalStateException("Unable to calculate boundingbox without pieces");
            });
        }
    }

    public List<StructurePiece> getPieces() {
        return this.pieces;
    }

    public void placeInChunk(GeneratorAccessSeed world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, StructureBoundingBox box, ChunkCoordIntPair chunkPos) {
        synchronized(this.pieces) {
            if (!this.pieces.isEmpty()) {
                StructureBoundingBox boundingBox = (this.pieces.get(0)).boundingBox;
                BlockPosition blockPos = boundingBox.getCenter();
                BlockPosition blockPos2 = new BlockPosition(blockPos.getX(), boundingBox.minY(), blockPos.getZ());
                Iterator<StructurePiece> iterator = this.pieces.iterator();

                while(iterator.hasNext()) {
                    StructurePiece structurePiece = iterator.next();
                    if (structurePiece.getBoundingBox().intersects(box) && !structurePiece.postProcess(world, structureAccessor, chunkGenerator, random, box, chunkPos, blockPos2)) {
                        iterator.remove();
                    }
                }

            }
        }
    }

    public NBTTagCompound createTag(WorldServer world, ChunkCoordIntPair chunkPos) {
        NBTTagCompound compoundTag = new NBTTagCompound();
        if (this.isValid()) {
            compoundTag.setString("id", IRegistry.STRUCTURE_FEATURE.getKey(this.getFeature()).toString());
            compoundTag.setInt("ChunkX", chunkPos.x);
            compoundTag.setInt("ChunkZ", chunkPos.z);
            compoundTag.setInt("references", this.references);
            NBTTagList listTag = new NBTTagList();
            synchronized(this.pieces) {
                for(StructurePiece structurePiece : this.pieces) {
                    listTag.add(structurePiece.createTag(world));
                }
            }

            compoundTag.set("Children", listTag);
            return compoundTag;
        } else {
            compoundTag.setString("id", "INVALID");
            return compoundTag;
        }
    }

    protected void moveBelowSeaLevel(int seaLevel, int i, Random random, int j) {
        int k = seaLevel - j;
        StructureBoundingBox boundingBox = this.getBoundingBox();
        int l = boundingBox.getYSpan() + i + 1;
        if (l < k) {
            l += random.nextInt(k - l);
        }

        int m = l - boundingBox.maxY();
        this.offsetPiecesVertically(m);
    }

    protected void moveInsideHeights(Random random, int minY, int maxY) {
        StructureBoundingBox boundingBox = this.getBoundingBox();
        int i = maxY - minY + 1 - boundingBox.getYSpan();
        int j;
        if (i > 1) {
            j = minY + random.nextInt(i);
        } else {
            j = minY;
        }

        int l = j - boundingBox.minY();
        this.offsetPiecesVertically(l);
    }

    protected void offsetPiecesVertically(int amount) {
        for(StructurePiece structurePiece : this.pieces) {
            structurePiece.move(0, amount, 0);
        }

        this.invalidateCache();
    }

    private void invalidateCache() {
        this.cachedBoundingBox = null;
    }

    public boolean isValid() {
        return !this.pieces.isEmpty();
    }

    public ChunkCoordIntPair getChunkPos() {
        return this.chunkPos;
    }

    public BlockPosition getLocatePos() {
        return new BlockPosition(this.chunkPos.getMinBlockX(), 0, this.chunkPos.getMinBlockZ());
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

    @Override
    public void addPiece(StructurePiece piece) {
        this.pieces.add(piece);
        this.invalidateCache();
    }

    @Nullable
    @Override
    public StructurePiece findCollisionPiece(StructureBoundingBox box) {
        return findCollisionPiece(this.pieces, box);
    }

    public void clearPieces() {
        this.pieces.clear();
        this.invalidateCache();
    }

    public boolean hasNoPieces() {
        return this.pieces.isEmpty();
    }

    @Nullable
    public static StructurePiece findCollisionPiece(List<StructurePiece> pieces, StructureBoundingBox box) {
        for(StructurePiece structurePiece : pieces) {
            if (structurePiece.getBoundingBox().intersects(box)) {
                return structurePiece;
            }
        }

        return null;
    }

    protected boolean isInsidePiece(BlockPosition pos) {
        for(StructurePiece structurePiece : this.pieces) {
            if (structurePiece.getBoundingBox().isInside(pos)) {
                return true;
            }
        }

        return false;
    }
}
