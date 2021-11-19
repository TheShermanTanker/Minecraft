package net.minecraft.world.entity.boss.enderdragon;

import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.particles.Particles;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntityLiving;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.TagsBlock;
import net.minecraft.util.MathHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.EntityDamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityExperienceOrb;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMoveType;
import net.minecraft.world.entity.IEntitySelector;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.targeting.PathfinderTargetCondition;
import net.minecraft.world.entity.boss.EntityComplexPart;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonControllerManager;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonControllerPhase;
import net.minecraft.world.entity.boss.enderdragon.phases.IDragonController;
import net.minecraft.world.entity.monster.IMonster;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.dimension.end.EnderDragonBattle;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.levelgen.feature.WorldGenEndTrophy;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathEntity;
import net.minecraft.world.level.pathfinder.PathPoint;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.Vec3D;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EntityEnderDragon extends EntityInsentient implements IMonster {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final DataWatcherObject<Integer> DATA_PHASE = DataWatcher.defineId(EntityEnderDragon.class, DataWatcherRegistry.INT);
    private static final PathfinderTargetCondition CRYSTAL_DESTROY_TARGETING = PathfinderTargetCondition.forCombat().range(64.0D);
    private static final int GROWL_INTERVAL_MIN = 200;
    private static final int GROWL_INTERVAL_MAX = 400;
    private static final float SITTING_ALLOWED_DAMAGE_PERCENTAGE = 0.25F;
    private static final String DRAGON_DEATH_TIME_KEY = "DragonDeathTime";
    private static final String DRAGON_PHASE_KEY = "DragonPhase";
    public final double[][] positions = new double[64][3];
    public int posPointer = -1;
    public final EntityComplexPart[] subEntities;
    public final EntityComplexPart head;
    private final EntityComplexPart neck;
    private final EntityComplexPart body;
    private final EntityComplexPart tail1;
    private final EntityComplexPart tail2;
    private final EntityComplexPart tail3;
    private final EntityComplexPart wing1;
    private final EntityComplexPart wing2;
    public float oFlapTime;
    public float flapTime;
    public boolean inWall;
    public int dragonDeathTime;
    public float yRotA;
    @Nullable
    public EntityEnderCrystal nearestCrystal;
    @Nullable
    private final EnderDragonBattle dragonFight;
    private final DragonControllerManager phaseManager;
    private int growlTime = 100;
    private int sittingDamageReceived;
    private final PathPoint[] nodes = new PathPoint[24];
    private final int[] nodeAdjacency = new int[24];
    private final Path openSet = new Path();

    public EntityEnderDragon(EntityTypes<? extends EntityEnderDragon> entityType, World world) {
        super(EntityTypes.ENDER_DRAGON, world);
        this.head = new EntityComplexPart(this, "head", 1.0F, 1.0F);
        this.neck = new EntityComplexPart(this, "neck", 3.0F, 3.0F);
        this.body = new EntityComplexPart(this, "body", 5.0F, 3.0F);
        this.tail1 = new EntityComplexPart(this, "tail", 2.0F, 2.0F);
        this.tail2 = new EntityComplexPart(this, "tail", 2.0F, 2.0F);
        this.tail3 = new EntityComplexPart(this, "tail", 2.0F, 2.0F);
        this.wing1 = new EntityComplexPart(this, "wing", 4.0F, 2.0F);
        this.wing2 = new EntityComplexPart(this, "wing", 4.0F, 2.0F);
        this.subEntities = new EntityComplexPart[]{this.head, this.neck, this.body, this.tail1, this.tail2, this.tail3, this.wing1, this.wing2};
        this.setHealth(this.getMaxHealth());
        this.noPhysics = true;
        this.noCulling = true;
        if (world instanceof WorldServer) {
            this.dragonFight = ((WorldServer)world).getDragonBattle();
        } else {
            this.dragonFight = null;
        }

        this.phaseManager = new DragonControllerManager(this);
    }

    public static AttributeProvider.Builder createAttributes() {
        return EntityInsentient.createMobAttributes().add(GenericAttributes.MAX_HEALTH, 200.0D);
    }

    @Override
    public boolean isFlapping() {
        float f = MathHelper.cos(this.flapTime * ((float)Math.PI * 2F));
        float g = MathHelper.cos(this.oFlapTime * ((float)Math.PI * 2F));
        return g <= -0.3F && f >= -0.3F;
    }

    @Override
    public void onFlap() {
        if (this.level.isClientSide && !this.isSilent()) {
            this.level.playLocalSound(this.locX(), this.locY(), this.locZ(), SoundEffects.ENDER_DRAGON_FLAP, this.getSoundCategory(), 5.0F, 0.8F + this.random.nextFloat() * 0.3F, false);
        }

    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.getDataWatcher().register(DATA_PHASE, DragonControllerPhase.HOVERING.getId());
    }

    public double[] getLatencyPos(int segmentNumber, float tickDelta) {
        if (this.isDeadOrDying()) {
            tickDelta = 0.0F;
        }

        tickDelta = 1.0F - tickDelta;
        int i = this.posPointer - segmentNumber & 63;
        int j = this.posPointer - segmentNumber - 1 & 63;
        double[] ds = new double[3];
        double d = this.positions[i][0];
        double e = MathHelper.wrapDegrees(this.positions[j][0] - d);
        ds[0] = d + e * (double)tickDelta;
        d = this.positions[i][1];
        e = this.positions[j][1] - d;
        ds[1] = d + e * (double)tickDelta;
        ds[2] = MathHelper.lerp((double)tickDelta, this.positions[i][2], this.positions[j][2]);
        return ds;
    }

    @Override
    public void movementTick() {
        this.processFlappingMovement();
        if (this.level.isClientSide) {
            this.setHealth(this.getHealth());
            if (!this.isSilent() && !this.phaseManager.getCurrentPhase().isSitting() && --this.growlTime < 0) {
                this.level.playLocalSound(this.locX(), this.locY(), this.locZ(), SoundEffects.ENDER_DRAGON_GROWL, this.getSoundCategory(), 2.5F, 0.8F + this.random.nextFloat() * 0.3F, false);
                this.growlTime = 200 + this.random.nextInt(200);
            }
        }

        this.oFlapTime = this.flapTime;
        if (this.isDeadOrDying()) {
            float f = (this.random.nextFloat() - 0.5F) * 8.0F;
            float g = (this.random.nextFloat() - 0.5F) * 4.0F;
            float h = (this.random.nextFloat() - 0.5F) * 8.0F;
            this.level.addParticle(Particles.EXPLOSION, this.locX() + (double)f, this.locY() + 2.0D + (double)g, this.locZ() + (double)h, 0.0D, 0.0D, 0.0D);
        } else {
            this.checkCrystals();
            Vec3D vec3 = this.getMot();
            float i = 0.2F / ((float)vec3.horizontalDistance() * 10.0F + 1.0F);
            i = i * (float)Math.pow(2.0D, vec3.y);
            if (this.phaseManager.getCurrentPhase().isSitting()) {
                this.flapTime += 0.1F;
            } else if (this.inWall) {
                this.flapTime += i * 0.5F;
            } else {
                this.flapTime += i;
            }

            this.setYRot(MathHelper.wrapDegrees(this.getYRot()));
            if (this.isNoAI()) {
                this.flapTime = 0.5F;
            } else {
                if (this.posPointer < 0) {
                    for(int j = 0; j < this.positions.length; ++j) {
                        this.positions[j][0] = (double)this.getYRot();
                        this.positions[j][1] = this.locY();
                    }
                }

                if (++this.posPointer == this.positions.length) {
                    this.posPointer = 0;
                }

                this.positions[this.posPointer][0] = (double)this.getYRot();
                this.positions[this.posPointer][1] = this.locY();
                if (this.level.isClientSide) {
                    if (this.lerpSteps > 0) {
                        double d = this.locX() + (this.lerpX - this.locX()) / (double)this.lerpSteps;
                        double e = this.locY() + (this.lerpY - this.locY()) / (double)this.lerpSteps;
                        double k = this.locZ() + (this.lerpZ - this.locZ()) / (double)this.lerpSteps;
                        double l = MathHelper.wrapDegrees(this.lerpYRot - (double)this.getYRot());
                        this.setYRot(this.getYRot() + (float)l / (float)this.lerpSteps);
                        this.setXRot(this.getXRot() + (float)(this.lerpXRot - (double)this.getXRot()) / (float)this.lerpSteps);
                        --this.lerpSteps;
                        this.setPosition(d, e, k);
                        this.setYawPitch(this.getYRot(), this.getXRot());
                    }

                    this.phaseManager.getCurrentPhase().doClientTick();
                } else {
                    IDragonController dragonPhaseInstance = this.phaseManager.getCurrentPhase();
                    dragonPhaseInstance.doServerTick();
                    if (this.phaseManager.getCurrentPhase() != dragonPhaseInstance) {
                        dragonPhaseInstance = this.phaseManager.getCurrentPhase();
                        dragonPhaseInstance.doServerTick();
                    }

                    Vec3D vec32 = dragonPhaseInstance.getFlyTargetLocation();
                    if (vec32 != null) {
                        double m = vec32.x - this.locX();
                        double n = vec32.y - this.locY();
                        double o = vec32.z - this.locZ();
                        double p = m * m + n * n + o * o;
                        float q = dragonPhaseInstance.getFlySpeed();
                        double r = Math.sqrt(m * m + o * o);
                        if (r > 0.0D) {
                            n = MathHelper.clamp(n / r, (double)(-q), (double)q);
                        }

                        this.setMot(this.getMot().add(0.0D, n * 0.01D, 0.0D));
                        this.setYRot(MathHelper.wrapDegrees(this.getYRot()));
                        Vec3D vec33 = vec32.subtract(this.locX(), this.locY(), this.locZ()).normalize();
                        Vec3D vec34 = (new Vec3D((double)MathHelper.sin(this.getYRot() * ((float)Math.PI / 180F)), this.getMot().y, (double)(-MathHelper.cos(this.getYRot() * ((float)Math.PI / 180F))))).normalize();
                        float s = Math.max(((float)vec34.dot(vec33) + 0.5F) / 1.5F, 0.0F);
                        if (Math.abs(m) > (double)1.0E-5F || Math.abs(o) > (double)1.0E-5F) {
                            double t = MathHelper.clamp(MathHelper.wrapDegrees(180.0D - MathHelper.atan2(m, o) * (double)(180F / (float)Math.PI) - (double)this.getYRot()), -50.0D, 50.0D);
                            this.yRotA *= 0.8F;
                            this.yRotA = (float)((double)this.yRotA + t * (double)dragonPhaseInstance.getTurnSpeed());
                            this.setYRot(this.getYRot() + this.yRotA * 0.1F);
                        }

                        float u = (float)(2.0D / (p + 1.0D));
                        float v = 0.06F;
                        this.moveRelative(0.06F * (s * u + (1.0F - u)), new Vec3D(0.0D, 0.0D, -1.0D));
                        if (this.inWall) {
                            this.move(EnumMoveType.SELF, this.getMot().scale((double)0.8F));
                        } else {
                            this.move(EnumMoveType.SELF, this.getMot());
                        }

                        Vec3D vec35 = this.getMot().normalize();
                        double w = 0.8D + 0.15D * (vec35.dot(vec34) + 1.0D) / 2.0D;
                        this.setMot(this.getMot().multiply(w, (double)0.91F, w));
                    }
                }

                this.yBodyRot = this.getYRot();
                Vec3D[] vec3s = new Vec3D[this.subEntities.length];

                for(int x = 0; x < this.subEntities.length; ++x) {
                    vec3s[x] = new Vec3D(this.subEntities[x].locX(), this.subEntities[x].locY(), this.subEntities[x].locZ());
                }

                float y = (float)(this.getLatencyPos(5, 1.0F)[1] - this.getLatencyPos(10, 1.0F)[1]) * 10.0F * ((float)Math.PI / 180F);
                float z = MathHelper.cos(y);
                float aa = MathHelper.sin(y);
                float ab = this.getYRot() * ((float)Math.PI / 180F);
                float ac = MathHelper.sin(ab);
                float ad = MathHelper.cos(ab);
                this.tickPart(this.body, (double)(ac * 0.5F), 0.0D, (double)(-ad * 0.5F));
                this.tickPart(this.wing1, (double)(ad * 4.5F), 2.0D, (double)(ac * 4.5F));
                this.tickPart(this.wing2, (double)(ad * -4.5F), 2.0D, (double)(ac * -4.5F));
                if (!this.level.isClientSide && this.hurtTime == 0) {
                    this.knockBack(this.level.getEntities(this, this.wing1.getBoundingBox().grow(4.0D, 2.0D, 4.0D).move(0.0D, -2.0D, 0.0D), IEntitySelector.NO_CREATIVE_OR_SPECTATOR));
                    this.knockBack(this.level.getEntities(this, this.wing2.getBoundingBox().grow(4.0D, 2.0D, 4.0D).move(0.0D, -2.0D, 0.0D), IEntitySelector.NO_CREATIVE_OR_SPECTATOR));
                    this.hurt(this.level.getEntities(this, this.head.getBoundingBox().inflate(1.0D), IEntitySelector.NO_CREATIVE_OR_SPECTATOR));
                    this.hurt(this.level.getEntities(this, this.neck.getBoundingBox().inflate(1.0D), IEntitySelector.NO_CREATIVE_OR_SPECTATOR));
                }

                float ae = MathHelper.sin(this.getYRot() * ((float)Math.PI / 180F) - this.yRotA * 0.01F);
                float af = MathHelper.cos(this.getYRot() * ((float)Math.PI / 180F) - this.yRotA * 0.01F);
                float ag = this.getHeadYOffset();
                this.tickPart(this.head, (double)(ae * 6.5F * z), (double)(ag + aa * 6.5F), (double)(-af * 6.5F * z));
                this.tickPart(this.neck, (double)(ae * 5.5F * z), (double)(ag + aa * 5.5F), (double)(-af * 5.5F * z));
                double[] ds = this.getLatencyPos(5, 1.0F);

                for(int ah = 0; ah < 3; ++ah) {
                    EntityComplexPart enderDragonPart = null;
                    if (ah == 0) {
                        enderDragonPart = this.tail1;
                    }

                    if (ah == 1) {
                        enderDragonPart = this.tail2;
                    }

                    if (ah == 2) {
                        enderDragonPart = this.tail3;
                    }

                    double[] es = this.getLatencyPos(12 + ah * 2, 1.0F);
                    float ai = this.getYRot() * ((float)Math.PI / 180F) + this.rotWrap(es[0] - ds[0]) * ((float)Math.PI / 180F);
                    float aj = MathHelper.sin(ai);
                    float ak = MathHelper.cos(ai);
                    float al = 1.5F;
                    float am = (float)(ah + 1) * 2.0F;
                    this.tickPart(enderDragonPart, (double)(-(ac * 1.5F + aj * am) * z), es[1] - ds[1] - (double)((am + 1.5F) * aa) + 1.5D, (double)((ad * 1.5F + ak * am) * z));
                }

                if (!this.level.isClientSide) {
                    this.inWall = this.checkWalls(this.head.getBoundingBox()) | this.checkWalls(this.neck.getBoundingBox()) | this.checkWalls(this.body.getBoundingBox());
                    if (this.dragonFight != null) {
                        this.dragonFight.updateDragon(this);
                    }
                }

                for(int an = 0; an < this.subEntities.length; ++an) {
                    this.subEntities[an].xo = vec3s[an].x;
                    this.subEntities[an].yo = vec3s[an].y;
                    this.subEntities[an].zo = vec3s[an].z;
                    this.subEntities[an].xOld = vec3s[an].x;
                    this.subEntities[an].yOld = vec3s[an].y;
                    this.subEntities[an].zOld = vec3s[an].z;
                }

            }
        }
    }

    private void tickPart(EntityComplexPart enderDragonPart, double dx, double dy, double dz) {
        enderDragonPart.setPosition(this.locX() + dx, this.locY() + dy, this.locZ() + dz);
    }

    private float getHeadYOffset() {
        if (this.phaseManager.getCurrentPhase().isSitting()) {
            return -1.0F;
        } else {
            double[] ds = this.getLatencyPos(5, 1.0F);
            double[] es = this.getLatencyPos(0, 1.0F);
            return (float)(ds[1] - es[1]);
        }
    }

    private void checkCrystals() {
        if (this.nearestCrystal != null) {
            if (this.nearestCrystal.isRemoved()) {
                this.nearestCrystal = null;
            } else if (this.tickCount % 10 == 0 && this.getHealth() < this.getMaxHealth()) {
                this.setHealth(this.getHealth() + 1.0F);
            }
        }

        if (this.random.nextInt(10) == 0) {
            List<EntityEnderCrystal> list = this.level.getEntitiesOfClass(EntityEnderCrystal.class, this.getBoundingBox().inflate(32.0D));
            EntityEnderCrystal endCrystal = null;
            double d = Double.MAX_VALUE;

            for(EntityEnderCrystal endCrystal2 : list) {
                double e = endCrystal2.distanceToSqr(this);
                if (e < d) {
                    d = e;
                    endCrystal = endCrystal2;
                }
            }

            this.nearestCrystal = endCrystal;
        }

    }

    private void knockBack(List<Entity> entities) {
        double d = (this.body.getBoundingBox().minX + this.body.getBoundingBox().maxX) / 2.0D;
        double e = (this.body.getBoundingBox().minZ + this.body.getBoundingBox().maxZ) / 2.0D;

        for(Entity entity : entities) {
            if (entity instanceof EntityLiving) {
                double f = entity.locX() - d;
                double g = entity.locZ() - e;
                double h = Math.max(f * f + g * g, 0.1D);
                entity.push(f / h * 4.0D, (double)0.2F, g / h * 4.0D);
                if (!this.phaseManager.getCurrentPhase().isSitting() && ((EntityLiving)entity).getLastHurtByMobTimestamp() < entity.tickCount - 2) {
                    entity.damageEntity(DamageSource.mobAttack(this), 5.0F);
                    this.doEnchantDamageEffects(this, entity);
                }
            }
        }

    }

    private void hurt(List<Entity> entities) {
        for(Entity entity : entities) {
            if (entity instanceof EntityLiving) {
                entity.damageEntity(DamageSource.mobAttack(this), 10.0F);
                this.doEnchantDamageEffects(this, entity);
            }
        }

    }

    private float rotWrap(double yawDegrees) {
        return (float)MathHelper.wrapDegrees(yawDegrees);
    }

    private boolean checkWalls(AxisAlignedBB box) {
        int i = MathHelper.floor(box.minX);
        int j = MathHelper.floor(box.minY);
        int k = MathHelper.floor(box.minZ);
        int l = MathHelper.floor(box.maxX);
        int m = MathHelper.floor(box.maxY);
        int n = MathHelper.floor(box.maxZ);
        boolean bl = false;
        boolean bl2 = false;

        for(int o = i; o <= l; ++o) {
            for(int p = j; p <= m; ++p) {
                for(int q = k; q <= n; ++q) {
                    BlockPosition blockPos = new BlockPosition(o, p, q);
                    IBlockData blockState = this.level.getType(blockPos);
                    if (!blockState.isAir() && blockState.getMaterial() != Material.FIRE) {
                        if (this.level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) && !blockState.is(TagsBlock.DRAGON_IMMUNE)) {
                            bl2 = this.level.removeBlock(blockPos, false) || bl2;
                        } else {
                            bl = true;
                        }
                    }
                }
            }
        }

        if (bl2) {
            BlockPosition blockPos2 = new BlockPosition(i + this.random.nextInt(l - i + 1), j + this.random.nextInt(m - j + 1), k + this.random.nextInt(n - k + 1));
            this.level.triggerEffect(2008, blockPos2, 0);
        }

        return bl;
    }

    public boolean hurt(EntityComplexPart part, DamageSource source, float amount) {
        if (this.phaseManager.getCurrentPhase().getControllerPhase() == DragonControllerPhase.DYING) {
            return false;
        } else {
            amount = this.phaseManager.getCurrentPhase().onHurt(source, amount);
            if (part != this.head) {
                amount = amount / 4.0F + Math.min(amount, 1.0F);
            }

            if (amount < 0.01F) {
                return false;
            } else {
                if (source.getEntity() instanceof EntityHuman || source.isExplosion()) {
                    float f = this.getHealth();
                    this.dealDamage(source, amount);
                    if (this.isDeadOrDying() && !this.phaseManager.getCurrentPhase().isSitting()) {
                        this.setHealth(1.0F);
                        this.phaseManager.setControllerPhase(DragonControllerPhase.DYING);
                    }

                    if (this.phaseManager.getCurrentPhase().isSitting()) {
                        this.sittingDamageReceived = (int)((float)this.sittingDamageReceived + (f - this.getHealth()));
                        if ((float)this.sittingDamageReceived > 0.25F * this.getMaxHealth()) {
                            this.sittingDamageReceived = 0;
                            this.phaseManager.setControllerPhase(DragonControllerPhase.TAKEOFF);
                        }
                    }
                }

                return true;
            }
        }
    }

    @Override
    public boolean damageEntity(DamageSource source, float amount) {
        if (source instanceof EntityDamageSource && ((EntityDamageSource)source).isThorns()) {
            this.hurt(this.body, source, amount);
        }

        return false;
    }

    protected boolean dealDamage(DamageSource source, float amount) {
        return super.damageEntity(source, amount);
    }

    @Override
    public void killEntity() {
        this.remove(Entity.RemovalReason.KILLED);
        if (this.dragonFight != null) {
            this.dragonFight.updateDragon(this);
            this.dragonFight.setDragonKilled(this);
        }

    }

    @Override
    protected void tickDeath() {
        if (this.dragonFight != null) {
            this.dragonFight.updateDragon(this);
        }

        ++this.dragonDeathTime;
        if (this.dragonDeathTime >= 180 && this.dragonDeathTime <= 200) {
            float f = (this.random.nextFloat() - 0.5F) * 8.0F;
            float g = (this.random.nextFloat() - 0.5F) * 4.0F;
            float h = (this.random.nextFloat() - 0.5F) * 8.0F;
            this.level.addParticle(Particles.EXPLOSION_EMITTER, this.locX() + (double)f, this.locY() + 2.0D + (double)g, this.locZ() + (double)h, 0.0D, 0.0D, 0.0D);
        }

        boolean bl = this.level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT);
        int i = 500;
        if (this.dragonFight != null && !this.dragonFight.isPreviouslyKilled()) {
            i = 12000;
        }

        if (this.level instanceof WorldServer) {
            if (this.dragonDeathTime > 150 && this.dragonDeathTime % 5 == 0 && bl) {
                EntityExperienceOrb.award((WorldServer)this.level, this.getPositionVector(), MathHelper.floor((float)i * 0.08F));
            }

            if (this.dragonDeathTime == 1 && !this.isSilent()) {
                this.level.broadcastWorldEvent(1028, this.getChunkCoordinates(), 0);
            }
        }

        this.move(EnumMoveType.SELF, new Vec3D(0.0D, (double)0.1F, 0.0D));
        this.setYRot(this.getYRot() + 20.0F);
        this.yBodyRot = this.getYRot();
        if (this.dragonDeathTime == 200 && this.level instanceof WorldServer) {
            if (bl) {
                EntityExperienceOrb.award((WorldServer)this.level, this.getPositionVector(), MathHelper.floor((float)i * 0.2F));
            }

            if (this.dragonFight != null) {
                this.dragonFight.setDragonKilled(this);
            }

            this.remove(Entity.RemovalReason.KILLED);
        }

    }

    public int findClosestNode() {
        if (this.nodes[0] == null) {
            for(int i = 0; i < 24; ++i) {
                int j = 5;
                int l;
                int m;
                if (i < 12) {
                    l = MathHelper.floor(60.0F * MathHelper.cos(2.0F * (-(float)Math.PI + 0.2617994F * (float)i)));
                    m = MathHelper.floor(60.0F * MathHelper.sin(2.0F * (-(float)Math.PI + 0.2617994F * (float)i)));
                } else if (i < 20) {
                    int k = i - 12;
                    l = MathHelper.floor(40.0F * MathHelper.cos(2.0F * (-(float)Math.PI + ((float)Math.PI / 8F) * (float)k)));
                    m = MathHelper.floor(40.0F * MathHelper.sin(2.0F * (-(float)Math.PI + ((float)Math.PI / 8F) * (float)k)));
                    j += 10;
                } else {
                    int var7 = i - 20;
                    l = MathHelper.floor(20.0F * MathHelper.cos(2.0F * (-(float)Math.PI + ((float)Math.PI / 4F) * (float)var7)));
                    m = MathHelper.floor(20.0F * MathHelper.sin(2.0F * (-(float)Math.PI + ((float)Math.PI / 4F) * (float)var7)));
                }

                int r = Math.max(this.level.getSeaLevel() + 10, this.level.getHighestBlockYAt(HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, new BlockPosition(l, 0, m)).getY() + j);
                this.nodes[i] = new PathPoint(l, r, m);
            }

            this.nodeAdjacency[0] = 6146;
            this.nodeAdjacency[1] = 8197;
            this.nodeAdjacency[2] = 8202;
            this.nodeAdjacency[3] = 16404;
            this.nodeAdjacency[4] = 32808;
            this.nodeAdjacency[5] = 32848;
            this.nodeAdjacency[6] = 65696;
            this.nodeAdjacency[7] = 131392;
            this.nodeAdjacency[8] = 131712;
            this.nodeAdjacency[9] = 263424;
            this.nodeAdjacency[10] = 526848;
            this.nodeAdjacency[11] = 525313;
            this.nodeAdjacency[12] = 1581057;
            this.nodeAdjacency[13] = 3166214;
            this.nodeAdjacency[14] = 2138120;
            this.nodeAdjacency[15] = 6373424;
            this.nodeAdjacency[16] = 4358208;
            this.nodeAdjacency[17] = 12910976;
            this.nodeAdjacency[18] = 9044480;
            this.nodeAdjacency[19] = 9706496;
            this.nodeAdjacency[20] = 15216640;
            this.nodeAdjacency[21] = 13688832;
            this.nodeAdjacency[22] = 11763712;
            this.nodeAdjacency[23] = 8257536;
        }

        return this.findClosestNode(this.locX(), this.locY(), this.locZ());
    }

    public int findClosestNode(double x, double y, double z) {
        float f = 10000.0F;
        int i = 0;
        PathPoint node = new PathPoint(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z));
        int j = 0;
        if (this.dragonFight == null || this.dragonFight.getCrystalsAlive() == 0) {
            j = 12;
        }

        for(int k = j; k < 24; ++k) {
            if (this.nodes[k] != null) {
                float g = this.nodes[k].distanceToSqr(node);
                if (g < f) {
                    f = g;
                    i = k;
                }
            }
        }

        return i;
    }

    @Nullable
    public PathEntity findPath(int from, int to, @Nullable PathPoint pathNode) {
        for(int i = 0; i < 24; ++i) {
            PathPoint node = this.nodes[i];
            node.closed = false;
            node.f = 0.0F;
            node.g = 0.0F;
            node.h = 0.0F;
            node.cameFrom = null;
            node.heapIdx = -1;
        }

        PathPoint node2 = this.nodes[from];
        PathPoint node3 = this.nodes[to];
        node2.g = 0.0F;
        node2.h = node2.distanceTo(node3);
        node2.f = node2.h;
        this.openSet.clear();
        this.openSet.insert(node2);
        PathPoint node4 = node2;
        int j = 0;
        if (this.dragonFight == null || this.dragonFight.getCrystalsAlive() == 0) {
            j = 12;
        }

        while(!this.openSet.isEmpty()) {
            PathPoint node5 = this.openSet.pop();
            if (node5.equals(node3)) {
                if (pathNode != null) {
                    pathNode.cameFrom = node3;
                    node3 = pathNode;
                }

                return this.reconstructPath(node2, node3);
            }

            if (node5.distanceTo(node3) < node4.distanceTo(node3)) {
                node4 = node5;
            }

            node5.closed = true;
            int k = 0;

            for(int l = 0; l < 24; ++l) {
                if (this.nodes[l] == node5) {
                    k = l;
                    break;
                }
            }

            for(int m = j; m < 24; ++m) {
                if ((this.nodeAdjacency[k] & 1 << m) > 0) {
                    PathPoint node6 = this.nodes[m];
                    if (!node6.closed) {
                        float f = node5.g + node5.distanceTo(node6);
                        if (!node6.inOpenSet() || f < node6.g) {
                            node6.cameFrom = node5;
                            node6.g = f;
                            node6.h = node6.distanceTo(node3);
                            if (node6.inOpenSet()) {
                                this.openSet.changeCost(node6, node6.g + node6.h);
                            } else {
                                node6.f = node6.g + node6.h;
                                this.openSet.insert(node6);
                            }
                        }
                    }
                }
            }
        }

        if (node4 == node2) {
            return null;
        } else {
            LOGGER.debug("Failed to find path from {} to {}", from, to);
            if (pathNode != null) {
                pathNode.cameFrom = node4;
                node4 = pathNode;
            }

            return this.reconstructPath(node2, node4);
        }
    }

    private PathEntity reconstructPath(PathPoint unused, PathPoint node) {
        List<PathPoint> list = Lists.newArrayList();
        PathPoint node2 = node;
        list.add(0, node);

        while(node2.cameFrom != null) {
            node2 = node2.cameFrom;
            list.add(0, node2);
        }

        return new PathEntity(list, new BlockPosition(node.x, node.y, node.z), true);
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        nbt.setInt("DragonPhase", this.phaseManager.getCurrentPhase().getControllerPhase().getId());
        nbt.setInt("DragonDeathTime", this.dragonDeathTime);
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        if (nbt.hasKey("DragonPhase")) {
            this.phaseManager.setControllerPhase(DragonControllerPhase.getById(nbt.getInt("DragonPhase")));
        }

        if (nbt.hasKey("DragonDeathTime")) {
            this.dragonDeathTime = nbt.getInt("DragonDeathTime");
        }

    }

    @Override
    public void checkDespawn() {
    }

    public EntityComplexPart[] getSubEntities() {
        return this.subEntities;
    }

    @Override
    public boolean isInteractable() {
        return false;
    }

    @Override
    public EnumSoundCategory getSoundCategory() {
        return EnumSoundCategory.HOSTILE;
    }

    @Override
    protected SoundEffect getSoundAmbient() {
        return SoundEffects.ENDER_DRAGON_AMBIENT;
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return SoundEffects.ENDER_DRAGON_HURT;
    }

    @Override
    public float getSoundVolume() {
        return 5.0F;
    }

    public float getHeadPartYOffset(int segmentOffset, double[] segment1, double[] segment2) {
        IDragonController dragonPhaseInstance = this.phaseManager.getCurrentPhase();
        DragonControllerPhase<? extends IDragonController> enderDragonPhase = dragonPhaseInstance.getControllerPhase();
        double f;
        if (enderDragonPhase != DragonControllerPhase.LANDING && enderDragonPhase != DragonControllerPhase.TAKEOFF) {
            if (dragonPhaseInstance.isSitting()) {
                f = (double)segmentOffset;
            } else if (segmentOffset == 6) {
                f = 0.0D;
            } else {
                f = segment2[1] - segment1[1];
            }
        } else {
            BlockPosition blockPos = this.level.getHighestBlockYAt(HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, WorldGenEndTrophy.END_PODIUM_LOCATION);
            double d = Math.max(Math.sqrt(blockPos.distSqr(this.getPositionVector(), true)) / 4.0D, 1.0D);
            f = (double)segmentOffset / d;
        }

        return (float)f;
    }

    public Vec3D getHeadLookVector(float tickDelta) {
        IDragonController dragonPhaseInstance = this.phaseManager.getCurrentPhase();
        DragonControllerPhase<? extends IDragonController> enderDragonPhase = dragonPhaseInstance.getControllerPhase();
        Vec3D vec32;
        if (enderDragonPhase != DragonControllerPhase.LANDING && enderDragonPhase != DragonControllerPhase.TAKEOFF) {
            if (dragonPhaseInstance.isSitting()) {
                float j = this.getXRot();
                float k = 1.5F;
                this.setXRot(-45.0F);
                vec32 = this.getViewVector(tickDelta);
                this.setXRot(j);
            } else {
                vec32 = this.getViewVector(tickDelta);
            }
        } else {
            BlockPosition blockPos = this.level.getHighestBlockYAt(HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, WorldGenEndTrophy.END_PODIUM_LOCATION);
            float f = Math.max((float)Math.sqrt(blockPos.distSqr(this.getPositionVector(), true)) / 4.0F, 1.0F);
            float g = 6.0F / f;
            float h = this.getXRot();
            float i = 1.5F;
            this.setXRot(-g * 1.5F * 5.0F);
            vec32 = this.getViewVector(tickDelta);
            this.setXRot(h);
        }

        return vec32;
    }

    public void onCrystalDestroyed(EntityEnderCrystal crystal, BlockPosition pos, DamageSource source) {
        EntityHuman player;
        if (source.getEntity() instanceof EntityHuman) {
            player = (EntityHuman)source.getEntity();
        } else {
            player = this.level.getNearestPlayer(CRYSTAL_DESTROY_TARGETING, (double)pos.getX(), (double)pos.getY(), (double)pos.getZ());
        }

        if (crystal == this.nearestCrystal) {
            this.hurt(this.head, DamageSource.explosion(player), 10.0F);
        }

        this.phaseManager.getCurrentPhase().onCrystalDestroyed(crystal, pos, source, player);
    }

    @Override
    public void onSyncedDataUpdated(DataWatcherObject<?> data) {
        if (DATA_PHASE.equals(data) && this.level.isClientSide) {
            this.phaseManager.setControllerPhase(DragonControllerPhase.getById(this.getDataWatcher().get(DATA_PHASE)));
        }

        super.onSyncedDataUpdated(data);
    }

    public DragonControllerManager getDragonControllerManager() {
        return this.phaseManager;
    }

    @Nullable
    public EnderDragonBattle getEnderDragonBattle() {
        return this.dragonFight;
    }

    @Override
    public boolean addEffect(MobEffect effect, @Nullable Entity source) {
        return false;
    }

    @Override
    protected boolean canRide(Entity entity) {
        return false;
    }

    @Override
    public boolean canPortal() {
        return false;
    }

    @Override
    public void recreateFromPacket(PacketPlayOutSpawnEntityLiving packet) {
        super.recreateFromPacket(packet);
        EntityComplexPart[] enderDragonParts = this.getSubEntities();

        for(int i = 0; i < enderDragonParts.length; ++i) {
            enderDragonParts[i].setId(i + packet.getId());
        }

    }

    @Override
    public boolean canAttack(EntityLiving target) {
        return target.canBeSeenAsEnemy();
    }
}
