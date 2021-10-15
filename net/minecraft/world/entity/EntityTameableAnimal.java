package net.minecraft.world.entity;

import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.core.particles.ParticleParam;
import net.minecraft.core.particles.Particles;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.players.NameReferencingFileConverter;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.animal.EntityAnimal;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.World;
import net.minecraft.world.scores.ScoreboardTeamBase;

public abstract class EntityTameableAnimal extends EntityAnimal implements ITameable {
    protected static final DataWatcherObject<Byte> DATA_FLAGS_ID = DataWatcher.defineId(EntityTameableAnimal.class, DataWatcherRegistry.BYTE);
    protected static final DataWatcherObject<Optional<UUID>> DATA_OWNERUUID_ID = DataWatcher.defineId(EntityTameableAnimal.class, DataWatcherRegistry.OPTIONAL_UUID);
    private boolean orderedToSit;

    protected EntityTameableAnimal(EntityTypes<? extends EntityTameableAnimal> type, World world) {
        super(type, world);
        this.reassessTameGoals();
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.entityData.register(DATA_FLAGS_ID, (byte)0);
        this.entityData.register(DATA_OWNERUUID_ID, Optional.empty());
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        if (this.getOwnerUUID() != null) {
            nbt.putUUID("Owner", this.getOwnerUUID());
        }

        nbt.setBoolean("Sitting", this.orderedToSit);
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        UUID uUID;
        if (nbt.hasUUID("Owner")) {
            uUID = nbt.getUUID("Owner");
        } else {
            String string = nbt.getString("Owner");
            uUID = NameReferencingFileConverter.convertMobOwnerIfNecessary(this.getMinecraftServer(), string);
        }

        if (uUID != null) {
            try {
                this.setOwnerUUID(uUID);
                this.setTamed(true);
            } catch (Throwable var4) {
                this.setTamed(false);
            }
        }

        this.orderedToSit = nbt.getBoolean("Sitting");
        this.setSitting(this.orderedToSit);
    }

    @Override
    public boolean canBeLeashed(EntityHuman player) {
        return !this.isLeashed();
    }

    protected void spawnTamingParticles(boolean positive) {
        ParticleParam particleOptions = Particles.HEART;
        if (!positive) {
            particleOptions = Particles.SMOKE;
        }

        for(int i = 0; i < 7; ++i) {
            double d = this.random.nextGaussian() * 0.02D;
            double e = this.random.nextGaussian() * 0.02D;
            double f = this.random.nextGaussian() * 0.02D;
            this.level.addParticle(particleOptions, this.getRandomX(1.0D), this.getRandomY() + 0.5D, this.getRandomZ(1.0D), d, e, f);
        }

    }

    @Override
    public void handleEntityEvent(byte status) {
        if (status == 7) {
            this.spawnTamingParticles(true);
        } else if (status == 6) {
            this.spawnTamingParticles(false);
        } else {
            super.handleEntityEvent(status);
        }

    }

    public boolean isTamed() {
        return (this.entityData.get(DATA_FLAGS_ID) & 4) != 0;
    }

    public void setTamed(boolean tamed) {
        byte b = this.entityData.get(DATA_FLAGS_ID);
        if (tamed) {
            this.entityData.set(DATA_FLAGS_ID, (byte)(b | 4));
        } else {
            this.entityData.set(DATA_FLAGS_ID, (byte)(b & -5));
        }

        this.reassessTameGoals();
    }

    protected void reassessTameGoals() {
    }

    public boolean isSitting() {
        return (this.entityData.get(DATA_FLAGS_ID) & 1) != 0;
    }

    public void setSitting(boolean inSittingPose) {
        byte b = this.entityData.get(DATA_FLAGS_ID);
        if (inSittingPose) {
            this.entityData.set(DATA_FLAGS_ID, (byte)(b | 1));
        } else {
            this.entityData.set(DATA_FLAGS_ID, (byte)(b & -2));
        }

    }

    @Nullable
    @Override
    public UUID getOwnerUUID() {
        return this.entityData.get(DATA_OWNERUUID_ID).orElse((UUID)null);
    }

    public void setOwnerUUID(@Nullable UUID uuid) {
        this.entityData.set(DATA_OWNERUUID_ID, Optional.ofNullable(uuid));
    }

    public void tame(EntityHuman player) {
        this.setTamed(true);
        this.setOwnerUUID(player.getUniqueID());
        if (player instanceof EntityPlayer) {
            CriterionTriggers.TAME_ANIMAL.trigger((EntityPlayer)player, this);
        }

    }

    @Nullable
    @Override
    public EntityLiving getOwner() {
        try {
            UUID uUID = this.getOwnerUUID();
            return uUID == null ? null : this.level.getPlayerByUUID(uUID);
        } catch (IllegalArgumentException var2) {
            return null;
        }
    }

    @Override
    public boolean canAttack(EntityLiving target) {
        return this.isOwnedBy(target) ? false : super.canAttack(target);
    }

    public boolean isOwnedBy(EntityLiving entity) {
        return entity == this.getOwner();
    }

    public boolean wantsToAttack(EntityLiving target, EntityLiving owner) {
        return true;
    }

    @Override
    public ScoreboardTeamBase getScoreboardTeam() {
        if (this.isTamed()) {
            EntityLiving livingEntity = this.getOwner();
            if (livingEntity != null) {
                return livingEntity.getScoreboardTeam();
            }
        }

        return super.getScoreboardTeam();
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        if (this.isTamed()) {
            EntityLiving livingEntity = this.getOwner();
            if (other == livingEntity) {
                return true;
            }

            if (livingEntity != null) {
                return livingEntity.isAlliedTo(other);
            }
        }

        return super.isAlliedTo(other);
    }

    @Override
    public void die(DamageSource source) {
        if (!this.level.isClientSide && this.level.getGameRules().getBoolean(GameRules.RULE_SHOWDEATHMESSAGES) && this.getOwner() instanceof EntityPlayer) {
            this.getOwner().sendMessage(this.getCombatTracker().getDeathMessage(), SystemUtils.NIL_UUID);
        }

        super.die(source);
    }

    public boolean isWillSit() {
        return this.orderedToSit;
    }

    public void setWillSit(boolean sitting) {
        this.orderedToSit = sitting;
    }
}
