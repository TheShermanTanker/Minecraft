package net.minecraft.server.network;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.game.PacketPlayOutKickDisconnect;
import net.minecraft.network.protocol.login.PacketLoginInCustomPayload;
import net.minecraft.network.protocol.login.PacketLoginInEncryptionBegin;
import net.minecraft.network.protocol.login.PacketLoginInListener;
import net.minecraft.network.protocol.login.PacketLoginInStart;
import net.minecraft.network.protocol.login.PacketLoginOutDisconnect;
import net.minecraft.network.protocol.login.PacketLoginOutEncryptionBegin;
import net.minecraft.network.protocol.login.PacketLoginOutSetCompression;
import net.minecraft.network.protocol.login.PacketLoginOutSuccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.util.CryptographyException;
import net.minecraft.util.MinecraftEncryption;
import net.minecraft.world.entity.player.EntityHuman;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LoginListener implements PacketLoginInListener {
    private static final AtomicInteger UNIQUE_THREAD_ID = new AtomicInteger(0);
    static final Logger LOGGER = LogManager.getLogger();
    private static final int MAX_TICKS_BEFORE_LOGIN = 600;
    private static final Random RANDOM = new Random();
    private final byte[] nonce = new byte[4];
    final MinecraftServer server;
    public final NetworkManager connection;
    public LoginListener.EnumProtocolState state = LoginListener.EnumProtocolState.HELLO;
    private int tick;
    @Nullable
    public GameProfile gameProfile;
    private final String serverId = "";
    @Nullable
    private EntityPlayer delayedAcceptPlayer;

    public LoginListener(MinecraftServer server, NetworkManager connection) {
        this.server = server;
        this.connection = connection;
        RANDOM.nextBytes(this.nonce);
    }

    public void tick() {
        if (this.state == LoginListener.EnumProtocolState.READY_TO_ACCEPT) {
            this.handleAcceptedLogin();
        } else if (this.state == LoginListener.EnumProtocolState.DELAY_ACCEPT) {
            EntityPlayer serverPlayer = this.server.getPlayerList().getPlayer(this.gameProfile.getId());
            if (serverPlayer == null) {
                this.state = LoginListener.EnumProtocolState.READY_TO_ACCEPT;
                this.placeNewPlayer(this.delayedAcceptPlayer);
                this.delayedAcceptPlayer = null;
            }
        }

        if (this.tick++ == 600) {
            this.disconnect(new ChatMessage("multiplayer.disconnect.slow_login"));
        }

    }

    @Override
    public NetworkManager getConnection() {
        return this.connection;
    }

    public void disconnect(IChatBaseComponent reason) {
        try {
            LOGGER.info("Disconnecting {}: {}", this.getUserName(), reason.getString());
            this.connection.sendPacket(new PacketLoginOutDisconnect(reason));
            this.connection.close(reason);
        } catch (Exception var3) {
            LOGGER.error("Error whilst disconnecting player", (Throwable)var3);
        }

    }

    public void handleAcceptedLogin() {
        if (!this.gameProfile.isComplete()) {
            this.gameProfile = this.createFakeProfile(this.gameProfile);
        }

        IChatBaseComponent component = this.server.getPlayerList().attemptLogin(this.connection.getSocketAddress(), this.gameProfile);
        if (component != null) {
            this.disconnect(component);
        } else {
            this.state = LoginListener.EnumProtocolState.ACCEPTED;
            if (this.server.getCompressionThreshold() >= 0 && !this.connection.isLocal()) {
                this.connection.send(new PacketLoginOutSetCompression(this.server.getCompressionThreshold()), (channelFuture) -> {
                    this.connection.setCompressionLevel(this.server.getCompressionThreshold(), true);
                });
            }

            this.connection.sendPacket(new PacketLoginOutSuccess(this.gameProfile));
            EntityPlayer serverPlayer = this.server.getPlayerList().getPlayer(this.gameProfile.getId());

            try {
                EntityPlayer serverPlayer2 = this.server.getPlayerList().processLogin(this.gameProfile);
                if (serverPlayer != null) {
                    this.state = LoginListener.EnumProtocolState.DELAY_ACCEPT;
                    this.delayedAcceptPlayer = serverPlayer2;
                } else {
                    this.placeNewPlayer(serverPlayer2);
                }
            } catch (Exception var5) {
                LOGGER.error("Couldn't place player in world", (Throwable)var5);
                IChatBaseComponent component2 = new ChatMessage("multiplayer.disconnect.invalid_player_data");
                this.connection.sendPacket(new PacketPlayOutKickDisconnect(component2));
                this.connection.close(component2);
            }
        }

    }

    private void placeNewPlayer(EntityPlayer player) {
        this.server.getPlayerList().placeNewPlayer(this.connection, player);
    }

    @Override
    public void onDisconnect(IChatBaseComponent reason) {
        LOGGER.info("{} lost connection: {}", this.getUserName(), reason.getString());
    }

    public String getUserName() {
        return this.gameProfile != null ? this.gameProfile + " (" + this.connection.getSocketAddress() + ")" : String.valueOf((Object)this.connection.getSocketAddress());
    }

    @Override
    public void handleHello(PacketLoginInStart packet) {
        Validate.validState(this.state == LoginListener.EnumProtocolState.HELLO, "Unexpected hello packet");
        this.gameProfile = packet.getGameProfile();
        if (this.server.getOnlineMode() && !this.connection.isLocal()) {
            this.state = LoginListener.EnumProtocolState.KEY;
            this.connection.sendPacket(new PacketLoginOutEncryptionBegin("", this.server.getKeyPair().getPublic().getEncoded(), this.nonce));
        } else {
            this.state = LoginListener.EnumProtocolState.READY_TO_ACCEPT;
        }

    }

    @Override
    public void handleKey(PacketLoginInEncryptionBegin packet) {
        Validate.validState(this.state == LoginListener.EnumProtocolState.KEY, "Unexpected key packet");
        PrivateKey privateKey = this.server.getKeyPair().getPrivate();

        final String string;
        try {
            if (!Arrays.equals(this.nonce, packet.getNonce(privateKey))) {
                throw new IllegalStateException("Protocol error");
            }

            SecretKey secretKey = packet.a(privateKey);
            Cipher cipher = MinecraftEncryption.getCipher(2, secretKey);
            Cipher cipher2 = MinecraftEncryption.getCipher(1, secretKey);
            string = (new BigInteger(MinecraftEncryption.digestData("", this.server.getKeyPair().getPublic(), secretKey))).toString(16);
            this.state = LoginListener.EnumProtocolState.AUTHENTICATING;
            this.connection.setEncryptionKey(cipher, cipher2);
        } catch (CryptographyException var7) {
            throw new IllegalStateException("Protocol error", var7);
        }

        Thread thread = new Thread("User Authenticator #" + UNIQUE_THREAD_ID.incrementAndGet()) {
            @Override
            public void run() {
                GameProfile gameProfile = LoginListener.this.gameProfile;

                try {
                    LoginListener.this.gameProfile = LoginListener.this.server.getMinecraftSessionService().hasJoinedServer(new GameProfile((UUID)null, gameProfile.getName()), string, this.getAddress());
                    if (LoginListener.this.gameProfile != null) {
                        LoginListener.LOGGER.info("UUID of player {} is {}", LoginListener.this.gameProfile.getName(), LoginListener.this.gameProfile.getId());
                        LoginListener.this.state = LoginListener.EnumProtocolState.READY_TO_ACCEPT;
                    } else if (LoginListener.this.server.isEmbeddedServer()) {
                        LoginListener.LOGGER.warn("Failed to verify username but will let them in anyway!");
                        LoginListener.this.gameProfile = LoginListener.this.createFakeProfile(gameProfile);
                        LoginListener.this.state = LoginListener.EnumProtocolState.READY_TO_ACCEPT;
                    } else {
                        LoginListener.this.disconnect(new ChatMessage("multiplayer.disconnect.unverified_username"));
                        LoginListener.LOGGER.error("Username '{}' tried to join with an invalid session", (Object)gameProfile.getName());
                    }
                } catch (AuthenticationUnavailableException var3) {
                    if (LoginListener.this.server.isEmbeddedServer()) {
                        LoginListener.LOGGER.warn("Authentication servers are down but will let them in anyway!");
                        LoginListener.this.gameProfile = LoginListener.this.createFakeProfile(gameProfile);
                        LoginListener.this.state = LoginListener.EnumProtocolState.READY_TO_ACCEPT;
                    } else {
                        LoginListener.this.disconnect(new ChatMessage("multiplayer.disconnect.authservers_down"));
                        LoginListener.LOGGER.error("Couldn't verify username because servers are unavailable");
                    }
                }

            }

            @Nullable
            private InetAddress getAddress() {
                SocketAddress socketAddress = LoginListener.this.connection.getSocketAddress();
                return LoginListener.this.server.getPreventProxyConnections() && socketAddress instanceof InetSocketAddress ? ((InetSocketAddress)socketAddress).getAddress() : null;
            }
        };
        thread.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER));
        thread.start();
    }

    @Override
    public void handleCustomQueryPacket(PacketLoginInCustomPayload packet) {
        this.disconnect(new ChatMessage("multiplayer.disconnect.unexpected_query_response"));
    }

    protected GameProfile createFakeProfile(GameProfile profile) {
        UUID uUID = EntityHuman.getOfflineUUID(profile.getName());
        return new GameProfile(uUID, profile.getName());
    }

    public static enum EnumProtocolState {
        HELLO,
        KEY,
        AUTHENTICATING,
        NEGOTIATING,
        READY_TO_ACCEPT,
        DELAY_ACCEPT,
        ACCEPTED;
    }
}
