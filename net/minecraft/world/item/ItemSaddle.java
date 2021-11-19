package net.minecraft.world.item;

import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ISaddleable;
import net.minecraft.world.entity.player.EntityHuman;

public class ItemSaddle extends Item {
    public ItemSaddle(Item.Info settings) {
        super(settings);
    }

    @Override
    public EnumInteractionResult interactLivingEntity(ItemStack stack, EntityHuman user, EntityLiving entity, EnumHand hand) {
        if (entity instanceof ISaddleable && entity.isAlive()) {
            ISaddleable saddleable = (ISaddleable)entity;
            if (!saddleable.hasSaddle() && saddleable.canSaddle()) {
                if (!user.level.isClientSide) {
                    saddleable.saddle(EnumSoundCategory.NEUTRAL);
                    stack.subtract(1);
                }

                return EnumInteractionResult.sidedSuccess(user.level.isClientSide);
            }
        }

        return EnumInteractionResult.PASS;
    }
}
