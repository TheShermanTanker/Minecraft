package net.minecraft.network.protocol.game;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.EnumChatFormat;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.scores.ScoreboardTeam;

public class PacketPlayOutScoreboardTeam implements Packet<PacketListenerPlayOut> {
    private static final int METHOD_ADD = 0;
    private static final int METHOD_REMOVE = 1;
    private static final int METHOD_CHANGE = 2;
    private static final int METHOD_JOIN = 3;
    private static final int METHOD_LEAVE = 4;
    private static final int MAX_VISIBILITY_LENGTH = 40;
    private static final int MAX_COLLISION_LENGTH = 40;
    private final int method;
    private final String name;
    private final Collection<String> players;
    private final Optional<PacketPlayOutScoreboardTeam.Parameters> parameters;

    private PacketPlayOutScoreboardTeam(String teamName, int packetType, Optional<PacketPlayOutScoreboardTeam.Parameters> team, Collection<String> playerNames) {
        this.name = teamName;
        this.method = packetType;
        this.parameters = team;
        this.players = ImmutableList.copyOf(playerNames);
    }

    public static PacketPlayOutScoreboardTeam createAddOrModifyPacket(ScoreboardTeam team, boolean updatePlayers) {
        return new PacketPlayOutScoreboardTeam(team.getName(), updatePlayers ? 0 : 2, Optional.of(new PacketPlayOutScoreboardTeam.Parameters(team)), (Collection<String>)(updatePlayers ? team.getPlayerNameSet() : ImmutableList.of()));
    }

    public static PacketPlayOutScoreboardTeam createRemovePacket(ScoreboardTeam team) {
        return new PacketPlayOutScoreboardTeam(team.getName(), 1, Optional.empty(), ImmutableList.of());
    }

    public static PacketPlayOutScoreboardTeam createPlayerPacket(ScoreboardTeam team, String playerName, PacketPlayOutScoreboardTeam.Action operation) {
        return new PacketPlayOutScoreboardTeam(team.getName(), operation == PacketPlayOutScoreboardTeam.Action.ADD ? 3 : 4, Optional.empty(), ImmutableList.of(playerName));
    }

    public PacketPlayOutScoreboardTeam(PacketDataSerializer buf) {
        this.name = buf.readUtf(16);
        this.method = buf.readByte();
        if (shouldHaveParameters(this.method)) {
            this.parameters = Optional.of(new PacketPlayOutScoreboardTeam.Parameters(buf));
        } else {
            this.parameters = Optional.empty();
        }

        if (shouldHavePlayerList(this.method)) {
            this.players = buf.readList(PacketDataSerializer::readUtf);
        } else {
            this.players = ImmutableList.of();
        }

    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeUtf(this.name);
        buf.writeByte(this.method);
        if (shouldHaveParameters(this.method)) {
            this.parameters.orElseThrow(() -> {
                return new IllegalStateException("Parameters not present, but method is" + this.method);
            }).write(buf);
        }

        if (shouldHavePlayerList(this.method)) {
            buf.writeCollection(this.players, PacketDataSerializer::writeUtf);
        }

    }

    private static boolean shouldHavePlayerList(int packetType) {
        return packetType == 0 || packetType == 3 || packetType == 4;
    }

    private static boolean shouldHaveParameters(int packetType) {
        return packetType == 0 || packetType == 2;
    }

    @Nullable
    public PacketPlayOutScoreboardTeam.Action getPlayerAction() {
        switch(this.method) {
        case 0:
        case 3:
            return PacketPlayOutScoreboardTeam.Action.ADD;
        case 1:
        case 2:
        default:
            return null;
        case 4:
            return PacketPlayOutScoreboardTeam.Action.REMOVE;
        }
    }

    @Nullable
    public PacketPlayOutScoreboardTeam.Action getTeamAction() {
        switch(this.method) {
        case 0:
            return PacketPlayOutScoreboardTeam.Action.ADD;
        case 1:
            return PacketPlayOutScoreboardTeam.Action.REMOVE;
        default:
            return null;
        }
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleSetPlayerTeamPacket(this);
    }

    public String getName() {
        return this.name;
    }

    public Collection<String> getPlayers() {
        return this.players;
    }

    public Optional<PacketPlayOutScoreboardTeam.Parameters> getParameters() {
        return this.parameters;
    }

    public static enum Action {
        ADD,
        REMOVE;
    }

    public static class Parameters {
        private final IChatBaseComponent displayName;
        private final IChatBaseComponent playerPrefix;
        private final IChatBaseComponent playerSuffix;
        private final String nametagVisibility;
        private final String collisionRule;
        private final EnumChatFormat color;
        private final int options;

        public Parameters(ScoreboardTeam team) {
            this.displayName = team.getDisplayName();
            this.options = team.packOptionData();
            this.nametagVisibility = team.getNameTagVisibility().name;
            this.collisionRule = team.getCollisionRule().name;
            this.color = team.getColor();
            this.playerPrefix = team.getPrefix();
            this.playerSuffix = team.getSuffix();
        }

        public Parameters(PacketDataSerializer buf) {
            this.displayName = buf.readComponent();
            this.options = buf.readByte();
            this.nametagVisibility = buf.readUtf(40);
            this.collisionRule = buf.readUtf(40);
            this.color = buf.readEnum(EnumChatFormat.class);
            this.playerPrefix = buf.readComponent();
            this.playerSuffix = buf.readComponent();
        }

        public IChatBaseComponent getDisplayName() {
            return this.displayName;
        }

        public int getOptions() {
            return this.options;
        }

        public EnumChatFormat getColor() {
            return this.color;
        }

        public String getNametagVisibility() {
            return this.nametagVisibility;
        }

        public String getCollisionRule() {
            return this.collisionRule;
        }

        public IChatBaseComponent getPlayerPrefix() {
            return this.playerPrefix;
        }

        public IChatBaseComponent getPlayerSuffix() {
            return this.playerSuffix;
        }

        public void write(PacketDataSerializer buf) {
            buf.writeComponent(this.displayName);
            buf.writeByte(this.options);
            buf.writeUtf(this.nametagVisibility);
            buf.writeUtf(this.collisionRule);
            buf.writeEnum(this.color);
            buf.writeComponent(this.playerPrefix);
            buf.writeComponent(this.playerSuffix);
        }
    }
}
