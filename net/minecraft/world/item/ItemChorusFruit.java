package net.minecraft.world.item;

import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.animal.EntityFox;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.World;

public class ItemChorusFruit extends Item {
    public ItemChorusFruit(Item.Info settings) {
        super(settings);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, World world, EntityLiving user) {
        ItemStack itemStack = super.finishUsingItem(stack, world, user);
        if (!world.isClientSide) {
            double d = user.locX();
            double e = user.locY();
            double f = user.locZ();

            for(int i = 0; i < 16; ++i) {
                double g = user.locX() + (user.getRandom().nextDouble() - 0.5D) * 16.0D;
                double h = MathHelper.clamp(user.locY() + (double)(user.getRandom().nextInt(16) - 8), (double)world.getMinBuildHeight(), (double)(world.getMinBuildHeight() + ((WorldServer)world).getLogicalHeight() - 1));
                double j = user.locZ() + (user.getRandom().nextDouble() - 0.5D) * 16.0D;
                if (user.isPassenger()) {
                    user.stopRiding();
                }

                if (user.randomTeleport(g, h, j, true)) {
                    SoundEffect soundEvent = user instanceof EntityFox ? SoundEffects.FOX_TELEPORT : SoundEffects.CHORUS_FRUIT_TELEPORT;
                    world.playSound((EntityHuman)null, d, e, f, soundEvent, SoundCategory.PLAYERS, 1.0F, 1.0F);
                    user.playSound(soundEvent, 1.0F, 1.0F);
                    break;
                }
            }

            if (user instanceof EntityHuman) {
                ((EntityHuman)user).getCooldownTracker().setCooldown(this, 20);
            }
        }

        return itemStack;
    }
}
