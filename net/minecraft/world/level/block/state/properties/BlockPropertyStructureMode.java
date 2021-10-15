package net.minecraft.world.level.block.state.properties;

import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.util.INamable;

public enum BlockPropertyStructureMode implements INamable {
    SAVE("save"),
    LOAD("load"),
    CORNER("corner"),
    DATA("data");

    private final String name;
    private final IChatBaseComponent displayName;

    private BlockPropertyStructureMode(String name) {
        this.name = name;
        this.displayName = new ChatMessage("structure_block.mode_info." + name);
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public IChatBaseComponent getDisplayName() {
        return this.displayName;
    }
}
