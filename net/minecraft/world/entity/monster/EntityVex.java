package net.minecraft.world.entity.monster;

import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.util.MathHelper;
import net.minecraft.world.DifficultyDamageScaler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.EnumMoveType;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.control.ControllerMove;
import net.minecraft.world.entity.ai.goal.PathfinderGoal;
import net.minecraft.world.entity.ai.goal.PathfinderGoalFloat;
import net.minecraft.world.entity.ai.goal.PathfinderGoalLookAtPlayer;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalHurtByTarget;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalNearestAttackableTarget;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalTarget;
import net.minecraft.world.entity.ai.targeting.PathfinderTargetCondition;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.raid.EntityRaider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldAccess;
import net.minecraft.world.phys.Vec3D;

public class EntityVex extends EntityMonster {
    public static final float FLAP_DEGREES_PER_TICK = 45.836624F;
    public static final int TICKS_PER_FLAP = MathHelper.ceil(3.9269907F);
    protected static final DataWatcherObject<Byte> DATA_FLAGS_ID = DataWatcher.defineId(EntityVex.class, DataWatcherRegistry.BYTE);
    private static final int FLAG_IS_CHARGING = 1;
    @Nullable
    EntityInsentient owner;
    @Nullable
    private BlockPosition boundOrigin;
    private boolean hasLimitedLife;
    private int limitedLifeTicks;

    public EntityVex(EntityTypes<? extends EntityVex> type, World world) {
        super(type, world);
        this.moveControl = new EntityVex.VexMoveControl(this);
        this.xpReward = 3;
    }

    @Override
    public boolean isFlapping() {
        return this.tickCount % TICKS_PER_FLAP == 0;
    }

    @Override
    public void move(EnumMoveType movementType, Vec3D movement) {
        super.move(movementType, movement);
        this.checkBlockCollisions();
    }

    @Override
    public void tick() {
        this.noPhysics = true;
        super.tick();
        this.noPhysics = false;
        this.setNoGravity(true);
        if (this.hasLimitedLife && --this.limitedLifeTicks <= 0) {
            this.limitedLifeTicks = 20;
            this.damageEntity(DamageSource.STARVE, 1.0F);
        }

    }

    @Override
    protected void initPathfinder() {
        super.initPathfinder();
        this.goalSelector.addGoal(0, new PathfinderGoalFloat(this));
        this.goalSelector.addGoal(4, new EntityVex.VexChargeAttackGoal());
        this.goalSelector.addGoal(8, new EntityVex.VexRandomMoveGoal());
        this.goalSelector.addGoal(9, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 3.0F, 1.0F));
        this.goalSelector.addGoal(10, new PathfinderGoalLookAtPlayer(this, EntityInsentient.class, 8.0F));
        this.targetSelector.addGoal(1, (new PathfinderGoalHurtByTarget(this, EntityRaider.class)).setAlertOthers());
        this.targetSelector.addGoal(2, new EntityVex.VexCopyOwnerTargetGoal(this));
        this.targetSelector.addGoal(3, new PathfinderGoalNearestAttackableTarget<>(this, EntityHuman.class, true));
    }

    public static AttributeProvider.Builder createAttributes() {
        return EntityMonster.createMonsterAttributes().add(GenericAttributes.MAX_HEALTH, 14.0D).add(GenericAttributes.ATTACK_DAMAGE, 4.0D);
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.entityData.register(DATA_FLAGS_ID, (byte)0);
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        if (nbt.hasKey("BoundX")) {
            this.boundOrigin = new BlockPosition(nbt.getInt("BoundX"), nbt.getInt("BoundY"), nbt.getInt("BoundZ"));
        }

        if (nbt.hasKey("LifeTicks")) {
            this.setLimitedLife(nbt.getInt("LifeTicks"));
        }

    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        if (this.boundOrigin != null) {
            nbt.setInt("BoundX", this.boundOrigin.getX());
            nbt.setInt("BoundY", this.boundOrigin.getY());
            nbt.setInt("BoundZ", this.boundOrigin.getZ());
        }

        if (this.hasLimitedLife) {
            nbt.setInt("LifeTicks", this.limitedLifeTicks);
        }

    }

    @Nullable
    public EntityInsentient getOwner() {
        return this.owner;
    }

    @Nullable
    public BlockPosition getBoundOrigin() {
        return this.boundOrigin;
    }

    public void setBoundOrigin(@Nullable BlockPosition pos) {
        this.boundOrigin = pos;
    }

    private boolean getVexFlag(int mask) {
        int i = this.entityData.get(DATA_FLAGS_ID);
        return (i & mask) != 0;
    }

    private void setVexFlag(int mask, boolean value) {
        int i = this.entityData.get(DATA_FLAGS_ID);
        if (value) {
            i = i | mask;
        } else {
            i = i & ~mask;
        }

        this.entityData.set(DATA_FLAGS_ID, (byte)(i & 255));
    }

    public boolean isCharging() {
        return this.getVexFlag(1);
    }

    public void setCharging(boolean charging) {
        this.setVexFlag(1, charging);
    }

    public void setOwner(EntityInsentient owner) {
        this.owner = owner;
    }

    public void setLimitedLife(int lifeTicks) {
        this.hasLimitedLife = true;
        this.limitedLifeTicks = lifeTicks;
    }

    @Override
    protected SoundEffect getSoundAmbient() {
        return SoundEffects.VEX_AMBIENT;
    }

    @Override
    public SoundEffect getSoundDeath() {
        return SoundEffects.VEX_DEATH;
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return SoundEffects.VEX_HURT;
    }

    @Override
    public float getBrightness() {
        return 1.0F;
    }

    @Nullable
    @Override
    public GroupDataEntity prepare(WorldAccess world, DifficultyDamageScaler difficulty, EnumMobSpawn spawnReason, @Nullable GroupDataEntity entityData, @Nullable NBTTagCompound entityNbt) {
        this.populateDefaultEquipmentSlots(difficulty);
        this.populateDefaultEquipmentEnchantments(difficulty);
        return super.prepare(world, difficulty, spawnReason, entityData, entityNbt);
    }

    @Override
    protected void populateDefaultEquipmentSlots(DifficultyDamageScaler difficulty) {
        this.setSlot(EnumItemSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        this.setDropChance(EnumItemSlot.MAINHAND, 0.0F);
    }

    class VexChargeAttackGoal extends PathfinderGoal {
        public VexChargeAttackGoal() {
            this.setFlags(EnumSet.of(PathfinderGoal.Type.MOVE));
        }

        @Override
        public boolean canUse() {
            if (EntityVex.this.getGoalTarget() != null && !EntityVex.this.getControllerMove().hasWanted() && EntityVex.this.random.nextInt(reducedTickDelay(7)) == 0) {
                return EntityVex.this.distanceToSqr(EntityVex.this.getGoalTarget()) > 4.0D;
            } else {
                return false;
            }
        }

        @Override
        public boolean canContinueToUse() {
            return EntityVex.this.getControllerMove().hasWanted() && EntityVex.this.isCharging() && EntityVex.this.getGoalTarget() != null && EntityVex.this.getGoalTarget().isAlive();
        }

        @Override
        public void start() {
            EntityLiving livingEntity = EntityVex.this.getGoalTarget();
            if (livingEntity != null) {
                Vec3D vec3 = livingEntity.getEyePosition();
                EntityVex.this.moveControl.setWantedPosition(vec3.x, vec3.y, vec3.z, 1.0D);
            }

            EntityVex.this.setCharging(true);
            EntityVex.this.playSound(SoundEffects.VEX_CHARGE, 1.0F, 1.0F);
        }

        @Override
        public void stop() {
            EntityVex.this.setCharging(false);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            EntityLiving livingEntity = EntityVex.this.getGoalTarget();
            if (livingEntity != null) {
                if (EntityVex.this.getBoundingBox().intersects(livingEntity.getBoundingBox())) {
                    EntityVex.this.attackEntity(livingEntity);
                    EntityVex.this.setCharging(false);
                } else {
                    double d = EntityVex.this.distanceToSqr(livingEntity);
                    if (d < 9.0D) {
                        Vec3D vec3 = livingEntity.getEyePosition();
                        EntityVex.this.moveControl.setWantedPosition(vec3.x, vec3.y, vec3.z, 1.0D);
                    }
                }

            }
        }
    }

    class VexCopyOwnerTargetGoal extends PathfinderGoalTarget {
        private final PathfinderTargetCondition copyOwnerTargeting = PathfinderTargetCondition.forNonCombat().ignoreLineOfSight().ignoreInvisibilityTesting();

        public VexCopyOwnerTargetGoal(EntityCreature mob) {
            super(mob, false);
        }

        @Override
        public boolean canUse() {
            return EntityVex.this.owner != null && EntityVex.this.owner.getGoalTarget() != null && this.canAttack(EntityVex.this.owner.getGoalTarget(), this.copyOwnerTargeting);
        }

        @Override
        public void start() {
            EntityVex.this.setGoalTarget(EntityVex.this.owner.getGoalTarget());
            super.start();
        }
    }

    class VexMoveControl extends ControllerMove {
        public VexMoveControl(EntityVex owner) {
            super(owner);
        }

        @Override
        public void tick() {
            if (this.operation == ControllerMove.Operation.MOVE_TO) {
                Vec3D vec3 = new Vec3D(this.wantedX - EntityVex.this.locX(), this.wantedY - EntityVex.this.locY(), this.wantedZ - EntityVex.this.locZ());
                double d = vec3.length();
                if (d < EntityVex.this.getBoundingBox().getSize()) {
                    this.operation = ControllerMove.Operation.WAIT;
                    EntityVex.this.setMot(EntityVex.this.getMot().scale(0.5D));
                } else {
                    EntityVex.this.setMot(EntityVex.this.getMot().add(vec3.scale(this.speedModifier * 0.05D / d)));
                    if (EntityVex.this.getGoalTarget() == null) {
                        Vec3D vec32 = EntityVex.this.getMot();
                        EntityVex.this.setYRot(-((float)MathHelper.atan2(vec32.x, vec32.z)) * (180F / (float)Math.PI));
                        EntityVex.this.yBodyRot = EntityVex.this.getYRot();
                    } else {
                        double e = EntityVex.this.getGoalTarget().locX() - EntityVex.this.locX();
                        double f = EntityVex.this.getGoalTarget().locZ() - EntityVex.this.locZ();
                        EntityVex.this.setYRot(-((float)MathHelper.atan2(e, f)) * (180F / (float)Math.PI));
                        EntityVex.this.yBodyRot = EntityVex.this.getYRot();
                    }
                }

            }
        }
    }

    class VexRandomMoveGoal extends PathfinderGoal {
        public VexRandomMoveGoal() {
            this.setFlags(EnumSet.of(PathfinderGoal.Type.MOVE));
        }

        @Override
        public boolean canUse() {
            return !EntityVex.this.getControllerMove().hasWanted() && EntityVex.this.random.nextInt(reducedTickDelay(7)) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void tick() {
            BlockPosition blockPos = EntityVex.this.getBoundOrigin();
            if (blockPos == null) {
                blockPos = EntityVex.this.getChunkCoordinates();
            }

            for(int i = 0; i < 3; ++i) {
                BlockPosition blockPos2 = blockPos.offset(EntityVex.this.random.nextInt(15) - 7, EntityVex.this.random.nextInt(11) - 5, EntityVex.this.random.nextInt(15) - 7);
                if (EntityVex.this.level.isEmpty(blockPos2)) {
                    EntityVex.this.moveControl.setWantedPosition((double)blockPos2.getX() + 0.5D, (double)blockPos2.getY() + 0.5D, (double)blockPos2.getZ() + 0.5D, 0.25D);
                    if (EntityVex.this.getGoalTarget() == null) {
                        EntityVex.this.getControllerLook().setLookAt((double)blockPos2.getX() + 0.5D, (double)blockPos2.getY() + 0.5D, (double)blockPos2.getZ() + 0.5D, 180.0F, 20.0F);
                    }
                    break;
                }
            }

        }
    }
}
