package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class SensorGolemLastSeen extends Sensor<EntityLiving> {
    private static final int GOLEM_SCAN_RATE = 200;
    private static final int MEMORY_TIME_TO_LIVE = 600;

    public SensorGolemLastSeen() {
        this(200);
    }

    public SensorGolemLastSeen(int senseInterval) {
        super(senseInterval);
    }

    @Override
    protected void doTick(WorldServer world, EntityLiving entity) {
        checkForNearbyGolem(entity);
    }

    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(MemoryModuleType.NEAREST_LIVING_ENTITIES);
    }

    public static void checkForNearbyGolem(EntityLiving entity) {
        Optional<List<EntityLiving>> optional = entity.getBehaviorController().getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES);
        if (optional.isPresent()) {
            boolean bl = optional.get().stream().anyMatch((livingEntity) -> {
                return livingEntity.getEntityType().equals(EntityTypes.IRON_GOLEM);
            });
            if (bl) {
                golemDetected(entity);
            }

        }
    }

    public static void golemDetected(EntityLiving entity) {
        entity.getBehaviorController().setMemoryWithExpiry(MemoryModuleType.GOLEM_DETECTED_RECENTLY, true, 600L);
    }
}
