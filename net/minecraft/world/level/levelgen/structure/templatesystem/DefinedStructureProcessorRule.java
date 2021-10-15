package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.block.state.IBlockData;

public class DefinedStructureProcessorRule extends DefinedStructureProcessor {
    public static final Codec<DefinedStructureProcessorRule> CODEC = DefinedStructureProcessorPredicates.CODEC.listOf().fieldOf("rules").xmap(DefinedStructureProcessorRule::new, (ruleProcessor) -> {
        return ruleProcessor.rules;
    }).codec();
    private final ImmutableList<DefinedStructureProcessorPredicates> rules;

    public DefinedStructureProcessorRule(List<? extends DefinedStructureProcessorPredicates> rules) {
        this.rules = ImmutableList.copyOf(rules);
    }

    @Nullable
    @Override
    public DefinedStructure.BlockInfo processBlock(IWorldReader world, BlockPosition pos, BlockPosition pivot, DefinedStructure.BlockInfo structureBlockInfo, DefinedStructure.BlockInfo structureBlockInfo2, DefinedStructureInfo data) {
        Random random = new Random(MathHelper.getSeed(structureBlockInfo2.pos));
        IBlockData blockState = world.getType(structureBlockInfo2.pos);

        for(DefinedStructureProcessorPredicates processorRule : this.rules) {
            if (processorRule.test(structureBlockInfo2.state, blockState, structureBlockInfo.pos, structureBlockInfo2.pos, pivot, random)) {
                return new DefinedStructure.BlockInfo(structureBlockInfo2.pos, processorRule.getOutputState(), processorRule.getOutputTag());
            }
        }

        return structureBlockInfo2;
    }

    @Override
    protected DefinedStructureStructureProcessorType<?> getType() {
        return DefinedStructureStructureProcessorType.RULE;
    }
}
