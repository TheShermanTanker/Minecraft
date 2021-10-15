package net.minecraft.advancements.critereon;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.WorldServer;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagsInstance;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidType;

public class CriterionConditionFluid {
    public static final CriterionConditionFluid ANY = new CriterionConditionFluid((Tag<FluidType>)null, (FluidType)null, CriterionTriggerProperties.ANY);
    @Nullable
    private final Tag<FluidType> tag;
    @Nullable
    private final FluidType fluid;
    private final CriterionTriggerProperties properties;

    public CriterionConditionFluid(@Nullable Tag<FluidType> tag, @Nullable FluidType fluid, CriterionTriggerProperties state) {
        this.tag = tag;
        this.fluid = fluid;
        this.properties = state;
    }

    public boolean matches(WorldServer world, BlockPosition pos) {
        if (this == ANY) {
            return true;
        } else if (!world.isLoaded(pos)) {
            return false;
        } else {
            Fluid fluidState = world.getFluid(pos);
            FluidType fluid = fluidState.getType();
            if (this.tag != null && !fluid.is(this.tag)) {
                return false;
            } else if (this.fluid != null && fluid != this.fluid) {
                return false;
            } else {
                return this.properties.matches(fluidState);
            }
        }
    }

    public static CriterionConditionFluid fromJson(@Nullable JsonElement json) {
        if (json != null && !json.isJsonNull()) {
            JsonObject jsonObject = ChatDeserializer.convertToJsonObject(json, "fluid");
            FluidType fluid = null;
            if (jsonObject.has("fluid")) {
                MinecraftKey resourceLocation = new MinecraftKey(ChatDeserializer.getAsString(jsonObject, "fluid"));
                fluid = IRegistry.FLUID.get(resourceLocation);
            }

            Tag<FluidType> tag = null;
            if (jsonObject.has("tag")) {
                MinecraftKey resourceLocation2 = new MinecraftKey(ChatDeserializer.getAsString(jsonObject, "tag"));
                tag = TagsInstance.getInstance().getTagOrThrow(IRegistry.FLUID_REGISTRY, resourceLocation2, (id) -> {
                    return new JsonSyntaxException("Unknown fluid tag '" + id + "'");
                });
            }

            CriterionTriggerProperties statePropertiesPredicate = CriterionTriggerProperties.fromJson(jsonObject.get("state"));
            return new CriterionConditionFluid(tag, fluid, statePropertiesPredicate);
        } else {
            return ANY;
        }
    }

    public JsonElement serializeToJson() {
        if (this == ANY) {
            return JsonNull.INSTANCE;
        } else {
            JsonObject jsonObject = new JsonObject();
            if (this.fluid != null) {
                jsonObject.addProperty("fluid", IRegistry.FLUID.getKey(this.fluid).toString());
            }

            if (this.tag != null) {
                jsonObject.addProperty("tag", TagsInstance.getInstance().getIdOrThrow(IRegistry.FLUID_REGISTRY, this.tag, () -> {
                    return new IllegalStateException("Unknown fluid tag");
                }).toString());
            }

            jsonObject.add("state", this.properties.serializeToJson());
            return jsonObject;
        }
    }

    public static class Builder {
        @Nullable
        private FluidType fluid;
        @Nullable
        private Tag<FluidType> fluids;
        private CriterionTriggerProperties properties = CriterionTriggerProperties.ANY;

        private Builder() {
        }

        public static CriterionConditionFluid.Builder fluid() {
            return new CriterionConditionFluid.Builder();
        }

        public CriterionConditionFluid.Builder of(FluidType fluid) {
            this.fluid = fluid;
            return this;
        }

        public CriterionConditionFluid.Builder of(Tag<FluidType> tag) {
            this.fluids = tag;
            return this;
        }

        public CriterionConditionFluid.Builder setProperties(CriterionTriggerProperties state) {
            this.properties = state;
            return this;
        }

        public CriterionConditionFluid build() {
            return new CriterionConditionFluid(this.fluids, this.fluid, this.properties);
        }
    }
}
