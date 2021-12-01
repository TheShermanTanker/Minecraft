package net.minecraft.world.level.levelgen.feature.structures;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import net.minecraft.core.BaseBlockPosition;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.BlockPropertyJigsawOrientation;
import net.minecraft.core.EnumDirection;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.BlockJigsaw;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnumBlockRotation;
import net.minecraft.world.level.block.entity.TileEntityJigsaw;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.StructureBoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureManager;

public class WorldGenFeatureDefinedStructurePoolFeature extends WorldGenFeatureDefinedStructurePoolStructure {
    public static final Codec<WorldGenFeatureDefinedStructurePoolFeature> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(PlacedFeature.CODEC.fieldOf("feature").forGetter((featurePoolElement) -> {
            return featurePoolElement.feature;
        }), projectionCodec()).apply(instance, WorldGenFeatureDefinedStructurePoolFeature::new);
    });
    private final Supplier<PlacedFeature> feature;
    private final NBTTagCompound defaultJigsawNBT;

    protected WorldGenFeatureDefinedStructurePoolFeature(Supplier<PlacedFeature> feature, WorldGenFeatureDefinedStructurePoolTemplate.Matching projection) {
        super(projection);
        this.feature = feature;
        this.defaultJigsawNBT = this.fillDefaultJigsawNBT();
    }

    private NBTTagCompound fillDefaultJigsawNBT() {
        NBTTagCompound compoundTag = new NBTTagCompound();
        compoundTag.setString("name", "minecraft:bottom");
        compoundTag.setString("final_state", "minecraft:air");
        compoundTag.setString("pool", "minecraft:empty");
        compoundTag.setString("target", "minecraft:empty");
        compoundTag.setString("joint", TileEntityJigsaw.JointType.ROLLABLE.getSerializedName());
        return compoundTag;
    }

    @Override
    public BaseBlockPosition getSize(DefinedStructureManager structureManager, EnumBlockRotation rotation) {
        return BaseBlockPosition.ZERO;
    }

    @Override
    public List<DefinedStructure.BlockInfo> getShuffledJigsawBlocks(DefinedStructureManager structureManager, BlockPosition pos, EnumBlockRotation rotation, Random random) {
        List<DefinedStructure.BlockInfo> list = Lists.newArrayList();
        list.add(new DefinedStructure.BlockInfo(pos, Blocks.JIGSAW.getBlockData().set(BlockJigsaw.ORIENTATION, BlockPropertyJigsawOrientation.fromFrontAndTop(EnumDirection.DOWN, EnumDirection.SOUTH)), this.defaultJigsawNBT));
        return list;
    }

    @Override
    public StructureBoundingBox getBoundingBox(DefinedStructureManager structureManager, BlockPosition pos, EnumBlockRotation rotation) {
        BaseBlockPosition vec3i = this.getSize(structureManager, rotation);
        return new StructureBoundingBox(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + vec3i.getX(), pos.getY() + vec3i.getY(), pos.getZ() + vec3i.getZ());
    }

    @Override
    public boolean place(DefinedStructureManager structureManager, GeneratorAccessSeed world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, BlockPosition pos, BlockPosition blockPos, EnumBlockRotation rotation, StructureBoundingBox box, Random random, boolean keepJigsaws) {
        return this.feature.get().place(world, chunkGenerator, random, pos);
    }

    @Override
    public WorldGenFeatureDefinedStructurePools<?> getType() {
        return WorldGenFeatureDefinedStructurePools.FEATURE;
    }

    @Override
    public String toString() {
        return "Feature[" + this.feature.get() + "]";
    }
}
