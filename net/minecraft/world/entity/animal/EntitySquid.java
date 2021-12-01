package net.minecraft.world.entity.animal;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.particles.ParticleParam;
import net.minecraft.core.particles.Particles;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.TagsFluid;
import net.minecraft.util.MathHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.EntitySize;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMoveType;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.goal.PathfinderGoal;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3D;

public class EntitySquid extends EntityWaterAnimal {
    public float xBodyRot;
    public float xBodyRotO;
    public float zBodyRot;
    public float zBodyRotO;
    public float tentacleMovement;
    public float oldTentacleMovement;
    public float tentacleAngle;
    public float oldTentacleAngle;
    private float speed;
    private float tentacleSpeed;
    private float rotateSpeed;
    private float tx;
    private float ty;
    private float tz;

    public EntitySquid(EntityTypes<? extends EntitySquid> type, World world) {
        super(type, world);
        this.random.setSeed((long)this.getId());
        this.tentacleSpeed = 1.0F / (this.random.nextFloat() + 1.0F) * 0.2F;
    }

    @Override
    protected void initPathfinder() {
        this.goalSelector.addGoal(0, new EntitySquid.PathfinderGoalSquid(this));
        this.goalSelector.addGoal(1, new EntitySquid.SquidFleeGoal());
    }

    public static AttributeProvider.Builder createAttributes() {
        return EntityInsentient.createMobAttributes().add(GenericAttributes.MAX_HEALTH, 10.0D);
    }

    @Override
    protected float getStandingEyeHeight(EntityPose pose, EntitySize dimensions) {
        return dimensions.height * 0.5F;
    }

    @Override
    protected SoundEffect getSoundAmbient() {
        return SoundEffects.SQUID_AMBIENT;
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return SoundEffects.SQUID_HURT;
    }

    @Override
    public SoundEffect getSoundDeath() {
        return SoundEffects.SQUID_DEATH;
    }

    protected SoundEffect getSquirtSound() {
        return SoundEffects.SQUID_SQUIRT;
    }

    @Override
    public boolean canBeLeashed(EntityHuman player) {
        return !this.isLeashed();
    }

    @Override
    public float getSoundVolume() {
        return 0.4F;
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.EVENTS;
    }

    @Override
    public void movementTick() {
        super.movementTick();
        this.xBodyRotO = this.xBodyRot;
        this.zBodyRotO = this.zBodyRot;
        this.oldTentacleMovement = this.tentacleMovement;
        this.oldTentacleAngle = this.tentacleAngle;
        this.tentacleMovement += this.tentacleSpeed;
        if ((double)this.tentacleMovement > (Math.PI * 2D)) {
            if (this.level.isClientSide) {
                this.tentacleMovement = ((float)Math.PI * 2F);
            } else {
                this.tentacleMovement = (float)((double)this.tentacleMovement - (Math.PI * 2D));
                if (this.random.nextInt(10) == 0) {
                    this.tentacleSpeed = 1.0F / (this.random.nextFloat() + 1.0F) * 0.2F;
                }

                this.level.broadcastEntityEffect(this, (byte)19);
            }
        }

        if (this.isInWaterOrBubble()) {
            if (this.tentacleMovement < (float)Math.PI) {
                float f = this.tentacleMovement / (float)Math.PI;
                this.tentacleAngle = MathHelper.sin(f * f * (float)Math.PI) * (float)Math.PI * 0.25F;
                if ((double)f > 0.75D) {
                    this.speed = 1.0F;
                    this.rotateSpeed = 1.0F;
                } else {
                    this.rotateSpeed *= 0.8F;
                }
            } else {
                this.tentacleAngle = 0.0F;
                this.speed *= 0.9F;
                this.rotateSpeed *= 0.99F;
            }

            if (!this.level.isClientSide) {
                this.setMot((double)(this.tx * this.speed), (double)(this.ty * this.speed), (double)(this.tz * this.speed));
            }

            Vec3D vec3 = this.getMot();
            double d = vec3.horizontalDistance();
            this.yBodyRot += (-((float)MathHelper.atan2(vec3.x, vec3.z)) * (180F / (float)Math.PI) - this.yBodyRot) * 0.1F;
            this.setYRot(this.yBodyRot);
            this.zBodyRot = (float)((double)this.zBodyRot + Math.PI * (double)this.rotateSpeed * 1.5D);
            this.xBodyRot += (-((float)MathHelper.atan2(d, vec3.y)) * (180F / (float)Math.PI) - this.xBodyRot) * 0.1F;
        } else {
            this.tentacleAngle = MathHelper.abs(MathHelper.sin(this.tentacleMovement)) * (float)Math.PI * 0.25F;
            if (!this.level.isClientSide) {
                double e = this.getMot().y;
                if (this.hasEffect(MobEffectList.LEVITATION)) {
                    e = 0.05D * (double)(this.getEffect(MobEffectList.LEVITATION).getAmplifier() + 1);
                } else if (!this.isNoGravity()) {
                    e -= 0.08D;
                }

                this.setMot(0.0D, e * (double)0.98F, 0.0D);
            }

            this.xBodyRot = (float)((double)this.xBodyRot + (double)(-90.0F - this.xBodyRot) * 0.02D);
        }

    }

    @Override
    public boolean damageEntity(DamageSource source, float amount) {
        if (super.damageEntity(source, amount) && this.getLastDamager() != null) {
            this.spawnInk();
            return true;
        } else {
            return false;
        }
    }

    private Vec3D rotateVector(Vec3D shootVector) {
        Vec3D vec3 = shootVector.xRot(this.xBodyRotO * ((float)Math.PI / 180F));
        return vec3.yRot(-this.yBodyRotO * ((float)Math.PI / 180F));
    }

    private void spawnInk() {
        this.playSound(this.getSquirtSound(), this.getSoundVolume(), this.getVoicePitch());
        Vec3D vec3 = this.rotateVector(new Vec3D(0.0D, -1.0D, 0.0D)).add(this.locX(), this.locY(), this.locZ());

        for(int i = 0; i < 30; ++i) {
            Vec3D vec32 = this.rotateVector(new Vec3D((double)this.random.nextFloat() * 0.6D - 0.3D, -1.0D, (double)this.random.nextFloat() * 0.6D - 0.3D));
            Vec3D vec33 = vec32.scale(0.3D + (double)(this.random.nextFloat() * 2.0F));
            ((WorldServer)this.level).sendParticles(this.getInkParticle(), vec3.x, vec3.y + 0.5D, vec3.z, 0, vec33.x, vec33.y, vec33.z, (double)0.1F);
        }

    }

    protected ParticleParam getInkParticle() {
        return Particles.SQUID_INK;
    }

    @Override
    public void travel(Vec3D movementInput) {
        this.move(EnumMoveType.SELF, this.getMot());
    }

    @Override
    public void handleEntityEvent(byte status) {
        if (status == 19) {
            this.tentacleMovement = 0.0F;
        } else {
            super.handleEntityEvent(status);
        }

    }

    public void setMovementVector(float x, float y, float z) {
        this.tx = x;
        this.ty = y;
        this.tz = z;
    }

    public boolean hasMovementVector() {
        return this.tx != 0.0F || this.ty != 0.0F || this.tz != 0.0F;
    }

    class PathfinderGoalSquid extends PathfinderGoal {
        private final EntitySquid squid;

        public PathfinderGoalSquid(EntitySquid squid) {
            this.squid = squid;
        }

        @Override
        public boolean canUse() {
            return true;
        }

        @Override
        public void tick() {
            int i = this.squid.getNoActionTime();
            if (i > 100) {
                this.squid.setMovementVector(0.0F, 0.0F, 0.0F);
            } else if (this.squid.getRandom().nextInt(reducedTickDelay(50)) == 0 || !this.squid.wasTouchingWater || !this.squid.hasMovementVector()) {
                float f = this.squid.getRandom().nextFloat() * ((float)Math.PI * 2F);
                float g = MathHelper.cos(f) * 0.2F;
                float h = -0.1F + this.squid.getRandom().nextFloat() * 0.2F;
                float j = MathHelper.sin(f) * 0.2F;
                this.squid.setMovementVector(g, h, j);
            }

        }
    }

    class SquidFleeGoal extends PathfinderGoal {
        private static final float SQUID_FLEE_SPEED = 3.0F;
        private static final float SQUID_FLEE_MIN_DISTANCE = 5.0F;
        private static final float SQUID_FLEE_MAX_DISTANCE = 10.0F;
        private int fleeTicks;

        @Override
        public boolean canUse() {
            EntityLiving livingEntity = EntitySquid.this.getLastDamager();
            if (EntitySquid.this.isInWater() && livingEntity != null) {
                return EntitySquid.this.distanceToSqr(livingEntity) < 100.0D;
            } else {
                return false;
            }
        }

        @Override
        public void start() {
            this.fleeTicks = 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            ++this.fleeTicks;
            EntityLiving livingEntity = EntitySquid.this.getLastDamager();
            if (livingEntity != null) {
                Vec3D vec3 = new Vec3D(EntitySquid.this.locX() - livingEntity.locX(), EntitySquid.this.locY() - livingEntity.locY(), EntitySquid.this.locZ() - livingEntity.locZ());
                IBlockData blockState = EntitySquid.this.level.getType(new BlockPosition(EntitySquid.this.locX() + vec3.x, EntitySquid.this.locY() + vec3.y, EntitySquid.this.locZ() + vec3.z));
                Fluid fluidState = EntitySquid.this.level.getFluid(new BlockPosition(EntitySquid.this.locX() + vec3.x, EntitySquid.this.locY() + vec3.y, EntitySquid.this.locZ() + vec3.z));
                if (fluidState.is(TagsFluid.WATER) || blockState.isAir()) {
                    double d = vec3.length();
                    if (d > 0.0D) {
                        vec3.normalize();
                        float f = 3.0F;
                        if (d > 5.0D) {
                            f = (float)((double)f - (d - 5.0D) / 5.0D);
                        }

                        if (f > 0.0F) {
                            vec3 = vec3.scale((double)f);
                        }
                    }

                    if (blockState.isAir()) {
                        vec3 = vec3.subtract(0.0D, vec3.y, 0.0D);
                    }

                    EntitySquid.this.setMovementVector((float)vec3.x / 20.0F, (float)vec3.y / 20.0F, (float)vec3.z / 20.0F);
                }

                if (this.fleeTicks % 10 == 5) {
                    EntitySquid.this.level.addParticle(Particles.BUBBLE, EntitySquid.this.locX(), EntitySquid.this.locY(), EntitySquid.this.locZ(), 0.0D, 0.0D, 0.0D);
                }

            }
        }
    }
}
