package net.minecraft.world.entity.boss.enderdragon.phases;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.boss.enderdragon.EntityEnderCrystal;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.phys.Vec3D;

public interface IDragonController {
    boolean isSitting();

    void doClientTick();

    void doServerTick();

    void onCrystalDestroyed(EntityEnderCrystal crystal, BlockPosition pos, DamageSource source, @Nullable EntityHuman player);

    void begin();

    void end();

    float getFlySpeed();

    float getTurnSpeed();

    DragonControllerPhase<? extends IDragonController> getControllerPhase();

    @Nullable
    Vec3D getFlyTargetLocation();

    float onHurt(DamageSource damageSource, float damage);
}
