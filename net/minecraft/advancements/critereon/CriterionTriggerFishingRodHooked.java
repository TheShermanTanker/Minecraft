package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Collection;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.projectile.EntityFishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameters;

public class CriterionTriggerFishingRodHooked extends CriterionTriggerAbstract<CriterionTriggerFishingRodHooked.TriggerInstance> {
    static final MinecraftKey ID = new MinecraftKey("fishing_rod_hooked");

    @Override
    public MinecraftKey getId() {
        return ID;
    }

    @Override
    public CriterionTriggerFishingRodHooked.TriggerInstance createInstance(JsonObject jsonObject, CriterionConditionEntity.Composite composite, LootDeserializationContext deserializationContext) {
        CriterionConditionItem itemPredicate = CriterionConditionItem.fromJson(jsonObject.get("rod"));
        CriterionConditionEntity.Composite composite2 = CriterionConditionEntity.Composite.fromJson(jsonObject, "entity", deserializationContext);
        CriterionConditionItem itemPredicate2 = CriterionConditionItem.fromJson(jsonObject.get("item"));
        return new CriterionTriggerFishingRodHooked.TriggerInstance(composite, itemPredicate, composite2, itemPredicate2);
    }

    public void trigger(EntityPlayer player, ItemStack rod, EntityFishingHook bobber, Collection<ItemStack> fishingLoots) {
        LootTableInfo lootContext = CriterionConditionEntity.createContext(player, (Entity)(bobber.getHooked() != null ? bobber.getHooked() : bobber));
        this.trigger(player, (conditions) -> {
            return conditions.matches(rod, lootContext, fishingLoots);
        });
    }

    public static class TriggerInstance extends CriterionInstanceAbstract {
        private final CriterionConditionItem rod;
        private final CriterionConditionEntity.Composite entity;
        private final CriterionConditionItem item;

        public TriggerInstance(CriterionConditionEntity.Composite player, CriterionConditionItem rod, CriterionConditionEntity.Composite hookedEntity, CriterionConditionItem caughtItem) {
            super(CriterionTriggerFishingRodHooked.ID, player);
            this.rod = rod;
            this.entity = hookedEntity;
            this.item = caughtItem;
        }

        public static CriterionTriggerFishingRodHooked.TriggerInstance fishedItem(CriterionConditionItem rod, CriterionConditionEntity bobber, CriterionConditionItem item) {
            return new CriterionTriggerFishingRodHooked.TriggerInstance(CriterionConditionEntity.Composite.ANY, rod, CriterionConditionEntity.Composite.wrap(bobber), item);
        }

        public boolean matches(ItemStack rod, LootTableInfo hookedEntityContext, Collection<ItemStack> fishingLoots) {
            if (!this.rod.matches(rod)) {
                return false;
            } else if (!this.entity.matches(hookedEntityContext)) {
                return false;
            } else {
                if (this.item != CriterionConditionItem.ANY) {
                    boolean bl = false;
                    Entity entity = hookedEntityContext.getContextParameter(LootContextParameters.THIS_ENTITY);
                    if (entity instanceof EntityItem) {
                        EntityItem itemEntity = (EntityItem)entity;
                        if (this.item.matches(itemEntity.getItemStack())) {
                            bl = true;
                        }
                    }

                    for(ItemStack itemStack : fishingLoots) {
                        if (this.item.matches(itemStack)) {
                            bl = true;
                            break;
                        }
                    }

                    if (!bl) {
                        return false;
                    }
                }

                return true;
            }
        }

        @Override
        public JsonObject serializeToJson(LootSerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            jsonObject.add("rod", this.rod.serializeToJson());
            jsonObject.add("entity", this.entity.toJson(predicateSerializer));
            jsonObject.add("item", this.item.serializeToJson());
            return jsonObject;
        }
    }
}
