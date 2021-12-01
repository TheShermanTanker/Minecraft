package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.tags.TagsFluid;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.phys.Vec3D;

public class PathfinderGoalPanic extends PathfinderGoal {
    public static final int WATER_CHECK_DISTANCE_VERTICAL = 1;
    protected final EntityCreature mob;
    protected final double speedModifier;
    protected double posX;
    protected double posY;
    protected double posZ;
    protected boolean isRunning;

    public PathfinderGoalPanic(EntityCreature mob, double speed) {
        this.mob = mob;
        this.speedModifier = speed;
        this.setFlags(EnumSet.of(PathfinderGoal.Type.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.mob.getLastDamager() == null && !this.mob.isBurning()) {
            return false;
        } else {
            if (this.mob.isBurning()) {
                BlockPosition blockPos = this.lookForWater(this.mob.level, this.mob, 5);
                if (blockPos != null) {
                    this.posX = (double)blockPos.getX();
                    this.posY = (double)blockPos.getY();
                    this.posZ = (double)blockPos.getZ();
                    return true;
                }
            }

            return this.findRandomPosition();
        }
    }

    protected boolean findRandomPosition() {
        Vec3D vec3 = DefaultRandomPos.getPos(this.mob, 5, 4);
        if (vec3 == null) {
            return false;
        } else {
            this.posX = vec3.x;
            this.posY = vec3.y;
            this.posZ = vec3.z;
            return true;
        }
    }

    public boolean isRunning() {
        return this.isRunning;
    }

    @Override
    public void start() {
        this.mob.getNavigation().moveTo(this.posX, this.posY, this.posZ, this.speedModifier);
        this.isRunning = true;
    }

    @Override
    public void stop() {
        this.isRunning = false;
    }

    @Override
    public boolean canContinueToUse() {
        return !this.mob.getNavigation().isDone();
    }

    @Nullable
    protected BlockPosition lookForWater(IBlockAccess blockView, Entity entity, int rangeX) {
        BlockPosition blockPos = entity.getChunkCoordinates();
        return !blockView.getType(blockPos).getCollisionShape(blockView, blockPos).isEmpty() ? null : BlockPosition.findClosestMatch(entity.getChunkCoordinates(), rangeX, 1, (blockPosx) -> {
            return blockView.getFluid(blockPosx).is(TagsFluid.WATER);
        }).orElse((BlockPosition)null);
    }
}
