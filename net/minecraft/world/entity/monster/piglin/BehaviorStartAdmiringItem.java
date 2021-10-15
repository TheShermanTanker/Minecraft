package net.minecraft.world.entity.monster.piglin;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.item.EntityItem;

public class BehaviorStartAdmiringItem<E extends EntityPiglin> extends Behavior<E> {
    private final int admireDuration;

    public BehaviorStartAdmiringItem(int duration) {
        super(ImmutableMap.of(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM, MemoryStatus.VALUE_PRESENT, MemoryModuleType.ADMIRING_ITEM, MemoryStatus.VALUE_ABSENT, MemoryModuleType.ADMIRING_DISABLED, MemoryStatus.VALUE_ABSENT, MemoryModuleType.DISABLE_WALK_TO_ADMIRE_ITEM, MemoryStatus.VALUE_ABSENT));
        this.admireDuration = duration;
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, E entity) {
        EntityItem itemEntity = entity.getBehaviorController().getMemory(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM).get();
        return PiglinAI.isLovedItem(itemEntity.getItemStack());
    }

    @Override
    protected void start(WorldServer world, E entity, long time) {
        entity.getBehaviorController().setMemoryWithExpiry(MemoryModuleType.ADMIRING_ITEM, true, (long)this.admireDuration);
    }
}
