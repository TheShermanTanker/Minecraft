package net.minecraft.world.level.levelgen.feature.structures;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrays;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.util.INamable;
import net.minecraft.world.level.block.EnumBlockRotation;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureProcessorGravity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WorldGenFeatureDefinedStructurePoolTemplate {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int SIZE_UNSET = Integer.MIN_VALUE;
    public static final Codec<WorldGenFeatureDefinedStructurePoolTemplate> DIRECT_CODEC;
    public static final Codec<Supplier<WorldGenFeatureDefinedStructurePoolTemplate>> CODEC = RegistryFileCodec.create(IRegistry.TEMPLATE_POOL_REGISTRY, DIRECT_CODEC);
    private final MinecraftKey name;
    private final List<Pair<WorldGenFeatureDefinedStructurePoolStructure, Integer>> rawTemplates;
    private final List<WorldGenFeatureDefinedStructurePoolStructure> templates;
    private final MinecraftKey fallback;
    private int maxSize = Integer.MIN_VALUE;

    public WorldGenFeatureDefinedStructurePoolTemplate(MinecraftKey id, MinecraftKey terminatorsId, List<Pair<WorldGenFeatureDefinedStructurePoolStructure, Integer>> elementCounts) {
        this.name = id;
        this.rawTemplates = elementCounts;
        this.templates = Lists.newArrayList();

        for(Pair<WorldGenFeatureDefinedStructurePoolStructure, Integer> pair : elementCounts) {
            WorldGenFeatureDefinedStructurePoolStructure structurePoolElement = pair.getFirst();

            for(int i = 0; i < pair.getSecond(); ++i) {
                this.templates.add(structurePoolElement);
            }
        }

        this.fallback = terminatorsId;
    }

    public WorldGenFeatureDefinedStructurePoolTemplate(MinecraftKey id, MinecraftKey terminatorsId, List<Pair<Function<WorldGenFeatureDefinedStructurePoolTemplate.Matching, ? extends WorldGenFeatureDefinedStructurePoolStructure>, Integer>> elementCounts, WorldGenFeatureDefinedStructurePoolTemplate.Matching projection) {
        this.name = id;
        this.rawTemplates = Lists.newArrayList();
        this.templates = Lists.newArrayList();

        for(Pair<Function<WorldGenFeatureDefinedStructurePoolTemplate.Matching, ? extends WorldGenFeatureDefinedStructurePoolStructure>, Integer> pair : elementCounts) {
            WorldGenFeatureDefinedStructurePoolStructure structurePoolElement = pair.getFirst().apply(projection);
            this.rawTemplates.add(Pair.of(structurePoolElement, pair.getSecond()));

            for(int i = 0; i < pair.getSecond(); ++i) {
                this.templates.add(structurePoolElement);
            }
        }

        this.fallback = terminatorsId;
    }

    public int getMaxSize(DefinedStructureManager structureManager) {
        if (this.maxSize == Integer.MIN_VALUE) {
            this.maxSize = this.templates.stream().filter((structurePoolElement) -> {
                return structurePoolElement != WorldGenFeatureDefinedStructurePoolEmpty.INSTANCE;
            }).mapToInt((element) -> {
                return element.getBoundingBox(structureManager, BlockPosition.ZERO, EnumBlockRotation.NONE).getYSpan();
            }).max().orElse(0);
        }

        return this.maxSize;
    }

    public MinecraftKey getFallback() {
        return this.fallback;
    }

    public WorldGenFeatureDefinedStructurePoolStructure getRandomTemplate(Random random) {
        return this.templates.get(random.nextInt(this.templates.size()));
    }

    public List<WorldGenFeatureDefinedStructurePoolStructure> getShuffledTemplates(Random random) {
        return ImmutableList.copyOf(ObjectArrays.shuffle(this.templates.toArray(new WorldGenFeatureDefinedStructurePoolStructure[0]), random));
    }

    public MinecraftKey getName() {
        return this.name;
    }

    public int size() {
        return this.templates.size();
    }

    static {
        DIRECT_CODEC = RecordCodecBuilder.create((instance) -> {
            return instance.group(MinecraftKey.CODEC.fieldOf("name").forGetter(WorldGenFeatureDefinedStructurePoolTemplate::getName), MinecraftKey.CODEC.fieldOf("fallback").forGetter(WorldGenFeatureDefinedStructurePoolTemplate::getFallback), Codec.mapPair(WorldGenFeatureDefinedStructurePoolStructure.CODEC.fieldOf("element"), Codec.intRange(1, 150).fieldOf("weight")).codec().listOf().fieldOf("elements").forGetter((structureTemplatePool) -> {
                return structureTemplatePool.rawTemplates;
            })).apply(instance, WorldGenFeatureDefinedStructurePoolTemplate::new);
        });
    }

    public static enum Matching implements INamable {
        TERRAIN_MATCHING("terrain_matching", ImmutableList.of(new DefinedStructureProcessorGravity(HeightMap.Type.WORLD_SURFACE_WG, -1))),
        RIGID("rigid", ImmutableList.of());

        public static final Codec<WorldGenFeatureDefinedStructurePoolTemplate.Matching> CODEC = INamable.fromEnum(WorldGenFeatureDefinedStructurePoolTemplate.Matching::values, WorldGenFeatureDefinedStructurePoolTemplate.Matching::byName);
        private static final Map<String, WorldGenFeatureDefinedStructurePoolTemplate.Matching> BY_NAME = Arrays.stream(values()).collect(Collectors.toMap(WorldGenFeatureDefinedStructurePoolTemplate.Matching::getName, (projection) -> {
            return projection;
        }));
        private final String name;
        private final ImmutableList<DefinedStructureProcessor> processors;

        private Matching(String id, ImmutableList<DefinedStructureProcessor> processors) {
            this.name = id;
            this.processors = processors;
        }

        public String getName() {
            return this.name;
        }

        public static WorldGenFeatureDefinedStructurePoolTemplate.Matching byName(String id) {
            return BY_NAME.get(id);
        }

        public ImmutableList<DefinedStructureProcessor> getProcessors() {
            return this.processors;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}
