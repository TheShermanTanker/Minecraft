package net.minecraft.core.dispenser;

import net.minecraft.core.EnumDirection;
import net.minecraft.core.IPosition;
import net.minecraft.core.ISourceBlock;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.BlockDispenser;

public class DispenseBehaviorItem implements IDispenseBehavior {
    @Override
    public final ItemStack dispense(ISourceBlock pointer, ItemStack stack) {
        ItemStack itemStack = this.a(pointer, stack);
        this.a(pointer);
        this.playAnimation(pointer, pointer.getBlockData().get(BlockDispenser.FACING));
        return itemStack;
    }

    protected ItemStack a(ISourceBlock pointer, ItemStack stack) {
        EnumDirection direction = pointer.getBlockData().get(BlockDispenser.FACING);
        IPosition position = BlockDispenser.getDispensePosition(pointer);
        ItemStack itemStack = stack.cloneAndSubtract(1);
        spawnItem(pointer.getWorld(), itemStack, 6, direction, position);
        return stack;
    }

    public static void spawnItem(World world, ItemStack stack, int offset, EnumDirection side, IPosition pos) {
        double d = pos.getX();
        double e = pos.getY();
        double f = pos.getZ();
        if (side.getAxis() == EnumDirection.EnumAxis.Y) {
            e = e - 0.125D;
        } else {
            e = e - 0.15625D;
        }

        EntityItem itemEntity = new EntityItem(world, d, e, f, stack);
        double g = world.random.nextDouble() * 0.1D + 0.2D;
        itemEntity.setMot(world.random.nextGaussian() * (double)0.0075F * (double)offset + (double)side.getAdjacentX() * g, world.random.nextGaussian() * (double)0.0075F * (double)offset + (double)0.2F, world.random.nextGaussian() * (double)0.0075F * (double)offset + (double)side.getAdjacentZ() * g);
        world.addEntity(itemEntity);
    }

    protected void a(ISourceBlock pointer) {
        pointer.getWorld().triggerEffect(1000, pointer.getBlockPosition(), 0);
    }

    protected void playAnimation(ISourceBlock pointer, EnumDirection side) {
        pointer.getWorld().triggerEffect(2000, pointer.getBlockPosition(), side.get3DDataValue());
    }
}
