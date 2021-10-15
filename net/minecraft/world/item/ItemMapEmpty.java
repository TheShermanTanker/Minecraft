package net.minecraft.world.item;

import net.minecraft.sounds.SoundEffects;
import net.minecraft.stats.StatisticList;
import net.minecraft.world.EnumHand;
import net.minecraft.world.InteractionResultWrapper;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.World;

public class ItemMapEmpty extends ItemWorldMapBase {
    public ItemMapEmpty(Item.Info settings) {
        super(settings);
    }

    @Override
    public InteractionResultWrapper<ItemStack> use(World world, EntityHuman user, EnumHand hand) {
        ItemStack itemStack = user.getItemInHand(hand);
        if (world.isClientSide) {
            return InteractionResultWrapper.success(itemStack);
        } else {
            if (!user.getAbilities().instabuild) {
                itemStack.subtract(1);
            }

            user.awardStat(StatisticList.ITEM_USED.get(this));
            user.level.playSound((EntityHuman)null, user, SoundEffects.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, user.getSoundCategory(), 1.0F, 1.0F);
            ItemStack itemStack2 = ItemWorldMap.createFilledMapView(world, user.getBlockX(), user.getBlockZ(), (byte)0, true, false);
            if (itemStack.isEmpty()) {
                return InteractionResultWrapper.consume(itemStack2);
            } else {
                if (!user.getInventory().pickup(itemStack2.cloneItemStack())) {
                    user.drop(itemStack2, false);
                }

                return InteractionResultWrapper.consume(itemStack);
            }
        }
    }
}
