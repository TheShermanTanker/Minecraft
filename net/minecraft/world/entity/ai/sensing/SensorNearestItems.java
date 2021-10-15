package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableSet;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.ai.BehaviorController;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.item.EntityItem;

public class SensorNearestItems extends Sensor<EntityInsentient> {
    private static final long XZ_RANGE = 8L;
    private static final long Y_RANGE = 4L;
    public static final int MAX_DISTANCE_TO_WANTED_ITEM = 9;

    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM);
    }

    @Override
    protected void doTick(WorldServer world, EntityInsentient entity) {
        BehaviorController<?> brain = entity.getBehaviorController();
        List<EntityItem> list = world.getEntitiesOfClass(EntityItem.class, entity.getBoundingBox().grow(8.0D, 4.0D, 8.0D), (itemEntity) -> {
            return true;
        });
        list.sort(Comparator.comparingDouble(entity::distanceToSqr));
        Optional<EntityItem> optional = list.stream().filter((itemEntity) -> {
            return entity.wantsToPickUp(itemEntity.getItemStack());
        }).filter((itemEntity) -> {
            return itemEntity.closerThan(entity, 9.0D);
        }).filter(entity::hasLineOfSight).findFirst();
        brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM, optional);
    }
}
