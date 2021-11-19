package net.minecraft.world.inventory;

import java.util.List;
import java.util.Random;
import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.core.IRegistry;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.stats.StatisticList;
import net.minecraft.world.IInventory;
import net.minecraft.world.InventorySubcontainer;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.item.ItemEnchantedBook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentManager;
import net.minecraft.world.item.enchantment.WeightedRandomEnchant;
import net.minecraft.world.level.block.Blocks;

public class ContainerEnchantTable extends Container {
    private final IInventory enchantSlots = new InventorySubcontainer(2) {
        @Override
        public void update() {
            super.update();
            ContainerEnchantTable.this.slotsChanged(this);
        }
    };
    private final ContainerAccess access;
    private final Random random = new Random();
    private final ContainerProperty enchantmentSeed = ContainerProperty.standalone();
    public final int[] costs = new int[3];
    public final int[] enchantClue = new int[]{-1, -1, -1};
    public final int[] levelClue = new int[]{-1, -1, -1};

    public ContainerEnchantTable(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, ContainerAccess.NULL);
    }

    public ContainerEnchantTable(int syncId, PlayerInventory playerInventory, ContainerAccess context) {
        super(Containers.ENCHANTMENT, syncId);
        this.access = context;
        this.addSlot(new Slot(this.enchantSlots, 0, 15, 47) {
            @Override
            public boolean isAllowed(ItemStack stack) {
                return true;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        this.addSlot(new Slot(this.enchantSlots, 1, 35, 47) {
            @Override
            public boolean isAllowed(ItemStack stack) {
                return stack.is(Items.LAPIS_LAZULI);
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

        this.addDataSlot(ContainerProperty.shared(this.costs, 0));
        this.addDataSlot(ContainerProperty.shared(this.costs, 1));
        this.addDataSlot(ContainerProperty.shared(this.costs, 2));
        this.addDataSlot(this.enchantmentSeed).set(playerInventory.player.getEnchantmentSeed());
        this.addDataSlot(ContainerProperty.shared(this.enchantClue, 0));
        this.addDataSlot(ContainerProperty.shared(this.enchantClue, 1));
        this.addDataSlot(ContainerProperty.shared(this.enchantClue, 2));
        this.addDataSlot(ContainerProperty.shared(this.levelClue, 0));
        this.addDataSlot(ContainerProperty.shared(this.levelClue, 1));
        this.addDataSlot(ContainerProperty.shared(this.levelClue, 2));
    }

    @Override
    public void slotsChanged(IInventory inventory) {
        if (inventory == this.enchantSlots) {
            ItemStack itemStack = inventory.getItem(0);
            if (!itemStack.isEmpty() && itemStack.canEnchant()) {
                this.access.execute((world, pos) -> {
                    int i = 0;

                    for(int j = -1; j <= 1; ++j) {
                        for(int k = -1; k <= 1; ++k) {
                            if ((j != 0 || k != 0) && world.isEmpty(pos.offset(k, 0, j)) && world.isEmpty(pos.offset(k, 1, j))) {
                                if (world.getType(pos.offset(k * 2, 0, j * 2)).is(Blocks.BOOKSHELF)) {
                                    ++i;
                                }

                                if (world.getType(pos.offset(k * 2, 1, j * 2)).is(Blocks.BOOKSHELF)) {
                                    ++i;
                                }

                                if (k != 0 && j != 0) {
                                    if (world.getType(pos.offset(k * 2, 0, j)).is(Blocks.BOOKSHELF)) {
                                        ++i;
                                    }

                                    if (world.getType(pos.offset(k * 2, 1, j)).is(Blocks.BOOKSHELF)) {
                                        ++i;
                                    }

                                    if (world.getType(pos.offset(k, 0, j * 2)).is(Blocks.BOOKSHELF)) {
                                        ++i;
                                    }

                                    if (world.getType(pos.offset(k, 1, j * 2)).is(Blocks.BOOKSHELF)) {
                                        ++i;
                                    }
                                }
                            }
                        }
                    }

                    this.random.setSeed((long)this.enchantmentSeed.get());

                    for(int l = 0; l < 3; ++l) {
                        this.costs[l] = EnchantmentManager.getEnchantmentCost(this.random, l, i, itemStack);
                        this.enchantClue[l] = -1;
                        this.levelClue[l] = -1;
                        if (this.costs[l] < l + 1) {
                            this.costs[l] = 0;
                        }
                    }

                    for(int m = 0; m < 3; ++m) {
                        if (this.costs[m] > 0) {
                            List<WeightedRandomEnchant> list = this.getEnchantmentList(itemStack, m, this.costs[m]);
                            if (list != null && !list.isEmpty()) {
                                WeightedRandomEnchant enchantmentInstance = list.get(this.random.nextInt(list.size()));
                                this.enchantClue[m] = IRegistry.ENCHANTMENT.getId(enchantmentInstance.enchantment);
                                this.levelClue[m] = enchantmentInstance.level;
                            }
                        }
                    }

                    this.broadcastChanges();
                });
            } else {
                for(int i = 0; i < 3; ++i) {
                    this.costs[i] = 0;
                    this.enchantClue[i] = -1;
                    this.levelClue[i] = -1;
                }
            }
        }

    }

    @Override
    public boolean clickMenuButton(EntityHuman player, int id) {
        ItemStack itemStack = this.enchantSlots.getItem(0);
        ItemStack itemStack2 = this.enchantSlots.getItem(1);
        int i = id + 1;
        if ((itemStack2.isEmpty() || itemStack2.getCount() < i) && !player.getAbilities().instabuild) {
            return false;
        } else if (this.costs[id] <= 0 || itemStack.isEmpty() || (player.experienceLevel < i || player.experienceLevel < this.costs[id]) && !player.getAbilities().instabuild) {
            return false;
        } else {
            this.access.execute((world, pos) -> {
                ItemStack itemStack3 = itemStack;
                List<WeightedRandomEnchant> list = this.getEnchantmentList(itemStack, id, this.costs[id]);
                if (!list.isEmpty()) {
                    player.enchantDone(itemStack, i);
                    boolean bl = itemStack.is(Items.BOOK);
                    if (bl) {
                        itemStack3 = new ItemStack(Items.ENCHANTED_BOOK);
                        NBTTagCompound compoundTag = itemStack.getTag();
                        if (compoundTag != null) {
                            itemStack3.setTag(compoundTag.c());
                        }

                        this.enchantSlots.setItem(0, itemStack3);
                    }

                    for(int k = 0; k < list.size(); ++k) {
                        WeightedRandomEnchant enchantmentInstance = list.get(k);
                        if (bl) {
                            ItemEnchantedBook.addEnchantment(itemStack3, enchantmentInstance);
                        } else {
                            itemStack3.addEnchantment(enchantmentInstance.enchantment, enchantmentInstance.level);
                        }
                    }

                    if (!player.getAbilities().instabuild) {
                        itemStack2.subtract(i);
                        if (itemStack2.isEmpty()) {
                            this.enchantSlots.setItem(1, ItemStack.EMPTY);
                        }
                    }

                    player.awardStat(StatisticList.ENCHANT_ITEM);
                    if (player instanceof EntityPlayer) {
                        CriterionTriggers.ENCHANTED_ITEM.trigger((EntityPlayer)player, itemStack3, i);
                    }

                    this.enchantSlots.update();
                    this.enchantmentSeed.set(player.getEnchantmentSeed());
                    this.slotsChanged(this.enchantSlots);
                    world.playSound((EntityHuman)null, pos, SoundEffects.ENCHANTMENT_TABLE_USE, EnumSoundCategory.BLOCKS, 1.0F, world.random.nextFloat() * 0.1F + 0.9F);
                }

            });
            return true;
        }
    }

    private List<WeightedRandomEnchant> getEnchantmentList(ItemStack stack, int slot, int level) {
        this.random.setSeed((long)(this.enchantmentSeed.get() + slot));
        List<WeightedRandomEnchant> list = EnchantmentManager.selectEnchantment(this.random, stack, level, false);
        if (stack.is(Items.BOOK) && list.size() > 1) {
            list.remove(this.random.nextInt(list.size()));
        }

        return list;
    }

    public int getGoldCount() {
        ItemStack itemStack = this.enchantSlots.getItem(1);
        return itemStack.isEmpty() ? 0 : itemStack.getCount();
    }

    public int getEnchantmentSeed() {
        return this.enchantmentSeed.get();
    }

    @Override
    public void removed(EntityHuman player) {
        super.removed(player);
        this.access.execute((world, pos) -> {
            this.clearContainer(player, this.enchantSlots);
        });
    }

    @Override
    public boolean canUse(EntityHuman player) {
        return stillValid(this.access, player, Blocks.ENCHANTING_TABLE);
    }

    @Override
    public ItemStack shiftClick(EntityHuman player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemStack2 = slot.getItem();
            itemStack = itemStack2.cloneItemStack();
            if (index == 0) {
                if (!this.moveItemStackTo(itemStack2, 2, 38, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (index == 1) {
                if (!this.moveItemStackTo(itemStack2, 2, 38, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (itemStack2.is(Items.LAPIS_LAZULI)) {
                if (!this.moveItemStackTo(itemStack2, 1, 2, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (this.slots.get(0).hasItem() || !this.slots.get(0).isAllowed(itemStack2)) {
                    return ItemStack.EMPTY;
                }

                ItemStack itemStack3 = itemStack2.cloneItemStack();
                itemStack3.setCount(1);
                itemStack2.subtract(1);
                this.slots.get(0).set(itemStack3);
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
