package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.util.MathHelper;

public class PosRuleTestLinear extends PosRuleTest {
    public static final Codec<PosRuleTestLinear> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Codec.FLOAT.fieldOf("min_chance").orElse(0.0F).forGetter((linearPosTest) -> {
            return linearPosTest.minChance;
        }), Codec.FLOAT.fieldOf("max_chance").orElse(0.0F).forGetter((linearPosTest) -> {
            return linearPosTest.maxChance;
        }), Codec.INT.fieldOf("min_dist").orElse(0).forGetter((linearPosTest) -> {
            return linearPosTest.minDist;
        }), Codec.INT.fieldOf("max_dist").orElse(0).forGetter((linearPosTest) -> {
            return linearPosTest.maxDist;
        })).apply(instance, PosRuleTestLinear::new);
    });
    private final float minChance;
    private final float maxChance;
    private final int minDist;
    private final int maxDist;

    public PosRuleTestLinear(float minChance, float maxChance, int minDistance, int maxDistance) {
        if (minDistance >= maxDistance) {
            throw new IllegalArgumentException("Invalid range: [" + minDistance + "," + maxDistance + "]");
        } else {
            this.minChance = minChance;
            this.maxChance = maxChance;
            this.minDist = minDistance;
            this.maxDist = maxDistance;
        }
    }

    @Override
    public boolean test(BlockPosition blockPos, BlockPosition blockPos2, BlockPosition pivot, Random random) {
        int i = blockPos2.distManhattan(pivot);
        float f = random.nextFloat();
        return f <= MathHelper.clampedLerp(this.minChance, this.maxChance, MathHelper.inverseLerp((float)i, (float)this.minDist, (float)this.maxDist));
    }

    @Override
    protected PosRuleTestType<?> getType() {
        return PosRuleTestType.LINEAR_POS_TEST;
    }
}
