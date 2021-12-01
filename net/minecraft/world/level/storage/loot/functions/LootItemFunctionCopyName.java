package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.Set;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.INamableTileEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameter;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameters;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class LootItemFunctionCopyName extends LootItemFunctionConditional {
    final LootItemFunctionCopyName.Source source;

    LootItemFunctionCopyName(LootItemCondition[] conditions, LootItemFunctionCopyName.Source source) {
        super(conditions);
        this.source = source;
    }

    @Override
    public LootItemFunctionType getType() {
        return LootItemFunctions.COPY_NAME;
    }

    @Override
    public Set<LootContextParameter<?>> getReferencedContextParams() {
        return ImmutableSet.of(this.source.param);
    }

    @Override
    public ItemStack run(ItemStack stack, LootTableInfo context) {
        Object object = context.getContextParameter(this.source.param);
        if (object instanceof INamableTileEntity) {
            INamableTileEntity nameable = (INamableTileEntity)object;
            if (nameable.hasCustomName()) {
                stack.setHoverName(nameable.getScoreboardDisplayName());
            }
        }

        return stack;
    }

    public static LootItemFunctionConditional.Builder<?> copyName(LootItemFunctionCopyName.Source source) {
        return simpleBuilder((conditions) -> {
            return new LootItemFunctionCopyName(conditions, source);
        });
    }

    public static class Serializer extends LootItemFunctionConditional.Serializer<LootItemFunctionCopyName> {
        @Override
        public void serialize(JsonObject json, LootItemFunctionCopyName object, JsonSerializationContext context) {
            super.serialize(json, object, context);
            json.addProperty("source", object.source.name);
        }

        @Override
        public LootItemFunctionCopyName deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootItemCondition[] lootItemConditions) {
            LootItemFunctionCopyName.Source nameSource = LootItemFunctionCopyName.Source.getByName(ChatDeserializer.getAsString(jsonObject, "source"));
            return new LootItemFunctionCopyName(lootItemConditions, nameSource);
        }
    }

    public static enum Source {
        THIS("this", LootContextParameters.THIS_ENTITY),
        KILLER("killer", LootContextParameters.KILLER_ENTITY),
        KILLER_PLAYER("killer_player", LootContextParameters.LAST_DAMAGE_PLAYER),
        BLOCK_ENTITY("block_entity", LootContextParameters.BLOCK_ENTITY);

        public final String name;
        public final LootContextParameter<?> param;

        private Source(String name, LootContextParameter<?> parameter) {
            this.name = name;
            this.param = parameter;
        }

        public static LootItemFunctionCopyName.Source getByName(String name) {
            for(LootItemFunctionCopyName.Source nameSource : values()) {
                if (nameSource.name.equals(name)) {
                    return nameSource;
                }
            }

            throw new IllegalArgumentException("Invalid name source " + name);
        }
    }
}
