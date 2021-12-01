package net.minecraft.world.level.block.entity;

import net.minecraft.core.BaseBlockPosition;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.ContainerUtil;
import net.minecraft.world.IInventory;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.inventory.ContainerChest;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.BlockBarrel;
import net.minecraft.world.level.block.state.IBlockData;

public class TileEntityBarrel extends TileEntityLootable {
    private NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
    public ContainerOpenersCounter openersCounter = new ContainerOpenersCounter() {
        @Override
        protected void onOpen(World world, BlockPosition pos, IBlockData state) {
            TileEntityBarrel.this.playOpenSound(state, SoundEffects.BARREL_OPEN);
            TileEntityBarrel.this.setOpenFlag(state, true);
        }

        @Override
        protected void onClose(World world, BlockPosition pos, IBlockData state) {
            TileEntityBarrel.this.playOpenSound(state, SoundEffects.BARREL_CLOSE);
            TileEntityBarrel.this.setOpenFlag(state, false);
        }

        @Override
        protected void openerCountChanged(World world, BlockPosition pos, IBlockData state, int oldViewerCount, int newViewerCount) {
        }

        @Override
        protected boolean isOwnContainer(EntityHuman player) {
            if (player.containerMenu instanceof ContainerChest) {
                IInventory container = ((ContainerChest)player.containerMenu).getContainer();
                return container == TileEntityBarrel.this;
            } else {
                return false;
            }
        }
    };

    public TileEntityBarrel(BlockPosition pos, IBlockData state) {
        super(TileEntityTypes.BARREL, pos, state);
    }

    @Override
    protected void saveAdditional(NBTTagCompound nbt) {
        super.saveAdditional(nbt);
        if (!this.trySaveLootTable(nbt)) {
            ContainerUtil.saveAllItems(nbt, this.items);
        }

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
    public int getSize() {
        return 27;
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
    protected IChatBaseComponent getContainerName() {
        return new ChatMessage("container.barrel");
    }

    @Override
    protected Container createContainer(int syncId, PlayerInventory playerInventory) {
        return ContainerChest.threeRows(syncId, playerInventory, this);
    }

    @Override
    public void startOpen(EntityHuman player) {
        if (!this.remove && !player.isSpectator()) {
            this.openersCounter.incrementOpeners(player, this.getWorld(), this.getPosition(), this.getBlock());
        }

    }

    @Override
    public void closeContainer(EntityHuman player) {
        if (!this.remove && !player.isSpectator()) {
            this.openersCounter.decrementOpeners(player, this.getWorld(), this.getPosition(), this.getBlock());
        }

    }

    public void recheckOpen() {
        if (!this.remove) {
            this.openersCounter.recheckOpeners(this.getWorld(), this.getPosition(), this.getBlock());
        }

    }

    public void setOpenFlag(IBlockData state, boolean open) {
        this.level.setTypeAndData(this.getPosition(), state.set(BlockBarrel.OPEN, Boolean.valueOf(open)), 3);
    }

    public void playOpenSound(IBlockData state, SoundEffect soundEvent) {
        BaseBlockPosition vec3i = state.get(BlockBarrel.FACING).getNormal();
        double d = (double)this.worldPosition.getX() + 0.5D + (double)vec3i.getX() / 2.0D;
        double e = (double)this.worldPosition.getY() + 0.5D + (double)vec3i.getY() / 2.0D;
        double f = (double)this.worldPosition.getZ() + 0.5D + (double)vec3i.getZ() / 2.0D;
        this.level.playSound((EntityHuman)null, d, e, f, soundEvent, EnumSoundCategory.BLOCKS, 0.5F, this.level.random.nextFloat() * 0.1F + 0.9F);
    }
}
