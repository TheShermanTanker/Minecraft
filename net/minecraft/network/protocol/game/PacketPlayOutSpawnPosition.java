package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPosition;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class PacketPlayOutSpawnPosition implements Packet<PacketListenerPlayOut> {
    public final BlockPosition pos;
    private final float angle;

    public PacketPlayOutSpawnPosition(BlockPosition pos, float angle) {
        this.pos = pos;
        this.angle = angle;
    }

    public PacketPlayOutSpawnPosition(PacketDataSerializer buf) {
        this.pos = buf.readBlockPos();
        this.angle = buf.readFloat();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeBlockPos(this.pos);
        buf.writeFloat(this.angle);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleSetSpawn(this);
    }

    public BlockPosition getPos() {
        return this.pos;
    }

    public float getAngle() {
        return this.angle;
    }
}
