package net.minecraft.world.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.EnumChatFormat;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.chat.IChatMutableComponent;
import net.minecraft.world.level.World;

public class ItemFireworksCharge extends Item {
    public ItemFireworksCharge(Item.Info settings) {
        super(settings);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World world, List<IChatBaseComponent> tooltip, TooltipFlag context) {
        NBTTagCompound compoundTag = stack.getTagElement("Explosion");
        if (compoundTag != null) {
            appendHoverText(compoundTag, tooltip);
        }

    }

    public static void appendHoverText(NBTTagCompound nbt, List<IChatBaseComponent> tooltip) {
        ItemFireworks.EffectType shape = ItemFireworks.EffectType.byId(nbt.getByte("Type"));
        tooltip.add((new ChatMessage("item.minecraft.firework_star.shape." + shape.getName())).withStyle(EnumChatFormat.GRAY));
        int[] is = nbt.getIntArray("Colors");
        if (is.length > 0) {
            tooltip.add(appendColors((new ChatComponentText("")).withStyle(EnumChatFormat.GRAY), is));
        }

        int[] js = nbt.getIntArray("FadeColors");
        if (js.length > 0) {
            tooltip.add(appendColors((new ChatMessage("item.minecraft.firework_star.fade_to")).append(" ").withStyle(EnumChatFormat.GRAY), js));
        }

        if (nbt.getBoolean("Trail")) {
            tooltip.add((new ChatMessage("item.minecraft.firework_star.trail")).withStyle(EnumChatFormat.GRAY));
        }

        if (nbt.getBoolean("Flicker")) {
            tooltip.add((new ChatMessage("item.minecraft.firework_star.flicker")).withStyle(EnumChatFormat.GRAY));
        }

    }

    private static IChatBaseComponent appendColors(IChatMutableComponent line, int[] colors) {
        for(int i = 0; i < colors.length; ++i) {
            if (i > 0) {
                line.append(", ");
            }

            line.addSibling(getColorName(colors[i]));
        }

        return line;
    }

    private static IChatBaseComponent getColorName(int color) {
        EnumColor dyeColor = EnumColor.byFireworkColor(color);
        return dyeColor == null ? new ChatMessage("item.minecraft.firework_star.custom_color") : new ChatMessage("item.minecraft.firework_star." + dyeColor.getName());
    }
}
