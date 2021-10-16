package net.minecraft.world.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.objects.Object2DoubleArrayMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.BlockUtil;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportSystemDetails;
import net.minecraft.ReportedException;
import net.minecraft.SystemUtils;
import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.ICommandListener;
import net.minecraft.commands.arguments.ArgumentAnchor;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.particles.ParticleParamBlock;
import net.minecraft.core.particles.Particles;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.chat.ChatClickable;
import net.minecraft.network.chat.ChatHoverable;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.chat.IChatMutableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntity;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagsBlock;
import net.minecraft.tags.TagsEntity;
import net.minecraft.tags.TagsFluid;
import net.minecraft.util.MathHelper;
import net.minecraft.util.StreamAccumulator;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.INamableTileEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.vehicle.EntityBoat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentManager;
import net.minecraft.world.item.enchantment.EnchantmentProtection;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IMaterial;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.RayTrace;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockFenceGate;
import net.minecraft.world.level.block.BlockHoney;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnumBlockMirror;
import net.minecraft.world.level.block.EnumBlockRotation;
import net.minecraft.world.level.block.EnumRenderType;
import net.minecraft.world.level.block.SoundEffectType;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.DimensionManager;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.IEntityCallback;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListenerRegistrar;
import net.minecraft.world.level.levelgen.HeightMap;
import net.minecraft.world.level.material.EnumPistonReaction;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidType;
import net.minecraft.world.level.portal.BlockPortalShape;
import net.minecraft.world.level.portal.ShapeDetectorShape;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.MovingObjectPosition;
import net.minecraft.world.phys.Vec2F;
import net.minecraft.world.phys.Vec3D;
import net.minecraft.world.phys.shapes.OperatorBoolean;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.phys.shapes.VoxelShapes;
import net.minecraft.world.scores.ScoreboardTeam;
import net.minecraft.world.scores.ScoreboardTeamBase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class Entity implements INamableTileEntity, EntityAccess, ICommandListener {
    protected static final Logger LOGGER = LogManager.getLogger();
    public static final String ID_TAG = "id";
    public static final String PASSENGERS_TAG = "Passengers";
    private static final AtomicInteger ENTITY_COUNTER = new AtomicInteger();
    private static final List<ItemStack> EMPTY_LIST = Collections.emptyList();
    public static final int BOARDING_COOLDOWN = 60;
    public static final int TOTAL_AIR_SUPPLY = 300;
    public static final int MAX_ENTITY_TAG_COUNT = 1024;
    public static final double DELTA_AFFECTED_BY_BLOCKS_BELOW = 0.5000001D;
    public static final float BREATHING_DISTANCE_BELOW_EYES = 0.11111111F;
    public static final int BASE_TICKS_REQUIRED_TO_FREEZE = 140;
    public static final int FREEZE_HURT_FREQUENCY = 40;
    private static final AxisAlignedBB INITIAL_AABB = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
    private static final double WATER_FLOW_SCALE = 0.014D;
    private static final double LAVA_FAST_FLOW_SCALE = 0.007D;
    private static final double LAVA_SLOW_FLOW_SCALE = 0.0023333333333333335D;
    public static final String UUID_TAG = "UUID";
    private static double viewScale = 1.0D;
    private final EntityTypes<?> type;
    private int id = ENTITY_COUNTER.incrementAndGet();
    public boolean blocksBuilding;
    public ImmutableList<Entity> passengers = ImmutableList.of();
    protected int boardingCooldown;
    @Nullable
    private Entity vehicle;
    public World level;
    public double xo;
    public double yo;
    public double zo;
    private Vec3D position;
    private BlockPosition blockPosition;
    private Vec3D deltaMovement = Vec3D.ZERO;
    private float yRot;
    private float xRot;
    public float yRotO;
    public float xRotO;
    private AxisAlignedBB bb = INITIAL_AABB;
    protected boolean onGround;
    public boolean horizontalCollision;
    public boolean verticalCollision;
    public boolean hurtMarked;
    protected Vec3D stuckSpeedMultiplier = Vec3D.ZERO;
    @Nullable
    private Entity.RemovalReason removalReason;
    public static final float DEFAULT_BB_WIDTH = 0.6F;
    public static final float DEFAULT_BB_HEIGHT = 1.8F;
    public float walkDistO;
    public float walkDist;
    public float moveDist;
    public float flyDist;
    public float fallDistance;
    private float nextStep = 1.0F;
    public double xOld;
    public double yOld;
    public double zOld;
    public float maxUpStep;
    public boolean noPhysics;
    protected final Random random = new Random();
    public int tickCount;
    public int remainingFireTicks = -this.getMaxFireTicks();
    public boolean wasTouchingWater;
    protected Object2DoubleMap<Tag<FluidType>> fluidHeight = new Object2DoubleArrayMap<>(2);
    protected boolean wasEyeInWater;
    @Nullable
    protected Tag<FluidType> fluidOnEyes;
    public int invulnerableTime;
    protected boolean firstTick = true;
    protected final DataWatcher entityData;
    protected static final DataWatcherObject<Byte> DATA_SHARED_FLAGS_ID = DataWatcher.defineId(Entity.class, DataWatcherRegistry.BYTE);
    protected static final int FLAG_ONFIRE = 0;
    private static final int FLAG_SHIFT_KEY_DOWN = 1;
    private static final int FLAG_SPRINTING = 3;
    private static final int FLAG_SWIMMING = 4;
    private static final int FLAG_INVISIBLE = 5;
    protected static final int FLAG_GLOWING = 6;
    protected static final int FLAG_FALL_FLYING = 7;
    private static final DataWatcherObject<Integer> DATA_AIR_SUPPLY_ID = DataWatcher.defineId(Entity.class, DataWatcherRegistry.INT);
    private static final DataWatcherObject<Optional<IChatBaseComponent>> DATA_CUSTOM_NAME = DataWatcher.defineId(Entity.class, DataWatcherRegistry.OPTIONAL_COMPONENT);
    private static final DataWatcherObject<Boolean> DATA_CUSTOM_NAME_VISIBLE = DataWatcher.defineId(Entity.class, DataWatcherRegistry.BOOLEAN);
    private static final DataWatcherObject<Boolean> DATA_SILENT = DataWatcher.defineId(Entity.class, DataWatcherRegistry.BOOLEAN);
    private static final DataWatcherObject<Boolean> DATA_NO_GRAVITY = DataWatcher.defineId(Entity.class, DataWatcherRegistry.BOOLEAN);
    protected static final DataWatcherObject<EntityPose> DATA_POSE = DataWatcher.defineId(Entity.class, DataWatcherRegistry.POSE);
    private static final DataWatcherObject<Integer> DATA_TICKS_FROZEN = DataWatcher.defineId(Entity.class, DataWatcherRegistry.INT);
    private IEntityCallback levelCallback = IEntityCallback.NULL;
    private Vec3D packetCoordinates;
    public boolean noCulling;
    public boolean hasImpulse;
    public int portalCooldown;
    public boolean isInsidePortal;
    protected int portalTime;
    protected BlockPosition portalEntrancePos;
    private boolean invulnerable;
    protected UUID uuid = MathHelper.createInsecureUUID(this.random);
    protected String stringUUID = this.uuid.toString();
    private boolean hasGlowingTag;
    private final Set<String> tags = Sets.newHashSet();
    private final double[] pistonDeltas = new double[]{0.0D, 0.0D, 0.0D};
    private long pistonDeltasGameTime;
    private EntitySize dimensions;
    private float eyeHeight;
    public boolean isInPowderSnow;
    public boolean wasInPowderSnow;
    public boolean wasOnFire;
    private float crystalSoundIntensity;
    private int lastCrystalSoundPlayTick;
    public boolean hasVisualFire;

    public Entity(EntityTypes<?> type, World world) {
        this.type = type;
        this.level = world;
        this.dimensions = type.getDimensions();
        this.position = Vec3D.ZERO;
        this.blockPosition = BlockPosition.ZERO;
        this.packetCoordinates = Vec3D.ZERO;
        this.entityData = new DataWatcher(this);
        this.entityData.register(DATA_SHARED_FLAGS_ID, (byte)0);
        this.entityData.register(DATA_AIR_SUPPLY_ID, this.getMaxAirSupply());
        this.entityData.register(DATA_CUSTOM_NAME_VISIBLE, false);
        this.entityData.register(DATA_CUSTOM_NAME, Optional.empty());
        this.entityData.register(DATA_SILENT, false);
        this.entityData.register(DATA_NO_GRAVITY, false);
        this.entityData.register(DATA_POSE, EntityPose.STANDING);
        this.entityData.register(DATA_TICKS_FROZEN, 0);
        this.initDatawatcher();
        this.setPosition(0.0D, 0.0D, 0.0D);
        this.eyeHeight = this.getHeadHeight(EntityPose.STANDING, this.dimensions);
    }

    public boolean isColliding(BlockPosition pos, IBlockData state) {
        VoxelShape voxelShape = state.getCollisionShape(this.level, pos, VoxelShapeCollision.of(this));
        VoxelShape voxelShape2 = voxelShape.move((double)pos.getX(), (double)pos.getY(), (double)pos.getZ());
        return VoxelShapes.joinIsNotEmpty(voxelShape2, VoxelShapes.create(this.getBoundingBox()), OperatorBoolean.AND);
    }

    public int getTeamColor() {
        ScoreboardTeamBase team = this.getScoreboardTeam();
        return team != null && team.getColor().getColor() != null ? team.getColor().getColor() : 16777215;
    }

    public boolean isSpectator() {
        return false;
    }

    public final void decouple() {
        if (this.isVehicle()) {
            this.ejectPassengers();
        }

        if (this.isPassenger()) {
            this.stopRiding();
        }

    }

    public void setPacketCoordinates(double x, double y, double z) {
        this.setPacketCoordinates(new Vec3D(x, y, z));
    }

    public void setPacketCoordinates(Vec3D pos) {
        this.packetCoordinates = pos;
    }

    public Vec3D getPacketCoordinates() {
        return this.packetCoordinates;
    }

    public EntityTypes<?> getEntityType() {
        return this.type;
    }

    @Override
    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Set<String> getScoreboardTags() {
        return this.tags;
    }

    public boolean addScoreboardTag(String tag) {
        return this.tags.size() >= 1024 ? false : this.tags.add(tag);
    }

    public boolean removeScoreboardTag(String tag) {
        return this.tags.remove(tag);
    }

    public void killEntity() {
        this.remove(Entity.RemovalReason.KILLED);
    }

    public final void die() {
        this.remove(Entity.RemovalReason.DISCARDED);
    }

    protected abstract void initDatawatcher();

    public DataWatcher getDataWatcher() {
        return this.entityData;
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof Entity) {
            return ((Entity)object).id == this.id;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.id;
    }

    public void remove(Entity.RemovalReason reason) {
        this.setRemoved(reason);
        if (reason == Entity.RemovalReason.KILLED) {
            this.gameEvent(GameEvent.ENTITY_KILLED);
        }

    }

    public void onClientRemoval() {
    }

    public void setPose(EntityPose pose) {
        this.entityData.set(DATA_POSE, pose);
    }

    public EntityPose getPose() {
        return this.entityData.get(DATA_POSE);
    }

    public boolean closerThan(Entity other, double radius) {
        double d = other.position.x - this.position.x;
        double e = other.position.y - this.position.y;
        double f = other.position.z - this.position.z;
        return d * d + e * e + f * f < radius * radius;
    }

    public void setYawPitch(float yaw, float pitch) {
        this.setYRot(yaw % 360.0F);
        this.setXRot(pitch % 360.0F);
    }

    public final void setPos(Vec3D pos) {
        this.setPosition(pos.getX(), pos.getY(), pos.getZ());
    }

    public void setPosition(double x, double y, double z) {
        this.setPositionRaw(x, y, z);
        this.setBoundingBox(this.makeBoundingBox());
    }

    protected AxisAlignedBB makeBoundingBox() {
        return this.dimensions.makeBoundingBox(this.position);
    }

    protected void reapplyPosition() {
        this.setPosition(this.position.x, this.position.y, this.position.z);
    }

    public void turn(double cursorDeltaX, double cursorDeltaY) {
        float f = (float)cursorDeltaY * 0.15F;
        float g = (float)cursorDeltaX * 0.15F;
        this.setXRot(this.getXRot() + f);
        this.setYRot(this.getYRot() + g);
        this.setXRot(MathHelper.clamp(this.getXRot(), -90.0F, 90.0F));
        this.xRotO += f;
        this.yRotO += g;
        this.xRotO = MathHelper.clamp(this.xRotO, -90.0F, 90.0F);
        if (this.vehicle != null) {
            this.vehicle.onPassengerTurned(this);
        }

    }

    public void tick() {
        this.entityBaseTick();
    }

    public void entityBaseTick() {
        this.level.getMethodProfiler().enter("entityBaseTick");
        if (this.isPassenger() && this.getVehicle().isRemoved()) {
            this.stopRiding();
        }

        if (this.boardingCooldown > 0) {
            --this.boardingCooldown;
        }

        this.walkDistO = this.walkDist;
        this.xRotO = this.getXRot();
        this.yRotO = this.getYRot();
        this.doPortalTick();
        if (this.canSpawnSprintParticle()) {
            this.spawnSprintParticle();
        }

        this.wasInPowderSnow = this.isInPowderSnow;
        this.isInPowderSnow = false;
        this.updateInWaterStateAndDoFluidPushing();
        this.updateFluidOnEyes();
        this.updateSwimming();
        if (this.level.isClientSide) {
            this.extinguish();
        } else if (this.remainingFireTicks > 0) {
            if (this.isFireProof()) {
                this.setFireTicks(this.remainingFireTicks - 4);
                if (this.remainingFireTicks < 0) {
                    this.extinguish();
                }
            } else {
                if (this.remainingFireTicks % 20 == 0 && !this.isInLava()) {
                    this.damageEntity(DamageSource.ON_FIRE, 1.0F);
                }

                this.setFireTicks(this.remainingFireTicks - 1);
            }

            if (this.getTicksFrozen() > 0) {
                this.setTicksFrozen(0);
                this.level.triggerEffect((EntityHuman)null, 1009, this.blockPosition, 1);
            }
        }

        if (this.isInLava()) {
            this.burnFromLava();
            this.fallDistance *= 0.5F;
        }

        this.checkOutOfWorld();
        if (!this.level.isClientSide) {
            this.setSharedFlagOnFire(this.remainingFireTicks > 0);
        }

        this.firstTick = false;
        this.level.getMethodProfiler().exit();
    }

    public void setSharedFlagOnFire(boolean onFire) {
        this.setFlag(0, onFire || this.hasVisualFire);
    }

    public void checkOutOfWorld() {
        if (this.locY() < (double)(this.level.getMinBuildHeight() - 64)) {
            this.outOfWorld();
        }

    }

    public void resetPortalCooldown() {
        this.portalCooldown = this.getDefaultPortalCooldown();
    }

    public boolean isOnPortalCooldown() {
        return this.portalCooldown > 0;
    }

    protected void processPortalCooldown() {
        if (this.isOnPortalCooldown()) {
            --this.portalCooldown;
        }

    }

    public int getPortalWaitTime() {
        return 0;
    }

    public void burnFromLava() {
        if (!this.isFireProof()) {
            this.setOnFire(15);
            if (this.damageEntity(DamageSource.LAVA, 4.0F)) {
                this.playSound(SoundEffects.GENERIC_BURN, 0.4F, 2.0F + this.random.nextFloat() * 0.4F);
            }

        }
    }

    public void setOnFire(int seconds) {
        int i = seconds * 20;
        if (this instanceof EntityLiving) {
            i = EnchantmentProtection.getFireAfterDampener((EntityLiving)this, i);
        }

        if (this.remainingFireTicks < i) {
            this.setFireTicks(i);
        }

    }

    public void setFireTicks(int ticks) {
        this.remainingFireTicks = ticks;
    }

    public int getFireTicks() {
        return this.remainingFireTicks;
    }

    public void extinguish() {
        this.setFireTicks(0);
    }

    protected void outOfWorld() {
        this.die();
    }

    public boolean isFree(double offsetX, double offsetY, double offsetZ) {
        return this.isFree(this.getBoundingBox().move(offsetX, offsetY, offsetZ));
    }

    private boolean isFree(AxisAlignedBB box) {
        return this.level.getCubes(this, box) && !this.level.containsLiquid(box);
    }

    public void setOnGround(boolean onGround) {
        this.onGround = onGround;
    }

    public boolean isOnGround() {
        return this.onGround;
    }

    public void move(EnumMoveType movementType, Vec3D movement) {
        if (this.noPhysics) {
            this.setPosition(this.locX() + movement.x, this.locY() + movement.y, this.locZ() + movement.z);
        } else {
            this.wasOnFire = this.isBurning();
            if (movementType == EnumMoveType.PISTON) {
                movement = this.limitPistonMovement(movement);
                if (movement.equals(Vec3D.ZERO)) {
                    return;
                }
            }

            this.level.getMethodProfiler().enter("move");
            if (this.stuckSpeedMultiplier.lengthSqr() > 1.0E-7D) {
                movement = movement.multiply(this.stuckSpeedMultiplier);
                this.stuckSpeedMultiplier = Vec3D.ZERO;
                this.setMot(Vec3D.ZERO);
            }

            movement = this.maybeBackOffFromEdge(movement, movementType);
            Vec3D vec3 = this.collide(movement);
            if (vec3.lengthSqr() > 1.0E-7D) {
                this.setPosition(this.locX() + vec3.x, this.locY() + vec3.y, this.locZ() + vec3.z);
            }

            this.level.getMethodProfiler().exit();
            this.level.getMethodProfiler().enter("rest");
            this.horizontalCollision = !MathHelper.equal(movement.x, vec3.x) || !MathHelper.equal(movement.z, vec3.z);
            this.verticalCollision = movement.y != vec3.y;
            this.onGround = this.verticalCollision && movement.y < 0.0D;
            BlockPosition blockPos = this.getOnPos();
            IBlockData blockState = this.level.getType(blockPos);
            this.checkFallDamage(vec3.y, this.onGround, blockState, blockPos);
            if (this.isRemoved()) {
                this.level.getMethodProfiler().exit();
            } else {
                Vec3D vec32 = this.getMot();
                if (movement.x != vec3.x) {
                    this.setMot(0.0D, vec32.y, vec32.z);
                }

                if (movement.z != vec3.z) {
                    this.setMot(vec32.x, vec32.y, 0.0D);
                }

                Block block = blockState.getBlock();
                if (movement.y != vec3.y) {
                    block.updateEntityAfterFallOn(this.level, this);
                }

                if (this.onGround && !this.isSteppingCarefully()) {
                    block.stepOn(this.level, blockPos, blockState, this);
                }

                Entity.MovementEmission movementEmission = this.getMovementEmission();
                if (movementEmission.emitsAnything() && !this.isPassenger()) {
                    double d = vec3.x;
                    double e = vec3.y;
                    double f = vec3.z;
                    this.flyDist = (float)((double)this.flyDist + vec3.length() * 0.6D);
                    if (!blockState.is(TagsBlock.CLIMBABLE) && !blockState.is(Blocks.POWDER_SNOW)) {
                        e = 0.0D;
                    }

                    this.walkDist += (float)vec3.horizontalDistance() * 0.6F;
                    this.moveDist += (float)Math.sqrt(d * d + e * e + f * f) * 0.6F;
                    if (this.moveDist > this.nextStep && !blockState.isAir()) {
                        this.nextStep = this.nextStep();
                        if (this.isInWater()) {
                            if (movementEmission.emitsSounds()) {
                                Entity entity = this.isVehicle() && this.getRidingPassenger() != null ? this.getRidingPassenger() : this;
                                float g = entity == this ? 0.35F : 0.4F;
                                Vec3D vec33 = entity.getMot();
                                float h = Math.min(1.0F, (float)Math.sqrt(vec33.x * vec33.x * (double)0.2F + vec33.y * vec33.y + vec33.z * vec33.z * (double)0.2F) * g);
                                this.playSwimSound(h);
                            }

                            if (movementEmission.emitsEvents()) {
                                this.gameEvent(GameEvent.SWIM);
                            }
                        } else {
                            if (movementEmission.emitsSounds()) {
                                this.playAmethystStepSound(blockState);
                                this.playStepSound(blockPos, blockState);
                            }

                            if (movementEmission.emitsEvents() && !blockState.is(TagsBlock.OCCLUDES_VIBRATION_SIGNALS)) {
                                this.gameEvent(GameEvent.STEP);
                            }
                        }
                    } else if (blockState.isAir()) {
                        this.processFlappingMovement();
                    }
                }

                this.tryCheckInsideBlocks();
                float i = this.getBlockSpeedFactor();
                this.setMot(this.getMot().multiply((double)i, 1.0D, (double)i));
                if (this.level.getBlockStatesIfLoaded(this.getBoundingBox().shrink(1.0E-6D)).noneMatch((state) -> {
                    return state.is(TagsBlock.FIRE) || state.is(Blocks.LAVA);
                })) {
                    if (this.remainingFireTicks <= 0) {
                        this.setFireTicks(-this.getMaxFireTicks());
                    }

                    if (this.wasOnFire && (this.isInPowderSnow || this.isInWaterRainOrBubble())) {
                        this.playEntityOnFireExtinguishedSound();
                    }
                }

                if (this.isBurning() && (this.isInPowderSnow || this.isInWaterRainOrBubble())) {
                    this.setFireTicks(-this.getMaxFireTicks());
                }

                this.level.getMethodProfiler().exit();
            }
        }
    }

    protected void tryCheckInsideBlocks() {
        try {
            this.checkBlockCollisions();
        } catch (Throwable var4) {
            CrashReport crashReport = CrashReport.forThrowable(var4, "Checking entity block collision");
            CrashReportSystemDetails crashReportCategory = crashReport.addCategory("Entity being checked for collision");
            this.appendEntityCrashDetails(crashReportCategory);
            throw new ReportedException(crashReport);
        }
    }

    protected void playEntityOnFireExtinguishedSound() {
        this.playSound(SoundEffects.GENERIC_EXTINGUISH_FIRE, 0.7F, 1.6F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
    }

    protected void processFlappingMovement() {
        if (this.isFlapping()) {
            this.onFlap();
            if (this.getMovementEmission().emitsEvents()) {
                this.gameEvent(GameEvent.FLAP);
            }
        }

    }

    public BlockPosition getOnPos() {
        int i = MathHelper.floor(this.position.x);
        int j = MathHelper.floor(this.position.y - (double)0.2F);
        int k = MathHelper.floor(this.position.z);
        BlockPosition blockPos = new BlockPosition(i, j, k);
        if (this.level.getType(blockPos).isAir()) {
            BlockPosition blockPos2 = blockPos.below();
            IBlockData blockState = this.level.getType(blockPos2);
            if (blockState.is(TagsBlock.FENCES) || blockState.is(TagsBlock.WALLS) || blockState.getBlock() instanceof BlockFenceGate) {
                return blockPos2;
            }
        }

        return blockPos;
    }

    protected float getBlockJumpFactor() {
        float f = this.level.getType(this.getChunkCoordinates()).getBlock().getJumpFactor();
        float g = this.level.getType(this.getBlockPosBelowThatAffectsMyMovement()).getBlock().getJumpFactor();
        return (double)f == 1.0D ? g : f;
    }

    protected float getBlockSpeedFactor() {
        IBlockData blockState = this.level.getType(this.getChunkCoordinates());
        float f = blockState.getBlock().getSpeedFactor();
        if (!blockState.is(Blocks.WATER) && !blockState.is(Blocks.BUBBLE_COLUMN)) {
            return (double)f == 1.0D ? this.level.getType(this.getBlockPosBelowThatAffectsMyMovement()).getBlock().getSpeedFactor() : f;
        } else {
            return f;
        }
    }

    protected BlockPosition getBlockPosBelowThatAffectsMyMovement() {
        return new BlockPosition(this.position.x, this.getBoundingBox().minY - 0.5000001D, this.position.z);
    }

    protected Vec3D maybeBackOffFromEdge(Vec3D movement, EnumMoveType type) {
        return movement;
    }

    protected Vec3D limitPistonMovement(Vec3D movement) {
        if (movement.lengthSqr() <= 1.0E-7D) {
            return movement;
        } else {
            long l = this.level.getTime();
            if (l != this.pistonDeltasGameTime) {
                Arrays.fill(this.pistonDeltas, 0.0D);
                this.pistonDeltasGameTime = l;
            }

            if (movement.x != 0.0D) {
                double d = this.applyPistonMovementRestriction(EnumDirection.EnumAxis.X, movement.x);
                return Math.abs(d) <= (double)1.0E-5F ? Vec3D.ZERO : new Vec3D(d, 0.0D, 0.0D);
            } else if (movement.y != 0.0D) {
                double e = this.applyPistonMovementRestriction(EnumDirection.EnumAxis.Y, movement.y);
                return Math.abs(e) <= (double)1.0E-5F ? Vec3D.ZERO : new Vec3D(0.0D, e, 0.0D);
            } else if (movement.z != 0.0D) {
                double f = this.applyPistonMovementRestriction(EnumDirection.EnumAxis.Z, movement.z);
                return Math.abs(f) <= (double)1.0E-5F ? Vec3D.ZERO : new Vec3D(0.0D, 0.0D, f);
            } else {
                return Vec3D.ZERO;
            }
        }
    }

    private double applyPistonMovementRestriction(EnumDirection.EnumAxis axis, double offsetFactor) {
        int i = axis.ordinal();
        double d = MathHelper.clamp(offsetFactor + this.pistonDeltas[i], -0.51D, 0.51D);
        offsetFactor = d - this.pistonDeltas[i];
        this.pistonDeltas[i] = d;
        return offsetFactor;
    }

    private Vec3D collide(Vec3D movement) {
        AxisAlignedBB aABB = this.getBoundingBox();
        VoxelShapeCollision collisionContext = VoxelShapeCollision.of(this);
        VoxelShape voxelShape = this.level.getWorldBorder().getCollisionShape();
        Stream<VoxelShape> stream = VoxelShapes.joinIsNotEmpty(voxelShape, VoxelShapes.create(aABB.shrink(1.0E-7D)), OperatorBoolean.AND) ? Stream.empty() : Stream.of(voxelShape);
        Stream<VoxelShape> stream2 = this.level.getEntityCollisions(this, aABB.expandTowards(movement), (entity) -> {
            return true;
        });
        StreamAccumulator<VoxelShape> rewindableStream = new StreamAccumulator<>(Stream.concat(stream2, stream));
        Vec3D vec3 = movement.lengthSqr() == 0.0D ? movement : collideBoundingBoxHeuristically(this, movement, aABB, this.level, collisionContext, rewindableStream);
        boolean bl = movement.x != vec3.x;
        boolean bl2 = movement.y != vec3.y;
        boolean bl3 = movement.z != vec3.z;
        boolean bl4 = this.onGround || bl2 && movement.y < 0.0D;
        if (this.maxUpStep > 0.0F && bl4 && (bl || bl3)) {
            Vec3D vec32 = collideBoundingBoxHeuristically(this, new Vec3D(movement.x, (double)this.maxUpStep, movement.z), aABB, this.level, collisionContext, rewindableStream);
            Vec3D vec33 = collideBoundingBoxHeuristically(this, new Vec3D(0.0D, (double)this.maxUpStep, 0.0D), aABB.expandTowards(movement.x, 0.0D, movement.z), this.level, collisionContext, rewindableStream);
            if (vec33.y < (double)this.maxUpStep) {
                Vec3D vec34 = collideBoundingBoxHeuristically(this, new Vec3D(movement.x, 0.0D, movement.z), aABB.move(vec33), this.level, collisionContext, rewindableStream).add(vec33);
                if (vec34.horizontalDistanceSqr() > vec32.horizontalDistanceSqr()) {
                    vec32 = vec34;
                }
            }

            if (vec32.horizontalDistanceSqr() > vec3.horizontalDistanceSqr()) {
                return vec32.add(collideBoundingBoxHeuristically(this, new Vec3D(0.0D, -vec32.y + movement.y, 0.0D), aABB.move(vec32), this.level, collisionContext, rewindableStream));
            }
        }

        return vec3;
    }

    public static Vec3D collideBoundingBoxHeuristically(@Nullable Entity entity, Vec3D movement, AxisAlignedBB entityBoundingBox, World world, VoxelShapeCollision context, StreamAccumulator<VoxelShape> collisions) {
        boolean bl = movement.x == 0.0D;
        boolean bl2 = movement.y == 0.0D;
        boolean bl3 = movement.z == 0.0D;
        if ((!bl || !bl2) && (!bl || !bl3) && (!bl2 || !bl3)) {
            StreamAccumulator<VoxelShape> rewindableStream = new StreamAccumulator<>(Stream.concat(collisions.getStream(), world.getBlockCollisions(entity, entityBoundingBox.expandTowards(movement))));
            return collideBoundingBoxLegacy(movement, entityBoundingBox, rewindableStream);
        } else {
            return collideBoundingBox(movement, entityBoundingBox, world, context, collisions);
        }
    }

    public static Vec3D collideBoundingBoxLegacy(Vec3D movement, AxisAlignedBB entityBoundingBox, StreamAccumulator<VoxelShape> collisions) {
        double d = movement.x;
        double e = movement.y;
        double f = movement.z;
        if (e != 0.0D) {
            e = VoxelShapes.collide(EnumDirection.EnumAxis.Y, entityBoundingBox, collisions.getStream(), e);
            if (e != 0.0D) {
                entityBoundingBox = entityBoundingBox.move(0.0D, e, 0.0D);
            }
        }

        boolean bl = Math.abs(d) < Math.abs(f);
        if (bl && f != 0.0D) {
            f = VoxelShapes.collide(EnumDirection.EnumAxis.Z, entityBoundingBox, collisions.getStream(), f);
            if (f != 0.0D) {
                entityBoundingBox = entityBoundingBox.move(0.0D, 0.0D, f);
            }
        }

        if (d != 0.0D) {
            d = VoxelShapes.collide(EnumDirection.EnumAxis.X, entityBoundingBox, collisions.getStream(), d);
            if (!bl && d != 0.0D) {
                entityBoundingBox = entityBoundingBox.move(d, 0.0D, 0.0D);
            }
        }

        if (!bl && f != 0.0D) {
            f = VoxelShapes.collide(EnumDirection.EnumAxis.Z, entityBoundingBox, collisions.getStream(), f);
        }

        return new Vec3D(d, e, f);
    }

    public static Vec3D collideBoundingBox(Vec3D movement, AxisAlignedBB entityBoundingBox, IWorldReader world, VoxelShapeCollision context, StreamAccumulator<VoxelShape> collisions) {
        double d = movement.x;
        double e = movement.y;
        double f = movement.z;
        if (e != 0.0D) {
            e = VoxelShapes.collide(EnumDirection.EnumAxis.Y, entityBoundingBox, world, e, context, collisions.getStream());
            if (e != 0.0D) {
                entityBoundingBox = entityBoundingBox.move(0.0D, e, 0.0D);
            }
        }

        boolean bl = Math.abs(d) < Math.abs(f);
        if (bl && f != 0.0D) {
            f = VoxelShapes.collide(EnumDirection.EnumAxis.Z, entityBoundingBox, world, f, context, collisions.getStream());
            if (f != 0.0D) {
                entityBoundingBox = entityBoundingBox.move(0.0D, 0.0D, f);
            }
        }

        if (d != 0.0D) {
            d = VoxelShapes.collide(EnumDirection.EnumAxis.X, entityBoundingBox, world, d, context, collisions.getStream());
            if (!bl && d != 0.0D) {
                entityBoundingBox = entityBoundingBox.move(d, 0.0D, 0.0D);
            }
        }

        if (!bl && f != 0.0D) {
            f = VoxelShapes.collide(EnumDirection.EnumAxis.Z, entityBoundingBox, world, f, context, collisions.getStream());
        }

        return new Vec3D(d, e, f);
    }

    protected float nextStep() {
        return (float)((int)this.moveDist + 1);
    }

    protected SoundEffect getSoundSwim() {
        return SoundEffects.GENERIC_SWIM;
    }

    protected SoundEffect getSoundSplash() {
        return SoundEffects.GENERIC_SPLASH;
    }

    protected SoundEffect getSoundSplashHighSpeed() {
        return SoundEffects.GENERIC_SPLASH;
    }

    protected void checkBlockCollisions() {
        AxisAlignedBB aABB = this.getBoundingBox();
        BlockPosition blockPos = new BlockPosition(aABB.minX + 0.001D, aABB.minY + 0.001D, aABB.minZ + 0.001D);
        BlockPosition blockPos2 = new BlockPosition(aABB.maxX - 0.001D, aABB.maxY - 0.001D, aABB.maxZ - 0.001D);
        if (this.level.areChunksLoadedBetween(blockPos, blockPos2)) {
            BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

            for(int i = blockPos.getX(); i <= blockPos2.getX(); ++i) {
                for(int j = blockPos.getY(); j <= blockPos2.getY(); ++j) {
                    for(int k = blockPos.getZ(); k <= blockPos2.getZ(); ++k) {
                        mutableBlockPos.set(i, j, k);
                        IBlockData blockState = this.level.getType(mutableBlockPos);

                        try {
                            blockState.entityInside(this.level, mutableBlockPos, this);
                            this.onInsideBlock(blockState);
                        } catch (Throwable var12) {
                            CrashReport crashReport = CrashReport.forThrowable(var12, "Colliding entity with block");
                            CrashReportSystemDetails crashReportCategory = crashReport.addCategory("Block being collided with");
                            CrashReportSystemDetails.populateBlockDetails(crashReportCategory, this.level, mutableBlockPos, blockState);
                            throw new ReportedException(crashReport);
                        }
                    }
                }
            }
        }

    }

    protected void onInsideBlock(IBlockData state) {
    }

    public void gameEvent(GameEvent event, @Nullable Entity entity, BlockPosition pos) {
        this.level.gameEvent(entity, event, pos);
    }

    public void gameEvent(GameEvent event, @Nullable Entity entity) {
        this.gameEvent(event, entity, this.blockPosition);
    }

    public void gameEvent(GameEvent event, BlockPosition pos) {
        this.gameEvent(event, this, pos);
    }

    public void gameEvent(GameEvent event) {
        this.gameEvent(event, this.blockPosition);
    }

    protected void playStepSound(BlockPosition pos, IBlockData state) {
        if (!state.getMaterial().isLiquid()) {
            IBlockData blockState = this.level.getType(pos.above());
            SoundEffectType soundType = blockState.is(TagsBlock.INSIDE_STEP_SOUND_BLOCKS) ? blockState.getStepSound() : state.getStepSound();
            this.playSound(soundType.getStepSound(), soundType.getVolume() * 0.15F, soundType.getPitch());
        }
    }

    private void playAmethystStepSound(IBlockData state) {
        if (state.is(TagsBlock.CRYSTAL_SOUND_BLOCKS) && this.tickCount >= this.lastCrystalSoundPlayTick + 20) {
            this.crystalSoundIntensity = (float)((double)this.crystalSoundIntensity * Math.pow((double)0.997F, (double)(this.tickCount - this.lastCrystalSoundPlayTick)));
            this.crystalSoundIntensity = Math.min(1.0F, this.crystalSoundIntensity + 0.07F);
            float f = 0.5F + this.crystalSoundIntensity * this.random.nextFloat() * 1.2F;
            float g = 0.1F + this.crystalSoundIntensity * 1.2F;
            this.playSound(SoundEffects.AMETHYST_BLOCK_CHIME, g, f);
            this.lastCrystalSoundPlayTick = this.tickCount;
        }

    }

    protected void playSwimSound(float volume) {
        this.playSound(this.getSoundSwim(), volume, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
    }

    protected void onFlap() {
    }

    protected boolean isFlapping() {
        return false;
    }

    public void playSound(SoundEffect sound, float volume, float pitch) {
        if (!this.isSilent()) {
            this.level.playSound((EntityHuman)null, this.locX(), this.locY(), this.locZ(), sound, this.getSoundCategory(), volume, pitch);
        }

    }

    public boolean isSilent() {
        return this.entityData.get(DATA_SILENT);
    }

    public void setSilent(boolean silent) {
        this.entityData.set(DATA_SILENT, silent);
    }

    public boolean isNoGravity() {
        return this.entityData.get(DATA_NO_GRAVITY);
    }

    public void setNoGravity(boolean noGravity) {
        this.entityData.set(DATA_NO_GRAVITY, noGravity);
    }

    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.ALL;
    }

    public boolean occludesVibrations() {
        return false;
    }

    protected void checkFallDamage(double heightDifference, boolean onGround, IBlockData landedState, BlockPosition landedPosition) {
        if (onGround) {
            if (this.fallDistance > 0.0F) {
                landedState.getBlock().fallOn(this.level, landedState, landedPosition, this, this.fallDistance);
                if (!landedState.is(TagsBlock.OCCLUDES_VIBRATION_SIGNALS)) {
                    this.gameEvent(GameEvent.HIT_GROUND);
                }
            }

            this.fallDistance = 0.0F;
        } else if (heightDifference < 0.0D) {
            this.fallDistance = (float)((double)this.fallDistance - heightDifference);
        }

    }

    public boolean isFireProof() {
        return this.getEntityType().fireImmune();
    }

    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        if (this.isVehicle()) {
            for(Entity entity : this.getPassengers()) {
                entity.causeFallDamage(fallDistance, damageMultiplier, damageSource);
            }
        }

        return false;
    }

    public boolean isInWater() {
        return this.wasTouchingWater;
    }

    public boolean isInRain() {
        BlockPosition blockPos = this.getChunkCoordinates();
        return this.level.isRainingAt(blockPos) || this.level.isRainingAt(new BlockPosition((double)blockPos.getX(), this.getBoundingBox().maxY, (double)blockPos.getZ()));
    }

    public boolean isInBubbleColumn() {
        return this.level.getType(this.getChunkCoordinates()).is(Blocks.BUBBLE_COLUMN);
    }

    public boolean isInWaterOrRain() {
        return this.isInWater() || this.isInRain();
    }

    public boolean isInWaterRainOrBubble() {
        return this.isInWater() || this.isInRain() || this.isInBubbleColumn();
    }

    public boolean isInWaterOrBubble() {
        return this.isInWater() || this.isInBubbleColumn();
    }

    public boolean isUnderWater() {
        return this.wasEyeInWater && this.isInWater();
    }

    public void updateSwimming() {
        if (this.isSwimming()) {
            this.setSwimming(this.isSprinting() && this.isInWater() && !this.isPassenger());
        } else {
            this.setSwimming(this.isSprinting() && this.isUnderWater() && !this.isPassenger() && this.level.getFluid(this.blockPosition).is(TagsFluid.WATER));
        }

    }

    protected boolean updateInWaterStateAndDoFluidPushing() {
        this.fluidHeight.clear();
        this.updateInWaterStateAndDoWaterCurrentPushing();
        double d = this.level.getDimensionManager().isNether() ? 0.007D : 0.0023333333333333335D;
        boolean bl = this.updateFluidHeightAndDoFluidPushing(TagsFluid.LAVA, d);
        return this.isInWater() || bl;
    }

    void updateInWaterStateAndDoWaterCurrentPushing() {
        if (this.getVehicle() instanceof EntityBoat) {
            this.wasTouchingWater = false;
        } else if (this.updateFluidHeightAndDoFluidPushing(TagsFluid.WATER, 0.014D)) {
            if (!this.wasTouchingWater && !this.firstTick) {
                this.doWaterSplashEffect();
            }

            this.fallDistance = 0.0F;
            this.wasTouchingWater = true;
            this.extinguish();
        } else {
            this.wasTouchingWater = false;
        }

    }

    private void updateFluidOnEyes() {
        this.wasEyeInWater = this.isEyeInFluid(TagsFluid.WATER);
        this.fluidOnEyes = null;
        double d = this.getHeadY() - (double)0.11111111F;
        Entity entity = this.getVehicle();
        if (entity instanceof EntityBoat) {
            EntityBoat boat = (EntityBoat)entity;
            if (!boat.isUnderWater() && boat.getBoundingBox().maxY >= d && boat.getBoundingBox().minY <= d) {
                return;
            }
        }

        BlockPosition blockPos = new BlockPosition(this.locX(), d, this.locZ());
        Fluid fluidState = this.level.getFluid(blockPos);

        for(Tag<FluidType> tag : TagsFluid.getStaticTags()) {
            if (fluidState.is(tag)) {
                double e = (double)((float)blockPos.getY() + fluidState.getHeight(this.level, blockPos));
                if (e > d) {
                    this.fluidOnEyes = tag;
                }

                return;
            }
        }

    }

    protected void doWaterSplashEffect() {
        Entity entity = this.isVehicle() && this.getRidingPassenger() != null ? this.getRidingPassenger() : this;
        float f = entity == this ? 0.2F : 0.9F;
        Vec3D vec3 = entity.getMot();
        float g = Math.min(1.0F, (float)Math.sqrt(vec3.x * vec3.x * (double)0.2F + vec3.y * vec3.y + vec3.z * vec3.z * (double)0.2F) * f);
        if (g < 0.25F) {
            this.playSound(this.getSoundSplash(), g, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
        } else {
            this.playSound(this.getSoundSplashHighSpeed(), g, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
        }

        float h = (float)MathHelper.floor(this.locY());

        for(int i = 0; (float)i < 1.0F + this.dimensions.width * 20.0F; ++i) {
            double d = (this.random.nextDouble() * 2.0D - 1.0D) * (double)this.dimensions.width;
            double e = (this.random.nextDouble() * 2.0D - 1.0D) * (double)this.dimensions.width;
            this.level.addParticle(Particles.BUBBLE, this.locX() + d, (double)(h + 1.0F), this.locZ() + e, vec3.x, vec3.y - this.random.nextDouble() * (double)0.2F, vec3.z);
        }

        for(int j = 0; (float)j < 1.0F + this.dimensions.width * 20.0F; ++j) {
            double k = (this.random.nextDouble() * 2.0D - 1.0D) * (double)this.dimensions.width;
            double l = (this.random.nextDouble() * 2.0D - 1.0D) * (double)this.dimensions.width;
            this.level.addParticle(Particles.SPLASH, this.locX() + k, (double)(h + 1.0F), this.locZ() + l, vec3.x, vec3.y, vec3.z);
        }

        this.gameEvent(GameEvent.SPLASH);
    }

    protected IBlockData getBlockStateOn() {
        return this.level.getType(this.getOnPos());
    }

    public boolean canSpawnSprintParticle() {
        return this.isSprinting() && !this.isInWater() && !this.isSpectator() && !this.isCrouching() && !this.isInLava() && this.isAlive();
    }

    protected void spawnSprintParticle() {
        int i = MathHelper.floor(this.locX());
        int j = MathHelper.floor(this.locY() - (double)0.2F);
        int k = MathHelper.floor(this.locZ());
        BlockPosition blockPos = new BlockPosition(i, j, k);
        IBlockData blockState = this.level.getType(blockPos);
        if (blockState.getRenderShape() != EnumRenderType.INVISIBLE) {
            Vec3D vec3 = this.getMot();
            this.level.addParticle(new ParticleParamBlock(Particles.BLOCK, blockState), this.locX() + (this.random.nextDouble() - 0.5D) * (double)this.dimensions.width, this.locY() + 0.1D, this.locZ() + (this.random.nextDouble() - 0.5D) * (double)this.dimensions.width, vec3.x * -4.0D, 1.5D, vec3.z * -4.0D);
        }

    }

    public boolean isEyeInFluid(Tag<FluidType> fluidTag) {
        return this.fluidOnEyes == fluidTag;
    }

    public boolean isInLava() {
        return !this.firstTick && this.fluidHeight.getDouble(TagsFluid.LAVA) > 0.0D;
    }

    public void moveRelative(float speed, Vec3D movementInput) {
        Vec3D vec3 = getInputVector(movementInput, speed, this.getYRot());
        this.setMot(this.getMot().add(vec3));
    }

    private static Vec3D getInputVector(Vec3D movementInput, float speed, float yaw) {
        double d = movementInput.lengthSqr();
        if (d < 1.0E-7D) {
            return Vec3D.ZERO;
        } else {
            Vec3D vec3 = (d > 1.0D ? movementInput.normalize() : movementInput).scale((double)speed);
            float f = MathHelper.sin(yaw * ((float)Math.PI / 180F));
            float g = MathHelper.cos(yaw * ((float)Math.PI / 180F));
            return new Vec3D(vec3.x * (double)g - vec3.z * (double)f, vec3.y, vec3.z * (double)g + vec3.x * (double)f);
        }
    }

    public float getBrightness() {
        return this.level.hasChunkAt(this.getBlockX(), this.getBlockZ()) ? this.level.getBrightness(new BlockPosition(this.locX(), this.getHeadY(), this.locZ())) : 0.0F;
    }

    public void setLocation(double x, double y, double z, float yaw, float pitch) {
        this.absMoveTo(x, y, z);
        this.setYRot(yaw % 360.0F);
        this.setXRot(MathHelper.clamp(pitch, -90.0F, 90.0F) % 360.0F);
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    public void absMoveTo(double x, double y, double z) {
        double d = MathHelper.clamp(x, -3.0E7D, 3.0E7D);
        double e = MathHelper.clamp(z, -3.0E7D, 3.0E7D);
        this.xo = d;
        this.yo = y;
        this.zo = e;
        this.setPosition(d, y, e);
    }

    public void moveTo(Vec3D pos) {
        this.teleportAndSync(pos.x, pos.y, pos.z);
    }

    public void teleportAndSync(double x, double y, double z) {
        this.setPositionRotation(x, y, z, this.getYRot(), this.getXRot());
    }

    public void setPositionRotation(BlockPosition pos, float yaw, float pitch) {
        this.setPositionRotation((double)pos.getX() + 0.5D, (double)pos.getY(), (double)pos.getZ() + 0.5D, yaw, pitch);
    }

    public void setPositionRotation(double x, double y, double z, float yaw, float pitch) {
        this.setPositionRaw(x, y, z);
        this.setYRot(yaw);
        this.setXRot(pitch);
        this.setOldPosAndRot();
        this.reapplyPosition();
    }

    public final void setOldPosAndRot() {
        double d = this.locX();
        double e = this.locY();
        double f = this.locZ();
        this.xo = d;
        this.yo = e;
        this.zo = f;
        this.xOld = d;
        this.yOld = e;
        this.zOld = f;
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    public float distanceTo(Entity entity) {
        float f = (float)(this.locX() - entity.locX());
        float g = (float)(this.locY() - entity.locY());
        float h = (float)(this.locZ() - entity.locZ());
        return MathHelper.sqrt(f * f + g * g + h * h);
    }

    public double distanceToSqr(double x, double y, double z) {
        double d = this.locX() - x;
        double e = this.locY() - y;
        double f = this.locZ() - z;
        return d * d + e * e + f * f;
    }

    public double distanceToSqr(Entity entity) {
        return this.distanceToSqr(entity.getPositionVector());
    }

    public double distanceToSqr(Vec3D vector) {
        double d = this.locX() - vector.x;
        double e = this.locY() - vector.y;
        double f = this.locZ() - vector.z;
        return d * d + e * e + f * f;
    }

    public void pickup(EntityHuman player) {
    }

    public void collide(Entity entity) {
        if (!this.isSameVehicle(entity)) {
            if (!entity.noPhysics && !this.noPhysics) {
                double d = entity.locX() - this.locX();
                double e = entity.locZ() - this.locZ();
                double f = MathHelper.absMax(d, e);
                if (f >= (double)0.01F) {
                    f = Math.sqrt(f);
                    d = d / f;
                    e = e / f;
                    double g = 1.0D / f;
                    if (g > 1.0D) {
                        g = 1.0D;
                    }

                    d = d * g;
                    e = e * g;
                    d = d * (double)0.05F;
                    e = e * (double)0.05F;
                    if (!this.isVehicle()) {
                        this.push(-d, 0.0D, -e);
                    }

                    if (!entity.isVehicle()) {
                        entity.push(d, 0.0D, e);
                    }
                }

            }
        }
    }

    public void push(double deltaX, double deltaY, double deltaZ) {
        this.setMot(this.getMot().add(deltaX, deltaY, deltaZ));
        this.hasImpulse = true;
    }

    protected void velocityChanged() {
        this.hurtMarked = true;
    }

    public boolean damageEntity(DamageSource source, float amount) {
        if (this.isInvulnerable(source)) {
            return false;
        } else {
            this.velocityChanged();
            return false;
        }
    }

    public final Vec3D getViewVector(float tickDelta) {
        return this.calculateViewVector(this.getViewXRot(tickDelta), this.getViewYRot(tickDelta));
    }

    public float getViewXRot(float tickDelta) {
        return tickDelta == 1.0F ? this.getXRot() : MathHelper.lerp(tickDelta, this.xRotO, this.getXRot());
    }

    public float getViewYRot(float tickDelta) {
        return tickDelta == 1.0F ? this.getYRot() : MathHelper.lerp(tickDelta, this.yRotO, this.getYRot());
    }

    protected final Vec3D calculateViewVector(float pitch, float yaw) {
        float f = pitch * ((float)Math.PI / 180F);
        float g = -yaw * ((float)Math.PI / 180F);
        float h = MathHelper.cos(g);
        float i = MathHelper.sin(g);
        float j = MathHelper.cos(f);
        float k = MathHelper.sin(f);
        return new Vec3D((double)(i * j), (double)(-k), (double)(h * j));
    }

    public final Vec3D getUpVector(float tickDelta) {
        return this.calculateUpVector(this.getViewXRot(tickDelta), this.getViewYRot(tickDelta));
    }

    protected final Vec3D calculateUpVector(float pitch, float yaw) {
        return this.calculateViewVector(pitch - 90.0F, yaw);
    }

    public final Vec3D getEyePosition() {
        return new Vec3D(this.locX(), this.getHeadY(), this.locZ());
    }

    public final Vec3D getEyePosition(float tickDelta) {
        double d = MathHelper.lerp((double)tickDelta, this.xo, this.locX());
        double e = MathHelper.lerp((double)tickDelta, this.yo, this.locY()) + (double)this.getHeadHeight();
        double f = MathHelper.lerp((double)tickDelta, this.zo, this.locZ());
        return new Vec3D(d, e, f);
    }

    public Vec3D getLightProbePosition(float tickDelta) {
        return this.getEyePosition(tickDelta);
    }

    public final Vec3D getPosition(float delta) {
        double d = MathHelper.lerp((double)delta, this.xo, this.locX());
        double e = MathHelper.lerp((double)delta, this.yo, this.locY());
        double f = MathHelper.lerp((double)delta, this.zo, this.locZ());
        return new Vec3D(d, e, f);
    }

    public MovingObjectPosition pick(double maxDistance, float tickDelta, boolean includeFluids) {
        Vec3D vec3 = this.getEyePosition(tickDelta);
        Vec3D vec32 = this.getViewVector(tickDelta);
        Vec3D vec33 = vec3.add(vec32.x * maxDistance, vec32.y * maxDistance, vec32.z * maxDistance);
        return this.level.rayTrace(new RayTrace(vec3, vec33, RayTrace.BlockCollisionOption.OUTLINE, includeFluids ? RayTrace.FluidCollisionOption.ANY : RayTrace.FluidCollisionOption.NONE, this));
    }

    public boolean isInteractable() {
        return false;
    }

    public boolean isCollidable() {
        return false;
    }

    public void awardKillScore(Entity killer, int score, DamageSource damageSource) {
        if (killer instanceof EntityPlayer) {
            CriterionTriggers.ENTITY_KILLED_PLAYER.trigger((EntityPlayer)killer, this, damageSource);
        }

    }

    public boolean shouldRender(double cameraX, double cameraY, double cameraZ) {
        double d = this.locX() - cameraX;
        double e = this.locY() - cameraY;
        double f = this.locZ() - cameraZ;
        double g = d * d + e * e + f * f;
        return this.shouldRenderAtSqrDistance(g);
    }

    public boolean shouldRenderAtSqrDistance(double distance) {
        double d = this.getBoundingBox().getSize();
        if (Double.isNaN(d)) {
            d = 1.0D;
        }

        d = d * 64.0D * viewScale;
        return distance < d * d;
    }

    public boolean saveAsPassenger(NBTTagCompound nbt) {
        if (this.removalReason != null && !this.removalReason.shouldSave()) {
            return false;
        } else {
            String string = this.getSaveID();
            if (string == null) {
                return false;
            } else {
                nbt.setString("id", string);
                this.save(nbt);
                return true;
            }
        }
    }

    public boolean save(NBTTagCompound nbt) {
        return this.isPassenger() ? false : this.saveAsPassenger(nbt);
    }

    public NBTTagCompound save(NBTTagCompound nbt) {
        try {
            if (this.vehicle != null) {
                nbt.set("Pos", this.newDoubleList(this.vehicle.locX(), this.locY(), this.vehicle.locZ()));
            } else {
                nbt.set("Pos", this.newDoubleList(this.locX(), this.locY(), this.locZ()));
            }

            Vec3D vec3 = this.getMot();
            nbt.set("Motion", this.newDoubleList(vec3.x, vec3.y, vec3.z));
            nbt.set("Rotation", this.newFloatList(this.getYRot(), this.getXRot()));
            nbt.setFloat("FallDistance", this.fallDistance);
            nbt.setShort("Fire", (short)this.remainingFireTicks);
            nbt.setShort("Air", (short)this.getAirTicks());
            nbt.setBoolean("OnGround", this.onGround);
            nbt.setBoolean("Invulnerable", this.invulnerable);
            nbt.setInt("PortalCooldown", this.portalCooldown);
            nbt.putUUID("UUID", this.getUniqueID());
            IChatBaseComponent component = this.getCustomName();
            if (component != null) {
                nbt.setString("CustomName", IChatBaseComponent.ChatSerializer.toJson(component));
            }

            if (this.getCustomNameVisible()) {
                nbt.setBoolean("CustomNameVisible", this.getCustomNameVisible());
            }

            if (this.isSilent()) {
                nbt.setBoolean("Silent", this.isSilent());
            }

            if (this.isNoGravity()) {
                nbt.setBoolean("NoGravity", this.isNoGravity());
            }

            if (this.hasGlowingTag) {
                nbt.setBoolean("Glowing", true);
            }

            int i = this.getTicksFrozen();
            if (i > 0) {
                nbt.setInt("TicksFrozen", this.getTicksFrozen());
            }

            if (this.hasVisualFire) {
                nbt.setBoolean("HasVisualFire", this.hasVisualFire);
            }

            if (!this.tags.isEmpty()) {
                NBTTagList listTag = new NBTTagList();

                for(String string : this.tags) {
                    listTag.add(NBTTagString.valueOf(string));
                }

                nbt.set("Tags", listTag);
            }

            this.saveData(nbt);
            if (this.isVehicle()) {
                NBTTagList listTag2 = new NBTTagList();

                for(Entity entity : this.getPassengers()) {
                    NBTTagCompound compoundTag = new NBTTagCompound();
                    if (entity.saveAsPassenger(compoundTag)) {
                        listTag2.add(compoundTag);
                    }
                }

                if (!listTag2.isEmpty()) {
                    nbt.set("Passengers", listTag2);
                }
            }

            return nbt;
        } catch (Throwable var9) {
            CrashReport crashReport = CrashReport.forThrowable(var9, "Saving entity NBT");
            CrashReportSystemDetails crashReportCategory = crashReport.addCategory("Entity being saved");
            this.appendEntityCrashDetails(crashReportCategory);
            throw new ReportedException(crashReport);
        }
    }

    public void load(NBTTagCompound nbt) {
        try {
            NBTTagList listTag = nbt.getList("Pos", 6);
            NBTTagList listTag2 = nbt.getList("Motion", 6);
            NBTTagList listTag3 = nbt.getList("Rotation", 5);
            double d = listTag2.getDouble(0);
            double e = listTag2.getDouble(1);
            double f = listTag2.getDouble(2);
            this.setMot(Math.abs(d) > 10.0D ? 0.0D : d, Math.abs(e) > 10.0D ? 0.0D : e, Math.abs(f) > 10.0D ? 0.0D : f);
            this.setPositionRaw(listTag.getDouble(0), MathHelper.clamp(listTag.getDouble(1), -2.0E7D, 2.0E7D), listTag.getDouble(2));
            this.setYRot(listTag3.getFloat(0));
            this.setXRot(listTag3.getFloat(1));
            this.setOldPosAndRot();
            this.setHeadRotation(this.getYRot());
            this.setYBodyRot(this.getYRot());
            this.fallDistance = nbt.getFloat("FallDistance");
            this.remainingFireTicks = nbt.getShort("Fire");
            if (nbt.hasKey("Air")) {
                this.setAirTicks(nbt.getShort("Air"));
            }

            this.onGround = nbt.getBoolean("OnGround");
            this.invulnerable = nbt.getBoolean("Invulnerable");
            this.portalCooldown = nbt.getInt("PortalCooldown");
            if (nbt.hasUUID("UUID")) {
                this.uuid = nbt.getUUID("UUID");
                this.stringUUID = this.uuid.toString();
            }

            if (Double.isFinite(this.locX()) && Double.isFinite(this.locY()) && Double.isFinite(this.locZ())) {
                if (Double.isFinite((double)this.getYRot()) && Double.isFinite((double)this.getXRot())) {
                    this.reapplyPosition();
                    this.setYawPitch(this.getYRot(), this.getXRot());
                    if (nbt.hasKeyOfType("CustomName", 8)) {
                        String string = nbt.getString("CustomName");

                        try {
                            this.setCustomName(IChatBaseComponent.ChatSerializer.fromJson(string));
                        } catch (Exception var14) {
                            LOGGER.warn("Failed to parse entity custom name {}", string, var14);
                        }
                    }

                    this.setCustomNameVisible(nbt.getBoolean("CustomNameVisible"));
                    this.setSilent(nbt.getBoolean("Silent"));
                    this.setNoGravity(nbt.getBoolean("NoGravity"));
                    this.setGlowingTag(nbt.getBoolean("Glowing"));
                    this.setTicksFrozen(nbt.getInt("TicksFrozen"));
                    this.hasVisualFire = nbt.getBoolean("HasVisualFire");
                    if (nbt.hasKeyOfType("Tags", 9)) {
                        this.tags.clear();
                        NBTTagList listTag4 = nbt.getList("Tags", 8);
                        int i = Math.min(listTag4.size(), 1024);

                        for(int j = 0; j < i; ++j) {
                            this.tags.add(listTag4.getString(j));
                        }
                    }

                    this.loadData(nbt);
                    if (this.repositionEntityAfterLoad()) {
                        this.reapplyPosition();
                    }

                } else {
                    throw new IllegalStateException("Entity has invalid rotation");
                }
            } else {
                throw new IllegalStateException("Entity has invalid position");
            }
        } catch (Throwable var15) {
            CrashReport crashReport = CrashReport.forThrowable(var15, "Loading entity NBT");
            CrashReportSystemDetails crashReportCategory = crashReport.addCategory("Entity being loaded");
            this.appendEntityCrashDetails(crashReportCategory);
            throw new ReportedException(crashReport);
        }
    }

    protected boolean repositionEntityAfterLoad() {
        return true;
    }

    @Nullable
    public final String getSaveID() {
        EntityTypes<?> entityType = this.getEntityType();
        MinecraftKey resourceLocation = EntityTypes.getName(entityType);
        return entityType.canSerialize() && resourceLocation != null ? resourceLocation.toString() : null;
    }

    protected abstract void loadData(NBTTagCompound nbt);

    protected abstract void saveData(NBTTagCompound nbt);

    protected NBTTagList newDoubleList(double... values) {
        NBTTagList listTag = new NBTTagList();

        for(double d : values) {
            listTag.add(NBTTagDouble.valueOf(d));
        }

        return listTag;
    }

    protected NBTTagList newFloatList(float... values) {
        NBTTagList listTag = new NBTTagList();

        for(float f : values) {
            listTag.add(NBTTagFloat.valueOf(f));
        }

        return listTag;
    }

    @Nullable
    public EntityItem spawnAtLocation(IMaterial item) {
        return this.spawnAtLocation(item, 0);
    }

    @Nullable
    public EntityItem spawnAtLocation(IMaterial item, int yOffset) {
        return this.spawnAtLocation(new ItemStack(item), (float)yOffset);
    }

    @Nullable
    public EntityItem spawnAtLocation(ItemStack stack) {
        return this.spawnAtLocation(stack, 0.0F);
    }

    @Nullable
    public EntityItem spawnAtLocation(ItemStack stack, float yOffset) {
        if (stack.isEmpty()) {
            return null;
        } else if (this.level.isClientSide) {
            return null;
        } else {
            EntityItem itemEntity = new EntityItem(this.level, this.locX(), this.locY() + (double)yOffset, this.locZ(), stack);
            itemEntity.defaultPickupDelay();
            this.level.addEntity(itemEntity);
            return itemEntity;
        }
    }

    public boolean isAlive() {
        return !this.isRemoved();
    }

    public boolean inBlock() {
        if (this.noPhysics) {
            return false;
        } else {
            float f = this.dimensions.width * 0.8F;
            AxisAlignedBB aABB = AxisAlignedBB.ofSize(this.getEyePosition(), (double)f, 1.0E-6D, (double)f);
            return this.level.getBlockCollisions(this, aABB, (state, pos) -> {
                return state.isSuffocating(this.level, pos);
            }).findAny().isPresent();
        }
    }

    public EnumInteractionResult interact(EntityHuman player, EnumHand hand) {
        return EnumInteractionResult.PASS;
    }

    public boolean canCollideWith(Entity other) {
        return other.canBeCollidedWith() && !this.isSameVehicle(other);
    }

    public boolean canBeCollidedWith() {
        return false;
    }

    public void passengerTick() {
        this.setMot(Vec3D.ZERO);
        this.tick();
        if (this.isPassenger()) {
            this.getVehicle().positionRider(this);
        }
    }

    public void positionRider(Entity passenger) {
        this.positionRider(passenger, Entity::setPosition);
    }

    private void positionRider(Entity passenger, Entity.MoveFunction positionUpdater) {
        if (this.hasPassenger(passenger)) {
            double d = this.locY() + this.getPassengersRidingOffset() + passenger.getMyRidingOffset();
            positionUpdater.accept(passenger, this.locX(), d, this.locZ());
        }
    }

    public void onPassengerTurned(Entity passenger) {
    }

    public double getMyRidingOffset() {
        return 0.0D;
    }

    public double getPassengersRidingOffset() {
        return (double)this.dimensions.height * 0.75D;
    }

    public boolean startRiding(Entity entity) {
        return this.startRiding(entity, false);
    }

    public boolean showVehicleHealth() {
        return this instanceof EntityLiving;
    }

    public boolean startRiding(Entity entity, boolean force) {
        if (entity == this.vehicle) {
            return false;
        } else {
            for(Entity entity2 = entity; entity2.vehicle != null; entity2 = entity2.vehicle) {
                if (entity2.vehicle == this) {
                    return false;
                }
            }

            if (force || this.canRide(entity) && entity.canAddPassenger(this)) {
                if (this.isPassenger()) {
                    this.stopRiding();
                }

                this.setPose(EntityPose.STANDING);
                this.vehicle = entity;
                this.vehicle.addPassenger(this);
                entity.getIndirectPassengersStream().filter((entityx) -> {
                    return entityx instanceof EntityPlayer;
                }).forEach((entityx) -> {
                    CriterionTriggers.START_RIDING_TRIGGER.trigger((EntityPlayer)entityx);
                });
                return true;
            } else {
                return false;
            }
        }
    }

    protected boolean canRide(Entity entity) {
        return !this.isSneaking() && this.boardingCooldown <= 0;
    }

    protected boolean canEnterPose(EntityPose pose) {
        return this.level.getCubes(this, this.getBoundingBoxForPose(pose).shrink(1.0E-7D));
    }

    public void ejectPassengers() {
        for(int i = this.passengers.size() - 1; i >= 0; --i) {
            this.passengers.get(i).stopRiding();
        }

    }

    public void removeVehicle() {
        if (this.vehicle != null) {
            Entity entity = this.vehicle;
            this.vehicle = null;
            entity.removePassenger(this);
        }

    }

    public void stopRiding() {
        this.removeVehicle();
    }

    protected void addPassenger(Entity passenger) {
        if (passenger.getVehicle() != this) {
            throw new IllegalStateException("Use x.startRiding(y), not y.addPassenger(x)");
        } else {
            if (this.passengers.isEmpty()) {
                this.passengers = ImmutableList.of(passenger);
            } else {
                List<Entity> list = Lists.newArrayList(this.passengers);
                if (!this.level.isClientSide && passenger instanceof EntityHuman && !(this.getRidingPassenger() instanceof EntityHuman)) {
                    list.add(0, passenger);
                } else {
                    list.add(passenger);
                }

                this.passengers = ImmutableList.copyOf(list);
            }

        }
    }

    protected void removePassenger(Entity passenger) {
        if (passenger.getVehicle() == this) {
            throw new IllegalStateException("Use x.stopRiding(y), not y.removePassenger(x)");
        } else {
            if (this.passengers.size() == 1 && this.passengers.get(0) == passenger) {
                this.passengers = ImmutableList.of();
            } else {
                this.passengers = this.passengers.stream().filter((entity2) -> {
                    return entity2 != passenger;
                }).collect(ImmutableList.toImmutableList());
            }

            passenger.boardingCooldown = 60;
        }
    }

    protected boolean canAddPassenger(Entity passenger) {
        return this.passengers.isEmpty();
    }

    public void lerpTo(double x, double y, double z, float yaw, float pitch, int interpolationSteps, boolean interpolate) {
        this.setPosition(x, y, z);
        this.setYawPitch(yaw, pitch);
    }

    public void lerpHeadTo(float yaw, int interpolationSteps) {
        this.setHeadRotation(yaw);
    }

    public float getPickRadius() {
        return 0.0F;
    }

    public Vec3D getLookDirection() {
        return this.calculateViewVector(this.getXRot(), this.getYRot());
    }

    public Vec2F getRotationVector() {
        return new Vec2F(this.getXRot(), this.getYRot());
    }

    public Vec3D getForward() {
        return Vec3D.directionFromRotation(this.getRotationVector());
    }

    public void handleInsidePortal(BlockPosition pos) {
        if (this.isOnPortalCooldown()) {
            this.resetPortalCooldown();
        } else {
            if (!this.level.isClientSide && !pos.equals(this.portalEntrancePos)) {
                this.portalEntrancePos = pos.immutableCopy();
            }

            this.isInsidePortal = true;
        }
    }

    protected void doPortalTick() {
        if (this.level instanceof WorldServer) {
            int i = this.getPortalWaitTime();
            WorldServer serverLevel = (WorldServer)this.level;
            if (this.isInsidePortal) {
                MinecraftServer minecraftServer = serverLevel.getMinecraftServer();
                ResourceKey<World> resourceKey = this.level.getDimensionKey() == World.NETHER ? World.OVERWORLD : World.NETHER;
                WorldServer serverLevel2 = minecraftServer.getWorldServer(resourceKey);
                if (serverLevel2 != null && minecraftServer.getAllowNether() && !this.isPassenger() && this.portalTime++ >= i) {
                    this.level.getMethodProfiler().enter("portal");
                    this.portalTime = i;
                    this.resetPortalCooldown();
                    this.changeDimension(serverLevel2);
                    this.level.getMethodProfiler().exit();
                }

                this.isInsidePortal = false;
            } else {
                if (this.portalTime > 0) {
                    this.portalTime -= 4;
                }

                if (this.portalTime < 0) {
                    this.portalTime = 0;
                }
            }

            this.processPortalCooldown();
        }
    }

    public int getDefaultPortalCooldown() {
        return 300;
    }

    public void lerpMotion(double x, double y, double z) {
        this.setMot(x, y, z);
    }

    public void handleEntityEvent(byte status) {
        switch(status) {
        case 53:
            BlockHoney.showSlideParticles(this);
        default:
        }
    }

    public void animateHurt() {
    }

    public Iterable<ItemStack> getHandSlots() {
        return EMPTY_LIST;
    }

    public Iterable<ItemStack> getArmorItems() {
        return EMPTY_LIST;
    }

    public Iterable<ItemStack> getAllSlots() {
        return Iterables.concat(this.getHandSlots(), this.getArmorItems());
    }

    public void setSlot(EnumItemSlot slot, ItemStack stack) {
    }

    public boolean isBurning() {
        boolean bl = this.level != null && this.level.isClientSide;
        return !this.isFireProof() && (this.remainingFireTicks > 0 || bl && this.getFlag(0));
    }

    public boolean isPassenger() {
        return this.getVehicle() != null;
    }

    public boolean isVehicle() {
        return !this.passengers.isEmpty();
    }

    public boolean rideableUnderWater() {
        return true;
    }

    public void setSneaking(boolean sneaking) {
        this.setFlag(1, sneaking);
    }

    public boolean isSneaking() {
        return this.getFlag(1);
    }

    public boolean isSteppingCarefully() {
        return this.isSneaking();
    }

    public boolean isSuppressingBounce() {
        return this.isSneaking();
    }

    public boolean isDiscrete() {
        return this.isSneaking();
    }

    public boolean isDescending() {
        return this.isSneaking();
    }

    public boolean isCrouching() {
        return this.getPose() == EntityPose.CROUCHING;
    }

    public boolean isSprinting() {
        return this.getFlag(3);
    }

    public void setSprinting(boolean sprinting) {
        this.setFlag(3, sprinting);
    }

    public boolean isSwimming() {
        return this.getFlag(4);
    }

    public boolean isVisuallySwimming() {
        return this.getPose() == EntityPose.SWIMMING;
    }

    public boolean isVisuallyCrawling() {
        return this.isVisuallySwimming() && !this.isInWater();
    }

    public void setSwimming(boolean swimming) {
        this.setFlag(4, swimming);
    }

    public final boolean hasGlowingTag() {
        return this.hasGlowingTag;
    }

    public final void setGlowingTag(boolean glowing) {
        this.hasGlowingTag = glowing;
        this.setFlag(6, this.isCurrentlyGlowing());
    }

    public boolean isCurrentlyGlowing() {
        return this.level.isClientSide() ? this.getFlag(6) : this.hasGlowingTag;
    }

    public boolean isInvisible() {
        return this.getFlag(5);
    }

    public boolean isInvisibleTo(EntityHuman player) {
        if (player.isSpectator()) {
            return false;
        } else {
            ScoreboardTeamBase team = this.getScoreboardTeam();
            return team != null && player != null && player.getScoreboardTeam() == team && team.canSeeFriendlyInvisibles() ? false : this.isInvisible();
        }
    }

    @Nullable
    public GameEventListenerRegistrar getGameEventListenerRegistrar() {
        return null;
    }

    @Nullable
    public ScoreboardTeamBase getScoreboardTeam() {
        return this.level.getScoreboard().getPlayerTeam(this.getName());
    }

    public boolean isAlliedTo(Entity other) {
        return this.isAlliedTo(other.getScoreboardTeam());
    }

    public boolean isAlliedTo(ScoreboardTeamBase team) {
        return this.getScoreboardTeam() != null ? this.getScoreboardTeam().isAlly(team) : false;
    }

    public void setInvisible(boolean invisible) {
        this.setFlag(5, invisible);
    }

    public boolean getFlag(int index) {
        return (this.entityData.get(DATA_SHARED_FLAGS_ID) & 1 << index) != 0;
    }

    public void setFlag(int index, boolean value) {
        byte b = this.entityData.get(DATA_SHARED_FLAGS_ID);
        if (value) {
            this.entityData.set(DATA_SHARED_FLAGS_ID, (byte)(b | 1 << index));
        } else {
            this.entityData.set(DATA_SHARED_FLAGS_ID, (byte)(b & ~(1 << index)));
        }

    }

    public int getMaxAirSupply() {
        return 300;
    }

    public int getAirTicks() {
        return this.entityData.get(DATA_AIR_SUPPLY_ID);
    }

    public void setAirTicks(int air) {
        this.entityData.set(DATA_AIR_SUPPLY_ID, air);
    }

    public int getTicksFrozen() {
        return this.entityData.get(DATA_TICKS_FROZEN);
    }

    public void setTicksFrozen(int frozenTicks) {
        this.entityData.set(DATA_TICKS_FROZEN, frozenTicks);
    }

    public float getPercentFrozen() {
        int i = this.getTicksRequiredToFreeze();
        return (float)Math.min(this.getTicksFrozen(), i) / (float)i;
    }

    public boolean isFullyFrozen() {
        return this.getTicksFrozen() >= this.getTicksRequiredToFreeze();
    }

    public int getTicksRequiredToFreeze() {
        return 140;
    }

    public void onLightningStrike(WorldServer world, EntityLightning lightning) {
        this.setFireTicks(this.remainingFireTicks + 1);
        if (this.remainingFireTicks == 0) {
            this.setOnFire(8);
        }

        this.damageEntity(DamageSource.LIGHTNING_BOLT, 5.0F);
    }

    public void onAboveBubbleCol(boolean drag) {
        Vec3D vec3 = this.getMot();
        double d;
        if (drag) {
            d = Math.max(-0.9D, vec3.y - 0.03D);
        } else {
            d = Math.min(1.8D, vec3.y + 0.1D);
        }

        this.setMot(vec3.x, d, vec3.z);
    }

    public void onInsideBubbleColumn(boolean drag) {
        Vec3D vec3 = this.getMot();
        double d;
        if (drag) {
            d = Math.max(-0.3D, vec3.y - 0.03D);
        } else {
            d = Math.min(0.7D, vec3.y + 0.06D);
        }

        this.setMot(vec3.x, d, vec3.z);
        this.fallDistance = 0.0F;
    }

    public void killed(WorldServer world, EntityLiving other) {
    }

    protected void moveTowardsClosestSpace(double x, double y, double z) {
        BlockPosition blockPos = new BlockPosition(x, y, z);
        Vec3D vec3 = new Vec3D(x - (double)blockPos.getX(), y - (double)blockPos.getY(), z - (double)blockPos.getZ());
        BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();
        EnumDirection direction = EnumDirection.UP;
        double d = Double.MAX_VALUE;

        for(EnumDirection direction2 : new EnumDirection[]{EnumDirection.NORTH, EnumDirection.SOUTH, EnumDirection.WEST, EnumDirection.EAST, EnumDirection.UP}) {
            mutableBlockPos.setWithOffset(blockPos, direction2);
            if (!this.level.getType(mutableBlockPos).isCollisionShapeFullBlock(this.level, mutableBlockPos)) {
                double e = vec3.get(direction2.getAxis());
                double f = direction2.getAxisDirection() == EnumDirection.EnumAxisDirection.POSITIVE ? 1.0D - e : e;
                if (f < d) {
                    d = f;
                    direction = direction2;
                }
            }
        }

        float g = this.random.nextFloat() * 0.2F + 0.1F;
        float h = (float)direction.getAxisDirection().getStep();
        Vec3D vec32 = this.getMot().scale(0.75D);
        if (direction.getAxis() == EnumDirection.EnumAxis.X) {
            this.setMot((double)(h * g), vec32.y, vec32.z);
        } else if (direction.getAxis() == EnumDirection.EnumAxis.Y) {
            this.setMot(vec32.x, (double)(h * g), vec32.z);
        } else if (direction.getAxis() == EnumDirection.EnumAxis.Z) {
            this.setMot(vec32.x, vec32.y, (double)(h * g));
        }

    }

    public void makeStuckInBlock(IBlockData state, Vec3D multiplier) {
        this.fallDistance = 0.0F;
        this.stuckSpeedMultiplier = multiplier;
    }

    private static IChatBaseComponent removeAction(IChatBaseComponent textComponent) {
        IChatMutableComponent mutableComponent = textComponent.plainCopy().setChatModifier(textComponent.getChatModifier().setChatClickable((ChatClickable)null));

        for(IChatBaseComponent component : textComponent.getSiblings()) {
            mutableComponent.addSibling(removeAction(component));
        }

        return mutableComponent;
    }

    @Override
    public IChatBaseComponent getDisplayName() {
        IChatBaseComponent component = this.getCustomName();
        return component != null ? removeAction(component) : this.getTypeName();
    }

    protected IChatBaseComponent getTypeName() {
        return this.type.getDescription();
    }

    public boolean is(Entity entity) {
        return this == entity;
    }

    public float getHeadRotation() {
        return 0.0F;
    }

    public void setHeadRotation(float headYaw) {
    }

    public void setYBodyRot(float bodyYaw) {
    }

    public boolean isAttackable() {
        return true;
    }

    public boolean skipAttackInteraction(Entity attacker) {
        return false;
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%s['%s'/%d, l='%s', x=%.2f, y=%.2f, z=%.2f]", this.getClass().getSimpleName(), this.getDisplayName().getString(), this.id, this.level == null ? "~NULL~" : this.level.toString(), this.locX(), this.locY(), this.locZ());
    }

    public boolean isInvulnerable(DamageSource damageSource) {
        return this.isRemoved() || this.invulnerable && damageSource != DamageSource.OUT_OF_WORLD && !damageSource.isCreativePlayer();
    }

    public boolean isInvulnerable() {
        return this.invulnerable;
    }

    public void setInvulnerable(boolean invulnerable) {
        this.invulnerable = invulnerable;
    }

    public void copyPosition(Entity entity) {
        this.setPositionRotation(entity.locX(), entity.locY(), entity.locZ(), entity.getYRot(), entity.getXRot());
    }

    public void restoreFrom(Entity original) {
        NBTTagCompound compoundTag = original.save(new NBTTagCompound());
        compoundTag.remove("Dimension");
        this.load(compoundTag);
        this.portalCooldown = original.portalCooldown;
        this.portalEntrancePos = original.portalEntrancePos;
    }

    @Nullable
    public Entity changeDimension(WorldServer destination) {
        if (this.level instanceof WorldServer && !this.isRemoved()) {
            this.level.getMethodProfiler().enter("changeDimension");
            this.decouple();
            this.level.getMethodProfiler().enter("reposition");
            ShapeDetectorShape portalInfo = this.findDimensionEntryPoint(destination);
            if (portalInfo == null) {
                return null;
            } else {
                this.level.getMethodProfiler().exitEnter("reloading");
                Entity entity = this.getEntityType().create(destination);
                if (entity != null) {
                    entity.restoreFrom(this);
                    entity.setPositionRotation(portalInfo.pos.x, portalInfo.pos.y, portalInfo.pos.z, portalInfo.yRot, entity.getXRot());
                    entity.setMot(portalInfo.speed);
                    destination.addEntityTeleport(entity);
                    if (destination.getDimensionKey() == World.END) {
                        WorldServer.makeObsidianPlatform(destination);
                    }
                }

                this.removeAfterChangingDimensions();
                this.level.getMethodProfiler().exit();
                ((WorldServer)this.level).resetEmptyTime();
                destination.resetEmptyTime();
                this.level.getMethodProfiler().exit();
                return entity;
            }
        } else {
            return null;
        }
    }

    protected void removeAfterChangingDimensions() {
        this.setRemoved(Entity.RemovalReason.CHANGED_DIMENSION);
    }

    @Nullable
    protected ShapeDetectorShape findDimensionEntryPoint(WorldServer destination) {
        boolean bl = this.level.getDimensionKey() == World.END && destination.getDimensionKey() == World.OVERWORLD;
        boolean bl2 = destination.getDimensionKey() == World.END;
        if (!bl && !bl2) {
            boolean bl3 = destination.getDimensionKey() == World.NETHER;
            if (this.level.getDimensionKey() != World.NETHER && !bl3) {
                return null;
            } else {
                WorldBorder worldBorder = destination.getWorldBorder();
                double d = Math.max(-2.9999872E7D, worldBorder.getMinX() + 16.0D);
                double e = Math.max(-2.9999872E7D, worldBorder.getMinZ() + 16.0D);
                double f = Math.min(2.9999872E7D, worldBorder.getMaxX() - 16.0D);
                double g = Math.min(2.9999872E7D, worldBorder.getMaxZ() - 16.0D);
                double h = DimensionManager.getTeleportationScale(this.level.getDimensionManager(), destination.getDimensionManager());
                BlockPosition blockPos3 = new BlockPosition(MathHelper.clamp(this.locX() * h, d, f), this.locY(), MathHelper.clamp(this.locZ() * h, e, g));
                return this.findOrCreatePortal(destination, blockPos3, bl3).map((rect) -> {
                    IBlockData blockState = this.level.getType(this.portalEntrancePos);
                    EnumDirection.EnumAxis axis;
                    Vec3D vec3;
                    if (blockState.hasProperty(BlockProperties.HORIZONTAL_AXIS)) {
                        axis = blockState.get(BlockProperties.HORIZONTAL_AXIS);
                        BlockUtil.Rectangle foundRectangle = BlockUtil.getLargestRectangleAround(this.portalEntrancePos, axis, 21, EnumDirection.EnumAxis.Y, 21, (blockPos) -> {
                            return this.level.getType(blockPos) == blockState;
                        });
                        vec3 = this.getRelativePortalPosition(axis, foundRectangle);
                    } else {
                        axis = EnumDirection.EnumAxis.X;
                        vec3 = new Vec3D(0.5D, 0.0D, 0.0D);
                    }

                    return BlockPortalShape.createPortalInfo(destination, rect, axis, vec3, this.getDimensions(this.getPose()), this.getMot(), this.getYRot(), this.getXRot());
                }).orElse((ShapeDetectorShape)null);
            }
        } else {
            BlockPosition blockPos;
            if (bl2) {
                blockPos = WorldServer.END_SPAWN_POINT;
            } else {
                blockPos = destination.getHighestBlockYAt(HeightMap.Type.MOTION_BLOCKING_NO_LEAVES, destination.getSpawn());
            }

            return new ShapeDetectorShape(new Vec3D((double)blockPos.getX() + 0.5D, (double)blockPos.getY(), (double)blockPos.getZ() + 0.5D), this.getMot(), this.getYRot(), this.getXRot());
        }
    }

    protected Vec3D getRelativePortalPosition(EnumDirection.EnumAxis portalAxis, BlockUtil.Rectangle portalRect) {
        return BlockPortalShape.getRelativePosition(portalRect, portalAxis, this.getPositionVector(), this.getDimensions(this.getPose()));
    }

    protected Optional<BlockUtil.Rectangle> findOrCreatePortal(WorldServer destWorld, BlockPosition destPos, boolean destIsNether) {
        return destWorld.getTravelAgent().findPortal(destPos, destIsNether);
    }

    public boolean canPortal() {
        return true;
    }

    public float getBlockExplosionResistance(Explosion explosion, IBlockAccess world, BlockPosition pos, IBlockData blockState, Fluid fluidState, float max) {
        return max;
    }

    public boolean shouldBlockExplode(Explosion explosion, IBlockAccess world, BlockPosition pos, IBlockData state, float explosionPower) {
        return true;
    }

    public int getMaxFallDistance() {
        return 3;
    }

    public boolean isIgnoreBlockTrigger() {
        return false;
    }

    public void appendEntityCrashDetails(CrashReportSystemDetails section) {
        section.setDetail("Entity Type", () -> {
            return EntityTypes.getName(this.getEntityType()) + " (" + this.getClass().getCanonicalName() + ")";
        });
        section.setDetail("Entity ID", this.id);
        section.setDetail("Entity Name", () -> {
            return this.getDisplayName().getString();
        });
        section.setDetail("Entity's Exact location", String.format(Locale.ROOT, "%.2f, %.2f, %.2f", this.locX(), this.locY(), this.locZ()));
        section.setDetail("Entity's Block location", CrashReportSystemDetails.formatLocation(this.level, MathHelper.floor(this.locX()), MathHelper.floor(this.locY()), MathHelper.floor(this.locZ())));
        Vec3D vec3 = this.getMot();
        section.setDetail("Entity's Momentum", String.format(Locale.ROOT, "%.2f, %.2f, %.2f", vec3.x, vec3.y, vec3.z));
        section.setDetail("Entity's Passengers", () -> {
            return this.getPassengers().toString();
        });
        section.setDetail("Entity's Vehicle", () -> {
            return String.valueOf((Object)this.getVehicle());
        });
    }

    public boolean displayFireAnimation() {
        return this.isBurning() && !this.isSpectator();
    }

    public void setUUID(UUID uuid) {
        this.uuid = uuid;
        this.stringUUID = this.uuid.toString();
    }

    @Override
    public UUID getUniqueID() {
        return this.uuid;
    }

    public String getUniqueIDString() {
        return this.stringUUID;
    }

    public String getName() {
        return this.stringUUID;
    }

    public boolean isPushedByFluid() {
        return true;
    }

    public static double getViewScale() {
        return viewScale;
    }

    public static void setViewScale(double value) {
        viewScale = value;
    }

    @Override
    public IChatBaseComponent getScoreboardDisplayName() {
        return ScoreboardTeam.formatNameForTeam(this.getScoreboardTeam(), this.getDisplayName()).format((style) -> {
            return style.setChatHoverable(this.createHoverEvent()).setInsertion(this.getUniqueIDString());
        });
    }

    public void setCustomName(@Nullable IChatBaseComponent name) {
        this.entityData.set(DATA_CUSTOM_NAME, Optional.ofNullable(name));
    }

    @Nullable
    @Override
    public IChatBaseComponent getCustomName() {
        return this.entityData.get(DATA_CUSTOM_NAME).orElse((IChatBaseComponent)null);
    }

    @Override
    public boolean hasCustomName() {
        return this.entityData.get(DATA_CUSTOM_NAME).isPresent();
    }

    public void setCustomNameVisible(boolean visible) {
        this.entityData.set(DATA_CUSTOM_NAME_VISIBLE, visible);
    }

    public boolean getCustomNameVisible() {
        return this.entityData.get(DATA_CUSTOM_NAME_VISIBLE);
    }

    public final void enderTeleportAndLoad(double destX, double destY, double destZ) {
        if (this.level instanceof WorldServer) {
            ChunkCoordIntPair chunkPos = new ChunkCoordIntPair(new BlockPosition(destX, destY, destZ));
            ((WorldServer)this.level).getChunkSource().addTicket(TicketType.POST_TELEPORT, chunkPos, 0, this.getId());
            this.level.getChunk(chunkPos.x, chunkPos.z);
            this.enderTeleportTo(destX, destY, destZ);
        }
    }

    public void dismountTo(double destX, double destY, double destZ) {
        this.enderTeleportTo(destX, destY, destZ);
    }

    public void enderTeleportTo(double destX, double destY, double destZ) {
        if (this.level instanceof WorldServer) {
            this.setPositionRotation(destX, destY, destZ, this.getYRot(), this.getXRot());
            this.recursiveStream().forEach((entity) -> {
                for(Entity entity2 : entity.passengers) {
                    entity.positionRider(entity2, Entity::teleportAndSync);
                }

            });
        }
    }

    public boolean shouldShowName() {
        return this.getCustomNameVisible();
    }

    public void onSyncedDataUpdated(DataWatcherObject<?> data) {
        if (DATA_POSE.equals(data)) {
            this.updateSize();
        }

    }

    public void updateSize() {
        EntitySize entityDimensions = this.dimensions;
        EntityPose pose = this.getPose();
        EntitySize entityDimensions2 = this.getDimensions(pose);
        this.dimensions = entityDimensions2;
        this.eyeHeight = this.getHeadHeight(pose, entityDimensions2);
        this.reapplyPosition();
        boolean bl = (double)entityDimensions2.width <= 4.0D && (double)entityDimensions2.height <= 4.0D;
        if (!this.level.isClientSide && !this.firstTick && !this.noPhysics && bl && (entityDimensions2.width > entityDimensions.width || entityDimensions2.height > entityDimensions.height) && !(this instanceof EntityHuman)) {
            Vec3D vec3 = this.getPositionVector().add(0.0D, (double)entityDimensions.height / 2.0D, 0.0D);
            double d = (double)Math.max(0.0F, entityDimensions2.width - entityDimensions.width) + 1.0E-6D;
            double e = (double)Math.max(0.0F, entityDimensions2.height - entityDimensions.height) + 1.0E-6D;
            VoxelShape voxelShape = VoxelShapes.create(AxisAlignedBB.ofSize(vec3, d, e, d));
            this.level.findFreePosition(this, voxelShape, vec3, (double)entityDimensions2.width, (double)entityDimensions2.height, (double)entityDimensions2.width).ifPresent((pos) -> {
                this.setPos(pos.add(0.0D, (double)(-entityDimensions2.height) / 2.0D, 0.0D));
            });
        }

    }

    public EnumDirection getDirection() {
        return EnumDirection.fromAngle((double)this.getYRot());
    }

    public EnumDirection getAdjustedDirection() {
        return this.getDirection();
    }

    protected ChatHoverable createHoverEvent() {
        return new ChatHoverable(ChatHoverable.EnumHoverAction.SHOW_ENTITY, new ChatHoverable.EntityTooltipInfo(this.getEntityType(), this.getUniqueID(), this.getDisplayName()));
    }

    public boolean broadcastToPlayer(EntityPlayer spectator) {
        return true;
    }

    @Override
    public final AxisAlignedBB getBoundingBox() {
        return this.bb;
    }

    public AxisAlignedBB getBoundingBoxForCulling() {
        return this.getBoundingBox();
    }

    protected AxisAlignedBB getBoundingBoxForPose(EntityPose pos) {
        EntitySize entityDimensions = this.getDimensions(pos);
        float f = entityDimensions.width / 2.0F;
        Vec3D vec3 = new Vec3D(this.locX() - (double)f, this.locY(), this.locZ() - (double)f);
        Vec3D vec32 = new Vec3D(this.locX() + (double)f, this.locY() + (double)entityDimensions.height, this.locZ() + (double)f);
        return new AxisAlignedBB(vec3, vec32);
    }

    public final void setBoundingBox(AxisAlignedBB boundingBox) {
        this.bb = boundingBox;
    }

    protected float getHeadHeight(EntityPose pose, EntitySize dimensions) {
        return dimensions.height * 0.85F;
    }

    public float getEyeHeight(EntityPose pose) {
        return this.getHeadHeight(pose, this.getDimensions(pose));
    }

    public final float getHeadHeight() {
        return this.eyeHeight;
    }

    public Vec3D getLeashOffset() {
        return new Vec3D(0.0D, (double)this.getHeadHeight(), (double)(this.getWidth() * 0.4F));
    }

    public SlotAccess getSlot(int mappedIndex) {
        return SlotAccess.NULL;
    }

    @Override
    public void sendMessage(IChatBaseComponent message, UUID sender) {
    }

    public World getCommandSenderWorld() {
        return this.level;
    }

    @Nullable
    public MinecraftServer getMinecraftServer() {
        return this.level.getMinecraftServer();
    }

    public EnumInteractionResult interactAt(EntityHuman player, Vec3D hitPos, EnumHand hand) {
        return EnumInteractionResult.PASS;
    }

    public boolean ignoreExplosion() {
        return false;
    }

    public void doEnchantDamageEffects(EntityLiving attacker, Entity target) {
        if (target instanceof EntityLiving) {
            EnchantmentManager.doPostHurtEffects((EntityLiving)target, attacker);
        }

        EnchantmentManager.doPostDamageEffects(attacker, target);
    }

    public void startSeenByPlayer(EntityPlayer player) {
    }

    public void stopSeenByPlayer(EntityPlayer player) {
    }

    public float rotate(EnumBlockRotation rotation) {
        float f = MathHelper.wrapDegrees(this.getYRot());
        switch(rotation) {
        case CLOCKWISE_180:
            return f + 180.0F;
        case COUNTERCLOCKWISE_90:
            return f + 270.0F;
        case CLOCKWISE_90:
            return f + 90.0F;
        default:
            return f;
        }
    }

    public float mirror(EnumBlockMirror mirror) {
        float f = MathHelper.wrapDegrees(this.getYRot());
        switch(mirror) {
        case LEFT_RIGHT:
            return -f;
        case FRONT_BACK:
            return 180.0F - f;
        default:
            return f;
        }
    }

    public boolean onlyOpCanSetNbt() {
        return false;
    }

    @Nullable
    public Entity getRidingPassenger() {
        return null;
    }

    public final List<Entity> getPassengers() {
        return this.passengers;
    }

    @Nullable
    public Entity getFirstPassenger() {
        return this.passengers.isEmpty() ? null : this.passengers.get(0);
    }

    public boolean hasPassenger(Entity passenger) {
        return this.passengers.contains(passenger);
    }

    public boolean hasPassenger(Predicate<Entity> predicate) {
        for(Entity entity : this.passengers) {
            if (predicate.test(entity)) {
                return true;
            }
        }

        return false;
    }

    private Stream<Entity> getIndirectPassengersStream() {
        return this.passengers.stream().flatMap(Entity::recursiveStream);
    }

    @Override
    public Stream<Entity> recursiveStream() {
        return Stream.concat(Stream.of(this), this.getIndirectPassengersStream());
    }

    @Override
    public Stream<Entity> getPassengersAndSelf() {
        return Stream.concat(this.passengers.stream().flatMap(Entity::getPassengersAndSelf), Stream.of(this));
    }

    public Iterable<Entity> getAllPassengers() {
        return () -> {
            return this.getIndirectPassengersStream().iterator();
        };
    }

    public boolean hasSinglePlayerPassenger() {
        return this.getIndirectPassengersStream().filter((entity) -> {
            return entity instanceof EntityHuman;
        }).count() == 1L;
    }

    public Entity getRootVehicle() {
        Entity entity;
        for(entity = this; entity.isPassenger(); entity = entity.getVehicle()) {
        }

        return entity;
    }

    public boolean isSameVehicle(Entity entity) {
        return this.getRootVehicle() == entity.getRootVehicle();
    }

    public boolean hasIndirectPassenger(Entity passenger) {
        return this.getIndirectPassengersStream().anyMatch((entity) -> {
            return entity == passenger;
        });
    }

    public boolean isControlledByLocalInstance() {
        Entity entity = this.getRidingPassenger();
        if (entity instanceof EntityHuman) {
            return ((EntityHuman)entity).isLocalPlayer();
        } else {
            return !this.level.isClientSide;
        }
    }

    protected static Vec3D getCollisionHorizontalEscapeVector(double vehicleWidth, double passengerWidth, float passengerYaw) {
        double d = (vehicleWidth + passengerWidth + (double)1.0E-5F) / 2.0D;
        float f = -MathHelper.sin(passengerYaw * ((float)Math.PI / 180F));
        float g = MathHelper.cos(passengerYaw * ((float)Math.PI / 180F));
        float h = Math.max(Math.abs(f), Math.abs(g));
        return new Vec3D((double)f * d / (double)h, 0.0D, (double)g * d / (double)h);
    }

    public Vec3D getDismountLocationForPassenger(EntityLiving passenger) {
        return new Vec3D(this.locX(), this.getBoundingBox().maxY, this.locZ());
    }

    @Nullable
    public Entity getVehicle() {
        return this.vehicle;
    }

    public EnumPistonReaction getPushReaction() {
        return EnumPistonReaction.NORMAL;
    }

    public SoundCategory getSoundCategory() {
        return SoundCategory.NEUTRAL;
    }

    public int getMaxFireTicks() {
        return 1;
    }

    public CommandListenerWrapper getCommandListener() {
        return new CommandListenerWrapper(this, this.getPositionVector(), this.getRotationVector(), this.level instanceof WorldServer ? (WorldServer)this.level : null, this.getPermissionLevel(), this.getDisplayName().getString(), this.getScoreboardDisplayName(), this.level.getMinecraftServer(), this);
    }

    protected int getPermissionLevel() {
        return 0;
    }

    public boolean hasPermissions(int permissionLevel) {
        return this.getPermissionLevel() >= permissionLevel;
    }

    @Override
    public boolean shouldSendSuccess() {
        return this.level.getGameRules().getBoolean(GameRules.RULE_SENDCOMMANDFEEDBACK);
    }

    @Override
    public boolean shouldSendFailure() {
        return true;
    }

    @Override
    public boolean shouldBroadcastCommands() {
        return true;
    }

    public void lookAt(ArgumentAnchor.Anchor anchorPoint, Vec3D target) {
        Vec3D vec3 = anchorPoint.apply(this);
        double d = target.x - vec3.x;
        double e = target.y - vec3.y;
        double f = target.z - vec3.z;
        double g = Math.sqrt(d * d + f * f);
        this.setXRot(MathHelper.wrapDegrees((float)(-(MathHelper.atan2(e, g) * (double)(180F / (float)Math.PI)))));
        this.setYRot(MathHelper.wrapDegrees((float)(MathHelper.atan2(f, d) * (double)(180F / (float)Math.PI)) - 90.0F));
        this.setHeadRotation(this.getYRot());
        this.xRotO = this.getXRot();
        this.yRotO = this.getYRot();
    }

    public boolean updateFluidHeightAndDoFluidPushing(Tag<FluidType> tag, double d) {
        if (this.touchingUnloadedChunk()) {
            return false;
        } else {
            AxisAlignedBB aABB = this.getBoundingBox().shrink(0.001D);
            int i = MathHelper.floor(aABB.minX);
            int j = MathHelper.ceil(aABB.maxX);
            int k = MathHelper.floor(aABB.minY);
            int l = MathHelper.ceil(aABB.maxY);
            int m = MathHelper.floor(aABB.minZ);
            int n = MathHelper.ceil(aABB.maxZ);
            double e = 0.0D;
            boolean bl = this.isPushedByFluid();
            boolean bl2 = false;
            Vec3D vec3 = Vec3D.ZERO;
            int o = 0;
            BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();

            for(int p = i; p < j; ++p) {
                for(int q = k; q < l; ++q) {
                    for(int r = m; r < n; ++r) {
                        mutableBlockPos.set(p, q, r);
                        Fluid fluidState = this.level.getFluid(mutableBlockPos);
                        if (fluidState.is(tag)) {
                            double f = (double)((float)q + fluidState.getHeight(this.level, mutableBlockPos));
                            if (f >= aABB.minY) {
                                bl2 = true;
                                e = Math.max(f - aABB.minY, e);
                                if (bl) {
                                    Vec3D vec32 = fluidState.getFlow(this.level, mutableBlockPos);
                                    if (e < 0.4D) {
                                        vec32 = vec32.scale(e);
                                    }

                                    vec3 = vec3.add(vec32);
                                    ++o;
                                }
                            }
                        }
                    }
                }
            }

            if (vec3.length() > 0.0D) {
                if (o > 0) {
                    vec3 = vec3.scale(1.0D / (double)o);
                }

                if (!(this instanceof EntityHuman)) {
                    vec3 = vec3.normalize();
                }

                Vec3D vec33 = this.getMot();
                vec3 = vec3.scale(d * 1.0D);
                double g = 0.003D;
                if (Math.abs(vec33.x) < 0.003D && Math.abs(vec33.z) < 0.003D && vec3.length() < 0.0045000000000000005D) {
                    vec3 = vec3.normalize().scale(0.0045000000000000005D);
                }

                this.setMot(this.getMot().add(vec3));
            }

            this.fluidHeight.put(tag, e);
            return bl2;
        }
    }

    public boolean touchingUnloadedChunk() {
        AxisAlignedBB aABB = this.getBoundingBox().inflate(1.0D);
        int i = MathHelper.floor(aABB.minX);
        int j = MathHelper.ceil(aABB.maxX);
        int k = MathHelper.floor(aABB.minZ);
        int l = MathHelper.ceil(aABB.maxZ);
        return !this.level.hasChunksAt(i, k, j, l);
    }

    public double getFluidHeight(Tag<FluidType> fluid) {
        return this.fluidHeight.getDouble(fluid);
    }

    public double getFluidJumpThreshold() {
        return (double)this.getHeadHeight() < 0.4D ? 0.0D : 0.4D;
    }

    public final float getWidth() {
        return this.dimensions.width;
    }

    public final float getHeight() {
        return this.dimensions.height;
    }

    public abstract Packet<?> getPacket();

    public EntitySize getDimensions(EntityPose pose) {
        return this.type.getDimensions();
    }

    public Vec3D getPositionVector() {
        return this.position;
    }

    @Override
    public BlockPosition getChunkCoordinates() {
        return this.blockPosition;
    }

    public IBlockData getFeetBlockState() {
        return this.level.getType(this.getChunkCoordinates());
    }

    public BlockPosition eyeBlockPosition() {
        return new BlockPosition(this.getEyePosition(1.0F));
    }

    public ChunkCoordIntPair chunkPosition() {
        return new ChunkCoordIntPair(this.blockPosition);
    }

    public Vec3D getMot() {
        return this.deltaMovement;
    }

    public void setMot(Vec3D velocity) {
        this.deltaMovement = velocity;
    }

    public void setMot(double x, double y, double z) {
        this.setMot(new Vec3D(x, y, z));
    }

    public final int getBlockX() {
        return this.blockPosition.getX();
    }

    public final double locX() {
        return this.position.x;
    }

    public double getX(double widthScale) {
        return this.position.x + (double)this.getWidth() * widthScale;
    }

    public double getRandomX(double widthScale) {
        return this.getX((2.0D * this.random.nextDouble() - 1.0D) * widthScale);
    }

    public final int getBlockY() {
        return this.blockPosition.getY();
    }

    public final double locY() {
        return this.position.y;
    }

    public double getY(double heightScale) {
        return this.position.y + (double)this.getHeight() * heightScale;
    }

    public double getRandomY() {
        return this.getY(this.random.nextDouble());
    }

    public double getHeadY() {
        return this.position.y + (double)this.eyeHeight;
    }

    public final int getBlockZ() {
        return this.blockPosition.getZ();
    }

    public final double locZ() {
        return this.position.z;
    }

    public double getZ(double widthScale) {
        return this.position.z + (double)this.getWidth() * widthScale;
    }

    public double getRandomZ(double widthScale) {
        return this.getZ((2.0D * this.random.nextDouble() - 1.0D) * widthScale);
    }

    public final void setPositionRaw(double x, double y, double z) {
        if (this.position.x != x || this.position.y != y || this.position.z != z) {
            this.position = new Vec3D(x, y, z);
            int i = MathHelper.floor(x);
            int j = MathHelper.floor(y);
            int k = MathHelper.floor(z);
            if (i != this.blockPosition.getX() || j != this.blockPosition.getY() || k != this.blockPosition.getZ()) {
                this.blockPosition = new BlockPosition(i, j, k);
            }

            this.levelCallback.onMove();
            GameEventListenerRegistrar gameEventListenerRegistrar = this.getGameEventListenerRegistrar();
            if (gameEventListenerRegistrar != null) {
                gameEventListenerRegistrar.onListenerMove(this.level);
            }
        }

    }

    public void checkDespawn() {
    }

    public Vec3D getRopeHoldPosition(float f) {
        return this.getPosition(f).add(0.0D, (double)this.eyeHeight * 0.7D, 0.0D);
    }

    public void recreateFromPacket(PacketPlayOutSpawnEntity packet) {
        int i = packet.getId();
        double d = packet.getX();
        double e = packet.getY();
        double f = packet.getZ();
        this.setPacketCoordinates(d, e, f);
        this.teleportAndSync(d, e, f);
        this.setXRot((float)(packet.getxRot() * 360) / 256.0F);
        this.setYRot((float)(packet.getyRot() * 360) / 256.0F);
        this.setId(i);
        this.setUUID(packet.getUUID());
    }

    @Nullable
    public ItemStack getPickResult() {
        return null;
    }

    public void setIsInPowderSnow(boolean inPowderSnow) {
        this.isInPowderSnow = inPowderSnow;
    }

    public boolean canFreeze() {
        return !TagsEntity.FREEZE_IMMUNE_ENTITY_TYPES.isTagged(this.getEntityType());
    }

    public float getYRot() {
        return this.yRot;
    }

    public void setYRot(float yaw) {
        if (!Float.isFinite(yaw)) {
            SystemUtils.logAndPauseIfInIde("Invalid entity rotation: " + yaw + ", discarding.");
        } else {
            this.yRot = yaw;
        }
    }

    public float getXRot() {
        return this.xRot;
    }

    public void setXRot(float pitch) {
        if (!Float.isFinite(pitch)) {
            SystemUtils.logAndPauseIfInIde("Invalid entity rotation: " + pitch + ", discarding.");
        } else {
            this.xRot = pitch;
        }
    }

    public final boolean isRemoved() {
        return this.removalReason != null;
    }

    @Nullable
    public Entity.RemovalReason getRemovalReason() {
        return this.removalReason;
    }

    @Override
    public final void setRemoved(Entity.RemovalReason reason) {
        if (this.removalReason == null) {
            this.removalReason = reason;
        }

        if (this.removalReason.shouldDestroy()) {
            this.stopRiding();
        }

        this.getPassengers().forEach(Entity::stopRiding);
        this.levelCallback.onRemove(reason);
    }

    public void unsetRemoved() {
        this.removalReason = null;
    }

    @Override
    public void setWorldCallback(IEntityCallback listener) {
        this.levelCallback = listener;
    }

    @Override
    public boolean shouldBeSaved() {
        if (this.removalReason != null && !this.removalReason.shouldSave()) {
            return false;
        } else if (this.isPassenger()) {
            return false;
        } else {
            return !this.isVehicle() || !this.hasSinglePlayerPassenger();
        }
    }

    @Override
    public boolean isAlwaysTicking() {
        return false;
    }

    public boolean mayInteract(World world, BlockPosition pos) {
        return true;
    }

    @FunctionalInterface
    public interface MoveFunction {
        void accept(Entity entity, double x, double y, double z);
    }

    public static enum MovementEmission {
        NONE(false, false),
        SOUNDS(true, false),
        EVENTS(false, true),
        ALL(true, true);

        final boolean sounds;
        final boolean events;

        private MovementEmission(boolean sounds, boolean events) {
            this.sounds = sounds;
            this.events = events;
        }

        public boolean emitsAnything() {
            return this.events || this.sounds;
        }

        public boolean emitsEvents() {
            return this.events;
        }

        public boolean emitsSounds() {
            return this.sounds;
        }
    }

    public static enum RemovalReason {
        KILLED(true, false),
        DISCARDED(true, false),
        UNLOADED_TO_CHUNK(false, true),
        UNLOADED_WITH_PLAYER(false, false),
        CHANGED_DIMENSION(false, false);

        private final boolean destroy;
        private final boolean save;

        private RemovalReason(boolean destroy, boolean save) {
            this.destroy = destroy;
            this.save = save;
        }

        public boolean shouldDestroy() {
            return this.destroy;
        }

        public boolean shouldSave() {
            return this.save;
        }
    }
}
