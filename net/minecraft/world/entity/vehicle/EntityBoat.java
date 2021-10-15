package net.minecraft.world.entity.vehicle;

import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.particles.Particles;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayInBoatMove;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntity;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.TagsFluid;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.EntitySize;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMoveType;
import net.minecraft.world.entity.IEntitySelector;
import net.minecraft.world.entity.animal.EntityAnimal;
import net.minecraft.world.entity.animal.EntityWaterAnimal;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockWaterLily;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.Vec3D;
import net.minecraft.world.phys.shapes.OperatorBoolean;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapes;

public class EntityBoat extends Entity {
    private static final DataWatcherObject<Integer> DATA_ID_HURT = DataWatcher.defineId(EntityBoat.class, DataWatcherRegistry.INT);
    private static final DataWatcherObject<Integer> DATA_ID_HURTDIR = DataWatcher.defineId(EntityBoat.class, DataWatcherRegistry.INT);
    private static final DataWatcherObject<Float> DATA_ID_DAMAGE = DataWatcher.defineId(EntityBoat.class, DataWatcherRegistry.FLOAT);
    private static final DataWatcherObject<Integer> DATA_ID_TYPE = DataWatcher.defineId(EntityBoat.class, DataWatcherRegistry.INT);
    private static final DataWatcherObject<Boolean> DATA_ID_PADDLE_LEFT = DataWatcher.defineId(EntityBoat.class, DataWatcherRegistry.BOOLEAN);
    private static final DataWatcherObject<Boolean> DATA_ID_PADDLE_RIGHT = DataWatcher.defineId(EntityBoat.class, DataWatcherRegistry.BOOLEAN);
    private static final DataWatcherObject<Integer> DATA_ID_BUBBLE_TIME = DataWatcher.defineId(EntityBoat.class, DataWatcherRegistry.INT);
    public static final int PADDLE_LEFT = 0;
    public static final int PADDLE_RIGHT = 1;
    private static final int TIME_TO_EJECT = 60;
    private static final double PADDLE_SPEED = (double)((float)Math.PI / 8F);
    public static final double PADDLE_SOUND_TIME = (double)((float)Math.PI / 4F);
    public static final int BUBBLE_TIME = 60;
    private final float[] paddlePositions = new float[2];
    private float invFriction;
    private float outOfControlTicks;
    private float deltaRotation;
    private int lerpSteps;
    private double lerpX;
    private double lerpY;
    private double lerpZ;
    private double lerpYRot;
    private double lerpXRot;
    private boolean inputLeft;
    private boolean inputRight;
    private boolean inputUp;
    private boolean inputDown;
    private double waterLevel;
    private float landFriction;
    private EntityBoat.EnumStatus status;
    private EntityBoat.EnumStatus oldStatus;
    private double lastYd;
    private boolean isAboveBubbleColumn;
    private boolean bubbleColumnDirectionIsDown;
    private float bubbleMultiplier;
    private float bubbleAngle;
    private float bubbleAngleO;

    public EntityBoat(EntityTypes<? extends EntityBoat> type, World world) {
        super(type, world);
        this.blocksBuilding = true;
    }

    public EntityBoat(World world, double x, double y, double z) {
        this(EntityTypes.BOAT, world);
        this.setPosition(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    @Override
    protected float getHeadHeight(EntityPose pose, EntitySize dimensions) {
        return dimensions.height;
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
    }

    @Override
    protected void initDatawatcher() {
        this.entityData.register(DATA_ID_HURT, 0);
        this.entityData.register(DATA_ID_HURTDIR, 1);
        this.entityData.register(DATA_ID_DAMAGE, 0.0F);
        this.entityData.register(DATA_ID_TYPE, EntityBoat.EnumBoatType.OAK.ordinal());
        this.entityData.register(DATA_ID_PADDLE_LEFT, false);
        this.entityData.register(DATA_ID_PADDLE_RIGHT, false);
        this.entityData.register(DATA_ID_BUBBLE_TIME, 0);
    }

    @Override
    public boolean canCollideWith(Entity other) {
        return canVehicleCollide(this, other);
    }

    public static boolean canVehicleCollide(Entity entity, Entity other) {
        return (other.canBeCollidedWith() || other.isCollidable()) && !entity.isSameVehicle(other);
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isCollidable() {
        return true;
    }

    @Override
    protected Vec3D getRelativePortalPosition(EnumDirection.EnumAxis portalAxis, BlockUtil.Rectangle portalRect) {
        return EntityLiving.resetForwardDirectionOfRelativePortalPosition(super.getRelativePortalPosition(portalAxis, portalRect));
    }

    @Override
    public double getPassengersRidingOffset() {
        return -0.1D;
    }

    @Override
    public boolean damageEntity(DamageSource source, float amount) {
        if (this.isInvulnerable(source)) {
            return false;
        } else if (!this.level.isClientSide && !this.isRemoved()) {
            this.setHurtDir(-this.getHurtDir());
            this.setHurtTime(10);
            this.setDamage(this.getDamage() + amount * 10.0F);
            this.velocityChanged();
            this.gameEvent(GameEvent.ENTITY_DAMAGED, source.getEntity());
            boolean bl = source.getEntity() instanceof EntityHuman && ((EntityHuman)source.getEntity()).getAbilities().instabuild;
            if (bl || this.getDamage() > 40.0F) {
                if (!bl && this.level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
                    this.spawnAtLocation(this.getDropItem());
                }

                this.die();
            }

            return true;
        } else {
            return true;
        }
    }

    @Override
    public void onAboveBubbleCol(boolean drag) {
        if (!this.level.isClientSide) {
            this.isAboveBubbleColumn = true;
            this.bubbleColumnDirectionIsDown = drag;
            if (this.getBubbleTime() == 0) {
                this.setBubbleTime(60);
            }
        }

        this.level.addParticle(Particles.SPLASH, this.locX() + (double)this.random.nextFloat(), this.locY() + 0.7D, this.locZ() + (double)this.random.nextFloat(), 0.0D, 0.0D, 0.0D);
        if (this.random.nextInt(20) == 0) {
            this.level.playLocalSound(this.locX(), this.locY(), this.locZ(), this.getSoundSplash(), this.getSoundCategory(), 1.0F, 0.8F + 0.4F * this.random.nextFloat(), false);
        }

        this.gameEvent(GameEvent.SPLASH, this.getRidingPassenger());
    }

    @Override
    public void collide(Entity entity) {
        if (entity instanceof EntityBoat) {
            if (entity.getBoundingBox().minY < this.getBoundingBox().maxY) {
                super.collide(entity);
            }
        } else if (entity.getBoundingBox().minY <= this.getBoundingBox().minY) {
            super.collide(entity);
        }

    }

    public Item getDropItem() {
        switch(this.getType()) {
        case OAK:
        default:
            return Items.OAK_BOAT;
        case SPRUCE:
            return Items.SPRUCE_BOAT;
        case BIRCH:
            return Items.BIRCH_BOAT;
        case JUNGLE:
            return Items.JUNGLE_BOAT;
        case ACACIA:
            return Items.ACACIA_BOAT;
        case DARK_OAK:
            return Items.DARK_OAK_BOAT;
        }
    }

    @Override
    public void animateHurt() {
        this.setHurtDir(-this.getHurtDir());
        this.setHurtTime(10);
        this.setDamage(this.getDamage() * 11.0F);
    }

    @Override
    public boolean isInteractable() {
        return !this.isRemoved();
    }

    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int interpolationSteps, boolean interpolate) {
        this.lerpX = x;
        this.lerpY = y;
        this.lerpZ = z;
        this.lerpYRot = (double)yaw;
        this.lerpXRot = (double)pitch;
        this.lerpSteps = 10;
    }

    @Override
    public EnumDirection getAdjustedDirection() {
        return this.getDirection().getClockWise();
    }

    @Override
    public void tick() {
        this.oldStatus = this.status;
        this.status = this.getStatus();
        if (this.status != EntityBoat.EnumStatus.UNDER_WATER && this.status != EntityBoat.EnumStatus.UNDER_FLOWING_WATER) {
            this.outOfControlTicks = 0.0F;
        } else {
            ++this.outOfControlTicks;
        }

        if (!this.level.isClientSide && this.outOfControlTicks >= 60.0F) {
            this.ejectPassengers();
        }

        if (this.getHurtTime() > 0) {
            this.setHurtTime(this.getHurtTime() - 1);
        }

        if (this.getDamage() > 0.0F) {
            this.setDamage(this.getDamage() - 1.0F);
        }

        super.tick();
        this.tickLerp();
        if (this.isControlledByLocalInstance()) {
            if (!(this.getFirstPassenger() instanceof EntityHuman)) {
                this.setPaddleState(false, false);
            }

            this.floatBoat();
            if (this.level.isClientSide) {
                this.controlBoat();
                this.level.sendPacketToServer(new PacketPlayInBoatMove(this.getPaddleState(0), this.getPaddleState(1)));
            }

            this.move(EnumMoveType.SELF, this.getMot());
        } else {
            this.setMot(Vec3D.ZERO);
        }

        this.tickBubbleColumn();

        for(int i = 0; i <= 1; ++i) {
            if (this.getPaddleState(i)) {
                if (!this.isSilent() && (double)(this.paddlePositions[i] % ((float)Math.PI * 2F)) <= (double)((float)Math.PI / 4F) && ((double)this.paddlePositions[i] + (double)((float)Math.PI / 8F)) % (double)((float)Math.PI * 2F) >= (double)((float)Math.PI / 4F)) {
                    SoundEffect soundEvent = this.getPaddleSound();
                    if (soundEvent != null) {
                        Vec3D vec3 = this.getViewVector(1.0F);
                        double d = i == 1 ? -vec3.z : vec3.z;
                        double e = i == 1 ? vec3.x : -vec3.x;
                        this.level.playSound((EntityHuman)null, this.locX() + d, this.locY(), this.locZ() + e, soundEvent, this.getSoundCategory(), 1.0F, 0.8F + 0.4F * this.random.nextFloat());
                        this.level.gameEvent(this.getRidingPassenger(), GameEvent.SPLASH, new BlockPosition(this.locX() + d, this.locY(), this.locZ() + e));
                    }
                }

                this.paddlePositions[i] = (float)((double)this.paddlePositions[i] + (double)((float)Math.PI / 8F));
            } else {
                this.paddlePositions[i] = 0.0F;
            }
        }

        this.checkBlockCollisions();
        List<Entity> list = this.level.getEntities(this, this.getBoundingBox().grow((double)0.2F, (double)-0.01F, (double)0.2F), IEntitySelector.pushableBy(this));
        if (!list.isEmpty()) {
            boolean bl = !this.level.isClientSide && !(this.getRidingPassenger() instanceof EntityHuman);

            for(int j = 0; j < list.size(); ++j) {
                Entity entity = list.get(j);
                if (!entity.hasPassenger(this)) {
                    if (bl && this.getPassengers().size() < 2 && !entity.isPassenger() && entity.getWidth() < this.getWidth() && entity instanceof EntityLiving && !(entity instanceof EntityWaterAnimal) && !(entity instanceof EntityHuman)) {
                        entity.startRiding(this);
                    } else {
                        this.collide(entity);
                    }
                }
            }
        }

    }

    private void tickBubbleColumn() {
        if (this.level.isClientSide) {
            int i = this.getBubbleTime();
            if (i > 0) {
                this.bubbleMultiplier += 0.05F;
            } else {
                this.bubbleMultiplier -= 0.1F;
            }

            this.bubbleMultiplier = MathHelper.clamp(this.bubbleMultiplier, 0.0F, 1.0F);
            this.bubbleAngleO = this.bubbleAngle;
            this.bubbleAngle = 10.0F * (float)Math.sin((double)(0.5F * (float)this.level.getTime())) * this.bubbleMultiplier;
        } else {
            if (!this.isAboveBubbleColumn) {
                this.setBubbleTime(0);
            }

            int j = this.getBubbleTime();
            if (j > 0) {
                --j;
                this.setBubbleTime(j);
                int k = 60 - j - 1;
                if (k > 0 && j == 0) {
                    this.setBubbleTime(0);
                    Vec3D vec3 = this.getMot();
                    if (this.bubbleColumnDirectionIsDown) {
                        this.setMot(vec3.add(0.0D, -0.7D, 0.0D));
                        this.ejectPassengers();
                    } else {
                        this.setMot(vec3.x, this.hasPassenger((entity) -> {
                            return entity instanceof EntityHuman;
                        }) ? 2.7D : 0.6D, vec3.z);
                    }
                }

                this.isAboveBubbleColumn = false;
            }
        }

    }

    @Nullable
    protected SoundEffect getPaddleSound() {
        switch(this.getStatus()) {
        case IN_WATER:
        case UNDER_WATER:
        case UNDER_FLOWING_WATER:
            return SoundEffects.BOAT_PADDLE_WATER;
        case ON_LAND:
            return SoundEffects.BOAT_PADDLE_LAND;
        case IN_AIR:
        default:
            return null;
        }
    }

    private void tickLerp() {
        if (this.isControlledByLocalInstance()) {
            this.lerpSteps = 0;
            this.setPacketCoordinates(this.locX(), this.locY(), this.locZ());
        }

        if (this.lerpSteps > 0) {
            double d = this.locX() + (this.lerpX - this.locX()) / (double)this.lerpSteps;
            double e = this.locY() + (this.lerpY - this.locY()) / (double)this.lerpSteps;
            double f = this.locZ() + (this.lerpZ - this.locZ()) / (double)this.lerpSteps;
            double g = MathHelper.wrapDegrees(this.lerpYRot - (double)this.getYRot());
            this.setYRot(this.getYRot() + (float)g / (float)this.lerpSteps);
            this.setXRot(this.getXRot() + (float)(this.lerpXRot - (double)this.getXRot()) / (float)this.lerpSteps);
            --this.lerpSteps;
            this.setPosition(d, e, f);
            this.setYawPitch(this.getYRot(), this.getXRot());
        }
    }

    public void setPaddleState(boolean leftMoving, boolean rightMoving) {
        this.entityData.set(DATA_ID_PADDLE_LEFT, leftMoving);
        this.entityData.set(DATA_ID_PADDLE_RIGHT, rightMoving);
    }

    public float getRowingTime(int paddle, float tickDelta) {
        return this.getPaddleState(paddle) ? (float)MathHelper.clampedLerp((double)this.paddlePositions[paddle] - (double)((float)Math.PI / 8F), (double)this.paddlePositions[paddle], (double)tickDelta) : 0.0F;
    }

    private EntityBoat.EnumStatus getStatus() {
        EntityBoat.EnumStatus status = this.isUnderwater();
        if (status != null) {
            this.waterLevel = this.getBoundingBox().maxY;
            return status;
        } else if (this.checkInWater()) {
            return EntityBoat.EnumStatus.IN_WATER;
        } else {
            float f = this.getGroundFriction();
            if (f > 0.0F) {
                this.landFriction = f;
                return EntityBoat.EnumStatus.ON_LAND;
            } else {
                return EntityBoat.EnumStatus.IN_AIR;
            }
        }
    }

    public float getWaterLevelAbove() {
        AxisAlignedBB aABB = this.getBoundingBox();
        int i = MathHelper.floor(aABB.minX);
        int j = MathHelper.ceil(aABB.maxX);
        int k = MathHelper.floor(aABB.maxY);
        int l = MathHelper.ceil(aABB.maxY - this.lastYd);
        int m = MathHelper.floor(aABB.minZ);
        int n = MathHelper.ceil(aABB.maxZ);
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

        label39:
        for(int o = k; o < l; ++o) {
            float f = 0.0F;

            for(int p = i; p < j; ++p) {
                for(int q = m; q < n; ++q) {
                    mutableBlockPos.set(p, o, q);
                    Fluid fluidState = this.level.getFluid(mutableBlockPos);
                    if (fluidState.is(TagsFluid.WATER)) {
                        f = Math.max(f, fluidState.getHeight(this.level, mutableBlockPos));
                    }

                    if (f >= 1.0F) {
                        continue label39;
                    }
                }
            }

            if (f < 1.0F) {
                return (float)mutableBlockPos.getY() + f;
            }
        }

        return (float)(l + 1);
    }

    public float getGroundFriction() {
        AxisAlignedBB aABB = this.getBoundingBox();
        AxisAlignedBB aABB2 = new AxisAlignedBB(aABB.minX, aABB.minY - 0.001D, aABB.minZ, aABB.maxX, aABB.minY, aABB.maxZ);
        int i = MathHelper.floor(aABB2.minX) - 1;
        int j = MathHelper.ceil(aABB2.maxX) + 1;
        int k = MathHelper.floor(aABB2.minY) - 1;
        int l = MathHelper.ceil(aABB2.maxY) + 1;
        int m = MathHelper.floor(aABB2.minZ) - 1;
        int n = MathHelper.ceil(aABB2.maxZ) + 1;
        VoxelShape voxelShape = VoxelShapes.create(aABB2);
        float f = 0.0F;
        int o = 0;
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

        for(int p = i; p < j; ++p) {
            for(int q = m; q < n; ++q) {
                int r = (p != i && p != j - 1 ? 0 : 1) + (q != m && q != n - 1 ? 0 : 1);
                if (r != 2) {
                    for(int s = k; s < l; ++s) {
                        if (r <= 0 || s != k && s != l - 1) {
                            mutableBlockPos.set(p, s, q);
                            IBlockData blockState = this.level.getType(mutableBlockPos);
                            if (!(blockState.getBlock() instanceof BlockWaterLily) && VoxelShapes.joinIsNotEmpty(blockState.getCollisionShape(this.level, mutableBlockPos).move((double)p, (double)s, (double)q), voxelShape, OperatorBoolean.AND)) {
                                f += blockState.getBlock().getFrictionFactor();
                                ++o;
                            }
                        }
                    }
                }
            }
        }

        return f / (float)o;
    }

    private boolean checkInWater() {
        AxisAlignedBB aABB = this.getBoundingBox();
        int i = MathHelper.floor(aABB.minX);
        int j = MathHelper.ceil(aABB.maxX);
        int k = MathHelper.floor(aABB.minY);
        int l = MathHelper.ceil(aABB.minY + 0.001D);
        int m = MathHelper.floor(aABB.minZ);
        int n = MathHelper.ceil(aABB.maxZ);
        boolean bl = false;
        this.waterLevel = -Double.MAX_VALUE;
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

        for(int o = i; o < j; ++o) {
            for(int p = k; p < l; ++p) {
                for(int q = m; q < n; ++q) {
                    mutableBlockPos.set(o, p, q);
                    Fluid fluidState = this.level.getFluid(mutableBlockPos);
                    if (fluidState.is(TagsFluid.WATER)) {
                        float f = (float)p + fluidState.getHeight(this.level, mutableBlockPos);
                        this.waterLevel = Math.max((double)f, this.waterLevel);
                        bl |= aABB.minY < (double)f;
                    }
                }
            }
        }

        return bl;
    }

    @Nullable
    private EntityBoat.EnumStatus isUnderwater() {
        AxisAlignedBB aABB = this.getBoundingBox();
        double d = aABB.maxY + 0.001D;
        int i = MathHelper.floor(aABB.minX);
        int j = MathHelper.ceil(aABB.maxX);
        int k = MathHelper.floor(aABB.maxY);
        int l = MathHelper.ceil(d);
        int m = MathHelper.floor(aABB.minZ);
        int n = MathHelper.ceil(aABB.maxZ);
        boolean bl = false;
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

        for(int o = i; o < j; ++o) {
            for(int p = k; p < l; ++p) {
                for(int q = m; q < n; ++q) {
                    mutableBlockPos.set(o, p, q);
                    Fluid fluidState = this.level.getFluid(mutableBlockPos);
                    if (fluidState.is(TagsFluid.WATER) && d < (double)((float)mutableBlockPos.getY() + fluidState.getHeight(this.level, mutableBlockPos))) {
                        if (!fluidState.isSource()) {
                            return EntityBoat.EnumStatus.UNDER_FLOWING_WATER;
                        }

                        bl = true;
                    }
                }
            }
        }

        return bl ? EntityBoat.EnumStatus.UNDER_WATER : null;
    }

    private void floatBoat() {
        double d = (double)-0.04F;
        double e = this.isNoGravity() ? 0.0D : (double)-0.04F;
        double f = 0.0D;
        this.invFriction = 0.05F;
        if (this.oldStatus == EntityBoat.EnumStatus.IN_AIR && this.status != EntityBoat.EnumStatus.IN_AIR && this.status != EntityBoat.EnumStatus.ON_LAND) {
            this.waterLevel = this.getY(1.0D);
            this.setPosition(this.locX(), (double)(this.getWaterLevelAbove() - this.getHeight()) + 0.101D, this.locZ());
            this.setMot(this.getMot().multiply(1.0D, 0.0D, 1.0D));
            this.lastYd = 0.0D;
            this.status = EntityBoat.EnumStatus.IN_WATER;
        } else {
            if (this.status == EntityBoat.EnumStatus.IN_WATER) {
                f = (this.waterLevel - this.locY()) / (double)this.getHeight();
                this.invFriction = 0.9F;
            } else if (this.status == EntityBoat.EnumStatus.UNDER_FLOWING_WATER) {
                e = -7.0E-4D;
                this.invFriction = 0.9F;
            } else if (this.status == EntityBoat.EnumStatus.UNDER_WATER) {
                f = (double)0.01F;
                this.invFriction = 0.45F;
            } else if (this.status == EntityBoat.EnumStatus.IN_AIR) {
                this.invFriction = 0.9F;
            } else if (this.status == EntityBoat.EnumStatus.ON_LAND) {
                this.invFriction = this.landFriction;
                if (this.getRidingPassenger() instanceof EntityHuman) {
                    this.landFriction /= 2.0F;
                }
            }

            Vec3D vec3 = this.getMot();
            this.setMot(vec3.x * (double)this.invFriction, vec3.y + e, vec3.z * (double)this.invFriction);
            this.deltaRotation *= this.invFriction;
            if (f > 0.0D) {
                Vec3D vec32 = this.getMot();
                this.setMot(vec32.x, (vec32.y + f * 0.06153846016296973D) * 0.75D, vec32.z);
            }
        }

    }

    private void controlBoat() {
        if (this.isVehicle()) {
            float f = 0.0F;
            if (this.inputLeft) {
                --this.deltaRotation;
            }

            if (this.inputRight) {
                ++this.deltaRotation;
            }

            if (this.inputRight != this.inputLeft && !this.inputUp && !this.inputDown) {
                f += 0.005F;
            }

            this.setYRot(this.getYRot() + this.deltaRotation);
            if (this.inputUp) {
                f += 0.04F;
            }

            if (this.inputDown) {
                f -= 0.005F;
            }

            this.setMot(this.getMot().add((double)(MathHelper.sin(-this.getYRot() * ((float)Math.PI / 180F)) * f), 0.0D, (double)(MathHelper.cos(this.getYRot() * ((float)Math.PI / 180F)) * f)));
            this.setPaddleState(this.inputRight && !this.inputLeft || this.inputUp, this.inputLeft && !this.inputRight || this.inputUp);
        }
    }

    @Override
    public void positionRider(Entity passenger) {
        if (this.hasPassenger(passenger)) {
            float f = 0.0F;
            float g = (float)((this.isRemoved() ? (double)0.01F : this.getPassengersRidingOffset()) + passenger.getMyRidingOffset());
            if (this.getPassengers().size() > 1) {
                int i = this.getPassengers().indexOf(passenger);
                if (i == 0) {
                    f = 0.2F;
                } else {
                    f = -0.6F;
                }

                if (passenger instanceof EntityAnimal) {
                    f = (float)((double)f + 0.2D);
                }
            }

            Vec3D vec3 = (new Vec3D((double)f, 0.0D, 0.0D)).yRot(-this.getYRot() * ((float)Math.PI / 180F) - ((float)Math.PI / 2F));
            passenger.setPosition(this.locX() + vec3.x, this.locY() + (double)g, this.locZ() + vec3.z);
            passenger.setYRot(passenger.getYRot() + this.deltaRotation);
            passenger.setHeadRotation(passenger.getHeadRotation() + this.deltaRotation);
            this.clampRotation(passenger);
            if (passenger instanceof EntityAnimal && this.getPassengers().size() > 1) {
                int j = passenger.getId() % 2 == 0 ? 90 : 270;
                passenger.setYBodyRot(((EntityAnimal)passenger).yBodyRot + (float)j);
                passenger.setHeadRotation(passenger.getHeadRotation() + (float)j);
            }

        }
    }

    @Override
    public Vec3D getDismountLocationForPassenger(EntityLiving passenger) {
        Vec3D vec3 = getCollisionHorizontalEscapeVector((double)(this.getWidth() * MathHelper.SQRT_OF_TWO), (double)passenger.getWidth(), passenger.getYRot());
        double d = this.locX() + vec3.x;
        double e = this.locZ() + vec3.z;
        BlockPosition blockPos = new BlockPosition(d, this.getBoundingBox().maxY, e);
        BlockPosition blockPos2 = blockPos.below();
        if (!this.level.isWaterAt(blockPos2)) {
            List<Vec3D> list = Lists.newArrayList();
            double f = this.level.getBlockFloorHeight(blockPos);
            if (DismountUtil.isBlockFloorValid(f)) {
                list.add(new Vec3D(d, (double)blockPos.getY() + f, e));
            }

            double g = this.level.getBlockFloorHeight(blockPos2);
            if (DismountUtil.isBlockFloorValid(g)) {
                list.add(new Vec3D(d, (double)blockPos2.getY() + g, e));
            }

            for(EntityPose pose : passenger.getDismountPoses()) {
                for(Vec3D vec32 : list) {
                    if (DismountUtil.canDismountTo(this.level, vec32, passenger, pose)) {
                        passenger.setPose(pose);
                        return vec32;
                    }
                }
            }
        }

        return super.getDismountLocationForPassenger(passenger);
    }

    protected void clampRotation(Entity entity) {
        entity.setYBodyRot(this.getYRot());
        float f = MathHelper.wrapDegrees(entity.getYRot() - this.getYRot());
        float g = MathHelper.clamp(f, -105.0F, 105.0F);
        entity.yRotO += g - f;
        entity.setYRot(entity.getYRot() + g - f);
        entity.setHeadRotation(entity.getYRot());
    }

    @Override
    public void onPassengerTurned(Entity passenger) {
        this.clampRotation(passenger);
    }

    @Override
    protected void saveData(NBTTagCompound nbt) {
        nbt.setString("Type", this.getType().getName());
    }

    @Override
    protected void loadData(NBTTagCompound nbt) {
        if (nbt.hasKeyOfType("Type", 8)) {
            this.setType(EntityBoat.EnumBoatType.byName(nbt.getString("Type")));
        }

    }

    @Override
    public EnumInteractionResult interact(EntityHuman player, EnumHand hand) {
        if (player.isSecondaryUseActive()) {
            return EnumInteractionResult.PASS;
        } else if (this.outOfControlTicks < 60.0F) {
            if (!this.level.isClientSide) {
                return player.startRiding(this) ? EnumInteractionResult.CONSUME : EnumInteractionResult.PASS;
            } else {
                return EnumInteractionResult.SUCCESS;
            }
        } else {
            return EnumInteractionResult.PASS;
        }
    }

    @Override
    protected void checkFallDamage(double heightDifference, boolean onGround, IBlockData landedState, BlockPosition landedPosition) {
        this.lastYd = this.getMot().y;
        if (!this.isPassenger()) {
            if (onGround) {
                if (this.fallDistance > 3.0F) {
                    if (this.status != EntityBoat.EnumStatus.ON_LAND) {
                        this.fallDistance = 0.0F;
                        return;
                    }

                    this.causeFallDamage(this.fallDistance, 1.0F, DamageSource.FALL);
                    if (!this.level.isClientSide && !this.isRemoved()) {
                        this.killEntity();
                        if (this.level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
                            for(int i = 0; i < 3; ++i) {
                                this.spawnAtLocation(this.getType().getPlanks());
                            }

                            for(int j = 0; j < 2; ++j) {
                                this.spawnAtLocation(Items.STICK);
                            }
                        }
                    }
                }

                this.fallDistance = 0.0F;
            } else if (!this.level.getFluid(this.getChunkCoordinates().below()).is(TagsFluid.WATER) && heightDifference < 0.0D) {
                this.fallDistance = (float)((double)this.fallDistance - heightDifference);
            }

        }
    }

    public boolean getPaddleState(int paddle) {
        return this.entityData.<Boolean>get(paddle == 0 ? DATA_ID_PADDLE_LEFT : DATA_ID_PADDLE_RIGHT) && this.getRidingPassenger() != null;
    }

    public void setDamage(float wobbleStrength) {
        this.entityData.set(DATA_ID_DAMAGE, wobbleStrength);
    }

    public float getDamage() {
        return this.entityData.get(DATA_ID_DAMAGE);
    }

    public void setHurtTime(int wobbleTicks) {
        this.entityData.set(DATA_ID_HURT, wobbleTicks);
    }

    public int getHurtTime() {
        return this.entityData.get(DATA_ID_HURT);
    }

    private void setBubbleTime(int wobbleTicks) {
        this.entityData.set(DATA_ID_BUBBLE_TIME, wobbleTicks);
    }

    private int getBubbleTime() {
        return this.entityData.get(DATA_ID_BUBBLE_TIME);
    }

    public float getBubbleAngle(float tickDelta) {
        return MathHelper.lerp(tickDelta, this.bubbleAngleO, this.bubbleAngle);
    }

    public void setHurtDir(int side) {
        this.entityData.set(DATA_ID_HURTDIR, side);
    }

    public int getHurtDir() {
        return this.entityData.get(DATA_ID_HURTDIR);
    }

    public void setType(EntityBoat.EnumBoatType type) {
        this.entityData.set(DATA_ID_TYPE, type.ordinal());
    }

    public EntityBoat.EnumBoatType getType() {
        return EntityBoat.EnumBoatType.byId(this.entityData.get(DATA_ID_TYPE));
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().size() < 2 && !this.isEyeInFluid(TagsFluid.WATER);
    }

    @Nullable
    @Override
    public Entity getRidingPassenger() {
        return this.getFirstPassenger();
    }

    public void setInput(boolean pressingLeft, boolean pressingRight, boolean pressingForward, boolean pressingBack) {
        this.inputLeft = pressingLeft;
        this.inputRight = pressingRight;
        this.inputUp = pressingForward;
        this.inputDown = pressingBack;
    }

    @Override
    public Packet<?> getPacket() {
        return new PacketPlayOutSpawnEntity(this);
    }

    @Override
    public boolean isUnderWater() {
        return this.status == EntityBoat.EnumStatus.UNDER_WATER || this.status == EntityBoat.EnumStatus.UNDER_FLOWING_WATER;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(this.getDropItem());
    }

    public static enum EnumBoatType {
        OAK(Blocks.OAK_PLANKS, "oak"),
        SPRUCE(Blocks.SPRUCE_PLANKS, "spruce"),
        BIRCH(Blocks.BIRCH_PLANKS, "birch"),
        JUNGLE(Blocks.JUNGLE_PLANKS, "jungle"),
        ACACIA(Blocks.ACACIA_PLANKS, "acacia"),
        DARK_OAK(Blocks.DARK_OAK_PLANKS, "dark_oak");

        private final String name;
        private final Block planks;

        private EnumBoatType(Block baseBlock, String name) {
            this.name = name;
            this.planks = baseBlock;
        }

        public String getName() {
            return this.name;
        }

        public Block getPlanks() {
            return this.planks;
        }

        @Override
        public String toString() {
            return this.name;
        }

        public static EntityBoat.EnumBoatType byId(int type) {
            EntityBoat.EnumBoatType[] types = values();
            if (type < 0 || type >= types.length) {
                type = 0;
            }

            return types[type];
        }

        public static EntityBoat.EnumBoatType byName(String name) {
            EntityBoat.EnumBoatType[] types = values();

            for(int i = 0; i < types.length; ++i) {
                if (types[i].getName().equals(name)) {
                    return types[i];
                }
            }

            return types[0];
        }
    }

    public static enum EnumStatus {
        IN_WATER,
        UNDER_WATER,
        UNDER_FLOWING_WATER,
        ON_LAND,
        IN_AIR;
    }
}
