package net.minecraft.network.protocol.game;

import java.util.EnumSet;
import java.util.Set;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class PacketPlayOutPosition implements Packet<PacketListenerPlayOut> {
    private final double x;
    private final double y;
    private final double z;
    private final float yRot;
    private final float xRot;
    private final Set<PacketPlayOutPosition.EnumPlayerTeleportFlags> relativeArguments;
    private final int id;
    private final boolean dismountVehicle;

    public PacketPlayOutPosition(double x, double y, double z, float yaw, float pitch, Set<PacketPlayOutPosition.EnumPlayerTeleportFlags> flags, int teleportId, boolean shouldDismount) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yRot = yaw;
        this.xRot = pitch;
        this.relativeArguments = flags;
        this.id = teleportId;
        this.dismountVehicle = shouldDismount;
    }

    public PacketPlayOutPosition(PacketDataSerializer buf) {
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
        this.yRot = buf.readFloat();
        this.xRot = buf.readFloat();
        this.relativeArguments = PacketPlayOutPosition.EnumPlayerTeleportFlags.unpack(buf.readUnsignedByte());
        this.id = buf.readVarInt();
        this.dismountVehicle = buf.readBoolean();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeDouble(this.x);
        buf.writeDouble(this.y);
        buf.writeDouble(this.z);
        buf.writeFloat(this.yRot);
        buf.writeFloat(this.xRot);
        buf.writeByte(PacketPlayOutPosition.EnumPlayerTeleportFlags.pack(this.relativeArguments));
        buf.writeVarInt(this.id);
        buf.writeBoolean(this.dismountVehicle);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleMovePlayer(this);
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

    public float getYRot() {
        return this.yRot;
    }

    public float getXRot() {
        return this.xRot;
    }

    public int getId() {
        return this.id;
    }

    public boolean requestDismountVehicle() {
        return this.dismountVehicle;
    }

    public Set<PacketPlayOutPosition.EnumPlayerTeleportFlags> getRelativeArguments() {
        return this.relativeArguments;
    }

    public static enum EnumPlayerTeleportFlags {
        X(0),
        Y(1),
        Z(2),
        Y_ROT(3),
        X_ROT(4);

        private final int bit;

        private EnumPlayerTeleportFlags(int shift) {
            this.bit = shift;
        }

        private int getMask() {
            return 1 << this.bit;
        }

        private boolean isSet(int mask) {
            return (mask & this.getMask()) == this.getMask();
        }

        public static Set<PacketPlayOutPosition.EnumPlayerTeleportFlags> unpack(int mask) {
            Set<PacketPlayOutPosition.EnumPlayerTeleportFlags> set = EnumSet.noneOf(PacketPlayOutPosition.EnumPlayerTeleportFlags.class);

            for(PacketPlayOutPosition.EnumPlayerTeleportFlags relativeArgument : values()) {
                if (relativeArgument.isSet(mask)) {
                    set.add(relativeArgument);
                }
            }

            return set;
        }

        public static int pack(Set<PacketPlayOutPosition.EnumPlayerTeleportFlags> flags) {
            int i = 0;

            for(PacketPlayOutPosition.EnumPlayerTeleportFlags relativeArgument : flags) {
                i |= relativeArgument.getMask();
            }

            return i;
        }
    }
}
