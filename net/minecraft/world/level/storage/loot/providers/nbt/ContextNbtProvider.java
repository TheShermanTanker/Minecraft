package net.minecraft.world.level.storage.loot.providers.nbt;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.advancements.critereon.CriterionConditionNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.storage.loot.JsonRegistry;
import net.minecraft.world.level.storage.loot.LootSerializer;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameter;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameters;

public class ContextNbtProvider implements NbtProvider {
    private static final String BLOCK_ENTITY_ID = "block_entity";
    private static final ContextNbtProvider.Getter BLOCK_ENTITY_PROVIDER = new ContextNbtProvider.Getter() {
        @Override
        public NBTBase get(LootTableInfo context) {
            TileEntity blockEntity = context.getContextParameter(LootContextParameters.BLOCK_ENTITY);
            return blockEntity != null ? blockEntity.saveWithFullMetadata() : null;
        }

        @Override
        public String getId() {
            return "block_entity";
        }

        @Override
        public Set<LootContextParameter<?>> getReferencedContextParams() {
            return ImmutableSet.of(LootContextParameters.BLOCK_ENTITY);
        }
    };
    public static final ContextNbtProvider BLOCK_ENTITY = new ContextNbtProvider(BLOCK_ENTITY_PROVIDER);
    final ContextNbtProvider.Getter getter;

    private static ContextNbtProvider.Getter forEntity(LootTableInfo.EntityTarget entityTarget) {
        return new ContextNbtProvider.Getter() {
            @Nullable
            @Override
            public NBTBase get(LootTableInfo context) {
                Entity entity = context.getContextParameter(entityTarget.getParam());
                return entity != null ? CriterionConditionNBT.getEntityTagToCompare(entity) : null;
            }

            @Override
            public String getId() {
                return entityTarget.name();
            }

            @Override
            public Set<LootContextParameter<?>> getReferencedContextParams() {
                return ImmutableSet.of(entityTarget.getParam());
            }
        };
    }

    private ContextNbtProvider(ContextNbtProvider.Getter target) {
        this.getter = target;
    }

    @Override
    public LootNbtProviderType getType() {
        return NbtProviders.CONTEXT;
    }

    @Nullable
    @Override
    public NBTBase get(LootTableInfo context) {
        return this.getter.get(context);
    }

    @Override
    public Set<LootContextParameter<?>> getReferencedContextParams() {
        return this.getter.getReferencedContextParams();
    }

    public static NbtProvider forContextEntity(LootTableInfo.EntityTarget target) {
        return new ContextNbtProvider(forEntity(target));
    }

    static ContextNbtProvider createFromContext(String target) {
        if (target.equals("block_entity")) {
            return new ContextNbtProvider(BLOCK_ENTITY_PROVIDER);
        } else {
            LootTableInfo.EntityTarget entityTarget = LootTableInfo.EntityTarget.getByName(target);
            return new ContextNbtProvider(forEntity(entityTarget));
        }
    }

    interface Getter {
        @Nullable
        NBTBase get(LootTableInfo context);

        String getId();

        Set<LootContextParameter<?>> getReferencedContextParams();
    }

    public static class InlineSerializer implements JsonRegistry.InlineSerializer<ContextNbtProvider> {
        @Override
        public JsonElement serialize(ContextNbtProvider object, JsonSerializationContext context) {
            return new JsonPrimitive(object.getter.getId());
        }

        @Override
        public ContextNbtProvider deserialize(JsonElement jsonElement, JsonDeserializationContext jsonDeserializationContext) {
            String string = jsonElement.getAsString();
            return ContextNbtProvider.createFromContext(string);
        }
    }

    public static class Serializer implements LootSerializer<ContextNbtProvider> {
        @Override
        public void serialize(JsonObject json, ContextNbtProvider object, JsonSerializationContext context) {
            json.addProperty("target", object.getter.getId());
        }

        @Override
        public ContextNbtProvider deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext) {
            String string = ChatDeserializer.getAsString(jsonObject, "target");
            return ContextNbtProvider.createFromContext(string);
        }
    }
}
