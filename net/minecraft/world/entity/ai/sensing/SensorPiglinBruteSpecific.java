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
        Optional<EntityInsentient> optional = Optional.empty();
        List<EntityPiglinAbstract> list = Lists.newArrayList();

        for(EntityLiving livingEntity : brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).orElse(ImmutableList.of())) {
            if (livingEntity instanceof EntitySkeletonWither || livingEntity instanceof EntityWither) {
                optional = Optional.of((EntityInsentient)livingEntity);
                break;
            }
        }

        for(EntityLiving livingEntity2 : brain.getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES).orElse(ImmutableList.of())) {
            if (livingEntity2 instanceof EntityPiglinAbstract && ((EntityPiglinAbstract)livingEntity2).isAdult()) {
                list.add((EntityPiglinAbstract)livingEntity2);
            }
        }

        brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_NEMESIS, optional);
        brain.setMemory(MemoryModuleType.NEARBY_ADULT_PIGLINS, list);
    }
}
