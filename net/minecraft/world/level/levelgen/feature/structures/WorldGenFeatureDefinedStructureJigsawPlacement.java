package net.minecraft.world.level.levelgen.feature.structures;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.IRegistry;
import net.minecraft.core.IRegistryCustom;
import net.minecraft.data.worldgen.WorldGenFeaturePieces;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.level.IWorldHeightAccess;
import net.minecraft.world.level.block.BlockJigsaw;
import net.minecraft.world.level.block.EnumBlockRotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.feature.StructureGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureVillageConfiguration;
import net.minecraft.world.level.levelgen.structure.StructureBoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePieceAccessor;
import net.minecraft.world.level.levelgen.structure.WorldGenFeaturePillagerOutpostPoolPiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureManager;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.shapes.OperatorBoolean;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapes;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WorldGenFeatureDefinedStructureJigsawPlacement {
    static final Logger LOGGER = LogManager.getLogger();

    public static void addPieces(IRegistryCustom dynamicRegistries, WorldGenFeatureVillageConfiguration config, WorldGenFeatureDefinedStructureJigsawPlacement.PieceFactory pieceFactory, ChunkGenerator chunkGenerator, DefinedStructureManager structureManager, BlockPosition pos, StructurePieceAccessor children, Random random, boolean modifyBoundingBox, boolean surface, IWorldHeightAccess world) {
        StructureGenerator.bootstrap();
        List<WorldGenFeaturePillagerOutpostPoolPiece> list = Lists.newArrayList();
        IRegistry<WorldGenFeatureDefinedStructurePoolTemplate> registry = dynamicRegistries.registryOrThrow(IRegistry.TEMPLATE_POOL_REGISTRY);
        EnumBlockRotation rotation = EnumBlockRotation.getRandom(random);
        WorldGenFeatureDefinedStructurePoolTemplate structureTemplatePool = config.startPool().get();
        WorldGenFeatureDefinedStructurePoolStructure structurePoolElement = structureTemplatePool.getRandomTemplate(random);
        if (structurePoolElement != WorldGenFeatureDefinedStructurePoolEmpty.INSTANCE) {
            WorldGenFeaturePillagerOutpostPoolPiece poolElementStructurePiece = pieceFactory.create(structureManager, structurePoolElement, pos, structurePoolElement.getGroundLevelDelta(), rotation, structurePoolElement.getBoundingBox(structureManager, pos, rotation));
            StructureBoundingBox boundingBox = poolElementStructurePiece.getBoundingBox();
            int i = (boundingBox.maxX() + boundingBox.minX()) / 2;
            int j = (boundingBox.maxZ() + boundingBox.minZ()) / 2;
            int k;
            if (surface) {
                k = pos.getY() + chunkGenerator.getFirstFreeHeight(i, j, HeightMap.Type.WORLD_SURFACE_WG, world);
            } else {
                k = pos.getY();
            }

            int m = boundingBox.minY() + poolElementStructurePiece.getGroundLevelDelta();
            poolElementStructurePiece.move(0, k - m, 0);
            list.add(poolElementStructurePiece);
            if (config.maxDepth() > 0) {
                int n = 80;
                AxisAlignedBB aABB = new AxisAlignedBB((double)(i - 80), (double)(k - 80), (double)(j - 80), (double)(i + 80 + 1), (double)(k + 80 + 1), (double)(j + 80 + 1));
                WorldGenFeatureDefinedStructureJigsawPlacement.Placer placer = new WorldGenFeatureDefinedStructureJigsawPlacement.Placer(registry, config.maxDepth(), pieceFactory, chunkGenerator, structureManager, list, random);
                placer.placing.addLast(new WorldGenFeatureDefinedStructureJigsawPlacement.PieceState(poolElementStructurePiece, new MutableObject<>(VoxelShapes.join(VoxelShapes.create(aABB), VoxelShapes.create(AxisAlignedBB.of(boundingBox)), OperatorBoolean.ONLY_FIRST)), k + 80, 0));

                while(!placer.placing.isEmpty()) {
                    WorldGenFeatureDefinedStructureJigsawPlacement.PieceState pieceState = placer.placing.removeFirst();
                    placer.tryPlacingChildren(pieceState.piece, pieceState.free, pieceState.boundsTop, pieceState.depth, modifyBoundingBox, world);
                }

                list.forEach(children::addPiece);
            }
        }
    }

    public static void addPieces(IRegistryCustom registryAccess, WorldGenFeaturePillagerOutpostPoolPiece poolElementStructurePiece, int i, WorldGenFeatureDefinedStructureJigsawPlacement.PieceFactory pieceFactory, ChunkGenerator chunkGenerator, DefinedStructureManager structureManager, List<? super WorldGenFeaturePillagerOutpostPoolPiece> list, Random random, IWorldHeightAccess levelHeightAccessor) {
        IRegistry<WorldGenFeatureDefinedStructurePoolTemplate> registry = registryAccess.registryOrThrow(IRegistry.TEMPLATE_POOL_REGISTRY);
        WorldGenFeatureDefinedStructureJigsawPlacement.Placer placer = new WorldGenFeatureDefinedStructureJigsawPlacement.Placer(registry, i, pieceFactory, chunkGenerator, structureManager, list, random);
        placer.placing.addLast(new WorldGenFeatureDefinedStructureJigsawPlacement.PieceState(poolElementStructurePiece, new MutableObject<>(VoxelShapes.INFINITY), 0, 0));

        while(!placer.placing.isEmpty()) {
            WorldGenFeatureDefinedStructureJigsawPlacement.PieceState pieceState = placer.placing.removeFirst();
            placer.tryPlacingChildren(pieceState.piece, pieceState.free, pieceState.boundsTop, pieceState.depth, false, levelHeightAccessor);
        }

    }

    public interface PieceFactory {
        WorldGenFeaturePillagerOutpostPoolPiece create(DefinedStructureManager structureManager, WorldGenFeatureDefinedStructurePoolStructure poolElement, BlockPosition pos, int i, EnumBlockRotation rotation, StructureBoundingBox elementBounds);
    }

    static final class PieceState {
        final WorldGenFeaturePillagerOutpostPoolPiece piece;
        final MutableObject<VoxelShape> free;
        final int boundsTop;
        final int depth;

        PieceState(WorldGenFeaturePillagerOutpostPoolPiece piece, MutableObject<VoxelShape> pieceShape, int minY, int currentSize) {
            this.piece = piece;
            this.free = pieceShape;
            this.boundsTop = minY;
            this.depth = currentSize;
        }
    }

    static final class Placer {
        private final IRegistry<WorldGenFeatureDefinedStructurePoolTemplate> pools;
        private final int maxDepth;
        private final WorldGenFeatureDefinedStructureJigsawPlacement.PieceFactory factory;
        private final ChunkGenerator chunkGenerator;
        private final DefinedStructureManager structureManager;
        private final List<? super WorldGenFeaturePillagerOutpostPoolPiece> pieces;
        private final Random random;
        final Deque<WorldGenFeatureDefinedStructureJigsawPlacement.PieceState> placing = Queues.newArrayDeque();

        Placer(IRegistry<WorldGenFeatureDefinedStructurePoolTemplate> registry, int maxSize, WorldGenFeatureDefinedStructureJigsawPlacement.PieceFactory pieceFactory, ChunkGenerator chunkGenerator, DefinedStructureManager structureManager, List<? super WorldGenFeaturePillagerOutpostPoolPiece> children, Random random) {
            this.pools = registry;
            this.maxDepth = maxSize;
            this.factory = pieceFactory;
            this.chunkGenerator = chunkGenerator;
            this.structureManager = structureManager;
            this.pieces = children;
            this.random = random;
        }

        void tryPlacingChildren(WorldGenFeaturePillagerOutpostPoolPiece piece, MutableObject<VoxelShape> pieceShape, int minY, int currentSize, boolean modifyBoundingBox, IWorldHeightAccess world) {
            WorldGenFeatureDefinedStructurePoolStructure structurePoolElement = piece.getElement();
            BlockPosition blockPos = piece.getPosition();
            EnumBlockRotation rotation = piece.getRotation();
            WorldGenFeatureDefinedStructurePoolTemplate.Matching projection = structurePoolElement.getProjection();
            boolean bl = projection == WorldGenFeatureDefinedStructurePoolTemplate.Matching.RIGID;
            MutableObject<VoxelShape> mutableObject = new MutableObject<>();
            StructureBoundingBox boundingBox = piece.getBoundingBox();
            int i = boundingBox.minY();

            label139:
            for(DefinedStructure.BlockInfo structureBlockInfo : structurePoolElement.getShuffledJigsawBlocks(this.structureManager, blockPos, rotation, this.random)) {
                EnumDirection direction = BlockJigsaw.getFrontFacing(structureBlockInfo.state);
                BlockPosition blockPos2 = structureBlockInfo.pos;
                BlockPosition blockPos3 = blockPos2.relative(direction);
                int j = blockPos2.getY() - i;
                int k = -1;
                MinecraftKey resourceLocation = new MinecraftKey(structureBlockInfo.nbt.getString("pool"));
                Optional<WorldGenFeatureDefinedStructurePoolTemplate> optional = this.pools.getOptional(resourceLocation);
                if (optional.isPresent() && (optional.get().size() != 0 || Objects.equals(resourceLocation, WorldGenFeaturePieces.EMPTY.location()))) {
                    MinecraftKey resourceLocation2 = optional.get().getFallback();
                    Optional<WorldGenFeatureDefinedStructurePoolTemplate> optional2 = this.pools.getOptional(resourceLocation2);
                    if (optional2.isPresent() && (optional2.get().size() != 0 || Objects.equals(resourceLocation2, WorldGenFeaturePieces.EMPTY.location()))) {
                        boolean bl2 = boundingBox.isInside(blockPos3);
                        MutableObject<VoxelShape> mutableObject2;
                        int l;
                        if (bl2) {
                            mutableObject2 = mutableObject;
                            l = i;
                            if (mutableObject.getValue() == null) {
                                mutableObject.setValue(VoxelShapes.create(AxisAlignedBB.of(boundingBox)));
                            }
                        } else {
                            mutableObject2 = pieceShape;
                            l = minY;
                        }

                        List<WorldGenFeatureDefinedStructurePoolStructure> list = Lists.newArrayList();
                        if (currentSize != this.maxDepth) {
                            list.addAll(optional.get().getShuffledTemplates(this.random));
                        }

                        list.addAll(optional2.get().getShuffledTemplates(this.random));

                        for(WorldGenFeatureDefinedStructurePoolStructure structurePoolElement2 : list) {
                            if (structurePoolElement2 == WorldGenFeatureDefinedStructurePoolEmpty.INSTANCE) {
                                break;
                            }

                            for(EnumBlockRotation rotation2 : EnumBlockRotation.getShuffled(this.random)) {
                                List<DefinedStructure.BlockInfo> list2 = structurePoolElement2.getShuffledJigsawBlocks(this.structureManager, BlockPosition.ZERO, rotation2, this.random);
                                StructureBoundingBox boundingBox2 = structurePoolElement2.getBoundingBox(this.structureManager, BlockPosition.ZERO, rotation2);
                                int o;
                                if (modifyBoundingBox && boundingBox2.getYSpan() <= 16) {
                                    o = list2.stream().mapToInt((structureBlockInfox) -> {
                                        if (!boundingBox2.isInside(structureBlockInfox.pos.relative(BlockJigsaw.getFrontFacing(structureBlockInfox.state)))) {
                                            return 0;
                                        } else {
                                            MinecraftKey resourceLocation = new MinecraftKey(structureBlockInfox.nbt.getString("pool"));
                                            Optional<WorldGenFeatureDefinedStructurePoolTemplate> optional = this.pools.getOptional(resourceLocation);
                                            Optional<WorldGenFeatureDefinedStructurePoolTemplate> optional2 = optional.flatMap((structureTemplatePool) -> {
                                                return this.pools.getOptional(structureTemplatePool.getFallback());
                                            });
                                            int i = optional.map((structureTemplatePool) -> {
                                                return structureTemplatePool.getMaxSize(this.structureManager);
                                            }).orElse(0);
                                            int j = optional2.map((structureTemplatePool) -> {
                                                return structureTemplatePool.getMaxSize(this.structureManager);
                                            }).orElse(0);
                                            return Math.max(i, j);
                                        }
                                    }).max().orElse(0);
                                } else {
                                    o = 0;
                                }

                                for(DefinedStructure.BlockInfo structureBlockInfo2 : list2) {
                                    if (BlockJigsaw.canAttach(structureBlockInfo, structureBlockInfo2)) {
                                        BlockPosition blockPos4 = structureBlockInfo2.pos;
                                        BlockPosition blockPos5 = blockPos3.subtract(blockPos4);
                                        StructureBoundingBox boundingBox3 = structurePoolElement2.getBoundingBox(this.structureManager, blockPos5, rotation2);
                                        int p = boundingBox3.minY();
                                        WorldGenFeatureDefinedStructurePoolTemplate.Matching projection2 = structurePoolElement2.getProjection();
                                        boolean bl3 = projection2 == WorldGenFeatureDefinedStructurePoolTemplate.Matching.RIGID;
                                        int q = blockPos4.getY();
                                        int r = j - q + BlockJigsaw.getFrontFacing(structureBlockInfo.state).getAdjacentY();
                                        int s;
                                        if (bl && bl3) {
                                            s = i + r;
                                        } else {
                                            if (k == -1) {
                                                k = this.chunkGenerator.getFirstFreeHeight(blockPos2.getX(), blockPos2.getZ(), HeightMap.Type.WORLD_SURFACE_WG, world);
                                            }

                                            s = k - q;
                                        }

                                        int u = s - p;
                                        StructureBoundingBox boundingBox4 = boundingBox3.moved(0, u, 0);
                                        BlockPosition blockPos6 = blockPos5.offset(0, u, 0);
                                        if (o > 0) {
                                            int v = Math.max(o + 1, boundingBox4.maxY() - boundingBox4.minY());
                                            boundingBox4.encapsulate(new BlockPosition(boundingBox4.minX(), boundingBox4.minY() + v, boundingBox4.minZ()));
                                        }

                                        if (!VoxelShapes.joinIsNotEmpty(mutableObject2.getValue(), VoxelShapes.create(AxisAlignedBB.of(boundingBox4).shrink(0.25D)), OperatorBoolean.ONLY_SECOND)) {
                                            mutableObject2.setValue(VoxelShapes.joinUnoptimized(mutableObject2.getValue(), VoxelShapes.create(AxisAlignedBB.of(boundingBox4)), OperatorBoolean.ONLY_FIRST));
                                            int w = piece.getGroundLevelDelta();
                                            int x;
                                            if (bl3) {
                                                x = w - r;
                                            } else {
                                                x = structurePoolElement2.getGroundLevelDelta();
                                            }

                                            WorldGenFeaturePillagerOutpostPoolPiece poolElementStructurePiece = this.factory.create(this.structureManager, structurePoolElement2, blockPos6, x, rotation2, boundingBox4);
                                            int z;
                                            if (bl) {
                                                z = i + j;
                                            } else if (bl3) {
                                                z = s + q;
                                            } else {
                                                if (k == -1) {
                                                    k = this.chunkGenerator.getFirstFreeHeight(blockPos2.getX(), blockPos2.getZ(), HeightMap.Type.WORLD_SURFACE_WG, world);
                                                }

                                                z = k + r / 2;
                                            }

                                            piece.addJunction(new WorldGenFeatureDefinedStructureJigsawJunction(blockPos3.getX(), z - j + w, blockPos3.getZ(), r, projection2));
                                            poolElementStructurePiece.addJunction(new WorldGenFeatureDefinedStructureJigsawJunction(blockPos2.getX(), z - q + x, blockPos2.getZ(), -r, projection));
                                            this.pieces.add(poolElementStructurePiece);
                                            if (currentSize + 1 <= this.maxDepth) {
                                                this.placing.addLast(new WorldGenFeatureDefinedStructureJigsawPlacement.PieceState(poolElementStructurePiece, mutableObject2, l, currentSize + 1));
                                            }
                                            continue label139;
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        WorldGenFeatureDefinedStructureJigsawPlacement.LOGGER.warn("Empty or non-existent fallback pool: {}", (Object)resourceLocation2);
                    }
                } else {
                    WorldGenFeatureDefinedStructureJigsawPlacement.LOGGER.warn("Empty or non-existent pool: {}", (Object)resourceLocation);
                }
            }

        }
    }
}
