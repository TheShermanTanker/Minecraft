package net.minecraft.world.level.levelgen;

import java.util.Arrays;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.SectionPosition;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.dimension.DimensionManager;
import net.minecraft.world.level.levelgen.synth.NoiseGeneratorNormal;
import org.apache.commons.lang3.mutable.MutableDouble;

public interface Aquifer {
    static Aquifer create(NoiseChunk chunkNoiseSampler, ChunkCoordIntPair chunkPos, NoiseGeneratorNormal barrierNoise, NoiseGeneratorNormal fluidLevelFloodednessNoise, NoiseGeneratorNormal fluidLevelSpreadNoise, NoiseGeneratorNormal fluidTypeNoise, PositionalRandomFactory randomDeriver, int minY, int height, Aquifer.FluidPicker fluidLevelSampler) {
        return new Aquifer.NoiseBasedAquifer(chunkNoiseSampler, chunkPos, barrierNoise, fluidLevelFloodednessNoise, fluidLevelSpreadNoise, fluidTypeNoise, randomDeriver, minY, height, fluidLevelSampler);
    }

    static Aquifer createDisabled(Aquifer.FluidPicker fluidLevelSampler) {
        return new Aquifer() {
            @Nullable
            @Override
            public IBlockData computeSubstance(int x, int y, int z, double d, double e) {
                return e > 0.0D ? null : fluidLevelSampler.computeFluid(x, y, z).at(y);
            }

            @Override
            public boolean shouldScheduleFluidUpdate() {
                return false;
            }
        };
    }

    @Nullable
    IBlockData computeSubstance(int x, int y, int z, double d, double e);

    boolean shouldScheduleFluidUpdate();

    public interface FluidPicker {
        Aquifer.FluidStatus computeFluid(int x, int y, int z);
    }

    public static final class FluidStatus {
        final int fluidLevel;
        final IBlockData fluidType;

        public FluidStatus(int y, IBlockData state) {
            this.fluidLevel = y;
            this.fluidType = state;
        }

        public IBlockData at(int y) {
            return y < this.fluidLevel ? this.fluidType : Blocks.AIR.getBlockData();
        }
    }

    public static class NoiseBasedAquifer implements Aquifer, Aquifer.FluidPicker {
        private static final int X_RANGE = 10;
        private static final int Y_RANGE = 9;
        private static final int Z_RANGE = 10;
        private static final int X_SEPARATION = 6;
        private static final int Y_SEPARATION = 3;
        private static final int Z_SEPARATION = 6;
        private static final int X_SPACING = 16;
        private static final int Y_SPACING = 12;
        private static final int Z_SPACING = 16;
        private static final int MAX_REASONABLE_DISTANCE_TO_AQUIFER_CENTER = 11;
        private static final double FLOWING_UPDATE_SIMULARITY = similarity(MathHelper.square(10), MathHelper.square(12));
        private final NoiseChunk noiseChunk;
        private final NoiseGeneratorNormal barrierNoise;
        private final NoiseGeneratorNormal fluidLevelFloodednessNoise;
        private final NoiseGeneratorNormal fluidLevelSpreadNoise;
        private final NoiseGeneratorNormal lavaNoise;
        private final PositionalRandomFactory positionalRandomFactory;
        private final Aquifer.FluidStatus[] aquiferCache;
        private final long[] aquiferLocationCache;
        private final Aquifer.FluidPicker globalFluidPicker;
        private boolean shouldScheduleFluidUpdate;
        private final int minGridX;
        private final int minGridY;
        private final int minGridZ;
        private final int gridSizeX;
        private final int gridSizeZ;
        private static final int[][] SURFACE_SAMPLING_OFFSETS_IN_CHUNKS = new int[][]{{-2, -1}, {-1, -1}, {0, -1}, {1, -1}, {-3, 0}, {-2, 0}, {-1, 0}, {0, 0}, {1, 0}, {-2, 1}, {-1, 1}, {0, 1}, {1, 1}};

        NoiseBasedAquifer(NoiseChunk chunkNoiseSampler, ChunkCoordIntPair chunkPos, NoiseGeneratorNormal barrierNoise, NoiseGeneratorNormal fluidLevelFloodednessNoise, NoiseGeneratorNormal fluidLevelSpreadNoise, NoiseGeneratorNormal fluidTypeNoise, PositionalRandomFactory randomDeriver, int minY, int height, Aquifer.FluidPicker fluidLevelSampler) {
            this.noiseChunk = chunkNoiseSampler;
            this.barrierNoise = barrierNoise;
            this.fluidLevelFloodednessNoise = fluidLevelFloodednessNoise;
            this.fluidLevelSpreadNoise = fluidLevelSpreadNoise;
            this.lavaNoise = fluidTypeNoise;
            this.positionalRandomFactory = randomDeriver;
            this.minGridX = this.gridX(chunkPos.getMinBlockX()) - 1;
            this.globalFluidPicker = fluidLevelSampler;
            int i = this.gridX(chunkPos.getMaxBlockX()) + 1;
            this.gridSizeX = i - this.minGridX + 1;
            this.minGridY = this.gridY(minY) - 1;
            int j = this.gridY(minY + height) + 1;
            int k = j - this.minGridY + 1;
            this.minGridZ = this.gridZ(chunkPos.getMinBlockZ()) - 1;
            int l = this.gridZ(chunkPos.getMaxBlockZ()) + 1;
            this.gridSizeZ = l - this.minGridZ + 1;
            int m = this.gridSizeX * k * this.gridSizeZ;
            this.aquiferCache = new Aquifer.FluidStatus[m];
            this.aquiferLocationCache = new long[m];
            Arrays.fill(this.aquiferLocationCache, Long.MAX_VALUE);
        }

        private int getIndex(int x, int y, int z) {
            int i = x - this.minGridX;
            int j = y - this.minGridY;
            int k = z - this.minGridZ;
            return (j * this.gridSizeZ + k) * this.gridSizeX + i;
        }

        @Nullable
        @Override
        public IBlockData computeSubstance(int x, int y, int z, double d, double e) {
            if (d <= -64.0D) {
                return this.globalFluidPicker.computeFluid(x, y, z).at(y);
            } else {
                if (e <= 0.0D) {
                    Aquifer.FluidStatus fluidStatus = this.globalFluidPicker.computeFluid(x, y, z);
                    double f;
                    IBlockData blockState;
                    boolean bl;
                    if (fluidStatus.at(y).is(Blocks.LAVA)) {
                        blockState = Blocks.LAVA.getBlockData();
                        f = 0.0D;
                        bl = false;
                    } else {
                        int i = Math.floorDiv(x - 5, 16);
                        int j = Math.floorDiv(y + 1, 12);
                        int k = Math.floorDiv(z - 5, 16);
                        int l = Integer.MAX_VALUE;
                        int m = Integer.MAX_VALUE;
                        int n = Integer.MAX_VALUE;
                        long o = 0L;
                        long p = 0L;
                        long q = 0L;

                        for(int r = 0; r <= 1; ++r) {
                            for(int s = -1; s <= 1; ++s) {
                                for(int t = 0; t <= 1; ++t) {
                                    int u = i + r;
                                    int v = j + s;
                                    int w = k + t;
                                    int aa = this.getIndex(u, v, w);
                                    long ab = this.aquiferLocationCache[aa];
                                    long ac;
                                    if (ab != Long.MAX_VALUE) {
                                        ac = ab;
                                    } else {
                                        RandomSource randomSource = this.positionalRandomFactory.at(u, v, w);
                                        ac = BlockPosition.asLong(u * 16 + randomSource.nextInt(10), v * 12 + randomSource.nextInt(9), w * 16 + randomSource.nextInt(10));
                                        this.aquiferLocationCache[aa] = ac;
                                    }

                                    int ae = BlockPosition.getX(ac) - x;
                                    int af = BlockPosition.getY(ac) - y;
                                    int ag = BlockPosition.getZ(ac) - z;
                                    int ah = ae * ae + af * af + ag * ag;
                                    if (l >= ah) {
                                        q = p;
                                        p = o;
                                        o = ac;
                                        n = m;
                                        m = l;
                                        l = ah;
                                    } else if (m >= ah) {
                                        q = p;
                                        p = ac;
                                        n = m;
                                        m = ah;
                                    } else if (n >= ah) {
                                        q = ac;
                                        n = ah;
                                    }
                                }
                            }
                        }

                        Aquifer.FluidStatus fluidStatus2 = this.getAquiferStatus(o);
                        Aquifer.FluidStatus fluidStatus3 = this.getAquiferStatus(p);
                        Aquifer.FluidStatus fluidStatus4 = this.getAquiferStatus(q);
                        double g = similarity(l, m);
                        double h = similarity(l, n);
                        double ai = similarity(m, n);
                        bl = g >= FLOWING_UPDATE_SIMULARITY;
                        if (fluidStatus2.at(y).is(Blocks.WATER) && this.globalFluidPicker.computeFluid(x, y - 1, z).at(y - 1).is(Blocks.LAVA)) {
                            f = 1.0D;
                        } else if (g > -1.0D) {
                            MutableDouble mutableDouble = new MutableDouble(Double.NaN);
                            double ak = this.calculatePressure(x, y, z, mutableDouble, fluidStatus2, fluidStatus3);
                            double al = this.calculatePressure(x, y, z, mutableDouble, fluidStatus2, fluidStatus4);
                            double am = this.calculatePressure(x, y, z, mutableDouble, fluidStatus3, fluidStatus4);
                            double an = Math.max(0.0D, g);
                            double ao = Math.max(0.0D, h);
                            double ap = Math.max(0.0D, ai);
                            double aq = 2.0D * an * Math.max(ak, Math.max(al * ao, am * ap));
                            f = Math.max(0.0D, aq);
                        } else {
                            f = 0.0D;
                        }

                        blockState = fluidStatus2.at(y);
                    }

                    if (e + f <= 0.0D) {
                        this.shouldScheduleFluidUpdate = bl;
                        return blockState;
                    }
                }

                this.shouldScheduleFluidUpdate = false;
                return null;
            }
        }

        @Override
        public boolean shouldScheduleFluidUpdate() {
            return this.shouldScheduleFluidUpdate;
        }

        private static double similarity(int i, int a) {
            double d = 25.0D;
            return 1.0D - (double)Math.abs(a - i) / 25.0D;
        }

        private double calculatePressure(int i, int j, int k, MutableDouble mutableDouble, Aquifer.FluidStatus fluidStatus, Aquifer.FluidStatus fluidStatus2) {
            IBlockData blockState = fluidStatus.at(j);
            IBlockData blockState2 = fluidStatus2.at(j);
            if ((!blockState.is(Blocks.LAVA) || !blockState2.is(Blocks.WATER)) && (!blockState.is(Blocks.WATER) || !blockState2.is(Blocks.LAVA))) {
                int l = Math.abs(fluidStatus.fluidLevel - fluidStatus2.fluidLevel);
                if (l == 0) {
                    return 0.0D;
                } else {
                    double d = 0.5D * (double)(fluidStatus.fluidLevel + fluidStatus2.fluidLevel);
                    double e = (double)j + 0.5D - d;
                    double f = (double)l / 2.0D;
                    double g = 0.0D;
                    double h = 2.5D;
                    double m = 1.5D;
                    double n = 3.0D;
                    double o = 10.0D;
                    double p = 3.0D;
                    double q = f - Math.abs(e);
                    double s;
                    if (e > 0.0D) {
                        double r = 0.0D + q;
                        if (r > 0.0D) {
                            s = r / 1.5D;
                        } else {
                            s = r / 2.5D;
                        }
                    } else {
                        double u = 3.0D + q;
                        if (u > 0.0D) {
                            s = u / 3.0D;
                        } else {
                            s = u / 10.0D;
                        }
                    }

                    if (!(s < -2.0D) && !(s > 2.0D)) {
                        double x = mutableDouble.getValue();
                        if (Double.isNaN(x)) {
                            double y = 0.5D;
                            double z = this.barrierNoise.getValue((double)i, (double)j * 0.5D, (double)k);
                            mutableDouble.setValue(z);
                            return z + s;
                        } else {
                            return x + s;
                        }
                    } else {
                        return s;
                    }
                }
            } else {
                return 1.0D;
            }
        }

        private int gridX(int x) {
            return Math.floorDiv(x, 16);
        }

        private int gridY(int y) {
            return Math.floorDiv(y, 12);
        }

        private int gridZ(int z) {
            return Math.floorDiv(z, 16);
        }

        private Aquifer.FluidStatus getAquiferStatus(long pos) {
            int i = BlockPosition.getX(pos);
            int j = BlockPosition.getY(pos);
            int k = BlockPosition.getZ(pos);
            int l = this.gridX(i);
            int m = this.gridY(j);
            int n = this.gridZ(k);
            int o = this.getIndex(l, m, n);
            Aquifer.FluidStatus fluidStatus = this.aquiferCache[o];
            if (fluidStatus != null) {
                return fluidStatus;
            } else {
                Aquifer.FluidStatus fluidStatus2 = this.computeFluid(i, j, k);
                this.aquiferCache[o] = fluidStatus2;
                return fluidStatus2;
            }
        }

        @Override
        public Aquifer.FluidStatus computeFluid(int x, int y, int z) {
            Aquifer.FluidStatus fluidStatus = this.globalFluidPicker.computeFluid(x, y, z);
            int i = Integer.MAX_VALUE;
            int j = y + 12;
            int k = y - 12;
            boolean bl = false;

            for(int[] is : SURFACE_SAMPLING_OFFSETS_IN_CHUNKS) {
                int l = x + SectionPosition.sectionToBlockCoord(is[0]);
                int m = z + SectionPosition.sectionToBlockCoord(is[1]);
                int n = this.noiseChunk.preliminarySurfaceLevel(l, m);
                int o = n + 8;
                boolean bl2 = is[0] == 0 && is[1] == 0;
                if (bl2 && k > o) {
                    return fluidStatus;
                }

                boolean bl3 = j > o;
                if (bl3 || bl2) {
                    Aquifer.FluidStatus fluidStatus2 = this.globalFluidPicker.computeFluid(l, o, m);
                    if (!fluidStatus2.at(o).isAir()) {
                        if (bl2) {
                            bl = true;
                        }

                        if (bl3) {
                            return fluidStatus2;
                        }
                    }
                }

                i = Math.min(i, n);
            }

            int p = i + 8 - y;
            int q = 64;
            double d = bl ? MathHelper.clampedMap((double)p, 0.0D, 64.0D, 1.0D, 0.0D) : 0.0D;
            double e = 0.67D;
            double f = MathHelper.clamp(this.fluidLevelFloodednessNoise.getValue((double)x, (double)y * 0.67D, (double)z), -1.0D, 1.0D);
            double g = MathHelper.map(d, 1.0D, 0.0D, -0.3D, 0.8D);
            if (f > g) {
                return fluidStatus;
            } else {
                double h = MathHelper.map(d, 1.0D, 0.0D, -0.8D, 0.4D);
                if (f <= h) {
                    return new Aquifer.FluidStatus(DimensionManager.WAY_BELOW_MIN_Y, fluidStatus.fluidType);
                } else {
                    int r = 16;
                    int s = 40;
                    int t = Math.floorDiv(x, 16);
                    int u = Math.floorDiv(y, 40);
                    int v = Math.floorDiv(z, 16);
                    int w = u * 40 + 20;
                    int aa = 10;
                    double ab = this.fluidLevelSpreadNoise.getValue((double)t, (double)u / 1.4D, (double)v) * 10.0D;
                    int ac = MathHelper.quantize(ab, 3);
                    int ad = w + ac;
                    int ae = Math.min(i, ad);
                    IBlockData blockState = this.getFluidType(x, y, z, fluidStatus, ad);
                    return new Aquifer.FluidStatus(ae, blockState);
                }
            }
        }

        private IBlockData getFluidType(int i, int j, int k, Aquifer.FluidStatus fluidStatus, int l) {
            if (l <= -10) {
                int m = 64;
                int n = 40;
                int o = Math.floorDiv(i, 64);
                int p = Math.floorDiv(j, 40);
                int q = Math.floorDiv(k, 64);
                double d = this.lavaNoise.getValue((double)o, (double)p, (double)q);
                if (Math.abs(d) > 0.3D) {
                    return Blocks.LAVA.getBlockData();
                }
            }

            return fluidStatus.fluidType;
        }
    }
}
