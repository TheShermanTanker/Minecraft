package net.minecraft.world.level.saveddata.maps;

import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.NBTTagCompound;

public class WorldMapFrame {
    private final BlockPosition pos;
    private final int rotation;
    private final int entityId;

    public WorldMapFrame(BlockPosition pos, int rotation, int entityId) {
        this.pos = pos;
        this.rotation = rotation;
        this.entityId = entityId;
    }

    public static WorldMapFrame load(NBTTagCompound nbt) {
        BlockPosition blockPos = GameProfileSerializer.readBlockPos(nbt.getCompound("Pos"));
        int i = nbt.getInt("Rotation");
        int j = nbt.getInt("EntityId");
        return new WorldMapFrame(blockPos, i, j);
    }

    public NBTTagCompound save() {
        NBTTagCompound compoundTag = new NBTTagCompound();
        compoundTag.set("Pos", GameProfileSerializer.writeBlockPos(this.pos));
        compoundTag.setInt("Rotation", this.rotation);
        compoundTag.setInt("EntityId", this.entityId);
        return compoundTag;
    }

    public BlockPosition getPos() {
        return this.pos;
    }

    public int getRotation() {
        return this.rotation;
    }

    public int getEntityId() {
        return this.entityId;
    }

    public String getId() {
        return frameId(this.pos);
    }

    public static String frameId(BlockPosition pos) {
        return "frame-" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }
}
