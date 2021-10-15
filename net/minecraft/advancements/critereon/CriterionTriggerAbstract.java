package net.minecraft.advancements.critereon;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.server.AdvancementDataPlayer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.level.storage.loot.LootTableInfo;

public abstract class CriterionTriggerAbstract<T extends CriterionInstanceAbstract> implements CriterionTrigger<T> {
    private final Map<AdvancementDataPlayer, Set<CriterionTrigger.Listener<T>>> players = Maps.newIdentityHashMap();

    @Override
    public final void addPlayerListener(AdvancementDataPlayer manager, CriterionTrigger.Listener<T> conditions) {
        this.players.computeIfAbsent(manager, (managerx) -> {
            return Sets.newHashSet();
        }).add(conditions);
    }

    @Override
    public final void removePlayerListener(AdvancementDataPlayer manager, CriterionTrigger.Listener<T> conditions) {
        Set<CriterionTrigger.Listener<T>> set = this.players.get(manager);
        if (set != null) {
            set.remove(conditions);
            if (set.isEmpty()) {
                this.players.remove(manager);
            }
        }

    }

    @Override
    public final void removePlayerListeners(AdvancementDataPlayer tracker) {
        this.players.remove(tracker);
    }

    protected abstract T createInstance(JsonObject obj, CriterionConditionEntity.Composite playerPredicate, LootDeserializationContext predicateDeserializer);

    @Override
    public final T createInstance(JsonObject jsonObject, LootDeserializationContext deserializationContext) {
        CriterionConditionEntity.Composite composite = CriterionConditionEntity.Composite.fromJson(jsonObject, "player", deserializationContext);
        return this.createInstance(jsonObject, composite, deserializationContext);
    }

    protected void trigger(EntityPlayer player, Predicate<T> tester) {
        AdvancementDataPlayer playerAdvancements = player.getAdvancementData();
        Set<CriterionTrigger.Listener<T>> set = this.players.get(playerAdvancements);
        if (set != null && !set.isEmpty()) {
            LootTableInfo lootContext = CriterionConditionEntity.createContext(player, player);
            List<CriterionTrigger.Listener<T>> list = null;

            for(CriterionTrigger.Listener<T> listener : set) {
                T abstractCriterionTriggerInstance = listener.getTriggerInstance();
                if (tester.test(abstractCriterionTriggerInstance) && abstractCriterionTriggerInstance.getPlayerPredicate().matches(lootContext)) {
                    if (list == null) {
                        list = Lists.newArrayList();
                    }

                    list.add(listener);
                }
            }

            if (list != null) {
                for(CriterionTrigger.Listener<T> listener2 : list) {
                    listener2.run(playerAdvancements);
                }
            }

        }
    }
}
