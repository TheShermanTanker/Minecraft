package net.minecraft.world.level.storage.loot.providers.nbt;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.nbt.NBTBase;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.level.storage.loot.LootSerializer;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameter;

public class StorageNbtProvider implements NbtProvider {
    final MinecraftKey id;

    StorageNbtProvider(MinecraftKey source) {
        this.id = source;
    }

    @Override
    public LootNbtProviderType getType() {
        return NbtProviders.STORAGE;
    }

    @Nullable
    @Override
    public NBTBase get(LootTableInfo context) {
        return context.getWorld().getMinecraftServer().getCommandStorage().get(this.id);
    }

    @Override
    public Set<LootContextParameter<?>> getReferencedContextParams() {
        return ImmutableSet.of();
    }

    public static class Serializer implements LootSerializer<StorageNbtProvider> {
        @Override
        public void serialize(JsonObject json, StorageNbtProvider object, JsonSerializationContext context) {
            json.addProperty("source", object.id.toString());
        }

        @Override
        public StorageNbtProvider deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext) {
            String string = ChatDeserializer.getAsString(jsonObject, "source");
            return new StorageNbtProvider(new MinecraftKey(string));
        }
    }
}
