package net.minecraft.world.entity.monster;

import com.mojang.math.Vector3fa;
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.core.BaseBlockPosition;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntityLiving;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.util.MathHelper;
import net.minecraft.world.DifficultyDamageScaler;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.EntitySize;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.EnumMoveType;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.entity.IEntitySelector;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.control.ControllerLook;
import net.minecraft.world.entity.ai.control.EntityAIBodyControl;
import net.minecraft.world.entity.ai.goal.PathfinderGoal;
import net.minecraft.world.entity.ai.goal.PathfinderGoalLookAtPlayer;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomLookaround;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalHurtByTarget;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalNearestAttackableTarget;
import net.minecraft.world.entity.animal.EntityGolem;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.EntityArrow;
import net.minecraft.world.entity.projectile.EntityShulkerBullet;
import net.minecraft.world.item.EnumColor;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldAccess;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.Vec3D;

public class EntityShulker extends EntityGolem implements IMonster {
    private static final UUID COVERED_ARMOR_MODIFIER_UUID = UUID.fromString("7E0292F2-9434-48D5-A29F-9583AF7DF27F");
    private static final AttributeModifier COVERED_ARMOR_MODIFIER = new AttributeModifier(COVERED_ARMOR_MODIFIER_UUID, "Covered armor bonus", 20.0D, AttributeModifier.Operation.ADDITION);
    protected static final DataWatcherObject<EnumDirection> DATA_ATTACH_FACE_ID = DataWatcher.defineId(EntityShulker.class, DataWatcherRegistry.DIRECTION);
    protected static final DataWatcherObject<Byte> DATA_PEEK_ID = DataWatcher.defineId(EntityShulker.class, DataWatcherRegistry.BYTE);
    public static final DataWatcherObject<Byte> DATA_COLOR_ID = DataWatcher.defineId(EntityShulker.class, DataWatcherRegistry.BYTE);
    private static final int TELEPORT_STEPS = 6;
    private static final byte NO_COLOR = 16;
    private static final byte DEFAULT_COLOR = 16;
    private static final int MAX_TELEPORT_DISTANCE = 8;
    private static final int OTHER_SHULKER_SCAN_RADIUS = 8;
    private static final int OTHER_SHULKER_LIMIT = 5;
    private static final float PEEK_PER_TICK = 0.05F;
    static final Vector3fa FORWARD = SystemUtils.make(() -> {
        BaseBlockPosition vec3i = EnumDirection.SOUTH.getNormal();
        return new Vector3fa((float)vec3i.getX(), (float)vec3i.getY(), (float)vec3i.getZ());
    });
    private float currentPeekAmountO;
    private float currentPeekAmount;
    @Nullable
    private BlockPosition clientOldAttachPosition;
    private int clientSideTeleportInterpolation;
    private static final float MAX_LID_OPEN = 1.0F;

    public EntityShulker(EntityTypes<? extends EntityShulker> type, World world) {
        super(type, world);
        this.xpReward = 5;
        this.lookControl = new EntityShulker.ControllerLookShulker(this);
    }

    @Override
    protected void initPathfinder() {
        this.goalSelector.addGoal(1, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 8.0F, 0.02F, true));
        this.goalSelector.addGoal(4, new EntityShulker.PathfinderGoalShulkerAttack());
        this.goalSelector.addGoal(7, new EntityShulker.PathfinderGoalShulkerPeek());
        this.goalSelector.addGoal(8, new PathfinderGoalRandomLookaround(this));
        this.targetSelector.addGoal(1, (new PathfinderGoalHurtByTarget(this, this.getClass())).setAlertOthers());
        this.targetSelector.addGoal(2, new EntityShulker.PathfinderGoalShulkerNearestAttackableTarget(this));
        this.targetSelector.addGoal(3, new EntityShulker.PathfinderGoalShulkerDefenseAttack(this));
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
    }

    @Override
    public EnumSoundCategory getSoundCategory() {
        return EnumSoundCategory.HOSTILE;
    }

    @Override
    protected SoundEffect getSoundAmbient() {
        return SoundEffects.SHULKER_AMBIENT;
    }

    @Override
    public void playAmbientSound() {
        if (!this.isClosed()) {
            super.playAmbientSound();
        }

    }

    @Override
    public SoundEffect getSoundDeath() {
        return SoundEffects.SHULKER_DEATH;
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return this.isClosed() ? SoundEffects.SHULKER_HURT_CLOSED : SoundEffects.SHULKER_HURT;
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.entityData.register(DATA_ATTACH_FACE_ID, EnumDirection.DOWN);
        this.entityData.register(DATA_PEEK_ID, (byte)0);
        this.entityData.register(DATA_COLOR_ID, (byte)16);
    }

    public static AttributeProvider.Builder createAttributes() {
        return EntityInsentient.createMobAttributes().add(GenericAttributes.MAX_HEALTH, 30.0D);
    }

    @Override
    protected EntityAIBodyControl createBodyControl() {
        return new EntityShulker.EntityShulkerBodyControl(this);
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        this.setAttachFace(EnumDirection.fromType1(nbt.getByte("AttachFace")));
        this.entityData.set(DATA_PEEK_ID, nbt.getByte("Peek"));
        if (nbt.hasKeyOfType("Color", 99)) {
            this.entityData.set(DATA_COLOR_ID, nbt.getByte("Color"));
        }

    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        nbt.setByte("AttachFace", (byte)this.getAttachFace().get3DDataValue());
        nbt.setByte("Peek", this.entityData.get(DATA_PEEK_ID));
        nbt.setByte("Color", this.entityData.get(DATA_COLOR_ID));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level.isClientSide && !this.isPassenger() && !this.canStayAt(this.getChunkCoordinates(), this.getAttachFace())) {
            this.findNewAttachment();
        }

        if (this.updatePeekAmount()) {
            this.onPeekAmountChange();
        }

        if (this.level.isClientSide) {
            if (this.clientSideTeleportInterpolation > 0) {
                --this.clientSideTeleportInterpolation;
            } else {
                this.clientOldAttachPosition = null;
            }
        }

    }

    private void findNewAttachment() {
        EnumDirection direction = this.findAttachableSurface(this.getChunkCoordinates());
        if (direction != null) {
            this.setAttachFace(direction);
        } else {
            this.teleportSomewhere();
        }

    }

    @Override
    protected AxisAlignedBB makeBoundingBox() {
        float f = getPhysicalPeek(this.currentPeekAmount);
        EnumDirection direction = this.getAttachFace().opposite();
        float g = this.getEntityType().getWidth() / 2.0F;
        return getProgressAabb(direction, f).move(this.locX() - (double)g, this.locY(), this.locZ() - (double)g);
    }

    private static float getPhysicalPeek(float f) {
        return 0.5F - MathHelper.sin((0.5F + f) * (float)Math.PI) * 0.5F;
    }

    private boolean updatePeekAmount() {
        this.currentPeekAmountO = this.currentPeekAmount;
        float f = (float)this.getPeek() * 0.01F;
        if (this.currentPeekAmount == f) {
            return false;
        } else {
            if (this.currentPeekAmount > f) {
                this.currentPeekAmount = MathHelper.clamp(this.currentPeekAmount - 0.05F, f, 1.0F);
            } else {
                this.currentPeekAmount = MathHelper.clamp(this.currentPeekAmount + 0.05F, 0.0F, f);
            }

            return true;
        }
    }

    private void onPeekAmountChange() {
        this.reapplyPosition();
        float f = getPhysicalPeek(this.currentPeekAmount);
        float g = getPhysicalPeek(this.currentPeekAmountO);
        EnumDirection direction = this.getAttachFace().opposite();
        float h = f - g;
        if (!(h <= 0.0F)) {
            for(Entity entity : this.level.getEntities(this, getProgressDeltaAabb(direction, g, f).move(this.locX() - 0.5D, this.locY(), this.locZ() - 0.5D), IEntitySelector.NO_SPECTATORS.and((entityx) -> {
                return !entityx.isSameVehicle(this);
            }))) {
                if (!(entity instanceof EntityShulker) && !entity.noPhysics) {
                    entity.move(EnumMoveType.SHULKER, new Vec3D((double)(h * (float)direction.getAdjacentX()), (double)(h * (float)direction.getAdjacentY()), (double)(h * (float)direction.getAdjacentZ())));
                }
            }

        }
    }

    public static AxisAlignedBB getProgressAabb(EnumDirection direction, float f) {
        return getProgressDeltaAabb(direction, -1.0F, f);
    }

    public static AxisAlignedBB getProgressDeltaAabb(EnumDirection direction, float f, float g) {
        double d = (double)Math.max(f, g);
        double e = (double)Math.min(f, g);
        return (new AxisAlignedBB(BlockPosition.ZERO)).expandTowards((double)direction.getAdjacentX() * d, (double)direction.getAdjacentY() * d, (double)direction.getAdjacentZ() * d).contract((double)(-direction.getAdjacentX()) * (1.0D + e), (double)(-direction.getAdjacentY()) * (1.0D + e), (double)(-direction.getAdjacentZ()) * (1.0D + e));
    }

    @Override
    public double getMyRidingOffset() {
        EntityTypes<?> entityType = this.getVehicle().getEntityType();
        return entityType != EntityTypes.BOAT && entityType != EntityTypes.MINECART ? super.getMyRidingOffset() : 0.1875D - this.getVehicle().getPassengersRidingOffset();
    }

    @Override
    public boolean startRiding(Entity entity, boolean force) {
        if (this.level.isClientSide()) {
            this.clientOldAttachPosition = null;
            this.clientSideTeleportInterpolation = 0;
        }

        this.setAttachFace(EnumDirection.DOWN);
        return super.startRiding(entity, force);
    }

    @Override
    public void stopRiding() {
        super.stopRiding();
        if (this.level.isClientSide) {
            this.clientOldAttachPosition = this.getChunkCoordinates();
        }

        this.yBodyRotO = 0.0F;
        this.yBodyRot = 0.0F;
    }

    @Nullable
    @Override
    public GroupDataEntity prepare(WorldAccess world, DifficultyDamageScaler difficulty, EnumMobSpawn spawnReason, @Nullable GroupDataEntity entityData, @Nullable NBTTagCompound entityNbt) {
        this.setYRot(0.0F);
        this.yHeadRot = this.getYRot();
        this.setOldPosAndRot();
        return super.prepare(world, difficulty, spawnReason, entityData, entityNbt);
    }

    @Override
    public void move(EnumMoveType movementType, Vec3D movement) {
        if (movementType == EnumMoveType.SHULKER_BOX) {
            this.teleportSomewhere();
        } else {
            super.move(movementType, movement);
        }

    }

    @Override
    public Vec3D getMot() {
        return Vec3D.ZERO;
    }

    @Override
    public void setMot(Vec3D velocity) {
    }

    @Override
    public void setPosition(double x, double y, double z) {
        BlockPosition blockPos = this.getChunkCoordinates();
        if (this.isPassenger()) {
            super.setPosition(x, y, z);
        } else {
            super.setPosition((double)MathHelper.floor(x) + 0.5D, (double)MathHelper.floor(y + 0.5D), (double)MathHelper.floor(z) + 0.5D);
        }

        if (this.tickCount != 0) {
            BlockPosition blockPos2 = this.getChunkCoordinates();
            if (!blockPos2.equals(blockPos)) {
                this.entityData.set(DATA_PEEK_ID, (byte)0);
                this.hasImpulse = true;
                if (this.level.isClientSide && !this.isPassenger() && !blockPos2.equals(this.clientOldAttachPosition)) {
                    this.clientOldAttachPosition = blockPos;
                    this.clientSideTeleportInterpolation = 6;
                    this.xOld = this.locX();
                    this.yOld = this.locY();
                    this.zOld = this.locZ();
                }
            }

        }
    }

    @Nullable
    protected EnumDirection findAttachableSurface(BlockPosition pos) {
        for(EnumDirection direction : EnumDirection.values()) {
            if (this.canStayAt(pos, direction)) {
                return direction;
            }
        }

        return null;
    }

    boolean canStayAt(BlockPosition pos, EnumDirection direction) {
        if (this.isPositionBlocked(pos)) {
            return false;
        } else {
            EnumDirection direction2 = direction.opposite();
            if (!this.level.loadedAndEntityCanStandOnFace(pos.relative(direction), this, direction2)) {
                return false;
            } else {
                AxisAlignedBB aABB = getProgressAabb(direction2, 1.0F).move(pos).shrink(1.0E-6D);
                return this.level.getCubes(this, aABB);
            }
        }
    }

    private boolean isPositionBlocked(BlockPosition pos) {
        IBlockData blockState = this.level.getType(pos);
        if (blockState.isAir()) {
            return false;
        } else {
            boolean bl = blockState.is(Blocks.MOVING_PISTON) && pos.equals(this.getChunkCoordinates());
            return !bl;
        }
    }

    protected boolean teleportSomewhere() {
        if (!this.isNoAI() && this.isAlive()) {
            BlockPosition blockPos = this.getChunkCoordinates();

            for(int i = 0; i < 5; ++i) {
                BlockPosition blockPos2 = blockPos.offset(MathHelper.randomBetweenInclusive(this.random, -8, 8), MathHelper.randomBetweenInclusive(this.random, -8, 8), MathHelper.randomBetweenInclusive(this.random, -8, 8));
                if (blockPos2.getY() > this.level.getMinBuildHeight() && this.level.isEmpty(blockPos2) && this.level.getWorldBorder().isWithinBounds(blockPos2) && this.level.getCubes(this, (new AxisAlignedBB(blockPos2)).shrink(1.0E-6D))) {
                    EnumDirection direction = this.findAttachableSurface(blockPos2);
                    if (direction != null) {
                        this.decouple();
                        this.setAttachFace(direction);
                        this.playSound(SoundEffects.SHULKER_TELEPORT, 1.0F, 1.0F);
                        this.setPosition((double)blockPos2.getX() + 0.5D, (double)blockPos2.getY(), (double)blockPos2.getZ() + 0.5D);
                        this.entityData.set(DATA_PEEK_ID, (byte)0);
                        this.setGoalTarget((EntityLiving)null);
                        return true;
                    }
                }
            }

            return false;
        } else {
            return false;
        }
    }

    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int interpolationSteps, boolean interpolate) {
        this.lerpSteps = 0;
        this.setPosition(x, y, z);
        this.setYawPitch(yaw, pitch);
    }

    @Override
    public boolean damageEntity(DamageSource source, float amount) {
        if (this.isClosed()) {
            Entity entity = source.getDirectEntity();
            if (entity instanceof EntityArrow) {
                return false;
            }
        }

        if (!super.damageEntity(source, amount)) {
            return false;
        } else {
            if ((double)this.getHealth() < (double)this.getMaxHealth() * 0.5D && this.random.nextInt(4) == 0) {
                this.teleportSomewhere();
            } else if (source.isProjectile()) {
                Entity entity2 = source.getDirectEntity();
                if (entity2 != null && entity2.getEntityType() == EntityTypes.SHULKER_BULLET) {
                    this.hitByShulkerBullet();
                }
            }

            return true;
        }
    }

    private boolean isClosed() {
        return this.getPeek() == 0;
    }

    private void hitByShulkerBullet() {
        Vec3D vec3 = this.getPositionVector();
        AxisAlignedBB aABB = this.getBoundingBox();
        if (!this.isClosed() && this.teleportSomewhere()) {
            int i = this.level.getEntities(EntityTypes.SHULKER, aABB.inflate(8.0D), Entity::isAlive).size();
            float f = (float)(i - 1) / 5.0F;
            if (!(this.level.random.nextFloat() < f)) {
                EntityShulker shulker = EntityTypes.SHULKER.create(this.level);
                EnumColor dyeColor = this.getColor();
                if (dyeColor != null) {
                    shulker.setColor(dyeColor);
                }

                shulker.moveTo(vec3);
                this.level.addEntity(shulker);
            }
        }
    }

    @Override
    public boolean canBeCollidedWith() {
        return this.isAlive();
    }

    public EnumDirection getAttachFace() {
        return this.entityData.get(DATA_ATTACH_FACE_ID);
    }

    public void setAttachFace(EnumDirection face) {
        this.entityData.set(DATA_ATTACH_FACE_ID, face);
    }

    @Override
    public void onSyncedDataUpdated(DataWatcherObject<?> data) {
        if (DATA_ATTACH_FACE_ID.equals(data)) {
            this.setBoundingBox(this.makeBoundingBox());
        }

        super.onSyncedDataUpdated(data);
    }

    public int getPeek() {
        return this.entityData.get(DATA_PEEK_ID);
    }

    public void setPeek(int peekAmount) {
        if (!this.level.isClientSide) {
            this.getAttributeInstance(GenericAttributes.ARMOR).removeModifier(COVERED_ARMOR_MODIFIER);
            if (peekAmount == 0) {
                this.getAttributeInstance(GenericAttributes.ARMOR).addPermanentModifier(COVERED_ARMOR_MODIFIER);
                this.playSound(SoundEffects.SHULKER_CLOSE, 1.0F, 1.0F);
                this.gameEvent(GameEvent.SHULKER_CLOSE);
            } else {
                this.playSound(SoundEffects.SHULKER_OPEN, 1.0F, 1.0F);
                this.gameEvent(GameEvent.SHULKER_OPEN);
            }
        }

        this.entityData.set(DATA_PEEK_ID, (byte)peekAmount);
    }

    public float getClientPeekAmount(float delta) {
        return MathHelper.lerp(delta, this.currentPeekAmountO, this.currentPeekAmount);
    }

    @Override
    protected float getStandingEyeHeight(EntityPose pose, EntitySize dimensions) {
        return 0.5F;
    }

    @Override
    public void recreateFromPacket(PacketPlayOutSpawnEntityLiving packet) {
        super.recreateFromPacket(packet);
        this.yBodyRot = 0.0F;
    }

    @Override
    public int getMaxHeadXRot() {
        return 180;
    }

    @Override
    public int getMaxHeadYRot() {
        return 180;
    }

    @Override
    public void collide(Entity entity) {
    }

    @Override
    public float getPickRadius() {
        return 0.0F;
    }

    public Optional<Vec3D> getRenderPosition(float f) {
        if (this.clientOldAttachPosition != null && this.clientSideTeleportInterpolation > 0) {
            double d = (double)((float)this.clientSideTeleportInterpolation - f) / 6.0D;
            d = d * d;
            BlockPosition blockPos = this.getChunkCoordinates();
            double e = (double)(blockPos.getX() - this.clientOldAttachPosition.getX()) * d;
            double g = (double)(blockPos.getY() - this.clientOldAttachPosition.getY()) * d;
            double h = (double)(blockPos.getZ() - this.clientOldAttachPosition.getZ()) * d;
            return Optional.of(new Vec3D(-e, -g, -h));
        } else {
            return Optional.empty();
        }
    }

    private void setColor(EnumColor color) {
        this.entityData.set(DATA_COLOR_ID, (byte)color.getColorIndex());
    }

    @Nullable
    public EnumColor getColor() {
        byte b = this.entityData.get(DATA_COLOR_ID);
        return b != 16 && b <= 15 ? EnumColor.fromColorIndex(b) : null;
    }

    class ControllerLookShulker extends ControllerLook {
        public ControllerLookShulker(EntityInsentient entity) {
            super(entity);
        }

        @Override
        protected void clampHeadRotationToBody() {
        }

        @Override
        protected Optional<Float> getYRotD() {
            EnumDirection direction = EntityShulker.this.getAttachFace().opposite();
            Vector3fa vector3f = EntityShulker.FORWARD.copy();
            vector3f.transform(direction.getRotation());
            BaseBlockPosition vec3i = direction.getNormal();
            Vector3fa vector3f2 = new Vector3fa((float)vec3i.getX(), (float)vec3i.getY(), (float)vec3i.getZ());
            vector3f2.cross(vector3f);
            double d = this.wantedX - this.mob.locX();
            double e = this.wantedY - this.mob.getHeadY();
            double f = this.wantedZ - this.mob.locZ();
            Vector3fa vector3f3 = new Vector3fa((float)d, (float)e, (float)f);
            float g = vector3f2.dot(vector3f3);
            float h = vector3f.dot(vector3f3);
            return !(Math.abs(g) > 1.0E-5F) && !(Math.abs(h) > 1.0E-5F) ? Optional.empty() : Optional.of((float)(MathHelper.atan2((double)(-g), (double)h) * (double)(180F / (float)Math.PI)));
        }

        @Override
        protected Optional<Float> getXRotD() {
            return Optional.of(0.0F);
        }
    }

    static class EntityShulkerBodyControl extends EntityAIBodyControl {
        public EntityShulkerBodyControl(EntityInsentient entity) {
            super(entity);
        }

        @Override
        public void clientTick() {
        }
    }

    class PathfinderGoalShulkerAttack extends PathfinderGoal {
        private int attackTime;

        public PathfinderGoalShulkerAttack() {
            this.setFlags(EnumSet.of(PathfinderGoal.Type.MOVE, PathfinderGoal.Type.LOOK));
        }

        @Override
        public boolean canUse() {
            EntityLiving livingEntity = EntityShulker.this.getGoalTarget();
            if (livingEntity != null && livingEntity.isAlive()) {
                return EntityShulker.this.level.getDifficulty() != EnumDifficulty.PEACEFUL;
            } else {
                return false;
            }
        }

        @Override
        public void start() {
            this.attackTime = 20;
            EntityShulker.this.setPeek(100);
        }

        @Override
        public void stop() {
            EntityShulker.this.setPeek(0);
        }

        @Override
        public void tick() {
            if (EntityShulker.this.level.getDifficulty() != EnumDifficulty.PEACEFUL) {
                --this.attackTime;
                EntityLiving livingEntity = EntityShulker.this.getGoalTarget();
                EntityShulker.this.getControllerLook().setLookAt(livingEntity, 180.0F, 180.0F);
                double d = EntityShulker.this.distanceToSqr(livingEntity);
                if (d < 400.0D) {
                    if (this.attackTime <= 0) {
                        this.attackTime = 20 + EntityShulker.this.random.nextInt(10) * 20 / 2;
                        EntityShulker.this.level.addEntity(new EntityShulkerBullet(EntityShulker.this.level, EntityShulker.this, livingEntity, EntityShulker.this.getAttachFace().getAxis()));
                        EntityShulker.this.playSound(SoundEffects.SHULKER_SHOOT, 2.0F, (EntityShulker.this.random.nextFloat() - EntityShulker.this.random.nextFloat()) * 0.2F + 1.0F);
                    }
                } else {
                    EntityShulker.this.setGoalTarget((EntityLiving)null);
                }

                super.tick();
            }
        }
    }

    static class PathfinderGoalShulkerDefenseAttack extends PathfinderGoalNearestAttackableTarget<EntityLiving> {
        public PathfinderGoalShulkerDefenseAttack(EntityShulker shulker) {
            super(shulker, EntityLiving.class, 10, true, false, (entity) -> {
                return entity instanceof IMonster;
            });
        }

        @Override
        public boolean canUse() {
            return this.mob.getScoreboardTeam() == null ? false : super.canUse();
        }

        @Override
        protected AxisAlignedBB getTargetSearchArea(double distance) {
            EnumDirection direction = ((EntityShulker)this.mob).getAttachFace();
            if (direction.getAxis() == EnumDirection.EnumAxis.X) {
                return this.mob.getBoundingBox().grow(4.0D, distance, distance);
            } else {
                return direction.getAxis() == EnumDirection.EnumAxis.Z ? this.mob.getBoundingBox().grow(distance, distance, 4.0D) : this.mob.getBoundingBox().grow(distance, 4.0D, distance);
            }
        }
    }

    class PathfinderGoalShulkerNearestAttackableTarget extends PathfinderGoalNearestAttackableTarget<EntityHuman> {
        public PathfinderGoalShulkerNearestAttackableTarget(EntityShulker shulker) {
            super(shulker, EntityHuman.class, true);
        }

        @Override
        public boolean canUse() {
            return EntityShulker.this.level.getDifficulty() == EnumDifficulty.PEACEFUL ? false : super.canUse();
        }

        @Override
        protected AxisAlignedBB getTargetSearchArea(double distance) {
            EnumDirection direction = ((EntityShulker)this.mob).getAttachFace();
            if (direction.getAxis() == EnumDirection.EnumAxis.X) {
                return this.mob.getBoundingBox().grow(4.0D, distance, distance);
            } else {
                return direction.getAxis() == EnumDirection.EnumAxis.Z ? this.mob.getBoundingBox().grow(distance, distance, 4.0D) : this.mob.getBoundingBox().grow(distance, 4.0D, distance);
            }
        }
    }

    class PathfinderGoalShulkerPeek extends PathfinderGoal {
        private int peekTime;

        @Override
        public boolean canUse() {
            return EntityShulker.this.getGoalTarget() == null && EntityShulker.this.random.nextInt(40) == 0 && EntityShulker.this.canStayAt(EntityShulker.this.getChunkCoordinates(), EntityShulker.this.getAttachFace());
        }

        @Override
        public boolean canContinueToUse() {
            return EntityShulker.this.getGoalTarget() == null && this.peekTime > 0;
        }

        @Override
        public void start() {
            this.peekTime = 20 * (1 + EntityShulker.this.random.nextInt(3));
            EntityShulker.this.setPeek(30);
        }

        @Override
        public void stop() {
            if (EntityShulker.this.getGoalTarget() == null) {
                EntityShulker.this.setPeek(0);
            }

        }

        @Override
        public void tick() {
            --this.peekTime;
        }
    }
}
