package net.minecraft.world.entity.ai.util;

import com.google.common.annotations.VisibleForTesting;
import java.util.Random;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.phys.Vec3D;

public class RandomPositionGenerator {
    private static final int RANDOM_POS_ATTEMPTS = 10;

    public static BlockPosition generateRandomDirection(Random random, int horizontalRange, int verticalRange) {
        int i = random.nextInt(2 * horizontalRange + 1) - horizontalRange;
        int j = random.nextInt(2 * verticalRange + 1) - verticalRange;
        int k = random.nextInt(2 * horizontalRange + 1) - horizontalRange;
        return new BlockPosition(i, j, k);
    }

    @Nullable
    public static BlockPosition generateRandomDirectionWithinRadians(Random random, int horizontalRange, int verticalRange, int startHeight, double directionX, double directionZ, double angleRange) {
        double d = MathHelper.atan2(directionZ, directionX) - (double)((float)Math.PI / 2F);
        double e = d + (double)(2.0F * random.nextFloat() - 1.0F) * angleRange;
        double f = Math.sqrt(random.nextDouble()) * (double)MathHelper.SQRT_OF_TWO * (double)horizontalRange;
        double g = -f * Math.sin(e);
        double h = f * Math.cos(e);
        if (!(Math.abs(g) > (double)horizontalRange) && !(Math.abs(h) > (double)horizontalRange)) {
            int i = random.nextInt(2 * verticalRange + 1) - verticalRange + startHeight;
            return new BlockPosition(g, (double)i, h);
        } else {
            return null;
        }
    }

    @VisibleForTesting
    public static BlockPosition moveUpOutOfSolid(BlockPosition pos, int maxY, Predicate<BlockPosition> condition) {
        if (!condition.test(pos)) {
            return pos;
        } else {
            BlockPosition blockPos;
            for(blockPos = pos.above(); blockPos.getY() < maxY && condition.test(blockPos); blockPos = blockPos.above()) {
            }

            return blockPos;
        }
    }

    @VisibleForTesting
    public static BlockPosition moveUpToAboveSolid(BlockPosition pos, int extraAbove, int max, Predicate<BlockPosition> condition) {
        if (extraAbove < 0) {
            throw new IllegalArgumentException("aboveSolidAmount was " + extraAbove + ", expected >= 0");
        } else if (!condition.test(pos)) {
            return pos;
        } else {
            BlockPosition blockPos;
            for(blockPos = pos.above(); blockPos.getY() < max && condition.test(blockPos); blockPos = blockPos.above()) {
            }

            BlockPosition blockPos2;
            BlockPosition blockPos3;
            for(blockPos2 = blockPos; blockPos2.getY() < max && blockPos2.getY() - blockPos.getY() < extraAbove; blockPos2 = blockPos3) {
                blockPos3 = blockPos2.above();
                if (condition.test(blockPos3)) {
                    break;
                }
            }

            return blockPos2;
        }
    }

    @Nullable
    public static Vec3D generateRandomPos(EntityCreature entity, Supplier<BlockPosition> factory) {
        return generateRandomPos(factory, entity::getWalkTargetValue);
    }

    @Nullable
    public static Vec3D generateRandomPos(Supplier<BlockPosition> factory, ToDoubleFunction<BlockPosition> scorer) {
        double d = Double.NEGATIVE_INFINITY;
        BlockPosition blockPos = null;

        for(int i = 0; i < 10; ++i) {
            BlockPosition blockPos2 = factory.get();
            if (blockPos2 != null) {
                double e = scorer.applyAsDouble(blockPos2);
                if (e > d) {
                    d = e;
                    blockPos = blockPos2;
                }
            }
        }

        return blockPos != null ? Vec3D.atBottomCenterOf(blockPos) : null;
    }

    public static BlockPosition generateRandomPosTowardDirection(EntityCreature entity, int horizontalRange, Random random, BlockPosition fuzz) {
        int i = fuzz.getX();
        int j = fuzz.getZ();
        if (entity.hasRestriction() && horizontalRange > 1) {
            BlockPosition blockPos = entity.getRestrictCenter();
            if (entity.locX() > (double)blockPos.getX()) {
                i -= random.nextInt(horizontalRange / 2);
            } else {
                i += random.nextInt(horizontalRange / 2);
            }

            if (entity.locZ() > (double)blockPos.getZ()) {
                j -= random.nextInt(horizontalRange / 2);
            } else {
                j += random.nextInt(horizontalRange / 2);
            }
        }

        return new BlockPosition((double)i + entity.locX(), (double)fuzz.getY() + entity.locY(), (double)j + entity.locZ());
    }
}
