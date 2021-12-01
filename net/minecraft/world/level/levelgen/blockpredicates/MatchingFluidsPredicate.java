package net.minecraft.world.level.levelgen.blockpredicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BaseBlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.material.FluidType;

class MatchingFluidsPredicate extends StateTestingPredicate {
    private final List<FluidType> fluids;
    public static final Codec<MatchingFluidsPredicate> CODEC = RecordCodecBuilder.create((instance) -> {
        return stateTestingCodec(instance).and(IRegistry.FLUID.byNameCodec().listOf().fieldOf("fluids").forGetter((predicate) -> {
            return predicate.fluids;
        })).apply(instance, MatchingFluidsPredicate::new);
    });

    public MatchingFluidsPredicate(BaseBlockPosition offset, List<FluidType> fluids) {
        super(offset);
        this.fluids = fluids;
    }

    @Override
    protected boolean test(IBlockData state) {
        return this.fluids.contains(state.getFluid().getType());
    }

    @Override
    public BlockPredicateType<?> type() {
        return BlockPredicateType.MATCHING_FLUIDS;
    }
}
