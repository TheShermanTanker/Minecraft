package net.minecraft.advancements;

import com.google.gson.JsonObject;
import net.minecraft.advancements.critereon.LootDeserializationContext;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.AdvancementDataPlayer;

public interface CriterionTrigger<T extends CriterionInstance> {
    MinecraftKey getId();

    void addPlayerListener(AdvancementDataPlayer manager, CriterionTrigger.Listener<T> conditions);

    void removePlayerListener(AdvancementDataPlayer manager, CriterionTrigger.Listener<T> conditions);

    void removePlayerListeners(AdvancementDataPlayer tracker);

    T createInstance(JsonObject obj, LootDeserializationContext predicateDeserializer);

    public static class Listener<T extends CriterionInstance> {
        private final T trigger;
        private final Advancement advancement;
        private final String criterion;

        public Listener(T conditions, Advancement advancement, String id) {
            this.trigger = conditions;
            this.advancement = advancement;
            this.criterion = id;
        }

        public T getTriggerInstance() {
            return this.trigger;
        }

        public void run(AdvancementDataPlayer tracker) {
            tracker.grantCriteria(this.advancement, this.criterion);
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            } else if (object != null && this.getClass() == object.getClass()) {
                CriterionTrigger.Listener<?> listener = (CriterionTrigger.Listener)object;
                if (!this.trigger.equals(listener.trigger)) {
                    return false;
                } else {
                    return !this.advancement.equals(listener.advancement) ? false : this.criterion.equals(listener.criterion);
                }
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            int i = this.trigger.hashCode();
            i = 31 * i + this.advancement.hashCode();
            return 31 * i + this.criterion.hashCode();
        }
    }
}
