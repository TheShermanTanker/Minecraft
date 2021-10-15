package net.minecraft.network.chat;

public enum ChatMessageType {
    CHAT((byte)0, false),
    SYSTEM((byte)1, true),
    GAME_INFO((byte)2, true);

    private final byte index;
    private final boolean interrupt;

    private ChatMessageType(byte id, boolean interruptsNarration) {
        this.index = id;
        this.interrupt = interruptsNarration;
    }

    public byte getIndex() {
        return this.index;
    }

    public static ChatMessageType getForIndex(byte id) {
        for(ChatMessageType chatType : values()) {
            if (id == chatType.index) {
                return chatType;
            }
        }

        return CHAT;
    }

    public boolean shouldInterrupt() {
        return this.interrupt;
    }
}
