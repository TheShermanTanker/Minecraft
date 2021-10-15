package net.minecraft.network.protocol.login;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;

public class PacketLoginInStart implements Packet<PacketLoginInListener> {
    private final GameProfile gameProfile;

    public PacketLoginInStart(GameProfile profile) {
        this.gameProfile = profile;
    }

    public PacketLoginInStart(PacketDataSerializer buf) {
        this.gameProfile = new GameProfile((UUID)null, buf.readUtf(16));
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeUtf(this.gameProfile.getName());
    }

    @Override
    public void handle(PacketLoginInListener listener) {
        listener.handleHello(this);
    }

    public GameProfile getGameProfile() {
        return this.gameProfile;
    }
}
