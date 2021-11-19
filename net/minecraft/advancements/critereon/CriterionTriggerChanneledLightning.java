package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootTableInfo;

public class CriterionTriggerChanneledLightning extends CriterionTriggerAbstract<CriterionTriggerChanneledLightning.CriterionInstanceTrigger> {
    static final MinecraftKey ID = new MinecraftKey("channeled_lightning");

    @Override
    public MinecraftKey getId() {
        return ID;
    }

    @Override
    public CriterionTriggerChanneledLightning.CriterionInstanceTrigger createInstance(JsonObject jsonObject, CriterionConditionEntity.Composite composite, LootDeserializationContext deserializationContext) {
        CriterionConditionEntity.Composite[] composites = CriterionConditionEntity.Composite.fromJsonArray(jsonObject, "victims", deserializationContext);
        return new CriterionTriggerChanneledLightning.CriterionInstanceTrigger(composite, composites);
    }

    public void trigger(EntityPlayer player, Collection<? extends Entity> victims) {
        List<LootTableInfo> list = victims.stream().map((entity) -> {
            return CriterionConditionEntity.createContext(player, entity);
        }).collect(Collectors.toList());
        this.trigger(player, (conditions) -> {
            return conditions.matches(list);
        });
    }

    public static class CriterionInstanceTrigger extends CriterionInstanceAbstract {
        private final CriterionConditionEntity.Composite[] victims;

        public CriterionInstanceTrigger(CriterionConditionEntity.Composite player, CriterionConditionEntity.Composite[] victims) {
            super(CriterionTriggerChanneledLightning.ID, player);
            this.victims = victims;
        }

        public static CriterionTriggerChanneledLightning.CriterionInstanceTrigger channeledLightning(CriterionConditionEntity... victims) {
            return new CriterionTriggerChanneledLightning.CriterionInstanceTrigger(CriterionConditionEntity.Composite.ANY, Stream.of(victims).map(CriterionConditionEntity.Composite::wrap).toArray((i) -> {
                return new CriterionConditionEntity.Composite[i];
            }));
        }

        public boolean matches(Collection<? extends LootTableInfo> victims) {
            for(CriterionConditionEntity.Composite composite : this.victims) {
                boolean bl = false;

                for(LootTableInfo lootContext : victims) {
                    if (composite.matches(lootContext)) {
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

        @Override
        public JsonObject serializeToJson(LootSerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            jsonObject.add("victims", CriterionConditionEntity.Composite.toJson(this.victims, predicateSerializer));
            return jsonObject;
        }
    }
}
