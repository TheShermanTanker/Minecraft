package net.minecraft.world.level.portal;

import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.TagsBlock;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.EntitySize;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.block.BlockPortal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.phys.Vec3D;

public class BlockPortalShape {
    private static final int MIN_WIDTH = 2;
    public static final int MAX_WIDTH = 21;
    private static final int MIN_HEIGHT = 3;
    public static final int MAX_HEIGHT = 21;
    private static final BlockBase.StatePredicate FRAME = (state, world, pos) -> {
        return state.is(Blocks.OBSIDIAN);
    };
    private final GeneratorAccess level;
    private final EnumDirection.EnumAxis axis;
    private final EnumDirection rightDir;
    private int numPortalBlocks;
    @Nullable
    private BlockPosition bottomLeft;
    private int height;
    private final int width;

    public static Optional<BlockPortalShape> findEmptyPortalShape(GeneratorAccess world, BlockPosition pos, EnumDirection.EnumAxis axis) {
        return findPortalShape(world, pos, (portalShape) -> {
            return portalShape.isValid() && portalShape.numPortalBlocks == 0;
        }, axis);
    }

    public static Optional<BlockPortalShape> findPortalShape(GeneratorAccess world, BlockPosition pos, Predicate<BlockPortalShape> predicate, EnumDirection.EnumAxis axis) {
        Optional<BlockPortalShape> optional = Optional.of(new BlockPortalShape(world, pos, axis)).filter(predicate);
        if (optional.isPresent()) {
            return optional;
        } else {
            EnumDirection.EnumAxis axis2 = axis == EnumDirection.EnumAxis.X ? EnumDirection.EnumAxis.Z : EnumDirection.EnumAxis.X;
            return Optional.of(new BlockPortalShape(world, pos, axis2)).filter(predicate);
        }
    }

    public BlockPortalShape(GeneratorAccess world, BlockPosition pos, EnumDirection.EnumAxis axis) {
        this.level = world;
        this.axis = axis;
        this.rightDir = axis == EnumDirection.EnumAxis.X ? EnumDirection.WEST : EnumDirection.SOUTH;
        this.bottomLeft = this.calculateBottomLeft(pos);
        if (this.bottomLeft == null) {
            this.bottomLeft = pos;
            this.width = 1;
            this.height = 1;
        } else {
            this.width = this.calculateWidth();
            if (this.width > 0) {
                this.height = this.calculateHeight();
            }
        }

    }

    @Nullable
    private BlockPosition calculateBottomLeft(BlockPosition pos) {
        for(int i = Math.max(this.level.getMinBuildHeight(), pos.getY() - 21); pos.getY() > i && isEmpty(this.level.getType(pos.below())); pos = pos.below()) {
        }

        EnumDirection direction = this.rightDir.opposite();
        int j = this.getDistanceUntilEdgeAboveFrame(pos, direction) - 1;
        return j < 0 ? null : pos.relative(direction, j);
    }

    private int calculateWidth() {
        int i = this.getDistanceUntilEdgeAboveFrame(this.bottomLeft, this.rightDir);
        return i >= 2 && i <= 21 ? i : 0;
    }

    private int getDistanceUntilEdgeAboveFrame(BlockPosition pos, EnumDirection direction) {
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

        for(int i = 0; i <= 21; ++i) {
            mutableBlockPos.set(pos).move(direction, i);
            IBlockData blockState = this.level.getType(mutableBlockPos);
            if (!isEmpty(blockState)) {
                if (FRAME.test(blockState, this.level, mutableBlockPos)) {
                    return i;
                }
                break;
            }

            IBlockData blockState2 = this.level.getType(mutableBlockPos.move(EnumDirection.DOWN));
            if (!FRAME.test(blockState2, this.level, mutableBlockPos)) {
                break;
            }
        }

        return 0;
    }

    private int calculateHeight() {
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();
        int i = this.getDistanceUntilTop(mutableBlockPos);
        return i >= 3 && i <= 21 && this.hasTopFrame(mutableBlockPos, i) ? i : 0;
    }

    private boolean hasTopFrame(BlockPosition.MutableBlockPosition mutableBlockPos, int i) {
        for(int j = 0; j < this.width; ++j) {
            BlockPosition.MutableBlockPosition mutableBlockPos2 = mutableBlockPos.set(this.bottomLeft).move(EnumDirection.UP, i).move(this.rightDir, j);
            if (!FRAME.test(this.level.getType(mutableBlockPos2), this.level, mutableBlockPos2)) {
                return false;
            }
        }

        return true;
    }

    private int getDistanceUntilTop(BlockPosition.MutableBlockPosition mutableBlockPos) {
        for(int i = 0; i < 21; ++i) {
            mutableBlockPos.set(this.bottomLeft).move(EnumDirection.UP, i).move(this.rightDir, -1);
            if (!FRAME.test(this.level.getType(mutableBlockPos), this.level, mutableBlockPos)) {
                return i;
            }

            mutableBlockPos.set(this.bottomLeft).move(EnumDirection.UP, i).move(this.rightDir, this.width);
            if (!FRAME.test(this.level.getType(mutableBlockPos), this.level, mutableBlockPos)) {
                return i;
            }

            for(int j = 0; j < this.width; ++j) {
                mutableBlockPos.set(this.bottomLeft).move(EnumDirection.UP, i).move(this.rightDir, j);
                IBlockData blockState = this.level.getType(mutableBlockPos);
                if (!isEmpty(blockState)) {
                    return i;
                }

                if (blockState.is(Blocks.NETHER_PORTAL)) {
                    ++this.numPortalBlocks;
                }
            }
        }

        return 21;
    }

    private static boolean isEmpty(IBlockData state) {
        return state.isAir() || state.is(TagsBlock.FIRE) || state.is(Blocks.NETHER_PORTAL);
    }

    public boolean isValid() {
        return this.bottomLeft != null && this.width >= 2 && this.width <= 21 && this.height >= 3 && this.height <= 21;
    }

    public void createPortal() {
        IBlockData blockState = Blocks.NETHER_PORTAL.getBlockData().set(BlockPortal.AXIS, this.axis);
        BlockPosition.betweenClosed(this.bottomLeft, this.bottomLeft.relative(EnumDirection.UP, this.height - 1).relative(this.rightDir, this.width - 1)).forEach((blockPos) -> {
            this.level.setTypeAndData(blockPos, blockState, 18);
        });
    }

    public boolean isComplete() {
        return this.isValid() && this.numPortalBlocks == this.width * this.height;
    }

    public static Vec3D getRelativePosition(BlockUtil.Rectangle portalRect, EnumDirection.EnumAxis portalAxis, Vec3D entityPos, EntitySize entityDimensions) {
        double d = (double)portalRect.axis1Size - (double)entityDimensions.width;
        double e = (double)portalRect.axis2Size - (double)entityDimensions.height;
        BlockPosition blockPos = portalRect.minCorner;
        double g;
        if (d > 0.0D) {
            float f = (float)blockPos.get(portalAxis) + entityDimensions.width / 2.0F;
            g = MathHelper.clamp(MathHelper.inverseLerp(entityPos.get(portalAxis) - (double)f, 0.0D, d), 0.0D, 1.0D);
        } else {
            g = 0.5D;
        }

        double i;
        if (e > 0.0D) {
            EnumDirection.EnumAxis axis = EnumDirection.EnumAxis.Y;
            i = MathHelper.clamp(MathHelper.inverseLerp(entityPos.get(axis) - (double)blockPos.get(axis), 0.0D, e), 0.0D, 1.0D);
        } else {
            i = 0.0D;
        }

        EnumDirection.EnumAxis axis2 = portalAxis == EnumDirection.EnumAxis.X ? EnumDirection.EnumAxis.Z : EnumDirection.EnumAxis.X;
        double k = entityPos.get(axis2) - ((double)blockPos.get(axis2) + 0.5D);
        return new Vec3D(g, i, k);
    }

    public static ShapeDetectorShape createPortalInfo(WorldServer destination, BlockUtil.Rectangle portalRect, EnumDirection.EnumAxis portalAxis, Vec3D offset, EntitySize dimensions, Vec3D velocity, float yaw, float pitch) {
        BlockPosition blockPos = portalRect.minCorner;
        IBlockData blockState = destination.getType(blockPos);
        EnumDirection.EnumAxis axis = blockState.getOptionalValue(BlockProperties.HORIZONTAL_AXIS).orElse(EnumDirection.EnumAxis.X);
        double d = (double)portalRect.axis1Size;
        double e = (double)portalRect.axis2Size;
        int i = portalAxis == axis ? 0 : 90;
        Vec3D vec3 = portalAxis == axis ? velocity : new Vec3D(velocity.z, velocity.y, -velocity.x);
        double f = (double)dimensions.width / 2.0D + (d - (double)dimensions.width) * offset.getX();
        double g = (e - (double)dimensions.height) * offset.getY();
        double h = 0.5D + offset.getZ();
        boolean bl = axis == EnumDirection.EnumAxis.X;
        Vec3D vec32 = new Vec3D((double)blockPos.getX() + (bl ? f : h), (double)blockPos.getY() + g, (double)blockPos.getZ() + (bl ? h : f));
        return new ShapeDetectorShape(vec32, vec3, yaw + (float)i, pitch);
    }
}
