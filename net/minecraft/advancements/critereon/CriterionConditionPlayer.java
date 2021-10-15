package net.minecraft.advancements.critereon;

import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.core.IRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.AdvancementDataPlayer;
import net.minecraft.server.AdvancementDataWorld;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.stats.RecipeBook;
import net.minecraft.stats.Statistic;
import net.minecraft.stats.StatisticManager;
import net.minecraft.stats.StatisticWrapper;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileHelper;
import net.minecraft.world.level.EnumGamemode;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.MovingObjectPosition;
import net.minecraft.world.phys.MovingObjectPositionEntity;
import net.minecraft.world.phys.Vec3D;

public class CriterionConditionPlayer {
    public static final CriterionConditionPlayer ANY = (new CriterionConditionPlayer.Builder()).build();
    public static final int LOOKING_AT_RANGE = 100;
    private final CriterionConditionValue.IntegerRange level;
    @Nullable
    private final EnumGamemode gameType;
    private final Map<Statistic<?>, CriterionConditionValue.IntegerRange> stats;
    private final Object2BooleanMap<MinecraftKey> recipes;
    private final Map<MinecraftKey, CriterionConditionPlayer.AdvancementPredicate> advancements;
    private final CriterionConditionEntity lookingAt;

    private static CriterionConditionPlayer.AdvancementPredicate advancementPredicateFromJson(JsonElement json) {
        if (json.isJsonPrimitive()) {
            boolean bl = json.getAsBoolean();
            return new CriterionConditionPlayer.AdvancementDonePredicate(bl);
        } else {
            Object2BooleanMap<String> object2BooleanMap = new Object2BooleanOpenHashMap<>();
            JsonObject jsonObject = ChatDeserializer.convertToJsonObject(json, "criterion data");
            jsonObject.entrySet().forEach((entry) -> {
                boolean bl = ChatDeserializer.convertToBoolean(entry.getValue(), "criterion test");
                object2BooleanMap.put(entry.getKey(), bl);
            });
            return new CriterionConditionPlayer.AdvancementCriterionsPredicate(object2BooleanMap);
        }
    }

    CriterionConditionPlayer(CriterionConditionValue.IntegerRange experienceLevel, @Nullable EnumGamemode gameMode, Map<Statistic<?>, CriterionConditionValue.IntegerRange> stats, Object2BooleanMap<MinecraftKey> recipes, Map<MinecraftKey, CriterionConditionPlayer.AdvancementPredicate> advancements, CriterionConditionEntity lookingAt) {
        this.level = experienceLevel;
        this.gameType = gameMode;
        this.stats = stats;
        this.recipes = recipes;
        this.advancements = advancements;
        this.lookingAt = lookingAt;
    }

    public boolean matches(Entity entity) {
        if (this == ANY) {
            return true;
        } else if (!(entity instanceof EntityPlayer)) {
            return false;
        } else {
            EntityPlayer serverPlayer = (EntityPlayer)entity;
            if (!this.level.matches(serverPlayer.experienceLevel)) {
                return false;
            } else if (this.gameType != null && this.gameType != serverPlayer.gameMode.getGameMode()) {
                return false;
            } else {
                StatisticManager statsCounter = serverPlayer.getStatisticManager();

                for(Entry<Statistic<?>, CriterionConditionValue.IntegerRange> entry : this.stats.entrySet()) {
                    int i = statsCounter.getStatisticValue(entry.getKey());
                    if (!entry.getValue().matches(i)) {
                        return false;
                    }
                }

                RecipeBook recipeBook = serverPlayer.getRecipeBook();

                for(it.unimi.dsi.fastutil.objects.Object2BooleanMap.Entry<MinecraftKey> entry2 : this.recipes.object2BooleanEntrySet()) {
                    if (recipeBook.hasDiscoveredRecipe(entry2.getKey()) != entry2.getBooleanValue()) {
                        return false;
                    }
                }

                if (!this.advancements.isEmpty()) {
                    AdvancementDataPlayer playerAdvancements = serverPlayer.getAdvancementData();
                    AdvancementDataWorld serverAdvancementManager = serverPlayer.getMinecraftServer().getAdvancementData();

                    for(Entry<MinecraftKey, CriterionConditionPlayer.AdvancementPredicate> entry3 : this.advancements.entrySet()) {
                        Advancement advancement = serverAdvancementManager.getAdvancement(entry3.getKey());
                        if (advancement == null || !entry3.getValue().test(playerAdvancements.getProgress(advancement))) {
                            return false;
                        }
                    }
                }

                if (this.lookingAt != CriterionConditionEntity.ANY) {
                    Vec3D vec3 = serverPlayer.getEyePosition();
                    Vec3D vec32 = serverPlayer.getViewVector(1.0F);
                    Vec3D vec33 = vec3.add(vec32.x * 100.0D, vec32.y * 100.0D, vec32.z * 100.0D);
                    MovingObjectPositionEntity entityHitResult = ProjectileHelper.getEntityHitResult(serverPlayer.level, serverPlayer, vec3, vec33, (new AxisAlignedBB(vec3, vec33)).inflate(1.0D), (entityx) -> {
                        return !entityx.isSpectator();
                    }, 0.0F);
                    if (entityHitResult == null || entityHitResult.getType() != MovingObjectPosition.EnumMovingObjectType.ENTITY) {
                        return false;
                    }

                    Entity entity2 = entityHitResult.getEntity();
                    if (!this.lookingAt.matches(serverPlayer, entity2) || !serverPlayer.hasLineOfSight(entity2)) {
                        return false;
                    }
                }

                return true;
            }
        }
    }

    public static CriterionConditionPlayer fromJson(@Nullable JsonElement json) {
        if (json != null && !json.isJsonNull()) {
            JsonObject jsonObject = ChatDeserializer.convertToJsonObject(json, "player");
            CriterionConditionValue.IntegerRange ints = CriterionConditionValue.IntegerRange.fromJson(jsonObject.get("level"));
            String string = ChatDeserializer.getAsString(jsonObject, "gamemode", "");
            EnumGamemode gameType = EnumGamemode.byName(string, (EnumGamemode)null);
            Map<Statistic<?>, CriterionConditionValue.IntegerRange> map = Maps.newHashMap();
            JsonArray jsonArray = ChatDeserializer.getAsJsonArray(jsonObject, "stats", (JsonArray)null);
            if (jsonArray != null) {
                for(JsonElement jsonElement : jsonArray) {
                    JsonObject jsonObject2 = ChatDeserializer.convertToJsonObject(jsonElement, "stats entry");
                    MinecraftKey resourceLocation = new MinecraftKey(ChatDeserializer.getAsString(jsonObject2, "type"));
                    StatisticWrapper<?> statType = IRegistry.STAT_TYPE.get(resourceLocation);
                    if (statType == null) {
                        throw new JsonParseException("Invalid stat type: " + resourceLocation);
                    }

                    MinecraftKey resourceLocation2 = new MinecraftKey(ChatDeserializer.getAsString(jsonObject2, "stat"));
                    Statistic<?> stat = getStat(statType, resourceLocation2);
                    CriterionConditionValue.IntegerRange ints2 = CriterionConditionValue.IntegerRange.fromJson(jsonObject2.get("value"));
                    map.put(stat, ints2);
                }
            }

            Object2BooleanMap<MinecraftKey> object2BooleanMap = new Object2BooleanOpenHashMap<>();
            JsonObject jsonObject3 = ChatDeserializer.getAsJsonObject(jsonObject, "recipes", new JsonObject());

            for(Entry<String, JsonElement> entry : jsonObject3.entrySet()) {
                MinecraftKey resourceLocation3 = new MinecraftKey(entry.getKey());
                boolean bl = ChatDeserializer.convertToBoolean(entry.getValue(), "recipe present");
                object2BooleanMap.put(resourceLocation3, bl);
            }

            Map<MinecraftKey, CriterionConditionPlayer.AdvancementPredicate> map2 = Maps.newHashMap();
            JsonObject jsonObject4 = ChatDeserializer.getAsJsonObject(jsonObject, "advancements", new JsonObject());

            for(Entry<String, JsonElement> entry2 : jsonObject4.entrySet()) {
                MinecraftKey resourceLocation4 = new MinecraftKey(entry2.getKey());
                CriterionConditionPlayer.AdvancementPredicate advancementPredicate = advancementPredicateFromJson(entry2.getValue());
                map2.put(resourceLocation4, advancementPredicate);
            }

            CriterionConditionEntity entityPredicate = CriterionConditionEntity.fromJson(jsonObject.get("looking_at"));
            return new CriterionConditionPlayer(ints, gameType, map, object2BooleanMap, map2, entityPredicate);
        } else {
            return ANY;
        }
    }

    private static <T> Statistic<T> getStat(StatisticWrapper<T> type, MinecraftKey id) {
        IRegistry<T> registry = type.getRegistry();
        T object = registry.get(id);
        if (object == null) {
            throw new JsonParseException("Unknown object " + id + " for stat type " + IRegistry.STAT_TYPE.getKey(type));
        } else {
            return type.get(object);
        }
    }

    private static <T> MinecraftKey getStatValueId(Statistic<T> stat) {
        return stat.getWrapper().getRegistry().getKey(stat.getValue());
    }

    public JsonElement serializeToJson() {
        if (this == ANY) {
            return JsonNull.INSTANCE;
        } else {
            JsonObject jsonObject = new JsonObject();
            jsonObject.add("level", this.level.serializeToJson());
            if (this.gameType != null) {
                jsonObject.addProperty("gamemode", this.gameType.getName());
            }

            if (!this.stats.isEmpty()) {
                JsonArray jsonArray = new JsonArray();
                this.stats.forEach((stat, ints) -> {
                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty("type", IRegistry.STAT_TYPE.getKey(stat.getWrapper()).toString());
                    jsonObject.addProperty("stat", getStatValueId(stat).toString());
                    jsonObject.add("value", ints.serializeToJson());
                    jsonArray.add(jsonObject);
                });
                jsonObject.add("stats", jsonArray);
            }

            if (!this.recipes.isEmpty()) {
                JsonObject jsonObject2 = new JsonObject();
                this.recipes.forEach((id, boolean_) -> {
                    jsonObject2.addProperty(id.toString(), boolean_);
                });
                jsonObject.add("recipes", jsonObject2);
            }

            if (!this.advancements.isEmpty()) {
                JsonObject jsonObject3 = new JsonObject();
                this.advancements.forEach((id, advancementPredicate) -> {
                    jsonObject3.add(id.toString(), advancementPredicate.toJson());
                });
                jsonObject.add("advancements", jsonObject3);
            }

            jsonObject.add("looking_at", this.lookingAt.serializeToJson());
            return jsonObject;
        }
    }

    static class AdvancementCriterionsPredicate implements CriterionConditionPlayer.AdvancementPredicate {
        private final Object2BooleanMap<String> criterions;

        public AdvancementCriterionsPredicate(Object2BooleanMap<String> criteria) {
            this.criterions = criteria;
        }

        @Override
        public JsonElement toJson() {
            JsonObject jsonObject = new JsonObject();
            this.criterions.forEach(jsonObject::addProperty);
            return jsonObject;
        }

        @Override
        public boolean test(AdvancementProgress advancementProgress) {
            for(it.unimi.dsi.fastutil.objects.Object2BooleanMap.Entry<String> entry : this.criterions.object2BooleanEntrySet()) {
                CriterionProgress criterionProgress = advancementProgress.getCriterionProgress(entry.getKey());
                if (criterionProgress == null || criterionProgress.isDone() != entry.getBooleanValue()) {
                    return false;
                }
            }

            return true;
        }
    }

    static class AdvancementDonePredicate implements CriterionConditionPlayer.AdvancementPredicate {
        private final boolean state;

        public AdvancementDonePredicate(boolean done) {
            this.state = done;
        }

        @Override
        public JsonElement toJson() {
            return new JsonPrimitive(this.state);
        }

        @Override
        public boolean test(AdvancementProgress advancementProgress) {
            return advancementProgress.isDone() == this.state;
        }
    }

    interface AdvancementPredicate extends Predicate<AdvancementProgress> {
        JsonElement toJson();
    }

    public static class Builder {
        private CriterionConditionValue.IntegerRange level = CriterionConditionValue.IntegerRange.ANY;
        @Nullable
        private EnumGamemode gameType;
        private final Map<Statistic<?>, CriterionConditionValue.IntegerRange> stats = Maps.newHashMap();
        private final Object2BooleanMap<MinecraftKey> recipes = new Object2BooleanOpenHashMap<>();
        private final Map<MinecraftKey, CriterionConditionPlayer.AdvancementPredicate> advancements = Maps.newHashMap();
        private CriterionConditionEntity lookingAt = CriterionConditionEntity.ANY;

        public static CriterionConditionPlayer.Builder player() {
            return new CriterionConditionPlayer.Builder();
        }

        public CriterionConditionPlayer.Builder setLevel(CriterionConditionValue.IntegerRange experienceLevel) {
            this.level = experienceLevel;
            return this;
        }

        public CriterionConditionPlayer.Builder addStat(Statistic<?> stat, CriterionConditionValue.IntegerRange value) {
            this.stats.put(stat, value);
            return this;
        }

        public CriterionConditionPlayer.Builder addRecipe(MinecraftKey id, boolean unlocked) {
            this.recipes.put(id, unlocked);
            return this;
        }

        public CriterionConditionPlayer.Builder setGameType(EnumGamemode gameMode) {
            this.gameType = gameMode;
            return this;
        }

        public CriterionConditionPlayer.Builder setLookingAt(CriterionConditionEntity lookingAt) {
            this.lookingAt = lookingAt;
            return this;
        }

        public CriterionConditionPlayer.Builder checkAdvancementDone(MinecraftKey id, boolean done) {
            this.advancements.put(id, new CriterionConditionPlayer.AdvancementDonePredicate(done));
            return this;
        }

        public CriterionConditionPlayer.Builder checkAdvancementCriterions(MinecraftKey id, Map<String, Boolean> criteria) {
            this.advancements.put(id, new CriterionConditionPlayer.AdvancementCriterionsPredicate(new Object2BooleanOpenHashMap<>(criteria)));
            return this;
        }

        public CriterionConditionPlayer build() {
            return new CriterionConditionPlayer(this.level, this.gameType, this.stats, this.recipes, this.advancements, this.lookingAt);
        }
    }
}
