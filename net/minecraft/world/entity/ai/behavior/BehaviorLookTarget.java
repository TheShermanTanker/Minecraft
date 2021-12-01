package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.Tag;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumCreatureType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;

public class BehaviorLookTarget extends Behavior<EntityLiving> {
    private final Predicate<EntityLiving> predicate;
    private final float maxDistSqr;
    private Optional<EntityLiving> nearestEntityMatchingTest = Optional.empty();

    public BehaviorLookTarget(Tag<EntityTypes<?>> entityType, float maxDistance) {
        this((entity) -> {
            return entity.getEntityType().is(entityType);
        }, maxDistance);
    }

    public BehaviorLookTarget(EnumCreatureType group, float maxDistance) {
        this((entity) -> {
            return group.equals(entity.getEntityType().getCategory());
        }, maxDistance);
    }

    public BehaviorLookTarget(EntityTypes<?> entityType, float maxDistance) {
        this((entity) -> {
            return entityType.equals(entity.getEntityType());
        }, maxDistance);
    }

    public BehaviorLookTarget(float maxDistance) {
        this((entity) -> {
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
        NearestVisibleLivingEntities nearestVisibleLivingEntities = entity.getBehaviorController().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).get();
        this.nearestEntityMatchingTest = nearestVisibleLivingEntities.findClosest(this.predicate.and((livingEntity2) -> {
            return livingEntity2.distanceToSqr(entity) <= (double)this.maxDistSqr;
        }));
        return this.nearestEntityMatchingTest.isPresent();
    }

    @Override
    protected void start(WorldServer world, EntityLiving entity, long time) {
        entity.getBehaviorController().setMemory(MemoryModuleType.LOOK_TARGET, new BehaviorPositionEntity(this.nearestEntityMatchingTest.get(), true));
        this.nearestEntityMatchingTest = Optional.empty();
    }
}
