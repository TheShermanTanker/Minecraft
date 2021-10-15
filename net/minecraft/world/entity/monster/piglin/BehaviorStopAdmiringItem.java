package net.minecraft.world.entity.monster.piglin;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.item.EntityItem;

public class BehaviorStopAdmiringItem<E extends EntityPiglin> extends Behavior<E> {
    private final int maxDistanceToItem;

    public BehaviorStopAdmiringItem(int range) {
        super(ImmutableMap.of(MemoryModuleType.ADMIRING_ITEM, MemoryStatus.VALUE_PRESENT, MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM, MemoryStatus.REGISTERED));
        this.maxDistanceToItem = range;
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, E entity) {
        if (!entity.getItemInOffHand().isEmpty()) {
            return false;
        } else {
            Optional<EntityItem> optional = entity.getBehaviorController().getMemory(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM);
            if (!optional.isPresent()) {
                return true;
            } else {
                return !optional.get().closerThan(entity, (double)this.maxDistanceToItem);
            }
        }
    }

    @Override
    protected void start(WorldServer world, E entity, long time) {
        entity.getBehaviorController().removeMemory(MemoryModuleType.ADMIRING_ITEM);
    }
}
