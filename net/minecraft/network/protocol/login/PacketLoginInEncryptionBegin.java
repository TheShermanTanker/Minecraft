package net.minecraft.network.protocol.login;

import java.security.PrivateKey;
import java.security.PublicKey;
import javax.crypto.SecretKey;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.util.CryptographyException;
import net.minecraft.util.MinecraftEncryption;

public class PacketLoginInEncryptionBegin implements Packet<PacketLoginInListener> {
    private final byte[] keybytes;
    private final byte[] nonce;

    public PacketLoginInEncryptionBegin(SecretKey secretKey, PublicKey publicKey, byte[] nonce) throws CryptographyException {
        this.keybytes = MinecraftEncryption.encryptUsingKey(publicKey, secretKey.getEncoded());
        this.nonce = MinecraftEncryption.encryptUsingKey(publicKey, nonce);
    }

    public PacketLoginInEncryptionBegin(PacketDataSerializer buf) {
        this.keybytes = buf.readByteArray();
        this.nonce = buf.readByteArray();
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeByteArray(this.keybytes);
        buf.writeByteArray(this.nonce);
    }

    @Override
    public void handle(PacketLoginInListener listener) {
        listener.handleKey(this);
    }

    public SecretKey a(PrivateKey privateKey) throws CryptographyException {
        return MinecraftEncryption.decryptByteToSecretKey(privateKey, this.keybytes);
    }

    public byte[] getNonce(PrivateKey privateKey) throws CryptographyException {
        return MinecraftEncryption.decryptUsingKey(privateKey, this.nonce);
    }
}
