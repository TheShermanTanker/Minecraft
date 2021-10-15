package net.minecraft.world.item;

import net.minecraft.stats.StatisticList;
import net.minecraft.world.EnumHand;
import net.minecraft.world.InteractionResultWrapper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ISteerable;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.World;

public class ItemCarrotStick<T extends Entity & ISteerable> extends Item {
    private final EntityTypes<T> canInteractWith;
    private final int consumeItemDamage;

    public ItemCarrotStick(Item.Info settings, EntityTypes<T> target, int damagePerUse) {
        super(settings);
        this.canInteractWith = target;
        this.consumeItemDamage = damagePerUse;
    }

    @Override
    public InteractionResultWrapper<ItemStack> use(World world, EntityHuman user, EnumHand hand) {
        ItemStack itemStack = user.getItemInHand(hand);
        if (world.isClientSide) {
            return InteractionResultWrapper.pass(itemStack);
        } else {
            Entity entity = user.getVehicle();
            if (user.isPassenger() && entity instanceof ISteerable && entity.getEntityType() == this.canInteractWith) {
                ISteerable itemSteerable = (ISteerable)entity;
                if (itemSteerable.boost()) {
                    itemStack.damage(this.consumeItemDamage, user, (p) -> {
                        p.broadcastItemBreak(hand);
                    });
                    if (itemStack.isEmpty()) {
                        ItemStack itemStack2 = new ItemStack(Items.FISHING_ROD);
                        itemStack2.setTag(itemStack.getTag());
                        return InteractionResultWrapper.success(itemStack2);
                    }

                    return InteractionResultWrapper.success(itemStack);
                }
            }

            user.awardStat(StatisticList.ITEM_USED.get(this));
            return InteractionResultWrapper.pass(itemStack);
        }
    }
}
