package net.minecraft.world.entity.animal.goat;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Dynamic;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.protocol.game.PacketDebug;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.TagsBlock;
import net.minecraft.util.MathHelper;
import net.minecraft.world.DifficultyDamageScaler;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityAgeable;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.EntitySize;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.entity.ai.BehaviorController;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.navigation.Navigation;
import net.minecraft.world.entity.ai.navigation.NavigationAbstract;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.animal.EntityAnimal;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemLiquidUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldAccess;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.Pathfinder;
import net.minecraft.world.level.pathfinder.PathfinderNormal;

public class EntityGoat extends EntityAnimal {
    public static final EntitySize LONG_JUMPING_DIMENSIONS = EntitySize.scalable(0.9F, 1.3F).scale(0.7F);
    private static final int ADULT_ATTACK_DAMAGE = 2;
    private static final int BABY_ATTACK_DAMAGE = 1;
    protected static final ImmutableList<SensorType<? extends Sensor<? super EntityGoat>>> SENSOR_TYPES = ImmutableList.of(SensorType.NEAREST_LIVING_ENTITIES, SensorType.NEAREST_PLAYERS, SensorType.NEAREST_ITEMS, SensorType.NEAREST_ADULT, SensorType.HURT_BY, SensorType.GOAT_TEMPTATIONS);
    protected static final ImmutableList<MemoryModuleType<?>> MEMORY_TYPES = ImmutableList.of(MemoryModuleType.LOOK_TARGET, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryModuleType.WALK_TARGET, MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, MemoryModuleType.PATH, MemoryModuleType.ATE_RECENTLY, MemoryModuleType.BREED_TARGET, MemoryModuleType.LONG_JUMP_COOLDOWN_TICKS, MemoryModuleType.LONG_JUMP_MID_JUMP, MemoryModuleType.TEMPTING_PLAYER, MemoryModuleType.NEAREST_VISIBLE_ADULT, MemoryModuleType.TEMPTATION_COOLDOWN_TICKS, MemoryModuleType.IS_TEMPTED, MemoryModuleType.RAM_COOLDOWN_TICKS, MemoryModuleType.RAM_TARGET);
    public static final int GOAT_FALL_DAMAGE_REDUCTION = 10;
    public static final double GOAT_SCREAMING_CHANCE = 0.02D;
    private static final DataWatcherObject<Boolean> DATA_IS_SCREAMING_GOAT = DataWatcher.defineId(EntityGoat.class, DataWatcherRegistry.BOOLEAN);
    private boolean isLoweringHead;
    private int lowerHeadTick;

    public EntityGoat(EntityTypes<? extends EntityGoat> type, World world) {
        super(type, world);
        this.getNavigation().setCanFloat(true);
    }

    @Override
    protected BehaviorController.Provider<EntityGoat> brainProvider() {
        return BehaviorController.provider(MEMORY_TYPES, SENSOR_TYPES);
    }

    @Override
    protected BehaviorController<?> makeBrain(Dynamic<?> dynamic) {
        return GoatAI.makeBrain(this.brainProvider().makeBrain(dynamic));
    }

    public static AttributeProvider.Builder createAttributes() {
        return EntityInsentient.createMobAttributes().add(GenericAttributes.MAX_HEALTH, 10.0D).add(GenericAttributes.MOVEMENT_SPEED, (double)0.2F).add(GenericAttributes.ATTACK_DAMAGE, 2.0D);
    }

    @Override
    protected void ageBoundaryReached() {
        if (this.isBaby()) {
            this.getAttributeInstance(GenericAttributes.ATTACK_DAMAGE).setValue(1.0D);
        } else {
            this.getAttributeInstance(GenericAttributes.ATTACK_DAMAGE).setValue(2.0D);
        }

    }

    @Override
    protected int calculateFallDamage(float fallDistance, float damageMultiplier) {
        return super.calculateFallDamage(fallDistance, damageMultiplier) - 10;
    }

    @Override
    protected SoundEffect getSoundAmbient() {
        return this.isScreamingGoat() ? SoundEffects.GOAT_SCREAMING_AMBIENT : SoundEffects.GOAT_AMBIENT;
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return this.isScreamingGoat() ? SoundEffects.GOAT_SCREAMING_HURT : SoundEffects.GOAT_HURT;
    }

    @Override
    public SoundEffect getSoundDeath() {
        return this.isScreamingGoat() ? SoundEffects.GOAT_SCREAMING_DEATH : SoundEffects.GOAT_DEATH;
    }

    @Override
    protected void playStepSound(BlockPosition pos, IBlockData state) {
        this.playSound(SoundEffects.GOAT_STEP, 0.15F, 1.0F);
    }

    protected SoundEffect getMilkingSound() {
        return this.isScreamingGoat() ? SoundEffects.GOAT_SCREAMING_MILK : SoundEffects.GOAT_MILK;
    }

    @Override
    public EntityGoat getBreedOffspring(WorldServer serverLevel, EntityAgeable ageableMob) {
        EntityGoat goat = EntityTypes.GOAT.create(serverLevel);
        if (goat != null) {
            GoatAI.initMemories(goat);
            boolean bl = ageableMob instanceof EntityGoat && ((EntityGoat)ageableMob).isScreamingGoat();
            goat.setScreamingGoat(bl || serverLevel.getRandom().nextDouble() < 0.02D);
        }

        return goat;
    }

    @Override
    public BehaviorController<EntityGoat> getBehaviorController() {
        return super.getBehaviorController();
    }

    @Override
    protected void mobTick() {
        this.level.getMethodProfiler().enter("goatBrain");
        this.getBehaviorController().tick((WorldServer)this.level, this);
        this.level.getMethodProfiler().exit();
        this.level.getMethodProfiler().enter("goatActivityUpdate");
        GoatAI.updateActivity(this);
        this.level.getMethodProfiler().exit();
        super.mobTick();
    }

    @Override
    public int getMaxHeadYRot() {
        return 15;
    }

    @Override
    public void setHeadRotation(float headYaw) {
        int i = this.getMaxHeadYRot();
        float f = MathHelper.degreesDifference(this.yBodyRot, headYaw);
        float g = MathHelper.clamp(f, (float)(-i), (float)i);
        super.setHeadRotation(this.yBodyRot + g);
    }

    @Override
    public SoundEffect getEatingSound(ItemStack stack) {
        return this.isScreamingGoat() ? SoundEffects.GOAT_SCREAMING_EAT : SoundEffects.GOAT_EAT;
    }

    @Override
    public EnumInteractionResult mobInteract(EntityHuman player, EnumHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (itemStack.is(Items.BUCKET) && !this.isBaby()) {
            player.playSound(this.getMilkingSound(), 1.0F, 1.0F);
            ItemStack itemStack2 = ItemLiquidUtil.createFilledResult(itemStack, player, Items.MILK_BUCKET.createItemStack());
            player.setItemInHand(hand, itemStack2);
            return EnumInteractionResult.sidedSuccess(this.level.isClientSide);
        } else {
            EnumInteractionResult interactionResult = super.mobInteract(player, hand);
            if (interactionResult.consumesAction() && this.isBreedItem(itemStack)) {
                this.level.playSound((EntityHuman)null, this, this.getEatingSound(itemStack), EnumSoundCategory.NEUTRAL, 1.0F, MathHelper.randomBetween(this.level.random, 0.8F, 1.2F));
            }

            return interactionResult;
        }
    }

    @Override
    public GroupDataEntity prepare(WorldAccess world, DifficultyDamageScaler difficulty, EnumMobSpawn spawnReason, @Nullable GroupDataEntity entityData, @Nullable NBTTagCompound entityNbt) {
        GoatAI.initMemories(this);
        this.setScreamingGoat(world.getRandom().nextDouble() < 0.02D);
        return super.prepare(world, difficulty, spawnReason, entityData, entityNbt);
    }

    @Override
    protected void sendDebugPackets() {
        super.sendDebugPackets();
        PacketDebug.sendEntityBrain(this);
    }

    @Override
    public EntitySize getDimensions(EntityPose pose) {
        return pose == EntityPose.LONG_JUMPING ? LONG_JUMPING_DIMENSIONS.scale(this.getScale()) : super.getDimensions(pose);
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        nbt.setBoolean("IsScreamingGoat", this.isScreamingGoat());
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        this.setScreamingGoat(nbt.getBoolean("IsScreamingGoat"));
    }

    @Override
    public void handleEntityEvent(byte status) {
        if (status == 58) {
            this.isLoweringHead = true;
        } else if (status == 59) {
            this.isLoweringHead = false;
        } else {
            super.handleEntityEvent(status);
        }

    }

    @Override
    public void movementTick() {
        if (this.isLoweringHead) {
            ++this.lowerHeadTick;
        } else {
            this.lowerHeadTick -= 2;
        }

        this.lowerHeadTick = MathHelper.clamp(this.lowerHeadTick, 0, 20);
        super.movementTick();
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.entityData.register(DATA_IS_SCREAMING_GOAT, false);
    }

    public boolean isScreamingGoat() {
        return this.entityData.get(DATA_IS_SCREAMING_GOAT);
    }

    public void setScreamingGoat(boolean screaming) {
        this.entityData.set(DATA_IS_SCREAMING_GOAT, screaming);
    }

    public float getRammingXHeadRot() {
        return (float)this.lowerHeadTick / 20.0F * 30.0F * ((float)Math.PI / 180F);
    }

    @Override
    protected NavigationAbstract createNavigation(World world) {
        return new EntityGoat.NavigationGoat(this, world);
    }

    public static boolean checkGoatSpawnRules(EntityTypes<? extends EntityAnimal> entityType, GeneratorAccess world, EnumMobSpawn spawnReason, BlockPosition pos, Random random) {
        return world.getType(pos.below()).is(TagsBlock.GOATS_SPAWNABLE_ON) && isBrightEnoughToSpawn(world, pos);
    }

    static class NavigationGoat extends Navigation {
        NavigationGoat(EntityGoat goat, World world) {
            super(goat, world);
        }

        @Override
        protected Pathfinder createPathFinder(int range) {
            this.nodeEvaluator = new EntityGoat.PathfinderGoat();
            return new Pathfinder(this.nodeEvaluator, range);
        }
    }

    static class PathfinderGoat extends PathfinderNormal {
        private final BlockPosition.MutableBlockPosition belowPos = new BlockPosition.MutableBlockPosition();

        @Override
        public PathType getBlockPathType(IBlockAccess world, int x, int y, int z) {
            this.belowPos.set(x, y - 1, z);
            PathType blockPathTypes = getBlockPathTypeRaw(world, this.belowPos);
            return blockPathTypes == PathType.POWDER_SNOW ? PathType.BLOCKED : getBlockPathTypeStatic(world, this.belowPos.move(EnumDirection.UP));
        }
    }
}
