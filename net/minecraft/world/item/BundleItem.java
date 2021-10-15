package net.minecraft.world.item;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.EnumChatFormat;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.stats.StatisticList;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumHand;
import net.minecraft.world.InteractionResultWrapper;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.BundleTooltip;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.level.World;

public class BundleItem extends Item {
    private static final String TAG_ITEMS = "Items";
    public static final int MAX_WEIGHT = 64;
    private static final int BUNDLE_IN_BUNDLE_WEIGHT = 4;
    private static final int BAR_COLOR = MathHelper.color(0.4F, 0.4F, 1.0F);

    public BundleItem(Item.Info settings) {
        super(settings);
    }

    public static float getFullnessDisplay(ItemStack stack) {
        return (float)getContentWeight(stack) / 64.0F;
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack stack, Slot slot, ClickAction clickType, EntityHuman player) {
        if (clickType != ClickAction.SECONDARY) {
            return false;
        } else {
            ItemStack itemStack = slot.getItem();
            if (itemStack.isEmpty()) {
                removeOne(stack).ifPresent((removedStack) -> {
                    add(stack, slot.safeInsert(removedStack));
                });
            } else if (itemStack.getItem().canFitInsideContainerItems()) {
                int i = (64 - getContentWeight(stack)) / getWeight(itemStack);
                add(stack, slot.safeTake(itemStack.getCount(), i, player));
            }

            return true;
        }
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack otherStack, Slot slot, ClickAction clickType, EntityHuman player, SlotAccess cursorStackReference) {
        if (clickType == ClickAction.SECONDARY && slot.allowModification(player)) {
            if (otherStack.isEmpty()) {
                removeOne(stack).ifPresent(cursorStackReference::set);
            } else {
                otherStack.subtract(add(stack, otherStack));
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    public InteractionResultWrapper<ItemStack> use(World world, EntityHuman user, EnumHand hand) {
        ItemStack itemStack = user.getItemInHand(hand);
        if (dropContents(itemStack, user)) {
            user.awardStat(StatisticList.ITEM_USED.get(this));
            return InteractionResultWrapper.sidedSuccess(itemStack, world.isClientSide());
        } else {
            return InteractionResultWrapper.fail(itemStack);
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getContentWeight(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.min(1 + 12 * getContentWeight(stack) / 64, 13);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return BAR_COLOR;
    }

    private static int add(ItemStack bundle, ItemStack stack) {
        if (!stack.isEmpty() && stack.getItem().canFitInsideContainerItems()) {
            NBTTagCompound compoundTag = bundle.getOrCreateTag();
            if (!compoundTag.hasKey("Items")) {
                compoundTag.set("Items", new NBTTagList());
            }

            int i = getContentWeight(bundle);
            int j = getWeight(stack);
            int k = Math.min(stack.getCount(), (64 - i) / j);
            if (k == 0) {
                return 0;
            } else {
                NBTTagList listTag = compoundTag.getList("Items", 10);
                Optional<NBTTagCompound> optional = getMatchingItem(stack, listTag);
                if (optional.isPresent()) {
                    NBTTagCompound compoundTag2 = optional.get();
                    ItemStack itemStack = ItemStack.of(compoundTag2);
                    itemStack.add(k);
                    itemStack.save(compoundTag2);
                    listTag.remove(compoundTag2);
                    listTag.add(0, (NBTBase)compoundTag2);
                } else {
                    ItemStack itemStack2 = stack.cloneItemStack();
                    itemStack2.setCount(k);
                    NBTTagCompound compoundTag3 = new NBTTagCompound();
                    itemStack2.save(compoundTag3);
                    listTag.add(0, (NBTBase)compoundTag3);
                }

                return k;
            }
        } else {
            return 0;
        }
    }

    private static Optional<NBTTagCompound> getMatchingItem(ItemStack stack, NBTTagList items) {
        return stack.is(Items.BUNDLE) ? Optional.empty() : items.stream().filter(NBTTagCompound.class::isInstance).map(NBTTagCompound.class::cast).filter((item) -> {
            return ItemStack.isSameItemSameTags(ItemStack.of(item), stack);
        }).findFirst();
    }

    private static int getWeight(ItemStack stack) {
        if (stack.is(Items.BUNDLE)) {
            return 4 + getContentWeight(stack);
        } else {
            if ((stack.is(Items.BEEHIVE) || stack.is(Items.BEE_NEST)) && stack.hasTag()) {
                NBTTagCompound compoundTag = stack.getTagElement("BlockEntityTag");
                if (compoundTag != null && !compoundTag.getList("Bees", 10).isEmpty()) {
                    return 64;
                }
            }

            return 64 / stack.getMaxStackSize();
        }
    }

    private static int getContentWeight(ItemStack stack) {
        return getContents(stack).mapToInt((itemStack) -> {
            return getWeight(itemStack) * itemStack.getCount();
        }).sum();
    }

    private static Optional<ItemStack> removeOne(ItemStack stack) {
        NBTTagCompound compoundTag = stack.getOrCreateTag();
        if (!compoundTag.hasKey("Items")) {
            return Optional.empty();
        } else {
            NBTTagList listTag = compoundTag.getList("Items", 10);
            if (listTag.isEmpty()) {
                return Optional.empty();
            } else {
                int i = 0;
                NBTTagCompound compoundTag2 = listTag.getCompound(0);
                ItemStack itemStack = ItemStack.of(compoundTag2);
                listTag.remove(0);
                if (listTag.isEmpty()) {
                    stack.removeTag("Items");
                }

                return Optional.of(itemStack);
            }
        }
    }

    private static boolean dropContents(ItemStack stack, EntityHuman player) {
        NBTTagCompound compoundTag = stack.getOrCreateTag();
        if (!compoundTag.hasKey("Items")) {
            return false;
        } else {
            if (player instanceof EntityPlayer) {
                NBTTagList listTag = compoundTag.getList("Items", 10);

                for(int i = 0; i < listTag.size(); ++i) {
                    NBTTagCompound compoundTag2 = listTag.getCompound(i);
                    ItemStack itemStack = ItemStack.of(compoundTag2);
                    player.drop(itemStack, true);
                }
            }

            stack.removeTag("Items");
            return true;
        }
    }

    private static Stream<ItemStack> getContents(ItemStack stack) {
        NBTTagCompound compoundTag = stack.getTag();
        if (compoundTag == null) {
            return Stream.empty();
        } else {
            NBTTagList listTag = compoundTag.getList("Items", 10);
            return listTag.stream().map(NBTTagCompound.class::cast).map(ItemStack::of);
        }
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        NonNullList<ItemStack> nonNullList = NonNullList.create();
        getContents(stack).forEach(nonNullList::add);
        return Optional.of(new BundleTooltip(nonNullList, getContentWeight(stack)));
    }

    @Override
    public void appendHoverText(ItemStack stack, World world, List<IChatBaseComponent> tooltip, TooltipFlag context) {
        tooltip.add((new ChatMessage("item.minecraft.bundle.fullness", getContentWeight(stack), 64)).withStyle(EnumChatFormat.GRAY));
    }

    @Override
    public void onDestroyed(EntityItem entity) {
        ItemLiquidUtil.onContainerDestroyed(entity, getContents(entity.getItemStack()));
    }
}
