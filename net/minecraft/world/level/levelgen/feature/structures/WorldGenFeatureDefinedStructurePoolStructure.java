package net.minecraft.world.level.levelgen.feature.structures;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.core.BaseBlockPosition;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.data.worldgen.WorldGenProcessorLists;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.EnumBlockRotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureConfigured;
import net.minecraft.world.level.levelgen.structure.StructureBoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.ProcessorList;

public abstract class WorldGenFeatureDefinedStructurePoolStructure {
    public static final Codec<WorldGenFeatureDefinedStructurePoolStructure> CODEC = IRegistry.STRUCTURE_POOL_ELEMENT.dispatch("element_type", WorldGenFeatureDefinedStructurePoolStructure::getType, WorldGenFeatureDefinedStructurePools::codec);
    @Nullable
    private volatile WorldGenFeatureDefinedStructurePoolTemplate.Matching projection;

    protected static <E extends WorldGenFeatureDefinedStructurePoolStructure> RecordCodecBuilder<E, WorldGenFeatureDefinedStructurePoolTemplate.Matching> projectionCodec() {
        return WorldGenFeatureDefinedStructurePoolTemplate.Matching.CODEC.fieldOf("projection").forGetter(WorldGenFeatureDefinedStructurePoolStructure::getProjection);
    }

    protected WorldGenFeatureDefinedStructurePoolStructure(WorldGenFeatureDefinedStructurePoolTemplate.Matching projection) {
        this.projection = projection;
    }

    public abstract BaseBlockPosition getSize(DefinedStructureManager structureManager, EnumBlockRotation rotation);

    public abstract List<DefinedStructure.BlockInfo> getShuffledJigsawBlocks(DefinedStructureManager structureManager, BlockPosition pos, EnumBlockRotation rotation, Random random);

    public abstract StructureBoundingBox getBoundingBox(DefinedStructureManager structureManager, BlockPosition pos, EnumBlockRotation rotation);

    public abstract boolean place(DefinedStructureManager structureManager, GeneratorAccessSeed world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, BlockPosition pos, BlockPosition blockPos, EnumBlockRotation rotation, StructureBoundingBox box, Random random, boolean keepJigsaws);

    public abstract WorldGenFeatureDefinedStructurePools<?> getType();

    public void handleDataMarker(GeneratorAccess levelAccessor, DefinedStructure.BlockInfo structureBlockInfo, BlockPosition blockPos, EnumBlockRotation rotation, Random random, StructureBoundingBox boundingBox) {
    }

    public WorldGenFeatureDefinedStructurePoolStructure setProjection(WorldGenFeatureDefinedStructurePoolTemplate.Matching projection) {
        this.projection = projection;
        return this;
    }

    public WorldGenFeatureDefinedStructurePoolTemplate.Matching getProjection() {
        WorldGenFeatureDefinedStructurePoolTemplate.Matching projection = this.projection;
        if (projection == null) {
            throw new IllegalStateException();
        } else {
            return projection;
        }
    }

    public int getGroundLevelDelta() {
        return 1;
    }

    public static Function<WorldGenFeatureDefinedStructurePoolTemplate.Matching, WorldGenFeatureDefinedStructurePoolEmpty> empty() {
        return (projection) -> {
            return WorldGenFeatureDefinedStructurePoolEmpty.INSTANCE;
        };
    }

    public static Function<WorldGenFeatureDefinedStructurePoolTemplate.Matching, WorldGenFeatureDefinedStructurePoolLegacySingle> legacy(String id) {
        return (projection) -> {
            return new WorldGenFeatureDefinedStructurePoolLegacySingle(Either.left(new MinecraftKey(id)), () -> {
                return WorldGenProcessorLists.EMPTY;
            }, projection);
        };
    }

    public static Function<WorldGenFeatureDefinedStructurePoolTemplate.Matching, WorldGenFeatureDefinedStructurePoolLegacySingle> legacy(String id, ProcessorList processors) {
        return (projection) -> {
            return new WorldGenFeatureDefinedStructurePoolLegacySingle(Either.left(new MinecraftKey(id)), () -> {
                return processors;
            }, projection);
        };
    }

    public static Function<WorldGenFeatureDefinedStructurePoolTemplate.Matching, WorldGenFeatureDefinedStructurePoolSingle> single(String id) {
        return (projection) -> {
            return new WorldGenFeatureDefinedStructurePoolSingle(Either.left(new MinecraftKey(id)), () -> {
                return WorldGenProcessorLists.EMPTY;
            }, projection);
        };
    }

    public static Function<WorldGenFeatureDefinedStructurePoolTemplate.Matching, WorldGenFeatureDefinedStructurePoolSingle> single(String id, ProcessorList processors) {
        return (projection) -> {
            return new WorldGenFeatureDefinedStructurePoolSingle(Either.left(new MinecraftKey(id)), () -> {
                return processors;
            }, projection);
        };
    }

    public static Function<WorldGenFeatureDefinedStructurePoolTemplate.Matching, WorldGenFeatureDefinedStructurePoolFeature> feature(WorldGenFeatureConfigured<?, ?> processors) {
        return (projection) -> {
            return new WorldGenFeatureDefinedStructurePoolFeature(() -> {
                return processors;
            }, projection);
        };
    }

    public static Function<WorldGenFeatureDefinedStructurePoolTemplate.Matching, WorldGenFeatureDefinedStructurePoolList> list(List<Function<WorldGenFeatureDefinedStructurePoolTemplate.Matching, ? extends WorldGenFeatureDefinedStructurePoolStructure>> list) {
        return (projection) -> {
            return new WorldGenFeatureDefinedStructurePoolList(list.stream().map((function) -> {
                return function.apply(projection);
            }).collect(Collectors.toList()), projection);
        };
    }
}
