package net.minecraft.network.protocol.game;

import net.minecraft.core.BaseBlockPosition;
import net.minecraft.core.BlockPosition;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.util.MathHelper;
import net.minecraft.world.level.block.EnumBlockMirror;
import net.minecraft.world.level.block.EnumBlockRotation;
import net.minecraft.world.level.block.entity.TileEntityStructure;
import net.minecraft.world.level.block.state.properties.BlockPropertyStructureMode;

public class PacketPlayInStruct implements Packet<PacketListenerPlayIn> {
    private static final int FLAG_IGNORE_ENTITIES = 1;
    private static final int FLAG_SHOW_AIR = 2;
    private static final int FLAG_SHOW_BOUNDING_BOX = 4;
    private final BlockPosition pos;
    private final TileEntityStructure.UpdateType updateType;
    private final BlockPropertyStructureMode mode;
    private final String name;
    private final BlockPosition offset;
    private final BaseBlockPosition size;
    private final EnumBlockMirror mirror;
    private final EnumBlockRotation rotation;
    private final String data;
    private final boolean ignoreEntities;
    private final boolean showAir;
    private final boolean showBoundingBox;
    private final float integrity;
    private final long seed;

    public PacketPlayInStruct(BlockPosition pos, TileEntityStructure.UpdateType action, BlockPropertyStructureMode mode, String structureName, BlockPosition offset, BaseBlockPosition size, EnumBlockMirror mirror, EnumBlockRotation rotation, String metadata, boolean ignoreEntities, boolean showAir, boolean showBoundingBox, float integrity, long seed) {
        this.pos = pos;
        this.updateType = action;
        this.mode = mode;
        this.name = structureName;
        this.offset = offset;
        this.size = size;
        this.mirror = mirror;
        this.rotation = rotation;
        this.data = metadata;
        this.ignoreEntities = ignoreEntities;
        this.showAir = showAir;
        this.showBoundingBox = showBoundingBox;
        this.integrity = integrity;
        this.seed = seed;
    }

    public PacketPlayInStruct(PacketDataSerializer buf) {
        this.pos = buf.readBlockPos();
        this.updateType = buf.readEnum(TileEntityStructure.UpdateType.class);
        this.mode = buf.readEnum(BlockPropertyStructureMode.class);
        this.name = buf.readUtf();
        int i = 48;
        this.offset = new BlockPosition(MathHelper.clamp((int)buf.readByte(), (int)-48, (int)48), MathHelper.clamp((int)buf.readByte(), (int)-48, (int)48), MathHelper.clamp((int)buf.readByte(), (int)-48, (int)48));
        int j = 48;
        this.size = new BaseBlockPosition(MathHelper.clamp((int)buf.readByte(), (int)0, (int)48), MathHelper.clamp((int)buf.readByte(), (int)0, (int)48), MathHelper.clamp((int)buf.readByte(), (int)0, (int)48));
        this.mirror = buf.readEnum(EnumBlockMirror.class);
        this.rotation = buf.readEnum(EnumBlockRotation.class);
        this.data = buf.readUtf(128);
        this.integrity = MathHelper.clamp(buf.readFloat(), 0.0F, 1.0F);
        this.seed = buf.readVarLong();
        int k = buf.readByte();
        this.ignoreEntities = (k & 1) != 0;
        this.showAir = (k & 2) != 0;
        this.showBoundingBox = (k & 4) != 0;
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeBlockPos(this.pos);
        buf.writeEnum(this.updateType);
        buf.writeEnum(this.mode);
        buf.writeUtf(this.name);
        buf.writeByte(this.offset.getX());
        buf.writeByte(this.offset.getY());
        buf.writeByte(this.offset.getZ());
        buf.writeByte(this.size.getX());
        buf.writeByte(this.size.getY());
        buf.writeByte(this.size.getZ());
        buf.writeEnum(this.mirror);
        buf.writeEnum(this.rotation);
        buf.writeUtf(this.data);
        buf.writeFloat(this.integrity);
        buf.writeVarLong(this.seed);
        int i = 0;
        if (this.ignoreEntities) {
            i |= 1;
        }

        if (this.showAir) {
            i |= 2;
        }

        if (this.showBoundingBox) {
            i |= 4;
        }

        buf.writeByte(i);
    }

    @Override
    public void handle(PacketListenerPlayIn listener) {
        listener.handleSetStructureBlock(this);
    }

    public BlockPosition getPos() {
        return this.pos;
    }

    public TileEntityStructure.UpdateType getUpdateType() {
        return this.updateType;
    }

    public BlockPropertyStructureMode getMode() {
        return this.mode;
    }

    public String getName() {
        return this.name;
    }

    public BlockPosition getOffset() {
        return this.offset;
    }

    public BaseBlockPosition getSize() {
        return this.size;
    }

    public EnumBlockMirror getMirror() {
        return this.mirror;
    }

    public EnumBlockRotation getRotation() {
        return this.rotation;
    }

    public String getData() {
        return this.data;
    }

    public boolean isIgnoreEntities() {
        return this.ignoreEntities;
    }

    public boolean isShowAir() {
        return this.showAir;
    }

    public boolean isShowBoundingBox() {
        return this.showBoundingBox;
    }

    public float getIntegrity() {
        return this.integrity;
    }

    public long getSeed() {
        return this.seed;
    }
}
