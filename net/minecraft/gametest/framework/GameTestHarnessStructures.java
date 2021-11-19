package net.minecraft.gametest.framework;

import com.google.common.collect.Lists;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.commands.arguments.blocks.ArgumentTileLocation;
import net.minecraft.core.BaseBlockPosition;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.data.structures.DebugReportProviderNBT;
import net.minecraft.data.structures.StructureUpdater;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.DispenserRegistry;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnumBlockMirror;
import net.minecraft.world.level.block.EnumBlockRotation;
import net.minecraft.world.level.block.entity.TileEntityCommand;
import net.minecraft.world.level.block.entity.TileEntityStructure;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockPropertyStructureMode;
import net.minecraft.world.level.levelgen.flat.GeneratorSettingsFlat;
import net.minecraft.world.level.levelgen.structure.StructureBoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureManager;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.Vec3D;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GameTestHarnessStructures {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final String DEFAULT_TEST_STRUCTURES_DIR = "gameteststructures";
    public static String testStructuresDir = "gameteststructures";
    private static final int HOW_MANY_CHUNKS_TO_LOAD_IN_EACH_DIRECTION_OF_STRUCTURE = 4;

    public static EnumBlockRotation getRotationForRotationSteps(int steps) {
        switch(steps) {
        case 0:
            return EnumBlockRotation.NONE;
        case 1:
            return EnumBlockRotation.CLOCKWISE_90;
        case 2:
            return EnumBlockRotation.CLOCKWISE_180;
        case 3:
            return EnumBlockRotation.COUNTERCLOCKWISE_90;
        default:
            throw new IllegalArgumentException("rotationSteps must be a value from 0-3. Got value " + steps);
        }
    }

    public static int getRotationStepsForRotation(EnumBlockRotation rotation) {
        switch(rotation) {
        case NONE:
            return 0;
        case CLOCKWISE_90:
            return 1;
        case CLOCKWISE_180:
            return 2;
        case COUNTERCLOCKWISE_90:
            return 3;
        default:
            throw new IllegalArgumentException("Unknown rotation value, don't know how many steps it represents: " + rotation);
        }
    }

    public static void main(String[] args) throws IOException {
        DispenserRegistry.init();
        Files.walk(Paths.get(testStructuresDir)).filter((path) -> {
            return path.toString().endsWith(".snbt");
        }).forEach((path) -> {
            try {
                String string = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
                NBTTagCompound compoundTag = GameProfileSerializer.snbtToStructure(string);
                NBTTagCompound compoundTag2 = StructureUpdater.update(path.toString(), compoundTag);
                DebugReportProviderNBT.writeSnbt(path, GameProfileSerializer.structureToSnbt(compoundTag2));
            } catch (IOException | CommandSyntaxException var4) {
                LOGGER.error("Something went wrong upgrading: {}", path, var4);
            }

        });
    }

    public static AxisAlignedBB getStructureBounds(TileEntityStructure structureBlockEntity) {
        BlockPosition blockPos = structureBlockEntity.getPosition();
        BlockPosition blockPos2 = blockPos.offset(structureBlockEntity.getStructureSize().offset(-1, -1, -1));
        BlockPosition blockPos3 = DefinedStructure.transform(blockPos2, EnumBlockMirror.NONE, structureBlockEntity.getRotation(), blockPos);
        return new AxisAlignedBB(blockPos, blockPos3);
    }

    public static StructureBoundingBox getStructureBoundingBox(TileEntityStructure structureBlockEntity) {
        BlockPosition blockPos = structureBlockEntity.getPosition();
        BlockPosition blockPos2 = blockPos.offset(structureBlockEntity.getStructureSize().offset(-1, -1, -1));
        BlockPosition blockPos3 = DefinedStructure.transform(blockPos2, EnumBlockMirror.NONE, structureBlockEntity.getRotation(), blockPos);
        return StructureBoundingBox.fromCorners(blockPos, blockPos3);
    }

    public static void addCommandBlockAndButtonToStartTest(BlockPosition pos, BlockPosition relativePos, EnumBlockRotation rotation, WorldServer world) {
        BlockPosition blockPos = DefinedStructure.transform(pos.offset(relativePos), EnumBlockMirror.NONE, rotation, pos);
        world.setTypeUpdate(blockPos, Blocks.COMMAND_BLOCK.getBlockData());
        TileEntityCommand commandBlockEntity = (TileEntityCommand)world.getTileEntity(blockPos);
        commandBlockEntity.getCommandBlock().setCommand("test runthis");
        BlockPosition blockPos2 = DefinedStructure.transform(blockPos.offset(0, 0, -1), EnumBlockMirror.NONE, rotation, blockPos);
        world.setTypeUpdate(blockPos2, Blocks.STONE_BUTTON.getBlockData().rotate(rotation));
    }

    public static void createNewEmptyStructureBlock(String structure, BlockPosition pos, BaseBlockPosition relativePos, EnumBlockRotation rotation, WorldServer world) {
        StructureBoundingBox boundingBox = getStructureBoundingBox(pos, relativePos, rotation);
        clearSpaceForStructure(boundingBox, pos.getY(), world);
        world.setTypeUpdate(pos, Blocks.STRUCTURE_BLOCK.getBlockData());
        TileEntityStructure structureBlockEntity = (TileEntityStructure)world.getTileEntity(pos);
        structureBlockEntity.setIgnoreEntities(false);
        structureBlockEntity.setStructureName(new MinecraftKey(structure));
        structureBlockEntity.setStructureSize(relativePos);
        structureBlockEntity.setUsageMode(BlockPropertyStructureMode.SAVE);
        structureBlockEntity.setShowBoundingBox(true);
    }

    public static TileEntityStructure spawnStructure(String structureName, BlockPosition pos, EnumBlockRotation rotation, int i, WorldServer world, boolean bl) {
        BaseBlockPosition vec3i = getStructureTemplate(structureName, world).getSize();
        StructureBoundingBox boundingBox = getStructureBoundingBox(pos, vec3i, rotation);
        BlockPosition blockPos;
        if (rotation == EnumBlockRotation.NONE) {
            blockPos = pos;
        } else if (rotation == EnumBlockRotation.CLOCKWISE_90) {
            blockPos = pos.offset(vec3i.getZ() - 1, 0, 0);
        } else if (rotation == EnumBlockRotation.CLOCKWISE_180) {
            blockPos = pos.offset(vec3i.getX() - 1, 0, vec3i.getZ() - 1);
        } else {
            if (rotation != EnumBlockRotation.COUNTERCLOCKWISE_90) {
                throw new IllegalArgumentException("Invalid rotation: " + rotation);
            }

            blockPos = pos.offset(0, 0, vec3i.getX() - 1);
        }

        forceLoadChunks(pos, world);
        clearSpaceForStructure(boundingBox, pos.getY(), world);
        TileEntityStructure structureBlockEntity = createStructureBlock(structureName, blockPos, rotation, world, bl);
        world.getBlockTicks().fetchTicksInArea(boundingBox, true, false);
        world.clearBlockEvents(boundingBox);
        return structureBlockEntity;
    }

    private static void forceLoadChunks(BlockPosition pos, WorldServer world) {
        ChunkCoordIntPair chunkPos = new ChunkCoordIntPair(pos);

        for(int i = -1; i < 4; ++i) {
            for(int j = -1; j < 4; ++j) {
                int k = chunkPos.x + i;
                int l = chunkPos.z + j;
                world.setForceLoaded(k, l, true);
            }
        }

    }

    public static void clearSpaceForStructure(StructureBoundingBox area, int altitude, WorldServer world) {
        StructureBoundingBox boundingBox = new StructureBoundingBox(area.minX() - 2, area.minY() - 3, area.minZ() - 3, area.maxX() + 3, area.maxY() + 20, area.maxZ() + 3);
        BlockPosition.betweenClosedStream(boundingBox).forEach((pos) -> {
            clearBlock(altitude, pos, world);
        });
        world.getBlockTicks().fetchTicksInArea(boundingBox, true, false);
        world.clearBlockEvents(boundingBox);
        AxisAlignedBB aABB = new AxisAlignedBB((double)boundingBox.minX(), (double)boundingBox.minY(), (double)boundingBox.minZ(), (double)boundingBox.maxX(), (double)boundingBox.maxY(), (double)boundingBox.maxZ());
        List<Entity> list = world.getEntitiesOfClass(Entity.class, aABB, (entity) -> {
            return !(entity instanceof EntityHuman);
        });
        list.forEach(Entity::die);
    }

    public static StructureBoundingBox getStructureBoundingBox(BlockPosition pos, BaseBlockPosition relativePos, EnumBlockRotation rotation) {
        BlockPosition blockPos = pos.offset(relativePos).offset(-1, -1, -1);
        BlockPosition blockPos2 = DefinedStructure.transform(blockPos, EnumBlockMirror.NONE, rotation, pos);
        StructureBoundingBox boundingBox = StructureBoundingBox.fromCorners(pos, blockPos2);
        int i = Math.min(boundingBox.minX(), boundingBox.maxX());
        int j = Math.min(boundingBox.minZ(), boundingBox.maxZ());
        return boundingBox.move(pos.getX() - i, 0, pos.getZ() - j);
    }

    public static Optional<BlockPosition> findStructureBlockContainingPos(BlockPosition pos, int radius, WorldServer world) {
        return findStructureBlocks(pos, radius, world).stream().filter((structureBlockPos) -> {
            return doesStructureContain(structureBlockPos, pos, world);
        }).findFirst();
    }

    @Nullable
    public static BlockPosition findNearestStructureBlock(BlockPosition pos, int radius, WorldServer world) {
        Comparator<BlockPosition> comparator = Comparator.comparingInt((posx) -> {
            return posx.distManhattan(pos);
        });
        Collection<BlockPosition> collection = findStructureBlocks(pos, radius, world);
        Optional<BlockPosition> optional = collection.stream().min(comparator);
        return optional.orElse((BlockPosition)null);
    }

    public static Collection<BlockPosition> findStructureBlocks(BlockPosition pos, int radius, WorldServer world) {
        Collection<BlockPosition> collection = Lists.newArrayList();
        AxisAlignedBB aABB = new AxisAlignedBB(pos);
        aABB = aABB.inflate((double)radius);

        for(int i = (int)aABB.minX; i <= (int)aABB.maxX; ++i) {
            for(int j = (int)aABB.minY; j <= (int)aABB.maxY; ++j) {
                for(int k = (int)aABB.minZ; k <= (int)aABB.maxZ; ++k) {
                    BlockPosition blockPos = new BlockPosition(i, j, k);
                    IBlockData blockState = world.getType(blockPos);
                    if (blockState.is(Blocks.STRUCTURE_BLOCK)) {
                        collection.add(blockPos);
                    }
                }
            }
        }

        return collection;
    }

    private static DefinedStructure getStructureTemplate(String structureId, WorldServer world) {
        DefinedStructureManager structureManager = world.getStructureManager();
        Optional<DefinedStructure> optional = structureManager.get(new MinecraftKey(structureId));
        if (optional.isPresent()) {
            return optional.get();
        } else {
            String string = structureId + ".snbt";
            Path path = Paths.get(testStructuresDir, string);
            NBTTagCompound compoundTag = tryLoadStructure(path);
            if (compoundTag == null) {
                throw new RuntimeException("Could not find structure file " + path + ", and the structure is not available in the world structures either.");
            } else {
                return structureManager.readStructure(compoundTag);
            }
        }
    }

    private static TileEntityStructure createStructureBlock(String name, BlockPosition pos, EnumBlockRotation rotation, WorldServer world, boolean bl) {
        world.setTypeUpdate(pos, Blocks.STRUCTURE_BLOCK.getBlockData());
        TileEntityStructure structureBlockEntity = (TileEntityStructure)world.getTileEntity(pos);
        structureBlockEntity.setUsageMode(BlockPropertyStructureMode.LOAD);
        structureBlockEntity.setRotation(rotation);
        structureBlockEntity.setIgnoreEntities(false);
        structureBlockEntity.setStructureName(new MinecraftKey(name));
        structureBlockEntity.loadStructure(world, bl);
        if (structureBlockEntity.getStructureSize() != BaseBlockPosition.ZERO) {
            return structureBlockEntity;
        } else {
            DefinedStructure structureTemplate = getStructureTemplate(name, world);
            structureBlockEntity.loadStructure(world, bl, structureTemplate);
            if (structureBlockEntity.getStructureSize() == BaseBlockPosition.ZERO) {
                throw new RuntimeException("Failed to load structure " + name);
            } else {
                return structureBlockEntity;
            }
        }
    }

    @Nullable
    private static NBTTagCompound tryLoadStructure(Path path) {
        try {
            BufferedReader bufferedReader = Files.newBufferedReader(path);
            String string = IOUtils.toString((Reader)bufferedReader);
            return GameProfileSerializer.snbtToStructure(string);
        } catch (IOException var3) {
            return null;
        } catch (CommandSyntaxException var4) {
            throw new RuntimeException("Error while trying to load structure " + path, var4);
        }
    }

    private static void clearBlock(int altitude, BlockPosition pos, WorldServer world) {
        IBlockData blockState = null;
        GeneratorSettingsFlat flatLevelGeneratorSettings = GeneratorSettingsFlat.getDefault(world.registryAccess().registryOrThrow(IRegistry.BIOME_REGISTRY));
        if (flatLevelGeneratorSettings instanceof GeneratorSettingsFlat) {
            List<IBlockData> list = flatLevelGeneratorSettings.getLayers();
            int i = pos.getY() - world.getMinBuildHeight();
            if (pos.getY() < altitude && i > 0 && i <= list.size()) {
                blockState = list.get(i - 1);
            }
        } else if (pos.getY() == altitude - 1) {
            blockState = world.getBiome(pos).getGenerationSettings().getSurfaceBuilderConfig().getTopMaterial();
        } else if (pos.getY() < altitude - 1) {
            blockState = world.getBiome(pos).getGenerationSettings().getSurfaceBuilderConfig().getUnderMaterial();
        }

        if (blockState == null) {
            blockState = Blocks.AIR.getBlockData();
        }

        ArgumentTileLocation blockInput = new ArgumentTileLocation(blockState, Collections.emptySet(), (NBTTagCompound)null);
        blockInput.place(world, pos, 2);
        world.update(pos, blockState.getBlock());
    }

    private static boolean doesStructureContain(BlockPosition structureBlockPos, BlockPosition pos, WorldServer world) {
        TileEntityStructure structureBlockEntity = (TileEntityStructure)world.getTileEntity(structureBlockPos);
        AxisAlignedBB aABB = getStructureBounds(structureBlockEntity).inflate(1.0D);
        return aABB.contains(Vec3D.atCenterOf(pos));
    }
}
