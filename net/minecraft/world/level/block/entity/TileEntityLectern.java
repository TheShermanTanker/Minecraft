package net.minecraft.world.level.block.entity;

import javax.annotation.Nullable;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.ICommandListener;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.Clearable;
import net.minecraft.world.IInventory;
import net.minecraft.world.ITileInventory;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.inventory.ContainerLectern;
import net.minecraft.world.inventory.IContainerProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemWrittenBook;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BlockLectern;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.Vec2F;
import net.minecraft.world.phys.Vec3D;

public class TileEntityLectern extends TileEntity implements Clearable, ITileInventory {
    public static final int DATA_PAGE = 0;
    public static final int NUM_DATA = 1;
    public static final int SLOT_BOOK = 0;
    public static final int NUM_SLOTS = 1;
    public final IInventory bookAccess = new IInventory() {
        @Override
        public int getSize() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return TileEntityLectern.this.book.isEmpty();
        }

        @Override
        public ItemStack getItem(int slot) {
            return slot == 0 ? TileEntityLectern.this.book : ItemStack.EMPTY;
        }

        @Override
        public ItemStack splitStack(int slot, int amount) {
            if (slot == 0) {
                ItemStack itemStack = TileEntityLectern.this.book.cloneAndSubtract(amount);
                if (TileEntityLectern.this.book.isEmpty()) {
                    TileEntityLectern.this.onBookItemRemove();
                }

                return itemStack;
            } else {
                return ItemStack.EMPTY;
            }
        }

        @Override
        public ItemStack splitWithoutUpdate(int slot) {
            if (slot == 0) {
                ItemStack itemStack = TileEntityLectern.this.book;
                TileEntityLectern.this.book = ItemStack.EMPTY;
                TileEntityLectern.this.onBookItemRemove();
                return itemStack;
            } else {
                return ItemStack.EMPTY;
            }
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public void update() {
            TileEntityLectern.this.update();
        }

        @Override
        public boolean stillValid(EntityHuman player) {
            if (TileEntityLectern.this.level.getTileEntity(TileEntityLectern.this.worldPosition) != TileEntityLectern.this) {
                return false;
            } else {
                return player.distanceToSqr((double)TileEntityLectern.this.worldPosition.getX() + 0.5D, (double)TileEntityLectern.this.worldPosition.getY() + 0.5D, (double)TileEntityLectern.this.worldPosition.getZ() + 0.5D) > 64.0D ? false : TileEntityLectern.this.hasBook();
            }
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return false;
        }

        @Override
        public void clear() {
        }
    };
    private final IContainerProperties dataAccess = new IContainerProperties() {
        @Override
        public int getProperty(int index) {
            return index == 0 ? TileEntityLectern.this.page : 0;
        }

        @Override
        public void setProperty(int index, int value) {
            if (index == 0) {
                TileEntityLectern.this.setPage(value);
            }

        }

        @Override
        public int getCount() {
            return 1;
        }
    };
    ItemStack book = ItemStack.EMPTY;
    int page;
    private int pageCount;

    public TileEntityLectern(BlockPosition pos, IBlockData state) {
        super(TileEntityTypes.LECTERN, pos, state);
    }

    public ItemStack getBook() {
        return this.book;
    }

    public boolean hasBook() {
        return this.book.is(Items.WRITABLE_BOOK) || this.book.is(Items.WRITTEN_BOOK);
    }

    public void setBook(ItemStack book) {
        this.setBook(book, (EntityHuman)null);
    }

    void onBookItemRemove() {
        this.page = 0;
        this.pageCount = 0;
        BlockLectern.setHasBook(this.getWorld(), this.getPosition(), this.getBlock(), false);
    }

    public void setBook(ItemStack book, @Nullable EntityHuman player) {
        this.book = this.resolveBook(book, player);
        this.page = 0;
        this.pageCount = ItemWrittenBook.getPageCount(this.book);
        this.update();
    }

    public void setPage(int currentPage) {
        int i = MathHelper.clamp(currentPage, 0, this.pageCount - 1);
        if (i != this.page) {
            this.page = i;
            this.update();
            BlockLectern.signalPageChange(this.getWorld(), this.getPosition(), this.getBlock());
        }

    }

    public int getPage() {
        return this.page;
    }

    public int getRedstoneSignal() {
        float f = this.pageCount > 1 ? (float)this.getPage() / ((float)this.pageCount - 1.0F) : 1.0F;
        return MathHelper.floor(f * 14.0F) + (this.hasBook() ? 1 : 0);
    }

    private ItemStack resolveBook(ItemStack book, @Nullable EntityHuman player) {
        if (this.level instanceof WorldServer && book.is(Items.WRITTEN_BOOK)) {
            ItemWrittenBook.resolveBookComponents(book, this.createCommandSourceStack(player), player);
        }

        return book;
    }

    private CommandListenerWrapper createCommandSourceStack(@Nullable EntityHuman player) {
        String string;
        IChatBaseComponent component;
        if (player == null) {
            string = "Lectern";
            component = new ChatComponentText("Lectern");
        } else {
            string = player.getDisplayName().getString();
            component = player.getScoreboardDisplayName();
        }

        Vec3D vec3 = Vec3D.atCenterOf(this.worldPosition);
        return new CommandListenerWrapper(ICommandListener.NULL, vec3, Vec2F.ZERO, (WorldServer)this.level, 2, string, component, this.level.getMinecraftServer(), player);
    }

    @Override
    public boolean isFilteredNBT() {
        return true;
    }

    @Override
    public void load(NBTTagCompound nbt) {
        super.load(nbt);
        if (nbt.hasKeyOfType("Book", 10)) {
            this.book = this.resolveBook(ItemStack.of(nbt.getCompound("Book")), (EntityHuman)null);
        } else {
            this.book = ItemStack.EMPTY;
        }

        this.pageCount = ItemWrittenBook.getPageCount(this.book);
        this.page = MathHelper.clamp(nbt.getInt("Page"), 0, this.pageCount - 1);
    }

    @Override
    protected void saveAdditional(NBTTagCompound nbt) {
        super.saveAdditional(nbt);
        if (!this.getBook().isEmpty()) {
            nbt.set("Book", this.getBook().save(new NBTTagCompound()));
            nbt.setInt("Page", this.page);
        }

    }

    @Override
    public void clear() {
        this.setBook(ItemStack.EMPTY);
    }

    @Override
    public Container createMenu(int syncId, PlayerInventory inv, EntityHuman player) {
        return new ContainerLectern(syncId, this.bookAccess, this.dataAccess);
    }

    @Override
    public IChatBaseComponent getScoreboardDisplayName() {
        return new ChatMessage("container.lectern");
    }
}
