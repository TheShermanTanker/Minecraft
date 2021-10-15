package net.minecraft.world.level.block.entity;

import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.world.ContainerUtil;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.inventory.ContainerDispenser;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.IBlockData;

public class TileEntityDispenser extends TileEntityLootable {
    private static final Random RANDOM = new Random();
    public static final int CONTAINER_SIZE = 9;
    private NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);

    protected TileEntityDispenser(TileEntityTypes<?> type, BlockPosition pos, IBlockData state) {
        super(type, pos, state);
    }

    public TileEntityDispenser(BlockPosition pos, IBlockData state) {
        this(TileEntityTypes.DISPENSER, pos, state);
    }

    @Override
    public int getSize() {
        return 9;
    }

    public int getRandomSlot() {
        this.unpackLootTable((EntityHuman)null);
        int i = -1;
        int j = 1;

        for(int k = 0; k < this.items.size(); ++k) {
            if (!this.items.get(k).isEmpty() && RANDOM.nextInt(j++) == 0) {
                i = k;
            }
        }

        return i;
    }

    public int addItem(ItemStack stack) {
        for(int i = 0; i < this.items.size(); ++i) {
            if (this.items.get(i).isEmpty()) {
                this.setItem(i, stack);
                return i;
            }
        }

        return -1;
    }

    @Override
    protected IChatBaseComponent getContainerName() {
        return new ChatMessage("container.dispenser");
    }

    @Override
    public void load(NBTTagCompound nbt) {
        super.load(nbt);
        this.items = NonNullList.withSize(this.getSize(), ItemStack.EMPTY);
        if (!this.tryLoadLootTable(nbt)) {
            ContainerUtil.loadAllItems(nbt, this.items);
        }

    }

    @Override
    public NBTTagCompound save(NBTTagCompound nbt) {
        super.save(nbt);
        if (!this.trySaveLootTable(nbt)) {
            ContainerUtil.saveAllItems(nbt, this.items);
        }

        return nbt;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> list) {
        this.items = list;
    }

    @Override
    protected Container createContainer(int syncId, PlayerInventory playerInventory) {
        return new ContainerDispenser(syncId, playerInventory, this);
    }
}
