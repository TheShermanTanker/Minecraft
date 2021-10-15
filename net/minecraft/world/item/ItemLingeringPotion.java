package net.minecraft.world.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.EnumHand;
import net.minecraft.world.InteractionResultWrapper;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.alchemy.PotionUtil;
import net.minecraft.world.level.World;

public class ItemLingeringPotion extends ItemPotionThrowable {
    public ItemLingeringPotion(Item.Info settings) {
        super(settings);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World world, List<IChatBaseComponent> tooltip, TooltipFlag context) {
        PotionUtil.addPotionTooltip(stack, tooltip, 0.25F);
    }

    @Override
    public InteractionResultWrapper<ItemStack> use(World world, EntityHuman user, EnumHand hand) {
        world.playSound((EntityHuman)null, user.locX(), user.locY(), user.locZ(), SoundEffects.LINGERING_POTION_THROW, SoundCategory.NEUTRAL, 0.5F, 0.4F / (world.getRandom().nextFloat() * 0.4F + 0.8F));
        return super.use(world, user, hand);
    }
}
