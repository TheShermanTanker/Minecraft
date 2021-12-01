package net.minecraft.world.item.crafting;

import com.google.gson.JsonObject;
import java.util.function.Function;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.resources.MinecraftKey;

public class RecipeSerializerComplex<T extends IRecipe<?>> implements RecipeSerializer<T> {
    private final Function<MinecraftKey, T> constructor;

    public RecipeSerializerComplex(Function<MinecraftKey, T> factory) {
        this.constructor = factory;
    }

    @Override
    public T fromJson(MinecraftKey id, JsonObject json) {
        return this.constructor.apply(id);
    }

    @Override
    public T fromNetwork(MinecraftKey id, PacketDataSerializer buf) {
        return this.constructor.apply(id);
    }

    @Override
    public void toNetwork(PacketDataSerializer buf, T recipe) {
    }
}
