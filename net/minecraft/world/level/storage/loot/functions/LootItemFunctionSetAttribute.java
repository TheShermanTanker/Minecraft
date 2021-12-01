package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSyntaxException;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.SystemUtils;
import net.minecraft.core.IRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.ai.attributes.AttributeBase;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameter;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

public class LootItemFunctionSetAttribute extends LootItemFunctionConditional {
    final List<LootItemFunctionSetAttribute.Modifier> modifiers;

    LootItemFunctionSetAttribute(LootItemCondition[] conditions, List<LootItemFunctionSetAttribute.Modifier> attributes) {
        super(conditions);
        this.modifiers = ImmutableList.copyOf(attributes);
    }

    @Override
    public LootItemFunctionType getType() {
        return LootItemFunctions.SET_ATTRIBUTES;
    }

    @Override
    public Set<LootContextParameter<?>> getReferencedContextParams() {
        return this.modifiers.stream().flatMap((attribute) -> {
            return attribute.amount.getReferencedContextParams().stream();
        }).collect(ImmutableSet.toImmutableSet());
    }

    @Override
    public ItemStack run(ItemStack stack, LootTableInfo context) {
        Random random = context.getRandom();

        for(LootItemFunctionSetAttribute.Modifier modifier : this.modifiers) {
            UUID uUID = modifier.id;
            if (uUID == null) {
                uUID = UUID.randomUUID();
            }

            EnumItemSlot equipmentSlot = SystemUtils.getRandom(modifier.slots, random);
            stack.addAttributeModifier(modifier.attribute, new AttributeModifier(uUID, modifier.name, (double)modifier.amount.getFloat(context), modifier.operation), equipmentSlot);
        }

        return stack;
    }

    public static LootItemFunctionSetAttribute.ModifierBuilder modifier(String name, AttributeBase attribute, AttributeModifier.Operation operation, NumberProvider amountRange) {
        return new LootItemFunctionSetAttribute.ModifierBuilder(name, attribute, operation, amountRange);
    }

    public static LootItemFunctionSetAttribute.Builder setAttributes() {
        return new LootItemFunctionSetAttribute.Builder();
    }

    public static class Builder extends LootItemFunctionConditional.Builder<LootItemFunctionSetAttribute.Builder> {
        private final List<LootItemFunctionSetAttribute.Modifier> modifiers = Lists.newArrayList();

        @Override
        protected LootItemFunctionSetAttribute.Builder getThis() {
            return this;
        }

        public LootItemFunctionSetAttribute.Builder withModifier(LootItemFunctionSetAttribute.ModifierBuilder attribute) {
            this.modifiers.add(attribute.build());
            return this;
        }

        @Override
        public LootItemFunction build() {
            return new LootItemFunctionSetAttribute(this.getConditions(), this.modifiers);
        }
    }

    static class Modifier {
        final String name;
        final AttributeBase attribute;
        final AttributeModifier.Operation operation;
        final NumberProvider amount;
        @Nullable
        final UUID id;
        final EnumItemSlot[] slots;

        Modifier(String name, AttributeBase attribute, AttributeModifier.Operation operation, NumberProvider amount, EnumItemSlot[] slots, @Nullable UUID id) {
            this.name = name;
            this.attribute = attribute;
            this.operation = operation;
            this.amount = amount;
            this.id = id;
            this.slots = slots;
        }

        public JsonObject serialize(JsonSerializationContext context) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("name", this.name);
            jsonObject.addProperty("attribute", IRegistry.ATTRIBUTE.getKey(this.attribute).toString());
            jsonObject.addProperty("operation", operationToString(this.operation));
            jsonObject.add("amount", context.serialize(this.amount));
            if (this.id != null) {
                jsonObject.addProperty("id", this.id.toString());
            }

            if (this.slots.length == 1) {
                jsonObject.addProperty("slot", this.slots[0].getSlotName());
            } else {
                JsonArray jsonArray = new JsonArray();

                for(EnumItemSlot equipmentSlot : this.slots) {
                    jsonArray.add(new JsonPrimitive(equipmentSlot.getSlotName()));
                }

                jsonObject.add("slot", jsonArray);
            }

            return jsonObject;
        }

        public static LootItemFunctionSetAttribute.Modifier deserialize(JsonObject json, JsonDeserializationContext context) {
            String string = ChatDeserializer.getAsString(json, "name");
            MinecraftKey resourceLocation = new MinecraftKey(ChatDeserializer.getAsString(json, "attribute"));
            AttributeBase attribute = IRegistry.ATTRIBUTE.get(resourceLocation);
            if (attribute == null) {
                throw new JsonSyntaxException("Unknown attribute: " + resourceLocation);
            } else {
                AttributeModifier.Operation operation = operationFromString(ChatDeserializer.getAsString(json, "operation"));
                NumberProvider numberProvider = ChatDeserializer.getAsObject(json, "amount", context, NumberProvider.class);
                UUID uUID = null;
                EnumItemSlot[] equipmentSlots;
                if (ChatDeserializer.isStringValue(json, "slot")) {
                    equipmentSlots = new EnumItemSlot[]{EnumItemSlot.fromName(ChatDeserializer.getAsString(json, "slot"))};
                } else {
                    if (!ChatDeserializer.isArrayNode(json, "slot")) {
                        throw new JsonSyntaxException("Invalid or missing attribute modifier slot; must be either string or array of strings.");
                    }

                    JsonArray jsonArray = ChatDeserializer.getAsJsonArray(json, "slot");
                    equipmentSlots = new EnumItemSlot[jsonArray.size()];
                    int i = 0;

                    for(JsonElement jsonElement : jsonArray) {
                        equipmentSlots[i++] = EnumItemSlot.fromName(ChatDeserializer.convertToString(jsonElement, "slot"));
                    }

                    if (equipmentSlots.length == 0) {
                        throw new JsonSyntaxException("Invalid attribute modifier slot; must contain at least one entry.");
                    }
                }

                if (json.has("id")) {
                    String string2 = ChatDeserializer.getAsString(json, "id");

                    try {
                        uUID = UUID.fromString(string2);
                    } catch (IllegalArgumentException var13) {
                        throw new JsonSyntaxException("Invalid attribute modifier id '" + string2 + "' (must be UUID format, with dashes)");
                    }
                }

                return new LootItemFunctionSetAttribute.Modifier(string, attribute, operation, numberProvider, equipmentSlots, uUID);
            }
        }

        private static String operationToString(AttributeModifier.Operation operation) {
            switch(operation) {
            case ADDITION:
                return "addition";
            case MULTIPLY_BASE:
                return "multiply_base";
            case MULTIPLY_TOTAL:
                return "multiply_total";
            default:
                throw new IllegalArgumentException("Unknown operation " + operation);
            }
        }

        private static AttributeModifier.Operation operationFromString(String name) {
            switch(name) {
            case "addition":
                return AttributeModifier.Operation.ADDITION;
            case "multiply_base":
                return AttributeModifier.Operation.MULTIPLY_BASE;
            case "multiply_total":
                return AttributeModifier.Operation.MULTIPLY_TOTAL;
            default:
                throw new JsonSyntaxException("Unknown attribute modifier operation " + name);
            }
        }
    }

    public static class ModifierBuilder {
        private final String name;
        private final AttributeBase attribute;
        private final AttributeModifier.Operation operation;
        private final NumberProvider amount;
        @Nullable
        private UUID id;
        private final Set<EnumItemSlot> slots = EnumSet.noneOf(EnumItemSlot.class);

        public ModifierBuilder(String name, AttributeBase attribute, AttributeModifier.Operation operation, NumberProvider amount) {
            this.name = name;
            this.attribute = attribute;
            this.operation = operation;
            this.amount = amount;
        }

        public LootItemFunctionSetAttribute.ModifierBuilder forSlot(EnumItemSlot slot) {
            this.slots.add(slot);
            return this;
        }

        public LootItemFunctionSetAttribute.ModifierBuilder withUuid(UUID uuid) {
            this.id = uuid;
            return this;
        }

        public LootItemFunctionSetAttribute.Modifier build() {
            return new LootItemFunctionSetAttribute.Modifier(this.name, this.attribute, this.operation, this.amount, this.slots.toArray(new EnumItemSlot[0]), this.id);
        }
    }

    public static class Serializer extends LootItemFunctionConditional.Serializer<LootItemFunctionSetAttribute> {
        @Override
        public void serialize(JsonObject json, LootItemFunctionSetAttribute object, JsonSerializationContext context) {
            super.serialize(json, object, context);
            JsonArray jsonArray = new JsonArray();

            for(LootItemFunctionSetAttribute.Modifier modifier : object.modifiers) {
                jsonArray.add(modifier.serialize(context));
            }

            json.add("modifiers", jsonArray);
        }

        @Override
        public LootItemFunctionSetAttribute deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootItemCondition[] lootItemConditions) {
            JsonArray jsonArray = ChatDeserializer.getAsJsonArray(jsonObject, "modifiers");
            List<LootItemFunctionSetAttribute.Modifier> list = Lists.newArrayListWithExpectedSize(jsonArray.size());

            for(JsonElement jsonElement : jsonArray) {
                list.add(LootItemFunctionSetAttribute.Modifier.deserialize(ChatDeserializer.convertToJsonObject(jsonElement, "modifier"), jsonDeserializationContext));
            }

            if (list.isEmpty()) {
                throw new JsonSyntaxException("Invalid attribute modifiers array; cannot be empty");
            } else {
                return new LootItemFunctionSetAttribute(lootItemConditions, list);
            }
        }
    }
}
