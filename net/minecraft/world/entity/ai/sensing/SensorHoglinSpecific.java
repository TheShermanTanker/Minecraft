package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.BehaviorController;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.monster.hoglin.EntityHoglin;
import net.minecraft.world.entity.monster.piglin.EntityPiglin;

public class SensorHoglinSpecific extends Sensor<EntityHoglin> {
    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryModuleType.NEAREST_REPELLENT, MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLIN, MemoryModuleType.NEAREST_VISIBLE_ADULT_HOGLINS, MemoryModuleType.VISIBLE_ADULT_PIGLIN_COUNT, MemoryModuleType.VISIBLE_ADULT_HOGLIN_COUNT);
    }

    @Override
    protected void doTick(WorldServer world, EntityHoglin entity) {
        BehaviorController<?> brain = entity.getBehaviorController();
        brain.setMemory(MemoryModuleType.NEAREST_REPELLENT, this.findNearestRepellent(world, entity));
        Optional<EntityPiglin> optional = Optional.empty();
        int i = 0;
        List<EntityHoglin> list = Lists.newArrayList();
        NearestVisibleLivingEntities nearestVisibleLivingEntities = brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).orElse(NearestVisibleLivingEntities.empty());

        for(EntityLiving livingEntity : nearestVisibleLivingEntities.findAll((livingEntityx) -> {
            return !livingEntityx.isBaby() && (livingEntityx instanceof EntityPiglin || livingEntityx instanceof EntityHoglin);
        })) {
            if (livingEntity instanceof EntityPiglin) {
                EntityPiglin piglin = (EntityPiglin)livingEntity;
                ++i;
                if (optional.isEmpty()) {
                    optional = Optional.of(piglin);
                }
            }

            if (livingEntity instanceof EntityHoglin) {
                EntityHoglin hoglin = (EntityHoglin)livingEntity;
                list.add(hoglin);
            }
        }

        brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLIN, optional);
        brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_ADULT_HOGLINS, list);
        brain.setMemory(MemoryModuleType.VISIBLE_ADULT_PIGLIN_COUNT, i);
        brain.setMemory(MemoryModuleType.VISIBLE_ADULT_HOGLIN_COUNT, list.size());
    }

    private Optional<BlockPosition> findNearestRepellent(WorldServer world, EntityHoglin hoglin) {
        return BlockPosition.findClosestMatch(hoglin.getChunkCoordinates(), 8, 4, (pos) -> {
            return world.getType(pos).is(TagsBlock.HOGLIN_REPELLENTS);
        });
    }
}
