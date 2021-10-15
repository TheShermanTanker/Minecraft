package net.minecraft.commands.arguments.selector;

import com.google.common.collect.Lists;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.advancements.critereon.CriterionConditionValue;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.commands.arguments.ArgumentEntity;
import net.minecraft.network.chat.ChatComponentUtils;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.Vec3D;

public class EntitySelector {
    public static final int INFINITE = Integer.MAX_VALUE;
    private static final EntityTypeTest<Entity, ?> ANY_TYPE = new EntityTypeTest<Entity, Entity>() {
        @Override
        public Entity tryCast(Entity obj) {
            return obj;
        }

        @Override
        public Class<? extends Entity> getBaseClass() {
            return Entity.class;
        }
    };
    private final int maxResults;
    private final boolean includesEntities;
    private final boolean worldLimited;
    private final Predicate<Entity> predicate;
    private final CriterionConditionValue.DoubleRange range;
    private final Function<Vec3D, Vec3D> position;
    @Nullable
    private final AxisAlignedBB aabb;
    private final BiConsumer<Vec3D, List<? extends Entity>> order;
    private final boolean currentEntity;
    @Nullable
    private final String playerName;
    @Nullable
    private final UUID entityUUID;
    private EntityTypeTest<Entity, ?> type;
    private final boolean usesSelector;

    public EntitySelector(int count, boolean includesNonPlayers, boolean localWorldOnly, Predicate<Entity> basePredicate, CriterionConditionValue.DoubleRange distance, Function<Vec3D, Vec3D> positionOffset, @Nullable AxisAlignedBB box, BiConsumer<Vec3D, List<? extends Entity>> sorter, boolean senderOnly, @Nullable String playerName, @Nullable UUID uuid, @Nullable EntityTypes<?> type, boolean usesAt) {
        this.maxResults = count;
        this.includesEntities = includesNonPlayers;
        this.worldLimited = localWorldOnly;
        this.predicate = basePredicate;
        this.range = distance;
        this.position = positionOffset;
        this.aabb = box;
        this.order = sorter;
        this.currentEntity = senderOnly;
        this.playerName = playerName;
        this.entityUUID = uuid;
        this.type = (EntityTypeTest<Entity, ?>)(type == null ? ANY_TYPE : type);
        this.usesSelector = usesAt;
    }

    public int getMaxResults() {
        return this.maxResults;
    }

    public boolean includesEntities() {
        return this.includesEntities;
    }

    public boolean isSelfSelector() {
        return this.currentEntity;
    }

    public boolean isWorldLimited() {
        return this.worldLimited;
    }

    public boolean usesSelector() {
        return this.usesSelector;
    }

    private void checkPermissions(CommandListenerWrapper commandSourceStack) throws CommandSyntaxException {
        if (this.usesSelector && !commandSourceStack.hasPermission(2)) {
            throw ArgumentEntity.ERROR_SELECTORS_NOT_ALLOWED.create();
        }
    }

    public Entity findSingleEntity(CommandListenerWrapper commandSourceStack) throws CommandSyntaxException {
        this.checkPermissions(commandSourceStack);
        List<? extends Entity> list = this.getEntities(commandSourceStack);
        if (list.isEmpty()) {
            throw ArgumentEntity.NO_ENTITIES_FOUND.create();
        } else if (list.size() > 1) {
            throw ArgumentEntity.ERROR_NOT_SINGLE_ENTITY.create();
        } else {
            return list.get(0);
        }
    }

    public List<? extends Entity> getEntities(CommandListenerWrapper commandSourceStack) throws CommandSyntaxException {
        this.checkPermissions(commandSourceStack);
        if (!this.includesEntities) {
            return this.findPlayers(commandSourceStack);
        } else if (this.playerName != null) {
            EntityPlayer serverPlayer = commandSourceStack.getServer().getPlayerList().getPlayer(this.playerName);
            return (List<? extends Entity>)(serverPlayer == null ? Collections.emptyList() : Lists.newArrayList(serverPlayer));
        } else if (this.entityUUID != null) {
            for(WorldServer serverLevel : commandSourceStack.getServer().getWorlds()) {
                Entity entity = serverLevel.getEntity(this.entityUUID);
                if (entity != null) {
                    return Lists.newArrayList(entity);
                }
            }

            return Collections.emptyList();
        } else {
            Vec3D vec3 = this.position.apply(commandSourceStack.getPosition());
            Predicate<Entity> predicate = this.getPredicate(vec3);
            if (this.currentEntity) {
                return (List<? extends Entity>)(commandSourceStack.getEntity() != null && predicate.test(commandSourceStack.getEntity()) ? Lists.newArrayList(commandSourceStack.getEntity()) : Collections.emptyList());
            } else {
                List<Entity> list = Lists.newArrayList();
                if (this.isWorldLimited()) {
                    this.addEntities(list, commandSourceStack.getWorld(), vec3, predicate);
                } else {
                    for(WorldServer serverLevel2 : commandSourceStack.getServer().getWorlds()) {
                        this.addEntities(list, serverLevel2, vec3, predicate);
                    }
                }

                return this.sortAndLimit(vec3, list);
            }
        }
    }

    private void addEntities(List<Entity> list, WorldServer serverLevel, Vec3D vec3, Predicate<Entity> predicate) {
        if (this.aabb != null) {
            list.addAll(serverLevel.getEntities(this.type, this.aabb.move(vec3), predicate));
        } else {
            list.addAll(serverLevel.getEntities(this.type, predicate));
        }

    }

    public EntityPlayer findSinglePlayer(CommandListenerWrapper commandSourceStack) throws CommandSyntaxException {
        this.checkPermissions(commandSourceStack);
        List<EntityPlayer> list = this.findPlayers(commandSourceStack);
        if (list.size() != 1) {
            throw ArgumentEntity.NO_PLAYERS_FOUND.create();
        } else {
            return list.get(0);
        }
    }

    public List<EntityPlayer> findPlayers(CommandListenerWrapper commandSourceStack) throws CommandSyntaxException {
        this.checkPermissions(commandSourceStack);
        if (this.playerName != null) {
            EntityPlayer serverPlayer = commandSourceStack.getServer().getPlayerList().getPlayer(this.playerName);
            return (List<EntityPlayer>)(serverPlayer == null ? Collections.emptyList() : Lists.newArrayList(serverPlayer));
        } else if (this.entityUUID != null) {
            EntityPlayer serverPlayer2 = commandSourceStack.getServer().getPlayerList().getPlayer(this.entityUUID);
            return (List<EntityPlayer>)(serverPlayer2 == null ? Collections.emptyList() : Lists.newArrayList(serverPlayer2));
        } else {
            Vec3D vec3 = this.position.apply(commandSourceStack.getPosition());
            Predicate<Entity> predicate = this.getPredicate(vec3);
            if (this.currentEntity) {
                if (commandSourceStack.getEntity() instanceof EntityPlayer) {
                    EntityPlayer serverPlayer3 = (EntityPlayer)commandSourceStack.getEntity();
                    if (predicate.test(serverPlayer3)) {
                        return Lists.newArrayList(serverPlayer3);
                    }
                }

                return Collections.emptyList();
            } else {
                List<EntityPlayer> list;
                if (this.isWorldLimited()) {
                    list = commandSourceStack.getWorld().getPlayers(predicate);
                } else {
                    list = Lists.newArrayList();

                    for(EntityPlayer serverPlayer4 : commandSourceStack.getServer().getPlayerList().getPlayers()) {
                        if (predicate.test(serverPlayer4)) {
                            list.add(serverPlayer4);
                        }
                    }
                }

                return this.sortAndLimit(vec3, list);
            }
        }
    }

    private Predicate<Entity> getPredicate(Vec3D vec3) {
        Predicate<Entity> predicate = this.predicate;
        if (this.aabb != null) {
            AxisAlignedBB aABB = this.aabb.move(vec3);
            predicate = predicate.and((entity) -> {
                return aABB.intersects(entity.getBoundingBox());
            });
        }

        if (!this.range.isAny()) {
            predicate = predicate.and((entity) -> {
                return this.range.matchesSqr(entity.distanceToSqr(vec3));
            });
        }

        return predicate;
    }

    private <T extends Entity> List<T> sortAndLimit(Vec3D vec3, List<T> list) {
        if (list.size() > 1) {
            this.order.accept(vec3, list);
        }

        return list.subList(0, Math.min(this.maxResults, list.size()));
    }

    public static IChatBaseComponent joinNames(List<? extends Entity> list) {
        return ChatComponentUtils.formatList(list, Entity::getScoreboardDisplayName);
    }
}
