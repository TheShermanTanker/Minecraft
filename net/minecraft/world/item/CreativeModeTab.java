package net.minecraft.world.item;

import javax.annotation.Nullable;
import net.minecraft.core.IRegistry;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.world.item.alchemy.PotionUtil;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.EnchantmentSlotType;
import net.minecraft.world.level.block.Blocks;

public abstract class CreativeModeTab {
    public static final CreativeModeTab[] TABS = new CreativeModeTab[12];
    public static final CreativeModeTab TAB_BUILDING_BLOCKS = (new CreativeModeTab(0, "buildingBlocks") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(Blocks.BRICKS);
        }
    }).setRecipeFolderName("building_blocks");
    public static final CreativeModeTab TAB_DECORATIONS = new CreativeModeTab(1, "decorations") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(Blocks.PEONY);
        }
    };
    public static final CreativeModeTab TAB_REDSTONE = new CreativeModeTab(2, "redstone") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(Items.REDSTONE);
        }
    };
    public static final CreativeModeTab TAB_TRANSPORTATION = new CreativeModeTab(3, "transportation") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(Blocks.POWERED_RAIL);
        }
    };
    public static final CreativeModeTab TAB_MISC = new CreativeModeTab(6, "misc") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(Items.LAVA_BUCKET);
        }
    };
    public static final CreativeModeTab TAB_SEARCH = (new CreativeModeTab(5, "search") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(Items.COMPASS);
        }
    }).setBackgroundSuffix("item_search.png");
    public static final CreativeModeTab TAB_FOOD = new CreativeModeTab(7, "food") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(Items.APPLE);
        }
    };
    public static final CreativeModeTab TAB_TOOLS = (new CreativeModeTab(8, "tools") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(Items.IRON_AXE);
        }
    }).setEnchantmentCategories(new EnchantmentSlotType[]{EnchantmentSlotType.VANISHABLE, EnchantmentSlotType.DIGGER, EnchantmentSlotType.FISHING_ROD, EnchantmentSlotType.BREAKABLE});
    public static final CreativeModeTab TAB_COMBAT = (new CreativeModeTab(9, "combat") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(Items.GOLDEN_SWORD);
        }
    }).setEnchantmentCategories(new EnchantmentSlotType[]{EnchantmentSlotType.VANISHABLE, EnchantmentSlotType.ARMOR, EnchantmentSlotType.ARMOR_FEET, EnchantmentSlotType.ARMOR_HEAD, EnchantmentSlotType.ARMOR_LEGS, EnchantmentSlotType.ARMOR_CHEST, EnchantmentSlotType.BOW, EnchantmentSlotType.WEAPON, EnchantmentSlotType.WEARABLE, EnchantmentSlotType.BREAKABLE, EnchantmentSlotType.TRIDENT, EnchantmentSlotType.CROSSBOW});
    public static final CreativeModeTab TAB_BREWING = new CreativeModeTab(10, "brewing") {
        @Override
        public ItemStack makeIcon() {
            return PotionUtil.setPotion(new ItemStack(Items.POTION), Potions.WATER);
        }
    };
    public static final CreativeModeTab TAB_MATERIALS = TAB_MISC;
    public static final CreativeModeTab TAB_HOTBAR = new CreativeModeTab(4, "hotbar") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(Blocks.BOOKSHELF);
        }

        @Override
        public void a(NonNullList<ItemStack> stacks) {
            throw new RuntimeException("Implement exception client-side.");
        }

        @Override
        public boolean m() {
            return true;
        }
    };
    public static final CreativeModeTab TAB_INVENTORY = (new CreativeModeTab(11, "inventory") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(Blocks.CHEST);
        }
    }).setBackgroundSuffix("inventory.png").hideScroll().hideTitle();
    private final int id;
    private final String langId;
    private final IChatBaseComponent displayName;
    private String recipeFolderName;
    private String backgroundSuffix = "items.png";
    private boolean canScroll = true;
    private boolean showTitle = true;
    private EnchantmentSlotType[] enchantmentCategories = new EnchantmentSlotType[0];
    private ItemStack iconItemStack;

    public CreativeModeTab(int index, String id) {
        this.id = index;
        this.langId = id;
        this.displayName = new ChatMessage("itemGroup." + id);
        this.iconItemStack = ItemStack.EMPTY;
        TABS[index] = this;
    }

    public int getId() {
        return this.id;
    }

    public String getRecipeFolderName() {
        return this.recipeFolderName == null ? this.langId : this.recipeFolderName;
    }

    public IChatBaseComponent getDisplayName() {
        return this.displayName;
    }

    public ItemStack getIconItem() {
        if (this.iconItemStack.isEmpty()) {
            this.iconItemStack = this.makeIcon();
        }

        return this.iconItemStack;
    }

    public abstract ItemStack makeIcon();

    public String getBackgroundSuffix() {
        return this.backgroundSuffix;
    }

    public CreativeModeTab setBackgroundSuffix(String texture) {
        this.backgroundSuffix = texture;
        return this;
    }

    public CreativeModeTab setRecipeFolderName(String name) {
        this.recipeFolderName = name;
        return this;
    }

    public boolean showTitle() {
        return this.showTitle;
    }

    public CreativeModeTab hideTitle() {
        this.showTitle = false;
        return this;
    }

    public boolean canScroll() {
        return this.canScroll;
    }

    public CreativeModeTab hideScroll() {
        this.canScroll = false;
        return this;
    }

    public int getColumn() {
        return this.id % 6;
    }

    public boolean isTopRow() {
        return this.id < 6;
    }

    public boolean m() {
        return this.getColumn() == 5;
    }

    public EnchantmentSlotType[] getEnchantmentCategories() {
        return this.enchantmentCategories;
    }

    public CreativeModeTab setEnchantmentCategories(EnchantmentSlotType... targets) {
        this.enchantmentCategories = targets;
        return this;
    }

    public boolean hasEnchantmentCategory(@Nullable EnchantmentSlotType target) {
        if (target != null) {
            for(EnchantmentSlotType enchantmentCategory : this.enchantmentCategories) {
                if (enchantmentCategory == target) {
                    return true;
                }
            }
        }

        return false;
    }

    public void a(NonNullList<ItemStack> stacks) {
        for(Item item : IRegistry.ITEM) {
            item.fillItemCategory(this, stacks);
        }

    }
}
