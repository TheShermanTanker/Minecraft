package net.minecraft.world.level.block.state.properties;

import net.minecraft.util.INamable;

public enum BlockPropertyWallHeight implements INamable {
    NONE("none"),
    LOW("low"),
    TALL("tall");

    private final String name;

    private BlockPropertyWallHeight(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.getSerializedName();
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
