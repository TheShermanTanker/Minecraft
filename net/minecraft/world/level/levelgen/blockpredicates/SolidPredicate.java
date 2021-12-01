package net.minecraft.world.level.levelgen.blockpredicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BaseBlockPosition;
import net.minecraft.world.level.block.state.IBlockData;

public class SolidPredicate extends StateTestingPredicate {
    public static final Codec<SolidPredicate> CODEC = RecordCodecBuilder.create((instance) -> {
        return stateTestingCodec(instance).apply(instance, SolidPredicate::new);
    });

    public SolidPredicate(BaseBlockPosition offset) {
        super(offset);
    }

    @Override
    protected boolean test(IBlockData state) {
        return state.getMaterial().isBuildable();
    }

    @Override
    public BlockPredicateType<?> type() {
        return BlockPredicateType.SOLID;
    }
}
