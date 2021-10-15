package net.minecraft.world.level.levelgen.feature;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.SectionPosition;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.boss.enderdragon.EntityEnderCrystal;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.WorldAccess;
import net.minecraft.world.level.block.BlockIronBars;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.dimension.DimensionManager;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureEndSpikeConfiguration;
import net.minecraft.world.phys.AxisAlignedBB;

public class WorldGenEnder extends WorldGenerator<WorldGenFeatureEndSpikeConfiguration> {
    public static final int NUMBER_OF_SPIKES = 10;
    private static final int SPIKE_DISTANCE = 42;
    private static final LoadingCache<Long, List<WorldGenEnder.Spike>> SPIKE_CACHE = CacheBuilder.newBuilder().expireAfterWrite(5L, TimeUnit.MINUTES).build(new WorldGenEnder.SpikeCacheLoader());

    public WorldGenEnder(Codec<WorldGenFeatureEndSpikeConfiguration> configCodec) {
        super(configCodec);
    }

    public static List<WorldGenEnder.Spike> getSpikesForLevel(GeneratorAccessSeed world) {
        Random random = new Random(world.getSeed());
        long l = random.nextLong() & 65535L;
        return SPIKE_CACHE.getUnchecked(l);
    }

    @Override
    public boolean generate(FeaturePlaceContext<WorldGenFeatureEndSpikeConfiguration> context) {
        WorldGenFeatureEndSpikeConfiguration spikeConfiguration = context.config();
        GeneratorAccessSeed worldGenLevel = context.level();
        Random random = context.random();
        BlockPosition blockPos = context.origin();
        List<WorldGenEnder.Spike> list = spikeConfiguration.getSpikes();
        if (list.isEmpty()) {
            list = getSpikesForLevel(worldGenLevel);
        }

        for(WorldGenEnder.Spike endSpike : list) {
            if (endSpike.isCenterWithinChunk(blockPos)) {
                this.placeSpike(worldGenLevel, random, spikeConfiguration, endSpike);
            }
        }

        return true;
    }

    private void placeSpike(WorldAccess world, Random random, WorldGenFeatureEndSpikeConfiguration config, WorldGenEnder.Spike spike) {
        int i = spike.getRadius();

        for(BlockPosition blockPos : BlockPosition.betweenClosed(new BlockPosition(spike.getCenterX() - i, world.getMinBuildHeight(), spike.getCenterZ() - i), new BlockPosition(spike.getCenterX() + i, spike.getHeight() + 10, spike.getCenterZ() + i))) {
            if (blockPos.distanceSquared((double)spike.getCenterX(), (double)blockPos.getY(), (double)spike.getCenterZ(), false) <= (double)(i * i + 1) && blockPos.getY() < spike.getHeight()) {
                this.setBlock(world, blockPos, Blocks.OBSIDIAN.getBlockData());
            } else if (blockPos.getY() > 65) {
                this.setBlock(world, blockPos, Blocks.AIR.getBlockData());
            }
        }

        if (spike.isGuarded()) {
            int j = -2;
            int k = 2;
            int l = 3;
            BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

            for(int m = -2; m <= 2; ++m) {
                for(int n = -2; n <= 2; ++n) {
                    for(int o = 0; o <= 3; ++o) {
                        boolean bl = MathHelper.abs(m) == 2;
                        boolean bl2 = MathHelper.abs(n) == 2;
                        boolean bl3 = o == 3;
                        if (bl || bl2 || bl3) {
                            boolean bl4 = m == -2 || m == 2 || bl3;
                            boolean bl5 = n == -2 || n == 2 || bl3;
                            IBlockData blockState = Blocks.IRON_BARS.getBlockData().set(BlockIronBars.NORTH, Boolean.valueOf(bl4 && n != -2)).set(BlockIronBars.SOUTH, Boolean.valueOf(bl4 && n != 2)).set(BlockIronBars.WEST, Boolean.valueOf(bl5 && m != -2)).set(BlockIronBars.EAST, Boolean.valueOf(bl5 && m != 2));
                            this.setBlock(world, mutableBlockPos.set(spike.getCenterX() + m, spike.getHeight() + o, spike.getCenterZ() + n), blockState);
                        }
                    }
                }
            }
        }

        EntityEnderCrystal endCrystal = EntityTypes.END_CRYSTAL.create(world.getLevel());
        endCrystal.setBeamTarget(config.getCrystalBeamTarget());
        endCrystal.setInvulnerable(config.isCrystalInvulnerable());
        endCrystal.setPositionRotation((double)spike.getCenterX() + 0.5D, (double)(spike.getHeight() + 1), (double)spike.getCenterZ() + 0.5D, random.nextFloat() * 360.0F, 0.0F);
        world.addEntity(endCrystal);
        this.setBlock(world, new BlockPosition(spike.getCenterX(), spike.getHeight(), spike.getCenterZ()), Blocks.BEDROCK.getBlockData());
    }

    public static class Spike {
        public static final Codec<WorldGenEnder.Spike> CODEC = RecordCodecBuilder.create((instance) -> {
            return instance.group(Codec.INT.fieldOf("centerX").orElse(0).forGetter((endSpike) -> {
                return endSpike.centerX;
            }), Codec.INT.fieldOf("centerZ").orElse(0).forGetter((endSpike) -> {
                return endSpike.centerZ;
            }), Codec.INT.fieldOf("radius").orElse(0).forGetter((endSpike) -> {
                return endSpike.radius;
            }), Codec.INT.fieldOf("height").orElse(0).forGetter((endSpike) -> {
                return endSpike.height;
            }), Codec.BOOL.fieldOf("guarded").orElse(false).forGetter((endSpike) -> {
                return endSpike.guarded;
            })).apply(instance, WorldGenEnder.Spike::new);
        });
        private final int centerX;
        private final int centerZ;
        private final int radius;
        private final int height;
        private final boolean guarded;
        private final AxisAlignedBB topBoundingBox;

        public Spike(int centerX, int centerZ, int radius, int height, boolean guarded) {
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.radius = radius;
            this.height = height;
            this.guarded = guarded;
            this.topBoundingBox = new AxisAlignedBB((double)(centerX - radius), (double)DimensionManager.MIN_Y, (double)(centerZ - radius), (double)(centerX + radius), (double)DimensionManager.MAX_Y, (double)(centerZ + radius));
        }

        public boolean isCenterWithinChunk(BlockPosition pos) {
            return SectionPosition.blockToSectionCoord(pos.getX()) == SectionPosition.blockToSectionCoord(this.centerX) && SectionPosition.blockToSectionCoord(pos.getZ()) == SectionPosition.blockToSectionCoord(this.centerZ);
        }

        public int getCenterX() {
            return this.centerX;
        }

        public int getCenterZ() {
            return this.centerZ;
        }

        public int getRadius() {
            return this.radius;
        }

        public int getHeight() {
            return this.height;
        }

        public boolean isGuarded() {
            return this.guarded;
        }

        public AxisAlignedBB getTopBoundingBox() {
            return this.topBoundingBox;
        }
    }

    static class SpikeCacheLoader extends CacheLoader<Long, List<WorldGenEnder.Spike>> {
        @Override
        public List<WorldGenEnder.Spike> load(Long long_) {
            List<Integer> list = IntStream.range(0, 10).boxed().collect(Collectors.toList());
            Collections.shuffle(list, new Random(long_));
            List<WorldGenEnder.Spike> list2 = Lists.newArrayList();

            for(int i = 0; i < 10; ++i) {
                int j = MathHelper.floor(42.0D * Math.cos(2.0D * (-Math.PI + (Math.PI / 10D) * (double)i)));
                int k = MathHelper.floor(42.0D * Math.sin(2.0D * (-Math.PI + (Math.PI / 10D) * (double)i)));
                int l = list.get(i);
                int m = 2 + l / 3;
                int n = 76 + l * 3;
                boolean bl = l == 1 || l == 2;
                list2.add(new WorldGenEnder.Spike(j, k, m, n, bl));
            }

            return list2;
        }
    }
}
