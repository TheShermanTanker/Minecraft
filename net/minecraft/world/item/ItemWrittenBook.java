package net.minecraft.world.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.EnumChatFormat;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.ChatComponentUtils;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.stats.StatisticList;
import net.minecraft.util.UtilColor;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.InteractionResultWrapper;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.context.ItemActionContext;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.BlockLectern;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;

public class ItemWrittenBook extends Item {
    public static final int TITLE_LENGTH = 16;
    public static final int TITLE_MAX_LENGTH = 32;
    public static final int PAGE_EDIT_LENGTH = 1024;
    public static final int PAGE_LENGTH = 32767;
    public static final int MAX_PAGES = 100;
    public static final int MAX_GENERATION = 2;
    public static final String TAG_TITLE = "title";
    public static final String TAG_FILTERED_TITLE = "filtered_title";
    public static final String TAG_AUTHOR = "author";
    public static final String TAG_PAGES = "pages";
    public static final String TAG_FILTERED_PAGES = "filtered_pages";
    public static final String TAG_GENERATION = "generation";
    public static final String TAG_RESOLVED = "resolved";

    public ItemWrittenBook(Item.Info settings) {
        super(settings);
    }

    public static boolean makeSureTagIsValid(@Nullable NBTTagCompound nbt) {
        if (!ItemBookAndQuill.makeSureTagIsValid(nbt)) {
            return false;
        } else if (!nbt.hasKeyOfType("title", 8)) {
            return false;
        } else {
            String string = nbt.getString("title");
            return string.length() > 32 ? false : nbt.hasKeyOfType("author", 8);
        }
    }

    public static int getGeneration(ItemStack stack) {
        return stack.getTag().getInt("generation");
    }

    public static int getPageCount(ItemStack stack) {
        NBTTagCompound compoundTag = stack.getTag();
        return compoundTag != null ? compoundTag.getList("pages", 8).size() : 0;
    }

    @Override
    public IChatBaseComponent getName(ItemStack stack) {
        NBTTagCompound compoundTag = stack.getTag();
        if (compoundTag != null) {
            String string = compoundTag.getString("title");
            if (!UtilColor.isNullOrEmpty(string)) {
                return new ChatComponentText(string);
            }
        }

        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World world, List<IChatBaseComponent> tooltip, TooltipFlag context) {
        if (stack.hasTag()) {
            NBTTagCompound compoundTag = stack.getTag();
            String string = compoundTag.getString("author");
            if (!UtilColor.isNullOrEmpty(string)) {
                tooltip.add((new ChatMessage("book.byAuthor", string)).withStyle(EnumChatFormat.GRAY));
            }

            tooltip.add((new ChatMessage("book.generation." + compoundTag.getInt("generation"))).withStyle(EnumChatFormat.GRAY));
        }

    }

    @Override
    public EnumInteractionResult useOn(ItemActionContext context) {
        World level = context.getWorld();
        BlockPosition blockPos = context.getClickPosition();
        IBlockData blockState = level.getType(blockPos);
        if (blockState.is(Blocks.LECTERN)) {
            return BlockLectern.tryPlaceBook(context.getEntity(), level, blockPos, blockState, context.getItemStack()) ? EnumInteractionResult.sidedSuccess(level.isClientSide) : EnumInteractionResult.PASS;
        } else {
            return EnumInteractionResult.PASS;
        }
    }

    @Override
    public InteractionResultWrapper<ItemStack> use(World world, EntityHuman user, EnumHand hand) {
        ItemStack itemStack = user.getItemInHand(hand);
        user.openBook(itemStack, hand);
        user.awardStat(StatisticList.ITEM_USED.get(this));
        return InteractionResultWrapper.sidedSuccess(itemStack, world.isClientSide());
    }

    public static boolean resolveBookComponents(ItemStack book, @Nullable CommandListenerWrapper commandSource, @Nullable EntityHuman player) {
        NBTTagCompound compoundTag = book.getTag();
        if (compoundTag != null && !compoundTag.getBoolean("resolved")) {
            compoundTag.setBoolean("resolved", true);
            if (!makeSureTagIsValid(compoundTag)) {
                return false;
            } else {
                NBTTagList listTag = compoundTag.getList("pages", 8);

                for(int i = 0; i < listTag.size(); ++i) {
                    listTag.set(i, (NBTBase)NBTTagString.valueOf(resolvePage(commandSource, player, listTag.getString(i))));
                }

                if (compoundTag.hasKeyOfType("filtered_pages", 10)) {
                    NBTTagCompound compoundTag2 = compoundTag.getCompound("filtered_pages");

                    for(String string : compoundTag2.getKeys()) {
                        compoundTag2.setString(string, resolvePage(commandSource, player, compoundTag2.getString(string)));
                    }
                }

                return true;
            }
        } else {
            return false;
        }
    }

    private static String resolvePage(@Nullable CommandListenerWrapper commandSource, @Nullable EntityHuman player, String text) {
        IChatBaseComponent component2;
        try {
            component2 = IChatBaseComponent.ChatSerializer.fromJsonLenient(text);
            component2 = ChatComponentUtils.filterForDisplay(commandSource, component2, player, 0);
        } catch (Exception var5) {
            component2 = new ChatComponentText(text);
        }

        return IChatBaseComponent.ChatSerializer.toJson(component2);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
