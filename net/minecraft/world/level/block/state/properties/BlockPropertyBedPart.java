package net.minecraft.world.level.block.state.properties;

import net.minecraft.util.INamable;

public enum BlockPropertyBedPart implements INamable {
    HEAD("head"),
    FOOT("foot");

    private final String name;

    private BlockPropertyBedPart(String name) {
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
