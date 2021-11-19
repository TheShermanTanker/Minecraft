package net.minecraft.world.inventory;

import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.IInventory;
import net.minecraft.world.InventorySubcontainer;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemWorldMap;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.maps.WorldMap;

public class ContainerCartography extends Container {
    public static final int MAP_SLOT = 0;
    public static final int ADDITIONAL_SLOT = 1;
    public static final int RESULT_SLOT = 2;
    private static final int INV_SLOT_START = 3;
    private static final int INV_SLOT_END = 30;
    private static final int USE_ROW_SLOT_START = 30;
    private static final int USE_ROW_SLOT_END = 39;
    private final ContainerAccess access;
    long lastSoundTime;
    public final IInventory container = new InventorySubcontainer(2) {
        @Override
        public void update() {
            ContainerCartography.this.slotsChanged(this);
            super.update();
        }
    };
    private final InventoryCraftResult resultContainer = new InventoryCraftResult() {
        @Override
        public void update() {
            ContainerCartography.this.slotsChanged(this);
            super.update();
        }
    };

    public ContainerCartography(int syncId, PlayerInventory inventory) {
        this(syncId, inventory, ContainerAccess.NULL);
    }

    public ContainerCartography(int syncId, PlayerInventory inventory, ContainerAccess context) {
        super(Containers.CARTOGRAPHY_TABLE, syncId);
        this.access = context;
        this.addSlot(new Slot(this.container, 0, 15, 15) {
            @Override
            public boolean isAllowed(ItemStack stack) {
                return stack.is(Items.FILLED_MAP);
            }
        });
        this.addSlot(new Slot(this.container, 1, 15, 52) {
            @Override
            public boolean isAllowed(ItemStack stack) {
                return stack.is(Items.PAPER) || stack.is(Items.MAP) || stack.is(Items.GLASS_PANE);
            }
        });
        this.addSlot(new Slot(this.resultContainer, 2, 145, 39) {
            @Override
            public boolean isAllowed(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(EntityHuman player, ItemStack stack) {
                ContainerCartography.this.slots.get(0).remove(1);
                ContainerCartography.this.slots.get(1).remove(1);
                stack.getItem().onCraftedBy(stack, player.level, player);
                context.execute((world, pos) -> {
                    long l = world.getTime();
                    if (ContainerCartography.this.lastSoundTime != l) {
                        world.playSound((EntityHuman)null, pos, SoundEffects.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, EnumSoundCategory.BLOCKS, 1.0F, 1.0F);
                        ContainerCartography.this.lastSoundTime = l;
                    }

                });
                super.onTake(player, stack);
            }
        });

        for(int i = 0; i < 3; ++i) {
            for(int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(inventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        for(int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(inventory, k, 8 + k * 18, 142));
        }

    }

    @Override
    public boolean canUse(EntityHuman player) {
        return stillValid(this.access, player, Blocks.CARTOGRAPHY_TABLE);
    }

    @Override
    public void slotsChanged(IInventory inventory) {
        ItemStack itemStack = this.container.getItem(0);
        ItemStack itemStack2 = this.container.getItem(1);
        ItemStack itemStack3 = this.resultContainer.getItem(2);
        if (itemStack3.isEmpty() || !itemStack.isEmpty() && !itemStack2.isEmpty()) {
            if (!itemStack.isEmpty() && !itemStack2.isEmpty()) {
                this.setupResultSlot(itemStack, itemStack2, itemStack3);
            }
        } else {
            this.resultContainer.splitWithoutUpdate(2);
        }

    }

    private void setupResultSlot(ItemStack map, ItemStack item, ItemStack oldResult) {
        this.access.execute((world, pos) -> {
            WorldMap mapItemSavedData = ItemWorldMap.getSavedMap(map, world);
            if (mapItemSavedData != null) {
                ItemStack itemStack4;
                if (item.is(Items.PAPER) && !mapItemSavedData.locked && mapItemSavedData.scale < 4) {
                    itemStack4 = map.cloneItemStack();
                    itemStack4.setCount(1);
                    itemStack4.getOrCreateTag().setInt("map_scale_direction", 1);
                    this.broadcastChanges();
                } else if (item.is(Items.GLASS_PANE) && !mapItemSavedData.locked) {
                    itemStack4 = map.cloneItemStack();
                    itemStack4.setCount(1);
                    itemStack4.getOrCreateTag().setBoolean("map_to_lock", true);
                    this.broadcastChanges();
                } else {
                    if (!item.is(Items.MAP)) {
                        this.resultContainer.splitWithoutUpdate(2);
                        this.broadcastChanges();
                        return;
                    }

                    itemStack4 = map.cloneItemStack();
                    itemStack4.setCount(2);
                    this.broadcastChanges();
                }

                if (!ItemStack.matches(itemStack4, oldResult)) {
                    this.resultContainer.setItem(2, itemStack4);
                    this.broadcastChanges();
                }

            }
        });
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != this.resultContainer && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public ItemStack shiftClick(EntityHuman player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemStack2 = slot.getItem();
            itemStack = itemStack2.cloneItemStack();
            if (index == 2) {
                itemStack2.getItem().onCraftedBy(itemStack2, player.level, player);
                if (!this.moveItemStackTo(itemStack2, 3, 39, true)) {
                    return ItemStack.EMPTY;
                }

                slot.onQuickCraft(itemStack2, itemStack);
            } else if (index != 1 && index != 0) {
                if (itemStack2.is(Items.FILLED_MAP)) {
                    if (!this.moveItemStackTo(itemStack2, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!itemStack2.is(Items.PAPER) && !itemStack2.is(Items.MAP) && !itemStack2.is(Items.GLASS_PANE)) {
                    if (index >= 3 && index < 30) {
                        if (!this.moveItemStackTo(itemStack2, 30, 39, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (index >= 30 && index < 39 && !this.moveItemStackTo(itemStack2, 3, 30, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(itemStack2, 1, 2, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemStack2, 3, 39, false)) {
                return ItemStack.EMPTY;
            }

            if (itemStack2.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            }

            slot.setChanged();
            if (itemStack2.getCount() == itemStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, itemStack2);
            this.broadcastChanges();
        }

        return itemStack;
    }

    @Override
    public void removed(EntityHuman player) {
        super.removed(player);
        this.resultContainer.splitWithoutUpdate(2);
        this.access.execute((world, pos) -> {
            this.clearContainer(player, this.container);
        });
    }
}
