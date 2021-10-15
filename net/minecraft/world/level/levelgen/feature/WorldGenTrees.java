package net.minecraft.world.level.levelgen.feature;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalInt;
import java.util.Random;
import java.util.Set;
import java.util.function.BiConsumer;
import net.minecraft.core.BaseBlockPosition;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.tags.TagsBlock;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.IWorldWriter;
import net.minecraft.world.level.VirtualWorldReadable;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.levelgen.feature.configurations.WorldGenFeatureTreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.WorldGenFoilagePlacer;
import net.minecraft.world.level.levelgen.structure.StructureBoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructure;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.shapes.VoxelShapeBitSet;
import net.minecraft.world.phys.shapes.VoxelShapeDiscrete;

public class WorldGenTrees extends WorldGenerator<WorldGenFeatureTreeConfiguration> {
    private static final int BLOCK_UPDATE_FLAGS = 19;

    public WorldGenTrees(Codec<WorldGenFeatureTreeConfiguration> configCodec) {
        super(configCodec);
    }

    public static boolean isFree(VirtualWorldReadable world, BlockPosition pos) {
        return validTreePos(world, pos) || world.isStateAtPosition(pos, (state) -> {
            return state.is(TagsBlock.LOGS);
        });
    }

    private static boolean isVine(VirtualWorldReadable world, BlockPosition pos) {
        return world.isStateAtPosition(pos, (state) -> {
            return state.is(Blocks.VINE);
        });
    }

    private static boolean isBlockWater(VirtualWorldReadable world, BlockPosition pos) {
        return world.isStateAtPosition(pos, (state) -> {
            return state.is(Blocks.WATER);
        });
    }

    public static boolean isAirOrLeaves(VirtualWorldReadable world, BlockPosition pos) {
        return world.isStateAtPosition(pos, (state) -> {
            return state.isAir() || state.is(TagsBlock.LEAVES);
        });
    }

    private static boolean isReplaceablePlant(VirtualWorldReadable world, BlockPosition pos) {
        return world.isStateAtPosition(pos, (state) -> {
            Material material = state.getMaterial();
            return material == Material.REPLACEABLE_PLANT;
        });
    }

    private static void setBlockKnownShape(IWorldWriter world, BlockPosition pos, IBlockData state) {
        world.setTypeAndData(pos, state, 19);
    }

    public static boolean validTreePos(VirtualWorldReadable world, BlockPosition pos) {
        return isAirOrLeaves(world, pos) || isReplaceablePlant(world, pos) || isBlockWater(world, pos);
    }

    private boolean doPlace(GeneratorAccessSeed world, Random random, BlockPosition pos, BiConsumer<BlockPosition, IBlockData> trunkReplacer, BiConsumer<BlockPosition, IBlockData> foliageReplacer, WorldGenFeatureTreeConfiguration config) {
        int i = config.trunkPlacer.getTreeHeight(random);
        int j = config.foliagePlacer.foliageHeight(random, i, config);
        int k = i - j;
        int l = config.foliagePlacer.foliageRadius(random, k);
        if (pos.getY() >= world.getMinBuildHeight() + 1 && pos.getY() + i + 1 <= world.getMaxBuildHeight()) {
            if (!config.saplingProvider.getState(random, pos).canPlace(world, pos)) {
                return false;
            } else {
                OptionalInt optionalInt = config.minimumSize.minClippedHeight();
                int m = this.getMaxFreeTreeHeight(world, i, pos, config);
                if (m >= i || optionalInt.isPresent() && m >= optionalInt.getAsInt()) {
                    List<WorldGenFoilagePlacer.FoliageAttachment> list = config.trunkPlacer.placeTrunk(world, trunkReplacer, random, m, pos, config);
                    list.forEach((foliageAttachment) -> {
                        config.foliagePlacer.createFoliage(world, foliageReplacer, random, config, m, foliageAttachment, j, l);
                    });
                    return true;
                } else {
                    return false;
                }
            }
        } else {
            return false;
        }
    }

    private int getMaxFreeTreeHeight(VirtualWorldReadable world, int height, BlockPosition pos, WorldGenFeatureTreeConfiguration config) {
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

        for(int i = 0; i <= height + 1; ++i) {
            int j = config.minimumSize.getSizeAtHeight(height, i);

            for(int k = -j; k <= j; ++k) {
                for(int l = -j; l <= j; ++l) {
                    mutableBlockPos.setWithOffset(pos, k, i, l);
                    if (!isFree(world, mutableBlockPos) || !config.ignoreVines && isVine(world, mutableBlockPos)) {
                        return i - 2;
                    }
                }
            }
        }

        return height;
    }

    @Override
    protected void setBlock(IWorldWriter world, BlockPosition pos, IBlockData state) {
        setBlockKnownShape(world, pos, state);
    }

    @Override
    public final boolean generate(FeaturePlaceContext<WorldGenFeatureTreeConfiguration> context) {
        GeneratorAccessSeed worldGenLevel = context.level();
        Random random = context.random();
        BlockPosition blockPos = context.origin();
        WorldGenFeatureTreeConfiguration treeConfiguration = context.config();
        Set<BlockPosition> set = Sets.newHashSet();
        Set<BlockPosition> set2 = Sets.newHashSet();
        Set<BlockPosition> set3 = Sets.newHashSet();
        BiConsumer<BlockPosition, IBlockData> biConsumer = (pos, state) -> {
            set.add(pos.immutableCopy());
            worldGenLevel.setTypeAndData(pos, state, 19);
        };
        BiConsumer<BlockPosition, IBlockData> biConsumer2 = (pos, state) -> {
            set2.add(pos.immutableCopy());
            worldGenLevel.setTypeAndData(pos, state, 19);
        };
        BiConsumer<BlockPosition, IBlockData> biConsumer3 = (pos, state) -> {
            set3.add(pos.immutableCopy());
            worldGenLevel.setTypeAndData(pos, state, 19);
        };
        boolean bl = this.doPlace(worldGenLevel, random, blockPos, biConsumer, biConsumer2, treeConfiguration);
        if (bl && (!set.isEmpty() || !set2.isEmpty())) {
            if (!treeConfiguration.decorators.isEmpty()) {
                List<BlockPosition> list = Lists.newArrayList(set);
                List<BlockPosition> list2 = Lists.newArrayList(set2);
                list.sort(Comparator.comparingInt(BaseBlockPosition::getY));
                list2.sort(Comparator.comparingInt(BaseBlockPosition::getY));
                treeConfiguration.decorators.forEach((treeDecorator) -> {
                    treeDecorator.place(worldGenLevel, biConsumer3, random, list, list2);
                });
            }

            return StructureBoundingBox.encapsulatingPositions(Iterables.concat(set, set2, set3)).map((box) -> {
                VoxelShapeDiscrete discreteVoxelShape = updateLeaves(worldGenLevel, box, set, set3);
                DefinedStructure.updateShapeAtEdge(worldGenLevel, 3, discreteVoxelShape, box.minX(), box.minY(), box.minZ());
                return true;
            }).orElse(false);
        } else {
            return false;
        }
    }

    private static VoxelShapeDiscrete updateLeaves(GeneratorAccess world, StructureBoundingBox box, Set<BlockPosition> trunkPositions, Set<BlockPosition> decorationPositions) {
        List<Set<BlockPosition>> list = Lists.newArrayList();
        VoxelShapeDiscrete discreteVoxelShape = new VoxelShapeBitSet(box.getXSpan(), box.getYSpan(), box.getZSpan());
        int i = 6;

        for(int j = 0; j < 6; ++j) {
            list.add(Sets.newHashSet());
        }

        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

        for(BlockPosition blockPos : Lists.newArrayList(decorationPositions)) {
            if (box.isInside(blockPos)) {
                discreteVoxelShape.fill(blockPos.getX() - box.minX(), blockPos.getY() - box.minY(), blockPos.getZ() - box.minZ());
            }
        }

        for(BlockPosition blockPos2 : Lists.newArrayList(trunkPositions)) {
            if (box.isInside(blockPos2)) {
                discreteVoxelShape.fill(blockPos2.getX() - box.minX(), blockPos2.getY() - box.minY(), blockPos2.getZ() - box.minZ());
            }

            for(EnumDirection direction : EnumDirection.values()) {
                mutableBlockPos.setWithOffset(blockPos2, direction);
                if (!trunkPositions.contains(mutableBlockPos)) {
                    IBlockData blockState = world.getType(mutableBlockPos);
                    if (blockState.hasProperty(BlockProperties.DISTANCE)) {
                        list.get(0).add(mutableBlockPos.immutableCopy());
                        setBlockKnownShape(world, mutableBlockPos, blockState.set(BlockProperties.DISTANCE, Integer.valueOf(1)));
                        if (box.isInside(mutableBlockPos)) {
                            discreteVoxelShape.fill(mutableBlockPos.getX() - box.minX(), mutableBlockPos.getY() - box.minY(), mutableBlockPos.getZ() - box.minZ());
                        }
                    }
                }
            }
        }

        for(int k = 1; k < 6; ++k) {
            Set<BlockPosition> set = list.get(k - 1);
            Set<BlockPosition> set2 = list.get(k);

            for(BlockPosition blockPos3 : set) {
                if (box.isInside(blockPos3)) {
                    discreteVoxelShape.fill(blockPos3.getX() - box.minX(), blockPos3.getY() - box.minY(), blockPos3.getZ() - box.minZ());
                }

                for(EnumDirection direction2 : EnumDirection.values()) {
                    mutableBlockPos.setWithOffset(blockPos3, direction2);
                    if (!set.contains(mutableBlockPos) && !set2.contains(mutableBlockPos)) {
                        IBlockData blockState2 = world.getType(mutableBlockPos);
                        if (blockState2.hasProperty(BlockProperties.DISTANCE)) {
                            int l = blockState2.get(BlockProperties.DISTANCE);
                            if (l > k + 1) {
                                IBlockData blockState3 = blockState2.set(BlockProperties.DISTANCE, Integer.valueOf(k + 1));
                                setBlockKnownShape(world, mutableBlockPos, blockState3);
                                if (box.isInside(mutableBlockPos)) {
                                    discreteVoxelShape.fill(mutableBlockPos.getX() - box.minX(), mutableBlockPos.getY() - box.minY(), mutableBlockPos.getZ() - box.minZ());
                                }

                                set2.add(mutableBlockPos.immutableCopy());
                            }
                        }
                    }
                }
            }
        }

        return discreteVoxelShape;
    }
}
