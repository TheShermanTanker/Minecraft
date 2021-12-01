package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.horse.EntityHorseAbstract;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.phys.Vec3D;

public class PathfinderGoalTame extends PathfinderGoal {
    private final EntityHorseAbstract horse;
    private final double speedModifier;
    private double posX;
    private double posY;
    private double posZ;

    public PathfinderGoalTame(EntityHorseAbstract horse, double speed) {
        this.horse = horse;
        this.speedModifier = speed;
        this.setFlags(EnumSet.of(PathfinderGoal.Type.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!this.horse.isTamed() && this.horse.isVehicle()) {
            Vec3D vec3 = DefaultRandomPos.getPos(this.horse, 5, 4);
            if (vec3 == null) {
                return false;
            } else {
                this.posX = vec3.x;
                this.posY = vec3.y;
                this.posZ = vec3.z;
                return true;
            }
        } else {
            return false;
        }
    }

    @Override
    public void start() {
        this.horse.getNavigation().moveTo(this.posX, this.posY, this.posZ, this.speedModifier);
    }

    @Override
    public boolean canContinueToUse() {
        return !this.horse.isTamed() && !this.horse.getNavigation().isDone() && this.horse.isVehicle();
    }

    @Override
    public void tick() {
        if (!this.horse.isTamed() && this.horse.getRandom().nextInt(this.adjustedTickDelay(50)) == 0) {
            Entity entity = this.horse.getPassengers().get(0);
            if (entity == null) {
                return;
            }

            if (entity instanceof EntityHuman) {
                int i = this.horse.getTemper();
                int j = this.horse.getMaxDomestication();
                if (j > 0 && this.horse.getRandom().nextInt(j) < i) {
                    this.horse.tameWithName((EntityHuman)entity);
                    return;
                }

                this.horse.modifyTemper(5);
            }

            this.horse.ejectPassengers();
            this.horse.makeMad();
            this.horse.level.broadcastEntityEffect(this.horse, (byte)6);
        }

    }
}
