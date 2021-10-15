package net.minecraft.world.entity.monster;

import java.util.Collection;
import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAreaEffectCloud;
import net.minecraft.world.entity.EntityLightning;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.IPowerable;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.goal.PathfinderGoalAvoidTarget;
import net.minecraft.world.entity.ai.goal.PathfinderGoalFloat;
import net.minecraft.world.entity.ai.goal.PathfinderGoalLookAtPlayer;
import net.minecraft.world.entity.ai.goal.PathfinderGoalMeleeAttack;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomLookaround;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomStrollLand;
import net.minecraft.world.entity.ai.goal.PathfinderGoalSwell;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalHurtByTarget;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalNearestAttackableTarget;
import net.minecraft.world.entity.animal.EntityCat;
import net.minecraft.world.entity.animal.EntityOcelot;
import net.minecraft.world.entity.animal.goat.EntityGoat;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.World;
import net.minecraft.world.level.gameevent.GameEvent;

public class EntityCreeper extends EntityMonster implements IPowerable {
    private static final DataWatcherObject<Integer> DATA_SWELL_DIR = DataWatcher.defineId(EntityCreeper.class, DataWatcherRegistry.INT);
    private static final DataWatcherObject<Boolean> DATA_IS_POWERED = DataWatcher.defineId(EntityCreeper.class, DataWatcherRegistry.BOOLEAN);
    private static final DataWatcherObject<Boolean> DATA_IS_IGNITED = DataWatcher.defineId(EntityCreeper.class, DataWatcherRegistry.BOOLEAN);
    private int oldSwell;
    public int swell;
    public int maxSwell = 30;
    public int explosionRadius = 3;
    private int droppedSkulls;

    public EntityCreeper(EntityTypes<? extends EntityCreeper> type, World world) {
        super(type, world);
    }

    @Override
    protected void initPathfinder() {
        this.goalSelector.addGoal(1, new PathfinderGoalFloat(this));
        this.goalSelector.addGoal(2, new PathfinderGoalSwell(this));
        this.goalSelector.addGoal(3, new PathfinderGoalAvoidTarget<>(this, EntityOcelot.class, 6.0F, 1.0D, 1.2D));
        this.goalSelector.addGoal(3, new PathfinderGoalAvoidTarget<>(this, EntityCat.class, 6.0F, 1.0D, 1.2D));
        this.goalSelector.addGoal(4, new PathfinderGoalMeleeAttack(this, 1.0D, false));
        this.goalSelector.addGoal(5, new PathfinderGoalRandomStrollLand(this, 0.8D));
        this.goalSelector.addGoal(6, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 8.0F));
        this.goalSelector.addGoal(6, new PathfinderGoalRandomLookaround(this));
        this.targetSelector.addGoal(1, new PathfinderGoalNearestAttackableTarget<>(this, EntityHuman.class, true));
        this.targetSelector.addGoal(2, new PathfinderGoalHurtByTarget(this));
    }

    public static AttributeProvider.Builder createAttributes() {
        return EntityMonster.createMonsterAttributes().add(GenericAttributes.MOVEMENT_SPEED, 0.25D);
    }

    @Override
    public int getMaxFallDistance() {
        return this.getGoalTarget() == null ? 3 : 3 + (int)(this.getHealth() - 1.0F);
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        boolean bl = super.causeFallDamage(fallDistance, damageMultiplier, damageSource);
        this.swell = (int)((float)this.swell + fallDistance * 1.5F);
        if (this.swell > this.maxSwell - 5) {
            this.swell = this.maxSwell - 5;
        }

        return bl;
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.entityData.register(DATA_SWELL_DIR, -1);
        this.entityData.register(DATA_IS_POWERED, false);
        this.entityData.register(DATA_IS_IGNITED, false);
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        if (this.entityData.get(DATA_IS_POWERED)) {
            nbt.setBoolean("powered", true);
        }

        nbt.setShort("Fuse", (short)this.maxSwell);
        nbt.setByte("ExplosionRadius", (byte)this.explosionRadius);
        nbt.setBoolean("ignited", this.isIgnited());
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        this.entityData.set(DATA_IS_POWERED, nbt.getBoolean("powered"));
        if (nbt.hasKeyOfType("Fuse", 99)) {
            this.maxSwell = nbt.getShort("Fuse");
        }

        if (nbt.hasKeyOfType("ExplosionRadius", 99)) {
            this.explosionRadius = nbt.getByte("ExplosionRadius");
        }

        if (nbt.getBoolean("ignited")) {
            this.ignite();
        }

    }

    @Override
    public void tick() {
        if (this.isAlive()) {
            this.oldSwell = this.swell;
            if (this.isIgnited()) {
                this.setSwellDir(1);
            }

            int i = this.getSwellDir();
            if (i > 0 && this.swell == 0) {
                this.playSound(SoundEffects.CREEPER_PRIMED, 1.0F, 0.5F);
                this.gameEvent(GameEvent.PRIME_FUSE);
            }

            this.swell += i;
            if (this.swell < 0) {
                this.swell = 0;
            }

            if (this.swell >= this.maxSwell) {
                this.swell = this.maxSwell;
                this.explode();
            }
        }

        super.tick();
    }

    @Override
    public void setGoalTarget(@Nullable EntityLiving target) {
        if (!(target instanceof EntityGoat)) {
            super.setGoalTarget(target);
        }
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return SoundEffects.CREEPER_HURT;
    }

    @Override
    public SoundEffect getSoundDeath() {
        return SoundEffects.CREEPER_DEATH;
    }

    @Override
    protected void dropDeathLoot(DamageSource source, int lootingMultiplier, boolean allowDrops) {
        super.dropDeathLoot(source, lootingMultiplier, allowDrops);
        Entity entity = source.getEntity();
        if (entity != this && entity instanceof EntityCreeper) {
            EntityCreeper creeper = (EntityCreeper)entity;
            if (creeper.canCauseHeadDrop()) {
                creeper.setCausedHeadDrop();
                this.spawnAtLocation(Items.CREEPER_HEAD);
            }
        }

    }

    @Override
    public boolean attackEntity(Entity target) {
        return true;
    }

    @Override
    public boolean isPowered() {
        return this.entityData.get(DATA_IS_POWERED);
    }

    public float getSwelling(float timeDelta) {
        return MathHelper.lerp(timeDelta, (float)this.oldSwell, (float)this.swell) / (float)(this.maxSwell - 2);
    }

    public int getSwellDir() {
        return this.entityData.get(DATA_SWELL_DIR);
    }

    public void setSwellDir(int fuseSpeed) {
        this.entityData.set(DATA_SWELL_DIR, fuseSpeed);
    }

    @Override
    public void onLightningStrike(WorldServer world, EntityLightning lightning) {
        super.onLightningStrike(world, lightning);
        this.entityData.set(DATA_IS_POWERED, true);
    }

    @Override
    protected EnumInteractionResult mobInteract(EntityHuman player, EnumHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (itemStack.is(Items.FLINT_AND_STEEL)) {
            this.level.playSound(player, this.locX(), this.locY(), this.locZ(), SoundEffects.FLINTANDSTEEL_USE, this.getSoundCategory(), 1.0F, this.random.nextFloat() * 0.4F + 0.8F);
            if (!this.level.isClientSide) {
                this.ignite();
                itemStack.damage(1, player, (playerx) -> {
                    playerx.broadcastItemBreak(hand);
                });
            }

            return EnumInteractionResult.sidedSuccess(this.level.isClientSide);
        } else {
            return super.mobInteract(player, hand);
        }
    }

    public void explode() {
        if (!this.level.isClientSide) {
            Explosion.Effect blockInteraction = this.level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) ? Explosion.Effect.DESTROY : Explosion.Effect.NONE;
            float f = this.isPowered() ? 2.0F : 1.0F;
            this.dead = true;
            this.level.explode(this, this.locX(), this.locY(), this.locZ(), (float)this.explosionRadius * f, blockInteraction);
            this.die();
            this.createEffectCloud();
        }

    }

    private void createEffectCloud() {
        Collection<MobEffect> collection = this.getEffects();
        if (!collection.isEmpty()) {
            EntityAreaEffectCloud areaEffectCloud = new EntityAreaEffectCloud(this.level, this.locX(), this.locY(), this.locZ());
            areaEffectCloud.setRadius(2.5F);
            areaEffectCloud.setRadiusOnUse(-0.5F);
            areaEffectCloud.setWaitTime(10);
            areaEffectCloud.setDuration(areaEffectCloud.getDuration() / 2);
            areaEffectCloud.setRadiusPerTick(-areaEffectCloud.getRadius() / (float)areaEffectCloud.getDuration());

            for(MobEffect mobEffectInstance : collection) {
                areaEffectCloud.addEffect(new MobEffect(mobEffectInstance));
            }

            this.level.addEntity(areaEffectCloud);
        }

    }

    public boolean isIgnited() {
        return this.entityData.get(DATA_IS_IGNITED);
    }

    public void ignite() {
        this.entityData.set(DATA_IS_IGNITED, true);
    }

    public boolean canCauseHeadDrop() {
        return this.isPowered() && this.droppedSkulls < 1;
    }

    public void setCausedHeadDrop() {
        ++this.droppedSkulls;
    }
}
