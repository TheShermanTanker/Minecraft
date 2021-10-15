package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.monster.EntityZombie;
import net.minecraft.world.entity.npc.EntityVillager;
import net.minecraft.world.level.storage.loot.LootTableInfo;

public class CriterionTriggerCuredZombieVillager extends CriterionTriggerAbstract<CriterionTriggerCuredZombieVillager.TriggerInstance> {
    static final MinecraftKey ID = new MinecraftKey("cured_zombie_villager");

    @Override
    public MinecraftKey getId() {
        return ID;
    }

    @Override
    public CriterionTriggerCuredZombieVillager.TriggerInstance createInstance(JsonObject jsonObject, CriterionConditionEntity.Composite composite, LootDeserializationContext deserializationContext) {
        CriterionConditionEntity.Composite composite2 = CriterionConditionEntity.Composite.fromJson(jsonObject, "zombie", deserializationContext);
        CriterionConditionEntity.Composite composite3 = CriterionConditionEntity.Composite.fromJson(jsonObject, "villager", deserializationContext);
        return new CriterionTriggerCuredZombieVillager.TriggerInstance(composite, composite2, composite3);
    }

    public void trigger(EntityPlayer player, EntityZombie zombie, EntityVillager villager) {
        LootTableInfo lootContext = CriterionConditionEntity.createContext(player, zombie);
        LootTableInfo lootContext2 = CriterionConditionEntity.createContext(player, villager);
        this.trigger(player, (conditions) -> {
            return conditions.matches(lootContext, lootContext2);
        });
    }

    public static class TriggerInstance extends CriterionInstanceAbstract {
        private final CriterionConditionEntity.Composite zombie;
        private final CriterionConditionEntity.Composite villager;

        public TriggerInstance(CriterionConditionEntity.Composite player, CriterionConditionEntity.Composite zombie, CriterionConditionEntity.Composite villager) {
            super(CriterionTriggerCuredZombieVillager.ID, player);
            this.zombie = zombie;
            this.villager = villager;
        }

        public static CriterionTriggerCuredZombieVillager.TriggerInstance curedZombieVillager() {
            return new CriterionTriggerCuredZombieVillager.TriggerInstance(CriterionConditionEntity.Composite.ANY, CriterionConditionEntity.Composite.ANY, CriterionConditionEntity.Composite.ANY);
        }

        public boolean matches(LootTableInfo zombieContext, LootTableInfo villagerContext) {
            if (!this.zombie.matches(zombieContext)) {
                return false;
            } else {
                return this.villager.matches(villagerContext);
            }
        }

        @Override
        public JsonObject serializeToJson(LootSerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            jsonObject.add("zombie", this.zombie.toJson(predicateSerializer));
            jsonObject.add("villager", this.villager.toJson(predicateSerializer));
            return jsonObject;
        }
    }
}
