package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3D;

public class PacketPlayOutEntityVelocity implements Packet<PacketListenerPlayOut> {
    private final int id;
    private final int xa;
    private final int ya;
    private final int za;

    public PacketPlayOutEntityVelocity(Entity entity) {
        this(entity.getId(), entity.getMot());
    }

    public PacketPlayOutEntityVelocity(int id, Vec3D velocity) {
        this.id = id;
        double d = 3.9D;
        double e = MathHelper.clamp(velocity.x, -3.9D, 3.9D);
        double f = MathHelper.clamp(velocity.y, -3.9D, 3.9D);
        double g = MathHelper.clamp(velocity.z, -3.9D, 3.9D);
        this.xa = (int)(e * 8000.0D);
        this.ya = (int)(f * 8000.0D);
        this.za = (int)(g * 8000.0D);
    }

    public PacketPlayOutEntityVelocity(PacketDataSerializer buf) {
        this.id = buf.readVarInt();
        this.xa = buf.readShort();
        this.ya = buf.readShort();
        this.za = buf.readShort();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeVarInt(this.id);
        buf.writeShort(this.xa);
        buf.writeShort(this.ya);
        buf.writeShort(this.za);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleSetEntityMotion(this);
    }

    public int getId() {
        return this.id;
    }

    public int getXa() {
        return this.xa;
    }

    public int getYa() {
        return this.ya;
    }

    public int getZa() {
        return this.za;
    }
}
