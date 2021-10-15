package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.Codec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class DefinedStructureProcessorLavaSubmergedBlock extends DefinedStructureProcessor {
    public static final Codec<DefinedStructureProcessorLavaSubmergedBlock> CODEC;
    public static final DefinedStructureProcessorLavaSubmergedBlock INSTANCE = new DefinedStructureProcessorLavaSubmergedBlock();

    @Nullable
    @Override
    public DefinedStructure.BlockInfo processBlock(IWorldReader world, BlockPosition pos, BlockPosition pivot, DefinedStructure.BlockInfo structureBlockInfo, DefinedStructure.BlockInfo structureBlockInfo2, DefinedStructureInfo data) {
        BlockPosition blockPos = structureBlockInfo2.pos;
        boolean bl = world.getType(blockPos).is(Blocks.LAVA);
        return bl && !Block.isShapeFullBlock(structureBlockInfo2.state.getShape(world, blockPos)) ? new DefinedStructure.BlockInfo(blockPos, Blocks.LAVA.getBlockData(), structureBlockInfo2.nbt) : structureBlockInfo2;
    }

    @Override
    protected DefinedStructureStructureProcessorType<?> getType() {
        return DefinedStructureStructureProcessorType.LAVA_SUBMERGED_BLOCK;
    }

    static {
        CODEC = Codec.unit(() -> {
            return INSTANCE;
        });
    }
}
