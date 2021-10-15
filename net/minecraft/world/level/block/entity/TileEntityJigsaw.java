package net.minecraft.world.level.block.entity;

import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BaseBlockPosition;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.game.PacketPlayOutTileEntityData;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.INamable;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockJigsaw;
import net.minecraft.world.level.block.EnumBlockRotation;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.structures.WorldGenFeatureDefinedStructureJigsawPlacement;
import net.minecraft.world.level.levelgen.feature.structures.WorldGenFeatureDefinedStructurePoolSingle;
import net.minecraft.world.level.levelgen.feature.structures.WorldGenFeatureDefinedStructurePoolStructure;
import net.minecraft.world.level.levelgen.structure.StructureBoundingBox;
import net.minecraft.world.level.levelgen.structure.WorldGenFeaturePillagerOutpostPoolPiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureManager;

public class TileEntityJigsaw extends TileEntity {
    public static final String TARGET = "target";
    public static final String POOL = "pool";
    public static final String JOINT = "joint";
    public static final String NAME = "name";
    public static final String FINAL_STATE = "final_state";
    private MinecraftKey name = new MinecraftKey("empty");
    private MinecraftKey target = new MinecraftKey("empty");
    private MinecraftKey pool = new MinecraftKey("empty");
    private TileEntityJigsaw.JointType joint = TileEntityJigsaw.JointType.ROLLABLE;
    private String finalState = "minecraft:air";

    public TileEntityJigsaw(BlockPosition pos, IBlockData state) {
        super(TileEntityTypes.JIGSAW, pos, state);
    }

    public MinecraftKey getName() {
        return this.name;
    }

    public MinecraftKey getTarget() {
        return this.target;
    }

    public MinecraftKey getPool() {
        return this.pool;
    }

    public String getFinalState() {
        return this.finalState;
    }

    public TileEntityJigsaw.JointType getJoint() {
        return this.joint;
    }

    public void setName(MinecraftKey value) {
        this.name = value;
    }

    public void setTarget(MinecraftKey target) {
        this.target = target;
    }

    public void setPool(MinecraftKey pool) {
        this.pool = pool;
    }

    public void setFinalState(String finalState) {
        this.finalState = finalState;
    }

    public void setJoint(TileEntityJigsaw.JointType joint) {
        this.joint = joint;
    }

    @Override
    public NBTTagCompound save(NBTTagCompound nbt) {
        super.save(nbt);
        nbt.setString("name", this.name.toString());
        nbt.setString("target", this.target.toString());
        nbt.setString("pool", this.pool.toString());
        nbt.setString("final_state", this.finalState);
        nbt.setString("joint", this.joint.getSerializedName());
        return nbt;
    }

    @Override
    public void load(NBTTagCompound nbt) {
        super.load(nbt);
        this.name = new MinecraftKey(nbt.getString("name"));
        this.target = new MinecraftKey(nbt.getString("target"));
        this.pool = new MinecraftKey(nbt.getString("pool"));
        this.finalState = nbt.getString("final_state");
        this.joint = TileEntityJigsaw.JointType.byName(nbt.getString("joint")).orElseGet(() -> {
            return BlockJigsaw.getFrontFacing(this.getBlock()).getAxis().isHorizontal() ? TileEntityJigsaw.JointType.ALIGNED : TileEntityJigsaw.JointType.ROLLABLE;
        });
    }

    @Nullable
    @Override
    public PacketPlayOutTileEntityData getUpdatePacket() {
        return new PacketPlayOutTileEntityData(this.worldPosition, 12, this.getUpdateTag());
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return this.save(new NBTTagCompound());
    }

    public void generate(WorldServer world, int maxDepth, boolean keepJigsaws) {
        ChunkGenerator chunkGenerator = world.getChunkSource().getChunkGenerator();
        DefinedStructureManager structureManager = world.getStructureManager();
        StructureManager structureFeatureManager = world.getStructureManager();
        Random random = world.getRandom();
        BlockPosition blockPos = this.getPosition();
        List<WorldGenFeaturePillagerOutpostPoolPiece> list = Lists.newArrayList();
        DefinedStructure structureTemplate = new DefinedStructure();
        structureTemplate.fillFromWorld(world, blockPos, new BaseBlockPosition(1, 1, 1), false, (Block)null);
        WorldGenFeatureDefinedStructurePoolStructure structurePoolElement = new WorldGenFeatureDefinedStructurePoolSingle(structureTemplate);
        WorldGenFeaturePillagerOutpostPoolPiece poolElementStructurePiece = new WorldGenFeaturePillagerOutpostPoolPiece(structureManager, structurePoolElement, blockPos, 1, EnumBlockRotation.NONE, new StructureBoundingBox(blockPos));
        WorldGenFeatureDefinedStructureJigsawPlacement.addPieces(world.registryAccess(), poolElementStructurePiece, maxDepth, WorldGenFeaturePillagerOutpostPoolPiece::new, chunkGenerator, structureManager, list, random, world);

        for(WorldGenFeaturePillagerOutpostPoolPiece poolElementStructurePiece2 : list) {
            poolElementStructurePiece2.place(world, structureFeatureManager, chunkGenerator, random, StructureBoundingBox.infinite(), blockPos, keepJigsaws);
        }

    }

    public static enum JointType implements INamable {
        ROLLABLE("rollable"),
        ALIGNED("aligned");

        private final String name;

        private JointType(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        public static Optional<TileEntityJigsaw.JointType> byName(String name) {
            return Arrays.stream(values()).filter((jointType) -> {
                return jointType.getSerializedName().equals(name);
            }).findFirst();
        }

        public IChatBaseComponent getTranslatedName() {
            return new ChatMessage("jigsaw_block.joint." + this.name);
        }
    }
}
