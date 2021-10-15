package net.minecraft.network.chat;

import java.util.function.UnaryOperator;
import net.minecraft.EnumChatFormat;

public interface IChatMutableComponent extends IChatBaseComponent {
    IChatMutableComponent setChatModifier(ChatModifier style);

    default IChatMutableComponent append(String text) {
        return this.addSibling(new ChatComponentText(text));
    }

    IChatMutableComponent addSibling(IChatBaseComponent text);

    default IChatMutableComponent format(UnaryOperator<ChatModifier> styleUpdater) {
        this.setChatModifier(styleUpdater.apply(this.getChatModifier()));
        return this;
    }

    default IChatMutableComponent withStyle(ChatModifier styleOverride) {
        this.setChatModifier(styleOverride.setChatModifier(this.getChatModifier()));
        return this;
    }

    default IChatMutableComponent withStyle(EnumChatFormat... formattings) {
        this.setChatModifier(this.getChatModifier().applyFormats(formattings));
        return this;
    }

    default IChatMutableComponent withStyle(EnumChatFormat formatting) {
        this.setChatModifier(this.getChatModifier().applyFormat(formatting));
        return this;
    }
}
