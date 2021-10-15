package net.minecraft.world.entity.vehicle;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.BlockUtil;
import net.minecraft.SystemUtils;
import net.minecraft.core.BaseBlockPosition;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntity;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.tags.TagsBlock;
import net.minecraft.util.MathHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.EntitySize;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMoveType;
import net.minecraft.world.entity.IEntitySelector;
import net.minecraft.world.entity.animal.EntityIronGolem;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockMinecartTrackAbstract;
import net.minecraft.world.level.block.BlockPoweredRail;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockPropertyTrackPosition;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.Vec3D;

public abstract class EntityMinecartAbstract extends Entity {
    private static final DataWatcherObject<Integer> DATA_ID_HURT = DataWatcher.defineId(EntityMinecartAbstract.class, DataWatcherRegistry.INT);
    private static final DataWatcherObject<Integer> DATA_ID_HURTDIR = DataWatcher.defineId(EntityMinecartAbstract.class, DataWatcherRegistry.INT);
    private static final DataWatcherObject<Float> DATA_ID_DAMAGE = DataWatcher.defineId(EntityMinecartAbstract.class, DataWatcherRegistry.FLOAT);
    private static final DataWatcherObject<Integer> DATA_ID_DISPLAY_BLOCK = DataWatcher.defineId(EntityMinecartAbstract.class, DataWatcherRegistry.INT);
    private static final DataWatcherObject<Integer> DATA_ID_DISPLAY_OFFSET = DataWatcher.defineId(EntityMinecartAbstract.class, DataWatcherRegistry.INT);
    private static final DataWatcherObject<Boolean> DATA_ID_CUSTOM_DISPLAY = DataWatcher.defineId(EntityMinecartAbstract.class, DataWatcherRegistry.BOOLEAN);
    private static final ImmutableMap<EntityPose, ImmutableList<Integer>> POSE_DISMOUNT_HEIGHTS = ImmutableMap.of(EntityPose.STANDING, ImmutableList.of(0, 1, -1), EntityPose.CROUCHING, ImmutableList.of(0, 1, -1), EntityPose.SWIMMING, ImmutableList.of(0, 1));
    protected static final float WATER_SLOWDOWN_FACTOR = 0.95F;
    private boolean flipped;
    private static final Map<BlockPropertyTrackPosition, Pair<BaseBlockPosition, BaseBlockPosition>> EXITS = SystemUtils.make(Maps.newEnumMap(BlockPropertyTrackPosition.class), (map) -> {
        BaseBlockPosition vec3i = EnumDirection.WEST.getNormal();
        BaseBlockPosition vec3i2 = EnumDirection.EAST.getNormal();
        BaseBlockPosition vec3i3 = EnumDirection.NORTH.getNormal();
        BaseBlockPosition vec3i4 = EnumDirection.SOUTH.getNormal();
        BaseBlockPosition vec3i5 = vec3i.down();
        BaseBlockPosition vec3i6 = vec3i2.down();
        BaseBlockPosition vec3i7 = vec3i3.down();
        BaseBlockPosition vec3i8 = vec3i4.down();
        map.put(BlockPropertyTrackPosition.NORTH_SOUTH, Pair.of(vec3i3, vec3i4));
        map.put(BlockPropertyTrackPosition.EAST_WEST, Pair.of(vec3i, vec3i2));
        map.put(BlockPropertyTrackPosition.ASCENDING_EAST, Pair.of(vec3i5, vec3i2));
        map.put(BlockPropertyTrackPosition.ASCENDING_WEST, Pair.of(vec3i, vec3i6));
        map.put(BlockPropertyTrackPosition.ASCENDING_NORTH, Pair.of(vec3i3, vec3i8));
        map.put(BlockPropertyTrackPosition.ASCENDING_SOUTH, Pair.of(vec3i7, vec3i4));
        map.put(BlockPropertyTrackPosition.SOUTH_EAST, Pair.of(vec3i4, vec3i2));
        map.put(BlockPropertyTrackPosition.SOUTH_WEST, Pair.of(vec3i4, vec3i));
        map.put(BlockPropertyTrackPosition.NORTH_WEST, Pair.of(vec3i3, vec3i));
        map.put(BlockPropertyTrackPosition.NORTH_EAST, Pair.of(vec3i3, vec3i2));
    });
    private int lSteps;
    private double lx;
    private double ly;
    private double lz;
    private double lyr;
    private double lxr;
    private double lxd;
    private double lyd;
    private double lzd;

    protected EntityMinecartAbstract(EntityTypes<?> type, World world) {
        super(type, world);
        this.blocksBuilding = true;
    }

    protected EntityMinecartAbstract(EntityTypes<?> type, World world, double x, double y, double z) {
        this(type, world);
        this.setPosition(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    public static EntityMinecartAbstract createMinecart(World world, double x, double y, double z, EntityMinecartAbstract.EnumMinecartType type) {
        if (type == EntityMinecartAbstract.EnumMinecartType.CHEST) {
            return new EntityMinecartChest(world, x, y, z);
        } else if (type == EntityMinecartAbstract.EnumMinecartType.FURNACE) {
            return new EntityMinecartFurnace(world, x, y, z);
        } else if (type == EntityMinecartAbstract.EnumMinecartType.TNT) {
            return new EntityMinecartTNT(world, x, y, z);
        } else if (type == EntityMinecartAbstract.EnumMinecartType.SPAWNER) {
            return new EntityMinecartMobSpawner(world, x, y, z);
        } else if (type == EntityMinecartAbstract.EnumMinecartType.HOPPER) {
            return new EntityMinecartHopper(world, x, y, z);
        } else {
            return (EntityMinecartAbstract)(type == EntityMinecartAbstract.EnumMinecartType.COMMAND_BLOCK ? new EntityMinecartCommandBlock(world, x, y, z) : new EntityMinecartRideable(world, x, y, z));
        }
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.EVENTS;
    }

    @Override
    protected void initDatawatcher() {
        this.entityData.register(DATA_ID_HURT, 0);
        this.entityData.register(DATA_ID_HURTDIR, 1);
        this.entityData.register(DATA_ID_DAMAGE, 0.0F);
        this.entityData.register(DATA_ID_DISPLAY_BLOCK, Block.getCombinedId(Blocks.AIR.getBlockData()));
        this.entityData.register(DATA_ID_DISPLAY_OFFSET, 6);
        this.entityData.register(DATA_ID_CUSTOM_DISPLAY, false);
    }

    @Override
    public boolean canCollideWith(Entity other) {
        return EntityBoat.canVehicleCollide(this, other);
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
        return 0.0D;
    }

    @Override
    public Vec3D getDismountLocationForPassenger(EntityLiving passenger) {
        EnumDirection direction = this.getAdjustedDirection();
        if (direction.getAxis() == EnumDirection.EnumAxis.Y) {
            return super.getDismountLocationForPassenger(passenger);
        } else {
            int[][] is = DismountUtil.offsetsForDirection(direction);
            BlockPosition blockPos = this.getChunkCoordinates();
            BlockPosition.MutableBlockPosition mutableBlockPos = new BlockPosition.MutableBlockPosition();
            ImmutableList<EntityPose> immutableList = passenger.getDismountPoses();

            for(EntityPose pose : immutableList) {
                EntitySize entityDimensions = passenger.getDimensions(pose);
                float f = Math.min(entityDimensions.width, 1.0F) / 2.0F;

                for(int i : POSE_DISMOUNT_HEIGHTS.get(pose)) {
                    for(int[] js : is) {
                        mutableBlockPos.set(blockPos.getX() + js[0], blockPos.getY() + i, blockPos.getZ() + js[1]);
                        double d = this.level.getBlockFloorHeight(DismountUtil.nonClimbableShape(this.level, mutableBlockPos), () -> {
                            return DismountUtil.nonClimbableShape(this.level, mutableBlockPos.below());
                        });
                        if (DismountUtil.isBlockFloorValid(d)) {
                            AxisAlignedBB aABB = new AxisAlignedBB((double)(-f), 0.0D, (double)(-f), (double)f, (double)entityDimensions.height, (double)f);
                            Vec3D vec3 = Vec3D.upFromBottomCenterOf(mutableBlockPos, d);
                            if (DismountUtil.canDismountTo(this.level, passenger, aABB.move(vec3))) {
                                passenger.setPose(pose);
                                return vec3;
                            }
                        }
                    }
                }
            }

            double e = this.getBoundingBox().maxY;
            mutableBlockPos.set((double)blockPos.getX(), e, (double)blockPos.getZ());

            for(EntityPose pose2 : immutableList) {
                double g = (double)passenger.getDimensions(pose2).height;
                int j = MathHelper.ceil(e - (double)mutableBlockPos.getY() + g);
                double h = DismountUtil.findCeilingFrom(mutableBlockPos, j, (pos) -> {
                    return this.level.getType(pos).getCollisionShape(this.level, pos);
                });
                if (e + g <= h) {
                    passenger.setPose(pose2);
                    break;
                }
            }

            return super.getDismountLocationForPassenger(passenger);
        }
    }

    @Override
    public boolean damageEntity(DamageSource source, float amount) {
        if (!this.level.isClientSide && !this.isRemoved()) {
            if (this.isInvulnerable(source)) {
                return false;
            } else {
                this.setHurtDir(-this.getHurtDir());
                this.setHurtTime(10);
                this.velocityChanged();
                this.setDamage(this.getDamage() + amount * 10.0F);
                this.gameEvent(GameEvent.ENTITY_DAMAGED, source.getEntity());
                boolean bl = source.getEntity() instanceof EntityHuman && ((EntityHuman)source.getEntity()).getAbilities().instabuild;
                if (bl || this.getDamage() > 40.0F) {
                    this.ejectPassengers();
                    if (bl && !this.hasCustomName()) {
                        this.die();
                    } else {
                        this.destroy(source);
                    }
                }

                return true;
            }
        } else {
            return true;
        }
    }

    @Override
    protected float getBlockSpeedFactor() {
        IBlockData blockState = this.level.getType(this.getChunkCoordinates());
        return blockState.is(TagsBlock.RAILS) ? 1.0F : super.getBlockSpeedFactor();
    }

    public void destroy(DamageSource damageSource) {
        this.remove(Entity.RemovalReason.KILLED);
        if (this.level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            ItemStack itemStack = new ItemStack(Items.MINECART);
            if (this.hasCustomName()) {
                itemStack.setHoverName(this.getCustomName());
            }

            this.spawnAtLocation(itemStack);
        }

    }

    @Override
    public void animateHurt() {
        this.setHurtDir(-this.getHurtDir());
        this.setHurtTime(10);
        this.setDamage(this.getDamage() + this.getDamage() * 10.0F);
    }

    @Override
    public boolean isInteractable() {
        return !this.isRemoved();
    }

    private static Pair<BaseBlockPosition, BaseBlockPosition> exits(BlockPropertyTrackPosition shape) {
        return EXITS.get(shape);
    }

    @Override
    public EnumDirection getAdjustedDirection() {
        return this.flipped ? this.getDirection().opposite().getClockWise() : this.getDirection().getClockWise();
    }

    @Override
    public void tick() {
        if (this.getType() > 0) {
            this.setHurtTime(this.getType() - 1);
        }

        if (this.getDamage() > 0.0F) {
            this.setDamage(this.getDamage() - 1.0F);
        }

        this.checkOutOfWorld();
        this.doPortalTick();
        if (this.level.isClientSide) {
            if (this.lSteps > 0) {
                double d = this.locX() + (this.lx - this.locX()) / (double)this.lSteps;
                double e = this.locY() + (this.ly - this.locY()) / (double)this.lSteps;
                double f = this.locZ() + (this.lz - this.locZ()) / (double)this.lSteps;
                double g = MathHelper.wrapDegrees(this.lyr - (double)this.getYRot());
                this.setYRot(this.getYRot() + (float)g / (float)this.lSteps);
                this.setXRot(this.getXRot() + (float)(this.lxr - (double)this.getXRot()) / (float)this.lSteps);
                --this.lSteps;
                this.setPosition(d, e, f);
                this.setYawPitch(this.getYRot(), this.getXRot());
            } else {
                this.reapplyPosition();
                this.setYawPitch(this.getYRot(), this.getXRot());
            }

        } else {
            if (!this.isNoGravity()) {
                double h = this.isInWater() ? -0.005D : -0.04D;
                this.setMot(this.getMot().add(0.0D, h, 0.0D));
            }

            int i = MathHelper.floor(this.locX());
            int j = MathHelper.floor(this.locY());
            int k = MathHelper.floor(this.locZ());
            if (this.level.getType(new BlockPosition(i, j - 1, k)).is(TagsBlock.RAILS)) {
                --j;
            }

            BlockPosition blockPos = new BlockPosition(i, j, k);
            IBlockData blockState = this.level.getType(blockPos);
            if (BlockMinecartTrackAbstract.isRail(blockState)) {
                this.moveAlongTrack(blockPos, blockState);
                if (blockState.is(Blocks.ACTIVATOR_RAIL)) {
                    this.activateMinecart(i, j, k, blockState.get(BlockPoweredRail.POWERED));
                }
            } else {
                this.comeOffTrack();
            }

            this.checkBlockCollisions();
            this.setXRot(0.0F);
            double l = this.xo - this.locX();
            double m = this.zo - this.locZ();
            if (l * l + m * m > 0.001D) {
                this.setYRot((float)(MathHelper.atan2(m, l) * 180.0D / Math.PI));
                if (this.flipped) {
                    this.setYRot(this.getYRot() + 180.0F);
                }
            }

            double n = (double)MathHelper.wrapDegrees(this.getYRot() - this.yRotO);
            if (n < -170.0D || n >= 170.0D) {
                this.setYRot(this.getYRot() + 180.0F);
                this.flipped = !this.flipped;
            }

            this.setYawPitch(this.getYRot(), this.getXRot());
            if (this.getMinecartType() == EntityMinecartAbstract.EnumMinecartType.RIDEABLE && this.getMot().horizontalDistanceSqr() > 0.01D) {
                List<Entity> list = this.level.getEntities(this, this.getBoundingBox().grow((double)0.2F, 0.0D, (double)0.2F), IEntitySelector.pushableBy(this));
                if (!list.isEmpty()) {
                    for(int o = 0; o < list.size(); ++o) {
                        Entity entity = list.get(o);
                        if (!(entity instanceof EntityHuman) && !(entity instanceof EntityIronGolem) && !(entity instanceof EntityMinecartAbstract) && !this.isVehicle() && !entity.isPassenger()) {
                            entity.startRiding(this);
                        } else {
                            entity.collide(this);
                        }
                    }
                }
            } else {
                for(Entity entity2 : this.level.getEntities(this, this.getBoundingBox().grow((double)0.2F, 0.0D, (double)0.2F))) {
                    if (!this.hasPassenger(entity2) && entity2.isCollidable() && entity2 instanceof EntityMinecartAbstract) {
                        entity2.collide(this);
                    }
                }
            }

            this.updateInWaterStateAndDoFluidPushing();
            if (this.isInLava()) {
                this.burnFromLava();
                this.fallDistance *= 0.5F;
            }

            this.firstTick = false;
        }
    }

    protected double getMaxSpeed() {
        return (this.isInWater() ? 4.0D : 8.0D) / 20.0D;
    }

    public void activateMinecart(int x, int y, int z, boolean powered) {
    }

    protected void comeOffTrack() {
        double d = this.getMaxSpeed();
        Vec3D vec3 = this.getMot();
        this.setMot(MathHelper.clamp(vec3.x, -d, d), vec3.y, MathHelper.clamp(vec3.z, -d, d));
        if (this.onGround) {
            this.setMot(this.getMot().scale(0.5D));
        }

        this.move(EnumMoveType.SELF, this.getMot());
        if (!this.onGround) {
            this.setMot(this.getMot().scale(0.95D));
        }

    }

    protected void moveAlongTrack(BlockPosition pos, IBlockData state) {
        this.fallDistance = 0.0F;
        double d = this.locX();
        double e = this.locY();
        double f = this.locZ();
        Vec3D vec3 = this.getPos(d, e, f);
        e = (double)pos.getY();
        boolean bl = false;
        boolean bl2 = false;
        if (state.is(Blocks.POWERED_RAIL)) {
            bl = state.get(BlockPoweredRail.POWERED);
            bl2 = !bl;
        }

        double g = 0.0078125D;
        if (this.isInWater()) {
            g *= 0.2D;
        }

        Vec3D vec32 = this.getMot();
        BlockPropertyTrackPosition railShape = state.get(((BlockMinecartTrackAbstract)state.getBlock()).getShapeProperty());
        switch(railShape) {
        case ASCENDING_EAST:
            this.setMot(vec32.add(-g, 0.0D, 0.0D));
            ++e;
            break;
        case ASCENDING_WEST:
            this.setMot(vec32.add(g, 0.0D, 0.0D));
            ++e;
            break;
        case ASCENDING_NORTH:
            this.setMot(vec32.add(0.0D, 0.0D, g));
            ++e;
            break;
        case ASCENDING_SOUTH:
            this.setMot(vec32.add(0.0D, 0.0D, -g));
            ++e;
        }

        vec32 = this.getMot();
        Pair<BaseBlockPosition, BaseBlockPosition> pair = exits(railShape);
        BaseBlockPosition vec3i = pair.getFirst();
        BaseBlockPosition vec3i2 = pair.getSecond();
        double h = (double)(vec3i2.getX() - vec3i.getX());
        double i = (double)(vec3i2.getZ() - vec3i.getZ());
        double j = Math.sqrt(h * h + i * i);
        double k = vec32.x * h + vec32.z * i;
        if (k < 0.0D) {
            h = -h;
            i = -i;
        }

        double l = Math.min(2.0D, vec32.horizontalDistance());
        vec32 = new Vec3D(l * h / j, vec32.y, l * i / j);
        this.setMot(vec32);
        Entity entity = this.getFirstPassenger();
        if (entity instanceof EntityHuman) {
            Vec3D vec33 = entity.getMot();
            double m = vec33.horizontalDistanceSqr();
            double n = this.getMot().horizontalDistanceSqr();
            if (m > 1.0E-4D && n < 0.01D) {
                this.setMot(this.getMot().add(vec33.x * 0.1D, 0.0D, vec33.z * 0.1D));
                bl2 = false;
            }
        }

        if (bl2) {
            double o = this.getMot().horizontalDistance();
            if (o < 0.03D) {
                this.setMot(Vec3D.ZERO);
            } else {
                this.setMot(this.getMot().multiply(0.5D, 0.0D, 0.5D));
            }
        }

        double p = (double)pos.getX() + 0.5D + (double)vec3i.getX() * 0.5D;
        double q = (double)pos.getZ() + 0.5D + (double)vec3i.getZ() * 0.5D;
        double r = (double)pos.getX() + 0.5D + (double)vec3i2.getX() * 0.5D;
        double s = (double)pos.getZ() + 0.5D + (double)vec3i2.getZ() * 0.5D;
        h = r - p;
        i = s - q;
        double t;
        if (h == 0.0D) {
            t = f - (double)pos.getZ();
        } else if (i == 0.0D) {
            t = d - (double)pos.getX();
        } else {
            double v = d - p;
            double w = f - q;
            t = (v * h + w * i) * 2.0D;
        }

        d = p + h * t;
        f = q + i * t;
        this.setPosition(d, e, f);
        double y = this.isVehicle() ? 0.75D : 1.0D;
        double z = this.getMaxSpeed();
        vec32 = this.getMot();
        this.move(EnumMoveType.SELF, new Vec3D(MathHelper.clamp(y * vec32.x, -z, z), 0.0D, MathHelper.clamp(y * vec32.z, -z, z)));
        if (vec3i.getY() != 0 && MathHelper.floor(this.locX()) - pos.getX() == vec3i.getX() && MathHelper.floor(this.locZ()) - pos.getZ() == vec3i.getZ()) {
            this.setPosition(this.locX(), this.locY() + (double)vec3i.getY(), this.locZ());
        } else if (vec3i2.getY() != 0 && MathHelper.floor(this.locX()) - pos.getX() == vec3i2.getX() && MathHelper.floor(this.locZ()) - pos.getZ() == vec3i2.getZ()) {
            this.setPosition(this.locX(), this.locY() + (double)vec3i2.getY(), this.locZ());
        }

        this.decelerate();
        Vec3D vec34 = this.getPos(this.locX(), this.locY(), this.locZ());
        if (vec34 != null && vec3 != null) {
            double aa = (vec3.y - vec34.y) * 0.05D;
            Vec3D vec35 = this.getMot();
            double ab = vec35.horizontalDistance();
            if (ab > 0.0D) {
                this.setMot(vec35.multiply((ab + aa) / ab, 1.0D, (ab + aa) / ab));
            }

            this.setPosition(this.locX(), vec34.y, this.locZ());
        }

        int ac = MathHelper.floor(this.locX());
        int ad = MathHelper.floor(this.locZ());
        if (ac != pos.getX() || ad != pos.getZ()) {
            Vec3D vec36 = this.getMot();
            double ae = vec36.horizontalDistance();
            this.setMot(ae * (double)(ac - pos.getX()), vec36.y, ae * (double)(ad - pos.getZ()));
        }

        if (bl) {
            Vec3D vec37 = this.getMot();
            double af = vec37.horizontalDistance();
            if (af > 0.01D) {
                double ag = 0.06D;
                this.setMot(vec37.add(vec37.x / af * 0.06D, 0.0D, vec37.z / af * 0.06D));
            } else {
                Vec3D vec38 = this.getMot();
                double ah = vec38.x;
                double ai = vec38.z;
                if (railShape == BlockPropertyTrackPosition.EAST_WEST) {
                    if (this.isRedstoneConductor(pos.west())) {
                        ah = 0.02D;
                    } else if (this.isRedstoneConductor(pos.east())) {
                        ah = -0.02D;
                    }
                } else {
                    if (railShape != BlockPropertyTrackPosition.NORTH_SOUTH) {
                        return;
                    }

                    if (this.isRedstoneConductor(pos.north())) {
                        ai = 0.02D;
                    } else if (this.isRedstoneConductor(pos.south())) {
                        ai = -0.02D;
                    }
                }

                this.setMot(ah, vec38.y, ai);
            }
        }

    }

    private boolean isRedstoneConductor(BlockPosition pos) {
        return this.level.getType(pos).isOccluding(this.level, pos);
    }

    protected void decelerate() {
        double d = this.isVehicle() ? 0.997D : 0.96D;
        Vec3D vec3 = this.getMot();
        vec3 = vec3.multiply(d, 0.0D, d);
        if (this.isInWater()) {
            vec3 = vec3.scale((double)0.95F);
        }

        this.setMot(vec3);
    }

    @Nullable
    public Vec3D getPosOffs(double x, double y, double z, double offset) {
        int i = MathHelper.floor(x);
        int j = MathHelper.floor(y);
        int k = MathHelper.floor(z);
        if (this.level.getType(new BlockPosition(i, j - 1, k)).is(TagsBlock.RAILS)) {
            --j;
        }

        IBlockData blockState = this.level.getType(new BlockPosition(i, j, k));
        if (BlockMinecartTrackAbstract.isRail(blockState)) {
            BlockPropertyTrackPosition railShape = blockState.get(((BlockMinecartTrackAbstract)blockState.getBlock()).getShapeProperty());
            y = (double)j;
            if (railShape.isAscending()) {
                y = (double)(j + 1);
            }

            Pair<BaseBlockPosition, BaseBlockPosition> pair = exits(railShape);
            BaseBlockPosition vec3i = pair.getFirst();
            BaseBlockPosition vec3i2 = pair.getSecond();
            double d = (double)(vec3i2.getX() - vec3i.getX());
            double e = (double)(vec3i2.getZ() - vec3i.getZ());
            double f = Math.sqrt(d * d + e * e);
            d = d / f;
            e = e / f;
            x = x + d * offset;
            z = z + e * offset;
            if (vec3i.getY() != 0 && MathHelper.floor(x) - i == vec3i.getX() && MathHelper.floor(z) - k == vec3i.getZ()) {
                y += (double)vec3i.getY();
            } else if (vec3i2.getY() != 0 && MathHelper.floor(x) - i == vec3i2.getX() && MathHelper.floor(z) - k == vec3i2.getZ()) {
                y += (double)vec3i2.getY();
            }

            return this.getPos(x, y, z);
        } else {
            return null;
        }
    }

    @Nullable
    public Vec3D getPos(double x, double y, double z) {
        int i = MathHelper.floor(x);
        int j = MathHelper.floor(y);
        int k = MathHelper.floor(z);
        if (this.level.getType(new BlockPosition(i, j - 1, k)).is(TagsBlock.RAILS)) {
            --j;
        }

        IBlockData blockState = this.level.getType(new BlockPosition(i, j, k));
        if (BlockMinecartTrackAbstract.isRail(blockState)) {
            BlockPropertyTrackPosition railShape = blockState.get(((BlockMinecartTrackAbstract)blockState.getBlock()).getShapeProperty());
            Pair<BaseBlockPosition, BaseBlockPosition> pair = exits(railShape);
            BaseBlockPosition vec3i = pair.getFirst();
            BaseBlockPosition vec3i2 = pair.getSecond();
            double d = (double)i + 0.5D + (double)vec3i.getX() * 0.5D;
            double e = (double)j + 0.0625D + (double)vec3i.getY() * 0.5D;
            double f = (double)k + 0.5D + (double)vec3i.getZ() * 0.5D;
            double g = (double)i + 0.5D + (double)vec3i2.getX() * 0.5D;
            double h = (double)j + 0.0625D + (double)vec3i2.getY() * 0.5D;
            double l = (double)k + 0.5D + (double)vec3i2.getZ() * 0.5D;
            double m = g - d;
            double n = (h - e) * 2.0D;
            double o = l - f;
            double p;
            if (m == 0.0D) {
                p = z - (double)k;
            } else if (o == 0.0D) {
                p = x - (double)i;
            } else {
                double r = x - d;
                double s = z - f;
                p = (r * m + s * o) * 2.0D;
            }

            x = d + m * p;
            y = e + n * p;
            z = f + o * p;
            if (n < 0.0D) {
                ++y;
            } else if (n > 0.0D) {
                y += 0.5D;
            }

            return new Vec3D(x, y, z);
        } else {
            return null;
        }
    }

    @Override
    public AxisAlignedBB getBoundingBoxForCulling() {
        AxisAlignedBB aABB = this.getBoundingBox();
        return this.hasCustomDisplay() ? aABB.inflate((double)Math.abs(this.getDisplayBlockOffset()) / 16.0D) : aABB;
    }

    @Override
    protected void loadData(NBTTagCompound nbt) {
        if (nbt.getBoolean("CustomDisplayTile")) {
            this.setDisplayBlock(GameProfileSerializer.readBlockState(nbt.getCompound("DisplayState")));
            this.setDisplayBlockOffset(nbt.getInt("DisplayOffset"));
        }

    }

    @Override
    protected void saveData(NBTTagCompound nbt) {
        if (this.hasCustomDisplay()) {
            nbt.setBoolean("CustomDisplayTile", true);
            nbt.set("DisplayState", GameProfileSerializer.writeBlockState(this.getDisplayBlock()));
            nbt.setInt("DisplayOffset", this.getDisplayBlockOffset());
        }

    }

    @Override
    public void collide(Entity entity) {
        if (!this.level.isClientSide) {
            if (!entity.noPhysics && !this.noPhysics) {
                if (!this.hasPassenger(entity)) {
                    double d = entity.locX() - this.locX();
                    double e = entity.locZ() - this.locZ();
                    double f = d * d + e * e;
                    if (f >= (double)1.0E-4F) {
                        f = Math.sqrt(f);
                        d = d / f;
                        e = e / f;
                        double g = 1.0D / f;
                        if (g > 1.0D) {
                            g = 1.0D;
                        }

                        d = d * g;
                        e = e * g;
                        d = d * (double)0.1F;
                        e = e * (double)0.1F;
                        d = d * 0.5D;
                        e = e * 0.5D;
                        if (entity instanceof EntityMinecartAbstract) {
                            double h = entity.locX() - this.locX();
                            double i = entity.locZ() - this.locZ();
                            Vec3D vec3 = (new Vec3D(h, 0.0D, i)).normalize();
                            Vec3D vec32 = (new Vec3D((double)MathHelper.cos(this.getYRot() * ((float)Math.PI / 180F)), 0.0D, (double)MathHelper.sin(this.getYRot() * ((float)Math.PI / 180F)))).normalize();
                            double j = Math.abs(vec3.dot(vec32));
                            if (j < (double)0.8F) {
                                return;
                            }

                            Vec3D vec33 = this.getMot();
                            Vec3D vec34 = entity.getMot();
                            if (((EntityMinecartAbstract)entity).getMinecartType() == EntityMinecartAbstract.EnumMinecartType.FURNACE && this.getMinecartType() != EntityMinecartAbstract.EnumMinecartType.FURNACE) {
                                this.setMot(vec33.multiply(0.2D, 1.0D, 0.2D));
                                this.push(vec34.x - d, 0.0D, vec34.z - e);
                                entity.setMot(vec34.multiply(0.95D, 1.0D, 0.95D));
                            } else if (((EntityMinecartAbstract)entity).getMinecartType() != EntityMinecartAbstract.EnumMinecartType.FURNACE && this.getMinecartType() == EntityMinecartAbstract.EnumMinecartType.FURNACE) {
                                entity.setMot(vec34.multiply(0.2D, 1.0D, 0.2D));
                                entity.push(vec33.x + d, 0.0D, vec33.z + e);
                                this.setMot(vec33.multiply(0.95D, 1.0D, 0.95D));
                            } else {
                                double k = (vec34.x + vec33.x) / 2.0D;
                                double l = (vec34.z + vec33.z) / 2.0D;
                                this.setMot(vec33.multiply(0.2D, 1.0D, 0.2D));
                                this.push(k - d, 0.0D, l - e);
                                entity.setMot(vec34.multiply(0.2D, 1.0D, 0.2D));
                                entity.push(k + d, 0.0D, l + e);
                            }
                        } else {
                            this.push(-d, 0.0D, -e);
                            entity.push(d / 4.0D, 0.0D, e / 4.0D);
                        }
                    }

                }
            }
        }
    }

    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int interpolationSteps, boolean interpolate) {
        this.lx = x;
        this.ly = y;
        this.lz = z;
        this.lyr = (double)yaw;
        this.lxr = (double)pitch;
        this.lSteps = interpolationSteps + 2;
        this.setMot(this.lxd, this.lyd, this.lzd);
    }

    @Override
    public void lerpMotion(double x, double y, double z) {
        this.lxd = x;
        this.lyd = y;
        this.lzd = z;
        this.setMot(this.lxd, this.lyd, this.lzd);
    }

    public void setDamage(float damageWobbleStrength) {
        this.entityData.set(DATA_ID_DAMAGE, damageWobbleStrength);
    }

    public float getDamage() {
        return this.entityData.get(DATA_ID_DAMAGE);
    }

    public void setHurtTime(int wobbleTicks) {
        this.entityData.set(DATA_ID_HURT, wobbleTicks);
    }

    public int getType() {
        return this.entityData.get(DATA_ID_HURT);
    }

    public void setHurtDir(int wobbleSide) {
        this.entityData.set(DATA_ID_HURTDIR, wobbleSide);
    }

    public int getHurtDir() {
        return this.entityData.get(DATA_ID_HURTDIR);
    }

    public abstract EntityMinecartAbstract.EnumMinecartType getMinecartType();

    public IBlockData getDisplayBlock() {
        return !this.hasCustomDisplay() ? this.getDefaultDisplayBlockState() : Block.getByCombinedId(this.getDataWatcher().get(DATA_ID_DISPLAY_BLOCK));
    }

    public IBlockData getDefaultDisplayBlockState() {
        return Blocks.AIR.getBlockData();
    }

    public int getDisplayBlockOffset() {
        return !this.hasCustomDisplay() ? this.getDefaultDisplayOffset() : this.getDataWatcher().get(DATA_ID_DISPLAY_OFFSET);
    }

    public int getDefaultDisplayOffset() {
        return 6;
    }

    public void setDisplayBlock(IBlockData state) {
        this.getDataWatcher().set(DATA_ID_DISPLAY_BLOCK, Block.getCombinedId(state));
        this.setCustomDisplay(true);
    }

    public void setDisplayBlockOffset(int offset) {
        this.getDataWatcher().set(DATA_ID_DISPLAY_OFFSET, offset);
        this.setCustomDisplay(true);
    }

    public boolean hasCustomDisplay() {
        return this.getDataWatcher().get(DATA_ID_CUSTOM_DISPLAY);
    }

    public void setCustomDisplay(boolean present) {
        this.getDataWatcher().set(DATA_ID_CUSTOM_DISPLAY, present);
    }

    @Override
    public Packet<?> getPacket() {
        return new PacketPlayOutSpawnEntity(this);
    }

    @Override
    public ItemStack getPickResult() {
        Item item;
        switch(this.getMinecartType()) {
        case FURNACE:
            item = Items.FURNACE_MINECART;
            break;
        case CHEST:
            item = Items.CHEST_MINECART;
            break;
        case TNT:
            item = Items.TNT_MINECART;
            break;
        case HOPPER:
            item = Items.HOPPER_MINECART;
            break;
        case COMMAND_BLOCK:
            item = Items.COMMAND_BLOCK_MINECART;
            break;
        default:
            item = Items.MINECART;
        }

        return new ItemStack(item);
    }

    public static enum EnumMinecartType {
        RIDEABLE,
        CHEST,
        FURNACE,
        TNT,
        SPAWNER,
        HOPPER,
        COMMAND_BLOCK;
    }
}
