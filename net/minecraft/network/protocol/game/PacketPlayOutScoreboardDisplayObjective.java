package net.minecraft.network.protocol.game;

import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.scores.ScoreboardObjective;

public class PacketPlayOutScoreboardDisplayObjective implements Packet<PacketListenerPlayOut> {
    private final int slot;
    private final String objectiveName;

    public PacketPlayOutScoreboardDisplayObjective(int slot, @Nullable ScoreboardObjective objective) {
        this.slot = slot;
        if (objective == null) {
            this.objectiveName = "";
        } else {
            this.objectiveName = objective.getName();
        }

    }

    public PacketPlayOutScoreboardDisplayObjective(PacketDataSerializer buf) {
        this.slot = buf.readByte();
        this.objectiveName = buf.readUtf(16);
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeByte(this.slot);
        buf.writeUtf(this.objectiveName);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleSetDisplayObjective(this);
    }

    public int getSlot() {
        return this.slot;
    }

    @Nullable
    public String getObjectiveName() {
        return Objects.equals(this.objectiveName, "") ? null : this.objectiveName;
    }
}
