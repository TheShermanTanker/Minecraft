package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.levelgen.feature.WorldGenEnder;

public class WorldGenFeatureEndSpikeConfiguration implements WorldGenFeatureConfiguration {
    public static final Codec<WorldGenFeatureEndSpikeConfiguration> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Codec.BOOL.fieldOf("crystal_invulnerable").orElse(false).forGetter((spikeConfiguration) -> {
            return spikeConfiguration.crystalInvulnerable;
        }), WorldGenEnder.Spike.CODEC.listOf().fieldOf("spikes").forGetter((spikeConfiguration) -> {
            return spikeConfiguration.spikes;
        }), BlockPosition.CODEC.optionalFieldOf("crystal_beam_target").forGetter((spikeConfiguration) -> {
            return Optional.ofNullable(spikeConfiguration.crystalBeamTarget);
        })).apply(instance, WorldGenFeatureEndSpikeConfiguration::new);
    });
    private final boolean crystalInvulnerable;
    private final List<WorldGenEnder.Spike> spikes;
    @Nullable
    private final BlockPosition crystalBeamTarget;

    public WorldGenFeatureEndSpikeConfiguration(boolean crystalInvulnerable, List<WorldGenEnder.Spike> spikes, @Nullable BlockPosition crystalBeamTarget) {
        this(crystalInvulnerable, spikes, Optional.ofNullable(crystalBeamTarget));
    }

    private WorldGenFeatureEndSpikeConfiguration(boolean crystalInvulnerable, List<WorldGenEnder.Spike> spikes, Optional<BlockPosition> crystalBeamTarget) {
        this.crystalInvulnerable = crystalInvulnerable;
        this.spikes = spikes;
        this.crystalBeamTarget = crystalBeamTarget.orElse((BlockPosition)null);
    }

    public boolean isCrystalInvulnerable() {
        return this.crystalInvulnerable;
    }

    public List<WorldGenEnder.Spike> getSpikes() {
        return this.spikes;
    }

    @Nullable
    public BlockPosition getCrystalBeamTarget() {
        return this.crystalBeamTarget;
    }
}
