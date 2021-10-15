package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;

public abstract class PosRuleTest {
    public static final Codec<PosRuleTest> CODEC = IRegistry.POS_RULE_TEST.dispatch("predicate_type", PosRuleTest::getType, PosRuleTestType::codec);

    public abstract boolean test(BlockPosition blockPos, BlockPosition blockPos2, BlockPosition pivot, Random random);

    protected abstract PosRuleTestType<?> getType();
}
