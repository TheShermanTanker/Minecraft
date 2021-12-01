package net.minecraft.world.level.biome;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.SystemUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.core.QuartPos;
import net.minecraft.util.Graph;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.apache.commons.lang3.mutable.MutableInt;

public abstract class WorldChunkManager implements BiomeResolver {
    public static final Codec<WorldChunkManager> CODEC = IRegistry.BIOME_SOURCE.byNameCodec().dispatchStable(WorldChunkManager::codec, Function.identity());
    private final Set<BiomeBase> possibleBiomes;
    private final List<BiomeSource$StepFeatureData> featuresPerStep;

    protected WorldChunkManager(Stream<Supplier<BiomeBase>> stream) {
        this(stream.map(Supplier::get).distinct().collect(ImmutableList.toImmutableList()));
    }

    protected WorldChunkManager(List<BiomeBase> biomes) {
        this.possibleBiomes = new ObjectLinkedOpenHashSet<>(biomes);
        this.featuresPerStep = this.buildFeaturesPerStep(biomes, true);
    }

    private List<BiomeSource$StepFeatureData> buildFeaturesPerStep(List<BiomeBase> biomes, boolean bl) {
        Object2IntMap<PlacedFeature> object2IntMap = new Object2IntOpenHashMap<>();
        MutableInt mutableInt = new MutableInt(0);
        Comparator<FeatureData> comparator = Comparator.comparingInt(FeatureData::step).thenComparingInt(FeatureData::featureIndex);
        Map<FeatureData, Set<FeatureData>> map = new TreeMap<>(comparator);
        int i = 0;

        record FeatureData(int featureIndex, int step, PlacedFeature feature) {
            FeatureData(int i, int j, PlacedFeature placedFeature) {
                this.featureIndex = i;
                this.step = j;
                this.feature = placedFeature;
            }

            public int featureIndex() {
                return this.featureIndex;
            }

            public int step() {
                return this.step;
            }

            public PlacedFeature feature() {
                return this.feature;
            }
        }

        for(BiomeBase biome : biomes) {
            List<FeatureData> list = Lists.newArrayList();
            List<List<Supplier<PlacedFeature>>> list2 = biome.getGenerationSettings().features();
            i = Math.max(i, list2.size());

            for(int j = 0; j < list2.size(); ++j) {
                for(Supplier<PlacedFeature> supplier : list2.get(j)) {
                    PlacedFeature placedFeature = supplier.get();
                    list.add(new FeatureData(object2IntMap.computeIfAbsent(placedFeature, (object) -> {
                        return mutableInt.getAndIncrement();
                    }), j, placedFeature));
                }
            }

            for(int k = 0; k < list.size(); ++k) {
                Set<FeatureData> set = map.computeIfAbsent(list.get(k), (arg) -> {
                    return new TreeSet<>(comparator);
                });
                if (k < list.size() - 1) {
                    set.add(list.get(k + 1));
                }
            }
        }

        Set<FeatureData> set2 = new TreeSet<>(comparator);
        Set<FeatureData> set3 = new TreeSet<>(comparator);
        List<FeatureData> list3 = Lists.newArrayList();

        for(FeatureData lv : map.keySet()) {
            if (!set3.isEmpty()) {
                throw new IllegalStateException("You somehow broke the universe; DFS bork (iteration finished with non-empty in-progress vertex set");
            }

            if (!set2.contains(lv) && Graph.depthFirstSearch(map, set2, set3, list3::add, lv)) {
                if (!bl) {
                    throw new IllegalStateException("Feature order cycle found");
                }

                List<BiomeBase> list4 = new ArrayList<>(biomes);

                int l;
                do {
                    l = list4.size();
                    ListIterator<BiomeBase> listIterator = list4.listIterator();

                    while(listIterator.hasNext()) {
                        BiomeBase biome2 = listIterator.next();
                        listIterator.remove();

                        try {
                            this.buildFeaturesPerStep(list4, false);
                        } catch (IllegalStateException var18) {
                            continue;
                        }

                        listIterator.add(biome2);
                    }
                } while(l != list4.size());

                throw new IllegalStateException("Feature order cycle found, involved biomes: " + list4);
            }
        }

        Collections.reverse(list3);
        Builder<BiomeSource$StepFeatureData> builder = ImmutableList.builder();

        for(int m = 0; m < i; ++m) {
            int n = m;
            List<PlacedFeature> list5 = list3.stream().filter((arg) -> {
                return arg.step() == n;
            }).map(FeatureData::feature).collect(Collectors.toList());
            int o = list5.size();
            Object2IntMap<PlacedFeature> object2IntMap2 = new Object2IntOpenCustomHashMap<>(o, SystemUtils.identityStrategy());

            for(int p = 0; p < o; ++p) {
                object2IntMap2.put(list5.get(p), p);
            }

            builder.add(new BiomeSource$StepFeatureData(list5, object2IntMap2));
        }

        return builder.build();
    }

    protected abstract Codec<? extends WorldChunkManager> codec();

    public abstract WorldChunkManager withSeed(long seed);

    public Set<BiomeBase> possibleBiomes() {
        return this.possibleBiomes;
    }

    public Set<BiomeBase> getBiomesWithin(int x, int y, int z, int radius, Climate.Sampler sampler) {
        int i = QuartPos.fromBlock(x - radius);
        int j = QuartPos.fromBlock(y - radius);
        int k = QuartPos.fromBlock(z - radius);
        int l = QuartPos.fromBlock(x + radius);
        int m = QuartPos.fromBlock(y + radius);
        int n = QuartPos.fromBlock(z + radius);
        int o = l - i + 1;
        int p = m - j + 1;
        int q = n - k + 1;
        Set<BiomeBase> set = Sets.newHashSet();

        for(int r = 0; r < q; ++r) {
            for(int s = 0; s < o; ++s) {
                for(int t = 0; t < p; ++t) {
                    int u = i + s;
                    int v = j + t;
                    int w = k + r;
                    set.add(this.getNoiseBiome(u, v, w, sampler));
                }
            }
        }

        return set;
    }

    @Nullable
    public BlockPosition findBiomeHorizontal(int x, int y, int z, int radius, Predicate<BiomeBase> predicate, Random random, Climate.Sampler noiseSampler) {
        return this.findBiomeHorizontal(x, y, z, radius, 1, predicate, random, false, noiseSampler);
    }

    @Nullable
    public BlockPosition findBiomeHorizontal(int x, int y, int z, int radius, int i, Predicate<BiomeBase> predicate, Random random, boolean bl, Climate.Sampler noiseSampler) {
        int j = QuartPos.fromBlock(x);
        int k = QuartPos.fromBlock(z);
        int l = QuartPos.fromBlock(radius);
        int m = QuartPos.fromBlock(y);
        BlockPosition blockPos = null;
        int n = 0;
        int o = bl ? 0 : l;

        for(int p = o; p <= l; p += i) {
            for(int q = SharedConstants.debugGenerateSquareTerrainWithoutNoise ? 0 : -p; q <= p; q += i) {
                boolean bl2 = Math.abs(q) == p;

                for(int r = -p; r <= p; r += i) {
                    if (bl) {
                        boolean bl3 = Math.abs(r) == p;
                        if (!bl3 && !bl2) {
                            continue;
                        }
                    }

                    int s = j + r;
                    int t = k + q;
                    if (predicate.test(this.getNoiseBiome(s, m, t, noiseSampler))) {
                        if (blockPos == null || random.nextInt(n + 1) == 0) {
                            blockPos = new BlockPosition(QuartPos.toBlock(s), y, QuartPos.toBlock(t));
                            if (bl) {
                                return blockPos;
                            }
                        }

                        ++n;
                    }
                }
            }
        }

        return blockPos;
    }

    @Override
    public abstract BiomeBase getNoiseBiome(int x, int y, int z, Climate.Sampler noise);

    public void addMultinoiseDebugInfo(List<String> info, BlockPosition pos, Climate.Sampler noiseSampler) {
    }

    public List<BiomeSource$StepFeatureData> featuresPerStep() {
        return this.featuresPerStep;
    }

    static {
        IRegistry.register(IRegistry.BIOME_SOURCE, "fixed", WorldChunkManagerHell.CODEC);
        IRegistry.register(IRegistry.BIOME_SOURCE, "multi_noise", WorldChunkManagerMultiNoise.CODEC);
        IRegistry.register(IRegistry.BIOME_SOURCE, "checkerboard", WorldChunkManagerCheckerBoard.CODEC);
        IRegistry.register(IRegistry.BIOME_SOURCE, "the_end", WorldChunkManagerTheEnd.CODEC);
    }
}
