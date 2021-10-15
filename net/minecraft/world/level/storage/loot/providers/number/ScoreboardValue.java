package net.minecraft.world.level.storage.loot.providers.number;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.Set;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.level.storage.loot.LootSerializer;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameter;
import net.minecraft.world.level.storage.loot.providers.score.ContextScoreboardNameProvider;
import net.minecraft.world.level.storage.loot.providers.score.ScoreboardNameProvider;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.ScoreboardObjective;

public class ScoreboardValue implements NumberProvider {
    final ScoreboardNameProvider target;
    final String score;
    final float scale;

    ScoreboardValue(ScoreboardNameProvider scoreboardNameProvider, String string, float f) {
        this.target = scoreboardNameProvider;
        this.score = string;
        this.scale = f;
    }

    @Override
    public LootNumberProviderType getType() {
        return NumberProviders.SCORE;
    }

    @Override
    public Set<LootContextParameter<?>> getReferencedContextParams() {
        return this.target.getReferencedContextParams();
    }

    public static ScoreboardValue fromScoreboard(LootTableInfo.EntityTarget target, String score) {
        return fromScoreboard(target, score, 1.0F);
    }

    public static ScoreboardValue fromScoreboard(LootTableInfo.EntityTarget target, String score, float scale) {
        return new ScoreboardValue(ContextScoreboardNameProvider.forTarget(target), score, scale);
    }

    @Override
    public float getFloat(LootTableInfo context) {
        String string = this.target.getScoreboardName(context);
        if (string == null) {
            return 0.0F;
        } else {
            Scoreboard scoreboard = context.getWorld().getScoreboard();
            ScoreboardObjective objective = scoreboard.getObjective(this.score);
            if (objective == null) {
                return 0.0F;
            } else {
                return !scoreboard.hasPlayerScore(string, objective) ? 0.0F : (float)scoreboard.getPlayerScoreForObjective(string, objective).getScore() * this.scale;
            }
        }
    }

    public static class Serializer implements LootSerializer<ScoreboardValue> {
        @Override
        public ScoreboardValue deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext) {
            String string = ChatDeserializer.getAsString(jsonObject, "score");
            float f = ChatDeserializer.getAsFloat(jsonObject, "scale", 1.0F);
            ScoreboardNameProvider scoreboardNameProvider = ChatDeserializer.getAsObject(jsonObject, "target", jsonDeserializationContext, ScoreboardNameProvider.class);
            return new ScoreboardValue(scoreboardNameProvider, string, f);
        }

        @Override
        public void serialize(JsonObject json, ScoreboardValue object, JsonSerializationContext context) {
            json.addProperty("score", object.score);
            json.add("target", context.serialize(object.target));
            json.addProperty("scale", object.scale);
        }
    }
}
