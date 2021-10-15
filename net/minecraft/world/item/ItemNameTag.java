package net.minecraft.world.item;

import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.player.EntityHuman;

public class ItemNameTag extends Item {
    public ItemNameTag(Item.Info settings) {
        super(settings);
    }

    @Override
    public EnumInteractionResult interactLivingEntity(ItemStack stack, EntityHuman user, EntityLiving entity, EnumHand hand) {
        if (stack.hasName() && !(entity instanceof EntityHuman)) {
            if (!user.level.isClientSide && entity.isAlive()) {
                entity.setCustomName(stack.getName());
                if (entity instanceof EntityInsentient) {
                    ((EntityInsentient)entity).setPersistent();
                }

                stack.subtract(1);
            }

            return EnumInteractionResult.sidedSuccess(user.level.isClientSide);
        } else {
            return EnumInteractionResult.PASS;
        }
    }
}
