package net.minecraft.world.level.levelgen.feature;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureManager;

public interface StructurePieceType$StructureTemplateType extends WorldGenFeatureStructurePieceType {
    StructurePiece load(DefinedStructureManager structureManager, NBTTagCompound nbt);

    @Override
    default StructurePiece load(StructurePieceSerializationContext context, NBTTagCompound nbt) {
        return this.load(context.structureManager(), nbt);
    }
}
