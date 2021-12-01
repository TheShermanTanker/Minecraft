package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureConfigured;
import org.apache.commons.lang3.mutable.MutableBoolean;

public class PlacedFeature {
    public static final Codec<PlacedFeature> DIRECT_CODEC;
    public static final Codec<Supplier<PlacedFeature>> CODEC = RegistryFileCodec.create(IRegistry.PLACED_FEATURE_REGISTRY, DIRECT_CODEC);
    public static final Codec<List<Supplier<PlacedFeature>>> LIST_CODEC = RegistryFileCodec.homogeneousList(IRegistry.PLACED_FEATURE_REGISTRY, DIRECT_CODEC);
    private final Supplier<WorldGenFeatureConfigured<?, ?>> feature;
    private final List<PlacementModifier> placement;

    public PlacedFeature(Supplier<WorldGenFeatureConfigured<?, ?>> feature, List<PlacementModifier> placementModifiers) {
        this.feature = feature;
        this.placement = placementModifiers;
    }

    public boolean place(GeneratorAccessSeed world, ChunkGenerator generator, Random random, BlockPosition pos) {
        return this.placeWithContext(new PlacementContext(world, generator, Optional.empty()), random, pos);
    }

    public boolean placeWithBiomeCheck(GeneratorAccessSeed world, ChunkGenerator generator, Random random, BlockPosition pos) {
        return this.placeWithContext(new PlacementContext(world, generator, Optional.of(this)), random, pos);
    }

    private boolean placeWithContext(PlacementContext context, Random random, BlockPosition pos) {
        Stream<BlockPosition> stream = Stream.of(pos);

        for(PlacementModifier placementModifier : this.placement) {
            stream = stream.flatMap((posx) -> {
                return placementModifier.getPositions(context, random, posx);
            });
        }

        WorldGenFeatureConfigured<?, ?> configuredFeature = this.feature.get();
        MutableBoolean mutableBoolean = new MutableBoolean();
        stream.forEach((blockPos) -> {
            if (configuredFeature.place(context.getLevel(), context.generator(), random, blockPos)) {
                mutableBoolean.setTrue();
            }

        });
        return mutableBoolean.isTrue();
    }

    public Stream<WorldGenFeatureConfigured<?, ?>> getFeatures() {
        return this.feature.get().getFeatures();
    }

    @VisibleForDebug
    public List<PlacementModifier> getPlacement() {
        return this.placement;
    }

    @Override
    public String toString() {
        return "Placed " + IRegistry.FEATURE.getKey(this.feature.get().feature());
    }

    static {
        DIRECT_CODEC = RecordCodecBuilder.create((instance) -> {
            return instance.group(WorldGenFeatureConfigured.CODEC.fieldOf("feature").forGetter((placedFeature) -> {
                return placedFeature.feature;
            }), PlacementModifier.CODEC.listOf().fieldOf("placement").forGetter((placedFeature) -> {
                return placedFeature.placement;
            })).apply(instance, PlacedFeature::new);
        });
    }
}
