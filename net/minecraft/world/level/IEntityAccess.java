package net.minecraft.world.level;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.ImmutableList.Builder;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.IEntitySelector;
import net.minecraft.world.entity.ai.targeting.PathfinderTargetCondition;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.shapes.OperatorBoolean;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapes;

public interface IEntityAccess {
    List<Entity> getEntities(@Nullable Entity except, AxisAlignedBB box, Predicate<? super Entity> predicate);

    <T extends Entity> List<T> getEntities(EntityTypeTest<Entity, T> filter, AxisAlignedBB box, Predicate<? super T> predicate);

    default <T extends Entity> List<T> getEntitiesOfClass(Class<T> entityClass, AxisAlignedBB box, Predicate<? super T> predicate) {
        return this.getEntities(EntityTypeTest.forClass(entityClass), box, predicate);
    }

    List<? extends EntityHuman> getPlayers();

    default List<Entity> getEntities(@Nullable Entity except, AxisAlignedBB box) {
        return this.getEntities(except, box, IEntitySelector.NO_SPECTATORS);
    }

    default boolean isUnobstructed(@Nullable Entity entity, VoxelShape shape) {
        if (shape.isEmpty()) {
            return true;
        } else {
            for(Entity entity2 : this.getEntities(entity, shape.getBoundingBox())) {
                if (!entity2.isRemoved() && entity2.blocksBuilding && (entity == null || !entity2.isSameVehicle(entity)) && VoxelShapes.joinIsNotEmpty(shape, VoxelShapes.create(entity2.getBoundingBox()), OperatorBoolean.AND)) {
                    return false;
                }
            }

            return true;
        }
    }

    default <T extends Entity> List<T> getEntitiesOfClass(Class<T> entityClass, AxisAlignedBB box) {
        return this.getEntitiesOfClass(entityClass, box, IEntitySelector.NO_SPECTATORS);
    }

    default List<VoxelShape> getEntityCollisions(@Nullable Entity entity, AxisAlignedBB box) {
        if (box.getSize() < 1.0E-7D) {
            return List.of();
        } else {
            Predicate<Entity> predicate = entity == null ? IEntitySelector.CAN_BE_COLLIDED_WITH : IEntitySelector.NO_SPECTATORS.and(entity::canCollideWith);
            List<Entity> list = this.getEntities(entity, box.inflate(1.0E-7D), predicate);
            if (list.isEmpty()) {
                return List.of();
            } else {
                Builder<VoxelShape> builder = ImmutableList.builderWithExpectedSize(list.size());

                for(Entity entity2 : list) {
                    builder.add(VoxelShapes.create(entity2.getBoundingBox()));
                }

                return builder.build();
            }
        }
    }

    @Nullable
    default EntityHuman getNearestPlayer(double x, double y, double z, double maxDistance, @Nullable Predicate<Entity> targetPredicate) {
        double d = -1.0D;
        EntityHuman player = null;

        for(EntityHuman player2 : this.getPlayers()) {
            if (targetPredicate == null || targetPredicate.test(player2)) {
                double e = player2.distanceToSqr(x, y, z);
                if ((maxDistance < 0.0D || e < maxDistance * maxDistance) && (d == -1.0D || e < d)) {
                    d = e;
                    player = player2;
                }
            }
        }

        return player;
    }

    @Nullable
    default EntityHuman findNearbyPlayer(Entity entity, double maxDistance) {
        return this.getNearestPlayer(entity.locX(), entity.locY(), entity.locZ(), maxDistance, false);
    }

    @Nullable
    default EntityHuman getNearestPlayer(double x, double y, double z, double maxDistance, boolean ignoreCreative) {
        Predicate<Entity> predicate = ignoreCreative ? IEntitySelector.NO_CREATIVE_OR_SPECTATOR : IEntitySelector.NO_SPECTATORS;
        return this.getNearestPlayer(x, y, z, maxDistance, predicate);
    }

    default boolean isPlayerNearby(double x, double y, double z, double range) {
        for(EntityHuman player : this.getPlayers()) {
            if (IEntitySelector.NO_SPECTATORS.test(player) && IEntitySelector.LIVING_ENTITY_STILL_ALIVE.test(player)) {
                double d = player.distanceToSqr(x, y, z);
                if (range < 0.0D || d < range * range) {
                    return true;
                }
            }
        }

        return false;
    }

    @Nullable
    default EntityHuman getNearestPlayer(PathfinderTargetCondition targetPredicate, EntityLiving entity) {
        return this.getNearestEntity(this.getPlayers(), targetPredicate, entity, entity.locX(), entity.locY(), entity.locZ());
    }

    @Nullable
    default EntityHuman getNearestPlayer(PathfinderTargetCondition targetPredicate, EntityLiving entity, double x, double y, double z) {
        return this.getNearestEntity(this.getPlayers(), targetPredicate, entity, x, y, z);
    }

    @Nullable
    default EntityHuman getNearestPlayer(PathfinderTargetCondition targetPredicate, double x, double y, double z) {
        return this.getNearestEntity(this.getPlayers(), targetPredicate, (EntityLiving)null, x, y, z);
    }

    @Nullable
    default <T extends EntityLiving> T getNearestEntity(Class<? extends T> entityClass, PathfinderTargetCondition targetPredicate, @Nullable EntityLiving entity, double x, double y, double z, AxisAlignedBB box) {
        return this.getNearestEntity(this.getEntitiesOfClass(entityClass, box, (livingEntity) -> {
            return true;
        }), targetPredicate, entity, x, y, z);
    }

    @Nullable
    default <T extends EntityLiving> T getNearestEntity(List<? extends T> entityList, PathfinderTargetCondition targetPredicate, @Nullable EntityLiving entity, double x, double y, double z) {
        double d = -1.0D;
        T livingEntity = null;

        for(T livingEntity2 : entityList) {
            if (targetPredicate.test(entity, livingEntity2)) {
                double e = livingEntity2.distanceToSqr(x, y, z);
                if (d == -1.0D || e < d) {
                    d = e;
                    livingEntity = livingEntity2;
                }
            }
        }

        return livingEntity;
    }

    default List<EntityHuman> getNearbyPlayers(PathfinderTargetCondition targetPredicate, EntityLiving entity, AxisAlignedBB box) {
        List<EntityHuman> list = Lists.newArrayList();

        for(EntityHuman player : this.getPlayers()) {
            if (box.contains(player.locX(), player.locY(), player.locZ()) && targetPredicate.test(entity, player)) {
                list.add(player);
            }
        }

        return list;
    }

    default <T extends EntityLiving> List<T> getNearbyEntities(Class<T> entityClass, PathfinderTargetCondition targetPredicate, EntityLiving targetingEntity, AxisAlignedBB box) {
        List<T> list = this.getEntitiesOfClass(entityClass, box, (livingEntityx) -> {
            return true;
        });
        List<T> list2 = Lists.newArrayList();

        for(T livingEntity : list) {
            if (targetPredicate.test(targetingEntity, livingEntity)) {
                list2.add(livingEntity);
            }
        }

        return list2;
    }

    @Nullable
    default EntityHuman getPlayerByUUID(UUID uuid) {
        for(int i = 0; i < this.getPlayers().size(); ++i) {
            EntityHuman player = this.getPlayers().get(i);
            if (uuid.equals(player.getUniqueID())) {
                return player;
            }
        }

        return null;
    }
}
