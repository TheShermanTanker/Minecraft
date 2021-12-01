package net.minecraft.world.level.block.entity;

import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.ResourceKeyInvalidException;
import net.minecraft.SystemUtils;
import net.minecraft.core.BaseBlockPosition;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.protocol.game.PacketPlayOutTileEntityData;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.UtilColor;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.block.BlockStructure;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnumBlockMirror;
import net.minecraft.world.level.block.EnumBlockRotation;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockPropertyStructureMode;
import net.minecraft.world.level.levelgen.structure.StructureBoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureInfo;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.DefinedStructureProcessorRotation;

public class TileEntityStructure extends TileEntity {
    private static final int SCAN_CORNER_BLOCKS_RANGE = 5;
    public static final int MAX_OFFSET_PER_AXIS = 48;
    public static final int MAX_SIZE_PER_AXIS = 48;
    public static final String AUTHOR_TAG = "author";
    private MinecraftKey structureName;
    public String author = "";
    public String metaData = "";
    public BlockPosition structurePos = new BlockPosition(0, 1, 0);
    public BaseBlockPosition structureSize = BaseBlockPosition.ZERO;
    public EnumBlockMirror mirror = EnumBlockMirror.NONE;
    public EnumBlockRotation rotation = EnumBlockRotation.NONE;
    public BlockPropertyStructureMode mode;
    public boolean ignoreEntities = true;
    private boolean powered;
    public boolean showAir;
    public boolean showBoundingBox = true;
    public float integrity = 1.0F;
    public long seed;

    public TileEntityStructure(BlockPosition pos, IBlockData state) {
        super(TileEntityTypes.STRUCTURE_BLOCK, pos, state);
        this.mode = state.get(BlockStructure.MODE);
    }

    @Override
    protected void saveAdditional(NBTTagCompound nbt) {
        super.saveAdditional(nbt);
        nbt.setString("name", this.getStructureName());
        nbt.setString("author", this.author);
        nbt.setString("metadata", this.metaData);
        nbt.setInt("posX", this.structurePos.getX());
        nbt.setInt("posY", this.structurePos.getY());
        nbt.setInt("posZ", this.structurePos.getZ());
        nbt.setInt("sizeX", this.structureSize.getX());
        nbt.setInt("sizeY", this.structureSize.getY());
        nbt.setInt("sizeZ", this.structureSize.getZ());
        nbt.setString("rotation", this.rotation.toString());
        nbt.setString("mirror", this.mirror.toString());
        nbt.setString("mode", this.mode.toString());
        nbt.setBoolean("ignoreEntities", this.ignoreEntities);
        nbt.setBoolean("powered", this.powered);
        nbt.setBoolean("showair", this.showAir);
        nbt.setBoolean("showboundingbox", this.showBoundingBox);
        nbt.setFloat("integrity", this.integrity);
        nbt.setLong("seed", this.seed);
    }

    @Override
    public void load(NBTTagCompound nbt) {
        super.load(nbt);
        this.setStructureName(nbt.getString("name"));
        this.author = nbt.getString("author");
        this.metaData = nbt.getString("metadata");
        int i = MathHelper.clamp(nbt.getInt("posX"), -48, 48);
        int j = MathHelper.clamp(nbt.getInt("posY"), -48, 48);
        int k = MathHelper.clamp(nbt.getInt("posZ"), -48, 48);
        this.structurePos = new BlockPosition(i, j, k);
        int l = MathHelper.clamp(nbt.getInt("sizeX"), 0, 48);
        int m = MathHelper.clamp(nbt.getInt("sizeY"), 0, 48);
        int n = MathHelper.clamp(nbt.getInt("sizeZ"), 0, 48);
        this.structureSize = new BaseBlockPosition(l, m, n);

        try {
            this.rotation = EnumBlockRotation.valueOf(nbt.getString("rotation"));
        } catch (IllegalArgumentException var11) {
            this.rotation = EnumBlockRotation.NONE;
        }

        try {
            this.mirror = EnumBlockMirror.valueOf(nbt.getString("mirror"));
        } catch (IllegalArgumentException var10) {
            this.mirror = EnumBlockMirror.NONE;
        }

        try {
            this.mode = BlockPropertyStructureMode.valueOf(nbt.getString("mode"));
        } catch (IllegalArgumentException var9) {
            this.mode = BlockPropertyStructureMode.DATA;
        }

        this.ignoreEntities = nbt.getBoolean("ignoreEntities");
        this.powered = nbt.getBoolean("powered");
        this.showAir = nbt.getBoolean("showair");
        this.showBoundingBox = nbt.getBoolean("showboundingbox");
        if (nbt.hasKey("integrity")) {
            this.integrity = nbt.getFloat("integrity");
        } else {
            this.integrity = 1.0F;
        }

        this.seed = nbt.getLong("seed");
        this.updateBlockState();
    }

    private void updateBlockState() {
        if (this.level != null) {
            BlockPosition blockPos = this.getPosition();
            IBlockData blockState = this.level.getType(blockPos);
            if (blockState.is(Blocks.STRUCTURE_BLOCK)) {
                this.level.setTypeAndData(blockPos, blockState.set(BlockStructure.MODE, this.mode), 2);
            }

        }
    }

    @Override
    public PacketPlayOutTileEntityData getUpdatePacket() {
        return PacketPlayOutTileEntityData.create(this);
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    public boolean usedBy(EntityHuman player) {
        if (!player.isCreativeAndOp()) {
            return false;
        } else {
            if (player.getCommandSenderWorld().isClientSide) {
                player.openStructureBlock(this);
            }

            return true;
        }
    }

    public String getStructureName() {
        return this.structureName == null ? "" : this.structureName.toString();
    }

    public String getStructurePath() {
        return this.structureName == null ? "" : this.structureName.getKey();
    }

    public boolean hasStructureName() {
        return this.structureName != null;
    }

    public void setStructureName(@Nullable String name) {
        this.setStructureName(UtilColor.isNullOrEmpty(name) ? null : MinecraftKey.tryParse(name));
    }

    public void setStructureName(@Nullable MinecraftKey structureName) {
        this.structureName = structureName;
    }

    public void setAuthor(EntityLiving entity) {
        this.author = entity.getDisplayName().getString();
    }

    public BlockPosition getStructurePos() {
        return this.structurePos;
    }

    public void setStructurePos(BlockPosition pos) {
        this.structurePos = pos;
    }

    public BaseBlockPosition getStructureSize() {
        return this.structureSize;
    }

    public void setStructureSize(BaseBlockPosition size) {
        this.structureSize = size;
    }

    public EnumBlockMirror getMirror() {
        return this.mirror;
    }

    public void setMirror(EnumBlockMirror mirror) {
        this.mirror = mirror;
    }

    public EnumBlockRotation getRotation() {
        return this.rotation;
    }

    public void setRotation(EnumBlockRotation rotation) {
        this.rotation = rotation;
    }

    public String getMetaData() {
        return this.metaData;
    }

    public void setMetaData(String metadata) {
        this.metaData = metadata;
    }

    public BlockPropertyStructureMode getUsageMode() {
        return this.mode;
    }

    public void setUsageMode(BlockPropertyStructureMode mode) {
        this.mode = mode;
        IBlockData blockState = this.level.getType(this.getPosition());
        if (blockState.is(Blocks.STRUCTURE_BLOCK)) {
            this.level.setTypeAndData(this.getPosition(), blockState.set(BlockStructure.MODE, mode), 2);
        }

    }

    public boolean isIgnoreEntities() {
        return this.ignoreEntities;
    }

    public void setIgnoreEntities(boolean ignoreEntities) {
        this.ignoreEntities = ignoreEntities;
    }

    public float getIntegrity() {
        return this.integrity;
    }

    public void setIntegrity(float integrity) {
        this.integrity = integrity;
    }

    public long getSeed() {
        return this.seed;
    }

    public void setSeed(long seed) {
        this.seed = seed;
    }

    public boolean detectSize() {
        if (this.mode != BlockPropertyStructureMode.SAVE) {
            return false;
        } else {
            BlockPosition blockPos = this.getPosition();
            int i = 80;
            BlockPosition blockPos2 = new BlockPosition(blockPos.getX() - 80, this.level.getMinBuildHeight(), blockPos.getZ() - 80);
            BlockPosition blockPos3 = new BlockPosition(blockPos.getX() + 80, this.level.getMaxBuildHeight() - 1, blockPos.getZ() + 80);
            Stream<BlockPosition> stream = this.getRelatedCorners(blockPos2, blockPos3);
            return calculateEnclosingBoundingBox(blockPos, stream).filter((box) -> {
                int i = box.maxX() - box.minX();
                int j = box.maxY() - box.minY();
                int k = box.maxZ() - box.minZ();
                if (i > 1 && j > 1 && k > 1) {
                    this.structurePos = new BlockPosition(box.minX() - blockPos.getX() + 1, box.minY() - blockPos.getY() + 1, box.minZ() - blockPos.getZ() + 1);
                    this.structureSize = new BaseBlockPosition(i - 1, j - 1, k - 1);
                    this.update();
                    IBlockData blockState = this.level.getType(blockPos);
                    this.level.notify(blockPos, blockState, blockState, 3);
                    return true;
                } else {
                    return false;
                }
            }).isPresent();
        }
    }

    private Stream<BlockPosition> getRelatedCorners(BlockPosition start, BlockPosition end) {
        return BlockPosition.betweenClosedStream(start, end).filter((pos) -> {
            return this.level.getType(pos).is(Blocks.STRUCTURE_BLOCK);
        }).map(this.level::getTileEntity).filter((blockEntity) -> {
            return blockEntity instanceof TileEntityStructure;
        }).map((blockEntity) -> {
            return (TileEntityStructure)blockEntity;
        }).filter((blockEntity) -> {
            return blockEntity.mode == BlockPropertyStructureMode.CORNER && Objects.equals(this.structureName, blockEntity.structureName);
        }).map(TileEntity::getPosition);
    }

    private static Optional<StructureBoundingBox> calculateEnclosingBoundingBox(BlockPosition pos, Stream<BlockPosition> corners) {
        Iterator<BlockPosition> iterator = corners.iterator();
        if (!iterator.hasNext()) {
            return Optional.empty();
        } else {
            BlockPosition blockPos = iterator.next();
            StructureBoundingBox boundingBox = new StructureBoundingBox(blockPos);
            if (iterator.hasNext()) {
                iterator.forEachRemaining(boundingBox::encapsulate);
            } else {
                boundingBox.encapsulate(pos);
            }

            return Optional.of(boundingBox);
        }
    }

    public boolean saveStructure() {
        return this.saveStructure(true);
    }

    public boolean saveStructure(boolean bl) {
        if (this.mode == BlockPropertyStructureMode.SAVE && !this.level.isClientSide && this.structureName != null) {
            BlockPosition blockPos = this.getPosition().offset(this.structurePos);
            WorldServer serverLevel = (WorldServer)this.level;
            DefinedStructureManager structureManager = serverLevel.getStructureManager();

            DefinedStructure structureTemplate;
            try {
                structureTemplate = structureManager.getOrCreate(this.structureName);
            } catch (ResourceKeyInvalidException var8) {
                return false;
            }

            structureTemplate.fillFromWorld(this.level, blockPos, this.structureSize, !this.ignoreEntities, Blocks.STRUCTURE_VOID);
            structureTemplate.setAuthor(this.author);
            if (bl) {
                try {
                    return structureManager.save(this.structureName);
                } catch (ResourceKeyInvalidException var7) {
                    return false;
                }
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    public boolean loadStructure(WorldServer world) {
        return this.loadStructure(world, true);
    }

    private static Random createRandom(long seed) {
        return seed == 0L ? new Random(SystemUtils.getMonotonicMillis()) : new Random(seed);
    }

    public boolean loadStructure(WorldServer world, boolean bl) {
        if (this.mode == BlockPropertyStructureMode.LOAD && this.structureName != null) {
            DefinedStructureManager structureManager = world.getStructureManager();

            Optional<DefinedStructure> optional;
            try {
                optional = structureManager.get(this.structureName);
            } catch (ResourceKeyInvalidException var6) {
                return false;
            }

            return !optional.isPresent() ? false : this.loadStructure(world, bl, optional.get());
        } else {
            return false;
        }
    }

    public boolean loadStructure(WorldServer world, boolean bl, DefinedStructure structure) {
        BlockPosition blockPos = this.getPosition();
        if (!UtilColor.isNullOrEmpty(structure.getAuthor())) {
            this.author = structure.getAuthor();
        }

        BaseBlockPosition vec3i = structure.getSize();
        boolean bl2 = this.structureSize.equals(vec3i);
        if (!bl2) {
            this.structureSize = vec3i;
            this.update();
            IBlockData blockState = world.getType(blockPos);
            world.notify(blockPos, blockState, blockState, 3);
        }

        if (bl && !bl2) {
            return false;
        } else {
            DefinedStructureInfo structurePlaceSettings = (new DefinedStructureInfo()).setMirror(this.mirror).setRotation(this.rotation).setIgnoreEntities(this.ignoreEntities);
            if (this.integrity < 1.0F) {
                structurePlaceSettings.clearProcessors().addProcessor(new DefinedStructureProcessorRotation(MathHelper.clamp(this.integrity, 0.0F, 1.0F))).setRandom(createRandom(this.seed));
            }

            BlockPosition blockPos2 = blockPos.offset(this.structurePos);
            structure.placeInWorld(world, blockPos2, blockPos2, structurePlaceSettings, createRandom(this.seed), 2);
            return true;
        }
    }

    public void unloadStructure() {
        if (this.structureName != null) {
            WorldServer serverLevel = (WorldServer)this.level;
            DefinedStructureManager structureManager = serverLevel.getStructureManager();
            structureManager.remove(this.structureName);
        }
    }

    public boolean isStructureLoadable() {
        if (this.mode == BlockPropertyStructureMode.LOAD && !this.level.isClientSide && this.structureName != null) {
            WorldServer serverLevel = (WorldServer)this.level;
            DefinedStructureManager structureManager = serverLevel.getStructureManager();

            try {
                return structureManager.get(this.structureName).isPresent();
            } catch (ResourceKeyInvalidException var4) {
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean isPowered() {
        return this.powered;
    }

    public void setPowered(boolean powered) {
        this.powered = powered;
    }

    public boolean getShowAir() {
        return this.showAir;
    }

    public void setShowAir(boolean showAir) {
        this.showAir = showAir;
    }

    public boolean getShowBoundingBox() {
        return this.showBoundingBox;
    }

    public void setShowBoundingBox(boolean showBoundingBox) {
        this.showBoundingBox = showBoundingBox;
    }

    public static enum UpdateType {
        UPDATE_DATA,
        SAVE_AREA,
        LOAD_AREA,
        SCAN_AREA;
    }
}
