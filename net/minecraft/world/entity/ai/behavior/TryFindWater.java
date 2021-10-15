package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.TagsFluid;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;

public class TryFindWater extends Behavior<EntityCreature> {
    private final int range;
    private final float speedModifier;
    private long nextOkStartTime;

    public TryFindWater(int range, float speed) {
        super(ImmutableMap.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED));
        this.range = range;
        this.speedModifier = speed;
    }

    @Override
    protected void stop(WorldServer serverLevel, EntityCreature pathfinderMob, long l) {
        this.nextOkStartTime = l + 20L + 2L;
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, EntityCreature entity) {
        return !entity.level.getFluid(entity.getChunkCoordinates()).is(TagsFluid.WATER);
    }

    @Override
    protected void start(WorldServer serverLevel, EntityCreature pathfinderMob, long l) {
        if (l >= this.nextOkStartTime) {
            BlockPosition blockPos = null;
            BlockPosition blockPos2 = null;
            BlockPosition blockPos3 = pathfinderMob.getChunkCoordinates();

            for(BlockPosition blockPos4 : BlockPosition.withinManhattan(blockPos3, this.range, this.range, this.range)) {
                if (blockPos4.getX() != blockPos3.getX() || blockPos4.getZ() != blockPos3.getZ()) {
                    IBlockData blockState = pathfinderMob.level.getType(blockPos4.above());
                    IBlockData blockState2 = pathfinderMob.level.getType(blockPos4);
                    if (blockState2.is(Blocks.WATER)) {
                        if (blockState.isAir()) {
                            blockPos = blockPos4.immutableCopy();
                            break;
                        }

                        if (blockPos2 == null && !blockPos4.closerThan(pathfinderMob.getPositionVector(), 1.5D)) {
                            blockPos2 = blockPos4.immutableCopy();
                        }
                    }
                }
            }

            if (blockPos == null) {
                blockPos = blockPos2;
            }

            if (blockPos != null) {
                this.nextOkStartTime = l + 40L;
                BehaviorUtil.setWalkAndLookTargetMemories(pathfinderMob, blockPos, this.speedModifier, 0);
            }

        }
    }
}
