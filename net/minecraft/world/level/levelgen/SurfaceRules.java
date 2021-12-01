package net.minecraft.world.level.levelgen;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.IChunkAccess;
import net.minecraft.world.level.levelgen.placement.CaveSurface;
import net.minecraft.world.level.levelgen.synth.NoiseGeneratorNormal;
import net.minecraft.world.level.levelgen.synth.NormalNoise$NoiseParameters;

public class SurfaceRules {
    public static final SurfaceRules.ConditionSource ON_FLOOR = stoneDepthCheck(0, false, false, CaveSurface.FLOOR);
    public static final SurfaceRules.ConditionSource UNDER_FLOOR = stoneDepthCheck(0, true, false, CaveSurface.FLOOR);
    public static final SurfaceRules.ConditionSource ON_CEILING = stoneDepthCheck(0, false, false, CaveSurface.CEILING);
    public static final SurfaceRules.ConditionSource UNDER_CEILING = stoneDepthCheck(0, true, false, CaveSurface.CEILING);

    public static SurfaceRules.ConditionSource stoneDepthCheck(int offset, boolean addSurfaceDepth, boolean addSecondarySurfaceDepth, CaveSurface surfaceType) {
        return new SurfaceRules.StoneDepthCheck(offset, addSurfaceDepth, addSecondarySurfaceDepth, surfaceType);
    }

    public static SurfaceRules.ConditionSource not(SurfaceRules.ConditionSource target) {
        return new SurfaceRules.NotConditionSource(target);
    }

    public static SurfaceRules.ConditionSource yBlockCheck(VerticalAnchor anchor, int runDepthMultiplier) {
        return new SurfaceRules.YConditionSource(anchor, runDepthMultiplier, false);
    }

    public static SurfaceRules.ConditionSource yStartCheck(VerticalAnchor anchor, int runDepthMultiplier) {
        return new SurfaceRules.YConditionSource(anchor, runDepthMultiplier, true);
    }

    public static SurfaceRules.ConditionSource waterBlockCheck(int offset, int runDepthMultiplier) {
        return new SurfaceRules.WaterConditionSource(offset, runDepthMultiplier, false);
    }

    public static SurfaceRules.ConditionSource waterStartCheck(int offset, int runDepthMultiplier) {
        return new SurfaceRules.WaterConditionSource(offset, runDepthMultiplier, true);
    }

    @SafeVarargs
    public static SurfaceRules.ConditionSource isBiome(ResourceKey<BiomeBase>... biomes) {
        return isBiome(List.of(biomes));
    }

    private static SurfaceRules.BiomeConditionSource isBiome(List<ResourceKey<BiomeBase>> biomes) {
        return new SurfaceRules.BiomeConditionSource(biomes);
    }

    public static SurfaceRules.ConditionSource noiseCondition(ResourceKey<NormalNoise$NoiseParameters> noise, double min) {
        return noiseCondition(noise, min, Double.MAX_VALUE);
    }

    public static SurfaceRules.ConditionSource noiseCondition(ResourceKey<NormalNoise$NoiseParameters> noise, double min, double max) {
        return new SurfaceRules.NoiseThresholdConditionSource(noise, min, max);
    }

    public static SurfaceRules.ConditionSource verticalGradient(String id, VerticalAnchor trueAtAndBelow, VerticalAnchor falseAtAndAbove) {
        return new SurfaceRules.VerticalGradientConditionSource(new MinecraftKey(id), trueAtAndBelow, falseAtAndAbove);
    }

    public static SurfaceRules.ConditionSource steep() {
        return SurfaceRules.Steep.INSTANCE;
    }

    public static SurfaceRules.ConditionSource hole() {
        return SurfaceRules.Hole.INSTANCE;
    }

    public static SurfaceRules.ConditionSource abovePreliminarySurface() {
        return SurfaceRules.AbovePreliminarySurface.INSTANCE;
    }

    public static SurfaceRules.ConditionSource temperature() {
        return SurfaceRules.Temperature.INSTANCE;
    }

    public static SurfaceRules.RuleSource ifTrue(SurfaceRules.ConditionSource condition, SurfaceRules.RuleSource rule) {
        return new SurfaceRules.TestRuleSource(condition, rule);
    }

    public static SurfaceRules.RuleSource sequence(SurfaceRules.RuleSource... rules) {
        if (rules.length == 0) {
            throw new IllegalArgumentException("Need at least 1 rule for a sequence");
        } else {
            return new SurfaceRules.SequenceRuleSource(Arrays.asList(rules));
        }
    }

    public static SurfaceRules.RuleSource state(IBlockData state) {
        return new SurfaceRules.BlockRuleSource(state);
    }

    public static SurfaceRules.RuleSource bandlands() {
        return SurfaceRules.Bandlands.INSTANCE;
    }

    static enum AbovePreliminarySurface implements SurfaceRules.ConditionSource {
        INSTANCE;

        static final Codec<SurfaceRules.AbovePreliminarySurface> CODEC = Codec.unit(INSTANCE);

        @Override
        public Codec<? extends SurfaceRules.ConditionSource> codec() {
            return CODEC;
        }

        @Override
        public SurfaceRules.Condition apply(SurfaceRules.Context context) {
            return context.abovePreliminarySurface;
        }
    }

    static enum Bandlands implements SurfaceRules.RuleSource {
        INSTANCE;

        static final Codec<SurfaceRules.Bandlands> CODEC = Codec.unit(INSTANCE);

        @Override
        public Codec<? extends SurfaceRules.RuleSource> codec() {
            return CODEC;
        }

        @Override
        public SurfaceRules.SurfaceRule apply(SurfaceRules.Context context) {
            return context.system::getBand;
        }
    }

    static record BiomeConditionSource(List<ResourceKey<BiomeBase>> biomes) implements SurfaceRules.ConditionSource {
        static final Codec<SurfaceRules.BiomeConditionSource> CODEC = ResourceKey.codec(IRegistry.BIOME_REGISTRY).listOf().fieldOf("biome_is").xmap(SurfaceRules::isBiome, SurfaceRules.BiomeConditionSource::biomes).codec();

        BiomeConditionSource(List<ResourceKey<BiomeBase>> list) {
            this.biomes = list;
        }

        @Override
        public Codec<? extends SurfaceRules.ConditionSource> codec() {
            return CODEC;
        }

        @Override
        public SurfaceRules.Condition apply(SurfaceRules.Context context) {
            // $FF: Couldn't be decompiled
        }

        public List<ResourceKey<BiomeBase>> biomes() {
            return this.biomes;
        }
    }

    static record BlockRuleSource(IBlockData resultState, SurfaceRules.StateRule rule) implements SurfaceRules.RuleSource {
        static final Codec<SurfaceRules.BlockRuleSource> CODEC = IBlockData.CODEC.xmap(SurfaceRules.BlockRuleSource::new, SurfaceRules.BlockRuleSource::resultState).fieldOf("result_state").codec();

        BlockRuleSource(IBlockData resultState) {
            this(resultState, new SurfaceRules.StateRule(resultState));
        }

        private BlockRuleSource(IBlockData blockState, SurfaceRules.StateRule stateRule) {
            this.resultState = blockState;
            this.rule = stateRule;
        }

        @Override
        public Codec<? extends SurfaceRules.RuleSource> codec() {
            return CODEC;
        }

        @Override
        public SurfaceRules.SurfaceRule apply(SurfaceRules.Context context) {
            return this.rule;
        }

        public IBlockData resultState() {
            return this.resultState;
        }

        public SurfaceRules.StateRule rule() {
            return this.rule;
        }
    }

    public interface Condition {
        boolean test();
    }

    public interface ConditionSource extends Function<SurfaceRules.Context, SurfaceRules.Condition> {
        Codec<SurfaceRules.ConditionSource> CODEC = IRegistry.CONDITION.byNameCodec().dispatch(SurfaceRules.ConditionSource::codec, Function.identity());

        static Codec<? extends SurfaceRules.ConditionSource> bootstrap() {
            IRegistry.register(IRegistry.CONDITION, "biome", SurfaceRules.BiomeConditionSource.CODEC);
            IRegistry.register(IRegistry.CONDITION, "noise_threshold", SurfaceRules.NoiseThresholdConditionSource.CODEC);
            IRegistry.register(IRegistry.CONDITION, "vertical_gradient", SurfaceRules.VerticalGradientConditionSource.CODEC);
            IRegistry.register(IRegistry.CONDITION, "y_above", SurfaceRules.YConditionSource.CODEC);
            IRegistry.register(IRegistry.CONDITION, "water", SurfaceRules.WaterConditionSource.CODEC);
            IRegistry.register(IRegistry.CONDITION, "temperature", SurfaceRules.Temperature.CODEC);
            IRegistry.register(IRegistry.CONDITION, "steep", SurfaceRules.Steep.CODEC);
            IRegistry.register(IRegistry.CONDITION, "not", SurfaceRules.NotConditionSource.CODEC);
            IRegistry.register(IRegistry.CONDITION, "hole", SurfaceRules.Hole.CODEC);
            IRegistry.register(IRegistry.CONDITION, "above_preliminary_surface", SurfaceRules.AbovePreliminarySurface.CODEC);
            IRegistry.register(IRegistry.CONDITION, "stone_depth", SurfaceRules.StoneDepthCheck.CODEC);
            return IRegistry.CONDITION.iterator().next();
        }

        Codec<? extends SurfaceRules.ConditionSource> codec();
    }

    public static final class Context {
        private static final int HOW_FAR_BELOW_PRELIMINARY_SURFACE_LEVEL_TO_BUILD_SURFACE = 8;
        private static final int SURFACE_CELL_BITS = 4;
        private static final int SURFACE_CELL_SIZE = 16;
        private static final int SURFACE_CELL_MASK = 15;
        public final SurfaceSystem system;
        final SurfaceRules.Condition temperature = new SurfaceRules.Context.TemperatureHelperCondition(this);
        final SurfaceRules.Condition steep = new SurfaceRules.Context.SteepMaterialCondition(this);
        final SurfaceRules.Condition hole = new SurfaceRules.Context.HoleCondition(this);
        final SurfaceRules.Condition abovePreliminarySurface = new SurfaceRules.Context.AbovePreliminarySurfaceCondition();
        final IChunkAccess chunk;
        private final NoiseChunk noiseChunk;
        private final Function<BlockPosition, BiomeBase> biomeGetter;
        private final IRegistry<BiomeBase> biomes;
        public final WorldGenerationContext context;
        private long lastPreliminarySurfaceCellOrigin = Long.MAX_VALUE;
        private final int[] preliminarySurfaceCache = new int[4];
        long lastUpdateXZ = -9223372036854775807L;
        public int blockX;
        public int blockZ;
        int surfaceDepth;
        private long lastSurfaceDepth2Update = this.lastUpdateXZ - 1L;
        private int surfaceSecondaryDepth;
        private long lastMinSurfaceLevelUpdate = this.lastUpdateXZ - 1L;
        private int minSurfaceLevel;
        long lastUpdateY = -9223372036854775807L;
        final BlockPosition.MutableBlockPosition pos = new BlockPosition.MutableBlockPosition();
        Supplier<BiomeBase> biome;
        Supplier<ResourceKey<BiomeBase>> biomeKey;
        public int blockY;
        int waterHeight;
        int stoneDepthBelow;
        int stoneDepthAbove;

        protected Context(SurfaceSystem surfaceBuilder, IChunkAccess chunk, NoiseChunk chunkNoiseSampler, Function<BlockPosition, BiomeBase> posToBiome, IRegistry<BiomeBase> biomeRegistry, WorldGenerationContext heightContext) {
            this.system = surfaceBuilder;
            this.chunk = chunk;
            this.noiseChunk = chunkNoiseSampler;
            this.biomeGetter = posToBiome;
            this.biomes = biomeRegistry;
            this.context = heightContext;
        }

        protected void updateXZ(int x, int z) {
            ++this.lastUpdateXZ;
            ++this.lastUpdateY;
            this.blockX = x;
            this.blockZ = z;
            this.surfaceDepth = this.system.getSurfaceDepth(x, z);
        }

        protected void updateY(int stoneDepthAbove, int stoneDepthBelow, int fluidHeight, int x, int y, int z) {
            ++this.lastUpdateY;
            this.biome = Suppliers.memoize(() -> {
                return this.biomeGetter.apply(this.pos.set(x, y, z));
            });
            this.biomeKey = Suppliers.memoize(() -> {
                return this.biomes.getResourceKey(this.biome.get()).orElseThrow(() -> {
                    return new IllegalStateException("Unregistered biome: " + this.biome);
                });
            });
            this.blockY = y;
            this.waterHeight = fluidHeight;
            this.stoneDepthBelow = stoneDepthBelow;
            this.stoneDepthAbove = stoneDepthAbove;
        }

        protected int getSurfaceSecondaryDepth() {
            if (this.lastSurfaceDepth2Update != this.lastUpdateXZ) {
                this.lastSurfaceDepth2Update = this.lastUpdateXZ;
                this.surfaceSecondaryDepth = this.system.getSurfaceSecondaryDepth(this.blockX, this.blockZ);
            }

            return this.surfaceSecondaryDepth;
        }

        private static int blockCoordToSurfaceCell(int i) {
            return i >> 4;
        }

        private static int surfaceCellToBlockCoord(int i) {
            return i << 4;
        }

        protected int getMinSurfaceLevel() {
            if (this.lastMinSurfaceLevelUpdate != this.lastUpdateXZ) {
                this.lastMinSurfaceLevelUpdate = this.lastUpdateXZ;
                int i = blockCoordToSurfaceCell(this.blockX);
                int j = blockCoordToSurfaceCell(this.blockZ);
                long l = ChunkCoordIntPair.pair(i, j);
                if (this.lastPreliminarySurfaceCellOrigin != l) {
                    this.lastPreliminarySurfaceCellOrigin = l;
                    this.preliminarySurfaceCache[0] = this.noiseChunk.preliminarySurfaceLevel(surfaceCellToBlockCoord(i), surfaceCellToBlockCoord(j));
                    this.preliminarySurfaceCache[1] = this.noiseChunk.preliminarySurfaceLevel(surfaceCellToBlockCoord(i + 1), surfaceCellToBlockCoord(j));
                    this.preliminarySurfaceCache[2] = this.noiseChunk.preliminarySurfaceLevel(surfaceCellToBlockCoord(i), surfaceCellToBlockCoord(j + 1));
                    this.preliminarySurfaceCache[3] = this.noiseChunk.preliminarySurfaceLevel(surfaceCellToBlockCoord(i + 1), surfaceCellToBlockCoord(j + 1));
                }

                int k = MathHelper.floor(MathHelper.lerp2((double)((float)(this.blockX & 15) / 16.0F), (double)((float)(this.blockZ & 15) / 16.0F), (double)this.preliminarySurfaceCache[0], (double)this.preliminarySurfaceCache[1], (double)this.preliminarySurfaceCache[2], (double)this.preliminarySurfaceCache[3]));
                this.minSurfaceLevel = k + this.surfaceDepth - 8;
            }

            return this.minSurfaceLevel;
        }

        final class AbovePreliminarySurfaceCondition implements SurfaceRules.Condition {
            @Override
            public boolean test() {
                return Context.this.blockY >= Context.this.getMinSurfaceLevel();
            }
        }

        static final class HoleCondition extends SurfaceRules.LazyXZCondition {
            HoleCondition(SurfaceRules.Context context) {
                super(context);
            }

            @Override
            protected boolean compute() {
                return this.context.surfaceDepth <= 0;
            }
        }

        static class SteepMaterialCondition extends SurfaceRules.LazyXZCondition {
            SteepMaterialCondition(SurfaceRules.Context context) {
                super(context);
            }

            @Override
            protected boolean compute() {
                int i = this.context.blockX & 15;
                int j = this.context.blockZ & 15;
                int k = Math.max(j - 1, 0);
                int l = Math.min(j + 1, 15);
                IChunkAccess chunkAccess = this.context.chunk;
                int m = chunkAccess.getHighestBlock(HeightMap.Type.WORLD_SURFACE_WG, i, k);
                int n = chunkAccess.getHighestBlock(HeightMap.Type.WORLD_SURFACE_WG, i, l);
                if (n >= m + 4) {
                    return true;
                } else {
                    int o = Math.max(i - 1, 0);
                    int p = Math.min(i + 1, 15);
                    int q = chunkAccess.getHighestBlock(HeightMap.Type.WORLD_SURFACE_WG, o, j);
                    int r = chunkAccess.getHighestBlock(HeightMap.Type.WORLD_SURFACE_WG, p, j);
                    return q >= r + 4;
                }
            }
        }

        static class TemperatureHelperCondition extends SurfaceRules.LazyYCondition {
            TemperatureHelperCondition(SurfaceRules.Context context) {
                super(context);
            }

            @Override
            protected boolean compute() {
                return this.context.biome.get().coldEnoughToSnow(this.context.pos.set(this.context.blockX, this.context.blockY, this.context.blockZ));
            }
        }
    }

    static enum Hole implements SurfaceRules.ConditionSource {
        INSTANCE;

        static final Codec<SurfaceRules.Hole> CODEC = Codec.unit(INSTANCE);

        @Override
        public Codec<? extends SurfaceRules.ConditionSource> codec() {
            return CODEC;
        }

        @Override
        public SurfaceRules.Condition apply(SurfaceRules.Context context) {
            return context.hole;
        }
    }

    public abstract static class LazyCondition implements SurfaceRules.Condition {
        protected final SurfaceRules.Context context;
        private long lastUpdate;
        @Nullable
        Boolean result;

        protected LazyCondition(SurfaceRules.Context context) {
            this.context = context;
            this.lastUpdate = this.getContextLastUpdate() - 1L;
        }

        @Override
        public boolean test() {
            long l = this.getContextLastUpdate();
            if (l == this.lastUpdate) {
                if (this.result == null) {
                    throw new IllegalStateException("Update triggered but the result is null");
                } else {
                    return this.result;
                }
            } else {
                this.lastUpdate = l;
                this.result = this.compute();
                return this.result;
            }
        }

        protected abstract long getContextLastUpdate();

        protected abstract boolean compute();
    }

    abstract static class LazyXZCondition extends SurfaceRules.LazyCondition {
        protected LazyXZCondition(SurfaceRules.Context context) {
            super(context);
        }

        @Override
        protected long getContextLastUpdate() {
            return this.context.lastUpdateXZ;
        }
    }

    public abstract static class LazyYCondition extends SurfaceRules.LazyCondition {
        protected LazyYCondition(SurfaceRules.Context context) {
            super(context);
        }

        @Override
        protected long getContextLastUpdate() {
            return this.context.lastUpdateY;
        }
    }

    static record NoiseThresholdConditionSource(ResourceKey<NormalNoise$NoiseParameters> noise, double minThreshold, double maxThreshold) implements SurfaceRules.ConditionSource {
        static final Codec<SurfaceRules.NoiseThresholdConditionSource> CODEC = RecordCodecBuilder.create((instance) -> {
            return instance.group(ResourceKey.codec(IRegistry.NOISE_REGISTRY).fieldOf("noise").forGetter(SurfaceRules.NoiseThresholdConditionSource::noise), Codec.DOUBLE.fieldOf("min_threshold").forGetter(SurfaceRules.NoiseThresholdConditionSource::minThreshold), Codec.DOUBLE.fieldOf("max_threshold").forGetter(SurfaceRules.NoiseThresholdConditionSource::maxThreshold)).apply(instance, SurfaceRules.NoiseThresholdConditionSource::new);
        });

        NoiseThresholdConditionSource(ResourceKey<NormalNoise$NoiseParameters> resourceKey, double d, double e) {
            this.noise = resourceKey;
            this.minThreshold = d;
            this.maxThreshold = e;
        }

        @Override
        public Codec<? extends SurfaceRules.ConditionSource> codec() {
            return CODEC;
        }

        @Override
        public SurfaceRules.Condition apply(SurfaceRules.Context context) {
            // $FF: Couldn't be decompiled
        }

        public ResourceKey<NormalNoise$NoiseParameters> noise() {
            return this.noise;
        }

        public double minThreshold() {
            return this.minThreshold;
        }

        public double maxThreshold() {
            return this.maxThreshold;
        }
    }

    static record NotCondition(SurfaceRules.Condition target) implements SurfaceRules.Condition {
        NotCondition(SurfaceRules.Condition condition) {
            this.target = condition;
        }

        @Override
        public boolean test() {
            return !this.target.test();
        }

        public SurfaceRules.Condition target() {
            return this.target;
        }
    }

    static record NotConditionSource(SurfaceRules.ConditionSource target) implements SurfaceRules.ConditionSource {
        static final Codec<SurfaceRules.NotConditionSource> CODEC = SurfaceRules.ConditionSource.CODEC.xmap(SurfaceRules.NotConditionSource::new, SurfaceRules.NotConditionSource::target).fieldOf("invert").codec();

        NotConditionSource(SurfaceRules.ConditionSource conditionSource) {
            this.target = conditionSource;
        }

        @Override
        public Codec<? extends SurfaceRules.ConditionSource> codec() {
            return CODEC;
        }

        @Override
        public SurfaceRules.Condition apply(SurfaceRules.Context context) {
            return new SurfaceRules.NotCondition(this.target.apply(context));
        }

        public SurfaceRules.ConditionSource target() {
            return this.target;
        }
    }

    public interface RuleSource extends Function<SurfaceRules.Context, SurfaceRules.SurfaceRule> {
        Codec<SurfaceRules.RuleSource> CODEC = IRegistry.RULE.byNameCodec().dispatch(SurfaceRules.RuleSource::codec, Function.identity());

        static Codec<? extends SurfaceRules.RuleSource> bootstrap() {
            IRegistry.register(IRegistry.RULE, "bandlands", SurfaceRules.Bandlands.CODEC);
            IRegistry.register(IRegistry.RULE, "block", SurfaceRules.BlockRuleSource.CODEC);
            IRegistry.register(IRegistry.RULE, "sequence", SurfaceRules.SequenceRuleSource.CODEC);
            IRegistry.register(IRegistry.RULE, "condition", SurfaceRules.TestRuleSource.CODEC);
            return IRegistry.RULE.iterator().next();
        }

        Codec<? extends SurfaceRules.RuleSource> codec();
    }

    static record SequenceRule(List<SurfaceRules.SurfaceRule> rules) implements SurfaceRules.SurfaceRule {
        SequenceRule(List<SurfaceRules.SurfaceRule> list) {
            this.rules = list;
        }

        @Nullable
        @Override
        public IBlockData tryApply(int x, int y, int z) {
            for(SurfaceRules.SurfaceRule surfaceRule : this.rules) {
                IBlockData blockState = surfaceRule.tryApply(x, y, z);
                if (blockState != null) {
                    return blockState;
                }
            }

            return null;
        }

        public List<SurfaceRules.SurfaceRule> rules() {
            return this.rules;
        }
    }

    static record SequenceRuleSource(List<SurfaceRules.RuleSource> sequence) implements SurfaceRules.RuleSource {
        static final Codec<SurfaceRules.SequenceRuleSource> CODEC = SurfaceRules.RuleSource.CODEC.listOf().xmap(SurfaceRules.SequenceRuleSource::new, SurfaceRules.SequenceRuleSource::sequence).fieldOf("sequence").codec();

        SequenceRuleSource(List<SurfaceRules.RuleSource> list) {
            this.sequence = list;
        }

        @Override
        public Codec<? extends SurfaceRules.RuleSource> codec() {
            return CODEC;
        }

        @Override
        public SurfaceRules.SurfaceRule apply(SurfaceRules.Context context) {
            if (this.sequence.size() == 1) {
                return this.sequence.get(0).apply(context);
            } else {
                Builder<SurfaceRules.SurfaceRule> builder = ImmutableList.builder();

                for(SurfaceRules.RuleSource ruleSource : this.sequence) {
                    builder.add(ruleSource.apply(context));
                }

                return new SurfaceRules.SequenceRule(builder.build());
            }
        }

        public List<SurfaceRules.RuleSource> sequence() {
            return this.sequence;
        }
    }

    static record StateRule(IBlockData state) implements SurfaceRules.SurfaceRule {
        StateRule(IBlockData blockState) {
            this.state = blockState;
        }

        @Override
        public IBlockData tryApply(int x, int y, int z) {
            return this.state;
        }

        public IBlockData state() {
            return this.state;
        }
    }

    static enum Steep implements SurfaceRules.ConditionSource {
        INSTANCE;

        static final Codec<SurfaceRules.Steep> CODEC = Codec.unit(INSTANCE);

        @Override
        public Codec<? extends SurfaceRules.ConditionSource> codec() {
            return CODEC;
        }

        @Override
        public SurfaceRules.Condition apply(SurfaceRules.Context context) {
            return context.steep;
        }
    }

    static record StoneDepthCheck(int offset, boolean addSurfaceDepth, boolean addSurfaceSecondaryDepth, CaveSurface surfaceType) implements SurfaceRules.ConditionSource {
        static final Codec<SurfaceRules.StoneDepthCheck> CODEC = RecordCodecBuilder.create((instance) -> {
            return instance.group(Codec.INT.fieldOf("offset").forGetter(SurfaceRules.StoneDepthCheck::offset), Codec.BOOL.fieldOf("add_surface_depth").forGetter(SurfaceRules.StoneDepthCheck::addSurfaceDepth), Codec.BOOL.fieldOf("add_surface_secondary_depth").forGetter(SurfaceRules.StoneDepthCheck::addSurfaceSecondaryDepth), CaveSurface.CODEC.fieldOf("surface_type").forGetter(SurfaceRules.StoneDepthCheck::surfaceType)).apply(instance, SurfaceRules.StoneDepthCheck::new);
        });

        StoneDepthCheck(int i, boolean bl, boolean bl2, CaveSurface caveSurface) {
            this.offset = i;
            this.addSurfaceDepth = bl;
            this.addSurfaceSecondaryDepth = bl2;
            this.surfaceType = caveSurface;
        }

        @Override
        public Codec<? extends SurfaceRules.ConditionSource> codec() {
            return CODEC;
        }

        @Override
        public SurfaceRules.Condition apply(SurfaceRules.Context context) {
            // $FF: Couldn't be decompiled
        }

        public int offset() {
            return this.offset;
        }

        public boolean addSurfaceDepth() {
            return this.addSurfaceDepth;
        }

        public boolean addSurfaceSecondaryDepth() {
            return this.addSurfaceSecondaryDepth;
        }

        public CaveSurface surfaceType() {
            return this.surfaceType;
        }
    }

    public interface SurfaceRule {
        @Nullable
        IBlockData tryApply(int x, int y, int z);
    }

    static enum Temperature implements SurfaceRules.ConditionSource {
        INSTANCE;

        static final Codec<SurfaceRules.Temperature> CODEC = Codec.unit(INSTANCE);

        @Override
        public Codec<? extends SurfaceRules.ConditionSource> codec() {
            return CODEC;
        }

        @Override
        public SurfaceRules.Condition apply(SurfaceRules.Context context) {
            return context.temperature;
        }
    }

    static record TestRule(SurfaceRules.Condition condition, SurfaceRules.SurfaceRule followup) implements SurfaceRules.SurfaceRule {
        TestRule(SurfaceRules.Condition condition, SurfaceRules.SurfaceRule surfaceRule) {
            this.condition = condition;
            this.followup = surfaceRule;
        }

        @Nullable
        @Override
        public IBlockData tryApply(int x, int y, int z) {
            return !this.condition.test() ? null : this.followup.tryApply(x, y, z);
        }

        public SurfaceRules.Condition condition() {
            return this.condition;
        }

        public SurfaceRules.SurfaceRule followup() {
            return this.followup;
        }
    }

    static record TestRuleSource(SurfaceRules.ConditionSource ifTrue, SurfaceRules.RuleSource thenRun) implements SurfaceRules.RuleSource {
        static final Codec<SurfaceRules.TestRuleSource> CODEC = RecordCodecBuilder.create((instance) -> {
            return instance.group(SurfaceRules.ConditionSource.CODEC.fieldOf("if_true").forGetter(SurfaceRules.TestRuleSource::ifTrue), SurfaceRules.RuleSource.CODEC.fieldOf("then_run").forGetter(SurfaceRules.TestRuleSource::thenRun)).apply(instance, SurfaceRules.TestRuleSource::new);
        });

        TestRuleSource(SurfaceRules.ConditionSource conditionSource, SurfaceRules.RuleSource ruleSource) {
            this.ifTrue = conditionSource;
            this.thenRun = ruleSource;
        }

        @Override
        public Codec<? extends SurfaceRules.RuleSource> codec() {
            return CODEC;
        }

        @Override
        public SurfaceRules.SurfaceRule apply(SurfaceRules.Context context) {
            return new SurfaceRules.TestRule(this.ifTrue.apply(context), this.thenRun.apply(context));
        }

        public SurfaceRules.ConditionSource ifTrue() {
            return this.ifTrue;
        }

        public SurfaceRules.RuleSource thenRun() {
            return this.thenRun;
        }
    }

    public static record VerticalGradientConditionSource(MinecraftKey randomName, VerticalAnchor trueAtAndBelow, VerticalAnchor falseAtAndAbove) implements SurfaceRules.ConditionSource {
        static final Codec<SurfaceRules.VerticalGradientConditionSource> CODEC = RecordCodecBuilder.create((instance) -> {
            return instance.group(MinecraftKey.CODEC.fieldOf("random_name").forGetter(SurfaceRules.VerticalGradientConditionSource::randomName), VerticalAnchor.CODEC.fieldOf("true_at_and_below").forGetter(SurfaceRules.VerticalGradientConditionSource::trueAtAndBelow), VerticalAnchor.CODEC.fieldOf("false_at_and_above").forGetter(SurfaceRules.VerticalGradientConditionSource::falseAtAndAbove)).apply(instance, SurfaceRules.VerticalGradientConditionSource::new);
        });

        VerticalGradientConditionSource(MinecraftKey resourceLocation, VerticalAnchor verticalAnchor, VerticalAnchor verticalAnchor2) {
            this.randomName = resourceLocation;
            this.trueAtAndBelow = verticalAnchor;
            this.falseAtAndAbove = verticalAnchor2;
        }

        @Override
        public Codec<? extends SurfaceRules.ConditionSource> codec() {
            return CODEC;
        }

        @Override
        public SurfaceRules.Condition apply(SurfaceRules.Context context) {
            // $FF: Couldn't be decompiled
        }

        public MinecraftKey randomName() {
            return this.randomName;
        }

        public VerticalAnchor trueAtAndBelow() {
            return this.trueAtAndBelow;
        }

        public VerticalAnchor falseAtAndAbove() {
            return this.falseAtAndAbove;
        }
    }

    static record WaterConditionSource(int offset, int surfaceDepthMultiplier, boolean addStoneDepth) implements SurfaceRules.ConditionSource {
        static final Codec<SurfaceRules.WaterConditionSource> CODEC = RecordCodecBuilder.create((instance) -> {
            return instance.group(Codec.INT.fieldOf("offset").forGetter(SurfaceRules.WaterConditionSource::offset), Codec.intRange(-20, 20).fieldOf("surface_depth_multiplier").forGetter(SurfaceRules.WaterConditionSource::surfaceDepthMultiplier), Codec.BOOL.fieldOf("add_stone_depth").forGetter(SurfaceRules.WaterConditionSource::addStoneDepth)).apply(instance, SurfaceRules.WaterConditionSource::new);
        });

        WaterConditionSource(int i, int j, boolean bl) {
            this.offset = i;
            this.surfaceDepthMultiplier = j;
            this.addStoneDepth = bl;
        }

        @Override
        public Codec<? extends SurfaceRules.ConditionSource> codec() {
            return CODEC;
        }

        @Override
        public SurfaceRules.Condition apply(SurfaceRules.Context context) {
            // $FF: Couldn't be decompiled
        }

        public int offset() {
            return this.offset;
        }

        public int surfaceDepthMultiplier() {
            return this.surfaceDepthMultiplier;
        }

        public boolean addStoneDepth() {
            return this.addStoneDepth;
        }
    }

    static record YConditionSource(VerticalAnchor anchor, int surfaceDepthMultiplier, boolean addStoneDepth) implements SurfaceRules.ConditionSource {
        static final Codec<SurfaceRules.YConditionSource> CODEC = RecordCodecBuilder.create((instance) -> {
            return instance.group(VerticalAnchor.CODEC.fieldOf("anchor").forGetter(SurfaceRules.YConditionSource::anchor), Codec.intRange(-20, 20).fieldOf("surface_depth_multiplier").forGetter(SurfaceRules.YConditionSource::surfaceDepthMultiplier), Codec.BOOL.fieldOf("add_stone_depth").forGetter(SurfaceRules.YConditionSource::addStoneDepth)).apply(instance, SurfaceRules.YConditionSource::new);
        });

        YConditionSource(VerticalAnchor verticalAnchor, int i, boolean bl) {
            this.anchor = verticalAnchor;
            this.surfaceDepthMultiplier = i;
            this.addStoneDepth = bl;
        }

        @Override
        public Codec<? extends SurfaceRules.ConditionSource> codec() {
            return CODEC;
        }

        @Override
        public SurfaceRules.Condition apply(SurfaceRules.Context context) {
            // $FF: Couldn't be decompiled
        }

        public VerticalAnchor anchor() {
            return this.anchor;
        }

        public int surfaceDepthMultiplier() {
            return this.surfaceDepthMultiplier;
        }

        public boolean addStoneDepth() {
            return this.addStoneDepth;
        }
    }
}
