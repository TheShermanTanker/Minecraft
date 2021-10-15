package net.minecraft.world.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.IRegistry;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.world.item.alchemy.PotionRegistry;
import net.minecraft.world.item.alchemy.PotionUtil;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.World;

public class ItemTippedArrow extends ItemArrow {
    public ItemTippedArrow(Item.Info settings) {
        super(settings);
    }

    @Override
    public ItemStack createItemStack() {
        return PotionUtil.setPotion(super.createItemStack(), Potions.POISON);
    }

    @Override
    public void fillItemCategory(CreativeModeTab group, NonNullList<ItemStack> stacks) {
        if (this.allowdedIn(group)) {
            for(PotionRegistry potion : IRegistry.POTION) {
                if (!potion.getEffects().isEmpty()) {
                    stacks.add(PotionUtil.setPotion(new ItemStack(this), potion));
                }
            }
        }

    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World world, List<IChatBaseComponent> tooltip, TooltipFlag context) {
        PotionUtil.addPotionTooltip(stack, tooltip, 0.125F);
    }

    @Override
    public String getDescriptionId(ItemStack stack) {
        return PotionUtil.getPotion(stack).getName(this.getName() + ".effect.");
    }
}
