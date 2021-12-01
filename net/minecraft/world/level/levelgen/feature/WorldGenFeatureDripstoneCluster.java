package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.tags.TagsBlock;
import net.minecraft.tags.TagsFluid;
import net.minecraft.util.MathHelper;
import net.minecraft.util.valueproviders.FloatProviderClampedNormal;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.Column;
import net.minecraft.world.level.levelgen.feature.configurations.DripstoneClusterConfiguration;

public class WorldGenFeatureDripstoneCluster extends WorldGenerator<DripstoneClusterConfiguration> {
    public WorldGenFeatureDripstoneCluster(Codec<DripstoneClusterConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeaturePlaceContext<DripstoneClusterConfiguration> context) {
        GeneratorAccessSeed worldGenLevel = context.level();
        BlockPosition blockPos = context.origin();
        DripstoneClusterConfiguration dripstoneClusterConfiguration = context.config();
        Random random = context.random();
        if (!DripstoneUtils.isEmptyOrWater(worldGenLevel, blockPos)) {
            return false;
        } else {
            int i = dripstoneClusterConfiguration.height.sample(random);
            float f = dripstoneClusterConfiguration.wetness.sample(random);
            float g = dripstoneClusterConfiguration.density.sample(random);
            int j = dripstoneClusterConfiguration.radius.sample(random);
            int k = dripstoneClusterConfiguration.radius.sample(random);

            for(int l = -j; l <= j; ++l) {
                for(int m = -k; m <= k; ++m) {
                    double d = this.getChanceOfStalagmiteOrStalactite(j, k, l, m, dripstoneClusterConfiguration);
                    BlockPosition blockPos2 = blockPos.offset(l, 0, m);
                    this.placeColumn(worldGenLevel, random, blockPos2, l, m, f, d, i, g, dripstoneClusterConfiguration);
                }
            }

            return true;
        }
    }

    private void placeColumn(GeneratorAccessSeed world, Random random, BlockPosition pos, int localX, int localZ, float wetness, double dripstoneChance, int height, float density, DripstoneClusterConfiguration config) {
        Optional<Column> optional = Column.scan(world, pos, config.floorToCeilingSearchRange, DripstoneUtils::isEmptyOrWater, DripstoneUtils::isDripstoneBaseOrLava);
        if (optional.isPresent()) {
            OptionalInt optionalInt = optional.get().getCeiling();
            OptionalInt optionalInt2 = optional.get().getFloor();
            if (optionalInt.isPresent() || optionalInt2.isPresent()) {
                boolean bl = random.nextFloat() < wetness;
                Column column;
                if (bl && optionalInt2.isPresent() && this.canPlacePool(world, pos.atY(optionalInt2.getAsInt()))) {
                    int i = optionalInt2.getAsInt();
                    column = optional.get().withFloor(OptionalInt.of(i - 1));
                    world.setTypeAndData(pos.atY(i), Blocks.WATER.getBlockData(), 2);
                } else {
                    column = optional.get();
                }

                OptionalInt optionalInt3 = column.getFloor();
                boolean bl2 = random.nextDouble() < dripstoneChance;
                int m;
                if (optionalInt.isPresent() && bl2 && !this.isLava(world, pos.atY(optionalInt.getAsInt()))) {
                    int j = config.dripstoneBlockLayerThickness.sample(random);
                    this.replaceBlocksWithDripstoneBlocks(world, pos.atY(optionalInt.getAsInt()), j, EnumDirection.UP);
                    int k;
                    if (optionalInt3.isPresent()) {
                        k = Math.min(height, optionalInt.getAsInt() - optionalInt3.getAsInt());
                    } else {
                        k = height;
                    }

                    m = this.getDripstoneHeight(random, localX, localZ, density, k, config);
                } else {
                    m = 0;
                }

                boolean bl3 = random.nextDouble() < dripstoneChance;
                int p;
                if (optionalInt3.isPresent() && bl3 && !this.isLava(world, pos.atY(optionalInt3.getAsInt()))) {
                    int o = config.dripstoneBlockLayerThickness.sample(random);
                    this.replaceBlocksWithDripstoneBlocks(world, pos.atY(optionalInt3.getAsInt()), o, EnumDirection.DOWN);
                    if (optionalInt.isPresent()) {
                        p = Math.max(0, m + MathHelper.randomBetweenInclusive(random, -config.maxStalagmiteStalactiteHeightDiff, config.maxStalagmiteStalactiteHeightDiff));
                    } else {
                        p = this.getDripstoneHeight(random, localX, localZ, density, height, config);
                    }
                } else {
                    p = 0;
                }

                int z;
                int y;
                if (optionalInt.isPresent() && optionalInt3.isPresent() && optionalInt.getAsInt() - m <= optionalInt3.getAsInt() + p) {
                    int s = optionalInt3.getAsInt();
                    int t = optionalInt.getAsInt();
                    int u = Math.max(t - m, s + 1);
                    int v = Math.min(s + p, t - 1);
                    int w = MathHelper.randomBetweenInclusive(random, u, v + 1);
                    int x = w - 1;
                    y = t - w;
                    z = x - s;
                } else {
                    y = m;
                    z = p;
                }

                boolean bl4 = random.nextBoolean() && y > 0 && z > 0 && column.getHeight().isPresent() && y + z == column.getHeight().getAsInt();
                if (optionalInt.isPresent()) {
                    DripstoneUtils.growPointedDripstone(world, pos.atY(optionalInt.getAsInt() - 1), EnumDirection.DOWN, y, bl4);
                }

                if (optionalInt3.isPresent()) {
                    DripstoneUtils.growPointedDripstone(world, pos.atY(optionalInt3.getAsInt() + 1), EnumDirection.UP, z, bl4);
                }

            }
        }
    }

    private boolean isLava(IWorldReader world, BlockPosition pos) {
        return world.getType(pos).is(Blocks.LAVA);
    }

    private int getDripstoneHeight(Random random, int localX, int localZ, float density, int height, DripstoneClusterConfiguration config) {
        if (random.nextFloat() > density) {
            return 0;
        } else {
            int i = Math.abs(localX) + Math.abs(localZ);
            float f = (float)MathHelper.clampedMap((double)i, 0.0D, (double)config.maxDistanceFromCenterAffectingHeightBias, (double)height / 2.0D, 0.0D);
            return (int)randomBetweenBiased(random, 0.0F, (float)height, f, (float)config.heightDeviation);
        }
    }

    private boolean canPlacePool(GeneratorAccessSeed world, BlockPosition pos) {
        IBlockData blockState = world.getType(pos);
        if (!blockState.is(Blocks.WATER) && !blockState.is(Blocks.DRIPSTONE_BLOCK) && !blockState.is(Blocks.POINTED_DRIPSTONE)) {
            for(EnumDirection direction : EnumDirection.EnumDirectionLimit.HORIZONTAL) {
                if (!this.canBeAdjacentToWater(world, pos.relative(direction))) {
                    return false;
                }
            }

            return this.canBeAdjacentToWater(world, pos.below());
        } else {
            return false;
        }
    }

    private boolean canBeAdjacentToWater(GeneratorAccess world, BlockPosition pos) {
        IBlockData blockState = world.getType(pos);
        return blockState.is(TagsBlock.BASE_STONE_OVERWORLD) || blockState.getFluid().is(TagsFluid.WATER);
    }

    private void replaceBlocksWithDripstoneBlocks(GeneratorAccessSeed world, BlockPosition pos, int height, EnumDirection direction) {
        BlockPosition.MutableBlockPosition mutableBlockPos = pos.mutable();

        for(int i = 0; i < height; ++i) {
            if (!DripstoneUtils.placeDripstoneBlockIfPossible(world, mutableBlockPos)) {
                return;
            }

            mutableBlockPos.move(direction);
        }

    }

    private double getChanceOfStalagmiteOrStalactite(int radiusX, int radiusZ, int localX, int localZ, DripstoneClusterConfiguration config) {
        int i = radiusX - Math.abs(localX);
        int j = radiusZ - Math.abs(localZ);
        int k = Math.min(i, j);
        return (double)MathHelper.clampedMap((float)k, 0.0F, (float)config.maxDistanceFromEdgeAffectingChanceOfDripstoneColumn, config.chanceOfDripstoneColumnAtMaxDistanceFromCenter, 1.0F);
    }

    private static float randomBetweenBiased(Random random, float min, float max, float mean, float deviation) {
        return FloatProviderClampedNormal.sample(random, mean, deviation, min, max);
    }
}
