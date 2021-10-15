package net.minecraft.network.protocol.game;

import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.util.MathHelper;
import net.minecraft.world.phys.Vec3D;

public class PacketPlayOutExplosion implements Packet<PacketListenerPlayOut> {
    private final double x;
    private final double y;
    private final double z;
    private final float power;
    private final List<BlockPosition> toBlow;
    private final float knockbackX;
    private final float knockbackY;
    private final float knockbackZ;

    public PacketPlayOutExplosion(double x, double y, double z, float radius, List<BlockPosition> affectedBlocks, @Nullable Vec3D playerVelocity) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.power = radius;
        this.toBlow = Lists.newArrayList(affectedBlocks);
        if (playerVelocity != null) {
            this.knockbackX = (float)playerVelocity.x;
            this.knockbackY = (float)playerVelocity.y;
            this.knockbackZ = (float)playerVelocity.z;
        } else {
            this.knockbackX = 0.0F;
            this.knockbackY = 0.0F;
            this.knockbackZ = 0.0F;
        }

    }

    public PacketPlayOutExplosion(PacketDataSerializer buf) {
        this.x = (double)buf.readFloat();
        this.y = (double)buf.readFloat();
        this.z = (double)buf.readFloat();
        this.power = buf.readFloat();
        int i = MathHelper.floor(this.x);
        int j = MathHelper.floor(this.y);
        int k = MathHelper.floor(this.z);
        this.toBlow = buf.readList((friendlyByteBuf) -> {
            int l = friendlyByteBuf.readByte() + i;
            int m = friendlyByteBuf.readByte() + j;
            int n = friendlyByteBuf.readByte() + k;
            return new BlockPosition(l, m, n);
        });
        this.knockbackX = buf.readFloat();
        this.knockbackY = buf.readFloat();
        this.knockbackZ = buf.readFloat();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeFloat((float)this.x);
        buf.writeFloat((float)this.y);
        buf.writeFloat((float)this.z);
        buf.writeFloat(this.power);
        int i = MathHelper.floor(this.x);
        int j = MathHelper.floor(this.y);
        int k = MathHelper.floor(this.z);
        buf.writeCollection(this.toBlow, (bufx, pos) -> {
            int l = pos.getX() - i;
            int m = pos.getY() - j;
            int n = pos.getZ() - k;
            bufx.writeByte(l);
            bufx.writeByte(m);
            bufx.writeByte(n);
        });
        buf.writeFloat(this.knockbackX);
        buf.writeFloat(this.knockbackY);
        buf.writeFloat(this.knockbackZ);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleExplosion(this);
    }

    public float getKnockbackX() {
        return this.knockbackX;
    }

    public float getKnockbackY() {
        return this.knockbackY;
    }

    public float getKnockbackZ() {
        return this.knockbackZ;
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

    public float getPower() {
        return this.power;
    }

    public List<BlockPosition> getToBlow() {
        return this.toBlow;
    }
}
