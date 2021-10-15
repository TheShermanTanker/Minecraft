package net.minecraft.world.entity.boss.enderdragon.phases;

import javax.annotation.Nullable;
import net.minecraft.world.entity.boss.enderdragon.EntityEnderDragon;
import net.minecraft.world.phys.Vec3D;

public class DragonControllerHover extends DragonControllerAbstract {
    private Vec3D targetLocation;

    public DragonControllerHover(EntityEnderDragon dragon) {
        super(dragon);
    }

    @Override
    public void doServerTick() {
        if (this.targetLocation == null) {
            this.targetLocation = this.dragon.getPositionVector();
        }

    }

    @Override
    public boolean isSitting() {
        return true;
    }

    @Override
    public void begin() {
        this.targetLocation = null;
    }

    @Override
    public float getFlySpeed() {
        return 1.0F;
    }

    @Nullable
    @Override
    public Vec3D getFlyTargetLocation() {
        return this.targetLocation;
    }

    @Override
    public DragonControllerPhase<DragonControllerHover> getControllerPhase() {
        return DragonControllerPhase.HOVERING;
    }
}
