package net.minecraft.world.entity.item;

import javax.annotation.Nullable;
import net.minecraft.core.particles.Particles;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntity;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.EntitySize;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMoveType;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.World;

public class EntityTNTPrimed extends Entity {
    private static final DataWatcherObject<Integer> DATA_FUSE_ID = DataWatcher.defineId(EntityTNTPrimed.class, DataWatcherRegistry.INT);
    private static final int DEFAULT_FUSE_TIME = 80;
    @Nullable
    public EntityLiving owner;

    public EntityTNTPrimed(EntityTypes<? extends EntityTNTPrimed> type, World world) {
        super(type, world);
        this.blocksBuilding = true;
    }

    public EntityTNTPrimed(World world, double x, double y, double z, @Nullable EntityLiving igniter) {
        this(EntityTypes.TNT, world);
        this.setPosition(x, y, z);
        double d = world.random.nextDouble() * (double)((float)Math.PI * 2F);
        this.setMot(-Math.sin(d) * 0.02D, (double)0.2F, -Math.cos(d) * 0.02D);
        this.setFuseTicks(80);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.owner = igniter;
    }

    @Override
    protected void initDatawatcher() {
        this.entityData.register(DATA_FUSE_ID, 80);
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
    }

    @Override
    public boolean isInteractable() {
        return !this.isRemoved();
    }

    @Override
    public void tick() {
        if (!this.isNoGravity()) {
            this.setMot(this.getMot().add(0.0D, -0.04D, 0.0D));
        }

        this.move(EnumMoveType.SELF, this.getMot());
        this.setMot(this.getMot().scale(0.98D));
        if (this.onGround) {
            this.setMot(this.getMot().multiply(0.7D, -0.5D, 0.7D));
        }

        int i = this.getFuseTicks() - 1;
        this.setFuseTicks(i);
        if (i <= 0) {
            this.die();
            if (!this.level.isClientSide) {
                this.explode();
            }
        } else {
            this.updateInWaterStateAndDoFluidPushing();
            if (this.level.isClientSide) {
                this.level.addParticle(Particles.SMOKE, this.locX(), this.locY() + 0.5D, this.locZ(), 0.0D, 0.0D, 0.0D);
            }
        }

    }

    private void explode() {
        float f = 4.0F;
        this.level.explode(this, this.locX(), this.getY(0.0625D), this.locZ(), 4.0F, Explosion.Effect.BREAK);
    }

    @Override
    protected void saveData(NBTTagCompound nbt) {
        nbt.setShort("Fuse", (short)this.getFuseTicks());
    }

    @Override
    protected void loadData(NBTTagCompound nbt) {
        this.setFuseTicks(nbt.getShort("Fuse"));
    }

    @Nullable
    public EntityLiving getSource() {
        return this.owner;
    }

    @Override
    protected float getHeadHeight(EntityPose pose, EntitySize dimensions) {
        return 0.15F;
    }

    public void setFuseTicks(int fuse) {
        this.entityData.set(DATA_FUSE_ID, fuse);
    }

    public int getFuseTicks() {
        return this.entityData.get(DATA_FUSE_ID);
    }

    @Override
    public Packet<?> getPacket() {
        return new PacketPlayOutSpawnEntity(this);
    }
}
