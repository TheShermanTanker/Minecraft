package net.minecraft.world.level.levelgen.blockpredicates;

import com.mojang.datafixers.Products.P1;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import com.mojang.serialization.codecs.RecordCodecBuilder.Mu;
import net.minecraft.core.BaseBlockPosition;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.block.state.IBlockData;

public abstract class StateTestingPredicate implements BlockPredicate {
    protected final BaseBlockPosition offset;

    protected static <P extends StateTestingPredicate> P1<Mu<P>, BaseBlockPosition> stateTestingCodec(Instance<P> instance) {
        return instance.group(BaseBlockPosition.offsetCodec(16).optionalFieldOf("offset", BaseBlockPosition.ZERO).forGetter((predicate) -> {
            return predicate.offset;
        }));
    }

    protected StateTestingPredicate(BaseBlockPosition offset) {
        this.offset = offset;
    }

    @Override
    public final boolean test(GeneratorAccessSeed worldGenLevel, BlockPosition blockPos) {
        return this.test(worldGenLevel.getType(blockPos.offset(this.offset)));
    }

    protected abstract boolean test(IBlockData state);
}
