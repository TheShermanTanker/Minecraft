package net.minecraft.world.entity.ai.goal;

import net.minecraft.core.BlockPosition;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.ai.navigation.Navigation;
import net.minecraft.world.entity.ai.util.PathfinderGoalUtil;
import net.minecraft.world.level.block.BlockDoor;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.pathfinder.PathEntity;
import net.minecraft.world.level.pathfinder.PathPoint;

public abstract class PathfinderGoalDoorInteract extends PathfinderGoal {
    protected EntityInsentient mob;
    protected BlockPosition doorPos = BlockPosition.ZERO;
    protected boolean hasDoor;
    private boolean passed;
    private float doorOpenDirX;
    private float doorOpenDirZ;

    public PathfinderGoalDoorInteract(EntityInsentient mob) {
        this.mob = mob;
        if (!PathfinderGoalUtil.hasGroundPathNavigation(mob)) {
            throw new IllegalArgumentException("Unsupported mob type for DoorInteractGoal");
        }
    }

    protected boolean isOpen() {
        if (!this.hasDoor) {
            return false;
        } else {
            IBlockData blockState = this.mob.level.getType(this.doorPos);
            if (!(blockState.getBlock() instanceof BlockDoor)) {
                this.hasDoor = false;
                return false;
            } else {
                return blockState.get(BlockDoor.OPEN);
            }
        }
    }

    protected void setOpen(boolean open) {
        if (this.hasDoor) {
            IBlockData blockState = this.mob.level.getType(this.doorPos);
            if (blockState.getBlock() instanceof BlockDoor) {
                ((BlockDoor)blockState.getBlock()).setDoor(this.mob, this.mob.level, blockState, this.doorPos, open);
            }
        }

    }

    @Override
    public boolean canUse() {
        if (!PathfinderGoalUtil.hasGroundPathNavigation(this.mob)) {
            return false;
        } else if (!this.mob.horizontalCollision) {
            return false;
        } else {
            Navigation groundPathNavigation = (Navigation)this.mob.getNavigation();
            PathEntity path = groundPathNavigation.getPath();
            if (path != null && !path.isDone() && groundPathNavigation.canOpenDoors()) {
                for(int i = 0; i < Math.min(path.getNextNodeIndex() + 2, path.getNodeCount()); ++i) {
                    PathPoint node = path.getNode(i);
                    this.doorPos = new BlockPosition(node.x, node.y + 1, node.z);
                    if (!(this.mob.distanceToSqr((double)this.doorPos.getX(), this.mob.locY(), (double)this.doorPos.getZ()) > 2.25D)) {
                        this.hasDoor = BlockDoor.isWoodenDoor(this.mob.level, this.doorPos);
                        if (this.hasDoor) {
                            return true;
                        }
                    }
                }

                this.doorPos = this.mob.getChunkCoordinates().above();
                this.hasDoor = BlockDoor.isWoodenDoor(this.mob.level, this.doorPos);
                return this.hasDoor;
            } else {
                return false;
            }
        }
    }

    @Override
    public boolean canContinueToUse() {
        return !this.passed;
    }

    @Override
    public void start() {
        this.passed = false;
        this.doorOpenDirX = (float)((double)this.doorPos.getX() + 0.5D - this.mob.locX());
        this.doorOpenDirZ = (float)((double)this.doorPos.getZ() + 0.5D - this.mob.locZ());
    }

    @Override
    public void tick() {
        float f = (float)((double)this.doorPos.getX() + 0.5D - this.mob.locX());
        float g = (float)((double)this.doorPos.getZ() + 0.5D - this.mob.locZ());
        float h = this.doorOpenDirX * f + this.doorOpenDirZ * g;
        if (h < 0.0F) {
            this.passed = true;
        }

    }
}
