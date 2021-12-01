package net.minecraft.world.entity.monster;

import javax.annotation.Nullable;
import net.minecraft.core.particles.Particles;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.DifficultyDamageScaler;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.EnumMonsterType;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.goal.PathfinderGoalBowShoot;
import net.minecraft.world.entity.ai.goal.PathfinderGoalFloat;
import net.minecraft.world.entity.ai.goal.PathfinderGoalLookAtPlayer;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomStroll;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalHurtByTarget;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalNearestAttackableTarget;
import net.minecraft.world.entity.animal.EntityIronGolem;
import net.minecraft.world.entity.npc.EntityVillagerAbstract;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.EntityArrow;
import net.minecraft.world.entity.projectile.ProjectileHelper;
import net.minecraft.world.entity.raid.EntityRaider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldAccess;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.Vec3D;

public class EntityIllagerIllusioner extends EntityIllagerWizard implements IRangedEntity {
    private static final int NUM_ILLUSIONS = 4;
    private static final int ILLUSION_TRANSITION_TICKS = 3;
    private static final int ILLUSION_SPREAD = 3;
    private int clientSideIllusionTicks;
    private final Vec3D[][] clientSideIllusionOffsets;

    public EntityIllagerIllusioner(EntityTypes<? extends EntityIllagerIllusioner> type, World world) {
        super(type, world);
        this.xpReward = 5;
        this.clientSideIllusionOffsets = new Vec3D[2][4];

        for(int i = 0; i < 4; ++i) {
            this.clientSideIllusionOffsets[0][i] = Vec3D.ZERO;
            this.clientSideIllusionOffsets[1][i] = Vec3D.ZERO;
        }

    }

    @Override
    protected void initPathfinder() {
        super.initPathfinder();
        this.goalSelector.addGoal(0, new PathfinderGoalFloat(this));
        this.goalSelector.addGoal(1, new EntityIllagerWizard.SpellcasterCastingSpellGoal());
        this.goalSelector.addGoal(4, new EntityIllagerIllusioner.IllusionerMirrorSpellGoal());
        this.goalSelector.addGoal(5, new EntityIllagerIllusioner.IllusionerBlindnessSpellGoal());
        this.goalSelector.addGoal(6, new PathfinderGoalBowShoot<>(this, 0.5D, 20, 15.0F));
        this.goalSelector.addGoal(8, new PathfinderGoalRandomStroll(this, 0.6D));
        this.goalSelector.addGoal(9, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 3.0F, 1.0F));
        this.goalSelector.addGoal(10, new PathfinderGoalLookAtPlayer(this, EntityInsentient.class, 8.0F));
        this.targetSelector.addGoal(1, (new PathfinderGoalHurtByTarget(this, EntityRaider.class)).setAlertOthers());
        this.targetSelector.addGoal(2, (new PathfinderGoalNearestAttackableTarget<>(this, EntityHuman.class, true)).setUnseenMemoryTicks(300));
        this.targetSelector.addGoal(3, (new PathfinderGoalNearestAttackableTarget<>(this, EntityVillagerAbstract.class, false)).setUnseenMemoryTicks(300));
        this.targetSelector.addGoal(3, (new PathfinderGoalNearestAttackableTarget<>(this, EntityIronGolem.class, false)).setUnseenMemoryTicks(300));
    }

    public static AttributeProvider.Builder createAttributes() {
        return EntityMonster.createMonsterAttributes().add(GenericAttributes.MOVEMENT_SPEED, 0.5D).add(GenericAttributes.FOLLOW_RANGE, 18.0D).add(GenericAttributes.MAX_HEALTH, 32.0D);
    }

    @Override
    public GroupDataEntity prepare(WorldAccess world, DifficultyDamageScaler difficulty, EnumMobSpawn spawnReason, @Nullable GroupDataEntity entityData, @Nullable NBTTagCompound entityNbt) {
        this.setSlot(EnumItemSlot.MAINHAND, new ItemStack(Items.BOW));
        return super.prepare(world, difficulty, spawnReason, entityData, entityNbt);
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
    }

    @Override
    public AxisAlignedBB getBoundingBoxForCulling() {
        return this.getBoundingBox().grow(3.0D, 0.0D, 3.0D);
    }

    @Override
    public void movementTick() {
        super.movementTick();
        if (this.level.isClientSide && this.isInvisible()) {
            --this.clientSideIllusionTicks;
            if (this.clientSideIllusionTicks < 0) {
                this.clientSideIllusionTicks = 0;
            }

            if (this.hurtTime != 1 && this.tickCount % 1200 != 0) {
                if (this.hurtTime == this.hurtDuration - 1) {
                    this.clientSideIllusionTicks = 3;

                    for(int l = 0; l < 4; ++l) {
                        this.clientSideIllusionOffsets[0][l] = this.clientSideIllusionOffsets[1][l];
                        this.clientSideIllusionOffsets[1][l] = new Vec3D(0.0D, 0.0D, 0.0D);
                    }
                }
            } else {
                this.clientSideIllusionTicks = 3;
                float f = -6.0F;
                int i = 13;

                for(int j = 0; j < 4; ++j) {
                    this.clientSideIllusionOffsets[0][j] = this.clientSideIllusionOffsets[1][j];
                    this.clientSideIllusionOffsets[1][j] = new Vec3D((double)(-6.0F + (float)this.random.nextInt(13)) * 0.5D, (double)Math.max(0, this.random.nextInt(6) - 4), (double)(-6.0F + (float)this.random.nextInt(13)) * 0.5D);
                }

                for(int k = 0; k < 16; ++k) {
                    this.level.addParticle(Particles.CLOUD, this.getRandomX(0.5D), this.getRandomY(), this.getZ(0.5D), 0.0D, 0.0D, 0.0D);
                }

                this.level.playLocalSound(this.locX(), this.locY(), this.locZ(), SoundEffects.ILLUSIONER_MIRROR_MOVE, this.getSoundCategory(), 1.0F, 1.0F, false);
            }
        }

    }

    @Override
    public SoundEffect getCelebrateSound() {
        return SoundEffects.ILLUSIONER_AMBIENT;
    }

    public Vec3D[] getIllusionOffsets(float f) {
        if (this.clientSideIllusionTicks <= 0) {
            return this.clientSideIllusionOffsets[1];
        } else {
            double d = (double)(((float)this.clientSideIllusionTicks - f) / 3.0F);
            d = Math.pow(d, 0.25D);
            Vec3D[] vec3s = new Vec3D[4];

            for(int i = 0; i < 4; ++i) {
                vec3s[i] = this.clientSideIllusionOffsets[1][i].scale(1.0D - d).add(this.clientSideIllusionOffsets[0][i].scale(d));
            }

            return vec3s;
        }
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        if (super.isAlliedTo(other)) {
            return true;
        } else if (other instanceof EntityLiving && ((EntityLiving)other).getMonsterType() == EnumMonsterType.ILLAGER) {
            return this.getScoreboardTeam() == null && other.getScoreboardTeam() == null;
        } else {
            return false;
        }
    }

    @Override
    protected SoundEffect getSoundAmbient() {
        return SoundEffects.ILLUSIONER_AMBIENT;
    }

    @Override
    public SoundEffect getSoundDeath() {
        return SoundEffects.ILLUSIONER_DEATH;
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return SoundEffects.ILLUSIONER_HURT;
    }

    @Override
    protected SoundEffect getSoundCastSpell() {
        return SoundEffects.ILLUSIONER_CAST_SPELL;
    }

    @Override
    public void applyRaidBuffs(int wave, boolean unused) {
    }

    @Override
    public void performRangedAttack(EntityLiving target, float pullProgress) {
        ItemStack itemStack = this.getProjectile(this.getItemInHand(ProjectileHelper.getWeaponHoldingHand(this, Items.BOW)));
        EntityArrow abstractArrow = ProjectileHelper.getMobArrow(this, itemStack, pullProgress);
        double d = target.locX() - this.locX();
        double e = target.getY(0.3333333333333333D) - abstractArrow.locY();
        double f = target.locZ() - this.locZ();
        double g = Math.sqrt(d * d + f * f);
        abstractArrow.shoot(d, e + g * (double)0.2F, f, 1.6F, (float)(14 - this.level.getDifficulty().getId() * 4));
        this.playSound(SoundEffects.SKELETON_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
        this.level.addEntity(abstractArrow);
    }

    @Override
    public EntityIllagerAbstract.IllagerArmPose getArmPose() {
        if (this.isCastingSpell()) {
            return EntityIllagerAbstract.IllagerArmPose.SPELLCASTING;
        } else {
            return this.isAggressive() ? EntityIllagerAbstract.IllagerArmPose.BOW_AND_ARROW : EntityIllagerAbstract.IllagerArmPose.CROSSED;
        }
    }

    class IllusionerBlindnessSpellGoal extends EntityIllagerWizard.PathfinderGoalCastSpell {
        private int lastTargetId;

        @Override
        public boolean canUse() {
            if (!super.canUse()) {
                return false;
            } else if (EntityIllagerIllusioner.this.getGoalTarget() == null) {
                return false;
            } else if (EntityIllagerIllusioner.this.getGoalTarget().getId() == this.lastTargetId) {
                return false;
            } else {
                return EntityIllagerIllusioner.this.level.getDamageScaler(EntityIllagerIllusioner.this.getChunkCoordinates()).isHarderThan((float)EnumDifficulty.NORMAL.ordinal());
            }
        }

        @Override
        public void start() {
            super.start();
            EntityLiving livingEntity = EntityIllagerIllusioner.this.getGoalTarget();
            if (livingEntity != null) {
                this.lastTargetId = livingEntity.getId();
            }

        }

        @Override
        protected int getCastingTime() {
            return 20;
        }

        @Override
        protected int getCastingInterval() {
            return 180;
        }

        @Override
        protected void performSpellCasting() {
            EntityIllagerIllusioner.this.getGoalTarget().addEffect(new MobEffect(MobEffectList.BLINDNESS, 400), EntityIllagerIllusioner.this);
        }

        @Override
        protected SoundEffect getSpellPrepareSound() {
            return SoundEffects.ILLUSIONER_PREPARE_BLINDNESS;
        }

        @Override
        protected EntityIllagerWizard.Spell getCastSpell() {
            return EntityIllagerWizard.Spell.BLINDNESS;
        }
    }

    class IllusionerMirrorSpellGoal extends EntityIllagerWizard.PathfinderGoalCastSpell {
        @Override
        public boolean canUse() {
            if (!super.canUse()) {
                return false;
            } else {
                return !EntityIllagerIllusioner.this.hasEffect(MobEffectList.INVISIBILITY);
            }
        }

        @Override
        protected int getCastingTime() {
            return 20;
        }

        @Override
        protected int getCastingInterval() {
            return 340;
        }

        @Override
        protected void performSpellCasting() {
            EntityIllagerIllusioner.this.addEffect(new MobEffect(MobEffectList.INVISIBILITY, 1200));
        }

        @Nullable
        @Override
        protected SoundEffect getSpellPrepareSound() {
            return SoundEffects.ILLUSIONER_PREPARE_MIRROR;
        }

        @Override
        protected EntityIllagerWizard.Spell getCastSpell() {
            return EntityIllagerWizard.Spell.DISAPPEAR;
        }
    }
}
