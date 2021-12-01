package net.minecraft.world.entity.monster;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.util.MathHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.EnumMonsterType;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.goal.PathfinderGoalAvoidTarget;
import net.minecraft.world.entity.ai.goal.PathfinderGoalFloat;
import net.minecraft.world.entity.ai.goal.PathfinderGoalLookAtPlayer;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomStroll;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalHurtByTarget;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalNearestAttackableTarget;
import net.minecraft.world.entity.ai.targeting.PathfinderTargetCondition;
import net.minecraft.world.entity.animal.EntityIronGolem;
import net.minecraft.world.entity.animal.EntitySheep;
import net.minecraft.world.entity.npc.EntityVillagerAbstract;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.EntityEvokerFangs;
import net.minecraft.world.entity.raid.EntityRaider;
import net.minecraft.world.item.EnumColor;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.shapes.VoxelShape;

public class EntityEvoker extends EntityIllagerWizard {
    @Nullable
    private EntitySheep wololoTarget;

    public EntityEvoker(EntityTypes<? extends EntityEvoker> type, World world) {
        super(type, world);
        this.xpReward = 10;
    }

    @Override
    protected void initPathfinder() {
        super.initPathfinder();
        this.goalSelector.addGoal(0, new PathfinderGoalFloat(this));
        this.goalSelector.addGoal(1, new EntityEvoker.EvokerCastingSpellGoal());
        this.goalSelector.addGoal(2, new PathfinderGoalAvoidTarget<>(this, EntityHuman.class, 8.0F, 0.6D, 1.0D));
        this.goalSelector.addGoal(4, new EntityEvoker.EvokerSummonSpellGoal());
        this.goalSelector.addGoal(5, new EntityEvoker.EvokerAttackSpellGoal());
        this.goalSelector.addGoal(6, new EntityEvoker.EvokerWololoSpellGoal());
        this.goalSelector.addGoal(8, new PathfinderGoalRandomStroll(this, 0.6D));
        this.goalSelector.addGoal(9, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 3.0F, 1.0F));
        this.goalSelector.addGoal(10, new PathfinderGoalLookAtPlayer(this, EntityInsentient.class, 8.0F));
        this.targetSelector.addGoal(1, (new PathfinderGoalHurtByTarget(this, EntityRaider.class)).setAlertOthers());
        this.targetSelector.addGoal(2, (new PathfinderGoalNearestAttackableTarget<>(this, EntityHuman.class, true)).setUnseenMemoryTicks(300));
        this.targetSelector.addGoal(3, (new PathfinderGoalNearestAttackableTarget<>(this, EntityVillagerAbstract.class, false)).setUnseenMemoryTicks(300));
        this.targetSelector.addGoal(3, new PathfinderGoalNearestAttackableTarget<>(this, EntityIronGolem.class, false));
    }

    public static AttributeProvider.Builder createAttributes() {
        return EntityMonster.createMonsterAttributes().add(GenericAttributes.MOVEMENT_SPEED, 0.5D).add(GenericAttributes.FOLLOW_RANGE, 12.0D).add(GenericAttributes.MAX_HEALTH, 24.0D);
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
    }

    @Override
    public SoundEffect getCelebrateSound() {
        return SoundEffects.EVOKER_CELEBRATE;
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
    }

    @Override
    protected void mobTick() {
        super.mobTick();
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        if (other == null) {
            return false;
        } else if (other == this) {
            return true;
        } else if (super.isAlliedTo(other)) {
            return true;
        } else if (other instanceof EntityVex) {
            return this.isAlliedTo(((EntityVex)other).getOwner());
        } else if (other instanceof EntityLiving && ((EntityLiving)other).getMonsterType() == EnumMonsterType.ILLAGER) {
            return this.getScoreboardTeam() == null && other.getScoreboardTeam() == null;
        } else {
            return false;
        }
    }

    @Override
    protected SoundEffect getSoundAmbient() {
        return SoundEffects.EVOKER_AMBIENT;
    }

    @Override
    public SoundEffect getSoundDeath() {
        return SoundEffects.EVOKER_DEATH;
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return SoundEffects.EVOKER_HURT;
    }

    public void setWololoTarget(@Nullable EntitySheep sheep) {
        this.wololoTarget = sheep;
    }

    @Nullable
    public EntitySheep getWololoTarget() {
        return this.wololoTarget;
    }

    @Override
    protected SoundEffect getSoundCastSpell() {
        return SoundEffects.EVOKER_CAST_SPELL;
    }

    @Override
    public void applyRaidBuffs(int wave, boolean unused) {
    }

    class EvokerAttackSpellGoal extends EntityIllagerWizard.PathfinderGoalCastSpell {
        @Override
        protected int getCastingTime() {
            return 40;
        }

        @Override
        protected int getCastingInterval() {
            return 100;
        }

        @Override
        protected void performSpellCasting() {
            EntityLiving livingEntity = EntityEvoker.this.getGoalTarget();
            double d = Math.min(livingEntity.locY(), EntityEvoker.this.locY());
            double e = Math.max(livingEntity.locY(), EntityEvoker.this.locY()) + 1.0D;
            float f = (float)MathHelper.atan2(livingEntity.locZ() - EntityEvoker.this.locZ(), livingEntity.locX() - EntityEvoker.this.locX());
            if (EntityEvoker.this.distanceToSqr(livingEntity) < 9.0D) {
                for(int i = 0; i < 5; ++i) {
                    float g = f + (float)i * (float)Math.PI * 0.4F;
                    this.createSpellEntity(EntityEvoker.this.locX() + (double)MathHelper.cos(g) * 1.5D, EntityEvoker.this.locZ() + (double)MathHelper.sin(g) * 1.5D, d, e, g, 0);
                }

                for(int j = 0; j < 8; ++j) {
                    float h = f + (float)j * (float)Math.PI * 2.0F / 8.0F + 1.2566371F;
                    this.createSpellEntity(EntityEvoker.this.locX() + (double)MathHelper.cos(h) * 2.5D, EntityEvoker.this.locZ() + (double)MathHelper.sin(h) * 2.5D, d, e, h, 3);
                }
            } else {
                for(int k = 0; k < 16; ++k) {
                    double l = 1.25D * (double)(k + 1);
                    int m = 1 * k;
                    this.createSpellEntity(EntityEvoker.this.locX() + (double)MathHelper.cos(f) * l, EntityEvoker.this.locZ() + (double)MathHelper.sin(f) * l, d, e, f, m);
                }
            }

        }

        private void createSpellEntity(double x, double z, double maxY, double y, float yaw, int warmup) {
            BlockPosition blockPos = new BlockPosition(x, y, z);
            boolean bl = false;
            double d = 0.0D;

            do {
                BlockPosition blockPos2 = blockPos.below();
                IBlockData blockState = EntityEvoker.this.level.getType(blockPos2);
                if (blockState.isFaceSturdy(EntityEvoker.this.level, blockPos2, EnumDirection.UP)) {
                    if (!EntityEvoker.this.level.isEmpty(blockPos)) {
                        IBlockData blockState2 = EntityEvoker.this.level.getType(blockPos);
                        VoxelShape voxelShape = blockState2.getCollisionShape(EntityEvoker.this.level, blockPos);
                        if (!voxelShape.isEmpty()) {
                            d = voxelShape.max(EnumDirection.EnumAxis.Y);
                        }
                    }

                    bl = true;
                    break;
                }

                blockPos = blockPos.below();
            } while(blockPos.getY() >= MathHelper.floor(maxY) - 1);

            if (bl) {
                EntityEvoker.this.level.addEntity(new EntityEvokerFangs(EntityEvoker.this.level, x, (double)blockPos.getY() + d, z, yaw, warmup, EntityEvoker.this));
            }

        }

        @Override
        protected SoundEffect getSpellPrepareSound() {
            return SoundEffects.EVOKER_PREPARE_ATTACK;
        }

        @Override
        protected EntityIllagerWizard.Spell getCastSpell() {
            return EntityIllagerWizard.Spell.FANGS;
        }
    }

    class EvokerCastingSpellGoal extends EntityIllagerWizard.SpellcasterCastingSpellGoal {
        @Override
        public void tick() {
            if (EntityEvoker.this.getGoalTarget() != null) {
                EntityEvoker.this.getControllerLook().setLookAt(EntityEvoker.this.getGoalTarget(), (float)EntityEvoker.this.getMaxHeadYRot(), (float)EntityEvoker.this.getMaxHeadXRot());
            } else if (EntityEvoker.this.getWololoTarget() != null) {
                EntityEvoker.this.getControllerLook().setLookAt(EntityEvoker.this.getWololoTarget(), (float)EntityEvoker.this.getMaxHeadYRot(), (float)EntityEvoker.this.getMaxHeadXRot());
            }

        }
    }

    class EvokerSummonSpellGoal extends EntityIllagerWizard.PathfinderGoalCastSpell {
        private final PathfinderTargetCondition vexCountTargeting = PathfinderTargetCondition.forNonCombat().range(16.0D).ignoreLineOfSight().ignoreInvisibilityTesting();

        @Override
        public boolean canUse() {
            if (!super.canUse()) {
                return false;
            } else {
                int i = EntityEvoker.this.level.getNearbyEntities(EntityVex.class, this.vexCountTargeting, EntityEvoker.this, EntityEvoker.this.getBoundingBox().inflate(16.0D)).size();
                return EntityEvoker.this.random.nextInt(8) + 1 > i;
            }
        }

        @Override
        protected int getCastingTime() {
            return 100;
        }

        @Override
        protected int getCastingInterval() {
            return 340;
        }

        @Override
        protected void performSpellCasting() {
            WorldServer serverLevel = (WorldServer)EntityEvoker.this.level;

            for(int i = 0; i < 3; ++i) {
                BlockPosition blockPos = EntityEvoker.this.getChunkCoordinates().offset(-2 + EntityEvoker.this.random.nextInt(5), 1, -2 + EntityEvoker.this.random.nextInt(5));
                EntityVex vex = EntityTypes.VEX.create(EntityEvoker.this.level);
                vex.setPositionRotation(blockPos, 0.0F, 0.0F);
                vex.prepare(serverLevel, EntityEvoker.this.level.getDamageScaler(blockPos), EnumMobSpawn.MOB_SUMMONED, (GroupDataEntity)null, (NBTTagCompound)null);
                vex.setOwner(EntityEvoker.this);
                vex.setBoundOrigin(blockPos);
                vex.setLimitedLife(20 * (30 + EntityEvoker.this.random.nextInt(90)));
                serverLevel.addAllEntities(vex);
            }

        }

        @Override
        protected SoundEffect getSpellPrepareSound() {
            return SoundEffects.EVOKER_PREPARE_SUMMON;
        }

        @Override
        protected EntityIllagerWizard.Spell getCastSpell() {
            return EntityIllagerWizard.Spell.SUMMON_VEX;
        }
    }

    public class EvokerWololoSpellGoal extends EntityIllagerWizard.PathfinderGoalCastSpell {
        private final PathfinderTargetCondition wololoTargeting = PathfinderTargetCondition.forNonCombat().range(16.0D).selector((livingEntity) -> {
            return ((EntitySheep)livingEntity).getColor() == EnumColor.BLUE;
        });

        @Override
        public boolean canUse() {
            if (EntityEvoker.this.getGoalTarget() != null) {
                return false;
            } else if (EntityEvoker.this.isCastingSpell()) {
                return false;
            } else if (EntityEvoker.this.tickCount < this.nextAttackTickCount) {
                return false;
            } else if (!EntityEvoker.this.level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
                return false;
            } else {
                List<EntitySheep> list = EntityEvoker.this.level.getNearbyEntities(EntitySheep.class, this.wololoTargeting, EntityEvoker.this, EntityEvoker.this.getBoundingBox().grow(16.0D, 4.0D, 16.0D));
                if (list.isEmpty()) {
                    return false;
                } else {
                    EntityEvoker.this.setWololoTarget(list.get(EntityEvoker.this.random.nextInt(list.size())));
                    return true;
                }
            }
        }

        @Override
        public boolean canContinueToUse() {
            return EntityEvoker.this.getWololoTarget() != null && this.attackWarmupDelay > 0;
        }

        @Override
        public void stop() {
            super.stop();
            EntityEvoker.this.setWololoTarget((EntitySheep)null);
        }

        @Override
        protected void performSpellCasting() {
            EntitySheep sheep = EntityEvoker.this.getWololoTarget();
            if (sheep != null && sheep.isAlive()) {
                sheep.setColor(EnumColor.RED);
            }

        }

        @Override
        protected int getCastWarmupTime() {
            return 40;
        }

        @Override
        protected int getCastingTime() {
            return 60;
        }

        @Override
        protected int getCastingInterval() {
            return 140;
        }

        @Override
        protected SoundEffect getSpellPrepareSound() {
            return SoundEffects.EVOKER_PREPARE_WOLOLO;
        }

        @Override
        protected EntityIllagerWizard.Spell getCastSpell() {
            return EntityIllagerWizard.Spell.WOLOLO;
        }
    }
}
