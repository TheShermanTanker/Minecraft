package net.minecraft.world.entity.boss.enderdragon.phases;

import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.targeting.PathfinderTargetCondition;
import net.minecraft.world.entity.boss.enderdragon.EntityEnderDragon;
import net.minecraft.world.phys.Vec3D;

public class DragonControllerLandedSearch extends DragonControllerLandedAbstract {
    private static final int SITTING_SCANNING_IDLE_TICKS = 100;
    private static final int SITTING_ATTACK_Y_VIEW_RANGE = 10;
    private static final int SITTING_ATTACK_VIEW_RANGE = 20;
    private static final int SITTING_CHARGE_VIEW_RANGE = 150;
    private static final PathfinderTargetCondition CHARGE_TARGETING = PathfinderTargetCondition.forCombat().range(150.0D);
    private final PathfinderTargetCondition scanTargeting;
    private int scanningTime;

    public DragonControllerLandedSearch(EntityEnderDragon dragon) {
        super(dragon);
        this.scanTargeting = PathfinderTargetCondition.forCombat().range(20.0D).selector((livingEntity) -> {
            return Math.abs(livingEntity.locY() - dragon.locY()) <= 10.0D;
        });
    }

    @Override
    public void doServerTick() {
        ++this.scanningTime;
        EntityLiving livingEntity = this.dragon.level.getNearestPlayer(this.scanTargeting, this.dragon, this.dragon.locX(), this.dragon.locY(), this.dragon.locZ());
        if (livingEntity != null) {
            if (this.scanningTime > 25) {
                this.dragon.getDragonControllerManager().setControllerPhase(DragonControllerPhase.SITTING_ATTACKING);
            } else {
                Vec3D vec3 = (new Vec3D(livingEntity.locX() - this.dragon.locX(), 0.0D, livingEntity.locZ() - this.dragon.locZ())).normalize();
                Vec3D vec32 = (new Vec3D((double)MathHelper.sin(this.dragon.getYRot() * ((float)Math.PI / 180F)), 0.0D, (double)(-MathHelper.cos(this.dragon.getYRot() * ((float)Math.PI / 180F))))).normalize();
                float f = (float)vec32.dot(vec3);
                float g = (float)(Math.acos((double)f) * (double)(180F / (float)Math.PI)) + 0.5F;
                if (g < 0.0F || g > 10.0F) {
                    double d = livingEntity.locX() - this.dragon.head.locX();
                    double e = livingEntity.locZ() - this.dragon.head.locZ();
                    double h = MathHelper.clamp(MathHelper.wrapDegrees(180.0D - MathHelper.atan2(d, e) * (double)(180F / (float)Math.PI) - (double)this.dragon.getYRot()), -100.0D, 100.0D);
                    this.dragon.yRotA *= 0.8F;
                    float i = (float)Math.sqrt(d * d + e * e) + 1.0F;
                    float j = i;
                    if (i > 40.0F) {
                        i = 40.0F;
                    }

                    this.dragon.yRotA = (float)((double)this.dragon.yRotA + h * (double)(0.7F / i / j));
                    this.dragon.setYRot(this.dragon.getYRot() + this.dragon.yRotA);
                }
            }
        } else if (this.scanningTime >= 100) {
            livingEntity = this.dragon.level.getNearestPlayer(CHARGE_TARGETING, this.dragon, this.dragon.locX(), this.dragon.locY(), this.dragon.locZ());
            this.dragon.getDragonControllerManager().setControllerPhase(DragonControllerPhase.TAKEOFF);
            if (livingEntity != null) {
                this.dragon.getDragonControllerManager().setControllerPhase(DragonControllerPhase.CHARGING_PLAYER);
                this.dragon.getDragonControllerManager().getPhase(DragonControllerPhase.CHARGING_PLAYER).setTarget(new Vec3D(livingEntity.locX(), livingEntity.locY(), livingEntity.locZ()));
            }
        }

    }

    @Override
    public void begin() {
        this.scanningTime = 0;
    }

    @Override
    public DragonControllerPhase<DragonControllerLandedSearch> getControllerPhase() {
        return DragonControllerPhase.SITTING_SCANNING;
    }
}
