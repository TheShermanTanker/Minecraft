package net.minecraft.world.level.levelgen;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import java.util.function.Function;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.dimension.DimensionManager;

public abstract class VerticalAnchor {
    public static final Codec<VerticalAnchor> CODEC = ExtraCodecs.xor(VerticalAnchor.Absolute.CODEC, ExtraCodecs.xor(VerticalAnchor.AboveBottom.CODEC, VerticalAnchor.BelowTop.CODEC)).xmap(VerticalAnchor::merge, VerticalAnchor::split);
    private static final VerticalAnchor BOTTOM = aboveBottom(0);
    private static final VerticalAnchor TOP = belowTop(0);
    private final int value;

    protected VerticalAnchor(int offset) {
        this.value = offset;
    }

    public static VerticalAnchor absolute(int offset) {
        return new VerticalAnchor.Absolute(offset);
    }

    public static VerticalAnchor aboveBottom(int offset) {
        return new VerticalAnchor.AboveBottom(offset);
    }

    public static VerticalAnchor belowTop(int offset) {
        return new VerticalAnchor.BelowTop(offset);
    }

    public static VerticalAnchor bottom() {
        return BOTTOM;
    }

    public static VerticalAnchor top() {
        return TOP;
    }

    private static VerticalAnchor merge(Either<VerticalAnchor.Absolute, Either<VerticalAnchor.AboveBottom, VerticalAnchor.BelowTop>> either) {
        return either.map(Function.identity(), (eitherx) -> {
            return eitherx.map(Function.identity(), Function.identity());
        });
    }

    private static Either<VerticalAnchor.Absolute, Either<VerticalAnchor.AboveBottom, VerticalAnchor.BelowTop>> split(VerticalAnchor yOffset) {
        return yOffset instanceof VerticalAnchor.Absolute ? Either.left((VerticalAnchor.Absolute)yOffset) : Either.right(yOffset instanceof VerticalAnchor.AboveBottom ? Either.left((VerticalAnchor.AboveBottom)yOffset) : Either.right((VerticalAnchor.BelowTop)yOffset));
    }

    protected int value() {
        return this.value;
    }

    public abstract int resolveY(WorldGenerationContext context);

    static final class AboveBottom extends VerticalAnchor {
        public static final Codec<VerticalAnchor.AboveBottom> CODEC = Codec.intRange(DimensionManager.MIN_Y, DimensionManager.MAX_Y).fieldOf("above_bottom").xmap(VerticalAnchor.AboveBottom::new, VerticalAnchor::value).codec();

        protected AboveBottom(int offset) {
            super(offset);
        }

        @Override
        public int resolveY(WorldGenerationContext context) {
            return context.getMinGenY() + this.value();
        }

        @Override
        public String toString() {
            return this.value() + " above bottom";
        }
    }

    static final class Absolute extends VerticalAnchor {
        public static final Codec<VerticalAnchor.Absolute> CODEC = Codec.intRange(DimensionManager.MIN_Y, DimensionManager.MAX_Y).fieldOf("absolute").xmap(VerticalAnchor.Absolute::new, VerticalAnchor::value).codec();

        protected Absolute(int offset) {
            super(offset);
        }

        @Override
        public int resolveY(WorldGenerationContext context) {
            return this.value();
        }

        @Override
        public String toString() {
            return this.value() + " absolute";
        }
    }

    static final class BelowTop extends VerticalAnchor {
        public static final Codec<VerticalAnchor.BelowTop> CODEC = Codec.intRange(DimensionManager.MIN_Y, DimensionManager.MAX_Y).fieldOf("below_top").xmap(VerticalAnchor.BelowTop::new, VerticalAnchor::value).codec();

        protected BelowTop(int offset) {
            super(offset);
        }

        @Override
        public int resolveY(WorldGenerationContext context) {
            return context.getGenDepth() - 1 + context.getMinGenY() - this.value();
        }

        @Override
        public String toString() {
            return this.value() + " below top";
        }
    }
}
