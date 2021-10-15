package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.BehaviorController;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.MemoryTarget;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3D;

public class BehaviorPlay extends Behavior<EntityCreature> {
    private static final int MAX_FLEE_XZ_DIST = 20;
    private static final int MAX_FLEE_Y_DIST = 8;
    private static final float FLEE_SPEED_MODIFIER = 0.6F;
    private static final float CHASE_SPEED_MODIFIER = 0.6F;
    private static final int MAX_CHASERS_PER_TARGET = 5;
    private static final int AVERAGE_WAIT_TIME_BETWEEN_RUNS = 10;

    public BehaviorPlay() {
        super(ImmutableMap.of(MemoryModuleType.VISIBLE_VILLAGER_BABIES, MemoryStatus.VALUE_PRESENT, MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.INTERACTION_TARGET, MemoryStatus.REGISTERED));
    }

    @Override
    protected boolean checkExtraStartConditions(WorldServer world, EntityCreature entity) {
        return world.getRandom().nextInt(10) == 0 && this.hasFriendsNearby(entity);
    }

    @Override
    protected void start(WorldServer world, EntityCreature entity, long time) {
        EntityLiving livingEntity = this.seeIfSomeoneIsChasingMe(entity);
        if (livingEntity != null) {
            this.fleeFromChaser(world, entity, livingEntity);
        } else {
            Optional<EntityLiving> optional = this.findSomeoneBeingChased(entity);
            if (optional.isPresent()) {
                chaseKid(entity, optional.get());
            } else {
                this.findSomeoneToChase(entity).ifPresent((livingEntityx) -> {
                    chaseKid(entity, livingEntityx);
                });
            }
        }
    }

    private void fleeFromChaser(WorldServer world, EntityCreature entity, EntityLiving unusedBaby) {
        for(int i = 0; i < 10; ++i) {
            Vec3D vec3 = LandRandomPos.getPos(entity, 20, 8);
            if (vec3 != null && world.isVillage(new BlockPosition(vec3))) {
                entity.getBehaviorController().setMemory(MemoryModuleType.WALK_TARGET, new MemoryTarget(vec3, 0.6F, 0));
                return;
            }
        }

    }

    private static void chaseKid(EntityCreature entity, EntityLiving target) {
        BehaviorController<?> brain = entity.getBehaviorController();
        brain.setMemory(MemoryModuleType.INTERACTION_TARGET, target);
        brain.setMemory(MemoryModuleType.LOOK_TARGET, new BehaviorPositionEntity(target, true));
        brain.setMemory(MemoryModuleType.WALK_TARGET, new MemoryTarget(new BehaviorPositionEntity(target, false), 0.6F, 1));
    }

    private Optional<EntityLiving> findSomeoneToChase(EntityCreature entity) {
        return this.getFriendsNearby(entity).stream().findAny();
    }

    private Optional<EntityLiving> findSomeoneBeingChased(EntityCreature entity) {
        Map<EntityLiving, Integer> map = this.checkHowManyChasersEachFriendHas(entity);
        return map.entrySet().stream().sorted(Comparator.comparingInt(Entry::getValue)).filter((entry) -> {
            return entry.getValue() > 0 && entry.getValue() <= 5;
        }).map(Entry::getKey).findFirst();
    }

    private Map<EntityLiving, Integer> checkHowManyChasersEachFriendHas(EntityCreature entity) {
        Map<EntityLiving, Integer> map = Maps.newHashMap();
        this.getFriendsNearby(entity).stream().filter(this::isChasingSomeone).forEach((livingEntity) -> {
            map.compute(this.whoAreYouChasing(livingEntity), (livingEntityx, integer) -> {
                return integer == null ? 1 : integer + 1;
            });
        });
        return map;
    }

    private List<EntityLiving> getFriendsNearby(EntityCreature entity) {
        return entity.getBehaviorController().getMemory(MemoryModuleType.VISIBLE_VILLAGER_BABIES).get();
    }

    private EntityLiving whoAreYouChasing(EntityLiving entity) {
        return entity.getBehaviorController().getMemory(MemoryModuleType.INTERACTION_TARGET).get();
    }

    @Nullable
    private EntityLiving seeIfSomeoneIsChasingMe(EntityLiving entity) {
        return entity.getBehaviorController().getMemory(MemoryModuleType.VISIBLE_VILLAGER_BABIES).get().stream().filter((livingEntity2) -> {
            return this.isFriendChasingMe(entity, livingEntity2);
        }).findAny().orElse((EntityLiving)null);
    }

    private boolean isChasingSomeone(EntityLiving entity) {
        return entity.getBehaviorController().getMemory(MemoryModuleType.INTERACTION_TARGET).isPresent();
    }

    private boolean isFriendChasingMe(EntityLiving entity, EntityLiving other) {
        return other.getBehaviorController().getMemory(MemoryModuleType.INTERACTION_TARGET).filter((livingEntity2) -> {
            return livingEntity2 == entity;
        }).isPresent();
    }

    private boolean hasFriendsNearby(EntityCreature entity) {
        return entity.getBehaviorController().hasMemory(MemoryModuleType.VISIBLE_VILLAGER_BABIES);
    }
}
