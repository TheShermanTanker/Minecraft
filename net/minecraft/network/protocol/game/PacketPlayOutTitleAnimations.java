package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class PacketPlayOutTitleAnimations implements Packet<PacketListenerPlayOut> {
    private final int fadeIn;
    private final int stay;
    private final int fadeOut;

    public PacketPlayOutTitleAnimations(int fadeInTicks, int remainTicks, int fadeOutTicks) {
        this.fadeIn = fadeInTicks;
        this.stay = remainTicks;
        this.fadeOut = fadeOutTicks;
    }

    public PacketPlayOutTitleAnimations(PacketDataSerializer buf) {
        this.fadeIn = buf.readInt();
        this.stay = buf.readInt();
        this.fadeOut = buf.readInt();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeInt(this.fadeIn);
        buf.writeInt(this.stay);
        buf.writeInt(this.fadeOut);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.setTitlesAnimation(this);
    }

    public int getFadeIn() {
        return this.fadeIn;
    }

    public int getStay() {
        return this.stay;
    }

    public int getFadeOut() {
        return this.fadeOut;
    }
}
