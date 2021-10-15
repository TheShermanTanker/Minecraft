package net.minecraft.world.level.levelgen.feature.structures;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.BaseBlockPosition;
import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.EnumBlockRotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.StructureBoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureManager;

public class WorldGenFeatureDefinedStructurePoolList extends WorldGenFeatureDefinedStructurePoolStructure {
    public static final Codec<WorldGenFeatureDefinedStructurePoolList> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(WorldGenFeatureDefinedStructurePoolStructure.CODEC.listOf().fieldOf("elements").forGetter((listPoolElement) -> {
            return listPoolElement.elements;
        }), projectionCodec()).apply(instance, WorldGenFeatureDefinedStructurePoolList::new);
    });
    private final List<WorldGenFeatureDefinedStructurePoolStructure> elements;

    public WorldGenFeatureDefinedStructurePoolList(List<WorldGenFeatureDefinedStructurePoolStructure> elements, WorldGenFeatureDefinedStructurePoolTemplate.Matching projection) {
        super(projection);
        if (elements.isEmpty()) {
            throw new IllegalArgumentException("Elements are empty");
        } else {
            this.elements = elements;
            this.setProjectionOnEachElement(projection);
        }
    }

    @Override
    public BaseBlockPosition getSize(DefinedStructureManager structureManager, EnumBlockRotation rotation) {
        int i = 0;
        int j = 0;
        int k = 0;

        for(WorldGenFeatureDefinedStructurePoolStructure structurePoolElement : this.elements) {
            BaseBlockPosition vec3i = structurePoolElement.getSize(structureManager, rotation);
            i = Math.max(i, vec3i.getX());
            j = Math.max(j, vec3i.getY());
            k = Math.max(k, vec3i.getZ());
        }

        return new BaseBlockPosition(i, j, k);
    }

    @Override
    public List<DefinedStructure.BlockInfo> getShuffledJigsawBlocks(DefinedStructureManager structureManager, BlockPosition pos, EnumBlockRotation rotation, Random random) {
        return this.elements.get(0).getShuffledJigsawBlocks(structureManager, pos, rotation, random);
    }

    @Override
    public StructureBoundingBox getBoundingBox(DefinedStructureManager structureManager, BlockPosition pos, EnumBlockRotation rotation) {
        Stream<StructureBoundingBox> stream = this.elements.stream().filter((structurePoolElement) -> {
            return structurePoolElement != WorldGenFeatureDefinedStructurePoolEmpty.INSTANCE;
        }).map((structurePoolElement) -> {
            return structurePoolElement.getBoundingBox(structureManager, pos, rotation);
        });
        return StructureBoundingBox.encapsulatingBoxes(stream::iterator).orElseThrow(() -> {
            return new IllegalStateException("Unable to calculate boundingbox for ListPoolElement");
        });
    }

    @Override
    public boolean place(DefinedStructureManager structureManager, GeneratorAccessSeed world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, BlockPosition pos, BlockPosition blockPos, EnumBlockRotation rotation, StructureBoundingBox box, Random random, boolean keepJigsaws) {
        for(WorldGenFeatureDefinedStructurePoolStructure structurePoolElement : this.elements) {
            if (!structurePoolElement.place(structureManager, world, structureAccessor, chunkGenerator, pos, blockPos, rotation, box, random, keepJigsaws)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public WorldGenFeatureDefinedStructurePools<?> getType() {
        return WorldGenFeatureDefinedStructurePools.LIST;
    }

    @Override
    public WorldGenFeatureDefinedStructurePoolStructure setProjection(WorldGenFeatureDefinedStructurePoolTemplate.Matching projection) {
        super.setProjection(projection);
        this.setProjectionOnEachElement(projection);
        return this;
    }

    @Override
    public String toString() {
        return "List[" + (String)this.elements.stream().map(Object::toString).collect(Collectors.joining(", ")) + "]";
    }

    private void setProjectionOnEachElement(WorldGenFeatureDefinedStructurePoolTemplate.Matching projection) {
        this.elements.forEach((structurePoolElement) -> {
            structurePoolElement.setProjection(projection);
        });
    }
}
