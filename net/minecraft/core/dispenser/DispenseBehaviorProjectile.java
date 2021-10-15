package net.minecraft.core.dispenser;

import net.minecraft.core.EnumDirection;
import net.minecraft.core.IPosition;
import net.minecraft.core.ISourceBlock;
import net.minecraft.world.entity.projectile.IProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.BlockDispenser;

public abstract class DispenseBehaviorProjectile extends DispenseBehaviorItem {
    @Override
    public ItemStack execute(ISourceBlock pointer, ItemStack stack) {
        World level = pointer.getWorld();
        IPosition position = BlockDispenser.getDispensePosition(pointer);
        EnumDirection direction = pointer.getBlockData().get(BlockDispenser.FACING);
        IProjectile projectile = this.getProjectile(level, position, stack);
        projectile.shoot((double)direction.getAdjacentX(), (double)((float)direction.getAdjacentY() + 0.1F), (double)direction.getAdjacentZ(), this.getPower(), this.getUncertainty());
        level.addEntity(projectile);
        stack.subtract(1);
        return stack;
    }

    @Override
    protected void playSound(ISourceBlock pointer) {
        pointer.getWorld().triggerEffect(1002, pointer.getBlockPosition(), 0);
    }

    protected abstract IProjectile getProjectile(World world, IPosition position, ItemStack stack);

    protected float getUncertainty() {
        return 6.0F;
    }

    protected float getPower() {
        return 1.1F;
    }
}
