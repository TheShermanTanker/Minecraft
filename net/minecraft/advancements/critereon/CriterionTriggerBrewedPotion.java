package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import javax.annotation.Nullable;
import net.minecraft.core.IRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.item.alchemy.PotionRegistry;

public class CriterionTriggerBrewedPotion extends CriterionTriggerAbstract<CriterionTriggerBrewedPotion.CriterionInstanceTrigger> {
    static final MinecraftKey ID = new MinecraftKey("brewed_potion");

    @Override
    public MinecraftKey getId() {
        return ID;
    }

    @Override
    public CriterionTriggerBrewedPotion.CriterionInstanceTrigger createInstance(JsonObject jsonObject, CriterionConditionEntity.Composite composite, LootDeserializationContext deserializationContext) {
        PotionRegistry potion = null;
        if (jsonObject.has("potion")) {
            MinecraftKey resourceLocation = new MinecraftKey(ChatDeserializer.getAsString(jsonObject, "potion"));
            potion = IRegistry.POTION.getOptional(resourceLocation).orElseThrow(() -> {
                return new JsonSyntaxException("Unknown potion '" + resourceLocation + "'");
            });
        }

        return new CriterionTriggerBrewedPotion.CriterionInstanceTrigger(composite, potion);
    }

    public void trigger(EntityPlayer player, PotionRegistry potion) {
        this.trigger(player, (conditions) -> {
            return conditions.matches(potion);
        });
    }

    public static class CriterionInstanceTrigger extends CriterionInstanceAbstract {
        @Nullable
        private final PotionRegistry potion;

        public CriterionInstanceTrigger(CriterionConditionEntity.Composite player, @Nullable PotionRegistry potion) {
            super(CriterionTriggerBrewedPotion.ID, player);
            this.potion = potion;
        }

        public static CriterionTriggerBrewedPotion.CriterionInstanceTrigger brewedPotion() {
            return new CriterionTriggerBrewedPotion.CriterionInstanceTrigger(CriterionConditionEntity.Composite.ANY, (PotionRegistry)null);
        }

        public boolean matches(PotionRegistry potion) {
            return this.potion == null || this.potion == potion;
        }

        @Override
        public JsonObject serializeToJson(LootSerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            if (this.potion != null) {
                jsonObject.addProperty("potion", IRegistry.POTION.getKey(this.potion).toString());
            }

            return jsonObject;
        }
    }
}
