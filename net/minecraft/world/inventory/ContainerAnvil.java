package net.minecraft.world.inventory;

import java.util.Map;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.item.ItemEnchantedBook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentManager;
import net.minecraft.world.level.block.BlockAnvil;
import net.minecraft.world.level.block.state.IBlockData;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ContainerAnvil extends ContainerAnvilAbstract {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final boolean DEBUG_COST = false;
    public static final int MAX_NAME_LENGTH = 50;
    private int repairItemCountCost;
    public String itemName;
    public final ContainerProperty cost = ContainerProperty.standalone();
    private static final int COST_FAIL = 0;
    private static final int COST_BASE = 1;
    private static final int COST_ADDED_BASE = 1;
    private static final int COST_REPAIR_MATERIAL = 1;
    private static final int COST_REPAIR_SACRIFICE = 2;
    private static final int COST_INCOMPATIBLE_PENALTY = 1;
    private static final int COST_RENAME = 1;

    public ContainerAnvil(int syncId, PlayerInventory inventory) {
        this(syncId, inventory, ContainerAccess.NULL);
    }

    public ContainerAnvil(int syncId, PlayerInventory inventory, ContainerAccess context) {
        super(Containers.ANVIL, syncId, inventory, context);
        this.addDataSlot(this.cost);
    }

    @Override
    protected boolean isValidBlock(IBlockData state) {
        return state.is(TagsBlock.ANVIL);
    }

    @Override
    protected boolean mayPickup(EntityHuman player, boolean present) {
        return (player.getAbilities().instabuild || player.experienceLevel >= this.cost.get()) && this.cost.get() > 0;
    }

    @Override
    protected void onTake(EntityHuman player, ItemStack stack) {
        if (!player.getAbilities().instabuild) {
            player.levelDown(-this.cost.get());
        }

        this.inputSlots.setItem(0, ItemStack.EMPTY);
        if (this.repairItemCountCost > 0) {
            ItemStack itemStack = this.inputSlots.getItem(1);
            if (!itemStack.isEmpty() && itemStack.getCount() > this.repairItemCountCost) {
                itemStack.subtract(this.repairItemCountCost);
                this.inputSlots.setItem(1, itemStack);
            } else {
                this.inputSlots.setItem(1, ItemStack.EMPTY);
            }
        } else {
            this.inputSlots.setItem(1, ItemStack.EMPTY);
        }

        this.cost.set(0);
        this.access.execute((world, pos) -> {
            IBlockData blockState = world.getType(pos);
            if (!player.getAbilities().instabuild && blockState.is(TagsBlock.ANVIL) && player.getRandom().nextFloat() < 0.12F) {
                IBlockData blockState2 = BlockAnvil.damage(blockState);
                if (blockState2 == null) {
                    world.removeBlock(pos, false);
                    world.triggerEffect(1029, pos, 0);
                } else {
                    world.setTypeAndData(pos, blockState2, 2);
                    world.triggerEffect(1030, pos, 0);
                }
            } else {
                world.triggerEffect(1030, pos, 0);
            }

        });
    }

    @Override
    public void createResult() {
        ItemStack itemStack = this.inputSlots.getItem(0);
        this.cost.set(1);
        int i = 0;
        int j = 0;
        int k = 0;
        if (itemStack.isEmpty()) {
            this.resultSlots.setItem(0, ItemStack.EMPTY);
            this.cost.set(0);
        } else {
            ItemStack itemStack2 = itemStack.cloneItemStack();
            ItemStack itemStack3 = this.inputSlots.getItem(1);
            Map<Enchantment, Integer> map = EnchantmentManager.getEnchantments(itemStack2);
            j = j + itemStack.getRepairCost() + (itemStack3.isEmpty() ? 0 : itemStack3.getRepairCost());
            this.repairItemCountCost = 0;
            if (!itemStack3.isEmpty()) {
                boolean bl = itemStack3.is(Items.ENCHANTED_BOOK) && !ItemEnchantedBook.getEnchantments(itemStack3).isEmpty();
                if (itemStack2.isDamageableItem() && itemStack2.getItem().isValidRepairItem(itemStack, itemStack3)) {
                    int l = Math.min(itemStack2.getDamage(), itemStack2.getMaxDamage() / 4);
                    if (l <= 0) {
                        this.resultSlots.setItem(0, ItemStack.EMPTY);
                        this.cost.set(0);
                        return;
                    }

                    int m;
                    for(m = 0; l > 0 && m < itemStack3.getCount(); ++m) {
                        int n = itemStack2.getDamage() - l;
                        itemStack2.setDamage(n);
                        ++i;
                        l = Math.min(itemStack2.getDamage(), itemStack2.getMaxDamage() / 4);
                    }

                    this.repairItemCountCost = m;
                } else {
                    if (!bl && (!itemStack2.is(itemStack3.getItem()) || !itemStack2.isDamageableItem())) {
                        this.resultSlots.setItem(0, ItemStack.EMPTY);
                        this.cost.set(0);
                        return;
                    }

                    if (itemStack2.isDamageableItem() && !bl) {
                        int o = itemStack.getMaxDamage() - itemStack.getDamage();
                        int p = itemStack3.getMaxDamage() - itemStack3.getDamage();
                        int q = p + itemStack2.getMaxDamage() * 12 / 100;
                        int r = o + q;
                        int s = itemStack2.getMaxDamage() - r;
                        if (s < 0) {
                            s = 0;
                        }

                        if (s < itemStack2.getDamage()) {
                            itemStack2.setDamage(s);
                            i += 2;
                        }
                    }

                    Map<Enchantment, Integer> map2 = EnchantmentManager.getEnchantments(itemStack3);
                    boolean bl2 = false;
                    boolean bl3 = false;

                    for(Enchantment enchantment : map2.keySet()) {
                        if (enchantment != null) {
                            int t = map.getOrDefault(enchantment, 0);
                            int u = map2.get(enchantment);
                            u = t == u ? u + 1 : Math.max(u, t);
                            boolean bl4 = enchantment.canEnchant(itemStack);
                            if (this.player.getAbilities().instabuild || itemStack.is(Items.ENCHANTED_BOOK)) {
                                bl4 = true;
                            }

                            for(Enchantment enchantment2 : map.keySet()) {
                                if (enchantment2 != enchantment && !enchantment.isCompatible(enchantment2)) {
                                    bl4 = false;
                                    ++i;
                                }
                            }

                            if (!bl4) {
                                bl3 = true;
                            } else {
                                bl2 = true;
                                if (u > enchantment.getMaxLevel()) {
                                    u = enchantment.getMaxLevel();
                                }

                                map.put(enchantment, u);
                                int v = 0;
                                switch(enchantment.getRarity()) {
                                case COMMON:
                                    v = 1;
                                    break;
                                case UNCOMMON:
                                    v = 2;
                                    break;
                                case RARE:
                                    v = 4;
                                    break;
                                case VERY_RARE:
                                    v = 8;
                                }

                                if (bl) {
                                    v = Math.max(1, v / 2);
                                }

                                i += v * u;
                                if (itemStack.getCount() > 1) {
                                    i = 40;
                                }
                            }
                        }
                    }

                    if (bl3 && !bl2) {
                        this.resultSlots.setItem(0, ItemStack.EMPTY);
                        this.cost.set(0);
                        return;
                    }
                }
            }

            if (StringUtils.isBlank(this.itemName)) {
                if (itemStack.hasName()) {
                    k = 1;
                    i += k;
                    itemStack2.resetHoverName();
                }
            } else if (!this.itemName.equals(itemStack.getName().getString())) {
                k = 1;
                i += k;
                itemStack2.setHoverName(new ChatComponentText(this.itemName));
            }

            this.cost.set(j + i);
            if (i <= 0) {
                itemStack2 = ItemStack.EMPTY;
            }

            if (k == i && k > 0 && this.cost.get() >= 40) {
                this.cost.set(39);
            }

            if (this.cost.get() >= 40 && !this.player.getAbilities().instabuild) {
                itemStack2 = ItemStack.EMPTY;
            }

            if (!itemStack2.isEmpty()) {
                int w = itemStack2.getRepairCost();
                if (!itemStack3.isEmpty() && w < itemStack3.getRepairCost()) {
                    w = itemStack3.getRepairCost();
                }

                if (k != i || k == 0) {
                    w = calculateIncreasedRepairCost(w);
                }

                itemStack2.setRepairCost(w);
                EnchantmentManager.setEnchantments(map, itemStack2);
            }

            this.resultSlots.setItem(0, itemStack2);
            this.broadcastChanges();
        }
    }

    public static int calculateIncreasedRepairCost(int cost) {
        return cost * 2 + 1;
    }

    public void setItemName(String newItemName) {
        this.itemName = newItemName;
        if (this.getSlot(2).hasItem()) {
            ItemStack itemStack = this.getSlot(2).getItem();
            if (StringUtils.isBlank(newItemName)) {
                itemStack.resetHoverName();
            } else {
                itemStack.setHoverName(new ChatComponentText(this.itemName));
            }
        }

        this.createResult();
    }

    public int getCost() {
        return this.cost.get();
    }
}
