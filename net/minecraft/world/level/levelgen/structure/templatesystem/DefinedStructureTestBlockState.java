package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.world.level.block.state.IBlockData;

public class DefinedStructureTestBlockState extends DefinedStructureRuleTest {
    public static final Codec<DefinedStructureTestBlockState> CODEC = IBlockData.CODEC.fieldOf("block_state").xmap(DefinedStructureTestBlockState::new, (blockStateMatchTest) -> {
        return blockStateMatchTest.blockState;
    }).codec();
    private final IBlockData blockState;

    public DefinedStructureTestBlockState(IBlockData blockState) {
        this.blockState = blockState;
    }

    @Override
    public boolean test(IBlockData state, Random random) {
        return state == this.blockState;
    }

    @Override
    protected DefinedStructureRuleTestType<?> getType() {
        return DefinedStructureRuleTestType.BLOCKSTATE_TEST;
    }
}
