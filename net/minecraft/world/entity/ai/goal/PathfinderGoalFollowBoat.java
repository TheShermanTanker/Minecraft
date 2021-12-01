package net.minecraft.world.entity.ai.goal;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.EnumMoveType;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.vehicle.EntityBoat;
import net.minecraft.world.phys.Vec3D;

public class PathfinderGoalFollowBoat extends PathfinderGoal {
    private int timeToRecalcPath;
    private final EntityCreature mob;
    @Nullable
    private EntityHuman following;
    private PathfinderGoalBoat currentGoal;

    public PathfinderGoalFollowBoat(EntityCreature mob) {
        this.mob = mob;
    }

    @Override
    public boolean canUse() {
        List<EntityBoat> list = this.mob.level.getEntitiesOfClass(EntityBoat.class, this.mob.getBoundingBox().inflate(5.0D));
        boolean bl = false;

        for(EntityBoat boat : list) {
            Entity entity = boat.getRidingPassenger();
            if (entity instanceof EntityHuman && (MathHelper.abs(((EntityHuman)entity).xxa) > 0.0F || MathHelper.abs(((EntityHuman)entity).zza) > 0.0F)) {
                bl = true;
                break;
            }
        }

        return this.following != null && (MathHelper.abs(this.following.xxa) > 0.0F || MathHelper.abs(this.following.zza) > 0.0F) || bl;
    }

    @Override
    public boolean isInterruptable() {
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return this.following != null && this.following.isPassenger() && (MathHelper.abs(this.following.xxa) > 0.0F || MathHelper.abs(this.following.zza) > 0.0F);
    }

    @Override
    public void start() {
        for(EntityBoat boat : this.mob.level.getEntitiesOfClass(EntityBoat.class, this.mob.getBoundingBox().inflate(5.0D))) {
            if (boat.getRidingPassenger() != null && boat.getRidingPassenger() instanceof EntityHuman) {
                this.following = (EntityHuman)boat.getRidingPassenger();
                break;
            }
        }

        this.timeToRecalcPath = 0;
        this.currentGoal = PathfinderGoalBoat.GO_TO_BOAT;
    }

    @Override
    public void stop() {
        this.following = null;
    }

    @Override
    public void tick() {
        boolean bl = MathHelper.abs(this.following.xxa) > 0.0F || MathHelper.abs(this.following.zza) > 0.0F;
        float f = this.currentGoal == PathfinderGoalBoat.GO_IN_BOAT_DIRECTION ? (bl ? 0.01F : 0.0F) : 0.015F;
        this.mob.moveRelative(f, new Vec3D((double)this.mob.xxa, (double)this.mob.yya, (double)this.mob.zza));
        this.mob.move(EnumMoveType.SELF, this.mob.getMot());
        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = this.adjustedTickDelay(10);
            if (this.currentGoal == PathfinderGoalBoat.GO_TO_BOAT) {
                BlockPosition blockPos = this.following.getChunkCoordinates().relative(this.following.getDirection().opposite());
                blockPos = blockPos.offset(0, -1, 0);
                this.mob.getNavigation().moveTo((double)blockPos.getX(), (double)blockPos.getY(), (double)blockPos.getZ(), 1.0D);
                if (this.mob.distanceTo(this.following) < 4.0F) {
                    this.timeToRecalcPath = 0;
                    this.currentGoal = PathfinderGoalBoat.GO_IN_BOAT_DIRECTION;
                }
            } else if (this.currentGoal == PathfinderGoalBoat.GO_IN_BOAT_DIRECTION) {
                EnumDirection direction = this.following.getAdjustedDirection();
                BlockPosition blockPos2 = this.following.getChunkCoordinates().relative(direction, 10);
                this.mob.getNavigation().moveTo((double)blockPos2.getX(), (double)(blockPos2.getY() - 1), (double)blockPos2.getZ(), 1.0D);
                if (this.mob.distanceTo(this.following) > 12.0F) {
                    this.timeToRecalcPath = 0;
                    this.currentGoal = PathfinderGoalBoat.GO_TO_BOAT;
                }
            }

        }
    }
}
