package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import java.util.Iterator;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.BehaviorController;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.level.block.BlockDoor;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.pathfinder.PathEntity;
import net.minecraft.world.level.pathfinder.PathPoint;

public class BehaviorInteractDoor extends Behavior<EntityLiving> {
    private static final int COOLDOWN_BEFORE_RERUNNING_IN_SAME_NODE = 20;
    private static final double SKIP_CLOSING_DOOR_IF_FURTHER_AWAY_THAN = 2.0D;
    private static final double MAX_DISTANCE_TO_HOLD_DOOR_OPEN_FOR_OTHER_MOBS = 2.0D;
    @Nullable
    private PathPoint lastCheckedNode;
    private int remainingCooldown;

    public BehaviorInteractDoor() {
        super(ImmutableMap.of(MemoryModuleType.PATH, MemoryStatus.VALUE_PRESENT, MemoryModuleType.DOORS_TO_CLOSE, MemoryStatus.REGISTERED));
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, EntityLiving entity) {
        PathEntity path = entity.getBehaviorController().getMemory(MemoryModuleType.PATH).get();
        if (!path.notStarted() && !path.isDone()) {
            if (!Objects.equals(this.lastCheckedNode, path.getNextNode())) {
                this.remainingCooldown = 20;
                return true;
            } else {
                if (this.remainingCooldown > 0) {
                    --this.remainingCooldown;
                }

                return this.remainingCooldown == 0;
            }
        } else {
            return false;
        }
    }

    @Override
    protected void start(WorldServer world, EntityLiving entity, long time) {
        PathEntity path = entity.getBehaviorController().getMemory(MemoryModuleType.PATH).get();
        this.lastCheckedNode = path.getNextNode();
        PathPoint node = path.getPreviousNode();
        PathPoint node2 = path.getNextNode();
        BlockPosition blockPos = node.asBlockPos();
        IBlockData blockState = world.getType(blockPos);
        if (blockState.is(TagsBlock.WOODEN_DOORS)) {
            BlockDoor doorBlock = (BlockDoor)blockState.getBlock();
            if (!doorBlock.isOpen(blockState)) {
                doorBlock.setDoor(entity, world, blockState, blockPos, true);
            }

            this.rememberDoorToClose(world, entity, blockPos);
        }

        BlockPosition blockPos2 = node2.asBlockPos();
        IBlockData blockState2 = world.getType(blockPos2);
        if (blockState2.is(TagsBlock.WOODEN_DOORS)) {
            BlockDoor doorBlock2 = (BlockDoor)blockState2.getBlock();
            if (!doorBlock2.isOpen(blockState2)) {
                doorBlock2.setDoor(entity, world, blockState2, blockPos2, true);
                this.rememberDoorToClose(world, entity, blockPos2);
            }
        }

        closeDoorsThatIHaveOpenedOrPassedThrough(world, entity, node, node2);
    }

    public static void closeDoorsThatIHaveOpenedOrPassedThrough(WorldServer world, EntityLiving entity, @Nullable PathPoint lastNode, @Nullable PathPoint currentNode) {
        BehaviorController<?> brain = entity.getBehaviorController();
        if (brain.hasMemory(MemoryModuleType.DOORS_TO_CLOSE)) {
            Iterator<GlobalPos> iterator = brain.getMemory(MemoryModuleType.DOORS_TO_CLOSE).get().iterator();

            while(iterator.hasNext()) {
                GlobalPos globalPos = iterator.next();
                BlockPosition blockPos = globalPos.getBlockPosition();
                if ((lastNode == null || !lastNode.asBlockPos().equals(blockPos)) && (currentNode == null || !currentNode.asBlockPos().equals(blockPos))) {
                    if (isDoorTooFarAway(world, entity, globalPos)) {
                        iterator.remove();
                    } else {
                        IBlockData blockState = world.getType(blockPos);
                        if (!blockState.is(TagsBlock.WOODEN_DOORS)) {
                            iterator.remove();
                        } else {
                            BlockDoor doorBlock = (BlockDoor)blockState.getBlock();
                            if (!doorBlock.isOpen(blockState)) {
                                iterator.remove();
                            } else if (areOtherMobsComingThroughDoor(world, entity, blockPos)) {
                                iterator.remove();
                            } else {
                                doorBlock.setDoor(entity, world, blockState, blockPos, false);
                                iterator.remove();
                            }
                        }
                    }
                }
            }
        }

    }

    private static boolean areOtherMobsComingThroughDoor(WorldServer world, EntityLiving entity, BlockPosition pos) {
        BehaviorController<?> brain = entity.getBehaviorController();
        return !brain.hasMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES) ? false : brain.getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES).get().stream().filter((livingEntity2) -> {
            return livingEntity2.getEntityType() == entity.getEntityType();
        }).filter((livingEntity) -> {
            return pos.closerThan(livingEntity.getPositionVector(), 2.0D);
        }).anyMatch((livingEntity) -> {
            return isMobComingThroughDoor(world, livingEntity, pos);
        });
    }

    private static boolean isMobComingThroughDoor(WorldServer world, EntityLiving entity, BlockPosition pos) {
        if (!entity.getBehaviorController().hasMemory(MemoryModuleType.PATH)) {
            return false;
        } else {
            PathEntity path = entity.getBehaviorController().getMemory(MemoryModuleType.PATH).get();
            if (path.isDone()) {
                return false;
            } else {
                PathPoint node = path.getPreviousNode();
                if (node == null) {
                    return false;
                } else {
                    PathPoint node2 = path.getNextNode();
                    return pos.equals(node.asBlockPos()) || pos.equals(node2.asBlockPos());
                }
            }
        }
    }

    private static boolean isDoorTooFarAway(WorldServer world, EntityLiving entity, GlobalPos doorPos) {
        return doorPos.getDimensionManager() != world.getDimensionKey() || !doorPos.getBlockPosition().closerThan(entity.getPositionVector(), 2.0D);
    }

    private void rememberDoorToClose(WorldServer world, EntityLiving entity, BlockPosition pos) {
        BehaviorController<?> brain = entity.getBehaviorController();
        GlobalPos globalPos = GlobalPos.create(world.getDimensionKey(), pos);
        if (brain.getMemory(MemoryModuleType.DOORS_TO_CLOSE).isPresent()) {
            brain.getMemory(MemoryModuleType.DOORS_TO_CLOSE).get().add(globalPos);
        } else {
            brain.setMemory(MemoryModuleType.DOORS_TO_CLOSE, Sets.newHashSet(globalPos));
        }

    }
}
