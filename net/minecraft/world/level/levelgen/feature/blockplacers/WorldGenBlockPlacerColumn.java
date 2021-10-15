package net.minecraft.world.level.levelgen.feature.blockplacers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.block.state.IBlockData;

public class WorldGenBlockPlacerColumn extends WorldGenBlockPlacer {
    public static final Codec<WorldGenBlockPlacerColumn> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(IntProvider.NON_NEGATIVE_CODEC.fieldOf("size").forGetter((columnPlacer) -> {
            return columnPlacer.size;
        })).apply(instance, WorldGenBlockPlacerColumn::new);
    });
    private final IntProvider size;

    public WorldGenBlockPlacerColumn(IntProvider size) {
        this.size = size;
    }

    @Override
    protected WorldGenBlockPlacers<?> type() {
        return WorldGenBlockPlacers.COLUMN_PLACER;
    }

    @Override
    public void place(GeneratorAccess world, BlockPosition pos, IBlockData state, Random random) {
        BlockPosition.MutableBlockPosition mutableBlockPos = pos.mutable();
        int i = this.size.sample(random);

        for(int j = 0; j < i; ++j) {
            world.setTypeAndData(mutableBlockPos, state, 2);
            mutableBlockPos.move(EnumDirection.UP);
        }

    }
}
