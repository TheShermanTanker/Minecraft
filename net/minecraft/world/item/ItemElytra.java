package net.minecraft.world.item;

import javax.annotation.Nullable;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.stats.StatisticList;
import net.minecraft.world.EnumHand;
import net.minecraft.world.InteractionResultWrapper;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.BlockDispenser;

public class ItemElytra extends Item implements ItemWearable {
    public ItemElytra(Item.Info settings) {
        super(settings);
        BlockDispenser.registerBehavior(this, ItemArmor.DISPENSE_ITEM_BEHAVIOR);
    }

    public static boolean isFlyEnabled(ItemStack stack) {
        return stack.getDamage() < stack.getMaxDamage() - 1;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack ingredient) {
        return ingredient.is(Items.PHANTOM_MEMBRANE);
    }

    @Override
    public InteractionResultWrapper<ItemStack> use(World world, EntityHuman user, EnumHand hand) {
        ItemStack itemStack = user.getItemInHand(hand);
        EnumItemSlot equipmentSlot = EntityInsentient.getEquipmentSlotForItem(itemStack);
        ItemStack itemStack2 = user.getEquipment(equipmentSlot);
        if (itemStack2.isEmpty()) {
            user.setSlot(equipmentSlot, itemStack.cloneItemStack());
            if (!world.isClientSide()) {
                user.awardStat(StatisticList.ITEM_USED.get(this));
            }

            itemStack.setCount(0);
            return InteractionResultWrapper.sidedSuccess(itemStack, world.isClientSide());
        } else {
            return InteractionResultWrapper.fail(itemStack);
        }
    }

    @Nullable
    @Override
    public SoundEffect getEquipSound() {
        return SoundEffects.ARMOR_EQUIP_ELYTRA;
    }
}
