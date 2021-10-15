package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPosition;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.level.block.entity.TileEntityJigsaw;

public class PacketPlayInSetJigsaw implements Packet<PacketListenerPlayIn> {
    private final BlockPosition pos;
    private final MinecraftKey name;
    private final MinecraftKey target;
    private final MinecraftKey pool;
    private final String finalState;
    private final TileEntityJigsaw.JointType joint;

    public PacketPlayInSetJigsaw(BlockPosition pos, MinecraftKey attachmentType, MinecraftKey targetPool, MinecraftKey pool, String finalState, TileEntityJigsaw.JointType jointType) {
        this.pos = pos;
        this.name = attachmentType;
        this.target = targetPool;
        this.pool = pool;
        this.finalState = finalState;
        this.joint = jointType;
    }

    public PacketPlayInSetJigsaw(PacketDataSerializer buf) {
        this.pos = buf.readBlockPos();
        this.name = buf.readResourceLocation();
        this.target = buf.readResourceLocation();
        this.pool = buf.readResourceLocation();
        this.finalState = buf.readUtf();
        this.joint = TileEntityJigsaw.JointType.byName(buf.readUtf()).orElse(TileEntityJigsaw.JointType.ALIGNED);
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeBlockPos(this.pos);
        buf.writeResourceLocation(this.name);
        buf.writeResourceLocation(this.target);
        buf.writeResourceLocation(this.pool);
        buf.writeUtf(this.finalState);
        buf.writeUtf(this.joint.getSerializedName());
    }

    @Override
    public void handle(PacketListenerPlayIn listener) {
        listener.handleSetJigsawBlock(this);
    }

    public BlockPosition getPos() {
        return this.pos;
    }

    public MinecraftKey getName() {
        return this.name;
    }

    public MinecraftKey getTarget() {
        return this.target;
    }

    public MinecraftKey getPool() {
        return this.pool;
    }

    public String getFinalState() {
        return this.finalState;
    }

    public TileEntityJigsaw.JointType getJoint() {
        return this.joint;
    }
}
