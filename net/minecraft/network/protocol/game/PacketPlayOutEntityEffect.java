package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectList;

public class PacketPlayOutEntityEffect implements Packet<PacketListenerPlayOut> {
    private static final int FLAG_AMBIENT = 1;
    private static final int FLAG_VISIBLE = 2;
    private static final int FLAG_SHOW_ICON = 4;
    private final int entityId;
    private final byte effectId;
    private final byte effectAmplifier;
    private final int effectDurationTicks;
    private final byte flags;

    public PacketPlayOutEntityEffect(int entityId, MobEffect effect) {
        this.entityId = entityId;
        this.effectId = (byte)(MobEffectList.getId(effect.getMobEffect()) & 255);
        this.effectAmplifier = (byte)(effect.getAmplifier() & 255);
        if (effect.getDuration() > 32767) {
            this.effectDurationTicks = 32767;
        } else {
            this.effectDurationTicks = effect.getDuration();
        }

        byte b = 0;
        if (effect.isAmbient()) {
            b = (byte)(b | 1);
        }

        if (effect.isShowParticles()) {
            b = (byte)(b | 2);
        }

        if (effect.isShowIcon()) {
            b = (byte)(b | 4);
        }

        this.flags = b;
    }

    public PacketPlayOutEntityEffect(PacketDataSerializer buf) {
        this.entityId = buf.readVarInt();
        this.effectId = buf.readByte();
        this.effectAmplifier = buf.readByte();
        this.effectDurationTicks = buf.readVarInt();
        this.flags = buf.readByte();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeVarInt(this.entityId);
        buf.writeByte(this.effectId);
        buf.writeByte(this.effectAmplifier);
        buf.writeVarInt(this.effectDurationTicks);
        buf.writeByte(this.flags);
    }

    public boolean isSuperLongDuration() {
        return this.effectDurationTicks == 32767;
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleUpdateMobEffect(this);
    }

    public int getEntityId() {
        return this.entityId;
    }

    public byte getEffectId() {
        return this.effectId;
    }

    public byte getEffectAmplifier() {
        return this.effectAmplifier;
    }

    public int getEffectDurationTicks() {
        return this.effectDurationTicks;
    }

    public boolean isEffectVisible() {
        return (this.flags & 2) == 2;
    }

    public boolean isEffectAmbient() {
        return (this.flags & 1) == 1;
    }

    public boolean effectShowsIcon() {
        return (this.flags & 4) == 4;
    }
}
