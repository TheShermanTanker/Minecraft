package net.minecraft.world.level.block.state.properties;

import net.minecraft.util.INamable;

public enum BlockPropertyComparatorMode implements INamable {
    COMPARE("compare"),
    SUBTRACT("subtract");

    private final String name;

    private BlockPropertyComparatorMode(String name) {
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
