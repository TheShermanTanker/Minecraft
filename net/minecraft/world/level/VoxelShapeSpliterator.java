package net.minecraft.world.level;

import java.util.Objects;
import java.util.Spliterators.AbstractSpliterator;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.CursorPosition;
import net.minecraft.core.SectionPosition;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.shapes.OperatorBoolean;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.phys.shapes.VoxelShapes;

public class VoxelShapeSpliterator extends AbstractSpliterator<VoxelShape> {
    @Nullable
    private final Entity source;
    private final AxisAlignedBB box;
    private final VoxelShapeCollision context;
    private final CursorPosition cursor;
    private final BlockPosition.MutableBlockPosition pos;
    private final VoxelShape entityShape;
    private final ICollisionAccess collisionGetter;
    private boolean needsBorderCheck;
    private final BiPredicate<IBlockData, BlockPosition> predicate;

    public VoxelShapeSpliterator(ICollisionAccess world, @Nullable Entity entity, AxisAlignedBB box) {
        this(world, entity, box, (state, pos) -> {
            return true;
        });
    }

    public VoxelShapeSpliterator(ICollisionAccess world, @Nullable Entity entity, AxisAlignedBB box, BiPredicate<IBlockData, BlockPosition> blockPredicate) {
        super(Long.MAX_VALUE, 1280);
        this.context = entity == null ? VoxelShapeCollision.empty() : VoxelShapeCollision.of(entity);
        this.pos = new BlockPosition.MutableBlockPosition();
        this.entityShape = VoxelShapes.create(box);
        this.collisionGetter = world;
        this.needsBorderCheck = entity != null;
        this.source = entity;
        this.box = box;
        this.predicate = blockPredicate;
        int i = MathHelper.floor(box.minX - 1.0E-7D) - 1;
        int j = MathHelper.floor(box.maxX + 1.0E-7D) + 1;
        int k = MathHelper.floor(box.minY - 1.0E-7D) - 1;
        int l = MathHelper.floor(box.maxY + 1.0E-7D) + 1;
        int m = MathHelper.floor(box.minZ - 1.0E-7D) - 1;
        int n = MathHelper.floor(box.maxZ + 1.0E-7D) + 1;
        this.cursor = new CursorPosition(i, k, m, j, l, n);
    }

    @Override
    public boolean tryAdvance(Consumer<? super VoxelShape> consumer) {
        return this.needsBorderCheck && this.worldBorderCheck(consumer) || this.collisionCheck(consumer);
    }

    boolean collisionCheck(Consumer<? super VoxelShape> action) {
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
                if (!this.predicate.test(blockState, this.pos) || l == 1 && !blockState.hasLargeCollisionShape() || l == 2 && !blockState.is(Blocks.MOVING_PISTON)) {
                    continue;
                }

                VoxelShape voxelShape = blockState.getCollisionShape(this.collisionGetter, this.pos, this.context);
                if (voxelShape == VoxelShapes.block()) {
                    if (!this.box.intersects((double)i, (double)j, (double)k, (double)i + 1.0D, (double)j + 1.0D, (double)k + 1.0D)) {
                        continue;
                    }

                    action.accept(voxelShape.move((double)i, (double)j, (double)k));
                    return true;
                }

                VoxelShape voxelShape2 = voxelShape.move((double)i, (double)j, (double)k);
                if (!VoxelShapes.joinIsNotEmpty(voxelShape2, this.entityShape, OperatorBoolean.AND)) {
                    continue;
                }

                action.accept(voxelShape2);
                return true;
            }

            return false;
        }
    }

    @Nullable
    private IBlockAccess getChunk(int x, int z) {
        int i = SectionPosition.blockToSectionCoord(x);
        int j = SectionPosition.blockToSectionCoord(z);
        return this.collisionGetter.getChunkForCollisions(i, j);
    }

    boolean worldBorderCheck(Consumer<? super VoxelShape> action) {
        Objects.requireNonNull(this.source);
        this.needsBorderCheck = false;
        WorldBorder worldBorder = this.collisionGetter.getWorldBorder();
        AxisAlignedBB aABB = this.source.getBoundingBox();
        if (!isBoxFullyWithinWorldBorder(worldBorder, aABB)) {
            VoxelShape voxelShape = worldBorder.getCollisionShape();
            if (!isOutsideBorder(voxelShape, aABB) && isCloseToBorder(voxelShape, aABB)) {
                action.accept(voxelShape);
                return true;
            }
        }

        return false;
    }

    private static boolean isCloseToBorder(VoxelShape worldBorderShape, AxisAlignedBB entityBox) {
        return VoxelShapes.joinIsNotEmpty(worldBorderShape, VoxelShapes.create(entityBox.inflate(1.0E-7D)), OperatorBoolean.AND);
    }

    private static boolean isOutsideBorder(VoxelShape worldBorderShape, AxisAlignedBB entityBox) {
        return VoxelShapes.joinIsNotEmpty(worldBorderShape, VoxelShapes.create(entityBox.shrink(1.0E-7D)), OperatorBoolean.AND);
    }

    public static boolean isBoxFullyWithinWorldBorder(WorldBorder border, AxisAlignedBB box) {
        double d = (double)MathHelper.floor(border.getMinX());
        double e = (double)MathHelper.floor(border.getMinZ());
        double f = (double)MathHelper.ceil(border.getMaxX());
        double g = (double)MathHelper.ceil(border.getMaxZ());
        return box.minX > d && box.minX < f && box.minZ > e && box.minZ < g && box.maxX > d && box.maxX < f && box.maxZ > e && box.maxZ < g;
    }
}
