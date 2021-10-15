package net.minecraft.network.protocol.game;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.List;
import net.minecraft.core.IRegistry;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.item.crafting.IRecipe;

public class PacketPlayOutRecipeUpdate implements Packet<PacketListenerPlayOut> {
    private final List<IRecipe<?>> recipes;

    public PacketPlayOutRecipeUpdate(Collection<IRecipe<?>> recipes) {
        this.recipes = Lists.newArrayList(recipes);
    }

    public PacketPlayOutRecipeUpdate(PacketDataSerializer buf) {
        this.recipes = buf.readList(PacketPlayOutRecipeUpdate::fromNetwork);
    }

    @Override
    public void write(PacketDataSerializer buf) {
        buf.writeCollection(this.recipes, PacketPlayOutRecipeUpdate::toNetwork);
    }

    @Override
    public void handle(PacketListenerPlayOut listener) {
        listener.handleUpdateRecipes(this);
    }

    public List<IRecipe<?>> getRecipes() {
        return this.recipes;
    }

    public static IRecipe<?> fromNetwork(PacketDataSerializer buf) {
        MinecraftKey resourceLocation = buf.readResourceLocation();
        MinecraftKey resourceLocation2 = buf.readResourceLocation();
        return IRegistry.RECIPE_SERIALIZER.getOptional(resourceLocation).orElseThrow(() -> {
            return new IllegalArgumentException("Unknown recipe serializer " + resourceLocation);
        }).fromNetwork(resourceLocation2, buf);
    }

    public static <T extends IRecipe<?>> void toNetwork(PacketDataSerializer buf, T recipe) {
        buf.writeResourceLocation(IRegistry.RECIPE_SERIALIZER.getKey(recipe.getRecipeSerializer()));
        buf.writeResourceLocation(recipe.getKey());
        recipe.getRecipeSerializer().toNetwork(buf, recipe);
    }
}
