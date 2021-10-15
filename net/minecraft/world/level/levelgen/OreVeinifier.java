package net.minecraft.world.level.levelgen;

import java.util.Random;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.synth.NoiseGeneratorNormal;

public class OreVeinifier {
    private static final float RARITY = 1.0F;
    private static final float RIDGE_NOISE_FREQUENCY = 4.0F;
    private static final float THICKNESS = 0.08F;
    private static final float VEININESS_THRESHOLD = 0.5F;
    private static final double VEININESS_FREQUENCY = 1.5D;
    private static final int EDGE_ROUNDOFF_BEGIN = 20;
    private static final double MAX_EDGE_ROUNDOFF = 0.2D;
    private static final float VEIN_SOLIDNESS = 0.7F;
    private static final float MIN_RICHNESS = 0.1F;
    private static final float MAX_RICHNESS = 0.3F;
    private static final float MAX_RICHNESS_THRESHOLD = 0.6F;
    private static final float CHANCE_OF_RAW_ORE_BLOCK = 0.02F;
    private static final float SKIP_ORE_IF_GAP_NOISE_IS_BELOW = -0.3F;
    private final int veinMaxY;
    private final int veinMinY;
    private final IBlockData normalBlock;
    private final NoiseGeneratorNormal veininessNoiseSource;
    private final NoiseGeneratorNormal veinANoiseSource;
    private final NoiseGeneratorNormal veinBNoiseSource;
    private final NoiseGeneratorNormal gapNoise;
    private final int cellWidth;
    private final int cellHeight;

    public OreVeinifier(long seed, IBlockData defaultState, int horizontalNoiseResolution, int verticalNoiseResolution, int minY) {
        Random random = new Random(seed);
        this.normalBlock = defaultState;
        this.veininessNoiseSource = NoiseGeneratorNormal.create(new SimpleRandomSource(random.nextLong()), -8, 1.0D);
        this.veinANoiseSource = NoiseGeneratorNormal.create(new SimpleRandomSource(random.nextLong()), -7, 1.0D);
        this.veinBNoiseSource = NoiseGeneratorNormal.create(new SimpleRandomSource(random.nextLong()), -7, 1.0D);
        this.gapNoise = NoiseGeneratorNormal.create(new SimpleRandomSource(0L), -5, 1.0D);
        this.cellWidth = horizontalNoiseResolution;
        this.cellHeight = verticalNoiseResolution;
        this.veinMaxY = Stream.of(OreVeinifier.VeinType.values()).mapToInt((veinType) -> {
            return veinType.maxY;
        }).max().orElse(minY);
        this.veinMinY = Stream.of(OreVeinifier.VeinType.values()).mapToInt((veinType) -> {
            return veinType.minY;
        }).min().orElse(minY);
    }

    public void fillVeininessNoiseColumn(double[] buffer, int x, int z, int minY, int noiseSizeY) {
        this.fillNoiseColumn(buffer, x, z, this.veininessNoiseSource, 1.5D, minY, noiseSizeY);
    }

    public void fillNoiseColumnA(double[] buffer, int x, int z, int minY, int noiseSizeY) {
        this.fillNoiseColumn(buffer, x, z, this.veinANoiseSource, 4.0D, minY, noiseSizeY);
    }

    public void fillNoiseColumnB(double[] buffer, int x, int z, int minY, int noiseSizeY) {
        this.fillNoiseColumn(buffer, x, z, this.veinBNoiseSource, 4.0D, minY, noiseSizeY);
    }

    public void fillNoiseColumn(double[] buffer, int x, int z, NoiseGeneratorNormal sampler, double scale, int minY, int noiseSizeY) {
        for(int i = 0; i < noiseSizeY; ++i) {
            int j = i + minY;
            int k = x * this.cellWidth;
            int l = j * this.cellHeight;
            int m = z * this.cellWidth;
            double d;
            if (l >= this.veinMinY && l <= this.veinMaxY) {
                d = sampler.getValue((double)k * scale, (double)l * scale, (double)m * scale);
            } else {
                d = 0.0D;
            }

            buffer[i] = d;
        }

    }

    public IBlockData oreVeinify(RandomSource random, int x, int y, int z, double oreFrequencyNoise, double firstOrePlacementNoise, double secondOrePlacementNoise) {
        IBlockData blockState = this.normalBlock;
        OreVeinifier.VeinType veinType = this.getVeinType(oreFrequencyNoise, y);
        if (veinType == null) {
            return blockState;
        } else if (random.nextFloat() > 0.7F) {
            return blockState;
        } else if (this.isVein(firstOrePlacementNoise, secondOrePlacementNoise)) {
            double d = MathHelper.clampedMap(Math.abs(oreFrequencyNoise), 0.5D, (double)0.6F, (double)0.1F, (double)0.3F);
            if ((double)random.nextFloat() < d && this.gapNoise.getValue((double)x, (double)y, (double)z) > (double)-0.3F) {
                return random.nextFloat() < 0.02F ? veinType.rawOreBlock : veinType.ore;
            } else {
                return veinType.filler;
            }
        } else {
            return blockState;
        }
    }

    private boolean isVein(double firstOrePlacementNoise, double secondOrePlacementNoise) {
        double d = Math.abs(1.0D * firstOrePlacementNoise) - (double)0.08F;
        double e = Math.abs(1.0D * secondOrePlacementNoise) - (double)0.08F;
        return Math.max(d, e) < 0.0D;
    }

    @Nullable
    private OreVeinifier.VeinType getVeinType(double oreFrequencyNoise, int y) {
        OreVeinifier.VeinType veinType = oreFrequencyNoise > 0.0D ? OreVeinifier.VeinType.COPPER : OreVeinifier.VeinType.IRON;
        int i = veinType.maxY - y;
        int j = y - veinType.minY;
        if (j >= 0 && i >= 0) {
            int k = Math.min(i, j);
            double d = MathHelper.clampedMap((double)k, 0.0D, 20.0D, -0.2D, 0.0D);
            return Math.abs(oreFrequencyNoise) + d < 0.5D ? null : veinType;
        } else {
            return null;
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
