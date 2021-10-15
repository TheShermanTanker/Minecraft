package net.minecraft.world.entity;

import com.google.common.base.Predicates;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.world.IInventory;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.ScoreboardTeamBase;

public final class IEntitySelector {
    public static final Predicate<Entity> ENTITY_STILL_ALIVE = Entity::isAlive;
    public static final Predicate<Entity> LIVING_ENTITY_STILL_ALIVE = (entity) -> {
        return entity.isAlive() && entity instanceof EntityLiving;
    };
    public static final Predicate<Entity> ENTITY_NOT_BEING_RIDDEN = (entity) -> {
        return entity.isAlive() && !entity.isVehicle() && !entity.isPassenger();
    };
    public static final Predicate<Entity> CONTAINER_ENTITY_SELECTOR = (entity) -> {
        return entity instanceof IInventory && entity.isAlive();
    };
    public static final Predicate<Entity> NO_CREATIVE_OR_SPECTATOR = (entity) -> {
        return !(entity instanceof EntityHuman) || !entity.isSpectator() && !((EntityHuman)entity).isCreative();
    };
    public static final Predicate<Entity> NO_SPECTATORS = (entity) -> {
        return !entity.isSpectator();
    };

    private IEntitySelector() {
    }

    public static Predicate<Entity> withinDistance(double x, double y, double z, double max) {
        double d = max * max;
        return (entity) -> {
            return entity != null && entity.distanceToSqr(x, y, z) <= d;
        };
    }

    public static Predicate<Entity> pushableBy(Entity entity) {
        ScoreboardTeamBase team = entity.getScoreboardTeam();
        ScoreboardTeamBase.EnumTeamPush collisionRule = team == null ? ScoreboardTeamBase.EnumTeamPush.ALWAYS : team.getCollisionRule();
        return (Predicate<Entity>)(collisionRule == ScoreboardTeamBase.EnumTeamPush.NEVER ? Predicates.alwaysFalse() : NO_SPECTATORS.and((entity2) -> {
            if (!entity2.isCollidable()) {
                return false;
            } else if (!entity.level.isClientSide || entity2 instanceof EntityHuman && ((EntityHuman)entity2).isLocalPlayer()) {
                ScoreboardTeamBase team2 = entity2.getScoreboardTeam();
                ScoreboardTeamBase.EnumTeamPush collisionRule2 = team2 == null ? ScoreboardTeamBase.EnumTeamPush.ALWAYS : team2.getCollisionRule();
                if (collisionRule2 == ScoreboardTeamBase.EnumTeamPush.NEVER) {
                    return false;
                } else {
                    boolean bl = team != null && team.isAlly(team2);
                    if ((collisionRule == ScoreboardTeamBase.EnumTeamPush.PUSH_OWN_TEAM || collisionRule2 == ScoreboardTeamBase.EnumTeamPush.PUSH_OWN_TEAM) && bl) {
                        return false;
                    } else {
                        return collisionRule != ScoreboardTeamBase.EnumTeamPush.PUSH_OTHER_TEAMS && collisionRule2 != ScoreboardTeamBase.EnumTeamPush.PUSH_OTHER_TEAMS || bl;
                    }
                }
            } else {
                return false;
            }
        }));
    }

    public static Predicate<Entity> notRiding(Entity entity) {
        return (entity2) -> {
            while(true) {
                if (entity2.isPassenger()) {
                    entity2 = entity2.getVehicle();
                    if (entity2 != entity) {
                        continue;
                    }

                    return false;
                }

                return true;
            }
        };
    }

    public static class EntitySelectorEquipable implements Predicate<Entity> {
        private final ItemStack itemStack;

        public EntitySelectorEquipable(ItemStack stack) {
            this.itemStack = stack;
        }

        @Override
        public boolean test(@Nullable Entity entity) {
            if (!entity.isAlive()) {
                return false;
            } else if (!(entity instanceof EntityLiving)) {
                return false;
            } else {
                EntityLiving livingEntity = (EntityLiving)entity;
                return livingEntity.canTakeItem(this.itemStack);
            }
        }
    }
}
