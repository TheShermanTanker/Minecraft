package net.minecraft.world.level.block.state.pattern;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BaseBlockPosition;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.level.IWorldReader;

public class ShapeDetector {
    private final Predicate<ShapeDetectorBlock>[][][] pattern;
    private final int depth;
    private final int height;
    private final int width;

    public ShapeDetector(Predicate<ShapeDetectorBlock>[][][] pattern) {
        this.pattern = pattern;
        this.depth = pattern.length;
        if (this.depth > 0) {
            this.height = pattern[0].length;
            if (this.height > 0) {
                this.width = pattern[0][0].length;
            } else {
                this.width = 0;
            }
        } else {
            this.height = 0;
            this.width = 0;
        }

    }

    public int getDepth() {
        return this.depth;
    }

    public int getHeight() {
        return this.height;
    }

    public int getWidth() {
        return this.width;
    }

    @VisibleForTesting
    public Predicate<ShapeDetectorBlock>[][][] getPattern() {
        return this.pattern;
    }

    @Nullable
    @VisibleForTesting
    public ShapeDetector.ShapeDetectorCollection matches(IWorldReader world, BlockPosition frontTopLeft, EnumDirection forwards, EnumDirection up) {
        LoadingCache<BlockPosition, ShapeDetectorBlock> loadingCache = createLevelCache(world, false);
        return this.matches(frontTopLeft, forwards, up, loadingCache);
    }

    @Nullable
    private ShapeDetector.ShapeDetectorCollection matches(BlockPosition frontTopLeft, EnumDirection forwards, EnumDirection up, LoadingCache<BlockPosition, ShapeDetectorBlock> cache) {
        for(int i = 0; i < this.width; ++i) {
            for(int j = 0; j < this.height; ++j) {
                for(int k = 0; k < this.depth; ++k) {
                    if (!this.pattern[k][j][i].test(cache.getUnchecked(translateAndRotate(frontTopLeft, forwards, up, i, j, k)))) {
                        return null;
                    }
                }
            }
        }

        return new ShapeDetector.ShapeDetectorCollection(frontTopLeft, forwards, up, cache, this.width, this.height, this.depth);
    }

    @Nullable
    public ShapeDetector.ShapeDetectorCollection find(IWorldReader world, BlockPosition pos) {
        LoadingCache<BlockPosition, ShapeDetectorBlock> loadingCache = createLevelCache(world, false);
        int i = Math.max(Math.max(this.width, this.height), this.depth);

        for(BlockPosition blockPos : BlockPosition.betweenClosed(pos, pos.offset(i - 1, i - 1, i - 1))) {
            for(EnumDirection direction : EnumDirection.values()) {
                for(EnumDirection direction2 : EnumDirection.values()) {
                    if (direction2 != direction && direction2 != direction.opposite()) {
                        ShapeDetector.ShapeDetectorCollection blockPatternMatch = this.matches(blockPos, direction, direction2, loadingCache);
                        if (blockPatternMatch != null) {
                            return blockPatternMatch;
                        }
                    }
                }
            }
        }

        return null;
    }

    public static LoadingCache<BlockPosition, ShapeDetectorBlock> createLevelCache(IWorldReader world, boolean forceLoad) {
        return CacheBuilder.newBuilder().build(new ShapeDetector.BlockLoader(world, forceLoad));
    }

    protected static BlockPosition translateAndRotate(BlockPosition pos, EnumDirection forwards, EnumDirection up, int offsetLeft, int offsetDown, int offsetForwards) {
        if (forwards != up && forwards != up.opposite()) {
            BaseBlockPosition vec3i = new BaseBlockPosition(forwards.getAdjacentX(), forwards.getAdjacentY(), forwards.getAdjacentZ());
            BaseBlockPosition vec3i2 = new BaseBlockPosition(up.getAdjacentX(), up.getAdjacentY(), up.getAdjacentZ());
            BaseBlockPosition vec3i3 = vec3i.cross(vec3i2);
            return pos.offset(vec3i2.getX() * -offsetDown + vec3i3.getX() * offsetLeft + vec3i.getX() * offsetForwards, vec3i2.getY() * -offsetDown + vec3i3.getY() * offsetLeft + vec3i.getY() * offsetForwards, vec3i2.getZ() * -offsetDown + vec3i3.getZ() * offsetLeft + vec3i.getZ() * offsetForwards);
        } else {
            throw new IllegalArgumentException("Invalid forwards & up combination");
        }
    }

    static class BlockLoader extends CacheLoader<BlockPosition, ShapeDetectorBlock> {
        private final IWorldReader level;
        private final boolean loadChunks;

        public BlockLoader(IWorldReader world, boolean forceLoad) {
            this.level = world;
            this.loadChunks = forceLoad;
        }

        @Override
        public ShapeDetectorBlock load(BlockPosition blockPos) {
            return new ShapeDetectorBlock(this.level, blockPos, this.loadChunks);
        }
    }

    public static class ShapeDetectorCollection {
        private final BlockPosition frontTopLeft;
        private final EnumDirection forwards;
        private final EnumDirection up;
        private final LoadingCache<BlockPosition, ShapeDetectorBlock> cache;
        private final int width;
        private final int height;
        private final int depth;

        public ShapeDetectorCollection(BlockPosition frontTopLeft, EnumDirection forwards, EnumDirection up, LoadingCache<BlockPosition, ShapeDetectorBlock> cache, int width, int height, int depth) {
            this.frontTopLeft = frontTopLeft;
            this.forwards = forwards;
            this.up = up;
            this.cache = cache;
            this.width = width;
            this.height = height;
            this.depth = depth;
        }

        public BlockPosition getFrontTopLeft() {
            return this.frontTopLeft;
        }

        public EnumDirection getFacing() {
            return this.forwards;
        }

        public EnumDirection getUp() {
            return this.up;
        }

        public int getWidth() {
            return this.width;
        }

        public int getHeight() {
            return this.height;
        }

        public int getDepth() {
            return this.depth;
        }

        public ShapeDetectorBlock getBlock(int offsetLeft, int offsetDown, int offsetForwards) {
            return this.cache.getUnchecked(ShapeDetector.translateAndRotate(this.frontTopLeft, this.getFacing(), this.getUp(), offsetLeft, offsetDown, offsetForwards));
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this).add("up", this.up).add("forwards", this.forwards).add("frontTopLeft", this.frontTopLeft).toString();
        }
    }
}
