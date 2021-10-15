package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.BehaviorController;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.level.block.BlockBell;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;

public class BehaviorBellRing extends Behavior<EntityLiving> {
    private static final float BELL_RING_CHANCE = 0.95F;
    public static final int RING_BELL_FROM_DISTANCE = 3;

    public BehaviorBellRing() {
        super(ImmutableMap.of(MemoryModuleType.MEETING_POINT, MemoryStatus.VALUE_PRESENT));
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, EntityLiving entity) {
        return world.random.nextFloat() > 0.95F;
    }

    @Override
    protected void start(WorldServer world, EntityLiving entity, long time) {
        BehaviorController<?> brain = entity.getBehaviorController();
        BlockPosition blockPos = brain.getMemory(MemoryModuleType.MEETING_POINT).get().getBlockPosition();
        if (blockPos.closerThan(entity.getChunkCoordinates(), 3.0D)) {
            IBlockData blockState = world.getType(blockPos);
            if (blockState.is(Blocks.BELL)) {
                BlockBell bellBlock = (BlockBell)blockState.getBlock();
                bellBlock.attemptToRing(entity, world, blockPos, (EnumDirection)null);
            }
        }

    }
}
