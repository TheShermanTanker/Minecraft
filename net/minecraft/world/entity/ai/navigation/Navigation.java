package net.minecraft.world.entity.ai.navigation;

import net.minecraft.core.BlockPosition;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.pathfinder.PathEntity;
import net.minecraft.world.level.pathfinder.PathPoint;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.Pathfinder;
import net.minecraft.world.level.pathfinder.PathfinderNormal;
import net.minecraft.world.phys.Vec3D;

public class Navigation extends NavigationAbstract {
    private boolean avoidSun;

    public Navigation(EntityInsentient mob, World world) {
        super(mob, world);
    }

    @Override
    protected Pathfinder createPathFinder(int range) {
        this.nodeEvaluator = new PathfinderNormal();
        this.nodeEvaluator.setCanPassDoors(true);
        return new Pathfinder(this.nodeEvaluator, range);
    }

    @Override
    protected boolean canUpdatePath() {
        return this.mob.isOnGround() || this.isInLiquid() || this.mob.isPassenger();
    }

    @Override
    protected Vec3D getTempMobPos() {
        return new Vec3D(this.mob.locX(), (double)this.getSurfaceY(), this.mob.locZ());
    }

    @Override
    public PathEntity createPath(BlockPosition target, int distance) {
        if (this.level.getType(target).isAir()) {
            BlockPosition blockPos;
            for(blockPos = target.below(); blockPos.getY() > this.level.getMinBuildHeight() && this.level.getType(blockPos).isAir(); blockPos = blockPos.below()) {
            }

            if (blockPos.getY() > this.level.getMinBuildHeight()) {
                return super.createPath(blockPos.above(), distance);
            }

            while(blockPos.getY() < this.level.getMaxBuildHeight() && this.level.getType(blockPos).isAir()) {
                blockPos = blockPos.above();
            }

            target = blockPos;
        }

        if (!this.level.getType(target).getMaterial().isBuildable()) {
            return super.createPath(target, distance);
        } else {
            BlockPosition blockPos2;
            for(blockPos2 = target.above(); blockPos2.getY() < this.level.getMaxBuildHeight() && this.level.getType(blockPos2).getMaterial().isBuildable(); blockPos2 = blockPos2.above()) {
            }

            return super.createPath(blockPos2, distance);
        }
    }

    @Override
    public PathEntity createPath(Entity entity, int distance) {
        return this.createPath(entity.getChunkCoordinates(), distance);
    }

    private int getSurfaceY() {
        if (this.mob.isInWater() && this.canFloat()) {
            int i = this.mob.getBlockY();
            IBlockData blockState = this.level.getType(new BlockPosition(this.mob.locX(), (double)i, this.mob.locZ()));
            int j = 0;

            while(blockState.is(Blocks.WATER)) {
                ++i;
                blockState = this.level.getType(new BlockPosition(this.mob.locX(), (double)i, this.mob.locZ()));
                ++j;
                if (j > 16) {
                    return this.mob.getBlockY();
                }
            }

            return i;
        } else {
            return MathHelper.floor(this.mob.locY() + 0.5D);
        }
    }

    @Override
    protected void trimPath() {
        super.trimPath();
        if (this.avoidSun) {
            if (this.level.canSeeSky(new BlockPosition(this.mob.locX(), this.mob.locY() + 0.5D, this.mob.locZ()))) {
                return;
            }

            for(int i = 0; i < this.path.getNodeCount(); ++i) {
                PathPoint node = this.path.getNode(i);
                if (this.level.canSeeSky(new BlockPosition(node.x, node.y, node.z))) {
                    this.path.truncateNodes(i);
                    return;
                }
            }
        }

    }

    protected boolean hasValidPathType(PathType pathType) {
        if (pathType == PathType.WATER) {
            return false;
        } else if (pathType == PathType.LAVA) {
            return false;
        } else {
            return pathType != PathType.OPEN;
        }
    }

    public void setCanOpenDoors(boolean canPathThroughDoors) {
        this.nodeEvaluator.setCanOpenDoors(canPathThroughDoors);
    }

    public boolean canPassDoors() {
        return this.nodeEvaluator.canPassDoors();
    }

    public void setCanPassDoors(boolean canEnterOpenDoors) {
        this.nodeEvaluator.setCanPassDoors(canEnterOpenDoors);
    }

    public boolean canOpenDoors() {
        return this.nodeEvaluator.canPassDoors();
    }

    public void setAvoidSun(boolean avoidSunlight) {
        this.avoidSun = avoidSunlight;
    }
}
