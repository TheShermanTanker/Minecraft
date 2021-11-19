package net.minecraft.advancements.critereon;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.storage.loot.LootTableInfo;

public class CriterionTriggerKilledByCrossbow extends CriterionTriggerAbstract<CriterionTriggerKilledByCrossbow.CriterionInstanceTrigger> {
    static final MinecraftKey ID = new MinecraftKey("killed_by_crossbow");

    @Override
    public MinecraftKey getId() {
        return ID;
    }

    @Override
    public CriterionTriggerKilledByCrossbow.CriterionInstanceTrigger createInstance(JsonObject jsonObject, CriterionConditionEntity.Composite composite, LootDeserializationContext deserializationContext) {
        CriterionConditionEntity.Composite[] composites = CriterionConditionEntity.Composite.fromJsonArray(jsonObject, "victims", deserializationContext);
        CriterionConditionValue.IntegerRange ints = CriterionConditionValue.IntegerRange.fromJson(jsonObject.get("unique_entity_types"));
        return new CriterionTriggerKilledByCrossbow.CriterionInstanceTrigger(composite, composites, ints);
    }

    public void trigger(EntityPlayer player, Collection<Entity> piercingKilledEntities) {
        List<LootTableInfo> list = Lists.newArrayList();
        Set<EntityTypes<?>> set = Sets.newHashSet();

        for(Entity entity : piercingKilledEntities) {
            set.add(entity.getEntityType());
            list.add(CriterionConditionEntity.createContext(player, entity));
        }

        this.trigger(player, (conditions) -> {
            return conditions.matches(list, set.size());
        });
    }

    public static class CriterionInstanceTrigger extends CriterionInstanceAbstract {
        private final CriterionConditionEntity.Composite[] victims;
        private final CriterionConditionValue.IntegerRange uniqueEntityTypes;

        public CriterionInstanceTrigger(CriterionConditionEntity.Composite player, CriterionConditionEntity.Composite[] victims, CriterionConditionValue.IntegerRange uniqueEntityTypes) {
            super(CriterionTriggerKilledByCrossbow.ID, player);
            this.victims = victims;
            this.uniqueEntityTypes = uniqueEntityTypes;
        }

        public static CriterionTriggerKilledByCrossbow.CriterionInstanceTrigger crossbowKilled(CriterionConditionEntity.Builder... victimPredicates) {
            CriterionConditionEntity.Composite[] composites = new CriterionConditionEntity.Composite[victimPredicates.length];

            for(int i = 0; i < victimPredicates.length; ++i) {
                CriterionConditionEntity.Builder builder = victimPredicates[i];
                composites[i] = CriterionConditionEntity.Composite.wrap(builder.build());
            }

            return new CriterionTriggerKilledByCrossbow.CriterionInstanceTrigger(CriterionConditionEntity.Composite.ANY, composites, CriterionConditionValue.IntegerRange.ANY);
        }

        public static CriterionTriggerKilledByCrossbow.CriterionInstanceTrigger crossbowKilled(CriterionConditionValue.IntegerRange uniqueEntityTypes) {
            CriterionConditionEntity.Composite[] composites = new CriterionConditionEntity.Composite[0];
            return new CriterionTriggerKilledByCrossbow.CriterionInstanceTrigger(CriterionConditionEntity.Composite.ANY, composites, uniqueEntityTypes);
        }

        public boolean matches(Collection<LootTableInfo> victimContexts, int uniqueEntityTypeCount) {
            if (this.victims.length > 0) {
                List<LootTableInfo> list = Lists.newArrayList(victimContexts);

                for(CriterionConditionEntity.Composite composite : this.victims) {
                    boolean bl = false;
                    Iterator<LootTableInfo> iterator = list.iterator();

                    while(iterator.hasNext()) {
                        LootTableInfo lootContext = iterator.next();
                        if (composite.matches(lootContext)) {
                            iterator.remove();
                            bl = true;
                            break;
                        }
                    }

                    if (!bl) {
                        return false;
                    }
                }
            }

            return this.uniqueEntityTypes.matches(uniqueEntityTypeCount);
        }

        @Override
        public JsonObject serializeToJson(LootSerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            jsonObject.add("victims", CriterionConditionEntity.Composite.toJson(this.victims, predicateSerializer));
            jsonObject.add("unique_entity_types", this.uniqueEntityTypes.serializeToJson());
            return jsonObject;
        }
    }
}
