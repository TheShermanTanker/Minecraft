package net.minecraft.world.item;

import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.stats.StatisticList;
import net.minecraft.world.EnumHand;
import net.minecraft.world.InteractionResultWrapper;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.EntityFishingHook;
import net.minecraft.world.item.enchantment.EnchantmentManager;
import net.minecraft.world.level.World;
import net.minecraft.world.level.gameevent.GameEvent;

public class ItemFishingRod extends Item implements ItemVanishable {
    public ItemFishingRod(Item.Info settings) {
        super(settings);
    }

    @Override
    public InteractionResultWrapper<ItemStack> use(World world, EntityHuman user, EnumHand hand) {
        ItemStack itemStack = user.getItemInHand(hand);
        if (user.fishing != null) {
            if (!world.isClientSide) {
                int i = user.fishing.retrieve(itemStack);
                itemStack.damage(i, user, (p) -> {
                    p.broadcastItemBreak(hand);
                });
            }

            world.playSound((EntityHuman)null, user.locX(), user.locY(), user.locZ(), SoundEffects.FISHING_BOBBER_RETRIEVE, SoundCategory.NEUTRAL, 1.0F, 0.4F / (world.getRandom().nextFloat() * 0.4F + 0.8F));
            world.gameEvent(user, GameEvent.FISHING_ROD_REEL_IN, user);
        } else {
            world.playSound((EntityHuman)null, user.locX(), user.locY(), user.locZ(), SoundEffects.FISHING_BOBBER_THROW, SoundCategory.NEUTRAL, 0.5F, 0.4F / (world.getRandom().nextFloat() * 0.4F + 0.8F));
            if (!world.isClientSide) {
                int j = EnchantmentManager.getFishingSpeedBonus(itemStack);
                int k = EnchantmentManager.getFishingLuckBonus(itemStack);
                world.addEntity(new EntityFishingHook(user, world, k, j));
            }

            user.awardStat(StatisticList.ITEM_USED.get(this));
            world.gameEvent(user, GameEvent.FISHING_ROD_CAST, user);
        }

        return InteractionResultWrapper.sidedSuccess(itemStack, world.isClientSide());
    }

    @Override
    public int getEnchantmentValue() {
        return 1;
    }
}
