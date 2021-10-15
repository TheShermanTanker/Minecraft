package net.minecraft.network.protocol.game;

import java.util.UUID;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.IRegistry;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.decoration.EntityPainting;
import net.minecraft.world.entity.decoration.Paintings;

public class PacketPlayOutSpawnEntityPainting implements Packet<PacketListenerPlayOut> {
    private final int id;
    private final UUID uuid;
    private final BlockPosition pos;
    private final EnumDirection direction;
    private final int motive;

    public PacketPlayOutSpawnEntityPainting(EntityPainting entity) {
        this.id = entity.getId();
        this.uuid = entity.getUniqueID();
        this.pos = entity.getBlockPosition();
        this.direction = entity.getDirection();
        this.motive = IRegistry.MOTIVE.getId(entity.motive);
    }

    public PacketPlayOutSpawnEntityPainting(PacketDataSerializer buf) {
        this.id = buf.readVarInt();
        this.uuid = buf.readUUID();
        this.motive = buf.readVarInt();
        this.pos = buf.readBlockPos();
        this.direction = EnumDirection.fromType2(buf.readUnsignedByte());
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeVarInt(this.id);
        buf.writeUUID(this.uuid);
        buf.writeVarInt(this.motive);
        buf.writeBlockPos(this.pos);
        buf.writeByte(this.direction.get2DRotationValue());
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleAddPainting(this);
    }

    public int getId() {
        return this.id;
    }

    public UUID getUUID() {
        return this.uuid;
    }

    public BlockPosition getPos() {
        return this.pos;
    }

    public EnumDirection getDirection() {
        return this.direction;
    }

    public Paintings getMotive() {
        return IRegistry.MOTIVE.fromId(this.motive);
    }
}
