package net.minecraft.world.entity.monster.piglin;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.BehaviorController;
import net.minecraft.world.entity.ai.behavior.BehaviorAttack;
import net.minecraft.world.entity.ai.behavior.BehaviorAttackTargetForget;
import net.minecraft.world.entity.ai.behavior.BehaviorAttackTargetSet;
import net.minecraft.world.entity.ai.behavior.BehaviorForgetAnger;
import net.minecraft.world.entity.ai.behavior.BehaviorGateSingle;
import net.minecraft.world.entity.ai.behavior.BehaviorInteract;
import net.minecraft.world.entity.ai.behavior.BehaviorInteractDoor;
import net.minecraft.world.entity.ai.behavior.BehaviorLook;
import net.minecraft.world.entity.ai.behavior.BehaviorLookInteract;
import net.minecraft.world.entity.ai.behavior.BehaviorLookTarget;
import net.minecraft.world.entity.ai.behavior.BehaviorNop;
import net.minecraft.world.entity.ai.behavior.BehaviorStrollPlace;
import net.minecraft.world.entity.ai.behavior.BehaviorStrollPosition;
import net.minecraft.world.entity.ai.behavior.BehaviorStrollRandomUnconstrained;
import net.minecraft.world.entity.ai.behavior.BehaviorUtil;
import net.minecraft.world.entity.ai.behavior.BehaviorWalkAwayOutOfRange;
import net.minecraft.world.entity.ai.behavior.BehavorMove;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.schedule.Activity;

public class PiglinBruteAI {
    private static final int ANGER_DURATION = 600;
    private static final int MELEE_ATTACK_COOLDOWN = 20;
    private static final double ACTIVITY_SOUND_LIKELIHOOD_PER_TICK = 0.0125D;
    private static final int MAX_LOOK_DIST = 8;
    private static final int INTERACTION_RANGE = 8;
    private static final double TARGETING_RANGE = 12.0D;
    private static final float SPEED_MULTIPLIER_WHEN_IDLING = 0.6F;
    private static final int HOME_CLOSE_ENOUGH_DISTANCE = 2;
    private static final int HOME_TOO_FAR_DISTANCE = 100;
    private static final int HOME_STROLL_AROUND_DISTANCE = 5;

    protected static BehaviorController<?> makeBrain(EntityPiglinBrute piglinBrute, BehaviorController<EntityPiglinBrute> brain) {
        initCoreActivity(piglinBrute, brain);
        initIdleActivity(piglinBrute, brain);
        initFightActivity(piglinBrute, brain);
        brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.useDefaultActivity();
        return brain;
    }

    protected static void initMemories(EntityPiglinBrute piglinBrute) {
        GlobalPos globalPos = GlobalPos.create(piglinBrute.level.getDimensionKey(), piglinBrute.getChunkCoordinates());
        piglinBrute.getBehaviorController().setMemory(MemoryModuleType.HOME, globalPos);
    }

    private static void initCoreActivity(EntityPiglinBrute piglinBrute, BehaviorController<EntityPiglinBrute> brain) {
        brain.addActivity(Activity.CORE, 0, ImmutableList.of(new BehaviorLook(45, 90), new BehavorMove(), new BehaviorInteractDoor(), new BehaviorForgetAnger<>()));
    }

    private static void initIdleActivity(EntityPiglinBrute piglinBrute, BehaviorController<EntityPiglinBrute> brain) {
        brain.addActivity(Activity.IDLE, 10, ImmutableList.of(new BehaviorAttackTargetSet<>(PiglinBruteAI::findNearestValidAttackTarget), createIdleLookBehaviors(), createIdleMovementBehaviors(), new BehaviorLookInteract(EntityTypes.PLAYER, 4)));
    }

    private static void initFightActivity(EntityPiglinBrute piglinBrute, BehaviorController<EntityPiglinBrute> brain) {
        brain.addActivityAndRemoveMemoryWhenStopped(Activity.FIGHT, 10, ImmutableList.of(new BehaviorAttackTargetForget<>((livingEntity) -> {
            return !isNearestValidAttackTarget(piglinBrute, livingEntity);
        }), new BehaviorWalkAwayOutOfRange(1.0F), new BehaviorAttack(20)), MemoryModuleType.ATTACK_TARGET);
    }

    private static BehaviorGateSingle<EntityPiglinBrute> createIdleLookBehaviors() {
        return new BehaviorGateSingle<>(ImmutableList.of(Pair.of(new BehaviorLookTarget(EntityTypes.PLAYER, 8.0F), 1), Pair.of(new BehaviorLookTarget(EntityTypes.PIGLIN, 8.0F), 1), Pair.of(new BehaviorLookTarget(EntityTypes.PIGLIN_BRUTE, 8.0F), 1), Pair.of(new BehaviorLookTarget(8.0F), 1), Pair.of(new BehaviorNop(30, 60), 1)));
    }

    private static BehaviorGateSingle<EntityPiglinBrute> createIdleMovementBehaviors() {
        return new BehaviorGateSingle<>(ImmutableList.of(Pair.of(new BehaviorStrollRandomUnconstrained(0.6F), 2), Pair.of(BehaviorInteract.of(EntityTypes.PIGLIN, 8, MemoryModuleType.INTERACTION_TARGET, 0.6F, 2), 2), Pair.of(BehaviorInteract.of(EntityTypes.PIGLIN_BRUTE, 8, MemoryModuleType.INTERACTION_TARGET, 0.6F, 2), 2), Pair.of(new BehaviorStrollPlace(MemoryModuleType.HOME, 0.6F, 2, 100), 2), Pair.of(new BehaviorStrollPosition(MemoryModuleType.HOME, 0.6F, 5), 2), Pair.of(new BehaviorNop(30, 60), 1)));
    }

    protected static void updateActivity(EntityPiglinBrute piglinBrute) {
        BehaviorController<EntityPiglinBrute> brain = piglinBrute.getBehaviorController();
        Activity activity = brain.getActiveNonCoreActivity().orElse((Activity)null);
        brain.setActiveActivityToFirstValid(ImmutableList.of(Activity.FIGHT, Activity.IDLE));
        Activity activity2 = brain.getActiveNonCoreActivity().orElse((Activity)null);
        if (activity != activity2) {
            playActivitySound(piglinBrute);
        }

        piglinBrute.setAggressive(brain.hasMemory(MemoryModuleType.ATTACK_TARGET));
    }

    private static boolean isNearestValidAttackTarget(EntityPiglinAbstract piglin, EntityLiving livingEntity) {
        return findNearestValidAttackTarget(piglin).filter((livingEntity2) -> {
            return livingEntity2 == livingEntity;
        }).isPresent();
    }

    private static Optional<? extends EntityLiving> findNearestValidAttackTarget(EntityPiglinAbstract piglin) {
        Optional<EntityLiving> optional = BehaviorUtil.getLivingEntityFromUUIDMemory(piglin, MemoryModuleType.ANGRY_AT);
        if (optional.isPresent() && Sensor.isEntityAttackableIgnoringLineOfSight(piglin, optional.get())) {
            return optional;
        } else {
            Optional<? extends EntityLiving> optional2 = getTargetIfWithinRange(piglin, MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER);
            return optional2.isPresent() ? optional2 : piglin.getBehaviorController().getMemory(MemoryModuleType.NEAREST_VISIBLE_NEMESIS);
        }
    }

    private static Optional<? extends EntityLiving> getTargetIfWithinRange(EntityPiglinAbstract piglin, MemoryModuleType<? extends EntityLiving> memoryModuleType) {
        return piglin.getBehaviorController().getMemory(memoryModuleType).filter((livingEntity) -> {
            return livingEntity.closerThan(piglin, 12.0D);
        });
    }

    protected static void wasHurtBy(EntityPiglinBrute piglinBrute, EntityLiving target) {
        if (!(target instanceof EntityPiglinAbstract)) {
            PiglinAI.maybeRetaliate(piglinBrute, target);
        }
    }

    protected static void setAngerTarget(EntityPiglinBrute piglinBrute, EntityLiving livingEntity) {
        piglinBrute.getBehaviorController().removeMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        piglinBrute.getBehaviorController().setMemoryWithExpiry(MemoryModuleType.ANGRY_AT, livingEntity.getUniqueID(), 600L);
    }

    protected static void maybePlayActivitySound(EntityPiglinBrute piglinBrute) {
        if ((double)piglinBrute.level.random.nextFloat() < 0.0125D) {
            playActivitySound(piglinBrute);
        }

    }

    private static void playActivitySound(EntityPiglinBrute piglinBrute) {
        piglinBrute.getBehaviorController().getActiveNonCoreActivity().ifPresent((activity) -> {
            if (activity == Activity.FIGHT) {
                piglinBrute.playAngrySound();
            }

        });
    }
}
