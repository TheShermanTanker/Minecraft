package net.minecraft.world.level.block.state.properties;

import net.minecraft.util.INamable;

public enum BlockPropertyBambooSize implements INamable {
    NONE("none"),
    SMALL("small"),
    LARGE("large");

    private final String name;

    private BlockPropertyBambooSize(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
