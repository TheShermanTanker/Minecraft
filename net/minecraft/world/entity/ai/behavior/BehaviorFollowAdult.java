package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.function.Function;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.EntityAgeable;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class BehaviorFollowAdult<E extends EntityAgeable> extends Behavior<E> {
    private final UniformInt followRange;
    private final Function<EntityLiving, Float> speedModifier;

    public BehaviorFollowAdult(UniformInt executionRange, float speed) {
        this(executionRange, (entity) -> {
            return speed;
        });
    }

    public BehaviorFollowAdult(UniformInt executionRange, Function<EntityLiving, Float> speed) {
        super(ImmutableMap.of(MemoryModuleType.NEAREST_VISIBLE_ADULT, MemoryStatus.VALUE_PRESENT, MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT));
        this.followRange = executionRange;
        this.speedModifier = speed;
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, E entity) {
        if (!entity.isBaby()) {
            return false;
        } else {
            EntityAgeable ageableMob = this.getNearestAdult(entity);
            return entity.closerThan(ageableMob, (double)(this.followRange.getMaxValue() + 1)) && !entity.closerThan(ageableMob, (double)this.followRange.getMinValue());
        }
    }

    @Override
    protected void start(WorldServer world, E entity, long time) {
        BehaviorUtil.setWalkAndLookTargetMemories(entity, this.getNearestAdult(entity), this.speedModifier.apply(entity), this.followRange.getMinValue() - 1);
    }

    private EntityAgeable getNearestAdult(E entity) {
        return entity.getBehaviorController().getMemory(MemoryModuleType.NEAREST_VISIBLE_ADULT).get();
    }
}
