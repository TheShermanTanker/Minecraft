package net.minecraft.world.level.block.entity;

import javax.annotation.Nullable;
import net.minecraft.CrashReportSystemDetails;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketListenerPlayOut;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.item.ItemBlock;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.IBlockData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class TileEntity {
    private static final Logger LOGGER = LogManager.getLogger();
    private final TileEntityTypes<?> type;
    @Nullable
    protected World level;
    protected final BlockPosition worldPosition;
    protected boolean remove;
    private IBlockData blockState;

    public TileEntity(TileEntityTypes<?> type, BlockPosition pos, IBlockData state) {
        this.type = type;
        this.worldPosition = pos.immutableCopy();
        this.blockState = state;
    }

    public static BlockPosition getPosFromTag(NBTTagCompound nbt) {
        return new BlockPosition(nbt.getInt("x"), nbt.getInt("y"), nbt.getInt("z"));
    }

    @Nullable
    public World getWorld() {
        return this.level;
    }

    public void setWorld(World world) {
        this.level = world;
    }

    public boolean hasWorld() {
        return this.level != null;
    }

    public void load(NBTTagCompound nbt) {
    }

    protected void saveAdditional(NBTTagCompound nbt) {
    }

    public final NBTTagCompound saveWithFullMetadata() {
        NBTTagCompound compoundTag = this.saveWithoutMetadata();
        this.saveMetadata(compoundTag);
        return compoundTag;
    }

    public final NBTTagCompound saveWithId() {
        NBTTagCompound compoundTag = this.saveWithoutMetadata();
        this.saveId(compoundTag);
        return compoundTag;
    }

    public final NBTTagCompound saveWithoutMetadata() {
        NBTTagCompound compoundTag = new NBTTagCompound();
        this.saveAdditional(compoundTag);
        return compoundTag;
    }

    private void saveId(NBTTagCompound nbt) {
        MinecraftKey resourceLocation = TileEntityTypes.getKey(this.getTileType());
        if (resourceLocation == null) {
            throw new RuntimeException(this.getClass() + " is missing a mapping! This is a bug!");
        } else {
            nbt.setString("id", resourceLocation.toString());
        }
    }

    public static void addEntityType(NBTTagCompound nbt, TileEntityTypes<?> type) {
        nbt.setString("id", TileEntityTypes.getKey(type).toString());
    }

    public void saveToItem(ItemStack stack) {
        ItemBlock.setBlockEntityData(stack, this.getTileType(), this.saveWithoutMetadata());
    }

    private void saveMetadata(NBTTagCompound nbt) {
        this.saveId(nbt);
        nbt.setInt("x", this.worldPosition.getX());
        nbt.setInt("y", this.worldPosition.getY());
        nbt.setInt("z", this.worldPosition.getZ());
    }

    @Nullable
    public static TileEntity create(BlockPosition pos, IBlockData state, NBTTagCompound nbt) {
        String string = nbt.getString("id");
        MinecraftKey resourceLocation = MinecraftKey.tryParse(string);
        if (resourceLocation == null) {
            LOGGER.error("Block entity has invalid type: {}", (Object)string);
            return null;
        } else {
            return IRegistry.BLOCK_ENTITY_TYPE.getOptional(resourceLocation).map((type) -> {
                try {
                    return type.create(pos, state);
                } catch (Throwable var5) {
                    LOGGER.error("Failed to create block entity {}", string, var5);
                    return null;
                }
            }).map((blockEntity) -> {
                try {
                    blockEntity.load(nbt);
                    return blockEntity;
                } catch (Throwable var4) {
                    LOGGER.error("Failed to load data for block entity {}", string, var4);
                    return null;
                }
            }).orElseGet(() -> {
                LOGGER.warn("Skipping BlockEntity with id {}", (Object)string);
                return null;
            });
        }
    }

    public void update() {
        if (this.level != null) {
            setChanged(this.level, this.worldPosition, this.blockState);
        }

    }

    protected static void setChanged(World world, BlockPosition pos, IBlockData state) {
        world.blockEntityChanged(pos);
        if (!state.isAir()) {
            world.updateAdjacentComparators(pos, state.getBlock());
        }

    }

    public BlockPosition getPosition() {
        return this.worldPosition;
    }

    public IBlockData getBlock() {
        return this.blockState;
    }

    @Nullable
    public Packet<PacketListenerPlayOut> getUpdatePacket() {
        return null;
    }

    public NBTTagCompound getUpdateTag() {
        return new NBTTagCompound();
    }

    public boolean isRemoved() {
        return this.remove;
    }

    public void setRemoved() {
        this.remove = true;
    }

    public void clearRemoved() {
        this.remove = false;
    }

    public boolean setProperty(int type, int data) {
        return false;
    }

    public void fillCrashReportCategory(CrashReportSystemDetails crashReportSection) {
        crashReportSection.setDetail("Name", () -> {
            return IRegistry.BLOCK_ENTITY_TYPE.getKey(this.getTileType()) + " // " + this.getClass().getCanonicalName();
        });
        if (this.level != null) {
            CrashReportSystemDetails.populateBlockDetails(crashReportSection, this.level, this.worldPosition, this.getBlock());
            CrashReportSystemDetails.populateBlockDetails(crashReportSection, this.level, this.worldPosition, this.level.getType(this.worldPosition));
        }
    }

    public boolean isFilteredNBT() {
        return false;
    }

    public TileEntityTypes<?> getTileType() {
        return this.type;
    }

    /** @deprecated */
    @Deprecated
    public void setBlockState(IBlockData state) {
        this.blockState = state;
    }
}
