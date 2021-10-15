package net.minecraft.network.protocol.game;

import javax.annotation.Nullable;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.effect.MobEffectList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.World;

public class PacketPlayOutRemoveEntityEffect implements Packet<PacketListenerPlayOut> {
    private final int entityId;
    private final MobEffectList effect;

    public PacketPlayOutRemoveEntityEffect(int entityId, MobEffectList effectType) {
        this.entityId = entityId;
        this.effect = effectType;
    }

    public PacketPlayOutRemoveEntityEffect(PacketDataSerializer buf) {
        this.entityId = buf.readVarInt();
        this.effect = MobEffectList.fromId(buf.readUnsignedByte());
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeVarInt(this.entityId);
        buf.writeByte(MobEffectList.getId(this.effect));
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleRemoveMobEffect(this);
    }

    @Nullable
    public Entity getEntity(World world) {
        return world.getEntity(this.entityId);
    }

    @Nullable
    public MobEffectList getEffect() {
        return this.effect;
    }
}
