package net.minecraft.world.entity.boss.enderdragon.phases;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.boss.enderdragon.EntityEnderCrystal;
import net.minecraft.world.entity.boss.enderdragon.EntityEnderDragon;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.phys.Vec3D;

public abstract class DragonControllerAbstract implements IDragonController {
    protected final EntityEnderDragon dragon;

    public DragonControllerAbstract(EntityEnderDragon dragon) {
        this.dragon = dragon;
    }

    @Override
    public boolean isSitting() {
        return false;
    }

    @Override
    public void doClientTick() {
    }

    @Override
    public void doServerTick() {
    }

    @Override
    public void onCrystalDestroyed(EntityEnderCrystal crystal, BlockPosition pos, DamageSource source, @Nullable EntityHuman player) {
    }

    @Override
    public void begin() {
    }

    @Override
    public void end() {
    }

    @Override
    public float getFlySpeed() {
        return 0.6F;
    }

    @Nullable
    @Override
    public Vec3D getFlyTargetLocation() {
        return null;
    }

    @Override
    public float onHurt(DamageSource damageSource, float damage) {
        return damage;
    }

    @Override
    public float getTurnSpeed() {
        float f = (float)this.dragon.getMot().horizontalDistance() + 1.0F;
        float g = Math.min(f, 40.0F);
        return 0.7F / g / f;
    }
}
