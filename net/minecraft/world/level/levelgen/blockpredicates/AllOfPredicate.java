package net.minecraft.world.level.levelgen.blockpredicates;

import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.GeneratorAccessSeed;

class AllOfPredicate extends CombiningPredicate {
    public static final Codec<AllOfPredicate> CODEC = codec(AllOfPredicate::new);

    public AllOfPredicate(List<BlockPredicate> predicates) {
        super(predicates);
    }

    @Override
    public boolean test(GeneratorAccessSeed worldGenLevel, BlockPosition blockPos) {
        for(BlockPredicate blockPredicate : this.predicates) {
            if (!blockPredicate.test(worldGenLevel, blockPos)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public BlockPredicateType<?> type() {
        return BlockPredicateType.ALL_OF;
    }
}
