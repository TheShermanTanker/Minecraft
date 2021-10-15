package net.minecraft.world.level.block.entity;

import java.util.Arrays;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.world.ContainerUtil;
import net.minecraft.world.IWorldInventory;
import net.minecraft.world.InventoryUtils;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.inventory.ContainerBrewingStand;
import net.minecraft.world.inventory.IContainerProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewer;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.BlockBrewingStand;
import net.minecraft.world.level.block.state.IBlockData;

public class TileEntityBrewingStand extends TileEntityContainer implements IWorldInventory {
    private static final int INGREDIENT_SLOT = 3;
    private static final int FUEL_SLOT = 4;
    private static final int[] SLOTS_FOR_UP = new int[]{3};
    private static final int[] SLOTS_FOR_DOWN = new int[]{0, 1, 2, 3};
    private static final int[] SLOTS_FOR_SIDES = new int[]{0, 1, 2, 4};
    public static final int FUEL_USES = 20;
    public static final int DATA_BREW_TIME = 0;
    public static final int DATA_FUEL_USES = 1;
    public static final int NUM_DATA_VALUES = 2;
    private NonNullList<ItemStack> items = NonNullList.withSize(5, ItemStack.EMPTY);
    public int brewTime;
    private boolean[] lastPotionCount;
    private Item ingredient;
    public int fuel;
    protected final IContainerProperties dataAccess = new IContainerProperties() {
        @Override
        public int getProperty(int index) {
            switch(index) {
            case 0:
                return TileEntityBrewingStand.this.brewTime;
            case 1:
                return TileEntityBrewingStand.this.fuel;
            default:
                return 0;
            }
        }

        @Override
        public void setProperty(int index, int value) {
            switch(index) {
            case 0:
                TileEntityBrewingStand.this.brewTime = value;
                break;
            case 1:
                TileEntityBrewingStand.this.fuel = value;
            }

        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public TileEntityBrewingStand(BlockPosition pos, IBlockData state) {
        super(TileEntityTypes.BREWING_STAND, pos, state);
    }

    @Override
    protected IChatBaseComponent getContainerName() {
        return new ChatMessage("container.brewing");
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

    public static void serverTick(World world, BlockPosition pos, IBlockData state, TileEntityBrewingStand blockEntity) {
        ItemStack itemStack = blockEntity.items.get(4);
        if (blockEntity.fuel <= 0 && itemStack.is(Items.BLAZE_POWDER)) {
            blockEntity.fuel = 20;
            itemStack.subtract(1);
            setChanged(world, pos, state);
        }

        boolean bl = isBrewable(blockEntity.items);
        boolean bl2 = blockEntity.brewTime > 0;
        ItemStack itemStack2 = blockEntity.items.get(3);
        if (bl2) {
            --blockEntity.brewTime;
            boolean bl3 = blockEntity.brewTime == 0;
            if (bl3 && bl) {
                doBrew(world, pos, blockEntity.items);
                setChanged(world, pos, state);
            } else if (!bl || !itemStack2.is(blockEntity.ingredient)) {
                blockEntity.brewTime = 0;
                setChanged(world, pos, state);
            }
        } else if (bl && blockEntity.fuel > 0) {
            --blockEntity.fuel;
            blockEntity.brewTime = 400;
            blockEntity.ingredient = itemStack2.getItem();
            setChanged(world, pos, state);
        }

        boolean[] bls = blockEntity.getPotionBits();
        if (!Arrays.equals(bls, blockEntity.lastPotionCount)) {
            blockEntity.lastPotionCount = bls;
            IBlockData blockState = state;
            if (!(state.getBlock() instanceof BlockBrewingStand)) {
                return;
            }

            for(int i = 0; i < BlockBrewingStand.HAS_BOTTLE.length; ++i) {
                blockState = blockState.set(BlockBrewingStand.HAS_BOTTLE[i], Boolean.valueOf(bls[i]));
            }

            world.setTypeAndData(pos, blockState, 2);
        }

    }

    private boolean[] getPotionBits() {
        boolean[] bls = new boolean[3];

        for(int i = 0; i < 3; ++i) {
            if (!this.items.get(i).isEmpty()) {
                bls[i] = true;
            }
        }

        return bls;
    }

    private static boolean isBrewable(NonNullList<ItemStack> slots) {
        ItemStack itemStack = slots.get(3);
        if (itemStack.isEmpty()) {
            return false;
        } else if (!PotionBrewer.isIngredient(itemStack)) {
            return false;
        } else {
            for(int i = 0; i < 3; ++i) {
                ItemStack itemStack2 = slots.get(i);
                if (!itemStack2.isEmpty() && PotionBrewer.hasMix(itemStack2, itemStack)) {
                    return true;
                }
            }

            return false;
        }
    }

    private static void doBrew(World world, BlockPosition pos, NonNullList<ItemStack> slots) {
        ItemStack itemStack = slots.get(3);

        for(int i = 0; i < 3; ++i) {
            slots.set(i, PotionBrewer.mix(itemStack, slots.get(i)));
        }

        itemStack.subtract(1);
        if (itemStack.getItem().hasCraftingRemainingItem()) {
            ItemStack itemStack2 = new ItemStack(itemStack.getItem().getCraftingRemainingItem());
            if (itemStack.isEmpty()) {
                itemStack = itemStack2;
            } else {
                InventoryUtils.dropItem(world, (double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), itemStack2);
            }
        }

        slots.set(3, itemStack);
        world.triggerEffect(1035, pos, 0);
    }

    @Override
    public void load(NBTTagCompound nbt) {
        super.load(nbt);
        this.items = NonNullList.withSize(this.getSize(), ItemStack.EMPTY);
        ContainerUtil.loadAllItems(nbt, this.items);
        this.brewTime = nbt.getShort("BrewTime");
        this.fuel = nbt.getByte("Fuel");
    }

    @Override
    public NBTTagCompound save(NBTTagCompound nbt) {
        super.save(nbt);
        nbt.setShort("BrewTime", (short)this.brewTime);
        ContainerUtil.saveAllItems(nbt, this.items);
        nbt.setByte("Fuel", (byte)this.fuel);
        return nbt;
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < this.items.size() ? this.items.get(slot) : ItemStack.EMPTY;
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
        if (slot >= 0 && slot < this.items.size()) {
            this.items.set(slot, stack);
        }

    }

    @Override
    public boolean stillValid(EntityHuman player) {
        if (this.level.getTileEntity(this.worldPosition) != this) {
            return false;
        } else {
            return !(player.distanceToSqr((double)this.worldPosition.getX() + 0.5D, (double)this.worldPosition.getY() + 0.5D, (double)this.worldPosition.getZ() + 0.5D) > 64.0D);
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == 3) {
            return PotionBrewer.isIngredient(stack);
        } else if (slot == 4) {
            return stack.is(Items.BLAZE_POWDER);
        } else {
            return (stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION) || stack.is(Items.GLASS_BOTTLE)) && this.getItem(slot).isEmpty();
        }
    }

    @Override
    public int[] getSlotsForFace(EnumDirection side) {
        if (side == EnumDirection.UP) {
            return SLOTS_FOR_UP;
        } else {
            return side == EnumDirection.DOWN ? SLOTS_FOR_DOWN : SLOTS_FOR_SIDES;
        }
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable EnumDirection dir) {
        return this.canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, EnumDirection dir) {
        return slot == 3 ? stack.is(Items.GLASS_BOTTLE) : true;
    }

    @Override
    public void clear() {
        this.items.clear();
    }

    @Override
    protected Container createContainer(int syncId, PlayerInventory playerInventory) {
        return new ContainerBrewingStand(syncId, playerInventory, this, this.dataAccess);
    }
}
