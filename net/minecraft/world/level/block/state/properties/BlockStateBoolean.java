package net.minecraft.world.level.block.state.properties;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Optional;

public class BlockStateBoolean extends IBlockState<Boolean> {
    private final ImmutableSet<Boolean> values = ImmutableSet.of(true, false);

    protected BlockStateBoolean(String name) {
        super(name, Boolean.class);
    }

    @Override
    public Collection<Boolean> getValues() {
        return this.values;
    }

    public static BlockStateBoolean of(String name) {
        return new BlockStateBoolean(name);
    }

    @Override
    public Optional<Boolean> getValue(String name) {
        return !"true".equals(name) && !"false".equals(name) ? Optional.empty() : Optional.of(Boolean.valueOf(name));
    }

    @Override
    public String getName(Boolean value) {
        return value.toString();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object instanceof BlockStateBoolean && super.equals(object)) {
            BlockStateBoolean booleanProperty = (BlockStateBoolean)object;
            return this.values.equals(booleanProperty.values);
        } else {
            return false;
        }
    }

    @Override
    public int generateHashCode() {
        return 31 * super.generateHashCode() + this.values.hashCode();
    }
}
