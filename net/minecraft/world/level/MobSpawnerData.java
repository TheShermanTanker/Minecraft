package net.minecraft.world.level;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.util.random.WeightedEntry;

public class MobSpawnerData extends WeightedEntry.IntrusiveBase {
    public static final int DEFAULT_WEIGHT = 1;
    public static final String DEFAULT_TYPE = "minecraft:pig";
    private final NBTTagCompound tag;

    public MobSpawnerData() {
        super(1);
        this.tag = new NBTTagCompound();
        this.tag.setString("id", "minecraft:pig");
    }

    public MobSpawnerData(NBTTagCompound nbt) {
        this(nbt.hasKeyOfType("Weight", 99) ? nbt.getInt("Weight") : 1, nbt.getCompound("Entity"));
    }

    public MobSpawnerData(int weight, NBTTagCompound entityNbt) {
        super(weight);
        this.tag = entityNbt;
        MinecraftKey resourceLocation = MinecraftKey.tryParse(entityNbt.getString("id"));
        if (resourceLocation != null) {
            entityNbt.setString("id", resourceLocation.toString());
        } else {
            entityNbt.setString("id", "minecraft:pig");
        }

    }

    public NBTTagCompound save() {
        NBTTagCompound compoundTag = new NBTTagCompound();
        compoundTag.set("Entity", this.tag);
        compoundTag.setInt("Weight", this.getWeight().asInt());
        return compoundTag;
    }

    public NBTTagCompound getEntity() {
        return this.tag;
    }
}
