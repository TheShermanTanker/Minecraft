package net.minecraft.world.entity.monster;

import java.util.Random;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.util.TimeRange;
import net.minecraft.util.valueproviders.IntProviderUniform;
import net.minecraft.world.DifficultyDamageScaler;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.IEntityAngerable;
import net.minecraft.world.entity.IEntitySelector;
import net.minecraft.world.entity.ai.attributes.AttributeModifiable;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomStrollLand;
import net.minecraft.world.entity.ai.goal.PathfinderGoalZombieAttack;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalHurtByTarget;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalNearestAttackableTarget;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalUniversalAngerReset;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AxisAlignedBB;

public class EntityPigZombie extends EntityZombie implements IEntityAngerable {
    private static final UUID SPEED_MODIFIER_ATTACKING_UUID = UUID.fromString("49455A49-7EC5-45BA-B886-3B90B23A1718");
    private static final AttributeModifier SPEED_MODIFIER_ATTACKING = new AttributeModifier(SPEED_MODIFIER_ATTACKING_UUID, "Attacking speed boost", 0.05D, AttributeModifier.Operation.ADDITION);
    private static final IntProviderUniform FIRST_ANGER_SOUND_DELAY = TimeRange.rangeOfSeconds(0, 1);
    private int playFirstAngerSoundIn;
    private static final IntProviderUniform PERSISTENT_ANGER_TIME = TimeRange.rangeOfSeconds(20, 39);
    private int remainingPersistentAngerTime;
    private UUID persistentAngerTarget;
    private static final int ALERT_RANGE_Y = 10;
    private static final IntProviderUniform ALERT_INTERVAL = TimeRange.rangeOfSeconds(4, 6);
    private int ticksUntilNextAlert;

    public EntityPigZombie(EntityTypes<? extends EntityPigZombie> type, World world) {
        super(type, world);
        this.setPathfindingMalus(PathType.LAVA, 8.0F);
    }

    @Override
    public void setAngerTarget(@Nullable UUID uuid) {
        this.persistentAngerTarget = uuid;
    }

    @Override
    public double getMyRidingOffset() {
        return this.isBaby() ? -0.05D : -0.45D;
    }

    @Override
    protected void addBehaviourGoals() {
        this.goalSelector.addGoal(2, new PathfinderGoalZombieAttack(this, 1.0D, false));
        this.goalSelector.addGoal(7, new PathfinderGoalRandomStrollLand(this, 1.0D));
        this.targetSelector.addGoal(1, (new PathfinderGoalHurtByTarget(this)).setAlertOthers());
        this.targetSelector.addGoal(2, new PathfinderGoalNearestAttackableTarget<>(this, EntityHuman.class, 10, true, false, this::isAngryAt));
        this.targetSelector.addGoal(3, new PathfinderGoalUniversalAngerReset<>(this, true));
    }

    public static AttributeProvider.Builder createAttributes() {
        return EntityZombie.createAttributes().add(GenericAttributes.SPAWN_REINFORCEMENTS_CHANCE, 0.0D).add(GenericAttributes.MOVEMENT_SPEED, (double)0.23F).add(GenericAttributes.ATTACK_DAMAGE, 5.0D);
    }

    @Override
    protected boolean convertsInWater() {
        return false;
    }

    @Override
    protected void mobTick() {
        AttributeModifiable attributeInstance = this.getAttributeInstance(GenericAttributes.MOVEMENT_SPEED);
        if (this.isAngry()) {
            if (!this.isBaby() && !attributeInstance.hasModifier(SPEED_MODIFIER_ATTACKING)) {
                attributeInstance.addTransientModifier(SPEED_MODIFIER_ATTACKING);
            }

            this.maybePlayFirstAngerSound();
        } else if (attributeInstance.hasModifier(SPEED_MODIFIER_ATTACKING)) {
            attributeInstance.removeModifier(SPEED_MODIFIER_ATTACKING);
        }

        this.updatePersistentAnger((WorldServer)this.level, true);
        if (this.getGoalTarget() != null) {
            this.maybeAlertOthers();
        }

        if (this.isAngry()) {
            this.lastHurtByPlayerTime = this.tickCount;
        }

        super.mobTick();
    }

    private void maybePlayFirstAngerSound() {
        if (this.playFirstAngerSoundIn > 0) {
            --this.playFirstAngerSoundIn;
            if (this.playFirstAngerSoundIn == 0) {
                this.playAngerSound();
            }
        }

    }

    private void maybeAlertOthers() {
        if (this.ticksUntilNextAlert > 0) {
            --this.ticksUntilNextAlert;
        } else {
            if (this.getEntitySenses().hasLineOfSight(this.getGoalTarget())) {
                this.alertOthers();
            }

            this.ticksUntilNextAlert = ALERT_INTERVAL.sample(this.random);
        }
    }

    private void alertOthers() {
        double d = this.getAttributeValue(GenericAttributes.FOLLOW_RANGE);
        AxisAlignedBB aABB = AxisAlignedBB.unitCubeFromLowerCorner(this.getPositionVector()).grow(d, 10.0D, d);
        this.level.getEntitiesOfClass(EntityPigZombie.class, aABB, IEntitySelector.NO_SPECTATORS).stream().filter((zombifiedPiglin) -> {
            return zombifiedPiglin != this;
        }).filter((zombifiedPiglin) -> {
            return zombifiedPiglin.getGoalTarget() == null;
        }).filter((zombifiedPiglin) -> {
            return !zombifiedPiglin.isAlliedTo(this.getGoalTarget());
        }).forEach((zombifiedPiglin) -> {
            zombifiedPiglin.setGoalTarget(this.getGoalTarget());
        });
    }

    private void playAngerSound() {
        this.playSound(SoundEffects.ZOMBIFIED_PIGLIN_ANGRY, this.getSoundVolume() * 2.0F, this.getVoicePitch() * 1.8F);
    }

    @Override
    public void setGoalTarget(@Nullable EntityLiving target) {
        if (this.getGoalTarget() == null && target != null) {
            this.playFirstAngerSoundIn = FIRST_ANGER_SOUND_DELAY.sample(this.random);
            this.ticksUntilNextAlert = ALERT_INTERVAL.sample(this.random);
        }

        if (target instanceof EntityHuman) {
            this.setLastHurtByPlayer((EntityHuman)target);
        }

        super.setGoalTarget(target);
    }

    @Override
    public void anger() {
        this.setAnger(PERSISTENT_ANGER_TIME.sample(this.random));
    }

    public static boolean checkZombifiedPiglinSpawnRules(EntityTypes<EntityPigZombie> type, GeneratorAccess world, EnumMobSpawn spawnReason, BlockPosition pos, Random random) {
        return world.getDifficulty() != EnumDifficulty.PEACEFUL && !world.getType(pos.below()).is(Blocks.NETHER_WART_BLOCK);
    }

    @Override
    public boolean checkSpawnObstruction(IWorldReader world) {
        return world.isUnobstructed(this) && !world.containsLiquid(this.getBoundingBox());
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        this.addPersistentAngerSaveData(nbt);
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        this.readPersistentAngerSaveData(this.level, nbt);
    }

    @Override
    public void setAnger(int ticks) {
        this.remainingPersistentAngerTime = ticks;
    }

    @Override
    public int getAnger() {
        return this.remainingPersistentAngerTime;
    }

    @Override
    protected SoundEffect getSoundAmbient() {
        return this.isAngry() ? SoundEffects.ZOMBIFIED_PIGLIN_ANGRY : SoundEffects.ZOMBIFIED_PIGLIN_AMBIENT;
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return SoundEffects.ZOMBIFIED_PIGLIN_HURT;
    }

    @Override
    public SoundEffect getSoundDeath() {
        return SoundEffects.ZOMBIFIED_PIGLIN_DEATH;
    }

    @Override
    protected void populateDefaultEquipmentSlots(DifficultyDamageScaler difficulty) {
        this.setSlot(EnumItemSlot.MAINHAND, new ItemStack(Items.GOLDEN_SWORD));
    }

    @Override
    protected ItemStack getSkull() {
        return ItemStack.EMPTY;
    }

    @Override
    protected void randomizeReinforcementsChance() {
        this.getAttributeInstance(GenericAttributes.SPAWN_REINFORCEMENTS_CHANCE).setValue(0.0D);
    }

    @Override
    public UUID getAngerTarget() {
        return this.persistentAngerTarget;
    }

    @Override
    public boolean isPreventingPlayerRest(EntityHuman player) {
        return this.isAngryAt(player);
    }

    @Override
    public boolean wantsToPickUp(ItemStack stack) {
        return this.canPickup(stack);
    }
}
