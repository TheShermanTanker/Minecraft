package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import net.minecraft.core.EnumDirection;
import net.minecraft.util.INamable;

public enum CaveSurface implements INamable {
    CEILING(EnumDirection.UP, 1, "ceiling"),
    FLOOR(EnumDirection.DOWN, -1, "floor");

    public static final Codec<CaveSurface> CODEC = INamable.fromEnum(CaveSurface::values, CaveSurface::byName);
    private final EnumDirection direction;
    private final int y;
    private final String id;
    private static final CaveSurface[] VALUES = values();

    private CaveSurface(EnumDirection direction, int offset, String name) {
        this.direction = direction;
        this.y = offset;
        this.id = name;
    }

    public EnumDirection getDirection() {
        return this.direction;
    }

    public int getY() {
        return this.y;
    }

    public static CaveSurface byName(String name) {
        for(CaveSurface caveSurface : VALUES) {
            if (caveSurface.getSerializedName().equals(name)) {
                return caveSurface;
            }
        }

        throw new IllegalArgumentException("Unknown Surface type: " + name);
    }

    @Override
    public String getSerializedName() {
        return this.id;
    }
}
