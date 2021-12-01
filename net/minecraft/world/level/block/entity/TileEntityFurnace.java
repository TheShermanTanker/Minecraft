package net.minecraft.world.level.block.entity;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagsItem;
import net.minecraft.util.MathHelper;
import net.minecraft.world.ContainerUtil;
import net.minecraft.world.IInventory;
import net.minecraft.world.IWorldInventory;
import net.minecraft.world.entity.EntityExperienceOrb;
import net.minecraft.world.entity.player.AutoRecipeStackManager;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.inventory.AutoRecipeOutput;
import net.minecraft.world.inventory.IContainerProperties;
import net.minecraft.world.inventory.RecipeHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.IRecipe;
import net.minecraft.world.item.crafting.RecipeCooking;
import net.minecraft.world.item.crafting.Recipes;
import net.minecraft.world.level.IMaterial;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.BlockFurnace;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.Vec3D;

public abstract class TileEntityFurnace extends TileEntityContainer implements IWorldInventory, RecipeHolder, AutoRecipeOutput {
    protected static final int SLOT_INPUT = 0;
    protected static final int SLOT_FUEL = 1;
    protected static final int SLOT_RESULT = 2;
    public static final int DATA_LIT_TIME = 0;
    private static final int[] SLOTS_FOR_UP = new int[]{0};
    private static final int[] SLOTS_FOR_DOWN = new int[]{2, 1};
    private static final int[] SLOTS_FOR_SIDES = new int[]{1};
    public static final int DATA_LIT_DURATION = 1;
    public static final int DATA_COOKING_PROGRESS = 2;
    public static final int DATA_COOKING_TOTAL_TIME = 3;
    public static final int NUM_DATA_VALUES = 4;
    public static final int BURN_TIME_STANDARD = 200;
    public static final int BURN_COOL_SPEED = 2;
    protected NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
    public int litTime;
    int litDuration;
    public int cookingProgress;
    public int cookingTotalTime;
    protected final IContainerProperties dataAccess = new IContainerProperties() {
        @Override
        public int getProperty(int index) {
            switch(index) {
            case 0:
                return TileEntityFurnace.this.litTime;
            case 1:
                return TileEntityFurnace.this.litDuration;
            case 2:
                return TileEntityFurnace.this.cookingProgress;
            case 3:
                return TileEntityFurnace.this.cookingTotalTime;
            default:
                return 0;
            }
        }

        @Override
        public void setProperty(int index, int value) {
            switch(index) {
            case 0:
                TileEntityFurnace.this.litTime = value;
                break;
            case 1:
                TileEntityFurnace.this.litDuration = value;
                break;
            case 2:
                TileEntityFurnace.this.cookingProgress = value;
                break;
            case 3:
                TileEntityFurnace.this.cookingTotalTime = value;
            }

        }

        @Override
        public int getCount() {
            return 4;
        }
    };
    private final Object2IntOpenHashMap<MinecraftKey> recipesUsed = new Object2IntOpenHashMap<>();
    public final Recipes<? extends RecipeCooking> recipeType;

    protected TileEntityFurnace(TileEntityTypes<?> blockEntityType, BlockPosition pos, IBlockData state, Recipes<? extends RecipeCooking> recipeType) {
        super(blockEntityType, pos, state);
        this.recipeType = recipeType;
    }

    public static Map<Item, Integer> getFuel() {
        Map<Item, Integer> map = Maps.newLinkedHashMap();
        add(map, Items.LAVA_BUCKET, 20000);
        add(map, Blocks.COAL_BLOCK, 16000);
        add(map, Items.BLAZE_ROD, 2400);
        add(map, Items.COAL, 1600);
        add(map, Items.CHARCOAL, 1600);
        add(map, TagsItem.LOGS, 300);
        add(map, TagsItem.PLANKS, 300);
        add(map, TagsItem.WOODEN_STAIRS, 300);
        add(map, TagsItem.WOODEN_SLABS, 150);
        add(map, TagsItem.WOODEN_TRAPDOORS, 300);
        add(map, TagsItem.WOODEN_PRESSURE_PLATES, 300);
        add(map, Blocks.OAK_FENCE, 300);
        add(map, Blocks.BIRCH_FENCE, 300);
        add(map, Blocks.SPRUCE_FENCE, 300);
        add(map, Blocks.JUNGLE_FENCE, 300);
        add(map, Blocks.DARK_OAK_FENCE, 300);
        add(map, Blocks.ACACIA_FENCE, 300);
        add(map, Blocks.OAK_FENCE_GATE, 300);
        add(map, Blocks.BIRCH_FENCE_GATE, 300);
        add(map, Blocks.SPRUCE_FENCE_GATE, 300);
        add(map, Blocks.JUNGLE_FENCE_GATE, 300);
        add(map, Blocks.DARK_OAK_FENCE_GATE, 300);
        add(map, Blocks.ACACIA_FENCE_GATE, 300);
        add(map, Blocks.NOTE_BLOCK, 300);
        add(map, Blocks.BOOKSHELF, 300);
        add(map, Blocks.LECTERN, 300);
        add(map, Blocks.JUKEBOX, 300);
        add(map, Blocks.CHEST, 300);
        add(map, Blocks.TRAPPED_CHEST, 300);
        add(map, Blocks.CRAFTING_TABLE, 300);
        add(map, Blocks.DAYLIGHT_DETECTOR, 300);
        add(map, TagsItem.BANNERS, 300);
        add(map, Items.BOW, 300);
        add(map, Items.FISHING_ROD, 300);
        add(map, Blocks.LADDER, 300);
        add(map, TagsItem.SIGNS, 200);
        add(map, Items.WOODEN_SHOVEL, 200);
        add(map, Items.WOODEN_SWORD, 200);
        add(map, Items.WOODEN_HOE, 200);
        add(map, Items.WOODEN_AXE, 200);
        add(map, Items.WOODEN_PICKAXE, 200);
        add(map, TagsItem.WOODEN_DOORS, 200);
        add(map, TagsItem.BOATS, 1200);
        add(map, TagsItem.WOOL, 100);
        add(map, TagsItem.WOODEN_BUTTONS, 100);
        add(map, Items.STICK, 100);
        add(map, TagsItem.SAPLINGS, 100);
        add(map, Items.BOWL, 100);
        add(map, TagsItem.CARPETS, 67);
        add(map, Blocks.DRIED_KELP_BLOCK, 4001);
        add(map, Items.CROSSBOW, 300);
        add(map, Blocks.BAMBOO, 50);
        add(map, Blocks.DEAD_BUSH, 100);
        add(map, Blocks.SCAFFOLDING, 400);
        add(map, Blocks.LOOM, 300);
        add(map, Blocks.BARREL, 300);
        add(map, Blocks.CARTOGRAPHY_TABLE, 300);
        add(map, Blocks.FLETCHING_TABLE, 300);
        add(map, Blocks.SMITHING_TABLE, 300);
        add(map, Blocks.COMPOSTER, 300);
        add(map, Blocks.AZALEA, 100);
        add(map, Blocks.FLOWERING_AZALEA, 100);
        return map;
    }

    private static boolean isNeverAFurnaceFuel(Item item) {
        return TagsItem.NON_FLAMMABLE_WOOD.isTagged(item);
    }

    private static void add(Map<Item, Integer> fuelTimes, Tag<Item> tag, int fuelTime) {
        for(Item item : tag.getTagged()) {
            if (!isNeverAFurnaceFuel(item)) {
                fuelTimes.put(item, fuelTime);
            }
        }

    }

    private static void add(Map<Item, Integer> fuelTimes, IMaterial item, int fuelTime) {
        Item item2 = item.getItem();
        if (isNeverAFurnaceFuel(item2)) {
            if (SharedConstants.IS_RUNNING_IN_IDE) {
                throw (IllegalStateException)SystemUtils.pauseInIde(new IllegalStateException("A developer tried to explicitly make fire resistant item " + item2.getName((ItemStack)null).getString() + " a furnace fuel. That will not work!"));
            }
        } else {
            fuelTimes.put(item2, fuelTime);
        }
    }

    private boolean isBurning() {
        return this.litTime > 0;
    }

    @Override
    public void load(NBTTagCompound nbt) {
        super.load(nbt);
        this.items = NonNullList.withSize(this.getSize(), ItemStack.EMPTY);
        ContainerUtil.loadAllItems(nbt, this.items);
        this.litTime = nbt.getShort("BurnTime");
        this.cookingProgress = nbt.getShort("CookTime");
        this.cookingTotalTime = nbt.getShort("CookTimeTotal");
        this.litDuration = this.fuelTime(this.items.get(1));
        NBTTagCompound compoundTag = nbt.getCompound("RecipesUsed");

        for(String string : compoundTag.getKeys()) {
            this.recipesUsed.put(new MinecraftKey(string), compoundTag.getInt(string));
        }

    }

    @Override
    protected void saveAdditional(NBTTagCompound nbt) {
        super.saveAdditional(nbt);
        nbt.setShort("BurnTime", (short)this.litTime);
        nbt.setShort("CookTime", (short)this.cookingProgress);
        nbt.setShort("CookTimeTotal", (short)this.cookingTotalTime);
        ContainerUtil.saveAllItems(nbt, this.items);
        NBTTagCompound compoundTag = new NBTTagCompound();
        this.recipesUsed.forEach((identifier, count) -> {
            compoundTag.setInt(identifier.toString(), count);
        });
        nbt.set("RecipesUsed", compoundTag);
    }

    public static void serverTick(World world, BlockPosition pos, IBlockData state, TileEntityFurnace blockEntity) {
        boolean bl = blockEntity.isBurning();
        boolean bl2 = false;
        if (blockEntity.isBurning()) {
            --blockEntity.litTime;
        }

        ItemStack itemStack = blockEntity.items.get(1);
        if (blockEntity.isBurning() || !itemStack.isEmpty() && !blockEntity.items.get(0).isEmpty()) {
            IRecipe<?> recipe = world.getCraftingManager().craft(blockEntity.recipeType, blockEntity, world).orElse((RecipeCooking)null);
            int i = blockEntity.getMaxStackSize();
            if (!blockEntity.isBurning() && canBurn(recipe, blockEntity.items, i)) {
                blockEntity.litTime = blockEntity.fuelTime(itemStack);
                blockEntity.litDuration = blockEntity.litTime;
                if (blockEntity.isBurning()) {
                    bl2 = true;
                    if (!itemStack.isEmpty()) {
                        Item item = itemStack.getItem();
                        itemStack.subtract(1);
                        if (itemStack.isEmpty()) {
                            Item item2 = item.getCraftingRemainingItem();
                            blockEntity.items.set(1, item2 == null ? ItemStack.EMPTY : new ItemStack(item2));
                        }
                    }
                }
            }

            if (blockEntity.isBurning() && canBurn(recipe, blockEntity.items, i)) {
                ++blockEntity.cookingProgress;
                if (blockEntity.cookingProgress == blockEntity.cookingTotalTime) {
                    blockEntity.cookingProgress = 0;
                    blockEntity.cookingTotalTime = getRecipeCookingTime(world, blockEntity.recipeType, blockEntity);
                    if (burn(recipe, blockEntity.items, i)) {
                        blockEntity.setRecipeUsed(recipe);
                    }

                    bl2 = true;
                }
            } else {
                blockEntity.cookingProgress = 0;
            }
        } else if (!blockEntity.isBurning() && blockEntity.cookingProgress > 0) {
            blockEntity.cookingProgress = MathHelper.clamp(blockEntity.cookingProgress - 2, 0, blockEntity.cookingTotalTime);
        }

        if (bl != blockEntity.isBurning()) {
            bl2 = true;
            state = state.set(BlockFurnace.LIT, Boolean.valueOf(blockEntity.isBurning()));
            world.setTypeAndData(pos, state, 3);
        }

        if (bl2) {
            setChanged(world, pos, state);
        }

    }

    private static boolean canBurn(@Nullable IRecipe<?> recipe, NonNullList<ItemStack> slots, int count) {
        if (!slots.get(0).isEmpty() && recipe != null) {
            ItemStack itemStack = recipe.getResult();
            if (itemStack.isEmpty()) {
                return false;
            } else {
                ItemStack itemStack2 = slots.get(2);
                if (itemStack2.isEmpty()) {
                    return true;
                } else if (!itemStack2.doMaterialsMatch(itemStack)) {
                    return false;
                } else if (itemStack2.getCount() < count && itemStack2.getCount() < itemStack2.getMaxStackSize()) {
                    return true;
                } else {
                    return itemStack2.getCount() < itemStack.getMaxStackSize();
                }
            }
        } else {
            return false;
        }
    }

    private static boolean burn(@Nullable IRecipe<?> recipe, NonNullList<ItemStack> slots, int count) {
        if (recipe != null && canBurn(recipe, slots, count)) {
            ItemStack itemStack = slots.get(0);
            ItemStack itemStack2 = recipe.getResult();
            ItemStack itemStack3 = slots.get(2);
            if (itemStack3.isEmpty()) {
                slots.set(2, itemStack2.cloneItemStack());
            } else if (itemStack3.is(itemStack2.getItem())) {
                itemStack3.add(1);
            }

            if (itemStack.is(Blocks.WET_SPONGE.getItem()) && !slots.get(1).isEmpty() && slots.get(1).is(Items.BUCKET)) {
                slots.set(1, new ItemStack(Items.WATER_BUCKET));
            }

            itemStack.subtract(1);
            return true;
        } else {
            return false;
        }
    }

    protected int fuelTime(ItemStack fuel) {
        if (fuel.isEmpty()) {
            return 0;
        } else {
            Item item = fuel.getItem();
            return getFuel().getOrDefault(item, 0);
        }
    }

    private static int getRecipeCookingTime(World world, Recipes<? extends RecipeCooking> recipeType, IInventory inventory) {
        return world.getCraftingManager().craft(recipeType, inventory, world).map(RecipeCooking::getCookingTime).orElse(200);
    }

    public static boolean isFuel(ItemStack stack) {
        return getFuel().containsKey(stack.getItem());
    }

    @Override
    public int[] getSlotsForFace(EnumDirection side) {
        if (side == EnumDirection.DOWN) {
            return SLOTS_FOR_DOWN;
        } else {
            return side == EnumDirection.UP ? SLOTS_FOR_UP : SLOTS_FOR_SIDES;
        }
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable EnumDirection dir) {
        return this.canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, EnumDirection dir) {
        if (dir == EnumDirection.DOWN && slot == 1) {
            return stack.is(Items.WATER_BUCKET) || stack.is(Items.BUCKET);
        } else {
            return true;
        }
    }

    @Override
    public int getSize() {
        return this.items.size();
    }

    @Override
    public boolean isEmpty() {
        for(ItemStack itemStack : this.items) {
            if (!itemStack.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    @Override
    public ItemStack splitStack(int slot, int amount) {
        return ContainerUtil.removeItem(this.items, slot, amount);
    }

    @Override
    public ItemStack splitWithoutUpdate(int slot) {
        return ContainerUtil.takeItem(this.items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        ItemStack itemStack = this.items.get(slot);
        boolean bl = !stack.isEmpty() && stack.doMaterialsMatch(itemStack) && ItemStack.equals(stack, itemStack);
        this.items.set(slot, stack);
        if (stack.getCount() > this.getMaxStackSize()) {
            stack.setCount(this.getMaxStackSize());
        }

        if (slot == 0 && !bl) {
            this.cookingTotalTime = getRecipeCookingTime(this.level, this.recipeType, this);
            this.cookingProgress = 0;
            this.update();
        }

    }

    @Override
    public boolean stillValid(EntityHuman player) {
        if (this.level.getTileEntity(this.worldPosition) != this) {
            return false;
        } else {
            return player.distanceToSqr((double)this.worldPosition.getX() + 0.5D, (double)this.worldPosition.getY() + 0.5D, (double)this.worldPosition.getZ() + 0.5D) <= 64.0D;
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == 2) {
            return false;
        } else if (slot != 1) {
            return true;
        } else {
            ItemStack itemStack = this.items.get(1);
            return isFuel(stack) || stack.is(Items.BUCKET) && !itemStack.is(Items.BUCKET);
        }
    }

    @Override
    public void clear() {
        this.items.clear();
    }

    @Override
    public void setRecipeUsed(@Nullable IRecipe<?> recipe) {
        if (recipe != null) {
            MinecraftKey resourceLocation = recipe.getKey();
            this.recipesUsed.addTo(resourceLocation, 1);
        }

    }

    @Nullable
    @Override
    public IRecipe<?> getRecipeUsed() {
        return null;
    }

    @Override
    public void awardUsedRecipes(EntityHuman player) {
    }

    public void awardUsedRecipesAndPopExperience(EntityPlayer player) {
        List<IRecipe<?>> list = this.getRecipesToAwardAndPopExperience(player.getWorldServer(), player.getPositionVector());
        player.discoverRecipes(list);
        this.recipesUsed.clear();
    }

    public List<IRecipe<?>> getRecipesToAwardAndPopExperience(WorldServer world, Vec3D pos) {
        List<IRecipe<?>> list = Lists.newArrayList();

        for(Entry<MinecraftKey> entry : this.recipesUsed.object2IntEntrySet()) {
            world.getCraftingManager().getRecipe(entry.getKey()).ifPresent((recipe) -> {
                list.add(recipe);
                createExperience(world, pos, entry.getIntValue(), ((RecipeCooking)recipe).getExperience());
            });
        }

        return list;
    }

    private static void createExperience(WorldServer world, Vec3D pos, int multiplier, float experience) {
        int i = MathHelper.floor((float)multiplier * experience);
        float f = MathHelper.frac((float)multiplier * experience);
        if (f != 0.0F && Math.random() < (double)f) {
            ++i;
        }

        EntityExperienceOrb.award(world, pos, i);
    }

    @Override
    public void fillStackedContents(AutoRecipeStackManager finder) {
        for(ItemStack itemStack : this.items) {
            finder.accountStack(itemStack);
        }

    }
}
