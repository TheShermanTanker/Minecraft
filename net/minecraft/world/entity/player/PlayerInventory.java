package net.minecraft.world.entity.player;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportSystemDetails;
import net.minecraft.ReportedException;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.game.PacketPlayOutSetSlot;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.tags.Tag;
import net.minecraft.world.ContainerUtil;
import net.minecraft.world.IInventory;
import net.minecraft.world.INamableTileEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemArmor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.IBlockData;

public class PlayerInventory implements IInventory, INamableTileEntity {
    public static final int POP_TIME_DURATION = 5;
    public static final int INVENTORY_SIZE = 36;
    private static final int SELECTION_SIZE = 9;
    public static final int SLOT_OFFHAND = 40;
    public static final int NOT_FOUND_INDEX = -1;
    public static final int[] ALL_ARMOR_SLOTS = new int[]{0, 1, 2, 3};
    public static final int[] HELMET_SLOT_ONLY = new int[]{3};
    public final NonNullList<ItemStack> items = NonNullList.withSize(36, ItemStack.EMPTY);
    public final NonNullList<ItemStack> armor = NonNullList.withSize(4, ItemStack.EMPTY);
    public final NonNullList<ItemStack> offhand = NonNullList.withSize(1, ItemStack.EMPTY);
    public final List<NonNullList<ItemStack>> compartments = ImmutableList.of(this.items, this.armor, this.offhand);
    public int selected;
    public final EntityHuman player;
    private int timesChanged;

    public PlayerInventory(EntityHuman player) {
        this.player = player;
    }

    public ItemStack getItemInHand() {
        return isHotbarSlot(this.selected) ? this.items.get(this.selected) : ItemStack.EMPTY;
    }

    public static int getHotbarSize() {
        return 9;
    }

    private boolean isSimilarAndNotFull(ItemStack existingStack, ItemStack stack) {
        return !existingStack.isEmpty() && ItemStack.isSameItemSameTags(existingStack, stack) && existingStack.isStackable() && existingStack.getCount() < existingStack.getMaxStackSize() && existingStack.getCount() < this.getMaxStackSize();
    }

    public int getFirstEmptySlotIndex() {
        for(int i = 0; i < this.items.size(); ++i) {
            if (this.items.get(i).isEmpty()) {
                return i;
            }
        }

        return -1;
    }

    public void setPickedItem(ItemStack stack) {
        int i = this.findSlotMatchingItem(stack);
        if (isHotbarSlot(i)) {
            this.selected = i;
        } else {
            if (i == -1) {
                this.selected = this.getSuitableHotbarSlot();
                if (!this.items.get(this.selected).isEmpty()) {
                    int j = this.getFirstEmptySlotIndex();
                    if (j != -1) {
                        this.items.set(j, this.items.get(this.selected));
                    }
                }

                this.items.set(this.selected, stack);
            } else {
                this.pickSlot(i);
            }

        }
    }

    public void pickSlot(int slot) {
        this.selected = this.getSuitableHotbarSlot();
        ItemStack itemStack = this.items.get(this.selected);
        this.items.set(this.selected, this.items.get(slot));
        this.items.set(slot, itemStack);
    }

    public static boolean isHotbarSlot(int slot) {
        return slot >= 0 && slot < 9;
    }

    public int findSlotMatchingItem(ItemStack stack) {
        for(int i = 0; i < this.items.size(); ++i) {
            if (!this.items.get(i).isEmpty() && ItemStack.isSameItemSameTags(stack, this.items.get(i))) {
                return i;
            }
        }

        return -1;
    }

    public int findSlotMatchingUnusedItem(ItemStack stack) {
        for(int i = 0; i < this.items.size(); ++i) {
            ItemStack itemStack = this.items.get(i);
            if (!this.items.get(i).isEmpty() && ItemStack.isSameItemSameTags(stack, this.items.get(i)) && !this.items.get(i).isDamaged() && !itemStack.hasEnchantments() && !itemStack.hasName()) {
                return i;
            }
        }

        return -1;
    }

    public int getSuitableHotbarSlot() {
        for(int i = 0; i < 9; ++i) {
            int j = (this.selected + i) % 9;
            if (this.items.get(j).isEmpty()) {
                return j;
            }
        }

        for(int k = 0; k < 9; ++k) {
            int l = (this.selected + k) % 9;
            if (!this.items.get(l).hasEnchantments()) {
                return l;
            }
        }

        return this.selected;
    }

    public void swapPaint(double scrollAmount) {
        if (scrollAmount > 0.0D) {
            scrollAmount = 1.0D;
        }

        if (scrollAmount < 0.0D) {
            scrollAmount = -1.0D;
        }

        for(this.selected = (int)((double)this.selected - scrollAmount); this.selected < 0; this.selected += 9) {
        }

        while(this.selected >= 9) {
            this.selected -= 9;
        }

    }

    public int clearOrCountMatchingItems(Predicate<ItemStack> shouldRemove, int maxCount, IInventory craftingInventory) {
        int i = 0;
        boolean bl = maxCount == 0;
        i = i + ContainerUtil.clearOrCountMatchingItems(this, shouldRemove, maxCount - i, bl);
        i = i + ContainerUtil.clearOrCountMatchingItems(craftingInventory, shouldRemove, maxCount - i, bl);
        ItemStack itemStack = this.player.containerMenu.getCarried();
        i = i + ContainerUtil.clearOrCountMatchingItems(itemStack, shouldRemove, maxCount - i, bl);
        if (itemStack.isEmpty()) {
            this.player.containerMenu.setCarried(ItemStack.EMPTY);
        }

        return i;
    }

    private int addResource(ItemStack stack) {
        int i = this.firstPartial(stack);
        if (i == -1) {
            i = this.getFirstEmptySlotIndex();
        }

        return i == -1 ? stack.getCount() : this.addResource(i, stack);
    }

    private int addResource(int slot, ItemStack stack) {
        Item item = stack.getItem();
        int i = stack.getCount();
        ItemStack itemStack = this.getItem(slot);
        if (itemStack.isEmpty()) {
            itemStack = new ItemStack(item, 0);
            if (stack.hasTag()) {
                itemStack.setTag(stack.getTag().copy());
            }

            this.setItem(slot, itemStack);
        }

        int j = i;
        if (i > itemStack.getMaxStackSize() - itemStack.getCount()) {
            j = itemStack.getMaxStackSize() - itemStack.getCount();
        }

        if (j > this.getMaxStackSize() - itemStack.getCount()) {
            j = this.getMaxStackSize() - itemStack.getCount();
        }

        if (j == 0) {
            return i;
        } else {
            i = i - j;
            itemStack.add(j);
            itemStack.setPopTime(5);
            return i;
        }
    }

    public int firstPartial(ItemStack stack) {
        if (this.isSimilarAndNotFull(this.getItem(this.selected), stack)) {
            return this.selected;
        } else if (this.isSimilarAndNotFull(this.getItem(40), stack)) {
            return 40;
        } else {
            for(int i = 0; i < this.items.size(); ++i) {
                if (this.isSimilarAndNotFull(this.items.get(i), stack)) {
                    return i;
                }
            }

            return -1;
        }
    }

    public void tick() {
        for(NonNullList<ItemStack> nonNullList : this.compartments) {
            for(int i = 0; i < nonNullList.size(); ++i) {
                if (!nonNullList.get(i).isEmpty()) {
                    nonNullList.get(i).inventoryTick(this.player.level, this.player, i, this.selected == i);
                }
            }
        }

    }

    public boolean pickup(ItemStack stack) {
        return this.add(-1, stack);
    }

    public boolean add(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        } else {
            try {
                if (stack.isDamaged()) {
                    if (slot == -1) {
                        slot = this.getFirstEmptySlotIndex();
                    }

                    if (slot >= 0) {
                        this.items.set(slot, stack.cloneItemStack());
                        this.items.get(slot).setPopTime(5);
                        stack.setCount(0);
                        return true;
                    } else if (this.player.getAbilities().instabuild) {
                        stack.setCount(0);
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    int i;
                    do {
                        i = stack.getCount();
                        if (slot == -1) {
                            stack.setCount(this.addResource(stack));
                        } else {
                            stack.setCount(this.addResource(slot, stack));
                        }
                    } while(!stack.isEmpty() && stack.getCount() < i);

                    if (stack.getCount() == i && this.player.getAbilities().instabuild) {
                        stack.setCount(0);
                        return true;
                    } else {
                        return stack.getCount() < i;
                    }
                }
            } catch (Throwable var6) {
                CrashReport crashReport = CrashReport.forThrowable(var6, "Adding item to inventory");
                CrashReportSystemDetails crashReportCategory = crashReport.addCategory("Item being added");
                crashReportCategory.setDetail("Item ID", Item.getId(stack.getItem()));
                crashReportCategory.setDetail("Item data", stack.getDamage());
                crashReportCategory.setDetail("Item name", () -> {
                    return stack.getName().getString();
                });
                throw new ReportedException(crashReport);
            }
        }
    }

    public void placeItemBackInInventory(ItemStack stack) {
        this.placeItemBackInInventory(stack, true);
    }

    public void placeItemBackInInventory(ItemStack stack, boolean notifiesClient) {
        while(true) {
            if (!stack.isEmpty()) {
                int i = this.firstPartial(stack);
                if (i == -1) {
                    i = this.getFirstEmptySlotIndex();
                }

                if (i != -1) {
                    int j = stack.getMaxStackSize() - this.getItem(i).getCount();
                    if (this.add(i, stack.cloneAndSubtract(j)) && notifiesClient && this.player instanceof EntityPlayer) {
                        ((EntityPlayer)this.player).connection.sendPacket(new PacketPlayOutSetSlot(-2, 0, i, this.getItem(i)));
                    }
                    continue;
                }

                this.player.drop(stack, false);
            }

            return;
        }
    }

    @Override
    public ItemStack splitStack(int slot, int amount) {
        List<ItemStack> list = null;

        for(NonNullList<ItemStack> nonNullList : this.compartments) {
            if (slot < nonNullList.size()) {
                list = nonNullList;
                break;
            }

            slot -= nonNullList.size();
        }

        return list != null && !list.get(slot).isEmpty() ? ContainerUtil.removeItem(list, slot, amount) : ItemStack.EMPTY;
    }

    public void removeItem(ItemStack stack) {
        for(NonNullList<ItemStack> nonNullList : this.compartments) {
            for(int i = 0; i < nonNullList.size(); ++i) {
                if (nonNullList.get(i) == stack) {
                    nonNullList.set(i, ItemStack.EMPTY);
                    break;
                }
            }
        }

    }

    @Override
    public ItemStack splitWithoutUpdate(int slot) {
        NonNullList<ItemStack> nonNullList = null;

        for(NonNullList<ItemStack> nonNullList2 : this.compartments) {
            if (slot < nonNullList2.size()) {
                nonNullList = nonNullList2;
                break;
            }

            slot -= nonNullList2.size();
        }

        if (nonNullList != null && !nonNullList.get(slot).isEmpty()) {
            ItemStack itemStack = nonNullList.get(slot);
            nonNullList.set(slot, ItemStack.EMPTY);
            return itemStack;
        } else {
            return ItemStack.EMPTY;
        }
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        NonNullList<ItemStack> nonNullList = null;

        for(NonNullList<ItemStack> nonNullList2 : this.compartments) {
            if (slot < nonNullList2.size()) {
                nonNullList = nonNullList2;
                break;
            }

            slot -= nonNullList2.size();
        }

        if (nonNullList != null) {
            nonNullList.set(slot, stack);
        }

    }

    public float getDestroySpeed(IBlockData block) {
        return this.items.get(this.selected).getDestroySpeed(block);
    }

    public NBTTagList save(NBTTagList nbtList) {
        for(int i = 0; i < this.items.size(); ++i) {
            if (!this.items.get(i).isEmpty()) {
                NBTTagCompound compoundTag = new NBTTagCompound();
                compoundTag.setByte("Slot", (byte)i);
                this.items.get(i).save(compoundTag);
                nbtList.add(compoundTag);
            }
        }

        for(int j = 0; j < this.armor.size(); ++j) {
            if (!this.armor.get(j).isEmpty()) {
                NBTTagCompound compoundTag2 = new NBTTagCompound();
                compoundTag2.setByte("Slot", (byte)(j + 100));
                this.armor.get(j).save(compoundTag2);
                nbtList.add(compoundTag2);
            }
        }

        for(int k = 0; k < this.offhand.size(); ++k) {
            if (!this.offhand.get(k).isEmpty()) {
                NBTTagCompound compoundTag3 = new NBTTagCompound();
                compoundTag3.setByte("Slot", (byte)(k + 150));
                this.offhand.get(k).save(compoundTag3);
                nbtList.add(compoundTag3);
            }
        }

        return nbtList;
    }

    public void load(NBTTagList nbtList) {
        this.items.clear();
        this.armor.clear();
        this.offhand.clear();

        for(int i = 0; i < nbtList.size(); ++i) {
            NBTTagCompound compoundTag = nbtList.getCompound(i);
            int j = compoundTag.getByte("Slot") & 255;
            ItemStack itemStack = ItemStack.of(compoundTag);
            if (!itemStack.isEmpty()) {
                if (j >= 0 && j < this.items.size()) {
                    this.items.set(j, itemStack);
                } else if (j >= 100 && j < this.armor.size() + 100) {
                    this.armor.set(j - 100, itemStack);
                } else if (j >= 150 && j < this.offhand.size() + 150) {
                    this.offhand.set(j - 150, itemStack);
                }
            }
        }

    }

    @Override
    public int getSize() {
        return this.items.size() + this.armor.size() + this.offhand.size();
    }

    @Override
    public boolean isEmpty() {
        for(ItemStack itemStack : this.items) {
            if (!itemStack.isEmpty()) {
                return false;
            }
        }

        for(ItemStack itemStack2 : this.armor) {
            if (!itemStack2.isEmpty()) {
                return false;
            }
        }

        for(ItemStack itemStack3 : this.offhand) {
            if (!itemStack3.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        List<ItemStack> list = null;

        for(NonNullList<ItemStack> nonNullList : this.compartments) {
            if (slot < nonNullList.size()) {
                list = nonNullList;
                break;
            }

            slot -= nonNullList.size();
        }

        return list == null ? ItemStack.EMPTY : list.get(slot);
    }

    @Override
    public IChatBaseComponent getDisplayName() {
        return new ChatMessage("container.inventory");
    }

    public ItemStack getArmor(int slot) {
        return this.armor.get(slot);
    }

    public void hurtArmor(DamageSource damageSource, float amount, int[] slots) {
        if (!(amount <= 0.0F)) {
            amount = amount / 4.0F;
            if (amount < 1.0F) {
                amount = 1.0F;
            }

            for(int i : slots) {
                ItemStack itemStack = this.armor.get(i);
                if ((!damageSource.isFire() || !itemStack.getItem().isFireResistant()) && itemStack.getItem() instanceof ItemArmor) {
                    itemStack.damage((int)amount, this.player, (player) -> {
                        player.broadcastItemBreak(EnumItemSlot.byTypeAndIndex(EnumItemSlot.Function.ARMOR, i));
                    });
                }
            }

        }
    }

    public void dropContents() {
        for(List<ItemStack> list : this.compartments) {
            for(int i = 0; i < list.size(); ++i) {
                ItemStack itemStack = list.get(i);
                if (!itemStack.isEmpty()) {
                    this.player.drop(itemStack, true, false);
                    list.set(i, ItemStack.EMPTY);
                }
            }
        }

    }

    @Override
    public void update() {
        ++this.timesChanged;
    }

    public int getTimesChanged() {
        return this.timesChanged;
    }

    @Override
    public boolean stillValid(EntityHuman player) {
        if (this.player.isRemoved()) {
            return false;
        } else {
            return !(player.distanceToSqr(this.player) > 64.0D);
        }
    }

    public boolean contains(ItemStack stack) {
        for(List<ItemStack> list : this.compartments) {
            for(ItemStack itemStack : list) {
                if (!itemStack.isEmpty() && itemStack.doMaterialsMatch(stack)) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean contains(Tag<Item> tag) {
        for(List<ItemStack> list : this.compartments) {
            for(ItemStack itemStack : list) {
                if (!itemStack.isEmpty() && itemStack.is(tag)) {
                    return true;
                }
            }
        }

        return false;
    }

    public void replaceWith(PlayerInventory other) {
        for(int i = 0; i < this.getSize(); ++i) {
            this.setItem(i, other.getItem(i));
        }

        this.selected = other.selected;
    }

    @Override
    public void clear() {
        for(List<ItemStack> list : this.compartments) {
            list.clear();
        }

    }

    public void fillStackedContents(AutoRecipeStackManager finder) {
        for(ItemStack itemStack : this.items) {
            finder.accountSimpleStack(itemStack);
        }

    }

    public ItemStack removeFromSelected(boolean entireStack) {
        ItemStack itemStack = this.getItemInHand();
        return itemStack.isEmpty() ? ItemStack.EMPTY : this.splitStack(this.selected, entireStack ? itemStack.getCount() : 1);
    }
}
