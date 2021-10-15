package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.core.BlockPosition;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.EnumMoveType;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.phys.Vec3D;

public class PathfinderGoalBreath extends PathfinderGoal {
    private final EntityCreature mob;

    public PathfinderGoalBreath(EntityCreature mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(PathfinderGoal.Type.MOVE, PathfinderGoal.Type.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.mob.getAirTicks() < 140;
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse();
    }

    @Override
    public boolean isInterruptable() {
        return false;
    }

    @Override
    public void start() {
        this.findAirPosition();
    }

    private void findAirPosition() {
        Iterable<BlockPosition> iterable = BlockPosition.betweenClosed(MathHelper.floor(this.mob.locX() - 1.0D), this.mob.getBlockY(), MathHelper.floor(this.mob.locZ() - 1.0D), MathHelper.floor(this.mob.locX() + 1.0D), MathHelper.floor(this.mob.locY() + 8.0D), MathHelper.floor(this.mob.locZ() + 1.0D));
        BlockPosition blockPos = null;

        for(BlockPosition blockPos2 : iterable) {
            if (this.givesAir(this.mob.level, blockPos2)) {
                blockPos = blockPos2;
                break;
            }
        }

        if (blockPos == null) {
            blockPos = new BlockPosition(this.mob.locX(), this.mob.locY() + 8.0D, this.mob.locZ());
        }

        this.mob.getNavigation().moveTo((double)blockPos.getX(), (double)(blockPos.getY() + 1), (double)blockPos.getZ(), 1.0D);
    }

    @Override
    public void tick() {
        this.findAirPosition();
        this.mob.moveRelative(0.02F, new Vec3D((double)this.mob.xxa, (double)this.mob.yya, (double)this.mob.zza));
        this.mob.move(EnumMoveType.SELF, this.mob.getMot());
    }

    private boolean givesAir(IWorldReader world, BlockPosition pos) {
        IBlockData blockState = world.getType(pos);
        return (world.getFluid(pos).isEmpty() || blockState.is(Blocks.BUBBLE_COLUMN)) && blockState.isPathfindable(world, pos, PathMode.LAND);
    }
}
