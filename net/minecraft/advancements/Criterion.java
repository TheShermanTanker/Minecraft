package net.minecraft.advancements;

import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.advancements.critereon.LootDeserializationContext;
import net.minecraft.advancements.critereon.LootSerializationContext;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.util.ChatDeserializer;

public class Criterion {
    @Nullable
    private final CriterionInstance trigger;

    public Criterion(CriterionInstance conditions) {
        this.trigger = conditions;
    }

    public Criterion() {
        this.trigger = null;
    }

    public void serializeToNetwork(PacketDataSerializer buf) {
    }

    public static Criterion criterionFromJson(JsonObject obj, LootDeserializationContext predicateDeserializer) {
        MinecraftKey resourceLocation = new MinecraftKey(ChatDeserializer.getAsString(obj, "trigger"));
        CriterionTrigger<?> criterionTrigger = CriterionTriggers.getCriterion(resourceLocation);
        if (criterionTrigger == null) {
            throw new JsonSyntaxException("Invalid criterion trigger: " + resourceLocation);
        } else {
            CriterionInstance criterionTriggerInstance = criterionTrigger.createInstance(ChatDeserializer.getAsJsonObject(obj, "conditions", new JsonObject()), predicateDeserializer);
            return new Criterion(criterionTriggerInstance);
        }
    }

    public static Criterion criterionFromNetwork(PacketDataSerializer buf) {
        return new Criterion();
    }

    public static Map<String, Criterion> criteriaFromJson(JsonObject obj, LootDeserializationContext predicateDeserializer) {
        Map<String, Criterion> map = Maps.newHashMap();

        for(Entry<String, JsonElement> entry : obj.entrySet()) {
            map.put(entry.getKey(), criterionFromJson(ChatDeserializer.convertToJsonObject(entry.getValue(), "criterion"), predicateDeserializer));
        }

        return map;
    }

    public static Map<String, Criterion> criteriaFromNetwork(PacketDataSerializer buf) {
        return buf.readMap(PacketDataSerializer::readUtf, Criterion::criterionFromNetwork);
    }

    public static void serializeToNetwork(Map<String, Criterion> criteria, PacketDataSerializer buf) {
        buf.writeMap(criteria, PacketDataSerializer::writeUtf, (bufx, criterion) -> {
            criterion.serializeToNetwork(bufx);
        });
    }

    @Nullable
    public CriterionInstance getTrigger() {
        return this.trigger;
    }

    public JsonElement serializeToJson() {
        if (this.trigger == null) {
            throw new JsonSyntaxException("Missing trigger");
        } else {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("trigger", this.trigger.getCriterion().toString());
            JsonObject jsonObject2 = this.trigger.serializeToJson(LootSerializationContext.INSTANCE);
            if (jsonObject2.size() != 0) {
                jsonObject.add("conditions", jsonObject2);
            }

            return jsonObject;
        }
    }
}
