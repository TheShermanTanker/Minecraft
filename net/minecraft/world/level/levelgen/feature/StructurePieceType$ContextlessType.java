package net.minecraft.world.level.levelgen.feature;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public interface StructurePieceType$ContextlessType extends WorldGenFeatureStructurePieceType {
    StructurePiece load(NBTTagCompound nbt);

    @Override
    default StructurePiece load(StructurePieceSerializationContext context, NBTTagCompound nbt) {
        return this.load(nbt);
    }
}
