package net.minecraft.world.level;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.SystemUtils;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.util.random.SimpleWeightedRandomList;

public record MobSpawnerData(NBTTagCompound entityToSpawn, Optional<SpawnData$CustomSpawnRules> customSpawnRules) {
    public static final Codec<MobSpawnerData> CODEC = RecordCodecBuilder.create((instance) -> {
        return instance.group(NBTTagCompound.CODEC.fieldOf("entity").forGetter((entry) -> {
            return entry.entityToSpawn;
        }), SpawnData$CustomSpawnRules.CODEC.optionalFieldOf("custom_spawn_rules").forGetter((spawnData) -> {
            return spawnData.customSpawnRules;
        })).apply(instance, MobSpawnerData::new);
    });
    public static final Codec<SimpleWeightedRandomList<MobSpawnerData>> LIST_CODEC = SimpleWeightedRandomList.wrappedCodecAllowingEmpty(CODEC);
    public static final String DEFAULT_TYPE = "minecraft:pig";

    public MobSpawnerData() {
        this(SystemUtils.make(new NBTTagCompound(), (nbt) -> {
            nbt.setString("id", "minecraft:pig");
        }), Optional.empty());
    }

    public MobSpawnerData(NBTTagCompound compoundTag, Optional<SpawnData$CustomSpawnRules> optional) {
        MinecraftKey resourceLocation = MinecraftKey.tryParse(compoundTag.getString("id"));
        compoundTag.setString("id", resourceLocation != null ? resourceLocation.toString() : "minecraft:pig");
        this.entityToSpawn = compoundTag;
        this.customSpawnRules = optional;
    }

    public NBTTagCompound getEntityToSpawn() {
        return this.entityToSpawn;
    }

    public Optional<SpawnData$CustomSpawnRules> getCustomSpawnRules() {
        return this.customSpawnRules;
    }

    public NBTTagCompound entityToSpawn() {
        return this.entityToSpawn;
    }

    public Optional<SpawnData$CustomSpawnRules> customSpawnRules() {
        return this.customSpawnRules;
    }
}
