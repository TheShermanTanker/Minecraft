package net.minecraft.world.level.levelgen.feature;

import java.util.function.Consumer;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.tags.TagsBlock;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.block.BlockDripstonePointed;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;

public class DripstoneUtils {
    protected static double getDripstoneHeight(double radius, double scale, double heightScale, double bluntness) {
        if (radius < bluntness) {
            radius = bluntness;
        }

        double d = 0.384D;
        double e = radius / scale * 0.384D;
        double f = 0.75D * Math.pow(e, 1.3333333333333333D);
        double g = Math.pow(e, 0.6666666666666666D);
        double h = 0.3333333333333333D * Math.log(e);
        double i = heightScale * (f - g - h);
        i = Math.max(i, 0.0D);
        return i / 0.384D * scale;
    }

    protected static boolean isCircleMostlyEmbeddedInStone(GeneratorAccessSeed world, BlockPosition pos, int height) {
        if (isEmptyOrWaterOrLava(world, pos)) {
            return false;
        } else {
            float f = 6.0F;
            float g = 6.0F / (float)height;

            for(float h = 0.0F; h < ((float)Math.PI * 2F); h += g) {
                int i = (int)(MathHelper.cos(h) * (float)height);
                int j = (int)(MathHelper.sin(h) * (float)height);
                if (isEmptyOrWaterOrLava(world, pos.offset(i, 0, j))) {
                    return false;
                }
            }

            return true;
        }
    }

    protected static boolean isEmptyOrWater(GeneratorAccess world, BlockPosition pos) {
        return world.isStateAtPosition(pos, DripstoneUtils::isEmptyOrWater);
    }

    protected static boolean isEmptyOrWaterOrLava(GeneratorAccess world, BlockPosition pos) {
        return world.isStateAtPosition(pos, DripstoneUtils::isEmptyOrWaterOrLava);
    }

    protected static void buildBaseToTipColumn(EnumDirection direction, int height, boolean merge, Consumer<IBlockData> callback) {
        if (height >= 3) {
            callback.accept(createPointedDripstone(direction, DripstoneThickness.BASE));

            for(int i = 0; i < height - 3; ++i) {
                callback.accept(createPointedDripstone(direction, DripstoneThickness.MIDDLE));
            }
        }

        if (height >= 2) {
            callback.accept(createPointedDripstone(direction, DripstoneThickness.FRUSTUM));
        }

        if (height >= 1) {
            callback.accept(createPointedDripstone(direction, merge ? DripstoneThickness.TIP_MERGE : DripstoneThickness.TIP));
        }

    }

    protected static void growPointedDripstone(GeneratorAccess world, BlockPosition pos, EnumDirection direction, int height, boolean merge) {
        if (isDripstoneBase(world.getType(pos.relative(direction.opposite())))) {
            BlockPosition.MutableBlockPosition mutableBlockPos = pos.mutable();
            buildBaseToTipColumn(direction, height, merge, (state) -> {
                if (state.is(Blocks.POINTED_DRIPSTONE)) {
                    state = state.set(BlockDripstonePointed.WATERLOGGED, Boolean.valueOf(world.isWaterAt(mutableBlockPos)));
                }

                world.setTypeAndData(mutableBlockPos, state, 2);
                mutableBlockPos.move(direction);
            });
        }
    }

    protected static boolean placeDripstoneBlockIfPossible(GeneratorAccess world, BlockPosition pos) {
        IBlockData blockState = world.getType(pos);
        if (blockState.is(TagsBlock.DRIPSTONE_REPLACEABLE)) {
            world.setTypeAndData(pos, Blocks.DRIPSTONE_BLOCK.getBlockData(), 2);
            return true;
        } else {
            return false;
        }
    }

    private static IBlockData createPointedDripstone(EnumDirection direction, DripstoneThickness thickness) {
        return Blocks.POINTED_DRIPSTONE.getBlockData().set(BlockDripstonePointed.TIP_DIRECTION, direction).set(BlockDripstonePointed.THICKNESS, thickness);
    }

    public static boolean isDripstoneBaseOrLava(IBlockData state) {
        return isDripstoneBase(state) || state.is(Blocks.LAVA);
    }

    public static boolean isDripstoneBase(IBlockData state) {
        return state.is(Blocks.DRIPSTONE_BLOCK) || state.is(TagsBlock.DRIPSTONE_REPLACEABLE);
    }

    public static boolean isEmptyOrWater(IBlockData state) {
        return state.isAir() || state.is(Blocks.WATER);
    }

    public static boolean isEmptyOrWaterOrLava(IBlockData state) {
        return state.isAir() || state.is(Blocks.WATER) || state.is(Blocks.LAVA);
    }
}
