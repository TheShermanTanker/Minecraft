package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.BehaviorController;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.boss.wither.EntityWither;
import net.minecraft.world.entity.monster.EntitySkeletonWither;
import net.minecraft.world.entity.monster.piglin.EntityPiglinAbstract;

public class SensorPiglinBruteSpecific extends Sensor<EntityLiving> {
    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryModuleType.NEAREST_VISIBLE_NEMESIS, MemoryModuleType.NEARBY_ADULT_PIGLINS);
    }

    @Override
    protected void doTick(WorldServer world, EntityLiving entity) {
        BehaviorController<?> brain = entity.getBehaviorController();
        List<EntityPiglinAbstract> list = Lists.newArrayList();
        NearestVisibleLivingEntities nearestVisibleLivingEntities = brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).orElse(NearestVisibleLivingEntities.empty());
        Optional<EntityInsentient> optional = nearestVisibleLivingEntities.findClosest((livingEntityx) -> {
            return livingEntityx instanceof EntitySkeletonWither || livingEntityx instanceof EntityWither;
        }).map(EntityInsentient.class::cast);

        for(EntityLiving livingEntity : brain.getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES).orElse(ImmutableList.of())) {
            if (livingEntity instanceof EntityPiglinAbstract && ((EntityPiglinAbstract)livingEntity).isAdult()) {
                list.add((EntityPiglinAbstract)livingEntity);
            }
        }

        brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_NEMESIS, optional);
        brain.setMemory(MemoryModuleType.NEARBY_ADULT_PIGLINS, list);
    }
}
