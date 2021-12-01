package net.minecraft.world.entity.npc;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.IRegistry;
import net.minecraft.core.particles.Particles;
import net.minecraft.nbt.DynamicOpsNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.game.PacketDebug;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.stats.StatisticList;
import net.minecraft.util.MathHelper;
import net.minecraft.world.DifficultyDamageScaler;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.InventorySubcontainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAgeable;
import net.minecraft.world.entity.EntityExperienceOrb;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLightning;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.entity.ReputationHandler;
import net.minecraft.world.entity.ai.BehaviorController;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.behavior.Behaviors;
import net.minecraft.world.entity.ai.gossip.Reputation;
import net.minecraft.world.entity.ai.gossip.ReputationType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.ai.navigation.Navigation;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorGolemLastSeen;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.ai.village.ReputationEvent;
import net.minecraft.world.entity.ai.village.poi.VillagePlace;
import net.minecraft.world.entity.ai.village.poi.VillagePlaceType;
import net.minecraft.world.entity.animal.EntityIronGolem;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.monster.EntityWitch;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.schedule.Schedule;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantRecipe;
import net.minecraft.world.item.trading.MerchantRecipeList;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldAccess;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.AxisAlignedBB;

public class EntityVillager extends EntityVillagerAbstract implements ReputationHandler, VillagerDataHolder {
    private static final DataWatcherObject<VillagerData> DATA_VILLAGER_DATA = DataWatcher.defineId(EntityVillager.class, DataWatcherRegistry.VILLAGER_DATA);
    public static final int BREEDING_FOOD_THRESHOLD = 12;
    public static final Map<Item, Integer> FOOD_POINTS = ImmutableMap.of(Items.BREAD, 4, Items.POTATO, 1, Items.CARROT, 1, Items.BEETROOT, 1);
    private static final int TRADES_PER_LEVEL = 2;
    private static final Set<Item> WANTED_ITEMS = ImmutableSet.of(Items.BREAD, Items.POTATO, Items.CARROT, Items.WHEAT, Items.WHEAT_SEEDS, Items.BEETROOT, Items.BEETROOT_SEEDS);
    private static final int MAX_GOSSIP_TOPICS = 10;
    private static final int GOSSIP_COOLDOWN = 1200;
    private static final int GOSSIP_DECAY_INTERVAL = 24000;
    private static final int REPUTATION_CHANGE_PER_EVENT = 25;
    private static final int HOW_FAR_AWAY_TO_TALK_TO_OTHER_VILLAGERS_ABOUT_GOLEMS = 10;
    private static final int HOW_MANY_VILLAGERS_NEED_TO_AGREE_TO_SPAWN_A_GOLEM = 5;
    private static final long TIME_SINCE_SLEEPING_FOR_GOLEM_SPAWNING = 24000L;
    @VisibleForTesting
    public static final float SPEED_MODIFIER = 0.5F;
    private int updateMerchantTimer;
    private boolean increaseProfessionLevelOnUpdate;
    @Nullable
    private EntityHuman lastTradedPlayer;
    private boolean chasing;
    private byte foodLevel;
    private final Reputation gossips = new Reputation();
    private long lastGossipTime;
    private long lastGossipDecayTime;
    private int villagerXp;
    private long lastRestockGameTime;
    public int numberOfRestocksToday;
    private long lastRestockCheckDayTime;
    private boolean assignProfessionWhenSpawned;
    private static final ImmutableList<MemoryModuleType<?>> MEMORY_TYPES = ImmutableList.of(MemoryModuleType.HOME, MemoryModuleType.JOB_SITE, MemoryModuleType.POTENTIAL_JOB_SITE, MemoryModuleType.MEETING_POINT, MemoryModuleType.NEAREST_LIVING_ENTITIES, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryModuleType.VISIBLE_VILLAGER_BABIES, MemoryModuleType.NEAREST_PLAYERS, MemoryModuleType.NEAREST_VISIBLE_PLAYER, MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER, MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM, MemoryModuleType.WALK_TARGET, MemoryModuleType.LOOK_TARGET, MemoryModuleType.INTERACTION_TARGET, MemoryModuleType.BREED_TARGET, MemoryModuleType.PATH, MemoryModuleType.DOORS_TO_CLOSE, MemoryModuleType.NEAREST_BED, MemoryModuleType.HURT_BY, MemoryModuleType.HURT_BY_ENTITY, MemoryModuleType.NEAREST_HOSTILE, MemoryModuleType.SECONDARY_JOB_SITE, MemoryModuleType.HIDING_PLACE, MemoryModuleType.HEARD_BELL_TIME, MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, MemoryModuleType.LAST_SLEPT, MemoryModuleType.LAST_WOKEN, MemoryModuleType.LAST_WORKED_AT_POI, MemoryModuleType.GOLEM_DETECTED_RECENTLY);
    private static final ImmutableList<SensorType<? extends Sensor<? super EntityVillager>>> SENSOR_TYPES = ImmutableList.of(SensorType.NEAREST_LIVING_ENTITIES, SensorType.NEAREST_PLAYERS, SensorType.NEAREST_ITEMS, SensorType.NEAREST_BED, SensorType.HURT_BY, SensorType.VILLAGER_HOSTILES, SensorType.VILLAGER_BABIES, SensorType.SECONDARY_POIS, SensorType.GOLEM_DETECTED);
    public static final Map<MemoryModuleType<GlobalPos>, BiPredicate<EntityVillager, VillagePlaceType>> POI_MEMORIES = ImmutableMap.of(MemoryModuleType.HOME, (villager, poiType) -> {
        return poiType == VillagePlaceType.HOME;
    }, MemoryModuleType.JOB_SITE, (villager, poiType) -> {
        return villager.getVillagerData().getProfession().getJobPoiType() == poiType;
    }, MemoryModuleType.POTENTIAL_JOB_SITE, (villager, poiType) -> {
        return VillagePlaceType.ALL_JOBS.test(poiType);
    }, MemoryModuleType.MEETING_POINT, (villager, poiType) -> {
        return poiType == VillagePlaceType.MEETING;
    });

    public EntityVillager(EntityTypes<? extends EntityVillager> entityType, World world) {
        this(entityType, world, VillagerType.PLAINS);
    }

    public EntityVillager(EntityTypes<? extends EntityVillager> entityType, World world, VillagerType type) {
        super(entityType, world);
        ((Navigation)this.getNavigation()).setCanOpenDoors(true);
        this.getNavigation().setCanFloat(true);
        this.setCanPickupLoot(true);
        this.setVillagerData(this.getVillagerData().withType(type).withProfession(VillagerProfession.NONE));
    }

    @Override
    public BehaviorController<EntityVillager> getBehaviorController() {
        return super.getBehaviorController();
    }

    @Override
    protected BehaviorController.Provider<EntityVillager> brainProvider() {
        return BehaviorController.provider(MEMORY_TYPES, SENSOR_TYPES);
    }

    @Override
    protected BehaviorController<?> makeBrain(Dynamic<?> dynamic) {
        BehaviorController<EntityVillager> brain = this.brainProvider().makeBrain(dynamic);
        this.registerBrainGoals(brain);
        return brain;
    }

    public void refreshBrain(WorldServer world) {
        BehaviorController<EntityVillager> brain = this.getBehaviorController();
        brain.stopAll(world, this);
        this.brain = brain.copyWithoutBehaviors();
        this.registerBrainGoals(this.getBehaviorController());
    }

    private void registerBrainGoals(BehaviorController<EntityVillager> brain) {
        VillagerProfession villagerProfession = this.getVillagerData().getProfession();
        if (this.isBaby()) {
            brain.setSchedule(Schedule.VILLAGER_BABY);
            brain.addActivity(Activity.PLAY, Behaviors.getPlayPackage(0.5F));
        } else {
            brain.setSchedule(Schedule.VILLAGER_DEFAULT);
            brain.addActivityWithConditions(Activity.WORK, Behaviors.getWorkPackage(villagerProfession, 0.5F), ImmutableSet.of(Pair.of(MemoryModuleType.JOB_SITE, MemoryStatus.VALUE_PRESENT)));
        }

        brain.addActivity(Activity.CORE, Behaviors.getCorePackage(villagerProfession, 0.5F));
        brain.addActivityWithConditions(Activity.MEET, Behaviors.getMeetPackage(villagerProfession, 0.5F), ImmutableSet.of(Pair.of(MemoryModuleType.MEETING_POINT, MemoryStatus.VALUE_PRESENT)));
        brain.addActivity(Activity.REST, Behaviors.getRestPackage(villagerProfession, 0.5F));
        brain.addActivity(Activity.IDLE, Behaviors.getIdlePackage(villagerProfession, 0.5F));
        brain.addActivity(Activity.PANIC, Behaviors.getPanicPackage(villagerProfession, 0.5F));
        brain.addActivity(Activity.PRE_RAID, Behaviors.getPreRaidPackage(villagerProfession, 0.5F));
        brain.addActivity(Activity.RAID, Behaviors.getRaidPackage(villagerProfession, 0.5F));
        brain.addActivity(Activity.HIDE, Behaviors.getHidePackage(villagerProfession, 0.5F));
        brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.setActiveActivityIfPossible(Activity.IDLE);
        brain.updateActivityFromSchedule(this.level.getDayTime(), this.level.getTime());
    }

    @Override
    protected void ageBoundaryReached() {
        super.ageBoundaryReached();
        if (this.level instanceof WorldServer) {
            this.refreshBrain((WorldServer)this.level);
        }

    }

    public static AttributeProvider.Builder createAttributes() {
        return EntityInsentient.createMobAttributes().add(GenericAttributes.MOVEMENT_SPEED, 0.5D).add(GenericAttributes.FOLLOW_RANGE, 48.0D);
    }

    public boolean assignProfessionWhenSpawned() {
        return this.assignProfessionWhenSpawned;
    }

    @Override
    protected void mobTick() {
        this.level.getMethodProfiler().enter("villagerBrain");
        this.getBehaviorController().tick((WorldServer)this.level, this);
        this.level.getMethodProfiler().exit();
        if (this.assignProfessionWhenSpawned) {
            this.assignProfessionWhenSpawned = false;
        }

        if (!this.isTrading() && this.updateMerchantTimer > 0) {
            --this.updateMerchantTimer;
            if (this.updateMerchantTimer <= 0) {
                if (this.increaseProfessionLevelOnUpdate) {
                    this.populateTrades();
                    this.increaseProfessionLevelOnUpdate = false;
                }

                this.addEffect(new MobEffect(MobEffectList.REGENERATION, 200, 0));
            }
        }

        if (this.lastTradedPlayer != null && this.level instanceof WorldServer) {
            ((WorldServer)this.level).onReputationEvent(ReputationEvent.TRADE, this.lastTradedPlayer, this);
            this.level.broadcastEntityEffect(this, (byte)14);
            this.lastTradedPlayer = null;
        }

        if (!this.isNoAI() && this.random.nextInt(100) == 0) {
            Raid raid = ((WorldServer)this.level).getRaidAt(this.getChunkCoordinates());
            if (raid != null && raid.isActive() && !raid.isOver()) {
                this.level.broadcastEntityEffect(this, (byte)42);
            }
        }

        if (this.getVillagerData().getProfession() == VillagerProfession.NONE && this.isTrading()) {
            this.stopTrading();
        }

        super.mobTick();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getUnhappyCounter() > 0) {
            this.setUnhappyCounter(this.getUnhappyCounter() - 1);
        }

        this.maybeDecayGossip();
    }

    @Override
    public EnumInteractionResult mobInteract(EntityHuman player, EnumHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (!itemStack.is(Items.VILLAGER_SPAWN_EGG) && this.isAlive() && !this.isTrading() && !this.isSleeping()) {
            if (this.isBaby()) {
                this.shakeHead();
                return EnumInteractionResult.sidedSuccess(this.level.isClientSide);
            } else {
                boolean bl = this.getOffers().isEmpty();
                if (hand == EnumHand.MAIN_HAND) {
                    if (bl && !this.level.isClientSide) {
                        this.shakeHead();
                    }

                    player.awardStat(StatisticList.TALKED_TO_VILLAGER);
                }

                if (bl) {
                    return EnumInteractionResult.sidedSuccess(this.level.isClientSide);
                } else {
                    if (!this.level.isClientSide && !this.offers.isEmpty()) {
                        this.startTrading(player);
                    }

                    return EnumInteractionResult.sidedSuccess(this.level.isClientSide);
                }
            }
        } else {
            return super.mobInteract(player, hand);
        }
    }

    public void shakeHead() {
        this.setUnhappyCounter(40);
        if (!this.level.isClientSide()) {
            this.playSound(SoundEffects.VILLAGER_NO, this.getSoundVolume(), this.getVoicePitch());
        }

    }

    private void startTrading(EntityHuman customer) {
        this.updateSpecialPrices(customer);
        this.setTradingPlayer(customer);
        this.openTrade(customer, this.getScoreboardDisplayName(), this.getVillagerData().getLevel());
    }

    @Override
    public void setTradingPlayer(@Nullable EntityHuman customer) {
        boolean bl = this.getTrader() != null && customer == null;
        super.setTradingPlayer(customer);
        if (bl) {
            this.stopTrading();
        }

    }

    @Override
    protected void stopTrading() {
        super.stopTrading();
        this.resetSpecialPrices();
    }

    private void resetSpecialPrices() {
        for(MerchantRecipe merchantOffer : this.getOffers()) {
            merchantOffer.setSpecialPrice();
        }

    }

    @Override
    public boolean canRestock() {
        return true;
    }

    @Override
    public boolean isClientSide() {
        return this.getLevel().isClientSide;
    }

    public void restock() {
        this.updateDemand();

        for(MerchantRecipe merchantOffer : this.getOffers()) {
            merchantOffer.resetUses();
        }

        this.lastRestockGameTime = this.level.getTime();
        ++this.numberOfRestocksToday;
    }

    private boolean needsToRestock() {
        for(MerchantRecipe merchantOffer : this.getOffers()) {
            if (merchantOffer.needsRestock()) {
                return true;
            }
        }

        return false;
    }

    private boolean allowedToRestock() {
        return this.numberOfRestocksToday == 0 || this.numberOfRestocksToday < 2 && this.level.getTime() > this.lastRestockGameTime + 2400L;
    }

    public boolean shouldRestock() {
        long l = this.lastRestockGameTime + 12000L;
        long m = this.level.getTime();
        boolean bl = m > l;
        long n = this.level.getDayTime();
        if (this.lastRestockCheckDayTime > 0L) {
            long o = this.lastRestockCheckDayTime / 24000L;
            long p = n / 24000L;
            bl |= p > o;
        }

        this.lastRestockCheckDayTime = n;
        if (bl) {
            this.lastRestockGameTime = m;
            this.resetNumberOfRestocks();
        }

        return this.allowedToRestock() && this.needsToRestock();
    }

    private void catchUpDemand() {
        int i = 2 - this.numberOfRestocksToday;
        if (i > 0) {
            for(MerchantRecipe merchantOffer : this.getOffers()) {
                merchantOffer.resetUses();
            }
        }

        for(int j = 0; j < i; ++j) {
            this.updateDemand();
        }

    }

    private void updateDemand() {
        for(MerchantRecipe merchantOffer : this.getOffers()) {
            merchantOffer.updateDemand();
        }

    }

    private void updateSpecialPrices(EntityHuman player) {
        int i = this.getPlayerReputation(player);
        if (i != 0) {
            for(MerchantRecipe merchantOffer : this.getOffers()) {
                merchantOffer.increaseSpecialPrice(-MathHelper.floor((float)i * merchantOffer.getPriceMultiplier()));
            }
        }

        if (player.hasEffect(MobEffectList.HERO_OF_THE_VILLAGE)) {
            MobEffect mobEffectInstance = player.getEffect(MobEffectList.HERO_OF_THE_VILLAGE);
            int j = mobEffectInstance.getAmplifier();

            for(MerchantRecipe merchantOffer2 : this.getOffers()) {
                double d = 0.3D + 0.0625D * (double)j;
                int k = (int)Math.floor(d * (double)merchantOffer2.getBaseCostA().getCount());
                merchantOffer2.increaseSpecialPrice(-Math.max(k, 1));
            }
        }

    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.entityData.register(DATA_VILLAGER_DATA, new VillagerData(VillagerType.PLAINS, VillagerProfession.NONE, 1));
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        VillagerData.CODEC.encodeStart(DynamicOpsNBT.INSTANCE, this.getVillagerData()).resultOrPartial(LOGGER::error).ifPresent((tag) -> {
            nbt.set("VillagerData", tag);
        });
        nbt.setByte("FoodLevel", this.foodLevel);
        nbt.set("Gossips", this.gossips.store(DynamicOpsNBT.INSTANCE).getValue());
        nbt.setInt("Xp", this.villagerXp);
        nbt.setLong("LastRestock", this.lastRestockGameTime);
        nbt.setLong("LastGossipDecay", this.lastGossipDecayTime);
        nbt.setInt("RestocksToday", this.numberOfRestocksToday);
        if (this.assignProfessionWhenSpawned) {
            nbt.setBoolean("AssignProfessionWhenSpawned", true);
        }

    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        if (nbt.hasKeyOfType("VillagerData", 10)) {
            DataResult<VillagerData> dataResult = VillagerData.CODEC.parse(new Dynamic<>(DynamicOpsNBT.INSTANCE, nbt.get("VillagerData")));
            dataResult.resultOrPartial(LOGGER::error).ifPresent(this::setVillagerData);
        }

        if (nbt.hasKeyOfType("Offers", 10)) {
            this.offers = new MerchantRecipeList(nbt.getCompound("Offers"));
        }

        if (nbt.hasKeyOfType("FoodLevel", 1)) {
            this.foodLevel = nbt.getByte("FoodLevel");
        }

        NBTTagList listTag = nbt.getList("Gossips", 10);
        this.gossips.update(new Dynamic<>(DynamicOpsNBT.INSTANCE, listTag));
        if (nbt.hasKeyOfType("Xp", 3)) {
            this.villagerXp = nbt.getInt("Xp");
        }

        this.lastRestockGameTime = nbt.getLong("LastRestock");
        this.lastGossipDecayTime = nbt.getLong("LastGossipDecay");
        this.setCanPickupLoot(true);
        if (this.level instanceof WorldServer) {
            this.refreshBrain((WorldServer)this.level);
        }

        this.numberOfRestocksToday = nbt.getInt("RestocksToday");
        if (nbt.hasKey("AssignProfessionWhenSpawned")) {
            this.assignProfessionWhenSpawned = nbt.getBoolean("AssignProfessionWhenSpawned");
        }

    }

    @Override
    public boolean isTypeNotPersistent(double distanceSquared) {
        return false;
    }

    @Nullable
    @Override
    protected SoundEffect getSoundAmbient() {
        if (this.isSleeping()) {
            return null;
        } else {
            return this.isTrading() ? SoundEffects.VILLAGER_TRADE : SoundEffects.VILLAGER_AMBIENT;
        }
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return SoundEffects.VILLAGER_HURT;
    }

    @Override
    public SoundEffect getSoundDeath() {
        return SoundEffects.VILLAGER_DEATH;
    }

    public void playWorkSound() {
        SoundEffect soundEvent = this.getVillagerData().getProfession().getWorkSound();
        if (soundEvent != null) {
            this.playSound(soundEvent, this.getSoundVolume(), this.getVoicePitch());
        }

    }

    @Override
    public void setVillagerData(VillagerData villagerData) {
        VillagerData villagerData2 = this.getVillagerData();
        if (villagerData2.getProfession() != villagerData.getProfession()) {
            this.offers = null;
        }

        this.entityData.set(DATA_VILLAGER_DATA, villagerData);
    }

    @Override
    public VillagerData getVillagerData() {
        return this.entityData.get(DATA_VILLAGER_DATA);
    }

    @Override
    protected void rewardTradeXp(MerchantRecipe offer) {
        int i = 3 + this.random.nextInt(4);
        this.villagerXp += offer.getXp();
        this.lastTradedPlayer = this.getTrader();
        if (this.shouldIncreaseLevel()) {
            this.updateMerchantTimer = 40;
            this.increaseProfessionLevelOnUpdate = true;
            i += 5;
        }

        if (offer.isRewardExp()) {
            this.level.addEntity(new EntityExperienceOrb(this.level, this.locX(), this.locY() + 0.5D, this.locZ(), i));
        }

    }

    public void setChasing(boolean bl) {
        this.chasing = bl;
    }

    public boolean isChasing() {
        return this.chasing;
    }

    @Override
    public void setLastDamager(@Nullable EntityLiving attacker) {
        if (attacker != null && this.level instanceof WorldServer) {
            ((WorldServer)this.level).onReputationEvent(ReputationEvent.VILLAGER_HURT, attacker, this);
            if (this.isAlive() && attacker instanceof EntityHuman) {
                this.level.broadcastEntityEffect(this, (byte)13);
            }
        }

        super.setLastDamager(attacker);
    }

    @Override
    public void die(DamageSource source) {
        LOGGER.info("Villager {} died, message: '{}'", this, source.getLocalizedDeathMessage(this).getString());
        Entity entity = source.getEntity();
        if (entity != null) {
            this.tellWitnessesThatIWasMurdered(entity);
        }

        this.releaseAllPois();
        super.die(source);
    }

    private void releaseAllPois() {
        this.releasePoi(MemoryModuleType.HOME);
        this.releasePoi(MemoryModuleType.JOB_SITE);
        this.releasePoi(MemoryModuleType.POTENTIAL_JOB_SITE);
        this.releasePoi(MemoryModuleType.MEETING_POINT);
    }

    private void tellWitnessesThatIWasMurdered(Entity killer) {
        World optional = this.level;
        if (optional instanceof WorldServer) {
            WorldServer serverLevel = (WorldServer)optional;
            Optional<NearestVisibleLivingEntities> optional = this.brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
            if (!optional.isEmpty()) {
                optional.get().findAll(ReputationHandler.class::isInstance).forEach((livingEntity) -> {
                    serverLevel.onReputationEvent(ReputationEvent.VILLAGER_KILLED, killer, (ReputationHandler)livingEntity);
                });
            }
        }
    }

    public void releasePoi(MemoryModuleType<GlobalPos> memoryModuleType) {
        if (this.level instanceof WorldServer) {
            MinecraftServer minecraftServer = ((WorldServer)this.level).getMinecraftServer();
            this.brain.getMemory(memoryModuleType).ifPresent((pos) -> {
                WorldServer serverLevel = minecraftServer.getWorldServer(pos.getDimensionManager());
                if (serverLevel != null) {
                    VillagePlace poiManager = serverLevel.getPoiManager();
                    Optional<VillagePlaceType> optional = poiManager.getType(pos.getBlockPosition());
                    BiPredicate<EntityVillager, VillagePlaceType> biPredicate = POI_MEMORIES.get(memoryModuleType);
                    if (optional.isPresent() && biPredicate.test(this, optional.get())) {
                        poiManager.release(pos.getBlockPosition());
                        PacketDebug.sendPoiTicketCountPacket(serverLevel, pos.getBlockPosition());
                    }

                }
            });
        }
    }

    @Override
    public boolean canBreed() {
        return this.foodLevel + this.countFoodPointsInInventory() >= 12 && this.getAge() == 0;
    }

    private boolean hungry() {
        return this.foodLevel < 12;
    }

    private void eatUntilFull() {
        if (this.hungry() && this.countFoodPointsInInventory() != 0) {
            for(int i = 0; i < this.getInventory().getSize(); ++i) {
                ItemStack itemStack = this.getInventory().getItem(i);
                if (!itemStack.isEmpty()) {
                    Integer integer = FOOD_POINTS.get(itemStack.getItem());
                    if (integer != null) {
                        int j = itemStack.getCount();

                        for(int k = j; k > 0; --k) {
                            this.foodLevel = (byte)(this.foodLevel + integer);
                            this.getInventory().splitStack(i, 1);
                            if (!this.hungry()) {
                                return;
                            }
                        }
                    }
                }
            }

        }
    }

    public int getPlayerReputation(EntityHuman player) {
        return this.gossips.getReputation(player.getUniqueID(), (gossipType) -> {
            return true;
        });
    }

    private void digestFood(int amount) {
        this.foodLevel = (byte)(this.foodLevel - amount);
    }

    public void eatAndDigestFood() {
        this.eatUntilFull();
        this.digestFood(12);
    }

    public void setOffers(MerchantRecipeList offers) {
        this.offers = offers;
    }

    private boolean shouldIncreaseLevel() {
        int i = this.getVillagerData().getLevel();
        return VillagerData.canLevelUp(i) && this.villagerXp >= VillagerData.getMaxXpPerLevel(i);
    }

    public void populateTrades() {
        this.setVillagerData(this.getVillagerData().withLevel(this.getVillagerData().getLevel() + 1));
        this.updateTrades();
    }

    @Override
    protected IChatBaseComponent getTypeName() {
        return new ChatMessage(this.getEntityType().getDescriptionId() + "." + IRegistry.VILLAGER_PROFESSION.getKey(this.getVillagerData().getProfession()).getKey());
    }

    @Override
    public void handleEntityEvent(byte status) {
        if (status == 12) {
            this.addParticlesAroundSelf(Particles.HEART);
        } else if (status == 13) {
            this.addParticlesAroundSelf(Particles.ANGRY_VILLAGER);
        } else if (status == 14) {
            this.addParticlesAroundSelf(Particles.HAPPY_VILLAGER);
        } else if (status == 42) {
            this.addParticlesAroundSelf(Particles.SPLASH);
        } else {
            super.handleEntityEvent(status);
        }

    }

    @Nullable
    @Override
    public GroupDataEntity prepare(WorldAccess world, DifficultyDamageScaler difficulty, EnumMobSpawn spawnReason, @Nullable GroupDataEntity entityData, @Nullable NBTTagCompound entityNbt) {
        if (spawnReason == EnumMobSpawn.BREEDING) {
            this.setVillagerData(this.getVillagerData().withProfession(VillagerProfession.NONE));
        }

        if (spawnReason == EnumMobSpawn.COMMAND || spawnReason == EnumMobSpawn.SPAWN_EGG || spawnReason == EnumMobSpawn.SPAWNER || spawnReason == EnumMobSpawn.DISPENSER) {
            this.setVillagerData(this.getVillagerData().withType(VillagerType.byBiome(world.getBiomeName(this.getChunkCoordinates()))));
        }

        if (spawnReason == EnumMobSpawn.STRUCTURE) {
            this.assignProfessionWhenSpawned = true;
        }

        return super.prepare(world, difficulty, spawnReason, entityData, entityNbt);
    }

    @Override
    public EntityVillager getBreedOffspring(WorldServer serverLevel, EntityAgeable ageableMob) {
        double d = this.random.nextDouble();
        VillagerType villagerType;
        if (d < 0.5D) {
            villagerType = VillagerType.byBiome(serverLevel.getBiomeName(this.getChunkCoordinates()));
        } else if (d < 0.75D) {
            villagerType = this.getVillagerData().getType();
        } else {
            villagerType = ((EntityVillager)ageableMob).getVillagerData().getType();
        }

        EntityVillager villager = new EntityVillager(EntityTypes.VILLAGER, serverLevel, villagerType);
        villager.prepare(serverLevel, serverLevel.getDamageScaler(villager.getChunkCoordinates()), EnumMobSpawn.BREEDING, (GroupDataEntity)null, (NBTTagCompound)null);
        return villager;
    }

    @Override
    public void onLightningStrike(WorldServer world, EntityLightning lightning) {
        if (world.getDifficulty() != EnumDifficulty.PEACEFUL) {
            LOGGER.info("Villager {} was struck by lightning {}.", this, lightning);
            EntityWitch witch = EntityTypes.WITCH.create(world);
            witch.setPositionRotation(this.locX(), this.locY(), this.locZ(), this.getYRot(), this.getXRot());
            witch.prepare(world, world.getDamageScaler(witch.getChunkCoordinates()), EnumMobSpawn.CONVERSION, (GroupDataEntity)null, (NBTTagCompound)null);
            witch.setNoAI(this.isNoAI());
            if (this.hasCustomName()) {
                witch.setCustomName(this.getCustomName());
                witch.setCustomNameVisible(this.getCustomNameVisible());
            }

            witch.setPersistent();
            world.addAllEntities(witch);
            this.releaseAllPois();
            this.die();
        } else {
            super.onLightningStrike(world, lightning);
        }

    }

    @Override
    protected void pickUpItem(EntityItem item) {
        ItemStack itemStack = item.getItemStack();
        if (this.wantsToPickUp(itemStack)) {
            InventorySubcontainer simpleContainer = this.getInventory();
            boolean bl = simpleContainer.canAddItem(itemStack);
            if (!bl) {
                return;
            }

            this.onItemPickup(item);
            this.receive(item, itemStack.getCount());
            ItemStack itemStack2 = simpleContainer.addItem(itemStack);
            if (itemStack2.isEmpty()) {
                item.die();
            } else {
                itemStack.setCount(itemStack2.getCount());
            }
        }

    }

    @Override
    public boolean wantsToPickUp(ItemStack stack) {
        Item item = stack.getItem();
        return (WANTED_ITEMS.contains(item) || this.getVillagerData().getProfession().getRequestedItems().contains(item)) && this.getInventory().canAddItem(stack);
    }

    public boolean hasExcessFood() {
        return this.countFoodPointsInInventory() >= 24;
    }

    public boolean wantsMoreFood() {
        return this.countFoodPointsInInventory() < 12;
    }

    private int countFoodPointsInInventory() {
        InventorySubcontainer simpleContainer = this.getInventory();
        return FOOD_POINTS.entrySet().stream().mapToInt((entry) -> {
            return simpleContainer.countItem(entry.getKey()) * entry.getValue();
        }).sum();
    }

    public boolean canPlant() {
        return this.getInventory().hasAnyOf(ImmutableSet.of(Items.WHEAT_SEEDS, Items.POTATO, Items.CARROT, Items.BEETROOT_SEEDS));
    }

    @Override
    protected void updateTrades() {
        VillagerData villagerData = this.getVillagerData();
        Int2ObjectMap<VillagerTrades.IMerchantRecipeOption[]> int2ObjectMap = VillagerTrades.TRADES.get(villagerData.getProfession());
        if (int2ObjectMap != null && !int2ObjectMap.isEmpty()) {
            VillagerTrades.IMerchantRecipeOption[] itemListings = int2ObjectMap.get(villagerData.getLevel());
            if (itemListings != null) {
                MerchantRecipeList merchantOffers = this.getOffers();
                this.addOffersFromItemListings(merchantOffers, itemListings, 2);
            }
        }
    }

    public void gossip(WorldServer world, EntityVillager villager, long time) {
        if ((time < this.lastGossipTime || time >= this.lastGossipTime + 1200L) && (time < villager.lastGossipTime || time >= villager.lastGossipTime + 1200L)) {
            this.gossips.transferFrom(villager.gossips, this.random, 10);
            this.lastGossipTime = time;
            villager.lastGossipTime = time;
            this.spawnGolemIfNeeded(world, time, 5);
        }
    }

    private void maybeDecayGossip() {
        long l = this.level.getTime();
        if (this.lastGossipDecayTime == 0L) {
            this.lastGossipDecayTime = l;
        } else if (l >= this.lastGossipDecayTime + 24000L) {
            this.gossips.decay();
            this.lastGossipDecayTime = l;
        }
    }

    public void spawnGolemIfNeeded(WorldServer world, long time, int requiredCount) {
        if (this.wantsToSpawnGolem(time)) {
            AxisAlignedBB aABB = this.getBoundingBox().grow(10.0D, 10.0D, 10.0D);
            List<EntityVillager> list = world.getEntitiesOfClass(EntityVillager.class, aABB);
            List<EntityVillager> list2 = list.stream().filter((villager) -> {
                return villager.wantsToSpawnGolem(time);
            }).limit(5L).collect(Collectors.toList());
            if (list2.size() >= requiredCount) {
                EntityIronGolem ironGolem = this.trySpawnGolem(world);
                if (ironGolem != null) {
                    list.forEach(SensorGolemLastSeen::golemDetected);
                }
            }
        }
    }

    public boolean wantsToSpawnGolem(long time) {
        if (!this.golemSpawnConditionsMet(this.level.getTime())) {
            return false;
        } else {
            return !this.brain.hasMemory(MemoryModuleType.GOLEM_DETECTED_RECENTLY);
        }
    }

    @Nullable
    private EntityIronGolem trySpawnGolem(WorldServer world) {
        BlockPosition blockPos = this.getChunkCoordinates();

        for(int i = 0; i < 10; ++i) {
            double d = (double)(world.random.nextInt(16) - 8);
            double e = (double)(world.random.nextInt(16) - 8);
            BlockPosition blockPos2 = this.findSpawnPositionForGolemInColumn(blockPos, d, e);
            if (blockPos2 != null) {
                EntityIronGolem ironGolem = EntityTypes.IRON_GOLEM.createCreature(world, (NBTTagCompound)null, (IChatBaseComponent)null, (EntityHuman)null, blockPos2, EnumMobSpawn.MOB_SUMMONED, false, false);
                if (ironGolem != null) {
                    if (ironGolem.checkSpawnRules(world, EnumMobSpawn.MOB_SUMMONED) && ironGolem.checkSpawnObstruction(world)) {
                        world.addAllEntities(ironGolem);
                        return ironGolem;
                    }

                    ironGolem.die();
                }
            }
        }

        return null;
    }

    @Nullable
    private BlockPosition findSpawnPositionForGolemInColumn(BlockPosition pos, double x, double z) {
        int i = 6;
        BlockPosition blockPos = pos.offset(x, 6.0D, z);
        IBlockData blockState = this.level.getType(blockPos);

        for(int j = 6; j >= -6; --j) {
            BlockPosition blockPos2 = blockPos;
            IBlockData blockState2 = blockState;
            blockPos = blockPos.below();
            blockState = this.level.getType(blockPos);
            if ((blockState2.isAir() || blockState2.getMaterial().isLiquid()) && blockState.getMaterial().isSolidBlocking()) {
                return blockPos2;
            }
        }

        return null;
    }

    @Override
    public void onReputationEventFrom(ReputationEvent interaction, Entity entity) {
        if (interaction == ReputationEvent.ZOMBIE_VILLAGER_CURED) {
            this.gossips.add(entity.getUniqueID(), ReputationType.MAJOR_POSITIVE, 20);
            this.gossips.add(entity.getUniqueID(), ReputationType.MINOR_POSITIVE, 25);
        } else if (interaction == ReputationEvent.TRADE) {
            this.gossips.add(entity.getUniqueID(), ReputationType.TRADING, 2);
        } else if (interaction == ReputationEvent.VILLAGER_HURT) {
            this.gossips.add(entity.getUniqueID(), ReputationType.MINOR_NEGATIVE, 25);
        } else if (interaction == ReputationEvent.VILLAGER_KILLED) {
            this.gossips.add(entity.getUniqueID(), ReputationType.MAJOR_NEGATIVE, 25);
        }

    }

    @Override
    public int getExperience() {
        return this.villagerXp;
    }

    public void setExperience(int amount) {
        this.villagerXp = amount;
    }

    private void resetNumberOfRestocks() {
        this.catchUpDemand();
        this.numberOfRestocksToday = 0;
    }

    public Reputation getGossips() {
        return this.gossips;
    }

    public void setGossips(NBTBase nbt) {
        this.gossips.update(new Dynamic<>(DynamicOpsNBT.INSTANCE, nbt));
    }

    @Override
    protected void sendDebugPackets() {
        super.sendDebugPackets();
        PacketDebug.sendEntityBrain(this);
    }

    @Override
    public void entitySleep(BlockPosition pos) {
        super.entitySleep(pos);
        this.brain.setMemory(MemoryModuleType.LAST_SLEPT, this.level.getTime());
        this.brain.removeMemory(MemoryModuleType.WALK_TARGET);
        this.brain.removeMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
    }

    @Override
    public void entityWakeup() {
        super.entityWakeup();
        this.brain.setMemory(MemoryModuleType.LAST_WOKEN, this.level.getTime());
    }

    private boolean golemSpawnConditionsMet(long worldTime) {
        Optional<Long> optional = this.brain.getMemory(MemoryModuleType.LAST_SLEPT);
        if (optional.isPresent()) {
            return worldTime - optional.get() < 24000L;
        } else {
            return false;
        }
    }
}
