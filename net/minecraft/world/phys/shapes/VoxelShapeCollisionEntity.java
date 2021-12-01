package net.minecraft.world.phys.shapes;

import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidType;
import net.minecraft.world.level.material.FluidTypeFlowing;

public class VoxelShapeCollisionEntity implements VoxelShapeCollision {
    protected static final VoxelShapeCollision EMPTY = new VoxelShapeCollisionEntity(false, -Double.MAX_VALUE, ItemStack.EMPTY, (fluid) -> {
        return false;
    }, (Entity)null) {
        @Override
        public boolean isAbove(VoxelShape shape, BlockPosition pos, boolean defaultValue) {
            return defaultValue;
        }
    };
    private final boolean descending;
    private final double entityBottom;
    private final ItemStack heldItem;
    private final Predicate<FluidType> canStandOnFluid;
    @Nullable
    private final Entity entity;

    protected VoxelShapeCollisionEntity(boolean descending, double minY, ItemStack heldItem, Predicate<FluidType> walkOnFluidPrecicate, @Nullable Entity entity) {
        this.descending = descending;
        this.entityBottom = minY;
        this.heldItem = heldItem;
        this.canStandOnFluid = walkOnFluidPrecicate;
        this.entity = entity;
    }

    /** @deprecated */
    @Deprecated
    protected VoxelShapeCollisionEntity(Entity entity) {
        this(entity.isDescending(), entity.locY(), entity instanceof EntityLiving ? ((EntityLiving)entity).getItemInMainHand() : ItemStack.EMPTY, entity instanceof EntityLiving ? ((EntityLiving)entity)::canStandOnFluid : (fluid) -> {
            return false;
        }, entity);
    }

    @Override
    public boolean isHoldingItem(Item item) {
        return this.heldItem.is(item);
    }

    @Override
    public boolean canStandOnFluid(Fluid state, FluidTypeFlowing fluid) {
        return this.canStandOnFluid.test(fluid) && !state.getType().isSame(fluid);
    }

    @Override
    public boolean isDescending() {
        return this.descending;
    }

    @Override
    public boolean isAbove(VoxelShape shape, BlockPosition pos, boolean defaultValue) {
        return this.entityBottom > (double)pos.getY() + shape.max(EnumDirection.EnumAxis.Y) - (double)1.0E-5F;
    }

    @Nullable
    public Entity getEntity() {
        return this.entity;
    }
}
