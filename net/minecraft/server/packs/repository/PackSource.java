package net.minecraft.server.packs.repository;

import net.minecraft.EnumChatFormat;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;

public interface PackSource {
    PackSource DEFAULT = passThrough();
    PackSource BUILT_IN = decorating("pack.source.builtin");
    PackSource WORLD = decorating("pack.source.world");
    PackSource SERVER = decorating("pack.source.server");

    IChatBaseComponent decorate(IChatBaseComponent packName);

    static PackSource passThrough() {
        return (name) -> {
            return name;
        };
    }

    static PackSource decorating(String source) {
        IChatBaseComponent component = new ChatMessage(source);
        return (name) -> {
            return (new ChatMessage("pack.nameAndSource", name, component)).withStyle(EnumChatFormat.GRAY);
        };
    }
}
