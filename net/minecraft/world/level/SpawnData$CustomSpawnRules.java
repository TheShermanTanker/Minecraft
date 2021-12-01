package net.minecraft.world.level;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.InclusiveRange;

public record SpawnData$CustomSpawnRules(InclusiveRange<Integer> blockLightLimit, InclusiveRange<Integer> skyLightLimit) {
    private static final InclusiveRange<Integer> LIGHT_RANGE = new InclusiveRange<>(0, 15);
    public static final Codec<SpawnData$CustomSpawnRules> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(InclusiveRange.INT.optionalFieldOf("block_light_limit", LIGHT_RANGE).flatXmap(SpawnData$CustomSpawnRules::checkLightBoundaries, SpawnData$CustomSpawnRules::checkLightBoundaries).forGetter((rules) -> {
            return rules.blockLightLimit;
        }), InclusiveRange.INT.optionalFieldOf("sky_light_limit", LIGHT_RANGE).flatXmap(SpawnData$CustomSpawnRules::checkLightBoundaries, SpawnData$CustomSpawnRules::checkLightBoundaries).forGetter((rules) -> {
            return rules.skyLightLimit;
        })).apply(instance, SpawnData$CustomSpawnRules::new);
    });

    public SpawnData$CustomSpawnRules(InclusiveRange<Integer> inclusiveRange, InclusiveRange<Integer> inclusiveRange2) {
        this.blockLightLimit = inclusiveRange;
        this.skyLightLimit = inclusiveRange2;
    }

    private static DataResult<InclusiveRange<Integer>> checkLightBoundaries(InclusiveRange<Integer> provider) {
        return !LIGHT_RANGE.contains(provider) ? DataResult.error("Light values must be withing range " + LIGHT_RANGE) : DataResult.success(provider);
    }

    public InclusiveRange<Integer> blockLightLimit() {
        return this.blockLightLimit;
    }

    public InclusiveRange<Integer> skyLightLimit() {
        return this.skyLightLimit;
    }
}
