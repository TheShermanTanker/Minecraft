package net.minecraft.world.entity.ai.navigation;

import net.minecraft.core.BlockPosition;
import net.minecraft.network.protocol.game.PacketDebug;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.level.World;
import net.minecraft.world.level.pathfinder.PathEntity;
import net.minecraft.world.level.pathfinder.Pathfinder;
import net.minecraft.world.level.pathfinder.PathfinderFlying;
import net.minecraft.world.phys.Vec3D;

public class NavigationFlying extends NavigationAbstract {
    public NavigationFlying(EntityInsentient mob, World world) {
        super(mob, world);
    }

    @Override
    protected Pathfinder createPathFinder(int range) {
        this.nodeEvaluator = new PathfinderFlying();
        this.nodeEvaluator.setCanPassDoors(true);
        return new Pathfinder(this.nodeEvaluator, range);
    }

    @Override
    protected boolean canUpdatePath() {
        return this.canFloat() && this.isInLiquid() || !this.mob.isPassenger();
    }

    @Override
    protected Vec3D getTempMobPos() {
        return this.mob.getPositionVector();
    }

    @Override
    public PathEntity createPath(Entity entity, int distance) {
        return this.createPath(entity.getChunkCoordinates(), distance);
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

    @Override
    public boolean isStableDestination(BlockPosition pos) {
        return this.level.getType(pos).entityCanStandOn(this.level, pos, this.mob);
    }
}
