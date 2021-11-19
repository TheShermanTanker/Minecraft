package net.minecraft.world.level.block.entity;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.protocol.game.PacketPlayOutTileEntityData;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.level.MobSpawnerAbstract;
import net.minecraft.world.level.MobSpawnerData;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;

public class TileEntityMobSpawner extends TileEntity {
    private final MobSpawnerAbstract spawner = new MobSpawnerAbstract() {
        @Override
        public void broadcastEvent(World world, BlockPosition pos, int i) {
            world.playBlockAction(pos, Blocks.SPAWNER, i, 0);
        }

        @Override
        public void setSpawnData(@Nullable World world, BlockPosition pos, MobSpawnerData spawnEntry) {
            super.setSpawnData(world, pos, spawnEntry);
            if (world != null) {
                IBlockData blockState = world.getType(pos);
                world.notify(pos, blockState, blockState, 4);
            }

        }
    };

    public TileEntityMobSpawner(BlockPosition pos, IBlockData state) {
        super(TileEntityTypes.MOB_SPAWNER, pos, state);
    }

    @Override
    public void load(NBTTagCompound nbt) {
        super.load(nbt);
        this.spawner.load(this.level, this.worldPosition, nbt);
    }

    @Override
    public NBTTagCompound save(NBTTagCompound nbt) {
        super.save(nbt);
        this.spawner.save(this.level, this.worldPosition, nbt);
        return nbt;
    }

    public static void clientTick(World world, BlockPosition pos, IBlockData state, TileEntityMobSpawner blockEntity) {
        blockEntity.spawner.clientTick(world, pos);
    }

    public static void serverTick(World world, BlockPosition pos, IBlockData state, TileEntityMobSpawner blockEntity) {
        blockEntity.spawner.serverTick((WorldServer)world, pos);
    }

    @Nullable
    @Override
    public PacketPlayOutTileEntityData getUpdatePacket() {
        return new PacketPlayOutTileEntityData(this.worldPosition, 1, this.getUpdateTag());
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        NBTTagCompound compoundTag = this.save(new NBTTagCompound());
        compoundTag.remove("SpawnPotentials");
        return compoundTag;
    }

    @Override
    public boolean setProperty(int type, int data) {
        return this.spawner.onEventTriggered(this.level, type) ? true : super.setProperty(type, data);
    }

    @Override
    public boolean isFilteredNBT() {
        return true;
    }

    public MobSpawnerAbstract getSpawner() {
        return this.spawner;
    }
}
