package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureEmptyConfiguration;

public class WorldGenFeatureBasaltPillar extends WorldGenerator<WorldGenFeatureEmptyConfiguration> {
    public WorldGenFeatureBasaltPillar(Codec<WorldGenFeatureEmptyConfiguration> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeaturePlaceContext<WorldGenFeatureEmptyConfiguration> context) {
        BlockPosition blockPos = context.origin();
        GeneratorAccessSeed worldGenLevel = context.level();
        Random random = context.random();
        if (worldGenLevel.isEmpty(blockPos) && !worldGenLevel.isEmpty(blockPos.above())) {
            BlockPosition.MutableBlockPosition mutableBlockPos = blockPos.mutable();
            BlockPosition.MutableBlockPosition mutableBlockPos2 = blockPos.mutable();
            boolean bl = true;
            boolean bl2 = true;
            boolean bl3 = true;
            boolean bl4 = true;

            while(worldGenLevel.isEmpty(mutableBlockPos)) {
                if (worldGenLevel.isOutsideWorld(mutableBlockPos)) {
                    return true;
                }

                worldGenLevel.setTypeAndData(mutableBlockPos, Blocks.BASALT.getBlockData(), 2);
                bl = bl && this.placeHangOff(worldGenLevel, random, mutableBlockPos2.setWithOffset(mutableBlockPos, EnumDirection.NORTH));
                bl2 = bl2 && this.placeHangOff(worldGenLevel, random, mutableBlockPos2.setWithOffset(mutableBlockPos, EnumDirection.SOUTH));
                bl3 = bl3 && this.placeHangOff(worldGenLevel, random, mutableBlockPos2.setWithOffset(mutableBlockPos, EnumDirection.WEST));
                bl4 = bl4 && this.placeHangOff(worldGenLevel, random, mutableBlockPos2.setWithOffset(mutableBlockPos, EnumDirection.EAST));
                mutableBlockPos.move(EnumDirection.DOWN);
            }

            mutableBlockPos.move(EnumDirection.UP);
            this.placeBaseHangOff(worldGenLevel, random, mutableBlockPos2.setWithOffset(mutableBlockPos, EnumDirection.NORTH));
            this.placeBaseHangOff(worldGenLevel, random, mutableBlockPos2.setWithOffset(mutableBlockPos, EnumDirection.SOUTH));
            this.placeBaseHangOff(worldGenLevel, random, mutableBlockPos2.setWithOffset(mutableBlockPos, EnumDirection.WEST));
            this.placeBaseHangOff(worldGenLevel, random, mutableBlockPos2.setWithOffset(mutableBlockPos, EnumDirection.EAST));
            mutableBlockPos.move(EnumDirection.DOWN);
            BlockPosition.MutableBlockPosition mutableBlockPos3 = new BlockPosition.MutableBlockPosition();

            for(int i = -3; i < 4; ++i) {
                for(int j = -3; j < 4; ++j) {
                    int k = MathHelper.abs(i) * MathHelper.abs(j);
                    if (random.nextInt(10) < 10 - k) {
                        mutableBlockPos3.set(mutableBlockPos.offset(i, 0, j));
                        int l = 3;

                        while(worldGenLevel.isEmpty(mutableBlockPos2.setWithOffset(mutableBlockPos3, EnumDirection.DOWN))) {
                            mutableBlockPos3.move(EnumDirection.DOWN);
                            --l;
                            if (l <= 0) {
                                break;
                            }
                        }

                        if (!worldGenLevel.isEmpty(mutableBlockPos2.setWithOffset(mutableBlockPos3, EnumDirection.DOWN))) {
                            worldGenLevel.setTypeAndData(mutableBlockPos3, Blocks.BASALT.getBlockData(), 2);
                        }
                    }
                }
            }

            return true;
        } else {
            return false;
        }
    }

    private void placeBaseHangOff(GeneratorAccess world, Random random, BlockPosition pos) {
        if (random.nextBoolean()) {
            world.setTypeAndData(pos, Blocks.BASALT.getBlockData(), 2);
        }

    }

    private boolean placeHangOff(GeneratorAccess world, Random random, BlockPosition pos) {
        if (random.nextInt(10) != 0) {
            world.setTypeAndData(pos, Blocks.BASALT.getBlockData(), 2);
            return true;
        } else {
            return false;
        }
    }
}
