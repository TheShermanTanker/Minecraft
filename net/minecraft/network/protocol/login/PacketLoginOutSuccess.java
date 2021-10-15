package net.minecraft.network.protocol.login;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.MinecraftSerializableUUID;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class PacketLoginOutSuccess implements Packet<PacketLoginOutListener> {
    private final GameProfile gameProfile;

    public PacketLoginOutSuccess(GameProfile profile) {
        this.gameProfile = profile;
    }

    public PacketLoginOutSuccess(PacketDataSerializer buf) {
        int[] is = new int[4];

        for(int i = 0; i < is.length; ++i) {
            is[i] = buf.readInt();
        }

        UUID uUID = MinecraftSerializableUUID.uuidFromIntArray(is);
        String string = buf.readUtf(16);
        this.gameProfile = new GameProfile(uUID, string);
    }

    @Override
    public void write(PacketDataSerializer buf) {
        for(int i : MinecraftSerializableUUID.uuidToIntArray(this.gameProfile.getId())) {
            buf.writeInt(i);
        }

        buf.writeUtf(this.gameProfile.getName());
    }

    @Override
    public void handle(PacketLoginOutListener listener) {
        listener.handleGameProfile(this);
    }

    public GameProfile getGameProfile() {
        return this.gameProfile;
    }
}
