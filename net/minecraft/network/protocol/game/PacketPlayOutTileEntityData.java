package net.minecraft.network.protocol.game;

import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityTypes;

public class PacketPlayOutTileEntityData implements Packet<PacketListenerPlayOut> {
    private final BlockPosition pos;
    private final TileEntityTypes<?> type;
    @Nullable
    private final NBTTagCompound tag;

    public static PacketPlayOutTileEntityData create(TileEntity blockEntity, Function<TileEntity, NBTTagCompound> nbtGetter) {
        return new PacketPlayOutTileEntityData(blockEntity.getPosition(), blockEntity.getTileType(), nbtGetter.apply(blockEntity));
    }

    public static PacketPlayOutTileEntityData create(TileEntity blockEntity) {
        return create(blockEntity, TileEntity::getUpdateTag);
    }

    private PacketPlayOutTileEntityData(BlockPosition pos, TileEntityTypes<?> blockEntityType, NBTTagCompound nbt) {
        this.pos = pos;
        this.type = blockEntityType;
        this.tag = nbt.isEmpty() ? null : nbt;
    }

    public PacketPlayOutTileEntityData(PacketDataSerializer buf) {
        this.pos = buf.readBlockPos();
        this.type = IRegistry.BLOCK_ENTITY_TYPE.fromId(buf.readVarInt());
        this.tag = buf.readNbt();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeBlockPos(this.pos);
        buf.writeVarInt(IRegistry.BLOCK_ENTITY_TYPE.getId(this.type));
        buf.writeNbt(this.tag);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleBlockEntityData(this);
    }

    public BlockPosition getPos() {
        return this.pos;
    }

    public TileEntityTypes<?> getType() {
        return this.type;
    }

    @Nullable
    public NBTTagCompound getTag() {
        return this.tag;
    }
}
