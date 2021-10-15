package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.core.BaseBlockPosition;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.RegistryBlockID;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.entity.decoration.EntityPainting;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.BlockAccessAir;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnumBlockMirror;
import net.minecraft.world.level.block.EnumBlockRotation;
import net.minecraft.world.level.block.IFluidContainer;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityLootable;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.levelgen.structure.StructureBoundingBox;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.Vec3D;
import net.minecraft.world.phys.shapes.VoxelShapeBitSet;
import net.minecraft.world.phys.shapes.VoxelShapeDiscrete;

public class DefinedStructure {
    public static final String PALETTE_TAG = "palette";
    public static final String PALETTE_LIST_TAG = "palettes";
    public static final String ENTITIES_TAG = "entities";
    public static final String BLOCKS_TAG = "blocks";
    public static final String BLOCK_TAG_POS = "pos";
    public static final String BLOCK_TAG_STATE = "state";
    public static final String BLOCK_TAG_NBT = "nbt";
    public static final String ENTITY_TAG_POS = "pos";
    public static final String ENTITY_TAG_BLOCKPOS = "blockPos";
    public static final String ENTITY_TAG_NBT = "nbt";
    public static final String SIZE_TAG = "size";
    static final int CHUNK_SIZE = 16;
    private final List<DefinedStructure.Palette> palettes = Lists.newArrayList();
    private final List<DefinedStructure.EntityInfo> entityInfoList = Lists.newArrayList();
    private BaseBlockPosition size = BaseBlockPosition.ZERO;
    private String author = "?";

    public BaseBlockPosition getSize() {
        return this.size;
    }

    public void setAuthor(String name) {
        this.author = name;
    }

    public String getAuthor() {
        return this.author;
    }

    public void fillFromWorld(World world, BlockPosition start, BaseBlockPosition dimensions, boolean includeEntities, @Nullable Block ignoredBlock) {
        if (dimensions.getX() >= 1 && dimensions.getY() >= 1 && dimensions.getZ() >= 1) {
            BlockPosition blockPos = start.offset(dimensions).offset(-1, -1, -1);
            List<DefinedStructure.BlockInfo> list = Lists.newArrayList();
            List<DefinedStructure.BlockInfo> list2 = Lists.newArrayList();
            List<DefinedStructure.BlockInfo> list3 = Lists.newArrayList();
            BlockPosition blockPos2 = new BlockPosition(Math.min(start.getX(), blockPos.getX()), Math.min(start.getY(), blockPos.getY()), Math.min(start.getZ(), blockPos.getZ()));
            BlockPosition blockPos3 = new BlockPosition(Math.max(start.getX(), blockPos.getX()), Math.max(start.getY(), blockPos.getY()), Math.max(start.getZ(), blockPos.getZ()));
            this.size = dimensions;

            for(BlockPosition blockPos4 : BlockPosition.betweenClosed(blockPos2, blockPos3)) {
                BlockPosition blockPos5 = blockPos4.subtract(blockPos2);
                IBlockData blockState = world.getType(blockPos4);
                if (ignoredBlock == null || !blockState.is(ignoredBlock)) {
                    TileEntity blockEntity = world.getTileEntity(blockPos4);
                    DefinedStructure.BlockInfo structureBlockInfo;
                    if (blockEntity != null) {
                        NBTTagCompound compoundTag = blockEntity.save(new NBTTagCompound());
                        compoundTag.remove("x");
                        compoundTag.remove("y");
                        compoundTag.remove("z");
                        structureBlockInfo = new DefinedStructure.BlockInfo(blockPos5, blockState, compoundTag.c());
                    } else {
                        structureBlockInfo = new DefinedStructure.BlockInfo(blockPos5, blockState, (NBTTagCompound)null);
                    }

                    addToLists(structureBlockInfo, list, list2, list3);
                }
            }

            List<DefinedStructure.BlockInfo> list4 = buildInfoList(list, list2, list3);
            this.palettes.clear();
            this.palettes.add(new DefinedStructure.Palette(list4));
            if (includeEntities) {
                this.fillEntityList(world, blockPos2, blockPos3.offset(1, 1, 1));
            } else {
                this.entityInfoList.clear();
            }

        }
    }

    private static void addToLists(DefinedStructure.BlockInfo structureBlockInfo, List<DefinedStructure.BlockInfo> list, List<DefinedStructure.BlockInfo> list2, List<DefinedStructure.BlockInfo> list3) {
        if (structureBlockInfo.nbt != null) {
            list2.add(structureBlockInfo);
        } else if (!structureBlockInfo.state.getBlock().hasDynamicShape() && structureBlockInfo.state.isCollisionShapeFullBlock(BlockAccessAir.INSTANCE, BlockPosition.ZERO)) {
            list.add(structureBlockInfo);
        } else {
            list3.add(structureBlockInfo);
        }

    }

    private static List<DefinedStructure.BlockInfo> buildInfoList(List<DefinedStructure.BlockInfo> list, List<DefinedStructure.BlockInfo> list2, List<DefinedStructure.BlockInfo> list3) {
        Comparator<DefinedStructure.BlockInfo> comparator = Comparator.comparingInt((structureBlockInfo) -> {
            return structureBlockInfo.pos.getY();
        }).thenComparingInt((structureBlockInfo) -> {
            return structureBlockInfo.pos.getX();
        }).thenComparingInt((structureBlockInfo) -> {
            return structureBlockInfo.pos.getZ();
        });
        list.sort(comparator);
        list3.sort(comparator);
        list2.sort(comparator);
        List<DefinedStructure.BlockInfo> list4 = Lists.newArrayList();
        list4.addAll(list);
        list4.addAll(list3);
        list4.addAll(list2);
        return list4;
    }

    private void fillEntityList(World world, BlockPosition firstCorner, BlockPosition secondCorner) {
        List<Entity> list = world.getEntitiesOfClass(Entity.class, new AxisAlignedBB(firstCorner, secondCorner), (entityx) -> {
            return !(entityx instanceof EntityHuman);
        });
        this.entityInfoList.clear();

        for(Entity entity : list) {
            Vec3D vec3 = new Vec3D(entity.locX() - (double)firstCorner.getX(), entity.locY() - (double)firstCorner.getY(), entity.locZ() - (double)firstCorner.getZ());
            NBTTagCompound compoundTag = new NBTTagCompound();
            entity.save(compoundTag);
            BlockPosition blockPos;
            if (entity instanceof EntityPainting) {
                blockPos = ((EntityPainting)entity).getBlockPosition().subtract(firstCorner);
            } else {
                blockPos = new BlockPosition(vec3);
            }

            this.entityInfoList.add(new DefinedStructure.EntityInfo(vec3, blockPos, compoundTag.c()));
        }

    }

    public List<DefinedStructure.BlockInfo> filterBlocks(BlockPosition pos, DefinedStructureInfo placementData, Block block) {
        return this.filterBlocks(pos, placementData, block, true);
    }

    public List<DefinedStructure.BlockInfo> filterBlocks(BlockPosition pos, DefinedStructureInfo placementData, Block block, boolean transformed) {
        List<DefinedStructure.BlockInfo> list = Lists.newArrayList();
        StructureBoundingBox boundingBox = placementData.getBoundingBox();
        if (this.palettes.isEmpty()) {
            return Collections.emptyList();
        } else {
            for(DefinedStructure.BlockInfo structureBlockInfo : placementData.getRandomPalette(this.palettes, pos).blocks(block)) {
                BlockPosition blockPos = transformed ? calculateRelativePosition(placementData, structureBlockInfo.pos).offset(pos) : structureBlockInfo.pos;
                if (boundingBox == null || boundingBox.isInside(blockPos)) {
                    list.add(new DefinedStructure.BlockInfo(blockPos, structureBlockInfo.state.rotate(placementData.getRotation()), structureBlockInfo.nbt));
                }
            }

            return list;
        }
    }

    public BlockPosition calculateConnectedPosition(DefinedStructureInfo placementData1, BlockPosition pos1, DefinedStructureInfo placementData2, BlockPosition pos2) {
        BlockPosition blockPos = calculateRelativePosition(placementData1, pos1);
        BlockPosition blockPos2 = calculateRelativePosition(placementData2, pos2);
        return blockPos.subtract(blockPos2);
    }

    public static BlockPosition calculateRelativePosition(DefinedStructureInfo placementData, BlockPosition pos) {
        return transform(pos, placementData.getMirror(), placementData.getRotation(), placementData.getRotationPivot());
    }

    public boolean placeInWorld(WorldAccess world, BlockPosition pos, BlockPosition pivot, DefinedStructureInfo placementData, Random random, int i) {
        if (this.palettes.isEmpty()) {
            return false;
        } else {
            List<DefinedStructure.BlockInfo> list = placementData.getRandomPalette(this.palettes, pos).blocks();
            if ((!list.isEmpty() || !placementData.isIgnoreEntities() && !this.entityInfoList.isEmpty()) && this.size.getX() >= 1 && this.size.getY() >= 1 && this.size.getZ() >= 1) {
                StructureBoundingBox boundingBox = placementData.getBoundingBox();
                List<BlockPosition> list2 = Lists.newArrayListWithCapacity(placementData.shouldKeepLiquids() ? list.size() : 0);
                List<BlockPosition> list3 = Lists.newArrayListWithCapacity(placementData.shouldKeepLiquids() ? list.size() : 0);
                List<Pair<BlockPosition, NBTTagCompound>> list4 = Lists.newArrayListWithCapacity(list.size());
                int j = Integer.MAX_VALUE;
                int k = Integer.MAX_VALUE;
                int l = Integer.MAX_VALUE;
                int m = Integer.MIN_VALUE;
                int n = Integer.MIN_VALUE;
                int o = Integer.MIN_VALUE;

                for(DefinedStructure.BlockInfo structureBlockInfo : processBlockInfos(world, pos, pivot, placementData, list)) {
                    BlockPosition blockPos = structureBlockInfo.pos;
                    if (boundingBox == null || boundingBox.isInside(blockPos)) {
                        Fluid fluidState = placementData.shouldKeepLiquids() ? world.getFluid(blockPos) : null;
                        IBlockData blockState = structureBlockInfo.state.mirror(placementData.getMirror()).rotate(placementData.getRotation());
                        if (structureBlockInfo.nbt != null) {
                            TileEntity blockEntity = world.getTileEntity(blockPos);
                            Clearable.tryClear(blockEntity);
                            world.setTypeAndData(blockPos, Blocks.BARRIER.getBlockData(), 20);
                        }

                        if (world.setTypeAndData(blockPos, blockState, i)) {
                            j = Math.min(j, blockPos.getX());
                            k = Math.min(k, blockPos.getY());
                            l = Math.min(l, blockPos.getZ());
                            m = Math.max(m, blockPos.getX());
                            n = Math.max(n, blockPos.getY());
                            o = Math.max(o, blockPos.getZ());
                            list4.add(Pair.of(blockPos, structureBlockInfo.nbt));
                            if (structureBlockInfo.nbt != null) {
                                TileEntity blockEntity2 = world.getTileEntity(blockPos);
                                if (blockEntity2 != null) {
                                    structureBlockInfo.nbt.setInt("x", blockPos.getX());
                                    structureBlockInfo.nbt.setInt("y", blockPos.getY());
                                    structureBlockInfo.nbt.setInt("z", blockPos.getZ());
                                    if (blockEntity2 instanceof TileEntityLootable) {
                                        structureBlockInfo.nbt.setLong("LootTableSeed", random.nextLong());
                                    }

                                    blockEntity2.load(structureBlockInfo.nbt);
                                }
                            }

                            if (fluidState != null) {
                                if (blockState.getFluid().isSource()) {
                                    list3.add(blockPos);
                                } else if (blockState.getBlock() instanceof IFluidContainer) {
                                    ((IFluidContainer)blockState.getBlock()).place(world, blockPos, blockState, fluidState);
                                    if (!fluidState.isSource()) {
                                        list2.add(blockPos);
                                    }
                                }
                            }
                        }
                    }
                }

                boolean bl = true;
                EnumDirection[] directions = new EnumDirection[]{EnumDirection.UP, EnumDirection.NORTH, EnumDirection.EAST, EnumDirection.SOUTH, EnumDirection.WEST};

                while(bl && !list2.isEmpty()) {
                    bl = false;
                    Iterator<BlockPosition> iterator = list2.iterator();

                    while(iterator.hasNext()) {
                        BlockPosition blockPos2 = iterator.next();
                        Fluid fluidState2 = world.getFluid(blockPos2);

                        for(int p = 0; p < directions.length && !fluidState2.isSource(); ++p) {
                            BlockPosition blockPos3 = blockPos2.relative(directions[p]);
                            Fluid fluidState3 = world.getFluid(blockPos3);
                            if (fluidState3.isSource() && !list3.contains(blockPos3)) {
                                fluidState2 = fluidState3;
                            }
                        }

                        if (fluidState2.isSource()) {
                            IBlockData blockState2 = world.getType(blockPos2);
                            Block block = blockState2.getBlock();
                            if (block instanceof IFluidContainer) {
                                ((IFluidContainer)block).place(world, blockPos2, blockState2, fluidState2);
                                bl = true;
                                iterator.remove();
                            }
                        }
                    }
                }

                if (j <= m) {
                    if (!placementData.getKnownShape()) {
                        VoxelShapeDiscrete discreteVoxelShape = new VoxelShapeBitSet(m - j + 1, n - k + 1, o - l + 1);
                        int q = j;
                        int r = k;
                        int s = l;

                        for(Pair<BlockPosition, NBTTagCompound> pair : list4) {
                            BlockPosition blockPos4 = pair.getFirst();
                            discreteVoxelShape.fill(blockPos4.getX() - q, blockPos4.getY() - r, blockPos4.getZ() - s);
                        }

                        updateShapeAtEdge(world, i, discreteVoxelShape, q, r, s);
                    }

                    for(Pair<BlockPosition, NBTTagCompound> pair2 : list4) {
                        BlockPosition blockPos5 = pair2.getFirst();
                        if (!placementData.getKnownShape()) {
                            IBlockData blockState3 = world.getType(blockPos5);
                            IBlockData blockState4 = Block.updateFromNeighbourShapes(blockState3, world, blockPos5);
                            if (blockState3 != blockState4) {
                                world.setTypeAndData(blockPos5, blockState4, i & -2 | 16);
                            }

                            world.update(blockPos5, blockState4.getBlock());
                        }

                        if (pair2.getSecond() != null) {
                            TileEntity blockEntity3 = world.getTileEntity(blockPos5);
                            if (blockEntity3 != null) {
                                blockEntity3.update();
                            }
                        }
                    }
                }

                if (!placementData.isIgnoreEntities()) {
                    this.placeEntities(world, pos, placementData.getMirror(), placementData.getRotation(), placementData.getRotationPivot(), boundingBox, placementData.shouldFinalizeEntities());
                }

                return true;
            } else {
                return false;
            }
        }
    }

    public static void updateShapeAtEdge(GeneratorAccess world, int flags, VoxelShapeDiscrete discreteVoxelShape, int startX, int startY, int startZ) {
        discreteVoxelShape.forAllFaces((direction, m, n, o) -> {
            BlockPosition blockPos = new BlockPosition(startX + m, startY + n, startZ + o);
            BlockPosition blockPos2 = blockPos.relative(direction);
            IBlockData blockState = world.getType(blockPos);
            IBlockData blockState2 = world.getType(blockPos2);
            IBlockData blockState3 = blockState.updateState(direction, blockState2, world, blockPos, blockPos2);
            if (blockState != blockState3) {
                world.setTypeAndData(blockPos, blockState3, flags & -2);
            }

            IBlockData blockState4 = blockState2.updateState(direction.opposite(), blockState3, world, blockPos2, blockPos);
            if (blockState2 != blockState4) {
                world.setTypeAndData(blockPos2, blockState4, flags & -2);
            }

        });
    }

    public static List<DefinedStructure.BlockInfo> processBlockInfos(GeneratorAccess world, BlockPosition pos, BlockPosition pivot, DefinedStructureInfo placementData, List<DefinedStructure.BlockInfo> list) {
        List<DefinedStructure.BlockInfo> list2 = Lists.newArrayList();

        for(DefinedStructure.BlockInfo structureBlockInfo : list) {
            BlockPosition blockPos = calculateRelativePosition(placementData, structureBlockInfo.pos).offset(pos);
            DefinedStructure.BlockInfo structureBlockInfo2 = new DefinedStructure.BlockInfo(blockPos, structureBlockInfo.state, structureBlockInfo.nbt != null ? structureBlockInfo.nbt.c() : null);

            for(Iterator<DefinedStructureProcessor> iterator = placementData.getProcessors().iterator(); structureBlockInfo2 != null && iterator.hasNext(); structureBlockInfo2 = iterator.next().processBlock(world, pos, pivot, structureBlockInfo, structureBlockInfo2, placementData)) {
            }

            if (structureBlockInfo2 != null) {
                list2.add(structureBlockInfo2);
            }
        }

        return list2;
    }

    private void placeEntities(WorldAccess world, BlockPosition pos, EnumBlockMirror mirror, EnumBlockRotation rotation, BlockPosition pivot, @Nullable StructureBoundingBox area, boolean bl) {
        for(DefinedStructure.EntityInfo structureEntityInfo : this.entityInfoList) {
            BlockPosition blockPos = transform(structureEntityInfo.blockPos, mirror, rotation, pivot).offset(pos);
            if (area == null || area.isInside(blockPos)) {
                NBTTagCompound compoundTag = structureEntityInfo.nbt.c();
                Vec3D vec3 = transform(structureEntityInfo.pos, mirror, rotation, pivot);
                Vec3D vec32 = vec3.add((double)pos.getX(), (double)pos.getY(), (double)pos.getZ());
                NBTTagList listTag = new NBTTagList();
                listTag.add(NBTTagDouble.valueOf(vec32.x));
                listTag.add(NBTTagDouble.valueOf(vec32.y));
                listTag.add(NBTTagDouble.valueOf(vec32.z));
                compoundTag.set("Pos", listTag);
                compoundTag.remove("UUID");
                createEntityIgnoreException(world, compoundTag).ifPresent((entity) -> {
                    float f = entity.mirror(mirror);
                    f = f + (entity.getYRot() - entity.rotate(rotation));
                    entity.setPositionRotation(vec32.x, vec32.y, vec32.z, f, entity.getXRot());
                    if (bl && entity instanceof EntityInsentient) {
                        ((EntityInsentient)entity).prepare(world, world.getDamageScaler(new BlockPosition(vec32)), EnumMobSpawn.STRUCTURE, (GroupDataEntity)null, compoundTag);
                    }

                    world.addAllEntities(entity);
                });
            }
        }

    }

    private static Optional<Entity> createEntityIgnoreException(WorldAccess world, NBTTagCompound nbt) {
        try {
            return EntityTypes.create(nbt, world.getLevel());
        } catch (Exception var3) {
            return Optional.empty();
        }
    }

    public BaseBlockPosition getSize(EnumBlockRotation rotation) {
        switch(rotation) {
        case COUNTERCLOCKWISE_90:
        case CLOCKWISE_90:
            return new BaseBlockPosition(this.size.getZ(), this.size.getY(), this.size.getX());
        default:
            return this.size;
        }
    }

    public static BlockPosition transform(BlockPosition pos, EnumBlockMirror mirror, EnumBlockRotation rotation, BlockPosition pivot) {
        int i = pos.getX();
        int j = pos.getY();
        int k = pos.getZ();
        boolean bl = true;
        switch(mirror) {
        case LEFT_RIGHT:
            k = -k;
            break;
        case FRONT_BACK:
            i = -i;
            break;
        default:
            bl = false;
        }

        int l = pivot.getX();
        int m = pivot.getZ();
        switch(rotation) {
        case COUNTERCLOCKWISE_90:
            return new BlockPosition(l - m + k, j, l + m - i);
        case CLOCKWISE_90:
            return new BlockPosition(l + m - k, j, m - l + i);
        case CLOCKWISE_180:
            return new BlockPosition(l + l - i, j, m + m - k);
        default:
            return bl ? new BlockPosition(i, j, k) : pos;
        }
    }

    public static Vec3D transform(Vec3D point, EnumBlockMirror mirror, EnumBlockRotation rotation, BlockPosition pivot) {
        double d = point.x;
        double e = point.y;
        double f = point.z;
        boolean bl = true;
        switch(mirror) {
        case LEFT_RIGHT:
            f = 1.0D - f;
            break;
        case FRONT_BACK:
            d = 1.0D - d;
            break;
        default:
            bl = false;
        }

        int i = pivot.getX();
        int j = pivot.getZ();
        switch(rotation) {
        case COUNTERCLOCKWISE_90:
            return new Vec3D((double)(i - j) + f, e, (double)(i + j + 1) - d);
        case CLOCKWISE_90:
            return new Vec3D((double)(i + j + 1) - f, e, (double)(j - i) + d);
        case CLOCKWISE_180:
            return new Vec3D((double)(i + i + 1) - d, e, (double)(j + j + 1) - f);
        default:
            return bl ? new Vec3D(d, e, f) : point;
        }
    }

    public BlockPosition getZeroPositionWithTransform(BlockPosition pos, EnumBlockMirror mirror, EnumBlockRotation rotation) {
        return getZeroPositionWithTransform(pos, mirror, rotation, this.getSize().getX(), this.getSize().getZ());
    }

    public static BlockPosition getZeroPositionWithTransform(BlockPosition pos, EnumBlockMirror mirror, EnumBlockRotation rotation, int offsetX, int offsetZ) {
        --offsetX;
        --offsetZ;
        int i = mirror == EnumBlockMirror.FRONT_BACK ? offsetX : 0;
        int j = mirror == EnumBlockMirror.LEFT_RIGHT ? offsetZ : 0;
        BlockPosition blockPos = pos;
        switch(rotation) {
        case COUNTERCLOCKWISE_90:
            blockPos = pos.offset(j, 0, offsetX - i);
            break;
        case CLOCKWISE_90:
            blockPos = pos.offset(offsetZ - j, 0, i);
            break;
        case CLOCKWISE_180:
            blockPos = pos.offset(offsetX - i, 0, offsetZ - j);
            break;
        case NONE:
            blockPos = pos.offset(i, 0, j);
        }

        return blockPos;
    }

    public StructureBoundingBox getBoundingBox(DefinedStructureInfo placementData, BlockPosition pos) {
        return this.getBoundingBox(pos, placementData.getRotation(), placementData.getRotationPivot(), placementData.getMirror());
    }

    public StructureBoundingBox getBoundingBox(BlockPosition pos, EnumBlockRotation rotation, BlockPosition pivot, EnumBlockMirror mirror) {
        return getBoundingBox(pos, rotation, pivot, mirror, this.size);
    }

    @VisibleForTesting
    protected static StructureBoundingBox getBoundingBox(BlockPosition pos, EnumBlockRotation rotation, BlockPosition pivot, EnumBlockMirror mirror, BaseBlockPosition dimensions) {
        BaseBlockPosition vec3i = dimensions.offset(-1, -1, -1);
        BlockPosition blockPos = transform(BlockPosition.ZERO, mirror, rotation, pivot);
        BlockPosition blockPos2 = transform(BlockPosition.ZERO.offset(vec3i), mirror, rotation, pivot);
        return StructureBoundingBox.fromCorners(blockPos, blockPos2).move(pos);
    }

    public NBTTagCompound save(NBTTagCompound nbt) {
        if (this.palettes.isEmpty()) {
            nbt.set("blocks", new NBTTagList());
            nbt.set("palette", new NBTTagList());
        } else {
            List<DefinedStructure.SimplePalette> list = Lists.newArrayList();
            DefinedStructure.SimplePalette simplePalette = new DefinedStructure.SimplePalette();
            list.add(simplePalette);

            for(int i = 1; i < this.palettes.size(); ++i) {
                list.add(new DefinedStructure.SimplePalette());
            }

            NBTTagList listTag = new NBTTagList();
            List<DefinedStructure.BlockInfo> list2 = this.palettes.get(0).blocks();

            for(int j = 0; j < list2.size(); ++j) {
                DefinedStructure.BlockInfo structureBlockInfo = list2.get(j);
                NBTTagCompound compoundTag = new NBTTagCompound();
                compoundTag.set("pos", this.newIntegerList(structureBlockInfo.pos.getX(), structureBlockInfo.pos.getY(), structureBlockInfo.pos.getZ()));
                int k = simplePalette.idFor(structureBlockInfo.state);
                compoundTag.setInt("state", k);
                if (structureBlockInfo.nbt != null) {
                    compoundTag.set("nbt", structureBlockInfo.nbt);
                }

                listTag.add(compoundTag);

                for(int l = 1; l < this.palettes.size(); ++l) {
                    DefinedStructure.SimplePalette simplePalette2 = list.get(l);
                    simplePalette2.addMapping((this.palettes.get(l).blocks().get(j)).state, k);
                }
            }

            nbt.set("blocks", listTag);
            if (list.size() == 1) {
                NBTTagList listTag2 = new NBTTagList();

                for(IBlockData blockState : simplePalette) {
                    listTag2.add(GameProfileSerializer.writeBlockState(blockState));
                }

                nbt.set("palette", listTag2);
            } else {
                NBTTagList listTag3 = new NBTTagList();

                for(DefinedStructure.SimplePalette simplePalette3 : list) {
                    NBTTagList listTag4 = new NBTTagList();

                    for(IBlockData blockState2 : simplePalette3) {
                        listTag4.add(GameProfileSerializer.writeBlockState(blockState2));
                    }

                    listTag3.add(listTag4);
                }

                nbt.set("palettes", listTag3);
            }
        }

        NBTTagList listTag5 = new NBTTagList();

        for(DefinedStructure.EntityInfo structureEntityInfo : this.entityInfoList) {
            NBTTagCompound compoundTag2 = new NBTTagCompound();
            compoundTag2.set("pos", this.newDoubleList(structureEntityInfo.pos.x, structureEntityInfo.pos.y, structureEntityInfo.pos.z));
            compoundTag2.set("blockPos", this.newIntegerList(structureEntityInfo.blockPos.getX(), structureEntityInfo.blockPos.getY(), structureEntityInfo.blockPos.getZ()));
            if (structureEntityInfo.nbt != null) {
                compoundTag2.set("nbt", structureEntityInfo.nbt);
            }

            listTag5.add(compoundTag2);
        }

        nbt.set("entities", listTag5);
        nbt.set("size", this.newIntegerList(this.size.getX(), this.size.getY(), this.size.getZ()));
        nbt.setInt("DataVersion", SharedConstants.getGameVersion().getWorldVersion());
        return nbt;
    }

    public void load(NBTTagCompound nbt) {
        this.palettes.clear();
        this.entityInfoList.clear();
        NBTTagList listTag = nbt.getList("size", 3);
        this.size = new BaseBlockPosition(listTag.getInt(0), listTag.getInt(1), listTag.getInt(2));
        NBTTagList listTag2 = nbt.getList("blocks", 10);
        if (nbt.hasKeyOfType("palettes", 9)) {
            NBTTagList listTag3 = nbt.getList("palettes", 9);

            for(int i = 0; i < listTag3.size(); ++i) {
                this.loadPalette(listTag3.getList(i), listTag2);
            }
        } else {
            this.loadPalette(nbt.getList("palette", 10), listTag2);
        }

        NBTTagList listTag4 = nbt.getList("entities", 10);

        for(int j = 0; j < listTag4.size(); ++j) {
            NBTTagCompound compoundTag = listTag4.getCompound(j);
            NBTTagList listTag5 = compoundTag.getList("pos", 6);
            Vec3D vec3 = new Vec3D(listTag5.getDouble(0), listTag5.getDouble(1), listTag5.getDouble(2));
            NBTTagList listTag6 = compoundTag.getList("blockPos", 3);
            BlockPosition blockPos = new BlockPosition(listTag6.getInt(0), listTag6.getInt(1), listTag6.getInt(2));
            if (compoundTag.hasKey("nbt")) {
                NBTTagCompound compoundTag2 = compoundTag.getCompound("nbt");
                this.entityInfoList.add(new DefinedStructure.EntityInfo(vec3, blockPos, compoundTag2));
            }
        }

    }

    private void loadPalette(NBTTagList paletteNbt, NBTTagList blocksNbt) {
        DefinedStructure.SimplePalette simplePalette = new DefinedStructure.SimplePalette();

        for(int i = 0; i < paletteNbt.size(); ++i) {
            simplePalette.addMapping(GameProfileSerializer.readBlockState(paletteNbt.getCompound(i)), i);
        }

        List<DefinedStructure.BlockInfo> list = Lists.newArrayList();
        List<DefinedStructure.BlockInfo> list2 = Lists.newArrayList();
        List<DefinedStructure.BlockInfo> list3 = Lists.newArrayList();

        for(int j = 0; j < blocksNbt.size(); ++j) {
            NBTTagCompound compoundTag = blocksNbt.getCompound(j);
            NBTTagList listTag = compoundTag.getList("pos", 3);
            BlockPosition blockPos = new BlockPosition(listTag.getInt(0), listTag.getInt(1), listTag.getInt(2));
            IBlockData blockState = simplePalette.stateFor(compoundTag.getInt("state"));
            NBTTagCompound compoundTag2;
            if (compoundTag.hasKey("nbt")) {
                compoundTag2 = compoundTag.getCompound("nbt");
            } else {
                compoundTag2 = null;
            }

            DefinedStructure.BlockInfo structureBlockInfo = new DefinedStructure.BlockInfo(blockPos, blockState, compoundTag2);
            addToLists(structureBlockInfo, list, list2, list3);
        }

        List<DefinedStructure.BlockInfo> list4 = buildInfoList(list, list2, list3);
        this.palettes.add(new DefinedStructure.Palette(list4));
    }

    private NBTTagList newIntegerList(int... ints) {
        NBTTagList listTag = new NBTTagList();

        for(int i : ints) {
            listTag.add(NBTTagInt.valueOf(i));
        }

        return listTag;
    }

    private NBTTagList newDoubleList(double... doubles) {
        NBTTagList listTag = new NBTTagList();

        for(double d : doubles) {
            listTag.add(NBTTagDouble.valueOf(d));
        }

        return listTag;
    }

    public static class BlockInfo {
        public final BlockPosition pos;
        public final IBlockData state;
        public final NBTTagCompound nbt;

        public BlockInfo(BlockPosition pos, IBlockData state, @Nullable NBTTagCompound nbt) {
            this.pos = pos;
            this.state = state;
            this.nbt = nbt;
        }

        @Override
        public String toString() {
            return String.format("<StructureBlockInfo | %s | %s | %s>", this.pos, this.state, this.nbt);
        }
    }

    public static class EntityInfo {
        public final Vec3D pos;
        public final BlockPosition blockPos;
        public final NBTTagCompound nbt;

        public EntityInfo(Vec3D pos, BlockPosition blockPos, NBTTagCompound nbt) {
            this.pos = pos;
            this.blockPos = blockPos;
            this.nbt = nbt;
        }
    }

    public static final class Palette {
        private final List<DefinedStructure.BlockInfo> blocks;
        private final Map<Block, List<DefinedStructure.BlockInfo>> cache = Maps.newHashMap();

        Palette(List<DefinedStructure.BlockInfo> infos) {
            this.blocks = infos;
        }

        public List<DefinedStructure.BlockInfo> blocks() {
            return this.blocks;
        }

        public List<DefinedStructure.BlockInfo> blocks(Block block) {
            return this.cache.computeIfAbsent(block, (blockx) -> {
                return this.blocks.stream().filter((structureBlockInfo) -> {
                    return structureBlockInfo.state.is(blockx);
                }).collect(Collectors.toList());
            });
        }
    }

    static class SimplePalette implements Iterable<IBlockData> {
        public static final IBlockData DEFAULT_BLOCK_STATE = Blocks.AIR.getBlockData();
        private final RegistryBlockID<IBlockData> ids = new RegistryBlockID<>(16);
        private int lastId;

        public int idFor(IBlockData state) {
            int i = this.ids.getId(state);
            if (i == -1) {
                i = this.lastId++;
                this.ids.addMapping(state, i);
            }

            return i;
        }

        @Nullable
        public IBlockData stateFor(int id) {
            IBlockData blockState = this.ids.fromId(id);
            return blockState == null ? DEFAULT_BLOCK_STATE : blockState;
        }

        @Override
        public Iterator<IBlockData> iterator() {
            return this.ids.iterator();
        }

        public void addMapping(IBlockData state, int id) {
            this.ids.addMapping(state, id);
        }
    }
}
