package net.minecraft.core.dispenser;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.ISourceBlock;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemBlock;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockActionContextDirectional;
import net.minecraft.world.level.block.BlockDispenser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DispenseBehaviorShulkerBox extends DispenseBehaviorMaybe {
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    protected ItemStack a(ISourceBlock pointer, ItemStack stack) {
        this.setSuccess(false);
        Item item = stack.getItem();
        if (item instanceof ItemBlock) {
            EnumDirection direction = pointer.getBlockData().get(BlockDispenser.FACING);
            BlockPosition blockPos = pointer.getBlockPosition().relative(direction);
            EnumDirection direction2 = pointer.getWorld().isEmpty(blockPos.below()) ? direction : EnumDirection.UP;

            try {
                this.setSuccess(((ItemBlock)item).place(new BlockActionContextDirectional(pointer.getWorld(), blockPos, direction, stack, direction2)).consumesAction());
            } catch (Exception var8) {
                LOGGER.error("Error trying to place shulker box at {}", blockPos, var8);
            }
        }

        return stack;
    }
}
