package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import javax.annotation.Nullable;
import net.minecraft.core.IRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.level.World;

public class CriterionTriggerChangedDimension extends CriterionTriggerAbstract<CriterionTriggerChangedDimension.TriggerInstance> {
    static final MinecraftKey ID = new MinecraftKey("changed_dimension");

    @Override
    public MinecraftKey getId() {
        return ID;
    }

    @Override
    public CriterionTriggerChangedDimension.TriggerInstance createInstance(JsonObject jsonObject, CriterionConditionEntity.Composite composite, LootDeserializationContext deserializationContext) {
        ResourceKey<World> resourceKey = jsonObject.has("from") ? ResourceKey.create(IRegistry.DIMENSION_REGISTRY, new MinecraftKey(ChatDeserializer.getAsString(jsonObject, "from"))) : null;
        ResourceKey<World> resourceKey2 = jsonObject.has("to") ? ResourceKey.create(IRegistry.DIMENSION_REGISTRY, new MinecraftKey(ChatDeserializer.getAsString(jsonObject, "to"))) : null;
        return new CriterionTriggerChangedDimension.TriggerInstance(composite, resourceKey, resourceKey2);
    }

    public void trigger(EntityPlayer player, ResourceKey<World> from, ResourceKey<World> to) {
        this.trigger(player, (conditions) -> {
            return conditions.matches(from, to);
        });
    }

    public static class TriggerInstance extends CriterionInstanceAbstract {
        @Nullable
        private final ResourceKey<World> from;
        @Nullable
        private final ResourceKey<World> to;

        public TriggerInstance(CriterionConditionEntity.Composite player, @Nullable ResourceKey<World> from, @Nullable ResourceKey<World> to) {
            super(CriterionTriggerChangedDimension.ID, player);
            this.from = from;
            this.to = to;
        }

        public static CriterionTriggerChangedDimension.TriggerInstance changedDimension() {
            return new CriterionTriggerChangedDimension.TriggerInstance(CriterionConditionEntity.Composite.ANY, (ResourceKey<World>)null, (ResourceKey<World>)null);
        }

        public static CriterionTriggerChangedDimension.TriggerInstance changedDimension(ResourceKey<World> from, ResourceKey<World> to) {
            return new CriterionTriggerChangedDimension.TriggerInstance(CriterionConditionEntity.Composite.ANY, from, to);
        }

        public static CriterionTriggerChangedDimension.TriggerInstance changedDimensionTo(ResourceKey<World> to) {
            return new CriterionTriggerChangedDimension.TriggerInstance(CriterionConditionEntity.Composite.ANY, (ResourceKey<World>)null, to);
        }

        public static CriterionTriggerChangedDimension.TriggerInstance changedDimensionFrom(ResourceKey<World> from) {
            return new CriterionTriggerChangedDimension.TriggerInstance(CriterionConditionEntity.Composite.ANY, from, (ResourceKey<World>)null);
        }

        public boolean matches(ResourceKey<World> from, ResourceKey<World> to) {
            if (this.from != null && this.from != from) {
                return false;
            } else {
                return this.to == null || this.to == to;
            }
        }

        @Override
        public JsonObject serializeToJson(LootSerializationContext predicateSerializer) {
            JsonObject jsonObject = super.serializeToJson(predicateSerializer);
            if (this.from != null) {
                jsonObject.addProperty("from", this.from.location().toString());
            }

            if (this.to != null) {
                jsonObject.addProperty("to", this.to.location().toString());
            }

            return jsonObject;
        }
    }
}
