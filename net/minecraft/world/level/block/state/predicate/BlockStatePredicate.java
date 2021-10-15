package net.minecraft.world.level.block.state.predicate;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.IBlockState;

public class BlockStatePredicate implements Predicate<IBlockData> {
    public static final Predicate<IBlockData> ANY = (blockState) -> {
        return true;
    };
    private final BlockStateList<Block, IBlockData> definition;
    private final Map<IBlockState<?>, Predicate<Object>> properties = Maps.newHashMap();

    private BlockStatePredicate(BlockStateList<Block, IBlockData> manager) {
        this.definition = manager;
    }

    public static BlockStatePredicate forBlock(Block block) {
        return new BlockStatePredicate(block.getStates());
    }

    @Override
    public boolean test(@Nullable IBlockData blockState) {
        if (blockState != null && blockState.getBlock().equals(this.definition.getBlock())) {
            if (this.properties.isEmpty()) {
                return true;
            } else {
                for(Entry<IBlockState<?>, Predicate<Object>> entry : this.properties.entrySet()) {
                    if (!this.applies(blockState, entry.getKey(), entry.getValue())) {
                        return false;
                    }
                }

                return true;
            }
        } else {
            return false;
        }
    }

    protected <T extends Comparable<T>> boolean applies(IBlockData blockState, IBlockState<T> property, Predicate<Object> predicate) {
        T comparable = blockState.get(property);
        return predicate.test(comparable);
    }

    public <V extends Comparable<V>> BlockStatePredicate where(IBlockState<V> property, Predicate<Object> predicate) {
        if (!this.definition.getProperties().contains(property)) {
            throw new IllegalArgumentException(this.definition + " cannot support property " + property);
        } else {
            this.properties.put(property, predicate);
            return this;
        }
    }
}
