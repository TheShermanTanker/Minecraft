package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.Codec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.levelgen.feature.WorldGenerator;

public class ProtectedBlockProcessor extends DefinedStructureProcessor {
    public final MinecraftKey cannotReplace;
    public static final Codec<ProtectedBlockProcessor> CODEC = MinecraftKey.CODEC.xmap(ProtectedBlockProcessor::new, (protectedBlockProcessor) -> {
        return protectedBlockProcessor.cannotReplace;
    });

    public ProtectedBlockProcessor(MinecraftKey protectedBlocksTag) {
        this.cannotReplace = protectedBlocksTag;
    }

    @Nullable
    @Override
    public DefinedStructure.BlockInfo processBlock(IWorldReader world, BlockPosition pos, BlockPosition pivot, DefinedStructure.BlockInfo structureBlockInfo, DefinedStructure.BlockInfo structureBlockInfo2, DefinedStructureInfo data) {
        return WorldGenerator.isReplaceable(this.cannotReplace).test(world.getType(structureBlockInfo2.pos)) ? structureBlockInfo2 : null;
    }

    @Override
    protected DefinedStructureStructureProcessorType<?> getType() {
        return DefinedStructureStructureProcessorType.PROTECTED_BLOCKS;
    }
}
