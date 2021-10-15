package net.minecraft.world.inventory;

import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.InventorySubcontainer;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.TileEntityEnderChest;

public class InventoryEnderChest extends InventorySubcontainer {
    @Nullable
    private TileEntityEnderChest activeChest;

    public InventoryEnderChest() {
        super(27);
    }

    public void setActiveChest(TileEntityEnderChest blockEntity) {
        this.activeChest = blockEntity;
    }

    public boolean isActiveChest(TileEntityEnderChest blockEntity) {
        return this.activeChest == blockEntity;
    }

    @Override
    public void fromTag(NBTTagList nbtList) {
        for(int i = 0; i < this.getSize(); ++i) {
            this.setItem(i, ItemStack.EMPTY);
        }

        for(int j = 0; j < nbtList.size(); ++j) {
            NBTTagCompound compoundTag = nbtList.getCompound(j);
            int k = compoundTag.getByte("Slot") & 255;
            if (k >= 0 && k < this.getSize()) {
                this.setItem(k, ItemStack.of(compoundTag));
            }
        }

    }

    @Override
    public NBTTagList createTag() {
        NBTTagList listTag = new NBTTagList();

        for(int i = 0; i < this.getSize(); ++i) {
            ItemStack itemStack = this.getItem(i);
            if (!itemStack.isEmpty()) {
                NBTTagCompound compoundTag = new NBTTagCompound();
                compoundTag.setByte("Slot", (byte)i);
                itemStack.save(compoundTag);
                listTag.add(compoundTag);
            }
        }

        return listTag;
    }

    @Override
    public boolean stillValid(EntityHuman player) {
        return this.activeChest != null && !this.activeChest.stillValid(player) ? false : super.stillValid(player);
    }

    @Override
    public void startOpen(EntityHuman player) {
        if (this.activeChest != null) {
            this.activeChest.startOpen(player);
        }

        super.startOpen(player);
    }

    @Override
    public void closeContainer(EntityHuman player) {
        if (this.activeChest != null) {
            this.activeChest.stopOpen(player);
        }

        super.closeContainer(player);
        this.activeChest = null;
    }
}
