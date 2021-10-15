package net.minecraft.world.entity.animal.horse;

import java.util.Arrays;
import java.util.Comparator;

public enum HorseColor {
    WHITE(0),
    CREAMY(1),
    CHESTNUT(2),
    BROWN(3),
    BLACK(4),
    GRAY(5),
    DARKBROWN(6);

    private static final HorseColor[] BY_ID = Arrays.stream(values()).sorted(Comparator.comparingInt(HorseColor::getId)).toArray((i) -> {
        return new HorseColor[i];
    });
    private final int id;

    private HorseColor(int index) {
        this.id = index;
    }

    public int getId() {
        return this.id;
    }

    public static HorseColor byId(int index) {
        return BY_ID[index % BY_ID.length];
    }
}
