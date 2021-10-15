package net.minecraft.core.dispenser;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.ISourceBlock;
import net.minecraft.tags.TagsFluid;
import net.minecraft.world.entity.vehicle.EntityBoat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.BlockDispenser;

public class DispenseBehaviorBoat extends DispenseBehaviorItem {
    private final DispenseBehaviorItem defaultDispenseItemBehavior = new DispenseBehaviorItem();
    private final EntityBoat.EnumBoatType type;

    public DispenseBehaviorBoat(EntityBoat.EnumBoatType type) {
        this.type = type;
    }

    @Override
    public ItemStack execute(ISourceBlock pointer, ItemStack stack) {
        EnumDirection direction = pointer.getBlockData().get(BlockDispenser.FACING);
        World level = pointer.getWorld();
        double d = pointer.getX() + (double)((float)direction.getAdjacentX() * 1.125F);
        double e = pointer.getY() + (double)((float)direction.getAdjacentY() * 1.125F);
        double f = pointer.getZ() + (double)((float)direction.getAdjacentZ() * 1.125F);
        BlockPosition blockPos = pointer.getBlockPosition().relative(direction);
        double g;
        if (level.getFluid(blockPos).is(TagsFluid.WATER)) {
            g = 1.0D;
        } else {
            if (!level.getType(blockPos).isAir() || !level.getFluid(blockPos.below()).is(TagsFluid.WATER)) {
                return this.defaultDispenseItemBehavior.dispense(pointer, stack);
            }

            g = 0.0D;
        }

        EntityBoat boat = new EntityBoat(level, d, e + g, f);
        boat.setType(this.type);
        boat.setYRot(direction.toYRot());
        level.addEntity(boat);
        stack.subtract(1);
        return stack;
    }

    @Override
    protected void playSound(ISourceBlock pointer) {
        pointer.getWorld().triggerEffect(1000, pointer.getBlockPosition(), 0);
    }
}
