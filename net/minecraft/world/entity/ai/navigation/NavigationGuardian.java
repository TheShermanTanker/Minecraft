package net.minecraft.world.entity.ai.navigation;

import net.minecraft.SystemUtils;
import net.minecraft.core.BaseBlockPosition;
import net.minecraft.core.BlockPosition;
import net.minecraft.network.protocol.game.PacketDebug;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.RayTrace;
import net.minecraft.world.level.World;
import net.minecraft.world.level.pathfinder.Pathfinder;
import net.minecraft.world.level.pathfinder.PathfinderWater;
import net.minecraft.world.phys.MovingObjectPosition;
import net.minecraft.world.phys.Vec3D;

public class NavigationGuardian extends NavigationAbstract {
    private boolean allowBreaching;

    public NavigationGuardian(EntityInsentient mob, World world) {
        super(mob, world);
    }

    @Override
    protected Pathfinder createPathFinder(int range) {
        this.allowBreaching = this.mob.getEntityType() == EntityTypes.DOLPHIN;
        this.nodeEvaluator = new PathfinderWater(this.allowBreaching);
        return new Pathfinder(this.nodeEvaluator, range);
    }

    @Override
    protected boolean canUpdatePath() {
        return this.allowBreaching || this.isInLiquid();
    }

    @Override
    protected Vec3D getTempMobPos() {
        return new Vec3D(this.mob.locX(), this.mob.getY(0.5D), this.mob.locZ());
    }

    @Override
    public void tick() {
        ++this.tick;
        if (this.hasDelayedRecomputation) {
            this.recomputePath();
        }

        if (!this.isDone()) {
            if (this.canUpdatePath()) {
                this.followThePath();
            } else if (this.path != null && !this.path.isDone()) {
                Vec3D vec3 = this.path.getNextEntityPos(this.mob);
                if (this.mob.getBlockX() == MathHelper.floor(vec3.x) && this.mob.getBlockY() == MathHelper.floor(vec3.y) && this.mob.getBlockZ() == MathHelper.floor(vec3.z)) {
                    this.path.advance();
                }
            }

            PacketDebug.sendPathFindingPacket(this.level, this.mob, this.path, this.maxDistanceToWaypoint);
            if (!this.isDone()) {
                Vec3D vec32 = this.path.getNextEntityPos(this.mob);
                this.mob.getControllerMove().setWantedPosition(vec32.x, vec32.y, vec32.z, this.speedModifier);
            }
        }
    }

    @Override
    protected void followThePath() {
        if (this.path != null) {
            Vec3D vec3 = this.getTempMobPos();
            float f = this.mob.getWidth();
            float g = f > 0.75F ? f / 2.0F : 0.75F - f / 2.0F;
            Vec3D vec32 = this.mob.getMot();
            if (Math.abs(vec32.x) > 0.2D || Math.abs(vec32.z) > 0.2D) {
                g = (float)((double)g * vec32.length() * 6.0D);
            }

            int i = 6;
            Vec3D vec33 = Vec3D.atBottomCenterOf(this.path.getNextNodePos());
            if (Math.abs(this.mob.locX() - vec33.x) < (double)g && Math.abs(this.mob.locZ() - vec33.z) < (double)g && Math.abs(this.mob.locY() - vec33.y) < (double)(g * 2.0F)) {
                this.path.advance();
            }

            for(int j = Math.min(this.path.getNextNodeIndex() + 6, this.path.getNodeCount() - 1); j > this.path.getNextNodeIndex(); --j) {
                vec33 = this.path.getEntityPosAtNode(this.mob, j);
                if (!(vec33.distanceSquared(vec3) > 36.0D) && this.canMoveDirectly(vec3, vec33, 0, 0, 0)) {
                    this.path.setNextNodeIndex(j);
                    break;
                }
            }

            this.doStuckDetection(vec3);
        }
    }

    @Override
    protected void doStuckDetection(Vec3D currentPos) {
        if (this.tick - this.lastStuckCheck > 100) {
            if (currentPos.distanceSquared(this.lastStuckCheckPos) < 2.25D) {
                this.stop();
            }

            this.lastStuckCheck = this.tick;
            this.lastStuckCheckPos = currentPos;
        }

        if (this.path != null && !this.path.isDone()) {
            BaseBlockPosition vec3i = this.path.getNextNodePos();
            if (vec3i.equals(this.timeoutCachedNode)) {
                this.timeoutTimer += SystemUtils.getMonotonicMillis() - this.lastTimeoutCheck;
            } else {
                this.timeoutCachedNode = vec3i;
                double d = currentPos.distanceTo(Vec3D.atCenterOf(this.timeoutCachedNode));
                this.timeoutLimit = this.mob.getSpeed() > 0.0F ? d / (double)this.mob.getSpeed() * 100.0D : 0.0D;
            }

            if (this.timeoutLimit > 0.0D && (double)this.timeoutTimer > this.timeoutLimit * 2.0D) {
                this.timeoutCachedNode = BaseBlockPosition.ZERO;
                this.timeoutTimer = 0L;
                this.timeoutLimit = 0.0D;
                this.stop();
            }

            this.lastTimeoutCheck = SystemUtils.getMonotonicMillis();
        }

    }

    @Override
    protected boolean canMoveDirectly(Vec3D origin, Vec3D target, int sizeX, int sizeY, int sizeZ) {
        Vec3D vec3 = new Vec3D(target.x, target.y + (double)this.mob.getHeight() * 0.5D, target.z);
        return this.level.rayTrace(new RayTrace(origin, vec3, RayTrace.BlockCollisionOption.COLLIDER, RayTrace.FluidCollisionOption.NONE, this.mob)).getType() == MovingObjectPosition.EnumMovingObjectType.MISS;
    }

    @Override
    public boolean isStableDestination(BlockPosition pos) {
        return !this.level.getType(pos).isSolidRender(this.level, pos);
    }

    @Override
    public void setCanFloat(boolean canSwim) {
    }
}
