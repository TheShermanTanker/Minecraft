package net.minecraft.network.protocol.game;

import java.util.UUID;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.phys.Vec3D;

public class PacketPlayOutSpawnEntity implements Packet<PacketListenerPlayOut> {
    public static final double MAGICAL_QUANTIZATION = 8000.0D;
    private final int id;
    private final UUID uuid;
    private final double x;
    private final double y;
    private final double z;
    private final int xa;
    private final int ya;
    private final int za;
    private final int xRot;
    private final int yRot;
    private final EntityTypes<?> type;
    private final int data;
    public static final double LIMIT = 3.9D;

    public PacketPlayOutSpawnEntity(int id, UUID uuid, double x, double y, double z, float pitch, float yaw, EntityTypes<?> entityTypeId, int entityData, Vec3D velocity) {
        this.id = id;
        this.uuid = uuid;
        this.x = x;
        this.y = y;
        this.z = z;
        this.xRot = MathHelper.floor(pitch * 256.0F / 360.0F);
        this.yRot = MathHelper.floor(yaw * 256.0F / 360.0F);
        this.type = entityTypeId;
        this.data = entityData;
        this.xa = (int)(MathHelper.clamp(velocity.x, -3.9D, 3.9D) * 8000.0D);
        this.ya = (int)(MathHelper.clamp(velocity.y, -3.9D, 3.9D) * 8000.0D);
        this.za = (int)(MathHelper.clamp(velocity.z, -3.9D, 3.9D) * 8000.0D);
    }

    public PacketPlayOutSpawnEntity(Entity entity) {
        this(entity, 0);
    }

    public PacketPlayOutSpawnEntity(Entity entity, int entityData) {
        this(entity.getId(), entity.getUniqueID(), entity.locX(), entity.locY(), entity.locZ(), entity.getXRot(), entity.getYRot(), entity.getEntityType(), entityData, entity.getMot());
    }

    public PacketPlayOutSpawnEntity(Entity entity, EntityTypes<?> entityType, int data, BlockPosition pos) {
        this(entity.getId(), entity.getUniqueID(), (double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), entity.getXRot(), entity.getYRot(), entityType, data, entity.getMot());
    }

    public PacketPlayOutSpawnEntity(PacketDataSerializer buf) {
        this.id = buf.readVarInt();
        this.uuid = buf.readUUID();
        this.type = IRegistry.ENTITY_TYPE.fromId(buf.readVarInt());
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
        this.xRot = buf.readByte();
        this.yRot = buf.readByte();
        this.data = buf.readInt();
        this.xa = buf.readShort();
        this.ya = buf.readShort();
        this.za = buf.readShort();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeVarInt(this.id);
        buf.writeUUID(this.uuid);
        buf.writeVarInt(IRegistry.ENTITY_TYPE.getId(this.type));
        buf.writeDouble(this.x);
        buf.writeDouble(this.y);
        buf.writeDouble(this.z);
        buf.writeByte(this.xRot);
        buf.writeByte(this.yRot);
        buf.writeInt(this.data);
        buf.writeShort(this.xa);
        buf.writeShort(this.ya);
        buf.writeShort(this.za);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleAddEntity(this);
    }

    public int getId() {
        return this.id;
    }

    public UUID getUUID() {
        return this.uuid;
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public double getZ() {
        return this.z;
    }

    public double getXa() {
        return (double)this.xa / 8000.0D;
    }

    public double getYa() {
        return (double)this.ya / 8000.0D;
    }

    public double getZa() {
        return (double)this.za / 8000.0D;
    }

    public int getxRot() {
        return this.xRot;
    }

    public int getyRot() {
        return this.yRot;
    }

    public EntityTypes<?> getType() {
        return this.type;
    }

    public int getData() {
        return this.data;
    }
}
