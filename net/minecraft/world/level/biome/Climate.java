package net.minecraft.world.level.biome;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.QuartPos;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.levelgen.NoiseSampler;

public class Climate {
    private static final boolean DEBUG_SLOW_BIOME_SEARCH = false;
    private static final float QUANTIZATION_FACTOR = 10000.0F;
    @VisibleForTesting
    protected static final int PARAMETER_COUNT = 7;

    public static Climate.TargetPoint target(float temperatureNoise, float humidityNoise, float continentalnessNoise, float erosionNoise, float depth, float weirdnessNoise) {
        return new Climate.TargetPoint(quantizeCoord(temperatureNoise), quantizeCoord(humidityNoise), quantizeCoord(continentalnessNoise), quantizeCoord(erosionNoise), quantizeCoord(depth), quantizeCoord(weirdnessNoise));
    }

    public static Climate.ParameterPoint parameters(float temperature, float humidity, float continentalness, float erosion, float depth, float weirdness, float offset) {
        return new Climate.ParameterPoint(Climate.Parameter.point(temperature), Climate.Parameter.point(humidity), Climate.Parameter.point(continentalness), Climate.Parameter.point(erosion), Climate.Parameter.point(depth), Climate.Parameter.point(weirdness), quantizeCoord(offset));
    }

    public static Climate.ParameterPoint parameters(Climate.Parameter temperature, Climate.Parameter humidity, Climate.Parameter continentalness, Climate.Parameter erosion, Climate.Parameter depth, Climate.Parameter weirdness, float offset) {
        return new Climate.ParameterPoint(temperature, humidity, continentalness, erosion, depth, weirdness, quantizeCoord(offset));
    }

    public static long quantizeCoord(float f) {
        return (long)(f * 10000.0F);
    }

    public static float unquantizeCoord(long l) {
        return (float)l / 10000.0F;
    }

    public static BlockPosition findSpawnPosition(List<Climate.ParameterPoint> noises, NoiseSampler sampler) {
        return (new Climate.SpawnFinder(noises, sampler)).result.location();
    }

    interface DistanceMetric<T> {
        long distance(Climate.RTree.Node<T> node, long[] ls);
    }

    public static record Parameter(long min, long max) {
        public static final Codec<Climate.Parameter> CODEC = ExtraCodecs.intervalCodec(Codec.floatRange(-2.0F, 2.0F), "min", "max", (min, max) -> {
            return min.compareTo(max) > 0 ? DataResult.error("Cannon construct interval, min > max (" + min + " > " + max + ")") : DataResult.success(new Climate.Parameter(Climate.quantizeCoord(min), Climate.quantizeCoord(max)));
        }, (parameter) -> {
            return Climate.unquantizeCoord(parameter.min());
        }, (parameter) -> {
            return Climate.unquantizeCoord(parameter.max());
        });

        public Parameter(long l, long m) {
            this.min = l;
            this.max = m;
        }

        public static Climate.Parameter point(float point) {
            return span(point, point);
        }

        public static Climate.Parameter span(float min, float max) {
            if (min > max) {
                throw new IllegalArgumentException("min > max: " + min + " " + max);
            } else {
                return new Climate.Parameter(Climate.quantizeCoord(min), Climate.quantizeCoord(max));
            }
        }

        public static Climate.Parameter span(Climate.Parameter min, Climate.Parameter max) {
            if (min.min() > max.max()) {
                throw new IllegalArgumentException("min > max: " + min + " " + max);
            } else {
                return new Climate.Parameter(min.min(), max.max());
            }
        }

        @Override
        public String toString() {
            return this.min == this.max ? String.format("%d", this.min) : String.format("[%d-%d]", this.min, this.max);
        }

        public long distance(long noise) {
            long l = noise - this.max;
            long m = this.min - noise;
            return l > 0L ? l : Math.max(m, 0L);
        }

        public long distance(Climate.Parameter other) {
            long l = other.min() - this.max;
            long m = this.min - other.max();
            return l > 0L ? l : Math.max(m, 0L);
        }

        public Climate.Parameter span(@Nullable Climate.Parameter other) {
            return other == null ? this : new Climate.Parameter(Math.min(this.min, other.min()), Math.max(this.max, other.max()));
        }

        public long min() {
            return this.min;
        }

        public long max() {
            return this.max;
        }
    }

    public static class ParameterList<T> {
        private final List<Pair<Climate.ParameterPoint, T>> values;
        private final Climate.RTree<T> index;

        public ParameterList(List<Pair<Climate.ParameterPoint, T>> entries) {
            this.values = entries;
            this.index = Climate.RTree.create(entries);
        }

        public List<Pair<Climate.ParameterPoint, T>> values() {
            return this.values;
        }

        public T findValue(Climate.TargetPoint targetPoint, T object) {
            return this.findValueIndex(targetPoint);
        }

        @VisibleForTesting
        public T findValueBruteForce(Climate.TargetPoint targetPoint, T object) {
            long l = Long.MAX_VALUE;
            T object2 = object;

            for(Pair<Climate.ParameterPoint, T> pair : this.values()) {
                long m = pair.getFirst().fitness(targetPoint);
                if (m < l) {
                    l = m;
                    object2 = pair.getSecond();
                }
            }

            return object2;
        }

        public T findValueIndex(Climate.TargetPoint targetPoint) {
            return this.findValueIndex(targetPoint, Climate.RTree.Node::distance);
        }

        protected T findValueIndex(Climate.TargetPoint targetPoint, Climate.DistanceMetric<T> distanceMetric) {
            return this.index.search(targetPoint, distanceMetric);
        }
    }

    public static record ParameterPoint(Climate.Parameter temperature, Climate.Parameter humidity, Climate.Parameter continentalness, Climate.Parameter erosion, Climate.Parameter depth, Climate.Parameter weirdness, long offset) {
        public static final Codec<Climate.ParameterPoint> CODEC = RecordCodecBuilder.create((instance) -> {
            return instance.group(Climate.Parameter.CODEC.fieldOf("temperature").forGetter((parameterPoint) -> {
                return parameterPoint.temperature;
            }), Climate.Parameter.CODEC.fieldOf("humidity").forGetter((parameterPoint) -> {
                return parameterPoint.humidity;
            }), Climate.Parameter.CODEC.fieldOf("continentalness").forGetter((parameterPoint) -> {
                return parameterPoint.continentalness;
            }), Climate.Parameter.CODEC.fieldOf("erosion").forGetter((parameterPoint) -> {
                return parameterPoint.erosion;
            }), Climate.Parameter.CODEC.fieldOf("depth").forGetter((parameterPoint) -> {
                return parameterPoint.depth;
            }), Climate.Parameter.CODEC.fieldOf("weirdness").forGetter((parameterPoint) -> {
                return parameterPoint.weirdness;
            }), Codec.floatRange(0.0F, 1.0F).fieldOf("offset").xmap(Climate::quantizeCoord, Climate::unquantizeCoord).forGetter((parameterPoint) -> {
                return parameterPoint.offset;
            })).apply(instance, Climate.ParameterPoint::new);
        });

        public ParameterPoint(Climate.Parameter temperature, Climate.Parameter humidity, Climate.Parameter continentalness, Climate.Parameter erosion, Climate.Parameter depth, Climate.Parameter weirdness, long l) {
            this.temperature = temperature;
            this.humidity = humidity;
            this.continentalness = continentalness;
            this.erosion = erosion;
            this.depth = depth;
            this.weirdness = weirdness;
            this.offset = l;
        }

        long fitness(Climate.TargetPoint point) {
            return MathHelper.square(this.temperature.distance(point.temperature)) + MathHelper.square(this.humidity.distance(point.humidity)) + MathHelper.square(this.continentalness.distance(point.continentalness)) + MathHelper.square(this.erosion.distance(point.erosion)) + MathHelper.square(this.depth.distance(point.depth)) + MathHelper.square(this.weirdness.distance(point.weirdness)) + MathHelper.square(this.offset);
        }

        protected List<Climate.Parameter> parameterSpace() {
            return ImmutableList.of(this.temperature, this.humidity, this.continentalness, this.erosion, this.depth, this.weirdness, new Climate.Parameter(this.offset, this.offset));
        }

        public Climate.Parameter temperature() {
            return this.temperature;
        }

        public Climate.Parameter humidity() {
            return this.humidity;
        }

        public Climate.Parameter continentalness() {
            return this.continentalness;
        }

        public Climate.Parameter erosion() {
            return this.erosion;
        }

        public Climate.Parameter depth() {
            return this.depth;
        }

        public Climate.Parameter weirdness() {
            return this.weirdness;
        }

        public long offset() {
            return this.offset;
        }
    }

    protected static final class RTree<T> {
        private static final int CHILDREN_PER_NODE = 10;
        private final Climate.RTree.Node<T> root;
        private final ThreadLocal<Climate.RTree.Leaf<T>> lastResult = new ThreadLocal<>();

        private RTree(Climate.RTree.Node<T> firstNode) {
            this.root = firstNode;
        }

        public static <T> Climate.RTree<T> create(List<Pair<Climate.ParameterPoint, T>> entries) {
            if (entries.isEmpty()) {
                throw new IllegalArgumentException("Need at least one value to build the search tree.");
            } else {
                int i = entries.get(0).getFirst().parameterSpace().size();
                if (i != 7) {
                    throw new IllegalStateException("Expecting parameter space to be 7, got " + i);
                } else {
                    List<Climate.RTree.Leaf<T>> list = entries.stream().map((entry) -> {
                        return new Climate.RTree.Leaf(entry.getFirst(), entry.getSecond());
                    }).collect(Collectors.toCollection(ArrayList::new));
                    return new Climate.RTree<>(build(i, list));
                }
            }
        }

        private static <T> Climate.RTree.Node<T> build(int parameterNumber, List<? extends Climate.RTree.Node<T>> subTree) {
            if (subTree.isEmpty()) {
                throw new IllegalStateException("Need at least one child to build a node");
            } else if (subTree.size() == 1) {
                return subTree.get(0);
            } else if (subTree.size() <= 10) {
                subTree.sort(Comparator.comparingLong((node) -> {
                    long l = 0L;

                    for(int j = 0; j < parameterNumber; ++j) {
                        Climate.Parameter parameter = node.parameterSpace[j];
                        l += Math.abs((parameter.min() + parameter.max()) / 2L);
                    }

                    return l;
                }));
                return new Climate.RTree.SubTree<>(subTree);
            } else {
                long l = Long.MAX_VALUE;
                int i = -1;
                List<Climate.RTree.SubTree<T>> list = null;

                for(int j = 0; j < parameterNumber; ++j) {
                    sort(subTree, parameterNumber, j, false);
                    List<Climate.RTree.SubTree<T>> list2 = bucketize(subTree);
                    long m = 0L;

                    for(Climate.RTree.SubTree<T> subTree2 : list2) {
                        m += cost(subTree2.parameterSpace);
                    }

                    if (l > m) {
                        l = m;
                        i = j;
                        list = list2;
                    }
                }

                sort(list, parameterNumber, i, true);
                return new Climate.RTree.SubTree<>(list.stream().map((node) -> {
                    return build(parameterNumber, Arrays.asList(node.children));
                }).collect(Collectors.toList()));
            }
        }

        private static <T> void sort(List<? extends Climate.RTree.Node<T>> subTree, int parameterNumber, int currentParameter, boolean abs) {
            Comparator<Climate.RTree.Node<T>> comparator = comparator(currentParameter, abs);

            for(int i = 1; i < parameterNumber; ++i) {
                comparator = comparator.thenComparing(comparator((currentParameter + i) % parameterNumber, abs));
            }

            subTree.sort(comparator);
        }

        private static <T> Comparator<Climate.RTree.Node<T>> comparator(int currentParameter, boolean abs) {
            return Comparator.comparingLong((node) -> {
                Climate.Parameter parameter = node.parameterSpace[currentParameter];
                long l = (parameter.min() + parameter.max()) / 2L;
                return abs ? Math.abs(l) : l;
            });
        }

        private static <T> List<Climate.RTree.SubTree<T>> bucketize(List<? extends Climate.RTree.Node<T>> nodes) {
            List<Climate.RTree.SubTree<T>> list = Lists.newArrayList();
            List<Climate.RTree.Node<T>> list2 = Lists.newArrayList();
            int i = (int)Math.pow(10.0D, Math.floor(Math.log((double)nodes.size() - 0.01D) / Math.log(10.0D)));

            for(Climate.RTree.Node<T> node : nodes) {
                list2.add(node);
                if (list2.size() >= i) {
                    list.add(new Climate.RTree.SubTree<>(list2));
                    list2 = Lists.newArrayList();
                }
            }

            if (!list2.isEmpty()) {
                list.add(new Climate.RTree.SubTree<>(list2));
            }

            return list;
        }

        private static long cost(Climate.Parameter[] parameters) {
            long l = 0L;

            for(Climate.Parameter parameter : parameters) {
                l += Math.abs(parameter.max() - parameter.min());
            }

            return l;
        }

        static <T> List<Climate.Parameter> buildParameterSpace(List<? extends Climate.RTree.Node<T>> subTree) {
            if (subTree.isEmpty()) {
                throw new IllegalArgumentException("SubTree needs at least one child");
            } else {
                int i = 7;
                List<Climate.Parameter> list = Lists.newArrayList();

                for(int j = 0; j < 7; ++j) {
                    list.add((Climate.Parameter)null);
                }

                for(Climate.RTree.Node<T> node : subTree) {
                    for(int k = 0; k < 7; ++k) {
                        list.set(k, node.parameterSpace[k].span(list.get(k)));
                    }
                }

                return list;
            }
        }

        public T search(Climate.TargetPoint point, Climate.DistanceMetric<T> distanceFunction) {
            long[] ls = point.toParameterArray();
            Climate.RTree.Leaf<T> leaf = this.root.search(ls, this.lastResult.get(), distanceFunction);
            this.lastResult.set(leaf);
            return leaf.value;
        }

        static final class Leaf<T> extends Climate.RTree.Node<T> {
            final T value;

            Leaf(Climate.ParameterPoint parameters, T value) {
                super(parameters.parameterSpace());
                this.value = value;
            }

            @Override
            protected Climate.RTree.Leaf<T> search(long[] otherParameters, @Nullable Climate.RTree.Leaf<T> alternative, Climate.DistanceMetric<T> distanceFunction) {
                return this;
            }
        }

        abstract static class Node<T> {
            protected final Climate.Parameter[] parameterSpace;

            protected Node(List<Climate.Parameter> subTree) {
                this.parameterSpace = subTree.toArray(new Climate.Parameter[0]);
            }

            protected abstract Climate.RTree.Leaf<T> search(long[] otherParameters, @Nullable Climate.RTree.Leaf<T> alternative, Climate.DistanceMetric<T> distanceFunction);

            protected long distance(long[] otherParameters) {
                long l = 0L;

                for(int i = 0; i < 7; ++i) {
                    l += MathHelper.square(this.parameterSpace[i].distance(otherParameters[i]));
                }

                return l;
            }

            @Override
            public String toString() {
                return Arrays.toString((Object[])this.parameterSpace);
            }
        }

        static final class SubTree<T> extends Climate.RTree.Node<T> {
            final Climate.RTree.Node<T>[] children;

            protected SubTree(List<? extends Climate.RTree.Node<T>> subTree) {
                this(Climate.RTree.buildParameterSpace(subTree), subTree);
            }

            protected SubTree(List<Climate.Parameter> parameters, List<? extends Climate.RTree.Node<T>> subTree) {
                super(parameters);
                this.children = subTree.toArray(new Climate.RTree.Node[0]);
            }

            @Override
            protected Climate.RTree.Leaf<T> search(long[] otherParameters, @Nullable Climate.RTree.Leaf<T> alternative, Climate.DistanceMetric<T> distanceFunction) {
                long l = alternative == null ? Long.MAX_VALUE : distanceFunction.distance(alternative, otherParameters);
                Climate.RTree.Leaf<T> leaf = alternative;

                for(Climate.RTree.Node<T> node : this.children) {
                    long m = distanceFunction.distance(node, otherParameters);
                    if (l > m) {
                        Climate.RTree.Leaf<T> leaf2 = node.search(otherParameters, leaf, distanceFunction);
                        long n = node == leaf2 ? m : distanceFunction.distance(leaf2, otherParameters);
                        if (l > n) {
                            l = n;
                            leaf = leaf2;
                        }
                    }
                }

                return leaf;
            }
        }
    }

    public interface Sampler {
        Climate.TargetPoint sample(int x, int y, int z);

        default BlockPosition findSpawnPosition() {
            return BlockPosition.ZERO;
        }
    }

    static class SpawnFinder {
        Climate.SpawnFinder.Result result;

        SpawnFinder(List<Climate.ParameterPoint> noises, NoiseSampler sampler) {
            this.result = getSpawnPositionAndFitness(noises, sampler, 0, 0);
            this.radialSearch(noises, sampler, 2048.0F, 512.0F);
            this.radialSearch(noises, sampler, 512.0F, 32.0F);
        }

        private void radialSearch(List<Climate.ParameterPoint> noises, NoiseSampler sampler, float maxDistance, float step) {
            float f = 0.0F;
            float g = step;
            BlockPosition blockPos = this.result.location();

            while(g <= maxDistance) {
                int i = blockPos.getX() + (int)(Math.sin((double)f) * (double)g);
                int j = blockPos.getZ() + (int)(Math.cos((double)f) * (double)g);
                Climate.SpawnFinder.Result result = getSpawnPositionAndFitness(noises, sampler, i, j);
                if (result.fitness() < this.result.fitness()) {
                    this.result = result;
                }

                f += step / g;
                if ((double)f > (Math.PI * 2D)) {
                    f = 0.0F;
                    g += step;
                }
            }

        }

        private static Climate.SpawnFinder.Result getSpawnPositionAndFitness(List<Climate.ParameterPoint> noises, NoiseSampler sampler, int x, int z) {
            double d = MathHelper.square(2500.0D);
            int i = 2;
            long l = (long)((double)MathHelper.square(10000.0F) * Math.pow((double)(MathHelper.square((long)x) + MathHelper.square((long)z)) / d, 2.0D));
            Climate.TargetPoint targetPoint = sampler.sample(QuartPos.fromBlock(x), 0, QuartPos.fromBlock(z));
            Climate.TargetPoint targetPoint2 = new Climate.TargetPoint(targetPoint.temperature(), targetPoint.humidity(), targetPoint.continentalness(), targetPoint.erosion(), 0L, targetPoint.weirdness());
            long m = Long.MAX_VALUE;

            for(Climate.ParameterPoint parameterPoint : noises) {
                m = Math.min(m, parameterPoint.fitness(targetPoint2));
            }

            return new Climate.SpawnFinder.Result(new BlockPosition(x, 0, z), l + m);
        }

        static record Result(BlockPosition location, long fitness) {
            Result(BlockPosition blockPos, long l) {
                this.location = blockPos;
                this.fitness = l;
            }

            public BlockPosition location() {
                return this.location;
            }

            public long fitness() {
                return this.fitness;
            }
        }
    }

    public static record TargetPoint(long temperature, long humidity, long continentalness, long erosion, long depth, long weirdness) {
        public TargetPoint(long l, long m, long n, long o, long p, long q) {
            this.temperature = l;
            this.humidity = m;
            this.continentalness = n;
            this.erosion = o;
            this.depth = p;
            this.weirdness = q;
        }

        @VisibleForTesting
        protected long[] toParameterArray() {
            return new long[]{this.temperature, this.humidity, this.continentalness, this.erosion, this.depth, this.weirdness, 0L};
        }

        public long temperature() {
            return this.temperature;
        }

        public long humidity() {
            return this.humidity;
        }

        public long continentalness() {
            return this.continentalness;
        }

        public long erosion() {
            return this.erosion;
        }

        public long depth() {
            return this.depth;
        }

        public long weirdness() {
            return this.weirdness;
        }
    }
}
