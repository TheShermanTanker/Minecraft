package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.IBlockData;

public class DefinedStructureProcessorBlockIgnore extends DefinedStructureProcessor {
    public static final Codec<DefinedStructureProcessorBlockIgnore> CODEC = IBlockData.CODEC.xmap(BlockBase.BlockData::getBlock, Block::getBlockData).listOf().fieldOf("blocks").xmap(DefinedStructureProcessorBlockIgnore::new, (blockIgnoreProcessor) -> {
        return blockIgnoreProcessor.toIgnore;
    }).codec();
    public static final DefinedStructureProcessorBlockIgnore STRUCTURE_BLOCK = new DefinedStructureProcessorBlockIgnore(ImmutableList.of(Blocks.STRUCTURE_BLOCK));
    public static final DefinedStructureProcessorBlockIgnore AIR = new DefinedStructureProcessorBlockIgnore(ImmutableList.of(Blocks.AIR));
    public static final DefinedStructureProcessorBlockIgnore STRUCTURE_AND_AIR = new DefinedStructureProcessorBlockIgnore(ImmutableList.of(Blocks.AIR, Blocks.STRUCTURE_BLOCK));
    private final ImmutableList<Block> toIgnore;

    public DefinedStructureProcessorBlockIgnore(List<Block> blocks) {
        this.toIgnore = ImmutableList.copyOf(blocks);
    }

    @Nullable
    @Override
    public DefinedStructure.BlockInfo processBlock(IWorldReader world, BlockPosition pos, BlockPosition pivot, DefinedStructure.BlockInfo structureBlockInfo, DefinedStructure.BlockInfo structureBlockInfo2, DefinedStructureInfo data) {
        return this.toIgnore.contains(structureBlockInfo2.state.getBlock()) ? null : structureBlockInfo2;
    }

    @Override
    protected DefinedStructureStructureProcessorType<?> getType() {
        return DefinedStructureStructureProcessorType.BLOCK_IGNORE;
    }
}
