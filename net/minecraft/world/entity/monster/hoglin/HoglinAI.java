package net.minecraft.world.entity.monster.hoglin;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPosition;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.util.TimeRange;
import net.minecraft.util.valueproviders.IntProviderUniform;
import net.minecraft.world.entity.EntityAgeable;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.BehaviorController;
import net.minecraft.world.entity.ai.behavior.BehaviorAttack;
import net.minecraft.world.entity.ai.behavior.BehaviorAttackTargetForget;
import net.minecraft.world.entity.ai.behavior.BehaviorAttackTargetSet;
import net.minecraft.world.entity.ai.behavior.BehaviorFollowAdult;
import net.minecraft.world.entity.ai.behavior.BehaviorGateSingle;
import net.minecraft.world.entity.ai.behavior.BehaviorLook;
import net.minecraft.world.entity.ai.behavior.BehaviorLookTarget;
import net.minecraft.world.entity.ai.behavior.BehaviorLookWalk;
import net.minecraft.world.entity.ai.behavior.BehaviorMakeLoveAnimal;
import net.minecraft.world.entity.ai.behavior.BehaviorNop;
import net.minecraft.world.entity.ai.behavior.BehaviorPacify;
import net.minecraft.world.entity.ai.behavior.BehaviorRemoveMemory;
import net.minecraft.world.entity.ai.behavior.BehaviorRunIf;
import net.minecraft.world.entity.ai.behavior.BehaviorRunSometimes;
import net.minecraft.world.entity.ai.behavior.BehaviorStrollRandomUnconstrained;
import net.minecraft.world.entity.ai.behavior.BehaviorUtil;
import net.minecraft.world.entity.ai.behavior.BehaviorWalkAway;
import net.minecraft.world.entity.ai.behavior.BehaviorWalkAwayOutOfRange;
import net.minecraft.world.entity.ai.behavior.BehavorMove;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.schedule.Activity;

public class HoglinAI {
    public static final int REPELLENT_DETECTION_RANGE_HORIZONTAL = 8;
    public static final int REPELLENT_DETECTION_RANGE_VERTICAL = 4;
    private static final IntProviderUniform RETREAT_DURATION = TimeRange.rangeOfSeconds(5, 20);
    private static final int ATTACK_DURATION = 200;
    private static final int DESIRED_DISTANCE_FROM_PIGLIN_WHEN_IDLING = 8;
    private static final int DESIRED_DISTANCE_FROM_PIGLIN_WHEN_RETREATING = 15;
    private static final int ATTACK_INTERVAL = 40;
    private static final int BABY_ATTACK_INTERVAL = 15;
    private static final int REPELLENT_PACIFY_TIME = 200;
    private static final IntProviderUniform ADULT_FOLLOW_RANGE = IntProviderUniform.of(5, 16);
    private static final float SPEED_MULTIPLIER_WHEN_AVOIDING_REPELLENT = 1.0F;
    private static final float SPEED_MULTIPLIER_WHEN_RETREATING = 1.3F;
    private static final float SPEED_MULTIPLIER_WHEN_MAKING_LOVE = 0.6F;
    private static final float SPEED_MULTIPLIER_WHEN_IDLING = 0.4F;
    private static final float SPEED_MULTIPLIER_WHEN_FOLLOWING_ADULT = 0.6F;

    protected static BehaviorController<?> makeBrain(BehaviorController<EntityHoglin> brain) {
        initCoreActivity(brain);
        initIdleActivity(brain);
        initFightActivity(brain);
        initRetreatActivity(brain);
        brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.useDefaultActivity();
        return brain;
    }

    private static void initCoreActivity(BehaviorController<EntityHoglin> brain) {
        brain.addActivity(Activity.CORE, 0, ImmutableList.of(new BehaviorLook(45, 90), new BehavorMove()));
    }

    private static void initIdleActivity(BehaviorController<EntityHoglin> brain) {
        brain.addActivity(Activity.IDLE, 10, ImmutableList.of(new BehaviorPacify(MemoryModuleType.NEAREST_REPELLENT, 200), new BehaviorMakeLoveAnimal(EntityTypes.HOGLIN, 0.6F), BehaviorWalkAway.pos(MemoryModuleType.NEAREST_REPELLENT, 1.0F, 8, true), new BehaviorAttackTargetSet<EntityHoglin>(HoglinAI::findNearestValidAttackTarget), new BehaviorRunIf<EntityHoglin>(EntityHoglin::isAdult, BehaviorWalkAway.entity(MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLIN, 0.4F, 8, false)), new BehaviorRunSometimes<EntityLiving>(new BehaviorLookTarget(8.0F), IntProviderUniform.of(30, 60)), new BehaviorFollowAdult(ADULT_FOLLOW_RANGE, 0.6F), createIdleMovementBehaviors()));
    }

    private static void initFightActivity(BehaviorController<EntityHoglin> brain) {
        brain.addActivityAndRemoveMemoryWhenStopped(Activity.FIGHT, 10, ImmutableList.of(new BehaviorPacify(MemoryModuleType.NEAREST_REPELLENT, 200), new BehaviorMakeLoveAnimal(EntityTypes.HOGLIN, 0.6F), new BehaviorWalkAwayOutOfRange(1.0F), new BehaviorRunIf<>(EntityHoglin::isAdult, new BehaviorAttack(40)), new BehaviorRunIf<>(EntityAgeable::isBaby, new BehaviorAttack(15)), new BehaviorAttackTargetForget(), new BehaviorRemoveMemory(HoglinAI::isBreeding, MemoryModuleType.ATTACK_TARGET)), MemoryModuleType.ATTACK_TARGET);
    }

    private static void initRetreatActivity(BehaviorController<EntityHoglin> brain) {
        brain.addActivityAndRemoveMemoryWhenStopped(Activity.AVOID, 10, ImmutableList.of(BehaviorWalkAway.entity(MemoryModuleType.AVOID_TARGET, 1.3F, 15, false), createIdleMovementBehaviors(), new BehaviorRunSometimes<EntityLiving>(new BehaviorLookTarget(8.0F), IntProviderUniform.of(30, 60)), new BehaviorRemoveMemory<EntityHoglin>(HoglinAI::wantsToStopFleeing, MemoryModuleType.AVOID_TARGET)), MemoryModuleType.AVOID_TARGET);
    }

    private static BehaviorGateSingle<EntityHoglin> createIdleMovementBehaviors() {
        return new BehaviorGateSingle<>(ImmutableList.of(Pair.of(new BehaviorStrollRandomUnconstrained(0.4F), 2), Pair.of(new BehaviorLookWalk(0.4F, 3), 2), Pair.of(new BehaviorNop(30, 60), 1)));
    }

    protected static void updateActivity(EntityHoglin hoglin) {
        BehaviorController<EntityHoglin> brain = hoglin.getBehaviorController();
        Activity activity = brain.getActiveNonCoreActivity().orElse((Activity)null);
        brain.setActiveActivityToFirstValid(ImmutableList.of(Activity.FIGHT, Activity.AVOID, Activity.IDLE));
        Activity activity2 = brain.getActiveNonCoreActivity().orElse((Activity)null);
        if (activity != activity2) {
            getSoundForCurrentActivity(hoglin).ifPresent(hoglin::playSound);
        }

        hoglin.setAggressive(brain.hasMemory(MemoryModuleType.ATTACK_TARGET));
    }

    protected static void onHitTarget(EntityHoglin hoglin, EntityLiving target) {
        if (!hoglin.isBaby()) {
            if (target.getEntityType() == EntityTypes.PIGLIN && piglinsOutnumberHoglins(hoglin)) {
                setAvoidTarget(hoglin, target);
                broadcastRetreat(hoglin, target);
            } else {
                broadcastAttackTarget(hoglin, target);
            }
        }
    }

    private static void broadcastRetreat(EntityHoglin hoglin, EntityLiving target) {
        getVisibleAdultHoglins(hoglin).forEach((hoglinx) -> {
            retreatFromNearestTarget(hoglinx, target);
        });
    }

    private static void retreatFromNearestTarget(EntityHoglin hoglin, EntityLiving target) {
        BehaviorController<EntityHoglin> brain = hoglin.getBehaviorController();
        EntityLiving livingEntity = BehaviorUtil.getNearestTarget(hoglin, brain.getMemory(MemoryModuleType.AVOID_TARGET), target);
        livingEntity = BehaviorUtil.getNearestTarget(hoglin, brain.getMemory(MemoryModuleType.ATTACK_TARGET), livingEntity);
        setAvoidTarget(hoglin, livingEntity);
    }

    private static void setAvoidTarget(EntityHoglin hoglin, EntityLiving target) {
        hoglin.getBehaviorController().removeMemory(MemoryModuleType.ATTACK_TARGET);
        hoglin.getBehaviorController().removeMemory(MemoryModuleType.WALK_TARGET);
        hoglin.getBehaviorController().setMemoryWithExpiry(MemoryModuleType.AVOID_TARGET, target, (long)RETREAT_DURATION.sample(hoglin.level.random));
    }

    private static Optional<? extends EntityLiving> findNearestValidAttackTarget(EntityHoglin hoglin) {
        return !isPacified(hoglin) && !isBreeding(hoglin) ? hoglin.getBehaviorController().getMemory(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER) : Optional.empty();
    }

    static boolean isPosNearNearestRepellent(EntityHoglin hoglin, BlockPosition pos) {
        Optional<BlockPosition> optional = hoglin.getBehaviorController().getMemory(MemoryModuleType.NEAREST_REPELLENT);
        return optional.isPresent() && optional.get().closerThan(pos, 8.0D);
    }

    private static boolean wantsToStopFleeing(EntityHoglin hoglin) {
        return hoglin.isAdult() && !piglinsOutnumberHoglins(hoglin);
    }

    private static boolean piglinsOutnumberHoglins(EntityHoglin hoglin) {
        if (hoglin.isBaby()) {
            return false;
        } else {
            int i = hoglin.getBehaviorController().getMemory(MemoryModuleType.VISIBLE_ADULT_PIGLIN_COUNT).orElse(0);
            int j = hoglin.getBehaviorController().getMemory(MemoryModuleType.VISIBLE_ADULT_HOGLIN_COUNT).orElse(0) + 1;
            return i > j;
        }
    }

    protected static void wasHurtBy(EntityHoglin hoglin, EntityLiving attacker) {
        BehaviorController<EntityHoglin> brain = hoglin.getBehaviorController();
        brain.removeMemory(MemoryModuleType.PACIFIED);
        brain.removeMemory(MemoryModuleType.BREED_TARGET);
        if (hoglin.isBaby()) {
            retreatFromNearestTarget(hoglin, attacker);
        } else {
            maybeRetaliate(hoglin, attacker);
        }
    }

    private static void maybeRetaliate(EntityHoglin hoglin, EntityLiving target) {
        if (!hoglin.getBehaviorController().isActive(Activity.AVOID) || target.getEntityType() != EntityTypes.PIGLIN) {
            if (target.getEntityType() != EntityTypes.HOGLIN) {
                if (!BehaviorUtil.isOtherTargetMuchFurtherAwayThanCurrentAttackTarget(hoglin, target, 4.0D)) {
                    if (Sensor.isEntityAttackable(hoglin, target)) {
                        setAttackTarget(hoglin, target);
                        broadcastAttackTarget(hoglin, target);
                    }
                }
            }
        }
    }

    private static void setAttackTarget(EntityHoglin hoglin, EntityLiving target) {
        BehaviorController<EntityHoglin> brain = hoglin.getBehaviorController();
        brain.removeMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        brain.removeMemory(MemoryModuleType.BREED_TARGET);
        brain.setMemoryWithExpiry(MemoryModuleType.ATTACK_TARGET, target, 200L);
    }

    private static void broadcastAttackTarget(EntityHoglin hoglin, EntityLiving target) {
        getVisibleAdultHoglins(hoglin).forEach((hoglinx) -> {
            setAttackTargetIfCloserThanCurrent(hoglinx, target);
        });
    }

    private static void setAttackTargetIfCloserThanCurrent(EntityHoglin hoglin, EntityLiving targetCandidate) {
        if (!isPacified(hoglin)) {
            Optional<EntityLiving> optional = hoglin.getBehaviorController().getMemory(MemoryModuleType.ATTACK_TARGET);
            EntityLiving livingEntity = BehaviorUtil.getNearestTarget(hoglin, optional, targetCandidate);
            setAttackTarget(hoglin, livingEntity);
        }
    }

    public static Optional<SoundEffect> getSoundForCurrentActivity(EntityHoglin hoglin) {
        return hoglin.getBehaviorController().getActiveNonCoreActivity().map((activity) -> {
            return getSoundForActivity(hoglin, activity);
        });
    }

    private static SoundEffect getSoundForActivity(EntityHoglin hoglin, Activity activity) {
        if (activity != Activity.AVOID && !hoglin.isConverting()) {
            if (activity == Activity.FIGHT) {
                return SoundEffects.HOGLIN_ANGRY;
            } else {
                return isNearRepellent(hoglin) ? SoundEffects.HOGLIN_RETREAT : SoundEffects.HOGLIN_AMBIENT;
            }
        } else {
            return SoundEffects.HOGLIN_RETREAT;
        }
    }

    private static List<EntityHoglin> getVisibleAdultHoglins(EntityHoglin hoglin) {
        return hoglin.getBehaviorController().getMemory(MemoryModuleType.NEAREST_VISIBLE_ADULT_HOGLINS).orElse(ImmutableList.of());
    }

    private static boolean isNearRepellent(EntityHoglin hoglin) {
        return hoglin.getBehaviorController().hasMemory(MemoryModuleType.NEAREST_REPELLENT);
    }

    private static boolean isBreeding(EntityHoglin hoglin) {
        return hoglin.getBehaviorController().hasMemory(MemoryModuleType.BREED_TARGET);
    }

    protected static boolean isPacified(EntityHoglin hoglin) {
        return hoglin.getBehaviorController().hasMemory(MemoryModuleType.PACIFIED);
    }
}
