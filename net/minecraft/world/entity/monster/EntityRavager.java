package net.minecraft.world.entity.monster;

import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.particles.Particles;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.TagsEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.goal.PathfinderGoal;
import net.minecraft.world.entity.ai.goal.PathfinderGoalFloat;
import net.minecraft.world.entity.ai.goal.PathfinderGoalLookAtPlayer;
import net.minecraft.world.entity.ai.goal.PathfinderGoalMeleeAttack;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomStrollLand;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalHurtByTarget;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalNearestAttackableTarget;
import net.minecraft.world.entity.ai.navigation.Navigation;
import net.minecraft.world.entity.ai.navigation.NavigationAbstract;
import net.minecraft.world.entity.animal.EntityIronGolem;
import net.minecraft.world.entity.npc.EntityVillagerAbstract;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.raid.EntityRaider;
import net.minecraft.world.entity.vehicle.EntityBoat;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockLeaves;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.Pathfinder;
import net.minecraft.world.level.pathfinder.PathfinderNormal;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.Vec3D;

public class EntityRavager extends EntityRaider {
    private static final Predicate<Entity> NO_RAVAGER_AND_ALIVE = (entity) -> {
        return entity.isAlive() && !(entity instanceof EntityRavager);
    };
    private static final double BASE_MOVEMENT_SPEED = 0.3D;
    private static final double ATTACK_MOVEMENT_SPEED = 0.35D;
    private static final int STUNNED_COLOR = 8356754;
    private static final double STUNNED_COLOR_BLUE = 0.5725490196078431D;
    private static final double STUNNED_COLOR_GREEN = 0.5137254901960784D;
    private static final double STUNNED_COLOR_RED = 0.4980392156862745D;
    private static final int ATTACK_DURATION = 10;
    public static final int STUN_DURATION = 40;
    private int attackTick;
    private int stunnedTick;
    private int roarTick;

    public EntityRavager(EntityTypes<? extends EntityRavager> type, World world) {
        super(type, world);
        this.maxUpStep = 1.0F;
        this.xpReward = 20;
    }

    @Override
    protected void initPathfinder() {
        super.initPathfinder();
        this.goalSelector.addGoal(0, new PathfinderGoalFloat(this));
        this.goalSelector.addGoal(4, new EntityRavager.PathfinderGoalRavagerMeleeAttack());
        this.goalSelector.addGoal(5, new PathfinderGoalRandomStrollLand(this, 0.4D));
        this.goalSelector.addGoal(6, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 6.0F));
        this.goalSelector.addGoal(10, new PathfinderGoalLookAtPlayer(this, EntityInsentient.class, 8.0F));
        this.targetSelector.addGoal(2, (new PathfinderGoalHurtByTarget(this, EntityRaider.class)).setAlertOthers());
        this.targetSelector.addGoal(3, new PathfinderGoalNearestAttackableTarget<>(this, EntityHuman.class, true));
        this.targetSelector.addGoal(4, new PathfinderGoalNearestAttackableTarget<>(this, EntityVillagerAbstract.class, true, (entity) -> {
            return !entity.isBaby();
        }));
        this.targetSelector.addGoal(4, new PathfinderGoalNearestAttackableTarget<>(this, EntityIronGolem.class, true));
    }

    @Override
    protected void updateControlFlags() {
        boolean bl = !(this.getRidingPassenger() instanceof EntityInsentient) || this.getRidingPassenger().getEntityType().is(TagsEntity.RAIDERS);
        boolean bl2 = !(this.getVehicle() instanceof EntityBoat);
        this.goalSelector.setControlFlag(PathfinderGoal.Type.MOVE, bl);
        this.goalSelector.setControlFlag(PathfinderGoal.Type.JUMP, bl && bl2);
        this.goalSelector.setControlFlag(PathfinderGoal.Type.LOOK, bl);
        this.goalSelector.setControlFlag(PathfinderGoal.Type.TARGET, bl);
    }

    public static AttributeProvider.Builder createAttributes() {
        return EntityMonster.createMonsterAttributes().add(GenericAttributes.MAX_HEALTH, 100.0D).add(GenericAttributes.MOVEMENT_SPEED, 0.3D).add(GenericAttributes.KNOCKBACK_RESISTANCE, 0.75D).add(GenericAttributes.ATTACK_DAMAGE, 12.0D).add(GenericAttributes.ATTACK_KNOCKBACK, 1.5D).add(GenericAttributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        nbt.setInt("AttackTick", this.attackTick);
        nbt.setInt("StunTick", this.stunnedTick);
        nbt.setInt("RoarTick", this.roarTick);
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        this.attackTick = nbt.getInt("AttackTick");
        this.stunnedTick = nbt.getInt("StunTick");
        this.roarTick = nbt.getInt("RoarTick");
    }

    @Override
    public SoundEffect getCelebrateSound() {
        return SoundEffects.RAVAGER_CELEBRATE;
    }

    @Override
    protected NavigationAbstract createNavigation(World world) {
        return new EntityRavager.NavigationRavager(this, world);
    }

    @Override
    public int getMaxHeadYRot() {
        return 45;
    }

    @Override
    public double getPassengersRidingOffset() {
        return 2.1D;
    }

    @Override
    public boolean canBeControlledByRider() {
        return !this.isNoAI() && this.getRidingPassenger() instanceof EntityLiving;
    }

    @Nullable
    @Override
    public Entity getRidingPassenger() {
        return this.getFirstPassenger();
    }

    @Override
    public void movementTick() {
        super.movementTick();
        if (this.isAlive()) {
            if (this.isFrozen()) {
                this.getAttributeInstance(GenericAttributes.MOVEMENT_SPEED).setValue(0.0D);
            } else {
                double d = this.getGoalTarget() != null ? 0.35D : 0.3D;
                double e = this.getAttributeInstance(GenericAttributes.MOVEMENT_SPEED).getBaseValue();
                this.getAttributeInstance(GenericAttributes.MOVEMENT_SPEED).setValue(MathHelper.lerp(0.1D, e, d));
            }

            if (this.horizontalCollision && this.level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
                boolean bl = false;
                AxisAlignedBB aABB = this.getBoundingBox().inflate(0.2D);

                for(BlockPosition blockPos : BlockPosition.betweenClosed(MathHelper.floor(aABB.minX), MathHelper.floor(aABB.minY), MathHelper.floor(aABB.minZ), MathHelper.floor(aABB.maxX), MathHelper.floor(aABB.maxY), MathHelper.floor(aABB.maxZ))) {
                    IBlockData blockState = this.level.getType(blockPos);
                    Block block = blockState.getBlock();
                    if (block instanceof BlockLeaves) {
                        bl = this.level.destroyBlock(blockPos, true, this) || bl;
                    }
                }

                if (!bl && this.onGround) {
                    this.jump();
                }
            }

            if (this.roarTick > 0) {
                --this.roarTick;
                if (this.roarTick == 10) {
                    this.roar();
                }
            }

            if (this.attackTick > 0) {
                --this.attackTick;
            }

            if (this.stunnedTick > 0) {
                --this.stunnedTick;
                this.stunEffect();
                if (this.stunnedTick == 0) {
                    this.playSound(SoundEffects.RAVAGER_ROAR, 1.0F, 1.0F);
                    this.roarTick = 20;
                }
            }

        }
    }

    private void stunEffect() {
        if (this.random.nextInt(6) == 0) {
            double d = this.locX() - (double)this.getWidth() * Math.sin((double)(this.yBodyRot * ((float)Math.PI / 180F))) + (this.random.nextDouble() * 0.6D - 0.3D);
            double e = this.locY() + (double)this.getHeight() - 0.3D;
            double f = this.locZ() + (double)this.getWidth() * Math.cos((double)(this.yBodyRot * ((float)Math.PI / 180F))) + (this.random.nextDouble() * 0.6D - 0.3D);
            this.level.addParticle(Particles.ENTITY_EFFECT, d, e, f, 0.4980392156862745D, 0.5137254901960784D, 0.5725490196078431D);
        }

    }

    @Override
    protected boolean isFrozen() {
        return super.isFrozen() || this.attackTick > 0 || this.stunnedTick > 0 || this.roarTick > 0;
    }

    @Override
    public boolean hasLineOfSight(Entity entity) {
        return this.stunnedTick <= 0 && this.roarTick <= 0 ? super.hasLineOfSight(entity) : false;
    }

    @Override
    protected void blockedByShield(EntityLiving target) {
        if (this.roarTick == 0) {
            if (this.random.nextDouble() < 0.5D) {
                this.stunnedTick = 40;
                this.playSound(SoundEffects.RAVAGER_STUNNED, 1.0F, 1.0F);
                this.level.broadcastEntityEffect(this, (byte)39);
                target.collide(this);
            } else {
                this.strongKnockback(target);
            }

            target.hurtMarked = true;
        }

    }

    private void roar() {
        if (this.isAlive()) {
            for(EntityLiving livingEntity : this.level.getEntitiesOfClass(EntityLiving.class, this.getBoundingBox().inflate(4.0D), NO_RAVAGER_AND_ALIVE)) {
                if (!(livingEntity instanceof EntityIllagerAbstract)) {
                    livingEntity.damageEntity(DamageSource.mobAttack(this), 6.0F);
                }

                this.strongKnockback(livingEntity);
            }

            Vec3D vec3 = this.getBoundingBox().getCenter();

            for(int i = 0; i < 40; ++i) {
                double d = this.random.nextGaussian() * 0.2D;
                double e = this.random.nextGaussian() * 0.2D;
                double f = this.random.nextGaussian() * 0.2D;
                this.level.addParticle(Particles.POOF, vec3.x, vec3.y, vec3.z, d, e, f);
            }

            this.level.gameEvent(this, GameEvent.RAVAGER_ROAR, this.eyeBlockPosition());
        }

    }

    private void strongKnockback(Entity entity) {
        double d = entity.locX() - this.locX();
        double e = entity.locZ() - this.locZ();
        double f = Math.max(d * d + e * e, 0.001D);
        entity.push(d / f * 4.0D, 0.2D, e / f * 4.0D);
    }

    @Override
    public void handleEntityEvent(byte status) {
        if (status == 4) {
            this.attackTick = 10;
            this.playSound(SoundEffects.RAVAGER_ATTACK, 1.0F, 1.0F);
        } else if (status == 39) {
            this.stunnedTick = 40;
        }

        super.handleEntityEvent(status);
    }

    public int getAttackTick() {
        return this.attackTick;
    }

    public int getStunnedTick() {
        return this.stunnedTick;
    }

    public int getRoarTick() {
        return this.roarTick;
    }

    @Override
    public boolean attackEntity(Entity target) {
        this.attackTick = 10;
        this.level.broadcastEntityEffect(this, (byte)4);
        this.playSound(SoundEffects.RAVAGER_ATTACK, 1.0F, 1.0F);
        return super.attackEntity(target);
    }

    @Nullable
    @Override
    protected SoundEffect getSoundAmbient() {
        return SoundEffects.RAVAGER_AMBIENT;
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return SoundEffects.RAVAGER_HURT;
    }

    @Override
    public SoundEffect getSoundDeath() {
        return SoundEffects.RAVAGER_DEATH;
    }

    @Override
    protected void playStepSound(BlockPosition pos, IBlockData state) {
        this.playSound(SoundEffects.RAVAGER_STEP, 0.15F, 1.0F);
    }

    @Override
    public boolean checkSpawnObstruction(IWorldReader world) {
        return !world.containsLiquid(this.getBoundingBox());
    }

    @Override
    public void applyRaidBuffs(int wave, boolean unused) {
    }

    @Override
    public boolean canBeLeader() {
        return false;
    }

    static class NavigationRavager extends Navigation {
        public NavigationRavager(EntityInsentient mob, World world) {
            super(mob, world);
        }

        @Override
        protected Pathfinder createPathFinder(int range) {
            this.nodeEvaluator = new EntityRavager.PathfinderRavager();
            return new Pathfinder(this.nodeEvaluator, range);
        }
    }

    class PathfinderGoalRavagerMeleeAttack extends PathfinderGoalMeleeAttack {
        public PathfinderGoalRavagerMeleeAttack() {
            super(EntityRavager.this, 1.0D, true);
        }

        @Override
        protected double getAttackReachSqr(EntityLiving entity) {
            float f = EntityRavager.this.getWidth() - 0.1F;
            return (double)(f * 2.0F * f * 2.0F + entity.getWidth());
        }
    }

    static class PathfinderRavager extends PathfinderNormal {
        @Override
        protected PathType evaluateBlockPathType(IBlockAccess world, boolean canOpenDoors, boolean canEnterOpenDoors, BlockPosition pos, PathType type) {
            return type == PathType.LEAVES ? PathType.OPEN : super.evaluateBlockPathType(world, canOpenDoors, canEnterOpenDoors, pos, type);
        }
    }
}
