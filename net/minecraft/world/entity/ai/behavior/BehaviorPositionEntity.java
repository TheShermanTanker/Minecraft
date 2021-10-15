package net.minecraft.world.entity.ai.behavior;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.phys.Vec3D;

public class BehaviorPositionEntity implements BehaviorPosition {
    private final Entity entity;
    private final boolean trackEyeHeight;

    public BehaviorPositionEntity(Entity entity, boolean useEyeHeight) {
        this.entity = entity;
        this.trackEyeHeight = useEyeHeight;
    }

    @Override
    public Vec3D currentPosition() {
        return this.trackEyeHeight ? this.entity.getPositionVector().add(0.0D, (double)this.entity.getHeadHeight(), 0.0D) : this.entity.getPositionVector();
    }

    @Override
    public BlockPosition currentBlockPosition() {
        return this.entity.getChunkCoordinates();
    }

    @Override
    public boolean isVisibleBy(EntityLiving entity) {
        if (!(this.entity instanceof EntityLiving)) {
            return true;
        } else {
            Optional<List<EntityLiving>> optional = entity.getBehaviorController().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
            return this.entity.isAlive() && optional.isPresent() && optional.get().contains(this.entity);
        }
    }

    public Entity getEntity() {
        return this.entity;
    }

    @Override
    public String toString() {
        return "EntityTracker for " + this.entity;
    }
}
