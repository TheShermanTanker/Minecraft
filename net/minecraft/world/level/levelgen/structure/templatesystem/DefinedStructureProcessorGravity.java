package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.levelgen.HeightMap;

public class DefinedStructureProcessorGravity extends DefinedStructureProcessor {
    public static final Codec<DefinedStructureProcessorGravity> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(HeightMap.Type.CODEC.fieldOf("heightmap").orElse(HeightMap.Type.WORLD_SURFACE_WG).forGetter((gravityProcessor) -> {
            return gravityProcessor.heightmap;
        }), Codec.INT.fieldOf("offset").orElse(0).forGetter((gravityProcessor) -> {
            return gravityProcessor.offset;
        })).apply(instance, DefinedStructureProcessorGravity::new);
    });
    private final HeightMap.Type heightmap;
    private final int offset;

    public DefinedStructureProcessorGravity(HeightMap.Type heightmap, int offset) {
        this.heightmap = heightmap;
        this.offset = offset;
    }

    @Nullable
    @Override
    public DefinedStructure.BlockInfo processBlock(IWorldReader world, BlockPosition pos, BlockPosition pivot, DefinedStructure.BlockInfo structureBlockInfo, DefinedStructure.BlockInfo structureBlockInfo2, DefinedStructureInfo data) {
        HeightMap.Type types;
        if (world instanceof WorldServer) {
            if (this.heightmap == HeightMap.Type.WORLD_SURFACE_WG) {
                types = HeightMap.Type.WORLD_SURFACE;
            } else if (this.heightmap == HeightMap.Type.OCEAN_FLOOR_WG) {
                types = HeightMap.Type.OCEAN_FLOOR;
            } else {
                types = this.heightmap;
            }
        } else {
            types = this.heightmap;
        }

        int i = world.getHeight(types, structureBlockInfo2.pos.getX(), structureBlockInfo2.pos.getZ()) + this.offset;
        int j = structureBlockInfo.pos.getY();
        return new DefinedStructure.BlockInfo(new BlockPosition(structureBlockInfo2.pos.getX(), i + j, structureBlockInfo2.pos.getZ()), structureBlockInfo2.state, structureBlockInfo2.nbt);
    }

    @Override
    protected DefinedStructureStructureProcessorType<?> getType() {
        return DefinedStructureStructureProcessorType.GRAVITY;
    }
}
