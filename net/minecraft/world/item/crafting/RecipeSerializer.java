package net.minecraft.world.item.crafting;

import com.google.gson.JsonObject;
import net.minecraft.core.IRegistry;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.resources.MinecraftKey;

public interface RecipeSerializer<T extends IRecipe<?>> {
    RecipeSerializer<ShapedRecipes> SHAPED_RECIPE = register("crafting_shaped", new ShapedRecipes.Serializer());
    RecipeSerializer<ShapelessRecipes> SHAPELESS_RECIPE = register("crafting_shapeless", new ShapelessRecipes.Serializer());
    RecipeSerializerComplex<RecipeArmorDye> ARMOR_DYE = register("crafting_special_armordye", new RecipeSerializerComplex<>(RecipeArmorDye::new));
    RecipeSerializerComplex<RecipeBookClone> BOOK_CLONING = register("crafting_special_bookcloning", new RecipeSerializerComplex<>(RecipeBookClone::new));
    RecipeSerializerComplex<RecipeMapClone> MAP_CLONING = register("crafting_special_mapcloning", new RecipeSerializerComplex<>(RecipeMapClone::new));
    RecipeSerializerComplex<RecipeMapExtend> MAP_EXTENDING = register("crafting_special_mapextending", new RecipeSerializerComplex<>(RecipeMapExtend::new));
    RecipeSerializerComplex<RecipeFireworks> FIREWORK_ROCKET = register("crafting_special_firework_rocket", new RecipeSerializerComplex<>(RecipeFireworks::new));
    RecipeSerializerComplex<RecipeFireworksStar> FIREWORK_STAR = register("crafting_special_firework_star", new RecipeSerializerComplex<>(RecipeFireworksStar::new));
    RecipeSerializerComplex<RecipeFireworksFade> FIREWORK_STAR_FADE = register("crafting_special_firework_star_fade", new RecipeSerializerComplex<>(RecipeFireworksFade::new));
    RecipeSerializerComplex<RecipeTippedArrow> TIPPED_ARROW = register("crafting_special_tippedarrow", new RecipeSerializerComplex<>(RecipeTippedArrow::new));
    RecipeSerializerComplex<RecipeBannerDuplicate> BANNER_DUPLICATE = register("crafting_special_bannerduplicate", new RecipeSerializerComplex<>(RecipeBannerDuplicate::new));
    RecipeSerializerComplex<RecipiesShield> SHIELD_DECORATION = register("crafting_special_shielddecoration", new RecipeSerializerComplex<>(RecipiesShield::new));
    RecipeSerializerComplex<RecipeShulkerBox> SHULKER_BOX_COLORING = register("crafting_special_shulkerboxcoloring", new RecipeSerializerComplex<>(RecipeShulkerBox::new));
    RecipeSerializerComplex<RecipeSuspiciousStew> SUSPICIOUS_STEW = register("crafting_special_suspiciousstew", new RecipeSerializerComplex<>(RecipeSuspiciousStew::new));
    RecipeSerializerComplex<RecipeRepair> REPAIR_ITEM = register("crafting_special_repairitem", new RecipeSerializerComplex<>(RecipeRepair::new));
    RecipeSerializerCooking<FurnaceRecipe> SMELTING_RECIPE = register("smelting", new RecipeSerializerCooking<>(FurnaceRecipe::new, 200));
    RecipeSerializerCooking<RecipeBlasting> BLASTING_RECIPE = register("blasting", new RecipeSerializerCooking<>(RecipeBlasting::new, 100));
    RecipeSerializerCooking<RecipeSmoking> SMOKING_RECIPE = register("smoking", new RecipeSerializerCooking<>(RecipeSmoking::new, 100));
    RecipeSerializerCooking<RecipeCampfire> CAMPFIRE_COOKING_RECIPE = register("campfire_cooking", new RecipeSerializerCooking<>(RecipeCampfire::new, 100));
    RecipeSerializer<RecipeStonecutting> STONECUTTER = register("stonecutting", new RecipeSingleItem.Serializer<>(RecipeStonecutting::new));
    RecipeSerializer<RecipeSmithing> SMITHING = register("smithing", new RecipeSmithing.Serializer());

    T fromJson(MinecraftKey id, JsonObject json);

    T fromNetwork(MinecraftKey id, PacketDataSerializer buf);

    void toNetwork(PacketDataSerializer buf, T recipe);

    static <S extends RecipeSerializer<T>, T extends IRecipe<?>> S register(String id, S serializer) {
        return IRegistry.register(IRegistry.RECIPE_SERIALIZER, id, serializer);
    }
}
