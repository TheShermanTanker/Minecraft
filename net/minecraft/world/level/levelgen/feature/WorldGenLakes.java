package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.SectionPosition;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.level.EnumSkyBlock;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.BaseStoneSource;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureLakeConfiguration;
import net.minecraft.world.level.material.Material;

public class WorldGenLakes extends WorldGenerator<WorldGenFeatureLakeConfiguration> {
    private static final IBlockData AIR = Blocks.CAVE_AIR.getBlockData();

    public WorldGenLakes(Codec<WorldGenFeatureLakeConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeaturePlaceContext<WorldGenFeatureLakeConfiguration> context) {
        BlockPosition blockPos = context.origin();
        GeneratorAccessSeed worldGenLevel = context.level();
        Random random = context.random();

        WorldGenFeatureLakeConfiguration blockStateConfiguration;
        for(blockStateConfiguration = context.config(); blockPos.getY() > worldGenLevel.getMinBuildHeight() + 5 && worldGenLevel.isEmpty(blockPos); blockPos = blockPos.below()) {
        }

        if (blockPos.getY() <= worldGenLevel.getMinBuildHeight() + 4) {
            return false;
        } else {
            blockPos = blockPos.below(4);
            if (worldGenLevel.startsForFeature(SectionPosition.of(blockPos), StructureGenerator.VILLAGE).findAny().isPresent()) {
                return false;
            } else {
                boolean[] bls = new boolean[2048];
                int i = random.nextInt(4) + 4;

                for(int j = 0; j < i; ++j) {
                    double d = random.nextDouble() * 6.0D + 3.0D;
                    double e = random.nextDouble() * 4.0D + 2.0D;
                    double f = random.nextDouble() * 6.0D + 3.0D;
                    double g = random.nextDouble() * (16.0D - d - 2.0D) + 1.0D + d / 2.0D;
                    double h = random.nextDouble() * (8.0D - e - 4.0D) + 2.0D + e / 2.0D;
                    double k = random.nextDouble() * (16.0D - f - 2.0D) + 1.0D + f / 2.0D;

                    for(int l = 1; l < 15; ++l) {
                        for(int m = 1; m < 15; ++m) {
                            for(int n = 1; n < 7; ++n) {
                                double o = ((double)l - g) / (d / 2.0D);
                                double p = ((double)n - h) / (e / 2.0D);
                                double q = ((double)m - k) / (f / 2.0D);
                                double r = o * o + p * p + q * q;
                                if (r < 1.0D) {
                                    bls[(l * 16 + m) * 8 + n] = true;
                                }
                            }
                        }
                    }
                }

                for(int s = 0; s < 16; ++s) {
                    for(int t = 0; t < 16; ++t) {
                        for(int u = 0; u < 8; ++u) {
                            boolean bl = !bls[(s * 16 + t) * 8 + u] && (s < 15 && bls[((s + 1) * 16 + t) * 8 + u] || s > 0 && bls[((s - 1) * 16 + t) * 8 + u] || t < 15 && bls[(s * 16 + t + 1) * 8 + u] || t > 0 && bls[(s * 16 + (t - 1)) * 8 + u] || u < 7 && bls[(s * 16 + t) * 8 + u + 1] || u > 0 && bls[(s * 16 + t) * 8 + (u - 1)]);
                            if (bl) {
                                Material material = worldGenLevel.getType(blockPos.offset(s, u, t)).getMaterial();
                                if (u >= 4 && material.isLiquid()) {
                                    return false;
                                }

                                if (u < 4 && !material.isBuildable() && worldGenLevel.getType(blockPos.offset(s, u, t)) != blockStateConfiguration.state) {
                                    return false;
                                }
                            }
                        }
                    }
                }

                for(int v = 0; v < 16; ++v) {
                    for(int w = 0; w < 16; ++w) {
                        for(int x = 0; x < 8; ++x) {
                            if (bls[(v * 16 + w) * 8 + x]) {
                                BlockPosition blockPos2 = blockPos.offset(v, x, w);
                                boolean bl2 = x >= 4;
                                worldGenLevel.setTypeAndData(blockPos2, bl2 ? AIR : blockStateConfiguration.state, 2);
                                if (bl2) {
                                    worldGenLevel.getBlockTickList().scheduleTick(blockPos2, AIR.getBlock(), 0);
                                    this.markAboveForPostProcessing(worldGenLevel, blockPos2);
                                }
                            }
                        }
                    }
                }

                for(int y = 0; y < 16; ++y) {
                    for(int z = 0; z < 16; ++z) {
                        for(int aa = 4; aa < 8; ++aa) {
                            if (bls[(y * 16 + z) * 8 + aa]) {
                                BlockPosition blockPos3 = blockPos.offset(y, aa - 1, z);
                                if (isDirt(worldGenLevel.getType(blockPos3)) && worldGenLevel.getBrightness(EnumSkyBlock.SKY, blockPos.offset(y, aa, z)) > 0) {
                                    BiomeBase biome = worldGenLevel.getBiome(blockPos3);
                                    if (biome.getGenerationSettings().getSurfaceBuilderConfig().getTopMaterial().is(Blocks.MYCELIUM)) {
                                        worldGenLevel.setTypeAndData(blockPos3, Blocks.MYCELIUM.getBlockData(), 2);
                                    } else {
                                        worldGenLevel.setTypeAndData(blockPos3, Blocks.GRASS_BLOCK.getBlockData(), 2);
                                    }
                                }
                            }
                        }
                    }
                }

                if (blockStateConfiguration.state.getMaterial() == Material.LAVA) {
                    BaseStoneSource baseStoneSource = context.chunkGenerator().getBaseStoneSource();

                    for(int ab = 0; ab < 16; ++ab) {
                        for(int ac = 0; ac < 16; ++ac) {
                            for(int ad = 0; ad < 8; ++ad) {
                                boolean bl3 = !bls[(ab * 16 + ac) * 8 + ad] && (ab < 15 && bls[((ab + 1) * 16 + ac) * 8 + ad] || ab > 0 && bls[((ab - 1) * 16 + ac) * 8 + ad] || ac < 15 && bls[(ab * 16 + ac + 1) * 8 + ad] || ac > 0 && bls[(ab * 16 + (ac - 1)) * 8 + ad] || ad < 7 && bls[(ab * 16 + ac) * 8 + ad + 1] || ad > 0 && bls[(ab * 16 + ac) * 8 + (ad - 1)]);
                                if (bl3 && (ad < 4 || random.nextInt(2) != 0)) {
                                    IBlockData blockState = worldGenLevel.getType(blockPos.offset(ab, ad, ac));
                                    if (blockState.getMaterial().isBuildable() && !blockState.is(TagsBlock.LAVA_POOL_STONE_CANNOT_REPLACE)) {
                                        BlockPosition blockPos4 = blockPos.offset(ab, ad, ac);
                                        worldGenLevel.setTypeAndData(blockPos4, baseStoneSource.getBaseBlock(blockPos4), 2);
                                        this.markAboveForPostProcessing(worldGenLevel, blockPos4);
                                    }
                                }
                            }
                        }
                    }
                }

                if (blockStateConfiguration.state.getMaterial() == Material.WATER) {
                    for(int ae = 0; ae < 16; ++ae) {
                        for(int af = 0; af < 16; ++af) {
                            int ag = 4;
                            BlockPosition blockPos5 = blockPos.offset(ae, 4, af);
                            if (worldGenLevel.getBiome(blockPos5).shouldFreeze(worldGenLevel, blockPos5, false)) {
                                worldGenLevel.setTypeAndData(blockPos5, Blocks.ICE.getBlockData(), 2);
                            }
                        }
                    }
                }

                return true;
            }
        }
    }
}
