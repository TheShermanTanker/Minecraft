package net.minecraft.world.entity.boss.enderdragon.phases;

import javax.annotation.Nullable;
import net.minecraft.world.entity.boss.enderdragon.EntityEnderDragon;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DragonControllerManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private final EntityEnderDragon dragon;
    private final IDragonController[] phases = new IDragonController[DragonControllerPhase.getCount()];
    @Nullable
    private IDragonController currentPhase;

    public DragonControllerManager(EntityEnderDragon dragon) {
        this.dragon = dragon;
        this.setControllerPhase(DragonControllerPhase.HOVERING);
    }

    public void setControllerPhase(DragonControllerPhase<?> type) {
        if (this.currentPhase == null || type != this.currentPhase.getControllerPhase()) {
            if (this.currentPhase != null) {
                this.currentPhase.end();
            }

            this.currentPhase = this.getPhase(type);
            if (!this.dragon.level.isClientSide) {
                this.dragon.getDataWatcher().set(EntityEnderDragon.DATA_PHASE, type.getId());
            }

            LOGGER.debug("Dragon is now in phase {} on the {}", type, this.dragon.level.isClientSide ? "client" : "server");
            this.currentPhase.begin();
        }
    }

    public IDragonController getCurrentPhase() {
        return this.currentPhase;
    }

    public <T extends IDragonController> T getPhase(DragonControllerPhase<T> type) {
        int i = type.getId();
        if (this.phases[i] == null) {
            this.phases[i] = type.createInstance(this.dragon);
        }

        return (T)this.phases[i];
    }
}
