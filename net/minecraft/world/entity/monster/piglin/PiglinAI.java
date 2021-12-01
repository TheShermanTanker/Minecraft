package net.minecraft.world.entity.monster.piglin;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.TagsItem;
import net.minecraft.util.TimeRange;
import net.minecraft.util.valueproviders.IntProviderUniform;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.BehaviorController;
import net.minecraft.world.entity.ai.behavior.BehaviorAttack;
import net.minecraft.world.entity.ai.behavior.BehaviorAttackTargetForget;
import net.minecraft.world.entity.ai.behavior.BehaviorAttackTargetSet;
import net.minecraft.world.entity.ai.behavior.BehaviorCelebrateDeath;
import net.minecraft.world.entity.ai.behavior.BehaviorCelebrateLocation;
import net.minecraft.world.entity.ai.behavior.BehaviorCrossbowAttack;
import net.minecraft.world.entity.ai.behavior.BehaviorExpirableMemory;
import net.minecraft.world.entity.ai.behavior.BehaviorFindAdmirableItem;
import net.minecraft.world.entity.ai.behavior.BehaviorForgetAnger;
import net.minecraft.world.entity.ai.behavior.BehaviorGateSingle;
import net.minecraft.world.entity.ai.behavior.BehaviorInteract;
import net.minecraft.world.entity.ai.behavior.BehaviorInteractDoor;
import net.minecraft.world.entity.ai.behavior.BehaviorLook;
import net.minecraft.world.entity.ai.behavior.BehaviorLookInteract;
import net.minecraft.world.entity.ai.behavior.BehaviorLookTarget;
import net.minecraft.world.entity.ai.behavior.BehaviorLookWalk;
import net.minecraft.world.entity.ai.behavior.BehaviorNop;
import net.minecraft.world.entity.ai.behavior.BehaviorRemoveMemory;
import net.minecraft.world.entity.ai.behavior.BehaviorRetreat;
import net.minecraft.world.entity.ai.behavior.BehaviorRunIf;
import net.minecraft.world.entity.ai.behavior.BehaviorRunSometimes;
import net.minecraft.world.entity.ai.behavior.BehaviorStartRiding;
import net.minecraft.world.entity.ai.behavior.BehaviorStopRiding;
import net.minecraft.world.entity.ai.behavior.BehaviorStrollRandomUnconstrained;
import net.minecraft.world.entity.ai.behavior.BehaviorUtil;
import net.minecraft.world.entity.ai.behavior.BehaviorWalkAway;
import net.minecraft.world.entity.ai.behavior.BehaviorWalkAwayOutOfRange;
import net.minecraft.world.entity.ai.behavior.BehavorMove;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.monster.hoglin.EntityHoglin;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.EnumArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemArmor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.LootTables;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameterSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameters;
import net.minecraft.world.phys.Vec3D;

public class PiglinAI {
    public static final int REPELLENT_DETECTION_RANGE_HORIZONTAL = 8;
    public static final int REPELLENT_DETECTION_RANGE_VERTICAL = 4;
    public static final Item BARTERING_ITEM = Items.GOLD_INGOT;
    private static final int PLAYER_ANGER_RANGE = 16;
    private static final int ANGER_DURATION = 600;
    private static final int ADMIRE_DURATION = 120;
    private static final int MAX_DISTANCE_TO_WALK_TO_ITEM = 9;
    private static final int MAX_TIME_TO_WALK_TO_ITEM = 200;
    private static final int HOW_LONG_TIME_TO_DISABLE_ADMIRE_WALKING_IF_CANT_REACH_ITEM = 200;
    private static final int CELEBRATION_TIME = 300;
    private static final IntProviderUniform TIME_BETWEEN_HUNTS = TimeRange.rangeOfSeconds(30, 120);
    private static final int BABY_FLEE_DURATION_AFTER_GETTING_HIT = 100;
    private static final int HIT_BY_PLAYER_MEMORY_TIMEOUT = 400;
    private static final int MAX_WALK_DISTANCE_TO_START_RIDING = 8;
    private static final IntProviderUniform RIDE_START_INTERVAL = TimeRange.rangeOfSeconds(10, 40);
    private static final IntProviderUniform RIDE_DURATION = TimeRange.rangeOfSeconds(10, 30);
    private static final IntProviderUniform RETREAT_DURATION = TimeRange.rangeOfSeconds(5, 20);
    private static final int MELEE_ATTACK_COOLDOWN = 20;
    private static final int EAT_COOLDOWN = 200;
    private static final int DESIRED_DISTANCE_FROM_ENTITY_WHEN_AVOIDING = 12;
    private static final int MAX_LOOK_DIST = 8;
    private static final int MAX_LOOK_DIST_FOR_PLAYER_HOLDING_LOVED_ITEM = 14;
    private static final int INTERACTION_RANGE = 8;
    private static final int MIN_DESIRED_DIST_FROM_TARGET_WHEN_HOLDING_CROSSBOW = 5;
    private static final float SPEED_WHEN_STRAFING_BACK_FROM_TARGET = 0.75F;
    private static final int DESIRED_DISTANCE_FROM_ZOMBIFIED = 6;
    private static final IntProviderUniform AVOID_ZOMBIFIED_DURATION = TimeRange.rangeOfSeconds(5, 7);
    private static final IntProviderUniform BABY_AVOID_NEMESIS_DURATION = TimeRange.rangeOfSeconds(5, 7);
    private static final float PROBABILITY_OF_CELEBRATION_DANCE = 0.1F;
    private static final float SPEED_MULTIPLIER_WHEN_AVOIDING = 1.0F;
    private static final float SPEED_MULTIPLIER_WHEN_RETREATING = 1.0F;
    private static final float SPEED_MULTIPLIER_WHEN_MOUNTING = 0.8F;
    private static final float SPEED_MULTIPLIER_WHEN_GOING_TO_WANTED_ITEM = 1.0F;
    private static final float SPEED_MULTIPLIER_WHEN_GOING_TO_CELEBRATE_LOCATION = 1.0F;
    private static final float SPEED_MULTIPLIER_WHEN_DANCING = 0.6F;
    private static final float SPEED_MULTIPLIER_WHEN_IDLING = 0.6F;

    protected static BehaviorController<?> makeBrain(EntityPiglin piglin, BehaviorController<EntityPiglin> brain) {
        initCoreActivity(brain);
        initIdleActivity(brain);
        initAdmireItemActivity(brain);
        initFightActivity(piglin, brain);
        initCelebrateActivity(brain);
        initRetreatActivity(brain);
        initRideHoglinActivity(brain);
        brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.useDefaultActivity();
        return brain;
    }

    protected static void initMemories(EntityPiglin piglin) {
        int i = TIME_BETWEEN_HUNTS.sample(piglin.level.random);
        piglin.getBehaviorController().setMemoryWithExpiry(MemoryModuleType.HUNTED_RECENTLY, true, (long)i);
    }

    private static void initCoreActivity(BehaviorController<EntityPiglin> piglin) {
        piglin.addActivity(Activity.CORE, 0, ImmutableList.of(new BehaviorLook(45, 90), new BehavorMove(), new BehaviorInteractDoor(), babyAvoidNemesis(), avoidZombified(), new BehaviorStopAdmiring(), new BehaviorStartAdmiringItem(120), new BehaviorCelebrateDeath(300, PiglinAI::wantsToDance), new BehaviorForgetAnger()));
    }

    private static void initIdleActivity(BehaviorController<EntityPiglin> piglin) {
        piglin.addActivity(Activity.IDLE, 10, ImmutableList.of(new BehaviorLookTarget(PiglinAI::isPlayerHoldingLovedItem, 14.0F), new BehaviorAttackTargetSet<>(EntityPiglinAbstract::isAdult, PiglinAI::findNearestValidAttackTarget), new BehaviorRunIf(EntityPiglin::canHunt, new BehaviorHuntHoglin<>()), avoidRepellent(), babySometimesRideBabyHoglin(), createIdleLookBehaviors(), createIdleMovementBehaviors(), new BehaviorLookInteract(EntityTypes.PLAYER, 4)));
    }

    private static void initFightActivity(EntityPiglin piglin, BehaviorController<EntityPiglin> brain) {
        brain.addActivityAndRemoveMemoryWhenStopped(Activity.FIGHT, 10, ImmutableList.of(new BehaviorAttackTargetForget<>((livingEntity) -> {
            return !isNearestValidAttackTarget(piglin, livingEntity);
        }), new BehaviorRunIf(PiglinAI::hasCrossbow, new BehaviorRetreat<>(5, 0.75F)), new BehaviorWalkAwayOutOfRange(1.0F), new BehaviorAttack(20), new BehaviorCrossbowAttack(), new BehaviorRememberHuntedHoglin(), new BehaviorRemoveMemory(PiglinAI::isNearZombified, MemoryModuleType.ATTACK_TARGET)), MemoryModuleType.ATTACK_TARGET);
    }

    private static void initCelebrateActivity(BehaviorController<EntityPiglin> brain) {
        brain.addActivityAndRemoveMemoryWhenStopped(Activity.CELEBRATE, 10, ImmutableList.of(avoidRepellent(), new BehaviorLookTarget(PiglinAI::isPlayerHoldingLovedItem, 14.0F), new BehaviorAttackTargetSet<EntityPiglin>(EntityPiglinAbstract::isAdult, PiglinAI::findNearestValidAttackTarget), new BehaviorRunIf<EntityPiglin>((piglin) -> {
            return !piglin.isDancing();
        }, new BehaviorCelebrateLocation<>(2, 1.0F)), new BehaviorRunIf<EntityPiglin>(EntityPiglin::isDancing, new BehaviorCelebrateLocation<>(4, 0.6F)), new BehaviorGateSingle(ImmutableList.of(Pair.of(new BehaviorLookTarget(EntityTypes.PIGLIN, 8.0F), 1), Pair.of(new BehaviorStrollRandomUnconstrained(0.6F, 2, 1), 1), Pair.of(new BehaviorNop(10, 20), 1)))), MemoryModuleType.CELEBRATE_LOCATION);
    }

    private static void initAdmireItemActivity(BehaviorController<EntityPiglin> brain) {
        brain.addActivityAndRemoveMemoryWhenStopped(Activity.ADMIRE_ITEM, 10, ImmutableList.of(new BehaviorFindAdmirableItem<>(PiglinAI::isNotHoldingLovedItemInOffHand, 1.0F, true, 9), new BehaviorStopAdmiringItem(9), new BehaviorAdmireTimeout(200, 200)), MemoryModuleType.ADMIRING_ITEM);
    }

    private static void initRetreatActivity(BehaviorController<EntityPiglin> brain) {
        brain.addActivityAndRemoveMemoryWhenStopped(Activity.AVOID, 10, ImmutableList.of(BehaviorWalkAway.entity(MemoryModuleType.AVOID_TARGET, 1.0F, 12, true), createIdleLookBehaviors(), createIdleMovementBehaviors(), new BehaviorRemoveMemory<EntityPiglin>(PiglinAI::wantsToStopFleeing, MemoryModuleType.AVOID_TARGET)), MemoryModuleType.AVOID_TARGET);
    }

    private static void initRideHoglinActivity(BehaviorController<EntityPiglin> brain) {
        brain.addActivityAndRemoveMemoryWhenStopped(Activity.RIDE, 10, ImmutableList.of(new BehaviorStartRiding<>(0.8F), new BehaviorLookTarget(PiglinAI::isPlayerHoldingLovedItem, 8.0F), new BehaviorRunIf(Entity::isPassenger, createIdleLookBehaviors()), new BehaviorStopRiding(8, PiglinAI::wantsToStopRiding)), MemoryModuleType.RIDE_TARGET);
    }

    private static BehaviorGateSingle<EntityPiglin> createIdleLookBehaviors() {
        return new BehaviorGateSingle<>(ImmutableList.of(Pair.of(new BehaviorLookTarget(EntityTypes.PLAYER, 8.0F), 1), Pair.of(new BehaviorLookTarget(EntityTypes.PIGLIN, 8.0F), 1), Pair.of(new BehaviorLookTarget(8.0F), 1), Pair.of(new BehaviorNop(30, 60), 1)));
    }

    private static BehaviorGateSingle<EntityPiglin> createIdleMovementBehaviors() {
        return new BehaviorGateSingle<>(ImmutableList.of(Pair.of(new BehaviorStrollRandomUnconstrained(0.6F), 2), Pair.of(BehaviorInteract.of(EntityTypes.PIGLIN, 8, MemoryModuleType.INTERACTION_TARGET, 0.6F, 2), 2), Pair.of(new BehaviorRunIf<>(PiglinAI::doesntSeeAnyPlayerHoldingLovedItem, new BehaviorLookWalk(0.6F, 3)), 2), Pair.of(new BehaviorNop(30, 60), 1)));
    }

    private static BehaviorWalkAway<BlockPosition> avoidRepellent() {
        return BehaviorWalkAway.pos(MemoryModuleType.NEAREST_REPELLENT, 1.0F, 8, false);
    }

    private static BehaviorExpirableMemory<EntityPiglin, EntityLiving> babyAvoidNemesis() {
        return new BehaviorExpirableMemory<>(EntityPiglin::isBaby, MemoryModuleType.NEAREST_VISIBLE_NEMESIS, MemoryModuleType.AVOID_TARGET, BABY_AVOID_NEMESIS_DURATION);
    }

    private static BehaviorExpirableMemory<EntityPiglin, EntityLiving> avoidZombified() {
        return new BehaviorExpirableMemory<>(PiglinAI::isNearZombified, MemoryModuleType.NEAREST_VISIBLE_ZOMBIFIED, MemoryModuleType.AVOID_TARGET, AVOID_ZOMBIFIED_DURATION);
    }

    protected static void updateActivity(EntityPiglin piglin) {
        BehaviorController<EntityPiglin> brain = piglin.getBehaviorController();
        Activity activity = brain.getActiveNonCoreActivity().orElse((Activity)null);
        brain.setActiveActivityToFirstValid(ImmutableList.of(Activity.ADMIRE_ITEM, Activity.FIGHT, Activity.AVOID, Activity.CELEBRATE, Activity.RIDE, Activity.IDLE));
        Activity activity2 = brain.getActiveNonCoreActivity().orElse((Activity)null);
        if (activity != activity2) {
            getSoundForCurrentActivity(piglin).ifPresent(piglin::playSound);
        }

        piglin.setAggressive(brain.hasMemory(MemoryModuleType.ATTACK_TARGET));
        if (!brain.hasMemory(MemoryModuleType.RIDE_TARGET) && isBabyRidingBaby(piglin)) {
            piglin.stopRiding();
        }

        if (!brain.hasMemory(MemoryModuleType.CELEBRATE_LOCATION)) {
            brain.removeMemory(MemoryModuleType.DANCING);
        }

        piglin.setDancing(brain.hasMemory(MemoryModuleType.DANCING));
    }

    private static boolean isBabyRidingBaby(EntityPiglin piglin) {
        if (!piglin.isBaby()) {
            return false;
        } else {
            Entity entity = piglin.getVehicle();
            return entity instanceof EntityPiglin && ((EntityPiglin)entity).isBaby() || entity instanceof EntityHoglin && ((EntityHoglin)entity).isBaby();
        }
    }

    protected static void pickUpItem(EntityPiglin piglin, EntityItem drop) {
        stopWalking(piglin);
        ItemStack itemStack;
        if (drop.getItemStack().is(Items.GOLD_NUGGET)) {
            piglin.receive(drop, drop.getItemStack().getCount());
            itemStack = drop.getItemStack();
            drop.die();
        } else {
            piglin.receive(drop, 1);
            itemStack = removeOneItemFromItemEntity(drop);
        }

        if (isLovedItem(itemStack)) {
            piglin.getBehaviorController().removeMemory(MemoryModuleType.TIME_TRYING_TO_REACH_ADMIRE_ITEM);
            holdInOffhand(piglin, itemStack);
            admireGoldItem(piglin);
        } else if (isFood(itemStack) && !hasEatenRecently(piglin)) {
            eat(piglin);
        } else {
            boolean bl = piglin.equipItemIfPossible(itemStack);
            if (!bl) {
                putInInventory(piglin, itemStack);
            }
        }
    }

    private static void holdInOffhand(EntityPiglin piglin, ItemStack stack) {
        if (isHoldingItemInOffHand(piglin)) {
            piglin.spawnAtLocation(piglin.getItemInHand(EnumHand.OFF_HAND));
        }

        piglin.holdInOffHand(stack);
    }

    private static ItemStack removeOneItemFromItemEntity(EntityItem stack) {
        ItemStack itemStack = stack.getItemStack();
        ItemStack itemStack2 = itemStack.cloneAndSubtract(1);
        if (itemStack.isEmpty()) {
            stack.die();
        } else {
            stack.setItemStack(itemStack);
        }

        return itemStack2;
    }

    protected static void stopHoldingOffHandItem(EntityPiglin piglin, boolean barter) {
        ItemStack itemStack = piglin.getItemInHand(EnumHand.OFF_HAND);
        piglin.setItemInHand(EnumHand.OFF_HAND, ItemStack.EMPTY);
        if (piglin.isAdult()) {
            boolean bl = isBarterCurrency(itemStack);
            if (barter && bl) {
                throwItems(piglin, getBarterResponseItems(piglin));
            } else if (!bl) {
                boolean bl2 = piglin.equipItemIfPossible(itemStack);
                if (!bl2) {
                    putInInventory(piglin, itemStack);
                }
            }
        } else {
            boolean bl3 = piglin.equipItemIfPossible(itemStack);
            if (!bl3) {
                ItemStack itemStack2 = piglin.getItemInMainHand();
                if (isLovedItem(itemStack2)) {
                    putInInventory(piglin, itemStack2);
                } else {
                    throwItems(piglin, Collections.singletonList(itemStack2));
                }

                piglin.holdInMainHand(itemStack);
            }
        }

    }

    protected static void cancelAdmiring(EntityPiglin piglin) {
        if (isAdmiringItem(piglin) && !piglin.getItemInOffHand().isEmpty()) {
            piglin.spawnAtLocation(piglin.getItemInOffHand());
            piglin.setItemInHand(EnumHand.OFF_HAND, ItemStack.EMPTY);
        }

    }

    private static void putInInventory(EntityPiglin piglin, ItemStack stack) {
        ItemStack itemStack = piglin.addToInventory(stack);
        throwItemsTowardRandomPos(piglin, Collections.singletonList(itemStack));
    }

    private static void throwItems(EntityPiglin piglin, List<ItemStack> items) {
        Optional<EntityHuman> optional = piglin.getBehaviorController().getMemory(MemoryModuleType.NEAREST_VISIBLE_PLAYER);
        if (optional.isPresent()) {
            throwItemsTowardPlayer(piglin, optional.get(), items);
        } else {
            throwItemsTowardRandomPos(piglin, items);
        }

    }

    private static void throwItemsTowardRandomPos(EntityPiglin piglin, List<ItemStack> items) {
        throwItemsTowardPos(piglin, items, getRandomNearbyPos(piglin));
    }

    private static void throwItemsTowardPlayer(EntityPiglin piglin, EntityHuman player, List<ItemStack> items) {
        throwItemsTowardPos(piglin, items, player.getPositionVector());
    }

    private static void throwItemsTowardPos(EntityPiglin piglin, List<ItemStack> items, Vec3D pos) {
        if (!items.isEmpty()) {
            piglin.swingHand(EnumHand.OFF_HAND);

            for(ItemStack itemStack : items) {
                BehaviorUtil.throwItem(piglin, itemStack, pos.add(0.0D, 1.0D, 0.0D));
            }
        }

    }

    private static List<ItemStack> getBarterResponseItems(EntityPiglin piglin) {
        LootTable lootTable = piglin.level.getMinecraftServer().getLootTableRegistry().getLootTable(LootTables.PIGLIN_BARTERING);
        return lootTable.populateLoot((new LootTableInfo.Builder((WorldServer)piglin.level)).set(LootContextParameters.THIS_ENTITY, piglin).withRandom(piglin.level.random).build(LootContextParameterSets.PIGLIN_BARTER));
    }

    private static boolean wantsToDance(EntityLiving piglin, EntityLiving target) {
        if (target.getEntityType() != EntityTypes.HOGLIN) {
            return false;
        } else {
            return (new Random(piglin.level.getTime())).nextFloat() < 0.1F;
        }
    }

    protected static boolean wantsToPickup(EntityPiglin piglin, ItemStack stack) {
        if (piglin.isBaby() && stack.is(TagsItem.IGNORED_BY_PIGLIN_BABIES)) {
            return false;
        } else if (stack.is(TagsItem.PIGLIN_REPELLENTS)) {
            return false;
        } else if (isAdmiringDisabled(piglin) && piglin.getBehaviorController().hasMemory(MemoryModuleType.ATTACK_TARGET)) {
            return false;
        } else if (isBarterCurrency(stack)) {
            return isNotHoldingLovedItemInOffHand(piglin);
        } else {
            boolean bl = piglin.canAddToInventory(stack);
            if (stack.is(Items.GOLD_NUGGET)) {
                return bl;
            } else if (isFood(stack)) {
                return !hasEatenRecently(piglin) && bl;
            } else if (!isLovedItem(stack)) {
                return piglin.canReplaceCurrentItem(stack);
            } else {
                return isNotHoldingLovedItemInOffHand(piglin) && bl;
            }
        }
    }

    protected static boolean isLovedItem(ItemStack stack) {
        return stack.is(TagsItem.PIGLIN_LOVED);
    }

    private static boolean wantsToStopRiding(EntityPiglin piglin, Entity ridden) {
        if (!(ridden instanceof EntityInsentient)) {
            return false;
        } else {
            EntityInsentient mob = (EntityInsentient)ridden;
            return !mob.isBaby() || !mob.isAlive() || wasHurtRecently(piglin) || wasHurtRecently(mob) || mob instanceof EntityPiglin && mob.getVehicle() == null;
        }
    }

    private static boolean isNearestValidAttackTarget(EntityPiglin piglin, EntityLiving target) {
        return findNearestValidAttackTarget(piglin).filter((livingEntity2) -> {
            return livingEntity2 == target;
        }).isPresent();
    }

    private static boolean isNearZombified(EntityPiglin piglin) {
        BehaviorController<EntityPiglin> brain = piglin.getBehaviorController();
        if (brain.hasMemory(MemoryModuleType.NEAREST_VISIBLE_ZOMBIFIED)) {
            EntityLiving livingEntity = brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_ZOMBIFIED).get();
            return piglin.closerThan(livingEntity, 6.0D);
        } else {
            return false;
        }
    }

    private static Optional<? extends EntityLiving> findNearestValidAttackTarget(EntityPiglin piglin) {
        BehaviorController<EntityPiglin> brain = piglin.getBehaviorController();
        if (isNearZombified(piglin)) {
            return Optional.empty();
        } else {
            Optional<EntityLiving> optional = BehaviorUtil.getLivingEntityFromUUIDMemory(piglin, MemoryModuleType.ANGRY_AT);
            if (optional.isPresent() && Sensor.isEntityAttackableIgnoringLineOfSight(piglin, optional.get())) {
                return optional;
            } else {
                if (brain.hasMemory(MemoryModuleType.UNIVERSAL_ANGER)) {
                    Optional<EntityHuman> optional2 = brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER);
                    if (optional2.isPresent()) {
                        return optional2;
                    }
                }

                Optional<EntityInsentient> optional3 = brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_NEMESIS);
                if (optional3.isPresent()) {
                    return optional3;
                } else {
                    Optional<EntityHuman> optional4 = brain.getMemory(MemoryModuleType.NEAREST_TARGETABLE_PLAYER_NOT_WEARING_GOLD);
                    return optional4.isPresent() && Sensor.isEntityAttackable(piglin, optional4.get()) ? optional4 : Optional.empty();
                }
            }
        }
    }

    public static void angerNearbyPiglins(EntityHuman player, boolean blockOpen) {
        List<EntityPiglin> list = player.level.getEntitiesOfClass(EntityPiglin.class, player.getBoundingBox().inflate(16.0D));
        list.stream().filter(PiglinAI::isIdle).filter((piglin) -> {
            return !blockOpen || BehaviorUtil.canSee(piglin, player);
        }).forEach((piglin) -> {
            if (piglin.level.getGameRules().getBoolean(GameRules.RULE_UNIVERSAL_ANGER)) {
                setAngerTargetToNearestTargetablePlayerIfFound(piglin, player);
            } else {
                setAngerTarget(piglin, player);
            }

        });
    }

    public static EnumInteractionResult mobInteract(EntityPiglin piglin, EntityHuman player, EnumHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (canAdmire(piglin, itemStack)) {
            ItemStack itemStack2 = itemStack.cloneAndSubtract(1);
            holdInOffhand(piglin, itemStack2);
            admireGoldItem(piglin);
            stopWalking(piglin);
            return EnumInteractionResult.CONSUME;
        } else {
            return EnumInteractionResult.PASS;
        }
    }

    protected static boolean canAdmire(EntityPiglin piglin, ItemStack nearbyItems) {
        return !isAdmiringDisabled(piglin) && !isAdmiringItem(piglin) && piglin.isAdult() && isBarterCurrency(nearbyItems);
    }

    protected static void wasHurtBy(EntityPiglin piglin, EntityLiving attacker) {
        if (!(attacker instanceof EntityPiglin)) {
            if (isHoldingItemInOffHand(piglin)) {
                stopHoldingOffHandItem(piglin, false);
            }

            BehaviorController<EntityPiglin> brain = piglin.getBehaviorController();
            brain.removeMemory(MemoryModuleType.CELEBRATE_LOCATION);
            brain.removeMemory(MemoryModuleType.DANCING);
            brain.removeMemory(MemoryModuleType.ADMIRING_ITEM);
            if (attacker instanceof EntityHuman) {
                brain.setMemoryWithExpiry(MemoryModuleType.ADMIRING_DISABLED, true, 400L);
            }

            getAvoidTarget(piglin).ifPresent((livingEntity2) -> {
                if (livingEntity2.getEntityType() != attacker.getEntityType()) {
                    brain.removeMemory(MemoryModuleType.AVOID_TARGET);
                }

            });
            if (piglin.isBaby()) {
                brain.setMemoryWithExpiry(MemoryModuleType.AVOID_TARGET, attacker, 100L);
                if (Sensor.isEntityAttackableIgnoringLineOfSight(piglin, attacker)) {
                    broadcastAngerTarget(piglin, attacker);
                }

            } else if (attacker.getEntityType() == EntityTypes.HOGLIN && hoglinsOutnumberPiglins(piglin)) {
                setAvoidTargetAndDontHuntForAWhile(piglin, attacker);
                broadcastRetreat(piglin, attacker);
            } else {
                maybeRetaliate(piglin, attacker);
            }
        }
    }

    protected static void maybeRetaliate(EntityPiglinAbstract piglin, EntityLiving target) {
        if (!piglin.getBehaviorController().isActive(Activity.AVOID)) {
            if (Sensor.isEntityAttackableIgnoringLineOfSight(piglin, target)) {
                if (!BehaviorUtil.isOtherTargetMuchFurtherAwayThanCurrentAttackTarget(piglin, target, 4.0D)) {
                    if (target.getEntityType() == EntityTypes.PLAYER && piglin.level.getGameRules().getBoolean(GameRules.RULE_UNIVERSAL_ANGER)) {
                        setAngerTargetToNearestTargetablePlayerIfFound(piglin, target);
                        broadcastUniversalAnger(piglin);
                    } else {
                        setAngerTarget(piglin, target);
                        broadcastAngerTarget(piglin, target);
                    }

                }
            }
        }
    }

    public static Optional<SoundEffect> getSoundForCurrentActivity(EntityPiglin piglin) {
        return piglin.getBehaviorController().getActiveNonCoreActivity().map((activity) -> {
            return getSoundForActivity(piglin, activity);
        });
    }

    private static SoundEffect getSoundForActivity(EntityPiglin piglin, Activity activity) {
        if (activity == Activity.FIGHT) {
            return SoundEffects.PIGLIN_ANGRY;
        } else if (piglin.isConverting()) {
            return SoundEffects.PIGLIN_RETREAT;
        } else if (activity == Activity.AVOID && isNearAvoidTarget(piglin)) {
            return SoundEffects.PIGLIN_RETREAT;
        } else if (activity == Activity.ADMIRE_ITEM) {
            return SoundEffects.PIGLIN_ADMIRING_ITEM;
        } else if (activity == Activity.CELEBRATE) {
            return SoundEffects.PIGLIN_CELEBRATE;
        } else if (seesPlayerHoldingLovedItem(piglin)) {
            return SoundEffects.PIGLIN_JEALOUS;
        } else {
            return isNearRepellent(piglin) ? SoundEffects.PIGLIN_RETREAT : SoundEffects.PIGLIN_AMBIENT;
        }
    }

    private static boolean isNearAvoidTarget(EntityPiglin piglin) {
        BehaviorController<EntityPiglin> brain = piglin.getBehaviorController();
        return !brain.hasMemory(MemoryModuleType.AVOID_TARGET) ? false : brain.getMemory(MemoryModuleType.AVOID_TARGET).get().closerThan(piglin, 12.0D);
    }

    protected static boolean hasAnyoneNearbyHuntedRecently(EntityPiglin piglin) {
        return piglin.getBehaviorController().hasMemory(MemoryModuleType.HUNTED_RECENTLY) || getVisibleAdultPiglins(piglin).stream().anyMatch((abstractPiglin) -> {
            return abstractPiglin.getBehaviorController().hasMemory(MemoryModuleType.HUNTED_RECENTLY);
        });
    }

    private static List<EntityPiglinAbstract> getVisibleAdultPiglins(EntityPiglin piglin) {
        return piglin.getBehaviorController().getMemory(MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLINS).orElse(ImmutableList.of());
    }

    private static List<EntityPiglinAbstract> getAdultPiglins(EntityPiglinAbstract piglin) {
        return piglin.getBehaviorController().getMemory(MemoryModuleType.NEARBY_ADULT_PIGLINS).orElse(ImmutableList.of());
    }

    public static boolean isWearingGold(EntityLiving entity) {
        for(ItemStack itemStack : entity.getArmorItems()) {
            Item item = itemStack.getItem();
            if (item instanceof ItemArmor && ((ItemArmor)item).getMaterial() == EnumArmorMaterial.GOLD) {
                return true;
            }
        }

        return false;
    }

    private static void stopWalking(EntityPiglin piglin) {
        piglin.getBehaviorController().removeMemory(MemoryModuleType.WALK_TARGET);
        piglin.getNavigation().stop();
    }

    private static BehaviorRunSometimes<EntityPiglin> babySometimesRideBabyHoglin() {
        return new BehaviorRunSometimes<>(new BehaviorExpirableMemory<>(EntityPiglin::isBaby, MemoryModuleType.NEAREST_VISIBLE_BABY_HOGLIN, MemoryModuleType.RIDE_TARGET, RIDE_DURATION), RIDE_START_INTERVAL);
    }

    protected static void broadcastAngerTarget(EntityPiglinAbstract piglin, EntityLiving target) {
        getAdultPiglins(piglin).forEach((abstractPiglin) -> {
            if (target.getEntityType() != EntityTypes.HOGLIN || abstractPiglin.canHunt() && ((EntityHoglin)target).canBeHunted()) {
                setAngerTargetIfCloserThanCurrent(abstractPiglin, target);
            }
        });
    }

    protected static void broadcastUniversalAnger(EntityPiglinAbstract piglin) {
        getAdultPiglins(piglin).forEach((abstractPiglin) -> {
            getNearestVisibleTargetablePlayer(abstractPiglin).ifPresent((player) -> {
                setAngerTarget(abstractPiglin, player);
            });
        });
    }

    protected static void broadcastDontKillAnyMoreHoglinsForAWhile(EntityPiglin piglin) {
        getVisibleAdultPiglins(piglin).forEach(PiglinAI::dontKillAnyMoreHoglinsForAWhile);
    }

    protected static void setAngerTarget(EntityPiglinAbstract piglin, EntityLiving target) {
        if (Sensor.isEntityAttackableIgnoringLineOfSight(piglin, target)) {
            piglin.getBehaviorController().removeMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
            piglin.getBehaviorController().setMemoryWithExpiry(MemoryModuleType.ANGRY_AT, target.getUniqueID(), 600L);
            if (target.getEntityType() == EntityTypes.HOGLIN && piglin.canHunt()) {
                dontKillAnyMoreHoglinsForAWhile(piglin);
            }

            if (target.getEntityType() == EntityTypes.PLAYER && piglin.level.getGameRules().getBoolean(GameRules.RULE_UNIVERSAL_ANGER)) {
                piglin.getBehaviorController().setMemoryWithExpiry(MemoryModuleType.UNIVERSAL_ANGER, true, 600L);
            }

        }
    }

    private static void setAngerTargetToNearestTargetablePlayerIfFound(EntityPiglinAbstract piglin, EntityLiving player) {
        Optional<EntityHuman> optional = getNearestVisibleTargetablePlayer(piglin);
        if (optional.isPresent()) {
            setAngerTarget(piglin, optional.get());
        } else {
            setAngerTarget(piglin, player);
        }

    }

    private static void setAngerTargetIfCloserThanCurrent(EntityPiglinAbstract piglin, EntityLiving target) {
        Optional<EntityLiving> optional = getAngerTarget(piglin);
        EntityLiving livingEntity = BehaviorUtil.getNearestTarget(piglin, optional, target);
        if (!optional.isPresent() || optional.get() != livingEntity) {
            setAngerTarget(piglin, livingEntity);
        }
    }

    private static Optional<EntityLiving> getAngerTarget(EntityPiglinAbstract piglin) {
        return BehaviorUtil.getLivingEntityFromUUIDMemory(piglin, MemoryModuleType.ANGRY_AT);
    }

    public static Optional<EntityLiving> getAvoidTarget(EntityPiglin piglin) {
        return piglin.getBehaviorController().hasMemory(MemoryModuleType.AVOID_TARGET) ? piglin.getBehaviorController().getMemory(MemoryModuleType.AVOID_TARGET) : Optional.empty();
    }

    public static Optional<EntityHuman> getNearestVisibleTargetablePlayer(EntityPiglinAbstract piglin) {
        return piglin.getBehaviorController().hasMemory(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER) ? piglin.getBehaviorController().getMemory(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER) : Optional.empty();
    }

    private static void broadcastRetreat(EntityPiglin piglin, EntityLiving target) {
        getVisibleAdultPiglins(piglin).stream().filter((abstractPiglin) -> {
            return abstractPiglin instanceof EntityPiglin;
        }).forEach((piglinx) -> {
            retreatFromNearestTarget((EntityPiglin)piglinx, target);
        });
    }

    private static void retreatFromNearestTarget(EntityPiglin piglin, EntityLiving target) {
        BehaviorController<EntityPiglin> brain = piglin.getBehaviorController();
        EntityLiving livingEntity = BehaviorUtil.getNearestTarget(piglin, brain.getMemory(MemoryModuleType.AVOID_TARGET), target);
        livingEntity = BehaviorUtil.getNearestTarget(piglin, brain.getMemory(MemoryModuleType.ATTACK_TARGET), livingEntity);
        setAvoidTargetAndDontHuntForAWhile(piglin, livingEntity);
    }

    private static boolean wantsToStopFleeing(EntityPiglin piglin) {
        BehaviorController<EntityPiglin> brain = piglin.getBehaviorController();
        if (!brain.hasMemory(MemoryModuleType.AVOID_TARGET)) {
            return true;
        } else {
            EntityLiving livingEntity = brain.getMemory(MemoryModuleType.AVOID_TARGET).get();
            EntityTypes<?> entityType = livingEntity.getEntityType();
            if (entityType == EntityTypes.HOGLIN) {
                return piglinsEqualOrOutnumberHoglins(piglin);
            } else if (isZombified(entityType)) {
                return !brain.isMemoryValue(MemoryModuleType.NEAREST_VISIBLE_ZOMBIFIED, livingEntity);
            } else {
                return false;
            }
        }
    }

    private static boolean piglinsEqualOrOutnumberHoglins(EntityPiglin piglin) {
        return !hoglinsOutnumberPiglins(piglin);
    }

    private static boolean hoglinsOutnumberPiglins(EntityPiglin piglins) {
        int i = piglins.getBehaviorController().getMemory(MemoryModuleType.VISIBLE_ADULT_PIGLIN_COUNT).orElse(0) + 1;
        int j = piglins.getBehaviorController().getMemory(MemoryModuleType.VISIBLE_ADULT_HOGLIN_COUNT).orElse(0);
        return j > i;
    }

    private static void setAvoidTargetAndDontHuntForAWhile(EntityPiglin piglin, EntityLiving target) {
        piglin.getBehaviorController().removeMemory(MemoryModuleType.ANGRY_AT);
        piglin.getBehaviorController().removeMemory(MemoryModuleType.ATTACK_TARGET);
        piglin.getBehaviorController().removeMemory(MemoryModuleType.WALK_TARGET);
        piglin.getBehaviorController().setMemoryWithExpiry(MemoryModuleType.AVOID_TARGET, target, (long)RETREAT_DURATION.sample(piglin.level.random));
        dontKillAnyMoreHoglinsForAWhile(piglin);
    }

    protected static void dontKillAnyMoreHoglinsForAWhile(EntityPiglinAbstract piglin) {
        piglin.getBehaviorController().setMemoryWithExpiry(MemoryModuleType.HUNTED_RECENTLY, true, (long)TIME_BETWEEN_HUNTS.sample(piglin.level.random));
    }

    private static boolean seesPlayerHoldingWantedItem(EntityPiglin piglin) {
        return piglin.getBehaviorController().hasMemory(MemoryModuleType.NEAREST_PLAYER_HOLDING_WANTED_ITEM);
    }

    private static void eat(EntityPiglin piglin) {
        piglin.getBehaviorController().setMemoryWithExpiry(MemoryModuleType.ATE_RECENTLY, true, 200L);
    }

    private static Vec3D getRandomNearbyPos(EntityPiglin piglin) {
        Vec3D vec3 = LandRandomPos.getPos(piglin, 4, 2);
        return vec3 == null ? piglin.getPositionVector() : vec3;
    }

    private static boolean hasEatenRecently(EntityPiglin piglin) {
        return piglin.getBehaviorController().hasMemory(MemoryModuleType.ATE_RECENTLY);
    }

    protected static boolean isIdle(EntityPiglinAbstract piglin) {
        return piglin.getBehaviorController().isActive(Activity.IDLE);
    }

    private static boolean hasCrossbow(EntityLiving piglin) {
        return piglin.isHolding(Items.CROSSBOW);
    }

    private static void admireGoldItem(EntityLiving entity) {
        entity.getBehaviorController().setMemoryWithExpiry(MemoryModuleType.ADMIRING_ITEM, true, 120L);
    }

    private static boolean isAdmiringItem(EntityPiglin entity) {
        return entity.getBehaviorController().hasMemory(MemoryModuleType.ADMIRING_ITEM);
    }

    private static boolean isBarterCurrency(ItemStack stack) {
        return stack.is(BARTERING_ITEM);
    }

    private static boolean isFood(ItemStack stack) {
        return stack.is(TagsItem.PIGLIN_FOOD);
    }

    private static boolean isNearRepellent(EntityPiglin piglin) {
        return piglin.getBehaviorController().hasMemory(MemoryModuleType.NEAREST_REPELLENT);
    }

    private static boolean seesPlayerHoldingLovedItem(EntityLiving entity) {
        return entity.getBehaviorController().hasMemory(MemoryModuleType.NEAREST_PLAYER_HOLDING_WANTED_ITEM);
    }

    private static boolean doesntSeeAnyPlayerHoldingLovedItem(EntityLiving piglin) {
        return !seesPlayerHoldingLovedItem(piglin);
    }

    public static boolean isPlayerHoldingLovedItem(EntityLiving target) {
        return target.getEntityType() == EntityTypes.PLAYER && target.isHolding(PiglinAI::isLovedItem);
    }

    private static boolean isAdmiringDisabled(EntityPiglin piglin) {
        return piglin.getBehaviorController().hasMemory(MemoryModuleType.ADMIRING_DISABLED);
    }

    private static boolean wasHurtRecently(EntityLiving piglin) {
        return piglin.getBehaviorController().hasMemory(MemoryModuleType.HURT_BY);
    }

    private static boolean isHoldingItemInOffHand(EntityPiglin piglin) {
        return !piglin.getItemInOffHand().isEmpty();
    }

    private static boolean isNotHoldingLovedItemInOffHand(EntityPiglin piglin) {
        return piglin.getItemInOffHand().isEmpty() || !isLovedItem(piglin.getItemInOffHand());
    }

    public static boolean isZombified(EntityTypes<?> entityType) {
        return entityType == EntityTypes.ZOMBIFIED_PIGLIN || entityType == EntityTypes.ZOGLIN;
    }
}
