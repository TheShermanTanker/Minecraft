package net.minecraft.world.entity;

import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.World;

public interface IEntityAngerable {
    String TAG_ANGER_TIME = "AngerTime";
    String TAG_ANGRY_AT = "AngryAt";

    int getAnger();

    void setAnger(int ticks);

    @Nullable
    UUID getAngerTarget();

    void setAngerTarget(@Nullable UUID uuid);

    void anger();

    default void addPersistentAngerSaveData(NBTTagCompound nbt) {
        nbt.setInt("AngerTime", this.getAnger());
        if (this.getAngerTarget() != null) {
            nbt.putUUID("AngryAt", this.getAngerTarget());
        }

    }

    default void readPersistentAngerSaveData(World world, NBTTagCompound nbt) {
        this.setAnger(nbt.getInt("AngerTime"));
        if (world instanceof WorldServer) {
            if (!nbt.hasUUID("AngryAt")) {
                this.setAngerTarget((UUID)null);
            } else {
                UUID uUID = nbt.getUUID("AngryAt");
                this.setAngerTarget(uUID);
                Entity entity = ((WorldServer)world).getEntity(uUID);
                if (entity != null) {
                    if (entity instanceof EntityInsentient) {
                        this.setLastDamager((EntityInsentient)entity);
                    }

                    if (entity.getEntityType() == EntityTypes.PLAYER) {
                        this.setLastHurtByPlayer((EntityHuman)entity);
                    }

                }
            }
        }
    }

    default void updatePersistentAnger(WorldServer world, boolean bl) {
        EntityLiving livingEntity = this.getGoalTarget();
        UUID uUID = this.getAngerTarget();
        if ((livingEntity == null || livingEntity.isDeadOrDying()) && uUID != null && world.getEntity(uUID) instanceof EntityInsentient) {
            this.pacify();
        } else {
            if (livingEntity != null && !Objects.equals(uUID, livingEntity.getUniqueID())) {
                this.setAngerTarget(livingEntity.getUniqueID());
                this.anger();
            }

            if (this.getAnger() > 0 && (livingEntity == null || livingEntity.getEntityType() != EntityTypes.PLAYER || !bl)) {
                this.setAnger(this.getAnger() - 1);
                if (this.getAnger() == 0) {
                    this.pacify();
                }
            }

        }
    }

    default boolean isAngryAt(EntityLiving livingEntity) {
        if (!this.canAttack(livingEntity)) {
            return false;
        } else {
            return livingEntity.getEntityType() == EntityTypes.PLAYER && this.isAngryAtAllPlayers(livingEntity.level) ? true : livingEntity.getUniqueID().equals(this.getAngerTarget());
        }
    }

    default boolean isAngryAtAllPlayers(World world) {
        return world.getGameRules().getBoolean(GameRules.RULE_UNIVERSAL_ANGER) && this.isAngry() && this.getAngerTarget() == null;
    }

    default boolean isAngry() {
        return this.getAnger() > 0;
    }

    default void playerDied(EntityHuman player) {
        if (player.level.getGameRules().getBoolean(GameRules.RULE_FORGIVE_DEAD_PLAYERS)) {
            if (player.getUniqueID().equals(this.getAngerTarget())) {
                this.pacify();
            }
        }
    }

    default void forgetCurrentTargetAndRefreshUniversalAnger() {
        this.pacify();
        this.anger();
    }

    default void pacify() {
        this.setLastDamager((EntityLiving)null);
        this.setAngerTarget((UUID)null);
        this.setGoalTarget((EntityLiving)null);
        this.setAnger(0);
    }

    @Nullable
    EntityLiving getLastDamager();

    void setLastDamager(@Nullable EntityLiving attacker);

    void setLastHurtByPlayer(@Nullable EntityHuman attacking);

    void setGoalTarget(@Nullable EntityLiving target);

    boolean canAttack(EntityLiving livingEntity);

    @Nullable
    EntityLiving getGoalTarget();
}
