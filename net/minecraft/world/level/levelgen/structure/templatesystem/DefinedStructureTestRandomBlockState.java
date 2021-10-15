package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Random;
import net.minecraft.world.level.block.state.IBlockData;

public class DefinedStructureTestRandomBlockState extends DefinedStructureRuleTest {
    public static final Codec<DefinedStructureTestRandomBlockState> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(IBlockData.CODEC.fieldOf("block_state").forGetter((randomBlockStateMatchTest) -> {
            return randomBlockStateMatchTest.blockState;
        }), Codec.FLOAT.fieldOf("probability").forGetter((randomBlockStateMatchTest) -> {
            return randomBlockStateMatchTest.probability;
        })).apply(instance, DefinedStructureTestRandomBlockState::new);
    });
    private final IBlockData blockState;
    private final float probability;

    public DefinedStructureTestRandomBlockState(IBlockData blockState, float probability) {
        this.blockState = blockState;
        this.probability = probability;
    }

    @Override
    public boolean test(IBlockData state, Random random) {
        return state == this.blockState && random.nextFloat() < this.probability;
    }

    @Override
    protected DefinedStructureRuleTestType<?> getType() {
        return DefinedStructureRuleTestType.RANDOM_BLOCKSTATE_TEST;
    }
}
