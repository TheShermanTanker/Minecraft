package net.minecraft.world.entity.boss.enderdragon.phases;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.particles.Particles;
import net.minecraft.world.entity.boss.enderdragon.EntityEnderDragon;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.feature.WorldGenEndTrophy;
import net.minecraft.world.phys.Vec3D;

public class DragonControllerLanding extends DragonControllerAbstract {
    @Nullable
    private Vec3D targetLocation;

    public DragonControllerLanding(EntityEnderDragon dragon) {
        super(dragon);
    }

    @Override
    public void doClientTick() {
        Vec3D vec3 = this.dragon.getHeadLookVector(1.0F).normalize();
        vec3.yRot((-(float)Math.PI / 4F));
        double d = this.dragon.head.locX();
        double e = this.dragon.head.getY(0.5D);
        double f = this.dragon.head.locZ();

        for(int i = 0; i < 8; ++i) {
            Random random = this.dragon.getRandom();
            double g = d + random.nextGaussian() / 2.0D;
            double h = e + random.nextGaussian() / 2.0D;
            double j = f + random.nextGaussian() / 2.0D;
            Vec3D vec32 = this.dragon.getMot();
            this.dragon.level.addParticle(Particles.DRAGON_BREATH, g, h, j, -vec3.x * (double)0.08F + vec32.x, -vec3.y * (double)0.3F + vec32.y, -vec3.z * (double)0.08F + vec32.z);
            vec3.yRot(0.19634955F);
        }

    }

    @Override
    public void doServerTick() {
        if (this.targetLocation == null) {
            this.targetLocation = Vec3D.atBottomCenterOf(this.dragon.level.getHighestBlockYAt(HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, WorldGenEndTrophy.END_PODIUM_LOCATION));
        }

        if (this.targetLocation.distanceToSqr(this.dragon.locX(), this.dragon.locY(), this.dragon.locZ()) < 1.0D) {
            this.dragon.getDragonControllerManager().getPhase(DragonControllerPhase.SITTING_FLAMING).resetFlameCount();
            this.dragon.getDragonControllerManager().setControllerPhase(DragonControllerPhase.SITTING_SCANNING);
        }

    }

    @Override
    public float getFlySpeed() {
        return 1.5F;
    }

    @Override
    public float getTurnSpeed() {
        float f = (float)this.dragon.getMot().horizontalDistance() + 1.0F;
        float g = Math.min(f, 40.0F);
        return g / f;
    }

    @Override
    public void begin() {
        this.targetLocation = null;
    }

    @Nullable
    @Override
    public Vec3D getFlyTargetLocation() {
        return this.targetLocation;
    }

    @Override
    public DragonControllerPhase<DragonControllerLanding> getControllerPhase() {
        return DragonControllerPhase.LANDING;
    }
}
