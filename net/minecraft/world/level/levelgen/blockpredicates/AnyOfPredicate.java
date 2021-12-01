package net.minecraft.world.level.levelgen.blockpredicates;

import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.GeneratorAccessSeed;

class AnyOfPredicate extends CombiningPredicate {
    public static final Codec<AnyOfPredicate> CODEC = codec(AnyOfPredicate::new);

    public AnyOfPredicate(List<BlockPredicate> predicates) {
        super(predicates);
    }

    @Override
    public boolean test(GeneratorAccessSeed worldGenLevel, BlockPosition blockPos) {
        for(BlockPredicate blockPredicate : this.predicates) {
            if (blockPredicate.test(worldGenLevel, blockPos)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public BlockPredicateType<?> type() {
        return BlockPredicateType.ANY_OF;
    }
}
