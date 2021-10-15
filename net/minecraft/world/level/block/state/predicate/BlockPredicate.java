package net.minecraft.world.level.block.state.predicate;

import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.IBlockData;

public class BlockPredicate implements Predicate<IBlockData> {
    private final Block block;

    public BlockPredicate(Block block) {
        this.block = block;
    }

    public static BlockPredicate forBlock(Block block) {
        return new BlockPredicate(block);
    }

    @Override
    public boolean test(@Nullable IBlockData blockState) {
        return blockState != null && blockState.is(this.block);
    }
}
