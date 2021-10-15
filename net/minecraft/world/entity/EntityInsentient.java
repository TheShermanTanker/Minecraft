package net.minecraft.world.entity;

import com.google.common.collect.Maps;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.Particles;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.protocol.game.PacketDebug;
import net.minecraft.network.protocol.game.PacketPlayOutAttachEntity;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.tags.Tag;
import net.minecraft.util.MathHelper;
import net.minecraft.world.DifficultyDamageScaler;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.control.ControllerJump;
import net.minecraft.world.entity.ai.control.ControllerLook;
import net.minecraft.world.entity.ai.control.ControllerMove;
import net.minecraft.world.entity.ai.control.EntityAIBodyControl;
import net.minecraft.world.entity.ai.goal.PathfinderGoal;
import net.minecraft.world.entity.ai.goal.PathfinderGoalSelector;
import net.minecraft.world.entity.ai.navigation.Navigation;
import net.minecraft.world.entity.ai.navigation.NavigationAbstract;
import net.minecraft.world.entity.ai.sensing.EntitySenses;
import net.minecraft.world.entity.decoration.EntityHanging;
import net.minecraft.world.entity.decoration.EntityLeash;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.monster.IMonster;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.vehicle.EntityBoat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemArmor;
import net.minecraft.world.item.ItemAxe;
import net.minecraft.world.item.ItemBlock;
import net.minecraft.world.item.ItemBow;
import net.minecraft.world.item.ItemCrossbow;
import net.minecraft.world.item.ItemMonsterEgg;
import net.minecraft.world.item.ItemProjectileWeapon;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemSword;
import net.minecraft.world.item.ItemTool;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentManager;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldAccess;
import net.minecraft.world.level.material.FluidType;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.loot.LootTableInfo;

public abstract class EntityInsentient extends EntityLiving {
    private static final DataWatcherObject<Byte> DATA_MOB_FLAGS_ID = DataWatcher.defineId(EntityInsentient.class, DataWatcherRegistry.BYTE);
    private static final int MOB_FLAG_NO_AI = 1;
    private static final int MOB_FLAG_LEFTHANDED = 2;
    private static final int MOB_FLAG_AGGRESSIVE = 4;
    public static final float MAX_WEARING_ARMOR_CHANCE = 0.15F;
    public static final float MAX_PICKUP_LOOT_CHANCE = 0.55F;
    public static final float MAX_ENCHANTED_ARMOR_CHANCE = 0.5F;
    public static final float MAX_ENCHANTED_WEAPON_CHANCE = 0.25F;
    public static final String LEASH_TAG = "Leash";
    private static final int PICKUP_REACH = 1;
    public static final float DEFAULT_EQUIPMENT_DROP_CHANCE = 0.085F;
    public int ambientSoundTime;
    protected int xpReward;
    protected ControllerLook lookControl;
    protected ControllerMove moveControl;
    protected ControllerJump jumpControl;
    private final EntityAIBodyControl bodyRotationControl;
    protected NavigationAbstract navigation;
    public PathfinderGoalSelector goalSelector;
    public PathfinderGoalSelector targetSelector;
    private EntityLiving target;
    private final EntitySenses sensing;
    private final NonNullList<ItemStack> handItems = NonNullList.withSize(2, ItemStack.EMPTY);
    public final float[] handDropChances = new float[2];
    private final NonNullList<ItemStack> armorItems = NonNullList.withSize(4, ItemStack.EMPTY);
    public final float[] armorDropChances = new float[4];
    private boolean canPickUpLoot;
    public boolean persistenceRequired;
    private final Map<PathType, Float> pathfindingMalus = Maps.newEnumMap(PathType.class);
    public MinecraftKey lootTable;
    public long lootTableSeed;
    @Nullable
    public Entity leashHolder;
    private int delayedLeashHolderId;
    @Nullable
    private NBTTagCompound leashInfoTag;
    private BlockPosition restrictCenter = BlockPosition.ZERO;
    private float restrictRadius = -1.0F;

    protected EntityInsentient(EntityTypes<? extends EntityInsentient> type, World world) {
        super(type, world);
        this.goalSelector = new PathfinderGoalSelector(world.getMethodProfilerSupplier());
        this.targetSelector = new PathfinderGoalSelector(world.getMethodProfilerSupplier());
        this.lookControl = new ControllerLook(this);
        this.moveControl = new ControllerMove(this);
        this.jumpControl = new ControllerJump(this);
        this.bodyRotationControl = this.createBodyControl();
        this.navigation = this.createNavigation(world);
        this.sensing = new EntitySenses(this);
        Arrays.fill(this.armorDropChances, 0.085F);
        Arrays.fill(this.handDropChances, 0.085F);
        if (world != null && !world.isClientSide) {
            this.initPathfinder();
        }

    }

    protected void initPathfinder() {
    }

    public static AttributeProvider.Builder createMobAttributes() {
        return EntityLiving.createLivingAttributes().add(GenericAttributes.FOLLOW_RANGE, 16.0D).add(GenericAttributes.ATTACK_KNOCKBACK);
    }

    protected NavigationAbstract createNavigation(World world) {
        return new Navigation(this, world);
    }

    protected boolean shouldPassengersInheritMalus() {
        return false;
    }

    public float getPathfindingMalus(PathType nodeType) {
        EntityInsentient mob;
        if (this.getVehicle() instanceof EntityInsentient && ((EntityInsentient)this.getVehicle()).shouldPassengersInheritMalus()) {
            mob = (EntityInsentient)this.getVehicle();
        } else {
            mob = this;
        }

        Float float_ = mob.pathfindingMalus.get(nodeType);
        return float_ == null ? nodeType.getMalus() : float_;
    }

    public void setPathfindingMalus(PathType nodeType, float penalty) {
        this.pathfindingMalus.put(nodeType, penalty);
    }

    public boolean canCutCorner(PathType type) {
        return type != PathType.DANGER_FIRE && type != PathType.DANGER_CACTUS && type != PathType.DANGER_OTHER && type != PathType.WALKABLE_DOOR;
    }

    protected EntityAIBodyControl createBodyControl() {
        return new EntityAIBodyControl(this);
    }

    public ControllerLook getControllerLook() {
        return this.lookControl;
    }

    public ControllerMove getControllerMove() {
        if (this.isPassenger() && this.getVehicle() instanceof EntityInsentient) {
            EntityInsentient mob = (EntityInsentient)this.getVehicle();
            return mob.getControllerMove();
        } else {
            return this.moveControl;
        }
    }

    public ControllerJump getControllerJump() {
        return this.jumpControl;
    }

    public NavigationAbstract getNavigation() {
        if (this.isPassenger() && this.getVehicle() instanceof EntityInsentient) {
            EntityInsentient mob = (EntityInsentient)this.getVehicle();
            return mob.getNavigation();
        } else {
            return this.navigation;
        }
    }

    public EntitySenses getEntitySenses() {
        return this.sensing;
    }

    @Nullable
    public EntityLiving getGoalTarget() {
        return this.target;
    }

    public void setGoalTarget(@Nullable EntityLiving target) {
        this.target = target;
    }

    @Override
    public boolean canAttackType(EntityTypes<?> type) {
        return type != EntityTypes.GHAST;
    }

    public boolean canFireProjectileWeapon(ItemProjectileWeapon weapon) {
        return false;
    }

    public void blockEaten() {
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.entityData.register(DATA_MOB_FLAGS_ID, (byte)0);
    }

    public int getAmbientSoundInterval() {
        return 80;
    }

    public void playAmbientSound() {
        SoundEffect soundEvent = this.getSoundAmbient();
        if (soundEvent != null) {
            this.playSound(soundEvent, this.getSoundVolume(), this.getVoicePitch());
        }

    }

    @Override
    public void entityBaseTick() {
        super.entityBaseTick();
        this.level.getMethodProfiler().enter("mobBaseTick");
        if (this.isAlive() && this.random.nextInt(1000) < this.ambientSoundTime++) {
            this.resetAmbientSoundTime();
            this.playAmbientSound();
        }

        this.level.getMethodProfiler().exit();
    }

    @Override
    protected void playHurtSound(DamageSource source) {
        this.resetAmbientSoundTime();
        super.playHurtSound(source);
    }

    private void resetAmbientSoundTime() {
        this.ambientSoundTime = -this.getAmbientSoundInterval();
    }

    @Override
    protected int getExpValue(EntityHuman player) {
        if (this.xpReward > 0) {
            int i = this.xpReward;

            for(int j = 0; j < this.armorItems.size(); ++j) {
                if (!this.armorItems.get(j).isEmpty() && this.armorDropChances[j] <= 1.0F) {
                    i += 1 + this.random.nextInt(3);
                }
            }

            for(int k = 0; k < this.handItems.size(); ++k) {
                if (!this.handItems.get(k).isEmpty() && this.handDropChances[k] <= 1.0F) {
                    i += 1 + this.random.nextInt(3);
                }
            }

            return i;
        } else {
            return this.xpReward;
        }
    }

    public void doSpawnEffect() {
        if (this.level.isClientSide) {
            for(int i = 0; i < 20; ++i) {
                double d = this.random.nextGaussian() * 0.02D;
                double e = this.random.nextGaussian() * 0.02D;
                double f = this.random.nextGaussian() * 0.02D;
                double g = 10.0D;
                this.level.addParticle(Particles.POOF, this.getX(1.0D) - d * 10.0D, this.getRandomY() - e * 10.0D, this.getRandomZ(1.0D) - f * 10.0D, d, e, f);
            }
        } else {
            this.level.broadcastEntityEffect(this, (byte)20);
        }

    }

    @Override
    public void handleEntityEvent(byte status) {
        if (status == 20) {
            this.doSpawnEffect();
        } else {
            super.handleEntityEvent(status);
        }

    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level.isClientSide) {
            this.tickLeash();
            if (this.tickCount % 5 == 0) {
                this.updateControlFlags();
            }
        }

    }

    protected void updateControlFlags() {
        boolean bl = !(this.getRidingPassenger() instanceof EntityInsentient);
        boolean bl2 = !(this.getVehicle() instanceof EntityBoat);
        this.goalSelector.setControlFlag(PathfinderGoal.Type.MOVE, bl);
        this.goalSelector.setControlFlag(PathfinderGoal.Type.JUMP, bl && bl2);
        this.goalSelector.setControlFlag(PathfinderGoal.Type.LOOK, bl);
    }

    @Override
    protected float tickHeadTurn(float bodyRotation, float headRotation) {
        this.bodyRotationControl.clientTick();
        return headRotation;
    }

    @Nullable
    protected SoundEffect getSoundAmbient() {
        return null;
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        nbt.setBoolean("CanPickUpLoot", this.canPickupLoot());
        nbt.setBoolean("PersistenceRequired", this.persistenceRequired);
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
        NBTTagList listTag3 = new NBTTagList();

        for(float f : this.armorDropChances) {
            listTag3.add(NBTTagFloat.valueOf(f));
        }

        nbt.set("ArmorDropChances", listTag3);
        NBTTagList listTag4 = new NBTTagList();

        for(float g : this.handDropChances) {
            listTag4.add(NBTTagFloat.valueOf(g));
        }

        nbt.set("HandDropChances", listTag4);
        if (this.leashHolder != null) {
            NBTTagCompound compoundTag3 = new NBTTagCompound();
            if (this.leashHolder instanceof EntityLiving) {
                UUID uUID = this.leashHolder.getUniqueID();
                compoundTag3.putUUID("UUID", uUID);
            } else if (this.leashHolder instanceof EntityHanging) {
                BlockPosition blockPos = ((EntityHanging)this.leashHolder).getBlockPosition();
                compoundTag3.setInt("X", blockPos.getX());
                compoundTag3.setInt("Y", blockPos.getY());
                compoundTag3.setInt("Z", blockPos.getZ());
            }

            nbt.set("Leash", compoundTag3);
        } else if (this.leashInfoTag != null) {
            nbt.set("Leash", this.leashInfoTag.c());
        }

        nbt.setBoolean("LeftHanded", this.isLeftHanded());
        if (this.lootTable != null) {
            nbt.setString("DeathLootTable", this.lootTable.toString());
            if (this.lootTableSeed != 0L) {
                nbt.setLong("DeathLootTableSeed", this.lootTableSeed);
            }
        }

        if (this.isNoAI()) {
            nbt.setBoolean("NoAI", this.isNoAI());
        }

    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        if (nbt.hasKeyOfType("CanPickUpLoot", 1)) {
            this.setCanPickupLoot(nbt.getBoolean("CanPickUpLoot"));
        }

        this.persistenceRequired = nbt.getBoolean("PersistenceRequired");
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

        if (nbt.hasKeyOfType("ArmorDropChances", 9)) {
            NBTTagList listTag3 = nbt.getList("ArmorDropChances", 5);

            for(int k = 0; k < listTag3.size(); ++k) {
                this.armorDropChances[k] = listTag3.getFloat(k);
            }
        }

        if (nbt.hasKeyOfType("HandDropChances", 9)) {
            NBTTagList listTag4 = nbt.getList("HandDropChances", 5);

            for(int l = 0; l < listTag4.size(); ++l) {
                this.handDropChances[l] = listTag4.getFloat(l);
            }
        }

        if (nbt.hasKeyOfType("Leash", 10)) {
            this.leashInfoTag = nbt.getCompound("Leash");
        }

        this.setLeftHanded(nbt.getBoolean("LeftHanded"));
        if (nbt.hasKeyOfType("DeathLootTable", 8)) {
            this.lootTable = new MinecraftKey(nbt.getString("DeathLootTable"));
            this.lootTableSeed = nbt.getLong("DeathLootTableSeed");
        }

        this.setNoAI(nbt.getBoolean("NoAI"));
    }

    @Override
    protected void dropFromLootTable(DamageSource source, boolean causedByPlayer) {
        super.dropFromLootTable(source, causedByPlayer);
        this.lootTable = null;
    }

    @Override
    protected LootTableInfo.Builder createLootContext(boolean causedByPlayer, DamageSource source) {
        return super.createLootContext(causedByPlayer, source).withOptionalRandomSeed(this.lootTableSeed, this.random);
    }

    @Override
    public final MinecraftKey getLootTable() {
        return this.lootTable == null ? this.getDefaultLootTable() : this.lootTable;
    }

    protected MinecraftKey getDefaultLootTable() {
        return super.getLootTable();
    }

    public void setZza(float forwardSpeed) {
        this.zza = forwardSpeed;
    }

    public void setYya(float upwardSpeed) {
        this.yya = upwardSpeed;
    }

    public void setXxa(float sidewaysMovement) {
        this.xxa = sidewaysMovement;
    }

    @Override
    public void setSpeed(float movementSpeed) {
        super.setSpeed(movementSpeed);
        this.setZza(movementSpeed);
    }

    @Override
    public void movementTick() {
        super.movementTick();
        this.level.getMethodProfiler().enter("looting");
        if (!this.level.isClientSide && this.canPickupLoot() && this.isAlive() && !this.dead && this.level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            for(EntityItem itemEntity : this.level.getEntitiesOfClass(EntityItem.class, this.getBoundingBox().grow(1.0D, 0.0D, 1.0D))) {
                if (!itemEntity.isRemoved() && !itemEntity.getItemStack().isEmpty() && !itemEntity.hasPickUpDelay() && this.wantsToPickUp(itemEntity.getItemStack())) {
                    this.pickUpItem(itemEntity);
                }
            }
        }

        this.level.getMethodProfiler().exit();
    }

    protected void pickUpItem(EntityItem item) {
        ItemStack itemStack = item.getItemStack();
        if (this.equipItemIfPossible(itemStack)) {
            this.onItemPickup(item);
            this.receive(item, itemStack.getCount());
            item.die();
        }

    }

    public boolean equipItemIfPossible(ItemStack equipment) {
        EnumItemSlot equipmentSlot = getEquipmentSlotForItem(equipment);
        ItemStack itemStack = this.getEquipment(equipmentSlot);
        boolean bl = this.canReplaceCurrentItem(equipment, itemStack);
        if (bl && this.canPickup(equipment)) {
            double d = (double)this.getEquipmentDropChance(equipmentSlot);
            if (!itemStack.isEmpty() && (double)Math.max(this.random.nextFloat() - 0.1F, 0.0F) < d) {
                this.spawnAtLocation(itemStack);
            }

            this.setItemSlotAndDropWhenKilled(equipmentSlot, equipment);
            this.playEquipSound(equipment);
            return true;
        } else {
            return false;
        }
    }

    protected void setItemSlotAndDropWhenKilled(EnumItemSlot slot, ItemStack stack) {
        this.setSlot(slot, stack);
        this.setGuaranteedDrop(slot);
        this.persistenceRequired = true;
    }

    public void setGuaranteedDrop(EnumItemSlot slot) {
        switch(slot.getType()) {
        case HAND:
            this.handDropChances[slot.getIndex()] = 2.0F;
            break;
        case ARMOR:
            this.armorDropChances[slot.getIndex()] = 2.0F;
        }

    }

    protected boolean canReplaceCurrentItem(ItemStack newStack, ItemStack oldStack) {
        if (oldStack.isEmpty()) {
            return true;
        } else if (newStack.getItem() instanceof ItemSword) {
            if (!(oldStack.getItem() instanceof ItemSword)) {
                return true;
            } else {
                ItemSword swordItem = (ItemSword)newStack.getItem();
                ItemSword swordItem2 = (ItemSword)oldStack.getItem();
                if (swordItem.getDamage() != swordItem2.getDamage()) {
                    return swordItem.getDamage() > swordItem2.getDamage();
                } else {
                    return this.canReplaceEqualItem(newStack, oldStack);
                }
            }
        } else if (newStack.getItem() instanceof ItemBow && oldStack.getItem() instanceof ItemBow) {
            return this.canReplaceEqualItem(newStack, oldStack);
        } else if (newStack.getItem() instanceof ItemCrossbow && oldStack.getItem() instanceof ItemCrossbow) {
            return this.canReplaceEqualItem(newStack, oldStack);
        } else if (newStack.getItem() instanceof ItemArmor) {
            if (EnchantmentManager.hasBindingCurse(oldStack)) {
                return false;
            } else if (!(oldStack.getItem() instanceof ItemArmor)) {
                return true;
            } else {
                ItemArmor armorItem = (ItemArmor)newStack.getItem();
                ItemArmor armorItem2 = (ItemArmor)oldStack.getItem();
                if (armorItem.getDefense() != armorItem2.getDefense()) {
                    return armorItem.getDefense() > armorItem2.getDefense();
                } else if (armorItem.getToughness() != armorItem2.getToughness()) {
                    return armorItem.getToughness() > armorItem2.getToughness();
                } else {
                    return this.canReplaceEqualItem(newStack, oldStack);
                }
            }
        } else {
            if (newStack.getItem() instanceof ItemTool) {
                if (oldStack.getItem() instanceof ItemBlock) {
                    return true;
                }

                if (oldStack.getItem() instanceof ItemTool) {
                    ItemTool diggerItem = (ItemTool)newStack.getItem();
                    ItemTool diggerItem2 = (ItemTool)oldStack.getItem();
                    if (diggerItem.getAttackDamage() != diggerItem2.getAttackDamage()) {
                        return diggerItem.getAttackDamage() > diggerItem2.getAttackDamage();
                    }

                    return this.canReplaceEqualItem(newStack, oldStack);
                }
            }

            return false;
        }
    }

    public boolean canReplaceEqualItem(ItemStack newStack, ItemStack oldStack) {
        if (newStack.getDamage() >= oldStack.getDamage() && (!newStack.hasTag() || oldStack.hasTag())) {
            if (newStack.hasTag() && oldStack.hasTag()) {
                return newStack.getTag().getKeys().stream().anyMatch((string) -> {
                    return !string.equals("Damage");
                }) && !oldStack.getTag().getKeys().stream().anyMatch((string) -> {
                    return !string.equals("Damage");
                });
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    public boolean canPickup(ItemStack stack) {
        return true;
    }

    public boolean wantsToPickUp(ItemStack stack) {
        return this.canPickup(stack);
    }

    public boolean isTypeNotPersistent(double distanceSquared) {
        return true;
    }

    public boolean isSpecialPersistence() {
        return this.isPassenger();
    }

    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public void checkDespawn() {
        if (this.level.getDifficulty() == EnumDifficulty.PEACEFUL && this.shouldDespawnInPeaceful()) {
            this.die();
        } else if (!this.isPersistent() && !this.isSpecialPersistence()) {
            Entity entity = this.level.findNearbyPlayer(this, -1.0D);
            if (entity != null) {
                double d = entity.distanceToSqr(this);
                int i = this.getEntityType().getCategory().getDespawnDistance();
                int j = i * i;
                if (d > (double)j && this.isTypeNotPersistent(d)) {
                    this.die();
                }

                int k = this.getEntityType().getCategory().getNoDespawnDistance();
                int l = k * k;
                if (this.noActionTime > 600 && this.random.nextInt(800) == 0 && d > (double)l && this.isTypeNotPersistent(d)) {
                    this.die();
                } else if (d < (double)l) {
                    this.noActionTime = 0;
                }
            }

        } else {
            this.noActionTime = 0;
        }
    }

    @Override
    protected final void doTick() {
        ++this.noActionTime;
        this.level.getMethodProfiler().enter("sensing");
        this.sensing.tick();
        this.level.getMethodProfiler().exit();
        this.level.getMethodProfiler().enter("targetSelector");
        this.targetSelector.doTick();
        this.level.getMethodProfiler().exit();
        this.level.getMethodProfiler().enter("goalSelector");
        this.goalSelector.doTick();
        this.level.getMethodProfiler().exit();
        this.level.getMethodProfiler().enter("navigation");
        this.navigation.tick();
        this.level.getMethodProfiler().exit();
        this.level.getMethodProfiler().enter("mob tick");
        this.mobTick();
        this.level.getMethodProfiler().exit();
        this.level.getMethodProfiler().enter("controls");
        this.level.getMethodProfiler().enter("move");
        this.moveControl.tick();
        this.level.getMethodProfiler().exitEnter("look");
        this.lookControl.tick();
        this.level.getMethodProfiler().exitEnter("jump");
        this.jumpControl.tick();
        this.level.getMethodProfiler().exit();
        this.level.getMethodProfiler().exit();
        this.sendDebugPackets();
    }

    protected void sendDebugPackets() {
        PacketDebug.sendGoalSelector(this.level, this, this.goalSelector);
    }

    protected void mobTick() {
    }

    public int getMaxHeadXRot() {
        return 40;
    }

    public int getMaxHeadYRot() {
        return 75;
    }

    public int getHeadRotSpeed() {
        return 10;
    }

    public void lookAt(Entity targetEntity, float maxYawChange, float maxPitchChange) {
        double d = targetEntity.locX() - this.locX();
        double e = targetEntity.locZ() - this.locZ();
        double f;
        if (targetEntity instanceof EntityLiving) {
            EntityLiving livingEntity = (EntityLiving)targetEntity;
            f = livingEntity.getHeadY() - this.getHeadY();
        } else {
            f = (targetEntity.getBoundingBox().minY + targetEntity.getBoundingBox().maxY) / 2.0D - this.getHeadY();
        }

        double h = Math.sqrt(d * d + e * e);
        float i = (float)(MathHelper.atan2(e, d) * (double)(180F / (float)Math.PI)) - 90.0F;
        float j = (float)(-(MathHelper.atan2(f, h) * (double)(180F / (float)Math.PI)));
        this.setXRot(this.rotlerp(this.getXRot(), j, maxPitchChange));
        this.setYRot(this.rotlerp(this.getYRot(), i, maxYawChange));
    }

    private float rotlerp(float oldAngle, float newAngle, float maxChangeInAngle) {
        float f = MathHelper.wrapDegrees(newAngle - oldAngle);
        if (f > maxChangeInAngle) {
            f = maxChangeInAngle;
        }

        if (f < -maxChangeInAngle) {
            f = -maxChangeInAngle;
        }

        return oldAngle + f;
    }

    public static boolean checkMobSpawnRules(EntityTypes<? extends EntityInsentient> type, GeneratorAccess world, EnumMobSpawn spawnReason, BlockPosition pos, Random random) {
        BlockPosition blockPos = pos.below();
        return spawnReason == EnumMobSpawn.SPAWNER || world.getType(blockPos).isValidSpawn(world, blockPos, type);
    }

    public boolean checkSpawnRules(GeneratorAccess world, EnumMobSpawn spawnReason) {
        return true;
    }

    public boolean checkSpawnObstruction(IWorldReader world) {
        return !world.containsLiquid(this.getBoundingBox()) && world.isUnobstructed(this);
    }

    public int getMaxSpawnGroup() {
        return 4;
    }

    public boolean isMaxGroupSizeReached(int count) {
        return false;
    }

    @Override
    public int getMaxFallDistance() {
        if (this.getGoalTarget() == null) {
            return 3;
        } else {
            int i = (int)(this.getHealth() - this.getMaxHealth() * 0.33F);
            i = i - (3 - this.level.getDifficulty().getId()) * 4;
            if (i < 0) {
                i = 0;
            }

            return i + 3;
        }
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
            this.handItems.set(slot.getIndex(), stack);
            break;
        case ARMOR:
            this.armorItems.set(slot.getIndex(), stack);
        }

    }

    @Override
    protected void dropDeathLoot(DamageSource source, int lootingMultiplier, boolean allowDrops) {
        super.dropDeathLoot(source, lootingMultiplier, allowDrops);

        for(EnumItemSlot equipmentSlot : EnumItemSlot.values()) {
            ItemStack itemStack = this.getEquipment(equipmentSlot);
            float f = this.getEquipmentDropChance(equipmentSlot);
            boolean bl = f > 1.0F;
            if (!itemStack.isEmpty() && !EnchantmentManager.shouldNotDrop(itemStack) && (allowDrops || bl) && Math.max(this.random.nextFloat() - (float)lootingMultiplier * 0.01F, 0.0F) < f) {
                if (!bl && itemStack.isDamageableItem()) {
                    itemStack.setDamage(itemStack.getMaxDamage() - this.random.nextInt(1 + this.random.nextInt(Math.max(itemStack.getMaxDamage() - 3, 1))));
                }

                this.spawnAtLocation(itemStack);
                this.setSlot(equipmentSlot, ItemStack.EMPTY);
            }
        }

    }

    protected float getEquipmentDropChance(EnumItemSlot slot) {
        float f;
        switch(slot.getType()) {
        case HAND:
            f = this.handDropChances[slot.getIndex()];
            break;
        case ARMOR:
            f = this.armorDropChances[slot.getIndex()];
            break;
        default:
            f = 0.0F;
        }

        return f;
    }

    protected void populateDefaultEquipmentSlots(DifficultyDamageScaler difficulty) {
        if (this.random.nextFloat() < 0.15F * difficulty.getSpecialMultiplier()) {
            int i = this.random.nextInt(2);
            float f = this.level.getDifficulty() == EnumDifficulty.HARD ? 0.1F : 0.25F;
            if (this.random.nextFloat() < 0.095F) {
                ++i;
            }

            if (this.random.nextFloat() < 0.095F) {
                ++i;
            }

            if (this.random.nextFloat() < 0.095F) {
                ++i;
            }

            boolean bl = true;

            for(EnumItemSlot equipmentSlot : EnumItemSlot.values()) {
                if (equipmentSlot.getType() == EnumItemSlot.Function.ARMOR) {
                    ItemStack itemStack = this.getEquipment(equipmentSlot);
                    if (!bl && this.random.nextFloat() < f) {
                        break;
                    }

                    bl = false;
                    if (itemStack.isEmpty()) {
                        Item item = getEquipmentForSlot(equipmentSlot, i);
                        if (item != null) {
                            this.setSlot(equipmentSlot, new ItemStack(item));
                        }
                    }
                }
            }
        }

    }

    @Nullable
    public static Item getEquipmentForSlot(EnumItemSlot equipmentSlot, int equipmentLevel) {
        switch(equipmentSlot) {
        case HEAD:
            if (equipmentLevel == 0) {
                return Items.LEATHER_HELMET;
            } else if (equipmentLevel == 1) {
                return Items.GOLDEN_HELMET;
            } else if (equipmentLevel == 2) {
                return Items.CHAINMAIL_HELMET;
            } else if (equipmentLevel == 3) {
                return Items.IRON_HELMET;
            } else if (equipmentLevel == 4) {
                return Items.DIAMOND_HELMET;
            }
        case CHEST:
            if (equipmentLevel == 0) {
                return Items.LEATHER_CHESTPLATE;
            } else if (equipmentLevel == 1) {
                return Items.GOLDEN_CHESTPLATE;
            } else if (equipmentLevel == 2) {
                return Items.CHAINMAIL_CHESTPLATE;
            } else if (equipmentLevel == 3) {
                return Items.IRON_CHESTPLATE;
            } else if (equipmentLevel == 4) {
                return Items.DIAMOND_CHESTPLATE;
            }
        case LEGS:
            if (equipmentLevel == 0) {
                return Items.LEATHER_LEGGINGS;
            } else if (equipmentLevel == 1) {
                return Items.GOLDEN_LEGGINGS;
            } else if (equipmentLevel == 2) {
                return Items.CHAINMAIL_LEGGINGS;
            } else if (equipmentLevel == 3) {
                return Items.IRON_LEGGINGS;
            } else if (equipmentLevel == 4) {
                return Items.DIAMOND_LEGGINGS;
            }
        case FEET:
            if (equipmentLevel == 0) {
                return Items.LEATHER_BOOTS;
            } else if (equipmentLevel == 1) {
                return Items.GOLDEN_BOOTS;
            } else if (equipmentLevel == 2) {
                return Items.CHAINMAIL_BOOTS;
            } else if (equipmentLevel == 3) {
                return Items.IRON_BOOTS;
            } else if (equipmentLevel == 4) {
                return Items.DIAMOND_BOOTS;
            }
        default:
            return null;
        }
    }

    protected void populateDefaultEquipmentEnchantments(DifficultyDamageScaler difficulty) {
        float f = difficulty.getSpecialMultiplier();
        this.enchantSpawnedWeapon(f);

        for(EnumItemSlot equipmentSlot : EnumItemSlot.values()) {
            if (equipmentSlot.getType() == EnumItemSlot.Function.ARMOR) {
                this.enchantSpawnedArmor(f, equipmentSlot);
            }
        }

    }

    protected void enchantSpawnedWeapon(float power) {
        if (!this.getItemInMainHand().isEmpty() && this.random.nextFloat() < 0.25F * power) {
            this.setSlot(EnumItemSlot.MAINHAND, EnchantmentManager.enchantItem(this.random, this.getItemInMainHand(), (int)(5.0F + power * (float)this.random.nextInt(18)), false));
        }

    }

    protected void enchantSpawnedArmor(float power, EnumItemSlot slot) {
        ItemStack itemStack = this.getEquipment(slot);
        if (!itemStack.isEmpty() && this.random.nextFloat() < 0.5F * power) {
            this.setSlot(slot, EnchantmentManager.enchantItem(this.random, itemStack, (int)(5.0F + power * (float)this.random.nextInt(18)), false));
        }

    }

    @Nullable
    public GroupDataEntity prepare(WorldAccess world, DifficultyDamageScaler difficulty, EnumMobSpawn spawnReason, @Nullable GroupDataEntity entityData, @Nullable NBTTagCompound entityNbt) {
        this.getAttributeInstance(GenericAttributes.FOLLOW_RANGE).addPermanentModifier(new AttributeModifier("Random spawn bonus", this.random.nextGaussian() * 0.05D, AttributeModifier.Operation.MULTIPLY_BASE));
        if (this.random.nextFloat() < 0.05F) {
            this.setLeftHanded(true);
        } else {
            this.setLeftHanded(false);
        }

        return entityData;
    }

    public boolean canBeControlledByRider() {
        return false;
    }

    public void setPersistent() {
        this.persistenceRequired = true;
    }

    public void setDropChance(EnumItemSlot slot, float chance) {
        switch(slot.getType()) {
        case HAND:
            this.handDropChances[slot.getIndex()] = chance;
            break;
        case ARMOR:
            this.armorDropChances[slot.getIndex()] = chance;
        }

    }

    public boolean canPickupLoot() {
        return this.canPickUpLoot;
    }

    public void setCanPickupLoot(boolean pickUpLoot) {
        this.canPickUpLoot = pickUpLoot;
    }

    @Override
    public boolean canTakeItem(ItemStack stack) {
        EnumItemSlot equipmentSlot = getEquipmentSlotForItem(stack);
        return this.getEquipment(equipmentSlot).isEmpty() && this.canPickupLoot();
    }

    public boolean isPersistent() {
        return this.persistenceRequired;
    }

    @Override
    public final EnumInteractionResult interact(EntityHuman player, EnumHand hand) {
        if (!this.isAlive()) {
            return EnumInteractionResult.PASS;
        } else if (this.getLeashHolder() == player) {
            this.unleash(true, !player.getAbilities().instabuild);
            return EnumInteractionResult.sidedSuccess(this.level.isClientSide);
        } else {
            EnumInteractionResult interactionResult = this.checkAndHandleImportantInteractions(player, hand);
            if (interactionResult.consumesAction()) {
                return interactionResult;
            } else {
                interactionResult = this.mobInteract(player, hand);
                return interactionResult.consumesAction() ? interactionResult : super.interact(player, hand);
            }
        }
    }

    private EnumInteractionResult checkAndHandleImportantInteractions(EntityHuman player, EnumHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (itemStack.is(Items.LEAD) && this.canBeLeashed(player)) {
            this.setLeashHolder(player, true);
            itemStack.subtract(1);
            return EnumInteractionResult.sidedSuccess(this.level.isClientSide);
        } else {
            if (itemStack.is(Items.NAME_TAG)) {
                EnumInteractionResult interactionResult = itemStack.interactLivingEntity(player, this, hand);
                if (interactionResult.consumesAction()) {
                    return interactionResult;
                }
            }

            if (itemStack.getItem() instanceof ItemMonsterEgg) {
                if (this.level instanceof WorldServer) {
                    ItemMonsterEgg spawnEggItem = (ItemMonsterEgg)itemStack.getItem();
                    Optional<EntityInsentient> optional = spawnEggItem.spawnOffspringFromSpawnEgg(player, this, this.getEntityType(), (WorldServer)this.level, this.getPositionVector(), itemStack);
                    optional.ifPresent((mob) -> {
                        this.onOffspringSpawnedFromEgg(player, mob);
                    });
                    return optional.isPresent() ? EnumInteractionResult.SUCCESS : EnumInteractionResult.PASS;
                } else {
                    return EnumInteractionResult.CONSUME;
                }
            } else {
                return EnumInteractionResult.PASS;
            }
        }
    }

    protected void onOffspringSpawnedFromEgg(EntityHuman player, EntityInsentient child) {
    }

    protected EnumInteractionResult mobInteract(EntityHuman player, EnumHand hand) {
        return EnumInteractionResult.PASS;
    }

    public boolean isWithinRestriction() {
        return this.isWithinRestriction(this.getChunkCoordinates());
    }

    public boolean isWithinRestriction(BlockPosition pos) {
        if (this.restrictRadius == -1.0F) {
            return true;
        } else {
            return this.restrictCenter.distSqr(pos) < (double)(this.restrictRadius * this.restrictRadius);
        }
    }

    public void restrictTo(BlockPosition target, int range) {
        this.restrictCenter = target;
        this.restrictRadius = (float)range;
    }

    public BlockPosition getRestrictCenter() {
        return this.restrictCenter;
    }

    public float getRestrictRadius() {
        return this.restrictRadius;
    }

    public void clearRestriction() {
        this.restrictRadius = -1.0F;
    }

    public boolean hasRestriction() {
        return this.restrictRadius != -1.0F;
    }

    @Nullable
    public <T extends EntityInsentient> T convertTo(EntityTypes<T> entityType, boolean keepEquipment) {
        if (this.isRemoved()) {
            return (T)null;
        } else {
            T mob = entityType.create(this.level);
            mob.copyPosition(this);
            mob.setBaby(this.isBaby());
            mob.setNoAI(this.isNoAI());
            if (this.hasCustomName()) {
                mob.setCustomName(this.getCustomName());
                mob.setCustomNameVisible(this.getCustomNameVisible());
            }

            if (this.isPersistent()) {
                mob.setPersistent();
            }

            mob.setInvulnerable(this.isInvulnerable());
            if (keepEquipment) {
                mob.setCanPickupLoot(this.canPickupLoot());

                for(EnumItemSlot equipmentSlot : EnumItemSlot.values()) {
                    ItemStack itemStack = this.getEquipment(equipmentSlot);
                    if (!itemStack.isEmpty()) {
                        mob.setSlot(equipmentSlot, itemStack.cloneItemStack());
                        mob.setDropChance(equipmentSlot, this.getEquipmentDropChance(equipmentSlot));
                        itemStack.setCount(0);
                    }
                }
            }

            this.level.addEntity(mob);
            if (this.isPassenger()) {
                Entity entity = this.getVehicle();
                this.stopRiding();
                mob.startRiding(entity, true);
            }

            this.die();
            return mob;
        }
    }

    protected void tickLeash() {
        if (this.leashInfoTag != null) {
            this.restoreLeashFromSave();
        }

        if (this.leashHolder != null) {
            if (!this.isAlive() || !this.leashHolder.isAlive()) {
                this.unleash(true, true);
            }

        }
    }

    public void unleash(boolean sendPacket, boolean dropItem) {
        if (this.leashHolder != null) {
            this.leashHolder = null;
            this.leashInfoTag = null;
            if (!this.level.isClientSide && dropItem) {
                this.spawnAtLocation(Items.LEAD);
            }

            if (!this.level.isClientSide && sendPacket && this.level instanceof WorldServer) {
                ((WorldServer)this.level).getChunkSource().broadcast(this, new PacketPlayOutAttachEntity(this, (Entity)null));
            }
        }

    }

    public boolean canBeLeashed(EntityHuman player) {
        return !this.isLeashed() && !(this instanceof IMonster);
    }

    public boolean isLeashed() {
        return this.leashHolder != null;
    }

    @Nullable
    public Entity getLeashHolder() {
        if (this.leashHolder == null && this.delayedLeashHolderId != 0 && this.level.isClientSide) {
            this.leashHolder = this.level.getEntity(this.delayedLeashHolderId);
        }

        return this.leashHolder;
    }

    public void setLeashHolder(Entity entity, boolean sendPacket) {
        this.leashHolder = entity;
        this.leashInfoTag = null;
        if (!this.level.isClientSide && sendPacket && this.level instanceof WorldServer) {
            ((WorldServer)this.level).getChunkSource().broadcast(this, new PacketPlayOutAttachEntity(this, this.leashHolder));
        }

        if (this.isPassenger()) {
            this.stopRiding();
        }

    }

    public void setDelayedLeashHolderId(int id) {
        this.delayedLeashHolderId = id;
        this.unleash(false, false);
    }

    @Override
    public boolean startRiding(Entity entity, boolean force) {
        boolean bl = super.startRiding(entity, force);
        if (bl && this.isLeashed()) {
            this.unleash(true, true);
        }

        return bl;
    }

    private void restoreLeashFromSave() {
        if (this.leashInfoTag != null && this.level instanceof WorldServer) {
            if (this.leashInfoTag.hasUUID("UUID")) {
                UUID uUID = this.leashInfoTag.getUUID("UUID");
                Entity entity = ((WorldServer)this.level).getEntity(uUID);
                if (entity != null) {
                    this.setLeashHolder(entity, true);
                    return;
                }
            } else if (this.leashInfoTag.hasKeyOfType("X", 99) && this.leashInfoTag.hasKeyOfType("Y", 99) && this.leashInfoTag.hasKeyOfType("Z", 99)) {
                BlockPosition blockPos = new BlockPosition(this.leashInfoTag.getInt("X"), this.leashInfoTag.getInt("Y"), this.leashInfoTag.getInt("Z"));
                this.setLeashHolder(EntityLeash.getOrCreateKnot(this.level, blockPos), true);
                return;
            }

            if (this.tickCount > 100) {
                this.spawnAtLocation(Items.LEAD);
                this.leashInfoTag = null;
            }
        }

    }

    @Override
    public boolean isControlledByLocalInstance() {
        return this.canBeControlledByRider() && super.isControlledByLocalInstance();
    }

    @Override
    public boolean doAITick() {
        return super.doAITick() && !this.isNoAI();
    }

    public void setNoAI(boolean aiDisabled) {
        byte b = this.entityData.get(DATA_MOB_FLAGS_ID);
        this.entityData.set(DATA_MOB_FLAGS_ID, aiDisabled ? (byte)(b | 1) : (byte)(b & -2));
    }

    public void setLeftHanded(boolean leftHanded) {
        byte b = this.entityData.get(DATA_MOB_FLAGS_ID);
        this.entityData.set(DATA_MOB_FLAGS_ID, leftHanded ? (byte)(b | 2) : (byte)(b & -3));
    }

    public void setAggressive(boolean attacking) {
        byte b = this.entityData.get(DATA_MOB_FLAGS_ID);
        this.entityData.set(DATA_MOB_FLAGS_ID, attacking ? (byte)(b | 4) : (byte)(b & -5));
    }

    public boolean isNoAI() {
        return (this.entityData.get(DATA_MOB_FLAGS_ID) & 1) != 0;
    }

    public boolean isLeftHanded() {
        return (this.entityData.get(DATA_MOB_FLAGS_ID) & 2) != 0;
    }

    public boolean isAggressive() {
        return (this.entityData.get(DATA_MOB_FLAGS_ID) & 4) != 0;
    }

    public void setBaby(boolean baby) {
    }

    @Override
    public EnumMainHand getMainHand() {
        return this.isLeftHanded() ? EnumMainHand.LEFT : EnumMainHand.RIGHT;
    }

    public double getMeleeAttackRangeSqr(EntityLiving target) {
        return (double)(this.getWidth() * 2.0F * this.getWidth() * 2.0F + target.getWidth());
    }

    @Override
    public boolean attackEntity(Entity target) {
        float f = (float)this.getAttributeValue(GenericAttributes.ATTACK_DAMAGE);
        float g = (float)this.getAttributeValue(GenericAttributes.ATTACK_KNOCKBACK);
        if (target instanceof EntityLiving) {
            f += EnchantmentManager.getDamageBonus(this.getItemInMainHand(), ((EntityLiving)target).getMonsterType());
            g += (float)EnchantmentManager.getKnockbackBonus(this);
        }

        int i = EnchantmentManager.getFireAspectEnchantmentLevel(this);
        if (i > 0) {
            target.setOnFire(i * 4);
        }

        boolean bl = target.damageEntity(DamageSource.mobAttack(this), f);
        if (bl) {
            if (g > 0.0F && target instanceof EntityLiving) {
                ((EntityLiving)target).knockback((double)(g * 0.5F), (double)MathHelper.sin(this.getYRot() * ((float)Math.PI / 180F)), (double)(-MathHelper.cos(this.getYRot() * ((float)Math.PI / 180F))));
                this.setMot(this.getMot().multiply(0.6D, 1.0D, 0.6D));
            }

            if (target instanceof EntityHuman) {
                EntityHuman player = (EntityHuman)target;
                this.maybeDisableShield(player, this.getItemInMainHand(), player.isHandRaised() ? player.getActiveItem() : ItemStack.EMPTY);
            }

            this.doEnchantDamageEffects(this, target);
            this.setLastHurtMob(target);
        }

        return bl;
    }

    private void maybeDisableShield(EntityHuman player, ItemStack mobStack, ItemStack playerStack) {
        if (!mobStack.isEmpty() && !playerStack.isEmpty() && mobStack.getItem() instanceof ItemAxe && playerStack.is(Items.SHIELD)) {
            float f = 0.25F + (float)EnchantmentManager.getDigSpeedEnchantmentLevel(this) * 0.05F;
            if (this.random.nextFloat() < f) {
                player.getCooldownTracker().setCooldown(Items.SHIELD, 100);
                this.level.broadcastEntityEffect(player, (byte)30);
            }
        }

    }

    public boolean isSunBurnTick() {
        if (this.level.isDay() && !this.level.isClientSide) {
            float f = this.getBrightness();
            BlockPosition blockPos = new BlockPosition(this.locX(), this.getHeadY(), this.locZ());
            boolean bl = this.isInWaterRainOrBubble() || this.isInPowderSnow || this.wasInPowderSnow;
            if (f > 0.5F && this.random.nextFloat() * 30.0F < (f - 0.4F) * 2.0F && !bl && this.level.canSeeSky(blockPos)) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected void jumpInLiquid(Tag<FluidType> fluid) {
        if (this.getNavigation().canFloat()) {
            super.jumpInLiquid(fluid);
        } else {
            this.setMot(this.getMot().add(0.0D, 0.3D, 0.0D));
        }

    }

    public void removeFreeWill() {
        this.goalSelector.removeAllGoals();
        this.getBehaviorController().removeAllBehaviors();
    }

    @Override
    protected void removeAfterChangingDimensions() {
        super.removeAfterChangingDimensions();
        this.unleash(true, false);
        this.getAllSlots().forEach((stack) -> {
            stack.setCount(0);
        });
    }

    @Nullable
    @Override
    public ItemStack getPickResult() {
        ItemMonsterEgg spawnEggItem = ItemMonsterEgg.byId(this.getEntityType());
        return spawnEggItem == null ? null : new ItemStack(spawnEggItem);
    }
}
