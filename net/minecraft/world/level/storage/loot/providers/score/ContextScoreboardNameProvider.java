package net.minecraft.world.level.storage.loot.providers.score;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.JsonRegistry;
import net.minecraft.world.level.storage.loot.LootSerializer;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameter;

public class ContextScoreboardNameProvider implements ScoreboardNameProvider {
    final LootTableInfo.EntityTarget target;

    ContextScoreboardNameProvider(LootTableInfo.EntityTarget target) {
        this.target = target;
    }

    public static ScoreboardNameProvider forTarget(LootTableInfo.EntityTarget target) {
        return new ContextScoreboardNameProvider(target);
    }

    @Override
    public LootScoreProviderType getType() {
        return ScoreboardNameProviders.CONTEXT;
    }

    @Nullable
    @Override
    public String getScoreboardName(LootTableInfo context) {
        Entity entity = context.getContextParameter(this.target.getParam());
        return entity != null ? entity.getName() : null;
    }

    @Override
    public Set<LootContextParameter<?>> getReferencedContextParams() {
        return ImmutableSet.of(this.target.getParam());
    }

    public static class InlineSerializer implements JsonRegistry.InlineSerializer<ContextScoreboardNameProvider> {
        @Override
        public JsonElement serialize(ContextScoreboardNameProvider object, JsonSerializationContext context) {
            return context.serialize(object.target);
        }

        @Override
        public ContextScoreboardNameProvider deserialize(JsonElement jsonElement, JsonDeserializationContext jsonDeserializationContext) {
            LootTableInfo.EntityTarget entityTarget = jsonDeserializationContext.deserialize(jsonElement, LootTableInfo.EntityTarget.class);
            return new ContextScoreboardNameProvider(entityTarget);
        }
    }

    public static class Serializer implements LootSerializer<ContextScoreboardNameProvider> {
        @Override
        public void serialize(JsonObject json, ContextScoreboardNameProvider object, JsonSerializationContext context) {
            json.addProperty("target", object.target.name());
        }

        @Override
        public ContextScoreboardNameProvider deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext) {
            LootTableInfo.EntityTarget entityTarget = ChatDeserializer.getAsObject(jsonObject, "target", jsonDeserializationContext, LootTableInfo.EntityTarget.class);
            return new ContextScoreboardNameProvider(entityTarget);
        }
    }
}
