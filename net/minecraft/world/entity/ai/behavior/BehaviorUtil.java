package net.minecraft.world.entity.ai.behavior;

import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.SectionPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityCreature;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.BehaviorController;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryTarget;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.npc.EntityVillager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemProjectileWeapon;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.pathfinder.PathMode;
import net.minecraft.world.phys.Vec3D;

public class BehaviorUtil {
    private BehaviorUtil() {
    }

    public static void lockGazeAndWalkToEachOther(EntityLiving first, EntityLiving second, float speed) {
        lookAtEachOther(first, second);
        setWalkAndLookTargetMemoriesToEachOther(first, second, speed);
    }

    public static boolean entityIsVisible(BehaviorController<?> brain, EntityLiving target) {
        Optional<NearestVisibleLivingEntities> optional = brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
        return optional.isPresent() && optional.get().contains(target);
    }

    public static boolean targetIsValid(BehaviorController<?> brain, MemoryModuleType<? extends EntityLiving> memoryModuleType, EntityTypes<?> entityType) {
        return targetIsValid(brain, memoryModuleType, (entity) -> {
            return entity.getEntityType() == entityType;
        });
    }

    private static boolean targetIsValid(BehaviorController<?> brain, MemoryModuleType<? extends EntityLiving> memoryType, Predicate<EntityLiving> filter) {
        return brain.getMemory(memoryType).filter(filter).filter(EntityLiving::isAlive).filter((target) -> {
            return entityIsVisible(brain, target);
        }).isPresent();
    }

    private static void lookAtEachOther(EntityLiving first, EntityLiving second) {
        lookAtEntity(first, second);
        lookAtEntity(second, first);
    }

    public static void lookAtEntity(EntityLiving entity, EntityLiving target) {
        entity.getBehaviorController().setMemory(MemoryModuleType.LOOK_TARGET, new BehaviorPositionEntity(target, true));
    }

    private static void setWalkAndLookTargetMemoriesToEachOther(EntityLiving first, EntityLiving second, float speed) {
        int i = 2;
        setWalkAndLookTargetMemories(first, second, speed, 2);
        setWalkAndLookTargetMemories(second, first, speed, 2);
    }

    public static void setWalkAndLookTargetMemories(EntityLiving entity, Entity target, float speed, int completionRange) {
        MemoryTarget walkTarget = new MemoryTarget(new BehaviorPositionEntity(target, false), speed, completionRange);
        entity.getBehaviorController().setMemory(MemoryModuleType.LOOK_TARGET, new BehaviorPositionEntity(target, true));
        entity.getBehaviorController().setMemory(MemoryModuleType.WALK_TARGET, walkTarget);
    }

    public static void setWalkAndLookTargetMemories(EntityLiving entity, BlockPosition target, float speed, int completionRange) {
        MemoryTarget walkTarget = new MemoryTarget(new BehaviorTarget(target), speed, completionRange);
        entity.getBehaviorController().setMemory(MemoryModuleType.LOOK_TARGET, new BehaviorTarget(target));
        entity.getBehaviorController().setMemory(MemoryModuleType.WALK_TARGET, walkTarget);
    }

    public static void throwItem(EntityLiving entity, ItemStack stack, Vec3D targetLocation) {
        double d = entity.getHeadY() - (double)0.3F;
        EntityItem itemEntity = new EntityItem(entity.level, entity.locX(), d, entity.locZ(), stack);
        float f = 0.3F;
        Vec3D vec3 = targetLocation.subtract(entity.getPositionVector());
        vec3 = vec3.normalize().scale((double)0.3F);
        itemEntity.setMot(vec3);
        itemEntity.defaultPickupDelay();
        entity.level.addEntity(itemEntity);
    }

    public static SectionPosition findSectionClosestToVillage(WorldServer world, SectionPosition center, int radius) {
        int i = world.sectionsToVillage(center);
        return SectionPosition.cube(center, radius).filter((sectionPos) -> {
            return world.sectionsToVillage(sectionPos) < i;
        }).min(Comparator.comparingInt(world::sectionsToVillage)).orElse(center);
    }

    public static boolean isWithinAttackRange(EntityInsentient mob, EntityLiving target, int rangedWeaponReachReduction) {
        Item item = mob.getItemInMainHand().getItem();
        if (item instanceof ItemProjectileWeapon) {
            ItemProjectileWeapon projectileWeaponItem = (ItemProjectileWeapon)item;
            if (mob.canFireProjectileWeapon((ItemProjectileWeapon)item)) {
                int i = projectileWeaponItem.getDefaultProjectileRange() - rangedWeaponReachReduction;
                return mob.closerThan(target, (double)i);
            }
        }

        return isWithinMeleeAttackRange(mob, target);
    }

    public static boolean isWithinMeleeAttackRange(EntityInsentient source, EntityLiving target) {
        double d = source.distanceToSqr(target.locX(), target.locY(), target.locZ());
        return d <= source.getMeleeAttackRangeSqr(target);
    }

    public static boolean isOtherTargetMuchFurtherAwayThanCurrentAttackTarget(EntityLiving source, EntityLiving target, double extraDistance) {
        Optional<EntityLiving> optional = source.getBehaviorController().getMemory(MemoryModuleType.ATTACK_TARGET);
        if (optional.isEmpty()) {
            return false;
        } else {
            double d = source.distanceToSqr(optional.get().getPositionVector());
            double e = source.distanceToSqr(target.getPositionVector());
            return e > d + extraDistance * extraDistance;
        }
    }

    public static boolean canSee(EntityLiving source, EntityLiving target) {
        BehaviorController<?> brain = source.getBehaviorController();
        return !brain.hasMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES) ? false : brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).get().contains(target);
    }

    public static EntityLiving getNearestTarget(EntityLiving source, Optional<EntityLiving> first, EntityLiving second) {
        return first.isEmpty() ? second : getTargetNearestMe(source, first.get(), second);
    }

    public static EntityLiving getTargetNearestMe(EntityLiving source, EntityLiving first, EntityLiving second) {
        Vec3D vec3 = first.getPositionVector();
        Vec3D vec32 = second.getPositionVector();
        return source.distanceToSqr(vec3) < source.distanceToSqr(vec32) ? first : second;
    }

    public static Optional<EntityLiving> getLivingEntityFromUUIDMemory(EntityLiving entity, MemoryModuleType<UUID> uuidMemoryModule) {
        Optional<UUID> optional = entity.getBehaviorController().getMemory(uuidMemoryModule);
        return optional.map((uuid) -> {
            return ((WorldServer)entity.level).getEntity(uuid);
        }).map((target) -> {
            EntityLiving var10000;
            if (target instanceof EntityLiving) {
                EntityLiving livingEntity = (EntityLiving)target;
                var10000 = livingEntity;
            } else {
                var10000 = null;
            }

            return var10000;
        });
    }

    public static Stream<EntityVillager> getNearbyVillagersWithCondition(EntityVillager villager, Predicate<EntityVillager> filter) {
        return villager.getBehaviorController().getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES).map((list) -> {
            return list.stream().filter((entity) -> {
                return entity instanceof EntityVillager && entity != villager;
            }).map((livingEntity) -> {
                return (EntityVillager)livingEntity;
            }).filter(EntityLiving::isAlive).filter(filter);
        }).orElseGet(Stream::empty);
    }

    @Nullable
    public static Vec3D getRandomSwimmablePos(EntityCreature entity, int horizontalRange, int verticalRange) {
        Vec3D vec3 = DefaultRandomPos.getPos(entity, horizontalRange, verticalRange);

        for(int i = 0; vec3 != null && !entity.level.getType(new BlockPosition(vec3)).isPathfindable(entity.level, new BlockPosition(vec3), PathMode.WATER) && i++ < 10; vec3 = DefaultRandomPos.getPos(entity, horizontalRange, verticalRange)) {
        }

        return vec3;
    }
}
