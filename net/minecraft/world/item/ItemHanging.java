package net.minecraft.world.item;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.EntityHanging;
import net.minecraft.world.entity.decoration.EntityItemFrame;
import net.minecraft.world.entity.decoration.EntityPainting;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.context.ItemActionContext;
import net.minecraft.world.level.World;
import net.minecraft.world.level.gameevent.GameEvent;

public class ItemHanging extends Item {
    private final EntityTypes<? extends EntityHanging> type;

    public ItemHanging(EntityTypes<? extends EntityHanging> type, Item.Info settings) {
        super(settings);
        this.type = type;
    }

    @Override
    public EnumInteractionResult useOn(ItemActionContext context) {
        BlockPosition blockPos = context.getClickPosition();
        EnumDirection direction = context.getClickedFace();
        BlockPosition blockPos2 = blockPos.relative(direction);
        EntityHuman player = context.getEntity();
        ItemStack itemStack = context.getItemStack();
        if (player != null && !this.mayPlace(player, direction, itemStack, blockPos2)) {
            return EnumInteractionResult.FAIL;
        } else {
            World level = context.getWorld();
            EntityHanging hangingEntity;
            if (this.type == EntityTypes.PAINTING) {
                hangingEntity = new EntityPainting(level, blockPos2, direction);
            } else if (this.type == EntityTypes.ITEM_FRAME) {
                hangingEntity = new EntityItemFrame(level, blockPos2, direction);
            } else {
                if (this.type != EntityTypes.GLOW_ITEM_FRAME) {
                    return EnumInteractionResult.sidedSuccess(level.isClientSide);
                }

                hangingEntity = new GlowItemFrame(level, blockPos2, direction);
            }

            NBTTagCompound compoundTag = itemStack.getTag();
            if (compoundTag != null) {
                EntityTypes.updateCustomEntityTag(level, player, hangingEntity, compoundTag);
            }

            if (hangingEntity.survives()) {
                if (!level.isClientSide) {
                    hangingEntity.playPlaceSound();
                    level.gameEvent(player, GameEvent.ENTITY_PLACE, blockPos);
                    level.addEntity(hangingEntity);
                }

                itemStack.subtract(1);
                return EnumInteractionResult.sidedSuccess(level.isClientSide);
            } else {
                return EnumInteractionResult.CONSUME;
            }
        }
    }

    protected boolean mayPlace(EntityHuman player, EnumDirection side, ItemStack stack, BlockPosition pos) {
        return !side.getAxis().isVertical() && player.mayUseItemAt(pos, side, stack);
    }
}
