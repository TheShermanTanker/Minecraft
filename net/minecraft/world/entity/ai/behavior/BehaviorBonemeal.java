package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.InventorySubcontainer;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.MemoryTarget;
import net.minecraft.world.entity.npc.EntityVillager;
import net.minecraft.world.item.ItemBoneMeal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockCrops;
import net.minecraft.world.level.block.state.IBlockData;

public class BehaviorBonemeal extends Behavior<EntityVillager> {
    private static final int BONEMEALING_DURATION = 80;
    private long nextWorkCycleTime;
    private long lastBonemealingSession;
    private int timeWorkedSoFar;
    private Optional<BlockPosition> cropPos = Optional.empty();

    public BehaviorBonemeal() {
        super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT));
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, EntityVillager entity) {
        if (entity.tickCount % 10 == 0 && (this.lastBonemealingSession == 0L || this.lastBonemealingSession + 160L <= (long)entity.tickCount)) {
            if (entity.getInventory().countItem(Items.BONE_MEAL) <= 0) {
                return false;
            } else {
                this.cropPos = this.pickNextTarget(world, entity);
                return this.cropPos.isPresent();
            }
        } else {
            return false;
        }
    }

    @Override
    protected boolean canStillUse(WorldServer serverLevel, EntityVillager villager, long l) {
        return this.timeWorkedSoFar < 80 && this.cropPos.isPresent();
    }

    private Optional<BlockPosition> pickNextTarget(WorldServer world, EntityVillager entity) {
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();
        Optional<BlockPosition> optional = Optional.empty();
        int i = 0;

        for(int j = -1; j <= 1; ++j) {
            for(int k = -1; k <= 1; ++k) {
                for(int l = -1; l <= 1; ++l) {
                    mutableBlockPos.setWithOffset(entity.getChunkCoordinates(), j, k, l);
                    if (this.validPos(mutableBlockPos, world)) {
                        ++i;
                        if (world.random.nextInt(i) == 0) {
                            optional = Optional.of(mutableBlockPos.immutableCopy());
                        }
                    }
                }
            }
        }

        return optional;
    }

    private boolean validPos(BlockPosition pos, WorldServer world) {
        IBlockData blockState = world.getType(pos);
        Block block = blockState.getBlock();
        return block instanceof BlockCrops && !((BlockCrops)block).isRipe(blockState);
    }

    @Override
    protected void start(WorldServer serverLevel, EntityVillager villager, long l) {
        this.setCurrentCropAsTarget(villager);
        villager.setSlot(EnumItemSlot.MAINHAND, new ItemStack(Items.BONE_MEAL));
        this.nextWorkCycleTime = l;
        this.timeWorkedSoFar = 0;
    }

    private void setCurrentCropAsTarget(EntityVillager villager) {
        this.cropPos.ifPresent((blockPos) -> {
            BehaviorTarget blockPosTracker = new BehaviorTarget(blockPos);
            villager.getBehaviorController().setMemory(MemoryModuleType.LOOK_TARGET, blockPosTracker);
            villager.getBehaviorController().setMemory(MemoryModuleType.WALK_TARGET, new MemoryTarget(blockPosTracker, 0.5F, 1));
        });
    }

    @Override
    protected void stop(WorldServer world, EntityVillager entity, long time) {
        entity.setSlot(EnumItemSlot.MAINHAND, ItemStack.EMPTY);
        this.lastBonemealingSession = (long)entity.tickCount;
    }

    @Override
    protected void tick(WorldServer world, EntityVillager entity, long time) {
        BlockPosition blockPos = this.cropPos.get();
        if (time >= this.nextWorkCycleTime && blockPos.closerThan(entity.getPositionVector(), 1.0D)) {
            ItemStack itemStack = ItemStack.EMPTY;
            InventorySubcontainer simpleContainer = entity.getInventory();
            int i = simpleContainer.getSize();

            for(int j = 0; j < i; ++j) {
                ItemStack itemStack2 = simpleContainer.getItem(j);
                if (itemStack2.is(Items.BONE_MEAL)) {
                    itemStack = itemStack2;
                    break;
                }
            }

            if (!itemStack.isEmpty() && ItemBoneMeal.growCrop(itemStack, world, blockPos)) {
                world.triggerEffect(1505, blockPos, 0);
                this.cropPos = this.pickNextTarget(world, entity);
                this.setCurrentCropAsTarget(entity);
                this.nextWorkCycleTime = time + 40L;
            }

            ++this.timeWorkedSoFar;
        }
    }
}
