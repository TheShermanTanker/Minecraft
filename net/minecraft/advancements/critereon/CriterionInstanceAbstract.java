package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.advancements.CriterionInstance;
import net.minecraft.resources.MinecraftKey;

public abstract class CriterionInstanceAbstract implements CriterionInstance {
    private final MinecraftKey criterion;
    private final CriterionConditionEntity.Composite player;

    public CriterionInstanceAbstract(MinecraftKey id, CriterionConditionEntity.Composite playerPredicate) {
        this.criterion = id;
        this.player = playerPredicate;
    }

    @Override
    public MinecraftKey getCriterion() {
        return this.criterion;
    }

    protected CriterionConditionEntity.Composite getPlayerPredicate() {
        return this.player;
    }

    @Override
    public JsonObject serializeToJson(LootSerializationContext predicateSerializer) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("player", this.player.toJson(predicateSerializer));
        return jsonObject;
    }

    @Override
    public String toString() {
        return "AbstractCriterionInstance{criterion=" + this.criterion + "}";
    }
}
