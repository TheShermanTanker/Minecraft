package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.MemoryTarget;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3D;

public class AnimalPanic extends Behavior<EntityCreature> {
    private static final int PANIC_MIN_DURATION = 100;
    private static final int PANIC_MAX_DURATION = 120;
    private static final int PANIC_DISTANCE_HORIZANTAL = 5;
    private static final int PANIC_DISTANCE_VERTICAL = 4;
    private final float speedMultiplier;

    public AnimalPanic(float speed) {
        super(ImmutableMap.of(MemoryModuleType.HURT_BY, MemoryStatus.VALUE_PRESENT), 100, 120);
        this.speedMultiplier = speed;
    }

    @Override
    protected boolean canStillUse(WorldServer serverLevel, EntityCreature pathfinderMob, long l) {
        return true;
    }

    @Override
    protected void start(WorldServer serverLevel, EntityCreature pathfinderMob, long l) {
        pathfinderMob.getBehaviorController().removeMemory(MemoryModuleType.WALK_TARGET);
    }

    @Override
    protected void tick(WorldServer serverLevel, EntityCreature pathfinderMob, long l) {
        if (pathfinderMob.getNavigation().isDone()) {
            Vec3D vec3 = LandRandomPos.getPos(pathfinderMob, 5, 4);
            if (vec3 != null) {
                pathfinderMob.getBehaviorController().setMemory(MemoryModuleType.WALK_TARGET, new MemoryTarget(vec3, this.speedMultiplier, 0));
            }
        }

    }
}
