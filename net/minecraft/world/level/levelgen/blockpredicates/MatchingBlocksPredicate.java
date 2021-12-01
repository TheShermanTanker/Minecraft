package net.minecraft.world.level.levelgen.blockpredicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BaseBlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.IBlockData;

class MatchingBlocksPredicate extends StateTestingPredicate {
    private final List<Block> blocks;
    public static final Codec<MatchingBlocksPredicate> CODEC = RecordCodecBuilder.create((instance) -> {
        return stateTestingCodec(instance).and(IRegistry.BLOCK.byNameCodec().listOf().fieldOf("blocks").forGetter((predicate) -> {
            return predicate.blocks;
        })).apply(instance, MatchingBlocksPredicate::new);
    });

    public MatchingBlocksPredicate(BaseBlockPosition offset, List<Block> blocks) {
        super(offset);
        this.blocks = blocks;
    }

    @Override
    protected boolean test(IBlockData state) {
        return this.blocks.contains(state.getBlock());
    }

    @Override
    public BlockPredicateType<?> type() {
        return BlockPredicateType.MATCHING_BLOCKS;
    }
}
