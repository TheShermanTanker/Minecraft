package net.minecraft.world.level.block.state.properties;

import net.minecraft.util.INamable;

public enum BlockPropertyHalf implements INamable {
    TOP("top"),
    BOTTOM("bottom");

    private final String name;

    private BlockPropertyHalf(String name) {
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
