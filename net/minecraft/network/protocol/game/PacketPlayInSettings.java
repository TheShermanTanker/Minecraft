package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.EnumMainHand;
import net.minecraft.world.entity.player.EnumChatVisibility;

public record PacketPlayInSettings(String language, int viewDistance, EnumChatVisibility chatVisibility, boolean chatColors, int modelCustomisation, EnumMainHand mainHand, boolean textFilteringEnabled, boolean allowsListing) implements Packet<PacketListenerPlayIn> {
    public final String language;
    public final int viewDistance;
    public static final int MAX_LANGUAGE_LENGTH = 16;

    public PacketPlayInSettings(PacketDataSerializer buf) {
        this(buf.readUtf(16), buf.readByte(), buf.readEnum(EnumChatVisibility.class), buf.readBoolean(), buf.readUnsignedByte(), buf.readEnum(EnumMainHand.class), buf.readBoolean(), buf.readBoolean());
    }

    public PacketPlayInSettings(String language, int viewDistance, EnumChatVisibility chatVisibility, boolean chatColors, int modelBitMask, EnumMainHand mainArm, boolean filterText, boolean bl) {
        this.language = language;
        this.viewDistance = viewDistance;
        this.chatVisibility = chatVisibility;
        this.chatColors = chatColors;
        this.modelCustomisation = modelBitMask;
        this.mainHand = mainArm;
        this.textFilteringEnabled = filterText;
        this.allowsListing = bl;
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeUtf(this.language);
        buf.writeByte(this.viewDistance);
        buf.writeEnum(this.chatVisibility);
        buf.writeBoolean(this.chatColors);
        buf.writeByte(this.modelCustomisation);
        buf.writeEnum(this.mainHand);
        buf.writeBoolean(this.textFilteringEnabled);
        buf.writeBoolean(this.allowsListing);
    }

    @Override
    public void handle(PacketListenerPlayIn listener) {
        listener.handleClientInformation(this);
    }

    public String language() {
        return this.language;
    }

    public int viewDistance() {
        return this.viewDistance;
    }

    public EnumChatVisibility chatVisibility() {
        return this.chatVisibility;
    }

    public boolean chatColors() {
        return this.chatColors;
    }

    public int modelCustomisation() {
        return this.modelCustomisation;
    }

    public EnumMainHand mainHand() {
        return this.mainHand;
    }

    public boolean textFilteringEnabled() {
        return this.textFilteringEnabled;
    }

    public boolean allowsListing() {
        return this.allowsListing;
    }
}
