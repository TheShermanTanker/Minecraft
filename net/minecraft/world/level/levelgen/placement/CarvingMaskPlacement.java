package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import java.util.Random;
import java.util.stream.Stream;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.levelgen.WorldGenStage;

public class CarvingMaskPlacement extends PlacementModifier {
    public static final Codec<CarvingMaskPlacement> CODEC = WorldGenStage.Features.CODEC.fieldOf("step").xmap(CarvingMaskPlacement::new, (config) -> {
        return config.step;
    }).codec();
    private final WorldGenStage.Features step;

    private CarvingMaskPlacement(WorldGenStage.Features step) {
        this.step = step;
    }

    public static CarvingMaskPlacement forStep(WorldGenStage.Features step) {
        return new CarvingMaskPlacement(step);
    }

    @Override
    public Stream<BlockPosition> getPositions(PlacementContext context, Random random, BlockPosition pos) {
        ChunkCoordIntPair chunkPos = new ChunkCoordIntPair(pos);
        return context.getCarvingMask(chunkPos, this.step).stream(chunkPos);
    }

    @Override
    public PlacementModifierType<?> type() {
        return PlacementModifierType.CARVING_MASK_PLACEMENT;
    }
}
