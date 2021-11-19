package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.InventorySubcontainer;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.MemoryTarget;
import net.minecraft.world.entity.npc.EntityVillager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockCrops;
import net.minecraft.world.level.block.BlockSoil;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;

public class BehaviorFarm extends Behavior<EntityVillager> {
    private static final int HARVEST_DURATION = 200;
    public static final float SPEED_MODIFIER = 0.5F;
    @Nullable
    private BlockPosition aboveFarmlandPos;
    private long nextOkStartTime;
    private int timeWorkedSoFar;
    private final List<BlockPosition> validFarmlandAroundVillager = Lists.newArrayList();

    public BehaviorFarm() {
        super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.SECONDARY_JOB_SITE, MemoryStatus.VALUE_PRESENT));
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, EntityVillager entity) {
        if (!world.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return false;
        } else if (entity.getVillagerData().getProfession() != VillagerProfession.FARMER) {
            return false;
        } else {
            BlockPosition.MutableBlockPosition mutableBlockPos = entity.getChunkCoordinates().mutable();
            this.validFarmlandAroundVillager.clear();

            for(int i = -1; i <= 1; ++i) {
                for(int j = -1; j <= 1; ++j) {
                    for(int k = -1; k <= 1; ++k) {
                        mutableBlockPos.set(entity.locX() + (double)i, entity.locY() + (double)j, entity.locZ() + (double)k);
                        if (this.validPos(mutableBlockPos, world)) {
                            this.validFarmlandAroundVillager.add(new BlockPosition(mutableBlockPos));
                        }
                    }
                }
            }

            this.aboveFarmlandPos = this.getValidFarmland(world);
            return this.aboveFarmlandPos != null;
        }
    }

    @Nullable
    private BlockPosition getValidFarmland(WorldServer world) {
        return this.validFarmlandAroundVillager.isEmpty() ? null : this.validFarmlandAroundVillager.get(world.getRandom().nextInt(this.validFarmlandAroundVillager.size()));
    }

    private boolean validPos(BlockPosition pos, WorldServer world) {
        IBlockData blockState = world.getType(pos);
        Block block = blockState.getBlock();
        Block block2 = world.getType(pos.below()).getBlock();
        return block instanceof BlockCrops && ((BlockCrops)block).isRipe(blockState) || blockState.isAir() && block2 instanceof BlockSoil;
    }

    @Override
    protected void start(WorldServer world, EntityVillager entity, long time) {
        if (time > this.nextOkStartTime && this.aboveFarmlandPos != null) {
            entity.getBehaviorController().setMemory(MemoryModuleType.LOOK_TARGET, new BehaviorTarget(this.aboveFarmlandPos));
            entity.getBehaviorController().setMemory(MemoryModuleType.WALK_TARGET, new MemoryTarget(new BehaviorTarget(this.aboveFarmlandPos), 0.5F, 1));
        }

    }

    @Override
    protected void stop(WorldServer serverLevel, EntityVillager villager, long l) {
        villager.getBehaviorController().removeMemory(MemoryModuleType.LOOK_TARGET);
        villager.getBehaviorController().removeMemory(MemoryModuleType.WALK_TARGET);
        this.timeWorkedSoFar = 0;
        this.nextOkStartTime = l + 40L;
    }

    @Override
    protected void tick(WorldServer serverLevel, EntityVillager villager, long l) {
        if (this.aboveFarmlandPos == null || this.aboveFarmlandPos.closerThan(villager.getPositionVector(), 1.0D)) {
            if (this.aboveFarmlandPos != null && l > this.nextOkStartTime) {
                IBlockData blockState = serverLevel.getType(this.aboveFarmlandPos);
                Block block = blockState.getBlock();
                Block block2 = serverLevel.getType(this.aboveFarmlandPos.below()).getBlock();
                if (block instanceof BlockCrops && ((BlockCrops)block).isRipe(blockState)) {
                    serverLevel.destroyBlock(this.aboveFarmlandPos, true, villager);
                }

                if (blockState.isAir() && block2 instanceof BlockSoil && villager.canPlant()) {
                    InventorySubcontainer simpleContainer = villager.getInventory();

                    for(int i = 0; i < simpleContainer.getSize(); ++i) {
                        ItemStack itemStack = simpleContainer.getItem(i);
                        boolean bl = false;
                        if (!itemStack.isEmpty()) {
                            if (itemStack.is(Items.WHEAT_SEEDS)) {
                                serverLevel.setTypeAndData(this.aboveFarmlandPos, Blocks.WHEAT.getBlockData(), 3);
                                bl = true;
                            } else if (itemStack.is(Items.POTATO)) {
                                serverLevel.setTypeAndData(this.aboveFarmlandPos, Blocks.POTATOES.getBlockData(), 3);
                                bl = true;
                            } else if (itemStack.is(Items.CARROT)) {
                                serverLevel.setTypeAndData(this.aboveFarmlandPos, Blocks.CARROTS.getBlockData(), 3);
                                bl = true;
                            } else if (itemStack.is(Items.BEETROOT_SEEDS)) {
                                serverLevel.setTypeAndData(this.aboveFarmlandPos, Blocks.BEETROOTS.getBlockData(), 3);
                                bl = true;
                            }
                        }

                        if (bl) {
                            serverLevel.playSound((EntityHuman)null, (double)this.aboveFarmlandPos.getX(), (double)this.aboveFarmlandPos.getY(), (double)this.aboveFarmlandPos.getZ(), SoundEffects.CROP_PLANTED, EnumSoundCategory.BLOCKS, 1.0F, 1.0F);
                            itemStack.subtract(1);
                            if (itemStack.isEmpty()) {
                                simpleContainer.setItem(i, ItemStack.EMPTY);
                            }
                            break;
                        }
                    }
                }

                if (block instanceof BlockCrops && !((BlockCrops)block).isRipe(blockState)) {
                    this.validFarmlandAroundVillager.remove(this.aboveFarmlandPos);
                    this.aboveFarmlandPos = this.getValidFarmland(serverLevel);
                    if (this.aboveFarmlandPos != null) {
                        this.nextOkStartTime = l + 20L;
                        villager.getBehaviorController().setMemory(MemoryModuleType.WALK_TARGET, new MemoryTarget(new BehaviorTarget(this.aboveFarmlandPos), 0.5F, 1));
                        villager.getBehaviorController().setMemory(MemoryModuleType.LOOK_TARGET, new BehaviorTarget(this.aboveFarmlandPos));
                    }
                }
            }

            ++this.timeWorkedSoFar;
        }
    }

    @Override
    protected boolean canStillUse(WorldServer serverLevel, EntityVillager villager, long l) {
        return this.timeWorkedSoFar < 200;
    }
}
