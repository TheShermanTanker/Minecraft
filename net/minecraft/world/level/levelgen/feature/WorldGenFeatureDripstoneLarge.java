package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Optional;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.tags.TagsBlock;
import net.minecraft.util.MathHelper;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Column;
import net.minecraft.world.level.levelgen.feature.configurations.LargeDripstoneConfiguration;
import net.minecraft.world.phys.Vec3D;

public class WorldGenFeatureDripstoneLarge extends WorldGenerator<LargeDripstoneConfiguration> {
    public WorldGenFeatureDripstoneLarge(Codec<LargeDripstoneConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeaturePlaceContext<LargeDripstoneConfiguration> context) {
        GeneratorAccessSeed worldGenLevel = context.level();
        BlockPosition blockPos = context.origin();
        LargeDripstoneConfiguration largeDripstoneConfiguration = context.config();
        Random random = context.random();
        if (!DripstoneUtils.isEmptyOrWater(worldGenLevel, blockPos)) {
            return false;
        } else {
            Optional<Column> optional = Column.scan(worldGenLevel, blockPos, largeDripstoneConfiguration.floorToCeilingSearchRange, DripstoneUtils::isEmptyOrWater, DripstoneUtils::isDripstoneBaseOrLava);
            if (optional.isPresent() && optional.get() instanceof Column.Range) {
                Column.Range range = (Column.Range)optional.get();
                if (range.height() < 4) {
                    return false;
                } else {
                    int i = (int)((float)range.height() * largeDripstoneConfiguration.maxColumnRadiusToCaveHeightRatio);
                    int j = MathHelper.clamp(i, largeDripstoneConfiguration.columnRadius.getMinValue(), largeDripstoneConfiguration.columnRadius.getMaxValue());
                    int k = MathHelper.randomBetweenInclusive(random, largeDripstoneConfiguration.columnRadius.getMinValue(), j);
                    WorldGenFeatureDripstoneLarge.LargeDripstone largeDripstone = makeDripstone(blockPos.atY(range.ceiling() - 1), false, random, k, largeDripstoneConfiguration.stalactiteBluntness, largeDripstoneConfiguration.heightScale);
                    WorldGenFeatureDripstoneLarge.LargeDripstone largeDripstone2 = makeDripstone(blockPos.atY(range.floor() + 1), true, random, k, largeDripstoneConfiguration.stalagmiteBluntness, largeDripstoneConfiguration.heightScale);
                    WorldGenFeatureDripstoneLarge.WindOffsetter windOffsetter;
                    if (largeDripstone.isSuitableForWind(largeDripstoneConfiguration) && largeDripstone2.isSuitableForWind(largeDripstoneConfiguration)) {
                        windOffsetter = new WorldGenFeatureDripstoneLarge.WindOffsetter(blockPos.getY(), random, largeDripstoneConfiguration.windSpeed);
                    } else {
                        windOffsetter = WorldGenFeatureDripstoneLarge.WindOffsetter.noWind();
                    }

                    boolean bl = largeDripstone.moveBackUntilBaseIsInsideStoneAndShrinkRadiusIfNecessary(worldGenLevel, windOffsetter);
                    boolean bl2 = largeDripstone2.moveBackUntilBaseIsInsideStoneAndShrinkRadiusIfNecessary(worldGenLevel, windOffsetter);
                    if (bl) {
                        largeDripstone.placeBlocks(worldGenLevel, random, windOffsetter);
                    }

                    if (bl2) {
                        largeDripstone2.placeBlocks(worldGenLevel, random, windOffsetter);
                    }

                    return true;
                }
            } else {
                return false;
            }
        }
    }

    private static WorldGenFeatureDripstoneLarge.LargeDripstone makeDripstone(BlockPosition pos, boolean isStalagmite, Random random, int scale, FloatProvider bluntness, FloatProvider heightScale) {
        return new WorldGenFeatureDripstoneLarge.LargeDripstone(pos, isStalagmite, scale, (double)bluntness.sample(random), (double)heightScale.sample(random));
    }

    private void placeDebugMarkers(GeneratorAccessSeed worldGenLevel, BlockPosition blockPos, Column.Range range, WorldGenFeatureDripstoneLarge.WindOffsetter windOffsetter) {
        worldGenLevel.setTypeAndData(windOffsetter.offset(blockPos.atY(range.ceiling() - 1)), Blocks.DIAMOND_BLOCK.getBlockData(), 2);
        worldGenLevel.setTypeAndData(windOffsetter.offset(blockPos.atY(range.floor() + 1)), Blocks.GOLD_BLOCK.getBlockData(), 2);

        for(BlockPosition.MutableBlockPosition mutableBlockPos = blockPos.atY(range.floor() + 2).mutable(); mutableBlockPos.getY() < range.ceiling() - 1; mutableBlockPos.move(EnumDirection.UP)) {
            BlockPosition blockPos2 = windOffsetter.offset(mutableBlockPos);
            if (DripstoneUtils.isEmptyOrWater(worldGenLevel, blockPos2) || worldGenLevel.getType(blockPos2).is(Blocks.DRIPSTONE_BLOCK)) {
                worldGenLevel.setTypeAndData(blockPos2, Blocks.CREEPER_HEAD.getBlockData(), 2);
            }
        }

    }

    static final class LargeDripstone {
        private BlockPosition root;
        private final boolean pointingUp;
        private int radius;
        private final double bluntness;
        private final double scale;

        LargeDripstone(BlockPosition blockPos, boolean bl, int i, double d, double e) {
            this.root = blockPos;
            this.pointingUp = bl;
            this.radius = i;
            this.bluntness = d;
            this.scale = e;
        }

        private int getHeight() {
            return this.getHeightAtRadius(0.0F);
        }

        private int getMinY() {
            return this.pointingUp ? this.root.getY() : this.root.getY() - this.getHeight();
        }

        private int getMaxY() {
            return !this.pointingUp ? this.root.getY() : this.root.getY() + this.getHeight();
        }

        boolean moveBackUntilBaseIsInsideStoneAndShrinkRadiusIfNecessary(GeneratorAccessSeed world, WorldGenFeatureDripstoneLarge.WindOffsetter wind) {
            while(this.radius > 1) {
                BlockPosition.MutableBlockPosition mutableBlockPos = this.root.mutable();
                int i = Math.min(10, this.getHeight());

                for(int j = 0; j < i; ++j) {
                    if (world.getType(mutableBlockPos).is(Blocks.LAVA)) {
                        return false;
                    }

                    if (DripstoneUtils.isCircleMostlyEmbeddedInStone(world, wind.offset(mutableBlockPos), this.radius)) {
                        this.root = mutableBlockPos;
                        return true;
                    }

                    mutableBlockPos.move(this.pointingUp ? EnumDirection.DOWN : EnumDirection.UP);
                }

                this.radius /= 2;
            }

            return false;
        }

        private int getHeightAtRadius(float height) {
            return (int)DripstoneUtils.getDripstoneHeight((double)height, (double)this.radius, this.scale, this.bluntness);
        }

        void placeBlocks(GeneratorAccessSeed world, Random random, WorldGenFeatureDripstoneLarge.WindOffsetter wind) {
            for(int i = -this.radius; i <= this.radius; ++i) {
                for(int j = -this.radius; j <= this.radius; ++j) {
                    float f = MathHelper.sqrt((float)(i * i + j * j));
                    if (!(f > (float)this.radius)) {
                        int k = this.getHeightAtRadius(f);
                        if (k > 0) {
                            if ((double)random.nextFloat() < 0.2D) {
                                k = (int)((float)k * MathHelper.randomBetween(random, 0.8F, 1.0F));
                            }

                            BlockPosition.MutableBlockPosition mutableBlockPos = this.root.offset(i, 0, j).mutable();
                            boolean bl = false;

                            for(int l = 0; l < k; ++l) {
                                BlockPosition blockPos = wind.offset(mutableBlockPos);
                                if (DripstoneUtils.isEmptyOrWaterOrLava(world, blockPos)) {
                                    bl = true;
                                    Block block = Blocks.DRIPSTONE_BLOCK;
                                    world.setTypeAndData(blockPos, block.getBlockData(), 2);
                                } else if (bl && world.getType(blockPos).is(TagsBlock.BASE_STONE_OVERWORLD)) {
                                    break;
                                }

                                mutableBlockPos.move(this.pointingUp ? EnumDirection.UP : EnumDirection.DOWN);
                            }
                        }
                    }
                }
            }

        }

        boolean isSuitableForWind(LargeDripstoneConfiguration config) {
            return this.radius >= config.minRadiusForWind && this.bluntness >= (double)config.minBluntnessForWind;
        }
    }

    static final class WindOffsetter {
        private final int originY;
        @Nullable
        private final Vec3D windSpeed;

        WindOffsetter(int y, Random random, FloatProvider wind) {
            this.originY = y;
            float f = wind.sample(random);
            float g = MathHelper.randomBetween(random, 0.0F, (float)Math.PI);
            this.windSpeed = new Vec3D((double)(MathHelper.cos(g) * f), 0.0D, (double)(MathHelper.sin(g) * f));
        }

        private WindOffsetter() {
            this.originY = 0;
            this.windSpeed = null;
        }

        static WorldGenFeatureDripstoneLarge.WindOffsetter noWind() {
            return new WorldGenFeatureDripstoneLarge.WindOffsetter();
        }

        BlockPosition offset(BlockPosition pos) {
            if (this.windSpeed == null) {
                return pos;
            } else {
                int i = this.originY - pos.getY();
                Vec3D vec3 = this.windSpeed.scale((double)i);
                return pos.offset(vec3.x, 0.0D, vec3.z);
            }
        }
    }
}
