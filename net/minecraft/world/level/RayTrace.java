package net.minecraft.world.level;

import java.util.function.Predicate;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3D;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.phys.shapes.VoxelShapes;

public class RayTrace {
    private final Vec3D from;
    private final Vec3D to;
    private final RayTrace.BlockCollisionOption block;
    private final RayTrace.FluidCollisionOption fluid;
    private final VoxelShapeCollision collisionContext;

    public RayTrace(Vec3D start, Vec3D end, RayTrace.BlockCollisionOption shapeType, RayTrace.FluidCollisionOption fluidHandling, Entity entity) {
        this.from = start;
        this.to = end;
        this.block = shapeType;
        this.fluid = fluidHandling;
        this.collisionContext = VoxelShapeCollision.of(entity);
    }

    public Vec3D getTo() {
        return this.to;
    }

    public Vec3D getFrom() {
        return this.from;
    }

    public VoxelShape getBlockShape(IBlockData state, IBlockAccess world, BlockPosition pos) {
        return this.block.get(state, world, pos, this.collisionContext);
    }

    public VoxelShape getFluidShape(Fluid state, IBlockAccess world, BlockPosition pos) {
        return this.fluid.canPick(state) ? state.getShape(world, pos) : VoxelShapes.empty();
    }

    public static enum BlockCollisionOption implements RayTrace.ShapeGetter {
        COLLIDER(BlockBase.BlockData::getCollisionShape),
        OUTLINE(BlockBase.BlockData::getShape),
        VISUAL(BlockBase.BlockData::getVisualShape);

        private final RayTrace.ShapeGetter shapeGetter;

        private BlockCollisionOption(RayTrace.ShapeGetter provider) {
            this.shapeGetter = provider;
        }

        @Override
        public VoxelShape get(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context) {
            return this.shapeGetter.get(state, world, pos, context);
        }
    }

    public static enum FluidCollisionOption {
        NONE((fluidState) -> {
            return false;
        }),
        SOURCE_ONLY(Fluid::isSource),
        ANY((fluidState) -> {
            return !fluidState.isEmpty();
        });

        private final Predicate<Fluid> canPick;

        private FluidCollisionOption(Predicate<Fluid> predicate) {
            this.canPick = predicate;
        }

        public boolean canPick(Fluid state) {
            return this.canPick.test(state);
        }
    }

    public interface ShapeGetter {
        VoxelShape get(IBlockData state, IBlockAccess world, BlockPosition pos, VoxelShapeCollision context);
    }
}
