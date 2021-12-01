package net.minecraft.world.level.levelgen.blockpredicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BaseBlockPosition;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.block.state.IBlockData;

public class WouldSurvivePredicate implements BlockPredicate {
    public static final Codec<WouldSurvivePredicate> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(BaseBlockPosition.offsetCodec(16).optionalFieldOf("offset", BaseBlockPosition.ZERO).forGetter((predicate) -> {
            return predicate.offset;
        }), IBlockData.CODEC.fieldOf("state").forGetter((predicate) -> {
            return predicate.state;
        })).apply(instance, WouldSurvivePredicate::new);
    });
    private final BaseBlockPosition offset;
    private final IBlockData state;

    protected WouldSurvivePredicate(BaseBlockPosition offset, IBlockData state) {
        this.offset = offset;
        this.state = state;
    }

    @Override
    public boolean test(GeneratorAccessSeed worldGenLevel, BlockPosition blockPos) {
        return this.state.canPlace(worldGenLevel, blockPos.offset(this.offset));
    }

    @Override
    public BlockPredicateType<?> type() {
        return BlockPredicateType.WOULD_SURVIVE;
    }
}
