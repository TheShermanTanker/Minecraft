package net.minecraft.world.level.levelgen;

import java.util.Arrays;
import net.minecraft.core.BlockPosition;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.synth.NoiseGeneratorNormal;

public interface Aquifer {
    int ALWAYS_LAVA_AT_OR_BELOW_Y_INDEX = 9;
    int ALWAYS_USE_SEA_LEVEL_WHEN_ABOVE = 30;

    static Aquifer create(ChunkCoordIntPair pos, NoiseGeneratorNormal edgeDensityNoise, NoiseGeneratorNormal fluidLevelNoise, NoiseGeneratorNormal fluidTypeNoise, GeneratorSettingBase settings, NoiseSampler columnSampler, int startY, int deltaY) {
        return new Aquifer.NoiseBasedAquifer(pos, edgeDensityNoise, fluidLevelNoise, fluidTypeNoise, settings, columnSampler, startY, deltaY);
    }

    static Aquifer createDisabled(int seaLevel, IBlockData state) {
        return new Aquifer() {
            @Override
            public IBlockData computeState(BaseStoneSource source, int x, int y, int z, double weight) {
                if (weight > 0.0D) {
                    return source.getBaseBlock(x, y, z);
                } else {
                    return y >= seaLevel ? Blocks.AIR.getBlockData() : state;
                }
            }

            @Override
            public boolean shouldScheduleFluidUpdate() {
                return false;
            }
        };
    }

    IBlockData computeState(BaseStoneSource source, int x, int y, int z, double weight);

    boolean shouldScheduleFluidUpdate();

    public static class NoiseBasedAquifer implements Aquifer {
        private static final int X_RANGE = 10;
        private static final int Y_RANGE = 9;
        private static final int Z_RANGE = 10;
        private static final int X_SEPARATION = 6;
        private static final int Y_SEPARATION = 3;
        private static final int Z_SEPARATION = 6;
        private static final int X_SPACING = 16;
        private static final int Y_SPACING = 12;
        private static final int Z_SPACING = 16;
        private final NoiseGeneratorNormal barrierNoise;
        private final NoiseGeneratorNormal waterLevelNoise;
        private final NoiseGeneratorNormal lavaNoise;
        private final GeneratorSettingBase noiseGeneratorSettings;
        private final Aquifer.NoiseBasedAquifer.AquiferStatus[] aquiferCache;
        private final long[] aquiferLocationCache;
        private boolean shouldScheduleFluidUpdate;
        private final NoiseSampler sampler;
        private final int minGridX;
        private final int minGridY;
        private final int minGridZ;
        private final int gridSizeX;
        private final int gridSizeZ;

        NoiseBasedAquifer(ChunkCoordIntPair pos, NoiseGeneratorNormal edgeDensityNoise, NoiseGeneratorNormal fluidLevelNoise, NoiseGeneratorNormal fluidTypeNoise, GeneratorSettingBase settings, NoiseSampler columnSampler, int startY, int deltaY) {
            this.barrierNoise = edgeDensityNoise;
            this.waterLevelNoise = fluidLevelNoise;
            this.lavaNoise = fluidTypeNoise;
            this.noiseGeneratorSettings = settings;
            this.sampler = columnSampler;
            this.minGridX = this.gridX(pos.getMinBlockX()) - 1;
            int i = this.gridX(pos.getMaxBlockX()) + 1;
            this.gridSizeX = i - this.minGridX + 1;
            this.minGridY = this.gridY(startY) - 1;
            int j = this.gridY(startY + deltaY) + 1;
            int k = j - this.minGridY + 1;
            this.minGridZ = this.gridZ(pos.getMinBlockZ()) - 1;
            int l = this.gridZ(pos.getMaxBlockZ()) + 1;
            this.gridSizeZ = l - this.minGridZ + 1;
            int m = this.gridSizeX * k * this.gridSizeZ;
            this.aquiferCache = new Aquifer.NoiseBasedAquifer.AquiferStatus[m];
            this.aquiferLocationCache = new long[m];
            Arrays.fill(this.aquiferLocationCache, Long.MAX_VALUE);
        }

        private int getIndex(int x, int y, int z) {
            int i = x - this.minGridX;
            int j = y - this.minGridY;
            int k = z - this.minGridZ;
            return (j * this.gridSizeZ + k) * this.gridSizeX + i;
        }

        @Override
        public IBlockData computeState(BaseStoneSource source, int x, int y, int z, double weight) {
            if (weight <= 0.0D) {
                double d;
                IBlockData blockState;
                boolean bl;
                if (this.isLavaLevel(y)) {
                    blockState = Blocks.LAVA.getBlockData();
                    d = 0.0D;
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
                                    SeededRandom worldgenRandom = new SeededRandom(MathHelper.getSeed(u, v * 3, w) + 1L);
                                    ac = BlockPosition.asLong(u * 16 + worldgenRandom.nextInt(10), v * 12 + worldgenRandom.nextInt(9), w * 16 + worldgenRandom.nextInt(10));
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

                    Aquifer.NoiseBasedAquifer.AquiferStatus aquiferStatus = this.getAquiferStatus(o);
                    Aquifer.NoiseBasedAquifer.AquiferStatus aquiferStatus2 = this.getAquiferStatus(p);
                    Aquifer.NoiseBasedAquifer.AquiferStatus aquiferStatus3 = this.getAquiferStatus(q);
                    double e = this.similarity(l, m);
                    double f = this.similarity(l, n);
                    double g = this.similarity(m, n);
                    bl = e > 0.0D;
                    if (aquiferStatus.fluidLevel >= y && aquiferStatus.fluidType.is(Blocks.WATER) && this.isLavaLevel(y - 1)) {
                        d = 1.0D;
                    } else if (e > -1.0D) {
                        double ai = 1.0D + (this.barrierNoise.getValue((double)x, (double)y, (double)z) + 0.05D) / 4.0D;
                        double aj = this.calculatePressure(y, ai, aquiferStatus, aquiferStatus2);
                        double ak = this.calculatePressure(y, ai, aquiferStatus, aquiferStatus3);
                        double al = this.calculatePressure(y, ai, aquiferStatus2, aquiferStatus3);
                        double am = Math.max(0.0D, e);
                        double an = Math.max(0.0D, f);
                        double ao = Math.max(0.0D, g);
                        double ap = 2.0D * am * Math.max(aj, Math.max(ak * an, al * ao));
                        d = Math.max(0.0D, ap);
                    } else {
                        d = 0.0D;
                    }

                    blockState = y >= aquiferStatus.fluidLevel ? Blocks.AIR.getBlockData() : aquiferStatus.fluidType;
                }

                if (weight + d <= 0.0D) {
                    this.shouldScheduleFluidUpdate = bl;
                    return blockState;
                }
            }

            this.shouldScheduleFluidUpdate = false;
            return source.getBaseBlock(x, y, z);
        }

        @Override
        public boolean shouldScheduleFluidUpdate() {
            return this.shouldScheduleFluidUpdate;
        }

        private boolean isLavaLevel(int y) {
            return y - this.noiseGeneratorSettings.noiseSettings().minY() <= 9;
        }

        private double similarity(int a, int b) {
            double d = 25.0D;
            return 1.0D - (double)Math.abs(b - a) / 25.0D;
        }

        private double calculatePressure(int y, double noise, Aquifer.NoiseBasedAquifer.AquiferStatus first, Aquifer.NoiseBasedAquifer.AquiferStatus second) {
            if (y <= first.fluidLevel && y <= second.fluidLevel && first.fluidType != second.fluidType) {
                return 1.0D;
            } else {
                int i = Math.abs(first.fluidLevel - second.fluidLevel);
                double d = 0.5D * (double)(first.fluidLevel + second.fluidLevel);
                double e = Math.abs(d - (double)y - 0.5D);
                return 0.5D * (double)i * noise - e;
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

        private Aquifer.NoiseBasedAquifer.AquiferStatus getAquiferStatus(long pos) {
            int i = BlockPosition.getX(pos);
            int j = BlockPosition.getY(pos);
            int k = BlockPosition.getZ(pos);
            int l = this.gridX(i);
            int m = this.gridY(j);
            int n = this.gridZ(k);
            int o = this.getIndex(l, m, n);
            Aquifer.NoiseBasedAquifer.AquiferStatus aquiferStatus = this.aquiferCache[o];
            if (aquiferStatus != null) {
                return aquiferStatus;
            } else {
                Aquifer.NoiseBasedAquifer.AquiferStatus aquiferStatus2 = this.computeAquifer(i, j, k);
                this.aquiferCache[o] = aquiferStatus2;
                return aquiferStatus2;
            }
        }

        private Aquifer.NoiseBasedAquifer.AquiferStatus computeAquifer(int x, int y, int z) {
            int i = this.noiseGeneratorSettings.seaLevel();
            if (y > 30) {
                return new Aquifer.NoiseBasedAquifer.AquiferStatus(i, Blocks.WATER.getBlockData());
            } else {
                int j = 64;
                int k = -10;
                int l = 40;
                double d = this.waterLevelNoise.getValue((double)Math.floorDiv(x, 64), (double)Math.floorDiv(y, 40) / 1.4D, (double)Math.floorDiv(z, 64)) * 30.0D + -10.0D;
                boolean bl = false;
                if (Math.abs(d) > 8.0D) {
                    d *= 4.0D;
                }

                int m = Math.floorDiv(y, 40) * 40 + 20;
                int n = m + MathHelper.floor(d);
                if (m == -20) {
                    double e = this.lavaNoise.getValue((double)Math.floorDiv(x, 64), (double)Math.floorDiv(y, 40) / 1.4D, (double)Math.floorDiv(z, 64));
                    bl = Math.abs(e) > (double)0.22F;
                }

                return new Aquifer.NoiseBasedAquifer.AquiferStatus(Math.min(56, n), bl ? Blocks.LAVA.getBlockData() : Blocks.WATER.getBlockData());
            }
        }

        static final class AquiferStatus {
            final int fluidLevel;
            final IBlockData fluidType;

            public AquiferStatus(int y, IBlockData state) {
                this.fluidLevel = y;
                this.fluidType = state;
            }
        }
    }
}
