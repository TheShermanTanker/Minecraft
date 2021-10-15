package net.minecraft.world.entity.ai.navigation;

import net.minecraft.core.BlockPosition;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.pathfinder.PathEntity;
import net.minecraft.world.level.pathfinder.PathMode;
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

    @Override
    protected boolean canMoveDirectly(Vec3D origin, Vec3D target, int sizeX, int sizeY, int sizeZ) {
        int i = MathHelper.floor(origin.x);
        int j = MathHelper.floor(origin.z);
        double d = target.x - origin.x;
        double e = target.z - origin.z;
        double f = d * d + e * e;
        if (f < 1.0E-8D) {
            return false;
        } else {
            double g = 1.0D / Math.sqrt(f);
            d = d * g;
            e = e * g;
            sizeX = sizeX + 2;
            sizeZ = sizeZ + 2;
            if (!this.canWalkOn(i, MathHelper.floor(origin.y), j, sizeX, sizeY, sizeZ, origin, d, e)) {
                return false;
            } else {
                sizeX = sizeX - 2;
                sizeZ = sizeZ - 2;
                double h = 1.0D / Math.abs(d);
                double k = 1.0D / Math.abs(e);
                double l = (double)i - origin.x;
                double m = (double)j - origin.z;
                if (d >= 0.0D) {
                    ++l;
                }

                if (e >= 0.0D) {
                    ++m;
                }

                l = l / d;
                m = m / e;
                int n = d < 0.0D ? -1 : 1;
                int o = e < 0.0D ? -1 : 1;
                int p = MathHelper.floor(target.x);
                int q = MathHelper.floor(target.z);
                int r = p - i;
                int s = q - j;

                while(r * n > 0 || s * o > 0) {
                    if (l < m) {
                        l += h;
                        i += n;
                        r = p - i;
                    } else {
                        m += k;
                        j += o;
                        s = q - j;
                    }

                    if (!this.canWalkOn(i, MathHelper.floor(origin.y), j, sizeX, sizeY, sizeZ, origin, d, e)) {
                        return false;
                    }
                }

                return true;
            }
        }
    }

    private boolean canWalkOn(int centerX, int centerY, int centerZ, int sizeX, int sizeY, int sizeZ, Vec3D entityPos, double lookVecX, double lookVecZ) {
        int i = centerX - sizeX / 2;
        int j = centerZ - sizeZ / 2;
        if (!this.canWalkAbove(i, centerY, j, sizeX, sizeY, sizeZ, entityPos, lookVecX, lookVecZ)) {
            return false;
        } else {
            for(int k = i; k < i + sizeX; ++k) {
                for(int l = j; l < j + sizeZ; ++l) {
                    double d = (double)k + 0.5D - entityPos.x;
                    double e = (double)l + 0.5D - entityPos.z;
                    if (!(d * lookVecX + e * lookVecZ < 0.0D)) {
                        PathType blockPathTypes = this.nodeEvaluator.getBlockPathType(this.level, k, centerY - 1, l, this.mob, sizeX, sizeY, sizeZ, true, true);
                        if (!this.hasValidPathType(blockPathTypes)) {
                            return false;
                        }

                        blockPathTypes = this.nodeEvaluator.getBlockPathType(this.level, k, centerY, l, this.mob, sizeX, sizeY, sizeZ, true, true);
                        float f = this.mob.getPathfindingMalus(blockPathTypes);
                        if (f < 0.0F || f >= 8.0F) {
                            return false;
                        }

                        if (blockPathTypes == PathType.DAMAGE_FIRE || blockPathTypes == PathType.DANGER_FIRE || blockPathTypes == PathType.DAMAGE_OTHER) {
                            return false;
                        }
                    }
                }
            }

            return true;
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

    private boolean canWalkAbove(int x, int y, int z, int sizeX, int sizeY, int sizeZ, Vec3D entityPos, double lookVecX, double lookVecZ) {
        for(BlockPosition blockPos : BlockPosition.betweenClosed(new BlockPosition(x, y, z), new BlockPosition(x + sizeX - 1, y + sizeY - 1, z + sizeZ - 1))) {
            double d = (double)blockPos.getX() + 0.5D - entityPos.x;
            double e = (double)blockPos.getZ() + 0.5D - entityPos.z;
            if (!(d * lookVecX + e * lookVecZ < 0.0D) && !this.level.getType(blockPos).isPathfindable(this.level, blockPos, PathMode.LAND)) {
                return false;
            }
        }

        return true;
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
