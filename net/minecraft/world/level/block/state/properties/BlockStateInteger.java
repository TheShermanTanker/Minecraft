package net.minecraft.world.level.block.state.properties;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public class BlockStateInteger extends IBlockState<Integer> {
    private final ImmutableSet<Integer> values;

    protected BlockStateInteger(String name, int min, int max) {
        super(name, Integer.class);
        if (min < 0) {
            throw new IllegalArgumentException("Min value of " + name + " must be 0 or greater");
        } else if (max <= min) {
            throw new IllegalArgumentException("Max value of " + name + " must be greater than min (" + min + ")");
        } else {
            Set<Integer> set = Sets.newHashSet();

            for(int i = min; i <= max; ++i) {
                set.add(i);
            }

            this.values = ImmutableSet.copyOf(set);
        }
    }

    @Override
    public Collection<Integer> getValues() {
        return this.values;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object instanceof BlockStateInteger && super.equals(object)) {
            BlockStateInteger integerProperty = (BlockStateInteger)object;
            return this.values.equals(integerProperty.values);
        } else {
            return false;
        }
    }

    @Override
    public int generateHashCode() {
        return 31 * super.generateHashCode() + this.values.hashCode();
    }

    public static BlockStateInteger of(String name, int min, int max) {
        return new BlockStateInteger(name, min, max);
    }

    @Override
    public Optional<Integer> getValue(String name) {
        try {
            Integer integer = Integer.valueOf(name);
            return this.values.contains(integer) ? Optional.of(integer) : Optional.empty();
        } catch (NumberFormatException var3) {
            return Optional.empty();
        }
    }

    @Override
    public String getName(Integer value) {
        return value.toString();
    }
}
