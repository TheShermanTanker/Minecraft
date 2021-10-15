package net.minecraft.advancements.critereon;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import javax.annotation.Nullable;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.util.MathHelper;

public class CriterionConditionDistance {
    public static final CriterionConditionDistance ANY = new CriterionConditionDistance(CriterionConditionValue.DoubleRange.ANY, CriterionConditionValue.DoubleRange.ANY, CriterionConditionValue.DoubleRange.ANY, CriterionConditionValue.DoubleRange.ANY, CriterionConditionValue.DoubleRange.ANY);
    private final CriterionConditionValue.DoubleRange x;
    private final CriterionConditionValue.DoubleRange y;
    private final CriterionConditionValue.DoubleRange z;
    private final CriterionConditionValue.DoubleRange horizontal;
    private final CriterionConditionValue.DoubleRange absolute;

    public CriterionConditionDistance(CriterionConditionValue.DoubleRange x, CriterionConditionValue.DoubleRange y, CriterionConditionValue.DoubleRange z, CriterionConditionValue.DoubleRange horizontal, CriterionConditionValue.DoubleRange absolute) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.horizontal = horizontal;
        this.absolute = absolute;
    }

    public static CriterionConditionDistance horizontal(CriterionConditionValue.DoubleRange horizontal) {
        return new CriterionConditionDistance(CriterionConditionValue.DoubleRange.ANY, CriterionConditionValue.DoubleRange.ANY, CriterionConditionValue.DoubleRange.ANY, horizontal, CriterionConditionValue.DoubleRange.ANY);
    }

    public static CriterionConditionDistance vertical(CriterionConditionValue.DoubleRange y) {
        return new CriterionConditionDistance(CriterionConditionValue.DoubleRange.ANY, y, CriterionConditionValue.DoubleRange.ANY, CriterionConditionValue.DoubleRange.ANY, CriterionConditionValue.DoubleRange.ANY);
    }

    public static CriterionConditionDistance absolute(CriterionConditionValue.DoubleRange absolute) {
        return new CriterionConditionDistance(CriterionConditionValue.DoubleRange.ANY, CriterionConditionValue.DoubleRange.ANY, CriterionConditionValue.DoubleRange.ANY, CriterionConditionValue.DoubleRange.ANY, absolute);
    }

    public boolean matches(double x0, double y0, double z0, double x1, double y1, double z1) {
        float f = (float)(x0 - x1);
        float g = (float)(y0 - y1);
        float h = (float)(z0 - z1);
        if (this.x.matches((double)MathHelper.abs(f)) && this.y.matches((double)MathHelper.abs(g)) && this.z.matches((double)MathHelper.abs(h))) {
            if (!this.horizontal.matchesSqr((double)(f * f + h * h))) {
                return false;
            } else {
                return this.absolute.matchesSqr((double)(f * f + g * g + h * h));
            }
        } else {
            return false;
        }
    }

    public static CriterionConditionDistance fromJson(@Nullable JsonElement json) {
        if (json != null && !json.isJsonNull()) {
            JsonObject jsonObject = ChatDeserializer.convertToJsonObject(json, "distance");
            CriterionConditionValue.DoubleRange doubles = CriterionConditionValue.DoubleRange.fromJson(jsonObject.get("x"));
            CriterionConditionValue.DoubleRange doubles2 = CriterionConditionValue.DoubleRange.fromJson(jsonObject.get("y"));
            CriterionConditionValue.DoubleRange doubles3 = CriterionConditionValue.DoubleRange.fromJson(jsonObject.get("z"));
            CriterionConditionValue.DoubleRange doubles4 = CriterionConditionValue.DoubleRange.fromJson(jsonObject.get("horizontal"));
            CriterionConditionValue.DoubleRange doubles5 = CriterionConditionValue.DoubleRange.fromJson(jsonObject.get("absolute"));
            return new CriterionConditionDistance(doubles, doubles2, doubles3, doubles4, doubles5);
        } else {
            return ANY;
        }
    }

    public JsonElement serializeToJson() {
        if (this == ANY) {
            return JsonNull.INSTANCE;
        } else {
            JsonObject jsonObject = new JsonObject();
            jsonObject.add("x", this.x.serializeToJson());
            jsonObject.add("y", this.y.serializeToJson());
            jsonObject.add("z", this.z.serializeToJson());
            jsonObject.add("horizontal", this.horizontal.serializeToJson());
            jsonObject.add("absolute", this.absolute.serializeToJson());
            return jsonObject;
        }
    }
}
