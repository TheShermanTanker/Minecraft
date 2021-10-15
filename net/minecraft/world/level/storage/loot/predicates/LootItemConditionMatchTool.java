package net.minecraft.world.level.storage.loot.predicates;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.Set;
import net.minecraft.advancements.critereon.CriterionConditionItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootSerializer;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameter;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameters;

public class LootItemConditionMatchTool implements LootItemCondition {
    final CriterionConditionItem predicate;

    public LootItemConditionMatchTool(CriterionConditionItem predicate) {
        this.predicate = predicate;
    }

    @Override
    public LootItemConditionType getType() {
        return LootItemConditions.MATCH_TOOL;
    }

    @Override
    public Set<LootContextParameter<?>> getReferencedContextParams() {
        return ImmutableSet.of(LootContextParameters.TOOL);
    }

    @Override
    public boolean test(LootTableInfo lootContext) {
        ItemStack itemStack = lootContext.getContextParameter(LootContextParameters.TOOL);
        return itemStack != null && this.predicate.matches(itemStack);
    }

    public static LootItemCondition.Builder toolMatches(CriterionConditionItem.Builder predicate) {
        return () -> {
            return new LootItemConditionMatchTool(predicate.build());
        };
    }

    public static class Serializer implements LootSerializer<LootItemConditionMatchTool> {
        @Override
        public void serialize(JsonObject json, LootItemConditionMatchTool object, JsonSerializationContext context) {
            json.add("predicate", object.predicate.serializeToJson());
        }

        @Override
        public LootItemConditionMatchTool deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext) {
            CriterionConditionItem itemPredicate = CriterionConditionItem.fromJson(jsonObject.get("predicate"));
            return new LootItemConditionMatchTool(itemPredicate);
        }
    }
}
