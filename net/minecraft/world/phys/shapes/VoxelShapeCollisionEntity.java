package net.minecraft.world.phys.shapes;

import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidType;
import net.minecraft.world.level.material.FluidTypeFlowing;

public class VoxelShapeCollisionEntity implements VoxelShapeCollision {
    protected static final VoxelShapeCollision EMPTY = new VoxelShapeCollisionEntity(false, -Double.MAX_VALUE, ItemStack.EMPTY, ItemStack.EMPTY, (fluid) -> {
        return false;
    }, Optional.empty()) {
        @Override
        public boolean isAbove(VoxelShape shape, BlockPosition pos, boolean defaultValue) {
            return defaultValue;
        }
    };
    private final boolean descending;
    private final double entityBottom;
    private final ItemStack heldItem;
    private final ItemStack footItem;
    private final Predicate<FluidType> canStandOnFluid;
    private final Optional<Entity> entity;

    protected VoxelShapeCollisionEntity(boolean descending, double minY, ItemStack boots, ItemStack heldItem, Predicate<FluidType> walkOnFluidPredicate, Optional<Entity> entity) {
        this.descending = descending;
        this.entityBottom = minY;
        this.footItem = boots;
        this.heldItem = heldItem;
        this.canStandOnFluid = walkOnFluidPredicate;
        this.entity = entity;
    }

    @Deprecated
    protected VoxelShapeCollisionEntity(Entity entity) {
        this(entity.isDescending(), entity.locY(), entity instanceof EntityLiving ? ((EntityLiving)entity).getEquipment(EnumItemSlot.FEET) : ItemStack.EMPTY, entity instanceof EntityLiving ? ((EntityLiving)entity).getItemInMainHand() : ItemStack.EMPTY, entity instanceof EntityLiving ? ((EntityLiving)entity)::canStandOnFluid : (fluid) -> {
            return false;
        }, Optional.of(entity));
    }

    @Override
    public boolean hasItemOnFeet(Item item) {
        return this.footItem.is(item);
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

    public Optional<Entity> getEntity() {
        return this.entity;
    }
}
