package net.minecraft.world.level;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityTypes;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.Vec3D;
import net.minecraft.world.phys.shapes.VoxelShape;

public interface IBlockAccess extends IWorldHeightAccess {
    @Nullable
    TileEntity getTileEntity(BlockPosition pos);

    default <T extends TileEntity> Optional<T> getBlockEntity(BlockPosition pos, TileEntityTypes<T> type) {
        TileEntity blockEntity = this.getTileEntity(pos);
        return blockEntity != null && blockEntity.getTileType() == type ? Optional.of((T)blockEntity) : Optional.empty();
    }

    IBlockData getType(BlockPosition pos);

    Fluid getFluid(BlockPosition pos);

    default int getLightEmission(BlockPosition pos) {
        return this.getType(pos).getLightEmission();
    }

    default int getMaxLightLevel() {
        return 15;
    }

    default Stream<IBlockData> getBlockStates(AxisAlignedBB box) {
        return BlockPosition.betweenClosedStream(box).map(this::getType);
    }

    default MovingObjectPositionBlock isBlockInLine(ClipBlockStateContext context) {
        return traverseBlocks(context.getFrom(), context.getTo(), context, (contextx, pos) -> {
            IBlockData blockState = this.getType(pos);
            Vec3D vec3 = contextx.getFrom().subtract(contextx.getTo());
            return contextx.isTargetBlock().test(blockState) ? new MovingObjectPositionBlock(contextx.getTo(), EnumDirection.getNearest(vec3.x, vec3.y, vec3.z), new BlockPosition(contextx.getTo()), false) : null;
        }, (contextx) -> {
            Vec3D vec3 = contextx.getFrom().subtract(contextx.getTo());
            return MovingObjectPositionBlock.miss(contextx.getTo(), EnumDirection.getNearest(vec3.x, vec3.y, vec3.z), new BlockPosition(contextx.getTo()));
        });
    }

    default MovingObjectPositionBlock rayTrace(RayTrace context) {
        return traverseBlocks(context.getFrom(), context.getTo(), context, (contextx, pos) -> {
            IBlockData blockState = this.getType(pos);
            Fluid fluidState = this.getFluid(pos);
            Vec3D vec3 = contextx.getFrom();
            Vec3D vec32 = contextx.getTo();
            VoxelShape voxelShape = contextx.getBlockShape(blockState, this, pos);
            MovingObjectPositionBlock blockHitResult = this.rayTrace(vec3, vec32, pos, voxelShape, blockState);
            VoxelShape voxelShape2 = contextx.getFluidShape(fluidState, this, pos);
            MovingObjectPositionBlock blockHitResult2 = voxelShape2.rayTrace(vec3, vec32, pos);
            double d = blockHitResult == null ? Double.MAX_VALUE : contextx.getFrom().distanceSquared(blockHitResult.getPos());
            double e = blockHitResult2 == null ? Double.MAX_VALUE : contextx.getFrom().distanceSquared(blockHitResult2.getPos());
            return d <= e ? blockHitResult : blockHitResult2;
        }, (contextx) -> {
            Vec3D vec3 = contextx.getFrom().subtract(contextx.getTo());
            return MovingObjectPositionBlock.miss(contextx.getTo(), EnumDirection.getNearest(vec3.x, vec3.y, vec3.z), new BlockPosition(contextx.getTo()));
        });
    }

    @Nullable
    default MovingObjectPositionBlock rayTrace(Vec3D start, Vec3D end, BlockPosition pos, VoxelShape shape, IBlockData state) {
        MovingObjectPositionBlock blockHitResult = shape.rayTrace(start, end, pos);
        if (blockHitResult != null) {
            MovingObjectPositionBlock blockHitResult2 = state.getInteractionShape(this, pos).rayTrace(start, end, pos);
            if (blockHitResult2 != null && blockHitResult2.getPos().subtract(start).lengthSqr() < blockHitResult.getPos().subtract(start).lengthSqr()) {
                return blockHitResult.withDirection(blockHitResult2.getDirection());
            }
        }

        return blockHitResult;
    }

    default double getBlockFloorHeight(VoxelShape blockCollisionShape, Supplier<VoxelShape> belowBlockCollisionShapeGetter) {
        if (!blockCollisionShape.isEmpty()) {
            return blockCollisionShape.max(EnumDirection.EnumAxis.Y);
        } else {
            double d = belowBlockCollisionShapeGetter.get().max(EnumDirection.EnumAxis.Y);
            return d >= 1.0D ? d - 1.0D : Double.NEGATIVE_INFINITY;
        }
    }

    default double getBlockFloorHeight(BlockPosition pos) {
        return this.getBlockFloorHeight(this.getType(pos).getCollisionShape(this, pos), () -> {
            BlockPosition blockPos2 = pos.below();
            return this.getType(blockPos2).getCollisionShape(this, blockPos2);
        });
    }

    static <T, C> T traverseBlocks(Vec3D start, Vec3D end, C context, BiFunction<C, BlockPosition, T> blockHitFactory, Function<C, T> missFactory) {
        if (start.equals(end)) {
            return missFactory.apply(context);
        } else {
            double d = MathHelper.lerp(-1.0E-7D, end.x, start.x);
            double e = MathHelper.lerp(-1.0E-7D, end.y, start.y);
            double f = MathHelper.lerp(-1.0E-7D, end.z, start.z);
            double g = MathHelper.lerp(-1.0E-7D, start.x, end.x);
            double h = MathHelper.lerp(-1.0E-7D, start.y, end.y);
            double i = MathHelper.lerp(-1.0E-7D, start.z, end.z);
            int j = MathHelper.floor(g);
            int k = MathHelper.floor(h);
            int l = MathHelper.floor(i);
            BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition(j, k, l);
            T object = blockHitFactory.apply(context, mutableBlockPos);
            if (object != null) {
                return object;
            } else {
                double m = d - g;
                double n = e - h;
                double o = f - i;
                int p = MathHelper.sign(m);
                int q = MathHelper.sign(n);
                int r = MathHelper.sign(o);
                double s = p == 0 ? Double.MAX_VALUE : (double)p / m;
                double t = q == 0 ? Double.MAX_VALUE : (double)q / n;
                double u = r == 0 ? Double.MAX_VALUE : (double)r / o;
                double v = s * (p > 0 ? 1.0D - MathHelper.frac(g) : MathHelper.frac(g));
                double w = t * (q > 0 ? 1.0D - MathHelper.frac(h) : MathHelper.frac(h));
                double x = u * (r > 0 ? 1.0D - MathHelper.frac(i) : MathHelper.frac(i));

                while(v <= 1.0D || w <= 1.0D || x <= 1.0D) {
                    if (v < w) {
                        if (v < x) {
                            j += p;
                            v += s;
                        } else {
                            l += r;
                            x += u;
                        }
                    } else if (w < x) {
                        k += q;
                        w += t;
                    } else {
                        l += r;
                        x += u;
                    }

                    T object2 = blockHitFactory.apply(context, mutableBlockPos.set(j, k, l));
                    if (object2 != null) {
                        return object2;
                    }
                }

                return missFactory.apply(context);
            }
        }
    }
}
