package net.minecraft.world.level.levelgen.structure;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.nbt.DynamicOpsNBT;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockLeaves;
import net.minecraft.world.level.block.BlockVine;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnumBlockMirror;
import net.minecraft.world.level.block.EnumBlockRotation;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureStructurePieceType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureInfo;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureProcessorBlackstoneReplace;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureProcessorBlockAge;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureProcessorBlockIgnore;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureProcessorLavaSubmergedBlock;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureProcessorPredicates;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureProcessorRule;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureTestBlock;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureTestRandomBlock;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureTestTrue;
import net.minecraft.world.level.levelgen.structure.templatesystem.ProtectedBlockProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WorldGenFeatureRuinedPortalPieces extends DefinedStructurePiece {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final float PROBABILITY_OF_GOLD_GONE = 0.3F;
    private static final float PROBABILITY_OF_MAGMA_INSTEAD_OF_NETHERRACK = 0.07F;
    private static final float PROBABILITY_OF_MAGMA_INSTEAD_OF_LAVA = 0.2F;
    private static final float DEFAULT_MOSSINESS = 0.2F;
    private final WorldGenFeatureRuinedPortalPieces.Position verticalPlacement;
    private final WorldGenFeatureRuinedPortalPieces.Properties properties;

    public WorldGenFeatureRuinedPortalPieces(DefinedStructureManager manager, BlockPosition pos, WorldGenFeatureRuinedPortalPieces.Position verticalPlacement, WorldGenFeatureRuinedPortalPieces.Properties properties, MinecraftKey id, DefinedStructure structure, EnumBlockRotation rotation, EnumBlockMirror mirror, BlockPosition blockPos) {
        super(WorldGenFeatureStructurePieceType.RUINED_PORTAL, 0, manager, id, id.toString(), makeSettings(mirror, rotation, verticalPlacement, blockPos, properties), pos);
        this.verticalPlacement = verticalPlacement;
        this.properties = properties;
    }

    public WorldGenFeatureRuinedPortalPieces(DefinedStructureManager manager, NBTTagCompound nbt) {
        super(WorldGenFeatureStructurePieceType.RUINED_PORTAL, nbt, manager, (id) -> {
            return makeSettings(manager, nbt, id);
        });
        this.verticalPlacement = WorldGenFeatureRuinedPortalPieces.Position.byName(nbt.getString("VerticalPlacement"));
        this.properties = WorldGenFeatureRuinedPortalPieces.Properties.CODEC.parse(new Dynamic<>(DynamicOpsNBT.INSTANCE, nbt.get("Properties"))).getOrThrow(true, LOGGER::error);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, NBTTagCompound nbt) {
        super.addAdditionalSaveData(context, nbt);
        nbt.setString("Rotation", this.placeSettings.getRotation().name());
        nbt.setString("Mirror", this.placeSettings.getMirror().name());
        nbt.setString("VerticalPlacement", this.verticalPlacement.getName());
        WorldGenFeatureRuinedPortalPieces.Properties.CODEC.encodeStart(DynamicOpsNBT.INSTANCE, this.properties).resultOrPartial(LOGGER::error).ifPresent((tag) -> {
            nbt.set("Properties", tag);
        });
    }

    private static DefinedStructureInfo makeSettings(DefinedStructureManager manager, NBTTagCompound nbt, MinecraftKey id) {
        DefinedStructure structureTemplate = manager.getOrCreate(id);
        BlockPosition blockPos = new BlockPosition(structureTemplate.getSize().getX() / 2, 0, structureTemplate.getSize().getZ() / 2);
        return makeSettings(EnumBlockMirror.valueOf(nbt.getString("Mirror")), EnumBlockRotation.valueOf(nbt.getString("Rotation")), WorldGenFeatureRuinedPortalPieces.Position.byName(nbt.getString("VerticalPlacement")), blockPos, WorldGenFeatureRuinedPortalPieces.Properties.CODEC.parse(new Dynamic<>(DynamicOpsNBT.INSTANCE, nbt.get("Properties"))).getOrThrow(true, LOGGER::error));
    }

    private static DefinedStructureInfo makeSettings(EnumBlockMirror mirror, EnumBlockRotation rotation, WorldGenFeatureRuinedPortalPieces.Position verticalPlacement, BlockPosition pos, WorldGenFeatureRuinedPortalPieces.Properties properties) {
        DefinedStructureProcessorBlockIgnore blockIgnoreProcessor = properties.airPocket ? DefinedStructureProcessorBlockIgnore.STRUCTURE_BLOCK : DefinedStructureProcessorBlockIgnore.STRUCTURE_AND_AIR;
        List<DefinedStructureProcessorPredicates> list = Lists.newArrayList();
        list.add(getBlockReplaceRule(Blocks.GOLD_BLOCK, 0.3F, Blocks.AIR));
        list.add(getLavaProcessorRule(verticalPlacement, properties));
        if (!properties.cold) {
            list.add(getBlockReplaceRule(Blocks.NETHERRACK, 0.07F, Blocks.MAGMA_BLOCK));
        }

        DefinedStructureInfo structurePlaceSettings = (new DefinedStructureInfo()).setRotation(rotation).setMirror(mirror).setRotationPivot(pos).addProcessor(blockIgnoreProcessor).addProcessor(new DefinedStructureProcessorRule(list)).addProcessor(new DefinedStructureProcessorBlockAge(properties.mossiness)).addProcessor(new ProtectedBlockProcessor(TagsBlock.FEATURES_CANNOT_REPLACE.getName())).addProcessor(new DefinedStructureProcessorLavaSubmergedBlock());
        if (properties.replaceWithBlackstone) {
            structurePlaceSettings.addProcessor(DefinedStructureProcessorBlackstoneReplace.INSTANCE);
        }

        return structurePlaceSettings;
    }

    private static DefinedStructureProcessorPredicates getLavaProcessorRule(WorldGenFeatureRuinedPortalPieces.Position verticalPlacement, WorldGenFeatureRuinedPortalPieces.Properties properties) {
        if (verticalPlacement == WorldGenFeatureRuinedPortalPieces.Position.ON_OCEAN_FLOOR) {
            return getBlockReplaceRule(Blocks.LAVA, Blocks.MAGMA_BLOCK);
        } else {
            return properties.cold ? getBlockReplaceRule(Blocks.LAVA, Blocks.NETHERRACK) : getBlockReplaceRule(Blocks.LAVA, 0.2F, Blocks.MAGMA_BLOCK);
        }
    }

    @Override
    public void postProcess(GeneratorAccessSeed world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, StructureBoundingBox chunkBox, ChunkCoordIntPair chunkPos, BlockPosition pos) {
        StructureBoundingBox boundingBox = this.template.getBoundingBox(this.placeSettings, this.templatePosition);
        if (chunkBox.isInside(boundingBox.getCenter())) {
            chunkBox.encapsulate(boundingBox);
            super.postProcess(world, structureAccessor, chunkGenerator, random, chunkBox, chunkPos, pos);
            this.spreadNetherrack(random, world);
            this.addNetherrackDripColumnsBelowPortal(random, world);
            if (this.properties.vines || this.properties.overgrown) {
                BlockPosition.betweenClosedStream(this.getBoundingBox()).forEach((posx) -> {
                    if (this.properties.vines) {
                        this.maybeAddVines(random, world, posx);
                    }

                    if (this.properties.overgrown) {
                        this.maybeAddLeavesAbove(random, world, posx);
                    }

                });
            }

        }
    }

    @Override
    protected void handleDataMarker(String metadata, BlockPosition pos, WorldAccess world, Random random, StructureBoundingBox boundingBox) {
    }

    private void maybeAddVines(Random random, GeneratorAccess world, BlockPosition pos) {
        IBlockData blockState = world.getType(pos);
        if (!blockState.isAir() && !blockState.is(Blocks.VINE)) {
            EnumDirection direction = getRandomHorizontalDirection(random);
            BlockPosition blockPos = pos.relative(direction);
            IBlockData blockState2 = world.getType(blockPos);
            if (blockState2.isAir()) {
                if (Block.isFaceFull(blockState.getCollisionShape(world, pos), direction)) {
                    BlockStateBoolean booleanProperty = BlockVine.getDirection(direction.opposite());
                    world.setTypeAndData(blockPos, Blocks.VINE.getBlockData().set(booleanProperty, Boolean.valueOf(true)), 3);
                }
            }
        }
    }

    private void maybeAddLeavesAbove(Random random, GeneratorAccess world, BlockPosition pos) {
        if (random.nextFloat() < 0.5F && world.getType(pos).is(Blocks.NETHERRACK) && world.getType(pos.above()).isAir()) {
            world.setTypeAndData(pos.above(), Blocks.JUNGLE_LEAVES.getBlockData().set(BlockLeaves.PERSISTENT, Boolean.valueOf(true)), 3);
        }

    }

    private void addNetherrackDripColumnsBelowPortal(Random random, GeneratorAccess world) {
        for(int i = this.boundingBox.minX() + 1; i < this.boundingBox.maxX(); ++i) {
            for(int j = this.boundingBox.minZ() + 1; j < this.boundingBox.maxZ(); ++j) {
                BlockPosition blockPos = new BlockPosition(i, this.boundingBox.minY(), j);
                if (world.getType(blockPos).is(Blocks.NETHERRACK)) {
                    this.addNetherrackDripColumn(random, world, blockPos.below());
                }
            }
        }

    }

    private void addNetherrackDripColumn(Random random, GeneratorAccess world, BlockPosition pos) {
        BlockPosition.MutableBlockPosition mutableBlockPos = pos.mutable();
        this.placeNetherrackOrMagma(random, world, mutableBlockPos);
        int i = 8;

        while(i > 0 && random.nextFloat() < 0.5F) {
            mutableBlockPos.move(EnumDirection.DOWN);
            --i;
            this.placeNetherrackOrMagma(random, world, mutableBlockPos);
        }

    }

    private void spreadNetherrack(Random random, GeneratorAccess world) {
        boolean bl = this.verticalPlacement == WorldGenFeatureRuinedPortalPieces.Position.ON_LAND_SURFACE || this.verticalPlacement == WorldGenFeatureRuinedPortalPieces.Position.ON_OCEAN_FLOOR;
        BlockPosition blockPos = this.boundingBox.getCenter();
        int i = blockPos.getX();
        int j = blockPos.getZ();
        float[] fs = new float[]{1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.9F, 0.9F, 0.8F, 0.7F, 0.6F, 0.4F, 0.2F};
        int k = fs.length;
        int l = (this.boundingBox.getXSpan() + this.boundingBox.getZSpan()) / 2;
        int m = random.nextInt(Math.max(1, 8 - l / 2));
        int n = 3;
        BlockPosition.MutableBlockPosition mutableBlockPos = BlockPosition.ZERO.mutable();

        for(int o = i - k; o <= i + k; ++o) {
            for(int p = j - k; p <= j + k; ++p) {
                int q = Math.abs(o - i) + Math.abs(p - j);
                int r = Math.max(0, q + m);
                if (r < k) {
                    float f = fs[r];
                    if (random.nextDouble() < (double)f) {
                        int s = getSurfaceY(world, o, p, this.verticalPlacement);
                        int t = bl ? s : Math.min(this.boundingBox.minY(), s);
                        mutableBlockPos.set(o, t, p);
                        if (Math.abs(t - this.boundingBox.minY()) <= 3 && this.canBlockBeReplacedByNetherrackOrMagma(world, mutableBlockPos)) {
                            this.placeNetherrackOrMagma(random, world, mutableBlockPos);
                            if (this.properties.overgrown) {
                                this.maybeAddLeavesAbove(random, world, mutableBlockPos);
                            }

                            this.addNetherrackDripColumn(random, world, mutableBlockPos.below());
                        }
                    }
                }
            }
        }

    }

    private boolean canBlockBeReplacedByNetherrackOrMagma(GeneratorAccess world, BlockPosition pos) {
        IBlockData blockState = world.getType(pos);
        return !blockState.is(Blocks.AIR) && !blockState.is(Blocks.OBSIDIAN) && !blockState.is(TagsBlock.FEATURES_CANNOT_REPLACE) && (this.verticalPlacement == WorldGenFeatureRuinedPortalPieces.Position.IN_NETHER || !blockState.is(Blocks.LAVA));
    }

    private void placeNetherrackOrMagma(Random random, GeneratorAccess world, BlockPosition pos) {
        if (!this.properties.cold && random.nextFloat() < 0.07F) {
            world.setTypeAndData(pos, Blocks.MAGMA_BLOCK.getBlockData(), 3);
        } else {
            world.setTypeAndData(pos, Blocks.NETHERRACK.getBlockData(), 3);
        }

    }

    private static int getSurfaceY(GeneratorAccess world, int x, int y, WorldGenFeatureRuinedPortalPieces.Position verticalPlacement) {
        return world.getHeight(getHeightMapType(verticalPlacement), x, y) - 1;
    }

    public static HeightMap.Type getHeightMapType(WorldGenFeatureRuinedPortalPieces.Position verticalPlacement) {
        return verticalPlacement == WorldGenFeatureRuinedPortalPieces.Position.ON_OCEAN_FLOOR ? HeightMap.Type.OCEAN_FLOOR_WG : HeightMap.Type.WORLD_SURFACE_WG;
    }

    private static DefinedStructureProcessorPredicates getBlockReplaceRule(Block old, float chance, Block updated) {
        return new DefinedStructureProcessorPredicates(new DefinedStructureTestRandomBlock(old, chance), DefinedStructureTestTrue.INSTANCE, updated.getBlockData());
    }

    private static DefinedStructureProcessorPredicates getBlockReplaceRule(Block old, Block updated) {
        return new DefinedStructureProcessorPredicates(new DefinedStructureTestBlock(old), DefinedStructureTestTrue.INSTANCE, updated.getBlockData());
    }

    public static enum Position {
        ON_LAND_SURFACE("on_land_surface"),
        PARTLY_BURIED("partly_buried"),
        ON_OCEAN_FLOOR("on_ocean_floor"),
        IN_MOUNTAIN("in_mountain"),
        UNDERGROUND("underground"),
        IN_NETHER("in_nether");

        private static final Map<String, WorldGenFeatureRuinedPortalPieces.Position> BY_NAME = Arrays.stream(values()).collect(Collectors.toMap(WorldGenFeatureRuinedPortalPieces.Position::getName, (verticalPlacement) -> {
            return verticalPlacement;
        }));
        private final String name;

        private Position(String id) {
            this.name = id;
        }

        public String getName() {
            return this.name;
        }

        public static WorldGenFeatureRuinedPortalPieces.Position byName(String id) {
            return BY_NAME.get(id);
        }
    }

    public static class Properties {
        public static final Codec<WorldGenFeatureRuinedPortalPieces.Properties> CODEC = RecordCodecBuilder.create((instance) -> {
            return instance.group(Codec.BOOL.fieldOf("cold").forGetter((properties) -> {
                return properties.cold;
            }), Codec.FLOAT.fieldOf("mossiness").forGetter((properties) -> {
                return properties.mossiness;
            }), Codec.BOOL.fieldOf("air_pocket").forGetter((properties) -> {
                return properties.airPocket;
            }), Codec.BOOL.fieldOf("overgrown").forGetter((properties) -> {
                return properties.overgrown;
            }), Codec.BOOL.fieldOf("vines").forGetter((properties) -> {
                return properties.vines;
            }), Codec.BOOL.fieldOf("replace_with_blackstone").forGetter((properties) -> {
                return properties.replaceWithBlackstone;
            })).apply(instance, WorldGenFeatureRuinedPortalPieces.Properties::new);
        });
        public boolean cold;
        public float mossiness = 0.2F;
        public boolean airPocket;
        public boolean overgrown;
        public boolean vines;
        public boolean replaceWithBlackstone;

        public Properties() {
        }

        public Properties(boolean cold, float mossiness, boolean airPocket, boolean overgrown, boolean vines, boolean replaceWithBlackstone) {
            this.cold = cold;
            this.mossiness = mossiness;
            this.airPocket = airPocket;
            this.overgrown = overgrown;
            this.vines = vines;
            this.replaceWithBlackstone = replaceWithBlackstone;
        }
    }
}
