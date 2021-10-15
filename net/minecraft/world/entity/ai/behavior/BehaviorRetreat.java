package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class BehaviorRetreat<E extends EntityInsentient> extends Behavior<E> {
    private final int tooCloseDistance;
    private final float strafeSpeed;

    public BehaviorRetreat(int distance, float forwardMovement) {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT));
        this.tooCloseDistance = distance;
        this.strafeSpeed = forwardMovement;
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, E entity) {
        return this.isTargetVisible(entity) && this.isTargetTooClose(entity);
    }

    @Override
    protected void start(WorldServer world, E entity, long time) {
        entity.getBehaviorController().setMemory(MemoryModuleType.LOOK_TARGET, new BehaviorPositionEntity(this.getTarget(entity), true));
        entity.getControllerMove().strafe(-this.strafeSpeed, 0.0F);
        entity.setYRot(MathHelper.rotateIfNecessary(entity.getYRot(), entity.yHeadRot, 0.0F));
    }

    private boolean isTargetVisible(E entity) {
        return entity.getBehaviorController().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).get().contains(this.getTarget(entity));
    }

    private boolean isTargetTooClose(E entity) {
        return this.getTarget(entity).closerThan(entity, (double)this.tooCloseDistance);
    }

    private EntityLiving getTarget(E entity) {
        return entity.getBehaviorController().getMemory(MemoryModuleType.ATTACK_TARGET).get();
    }
}
