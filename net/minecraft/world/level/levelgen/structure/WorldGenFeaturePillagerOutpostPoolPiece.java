package net.minecraft.world.level.levelgen.structure;

import com.google.common.collect.Lists;
import com.mojang.serialization.Dynamic;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.DynamicOpsNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.resources.RegistryReadOps;
import net.minecraft.resources.RegistryWriteOps;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.GeneratorAccessSeed;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.EnumBlockRotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.WorldGenFeatureStructurePieceType;
import net.minecraft.world.level.levelgen.feature.structures.WorldGenFeatureDefinedStructureJigsawJunction;
import net.minecraft.world.level.levelgen.feature.structures.WorldGenFeatureDefinedStructurePoolStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WorldGenFeaturePillagerOutpostPoolPiece extends StructurePiece {
    private static final Logger LOGGER = LogManager.getLogger();
    protected final WorldGenFeatureDefinedStructurePoolStructure element;
    protected BlockPosition position;
    private final int groundLevelDelta;
    protected final EnumBlockRotation rotation;
    private final List<WorldGenFeatureDefinedStructureJigsawJunction> junctions = Lists.newArrayList();
    private final DefinedStructureManager structureManager;

    public WorldGenFeaturePillagerOutpostPoolPiece(DefinedStructureManager structureManager, WorldGenFeatureDefinedStructurePoolStructure poolElement, BlockPosition pos, int groundLevelDelta, EnumBlockRotation rotation, StructureBoundingBox boundingBox) {
        super(WorldGenFeatureStructurePieceType.JIGSAW, 0, boundingBox);
        this.structureManager = structureManager;
        this.element = poolElement;
        this.position = pos;
        this.groundLevelDelta = groundLevelDelta;
        this.rotation = rotation;
    }

    public WorldGenFeaturePillagerOutpostPoolPiece(WorldServer world, NBTTagCompound nbt) {
        super(WorldGenFeatureStructurePieceType.JIGSAW, nbt);
        this.structureManager = world.getStructureManager();
        this.position = new BlockPosition(nbt.getInt("PosX"), nbt.getInt("PosY"), nbt.getInt("PosZ"));
        this.groundLevelDelta = nbt.getInt("ground_level_delta");
        RegistryReadOps<NBTBase> registryReadOps = RegistryReadOps.create(DynamicOpsNBT.INSTANCE, world.getMinecraftServer().getResourceManager(), world.getMinecraftServer().getCustomRegistry());
        this.element = WorldGenFeatureDefinedStructurePoolStructure.CODEC.parse(registryReadOps, nbt.getCompound("pool_element")).resultOrPartial(LOGGER::error).orElseThrow(() -> {
            return new IllegalStateException("Invalid pool element found");
        });
        this.rotation = EnumBlockRotation.valueOf(nbt.getString("rotation"));
        this.boundingBox = this.element.getBoundingBox(this.structureManager, this.position, this.rotation);
        NBTTagList listTag = nbt.getList("junctions", 10);
        this.junctions.clear();
        listTag.forEach((tag) -> {
            this.junctions.add(WorldGenFeatureDefinedStructureJigsawJunction.deserialize(new Dynamic<>(registryReadOps, tag)));
        });
    }

    @Override
    protected void addAdditionalSaveData(WorldServer world, NBTTagCompound nbt) {
        nbt.setInt("PosX", this.position.getX());
        nbt.setInt("PosY", this.position.getY());
        nbt.setInt("PosZ", this.position.getZ());
        nbt.setInt("ground_level_delta", this.groundLevelDelta);
        RegistryWriteOps<NBTBase> registryWriteOps = RegistryWriteOps.create(DynamicOpsNBT.INSTANCE, world.getMinecraftServer().getCustomRegistry());
        WorldGenFeatureDefinedStructurePoolStructure.CODEC.encodeStart(registryWriteOps, this.element).resultOrPartial(LOGGER::error).ifPresent((tag) -> {
            nbt.set("pool_element", tag);
        });
        nbt.setString("rotation", this.rotation.name());
        NBTTagList listTag = new NBTTagList();

        for(WorldGenFeatureDefinedStructureJigsawJunction jigsawJunction : this.junctions) {
            listTag.add(jigsawJunction.serialize(registryWriteOps).getValue());
        }

        nbt.set("junctions", listTag);
    }

    @Override
    public boolean postProcess(GeneratorAccessSeed world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, StructureBoundingBox boundingBox, ChunkCoordIntPair chunkPos, BlockPosition pos) {
        return this.place(world, structureAccessor, chunkGenerator, random, boundingBox, pos, false);
    }

    public boolean place(GeneratorAccessSeed world, StructureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, StructureBoundingBox boundingBox, BlockPosition pos, boolean keepJigsaws) {
        return this.element.place(this.structureManager, world, structureAccessor, chunkGenerator, this.position, pos, this.rotation, boundingBox, random, keepJigsaws);
    }

    @Override
    public void move(int x, int y, int z) {
        super.move(x, y, z);
        this.position = this.position.offset(x, y, z);
    }

    @Override
    public EnumBlockRotation getRotation() {
        return this.rotation;
    }

    @Override
    public String toString() {
        return String.format("<%s | %s | %s | %s>", this.getClass().getSimpleName(), this.position, this.rotation, this.element);
    }

    public WorldGenFeatureDefinedStructurePoolStructure getElement() {
        return this.element;
    }

    public BlockPosition getPosition() {
        return this.position;
    }

    public int getGroundLevelDelta() {
        return this.groundLevelDelta;
    }

    public void addJunction(WorldGenFeatureDefinedStructureJigsawJunction junction) {
        this.junctions.add(junction);
    }

    public List<WorldGenFeatureDefinedStructureJigsawJunction> getJunctions() {
        return this.junctions;
    }
}
