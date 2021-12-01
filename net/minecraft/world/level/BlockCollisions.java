package net.minecraft.world.level;

import com.google.common.collect.AbstractIterator;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.CursorPosition;
import net.minecraft.core.SectionPosition;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.shapes.OperatorBoolean;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.phys.shapes.VoxelShapes;

public class BlockCollisions extends AbstractIterator<VoxelShape> {
    private final AxisAlignedBB box;
    private final VoxelShapeCollision context;
    private final CursorPosition cursor;
    private final BlockPosition.MutableBlockPosition pos;
    private final VoxelShape entityShape;
    private final ICollisionAccess collisionGetter;
    private final boolean onlySuffocatingBlocks;
    @Nullable
    private IBlockAccess cachedBlockGetter;
    private long cachedBlockGetterPos;

    public BlockCollisions(ICollisionAccess world, @Nullable Entity entity, AxisAlignedBB box) {
        this(world, entity, box, false);
    }

    public BlockCollisions(ICollisionAccess world, @Nullable Entity entity, AxisAlignedBB box, boolean forEntity) {
        this.context = entity == null ? VoxelShapeCollision.empty() : VoxelShapeCollision.of(entity);
        this.pos = new BlockPosition.MutableBlockPosition();
        this.entityShape = VoxelShapes.create(box);
        this.collisionGetter = world;
        this.box = box;
        this.onlySuffocatingBlocks = forEntity;
        int i = MathHelper.floor(box.minX - 1.0E-7D) - 1;
        int j = MathHelper.floor(box.maxX + 1.0E-7D) + 1;
        int k = MathHelper.floor(box.minY - 1.0E-7D) - 1;
        int l = MathHelper.floor(box.maxY + 1.0E-7D) + 1;
        int m = MathHelper.floor(box.minZ - 1.0E-7D) - 1;
        int n = MathHelper.floor(box.maxZ + 1.0E-7D) + 1;
        this.cursor = new CursorPosition(i, k, m, j, l, n);
    }

    @Nullable
    private IBlockAccess getChunk(int x, int z) {
        int i = SectionPosition.blockToSectionCoord(x);
        int j = SectionPosition.blockToSectionCoord(z);
        long l = ChunkCoordIntPair.pair(i, j);
        if (this.cachedBlockGetter != null && this.cachedBlockGetterPos == l) {
            return this.cachedBlockGetter;
        } else {
            IBlockAccess blockGetter = this.collisionGetter.getChunkForCollisions(i, j);
            this.cachedBlockGetter = blockGetter;
            this.cachedBlockGetterPos = l;
            return blockGetter;
        }
    }

    @Override
    protected VoxelShape computeNext() {
        while(true) {
            if (this.cursor.advance()) {
                int i = this.cursor.nextX();
                int j = this.cursor.nextY();
                int k = this.cursor.nextZ();
                int l = this.cursor.getNextType();
                if (l == 3) {
                    continue;
                }

                IBlockAccess blockGetter = this.getChunk(i, k);
                if (blockGetter == null) {
                    continue;
                }

                this.pos.set(i, j, k);
                IBlockData blockState = blockGetter.getType(this.pos);
                if (this.onlySuffocatingBlocks && !blockState.isSuffocating(blockGetter, this.pos) || l == 1 && !blockState.hasLargeCollisionShape() || l == 2 && !blockState.is(Blocks.MOVING_PISTON)) {
                    continue;
                }

                VoxelShape voxelShape = blockState.getCollisionShape(this.collisionGetter, this.pos, this.context);
                if (voxelShape == VoxelShapes.block()) {
                    if (!this.box.intersects((double)i, (double)j, (double)k, (double)i + 1.0D, (double)j + 1.0D, (double)k + 1.0D)) {
                        continue;
                    }

                    return voxelShape.move((double)i, (double)j, (double)k);
                }

                VoxelShape voxelShape2 = voxelShape.move((double)i, (double)j, (double)k);
                if (!VoxelShapes.joinIsNotEmpty(voxelShape2, this.entityShape, OperatorBoolean.AND)) {
                    continue;
                }

                return voxelShape2;
            }

            return this.endOfData();
        }
    }
}
