package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Set;
import java.util.function.UnaryOperator;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandListenerWrapper;
import net.minecraft.network.chat.ChatComponentUtils;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameter;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LootItemFunctionSetName extends LootItemFunctionConditional {
    private static final Logger LOGGER = LogManager.getLogger();
    final IChatBaseComponent name;
    @Nullable
    final LootTableInfo.EntityTarget resolutionContext;

    LootItemFunctionSetName(LootItemCondition[] conditions, @Nullable IChatBaseComponent component, @Nullable LootTableInfo.EntityTarget entityTarget) {
        super(conditions);
        this.name = component;
        this.resolutionContext = entityTarget;
    }

    @Override
    public LootItemFunctionType getType() {
        return LootItemFunctions.SET_NAME;
    }

    @Override
    public Set<LootContextParameter<?>> getReferencedContextParams() {
        return this.resolutionContext != null ? ImmutableSet.of(this.resolutionContext.getParam()) : ImmutableSet.of();
    }

    public static UnaryOperator<IChatBaseComponent> createResolver(LootTableInfo context, @Nullable LootTableInfo.EntityTarget sourceEntity) {
        if (sourceEntity != null) {
            Entity entity = context.getContextParameter(sourceEntity.getParam());
            if (entity != null) {
                CommandListenerWrapper commandSourceStack = entity.getCommandListener().withPermission(2);
                return (textComponent) -> {
                    try {
                        return ChatComponentUtils.filterForDisplay(commandSourceStack, textComponent, entity, 0);
                    } catch (CommandSyntaxException var4) {
                        LOGGER.warn("Failed to resolve text component", (Throwable)var4);
                        return textComponent;
                    }
                };
            }
        }

        return (textComponent) -> {
            return textComponent;
        };
    }

    @Override
    public ItemStack run(ItemStack stack, LootTableInfo context) {
        if (this.name != null) {
            stack.setHoverName(createResolver(context, this.resolutionContext).apply(this.name));
        }

        return stack;
    }

    public static LootItemFunctionConditional.Builder<?> setName(IChatBaseComponent name) {
        return simpleBuilder((conditions) -> {
            return new LootItemFunctionSetName(conditions, name, (LootTableInfo.EntityTarget)null);
        });
    }

    public static LootItemFunctionConditional.Builder<?> setName(IChatBaseComponent name, LootTableInfo.EntityTarget target) {
        return simpleBuilder((conditions) -> {
            return new LootItemFunctionSetName(conditions, name, target);
        });
    }

    public static class Serializer extends LootItemFunctionConditional.Serializer<LootItemFunctionSetName> {
        @Override
        public void serialize(JsonObject json, LootItemFunctionSetName object, JsonSerializationContext context) {
            super.serialize(json, object, context);
            if (object.name != null) {
                json.add("name", IChatBaseComponent.ChatSerializer.toJsonTree(object.name));
            }

            if (object.resolutionContext != null) {
                json.add("entity", context.serialize(object.resolutionContext));
            }

        }

        @Override
        public LootItemFunctionSetName deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootItemCondition[] lootItemConditions) {
            IChatBaseComponent component = IChatBaseComponent.ChatSerializer.fromJson(jsonObject.get("name"));
            LootTableInfo.EntityTarget entityTarget = ChatDeserializer.getAsObject(jsonObject, "entity", (LootTableInfo.EntityTarget)null, jsonDeserializationContext, LootTableInfo.EntityTarget.class);
            return new LootItemFunctionSetName(lootItemConditions, component, entityTarget);
        }
    }
}
