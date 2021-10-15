package net.minecraft.world.entity.animal.axolotl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import java.util.Optional;
import net.minecraft.core.BlockPosition;
import net.minecraft.tags.TagsItem;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.BehaviorController;
import net.minecraft.world.entity.ai.behavior.BehaviorAttack;
import net.minecraft.world.entity.ai.behavior.BehaviorAttackTargetForget;
import net.minecraft.world.entity.ai.behavior.BehaviorAttackTargetSet;
import net.minecraft.world.entity.ai.behavior.BehaviorFollowAdult;
import net.minecraft.world.entity.ai.behavior.BehaviorGate;
import net.minecraft.world.entity.ai.behavior.BehaviorGateSingle;
import net.minecraft.world.entity.ai.behavior.BehaviorLook;
import net.minecraft.world.entity.ai.behavior.BehaviorLookTarget;
import net.minecraft.world.entity.ai.behavior.BehaviorLookWalk;
import net.minecraft.world.entity.ai.behavior.BehaviorMakeLoveAnimal;
import net.minecraft.world.entity.ai.behavior.BehaviorNop;
import net.minecraft.world.entity.ai.behavior.BehaviorPosition;
import net.minecraft.world.entity.ai.behavior.BehaviorRandomSwim;
import net.minecraft.world.entity.ai.behavior.BehaviorRemoveMemory;
import net.minecraft.world.entity.ai.behavior.BehaviorRunIf;
import net.minecraft.world.entity.ai.behavior.BehaviorRunSometimes;
import net.minecraft.world.entity.ai.behavior.BehaviorStrollRandomUnconstrained;
import net.minecraft.world.entity.ai.behavior.BehaviorWalkAwayOutOfRange;
import net.minecraft.world.entity.ai.behavior.BehavorMove;
import net.minecraft.world.entity.ai.behavior.CountDownCooldownTicks;
import net.minecraft.world.entity.ai.behavior.FollowTemptation;
import net.minecraft.world.entity.ai.behavior.TryFindWater;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.crafting.RecipeItemStack;
import net.minecraft.world.level.World;

public class AxolotlAI {
    private static final UniformInt ADULT_FOLLOW_RANGE = UniformInt.of(5, 16);
    private static final float SPEED_MULTIPLIER_WHEN_MAKING_LOVE = 0.2F;
    private static final float SPEED_MULTIPLIER_ON_LAND = 0.15F;
    private static final float SPEED_MULTIPLIER_WHEN_IDLING_IN_WATER = 0.5F;
    private static final float SPEED_MULTIPLIER_WHEN_CHASING_IN_WATER = 0.6F;
    private static final float SPEED_MULTIPLIER_WHEN_FOLLOWING_ADULT_IN_WATER = 0.6F;

    protected static BehaviorController<?> makeBrain(BehaviorController<EntityAxolotl> brain) {
        initCoreActivity(brain);
        initIdleActivity(brain);
        initFightActivity(brain);
        initPlayDeadActivity(brain);
        brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.useDefaultActivity();
        return brain;
    }

    private static void initPlayDeadActivity(BehaviorController<EntityAxolotl> brain) {
        brain.addActivityAndRemoveMemoriesWhenStopped(Activity.PLAY_DEAD, ImmutableList.of(Pair.of(0, new PlayDead()), Pair.of(1, new BehaviorRemoveMemory<>(AxolotlAI::isBreeding, MemoryModuleType.PLAY_DEAD_TICKS))), ImmutableSet.of(Pair.of(MemoryModuleType.PLAY_DEAD_TICKS, MemoryStatus.VALUE_PRESENT)), ImmutableSet.of(MemoryModuleType.PLAY_DEAD_TICKS));
    }

    private static void initFightActivity(BehaviorController<EntityAxolotl> brain) {
        brain.addActivityAndRemoveMemoryWhenStopped(Activity.FIGHT, 0, ImmutableList.of(new BehaviorAttackTargetForget<>(EntityAxolotl::onStopAttacking), new BehaviorWalkAwayOutOfRange(AxolotlAI::getSpeedModifierChasing), new BehaviorAttack(20), new BehaviorRemoveMemory(AxolotlAI::isBreeding, MemoryModuleType.ATTACK_TARGET)), MemoryModuleType.ATTACK_TARGET);
    }

    private static void initCoreActivity(BehaviorController<EntityAxolotl> brain) {
        brain.addActivity(Activity.CORE, 0, ImmutableList.of(new BehaviorLook(45, 90), new BehavorMove(), new ValidatePlayDead(), new CountDownCooldownTicks(MemoryModuleType.TEMPTATION_COOLDOWN_TICKS)));
    }

    private static void initIdleActivity(BehaviorController<EntityAxolotl> brain) {
        brain.addActivity(Activity.IDLE, ImmutableList.of(Pair.of(0, new BehaviorRunSometimes<>(new BehaviorLookTarget(EntityTypes.PLAYER, 6.0F), UniformInt.of(30, 60))), Pair.of(1, new BehaviorMakeLoveAnimal(EntityTypes.AXOLOTL, 0.2F)), Pair.of(2, new BehaviorGateSingle<>(ImmutableList.of(Pair.of(new FollowTemptation(AxolotlAI::getSpeedModifier), 1), Pair.of(new BehaviorFollowAdult<>(ADULT_FOLLOW_RANGE, AxolotlAI::getSpeedModifierFollowingAdult), 1)))), Pair.of(3, new BehaviorAttackTargetSet<>(AxolotlAI::findNearestValidAttackTarget)), Pair.of(3, new TryFindWater(6, 0.15F)), Pair.of(4, new BehaviorGate<>(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT), ImmutableSet.of(), BehaviorGate.Order.ORDERED, BehaviorGate.Execution.TRY_ALL, ImmutableList.of(Pair.of(new BehaviorRandomSwim(0.5F), 2), Pair.of(new BehaviorStrollRandomUnconstrained(0.15F, false), 2), Pair.of(new BehaviorLookWalk(AxolotlAI::canSetWalkTargetFromLookTarget, AxolotlAI::getSpeedModifier, 3), 3), Pair.of(new BehaviorRunIf<>(Entity::isInWaterOrBubble, new BehaviorNop(30, 60)), 5), Pair.of(new BehaviorRunIf<>(Entity::isOnGround, new BehaviorNop(200, 400)), 5))))));
    }

    private static boolean canSetWalkTargetFromLookTarget(EntityLiving entity) {
        World level = entity.level;
        Optional<BehaviorPosition> optional = entity.getBehaviorController().getMemory(MemoryModuleType.LOOK_TARGET);
        if (optional.isPresent()) {
            BlockPosition blockPos = optional.get().currentBlockPosition();
            return level.isWaterAt(blockPos) == entity.isInWaterOrBubble();
        } else {
            return false;
        }
    }

    public static void updateActivity(EntityAxolotl axolotl) {
        BehaviorController<EntityAxolotl> brain = axolotl.getBehaviorController();
        Activity activity = brain.getActiveNonCoreActivity().orElse((Activity)null);
        if (activity != Activity.PLAY_DEAD) {
            brain.setActiveActivityToFirstValid(ImmutableList.of(Activity.PLAY_DEAD, Activity.FIGHT, Activity.IDLE));
            if (activity == Activity.FIGHT && brain.getActiveNonCoreActivity().orElse((Activity)null) != Activity.FIGHT) {
                brain.setMemoryWithExpiry(MemoryModuleType.HAS_HUNTING_COOLDOWN, true, 2400L);
            }
        }

    }

    private static float getSpeedModifierChasing(EntityLiving entity) {
        return entity.isInWaterOrBubble() ? 0.6F : 0.15F;
    }

    private static float getSpeedModifierFollowingAdult(EntityLiving entity) {
        return entity.isInWaterOrBubble() ? 0.6F : 0.15F;
    }

    private static float getSpeedModifier(EntityLiving entity) {
        return entity.isInWaterOrBubble() ? 0.5F : 0.15F;
    }

    private static Optional<? extends EntityLiving> findNearestValidAttackTarget(EntityAxolotl axolotl) {
        return isBreeding(axolotl) ? Optional.empty() : axolotl.getBehaviorController().getMemory(MemoryModuleType.NEAREST_ATTACKABLE);
    }

    private static boolean isBreeding(EntityAxolotl axolotl) {
        return axolotl.getBehaviorController().hasMemory(MemoryModuleType.BREED_TARGET);
    }

    public static RecipeItemStack getTemptations() {
        return RecipeItemStack.of(TagsItem.AXOLOTL_TEMPT_ITEMS);
    }
}
