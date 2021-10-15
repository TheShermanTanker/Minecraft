package net.minecraft.world.level.levelgen.carver;

import com.mojang.serialization.Codec;
import java.util.BitSet;
import java.util.Random;
import java.util.function.Function;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.SectionPosition;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.chunk.IChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;

public class WorldGenCaves extends WorldGenCarverAbstract<CaveCarverConfiguration> {
    public WorldGenCaves(Codec<CaveCarverConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean isStartChunk(CaveCarverConfiguration config, Random random) {
        return random.nextFloat() <= config.probability;
    }

    @Override
    public boolean carve(CarvingContext context, CaveCarverConfiguration config, IChunkAccess chunk, Function<BlockPosition, BiomeBase> posToBiome, Random random, Aquifer aquifer, ChunkCoordIntPair pos, BitSet carvingMask) {
        int i = SectionPosition.sectionToBlockCoord(this.getRange() * 2 - 1);
        int j = random.nextInt(random.nextInt(random.nextInt(this.getCaveBound()) + 1) + 1);

        for(int k = 0; k < j; ++k) {
            double d = (double)pos.getBlockX(random.nextInt(16));
            double e = (double)config.y.sample(random, context);
            double f = (double)pos.getBlockZ(random.nextInt(16));
            double g = (double)config.horizontalRadiusMultiplier.sample(random);
            double h = (double)config.verticalRadiusMultiplier.sample(random);
            double l = (double)config.floorLevel.sample(random);
            WorldGenCarverAbstract.CarveSkipChecker carveSkipChecker = (contextx, scaledRelativeX, scaledRelativeY, scaledRelativeZ, y) -> {
                return shouldSkip(scaledRelativeX, scaledRelativeY, scaledRelativeZ, l);
            };
            int m = 1;
            if (random.nextInt(4) == 0) {
                double n = (double)config.yScale.sample(random);
                float o = 1.0F + random.nextFloat() * 6.0F;
                this.createRoom(context, config, chunk, posToBiome, random.nextLong(), aquifer, d, e, f, o, n, carvingMask, carveSkipChecker);
                m += random.nextInt(4);
            }

            for(int p = 0; p < m; ++p) {
                float q = random.nextFloat() * ((float)Math.PI * 2F);
                float r = (random.nextFloat() - 0.5F) / 4.0F;
                float s = this.getThickness(random);
                int t = i - random.nextInt(i / 4);
                int u = 0;
                this.createTunnel(context, config, chunk, posToBiome, random.nextLong(), aquifer, d, e, f, g, h, s, q, r, 0, t, this.getYScale(), carvingMask, carveSkipChecker);
            }
        }

        return true;
    }

    protected int getCaveBound() {
        return 15;
    }

    protected float getThickness(Random random) {
        float f = random.nextFloat() * 2.0F + random.nextFloat();
        if (random.nextInt(10) == 0) {
            f *= random.nextFloat() * random.nextFloat() * 3.0F + 1.0F;
        }

        return f;
    }

    protected double getYScale() {
        return 1.0D;
    }

    protected void createRoom(CarvingContext context, CaveCarverConfiguration config, IChunkAccess chunk, Function<BlockPosition, BiomeBase> posToBiome, long seed, Aquifer aquifer, double x, double y, double z, float yaw, double yawPitchRatio, BitSet carvingMask, WorldGenCarverAbstract.CarveSkipChecker skipPredicate) {
        double d = 1.5D + (double)(MathHelper.sin(((float)Math.PI / 2F)) * yaw);
        double e = d * yawPitchRatio;
        this.carveEllipsoid(context, config, chunk, posToBiome, seed, aquifer, x + 1.0D, y, z, d, e, carvingMask, skipPredicate);
    }

    protected void createTunnel(CarvingContext context, CaveCarverConfiguration config, IChunkAccess chunk, Function<BlockPosition, BiomeBase> posToBiome, long seed, Aquifer aquifer, double x, double y, double z, double horizontalScale, double verticalScale, float width, float yaw, float pitch, int branchStartIndex, int branchCount, double yawPitchRatio, BitSet carvingMask, WorldGenCarverAbstract.CarveSkipChecker skipPredicate) {
        Random random = new Random(seed);
        int i = random.nextInt(branchCount / 2) + branchCount / 4;
        boolean bl = random.nextInt(6) == 0;
        float f = 0.0F;
        float g = 0.0F;

        for(int j = branchStartIndex; j < branchCount; ++j) {
            double d = 1.5D + (double)(MathHelper.sin((float)Math.PI * (float)j / (float)branchCount) * width);
            double e = d * yawPitchRatio;
            float h = MathHelper.cos(pitch);
            x += (double)(MathHelper.cos(yaw) * h);
            y += (double)MathHelper.sin(pitch);
            z += (double)(MathHelper.sin(yaw) * h);
            pitch = pitch * (bl ? 0.92F : 0.7F);
            pitch = pitch + g * 0.1F;
            yaw += f * 0.1F;
            g = g * 0.9F;
            f = f * 0.75F;
            g = g + (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 2.0F;
            f = f + (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 4.0F;
            if (j == i && width > 1.0F) {
                this.createTunnel(context, config, chunk, posToBiome, random.nextLong(), aquifer, x, y, z, horizontalScale, verticalScale, random.nextFloat() * 0.5F + 0.5F, yaw - ((float)Math.PI / 2F), pitch / 3.0F, j, branchCount, 1.0D, carvingMask, skipPredicate);
                this.createTunnel(context, config, chunk, posToBiome, random.nextLong(), aquifer, x, y, z, horizontalScale, verticalScale, random.nextFloat() * 0.5F + 0.5F, yaw + ((float)Math.PI / 2F), pitch / 3.0F, j, branchCount, 1.0D, carvingMask, skipPredicate);
                return;
            }

            if (random.nextInt(4) != 0) {
                if (!canReach(chunk.getPos(), x, z, j, branchCount, width)) {
                    return;
                }

                this.carveEllipsoid(context, config, chunk, posToBiome, seed, aquifer, x, y, z, d * horizontalScale, e * verticalScale, carvingMask, skipPredicate);
            }
        }

    }

    private static boolean shouldSkip(double scaledRelativeX, double scaledRelativeY, double scaledRelativeZ, double floorY) {
        if (scaledRelativeY <= floorY) {
            return true;
        } else {
            return scaledRelativeX * scaledRelativeX + scaledRelativeY * scaledRelativeY + scaledRelativeZ * scaledRelativeZ >= 1.0D;
        }
    }
}
