package net.minecraft.world.level.levelgen;

import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPosition;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.util.MathHelper;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import net.minecraft.world.level.biome.TerrainShaper;
import net.minecraft.world.level.biome.WorldChunkManagerTheEnd;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;
import net.minecraft.world.level.levelgen.synth.NoiseGenerator3Handler;
import net.minecraft.world.level.levelgen.synth.NoiseGeneratorNormal;
import net.minecraft.world.level.levelgen.synth.NoiseUtils;
import net.minecraft.world.level.levelgen.synth.NormalNoise$NoiseParameters;

public class NoiseSampler implements Climate.Sampler {
    private static final float ORE_VEIN_RARITY = 1.0F;
    private static final float ORE_THICKNESS = 0.08F;
    private static final float VEININESS_THRESHOLD = 0.4F;
    private static final double VEININESS_FREQUENCY = 1.5D;
    private static final int EDGE_ROUNDOFF_BEGIN = 20;
    private static final double MAX_EDGE_ROUNDOFF = 0.2D;
    private static final float VEIN_SOLIDNESS = 0.7F;
    private static final float MIN_RICHNESS = 0.1F;
    private static final float MAX_RICHNESS = 0.3F;
    private static final float MAX_RICHNESS_THRESHOLD = 0.6F;
    private static final float CHANCE_OF_RAW_ORE_BLOCK = 0.02F;
    private static final float SKIP_ORE_IF_GAP_NOISE_IS_BELOW = -0.3F;
    private static final double NOODLE_SPACING_AND_STRAIGHTNESS = 1.5D;
    private final NoiseSettings noiseSettings;
    private final boolean isNoiseCavesEnabled;
    private final NoiseChunk.InterpolatableNoise baseNoise;
    private final BlendedNoise blendedNoise;
    @Nullable
    private final NoiseGenerator3Handler islandNoise;
    private final NoiseGeneratorNormal jaggedNoise;
    private final NoiseGeneratorNormal barrierNoise;
    private final NoiseGeneratorNormal fluidLevelFloodednessNoise;
    private final NoiseGeneratorNormal fluidLevelSpreadNoise;
    private final NoiseGeneratorNormal lavaNoise;
    private final NoiseGeneratorNormal layerNoiseSource;
    private final NoiseGeneratorNormal pillarNoiseSource;
    private final NoiseGeneratorNormal pillarRarenessModulator;
    private final NoiseGeneratorNormal pillarThicknessModulator;
    private final NoiseGeneratorNormal spaghetti2DNoiseSource;
    private final NoiseGeneratorNormal spaghetti2DElevationModulator;
    private final NoiseGeneratorNormal spaghetti2DRarityModulator;
    private final NoiseGeneratorNormal spaghetti2DThicknessModulator;
    private final NoiseGeneratorNormal spaghetti3DNoiseSource1;
    private final NoiseGeneratorNormal spaghetti3DNoiseSource2;
    private final NoiseGeneratorNormal spaghetti3DRarityModulator;
    private final NoiseGeneratorNormal spaghetti3DThicknessModulator;
    private final NoiseGeneratorNormal spaghettiRoughnessNoise;
    private final NoiseGeneratorNormal spaghettiRoughnessModulator;
    private final NoiseGeneratorNormal bigEntranceNoiseSource;
    private final NoiseGeneratorNormal cheeseNoiseSource;
    private final NoiseGeneratorNormal temperatureNoise;
    private final NoiseGeneratorNormal humidityNoise;
    private final NoiseGeneratorNormal continentalnessNoise;
    private final NoiseGeneratorNormal erosionNoise;
    private final NoiseGeneratorNormal weirdnessNoise;
    private final NoiseGeneratorNormal offsetNoise;
    private final NoiseGeneratorNormal gapNoise;
    private final NoiseChunk.InterpolatableNoise veininess;
    private final NoiseChunk.InterpolatableNoise veinA;
    private final NoiseChunk.InterpolatableNoise veinB;
    private final NoiseChunk.InterpolatableNoise noodleToggle;
    private final NoiseChunk.InterpolatableNoise noodleThickness;
    private final NoiseChunk.InterpolatableNoise noodleRidgeA;
    private final NoiseChunk.InterpolatableNoise noodleRidgeB;
    private final PositionalRandomFactory aquiferPositionalRandomFactory;
    private final PositionalRandomFactory oreVeinsPositionalRandomFactory;
    private final PositionalRandomFactory depthBasedLayerPositionalRandomFactory;
    private final List<Climate.ParameterPoint> spawnTarget = (new OverworldBiomeBuilder()).spawnTarget();
    private final boolean amplified;

    public NoiseSampler(NoiseSettings config, boolean hasNoiseCaves, long seed, IRegistry<NormalNoise$NoiseParameters> noiseRegistry, WorldgenRandom$Algorithm randomProvider) {
        this.noiseSettings = config;
        this.isNoiseCavesEnabled = hasNoiseCaves;
        this.baseNoise = (chunkNoiseSampler) -> {
            return chunkNoiseSampler.createNoiseInterpolator((x, y, z) -> {
                return this.calculateBaseNoise(x, y, z, chunkNoiseSampler.noiseData(QuartPos.fromBlock(x), QuartPos.fromBlock(z)).terrainInfo(), chunkNoiseSampler.getBlender());
            });
        };
        if (config.islandNoiseOverride()) {
            RandomSource randomSource = randomProvider.newInstance(seed);
            randomSource.consumeCount(17292);
            this.islandNoise = new NoiseGenerator3Handler(randomSource);
        } else {
            this.islandNoise = null;
        }

        this.amplified = config.isAmplified();
        int i = config.minY();
        int j = Stream.of(NoiseSampler.VeinType.values()).mapToInt((veinType) -> {
            return veinType.minY;
        }).min().orElse(i);
        int k = Stream.of(NoiseSampler.VeinType.values()).mapToInt((veinType) -> {
            return veinType.maxY;
        }).max().orElse(i);
        float f = 4.0F;
        double d = 2.6666666666666665D;
        int l = i + 4;
        int m = i + config.height();
        boolean bl = config.largeBiomes();
        PositionalRandomFactory positionalRandomFactory = randomProvider.newInstance(seed).forkPositional();
        if (randomProvider != WorldgenRandom$Algorithm.LEGACY) {
            this.blendedNoise = new BlendedNoise(positionalRandomFactory.fromHashOf(new MinecraftKey("terrain")), config.noiseSamplingSettings(), config.getCellWidth(), config.getCellHeight());
            this.temperatureNoise = Noises.instantiate(noiseRegistry, positionalRandomFactory, bl ? Noises.TEMPERATURE_LARGE : Noises.TEMPERATURE);
            this.humidityNoise = Noises.instantiate(noiseRegistry, positionalRandomFactory, bl ? Noises.VEGETATION_LARGE : Noises.VEGETATION);
            this.offsetNoise = Noises.instantiate(noiseRegistry, positionalRandomFactory, Noises.SHIFT);
        } else {
            this.blendedNoise = new BlendedNoise(randomProvider.newInstance(seed), config.noiseSamplingSettings(), config.getCellWidth(), config.getCellHeight());
            this.temperatureNoise = NoiseGeneratorNormal.createLegacyNetherBiome(randomProvider.newInstance(seed), new NormalNoise$NoiseParameters(-7, 1.0D, 1.0D));
            this.humidityNoise = NoiseGeneratorNormal.createLegacyNetherBiome(randomProvider.newInstance(seed + 1L), new NormalNoise$NoiseParameters(-7, 1.0D, 1.0D));
            this.offsetNoise = NoiseGeneratorNormal.create(positionalRandomFactory.fromHashOf(Noises.SHIFT.location()), new NormalNoise$NoiseParameters(0, 0.0D));
        }

        this.aquiferPositionalRandomFactory = positionalRandomFactory.fromHashOf(new MinecraftKey("aquifer")).forkPositional();
        this.oreVeinsPositionalRandomFactory = positionalRandomFactory.fromHashOf(new MinecraftKey("ore")).forkPositional();
        this.depthBasedLayerPositionalRandomFactory = positionalRandomFactory.fromHashOf(new MinecraftKey("depth_based_layer")).forkPositional();
        this.barrierNoise = Noises.instantiate(noiseRegistry, positionalRandomFactory, Noises.AQUIFER_BARRIER);
        this.fluidLevelFloodednessNoise = Noises.instantiate(noiseRegistry, positionalRandomFactory, Noises.AQUIFER_FLUID_LEVEL_FLOODEDNESS);
        this.lavaNoise = Noises.instantiate(noiseRegistry, positionalRandomFactory, Noises.AQUIFER_LAVA);
        this.fluidLevelSpreadNoise = Noises.instantiate(noiseRegistry, positionalRandomFactory, Noises.AQUIFER_FLUID_LEVEL_SPREAD);
        this.pillarNoiseSource = Noises.instantiate(noiseRegistry, positionalRandomFactory, Noises.PILLAR);
        this.pillarRarenessModulator = Noises.instantiate(noiseRegistry, positionalRandomFactory, Noises.PILLAR_RARENESS);
        this.pillarThicknessModulator = Noises.instantiate(noiseRegistry, positionalRandomFactory, Noises.PILLAR_THICKNESS);
        this.spaghetti2DNoiseSource = Noises.instantiate(noiseRegistry, positionalRandomFactory, Noises.SPAGHETTI_2D);
        this.spaghetti2DElevationModulator = Noises.instantiate(noiseRegistry, positionalRandomFactory, Noises.SPAGHETTI_2D_ELEVATION);
        this.spaghetti2DRarityModulator = Noises.instantiate(noiseRegistry, positionalRandomFactory, Noises.SPAGHETTI_2D_MODULATOR);
        this.spaghetti2DThicknessModulator = Noises.instantiate(noiseRegistry, positionalRandomFactory, Noises.SPAGHETTI_2D_THICKNESS);
        this.spaghetti3DNoiseSource1 = Noises.instantiate(noiseRegistry, positionalRandomFactory, Noises.SPAGHETTI_3D_1);
        this.spaghetti3DNoiseSource2 = Noises.instantiate(noiseRegistry, positionalRandomFactory, Noises.SPAGHETTI_3D_2);
        this.spaghetti3DRarityModulator = Noises.instantiate(noiseRegistry, positionalRandomFactory, Noises.SPAGHETTI_3D_RARITY);
        this.spaghetti3DThicknessModulator = Noises.instantiate(noiseRegistry, positionalRandomFactory, Noises.SPAGHETTI_3D_THICKNESS);
        this.spaghettiRoughnessNoise = Noises.instantiate(noiseRegistry, positionalRandomFactory, Noises.SPAGHETTI_ROUGHNESS);
        this.spaghettiRoughnessModulator = Noises.instantiate(noiseRegistry, positionalRandomFactory, Noises.SPAGHETTI_ROUGHNESS_MODULATOR);
        this.bigEntranceNoiseSource = Noises.instantiate(noiseRegistry, positionalRandomFactory, Noises.CAVE_ENTRANCE);
        this.layerNoiseSource = Noises.instantiate(noiseRegistry, positionalRandomFactory, Noises.CAVE_LAYER);
        this.cheeseNoiseSource = Noises.instantiate(noiseRegistry, positionalRandomFactory, Noises.CAVE_CHEESE);
        this.continentalnessNoise = Noises.instantiate(noiseRegistry, positionalRandomFactory, bl ? Noises.CONTINENTALNESS_LARGE : Noises.CONTINENTALNESS);
        this.erosionNoise = Noises.instantiate(noiseRegistry, positionalRandomFactory, bl ? Noises.EROSION_LARGE : Noises.EROSION);
        this.weirdnessNoise = Noises.instantiate(noiseRegistry, positionalRandomFactory, Noises.RIDGE);
        this.veininess = yLimitedInterpolatableNoise(Noises.instantiate(noiseRegistry, positionalRandomFactory, Noises.ORE_VEININESS), j, k, 0, 1.5D);
        this.veinA = yLimitedInterpolatableNoise(Noises.instantiate(noiseRegistry, positionalRandomFactory, Noises.ORE_VEIN_A), j, k, 0, 4.0D);
        this.veinB = yLimitedInterpolatableNoise(Noises.instantiate(noiseRegistry, positionalRandomFactory, Noises.ORE_VEIN_B), j, k, 0, 4.0D);
        this.gapNoise = Noises.instantiate(noiseRegistry, positionalRandomFactory, Noises.ORE_GAP);
        this.noodleToggle = yLimitedInterpolatableNoise(Noises.instantiate(noiseRegistry, positionalRandomFactory, Noises.NOODLE), l, m, -1, 1.0D);
        this.noodleThickness = yLimitedInterpolatableNoise(Noises.instantiate(noiseRegistry, positionalRandomFactory, Noises.NOODLE_THICKNESS), l, m, 0, 1.0D);
        this.noodleRidgeA = yLimitedInterpolatableNoise(Noises.instantiate(noiseRegistry, positionalRandomFactory, Noises.NOODLE_RIDGE_A), l, m, 0, 2.6666666666666665D);
        this.noodleRidgeB = yLimitedInterpolatableNoise(Noises.instantiate(noiseRegistry, positionalRandomFactory, Noises.NOODLE_RIDGE_B), l, m, 0, 2.6666666666666665D);
        this.jaggedNoise = Noises.instantiate(noiseRegistry, positionalRandomFactory, Noises.JAGGED);
    }

    private static NoiseChunk.InterpolatableNoise yLimitedInterpolatableNoise(NoiseGeneratorNormal noiseSampler, int minY, int maxY, int alternative, double scale) {
        NoiseChunk.NoiseFiller noiseFiller = (x, y, z) -> {
            return y <= maxY && y >= minY ? noiseSampler.getValue((double)x * scale, (double)y * scale, (double)z * scale) : (double)alternative;
        };
        return (chunkNoiseSampler) -> {
            return chunkNoiseSampler.createNoiseInterpolator(noiseFiller);
        };
    }

    private double calculateBaseNoise(int x, int y, int z, TerrainInfo point, Blender blender) {
        double d = this.blendedNoise.calculateNoise(x, y, z);
        boolean bl = !this.isNoiseCavesEnabled;
        return this.calculateBaseNoise(x, y, z, point, d, bl, true, blender);
    }

    private double calculateBaseNoise(int x, int y, int z, TerrainInfo point, double noise, boolean hasNoNoiseCaves, boolean bl, Blender blender) {
        double d;
        if (this.islandNoise != null) {
            d = ((double)WorldChunkManagerTheEnd.getHeightValue(this.islandNoise, x / 8, z / 8) - 8.0D) / 128.0D;
        } else {
            double e = bl ? this.sampleJaggedNoise(point.jaggedness(), (double)x, (double)z) : 0.0D;
            double f = (this.computeBaseDensity(y, point) + e) * point.factor();
            d = f * (double)(f > 0.0D ? 4 : 1);
        }

        double h = d + noise;
        double i = 1.5625D;
        double r;
        double s;
        double t;
        if (!hasNoNoiseCaves && !(h < -64.0D)) {
            double m = h - 1.5625D;
            boolean bl2 = m < 0.0D;
            double n = this.getBigEntrances(x, y, z);
            double o = this.spaghettiRoughness(x, y, z);
            double p = this.getSpaghetti3D(x, y, z);
            double q = Math.min(n, p + o);
            if (bl2) {
                r = h;
                s = q * 5.0D;
                t = -64.0D;
            } else {
                double u = this.getLayerizedCaverns(x, y, z);
                if (u > 64.0D) {
                    r = 64.0D;
                } else {
                    double w = this.cheeseNoiseSource.getValue((double)x, (double)y / 1.5D, (double)z);
                    double aa = MathHelper.clamp(w + 0.27D, -1.0D, 1.0D);
                    double ab = m * 1.28D;
                    double ac = aa + MathHelper.clampedLerp(0.5D, 0.0D, ab);
                    r = ac + u;
                }

                double ae = this.getSpaghetti2D(x, y, z);
                s = Math.min(q, ae + o);
                t = this.getPillars(x, y, z);
            }
        } else {
            r = h;
            s = 64.0D;
            t = -64.0D;
        }

        double ah = Math.max(Math.min(r, s), t);
        ah = this.applySlide(ah, y / this.noiseSettings.getCellHeight());
        ah = blender.blendDensity(x, y, z, ah);
        return MathHelper.clamp(ah, -64.0D, 64.0D);
    }

    private double sampleJaggedNoise(double d, double e, double f) {
        if (d == 0.0D) {
            return 0.0D;
        } else {
            float g = 1500.0F;
            double h = this.jaggedNoise.getValue(e * 1500.0D, 0.0D, f * 1500.0D);
            return h > 0.0D ? d * h : d / 2.0D * h;
        }
    }

    private double computeBaseDensity(int i, TerrainInfo terrainInfo) {
        double d = 1.0D - (double)i / 128.0D;
        return d + terrainInfo.offset();
    }

    private double applySlide(double noise, int y) {
        int i = y - this.noiseSettings.getMinCellY();
        noise = this.noiseSettings.topSlideSettings().applySlide(noise, this.noiseSettings.getCellCountY() - i);
        return this.noiseSettings.bottomSlideSettings().applySlide(noise, i);
    }

    protected NoiseChunk.BlockStateFiller makeBaseNoiseFiller(NoiseChunk chunkNoiseSampler, NoiseChunk.NoiseFiller columnSampler, boolean hasNoodleCaves) {
        NoiseChunk.Sampler sampler = this.baseNoise.instantiate(chunkNoiseSampler);
        NoiseChunk.Sampler sampler2 = hasNoodleCaves ? this.noodleToggle.instantiate(chunkNoiseSampler) : () -> {
            return -1.0D;
        };
        NoiseChunk.Sampler sampler3 = hasNoodleCaves ? this.noodleThickness.instantiate(chunkNoiseSampler) : () -> {
            return 0.0D;
        };
        NoiseChunk.Sampler sampler4 = hasNoodleCaves ? this.noodleRidgeA.instantiate(chunkNoiseSampler) : () -> {
            return 0.0D;
        };
        NoiseChunk.Sampler sampler5 = hasNoodleCaves ? this.noodleRidgeB.instantiate(chunkNoiseSampler) : () -> {
            return 0.0D;
        };
        return (x, y, z) -> {
            double d = sampler.sample();
            double e = MathHelper.clamp(d * 0.64D, -1.0D, 1.0D);
            e = e / 2.0D - e * e * e / 24.0D;
            if (sampler2.sample() >= 0.0D) {
                double f = 0.05D;
                double g = 0.1D;
                double h = MathHelper.clampedMap(sampler3.sample(), -1.0D, 1.0D, 0.05D, 0.1D);
                double i = Math.abs(1.5D * sampler4.sample()) - h;
                double j = Math.abs(1.5D * sampler5.sample()) - h;
                e = Math.min(e, Math.max(i, j));
            }

            e = e + columnSampler.calculateNoise(x, y, z);
            return chunkNoiseSampler.aquifer().computeSubstance(x, y, z, d, e);
        };
    }

    protected NoiseChunk.BlockStateFiller makeOreVeinifier(NoiseChunk chunkNoiseSampler, boolean hasOreVeins) {
        if (!hasOreVeins) {
            return (x, y, z) -> {
                return null;
            };
        } else {
            NoiseChunk.Sampler sampler = this.veininess.instantiate(chunkNoiseSampler);
            NoiseChunk.Sampler sampler2 = this.veinA.instantiate(chunkNoiseSampler);
            NoiseChunk.Sampler sampler3 = this.veinB.instantiate(chunkNoiseSampler);
            IBlockData blockState = null;
            return (x, y, z) -> {
                RandomSource randomSource = this.oreVeinsPositionalRandomFactory.at(x, y, z);
                double d = sampler.sample();
                NoiseSampler.VeinType veinType = this.getVeinType(d, y);
                if (veinType == null) {
                    return blockState;
                } else if (randomSource.nextFloat() > 0.7F) {
                    return blockState;
                } else if (this.isVein(sampler2.sample(), sampler3.sample())) {
                    double e = MathHelper.clampedMap(Math.abs(d), (double)0.4F, (double)0.6F, (double)0.1F, (double)0.3F);
                    if ((double)randomSource.nextFloat() < e && this.gapNoise.getValue((double)x, (double)y, (double)z) > (double)-0.3F) {
                        return randomSource.nextFloat() < 0.02F ? veinType.rawOreBlock : veinType.ore;
                    } else {
                        return veinType.filler;
                    }
                } else {
                    return blockState;
                }
            };
        }
    }

    protected int getPreliminarySurfaceLevel(int x, int z, TerrainInfo point) {
        for(int i = this.noiseSettings.getMinCellY() + this.noiseSettings.getCellCountY(); i >= this.noiseSettings.getMinCellY(); --i) {
            int j = i * this.noiseSettings.getCellHeight();
            double d = -0.703125D;
            double e = this.calculateBaseNoise(x, j, z, point, -0.703125D, true, false, Blender.empty());
            if (e > 0.390625D) {
                return j;
            }
        }

        return Integer.MAX_VALUE;
    }

    protected Aquifer createAquifer(NoiseChunk chunkNoiseSampler, int x, int z, int minimumY, int height, Aquifer.FluidPicker fluidLevelSampler, boolean hasAquifers) {
        if (!hasAquifers) {
            return Aquifer.createDisabled(fluidLevelSampler);
        } else {
            int i = SectionPosition.blockToSectionCoord(x);
            int j = SectionPosition.blockToSectionCoord(z);
            return Aquifer.create(chunkNoiseSampler, new ChunkCoordIntPair(i, j), this.barrierNoise, this.fluidLevelFloodednessNoise, this.fluidLevelSpreadNoise, this.lavaNoise, this.aquiferPositionalRandomFactory, minimumY * this.noiseSettings.getCellHeight(), height * this.noiseSettings.getCellHeight(), fluidLevelSampler);
        }
    }

    @VisibleForDebug
    public NoiseSampler.FlatNoiseData noiseData(int i, int j, Blender blender) {
        double d = (double)i + this.getOffset(i, 0, j);
        double e = (double)j + this.getOffset(j, i, 0);
        double f = this.getContinentalness(d, 0.0D, e);
        double g = this.getWeirdness(d, 0.0D, e);
        double h = this.getErosion(d, 0.0D, e);
        TerrainInfo terrainInfo = this.terrainInfo(QuartPos.toBlock(i), QuartPos.toBlock(j), (float)f, (float)g, (float)h, blender);
        return new NoiseSampler.FlatNoiseData(d, e, f, g, h, terrainInfo);
    }

    @Override
    public Climate.TargetPoint sample(int x, int y, int z) {
        return this.target(x, y, z, this.noiseData(x, z, Blender.empty()));
    }

    @VisibleForDebug
    public Climate.TargetPoint target(int i, int j, int k, NoiseSampler.FlatNoiseData flatNoiseData) {
        double d = flatNoiseData.shiftedX();
        double e = (double)j + this.getOffset(j, k, i);
        double f = flatNoiseData.shiftedZ();
        double g = this.computeBaseDensity(QuartPos.toBlock(j), flatNoiseData.terrainInfo());
        return Climate.target((float)this.getTemperature(d, e, f), (float)this.getHumidity(d, e, f), (float)flatNoiseData.continentalness(), (float)flatNoiseData.erosion(), (float)g, (float)flatNoiseData.weirdness());
    }

    public TerrainInfo terrainInfo(int x, int z, float continentalness, float weirdness, float erosion, Blender blender) {
        TerrainShaper terrainShaper = this.noiseSettings.terrainShaper();
        TerrainShaper.Point point = terrainShaper.makePoint(continentalness, erosion, weirdness);
        float f = terrainShaper.offset(point);
        float g = terrainShaper.factor(point);
        float h = terrainShaper.jaggedness(point);
        TerrainInfo terrainInfo = new TerrainInfo((double)f, (double)g, (double)h);
        return blender.blendOffsetAndFactor(x, z, terrainInfo);
    }

    @Override
    public BlockPosition findSpawnPosition() {
        return Climate.findSpawnPosition(this.spawnTarget, this);
    }

    @VisibleForDebug
    public double getOffset(int x, int y, int z) {
        return this.offsetNoise.getValue((double)x, (double)y, (double)z) * 4.0D;
    }

    private double getTemperature(double x, double y, double z) {
        return this.temperatureNoise.getValue(x, 0.0D, z);
    }

    private double getHumidity(double x, double y, double z) {
        return this.humidityNoise.getValue(x, 0.0D, z);
    }

    @VisibleForDebug
    public double getContinentalness(double x, double y, double z) {
        if (SharedConstants.debugGenerateSquareTerrainWithoutNoise) {
            if (SharedConstants.debugVoidTerrain(new ChunkCoordIntPair(QuartPos.toSection(MathHelper.floor(x)), QuartPos.toSection(MathHelper.floor(z))))) {
                return -1.0D;
            } else {
                double d = MathHelper.frac(x / 2048.0D) * 2.0D - 1.0D;
                return d * d * (double)(d < 0.0D ? -1 : 1);
            }
        } else if (SharedConstants.debugGenerateStripedTerrainWithoutNoise) {
            double e = x * 0.005D;
            return Math.sin(e + 0.5D * Math.sin(e));
        } else {
            return this.continentalnessNoise.getValue(x, y, z);
        }
    }

    @VisibleForDebug
    public double getErosion(double x, double y, double z) {
        if (SharedConstants.debugGenerateSquareTerrainWithoutNoise) {
            if (SharedConstants.debugVoidTerrain(new ChunkCoordIntPair(QuartPos.toSection(MathHelper.floor(x)), QuartPos.toSection(MathHelper.floor(z))))) {
                return -1.0D;
            } else {
                double d = MathHelper.frac(z / 256.0D) * 2.0D - 1.0D;
                return d * d * (double)(d < 0.0D ? -1 : 1);
            }
        } else if (SharedConstants.debugGenerateStripedTerrainWithoutNoise) {
            double e = z * 0.005D;
            return Math.sin(e + 0.5D * Math.sin(e));
        } else {
            return this.erosionNoise.getValue(x, y, z);
        }
    }

    @VisibleForDebug
    public double getWeirdness(double x, double y, double z) {
        return this.weirdnessNoise.getValue(x, y, z);
    }

    private double getBigEntrances(int x, int y, int z) {
        double d = 0.75D;
        double e = 0.5D;
        double f = 0.37D;
        double g = this.bigEntranceNoiseSource.getValue((double)x * 0.75D, (double)y * 0.5D, (double)z * 0.75D) + 0.37D;
        int i = -10;
        double h = (double)(y - -10) / 40.0D;
        double j = 0.3D;
        return g + MathHelper.clampedLerp(0.3D, 0.0D, h);
    }

    private double getPillars(int x, int y, int z) {
        double d = 0.0D;
        double e = 2.0D;
        double f = NoiseUtils.sampleNoiseAndMapToRange(this.pillarRarenessModulator, (double)x, (double)y, (double)z, 0.0D, 2.0D);
        double g = 0.0D;
        double h = 1.1D;
        double i = NoiseUtils.sampleNoiseAndMapToRange(this.pillarThicknessModulator, (double)x, (double)y, (double)z, 0.0D, 1.1D);
        i = Math.pow(i, 3.0D);
        double j = 25.0D;
        double k = 0.3D;
        double l = this.pillarNoiseSource.getValue((double)x * 25.0D, (double)y * 0.3D, (double)z * 25.0D);
        l = i * (l * 2.0D - f);
        return l > 0.03D ? l : Double.NEGATIVE_INFINITY;
    }

    private double getLayerizedCaverns(int x, int y, int z) {
        double d = this.layerNoiseSource.getValue((double)x, (double)(y * 8), (double)z);
        return MathHelper.square(d) * 4.0D;
    }

    private double getSpaghetti3D(int x, int y, int z) {
        double d = this.spaghetti3DRarityModulator.getValue((double)(x * 2), (double)y, (double)(z * 2));
        double e = NoiseSampler.QuantizedSpaghettiRarity.getSpaghettiRarity3D(d);
        double f = 0.065D;
        double g = 0.088D;
        double h = NoiseUtils.sampleNoiseAndMapToRange(this.spaghetti3DThicknessModulator, (double)x, (double)y, (double)z, 0.065D, 0.088D);
        double i = sampleWithRarity(this.spaghetti3DNoiseSource1, (double)x, (double)y, (double)z, e);
        double j = Math.abs(e * i) - h;
        double k = sampleWithRarity(this.spaghetti3DNoiseSource2, (double)x, (double)y, (double)z, e);
        double l = Math.abs(e * k) - h;
        return clampToUnit(Math.max(j, l));
    }

    private double getSpaghetti2D(int x, int y, int z) {
        double d = this.spaghetti2DRarityModulator.getValue((double)(x * 2), (double)y, (double)(z * 2));
        double e = NoiseSampler.QuantizedSpaghettiRarity.getSphaghettiRarity2D(d);
        double f = 0.6D;
        double g = 1.3D;
        double h = NoiseUtils.sampleNoiseAndMapToRange(this.spaghetti2DThicknessModulator, (double)(x * 2), (double)y, (double)(z * 2), 0.6D, 1.3D);
        double i = sampleWithRarity(this.spaghetti2DNoiseSource, (double)x, (double)y, (double)z, e);
        double j = 0.083D;
        double k = Math.abs(e * i) - 0.083D * h;
        int l = this.noiseSettings.getMinCellY();
        int m = 8;
        double n = NoiseUtils.sampleNoiseAndMapToRange(this.spaghetti2DElevationModulator, (double)x, 0.0D, (double)z, (double)l, 8.0D);
        double o = Math.abs(n - (double)y / 8.0D) - 1.0D * h;
        o = o * o * o;
        return clampToUnit(Math.max(o, k));
    }

    private double spaghettiRoughness(int x, int y, int z) {
        double d = NoiseUtils.sampleNoiseAndMapToRange(this.spaghettiRoughnessModulator, (double)x, (double)y, (double)z, 0.0D, 0.1D);
        return (0.4D - Math.abs(this.spaghettiRoughnessNoise.getValue((double)x, (double)y, (double)z))) * d;
    }

    public PositionalRandomFactory getDepthBasedLayerPositionalRandom() {
        return this.depthBasedLayerPositionalRandomFactory;
    }

    private static double clampToUnit(double value) {
        return MathHelper.clamp(value, -1.0D, 1.0D);
    }

    private static double sampleWithRarity(NoiseGeneratorNormal sampler, double x, double y, double z, double invertedScale) {
        return sampler.getValue(x / invertedScale, y / invertedScale, z / invertedScale);
    }

    private boolean isVein(double firstOrePlacementNoise, double secondOrePlacementNoise) {
        double d = Math.abs(1.0D * firstOrePlacementNoise) - (double)0.08F;
        double e = Math.abs(1.0D * secondOrePlacementNoise) - (double)0.08F;
        return Math.max(d, e) < 0.0D;
    }

    @Nullable
    private NoiseSampler.VeinType getVeinType(double oreFrequencyNoise, int y) {
        NoiseSampler.VeinType veinType = oreFrequencyNoise > 0.0D ? NoiseSampler.VeinType.COPPER : NoiseSampler.VeinType.IRON;
        int i = veinType.maxY - y;
        int j = y - veinType.minY;
        if (j >= 0 && i >= 0) {
            int k = Math.min(i, j);
            double d = MathHelper.clampedMap((double)k, 0.0D, 20.0D, -0.2D, 0.0D);
            return Math.abs(oreFrequencyNoise) + d < (double)0.4F ? null : veinType;
        } else {
            return null;
        }
    }

    public static record FlatNoiseData(double shiftedX, double shiftedZ, double continentalness, double weirdness, double erosion, TerrainInfo terrainInfo) {
        public FlatNoiseData(double d, double e, double f, double g, double h, TerrainInfo terrainInfo) {
            this.shiftedX = d;
            this.shiftedZ = e;
            this.continentalness = f;
            this.weirdness = g;
            this.erosion = h;
            this.terrainInfo = terrainInfo;
        }

        public double shiftedX() {
            return this.shiftedX;
        }

        public double shiftedZ() {
            return this.shiftedZ;
        }

        public double continentalness() {
            return this.continentalness;
        }

        public double weirdness() {
            return this.weirdness;
        }

        public double erosion() {
            return this.erosion;
        }

        public TerrainInfo terrainInfo() {
            return this.terrainInfo;
        }
    }

    static final class QuantizedSpaghettiRarity {
        private QuantizedSpaghettiRarity() {
        }

        static double getSphaghettiRarity2D(double value) {
            if (value < -0.75D) {
                return 0.5D;
            } else if (value < -0.5D) {
                return 0.75D;
            } else if (value < 0.5D) {
                return 1.0D;
            } else {
                return value < 0.75D ? 2.0D : 3.0D;
            }
        }

        static double getSpaghettiRarity3D(double value) {
            if (value < -0.5D) {
                return 0.75D;
            } else if (value < 0.0D) {
                return 1.0D;
            } else {
                return value < 0.5D ? 1.5D : 2.0D;
            }
        }
    }

    static enum VeinType {
        COPPER(Blocks.COPPER_ORE.getBlockData(), Blocks.RAW_COPPER_BLOCK.getBlockData(), Blocks.GRANITE.getBlockData(), 0, 50),
        IRON(Blocks.DEEPSLATE_IRON_ORE.getBlockData(), Blocks.RAW_IRON_BLOCK.getBlockData(), Blocks.TUFF.getBlockData(), -60, -8);

        final IBlockData ore;
        final IBlockData rawOreBlock;
        final IBlockData filler;
        final int minY;
        final int maxY;

        private VeinType(IBlockData ore, IBlockData rawBlock, IBlockData stone, int minY, int maxY) {
            this.ore = ore;
            this.rawOreBlock = rawBlock;
            this.filler = stone;
            this.minY = minY;
            this.maxY = maxY;
        }
    }
}
