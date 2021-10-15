package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.Codec;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.IWorldReader;

public class DefinedStructureProcessorRotation extends DefinedStructureProcessor {
    public static final Codec<DefinedStructureProcessorRotation> CODEC = Codec.FLOAT.fieldOf("integrity").orElse(1.0F).xmap(DefinedStructureProcessorRotation::new, (blockRotProcessor) -> {
        return blockRotProcessor.integrity;
    }).codec();
    private final float integrity;

    public DefinedStructureProcessorRotation(float integrity) {
        this.integrity = integrity;
    }

    @Nullable
    @Override
    public DefinedStructure.BlockInfo processBlock(IWorldReader world, BlockPosition pos, BlockPosition pivot, DefinedStructure.BlockInfo structureBlockInfo, DefinedStructure.BlockInfo structureBlockInfo2, DefinedStructureInfo data) {
        Random random = data.getRandom(structureBlockInfo2.pos);
        return !(this.integrity >= 1.0F) && !(random.nextFloat() <= this.integrity) ? null : structureBlockInfo2;
    }

    @Override
    protected DefinedStructureStructureProcessorType<?> getType() {
        return DefinedStructureStructureProcessorType.BLOCK_ROT;
    }
}
