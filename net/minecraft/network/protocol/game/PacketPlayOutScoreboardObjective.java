package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.scores.ScoreboardObjective;
import net.minecraft.world.scores.criteria.IScoreboardCriteria;

public class PacketPlayOutScoreboardObjective implements Packet<PacketListenerPlayOut> {
    public static final int METHOD_ADD = 0;
    public static final int METHOD_REMOVE = 1;
    public static final int METHOD_CHANGE = 2;
    private final String objectiveName;
    private final IChatBaseComponent displayName;
    private final IScoreboardCriteria.EnumScoreboardHealthDisplay renderType;
    private final int method;

    public PacketPlayOutScoreboardObjective(ScoreboardObjective objective, int mode) {
        this.objectiveName = objective.getName();
        this.displayName = objective.getDisplayName();
        this.renderType = objective.getRenderType();
        this.method = mode;
    }

    public PacketPlayOutScoreboardObjective(PacketDataSerializer buf) {
        this.objectiveName = buf.readUtf(16);
        this.method = buf.readByte();
        if (this.method != 0 && this.method != 2) {
            this.displayName = ChatComponentText.EMPTY;
            this.renderType = IScoreboardCriteria.EnumScoreboardHealthDisplay.INTEGER;
        } else {
            this.displayName = buf.readComponent();
            this.renderType = buf.readEnum(IScoreboardCriteria.EnumScoreboardHealthDisplay.class);
        }

    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeUtf(this.objectiveName);
        buf.writeByte(this.method);
        if (this.method == 0 || this.method == 2) {
            buf.writeComponent(this.displayName);
            buf.writeEnum(this.renderType);
        }

    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleAddObjective(this);
    }

    public String getObjectiveName() {
        return this.objectiveName;
    }

    public IChatBaseComponent getDisplayName() {
        return this.displayName;
    }

    public int getMethod() {
        return this.method;
    }

    public IScoreboardCriteria.EnumScoreboardHealthDisplay getRenderType() {
        return this.renderType;
    }
}
