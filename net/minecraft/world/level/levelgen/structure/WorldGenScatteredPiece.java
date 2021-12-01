package net.minecraft.world.level.levelgen.structure;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureStructurePieceType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public abstract class WorldGenScatteredPiece extends StructurePiece {
    protected final int width;
    protected final int height;
    protected final int depth;
    protected int heightPosition = -1;

    protected WorldGenScatteredPiece(WorldGenFeatureStructurePieceType type, int x, int y, int z, int width, int height, int depth, EnumDirection orientation) {
        super(type, 0, StructurePiece.makeBoundingBox(x, y, z, orientation, width, height, depth));
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.setOrientation(orientation);
    }

    protected WorldGenScatteredPiece(WorldGenFeatureStructurePieceType type, NBTTagCompound nbt) {
        super(type, nbt);
        this.width = nbt.getInt("Width");
        this.height = nbt.getInt("Height");
        this.depth = nbt.getInt("Depth");
        this.heightPosition = nbt.getInt("HPos");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, NBTTagCompound nbt) {
        nbt.setInt("Width", this.width);
        nbt.setInt("Height", this.height);
        nbt.setInt("Depth", this.depth);
        nbt.setInt("HPos", this.heightPosition);
    }

    protected boolean updateAverageGroundHeight(GeneratorAccess world, StructureBoundingBox boundingBox, int deltaY) {
        if (this.heightPosition >= 0) {
            return true;
        } else {
            int i = 0;
            int j = 0;
            BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

            for(int k = this.boundingBox.minZ(); k <= this.boundingBox.maxZ(); ++k) {
                for(int l = this.boundingBox.minX(); l <= this.boundingBox.maxX(); ++l) {
                    mutableBlockPos.set(l, 64, k);
                    if (boundingBox.isInside(mutableBlockPos)) {
                        i += world.getHighestBlockYAt(HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, mutableBlockPos).getY();
                        ++j;
                    }
                }
            }

            if (j == 0) {
                return false;
            } else {
                this.heightPosition = i / j;
                this.boundingBox.move(0, this.heightPosition - this.boundingBox.minY() + deltaY, 0);
                return true;
            }
        }
    }

    protected boolean updateHeightPositionToLowestGroundHeight(GeneratorAccess world, int i) {
        if (this.heightPosition >= 0) {
            return true;
        } else {
            int j = world.getMaxBuildHeight();
            boolean bl = false;
            BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

            for(int k = this.boundingBox.minZ(); k <= this.boundingBox.maxZ(); ++k) {
                for(int l = this.boundingBox.minX(); l <= this.boundingBox.maxX(); ++l) {
                    mutableBlockPos.set(l, 0, k);
                    j = Math.min(j, world.getHighestBlockYAt(HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, mutableBlockPos).getY());
                    bl = true;
                }
            }

            if (!bl) {
                return false;
            } else {
                this.heightPosition = j;
                this.boundingBox.move(0, this.heightPosition - this.boundingBox.minY() + i, 0);
                return true;
            }
        }
    }
}
