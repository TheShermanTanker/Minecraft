package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.npc.EntityVillagerAbstract;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTableInfo;

public class CriterionTriggerVillagerTrade extends CriterionTriggerAbstract<CriterionTriggerVillagerTrade.CriterionInstanceTrigger> {
    static final MinecraftKey ID = new MinecraftKey("villager_trade");

    @Override
    public MinecraftKey getId() {
        return ID;
    }

    @Override
    public CriterionTriggerVillagerTrade.CriterionInstanceTrigger createInstance(JsonObject jsonObject, CriterionConditionEntity.Composite composite, LootDeserializationContext deserializationContext) {
        CriterionConditionEntity.Composite composite2 = CriterionConditionEntity.Composite.fromJson(jsonObject, "villager", deserializationContext);
        CriterionConditionItem itemPredicate = CriterionConditionItem.fromJson(jsonObject.get("item"));
        return new CriterionTriggerVillagerTrade.CriterionInstanceTrigger(composite, composite2, itemPredicate);
    }

    public void trigger(EntityPlayer player, EntityVillagerAbstract merchant, ItemStack stack) {
        LootTableInfo lootContext = CriterionConditionEntity.createContext(player, merchant);
        this.trigger(player, (conditions) -> {
            return conditions.matches(lootContext, stack);
        });
    }

    public static class CriterionInstanceTrigger extends CriterionInstanceAbstract {
        private final CriterionConditionEntity.Composite villager;
        private final CriterionConditionItem item;

        public CriterionInstanceTrigger(CriterionConditionEntity.Composite player, CriterionConditionEntity.Composite villager, CriterionConditionItem item) {
            super(CriterionTriggerVillagerTrade.ID, player);
            this.villager = villager;
            this.item = item;
        }

        public static CriterionTriggerVillagerTrade.CriterionInstanceTrigger tradedWithVillager() {
            return new CriterionTriggerVillagerTrade.CriterionInstanceTrigger(CriterionConditionEntity.Composite.ANY, CriterionConditionEntity.Composite.ANY, CriterionConditionItem.ANY);
        }

        public static CriterionTriggerVillagerTrade.CriterionInstanceTrigger tradedWithVillager(CriterionConditionEntity.Builder playerPredicate) {
            return new CriterionTriggerVillagerTrade.CriterionInstanceTrigger(CriterionConditionEntity.Composite.wrap(playerPredicate.build()), CriterionConditionEntity.Composite.ANY, CriterionConditionItem.ANY);
        }

        public boolean matches(LootTableInfo merchantContext, ItemStack stack) {
            if (!this.villager.matches(merchantContext)) {
                return false;
            } else {
                return this.item.matches(stack);
            }
        }

        @Override
        public JsonObject serializeToJson(LootSerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            jsonObject.add("item", this.item.serializeToJson());
            jsonObject.add("villager", this.villager.toJson(predicateSerializer));
            return jsonObject;
        }
    }
}
