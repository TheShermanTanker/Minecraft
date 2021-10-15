package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import javax.annotation.Nullable;
import net.minecraft.commands.arguments.blocks.ArgumentBlock;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;

public class DefinedStructureProcessorJigsawReplacement extends DefinedStructureProcessor {
    public static final Codec<DefinedStructureProcessorJigsawReplacement> CODEC;
    public static final DefinedStructureProcessorJigsawReplacement INSTANCE = new DefinedStructureProcessorJigsawReplacement();

    private DefinedStructureProcessorJigsawReplacement() {
    }

    @Nullable
    @Override
    public DefinedStructure.BlockInfo processBlock(IWorldReader world, BlockPosition pos, BlockPosition pivot, DefinedStructure.BlockInfo structureBlockInfo, DefinedStructure.BlockInfo structureBlockInfo2, DefinedStructureInfo data) {
        IBlockData blockState = structureBlockInfo2.state;
        if (blockState.is(Blocks.JIGSAW)) {
            String string = structureBlockInfo2.nbt.getString("final_state");
            ArgumentBlock blockStateParser = new ArgumentBlock(new StringReader(string), false);

            try {
                blockStateParser.parse(true);
            } catch (CommandSyntaxException var11) {
                throw new RuntimeException(var11);
            }

            return blockStateParser.getBlockData().is(Blocks.STRUCTURE_VOID) ? null : new DefinedStructure.BlockInfo(structureBlockInfo2.pos, blockStateParser.getBlockData(), (NBTTagCompound)null);
        } else {
            return structureBlockInfo2;
        }
    }

    @Override
    protected DefinedStructureStructureProcessorType<?> getType() {
        return DefinedStructureStructureProcessorType.JIGSAW_REPLACEMENT;
    }

    static {
        CODEC = Codec.unit(() -> {
            return INSTANCE;
        });
    }
}
