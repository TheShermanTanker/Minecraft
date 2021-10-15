package net.minecraft.world.entity.projectile;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.particles.Particles;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntity;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.stats.StatisticList;
import net.minecraft.tags.TagsFluid;
import net.minecraft.tags.TagsItem;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityExperienceOrb;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMoveType;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.LootTables;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameterSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameters;
import net.minecraft.world.phys.MovingObjectPosition;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.MovingObjectPositionEntity;
import net.minecraft.world.phys.Vec3D;

public class EntityFishingHook extends IProjectile {
    private final Random syncronizedRandom = new Random();
    private boolean biting;
    private int outOfWaterTime;
    private static final int MAX_OUT_OF_WATER_TIME = 10;
    public static final DataWatcherObject<Integer> DATA_HOOKED_ENTITY = DataWatcher.defineId(EntityFishingHook.class, DataWatcherRegistry.INT);
    private static final DataWatcherObject<Boolean> DATA_BITING = DataWatcher.defineId(EntityFishingHook.class, DataWatcherRegistry.BOOLEAN);
    private int life;
    private int nibble;
    private int timeUntilLured;
    private int timeUntilHooked;
    private float fishAngle;
    private boolean openWater = true;
    @Nullable
    public Entity hookedIn;
    public EntityFishingHook.HookState currentState = EntityFishingHook.HookState.FLYING;
    private final int luck;
    private final int lureSpeed;

    private EntityFishingHook(EntityTypes<? extends EntityFishingHook> type, World world, int lureLevel, int luckOfTheSeaLevel) {
        super(type, world);
        this.noCulling = true;
        this.luck = Math.max(0, lureLevel);
        this.lureSpeed = Math.max(0, luckOfTheSeaLevel);
    }

    public EntityFishingHook(EntityTypes<? extends EntityFishingHook> type, World world) {
        this(type, world, 0, 0);
    }

    public EntityFishingHook(EntityHuman thrower, World world, int lureLevel, int luckOfTheSeaLevel) {
        this(EntityTypes.FISHING_BOBBER, world, lureLevel, luckOfTheSeaLevel);
        this.setShooter(thrower);
        float f = thrower.getXRot();
        float g = thrower.getYRot();
        float h = MathHelper.cos(-g * ((float)Math.PI / 180F) - (float)Math.PI);
        float i = MathHelper.sin(-g * ((float)Math.PI / 180F) - (float)Math.PI);
        float j = -MathHelper.cos(-f * ((float)Math.PI / 180F));
        float k = MathHelper.sin(-f * ((float)Math.PI / 180F));
        double d = thrower.locX() - (double)i * 0.3D;
        double e = thrower.getHeadY();
        double l = thrower.locZ() - (double)h * 0.3D;
        this.setPositionRotation(d, e, l, g, f);
        Vec3D vec3 = new Vec3D((double)(-i), (double)MathHelper.clamp(-(k / j), -5.0F, 5.0F), (double)(-h));
        double m = vec3.length();
        vec3 = vec3.multiply(0.6D / m + 0.5D + this.random.nextGaussian() * 0.0045D, 0.6D / m + 0.5D + this.random.nextGaussian() * 0.0045D, 0.6D / m + 0.5D + this.random.nextGaussian() * 0.0045D);
        this.setMot(vec3);
        this.setYRot((float)(MathHelper.atan2(vec3.x, vec3.z) * (double)(180F / (float)Math.PI)));
        this.setXRot((float)(MathHelper.atan2(vec3.y, vec3.horizontalDistance()) * (double)(180F / (float)Math.PI)));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    @Override
    protected void initDatawatcher() {
        this.getDataWatcher().register(DATA_HOOKED_ENTITY, 0);
        this.getDataWatcher().register(DATA_BITING, false);
    }

    @Override
    public void onSyncedDataUpdated(DataWatcherObject<?> data) {
        if (DATA_HOOKED_ENTITY.equals(data)) {
            int i = this.getDataWatcher().get(DATA_HOOKED_ENTITY);
            this.hookedIn = i > 0 ? this.level.getEntity(i - 1) : null;
        }

        if (DATA_BITING.equals(data)) {
            this.biting = this.getDataWatcher().get(DATA_BITING);
            if (this.biting) {
                this.setMot(this.getMot().x, (double)(-0.4F * MathHelper.nextFloat(this.syncronizedRandom, 0.6F, 1.0F)), this.getMot().z);
            }
        }

        super.onSyncedDataUpdated(data);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double d = 64.0D;
        return distance < 4096.0D;
    }

    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int interpolationSteps, boolean interpolate) {
    }

    @Override
    public void tick() {
        this.syncronizedRandom.setSeed(this.getUniqueID().getLeastSignificantBits() ^ this.level.getTime());
        super.tick();
        EntityHuman player = this.getOwner();
        if (player == null) {
            this.die();
        } else if (this.level.isClientSide || !this.shouldStopFishing(player)) {
            if (this.onGround) {
                ++this.life;
                if (this.life >= 1200) {
                    this.die();
                    return;
                }
            } else {
                this.life = 0;
            }

            float f = 0.0F;
            BlockPosition blockPos = this.getChunkCoordinates();
            Fluid fluidState = this.level.getFluid(blockPos);
            if (fluidState.is(TagsFluid.WATER)) {
                f = fluidState.getHeight(this.level, blockPos);
            }

            boolean bl = f > 0.0F;
            if (this.currentState == EntityFishingHook.HookState.FLYING) {
                if (this.hookedIn != null) {
                    this.setMot(Vec3D.ZERO);
                    this.currentState = EntityFishingHook.HookState.HOOKED_IN_ENTITY;
                    return;
                }

                if (bl) {
                    this.setMot(this.getMot().multiply(0.3D, 0.2D, 0.3D));
                    this.currentState = EntityFishingHook.HookState.BOBBING;
                    return;
                }

                this.checkCollision();
            } else {
                if (this.currentState == EntityFishingHook.HookState.HOOKED_IN_ENTITY) {
                    if (this.hookedIn != null) {
                        if (!this.hookedIn.isRemoved() && this.hookedIn.level.getDimensionKey() == this.level.getDimensionKey()) {
                            this.setPosition(this.hookedIn.locX(), this.hookedIn.getY(0.8D), this.hookedIn.locZ());
                        } else {
                            this.updateHookedEntity((Entity)null);
                            this.currentState = EntityFishingHook.HookState.FLYING;
                        }
                    }

                    return;
                }

                if (this.currentState == EntityFishingHook.HookState.BOBBING) {
                    Vec3D vec3 = this.getMot();
                    double d = this.locY() + vec3.y - (double)blockPos.getY() - (double)f;
                    if (Math.abs(d) < 0.01D) {
                        d += Math.signum(d) * 0.1D;
                    }

                    this.setMot(vec3.x * 0.9D, vec3.y - d * (double)this.random.nextFloat() * 0.2D, vec3.z * 0.9D);
                    if (this.nibble <= 0 && this.timeUntilHooked <= 0) {
                        this.openWater = true;
                    } else {
                        this.openWater = this.openWater && this.outOfWaterTime < 10 && this.calculateOpenWater(blockPos);
                    }

                    if (bl) {
                        this.outOfWaterTime = Math.max(0, this.outOfWaterTime - 1);
                        if (this.biting) {
                            this.setMot(this.getMot().add(0.0D, -0.1D * (double)this.syncronizedRandom.nextFloat() * (double)this.syncronizedRandom.nextFloat(), 0.0D));
                        }

                        if (!this.level.isClientSide) {
                            this.catchingFish(blockPos);
                        }
                    } else {
                        this.outOfWaterTime = Math.min(10, this.outOfWaterTime + 1);
                    }
                }
            }

            if (!fluidState.is(TagsFluid.WATER)) {
                this.setMot(this.getMot().add(0.0D, -0.03D, 0.0D));
            }

            this.move(EnumMoveType.SELF, this.getMot());
            this.updateRotation();
            if (this.currentState == EntityFishingHook.HookState.FLYING && (this.onGround || this.horizontalCollision)) {
                this.setMot(Vec3D.ZERO);
            }

            double e = 0.92D;
            this.setMot(this.getMot().scale(0.92D));
            this.reapplyPosition();
        }
    }

    private boolean shouldStopFishing(EntityHuman player) {
        ItemStack itemStack = player.getItemInMainHand();
        ItemStack itemStack2 = player.getItemInOffHand();
        boolean bl = itemStack.is(Items.FISHING_ROD);
        boolean bl2 = itemStack2.is(Items.FISHING_ROD);
        if (!player.isRemoved() && player.isAlive() && (bl || bl2) && !(this.distanceToSqr(player) > 1024.0D)) {
            return false;
        } else {
            this.die();
            return true;
        }
    }

    private void checkCollision() {
        MovingObjectPosition hitResult = ProjectileHelper.getHitResult(this, this::canHitEntity);
        this.onHit(hitResult);
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) || entity.isAlive() && entity instanceof EntityItem;
    }

    @Override
    protected void onHitEntity(MovingObjectPositionEntity entityHitResult) {
        super.onHitEntity(entityHitResult);
        if (!this.level.isClientSide) {
            this.updateHookedEntity(entityHitResult.getEntity());
        }

    }

    @Override
    protected void onHitBlock(MovingObjectPositionBlock blockHitResult) {
        super.onHitBlock(blockHitResult);
        this.setMot(this.getMot().normalize().scale(blockHitResult.distanceTo(this)));
    }

    public void updateHookedEntity(@Nullable Entity entity) {
        this.hookedIn = entity;
        this.getDataWatcher().set(DATA_HOOKED_ENTITY, entity == null ? 0 : entity.getId() + 1);
    }

    private void catchingFish(BlockPosition pos) {
        WorldServer serverLevel = (WorldServer)this.level;
        int i = 1;
        BlockPosition blockPos = pos.above();
        if (this.random.nextFloat() < 0.25F && this.level.isRainingAt(blockPos)) {
            ++i;
        }

        if (this.random.nextFloat() < 0.5F && !this.level.canSeeSky(blockPos)) {
            --i;
        }

        if (this.nibble > 0) {
            --this.nibble;
            if (this.nibble <= 0) {
                this.timeUntilLured = 0;
                this.timeUntilHooked = 0;
                this.getDataWatcher().set(DATA_BITING, false);
            }
        } else if (this.timeUntilHooked > 0) {
            this.timeUntilHooked -= i;
            if (this.timeUntilHooked > 0) {
                this.fishAngle = (float)((double)this.fishAngle + this.random.nextGaussian() * 4.0D);
                float f = this.fishAngle * ((float)Math.PI / 180F);
                float g = MathHelper.sin(f);
                float h = MathHelper.cos(f);
                double d = this.locX() + (double)(g * (float)this.timeUntilHooked * 0.1F);
                double e = (double)((float)MathHelper.floor(this.locY()) + 1.0F);
                double j = this.locZ() + (double)(h * (float)this.timeUntilHooked * 0.1F);
                IBlockData blockState = serverLevel.getType(new BlockPosition(d, e - 1.0D, j));
                if (blockState.is(Blocks.WATER)) {
                    if (this.random.nextFloat() < 0.15F) {
                        serverLevel.sendParticles(Particles.BUBBLE, d, e - (double)0.1F, j, 1, (double)g, 0.1D, (double)h, 0.0D);
                    }

                    float k = g * 0.04F;
                    float l = h * 0.04F;
                    serverLevel.sendParticles(Particles.FISHING, d, e, j, 0, (double)l, 0.01D, (double)(-k), 1.0D);
                    serverLevel.sendParticles(Particles.FISHING, d, e, j, 0, (double)(-l), 0.01D, (double)k, 1.0D);
                }
            } else {
                this.playSound(SoundEffects.FISHING_BOBBER_SPLASH, 0.25F, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
                double m = this.locY() + 0.5D;
                serverLevel.sendParticles(Particles.BUBBLE, this.locX(), m, this.locZ(), (int)(1.0F + this.getWidth() * 20.0F), (double)this.getWidth(), 0.0D, (double)this.getWidth(), (double)0.2F);
                serverLevel.sendParticles(Particles.FISHING, this.locX(), m, this.locZ(), (int)(1.0F + this.getWidth() * 20.0F), (double)this.getWidth(), 0.0D, (double)this.getWidth(), (double)0.2F);
                this.nibble = MathHelper.nextInt(this.random, 20, 40);
                this.getDataWatcher().set(DATA_BITING, true);
            }
        } else if (this.timeUntilLured > 0) {
            this.timeUntilLured -= i;
            float n = 0.15F;
            if (this.timeUntilLured < 20) {
                n = (float)((double)n + (double)(20 - this.timeUntilLured) * 0.05D);
            } else if (this.timeUntilLured < 40) {
                n = (float)((double)n + (double)(40 - this.timeUntilLured) * 0.02D);
            } else if (this.timeUntilLured < 60) {
                n = (float)((double)n + (double)(60 - this.timeUntilLured) * 0.01D);
            }

            if (this.random.nextFloat() < n) {
                float o = MathHelper.nextFloat(this.random, 0.0F, 360.0F) * ((float)Math.PI / 180F);
                float p = MathHelper.nextFloat(this.random, 25.0F, 60.0F);
                double q = this.locX() + (double)(MathHelper.sin(o) * p * 0.1F);
                double r = (double)((float)MathHelper.floor(this.locY()) + 1.0F);
                double s = this.locZ() + (double)(MathHelper.cos(o) * p * 0.1F);
                IBlockData blockState2 = serverLevel.getType(new BlockPosition(q, r - 1.0D, s));
                if (blockState2.is(Blocks.WATER)) {
                    serverLevel.sendParticles(Particles.SPLASH, q, r, s, 2 + this.random.nextInt(2), (double)0.1F, 0.0D, (double)0.1F, 0.0D);
                }
            }

            if (this.timeUntilLured <= 0) {
                this.fishAngle = MathHelper.nextFloat(this.random, 0.0F, 360.0F);
                this.timeUntilHooked = MathHelper.nextInt(this.random, 20, 80);
            }
        } else {
            this.timeUntilLured = MathHelper.nextInt(this.random, 100, 600);
            this.timeUntilLured -= this.lureSpeed * 20 * 5;
        }

    }

    private boolean calculateOpenWater(BlockPosition pos) {
        EntityFishingHook.WaterPosition openWaterType = EntityFishingHook.WaterPosition.INVALID;

        for(int i = -1; i <= 2; ++i) {
            EntityFishingHook.WaterPosition openWaterType2 = this.getOpenWaterTypeForArea(pos.offset(-2, i, -2), pos.offset(2, i, 2));
            switch(openWaterType2) {
            case INVALID:
                return false;
            case ABOVE_WATER:
                if (openWaterType == EntityFishingHook.WaterPosition.INVALID) {
                    return false;
                }
                break;
            case INSIDE_WATER:
                if (openWaterType == EntityFishingHook.WaterPosition.ABOVE_WATER) {
                    return false;
                }
            }

            openWaterType = openWaterType2;
        }

        return true;
    }

    private EntityFishingHook.WaterPosition getOpenWaterTypeForArea(BlockPosition start, BlockPosition end) {
        return BlockPosition.betweenClosedStream(start, end).map(this::getOpenWaterTypeForBlock).reduce((openWaterType, openWaterType2) -> {
            return openWaterType == openWaterType2 ? openWaterType : EntityFishingHook.WaterPosition.INVALID;
        }).orElse(EntityFishingHook.WaterPosition.INVALID);
    }

    private EntityFishingHook.WaterPosition getOpenWaterTypeForBlock(BlockPosition pos) {
        IBlockData blockState = this.level.getType(pos);
        if (!blockState.isAir() && !blockState.is(Blocks.LILY_PAD)) {
            Fluid fluidState = blockState.getFluid();
            return fluidState.is(TagsFluid.WATER) && fluidState.isSource() && blockState.getCollisionShape(this.level, pos).isEmpty() ? EntityFishingHook.WaterPosition.INSIDE_WATER : EntityFishingHook.WaterPosition.INVALID;
        } else {
            return EntityFishingHook.WaterPosition.ABOVE_WATER;
        }
    }

    public boolean isInOpenWater() {
        return this.openWater;
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
    }

    public int retrieve(ItemStack usedItem) {
        EntityHuman player = this.getOwner();
        if (!this.level.isClientSide && player != null && !this.shouldStopFishing(player)) {
            int i = 0;
            if (this.hookedIn != null) {
                this.reel(this.hookedIn);
                CriterionTriggers.FISHING_ROD_HOOKED.trigger((EntityPlayer)player, usedItem, this, Collections.emptyList());
                this.level.broadcastEntityEffect(this, (byte)31);
                i = this.hookedIn instanceof EntityItem ? 3 : 5;
            } else if (this.nibble > 0) {
                LootTableInfo.Builder builder = (new LootTableInfo.Builder((WorldServer)this.level)).set(LootContextParameters.ORIGIN, this.getPositionVector()).set(LootContextParameters.TOOL, usedItem).set(LootContextParameters.THIS_ENTITY, this).withRandom(this.random).withLuck((float)this.luck + player.getLuck());
                LootTable lootTable = this.level.getMinecraftServer().getLootTableRegistry().getLootTable(LootTables.FISHING);
                List<ItemStack> list = lootTable.populateLoot(builder.build(LootContextParameterSets.FISHING));
                CriterionTriggers.FISHING_ROD_HOOKED.trigger((EntityPlayer)player, usedItem, this, list);

                for(ItemStack itemStack : list) {
                    EntityItem itemEntity = new EntityItem(this.level, this.locX(), this.locY(), this.locZ(), itemStack);
                    double d = player.locX() - this.locX();
                    double e = player.locY() - this.locY();
                    double f = player.locZ() - this.locZ();
                    double g = 0.1D;
                    itemEntity.setMot(d * 0.1D, e * 0.1D + Math.sqrt(Math.sqrt(d * d + e * e + f * f)) * 0.08D, f * 0.1D);
                    this.level.addEntity(itemEntity);
                    player.level.addEntity(new EntityExperienceOrb(player.level, player.locX(), player.locY() + 0.5D, player.locZ() + 0.5D, this.random.nextInt(6) + 1));
                    if (itemStack.is(TagsItem.FISHES)) {
                        player.awardStat(StatisticList.FISH_CAUGHT, 1);
                    }
                }

                i = 1;
            }

            if (this.onGround) {
                i = 2;
            }

            this.die();
            return i;
        } else {
            return 0;
        }
    }

    @Override
    public void handleEntityEvent(byte status) {
        if (status == 31 && this.level.isClientSide && this.hookedIn instanceof EntityHuman && ((EntityHuman)this.hookedIn).isLocalPlayer()) {
            this.reel(this.hookedIn);
        }

        super.handleEntityEvent(status);
    }

    public void reel(Entity entity) {
        Entity entity2 = this.getShooter();
        if (entity2 != null) {
            Vec3D vec3 = (new Vec3D(entity2.locX() - this.locX(), entity2.locY() - this.locY(), entity2.locZ() - this.locZ())).scale(0.1D);
            entity.setMot(entity.getMot().add(vec3));
        }
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        this.updateOwnerInfo((EntityFishingHook)null);
        super.remove(reason);
    }

    @Override
    public void onClientRemoval() {
        this.updateOwnerInfo((EntityFishingHook)null);
    }

    @Override
    public void setShooter(@Nullable Entity entity) {
        super.setShooter(entity);
        this.updateOwnerInfo(this);
    }

    private void updateOwnerInfo(@Nullable EntityFishingHook fishingBobber) {
        EntityHuman player = this.getOwner();
        if (player != null) {
            player.fishing = fishingBobber;
        }

    }

    @Nullable
    public EntityHuman getOwner() {
        Entity entity = this.getShooter();
        return entity instanceof EntityHuman ? (EntityHuman)entity : null;
    }

    @Nullable
    public Entity getHooked() {
        return this.hookedIn;
    }

    @Override
    public boolean canPortal() {
        return false;
    }

    @Override
    public Packet<?> getPacket() {
        Entity entity = this.getShooter();
        return new PacketPlayOutSpawnEntity(this, entity == null ? this.getId() : entity.getId());
    }

    @Override
    public void recreateFromPacket(PacketPlayOutSpawnEntity packet) {
        super.recreateFromPacket(packet);
        if (this.getOwner() == null) {
            int i = packet.getData();
            LOGGER.error("Failed to recreate fishing hook on client. {} (id: {}) is not a valid owner.", this.level.getEntity(i), i);
            this.killEntity();
        }

    }

    public static enum HookState {
        FLYING,
        HOOKED_IN_ENTITY,
        BOBBING;
    }

    static enum WaterPosition {
        ABOVE_WATER,
        INSIDE_WATER,
        INVALID;
    }
}
