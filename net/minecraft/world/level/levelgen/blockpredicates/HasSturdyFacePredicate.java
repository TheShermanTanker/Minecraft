package net.minecraft.world.level.levelgen.blockpredicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BaseBlockPosition;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.level.GeneratorAccessSeed;

public class HasSturdyFacePredicate implements BlockPredicate {
    private final BaseBlockPosition offset;
    private final EnumDirection direction;
    public static final Codec<HasSturdyFacePredicate> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(BaseBlockPosition.offsetCodec(16).optionalFieldOf("offset", BaseBlockPosition.ZERO).forGetter((hasSturdyFacePredicate) -> {
            return hasSturdyFacePredicate.offset;
        }), EnumDirection.CODEC.fieldOf("direction").forGetter((hasSturdyFacePredicate) -> {
            return hasSturdyFacePredicate.direction;
        })).apply(instance, HasSturdyFacePredicate::new);
    });

    public HasSturdyFacePredicate(BaseBlockPosition offset, EnumDirection face) {
        this.offset = offset;
        this.direction = face;
    }

    @Override
    public boolean test(GeneratorAccessSeed worldGenLevel, BlockPosition blockPos) {
        BlockPosition blockPos2 = blockPos.offset(this.offset);
        return worldGenLevel.getType(blockPos2).isFaceSturdy(worldGenLevel, blockPos2, this.direction);
    }

    @Override
    public BlockPredicateType<?> type() {
        return BlockPredicateType.HAS_STURDY_FACE;
    }
}
