package net.minecraft.world.level.block.state.properties;

import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.core.EnumDirection;

public class BlockStateDirection extends BlockStateEnum<EnumDirection> {
    protected BlockStateDirection(String name, Collection<EnumDirection> values) {
        super(name, EnumDirection.class, values);
    }

    public static BlockStateDirection of(String name) {
        return create(name, (direction) -> {
            return true;
        });
    }

    public static BlockStateDirection create(String name, Predicate<EnumDirection> filter) {
        return create(name, Arrays.stream(EnumDirection.values()).filter(filter).collect(Collectors.toList()));
    }

    public static BlockStateDirection create(String name, EnumDirection... values) {
        return create(name, Lists.newArrayList(values));
    }

    public static BlockStateDirection create(String name, Collection<EnumDirection> values) {
        return new BlockStateDirection(name, values);
    }
}
