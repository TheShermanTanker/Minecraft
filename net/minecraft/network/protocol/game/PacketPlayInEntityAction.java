package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.Entity;

public class PacketPlayInEntityAction implements Packet<PacketListenerPlayIn> {
    private final int id;
    private final PacketPlayInEntityAction.EnumPlayerAction action;
    private final int data;

    public PacketPlayInEntityAction(Entity entity, PacketPlayInEntityAction.EnumPlayerAction mode) {
        this(entity, mode, 0);
    }

    public PacketPlayInEntityAction(Entity entity, PacketPlayInEntityAction.EnumPlayerAction mode, int mountJumpHeight) {
        this.id = entity.getId();
        this.action = mode;
        this.data = mountJumpHeight;
    }

    public PacketPlayInEntityAction(PacketDataSerializer buf) {
        this.id = buf.readVarInt();
        this.action = buf.readEnum(PacketPlayInEntityAction.EnumPlayerAction.class);
        this.data = buf.readVarInt();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeVarInt(this.id);
        buf.writeEnum(this.action);
        buf.writeVarInt(this.data);
    }

    @Override
    public void handle(PacketListenerPlayIn listener) {
        listener.handlePlayerCommand(this);
    }

    public int getId() {
        return this.id;
    }

    public PacketPlayInEntityAction.EnumPlayerAction getAction() {
        return this.action;
    }

    public int getData() {
        return this.data;
    }

    public static enum EnumPlayerAction {
        PRESS_SHIFT_KEY,
        RELEASE_SHIFT_KEY,
        STOP_SLEEPING,
        START_SPRINTING,
        STOP_SPRINTING,
        START_RIDING_JUMP,
        STOP_RIDING_JUMP,
        OPEN_INVENTORY,
        START_FALL_FLYING;
    }
}
