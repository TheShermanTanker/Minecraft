package net.minecraft.advancements.critereon;

import com.google.common.base.Joiner;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import javax.annotation.Nullable;
import net.minecraft.core.IRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagsInstance;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.entity.EntityTypes;

public abstract class CriterionConditionEntityType {
    public static final CriterionConditionEntityType ANY = new CriterionConditionEntityType() {
        @Override
        public boolean matches(EntityTypes<?> type) {
            return true;
        }

        @Override
        public JsonElement serializeToJson() {
            return JsonNull.INSTANCE;
        }
    };
    private static final Joiner COMMA_JOINER = Joiner.on(", ");

    public abstract boolean matches(EntityTypes<?> type);

    public abstract JsonElement serializeToJson();

    public static CriterionConditionEntityType fromJson(@Nullable JsonElement json) {
        if (json != null && !json.isJsonNull()) {
            String string = ChatDeserializer.convertToString(json, "type");
            if (string.startsWith("#")) {
                MinecraftKey resourceLocation = new MinecraftKey(string.substring(1));
                return new CriterionConditionEntityType.TagPredicate(TagsInstance.getInstance().getTagOrThrow(IRegistry.ENTITY_TYPE_REGISTRY, resourceLocation, (resourceLocation) -> {
                    return new JsonSyntaxException("Unknown entity tag '" + resourceLocation + "'");
                }));
            } else {
                MinecraftKey resourceLocation2 = new MinecraftKey(string);
                EntityTypes<?> entityType = IRegistry.ENTITY_TYPE.getOptional(resourceLocation2).orElseThrow(() -> {
                    return new JsonSyntaxException("Unknown entity type '" + resourceLocation2 + "', valid types are: " + COMMA_JOINER.join(IRegistry.ENTITY_TYPE.keySet()));
                });
                return new CriterionConditionEntityType.TypePredicate(entityType);
            }
        } else {
            return ANY;
        }
    }

    public static CriterionConditionEntityType of(EntityTypes<?> type) {
        return new CriterionConditionEntityType.TypePredicate(type);
    }

    public static CriterionConditionEntityType of(Tag<EntityTypes<?>> tag) {
        return new CriterionConditionEntityType.TagPredicate(tag);
    }

    static class TagPredicate extends CriterionConditionEntityType {
        private final Tag<EntityTypes<?>> tag;

        public TagPredicate(Tag<EntityTypes<?>> tag) {
            this.tag = tag;
        }

        @Override
        public boolean matches(EntityTypes<?> type) {
            return type.is(this.tag);
        }

        @Override
        public JsonElement serializeToJson() {
            return new JsonPrimitive("#" + TagsInstance.getInstance().getIdOrThrow(IRegistry.ENTITY_TYPE_REGISTRY, this.tag, () -> {
                return new IllegalStateException("Unknown entity type tag");
            }));
        }
    }

    static class TypePredicate extends CriterionConditionEntityType {
        private final EntityTypes<?> type;

        public TypePredicate(EntityTypes<?> type) {
            this.type = type;
        }

        @Override
        public boolean matches(EntityTypes<?> type) {
            return this.type == type;
        }

        @Override
        public JsonElement serializeToJson() {
            return new JsonPrimitive(IRegistry.ENTITY_TYPE.getKey(this.type).toString());
        }
    }
}
