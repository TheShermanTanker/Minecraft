package net.minecraft.network.protocol.game;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.level.EnumGamemode;

public class PacketPlayOutPlayerInfo implements Packet<PacketListenerPlayOut> {
    private final PacketPlayOutPlayerInfo.EnumPlayerInfoAction action;
    private final List<PacketPlayOutPlayerInfo.PlayerInfoData> entries;

    public PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction action, EntityPlayer... players) {
        this.action = action;
        this.entries = Lists.newArrayListWithCapacity(players.length);

        for(EntityPlayer serverPlayer : players) {
            this.entries.add(new PacketPlayOutPlayerInfo.PlayerInfoData(serverPlayer.getProfile(), serverPlayer.latency, serverPlayer.gameMode.getGameMode(), serverPlayer.getPlayerListName()));
        }

    }

    public PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction action, Collection<EntityPlayer> players) {
        this.action = action;
        this.entries = Lists.newArrayListWithCapacity(players.size());

        for(EntityPlayer serverPlayer : players) {
            this.entries.add(new PacketPlayOutPlayerInfo.PlayerInfoData(serverPlayer.getProfile(), serverPlayer.latency, serverPlayer.gameMode.getGameMode(), serverPlayer.getPlayerListName()));
        }

    }

    public PacketPlayOutPlayerInfo(PacketDataSerializer buf) {
        this.action = buf.readEnum(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.class);
        this.entries = buf.readList(this.action::read);
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeEnum(this.action);
        buf.writeCollection(this.entries, this.action::write);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handlePlayerInfo(this);
    }

    public List<PacketPlayOutPlayerInfo.PlayerInfoData> getEntries() {
        return this.entries;
    }

    public PacketPlayOutPlayerInfo.EnumPlayerInfoAction getAction() {
        return this.action;
    }

    @Nullable
    static IChatBaseComponent readDisplayName(PacketDataSerializer buf) {
        return buf.readBoolean() ? buf.readComponent() : null;
    }

    static void writeDisplayName(PacketDataSerializer buf, @Nullable IChatBaseComponent text) {
        if (text == null) {
            buf.writeBoolean(false);
        } else {
            buf.writeBoolean(true);
            buf.writeComponent(text);
        }

    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("action", this.action).add("entries", this.entries).toString();
    }

    public static enum EnumPlayerInfoAction {
        ADD_PLAYER {
            @Override
            protected PacketPlayOutPlayerInfo.PlayerInfoData read(PacketDataSerializer buf) {
                GameProfile gameProfile = new GameProfile(buf.readUUID(), buf.readUtf(16));
                PropertyMap propertyMap = gameProfile.getProperties();
                buf.readWithCount((bufx) -> {
                    String string = bufx.readUtf();
                    String string2 = bufx.readUtf();
                    if (bufx.readBoolean()) {
                        String string3 = bufx.readUtf();
                        propertyMap.put(string, new Property(string, string2, string3));
                    } else {
                        propertyMap.put(string, new Property(string, string2));
                    }

                });
                EnumGamemode gameType = EnumGamemode.getById(buf.readVarInt());
                int i = buf.readVarInt();
                IChatBaseComponent component = PacketPlayOutPlayerInfo.readDisplayName(buf);
                return new PacketPlayOutPlayerInfo.PlayerInfoData(gameProfile, i, gameType, component);
            }

            @Override
            protected void write(PacketDataSerializer buf, PacketPlayOutPlayerInfo.PlayerInfoData entry) {
                buf.writeUUID(entry.getProfile().getId());
                buf.writeUtf(entry.getProfile().getName());
                buf.writeCollection(entry.getProfile().getProperties().values(), (bufx, property) -> {
                    bufx.writeUtf(property.getName());
                    bufx.writeUtf(property.getValue());
                    if (property.hasSignature()) {
                        bufx.writeBoolean(true);
                        bufx.writeUtf(property.getSignature());
                    } else {
                        bufx.writeBoolean(false);
                    }

                });
                buf.writeVarInt(entry.getGameMode().getId());
                buf.writeVarInt(entry.getLatency());
                PacketPlayOutPlayerInfo.writeDisplayName(buf, entry.getDisplayName());
            }
        },
        UPDATE_GAME_MODE {
            @Override
            protected PacketPlayOutPlayerInfo.PlayerInfoData read(PacketDataSerializer buf) {
                GameProfile gameProfile = new GameProfile(buf.readUUID(), (String)null);
                EnumGamemode gameType = EnumGamemode.getById(buf.readVarInt());
                return new PacketPlayOutPlayerInfo.PlayerInfoData(gameProfile, 0, gameType, (IChatBaseComponent)null);
            }

            @Override
            protected void write(PacketDataSerializer buf, PacketPlayOutPlayerInfo.PlayerInfoData entry) {
                buf.writeUUID(entry.getProfile().getId());
                buf.writeVarInt(entry.getGameMode().getId());
            }
        },
        UPDATE_LATENCY {
            @Override
            protected PacketPlayOutPlayerInfo.PlayerInfoData read(PacketDataSerializer buf) {
                GameProfile gameProfile = new GameProfile(buf.readUUID(), (String)null);
                int i = buf.readVarInt();
                return new PacketPlayOutPlayerInfo.PlayerInfoData(gameProfile, i, (EnumGamemode)null, (IChatBaseComponent)null);
            }

            @Override
            protected void write(PacketDataSerializer buf, PacketPlayOutPlayerInfo.PlayerInfoData entry) {
                buf.writeUUID(entry.getProfile().getId());
                buf.writeVarInt(entry.getLatency());
            }
        },
        UPDATE_DISPLAY_NAME {
            @Override
            protected PacketPlayOutPlayerInfo.PlayerInfoData read(PacketDataSerializer buf) {
                GameProfile gameProfile = new GameProfile(buf.readUUID(), (String)null);
                IChatBaseComponent component = PacketPlayOutPlayerInfo.readDisplayName(buf);
                return new PacketPlayOutPlayerInfo.PlayerInfoData(gameProfile, 0, (EnumGamemode)null, component);
            }

            @Override
            protected void write(PacketDataSerializer buf, PacketPlayOutPlayerInfo.PlayerInfoData entry) {
                buf.writeUUID(entry.getProfile().getId());
                PacketPlayOutPlayerInfo.writeDisplayName(buf, entry.getDisplayName());
            }
        },
        REMOVE_PLAYER {
            @Override
            protected PacketPlayOutPlayerInfo.PlayerInfoData read(PacketDataSerializer buf) {
                GameProfile gameProfile = new GameProfile(buf.readUUID(), (String)null);
                return new PacketPlayOutPlayerInfo.PlayerInfoData(gameProfile, 0, (EnumGamemode)null, (IChatBaseComponent)null);
            }

            @Override
            protected void write(PacketDataSerializer buf, PacketPlayOutPlayerInfo.PlayerInfoData entry) {
                buf.writeUUID(entry.getProfile().getId());
            }
        };

        protected abstract PacketPlayOutPlayerInfo.PlayerInfoData read(PacketDataSerializer buf);

        protected abstract void write(PacketDataSerializer buf, PacketPlayOutPlayerInfo.PlayerInfoData entry);
    }

    public static class PlayerInfoData {
        private final int latency;
        private final EnumGamemode gameMode;
        private final GameProfile profile;
        @Nullable
        private final IChatBaseComponent displayName;

        public PlayerInfoData(GameProfile profile, int latency, @Nullable EnumGamemode gameMode, @Nullable IChatBaseComponent displayName) {
            this.profile = profile;
            this.latency = latency;
            this.gameMode = gameMode;
            this.displayName = displayName;
        }

        public GameProfile getProfile() {
            return this.profile;
        }

        public int getLatency() {
            return this.latency;
        }

        public EnumGamemode getGameMode() {
            return this.gameMode;
        }

        @Nullable
        public IChatBaseComponent getDisplayName() {
            return this.displayName;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this).add("latency", this.latency).add("gameMode", this.gameMode).add("profile", this.profile).add("displayName", this.displayName == null ? null : IChatBaseComponent.ChatSerializer.toJson(this.displayName)).toString();
        }
    }
}
