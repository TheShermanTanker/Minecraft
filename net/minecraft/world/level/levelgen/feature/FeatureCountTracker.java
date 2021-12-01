package net.minecraft.world.level.levelgen.feature;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import net.minecraft.core.IRegistry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FeatureCountTracker {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final LoadingCache<WorldServer, FeatureCountTracker.LevelData> data = CacheBuilder.newBuilder().weakKeys().expireAfterAccess(5L, TimeUnit.MINUTES).build(new CacheLoader<WorldServer, FeatureCountTracker.LevelData>() {
        @Override
        public FeatureCountTracker.LevelData load(WorldServer serverLevel) {
            return new FeatureCountTracker.LevelData(Object2IntMaps.synchronize(new Object2IntOpenHashMap<>()), new MutableInt(0));
        }
    });

    public static void chunkDecorated(WorldServer world) {
        try {
            data.get(world).chunksWithFeatures().increment();
        } catch (Exception var2) {
            LOGGER.error(var2);
        }

    }

    public static void featurePlaced(WorldServer world, WorldGenFeatureConfigured<?, ?> configuredFeature, Optional<PlacedFeature> placedFeature) {
        try {
            data.get(world).featureData().computeInt(new FeatureCountTracker.FeatureData(configuredFeature, placedFeature), (featureData, count) -> {
                return count == null ? 1 : count + 1;
            });
        } catch (Exception var4) {
            LOGGER.error(var4);
        }

    }

    public static void clearCounts() {
        data.invalidateAll();
        LOGGER.debug("Cleared feature counts");
    }

    public static void logCounts() {
        LOGGER.debug("Logging feature counts:");
        data.asMap().forEach((world, features) -> {
            String string = world.getDimensionKey().location().toString();
            boolean bl = world.getMinecraftServer().isRunning();
            IRegistry<PlacedFeature> registry = world.registryAccess().registryOrThrow(IRegistry.PLACED_FEATURE_REGISTRY);
            String string2 = (bl ? "running" : "dead") + " " + string;
            Integer integer = features.chunksWithFeatures().getValue();
            LOGGER.debug(string2 + " total_chunks: " + integer);
            features.featureData().forEach((featureData, count) -> {
                LOGGER.debug(string2 + " " + String.format("%10d ", count) + String.format("%10f ", (double)count.intValue() / (double)integer.intValue()) + featureData.topFeature().flatMap(registry::getResourceKey).map(ResourceKey::location) + " " + featureData.feature().feature() + " " + featureData.feature());
            });
        });
    }

    static record FeatureData(WorldGenFeatureConfigured<?, ?> feature, Optional<PlacedFeature> topFeature) {
        FeatureData(WorldGenFeatureConfigured<?, ?> configuredFeature, Optional<PlacedFeature> optional) {
            this.feature = configuredFeature;
            this.topFeature = optional;
        }

        public WorldGenFeatureConfigured<?, ?> feature() {
            return this.feature;
        }

        public Optional<PlacedFeature> topFeature() {
            return this.topFeature;
        }
    }

    static record LevelData(Object2IntMap<FeatureCountTracker.FeatureData> featureData, MutableInt chunksWithFeatures) {
        LevelData(Object2IntMap<FeatureCountTracker.FeatureData> object2IntMap, MutableInt mutableInt) {
            this.featureData = object2IntMap;
            this.chunksWithFeatures = mutableInt;
        }

        public Object2IntMap<FeatureCountTracker.FeatureData> featureData() {
            return this.featureData;
        }

        public MutableInt chunksWithFeatures() {
            return this.chunksWithFeatures;
        }
    }
}
