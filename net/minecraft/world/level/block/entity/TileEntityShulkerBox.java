package net.minecraft.world.level.block.entity;

import java.util.List;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.util.MathHelper;
import net.minecraft.world.ContainerUtil;
import net.minecraft.world.IWorldInventory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EnumMoveType;
import net.minecraft.world.entity.monster.EntityShulker;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.inventory.ContainerShulkerBox;
import net.minecraft.world.item.EnumColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockShulkerBox;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.EnumPistonReaction;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.Vec3D;

public class TileEntityShulkerBox extends TileEntityLootable implements IWorldInventory {
    public static final int COLUMNS = 9;
    public static final int ROWS = 3;
    public static final int CONTAINER_SIZE = 27;
    public static final int EVENT_SET_OPEN_COUNT = 1;
    public static final int OPENING_TICK_LENGTH = 10;
    public static final float MAX_LID_HEIGHT = 0.5F;
    public static final float MAX_LID_ROTATION = 270.0F;
    public static final String ITEMS_TAG = "Items";
    private static final int[] SLOTS = IntStream.range(0, 27).toArray();
    private NonNullList<ItemStack> itemStacks = NonNullList.withSize(27, ItemStack.EMPTY);
    public int openCount;
    private TileEntityShulkerBox.AnimationPhase animationStatus = TileEntityShulkerBox.AnimationPhase.CLOSED;
    private float progress;
    private float progressOld;
    @Nullable
    private final EnumColor color;

    public TileEntityShulkerBox(@Nullable EnumColor color, BlockPosition pos, IBlockData state) {
        super(TileEntityTypes.SHULKER_BOX, pos, state);
        this.color = color;
    }

    public TileEntityShulkerBox(BlockPosition pos, IBlockData state) {
        super(TileEntityTypes.SHULKER_BOX, pos, state);
        this.color = BlockShulkerBox.getColorFromBlock(state.getBlock());
    }

    public static void tick(World world, BlockPosition pos, IBlockData state, TileEntityShulkerBox blockEntity) {
        blockEntity.updateAnimation(world, pos, state);
    }

    private void updateAnimation(World world, BlockPosition pos, IBlockData state) {
        this.progressOld = this.progress;
        switch(this.animationStatus) {
        case CLOSED:
            this.progress = 0.0F;
            break;
        case OPENING:
            this.progress += 0.1F;
            if (this.progress >= 1.0F) {
                this.animationStatus = TileEntityShulkerBox.AnimationPhase.OPENED;
                this.progress = 1.0F;
                doNeighborUpdates(world, pos, state);
            }

            this.moveCollidedEntities(world, pos, state);
            break;
        case CLOSING:
            this.progress -= 0.1F;
            if (this.progress <= 0.0F) {
                this.animationStatus = TileEntityShulkerBox.AnimationPhase.CLOSED;
                this.progress = 0.0F;
                doNeighborUpdates(world, pos, state);
            }
            break;
        case OPENED:
            this.progress = 1.0F;
        }

    }

    public TileEntityShulkerBox.AnimationPhase getAnimationStatus() {
        return this.animationStatus;
    }

    public AxisAlignedBB getBoundingBox(IBlockData state) {
        return EntityShulker.getProgressAabb(state.get(BlockShulkerBox.FACING), 0.5F * this.getProgress(1.0F));
    }

    private void moveCollidedEntities(World world, BlockPosition pos, IBlockData state) {
        if (state.getBlock() instanceof BlockShulkerBox) {
            EnumDirection direction = state.get(BlockShulkerBox.FACING);
            AxisAlignedBB aABB = EntityShulker.getProgressDeltaAabb(direction, this.progressOld, this.progress).move(pos);
            List<Entity> list = world.getEntities((Entity)null, aABB);
            if (!list.isEmpty()) {
                for(int i = 0; i < list.size(); ++i) {
                    Entity entity = list.get(i);
                    if (entity.getPushReaction() != EnumPistonReaction.IGNORE) {
                        entity.move(EnumMoveType.SHULKER_BOX, new Vec3D((aABB.getXsize() + 0.01D) * (double)direction.getAdjacentX(), (aABB.getYsize() + 0.01D) * (double)direction.getAdjacentY(), (aABB.getZsize() + 0.01D) * (double)direction.getAdjacentZ()));
                    }
                }

            }
        }
    }

    @Override
    public int getSize() {
        return this.itemStacks.size();
    }

    @Override
    public boolean setProperty(int type, int data) {
        if (type == 1) {
            this.openCount = data;
            if (data == 0) {
                this.animationStatus = TileEntityShulkerBox.AnimationPhase.CLOSING;
                doNeighborUpdates(this.getWorld(), this.worldPosition, this.getBlock());
            }

            if (data == 1) {
                this.animationStatus = TileEntityShulkerBox.AnimationPhase.OPENING;
                doNeighborUpdates(this.getWorld(), this.worldPosition, this.getBlock());
            }

            return true;
        } else {
            return super.setProperty(type, data);
        }
    }

    private static void doNeighborUpdates(World world, BlockPosition pos, IBlockData state) {
        state.updateNeighbourShapes(world, pos, 3);
    }

    @Override
    public void startOpen(EntityHuman player) {
        if (!player.isSpectator()) {
            if (this.openCount < 0) {
                this.openCount = 0;
            }

            ++this.openCount;
            this.level.playBlockAction(this.worldPosition, this.getBlock().getBlock(), 1, this.openCount);
            if (this.openCount == 1) {
                this.level.gameEvent(player, GameEvent.CONTAINER_OPEN, this.worldPosition);
                this.level.playSound((EntityHuman)null, this.worldPosition, SoundEffects.SHULKER_BOX_OPEN, EnumSoundCategory.BLOCKS, 0.5F, this.level.random.nextFloat() * 0.1F + 0.9F);
            }
        }

    }

    @Override
    public void closeContainer(EntityHuman player) {
        if (!player.isSpectator()) {
            --this.openCount;
            this.level.playBlockAction(this.worldPosition, this.getBlock().getBlock(), 1, this.openCount);
            if (this.openCount <= 0) {
                this.level.gameEvent(player, GameEvent.CONTAINER_CLOSE, this.worldPosition);
                this.level.playSound((EntityHuman)null, this.worldPosition, SoundEffects.SHULKER_BOX_CLOSE, EnumSoundCategory.BLOCKS, 0.5F, this.level.random.nextFloat() * 0.1F + 0.9F);
            }
        }

    }

    @Override
    protected IChatBaseComponent getContainerName() {
        return new ChatMessage("container.shulkerBox");
    }

    @Override
    public void load(NBTTagCompound nbt) {
        super.load(nbt);
        this.loadFromTag(nbt);
    }

    @Override
    public NBTTagCompound save(NBTTagCompound nbt) {
        super.save(nbt);
        return this.saveToTag(nbt);
    }

    public void loadFromTag(NBTTagCompound nbt) {
        this.itemStacks = NonNullList.withSize(this.getSize(), ItemStack.EMPTY);
        if (!this.tryLoadLootTable(nbt) && nbt.hasKeyOfType("Items", 9)) {
            ContainerUtil.loadAllItems(nbt, this.itemStacks);
        }

    }

    public NBTTagCompound saveToTag(NBTTagCompound nbt) {
        if (!this.trySaveLootTable(nbt)) {
            ContainerUtil.saveAllItems(nbt, this.itemStacks, false);
        }

        return nbt;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.itemStacks;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> list) {
        this.itemStacks = list;
    }

    @Override
    public int[] getSlotsForFace(EnumDirection side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable EnumDirection dir) {
        return !(Block.asBlock(stack.getItem()) instanceof BlockShulkerBox);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, EnumDirection dir) {
        return true;
    }

    public float getProgress(float delta) {
        return MathHelper.lerp(delta, this.progressOld, this.progress);
    }

    @Nullable
    public EnumColor getColor() {
        return this.color;
    }

    @Override
    protected Container createContainer(int syncId, PlayerInventory playerInventory) {
        return new ContainerShulkerBox(syncId, playerInventory, this);
    }

    public boolean isClosed() {
        return this.animationStatus == TileEntityShulkerBox.AnimationPhase.CLOSED;
    }

    public static enum AnimationPhase {
        CLOSED,
        OPENING,
        OPENED,
        CLOSING;
    }
}
