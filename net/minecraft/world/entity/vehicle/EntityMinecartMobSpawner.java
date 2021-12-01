package net.minecraft.world.entity.vehicle;

import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.MobSpawnerAbstract;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;

public class EntityMinecartMobSpawner extends EntityMinecartAbstract {
    private final MobSpawnerAbstract spawner = new MobSpawnerAbstract() {
        @Override
        public void broadcastEvent(World world, BlockPosition pos, int i) {
            world.broadcastEntityEffect(EntityMinecartMobSpawner.this, (byte)i);
        }
    };
    private final Runnable ticker;

    public EntityMinecartMobSpawner(EntityTypes<? extends EntityMinecartMobSpawner> type, World world) {
        super(type, world);
        this.ticker = this.createTicker(world);
    }

    public EntityMinecartMobSpawner(World world, double x, double y, double z) {
        super(EntityTypes.SPAWNER_MINECART, world, x, y, z);
        this.ticker = this.createTicker(world);
    }

    private Runnable createTicker(World world) {
        return world instanceof WorldServer ? () -> {
            this.spawner.serverTick((WorldServer)world, this.getChunkCoordinates());
        } : () -> {
            this.spawner.clientTick(world, this.getChunkCoordinates());
        };
    }

    @Override
    public EntityMinecartAbstract.EnumMinecartType getMinecartType() {
        return EntityMinecartAbstract.EnumMinecartType.SPAWNER;
    }

    @Override
    public IBlockData getDefaultDisplayBlockState() {
        return Blocks.SPAWNER.getBlockData();
    }

    @Override
    protected void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        this.spawner.load(this.level, this.getChunkCoordinates(), nbt);
    }

    @Override
    protected void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        this.spawner.save(nbt);
    }

    @Override
    public void handleEntityEvent(byte status) {
        this.spawner.onEventTriggered(this.level, status);
    }

    @Override
    public void tick() {
        super.tick();
        this.ticker.run();
    }

    public MobSpawnerAbstract getSpawner() {
        return this.spawner;
    }

    @Override
    public boolean onlyOpCanSetNbt() {
        return true;
    }
}
