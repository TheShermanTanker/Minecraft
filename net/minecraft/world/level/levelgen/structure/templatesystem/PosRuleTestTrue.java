package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPosition;

public class PosRuleTestTrue extends PosRuleTest {
    public static final Codec<PosRuleTestTrue> CODEC;
    public static final PosRuleTestTrue INSTANCE = new PosRuleTestTrue();

    private PosRuleTestTrue() {
    }

    @Override
    public boolean test(BlockPosition blockPos, BlockPosition blockPos2, BlockPosition pivot, Random random) {
        return true;
    }

    @Override
    protected PosRuleTestType<?> getType() {
        return PosRuleTestType.ALWAYS_TRUE_TEST;
    }

    static {
        CODEC = Codec.unit(() -> {
            return INSTANCE;
        });
    }
}
