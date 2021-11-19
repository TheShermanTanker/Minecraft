package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.IMaterial;

public class CriterionTriggerShotCrossbow extends CriterionTriggerAbstract<CriterionTriggerShotCrossbow.CriterionInstanceTrigger> {
    static final MinecraftKey ID = new MinecraftKey("shot_crossbow");

    @Override
    public MinecraftKey getId() {
        return ID;
    }

    @Override
    public CriterionTriggerShotCrossbow.CriterionInstanceTrigger createInstance(JsonObject jsonObject, CriterionConditionEntity.Composite composite, LootDeserializationContext deserializationContext) {
        CriterionConditionItem itemPredicate = CriterionConditionItem.fromJson(jsonObject.get("item"));
        return new CriterionTriggerShotCrossbow.CriterionInstanceTrigger(composite, itemPredicate);
    }

    public void trigger(EntityPlayer player, ItemStack stack) {
        this.trigger(player, (conditions) -> {
            return conditions.matches(stack);
        });
    }

    public static class CriterionInstanceTrigger extends CriterionInstanceAbstract {
        private final CriterionConditionItem item;

        public CriterionInstanceTrigger(CriterionConditionEntity.Composite player, CriterionConditionItem item) {
            super(CriterionTriggerShotCrossbow.ID, player);
            this.item = item;
        }

        public static CriterionTriggerShotCrossbow.CriterionInstanceTrigger shotCrossbow(CriterionConditionItem itemPredicate) {
            return new CriterionTriggerShotCrossbow.CriterionInstanceTrigger(CriterionConditionEntity.Composite.ANY, itemPredicate);
        }

        public static CriterionTriggerShotCrossbow.CriterionInstanceTrigger shotCrossbow(IMaterial item) {
            return new CriterionTriggerShotCrossbow.CriterionInstanceTrigger(CriterionConditionEntity.Composite.ANY, CriterionConditionItem.Builder.item().of(item).build());
        }

        public boolean matches(ItemStack stack) {
            return this.item.matches(stack);
        }

        @Override
        public JsonObject serializeToJson(LootSerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            jsonObject.add("item", this.item.serializeToJson());
            return jsonObject;
        }
    }
}
