package net.minecraft.world.level.block.state.properties;

import net.minecraft.util.INamable;

public enum BlockPropertyDoorHinge implements INamable {
    LEFT,
    RIGHT;

    @Override
    public String toString() {
        return this.getSerializedName();
    }

    @Override
    public String getSerializedName() {
        return this == LEFT ? "left" : "right";
    }
}
