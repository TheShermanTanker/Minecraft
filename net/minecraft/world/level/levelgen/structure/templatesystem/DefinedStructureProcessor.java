package net.minecraft.world.level.levelgen.structure.templatesystem;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.IWorldReader;

public abstract class DefinedStructureProcessor {
    @Nullable
    public abstract DefinedStructure.BlockInfo processBlock(IWorldReader world, BlockPosition pos, BlockPosition pivot, DefinedStructure.BlockInfo structureBlockInfo, DefinedStructure.BlockInfo structureBlockInfo2, DefinedStructureInfo data);

    protected abstract DefinedStructureStructureProcessorType<?> getType();
}
