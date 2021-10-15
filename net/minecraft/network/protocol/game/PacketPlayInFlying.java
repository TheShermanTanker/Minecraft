package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public abstract class PacketPlayInFlying implements Packet<PacketListenerPlayIn> {
    public final double x;
    public final double y;
    public final double z;
    public final float yRot;
    public final float xRot;
    protected final boolean onGround;
    public final boolean hasPos;
    public final boolean hasRot;

    protected PacketPlayInFlying(double x, double y, double z, float yaw, float pitch, boolean onGround, boolean changePosition, boolean changeLook) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yRot = yaw;
        this.xRot = pitch;
        this.onGround = onGround;
        this.hasPos = changePosition;
        this.hasRot = changeLook;
    }

    @Override
    public void handle(PacketListenerPlayIn listener) {
        listener.handleMovePlayer(this);
    }

    public double getX(double currentX) {
        return this.hasPos ? this.x : currentX;
    }

    public double getY(double currentY) {
        return this.hasPos ? this.y : currentY;
    }

    public double getZ(double currentZ) {
        return this.hasPos ? this.z : currentZ;
    }

    public float getYRot(float currentYaw) {
        return this.hasRot ? this.yRot : currentYaw;
    }

    public float getXRot(float currentPitch) {
        return this.hasRot ? this.xRot : currentPitch;
    }

    public boolean isOnGround() {
        return this.onGround;
    }

    public boolean hasPosition() {
        return this.hasPos;
    }

    public boolean hasRotation() {
        return this.hasRot;
    }

    public static class PacketPlayInLook extends PacketPlayInFlying {
        public PacketPlayInLook(float yaw, float pitch, boolean onGround) {
            super(0.0D, 0.0D, 0.0D, yaw, pitch, onGround, false, true);
        }

        public static PacketPlayInFlying.PacketPlayInLook read(PacketDataSerializer buf) {
            float f = buf.readFloat();
            float g = buf.readFloat();
            boolean bl = buf.readUnsignedByte() != 0;
            return new PacketPlayInFlying.PacketPlayInLook(f, g, bl);
        }

        @Override
        public void write(PacketDataSerializer buf) {
            buf.writeFloat(this.yRot);
            buf.writeFloat(this.xRot);
            buf.writeByte(this.onGround ? 1 : 0);
        }
    }

    public static class PacketPlayInPosition extends PacketPlayInFlying {
        public PacketPlayInPosition(double x, double y, double z, boolean onGround) {
            super(x, y, z, 0.0F, 0.0F, onGround, true, false);
        }

        public static PacketPlayInFlying.PacketPlayInPosition read(PacketDataSerializer buf) {
            double d = buf.readDouble();
            double e = buf.readDouble();
            double f = buf.readDouble();
            boolean bl = buf.readUnsignedByte() != 0;
            return new PacketPlayInFlying.PacketPlayInPosition(d, e, f, bl);
        }

        @Override
        public void write(PacketDataSerializer buf) {
            buf.writeDouble(this.x);
            buf.writeDouble(this.y);
            buf.writeDouble(this.z);
            buf.writeByte(this.onGround ? 1 : 0);
        }
    }

    public static class PacketPlayInPositionLook extends PacketPlayInFlying {
        public PacketPlayInPositionLook(double x, double y, double z, float yaw, float pitch, boolean onGround) {
            super(x, y, z, yaw, pitch, onGround, true, true);
        }

        public static PacketPlayInFlying.PacketPlayInPositionLook read(PacketDataSerializer buf) {
            double d = buf.readDouble();
            double e = buf.readDouble();
            double f = buf.readDouble();
            float g = buf.readFloat();
            float h = buf.readFloat();
            boolean bl = buf.readUnsignedByte() != 0;
            return new PacketPlayInFlying.PacketPlayInPositionLook(d, e, f, g, h, bl);
        }

        @Override
        public void write(PacketDataSerializer buf) {
            buf.writeDouble(this.x);
            buf.writeDouble(this.y);
            buf.writeDouble(this.z);
            buf.writeFloat(this.yRot);
            buf.writeFloat(this.xRot);
            buf.writeByte(this.onGround ? 1 : 0);
        }
    }

    public static class StatusOnly extends PacketPlayInFlying {
        public StatusOnly(boolean onGround) {
            super(0.0D, 0.0D, 0.0D, 0.0F, 0.0F, onGround, false, false);
        }

        public static PacketPlayInFlying.StatusOnly read(PacketDataSerializer buf) {
            boolean bl = buf.readUnsignedByte() != 0;
            return new PacketPlayInFlying.StatusOnly(bl);
        }

        @Override
        public void write(PacketDataSerializer buf) {
            buf.writeByte(this.onGround ? 1 : 0);
        }
    }
}
