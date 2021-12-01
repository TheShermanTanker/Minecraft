package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Random;
import java.util.stream.Stream;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;

public class EnvironmentScanPlacement extends PlacementModifier {
    private final EnumDirection directionOfSearch;
    private final BlockPredicate targetCondition;
    private final BlockPredicate allowedSearchCondition;
    private final int maxSteps;
    public static final Codec<EnvironmentScanPlacement> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(EnumDirection.VERTICAL_CODEC.fieldOf("direction_of_search").forGetter((environmentScanPlacement) -> {
            return environmentScanPlacement.directionOfSearch;
        }), BlockPredicate.CODEC.fieldOf("target_condition").forGetter((environmentScanPlacement) -> {
            return environmentScanPlacement.targetCondition;
        }), BlockPredicate.CODEC.optionalFieldOf("allowed_search_condition", BlockPredicate.alwaysTrue()).forGetter((environmentScanPlacement) -> {
            return environmentScanPlacement.allowedSearchCondition;
        }), Codec.intRange(1, 32).fieldOf("max_steps").forGetter((environmentScanPlacement) -> {
            return environmentScanPlacement.maxSteps;
        })).apply(instance, EnvironmentScanPlacement::new);
    });

    private EnvironmentScanPlacement(EnumDirection direction, BlockPredicate targetPredicate, BlockPredicate allowedSearchPredicate, int maxSteps) {
        this.directionOfSearch = direction;
        this.targetCondition = targetPredicate;
        this.allowedSearchCondition = allowedSearchPredicate;
        this.maxSteps = maxSteps;
    }

    public static EnvironmentScanPlacement scanningFor(EnumDirection direction, BlockPredicate targetPredicate, BlockPredicate allowedSearchPredicate, int maxSteps) {
        return new EnvironmentScanPlacement(direction, targetPredicate, allowedSearchPredicate, maxSteps);
    }

    public static EnvironmentScanPlacement scanningFor(EnumDirection direction, BlockPredicate targetPredicate, int maxSteps) {
        return scanningFor(direction, targetPredicate, BlockPredicate.alwaysTrue(), maxSteps);
    }

    @Override
    public Stream<BlockPosition> getPositions(PlacementContext context, Random random, BlockPosition pos) {
        BlockPosition.MutableBlockPosition mutableBlockPos = pos.mutable();
        GeneratorAccessSeed worldGenLevel = context.getLevel();
        if (!this.allowedSearchCondition.test(worldGenLevel, mutableBlockPos)) {
            return Stream.of();
        } else {
            int i = 0;

            while(true) {
                if (i < this.maxSteps) {
                    if (this.targetCondition.test(worldGenLevel, mutableBlockPos)) {
                        return Stream.of(mutableBlockPos);
                    }

                    mutableBlockPos.move(this.directionOfSearch);
                    if (worldGenLevel.isOutsideBuildHeight(mutableBlockPos.getY())) {
                        return Stream.of();
                    }

                    if (this.allowedSearchCondition.test(worldGenLevel, mutableBlockPos)) {
                        ++i;
                        continue;
                    }
                }

                if (this.targetCondition.test(worldGenLevel, mutableBlockPos)) {
                    return Stream.of(mutableBlockPos);
                }

                return Stream.of();
            }
        }
    }

    @Override
    public PlacementModifierType<?> type() {
        return PlacementModifierType.ENVIRONMENT_SCAN;
    }
}
