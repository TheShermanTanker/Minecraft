package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.function.Predicate;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.Tag;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumCreatureType;
import net.minecraft.world.entity.ai.BehaviorController;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class BehaviorLookTarget extends Behavior<EntityLiving> {
    private final Predicate<EntityLiving> predicate;
    private final float maxDistSqr;

    public BehaviorLookTarget(Tag<EntityTypes<?>> entityType, float maxDistance) {
        this((livingEntity) -> {
            return livingEntity.getEntityType().is(entityType);
        }, maxDistance);
    }

    public BehaviorLookTarget(EnumCreatureType group, float maxDistance) {
        this((livingEntity) -> {
            return group.equals(livingEntity.getEntityType().getCategory());
        }, maxDistance);
    }

    public BehaviorLookTarget(EntityTypes<?> entityType, float maxDistance) {
        this((livingEntity) -> {
            return entityType.equals(livingEntity.getEntityType());
        }, maxDistance);
    }

    public BehaviorLookTarget(float maxDistance) {
        this((livingEntity) -> {
            return true;
        }, maxDistance);
    }

    public BehaviorLookTarget(Predicate<EntityLiving> predicate, float maxDistance) {
        super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT));
        this.predicate = predicate;
        this.maxDistSqr = maxDistance * maxDistance;
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, EntityLiving entity) {
        return entity.getBehaviorController().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).get().stream().anyMatch(this.predicate);
    }

    @Override
    protected void start(WorldServer world, EntityLiving entity, long time) {
        BehaviorController<?> brain = entity.getBehaviorController();
        brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).ifPresent((list) -> {
            list.stream().filter(this.predicate).filter((livingEntity2) -> {
                return livingEntity2.distanceToSqr(entity) <= (double)this.maxDistSqr;
            }).findFirst().ifPresent((livingEntity) -> {
                brain.setMemory(MemoryModuleType.LOOK_TARGET, new BehaviorPositionEntity(livingEntity, true));
            });
        });
    }
}
