package net.minecraft.advancements.critereon;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.Tag;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.EntityCat;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameterSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameters;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionEntityProperty;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditions;
import net.minecraft.world.phys.Vec3D;
import net.minecraft.world.scores.ScoreboardTeamBase;

public class CriterionConditionEntity {
    public static final CriterionConditionEntity ANY = new CriterionConditionEntity(CriterionConditionEntityType.ANY, CriterionConditionDistance.ANY, CriterionConditionLocation.ANY, CriterionConditionLocation.ANY, CriterionConditionMobEffect.ANY, CriterionConditionNBT.ANY, CriterionConditionEntityFlags.ANY, CriterionConditionEntityEquipment.ANY, CriterionConditionPlayer.ANY, CriterionConditionInOpenWater.ANY, LighthingBoltPredicate.ANY, (String)null, (MinecraftKey)null);
    private final CriterionConditionEntityType entityType;
    private final CriterionConditionDistance distanceToPlayer;
    private final CriterionConditionLocation location;
    private final CriterionConditionLocation steppingOnLocation;
    private final CriterionConditionMobEffect effects;
    private final CriterionConditionNBT nbt;
    private final CriterionConditionEntityFlags flags;
    private final CriterionConditionEntityEquipment equipment;
    private final CriterionConditionPlayer player;
    private final CriterionConditionInOpenWater fishingHook;
    private final LighthingBoltPredicate lighthingBolt;
    private final CriterionConditionEntity vehicle;
    private final CriterionConditionEntity passenger;
    private final CriterionConditionEntity targetedEntity;
    @Nullable
    private final String team;
    @Nullable
    private final MinecraftKey catType;

    private CriterionConditionEntity(CriterionConditionEntityType type, CriterionConditionDistance distance, CriterionConditionLocation location, CriterionConditionLocation steppingOn, CriterionConditionMobEffect effects, CriterionConditionNBT nbt, CriterionConditionEntityFlags flags, CriterionConditionEntityEquipment equipment, CriterionConditionPlayer player, CriterionConditionInOpenWater fishingHook, LighthingBoltPredicate lightningBolt, @Nullable String team, @Nullable MinecraftKey catType) {
        this.entityType = type;
        this.distanceToPlayer = distance;
        this.location = location;
        this.steppingOnLocation = steppingOn;
        this.effects = effects;
        this.nbt = nbt;
        this.flags = flags;
        this.equipment = equipment;
        this.player = player;
        this.fishingHook = fishingHook;
        this.lighthingBolt = lightningBolt;
        this.passenger = this;
        this.vehicle = this;
        this.targetedEntity = this;
        this.team = team;
        this.catType = catType;
    }

    CriterionConditionEntity(CriterionConditionEntityType type, CriterionConditionDistance distance, CriterionConditionLocation location, CriterionConditionLocation steppingOn, CriterionConditionMobEffect effects, CriterionConditionNBT nbt, CriterionConditionEntityFlags flags, CriterionConditionEntityEquipment equipment, CriterionConditionPlayer player, CriterionConditionInOpenWater fishingHook, LighthingBoltPredicate lighthingBoltPredicate, CriterionConditionEntity vehicle, CriterionConditionEntity entityPredicate, CriterionConditionEntity targetedEntity, @Nullable String team, @Nullable MinecraftKey catType) {
        this.entityType = type;
        this.distanceToPlayer = distance;
        this.location = location;
        this.steppingOnLocation = steppingOn;
        this.effects = effects;
        this.nbt = nbt;
        this.flags = flags;
        this.equipment = equipment;
        this.player = player;
        this.fishingHook = fishingHook;
        this.lighthingBolt = lighthingBoltPredicate;
        this.vehicle = vehicle;
        this.passenger = entityPredicate;
        this.targetedEntity = targetedEntity;
        this.team = team;
        this.catType = catType;
    }

    public boolean matches(EntityPlayer player, @Nullable Entity entity) {
        return this.matches(player.getWorldServer(), player.getPositionVector(), entity);
    }

    public boolean matches(WorldServer world, @Nullable Vec3D pos, @Nullable Entity entity) {
        if (this == ANY) {
            return true;
        } else if (entity == null) {
            return false;
        } else if (!this.entityType.matches(entity.getEntityType())) {
            return false;
        } else {
            if (pos == null) {
                if (this.distanceToPlayer != CriterionConditionDistance.ANY) {
                    return false;
                }
            } else if (!this.distanceToPlayer.matches(pos.x, pos.y, pos.z, entity.locX(), entity.locY(), entity.locZ())) {
                return false;
            }

            if (!this.location.matches(world, entity.locX(), entity.locY(), entity.locZ())) {
                return false;
            } else {
                if (this.steppingOnLocation != CriterionConditionLocation.ANY) {
                    Vec3D vec3 = Vec3D.atCenterOf(entity.getOnPos());
                    if (!this.steppingOnLocation.matches(world, vec3.getX(), vec3.getY(), vec3.getZ())) {
                        return false;
                    }
                }

                if (!this.effects.matches(entity)) {
                    return false;
                } else if (!this.nbt.matches(entity)) {
                    return false;
                } else if (!this.flags.matches(entity)) {
                    return false;
                } else if (!this.equipment.matches(entity)) {
                    return false;
                } else if (!this.player.matches(entity)) {
                    return false;
                } else if (!this.fishingHook.matches(entity)) {
                    return false;
                } else if (!this.lighthingBolt.matches(entity, world, pos)) {
                    return false;
                } else if (!this.vehicle.matches(world, pos, entity.getVehicle())) {
                    return false;
                } else if (this.passenger != ANY && entity.getPassengers().stream().noneMatch((entityx) -> {
                    return this.passenger.matches(world, pos, entityx);
                })) {
                    return false;
                } else if (!this.targetedEntity.matches(world, pos, entity instanceof EntityInsentient ? ((EntityInsentient)entity).getGoalTarget() : null)) {
                    return false;
                } else {
                    if (this.team != null) {
                        ScoreboardTeamBase team = entity.getScoreboardTeam();
                        if (team == null || !this.team.equals(team.getName())) {
                            return false;
                        }
                    }

                    return this.catType == null || entity instanceof EntityCat && ((EntityCat)entity).getResourceLocation().equals(this.catType);
                }
            }
        }
    }

    public static CriterionConditionEntity fromJson(@Nullable JsonElement json) {
        if (json != null && !json.isJsonNull()) {
            JsonObject jsonObject = ChatDeserializer.convertToJsonObject(json, "entity");
            CriterionConditionEntityType entityTypePredicate = CriterionConditionEntityType.fromJson(jsonObject.get("type"));
            CriterionConditionDistance distancePredicate = CriterionConditionDistance.fromJson(jsonObject.get("distance"));
            CriterionConditionLocation locationPredicate = CriterionConditionLocation.fromJson(jsonObject.get("location"));
            CriterionConditionLocation locationPredicate2 = CriterionConditionLocation.fromJson(jsonObject.get("stepping_on"));
            CriterionConditionMobEffect mobEffectsPredicate = CriterionConditionMobEffect.fromJson(jsonObject.get("effects"));
            CriterionConditionNBT nbtPredicate = CriterionConditionNBT.fromJson(jsonObject.get("nbt"));
            CriterionConditionEntityFlags entityFlagsPredicate = CriterionConditionEntityFlags.fromJson(jsonObject.get("flags"));
            CriterionConditionEntityEquipment entityEquipmentPredicate = CriterionConditionEntityEquipment.fromJson(jsonObject.get("equipment"));
            CriterionConditionPlayer playerPredicate = CriterionConditionPlayer.fromJson(jsonObject.get("player"));
            CriterionConditionInOpenWater fishingHookPredicate = CriterionConditionInOpenWater.fromJson(jsonObject.get("fishing_hook"));
            CriterionConditionEntity entityPredicate = fromJson(jsonObject.get("vehicle"));
            CriterionConditionEntity entityPredicate2 = fromJson(jsonObject.get("passenger"));
            CriterionConditionEntity entityPredicate3 = fromJson(jsonObject.get("targeted_entity"));
            LighthingBoltPredicate lighthingBoltPredicate = LighthingBoltPredicate.fromJson(jsonObject.get("lightning_bolt"));
            String string = ChatDeserializer.getAsString(jsonObject, "team", (String)null);
            MinecraftKey resourceLocation = jsonObject.has("catType") ? new MinecraftKey(ChatDeserializer.getAsString(jsonObject, "catType")) : null;
            return (new CriterionConditionEntity.Builder()).entityType(entityTypePredicate).distance(distancePredicate).located(locationPredicate).steppingOn(locationPredicate2).effects(mobEffectsPredicate).nbt(nbtPredicate).flags(entityFlagsPredicate).equipment(entityEquipmentPredicate).player(playerPredicate).fishingHook(fishingHookPredicate).lighthingBolt(lighthingBoltPredicate).team(string).vehicle(entityPredicate).passenger(entityPredicate2).targetedEntity(entityPredicate3).catType(resourceLocation).build();
        } else {
            return ANY;
        }
    }

    public JsonElement serializeToJson() {
        if (this == ANY) {
            return JsonNull.INSTANCE;
        } else {
            JsonObject jsonObject = new JsonObject();
            jsonObject.add("type", this.entityType.serializeToJson());
            jsonObject.add("distance", this.distanceToPlayer.serializeToJson());
            jsonObject.add("location", this.location.serializeToJson());
            jsonObject.add("stepping_on", this.steppingOnLocation.serializeToJson());
            jsonObject.add("effects", this.effects.serializeToJson());
            jsonObject.add("nbt", this.nbt.serializeToJson());
            jsonObject.add("flags", this.flags.serializeToJson());
            jsonObject.add("equipment", this.equipment.serializeToJson());
            jsonObject.add("player", this.player.serializeToJson());
            jsonObject.add("fishing_hook", this.fishingHook.serializeToJson());
            jsonObject.add("lightning_bolt", this.lighthingBolt.serializeToJson());
            jsonObject.add("vehicle", this.vehicle.serializeToJson());
            jsonObject.add("passenger", this.passenger.serializeToJson());
            jsonObject.add("targeted_entity", this.targetedEntity.serializeToJson());
            jsonObject.addProperty("team", this.team);
            if (this.catType != null) {
                jsonObject.addProperty("catType", this.catType.toString());
            }

            return jsonObject;
        }
    }

    public static LootTableInfo createContext(EntityPlayer player, Entity target) {
        return (new LootTableInfo.Builder(player.getWorldServer())).set(LootContextParameters.THIS_ENTITY, target).set(LootContextParameters.ORIGIN, player.getPositionVector()).withRandom(player.getRandom()).build(LootContextParameterSets.ADVANCEMENT_ENTITY);
    }

    public static class Builder {
        private CriterionConditionEntityType entityType = CriterionConditionEntityType.ANY;
        private CriterionConditionDistance distanceToPlayer = CriterionConditionDistance.ANY;
        private CriterionConditionLocation location = CriterionConditionLocation.ANY;
        private CriterionConditionLocation steppingOnLocation = CriterionConditionLocation.ANY;
        private CriterionConditionMobEffect effects = CriterionConditionMobEffect.ANY;
        private CriterionConditionNBT nbt = CriterionConditionNBT.ANY;
        private CriterionConditionEntityFlags flags = CriterionConditionEntityFlags.ANY;
        private CriterionConditionEntityEquipment equipment = CriterionConditionEntityEquipment.ANY;
        private CriterionConditionPlayer player = CriterionConditionPlayer.ANY;
        private CriterionConditionInOpenWater fishingHook = CriterionConditionInOpenWater.ANY;
        private LighthingBoltPredicate lighthingBolt = LighthingBoltPredicate.ANY;
        private CriterionConditionEntity vehicle = CriterionConditionEntity.ANY;
        private CriterionConditionEntity passenger = CriterionConditionEntity.ANY;
        private CriterionConditionEntity targetedEntity = CriterionConditionEntity.ANY;
        private String team;
        private MinecraftKey catType;

        public static CriterionConditionEntity.Builder entity() {
            return new CriterionConditionEntity.Builder();
        }

        public CriterionConditionEntity.Builder of(EntityTypes<?> type) {
            this.entityType = CriterionConditionEntityType.of(type);
            return this;
        }

        public CriterionConditionEntity.Builder of(Tag<EntityTypes<?>> tag) {
            this.entityType = CriterionConditionEntityType.of(tag);
            return this;
        }

        public CriterionConditionEntity.Builder of(MinecraftKey catType) {
            this.catType = catType;
            return this;
        }

        public CriterionConditionEntity.Builder entityType(CriterionConditionEntityType type) {
            this.entityType = type;
            return this;
        }

        public CriterionConditionEntity.Builder distance(CriterionConditionDistance distance) {
            this.distanceToPlayer = distance;
            return this;
        }

        public CriterionConditionEntity.Builder located(CriterionConditionLocation location) {
            this.location = location;
            return this;
        }

        public CriterionConditionEntity.Builder steppingOn(CriterionConditionLocation location) {
            this.steppingOnLocation = location;
            return this;
        }

        public CriterionConditionEntity.Builder effects(CriterionConditionMobEffect effects) {
            this.effects = effects;
            return this;
        }

        public CriterionConditionEntity.Builder nbt(CriterionConditionNBT nbt) {
            this.nbt = nbt;
            return this;
        }

        public CriterionConditionEntity.Builder flags(CriterionConditionEntityFlags flags) {
            this.flags = flags;
            return this;
        }

        public CriterionConditionEntity.Builder equipment(CriterionConditionEntityEquipment equipment) {
            this.equipment = equipment;
            return this;
        }

        public CriterionConditionEntity.Builder player(CriterionConditionPlayer player) {
            this.player = player;
            return this;
        }

        public CriterionConditionEntity.Builder fishingHook(CriterionConditionInOpenWater fishHook) {
            this.fishingHook = fishHook;
            return this;
        }

        public CriterionConditionEntity.Builder lighthingBolt(LighthingBoltPredicate lightningBolt) {
            this.lighthingBolt = lightningBolt;
            return this;
        }

        public CriterionConditionEntity.Builder vehicle(CriterionConditionEntity vehicle) {
            this.vehicle = vehicle;
            return this;
        }

        public CriterionConditionEntity.Builder passenger(CriterionConditionEntity passenger) {
            this.passenger = passenger;
            return this;
        }

        public CriterionConditionEntity.Builder targetedEntity(CriterionConditionEntity targetedEntity) {
            this.targetedEntity = targetedEntity;
            return this;
        }

        public CriterionConditionEntity.Builder team(@Nullable String team) {
            this.team = team;
            return this;
        }

        public CriterionConditionEntity.Builder catType(@Nullable MinecraftKey catType) {
            this.catType = catType;
            return this;
        }

        public CriterionConditionEntity build() {
            return new CriterionConditionEntity(this.entityType, this.distanceToPlayer, this.location, this.steppingOnLocation, this.effects, this.nbt, this.flags, this.equipment, this.player, this.fishingHook, this.lighthingBolt, this.vehicle, this.passenger, this.targetedEntity, this.team, this.catType);
        }
    }

    public static class Composite {
        public static final CriterionConditionEntity.Composite ANY = new CriterionConditionEntity.Composite(new LootItemCondition[0]);
        private final LootItemCondition[] conditions;
        private final Predicate<LootTableInfo> compositePredicates;

        private Composite(LootItemCondition[] conditions) {
            this.conditions = conditions;
            this.compositePredicates = LootItemConditions.andConditions(conditions);
        }

        public static CriterionConditionEntity.Composite create(LootItemCondition... conditions) {
            return new CriterionConditionEntity.Composite(conditions);
        }

        public static CriterionConditionEntity.Composite fromJson(JsonObject root, String key, LootDeserializationContext predicateDeserializer) {
            JsonElement jsonElement = root.get(key);
            return fromElement(key, predicateDeserializer, jsonElement);
        }

        public static CriterionConditionEntity.Composite[] fromJsonArray(JsonObject root, String key, LootDeserializationContext predicateDeserializer) {
            JsonElement jsonElement = root.get(key);
            if (jsonElement != null && !jsonElement.isJsonNull()) {
                JsonArray jsonArray = ChatDeserializer.convertToJsonArray(jsonElement, key);
                CriterionConditionEntity.Composite[] composites = new CriterionConditionEntity.Composite[jsonArray.size()];

                for(int i = 0; i < jsonArray.size(); ++i) {
                    composites[i] = fromElement(key + "[" + i + "]", predicateDeserializer, jsonArray.get(i));
                }

                return composites;
            } else {
                return new CriterionConditionEntity.Composite[0];
            }
        }

        private static CriterionConditionEntity.Composite fromElement(String key, LootDeserializationContext predicateDeserializer, @Nullable JsonElement json) {
            if (json != null && json.isJsonArray()) {
                LootItemCondition[] lootItemConditions = predicateDeserializer.deserializeConditions(json.getAsJsonArray(), predicateDeserializer.getAdvancementId() + "/" + key, LootContextParameterSets.ADVANCEMENT_ENTITY);
                return new CriterionConditionEntity.Composite(lootItemConditions);
            } else {
                CriterionConditionEntity entityPredicate = CriterionConditionEntity.fromJson(json);
                return wrap(entityPredicate);
            }
        }

        public static CriterionConditionEntity.Composite wrap(CriterionConditionEntity predicate) {
            if (predicate == CriterionConditionEntity.ANY) {
                return ANY;
            } else {
                LootItemCondition lootItemCondition = LootItemConditionEntityProperty.hasProperties(LootTableInfo.EntityTarget.THIS, predicate).build();
                return new CriterionConditionEntity.Composite(new LootItemCondition[]{lootItemCondition});
            }
        }

        public boolean matches(LootTableInfo context) {
            return this.compositePredicates.test(context);
        }

        public JsonElement toJson(LootSerializationContext predicateSerializer) {
            return (JsonElement)(this.conditions.length == 0 ? JsonNull.INSTANCE : predicateSerializer.serializeConditions(this.conditions));
        }

        public static JsonElement toJson(CriterionConditionEntity.Composite[] predicates, LootSerializationContext predicateSerializer) {
            if (predicates.length == 0) {
                return JsonNull.INSTANCE;
            } else {
                JsonArray jsonArray = new JsonArray();

                for(CriterionConditionEntity.Composite composite : predicates) {
                    jsonArray.add(composite.toJson(predicateSerializer));
                }

                return jsonArray;
            }
        }
    }
}
