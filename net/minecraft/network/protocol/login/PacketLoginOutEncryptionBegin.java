package net.minecraft.network.protocol.login;

import java.security.PublicKey;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.util.CryptographyException;
import net.minecraft.util.MinecraftEncryption;

public class PacketLoginOutEncryptionBegin implements Packet<PacketLoginOutListener> {
    private final String serverId;
    private final byte[] publicKey;
    private final byte[] nonce;

    public PacketLoginOutEncryptionBegin(String serverId, byte[] publicKey, byte[] nonce) {
        this.serverId = serverId;
        this.publicKey = publicKey;
        this.nonce = nonce;
    }

    public PacketLoginOutEncryptionBegin(PacketDataSerializer buf) {
        this.serverId = buf.readUtf(20);
        this.publicKey = buf.readByteArray();
        this.nonce = buf.readByteArray();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeUtf(this.serverId);
        buf.writeByteArray(this.publicKey);
        buf.writeByteArray(this.nonce);
    }

    @Override
    public void handle(PacketLoginOutListener listener) {
        listener.handleHello(this);
    }

    public String getServerId() {
        return this.serverId;
    }

    public PublicKey getPublicKey() throws CryptographyException {
        return MinecraftEncryption.byteToPublicKey(this.publicKey);
    }

    public byte[] getNonce() {
        return this.nonce;
    }
}
