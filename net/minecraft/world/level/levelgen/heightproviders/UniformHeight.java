package net.minecraft.world.level.levelgen.heightproviders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Random;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UniformHeight extends HeightProvider {
    public static final Codec<UniformHeight> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(VerticalAnchor.CODEC.fieldOf("min_inclusive").forGetter((uniformHeight) -> {
            return uniformHeight.minInclusive;
        }), VerticalAnchor.CODEC.fieldOf("max_inclusive").forGetter((uniformHeight) -> {
            return uniformHeight.maxInclusive;
        })).apply(instance, UniformHeight::new);
    });
    private static final Logger LOGGER = LogManager.getLogger();
    private final VerticalAnchor minInclusive;
    private final VerticalAnchor maxInclusive;

    private UniformHeight(VerticalAnchor minOffset, VerticalAnchor maxOffset) {
        this.minInclusive = minOffset;
        this.maxInclusive = maxOffset;
    }

    public static UniformHeight of(VerticalAnchor minOffset, VerticalAnchor maxOffset) {
        return new UniformHeight(minOffset, maxOffset);
    }

    @Override
    public int sample(Random random, WorldGenerationContext context) {
        int i = this.minInclusive.resolveY(context);
        int j = this.maxInclusive.resolveY(context);
        if (i > j) {
            LOGGER.warn("Empty height range: {}", (Object)this);
            return i;
        } else {
            return MathHelper.randomBetweenInclusive(random, i, j);
        }
    }

    @Override
    public HeightProviderType<?> getType() {
        return HeightProviderType.UNIFORM;
    }

    @Override
    public String toString() {
        return "[" + this.minInclusive + "-" + this.maxInclusive + "]";
    }
}
