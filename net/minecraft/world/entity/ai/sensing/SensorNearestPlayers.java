package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableSet;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.IEntitySelector;
import net.minecraft.world.entity.ai.BehaviorController;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.EntityHuman;

public class SensorNearestPlayers extends Sensor<EntityLiving> {
    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(MemoryModuleType.NEAREST_PLAYERS, MemoryModuleType.NEAREST_VISIBLE_PLAYER, MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER);
    }

    @Override
    protected void doTick(WorldServer world, EntityLiving entity) {
        List<EntityHuman> list = world.getPlayers().stream().filter(IEntitySelector.NO_SPECTATORS).filter((serverPlayer) -> {
            return entity.closerThan(serverPlayer, 16.0D);
        }).sorted(Comparator.comparingDouble(entity::distanceToSqr)).collect(Collectors.toList());
        BehaviorController<?> brain = entity.getBehaviorController();
        brain.setMemory(MemoryModuleType.NEAREST_PLAYERS, list);
        List<EntityHuman> list2 = list.stream().filter((player) -> {
            return isEntityTargetable(entity, player);
        }).collect(Collectors.toList());
        brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_PLAYER, list2.isEmpty() ? null : list2.get(0));
        Optional<EntityHuman> optional = list2.stream().filter((player) -> {
            return isEntityAttackable(entity, player);
        }).findFirst();
        brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER, optional);
    }
}
