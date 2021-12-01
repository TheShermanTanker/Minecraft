package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Random;
import net.minecraft.core.IRegistry;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.IBlockData;

public class DefinedStructureTestRandomBlock extends DefinedStructureRuleTest {
    public static final Codec<DefinedStructureTestRandomBlock> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(IRegistry.BLOCK.byNameCodec().fieldOf("block").forGetter((randomBlockMatchTest) -> {
            return randomBlockMatchTest.block;
        }), Codec.FLOAT.fieldOf("probability").forGetter((randomBlockMatchTest) -> {
            return randomBlockMatchTest.probability;
        })).apply(instance, DefinedStructureTestRandomBlock::new);
    });
    private final Block block;
    private final float probability;

    public DefinedStructureTestRandomBlock(Block block, float probability) {
        this.block = block;
        this.probability = probability;
    }

    @Override
    public boolean test(IBlockData state, Random random) {
        return state.is(this.block) && random.nextFloat() < this.probability;
    }

    @Override
    protected DefinedStructureRuleTestType<?> getType() {
        return DefinedStructureRuleTestType.RANDOM_BLOCK_TEST;
    }
}
