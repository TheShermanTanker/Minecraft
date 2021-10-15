package net.minecraft.advancements.critereon;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import javax.annotation.Nullable;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.MojangsonParser;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;

public class CriterionConditionNBT {
    public static final CriterionConditionNBT ANY = new CriterionConditionNBT((NBTTagCompound)null);
    @Nullable
    private final NBTTagCompound tag;

    public CriterionConditionNBT(@Nullable NBTTagCompound nbt) {
        this.tag = nbt;
    }

    public boolean matches(ItemStack stack) {
        return this == ANY ? true : this.matches(stack.getTag());
    }

    public boolean matches(Entity entity) {
        return this == ANY ? true : this.matches(getEntityTagToCompare(entity));
    }

    public boolean matches(@Nullable NBTBase element) {
        if (element == null) {
            return this == ANY;
        } else {
            return this.tag == null || GameProfileSerializer.compareNbt(this.tag, element, true);
        }
    }

    public JsonElement serializeToJson() {
        return (JsonElement)(this != ANY && this.tag != null ? new JsonPrimitive(this.tag.toString()) : JsonNull.INSTANCE);
    }

    public static CriterionConditionNBT fromJson(@Nullable JsonElement json) {
        if (json != null && !json.isJsonNull()) {
            NBTTagCompound compoundTag;
            try {
                compoundTag = MojangsonParser.parse(ChatDeserializer.convertToString(json, "nbt"));
            } catch (CommandSyntaxException var3) {
                throw new JsonSyntaxException("Invalid nbt tag: " + var3.getMessage());
            }

            return new CriterionConditionNBT(compoundTag);
        } else {
            return ANY;
        }
    }

    public static NBTTagCompound getEntityTagToCompare(Entity entity) {
        NBTTagCompound compoundTag = entity.save(new NBTTagCompound());
        if (entity instanceof EntityHuman) {
            ItemStack itemStack = ((EntityHuman)entity).getInventory().getItemInHand();
            if (!itemStack.isEmpty()) {
                compoundTag.set("SelectedItem", itemStack.save(new NBTTagCompound()));
            }
        }

        return compoundTag;
    }
}
