package net.minecraft.world.entity.monster;

import java.util.EnumSet;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.EntityDamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.EntitySize;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.EnumMonsterType;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.goal.PathfinderGoal;
import net.minecraft.world.entity.ai.goal.PathfinderGoalFloat;
import net.minecraft.world.entity.ai.goal.PathfinderGoalMeleeAttack;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomStroll;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalHurtByTarget;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalNearestAttackableTarget;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockMonsterEggs;
import net.minecraft.world.level.block.state.IBlockData;

public class EntitySilverfish extends EntityMonster {
    @Nullable
    private EntitySilverfish.PathfinderGoalSilverfishWakeOthers friendsGoal;

    public EntitySilverfish(EntityTypes<? extends EntitySilverfish> type, World world) {
        super(type, world);
    }

    @Override
    protected void initPathfinder() {
        this.friendsGoal = new EntitySilverfish.PathfinderGoalSilverfishWakeOthers(this);
        this.goalSelector.addGoal(1, new PathfinderGoalFloat(this));
        this.goalSelector.addGoal(3, this.friendsGoal);
        this.goalSelector.addGoal(4, new PathfinderGoalMeleeAttack(this, 1.0D, false));
        this.goalSelector.addGoal(5, new EntitySilverfish.PathfinderGoalSilverfishHideInBlock(this));
        this.targetSelector.addGoal(1, (new PathfinderGoalHurtByTarget(this)).setAlertOthers());
        this.targetSelector.addGoal(2, new PathfinderGoalNearestAttackableTarget<>(this, EntityHuman.class, true));
    }

    @Override
    public double getMyRidingOffset() {
        return 0.1D;
    }

    @Override
    protected float getStandingEyeHeight(EntityPose pose, EntitySize dimensions) {
        return 0.13F;
    }

    public static AttributeProvider.Builder createAttributes() {
        return EntityMonster.createMonsterAttributes().add(GenericAttributes.MAX_HEALTH, 8.0D).add(GenericAttributes.MOVEMENT_SPEED, 0.25D).add(GenericAttributes.ATTACK_DAMAGE, 1.0D);
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.EVENTS;
    }

    @Override
    protected SoundEffect getSoundAmbient() {
        return SoundEffects.SILVERFISH_AMBIENT;
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return SoundEffects.SILVERFISH_HURT;
    }

    @Override
    public SoundEffect getSoundDeath() {
        return SoundEffects.SILVERFISH_DEATH;
    }

    @Override
    protected void playStepSound(BlockPosition pos, IBlockData state) {
        this.playSound(SoundEffects.SILVERFISH_STEP, 0.15F, 1.0F);
    }

    @Override
    public boolean damageEntity(DamageSource source, float amount) {
        if (this.isInvulnerable(source)) {
            return false;
        } else {
            if ((source instanceof EntityDamageSource || source == DamageSource.MAGIC) && this.friendsGoal != null) {
                this.friendsGoal.notifyHurt();
            }

            return super.damageEntity(source, amount);
        }
    }

    @Override
    public void tick() {
        this.yBodyRot = this.getYRot();
        super.tick();
    }

    @Override
    public void setYBodyRot(float bodyYaw) {
        this.setYRot(bodyYaw);
        super.setYBodyRot(bodyYaw);
    }

    @Override
    public float getWalkTargetValue(BlockPosition pos, IWorldReader world) {
        return BlockMonsterEggs.isCompatibleHostBlock(world.getType(pos.below())) ? 10.0F : super.getWalkTargetValue(pos, world);
    }

    public static boolean checkSilverfishSpawnRules(EntityTypes<EntitySilverfish> type, GeneratorAccess world, EnumMobSpawn spawnReason, BlockPosition pos, Random random) {
        if (checkAnyLightMonsterSpawnRules(type, world, spawnReason, pos, random)) {
            EntityHuman player = world.getNearestPlayer((double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D, 5.0D, true);
            return player == null;
        } else {
            return false;
        }
    }

    @Override
    public EnumMonsterType getMonsterType() {
        return EnumMonsterType.ARTHROPOD;
    }

    static class PathfinderGoalSilverfishHideInBlock extends PathfinderGoalRandomStroll {
        @Nullable
        private EnumDirection selectedDirection;
        private boolean doMerge;

        public PathfinderGoalSilverfishHideInBlock(EntitySilverfish silverfish) {
            super(silverfish, 1.0D, 10);
            this.setFlags(EnumSet.of(PathfinderGoal.Type.MOVE));
        }

        @Override
        public boolean canUse() {
            if (this.mob.getGoalTarget() != null) {
                return false;
            } else if (!this.mob.getNavigation().isDone()) {
                return false;
            } else {
                Random random = this.mob.getRandom();
                if (this.mob.level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) && random.nextInt(reducedTickDelay(10)) == 0) {
                    this.selectedDirection = EnumDirection.getRandom(random);
                    BlockPosition blockPos = (new BlockPosition(this.mob.locX(), this.mob.locY() + 0.5D, this.mob.locZ())).relative(this.selectedDirection);
                    IBlockData blockState = this.mob.level.getType(blockPos);
                    if (BlockMonsterEggs.isCompatibleHostBlock(blockState)) {
                        this.doMerge = true;
                        return true;
                    }
                }

                this.doMerge = false;
                return super.canUse();
            }
        }

        @Override
        public boolean canContinueToUse() {
            return this.doMerge ? false : super.canContinueToUse();
        }

        @Override
        public void start() {
            if (!this.doMerge) {
                super.start();
            } else {
                GeneratorAccess levelAccessor = this.mob.level;
                BlockPosition blockPos = (new BlockPosition(this.mob.locX(), this.mob.locY() + 0.5D, this.mob.locZ())).relative(this.selectedDirection);
                IBlockData blockState = levelAccessor.getType(blockPos);
                if (BlockMonsterEggs.isCompatibleHostBlock(blockState)) {
                    levelAccessor.setTypeAndData(blockPos, BlockMonsterEggs.infestedStateByHost(blockState), 3);
                    this.mob.doSpawnEffect();
                    this.mob.die();
                }

            }
        }
    }

    static class PathfinderGoalSilverfishWakeOthers extends PathfinderGoal {
        private final EntitySilverfish silverfish;
        private int lookForFriends;

        public PathfinderGoalSilverfishWakeOthers(EntitySilverfish silverfish) {
            this.silverfish = silverfish;
        }

        public void notifyHurt() {
            if (this.lookForFriends == 0) {
                this.lookForFriends = this.adjustedTickDelay(20);
            }

        }

        @Override
        public boolean canUse() {
            return this.lookForFriends > 0;
        }

        @Override
        public void tick() {
            --this.lookForFriends;
            if (this.lookForFriends <= 0) {
                World level = this.silverfish.level;
                Random random = this.silverfish.getRandom();
                BlockPosition blockPos = this.silverfish.getChunkCoordinates();

                for(int i = 0; i <= 5 && i >= -5; i = (i <= 0 ? 1 : 0) - i) {
                    for(int j = 0; j <= 10 && j >= -10; j = (j <= 0 ? 1 : 0) - j) {
                        for(int k = 0; k <= 10 && k >= -10; k = (k <= 0 ? 1 : 0) - k) {
                            BlockPosition blockPos2 = blockPos.offset(j, i, k);
                            IBlockData blockState = level.getType(blockPos2);
                            Block block = blockState.getBlock();
                            if (block instanceof BlockMonsterEggs) {
                                if (level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
                                    level.destroyBlock(blockPos2, true, this.silverfish);
                                } else {
                                    level.setTypeAndData(blockPos2, ((BlockMonsterEggs)block).hostStateByInfested(level.getType(blockPos2)), 3);
                                }

                                if (random.nextBoolean()) {
                                    return;
                                }
                            }
                        }
                    }
                }
            }

        }
    }
}
