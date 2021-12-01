package net.minecraft.network.protocol.game;

import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.ScoreboardServer;

public class PacketPlayOutScoreboardScore implements Packet<PacketListenerPlayOut> {
    private final String owner;
    @Nullable
    private final String objectiveName;
    private final int score;
    private final ScoreboardServer.Action method;

    public PacketPlayOutScoreboardScore(ScoreboardServer.Action mode, @Nullable String objectiveName, String playerName, int score) {
        if (mode != ScoreboardServer.Action.REMOVE && objectiveName == null) {
            throw new IllegalArgumentException("Need an objective name");
        } else {
            this.owner = playerName;
            this.objectiveName = objectiveName;
            this.score = score;
            this.method = mode;
        }
    }

    public PacketPlayOutScoreboardScore(PacketDataSerializer buf) {
        this.owner = buf.readUtf();
        this.method = buf.readEnum(ScoreboardServer.Action.class);
        String string = buf.readUtf();
        this.objectiveName = Objects.equals(string, "") ? null : string;
        if (this.method != ScoreboardServer.Action.REMOVE) {
            this.score = buf.readVarInt();
        } else {
            this.score = 0;
        }

    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeUtf(this.owner);
        buf.writeEnum(this.method);
        buf.writeUtf(this.objectiveName == null ? "" : this.objectiveName);
        if (this.method != ScoreboardServer.Action.REMOVE) {
            buf.writeVarInt(this.score);
        }

    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleSetScore(this);
    }

    public String getOwner() {
        return this.owner;
    }

    @Nullable
    public String getObjectiveName() {
        return this.objectiveName;
    }

    public int getScore() {
        return this.score;
    }

    public ScoreboardServer.Action getMethod() {
        return this.method;
    }
}
