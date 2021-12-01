package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.MemoryTarget;

public class BehaviorStrollInside extends Behavior<EntityCreature> {
    private final float speedModifier;

    public BehaviorStrollInside(float speed) {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT));
        this.speedModifier = speed;
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, EntityCreature entity) {
        return !world.canSeeSky(entity.getChunkCoordinates());
    }

    @Override
    protected void start(WorldServer world, EntityCreature entity, long time) {
        BlockPosition blockPos = entity.getChunkCoordinates();
        List<BlockPosition> list = BlockPosition.betweenClosedStream(blockPos.offset(-1, -1, -1), blockPos.offset(1, 1, 1)).map(BlockPosition::immutableCopy).collect(Collectors.toList());
        Collections.shuffle(list);
        Optional<BlockPosition> optional = list.stream().filter((pos) -> {
            return !world.canSeeSky(pos);
        }).filter((pos) -> {
            return world.loadedAndEntityCanStandOn(pos, entity);
        }).filter((blockPosx) -> {
            return world.getCubes(entity);
        }).findFirst();
        optional.ifPresent((pos) -> {
            entity.getBehaviorController().setMemory(MemoryModuleType.WALK_TARGET, new MemoryTarget(pos, this.speedModifier, 0));
        });
    }
}
