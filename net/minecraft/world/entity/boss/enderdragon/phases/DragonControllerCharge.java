package net.minecraft.world.entity.boss.enderdragon.phases;

import javax.annotation.Nullable;
import net.minecraft.world.entity.boss.enderdragon.EntityEnderDragon;
import net.minecraft.world.phys.Vec3D;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DragonControllerCharge extends DragonControllerAbstract {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int CHARGE_RECOVERY_TIME = 10;
    private Vec3D targetLocation;
    private int timeSinceCharge;

    public DragonControllerCharge(EntityEnderDragon dragon) {
        super(dragon);
    }

    @Override
    public void doServerTick() {
        if (this.targetLocation == null) {
            LOGGER.warn("Aborting charge player as no target was set.");
            this.dragon.getDragonControllerManager().setControllerPhase(DragonControllerPhase.HOLDING_PATTERN);
        } else if (this.timeSinceCharge > 0 && this.timeSinceCharge++ >= 10) {
            this.dragon.getDragonControllerManager().setControllerPhase(DragonControllerPhase.HOLDING_PATTERN);
        } else {
            double d = this.targetLocation.distanceToSqr(this.dragon.locX(), this.dragon.locY(), this.dragon.locZ());
            if (d < 100.0D || d > 22500.0D || this.dragon.horizontalCollision || this.dragon.verticalCollision) {
                ++this.timeSinceCharge;
            }

        }
    }

    @Override
    public void begin() {
        this.targetLocation = null;
        this.timeSinceCharge = 0;
    }

    public void setTarget(Vec3D pathTarget) {
        this.targetLocation = pathTarget;
    }

    @Override
    public float getFlySpeed() {
        return 3.0F;
    }

    @Nullable
    @Override
    public Vec3D getFlyTargetLocation() {
        return this.targetLocation;
    }

    @Override
    public DragonControllerPhase<DragonControllerCharge> getControllerPhase() {
        return DragonControllerPhase.CHARGING_PLAYER;
    }
}
