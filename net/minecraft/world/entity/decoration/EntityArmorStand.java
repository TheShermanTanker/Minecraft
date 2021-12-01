package net.minecraft.world.entity.decoration;

import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Vector3f;
import net.minecraft.core.particles.ParticleParamBlock;
import net.minecraft.core.particles.Particles;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLightning;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.EntitySize;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.EnumMainHand;
import net.minecraft.world.entity.LivingEntity$Fallsounds;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.EntityArrow;
import net.minecraft.world.entity.vehicle.EntityMinecartAbstract;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.EnumSkyBlock;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.EnumPistonReaction;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.Vec3D;

public class EntityArmorStand extends EntityLiving {
    public static final int WOBBLE_TIME = 5;
    private static final boolean ENABLE_ARMS = true;
    private static final Vector3f DEFAULT_HEAD_POSE = new Vector3f(0.0F, 0.0F, 0.0F);
    private static final Vector3f DEFAULT_BODY_POSE = new Vector3f(0.0F, 0.0F, 0.0F);
    private static final Vector3f DEFAULT_LEFT_ARM_POSE = new Vector3f(-10.0F, 0.0F, -10.0F);
    private static final Vector3f DEFAULT_RIGHT_ARM_POSE = new Vector3f(-15.0F, 0.0F, 10.0F);
    private static final Vector3f DEFAULT_LEFT_LEG_POSE = new Vector3f(-1.0F, 0.0F, -1.0F);
    private static final Vector3f DEFAULT_RIGHT_LEG_POSE = new Vector3f(1.0F, 0.0F, 1.0F);
    private static final EntitySize MARKER_DIMENSIONS = new EntitySize(0.0F, 0.0F, true);
    private static final EntitySize BABY_DIMENSIONS = EntityTypes.ARMOR_STAND.getDimensions().scale(0.5F);
    private static final double FEET_OFFSET = 0.1D;
    private static final double CHEST_OFFSET = 0.9D;
    private static final double LEGS_OFFSET = 0.4D;
    private static final double HEAD_OFFSET = 1.6D;
    public static final int DISABLE_TAKING_OFFSET = 8;
    public static final int DISABLE_PUTTING_OFFSET = 16;
    public static final int CLIENT_FLAG_SMALL = 1;
    public static final int CLIENT_FLAG_SHOW_ARMS = 4;
    public static final int CLIENT_FLAG_NO_BASEPLATE = 8;
    public static final int CLIENT_FLAG_MARKER = 16;
    public static final DataWatcherObject<Byte> DATA_CLIENT_FLAGS = DataWatcher.defineId(EntityArmorStand.class, DataWatcherRegistry.BYTE);
    public static final DataWatcherObject<Vector3f> DATA_HEAD_POSE = DataWatcher.defineId(EntityArmorStand.class, DataWatcherRegistry.ROTATIONS);
    public static final DataWatcherObject<Vector3f> DATA_BODY_POSE = DataWatcher.defineId(EntityArmorStand.class, DataWatcherRegistry.ROTATIONS);
    public static final DataWatcherObject<Vector3f> DATA_LEFT_ARM_POSE = DataWatcher.defineId(EntityArmorStand.class, DataWatcherRegistry.ROTATIONS);
    public static final DataWatcherObject<Vector3f> DATA_RIGHT_ARM_POSE = DataWatcher.defineId(EntityArmorStand.class, DataWatcherRegistry.ROTATIONS);
    public static final DataWatcherObject<Vector3f> DATA_LEFT_LEG_POSE = DataWatcher.defineId(EntityArmorStand.class, DataWatcherRegistry.ROTATIONS);
    public static final DataWatcherObject<Vector3f> DATA_RIGHT_LEG_POSE = DataWatcher.defineId(EntityArmorStand.class, DataWatcherRegistry.ROTATIONS);
    private static final Predicate<Entity> RIDABLE_MINECARTS = (entity) -> {
        return entity instanceof EntityMinecartAbstract && ((EntityMinecartAbstract)entity).getMinecartType() == EntityMinecartAbstract.EnumMinecartType.RIDEABLE;
    };
    private final NonNullList<ItemStack> handItems = NonNullList.withSize(2, ItemStack.EMPTY);
    private final NonNullList<ItemStack> armorItems = NonNullList.withSize(4, ItemStack.EMPTY);
    private boolean invisible;
    public long lastHit;
    public int disabledSlots;
    public Vector3f headPose = DEFAULT_HEAD_POSE;
    public Vector3f bodyPose = DEFAULT_BODY_POSE;
    public Vector3f leftArmPose = DEFAULT_LEFT_ARM_POSE;
    public Vector3f rightArmPose = DEFAULT_RIGHT_ARM_POSE;
    public Vector3f leftLegPose = DEFAULT_LEFT_LEG_POSE;
    public Vector3f rightLegPose = DEFAULT_RIGHT_LEG_POSE;

    public EntityArmorStand(EntityTypes<? extends EntityArmorStand> type, World world) {
        super(type, world);
        this.maxUpStep = 0.0F;
    }

    public EntityArmorStand(World world, double x, double y, double z) {
        this(EntityTypes.ARMOR_STAND, world);
        this.setPosition(x, y, z);
    }

    @Override
    public void updateSize() {
        double d = this.locX();
        double e = this.locY();
        double f = this.locZ();
        super.updateSize();
        this.setPosition(d, e, f);
    }

    private boolean hasPhysics() {
        return !this.isMarker() && !this.isNoGravity();
    }

    @Override
    public boolean doAITick() {
        return super.doAITick() && this.hasPhysics();
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.entityData.register(DATA_CLIENT_FLAGS, (byte)0);
        this.entityData.register(DATA_HEAD_POSE, DEFAULT_HEAD_POSE);
        this.entityData.register(DATA_BODY_POSE, DEFAULT_BODY_POSE);
        this.entityData.register(DATA_LEFT_ARM_POSE, DEFAULT_LEFT_ARM_POSE);
        this.entityData.register(DATA_RIGHT_ARM_POSE, DEFAULT_RIGHT_ARM_POSE);
        this.entityData.register(DATA_LEFT_LEG_POSE, DEFAULT_LEFT_LEG_POSE);
        this.entityData.register(DATA_RIGHT_LEG_POSE, DEFAULT_RIGHT_LEG_POSE);
    }

    @Override
    public Iterable<ItemStack> getHandSlots() {
        return this.handItems;
    }

    @Override
    public Iterable<ItemStack> getArmorItems() {
        return this.armorItems;
    }

    @Override
    public ItemStack getEquipment(EnumItemSlot slot) {
        switch(slot.getType()) {
        case HAND:
            return this.handItems.get(slot.getIndex());
        case ARMOR:
            return this.armorItems.get(slot.getIndex());
        default:
            return ItemStack.EMPTY;
        }
    }

    @Override
    public void setSlot(EnumItemSlot slot, ItemStack stack) {
        this.verifyEquippedItem(stack);
        switch(slot.getType()) {
        case HAND:
            this.playEquipSound(stack);
            this.handItems.set(slot.getIndex(), stack);
            break;
        case ARMOR:
            this.playEquipSound(stack);
            this.armorItems.set(slot.getIndex(), stack);
        }

    }

    @Override
    public boolean canTakeItem(ItemStack stack) {
        EnumItemSlot equipmentSlot = EntityInsentient.getEquipmentSlotForItem(stack);
        return this.getEquipment(equipmentSlot).isEmpty() && !this.isDisabled(equipmentSlot);
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        NBTTagList listTag = new NBTTagList();

        for(ItemStack itemStack : this.armorItems) {
            NBTTagCompound compoundTag = new NBTTagCompound();
            if (!itemStack.isEmpty()) {
                itemStack.save(compoundTag);
            }

            listTag.add(compoundTag);
        }

        nbt.set("ArmorItems", listTag);
        NBTTagList listTag2 = new NBTTagList();

        for(ItemStack itemStack2 : this.handItems) {
            NBTTagCompound compoundTag2 = new NBTTagCompound();
            if (!itemStack2.isEmpty()) {
                itemStack2.save(compoundTag2);
            }

            listTag2.add(compoundTag2);
        }

        nbt.set("HandItems", listTag2);
        nbt.setBoolean("Invisible", this.isInvisible());
        nbt.setBoolean("Small", this.isSmall());
        nbt.setBoolean("ShowArms", this.hasArms());
        nbt.setInt("DisabledSlots", this.disabledSlots);
        nbt.setBoolean("NoBasePlate", this.hasBasePlate());
        if (this.isMarker()) {
            nbt.setBoolean("Marker", this.isMarker());
        }

        nbt.set("Pose", this.writePose());
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        if (nbt.hasKeyOfType("ArmorItems", 9)) {
            NBTTagList listTag = nbt.getList("ArmorItems", 10);

            for(int i = 0; i < this.armorItems.size(); ++i) {
                this.armorItems.set(i, ItemStack.of(listTag.getCompound(i)));
            }
        }

        if (nbt.hasKeyOfType("HandItems", 9)) {
            NBTTagList listTag2 = nbt.getList("HandItems", 10);

            for(int j = 0; j < this.handItems.size(); ++j) {
                this.handItems.set(j, ItemStack.of(listTag2.getCompound(j)));
            }
        }

        this.setInvisible(nbt.getBoolean("Invisible"));
        this.setSmall(nbt.getBoolean("Small"));
        this.setArms(nbt.getBoolean("ShowArms"));
        this.disabledSlots = nbt.getInt("DisabledSlots");
        this.setBasePlate(nbt.getBoolean("NoBasePlate"));
        this.setMarker(nbt.getBoolean("Marker"));
        this.noPhysics = !this.hasPhysics();
        NBTTagCompound compoundTag = nbt.getCompound("Pose");
        this.readPose(compoundTag);
    }

    private void readPose(NBTTagCompound nbt) {
        NBTTagList listTag = nbt.getList("Head", 5);
        this.setHeadPose(listTag.isEmpty() ? DEFAULT_HEAD_POSE : new Vector3f(listTag));
        NBTTagList listTag2 = nbt.getList("Body", 5);
        this.setBodyPose(listTag2.isEmpty() ? DEFAULT_BODY_POSE : new Vector3f(listTag2));
        NBTTagList listTag3 = nbt.getList("LeftArm", 5);
        this.setLeftArmPose(listTag3.isEmpty() ? DEFAULT_LEFT_ARM_POSE : new Vector3f(listTag3));
        NBTTagList listTag4 = nbt.getList("RightArm", 5);
        this.setRightArmPose(listTag4.isEmpty() ? DEFAULT_RIGHT_ARM_POSE : new Vector3f(listTag4));
        NBTTagList listTag5 = nbt.getList("LeftLeg", 5);
        this.setLeftLegPose(listTag5.isEmpty() ? DEFAULT_LEFT_LEG_POSE : new Vector3f(listTag5));
        NBTTagList listTag6 = nbt.getList("RightLeg", 5);
        this.setRightLegPose(listTag6.isEmpty() ? DEFAULT_RIGHT_LEG_POSE : new Vector3f(listTag6));
    }

    private NBTTagCompound writePose() {
        NBTTagCompound compoundTag = new NBTTagCompound();
        if (!DEFAULT_HEAD_POSE.equals(this.headPose)) {
            compoundTag.set("Head", this.headPose.save());
        }

        if (!DEFAULT_BODY_POSE.equals(this.bodyPose)) {
            compoundTag.set("Body", this.bodyPose.save());
        }

        if (!DEFAULT_LEFT_ARM_POSE.equals(this.leftArmPose)) {
            compoundTag.set("LeftArm", this.leftArmPose.save());
        }

        if (!DEFAULT_RIGHT_ARM_POSE.equals(this.rightArmPose)) {
            compoundTag.set("RightArm", this.rightArmPose.save());
        }

        if (!DEFAULT_LEFT_LEG_POSE.equals(this.leftLegPose)) {
            compoundTag.set("LeftLeg", this.leftLegPose.save());
        }

        if (!DEFAULT_RIGHT_LEG_POSE.equals(this.rightLegPose)) {
            compoundTag.set("RightLeg", this.rightLegPose.save());
        }

        return compoundTag;
    }

    @Override
    public boolean isCollidable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    protected void collideNearby() {
        List<Entity> list = this.level.getEntities(this, this.getBoundingBox(), RIDABLE_MINECARTS);

        for(int i = 0; i < list.size(); ++i) {
            Entity entity = list.get(i);
            if (this.distanceToSqr(entity) <= 0.2D) {
                entity.collide(this);
            }
        }

    }

    @Override
    public EnumInteractionResult interactAt(EntityHuman player, Vec3D hitPos, EnumHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (!this.isMarker() && !itemStack.is(Items.NAME_TAG)) {
            if (player.isSpectator()) {
                return EnumInteractionResult.SUCCESS;
            } else if (player.level.isClientSide) {
                return EnumInteractionResult.CONSUME;
            } else {
                EnumItemSlot equipmentSlot = EntityInsentient.getEquipmentSlotForItem(itemStack);
                if (itemStack.isEmpty()) {
                    EnumItemSlot equipmentSlot2 = this.getClickedSlot(hitPos);
                    EnumItemSlot equipmentSlot3 = this.isDisabled(equipmentSlot2) ? equipmentSlot : equipmentSlot2;
                    if (this.hasItemInSlot(equipmentSlot3) && this.swapItem(player, equipmentSlot3, itemStack, hand)) {
                        return EnumInteractionResult.SUCCESS;
                    }
                } else {
                    if (this.isDisabled(equipmentSlot)) {
                        return EnumInteractionResult.FAIL;
                    }

                    if (equipmentSlot.getType() == EnumItemSlot.Function.HAND && !this.hasArms()) {
                        return EnumInteractionResult.FAIL;
                    }

                    if (this.swapItem(player, equipmentSlot, itemStack, hand)) {
                        return EnumInteractionResult.SUCCESS;
                    }
                }

                return EnumInteractionResult.PASS;
            }
        } else {
            return EnumInteractionResult.PASS;
        }
    }

    private EnumItemSlot getClickedSlot(Vec3D hitPos) {
        EnumItemSlot equipmentSlot = EnumItemSlot.MAINHAND;
        boolean bl = this.isSmall();
        double d = bl ? hitPos.y * 2.0D : hitPos.y;
        EnumItemSlot equipmentSlot2 = EnumItemSlot.FEET;
        if (d >= 0.1D && d < 0.1D + (bl ? 0.8D : 0.45D) && this.hasItemInSlot(equipmentSlot2)) {
            equipmentSlot = EnumItemSlot.FEET;
        } else if (d >= 0.9D + (bl ? 0.3D : 0.0D) && d < 0.9D + (bl ? 1.0D : 0.7D) && this.hasItemInSlot(EnumItemSlot.CHEST)) {
            equipmentSlot = EnumItemSlot.CHEST;
        } else if (d >= 0.4D && d < 0.4D + (bl ? 1.0D : 0.8D) && this.hasItemInSlot(EnumItemSlot.LEGS)) {
            equipmentSlot = EnumItemSlot.LEGS;
        } else if (d >= 1.6D && this.hasItemInSlot(EnumItemSlot.HEAD)) {
            equipmentSlot = EnumItemSlot.HEAD;
        } else if (!this.hasItemInSlot(EnumItemSlot.MAINHAND) && this.hasItemInSlot(EnumItemSlot.OFFHAND)) {
            equipmentSlot = EnumItemSlot.OFFHAND;
        }

        return equipmentSlot;
    }

    public boolean isDisabled(EnumItemSlot slot) {
        return (this.disabledSlots & 1 << slot.getSlotFlag()) != 0 || slot.getType() == EnumItemSlot.Function.HAND && !this.hasArms();
    }

    private boolean swapItem(EntityHuman player, EnumItemSlot slot, ItemStack stack, EnumHand hand) {
        ItemStack itemStack = this.getEquipment(slot);
        if (!itemStack.isEmpty() && (this.disabledSlots & 1 << slot.getSlotFlag() + 8) != 0) {
            return false;
        } else if (itemStack.isEmpty() && (this.disabledSlots & 1 << slot.getSlotFlag() + 16) != 0) {
            return false;
        } else if (player.getAbilities().instabuild && itemStack.isEmpty() && !stack.isEmpty()) {
            ItemStack itemStack2 = stack.cloneItemStack();
            itemStack2.setCount(1);
            this.setSlot(slot, itemStack2);
            return true;
        } else if (!stack.isEmpty() && stack.getCount() > 1) {
            if (!itemStack.isEmpty()) {
                return false;
            } else {
                ItemStack itemStack3 = stack.cloneItemStack();
                itemStack3.setCount(1);
                this.setSlot(slot, itemStack3);
                stack.subtract(1);
                return true;
            }
        } else {
            this.setSlot(slot, stack);
            player.setItemInHand(hand, itemStack);
            return true;
        }
    }

    @Override
    public boolean damageEntity(DamageSource source, float amount) {
        if (!this.level.isClientSide && !this.isRemoved()) {
            if (DamageSource.OUT_OF_WORLD.equals(source)) {
                this.killEntity();
                return false;
            } else if (!this.isInvulnerable(source) && !this.invisible && !this.isMarker()) {
                if (source.isExplosion()) {
                    this.brokenByAnything(source);
                    this.killEntity();
                    return false;
                } else if (DamageSource.IN_FIRE.equals(source)) {
                    if (this.isBurning()) {
                        this.causeDamage(source, 0.15F);
                    } else {
                        this.setOnFire(5);
                    }

                    return false;
                } else if (DamageSource.ON_FIRE.equals(source) && this.getHealth() > 0.5F) {
                    this.causeDamage(source, 4.0F);
                    return false;
                } else {
                    boolean bl = source.getDirectEntity() instanceof EntityArrow;
                    boolean bl2 = bl && ((EntityArrow)source.getDirectEntity()).getPierceLevel() > 0;
                    boolean bl3 = "player".equals(source.getMsgId());
                    if (!bl3 && !bl) {
                        return false;
                    } else if (source.getEntity() instanceof EntityHuman && !((EntityHuman)source.getEntity()).getAbilities().mayBuild) {
                        return false;
                    } else if (source.isCreativePlayer()) {
                        this.playBrokenSound();
                        this.showBreakingParticles();
                        this.killEntity();
                        return bl2;
                    } else {
                        long l = this.level.getTime();
                        if (l - this.lastHit > 5L && !bl) {
                            this.level.broadcastEntityEffect(this, (byte)32);
                            this.gameEvent(GameEvent.ENTITY_DAMAGED, source.getEntity());
                            this.lastHit = l;
                        } else {
                            this.brokenByPlayer(source);
                            this.showBreakingParticles();
                            this.killEntity();
                        }

                        return true;
                    }
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public void handleEntityEvent(byte status) {
        if (status == 32) {
            if (this.level.isClientSide) {
                this.level.playLocalSound(this.locX(), this.locY(), this.locZ(), SoundEffects.ARMOR_STAND_HIT, this.getSoundCategory(), 0.3F, 1.0F, false);
                this.lastHit = this.level.getTime();
            }
        } else {
            super.handleEntityEvent(status);
        }

    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double d = this.getBoundingBox().getSize() * 4.0D;
        if (Double.isNaN(d) || d == 0.0D) {
            d = 4.0D;
        }

        d = d * 64.0D;
        return distance < d * d;
    }

    private void showBreakingParticles() {
        if (this.level instanceof WorldServer) {
            ((WorldServer)this.level).sendParticles(new ParticleParamBlock(Particles.BLOCK, Blocks.OAK_PLANKS.getBlockData()), this.locX(), this.getY(0.6666666666666666D), this.locZ(), 10, (double)(this.getWidth() / 4.0F), (double)(this.getHeight() / 4.0F), (double)(this.getWidth() / 4.0F), 0.05D);
        }

    }

    private void causeDamage(DamageSource damageSource, float amount) {
        float f = this.getHealth();
        f = f - amount;
        if (f <= 0.5F) {
            this.brokenByAnything(damageSource);
            this.killEntity();
        } else {
            this.setHealth(f);
            this.gameEvent(GameEvent.ENTITY_DAMAGED, damageSource.getEntity());
        }

    }

    private void brokenByPlayer(DamageSource damageSource) {
        Block.popResource(this.level, this.getChunkCoordinates(), new ItemStack(Items.ARMOR_STAND));
        this.brokenByAnything(damageSource);
    }

    private void brokenByAnything(DamageSource damageSource) {
        this.playBrokenSound();
        this.dropAllDeathLoot(damageSource);

        for(int i = 0; i < this.handItems.size(); ++i) {
            ItemStack itemStack = this.handItems.get(i);
            if (!itemStack.isEmpty()) {
                Block.popResource(this.level, this.getChunkCoordinates().above(), itemStack);
                this.handItems.set(i, ItemStack.EMPTY);
            }
        }

        for(int j = 0; j < this.armorItems.size(); ++j) {
            ItemStack itemStack2 = this.armorItems.get(j);
            if (!itemStack2.isEmpty()) {
                Block.popResource(this.level, this.getChunkCoordinates().above(), itemStack2);
                this.armorItems.set(j, ItemStack.EMPTY);
            }
        }

    }

    private void playBrokenSound() {
        this.level.playSound((EntityHuman)null, this.locX(), this.locY(), this.locZ(), SoundEffects.ARMOR_STAND_BREAK, this.getSoundCategory(), 1.0F, 1.0F);
    }

    @Override
    protected float tickHeadTurn(float bodyRotation, float headRotation) {
        this.yBodyRotO = this.yRotO;
        this.yBodyRot = this.getYRot();
        return 0.0F;
    }

    @Override
    protected float getStandingEyeHeight(EntityPose pose, EntitySize dimensions) {
        return dimensions.height * (this.isBaby() ? 0.5F : 0.9F);
    }

    @Override
    public double getMyRidingOffset() {
        return this.isMarker() ? 0.0D : (double)0.1F;
    }

    @Override
    public void travel(Vec3D movementInput) {
        if (this.hasPhysics()) {
            super.travel(movementInput);
        }
    }

    @Override
    public void setYBodyRot(float bodyYaw) {
        this.yBodyRotO = this.yRotO = bodyYaw;
        this.yHeadRotO = this.yHeadRot = bodyYaw;
    }

    @Override
    public void setHeadRotation(float headYaw) {
        this.yBodyRotO = this.yRotO = headYaw;
        this.yHeadRotO = this.yHeadRot = headYaw;
    }

    @Override
    public void tick() {
        super.tick();
        Vector3f rotations = this.entityData.get(DATA_HEAD_POSE);
        if (!this.headPose.equals(rotations)) {
            this.setHeadPose(rotations);
        }

        Vector3f rotations2 = this.entityData.get(DATA_BODY_POSE);
        if (!this.bodyPose.equals(rotations2)) {
            this.setBodyPose(rotations2);
        }

        Vector3f rotations3 = this.entityData.get(DATA_LEFT_ARM_POSE);
        if (!this.leftArmPose.equals(rotations3)) {
            this.setLeftArmPose(rotations3);
        }

        Vector3f rotations4 = this.entityData.get(DATA_RIGHT_ARM_POSE);
        if (!this.rightArmPose.equals(rotations4)) {
            this.setRightArmPose(rotations4);
        }

        Vector3f rotations5 = this.entityData.get(DATA_LEFT_LEG_POSE);
        if (!this.leftLegPose.equals(rotations5)) {
            this.setLeftLegPose(rotations5);
        }

        Vector3f rotations6 = this.entityData.get(DATA_RIGHT_LEG_POSE);
        if (!this.rightLegPose.equals(rotations6)) {
            this.setRightLegPose(rotations6);
        }

    }

    @Override
    protected void updateInvisibilityStatus() {
        this.setInvisible(this.invisible);
    }

    @Override
    public void setInvisible(boolean invisible) {
        this.invisible = invisible;
        super.setInvisible(invisible);
    }

    @Override
    public boolean isBaby() {
        return this.isSmall();
    }

    @Override
    public void killEntity() {
        this.remove(Entity.RemovalReason.KILLED);
    }

    @Override
    public boolean ignoreExplosion() {
        return this.isInvisible();
    }

    @Override
    public EnumPistonReaction getPushReaction() {
        return this.isMarker() ? EnumPistonReaction.IGNORE : super.getPushReaction();
    }

    public void setSmall(boolean small) {
        this.entityData.set(DATA_CLIENT_FLAGS, this.setBit(this.entityData.get(DATA_CLIENT_FLAGS), 1, small));
    }

    public boolean isSmall() {
        return (this.entityData.get(DATA_CLIENT_FLAGS) & 1) != 0;
    }

    public void setArms(boolean showArms) {
        this.entityData.set(DATA_CLIENT_FLAGS, this.setBit(this.entityData.get(DATA_CLIENT_FLAGS), 4, showArms));
    }

    public boolean hasArms() {
        return (this.entityData.get(DATA_CLIENT_FLAGS) & 4) != 0;
    }

    public void setBasePlate(boolean hideBasePlate) {
        this.entityData.set(DATA_CLIENT_FLAGS, this.setBit(this.entityData.get(DATA_CLIENT_FLAGS), 8, hideBasePlate));
    }

    public boolean hasBasePlate() {
        return (this.entityData.get(DATA_CLIENT_FLAGS) & 8) != 0;
    }

    public void setMarker(boolean marker) {
        this.entityData.set(DATA_CLIENT_FLAGS, this.setBit(this.entityData.get(DATA_CLIENT_FLAGS), 16, marker));
    }

    public boolean isMarker() {
        return (this.entityData.get(DATA_CLIENT_FLAGS) & 16) != 0;
    }

    private byte setBit(byte value, int bitField, boolean set) {
        if (set) {
            value = (byte)(value | bitField);
        } else {
            value = (byte)(value & ~bitField);
        }

        return value;
    }

    public void setHeadPose(Vector3f angle) {
        this.headPose = angle;
        this.entityData.set(DATA_HEAD_POSE, angle);
    }

    public void setBodyPose(Vector3f angle) {
        this.bodyPose = angle;
        this.entityData.set(DATA_BODY_POSE, angle);
    }

    public void setLeftArmPose(Vector3f angle) {
        this.leftArmPose = angle;
        this.entityData.set(DATA_LEFT_ARM_POSE, angle);
    }

    public void setRightArmPose(Vector3f angle) {
        this.rightArmPose = angle;
        this.entityData.set(DATA_RIGHT_ARM_POSE, angle);
    }

    public void setLeftLegPose(Vector3f angle) {
        this.leftLegPose = angle;
        this.entityData.set(DATA_LEFT_LEG_POSE, angle);
    }

    public void setRightLegPose(Vector3f angle) {
        this.rightLegPose = angle;
        this.entityData.set(DATA_RIGHT_LEG_POSE, angle);
    }

    public Vector3f getHeadPose() {
        return this.headPose;
    }

    public Vector3f getBodyPose() {
        return this.bodyPose;
    }

    public Vector3f getLeftArmPose() {
        return this.leftArmPose;
    }

    public Vector3f getRightArmPose() {
        return this.rightArmPose;
    }

    public Vector3f getLeftLegPose() {
        return this.leftLegPose;
    }

    public Vector3f getRightLegPose() {
        return this.rightLegPose;
    }

    @Override
    public boolean isInteractable() {
        return super.isInteractable() && !this.isMarker();
    }

    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        return attacker instanceof EntityHuman && !this.level.mayInteract((EntityHuman)attacker, this.getChunkCoordinates());
    }

    @Override
    public EnumMainHand getMainHand() {
        return EnumMainHand.RIGHT;
    }

    @Override
    public LivingEntity$Fallsounds getFallSounds() {
        return new LivingEntity$Fallsounds(SoundEffects.ARMOR_STAND_FALL, SoundEffects.ARMOR_STAND_FALL);
    }

    @Nullable
    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return SoundEffects.ARMOR_STAND_HIT;
    }

    @Nullable
    @Override
    public SoundEffect getSoundDeath() {
        return SoundEffects.ARMOR_STAND_BREAK;
    }

    @Override
    public void onLightningStrike(WorldServer world, EntityLightning lightning) {
    }

    @Override
    public boolean isAffectedByPotions() {
        return false;
    }

    @Override
    public void onSyncedDataUpdated(DataWatcherObject<?> data) {
        if (DATA_CLIENT_FLAGS.equals(data)) {
            this.updateSize();
            this.blocksBuilding = !this.isMarker();
        }

        super.onSyncedDataUpdated(data);
    }

    @Override
    public boolean attackable() {
        return false;
    }

    @Override
    public EntitySize getDimensions(EntityPose pose) {
        return this.getDimensionsMarker(this.isMarker());
    }

    private EntitySize getDimensionsMarker(boolean marker) {
        if (marker) {
            return MARKER_DIMENSIONS;
        } else {
            return this.isBaby() ? BABY_DIMENSIONS : this.getEntityType().getDimensions();
        }
    }

    @Override
    public Vec3D getLightProbePosition(float tickDelta) {
        if (this.isMarker()) {
            AxisAlignedBB aABB = this.getDimensionsMarker(false).makeBoundingBox(this.getPositionVector());
            BlockPosition blockPos = this.getChunkCoordinates();
            int i = Integer.MIN_VALUE;

            for(BlockPosition blockPos2 : BlockPosition.betweenClosed(new BlockPosition(aABB.minX, aABB.minY, aABB.minZ), new BlockPosition(aABB.maxX, aABB.maxY, aABB.maxZ))) {
                int j = Math.max(this.level.getBrightness(EnumSkyBlock.BLOCK, blockPos2), this.level.getBrightness(EnumSkyBlock.SKY, blockPos2));
                if (j == 15) {
                    return Vec3D.atCenterOf(blockPos2);
                }

                if (j > i) {
                    i = j;
                    blockPos = blockPos2.immutableCopy();
                }
            }

            return Vec3D.atCenterOf(blockPos);
        } else {
            return super.getLightProbePosition(tickDelta);
        }
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(Items.ARMOR_STAND);
    }

    @Override
    public boolean canBeSeenByAnyone() {
        return !this.isInvisible() && !this.isMarker();
    }
}
