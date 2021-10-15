package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.EnumMainHand;
import net.minecraft.world.entity.player.EnumChatVisibility;

public class PacketPlayInSettings implements Packet<PacketListenerPlayIn> {
    public static final int MAX_LANGUAGE_LENGTH = 16;
    public final String language;
    public final int viewDistance;
    private final EnumChatVisibility chatVisibility;
    private final boolean chatColors;
    private final int modelCustomisation;
    private final EnumMainHand mainHand;
    private final boolean textFilteringEnabled;

    public PacketPlayInSettings(String language, int viewDistance, EnumChatVisibility chatVisibility, boolean chatColors, int modelBitMask, EnumMainHand mainArm, boolean filterText) {
        this.language = language;
        this.viewDistance = viewDistance;
        this.chatVisibility = chatVisibility;
        this.chatColors = chatColors;
        this.modelCustomisation = modelBitMask;
        this.mainHand = mainArm;
        this.textFilteringEnabled = filterText;
    }

    public PacketPlayInSettings(PacketDataSerializer buf) {
        this.language = buf.readUtf(16);
        this.viewDistance = buf.readByte();
        this.chatVisibility = buf.readEnum(EnumChatVisibility.class);
        this.chatColors = buf.readBoolean();
        this.modelCustomisation = buf.readUnsignedByte();
        this.mainHand = buf.readEnum(EnumMainHand.class);
        this.textFilteringEnabled = buf.readBoolean();
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
    }

    @Override
    public void handle(PacketListenerPlayIn listener) {
        listener.handleClientInformation(this);
    }

    public String getLanguage() {
        return this.language;
    }

    public int getViewDistance() {
        return this.viewDistance;
    }

    public EnumChatVisibility getChatVisibility() {
        return this.chatVisibility;
    }

    public boolean getChatColors() {
        return this.chatColors;
    }

    public int getModelCustomisation() {
        return this.modelCustomisation;
    }

    public EnumMainHand getMainHand() {
        return this.mainHand;
    }

    public boolean isTextFilteringEnabled() {
        return this.textFilteringEnabled;
    }
}
