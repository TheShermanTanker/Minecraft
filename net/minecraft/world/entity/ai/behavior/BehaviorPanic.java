package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.BehaviorController;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.EntityVillager;
import net.minecraft.world.entity.schedule.Activity;

public class BehaviorPanic extends Behavior<EntityVillager> {
    public BehaviorPanic() {
        super(ImmutableMap.of());
    }

    @Override
    protected boolean canStillUse(WorldServer serverLevel, EntityVillager villager, long l) {
        return isHurt(villager) || hasHostile(villager);
    }

    @Override
    protected void start(WorldServer serverLevel, EntityVillager villager, long l) {
        if (isHurt(villager) || hasHostile(villager)) {
            BehaviorController<?> brain = villager.getBehaviorController();
            if (!brain.isActive(Activity.PANIC)) {
                brain.removeMemory(MemoryModuleType.PATH);
                brain.removeMemory(MemoryModuleType.WALK_TARGET);
                brain.removeMemory(MemoryModuleType.LOOK_TARGET);
                brain.removeMemory(MemoryModuleType.BREED_TARGET);
                brain.removeMemory(MemoryModuleType.INTERACTION_TARGET);
            }

            brain.setActiveActivityIfPossible(Activity.PANIC);
        }

    }

    @Override
    protected void tick(WorldServer serverLevel, EntityVillager villager, long l) {
        if (l % 100L == 0L) {
            villager.spawnGolemIfNeeded(serverLevel, l, 3);
        }

    }

    public static boolean hasHostile(EntityLiving entity) {
        return entity.getBehaviorController().hasMemory(MemoryModuleType.NEAREST_HOSTILE);
    }

    public static boolean isHurt(EntityLiving entity) {
        return entity.getBehaviorController().hasMemory(MemoryModuleType.HURT_BY);
    }
}
