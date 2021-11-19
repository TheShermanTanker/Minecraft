package net.minecraft.advancements;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.commands.CustomFunction;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTableInfo;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameterSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParameters;

public class AdvancementRewards {
    public static final AdvancementRewards EMPTY = new AdvancementRewards(0, new MinecraftKey[0], new MinecraftKey[0], CustomFunction.CacheableFunction.NONE);
    private final int experience;
    private final MinecraftKey[] loot;
    private final MinecraftKey[] recipes;
    private final CustomFunction.CacheableFunction function;

    public AdvancementRewards(int experience, MinecraftKey[] loot, MinecraftKey[] recipes, CustomFunction.CacheableFunction function) {
        this.experience = experience;
        this.loot = loot;
        this.recipes = recipes;
        this.function = function;
    }

    public MinecraftKey[] getRecipes() {
        return this.recipes;
    }

    public void grant(EntityPlayer player) {
        player.giveExp(this.experience);
        LootTableInfo lootContext = (new LootTableInfo.Builder(player.getWorldServer())).set(LootContextParameters.THIS_ENTITY, player).set(LootContextParameters.ORIGIN, player.getPositionVector()).withRandom(player.getRandom()).build(LootContextParameterSets.ADVANCEMENT_REWARD);
        boolean bl = false;

        for(MinecraftKey resourceLocation : this.loot) {
            for(ItemStack itemStack : player.server.getLootTableRegistry().getLootTable(resourceLocation).populateLoot(lootContext)) {
                if (player.addItem(itemStack)) {
                    player.level.playSound((EntityHuman)null, player.locX(), player.locY(), player.locZ(), SoundEffects.ITEM_PICKUP, EnumSoundCategory.PLAYERS, 0.2F, ((player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F);
                    bl = true;
                } else {
                    EntityItem itemEntity = player.drop(itemStack, false);
                    if (itemEntity != null) {
                        itemEntity.setNoPickUpDelay();
                        itemEntity.setOwner(player.getUniqueID());
                    }
                }
            }
        }

        if (bl) {
            player.containerMenu.broadcastChanges();
        }

        if (this.recipes.length > 0) {
            player.awardRecipesByKey(this.recipes);
        }

        MinecraftServer minecraftServer = player.server;
        this.function.get(minecraftServer.getFunctionData()).ifPresent((commandFunction) -> {
            minecraftServer.getFunctionData().execute(commandFunction, player.getCommandListener().withSuppressedOutput().withPermission(2));
        });
    }

    @Override
    public String toString() {
        return "AdvancementRewards{experience=" + this.experience + ", loot=" + Arrays.toString((Object[])this.loot) + ", recipes=" + Arrays.toString((Object[])this.recipes) + ", function=" + this.function + "}";
    }

    public JsonElement serializeToJson() {
        if (this == EMPTY) {
            return JsonNull.INSTANCE;
        } else {
            JsonObject jsonObject = new JsonObject();
            if (this.experience != 0) {
                jsonObject.addProperty("experience", this.experience);
            }

            if (this.loot.length > 0) {
                JsonArray jsonArray = new JsonArray();

                for(MinecraftKey resourceLocation : this.loot) {
                    jsonArray.add(resourceLocation.toString());
                }

                jsonObject.add("loot", jsonArray);
            }

            if (this.recipes.length > 0) {
                JsonArray jsonArray2 = new JsonArray();

                for(MinecraftKey resourceLocation2 : this.recipes) {
                    jsonArray2.add(resourceLocation2.toString());
                }

                jsonObject.add("recipes", jsonArray2);
            }

            if (this.function.getId() != null) {
                jsonObject.addProperty("function", this.function.getId().toString());
            }

            return jsonObject;
        }
    }

    public static AdvancementRewards deserialize(JsonObject json) throws JsonParseException {
        int i = ChatDeserializer.getAsInt(json, "experience", 0);
        JsonArray jsonArray = ChatDeserializer.getAsJsonArray(json, "loot", new JsonArray());
        MinecraftKey[] resourceLocations = new MinecraftKey[jsonArray.size()];

        for(int j = 0; j < resourceLocations.length; ++j) {
            resourceLocations[j] = new MinecraftKey(ChatDeserializer.convertToString(jsonArray.get(j), "loot[" + j + "]"));
        }

        JsonArray jsonArray2 = ChatDeserializer.getAsJsonArray(json, "recipes", new JsonArray());
        MinecraftKey[] resourceLocations2 = new MinecraftKey[jsonArray2.size()];

        for(int k = 0; k < resourceLocations2.length; ++k) {
            resourceLocations2[k] = new MinecraftKey(ChatDeserializer.convertToString(jsonArray2.get(k), "recipes[" + k + "]"));
        }

        CustomFunction.CacheableFunction cacheableFunction;
        if (json.has("function")) {
            cacheableFunction = new CustomFunction.CacheableFunction(new MinecraftKey(ChatDeserializer.getAsString(json, "function")));
        } else {
            cacheableFunction = CustomFunction.CacheableFunction.NONE;
        }

        return new AdvancementRewards(i, resourceLocations, resourceLocations2, cacheableFunction);
    }

    public static class Builder {
        private int experience;
        private final List<MinecraftKey> loot = Lists.newArrayList();
        private final List<MinecraftKey> recipes = Lists.newArrayList();
        @Nullable
        private MinecraftKey function;

        public static AdvancementRewards.Builder experience(int experience) {
            return (new AdvancementRewards.Builder()).addExperience(experience);
        }

        public AdvancementRewards.Builder addExperience(int experience) {
            this.experience += experience;
            return this;
        }

        public static AdvancementRewards.Builder loot(MinecraftKey loot) {
            return (new AdvancementRewards.Builder()).addLootTable(loot);
        }

        public AdvancementRewards.Builder addLootTable(MinecraftKey loot) {
            this.loot.add(loot);
            return this;
        }

        public static AdvancementRewards.Builder recipe(MinecraftKey recipe) {
            return (new AdvancementRewards.Builder()).addRecipe(recipe);
        }

        public AdvancementRewards.Builder addRecipe(MinecraftKey recipe) {
            this.recipes.add(recipe);
            return this;
        }

        public static AdvancementRewards.Builder function(MinecraftKey function) {
            return (new AdvancementRewards.Builder()).runs(function);
        }

        public AdvancementRewards.Builder runs(MinecraftKey function) {
            this.function = function;
            return this;
        }

        public AdvancementRewards build() {
            return new AdvancementRewards(this.experience, this.loot.toArray(new MinecraftKey[0]), this.recipes.toArray(new MinecraftKey[0]), this.function == null ? CustomFunction.CacheableFunction.NONE : new CustomFunction.CacheableFunction(this.function));
        }
    }
}
