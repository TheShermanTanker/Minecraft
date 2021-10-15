package net.minecraft.world.entity.monster;

import com.google.common.collect.Maps;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.DifficultyDamageScaler;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.IInventory;
import net.minecraft.world.InventorySubcontainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.EnumMonsterType;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.goal.PathfinderGoalCrossbowAttack;
import net.minecraft.world.entity.ai.goal.PathfinderGoalFloat;
import net.minecraft.world.entity.ai.goal.PathfinderGoalLookAtPlayer;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomStroll;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalHurtByTarget;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalNearestAttackableTarget;
import net.minecraft.world.entity.animal.EntityIronGolem;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.npc.EntityVillagerAbstract;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.IProjectile;
import net.minecraft.world.entity.raid.EntityRaider;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.item.ItemBanner;
import net.minecraft.world.item.ItemProjectileWeapon;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentManager;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldAccess;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;

public class EntityPillager extends EntityIllagerAbstract implements ICrossbow, InventoryCarrier {
    private static final DataWatcherObject<Boolean> IS_CHARGING_CROSSBOW = DataWatcher.defineId(EntityPillager.class, DataWatcherRegistry.BOOLEAN);
    private static final int INVENTORY_SIZE = 5;
    private static final int SLOT_OFFSET = 300;
    private static final float CROSSBOW_POWER = 1.6F;
    public final InventorySubcontainer inventory = new InventorySubcontainer(5);

    public EntityPillager(EntityTypes<? extends EntityPillager> type, World world) {
        super(type, world);
    }

    @Override
    protected void initPathfinder() {
        super.initPathfinder();
        this.goalSelector.addGoal(0, new PathfinderGoalFloat(this));
        this.goalSelector.addGoal(2, new EntityRaider.HoldGroundAttackGoal(this, 10.0F));
        this.goalSelector.addGoal(3, new PathfinderGoalCrossbowAttack<>(this, 1.0D, 8.0F));
        this.goalSelector.addGoal(8, new PathfinderGoalRandomStroll(this, 0.6D));
        this.goalSelector.addGoal(9, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 15.0F, 1.0F));
        this.goalSelector.addGoal(10, new PathfinderGoalLookAtPlayer(this, EntityInsentient.class, 15.0F));
        this.targetSelector.addGoal(1, (new PathfinderGoalHurtByTarget(this, EntityRaider.class)).setAlertOthers());
        this.targetSelector.addGoal(2, new PathfinderGoalNearestAttackableTarget<>(this, EntityHuman.class, true));
        this.targetSelector.addGoal(3, new PathfinderGoalNearestAttackableTarget<>(this, EntityVillagerAbstract.class, false));
        this.targetSelector.addGoal(3, new PathfinderGoalNearestAttackableTarget<>(this, EntityIronGolem.class, true));
    }

    public static AttributeProvider.Builder createAttributes() {
        return EntityMonster.createMonsterAttributes().add(GenericAttributes.MOVEMENT_SPEED, (double)0.35F).add(GenericAttributes.MAX_HEALTH, 24.0D).add(GenericAttributes.ATTACK_DAMAGE, 5.0D).add(GenericAttributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.entityData.register(IS_CHARGING_CROSSBOW, false);
    }

    @Override
    public boolean canFireProjectileWeapon(ItemProjectileWeapon weapon) {
        return weapon == Items.CROSSBOW;
    }

    public boolean isChargingCrossbow() {
        return this.entityData.get(IS_CHARGING_CROSSBOW);
    }

    @Override
    public void setChargingCrossbow(boolean charging) {
        this.entityData.set(IS_CHARGING_CROSSBOW, charging);
    }

    @Override
    public void onCrossbowAttackPerformed() {
        this.noActionTime = 0;
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        NBTTagList listTag = new NBTTagList();

        for(int i = 0; i < this.inventory.getSize(); ++i) {
            ItemStack itemStack = this.inventory.getItem(i);
            if (!itemStack.isEmpty()) {
                listTag.add(itemStack.save(new NBTTagCompound()));
            }
        }

        nbt.set("Inventory", listTag);
    }

    @Override
    public EntityIllagerAbstract.IllagerArmPose getArmPose() {
        if (this.isChargingCrossbow()) {
            return EntityIllagerAbstract.IllagerArmPose.CROSSBOW_CHARGE;
        } else if (this.isHolding(Items.CROSSBOW)) {
            return EntityIllagerAbstract.IllagerArmPose.CROSSBOW_HOLD;
        } else {
            return this.isAggressive() ? EntityIllagerAbstract.IllagerArmPose.ATTACKING : EntityIllagerAbstract.IllagerArmPose.NEUTRAL;
        }
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        NBTTagList listTag = nbt.getList("Inventory", 10);

        for(int i = 0; i < listTag.size(); ++i) {
            ItemStack itemStack = ItemStack.of(listTag.getCompound(i));
            if (!itemStack.isEmpty()) {
                this.inventory.addItem(itemStack);
            }
        }

        this.setCanPickupLoot(true);
    }

    @Override
    public float getWalkTargetValue(BlockPosition pos, IWorldReader world) {
        IBlockData blockState = world.getType(pos.below());
        return !blockState.is(Blocks.GRASS_BLOCK) && !blockState.is(Blocks.SAND) ? 0.5F - world.getBrightness(pos) : 10.0F;
    }

    @Override
    public int getMaxSpawnGroup() {
        return 1;
    }

    @Nullable
    @Override
    public GroupDataEntity prepare(WorldAccess world, DifficultyDamageScaler difficulty, EnumMobSpawn spawnReason, @Nullable GroupDataEntity entityData, @Nullable NBTTagCompound entityNbt) {
        this.populateDefaultEquipmentSlots(difficulty);
        this.populateDefaultEquipmentEnchantments(difficulty);
        return super.prepare(world, difficulty, spawnReason, entityData, entityNbt);
    }

    @Override
    protected void populateDefaultEquipmentSlots(DifficultyDamageScaler difficulty) {
        this.setSlot(EnumItemSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
    }

    @Override
    protected void enchantSpawnedWeapon(float power) {
        super.enchantSpawnedWeapon(power);
        if (this.random.nextInt(300) == 0) {
            ItemStack itemStack = this.getItemInMainHand();
            if (itemStack.is(Items.CROSSBOW)) {
                Map<Enchantment, Integer> map = EnchantmentManager.getEnchantments(itemStack);
                map.putIfAbsent(Enchantments.PIERCING, 1);
                EnchantmentManager.setEnchantments(map, itemStack);
                this.setSlot(EnumItemSlot.MAINHAND, itemStack);
            }
        }

    }

    @Override
    public boolean isAlliedTo(Entity other) {
        if (super.isAlliedTo(other)) {
            return true;
        } else if (other instanceof EntityLiving && ((EntityLiving)other).getMonsterType() == EnumMonsterType.ILLAGER) {
            return this.getScoreboardTeam() == null && other.getScoreboardTeam() == null;
        } else {
            return false;
        }
    }

    @Override
    protected SoundEffect getSoundAmbient() {
        return SoundEffects.PILLAGER_AMBIENT;
    }

    @Override
    public SoundEffect getSoundDeath() {
        return SoundEffects.PILLAGER_DEATH;
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return SoundEffects.PILLAGER_HURT;
    }

    @Override
    public void performRangedAttack(EntityLiving target, float pullProgress) {
        this.performCrossbowAttack(this, 1.6F);
    }

    @Override
    public void shootCrossbowProjectile(EntityLiving target, ItemStack crossbow, IProjectile projectile, float multiShotSpray) {
        this.shootCrossbowProjectile(this, target, projectile, multiShotSpray, 1.6F);
    }

    @Override
    public IInventory getInventory() {
        return this.inventory;
    }

    @Override
    protected void pickUpItem(EntityItem item) {
        ItemStack itemStack = item.getItemStack();
        if (itemStack.getItem() instanceof ItemBanner) {
            super.pickUpItem(item);
        } else if (this.wantsItem(itemStack)) {
            this.onItemPickup(item);
            ItemStack itemStack2 = this.inventory.addItem(itemStack);
            if (itemStack2.isEmpty()) {
                item.die();
            } else {
                itemStack.setCount(itemStack2.getCount());
            }
        }

    }

    private boolean wantsItem(ItemStack stack) {
        return this.hasActiveRaid() && stack.is(Items.WHITE_BANNER);
    }

    @Override
    public SlotAccess getSlot(int mappedIndex) {
        int i = mappedIndex - 300;
        return i >= 0 && i < this.inventory.getSize() ? SlotAccess.forContainer(this.inventory, i) : super.getSlot(mappedIndex);
    }

    @Override
    public void applyRaidBuffs(int wave, boolean unused) {
        Raid raid = this.getCurrentRaid();
        boolean bl = this.random.nextFloat() <= raid.getEnchantOdds();
        if (bl) {
            ItemStack itemStack = new ItemStack(Items.CROSSBOW);
            Map<Enchantment, Integer> map = Maps.newHashMap();
            if (wave > raid.getNumGroups(EnumDifficulty.NORMAL)) {
                map.put(Enchantments.QUICK_CHARGE, 2);
            } else if (wave > raid.getNumGroups(EnumDifficulty.EASY)) {
                map.put(Enchantments.QUICK_CHARGE, 1);
            }

            map.put(Enchantments.MULTISHOT, 1);
            EnchantmentManager.setEnchantments(map, itemStack);
            this.setSlot(EnumItemSlot.MAINHAND, itemStack);
        }

    }

    @Override
    public SoundEffect getCelebrateSound() {
        return SoundEffects.PILLAGER_CELEBRATE;
    }
}
