package net.minecraft.world.level.block;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.SourceBlock;
import net.minecraft.core.dispenser.DispenseBehaviorItem;
import net.minecraft.core.dispenser.IDispenseBehavior;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.IInventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityDispenser;
import net.minecraft.world.level.block.entity.TileEntityDropper;
import net.minecraft.world.level.block.entity.TileEntityHopper;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;

public class BlockDropper extends BlockDispenser {
    private static final IDispenseBehavior DISPENSE_BEHAVIOUR = new DispenseBehaviorItem();

    public BlockDropper(BlockBase.Info settings) {
        super(settings);
    }

    @Override
    protected IDispenseBehavior getDispenseMethod(ItemStack stack) {
        return DISPENSE_BEHAVIOUR;
    }

    @Override
    public TileEntity createTile(BlockPosition pos, IBlockData state) {
        return new TileEntityDropper(pos, state);
    }

    @Override
    public void dispense(WorldServer world, BlockPosition pos) {
        SourceBlock blockSourceImpl = new SourceBlock(world, pos);
        TileEntityDispenser dispenserBlockEntity = blockSourceImpl.getTileEntity();
        int i = dispenserBlockEntity.getRandomSlot();
        if (i < 0) {
            world.triggerEffect(1001, pos, 0);
        } else {
            ItemStack itemStack = dispenserBlockEntity.getItem(i);
            if (!itemStack.isEmpty()) {
                EnumDirection direction = world.getType(pos).get(FACING);
                IInventory container = TileEntityHopper.getContainerAt(world, pos.relative(direction));
                ItemStack itemStack2;
                if (container == null) {
                    itemStack2 = DISPENSE_BEHAVIOUR.dispense(blockSourceImpl, itemStack);
                } else {
                    itemStack2 = TileEntityHopper.addItem(dispenserBlockEntity, container, itemStack.cloneItemStack().cloneAndSubtract(1), direction.opposite());
                    if (itemStack2.isEmpty()) {
                        itemStack2 = itemStack.cloneItemStack();
                        itemStack2.subtract(1);
                    } else {
                        itemStack2 = itemStack.cloneItemStack();
                    }
                }

                dispenserBlockEntity.setItem(i, itemStack2);
            }
        }
    }
}
