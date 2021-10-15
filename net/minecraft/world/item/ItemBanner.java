package net.minecraft.world.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.EnumChatFormat;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockBannerAbstract;
import net.minecraft.world.level.block.entity.EnumBannerPatternType;
import org.apache.commons.lang3.Validate;

public class ItemBanner extends ItemBlockWallable {
    private static final String PATTERN_PREFIX = "block.minecraft.banner.";

    public ItemBanner(Block standingBlock, Block wallBlock, Item.Info settings) {
        super(standingBlock, wallBlock, settings);
        Validate.isInstanceOf(BlockBannerAbstract.class, standingBlock);
        Validate.isInstanceOf(BlockBannerAbstract.class, wallBlock);
    }

    public static void appendHoverTextFromBannerBlockEntityTag(ItemStack stack, List<IChatBaseComponent> tooltip) {
        NBTTagCompound compoundTag = stack.getTagElement("BlockEntityTag");
        if (compoundTag != null && compoundTag.hasKey("Patterns")) {
            NBTTagList listTag = compoundTag.getList("Patterns", 10);

            for(int i = 0; i < listTag.size() && i < 6; ++i) {
                NBTTagCompound compoundTag2 = listTag.getCompound(i);
                EnumColor dyeColor = EnumColor.fromColorIndex(compoundTag2.getInt("Color"));
                EnumBannerPatternType bannerPattern = EnumBannerPatternType.byHash(compoundTag2.getString("Pattern"));
                if (bannerPattern != null) {
                    tooltip.add((new ChatMessage("block.minecraft.banner." + bannerPattern.getFilename() + "." + dyeColor.getName())).withStyle(EnumChatFormat.GRAY));
                }
            }

        }
    }

    public EnumColor getColor() {
        return ((BlockBannerAbstract)this.getBlock()).getColor();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World world, List<IChatBaseComponent> tooltip, TooltipFlag context) {
        appendHoverTextFromBannerBlockEntityTag(stack, tooltip);
    }
}
