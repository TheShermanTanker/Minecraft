package net.minecraft.world.item;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.ISourceBlock;
import net.minecraft.core.dispenser.DispenseBehaviorItem;
import net.minecraft.core.dispenser.IDispenseBehavior;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.vehicle.EntityMinecartAbstract;
import net.minecraft.world.item.context.ItemActionContext;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.BlockDispenser;
import net.minecraft.world.level.block.BlockMinecartTrackAbstract;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockPropertyTrackPosition;
import net.minecraft.world.level.gameevent.GameEvent;

public class ItemMinecart extends Item {
    private static final IDispenseBehavior DISPENSE_ITEM_BEHAVIOR = new DispenseBehaviorItem() {
        private final DispenseBehaviorItem defaultDispenseItemBehavior = new DispenseBehaviorItem();

        @Override
        public ItemStack execute(ISourceBlock pointer, ItemStack stack) {
            EnumDirection direction = pointer.getBlockData().get(BlockDispenser.FACING);
            World level = pointer.getWorld();
            double d = pointer.getX() + (double)direction.getAdjacentX() * 1.125D;
            double e = Math.floor(pointer.getY()) + (double)direction.getAdjacentY();
            double f = pointer.getZ() + (double)direction.getAdjacentZ() * 1.125D;
            BlockPosition blockPos = pointer.getBlockPosition().relative(direction);
            IBlockData blockState = level.getType(blockPos);
            BlockPropertyTrackPosition railShape = blockState.getBlock() instanceof BlockMinecartTrackAbstract ? blockState.get(((BlockMinecartTrackAbstract)blockState.getBlock()).getShapeProperty()) : BlockPropertyTrackPosition.NORTH_SOUTH;
            double g;
            if (blockState.is(TagsBlock.RAILS)) {
                if (railShape.isAscending()) {
                    g = 0.6D;
                } else {
                    g = 0.1D;
                }
            } else {
                if (!blockState.isAir() || !level.getType(blockPos.below()).is(TagsBlock.RAILS)) {
                    return this.defaultDispenseItemBehavior.dispense(pointer, stack);
                }

                IBlockData blockState2 = level.getType(blockPos.below());
                BlockPropertyTrackPosition railShape2 = blockState2.getBlock() instanceof BlockMinecartTrackAbstract ? blockState2.get(((BlockMinecartTrackAbstract)blockState2.getBlock()).getShapeProperty()) : BlockPropertyTrackPosition.NORTH_SOUTH;
                if (direction != EnumDirection.DOWN && railShape2.isAscending()) {
                    g = -0.4D;
                } else {
                    g = -0.9D;
                }
            }

            EntityMinecartAbstract abstractMinecart = EntityMinecartAbstract.createMinecart(level, d, e + g, f, ((ItemMinecart)stack.getItem()).type);
            if (stack.hasName()) {
                abstractMinecart.setCustomName(stack.getName());
            }

            level.addEntity(abstractMinecart);
            stack.subtract(1);
            return stack;
        }

        @Override
        protected void playSound(ISourceBlock pointer) {
            pointer.getWorld().triggerEffect(1000, pointer.getBlockPosition(), 0);
        }
    };
    final EntityMinecartAbstract.EnumMinecartType type;

    public ItemMinecart(EntityMinecartAbstract.EnumMinecartType type, Item.Info settings) {
        super(settings);
        this.type = type;
        BlockDispenser.registerBehavior(this, DISPENSE_ITEM_BEHAVIOR);
    }

    @Override
    public EnumInteractionResult useOn(ItemActionContext context) {
        World level = context.getWorld();
        BlockPosition blockPos = context.getClickPosition();
        IBlockData blockState = level.getType(blockPos);
        if (!blockState.is(TagsBlock.RAILS)) {
            return EnumInteractionResult.FAIL;
        } else {
            ItemStack itemStack = context.getItemStack();
            if (!level.isClientSide) {
                BlockPropertyTrackPosition railShape = blockState.getBlock() instanceof BlockMinecartTrackAbstract ? blockState.get(((BlockMinecartTrackAbstract)blockState.getBlock()).getShapeProperty()) : BlockPropertyTrackPosition.NORTH_SOUTH;
                double d = 0.0D;
                if (railShape.isAscending()) {
                    d = 0.5D;
                }

                EntityMinecartAbstract abstractMinecart = EntityMinecartAbstract.createMinecart(level, (double)blockPos.getX() + 0.5D, (double)blockPos.getY() + 0.0625D + d, (double)blockPos.getZ() + 0.5D, this.type);
                if (itemStack.hasName()) {
                    abstractMinecart.setCustomName(itemStack.getName());
                }

                level.addEntity(abstractMinecart);
                level.gameEvent(context.getEntity(), GameEvent.ENTITY_PLACE, blockPos);
            }

            itemStack.subtract(1);
            return EnumInteractionResult.sidedSuccess(level.isClientSide);
        }
    }
}
