package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.ContainerUtil;
import net.minecraft.world.IInventory;
import net.minecraft.world.InventoryLargeChest;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.inventory.ContainerChest;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockChest;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockPropertyChestType;

public class TileEntityChest extends TileEntityLootable implements LidBlockEntity {
    private static final int EVENT_SET_OPEN_COUNT = 1;
    private NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
    public final ContainerOpenersCounter openersCounter = new ContainerOpenersCounter() {
        @Override
        protected void onOpen(World world, BlockPosition pos, IBlockData state) {
            TileEntityChest.playOpenSound(world, pos, state, SoundEffects.CHEST_OPEN);
        }

        @Override
        protected void onClose(World world, BlockPosition pos, IBlockData state) {
            TileEntityChest.playOpenSound(world, pos, state, SoundEffects.CHEST_CLOSE);
        }

        @Override
        protected void openerCountChanged(World world, BlockPosition pos, IBlockData state, int oldViewerCount, int newViewerCount) {
            TileEntityChest.this.signalOpenCount(world, pos, state, oldViewerCount, newViewerCount);
        }

        @Override
        protected boolean isOwnContainer(EntityHuman player) {
            if (!(player.containerMenu instanceof ContainerChest)) {
                return false;
            } else {
                IInventory container = ((ContainerChest)player.containerMenu).getContainer();
                return container == TileEntityChest.this || container instanceof InventoryLargeChest && ((InventoryLargeChest)container).contains(TileEntityChest.this);
            }
        }
    };
    private final ChestLidController chestLidController = new ChestLidController();

    protected TileEntityChest(TileEntityTypes<?> type, BlockPosition pos, IBlockData state) {
        super(type, pos, state);
    }

    public TileEntityChest(BlockPosition pos, IBlockData state) {
        this(TileEntityTypes.CHEST, pos, state);
    }

    @Override
    public int getSize() {
        return 27;
    }

    @Override
    protected IChatBaseComponent getContainerName() {
        return new ChatMessage("container.chest");
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

    public static void lidAnimateTick(World world, BlockPosition pos, IBlockData state, TileEntityChest blockEntity) {
        blockEntity.chestLidController.tickLid();
    }

    public static void playOpenSound(World world, BlockPosition pos, IBlockData state, SoundEffect soundEvent) {
        BlockPropertyChestType chestType = state.get(BlockChest.TYPE);
        if (chestType != BlockPropertyChestType.LEFT) {
            double d = (double)pos.getX() + 0.5D;
            double e = (double)pos.getY() + 0.5D;
            double f = (double)pos.getZ() + 0.5D;
            if (chestType == BlockPropertyChestType.RIGHT) {
                EnumDirection direction = BlockChest.getConnectedDirection(state);
                d += (double)direction.getAdjacentX() * 0.5D;
                f += (double)direction.getAdjacentZ() * 0.5D;
            }

            world.playSound((EntityHuman)null, d, e, f, soundEvent, SoundCategory.BLOCKS, 0.5F, world.random.nextFloat() * 0.1F + 0.9F);
        }
    }

    @Override
    public boolean setProperty(int type, int data) {
        if (type == 1) {
            this.chestLidController.shouldBeOpen(data > 0);
            return true;
        } else {
            return super.setProperty(type, data);
        }
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

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> list) {
        this.items = list;
    }

    @Override
    public float getOpenNess(float tickDelta) {
        return this.chestLidController.getOpenness(tickDelta);
    }

    public static int getOpenCount(IBlockAccess world, BlockPosition pos) {
        IBlockData blockState = world.getType(pos);
        if (blockState.isTileEntity()) {
            TileEntity blockEntity = world.getTileEntity(pos);
            if (blockEntity instanceof TileEntityChest) {
                return ((TileEntityChest)blockEntity).openersCounter.getOpenerCount();
            }
        }

        return 0;
    }

    public static void swapContents(TileEntityChest from, TileEntityChest to) {
        NonNullList<ItemStack> nonNullList = from.getItems();
        from.setItems(to.getItems());
        to.setItems(nonNullList);
    }

    @Override
    protected Container createContainer(int syncId, PlayerInventory playerInventory) {
        return ContainerChest.threeRows(syncId, playerInventory, this);
    }

    public void recheckOpen() {
        if (!this.remove) {
            this.openersCounter.recheckOpeners(this.getWorld(), this.getPosition(), this.getBlock());
        }

    }

    protected void signalOpenCount(World world, BlockPosition pos, IBlockData state, int oldViewerCount, int newViewerCount) {
        Block block = state.getBlock();
        world.playBlockAction(pos, block, 1, newViewerCount);
    }
}
