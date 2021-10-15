package net.minecraft.world.inventory;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.IInventory;
import net.minecraft.world.InventorySubcontainer;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.item.EnumColor;
import net.minecraft.world.item.ItemBanner;
import net.minecraft.world.item.ItemBannerPattern;
import net.minecraft.world.item.ItemDye;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.EnumBannerPatternType;

public class ContainerLoom extends Container {
    private static final int INV_SLOT_START = 4;
    private static final int INV_SLOT_END = 31;
    private static final int USE_ROW_SLOT_START = 31;
    private static final int USE_ROW_SLOT_END = 40;
    private final ContainerAccess access;
    final ContainerProperty selectedBannerPatternIndex = ContainerProperty.standalone();
    Runnable slotUpdateListener = () -> {
    };
    final Slot bannerSlot;
    final Slot dyeSlot;
    private final Slot patternSlot;
    private final Slot resultSlot;
    long lastSoundTime;
    private final IInventory inputContainer = new InventorySubcontainer(3) {
        @Override
        public void update() {
            super.update();
            ContainerLoom.this.slotsChanged(this);
            ContainerLoom.this.slotUpdateListener.run();
        }
    };
    private final IInventory outputContainer = new InventorySubcontainer(1) {
        @Override
        public void update() {
            super.update();
            ContainerLoom.this.slotUpdateListener.run();
        }
    };

    public ContainerLoom(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, ContainerAccess.NULL);
    }

    public ContainerLoom(int syncId, PlayerInventory playerInventory, ContainerAccess context) {
        super(Containers.LOOM, syncId);
        this.access = context;
        this.bannerSlot = this.addSlot(new Slot(this.inputContainer, 0, 13, 26) {
            @Override
            public boolean isAllowed(ItemStack stack) {
                return stack.getItem() instanceof ItemBanner;
            }
        });
        this.dyeSlot = this.addSlot(new Slot(this.inputContainer, 1, 33, 26) {
            @Override
            public boolean isAllowed(ItemStack stack) {
                return stack.getItem() instanceof ItemDye;
            }
        });
        this.patternSlot = this.addSlot(new Slot(this.inputContainer, 2, 23, 45) {
            @Override
            public boolean isAllowed(ItemStack stack) {
                return stack.getItem() instanceof ItemBannerPattern;
            }
        });
        this.resultSlot = this.addSlot(new Slot(this.outputContainer, 0, 143, 58) {
            @Override
            public boolean isAllowed(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(EntityHuman player, ItemStack stack) {
                ContainerLoom.this.bannerSlot.remove(1);
                ContainerLoom.this.dyeSlot.remove(1);
                if (!ContainerLoom.this.bannerSlot.hasItem() || !ContainerLoom.this.dyeSlot.hasItem()) {
                    ContainerLoom.this.selectedBannerPatternIndex.set(0);
                }

                context.execute((world, pos) -> {
                    long l = world.getTime();
                    if (ContainerLoom.this.lastSoundTime != l) {
                        world.playSound((EntityHuman)null, pos, SoundEffects.UI_LOOM_TAKE_RESULT, SoundCategory.BLOCKS, 1.0F, 1.0F);
                        ContainerLoom.this.lastSoundTime = l;
                    }

                });
                super.onTake(player, stack);
            }
        });

        for(int i = 0; i < 3; ++i) {
            for(int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        for(int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(playerInventory, k, 8 + k * 18, 142));
        }

        this.addDataSlot(this.selectedBannerPatternIndex);
    }

    public int getSelectedBannerPatternIndex() {
        return this.selectedBannerPatternIndex.get();
    }

    @Override
    public boolean canUse(EntityHuman player) {
        return stillValid(this.access, player, Blocks.LOOM);
    }

    @Override
    public boolean clickMenuButton(EntityHuman player, int id) {
        if (id > 0 && id <= EnumBannerPatternType.AVAILABLE_PATTERNS) {
            this.selectedBannerPatternIndex.set(id);
            this.setupResultSlot();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void slotsChanged(IInventory inventory) {
        ItemStack itemStack = this.bannerSlot.getItem();
        ItemStack itemStack2 = this.dyeSlot.getItem();
        ItemStack itemStack3 = this.patternSlot.getItem();
        ItemStack itemStack4 = this.resultSlot.getItem();
        if (itemStack4.isEmpty() || !itemStack.isEmpty() && !itemStack2.isEmpty() && this.selectedBannerPatternIndex.get() > 0 && (this.selectedBannerPatternIndex.get() < EnumBannerPatternType.COUNT - EnumBannerPatternType.PATTERN_ITEM_COUNT || !itemStack3.isEmpty())) {
            if (!itemStack3.isEmpty() && itemStack3.getItem() instanceof ItemBannerPattern) {
                NBTTagCompound compoundTag = itemStack.getOrCreateTagElement("BlockEntityTag");
                boolean bl = compoundTag.hasKeyOfType("Patterns", 9) && !itemStack.isEmpty() && compoundTag.getList("Patterns", 10).size() >= 6;
                if (bl) {
                    this.selectedBannerPatternIndex.set(0);
                } else {
                    this.selectedBannerPatternIndex.set(((ItemBannerPattern)itemStack3.getItem()).getBannerPattern().ordinal());
                }
            }
        } else {
            this.resultSlot.set(ItemStack.EMPTY);
            this.selectedBannerPatternIndex.set(0);
        }

        this.setupResultSlot();
        this.broadcastChanges();
    }

    public void registerUpdateListener(Runnable inventoryChangeListener) {
        this.slotUpdateListener = inventoryChangeListener;
    }

    @Override
    public ItemStack shiftClick(EntityHuman player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemStack2 = slot.getItem();
            itemStack = itemStack2.cloneItemStack();
            if (index == this.resultSlot.index) {
                if (!this.moveItemStackTo(itemStack2, 4, 40, true)) {
                    return ItemStack.EMPTY;
                }

                slot.onQuickCraft(itemStack2, itemStack);
            } else if (index != this.dyeSlot.index && index != this.bannerSlot.index && index != this.patternSlot.index) {
                if (itemStack2.getItem() instanceof ItemBanner) {
                    if (!this.moveItemStackTo(itemStack2, this.bannerSlot.index, this.bannerSlot.index + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (itemStack2.getItem() instanceof ItemDye) {
                    if (!this.moveItemStackTo(itemStack2, this.dyeSlot.index, this.dyeSlot.index + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (itemStack2.getItem() instanceof ItemBannerPattern) {
                    if (!this.moveItemStackTo(itemStack2, this.patternSlot.index, this.patternSlot.index + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index >= 4 && index < 31) {
                    if (!this.moveItemStackTo(itemStack2, 31, 40, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index >= 31 && index < 40 && !this.moveItemStackTo(itemStack2, 4, 31, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemStack2, 4, 40, false)) {
                return ItemStack.EMPTY;
            }

            if (itemStack2.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemStack2.getCount() == itemStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, itemStack2);
        }

        return itemStack;
    }

    @Override
    public void removed(EntityHuman player) {
        super.removed(player);
        this.access.execute((world, pos) -> {
            this.clearContainer(player, this.inputContainer);
        });
    }

    private void setupResultSlot() {
        if (this.selectedBannerPatternIndex.get() > 0) {
            ItemStack itemStack = this.bannerSlot.getItem();
            ItemStack itemStack2 = this.dyeSlot.getItem();
            ItemStack itemStack3 = ItemStack.EMPTY;
            if (!itemStack.isEmpty() && !itemStack2.isEmpty()) {
                itemStack3 = itemStack.cloneItemStack();
                itemStack3.setCount(1);
                EnumBannerPatternType bannerPattern = EnumBannerPatternType.values()[this.selectedBannerPatternIndex.get()];
                EnumColor dyeColor = ((ItemDye)itemStack2.getItem()).getDyeColor();
                NBTTagCompound compoundTag = itemStack3.getOrCreateTagElement("BlockEntityTag");
                NBTTagList listTag;
                if (compoundTag.hasKeyOfType("Patterns", 9)) {
                    listTag = compoundTag.getList("Patterns", 10);
                } else {
                    listTag = new NBTTagList();
                    compoundTag.set("Patterns", listTag);
                }

                NBTTagCompound compoundTag2 = new NBTTagCompound();
                compoundTag2.setString("Pattern", bannerPattern.getHashname());
                compoundTag2.setInt("Color", dyeColor.getColorIndex());
                listTag.add(compoundTag2);
            }

            if (!ItemStack.matches(itemStack3, this.resultSlot.getItem())) {
                this.resultSlot.set(itemStack3);
            }
        }

    }

    public Slot getBannerSlot() {
        return this.bannerSlot;
    }

    public Slot getDyeSlot() {
        return this.dyeSlot;
    }

    public Slot getPatternSlot() {
        return this.patternSlot;
    }

    public Slot getResultSlot() {
        return this.resultSlot;
    }
}
