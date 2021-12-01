package net.minecraft.advancements.critereon;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.ChatDeserializer;

public class CriterionConditionLight {
    public static final CriterionConditionLight ANY = new CriterionConditionLight(CriterionConditionValue.IntegerRange.ANY);
    private final CriterionConditionValue.IntegerRange composite;

    CriterionConditionLight(CriterionConditionValue.IntegerRange range) {
        this.composite = range;
    }

    public boolean matches(WorldServer world, BlockPosition pos) {
        if (this == ANY) {
            return true;
        } else if (!world.isLoaded(pos)) {
            return false;
        } else {
            return this.composite.matches(world.getLightLevel(pos));
        }
    }

    public JsonElement serializeToJson() {
        if (this == ANY) {
            return JsonNull.INSTANCE;
        } else {
            JsonObject jsonObject = new JsonObject();
            jsonObject.add("light", this.composite.serializeToJson());
            return jsonObject;
        }
    }

    public static CriterionConditionLight fromJson(@Nullable JsonElement json) {
        if (json != null && !json.isJsonNull()) {
            JsonObject jsonObject = ChatDeserializer.convertToJsonObject(json, "light");
            CriterionConditionValue.IntegerRange ints = CriterionConditionValue.IntegerRange.fromJson(jsonObject.get("light"));
            return new CriterionConditionLight(ints);
        } else {
            return ANY;
        }
    }

    public static class Builder {
        private CriterionConditionValue.IntegerRange composite = CriterionConditionValue.IntegerRange.ANY;

        public static CriterionConditionLight.Builder light() {
            return new CriterionConditionLight.Builder();
        }

        public CriterionConditionLight.Builder setComposite(CriterionConditionValue.IntegerRange light) {
            this.composite = light;
            return this;
        }

        public CriterionConditionLight build() {
            return new CriterionConditionLight(this.composite);
        }
    }
}
