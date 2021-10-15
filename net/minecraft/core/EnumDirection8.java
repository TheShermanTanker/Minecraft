package net.minecraft.core;

import com.google.common.collect.Sets;
import java.util.Arrays;
import java.util.Set;

public enum EnumDirection8 {
    NORTH(EnumDirection.NORTH),
    NORTH_EAST(EnumDirection.NORTH, EnumDirection.EAST),
    EAST(EnumDirection.EAST),
    SOUTH_EAST(EnumDirection.SOUTH, EnumDirection.EAST),
    SOUTH(EnumDirection.SOUTH),
    SOUTH_WEST(EnumDirection.SOUTH, EnumDirection.WEST),
    WEST(EnumDirection.WEST),
    NORTH_WEST(EnumDirection.NORTH, EnumDirection.WEST);

    private final Set<EnumDirection> directions;

    private EnumDirection8(EnumDirection... directions) {
        this.directions = Sets.immutableEnumSet(Arrays.asList(directions));
    }

    public Set<EnumDirection> getDirections() {
        return this.directions;
    }
}
