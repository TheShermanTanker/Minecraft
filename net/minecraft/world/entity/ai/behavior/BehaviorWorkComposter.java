package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.InventorySubcontainer;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.EntityVillager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BlockComposter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;

public class BehaviorWorkComposter extends BehaviorWork {
    private static final List<Item> COMPOSTABLE_ITEMS = ImmutableList.of(Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS);

    @Override
    protected void doWork(WorldServer world, EntityVillager entity) {
        Optional<GlobalPos> optional = entity.getBehaviorController().getMemory(MemoryModuleType.JOB_SITE);
        if (optional.isPresent()) {
            GlobalPos globalPos = optional.get();
            IBlockData blockState = world.getType(globalPos.getBlockPosition());
            if (blockState.is(Blocks.COMPOSTER)) {
                this.makeBread(entity);
                this.compostItems(world, entity, globalPos, blockState);
            }

        }
    }

    private void compostItems(WorldServer world, EntityVillager entity, GlobalPos pos, IBlockData composterState) {
        BlockPosition blockPos = pos.getBlockPosition();
        if (composterState.get(BlockComposter.LEVEL) == 8) {
            composterState = BlockComposter.extractProduce(composterState, world, blockPos);
        }

        int i = 20;
        int j = 10;
        int[] is = new int[COMPOSTABLE_ITEMS.size()];
        InventorySubcontainer simpleContainer = entity.getInventory();
        int k = simpleContainer.getSize();
        IBlockData blockState = composterState;

        for(int l = k - 1; l >= 0 && i > 0; --l) {
            ItemStack itemStack = simpleContainer.getItem(l);
            int m = COMPOSTABLE_ITEMS.indexOf(itemStack.getItem());
            if (m != -1) {
                int n = itemStack.getCount();
                int o = is[m] + n;
                is[m] = o;
                int p = Math.min(Math.min(o - 10, i), n);
                if (p > 0) {
                    i -= p;

                    for(int q = 0; q < p; ++q) {
                        blockState = BlockComposter.insertItem(blockState, world, itemStack, blockPos);
                        if (blockState.get(BlockComposter.LEVEL) == 7) {
                            this.spawnComposterFillEffects(world, composterState, blockPos, blockState);
                            return;
                        }
                    }
                }
            }
        }

        this.spawnComposterFillEffects(world, composterState, blockPos, blockState);
    }

    private void spawnComposterFillEffects(WorldServer world, IBlockData oldState, BlockPosition pos, IBlockData newState) {
        world.triggerEffect(1500, pos, newState != oldState ? 1 : 0);
    }

    private void makeBread(EntityVillager entity) {
        InventorySubcontainer simpleContainer = entity.getInventory();
        if (simpleContainer.countItem(Items.BREAD) <= 36) {
            int i = simpleContainer.countItem(Items.WHEAT);
            int j = 3;
            int k = 3;
            int l = Math.min(3, i / 3);
            if (l != 0) {
                int m = l * 3;
                simpleContainer.removeItemType(Items.WHEAT, m);
                ItemStack itemStack = simpleContainer.addItem(new ItemStack(Items.BREAD, l));
                if (!itemStack.isEmpty()) {
                    entity.spawnAtLocation(itemStack, 0.5F);
                }

            }
        }
    }
}
