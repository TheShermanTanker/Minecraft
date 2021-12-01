package net.minecraft.world.inventory;

import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.IInventory;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.player.AutoRecipeStackManager;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.IRecipe;
import net.minecraft.world.item.enchantment.EnchantmentManager;

public class ContainerPlayer extends ContainerRecipeBook<InventoryCrafting> {
    public static final int CONTAINER_ID = 0;
    public static final int RESULT_SLOT = 0;
    public static final int CRAFT_SLOT_START = 1;
    public static final int CRAFT_SLOT_END = 5;
    public static final int ARMOR_SLOT_START = 5;
    public static final int ARMOR_SLOT_END = 9;
    public static final int INV_SLOT_START = 9;
    public static final int INV_SLOT_END = 36;
    public static final int USE_ROW_SLOT_START = 36;
    public static final int USE_ROW_SLOT_END = 45;
    public static final int SHIELD_SLOT = 45;
    public static final MinecraftKey BLOCK_ATLAS = new MinecraftKey("textures/atlas/blocks.png");
    public static final MinecraftKey EMPTY_ARMOR_SLOT_HELMET = new MinecraftKey("item/empty_armor_slot_helmet");
    public static final MinecraftKey EMPTY_ARMOR_SLOT_CHESTPLATE = new MinecraftKey("item/empty_armor_slot_chestplate");
    public static final MinecraftKey EMPTY_ARMOR_SLOT_LEGGINGS = new MinecraftKey("item/empty_armor_slot_leggings");
    public static final MinecraftKey EMPTY_ARMOR_SLOT_BOOTS = new MinecraftKey("item/empty_armor_slot_boots");
    public static final MinecraftKey EMPTY_ARMOR_SLOT_SHIELD = new MinecraftKey("item/empty_armor_slot_shield");
    static final MinecraftKey[] TEXTURE_EMPTY_SLOTS = new MinecraftKey[]{EMPTY_ARMOR_SLOT_BOOTS, EMPTY_ARMOR_SLOT_LEGGINGS, EMPTY_ARMOR_SLOT_CHESTPLATE, EMPTY_ARMOR_SLOT_HELMET};
    private static final EnumItemSlot[] SLOT_IDS = new EnumItemSlot[]{EnumItemSlot.HEAD, EnumItemSlot.CHEST, EnumItemSlot.LEGS, EnumItemSlot.FEET};
    private final InventoryCrafting craftSlots = new InventoryCrafting(this, 2, 2);
    private final InventoryCraftResult resultSlots = new InventoryCraftResult();
    public final boolean active;
    private final EntityHuman owner;

    public ContainerPlayer(PlayerInventory inventory, boolean onServer, EntityHuman owner) {
        super((Containers<?>)null, 0);
        this.active = onServer;
        this.owner = owner;
        this.addSlot(new SlotResult(inventory.player, this.craftSlots, this.resultSlots, 0, 154, 28));

        for(int i = 0; i < 2; ++i) {
            for(int j = 0; j < 2; ++j) {
                this.addSlot(new Slot(this.craftSlots, j + i * 2, 98 + j * 18, 18 + i * 18));
            }
        }

        for(int k = 0; k < 4; ++k) {
            final EnumItemSlot equipmentSlot = SLOT_IDS[k];
            this.addSlot(new Slot(inventory, 39 - k, 8, 8 + k * 18) {
                @Override
                public int getMaxStackSize() {
                    return 1;
                }

                @Override
                public boolean isAllowed(ItemStack stack) {
                    return equipmentSlot == EntityInsentient.getEquipmentSlotForItem(stack);
                }

                @Override
                public boolean isAllowed(EntityHuman playerEntity) {
                    ItemStack itemStack = this.getItem();
                    return !itemStack.isEmpty() && !playerEntity.isCreative() && EnchantmentManager.hasBindingCurse(itemStack) ? false : super.isAllowed(playerEntity);
                }

                @Override
                public Pair<MinecraftKey, MinecraftKey> getNoItemIcon() {
                    return Pair.of(ContainerPlayer.BLOCK_ATLAS, ContainerPlayer.TEXTURE_EMPTY_SLOTS[equipmentSlot.getIndex()]);
                }
            });
        }

        for(int l = 0; l < 3; ++l) {
            for(int m = 0; m < 9; ++m) {
                this.addSlot(new Slot(inventory, m + (l + 1) * 9, 8 + m * 18, 84 + l * 18));
            }
        }

        for(int n = 0; n < 9; ++n) {
            this.addSlot(new Slot(inventory, n, 8 + n * 18, 142));
        }

        this.addSlot(new Slot(inventory, 40, 77, 62) {
            @Override
            public Pair<MinecraftKey, MinecraftKey> getNoItemIcon() {
                return Pair.of(ContainerPlayer.BLOCK_ATLAS, ContainerPlayer.EMPTY_ARMOR_SLOT_SHIELD);
            }
        });
    }

    public static boolean isHotbarSlot(int slot) {
        return slot >= 36 && slot < 45 || slot == 45;
    }

    @Override
    public void fillCraftSlotsStackedContents(AutoRecipeStackManager finder) {
        this.craftSlots.fillStackedContents(finder);
    }

    @Override
    public void clearCraftingContent() {
        this.resultSlots.clear();
        this.craftSlots.clear();
    }

    @Override
    public boolean recipeMatches(IRecipe<? super InventoryCrafting> recipe) {
        return recipe.matches(this.craftSlots, this.owner.level);
    }

    @Override
    public void slotsChanged(IInventory inventory) {
        ContainerWorkbench.slotChangedCraftingGrid(this, this.owner.level, this.owner, this.craftSlots, this.resultSlots);
    }

    @Override
    public void removed(EntityHuman player) {
        super.removed(player);
        this.resultSlots.clear();
        if (!player.level.isClientSide) {
            this.clearContainer(player, this.craftSlots);
        }
    }

    @Override
    public boolean canUse(EntityHuman player) {
        return true;
    }

    @Override
    public ItemStack shiftClick(EntityHuman player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemStack2 = slot.getItem();
            itemStack = itemStack2.cloneItemStack();
            EnumItemSlot equipmentSlot = EntityInsentient.getEquipmentSlotForItem(itemStack);
            if (index == 0) {
                if (!this.moveItemStackTo(itemStack2, 9, 45, true)) {
                    return ItemStack.EMPTY;
                }

                slot.onQuickCraft(itemStack2, itemStack);
            } else if (index >= 1 && index < 5) {
                if (!this.moveItemStackTo(itemStack2, 9, 45, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 5 && index < 9) {
                if (!this.moveItemStackTo(itemStack2, 9, 45, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (equipmentSlot.getType() == EnumItemSlot.Function.ARMOR && !this.slots.get(8 - equipmentSlot.getIndex()).hasItem()) {
                int i = 8 - equipmentSlot.getIndex();
                if (!this.moveItemStackTo(itemStack2, i, i + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (equipmentSlot == EnumItemSlot.OFFHAND && !this.slots.get(45).hasItem()) {
                if (!this.moveItemStackTo(itemStack2, 45, 46, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 9 && index < 36) {
                if (!this.moveItemStackTo(itemStack2, 36, 45, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 36 && index < 45) {
                if (!this.moveItemStackTo(itemStack2, 9, 36, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemStack2, 9, 45, false)) {
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
            if (index == 0) {
                player.drop(itemStack2, false);
            }
        }

        return itemStack;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != this.resultSlots && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public int getResultSlotIndex() {
        return 0;
    }

    @Override
    public int getGridWidth() {
        return this.craftSlots.getWidth();
    }

    @Override
    public int getGridHeight() {
        return this.craftSlots.getHeight();
    }

    @Override
    public int getSize() {
        return 5;
    }

    public InventoryCrafting getCraftSlots() {
        return this.craftSlots;
    }

    @Override
    public RecipeBookType getRecipeBookType() {
        return RecipeBookType.CRAFTING;
    }

    @Override
    public boolean shouldMoveToInventory(int index) {
        return index != this.getResultSlotIndex();
    }
}
