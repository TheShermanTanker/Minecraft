package net.minecraft.world.level.block.entity;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.ChestLock;
import net.minecraft.world.IInventory;
import net.minecraft.world.INamableTileEntity;
import net.minecraft.world.ITileInventory;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.level.block.state.IBlockData;

public abstract class TileEntityContainer extends TileEntity implements IInventory, ITileInventory, INamableTileEntity {
    public ChestLock lockKey = ChestLock.NO_LOCK;
    public IChatBaseComponent name;

    protected TileEntityContainer(TileEntityTypes<?> type, BlockPosition pos, IBlockData state) {
        super(type, pos, state);
    }

    @Override
    public void load(NBTTagCompound nbt) {
        super.load(nbt);
        this.lockKey = ChestLock.fromTag(nbt);
        if (nbt.hasKeyOfType("CustomName", 8)) {
            this.name = IChatBaseComponent.ChatSerializer.fromJson(nbt.getString("CustomName"));
        }

    }

    @Override
    public NBTTagCompound save(NBTTagCompound nbt) {
        super.save(nbt);
        this.lockKey.addToTag(nbt);
        if (this.name != null) {
            nbt.setString("CustomName", IChatBaseComponent.ChatSerializer.toJson(this.name));
        }

        return nbt;
    }

    public void setCustomName(IChatBaseComponent customName) {
        this.name = customName;
    }

    @Override
    public IChatBaseComponent getDisplayName() {
        return this.name != null ? this.name : this.getContainerName();
    }

    @Override
    public IChatBaseComponent getScoreboardDisplayName() {
        return this.getDisplayName();
    }

    @Nullable
    @Override
    public IChatBaseComponent getCustomName() {
        return this.name;
    }

    protected abstract IChatBaseComponent getContainerName();

    public boolean canOpen(EntityHuman player) {
        return canUnlock(player, this.lockKey, this.getScoreboardDisplayName());
    }

    public static boolean canUnlock(EntityHuman player, ChestLock lock, IChatBaseComponent containerName) {
        if (!player.isSpectator() && !lock.unlocksWith(player.getItemInMainHand())) {
            player.displayClientMessage(new ChatMessage("container.isLocked", containerName), true);
            player.playNotifySound(SoundEffects.CHEST_LOCKED, EnumSoundCategory.BLOCKS, 1.0F, 1.0F);
            return false;
        } else {
            return true;
        }
    }

    @Nullable
    @Override
    public Container createMenu(int syncId, PlayerInventory inv, EntityHuman player) {
        return this.canOpen(player) ? this.createContainer(syncId, inv) : null;
    }

    protected abstract Container createContainer(int syncId, PlayerInventory playerInventory);
}
