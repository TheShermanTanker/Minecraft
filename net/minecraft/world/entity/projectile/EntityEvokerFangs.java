package net.minecraft.world.entity.projectile;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.particles.Particles;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntity;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.World;

public class EntityEvokerFangs extends Entity {
    public static final int ATTACK_DURATION = 20;
    public static final int LIFE_OFFSET = 2;
    public static final int ATTACK_TRIGGER_TICKS = 14;
    private int warmupDelayTicks;
    private boolean sentSpikeEvent;
    private int lifeTicks = 22;
    private boolean clientSideAttackStarted;
    @Nullable
    private EntityLiving owner;
    @Nullable
    private UUID ownerUUID;

    public EntityEvokerFangs(EntityTypes<? extends EntityEvokerFangs> type, World world) {
        super(type, world);
    }

    public EntityEvokerFangs(World world, double x, double y, double z, float yaw, int warmup, EntityLiving owner) {
        this(EntityTypes.EVOKER_FANGS, world);
        this.warmupDelayTicks = warmup;
        this.setOwner(owner);
        this.setYRot(yaw * (180F / (float)Math.PI));
        this.setPosition(x, y, z);
    }

    @Override
    protected void initDatawatcher() {
    }

    public void setOwner(@Nullable EntityLiving owner) {
        this.owner = owner;
        this.ownerUUID = owner == null ? null : owner.getUniqueID();
    }

    @Nullable
    public EntityLiving getOwner() {
        if (this.owner == null && this.ownerUUID != null && this.level instanceof WorldServer) {
            Entity entity = ((WorldServer)this.level).getEntity(this.ownerUUID);
            if (entity instanceof EntityLiving) {
                this.owner = (EntityLiving)entity;
            }
        }

        return this.owner;
    }

    @Override
    protected void loadData(NBTTagCompound nbt) {
        this.warmupDelayTicks = nbt.getInt("Warmup");
        if (nbt.hasUUID("Owner")) {
            this.ownerUUID = nbt.getUUID("Owner");
        }

    }

    @Override
    protected void saveData(NBTTagCompound nbt) {
        nbt.setInt("Warmup", this.warmupDelayTicks);
        if (this.ownerUUID != null) {
            nbt.putUUID("Owner", this.ownerUUID);
        }

    }

    @Override
    public void tick() {
        super.tick();
        if (this.level.isClientSide) {
            if (this.clientSideAttackStarted) {
                --this.lifeTicks;
                if (this.lifeTicks == 14) {
                    for(int i = 0; i < 12; ++i) {
                        double d = this.locX() + (this.random.nextDouble() * 2.0D - 1.0D) * (double)this.getWidth() * 0.5D;
                        double e = this.locY() + 0.05D + this.random.nextDouble();
                        double f = this.locZ() + (this.random.nextDouble() * 2.0D - 1.0D) * (double)this.getWidth() * 0.5D;
                        double g = (this.random.nextDouble() * 2.0D - 1.0D) * 0.3D;
                        double h = 0.3D + this.random.nextDouble() * 0.3D;
                        double j = (this.random.nextDouble() * 2.0D - 1.0D) * 0.3D;
                        this.level.addParticle(Particles.CRIT, d, e + 1.0D, f, g, h, j);
                    }
                }
            }
        } else if (--this.warmupDelayTicks < 0) {
            if (this.warmupDelayTicks == -8) {
                for(EntityLiving livingEntity : this.level.getEntitiesOfClass(EntityLiving.class, this.getBoundingBox().grow(0.2D, 0.0D, 0.2D))) {
                    this.dealDamageTo(livingEntity);
                }
            }

            if (!this.sentSpikeEvent) {
                this.level.broadcastEntityEffect(this, (byte)4);
                this.sentSpikeEvent = true;
            }

            if (--this.lifeTicks < 0) {
                this.die();
            }
        }

    }

    private void dealDamageTo(EntityLiving target) {
        EntityLiving livingEntity = this.getOwner();
        if (target.isAlive() && !target.isInvulnerable() && target != livingEntity) {
            if (livingEntity == null) {
                target.damageEntity(DamageSource.MAGIC, 6.0F);
            } else {
                if (livingEntity.isAlliedTo(target)) {
                    return;
                }

                target.damageEntity(DamageSource.indirectMagic(this, livingEntity), 6.0F);
            }

        }
    }

    @Override
    public void handleEntityEvent(byte status) {
        super.handleEntityEvent(status);
        if (status == 4) {
            this.clientSideAttackStarted = true;
            if (!this.isSilent()) {
                this.level.playLocalSound(this.locX(), this.locY(), this.locZ(), SoundEffects.EVOKER_FANGS_ATTACK, this.getSoundCategory(), 1.0F, this.random.nextFloat() * 0.2F + 0.85F, false);
            }
        }

    }

    public float getAnimationProgress(float tickDelta) {
        if (!this.clientSideAttackStarted) {
            return 0.0F;
        } else {
            int i = this.lifeTicks - 2;
            return i <= 0 ? 1.0F : 1.0F - ((float)i - tickDelta) / 20.0F;
        }
    }

    @Override
    public Packet<?> getPacket() {
        return new PacketPlayOutSpawnEntity(this);
    }
}
