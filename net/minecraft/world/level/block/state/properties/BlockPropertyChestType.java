package net.minecraft.world.level.block.state.properties;

import net.minecraft.util.INamable;

public enum BlockPropertyChestType implements INamable {
    SINGLE("single", 0),
    LEFT("left", 2),
    RIGHT("right", 1);

    public static final BlockPropertyChestType[] BY_ID = values();
    private final String name;
    private final int opposite;

    private BlockPropertyChestType(String name, int opposite) {
        this.name = name;
        this.opposite = opposite;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public BlockPropertyChestType getOpposite() {
        return BY_ID[this.opposite];
    }
}
