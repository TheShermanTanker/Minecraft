package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.world.level.block.state.IBlockData;

public class DefinedStructureTestTrue extends DefinedStructureRuleTest {
    public static final Codec<DefinedStructureTestTrue> CODEC;
    public static final DefinedStructureTestTrue INSTANCE = new DefinedStructureTestTrue();

    private DefinedStructureTestTrue() {
    }

    @Override
    public boolean test(IBlockData state, Random random) {
        return true;
    }

    @Override
    protected DefinedStructureRuleTestType<?> getType() {
        return DefinedStructureRuleTestType.ALWAYS_TRUE_TEST;
    }

    static {
        CODEC = Codec.unit(() -> {
            return INSTANCE;
        });
    }
}
