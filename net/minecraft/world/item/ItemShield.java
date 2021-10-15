package net.minecraft.world.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.tags.TagsItem;
import net.minecraft.world.EnumHand;
import net.minecraft.world.InteractionResultWrapper;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.BlockDispenser;

public class ItemShield extends Item {
    public static final int EFFECTIVE_BLOCK_DELAY = 5;
    public static final float MINIMUM_DURABILITY_DAMAGE = 3.0F;
    public static final String TAG_BASE_COLOR = "Base";

    public ItemShield(Item.Info settings) {
        super(settings);
        BlockDispenser.registerBehavior(this, ItemArmor.DISPENSE_ITEM_BEHAVIOR);
    }

    @Override
    public String getDescriptionId(ItemStack stack) {
        return stack.getTagElement("BlockEntityTag") != null ? this.getName() + "." + getColor(stack).getName() : super.getDescriptionId(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World world, List<IChatBaseComponent> tooltip, TooltipFlag context) {
        ItemBanner.appendHoverTextFromBannerBlockEntityTag(stack, tooltip);
    }

    @Override
    public EnumAnimation getUseAnimation(ItemStack stack) {
        return EnumAnimation.BLOCK;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public InteractionResultWrapper<ItemStack> use(World world, EntityHuman user, EnumHand hand) {
        ItemStack itemStack = user.getItemInHand(hand);
        user.startUsingItem(hand);
        return InteractionResultWrapper.consume(itemStack);
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack ingredient) {
        return ingredient.is(TagsItem.PLANKS) || super.isValidRepairItem(stack, ingredient);
    }

    public static EnumColor getColor(ItemStack stack) {
        return EnumColor.fromColorIndex(stack.getOrCreateTagElement("BlockEntityTag").getInt("Base"));
    }
}
