package net.minecraft.world.level.material;

import com.google.common.base.Preconditions;

public enum MaterialColor$Brightness {
    LOW(0, 180),
    NORMAL(1, 220),
    HIGH(2, 255),
    LOWEST(3, 135);

    private static final MaterialColor$Brightness[] VALUES = new MaterialColor$Brightness[]{LOW, NORMAL, HIGH, LOWEST};
    public final int id;
    public final int modifier;

    private MaterialColor$Brightness(int id, int brightness) {
        this.id = id;
        this.modifier = brightness;
    }

    public static MaterialColor$Brightness byId(int id) {
        Preconditions.checkPositionIndex(id, VALUES.length, "brightness id");
        return byIdUnsafe(id);
    }

    static MaterialColor$Brightness byIdUnsafe(int id) {
        return VALUES[id];
    }
}
