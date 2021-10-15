package net.minecraft.world.level.block.state.properties;

import net.minecraft.util.INamable;

public enum BlockPropertySlabType implements INamable {
    TOP("top"),
    BOTTOM("bottom"),
    DOUBLE("double");

    private final String name;

    private BlockPropertySlabType(String name) {
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
