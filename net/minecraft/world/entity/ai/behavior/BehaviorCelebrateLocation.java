package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class BehaviorCelebrateLocation<E extends EntityInsentient> extends Behavior<E> {
    private final int closeEnoughDist;
    private final float speedModifier;

    public BehaviorCelebrateLocation(int completionRange, float speed) {
        super(ImmutableMap.of(MemoryModuleType.CELEBRATE_LOCATION, MemoryStatus.VALUE_PRESENT, MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED));
        this.closeEnoughDist = completionRange;
        this.speedModifier = speed;
    }

    @Override
    protected void start(WorldServer world, EntityInsentient entity, long time) {
        BlockPosition blockPos = getCelebrateLocation(entity);
        boolean bl = blockPos.closerThan(entity.getChunkCoordinates(), (double)this.closeEnoughDist);
        if (!bl) {
            BehaviorUtil.setWalkAndLookTargetMemories(entity, getNearbyPos(entity, blockPos), this.speedModifier, this.closeEnoughDist);
        }

    }

    private static BlockPosition getNearbyPos(EntityInsentient mob, BlockPosition pos) {
        Random random = mob.level.random;
        return pos.offset(getRandomOffset(random), 0, getRandomOffset(random));
    }

    private static int getRandomOffset(Random random) {
        return random.nextInt(3) - 1;
    }

    private static BlockPosition getCelebrateLocation(EntityInsentient entity) {
        return entity.getBehaviorController().getMemory(MemoryModuleType.CELEBRATE_LOCATION).get();
    }
}
