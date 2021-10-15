package net.minecraft.world.inventory;

import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.IInventory;
import net.minecraft.world.InventorySubcontainer;
import net.minecraft.world.entity.EntityExperienceOrb;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentManager;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3D;

public class ContainerGrindstone extends Container {
    public static final int MAX_NAME_LENGTH = 35;
    public static final int INPUT_SLOT = 0;
    public static final int ADDITIONAL_SLOT = 1;
    public static final int RESULT_SLOT = 2;
    private static final int INV_SLOT_START = 3;
    private static final int INV_SLOT_END = 30;
    private static final int USE_ROW_SLOT_START = 30;
    private static final int USE_ROW_SLOT_END = 39;
    private final IInventory resultSlots = new InventoryCraftResult();
    final IInventory repairSlots = new InventorySubcontainer(2) {
        @Override
        public void update() {
            super.update();
            ContainerGrindstone.this.slotsChanged(this);
        }
    };
    private final ContainerAccess access;

    public ContainerGrindstone(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, ContainerAccess.NULL);
    }

    public ContainerGrindstone(int syncId, PlayerInventory playerInventory, ContainerAccess context) {
        super(Containers.GRINDSTONE, syncId);
        this.access = context;
        this.addSlot(new Slot(this.repairSlots, 0, 49, 19) {
            @Override
            public boolean isAllowed(ItemStack stack) {
                return stack.isDamageableItem() || stack.is(Items.ENCHANTED_BOOK) || stack.hasEnchantments();
            }
        });
        this.addSlot(new Slot(this.repairSlots, 1, 49, 40) {
            @Override
            public boolean isAllowed(ItemStack stack) {
                return stack.isDamageableItem() || stack.is(Items.ENCHANTED_BOOK) || stack.hasEnchantments();
            }
        });
        this.addSlot(new Slot(this.resultSlots, 2, 129, 34) {
            @Override
            public boolean isAllowed(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(EntityHuman player, ItemStack stack) {
                context.execute((world, pos) -> {
                    if (world instanceof WorldServer) {
                        EntityExperienceOrb.award((WorldServer)world, Vec3D.atCenterOf(pos), this.getExperienceAmount(world));
                    }

                    world.triggerEffect(1042, pos, 0);
                });
                ContainerGrindstone.this.repairSlots.setItem(0, ItemStack.EMPTY);
                ContainerGrindstone.this.repairSlots.setItem(1, ItemStack.EMPTY);
            }

            private int getExperienceAmount(World world) {
                int i = 0;
                i = i + this.getExperienceFromItem(ContainerGrindstone.this.repairSlots.getItem(0));
                i = i + this.getExperienceFromItem(ContainerGrindstone.this.repairSlots.getItem(1));
                if (i > 0) {
                    int j = (int)Math.ceil((double)i / 2.0D);
                    return j + world.random.nextInt(j);
                } else {
                    return 0;
                }
            }

            private int getExperienceFromItem(ItemStack stack) {
                int i = 0;
                Map<Enchantment, Integer> map = EnchantmentManager.getEnchantments(stack);

                for(Entry<Enchantment, Integer> entry : map.entrySet()) {
                    Enchantment enchantment = entry.getKey();
                    Integer integer = entry.getValue();
                    if (!enchantment.isCurse()) {
                        i += enchantment.getMinCost(integer);
                    }
                }

                return i;
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

    }

    @Override
    public void slotsChanged(IInventory inventory) {
        super.slotsChanged(inventory);
        if (inventory == this.repairSlots) {
            this.createResult();
        }

    }

    private void createResult() {
        ItemStack itemStack = this.repairSlots.getItem(0);
        ItemStack itemStack2 = this.repairSlots.getItem(1);
        boolean bl = !itemStack.isEmpty() || !itemStack2.isEmpty();
        boolean bl2 = !itemStack.isEmpty() && !itemStack2.isEmpty();
        if (!bl) {
            this.resultSlots.setItem(0, ItemStack.EMPTY);
        } else {
            boolean bl3 = !itemStack.isEmpty() && !itemStack.is(Items.ENCHANTED_BOOK) && !itemStack.hasEnchantments() || !itemStack2.isEmpty() && !itemStack2.is(Items.ENCHANTED_BOOK) && !itemStack2.hasEnchantments();
            if (itemStack.getCount() > 1 || itemStack2.getCount() > 1 || !bl2 && bl3) {
                this.resultSlots.setItem(0, ItemStack.EMPTY);
                this.broadcastChanges();
                return;
            }

            int i = 1;
            int m;
            ItemStack itemStack3;
            if (bl2) {
                if (!itemStack.is(itemStack2.getItem())) {
                    this.resultSlots.setItem(0, ItemStack.EMPTY);
                    this.broadcastChanges();
                    return;
                }

                Item item = itemStack.getItem();
                int j = item.getMaxDurability() - itemStack.getDamage();
                int k = item.getMaxDurability() - itemStack2.getDamage();
                int l = j + k + item.getMaxDurability() * 5 / 100;
                m = Math.max(item.getMaxDurability() - l, 0);
                itemStack3 = this.mergeEnchants(itemStack, itemStack2);
                if (!itemStack3.isDamageableItem()) {
                    if (!ItemStack.matches(itemStack, itemStack2)) {
                        this.resultSlots.setItem(0, ItemStack.EMPTY);
                        this.broadcastChanges();
                        return;
                    }

                    i = 2;
                }
            } else {
                boolean bl4 = !itemStack.isEmpty();
                m = bl4 ? itemStack.getDamage() : itemStack2.getDamage();
                itemStack3 = bl4 ? itemStack : itemStack2;
            }

            this.resultSlots.setItem(0, this.removeNonCurses(itemStack3, m, i));
        }

        this.broadcastChanges();
    }

    private ItemStack mergeEnchants(ItemStack target, ItemStack source) {
        ItemStack itemStack = target.cloneItemStack();
        Map<Enchantment, Integer> map = EnchantmentManager.getEnchantments(source);

        for(Entry<Enchantment, Integer> entry : map.entrySet()) {
            Enchantment enchantment = entry.getKey();
            if (!enchantment.isCurse() || EnchantmentManager.getEnchantmentLevel(enchantment, itemStack) == 0) {
                itemStack.addEnchantment(enchantment, entry.getValue());
            }
        }

        return itemStack;
    }

    private ItemStack removeNonCurses(ItemStack item, int damage, int amount) {
        ItemStack itemStack = item.cloneItemStack();
        itemStack.removeTag("Enchantments");
        itemStack.removeTag("StoredEnchantments");
        if (damage > 0) {
            itemStack.setDamage(damage);
        } else {
            itemStack.removeTag("Damage");
        }

        itemStack.setCount(amount);
        Map<Enchantment, Integer> map = EnchantmentManager.getEnchantments(item).entrySet().stream().filter((entry) -> {
            return entry.getKey().isCurse();
        }).collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        EnchantmentManager.setEnchantments(map, itemStack);
        itemStack.setRepairCost(0);
        if (itemStack.is(Items.ENCHANTED_BOOK) && map.size() == 0) {
            itemStack = new ItemStack(Items.BOOK);
            if (item.hasName()) {
                itemStack.setHoverName(item.getName());
            }
        }

        for(int i = 0; i < map.size(); ++i) {
            itemStack.setRepairCost(ContainerAnvil.calculateIncreasedRepairCost(itemStack.getRepairCost()));
        }

        return itemStack;
    }

    @Override
    public void removed(EntityHuman player) {
        super.removed(player);
        this.access.execute((world, pos) -> {
            this.clearContainer(player, this.repairSlots);
        });
    }

    @Override
    public boolean canUse(EntityHuman player) {
        return stillValid(this.access, player, Blocks.GRINDSTONE);
    }

    @Override
    public ItemStack shiftClick(EntityHuman player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemStack2 = slot.getItem();
            itemStack = itemStack2.cloneItemStack();
            ItemStack itemStack3 = this.repairSlots.getItem(0);
            ItemStack itemStack4 = this.repairSlots.getItem(1);
            if (index == 2) {
                if (!this.moveItemStackTo(itemStack2, 3, 39, true)) {
                    return ItemStack.EMPTY;
                }

                slot.onQuickCraft(itemStack2, itemStack);
            } else if (index != 0 && index != 1) {
                if (!itemStack3.isEmpty() && !itemStack4.isEmpty()) {
                    if (index >= 3 && index < 30) {
                        if (!this.moveItemStackTo(itemStack2, 30, 39, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (index >= 30 && index < 39 && !this.moveItemStackTo(itemStack2, 3, 30, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(itemStack2, 0, 2, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemStack2, 3, 39, false)) {
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
}
