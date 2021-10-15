package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.mojang.authlib.GameProfile;
import java.util.Set;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameter;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class LootItemFunctionFillPlayerHead extends LootItemFunctionConditional {
    final LootTableInfo.EntityTarget entityTarget;

    public LootItemFunctionFillPlayerHead(LootItemCondition[] conditions, LootTableInfo.EntityTarget entity) {
        super(conditions);
        this.entityTarget = entity;
    }

    @Override
    public LootItemFunctionType getType() {
        return LootItemFunctions.FILL_PLAYER_HEAD;
    }

    @Override
    public Set<LootContextParameter<?>> getReferencedContextParams() {
        return ImmutableSet.of(this.entityTarget.getParam());
    }

    @Override
    public ItemStack run(ItemStack stack, LootTableInfo context) {
        if (stack.is(Items.PLAYER_HEAD)) {
            Entity entity = context.getContextParameter(this.entityTarget.getParam());
            if (entity instanceof EntityHuman) {
                GameProfile gameProfile = ((EntityHuman)entity).getProfile();
                stack.getOrCreateTag().set("SkullOwner", GameProfileSerializer.serialize(new NBTTagCompound(), gameProfile));
            }
        }

        return stack;
    }

    public static LootItemFunctionConditional.Builder<?> fillPlayerHead(LootTableInfo.EntityTarget target) {
        return simpleBuilder((conditions) -> {
            return new LootItemFunctionFillPlayerHead(conditions, target);
        });
    }

    public static class Serializer extends LootItemFunctionConditional.Serializer<LootItemFunctionFillPlayerHead> {
        @Override
        public void serialize(JsonObject json, LootItemFunctionFillPlayerHead object, JsonSerializationContext context) {
            super.serialize(json, object, context);
            json.add("entity", context.serialize(object.entityTarget));
        }

        @Override
        public LootItemFunctionFillPlayerHead deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootItemCondition[] lootItemConditions) {
            LootTableInfo.EntityTarget entityTarget = ChatDeserializer.getAsObject(jsonObject, "entity", jsonDeserializationContext, LootTableInfo.EntityTarget.class);
            return new LootItemFunctionFillPlayerHead(lootItemConditions, entityTarget);
        }
    }
}
