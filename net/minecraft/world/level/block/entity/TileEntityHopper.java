package net.minecraft.world.level.block.entity;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.world.ContainerUtil;
import net.minecraft.world.IInventory;
import net.minecraft.world.IInventoryHolder;
import net.minecraft.world.IWorldInventory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.IEntitySelector;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.inventory.ContainerHopper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockChest;
import net.minecraft.world.level.block.BlockHopper;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.shapes.OperatorBoolean;
import net.minecraft.world.phys.shapes.VoxelShapes;

public class TileEntityHopper extends TileEntityLootable implements IHopper {
    public static final int MOVE_ITEM_SPEED = 8;
    public static final int HOPPER_CONTAINER_SIZE = 5;
    private NonNullList<ItemStack> items = NonNullList.withSize(5, ItemStack.EMPTY);
    private int cooldownTime = -1;
    private long tickedGameTime;

    public TileEntityHopper(BlockPosition pos, IBlockData state) {
        super(TileEntityTypes.HOPPER, pos, state);
    }

    @Override
    public void load(NBTTagCompound nbt) {
        super.load(nbt);
        this.items = NonNullList.withSize(this.getSize(), ItemStack.EMPTY);
        if (!this.tryLoadLootTable(nbt)) {
            ContainerUtil.loadAllItems(nbt, this.items);
        }

        this.cooldownTime = nbt.getInt("TransferCooldown");
    }

    @Override
    public NBTTagCompound save(NBTTagCompound nbt) {
        super.save(nbt);
        if (!this.trySaveLootTable(nbt)) {
            ContainerUtil.saveAllItems(nbt, this.items);
        }

        nbt.setInt("TransferCooldown", this.cooldownTime);
        return nbt;
    }

    @Override
    public int getSize() {
        return this.items.size();
    }

    @Override
    public ItemStack splitStack(int slot, int amount) {
        this.unpackLootTable((EntityHuman)null);
        return ContainerUtil.removeItem(this.getItems(), slot, amount);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.unpackLootTable((EntityHuman)null);
        this.getItems().set(slot, stack);
        if (stack.getCount() > this.getMaxStackSize()) {
            stack.setCount(this.getMaxStackSize());
        }

    }

    @Override
    protected IChatBaseComponent getContainerName() {
        return new ChatMessage("container.hopper");
    }

    public static void pushItemsTick(World world, BlockPosition pos, IBlockData state, TileEntityHopper blockEntity) {
        --blockEntity.cooldownTime;
        blockEntity.tickedGameTime = world.getTime();
        if (!blockEntity.isOnCooldown()) {
            blockEntity.setCooldown(0);
            tryMoveItems(world, pos, state, blockEntity, () -> {
                return suckInItems(world, blockEntity);
            });
        }

    }

    private static boolean tryMoveItems(World world, BlockPosition pos, IBlockData state, TileEntityHopper blockEntity, BooleanSupplier booleanSupplier) {
        if (world.isClientSide) {
            return false;
        } else {
            if (!blockEntity.isOnCooldown() && state.get(BlockHopper.ENABLED)) {
                boolean bl = false;
                if (!blockEntity.isEmpty()) {
                    bl = ejectItems(world, pos, state, blockEntity);
                }

                if (!blockEntity.inventoryFull()) {
                    bl |= booleanSupplier.getAsBoolean();
                }

                if (bl) {
                    blockEntity.setCooldown(8);
                    setChanged(world, pos, state);
                    return true;
                }
            }

            return false;
        }
    }

    private boolean inventoryFull() {
        for(ItemStack itemStack : this.items) {
            if (itemStack.isEmpty() || itemStack.getCount() != itemStack.getMaxStackSize()) {
                return false;
            }
        }

        return true;
    }

    private static boolean ejectItems(World world, BlockPosition pos, IBlockData state, IInventory inventory) {
        IInventory container = getAttachedContainer(world, pos, state);
        if (container == null) {
            return false;
        } else {
            EnumDirection direction = state.get(BlockHopper.FACING).opposite();
            if (isFullContainer(container, direction)) {
                return false;
            } else {
                for(int i = 0; i < inventory.getSize(); ++i) {
                    if (!inventory.getItem(i).isEmpty()) {
                        ItemStack itemStack = inventory.getItem(i).cloneItemStack();
                        ItemStack itemStack2 = addItem(inventory, container, inventory.splitStack(i, 1), direction);
                        if (itemStack2.isEmpty()) {
                            container.update();
                            return true;
                        }

                        inventory.setItem(i, itemStack);
                    }
                }

                return false;
            }
        }
    }

    private static IntStream getSlots(IInventory inventory, EnumDirection side) {
        return inventory instanceof IWorldInventory ? IntStream.of(((IWorldInventory)inventory).getSlotsForFace(side)) : IntStream.range(0, inventory.getSize());
    }

    private static boolean isFullContainer(IInventory inventory, EnumDirection direction) {
        return getSlots(inventory, direction).allMatch((i) -> {
            ItemStack itemStack = inventory.getItem(i);
            return itemStack.getCount() >= itemStack.getMaxStackSize();
        });
    }

    private static boolean isEmptyContainer(IInventory inv, EnumDirection facing) {
        return getSlots(inv, facing).allMatch((i) -> {
            return inv.getItem(i).isEmpty();
        });
    }

    public static boolean suckInItems(World world, IHopper hopper) {
        IInventory container = getSourceContainer(world, hopper);
        if (container != null) {
            EnumDirection direction = EnumDirection.DOWN;
            return isEmptyContainer(container, direction) ? false : getSlots(container, direction).anyMatch((i) -> {
                return tryTakeInItemFromSlot(hopper, container, i, direction);
            });
        } else {
            for(EntityItem itemEntity : getItemsAtAndAbove(world, hopper)) {
                if (addItem(hopper, itemEntity)) {
                    return true;
                }
            }

            return false;
        }
    }

    private static boolean tryTakeInItemFromSlot(IHopper hopper, IInventory inventory, int slot, EnumDirection side) {
        ItemStack itemStack = inventory.getItem(slot);
        if (!itemStack.isEmpty() && canTakeItemFromContainer(inventory, itemStack, slot, side)) {
            ItemStack itemStack2 = itemStack.cloneItemStack();
            ItemStack itemStack3 = addItem(inventory, hopper, inventory.splitStack(slot, 1), (EnumDirection)null);
            if (itemStack3.isEmpty()) {
                inventory.update();
                return true;
            }

            inventory.setItem(slot, itemStack2);
        }

        return false;
    }

    public static boolean addItem(IInventory inventory, EntityItem itemEntity) {
        boolean bl = false;
        ItemStack itemStack = itemEntity.getItemStack().cloneItemStack();
        ItemStack itemStack2 = addItem((IInventory)null, inventory, itemStack, (EnumDirection)null);
        if (itemStack2.isEmpty()) {
            bl = true;
            itemEntity.die();
        } else {
            itemEntity.setItemStack(itemStack2);
        }

        return bl;
    }

    public static ItemStack addItem(@Nullable IInventory from, IInventory to, ItemStack stack, @Nullable EnumDirection side) {
        if (to instanceof IWorldInventory && side != null) {
            IWorldInventory worldlyContainer = (IWorldInventory)to;
            int[] is = worldlyContainer.getSlotsForFace(side);

            for(int i = 0; i < is.length && !stack.isEmpty(); ++i) {
                stack = tryMoveInItem(from, to, stack, is[i], side);
            }
        } else {
            int j = to.getSize();

            for(int k = 0; k < j && !stack.isEmpty(); ++k) {
                stack = tryMoveInItem(from, to, stack, k, side);
            }
        }

        return stack;
    }

    private static boolean canPlaceItemInContainer(IInventory inventory, ItemStack stack, int slot, @Nullable EnumDirection side) {
        if (!inventory.canPlaceItem(slot, stack)) {
            return false;
        } else {
            return !(inventory instanceof IWorldInventory) || ((IWorldInventory)inventory).canPlaceItemThroughFace(slot, stack, side);
        }
    }

    private static boolean canTakeItemFromContainer(IInventory inv, ItemStack stack, int slot, EnumDirection facing) {
        return !(inv instanceof IWorldInventory) || ((IWorldInventory)inv).canTakeItemThroughFace(slot, stack, facing);
    }

    private static ItemStack tryMoveInItem(@Nullable IInventory from, IInventory to, ItemStack stack, int slot, @Nullable EnumDirection direction) {
        ItemStack itemStack = to.getItem(slot);
        if (canPlaceItemInContainer(to, stack, slot, direction)) {
            boolean bl = false;
            boolean bl2 = to.isEmpty();
            if (itemStack.isEmpty()) {
                to.setItem(slot, stack);
                stack = ItemStack.EMPTY;
                bl = true;
            } else if (canMergeItems(itemStack, stack)) {
                int i = stack.getMaxStackSize() - itemStack.getCount();
                int j = Math.min(stack.getCount(), i);
                stack.subtract(j);
                itemStack.add(j);
                bl = j > 0;
            }

            if (bl) {
                if (bl2 && to instanceof TileEntityHopper) {
                    TileEntityHopper hopperBlockEntity = (TileEntityHopper)to;
                    if (!hopperBlockEntity.isOnCustomCooldown()) {
                        int k = 0;
                        if (from instanceof TileEntityHopper) {
                            TileEntityHopper hopperBlockEntity2 = (TileEntityHopper)from;
                            if (hopperBlockEntity.tickedGameTime >= hopperBlockEntity2.tickedGameTime) {
                                k = 1;
                            }
                        }

                        hopperBlockEntity.setCooldown(8 - k);
                    }
                }

                to.update();
            }
        }

        return stack;
    }

    @Nullable
    private static IInventory getAttachedContainer(World world, BlockPosition pos, IBlockData state) {
        EnumDirection direction = state.get(BlockHopper.FACING);
        return getContainerAt(world, pos.relative(direction));
    }

    @Nullable
    private static IInventory getSourceContainer(World world, IHopper hopper) {
        return getContainerAt(world, hopper.getWorldX(), hopper.getWorldY() + 1.0D, hopper.getWorldZ());
    }

    public static List<EntityItem> getItemsAtAndAbove(World world, IHopper hopper) {
        return hopper.getSuckShape().toList().stream().flatMap((aABB) -> {
            return world.getEntitiesOfClass(EntityItem.class, aABB.move(hopper.getWorldX() - 0.5D, hopper.getWorldY() - 0.5D, hopper.getWorldZ() - 0.5D), IEntitySelector.ENTITY_STILL_ALIVE).stream();
        }).collect(Collectors.toList());
    }

    @Nullable
    public static IInventory getContainerAt(World world, BlockPosition pos) {
        return getContainerAt(world, (double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D);
    }

    @Nullable
    private static IInventory getContainerAt(World world, double x, double y, double z) {
        IInventory container = null;
        BlockPosition blockPos = new BlockPosition(x, y, z);
        IBlockData blockState = world.getType(blockPos);
        Block block = blockState.getBlock();
        if (block instanceof IInventoryHolder) {
            container = ((IInventoryHolder)block).getContainer(blockState, world, blockPos);
        } else if (blockState.isTileEntity()) {
            TileEntity blockEntity = world.getTileEntity(blockPos);
            if (blockEntity instanceof IInventory) {
                container = (IInventory)blockEntity;
                if (container instanceof TileEntityChest && block instanceof BlockChest) {
                    container = BlockChest.getInventory((BlockChest)block, blockState, world, blockPos, true);
                }
            }
        }

        if (container == null) {
            List<Entity> list = world.getEntities((Entity)null, new AxisAlignedBB(x - 0.5D, y - 0.5D, z - 0.5D, x + 0.5D, y + 0.5D, z + 0.5D), IEntitySelector.CONTAINER_ENTITY_SELECTOR);
            if (!list.isEmpty()) {
                container = (IInventory)list.get(world.random.nextInt(list.size()));
            }
        }

        return container;
    }

    private static boolean canMergeItems(ItemStack first, ItemStack second) {
        if (!first.is(second.getItem())) {
            return false;
        } else if (first.getDamage() != second.getDamage()) {
            return false;
        } else if (first.getCount() > first.getMaxStackSize()) {
            return false;
        } else {
            return ItemStack.equals(first, second);
        }
    }

    @Override
    public double getWorldX() {
        return (double)this.worldPosition.getX() + 0.5D;
    }

    @Override
    public double getWorldY() {
        return (double)this.worldPosition.getY() + 0.5D;
    }

    @Override
    public double getWorldZ() {
        return (double)this.worldPosition.getZ() + 0.5D;
    }

    private void setCooldown(int cooldown) {
        this.cooldownTime = cooldown;
    }

    private boolean isOnCooldown() {
        return this.cooldownTime > 0;
    }

    private boolean isOnCustomCooldown() {
        return this.cooldownTime > 8;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> list) {
        this.items = list;
    }

    public static void entityInside(World world, BlockPosition pos, IBlockData state, Entity entity, TileEntityHopper blockEntity) {
        if (entity instanceof EntityItem && VoxelShapes.joinIsNotEmpty(VoxelShapes.create(entity.getBoundingBox().move((double)(-pos.getX()), (double)(-pos.getY()), (double)(-pos.getZ()))), blockEntity.getSuckShape(), OperatorBoolean.AND)) {
            tryMoveItems(world, pos, state, blockEntity, () -> {
                return addItem(blockEntity, (EntityItem)entity);
            });
        }

    }

    @Override
    protected Container createContainer(int syncId, PlayerInventory playerInventory) {
        return new ContainerHopper(syncId, playerInventory, this);
    }
}
