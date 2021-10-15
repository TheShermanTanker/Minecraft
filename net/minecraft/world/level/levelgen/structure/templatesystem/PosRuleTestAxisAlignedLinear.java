package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.util.MathHelper;

public class PosRuleTestAxisAlignedLinear extends PosRuleTest {
    public static final Codec<PosRuleTestAxisAlignedLinear> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(Codec.FLOAT.fieldOf("min_chance").orElse(0.0F).forGetter((axisAlignedLinearPosTest) -> {
            return axisAlignedLinearPosTest.minChance;
        }), Codec.FLOAT.fieldOf("max_chance").orElse(0.0F).forGetter((axisAlignedLinearPosTest) -> {
            return axisAlignedLinearPosTest.maxChance;
        }), Codec.INT.fieldOf("min_dist").orElse(0).forGetter((axisAlignedLinearPosTest) -> {
            return axisAlignedLinearPosTest.minDist;
        }), Codec.INT.fieldOf("max_dist").orElse(0).forGetter((axisAlignedLinearPosTest) -> {
            return axisAlignedLinearPosTest.maxDist;
        }), EnumDirection.EnumAxis.CODEC.fieldOf("axis").orElse(EnumDirection.EnumAxis.Y).forGetter((axisAlignedLinearPosTest) -> {
            return axisAlignedLinearPosTest.axis;
        })).apply(instance, PosRuleTestAxisAlignedLinear::new);
    });
    private final float minChance;
    private final float maxChance;
    private final int minDist;
    private final int maxDist;
    private final EnumDirection.EnumAxis axis;

    public PosRuleTestAxisAlignedLinear(float minChance, float maxChance, int minDistance, int maxDistance, EnumDirection.EnumAxis axis) {
        if (minDistance >= maxDistance) {
            throw new IllegalArgumentException("Invalid range: [" + minDistance + "," + maxDistance + "]");
        } else {
            this.minChance = minChance;
            this.maxChance = maxChance;
            this.minDist = minDistance;
            this.maxDist = maxDistance;
            this.axis = axis;
        }
    }

    @Override
    public boolean test(BlockPosition blockPos, BlockPosition blockPos2, BlockPosition pivot, Random random) {
        EnumDirection direction = EnumDirection.get(EnumDirection.EnumAxisDirection.POSITIVE, this.axis);
        float f = (float)Math.abs((blockPos2.getX() - pivot.getX()) * direction.getAdjacentX());
        float g = (float)Math.abs((blockPos2.getY() - pivot.getY()) * direction.getAdjacentY());
        float h = (float)Math.abs((blockPos2.getZ() - pivot.getZ()) * direction.getAdjacentZ());
        int i = (int)(f + g + h);
        float j = random.nextFloat();
        return (double)j <= MathHelper.clampedLerp((double)this.minChance, (double)this.maxChance, MathHelper.inverseLerp((double)i, (double)this.minDist, (double)this.maxDist));
    }

    @Override
    protected PosRuleTestType<?> getType() {
        return PosRuleTestType.AXIS_ALIGNED_LINEAR_POS_TEST;
    }
}
