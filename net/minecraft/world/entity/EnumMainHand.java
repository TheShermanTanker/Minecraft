package net.minecraft.world.entity;

import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;

public enum EnumMainHand {
    LEFT(new ChatMessage("options.mainHand.left")),
    RIGHT(new ChatMessage("options.mainHand.right"));

    private final IChatBaseComponent name;

    private EnumMainHand(IChatBaseComponent optionName) {
        this.name = optionName;
    }

    public EnumMainHand getOpposite() {
        return this == LEFT ? RIGHT : LEFT;
    }

    @Override
    public String toString() {
        return this.name.getString();
    }

    public IChatBaseComponent getName() {
        return this.name;
    }
}
