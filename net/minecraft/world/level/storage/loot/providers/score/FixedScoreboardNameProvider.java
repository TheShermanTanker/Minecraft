package net.minecraft.world.level.storage.loot.providers.score;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.level.storage.loot.LootSerializer;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameter;

public class FixedScoreboardNameProvider implements ScoreboardNameProvider {
    final String name;

    FixedScoreboardNameProvider(String string) {
        this.name = string;
    }

    public static ScoreboardNameProvider forName(String name) {
        return new FixedScoreboardNameProvider(name);
    }

    @Override
    public LootScoreProviderType getType() {
        return ScoreboardNameProviders.FIXED;
    }

    public String getName() {
        return this.name;
    }

    @Nullable
    @Override
    public String getScoreboardName(LootTableInfo context) {
        return this.name;
    }

    @Override
    public Set<LootContextParameter<?>> getReferencedContextParams() {
        return ImmutableSet.of();
    }

    public static class Serializer implements LootSerializer<FixedScoreboardNameProvider> {
        @Override
        public void serialize(JsonObject json, FixedScoreboardNameProvider object, JsonSerializationContext context) {
            json.addProperty("name", object.name);
        }

        @Override
        public FixedScoreboardNameProvider deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext) {
            String string = ChatDeserializer.getAsString(jsonObject, "name");
            return new FixedScoreboardNameProvider(string);
        }
    }
}
