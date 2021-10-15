package net.minecraft.world.level.levelgen.structure;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureStructurePieceType;

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
    protected void addAdditionalSaveData(WorldServer world, NBTTagCompound nbt) {
        nbt.setInt("Width", this.width);
        nbt.setInt("Height", this.height);
        nbt.setInt("Depth", this.depth);
        nbt.setInt("HPos", this.heightPosition);
    }

    protected boolean updateAverageGroundHeight(GeneratorAccess world, StructureBoundingBox boundingBox, int i) {
        if (this.heightPosition >= 0) {
            return true;
        } else {
            int j = 0;
            int k = 0;
            BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

            for(int l = this.boundingBox.minZ(); l <= this.boundingBox.maxZ(); ++l) {
                for(int m = this.boundingBox.minX(); m <= this.boundingBox.maxX(); ++m) {
                    mutableBlockPos.set(m, 64, l);
                    if (boundingBox.isInside(mutableBlockPos)) {
                        j += world.getHighestBlockYAt(HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, mutableBlockPos).getY();
                        ++k;
                    }
                }
            }

            if (k == 0) {
                return false;
            } else {
                this.heightPosition = j / k;
                this.boundingBox.move(0, this.heightPosition - this.boundingBox.minY() + i, 0);
                return true;
            }
        }
    }
}
