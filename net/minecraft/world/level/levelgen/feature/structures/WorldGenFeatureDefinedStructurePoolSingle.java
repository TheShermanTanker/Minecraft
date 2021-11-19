package net.minecraft.world.level.levelgen.feature.structures;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.BaseBlockPosition;
import net.minecraft.core.BlockPosition;
import net.minecraft.data.worldgen.WorldGenProcessorLists;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnumBlockRotation;
import net.minecraft.world.level.block.state.properties.BlockPropertyStructureMode;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.StructureBoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureInfo;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureProcessorBlockIgnore;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureProcessorJigsawReplacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureStructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.ProcessorList;

public class WorldGenFeatureDefinedStructurePoolSingle extends WorldGenFeatureDefinedStructurePoolStructure {
    private static final Codec<Either<MinecraftKey, DefinedStructure>> TEMPLATE_CODEC = Codec.of(WorldGenFeatureDefinedStructurePoolSingle::encodeTemplate, MinecraftKey.CODEC.map(Either::left));
    public static final Codec<WorldGenFeatureDefinedStructurePoolSingle> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(templateCodec(), processorsCodec(), projectionCodec()).apply(instance, WorldGenFeatureDefinedStructurePoolSingle::new);
    });
    protected final Either<MinecraftKey, DefinedStructure> template;
    protected final Supplier<ProcessorList> processors;

    private static <T> DataResult<T> encodeTemplate(Either<MinecraftKey, DefinedStructure> either, DynamicOps<T> dynamicOps, T object) {
        Optional<MinecraftKey> optional = either.left();
        return !optional.isPresent() ? DataResult.error("Can not serialize a runtime pool element") : MinecraftKey.CODEC.encode(optional.get(), dynamicOps, object);
    }

    protected static <E extends WorldGenFeatureDefinedStructurePoolSingle> RecordCodecBuilder<E, Supplier<ProcessorList>> processorsCodec() {
        return DefinedStructureStructureProcessorType.LIST_CODEC.fieldOf("processors").forGetter((singlePoolElement) -> {
            return singlePoolElement.processors;
        });
    }

    protected static <E extends WorldGenFeatureDefinedStructurePoolSingle> RecordCodecBuilder<E, Either<MinecraftKey, DefinedStructure>> templateCodec() {
        return TEMPLATE_CODEC.fieldOf("location").forGetter((singlePoolElement) -> {
            return singlePoolElement.template;
        });
    }

    protected WorldGenFeatureDefinedStructurePoolSingle(Either<MinecraftKey, DefinedStructure> location, Supplier<ProcessorList> processors, WorldGenFeatureDefinedStructurePoolTemplate.Matching projection) {
        super(projection);
        this.template = location;
        this.processors = processors;
    }

    public WorldGenFeatureDefinedStructurePoolSingle(DefinedStructure structure) {
        this(Either.right(structure), () -> {
            return WorldGenProcessorLists.EMPTY;
        }, WorldGenFeatureDefinedStructurePoolTemplate.Matching.RIGID);
    }

    @Override
    public BaseBlockPosition getSize(DefinedStructureManager structureManager, EnumBlockRotation rotation) {
        DefinedStructure structureTemplate = this.getTemplate(structureManager);
        return structureTemplate.getSize(rotation);
    }

    private DefinedStructure getTemplate(DefinedStructureManager structureManager) {
        return this.template.map(structureManager::getOrCreate, Function.identity());
    }

    public List<DefinedStructure.BlockInfo> getDataMarkers(DefinedStructureManager structureManager, BlockPosition pos, EnumBlockRotation rotation, boolean mirroredAndRotated) {
        DefinedStructure structureTemplate = this.getTemplate(structureManager);
        List<DefinedStructure.BlockInfo> list = structureTemplate.filterBlocks(pos, (new DefinedStructureInfo()).setRotation(rotation), Blocks.STRUCTURE_BLOCK, mirroredAndRotated);
        List<DefinedStructure.BlockInfo> list2 = Lists.newArrayList();

        for(DefinedStructure.BlockInfo structureBlockInfo : list) {
            if (structureBlockInfo.nbt != null) {
                BlockPropertyStructureMode structureMode = BlockPropertyStructureMode.valueOf(structureBlockInfo.nbt.getString("mode"));
                if (structureMode == BlockPropertyStructureMode.DATA) {
                    list2.add(structureBlockInfo);
                }
            }
        }

        return list2;
    }

    @Override
    public List<DefinedStructure.BlockInfo> getShuffledJigsawBlocks(DefinedStructureManager structureManager, BlockPosition pos, EnumBlockRotation rotation, Random random) {
        DefinedStructure structureTemplate = this.getTemplate(structureManager);
        List<DefinedStructure.BlockInfo> list = structureTemplate.filterBlocks(pos, (new DefinedStructureInfo()).setRotation(rotation), Blocks.JIGSAW, true);
        Collections.shuffle(list, random);
        return list;
    }

    @Override
    public StructureBoundingBox getBoundingBox(DefinedStructureManager structureManager, BlockPosition pos, EnumBlockRotation rotation) {
        DefinedStructure structureTemplate = this.getTemplate(structureManager);
        return structureTemplate.getBoundingBox((new DefinedStructureInfo()).setRotation(rotation), pos);
    }

    @Override
    public boolean place(DefinedStructureManager structureManager, GeneratorAccessSeed world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, BlockPosition pos, BlockPosition blockPos, EnumBlockRotation rotation, StructureBoundingBox box, Random random, boolean keepJigsaws) {
        DefinedStructure structureTemplate = this.getTemplate(structureManager);
        DefinedStructureInfo structurePlaceSettings = this.getSettings(rotation, box, keepJigsaws);
        if (!structureTemplate.placeInWorld(world, pos, blockPos, structurePlaceSettings, random, 18)) {
            return false;
        } else {
            for(DefinedStructure.BlockInfo structureBlockInfo : DefinedStructure.processBlockInfos(world, pos, blockPos, structurePlaceSettings, this.getDataMarkers(structureManager, pos, rotation, false))) {
                this.handleDataMarker(world, structureBlockInfo, pos, rotation, random, box);
            }

            return true;
        }
    }

    protected DefinedStructureInfo getSettings(EnumBlockRotation rotation, StructureBoundingBox box, boolean keepJigsaws) {
        DefinedStructureInfo structurePlaceSettings = new DefinedStructureInfo();
        structurePlaceSettings.setBoundingBox(box);
        structurePlaceSettings.setRotation(rotation);
        structurePlaceSettings.setKnownShape(true);
        structurePlaceSettings.setIgnoreEntities(false);
        structurePlaceSettings.addProcessor(DefinedStructureProcessorBlockIgnore.STRUCTURE_BLOCK);
        structurePlaceSettings.setFinalizeEntities(true);
        if (!keepJigsaws) {
            structurePlaceSettings.addProcessor(DefinedStructureProcessorJigsawReplacement.INSTANCE);
        }

        this.processors.get().list().forEach(structurePlaceSettings::addProcessor);
        this.getProjection().getProcessors().forEach(structurePlaceSettings::addProcessor);
        return structurePlaceSettings;
    }

    @Override
    public WorldGenFeatureDefinedStructurePools<?> getType() {
        return WorldGenFeatureDefinedStructurePools.SINGLE;
    }

    @Override
    public String toString() {
        return "Single[" + this.template + "]";
    }
}
