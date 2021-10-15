package net.minecraft.world.entity.monster.piglin;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.Items;

public class BehaviorStopAdmiring<E extends EntityPiglin> extends Behavior<E> {
    public BehaviorStopAdmiring() {
        super(ImmutableMap.of(MemoryModuleType.ADMIRING_ITEM, MemoryStatus.VALUE_ABSENT));
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, E entity) {
        return !entity.getItemInOffHand().isEmpty() && !entity.getItemInOffHand().is(Items.SHIELD);
    }

    @Override
    protected void start(WorldServer world, E entity, long time) {
        PiglinAI.stopHoldingOffHandItem(entity, true);
    }
}
