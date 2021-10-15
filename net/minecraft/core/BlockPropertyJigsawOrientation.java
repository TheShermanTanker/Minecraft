package net.minecraft.core;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.SystemUtils;
import net.minecraft.util.INamable;

public enum BlockPropertyJigsawOrientation implements INamable {
    DOWN_EAST("down_east", EnumDirection.DOWN, EnumDirection.EAST),
    DOWN_NORTH("down_north", EnumDirection.DOWN, EnumDirection.NORTH),
    DOWN_SOUTH("down_south", EnumDirection.DOWN, EnumDirection.SOUTH),
    DOWN_WEST("down_west", EnumDirection.DOWN, EnumDirection.WEST),
    UP_EAST("up_east", EnumDirection.UP, EnumDirection.EAST),
    UP_NORTH("up_north", EnumDirection.UP, EnumDirection.NORTH),
    UP_SOUTH("up_south", EnumDirection.UP, EnumDirection.SOUTH),
    UP_WEST("up_west", EnumDirection.UP, EnumDirection.WEST),
    WEST_UP("west_up", EnumDirection.WEST, EnumDirection.UP),
    EAST_UP("east_up", EnumDirection.EAST, EnumDirection.UP),
    NORTH_UP("north_up", EnumDirection.NORTH, EnumDirection.UP),
    SOUTH_UP("south_up", EnumDirection.SOUTH, EnumDirection.UP);

    private static final Int2ObjectMap<BlockPropertyJigsawOrientation> LOOKUP_TOP_FRONT = SystemUtils.make(new Int2ObjectOpenHashMap<>(values().length), (int2ObjectOpenHashMap) -> {
        for(BlockPropertyJigsawOrientation frontAndTop : values()) {
            int2ObjectOpenHashMap.put(lookupKey(frontAndTop.front, frontAndTop.top), frontAndTop);
        }

    });
    private final String name;
    private final EnumDirection top;
    private final EnumDirection front;

    private static int lookupKey(EnumDirection facing, EnumDirection rotation) {
        return rotation.ordinal() << 3 | facing.ordinal();
    }

    private BlockPropertyJigsawOrientation(String name, EnumDirection facing, EnumDirection rotation) {
        this.name = name;
        this.front = facing;
        this.top = rotation;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public static BlockPropertyJigsawOrientation fromFrontAndTop(EnumDirection facing, EnumDirection rotation) {
        int i = lookupKey(facing, rotation);
        return LOOKUP_TOP_FRONT.get(i);
    }

    public EnumDirection front() {
        return this.front;
    }

    public EnumDirection top() {
        return this.top;
    }
}
