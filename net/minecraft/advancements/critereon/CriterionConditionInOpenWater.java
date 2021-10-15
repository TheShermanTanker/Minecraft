package net.minecraft.advancements.critereon;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import javax.annotation.Nullable;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.EntityFishingHook;

public class CriterionConditionInOpenWater {
    public static final CriterionConditionInOpenWater ANY = new CriterionConditionInOpenWater(false);
    private static final String IN_OPEN_WATER_KEY = "in_open_water";
    private final boolean inOpenWater;

    private CriterionConditionInOpenWater(boolean inOpenWater) {
        this.inOpenWater = inOpenWater;
    }

    public static CriterionConditionInOpenWater inOpenWater(boolean inOpenWater) {
        return new CriterionConditionInOpenWater(inOpenWater);
    }

    public static CriterionConditionInOpenWater fromJson(@Nullable JsonElement json) {
        if (json != null && !json.isJsonNull()) {
            JsonObject jsonObject = ChatDeserializer.convertToJsonObject(json, "fishing_hook");
            JsonElement jsonElement = jsonObject.get("in_open_water");
            return jsonElement != null ? new CriterionConditionInOpenWater(ChatDeserializer.convertToBoolean(jsonElement, "in_open_water")) : ANY;
        } else {
            return ANY;
        }
    }

    public JsonElement serializeToJson() {
        if (this == ANY) {
            return JsonNull.INSTANCE;
        } else {
            JsonObject jsonObject = new JsonObject();
            jsonObject.add("in_open_water", new JsonPrimitive(this.inOpenWater));
            return jsonObject;
        }
    }

    public boolean matches(Entity entity) {
        if (this == ANY) {
            return true;
        } else if (!(entity instanceof EntityFishingHook)) {
            return false;
        } else {
            EntityFishingHook fishingHook = (EntityFishingHook)entity;
            return this.inOpenWater == fishingHook.isInOpenWater();
        }
    }
}
