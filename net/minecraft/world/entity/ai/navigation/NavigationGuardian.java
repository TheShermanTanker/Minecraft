package net.minecraft.world.entity.ai.navigation;

import net.minecraft.core.BlockPosition;
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
    protected double getGroundY(Vec3D pos) {
        return pos.y;
    }

    @Override
    protected boolean canMoveDirectly(Vec3D origin, Vec3D target) {
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
