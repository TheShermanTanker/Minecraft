package net.minecraft.world.entity.boss.enderdragon.phases;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.particles.Particles;
import net.minecraft.util.MathHelper;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityAreaEffectCloud;
import net.minecraft.world.entity.boss.enderdragon.EntityEnderDragon;
import net.minecraft.world.phys.Vec3D;

public class DragonControllerLandedFlame extends DragonControllerLandedAbstract {
    private static final int FLAME_DURATION = 200;
    private static final int SITTING_FLAME_ATTACKS_COUNT = 4;
    private static final int WARMUP_TIME = 10;
    private int flameTicks;
    private int flameCount;
    private EntityAreaEffectCloud flame;

    public DragonControllerLandedFlame(EntityEnderDragon dragon) {
        super(dragon);
    }

    @Override
    public void doClientTick() {
        ++this.flameTicks;
        if (this.flameTicks % 2 == 0 && this.flameTicks < 10) {
            Vec3D vec3 = this.dragon.getHeadLookVector(1.0F).normalize();
            vec3.yRot((-(float)Math.PI / 4F));
            double d = this.dragon.head.locX();
            double e = this.dragon.head.getY(0.5D);
            double f = this.dragon.head.locZ();

            for(int i = 0; i < 8; ++i) {
                double g = d + this.dragon.getRandom().nextGaussian() / 2.0D;
                double h = e + this.dragon.getRandom().nextGaussian() / 2.0D;
                double j = f + this.dragon.getRandom().nextGaussian() / 2.0D;

                for(int k = 0; k < 6; ++k) {
                    this.dragon.level.addParticle(Particles.DRAGON_BREATH, g, h, j, -vec3.x * (double)0.08F * (double)k, -vec3.y * (double)0.6F, -vec3.z * (double)0.08F * (double)k);
                }

                vec3.yRot(0.19634955F);
            }
        }

    }

    @Override
    public void doServerTick() {
        ++this.flameTicks;
        if (this.flameTicks >= 200) {
            if (this.flameCount >= 4) {
                this.dragon.getDragonControllerManager().setControllerPhase(DragonControllerPhase.TAKEOFF);
            } else {
                this.dragon.getDragonControllerManager().setControllerPhase(DragonControllerPhase.SITTING_SCANNING);
            }
        } else if (this.flameTicks == 10) {
            Vec3D vec3 = (new Vec3D(this.dragon.head.locX() - this.dragon.locX(), 0.0D, this.dragon.head.locZ() - this.dragon.locZ())).normalize();
            float f = 5.0F;
            double d = this.dragon.head.locX() + vec3.x * 5.0D / 2.0D;
            double e = this.dragon.head.locZ() + vec3.z * 5.0D / 2.0D;
            double g = this.dragon.head.getY(0.5D);
            double h = g;
            BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition(d, g, e);

            while(this.dragon.level.isEmpty(mutableBlockPos)) {
                --h;
                if (h < 0.0D) {
                    h = g;
                    break;
                }

                mutableBlockPos.set(d, h, e);
            }

            h = (double)(MathHelper.floor(h) + 1);
            this.flame = new EntityAreaEffectCloud(this.dragon.level, d, h, e);
            this.flame.setSource(this.dragon);
            this.flame.setRadius(5.0F);
            this.flame.setDuration(200);
            this.flame.setParticle(Particles.DRAGON_BREATH);
            this.flame.addEffect(new MobEffect(MobEffects.HARM));
            this.dragon.level.addEntity(this.flame);
        }

    }

    @Override
    public void begin() {
        this.flameTicks = 0;
        ++this.flameCount;
    }

    @Override
    public void end() {
        if (this.flame != null) {
            this.flame.die();
            this.flame = null;
        }

    }

    @Override
    public DragonControllerPhase<DragonControllerLandedFlame> getControllerPhase() {
        return DragonControllerPhase.SITTING_FLAMING;
    }

    public void resetFlameCount() {
        this.flameCount = 0;
    }
}
