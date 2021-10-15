package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.BehaviorController;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.animal.EntityAnimal;

public class BehaviorMakeLoveAnimal extends Behavior<EntityAnimal> {
    private static final int BREED_RANGE = 3;
    private static final int MIN_DURATION = 60;
    private static final int MAX_DURATION = 110;
    private final EntityTypes<? extends EntityAnimal> partnerType;
    private final float speedModifier;
    private long spawnChildAtTime;

    public BehaviorMakeLoveAnimal(EntityTypes<? extends EntityAnimal> targetType, float speed) {
        super(ImmutableMap.of(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT, MemoryModuleType.BREED_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED), 110);
        this.partnerType = targetType;
        this.speedModifier = speed;
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, EntityAnimal entity) {
        return entity.isInLove() && this.findValidBreedPartner(entity).isPresent();
    }

    @Override
    protected void start(WorldServer world, EntityAnimal entity, long time) {
        EntityAnimal animal = this.findValidBreedPartner(entity).get();
        entity.getBehaviorController().setMemory(MemoryModuleType.BREED_TARGET, animal);
        animal.getBehaviorController().setMemory(MemoryModuleType.BREED_TARGET, entity);
        BehaviorUtil.lockGazeAndWalkToEachOther(entity, animal, this.speedModifier);
        int i = 60 + entity.getRandom().nextInt(50);
        this.spawnChildAtTime = time + (long)i;
    }

    @Override
    protected boolean canStillUse(WorldServer world, EntityAnimal entity, long time) {
        if (!this.hasBreedTargetOfRightType(entity)) {
            return false;
        } else {
            EntityAnimal animal = this.getBreedTarget(entity);
            return animal.isAlive() && entity.mate(animal) && BehaviorUtil.entityIsVisible(entity.getBehaviorController(), animal) && time <= this.spawnChildAtTime;
        }
    }

    @Override
    protected void tick(WorldServer serverLevel, EntityAnimal animal, long l) {
        EntityAnimal animal2 = this.getBreedTarget(animal);
        BehaviorUtil.lockGazeAndWalkToEachOther(animal, animal2, this.speedModifier);
        if (animal.closerThan(animal2, 3.0D)) {
            if (l >= this.spawnChildAtTime) {
                animal.spawnChildFromBreeding(serverLevel, animal2);
                animal.getBehaviorController().removeMemory(MemoryModuleType.BREED_TARGET);
                animal2.getBehaviorController().removeMemory(MemoryModuleType.BREED_TARGET);
            }

        }
    }

    @Override
    protected void stop(WorldServer serverLevel, EntityAnimal animal, long l) {
        animal.getBehaviorController().removeMemory(MemoryModuleType.BREED_TARGET);
        animal.getBehaviorController().removeMemory(MemoryModuleType.WALK_TARGET);
        animal.getBehaviorController().removeMemory(MemoryModuleType.LOOK_TARGET);
        this.spawnChildAtTime = 0L;
    }

    private EntityAnimal getBreedTarget(EntityAnimal animal) {
        return (EntityAnimal)animal.getBehaviorController().getMemory(MemoryModuleType.BREED_TARGET).get();
    }

    private boolean hasBreedTargetOfRightType(EntityAnimal animal) {
        BehaviorController<?> brain = animal.getBehaviorController();
        return brain.hasMemory(MemoryModuleType.BREED_TARGET) && brain.getMemory(MemoryModuleType.BREED_TARGET).get().getEntityType() == this.partnerType;
    }

    private Optional<? extends EntityAnimal> findValidBreedPartner(EntityAnimal animal) {
        return animal.getBehaviorController().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).get().stream().filter((livingEntity) -> {
            return livingEntity.getEntityType() == this.partnerType;
        }).map((livingEntity) -> {
            return (EntityAnimal)livingEntity;
        }).filter(animal::mate).findFirst();
    }
}
