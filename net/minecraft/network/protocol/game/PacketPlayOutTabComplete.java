package net.minecraft.network.protocol.game;

import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import java.util.List;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.chat.ChatComponentUtils;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.Packet;

public class PacketPlayOutTabComplete implements Packet<PacketListenerPlayOut> {
    private final int id;
    private final Suggestions suggestions;

    public PacketPlayOutTabComplete(int completionId, Suggestions suggestions) {
        this.id = completionId;
        this.suggestions = suggestions;
    }

    public PacketPlayOutTabComplete(PacketDataSerializer buf) {
        this.id = buf.readVarInt();
        int i = buf.readVarInt();
        int j = buf.readVarInt();
        StringRange stringRange = StringRange.between(i, i + j);
        List<Suggestion> list = buf.readList((bufx) -> {
            String string = bufx.readUtf();
            IChatBaseComponent component = bufx.readBoolean() ? bufx.readComponent() : null;
            return new Suggestion(stringRange, string, component);
        });
        this.suggestions = new Suggestions(stringRange, list);
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeVarInt(this.id);
        buf.writeVarInt(this.suggestions.getRange().getStart());
        buf.writeVarInt(this.suggestions.getRange().getLength());
        buf.writeCollection(this.suggestions.getList(), (bufx, suggestion) -> {
            bufx.writeUtf(suggestion.getText());
            bufx.writeBoolean(suggestion.getTooltip() != null);
            if (suggestion.getTooltip() != null) {
                bufx.writeComponent(ChatComponentUtils.fromMessage(suggestion.getTooltip()));
            }

        });
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleCommandSuggestions(this);
    }

    public int getId() {
        return this.id;
    }

    public Suggestions getSuggestions() {
        return this.suggestions;
    }
}
