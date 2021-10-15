package net.minecraft.world.item;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.stats.StatisticList;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.InteractionResultWrapper;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.context.ItemActionContext;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.BlockLectern;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;

public class ItemBookAndQuill extends Item {
    public ItemBookAndQuill(Item.Info settings) {
        super(settings);
    }

    @Override
    public EnumInteractionResult useOn(ItemActionContext context) {
        World level = context.getWorld();
        BlockPosition blockPos = context.getClickPosition();
        IBlockData blockState = level.getType(blockPos);
        if (blockState.is(Blocks.LECTERN)) {
            return BlockLectern.tryPlaceBook(context.getEntity(), level, blockPos, blockState, context.getItemStack()) ? EnumInteractionResult.sidedSuccess(level.isClientSide) : EnumInteractionResult.PASS;
        } else {
            return EnumInteractionResult.PASS;
        }
    }

    @Override
    public InteractionResultWrapper<ItemStack> use(World world, EntityHuman user, EnumHand hand) {
        ItemStack itemStack = user.getItemInHand(hand);
        user.openBook(itemStack, hand);
        user.awardStat(StatisticList.ITEM_USED.get(this));
        return InteractionResultWrapper.sidedSuccess(itemStack, world.isClientSide());
    }

    public static boolean makeSureTagIsValid(@Nullable NBTTagCompound nbt) {
        if (nbt == null) {
            return false;
        } else if (!nbt.hasKeyOfType("pages", 9)) {
            return false;
        } else {
            NBTTagList listTag = nbt.getList("pages", 8);

            for(int i = 0; i < listTag.size(); ++i) {
                String string = listTag.getString(i);
                if (string.length() > 32767) {
                    return false;
                }
            }

            return true;
        }
    }
}
