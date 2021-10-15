package net.minecraft.world.entity.animal.horse;

import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.DifficultyDamageScaler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAgeable;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.entity.ai.goal.PathfinderGoal;
import net.minecraft.world.entity.ai.goal.PathfinderGoalPanic;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalTarget;
import net.minecraft.world.entity.ai.targeting.PathfinderTargetCondition;
import net.minecraft.world.entity.npc.EntityVillagerTrader;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldAccess;

public class EntityLlamaTrader extends EntityLlama {
    private int despawnDelay = 47999;

    public EntityLlamaTrader(EntityTypes<? extends EntityLlamaTrader> type, World world) {
        super(type, world);
    }

    @Override
    public boolean isTraderLlama() {
        return true;
    }

    @Override
    protected EntityLlama makeBabyLlama() {
        return EntityTypes.TRADER_LLAMA.create(this.level);
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        nbt.setInt("DespawnDelay", this.despawnDelay);
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        if (nbt.hasKeyOfType("DespawnDelay", 99)) {
            this.despawnDelay = nbt.getInt("DespawnDelay");
        }

    }

    @Override
    protected void initPathfinder() {
        super.initPathfinder();
        this.goalSelector.addGoal(1, new PathfinderGoalPanic(this, 2.0D));
        this.targetSelector.addGoal(1, new EntityLlamaTrader.TraderLlamaDefendWanderingTraderGoal(this));
    }

    public void setDespawnDelay(int despawnDelay) {
        this.despawnDelay = despawnDelay;
    }

    @Override
    protected void doPlayerRide(EntityHuman player) {
        Entity entity = this.getLeashHolder();
        if (!(entity instanceof EntityVillagerTrader)) {
            super.doPlayerRide(player);
        }
    }

    @Override
    public void movementTick() {
        super.movementTick();
        if (!this.level.isClientSide) {
            this.maybeDespawn();
        }

    }

    private void maybeDespawn() {
        if (this.canDespawn()) {
            this.despawnDelay = this.isLeashedToWanderingTrader() ? ((EntityVillagerTrader)this.getLeashHolder()).getDespawnDelay() - 1 : this.despawnDelay - 1;
            if (this.despawnDelay <= 0) {
                this.unleash(true, false);
                this.die();
            }

        }
    }

    private boolean canDespawn() {
        return !this.isTamed() && !this.isLeashedToSomethingOtherThanTheWanderingTrader() && !this.hasSinglePlayerPassenger();
    }

    private boolean isLeashedToWanderingTrader() {
        return this.getLeashHolder() instanceof EntityVillagerTrader;
    }

    private boolean isLeashedToSomethingOtherThanTheWanderingTrader() {
        return this.isLeashed() && !this.isLeashedToWanderingTrader();
    }

    @Nullable
    @Override
    public GroupDataEntity prepare(WorldAccess world, DifficultyDamageScaler difficulty, EnumMobSpawn spawnReason, @Nullable GroupDataEntity entityData, @Nullable NBTTagCompound entityNbt) {
        if (spawnReason == EnumMobSpawn.EVENT) {
            this.setAgeRaw(0);
        }

        if (entityData == null) {
            entityData = new EntityAgeable.GroupDataAgeable(false);
        }

        return super.prepare(world, difficulty, spawnReason, entityData, entityNbt);
    }

    protected static class TraderLlamaDefendWanderingTraderGoal extends PathfinderGoalTarget {
        private final EntityLlama llama;
        private EntityLiving ownerLastHurtBy;
        private int timestamp;

        public TraderLlamaDefendWanderingTraderGoal(EntityLlama llama) {
            super(llama, false);
            this.llama = llama;
            this.setFlags(EnumSet.of(PathfinderGoal.Type.TARGET));
        }

        @Override
        public boolean canUse() {
            if (!this.llama.isLeashed()) {
                return false;
            } else {
                Entity entity = this.llama.getLeashHolder();
                if (!(entity instanceof EntityVillagerTrader)) {
                    return false;
                } else {
                    EntityVillagerTrader wanderingTrader = (EntityVillagerTrader)entity;
                    this.ownerLastHurtBy = wanderingTrader.getLastDamager();
                    int i = wanderingTrader.getLastHurtByMobTimestamp();
                    return i != this.timestamp && this.canAttack(this.ownerLastHurtBy, PathfinderTargetCondition.DEFAULT);
                }
            }
        }

        @Override
        public void start() {
            this.mob.setGoalTarget(this.ownerLastHurtBy);
            Entity entity = this.llama.getLeashHolder();
            if (entity instanceof EntityVillagerTrader) {
                this.timestamp = ((EntityVillagerTrader)entity).getLastHurtByMobTimestamp();
            }

            super.start();
        }
    }
}
