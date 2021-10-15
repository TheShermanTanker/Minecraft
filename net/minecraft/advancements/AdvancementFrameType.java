package net.minecraft.advancements;

import net.minecraft.EnumChatFormat;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;

public enum AdvancementFrameType {
    TASK("task", 0, EnumChatFormat.GREEN),
    CHALLENGE("challenge", 26, EnumChatFormat.DARK_PURPLE),
    GOAL("goal", 52, EnumChatFormat.GREEN);

    private final String name;
    private final int texture;
    private final EnumChatFormat chatColor;
    private final IChatBaseComponent displayName;

    private AdvancementFrameType(String id, int texV, EnumChatFormat titleFormat) {
        this.name = id;
        this.texture = texV;
        this.chatColor = titleFormat;
        this.displayName = new ChatMessage("advancements.toast." + id);
    }

    public String getName() {
        return this.name;
    }

    public int getTexture() {
        return this.texture;
    }

    public static AdvancementFrameType byName(String name) {
        for(AdvancementFrameType frameType : values()) {
            if (frameType.name.equals(name)) {
                return frameType;
            }
        }

        throw new IllegalArgumentException("Unknown frame type '" + name + "'");
    }

    public EnumChatFormat getChatColor() {
        return this.chatColor;
    }

    public IChatBaseComponent getDisplayName() {
        return this.displayName;
    }
}
