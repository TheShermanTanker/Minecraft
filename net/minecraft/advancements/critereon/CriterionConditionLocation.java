package net.minecraft.advancements.critereon;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.world.level.World;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.block.BlockCampfire;
import net.minecraft.world.level.levelgen.feature.StructureGenerator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CriterionConditionLocation {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final CriterionConditionLocation ANY = new CriterionConditionLocation(CriterionConditionValue.DoubleRange.ANY, CriterionConditionValue.DoubleRange.ANY, CriterionConditionValue.DoubleRange.ANY, (ResourceKey<BiomeBase>)null, (StructureGenerator<?>)null, (ResourceKey<World>)null, (Boolean)null, CriterionConditionLight.ANY, CriterionConditionBlock.ANY, CriterionConditionFluid.ANY);
    private final CriterionConditionValue.DoubleRange x;
    private final CriterionConditionValue.DoubleRange y;
    private final CriterionConditionValue.DoubleRange z;
    @Nullable
    private final ResourceKey<BiomeBase> biome;
    @Nullable
    private final StructureGenerator<?> feature;
    @Nullable
    private final ResourceKey<World> dimension;
    @Nullable
    private final Boolean smokey;
    private final CriterionConditionLight light;
    private final CriterionConditionBlock block;
    private final CriterionConditionFluid fluid;

    public CriterionConditionLocation(CriterionConditionValue.DoubleRange x, CriterionConditionValue.DoubleRange y, CriterionConditionValue.DoubleRange z, @Nullable ResourceKey<BiomeBase> biome, @Nullable StructureGenerator<?> feature, @Nullable ResourceKey<World> dimension, @Nullable Boolean smokey, CriterionConditionLight light, CriterionConditionBlock block, CriterionConditionFluid fluid) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.biome = biome;
        this.feature = feature;
        this.dimension = dimension;
        this.smokey = smokey;
        this.light = light;
        this.block = block;
        this.fluid = fluid;
    }

    public static CriterionConditionLocation inBiome(ResourceKey<BiomeBase> biome) {
        return new CriterionConditionLocation(CriterionConditionValue.DoubleRange.ANY, CriterionConditionValue.DoubleRange.ANY, CriterionConditionValue.DoubleRange.ANY, biome, (StructureGenerator<?>)null, (ResourceKey<World>)null, (Boolean)null, CriterionConditionLight.ANY, CriterionConditionBlock.ANY, CriterionConditionFluid.ANY);
    }

    public static CriterionConditionLocation inDimension(ResourceKey<World> dimension) {
        return new CriterionConditionLocation(CriterionConditionValue.DoubleRange.ANY, CriterionConditionValue.DoubleRange.ANY, CriterionConditionValue.DoubleRange.ANY, (ResourceKey<BiomeBase>)null, (StructureGenerator<?>)null, dimension, (Boolean)null, CriterionConditionLight.ANY, CriterionConditionBlock.ANY, CriterionConditionFluid.ANY);
    }

    public static CriterionConditionLocation inFeature(StructureGenerator<?> feature) {
        return new CriterionConditionLocation(CriterionConditionValue.DoubleRange.ANY, CriterionConditionValue.DoubleRange.ANY, CriterionConditionValue.DoubleRange.ANY, (ResourceKey<BiomeBase>)null, feature, (ResourceKey<World>)null, (Boolean)null, CriterionConditionLight.ANY, CriterionConditionBlock.ANY, CriterionConditionFluid.ANY);
    }

    public boolean matches(WorldServer serverLevel, double d, double e, double f) {
        if (!this.x.matches(d)) {
            return false;
        } else if (!this.y.matches(e)) {
            return false;
        } else if (!this.z.matches(f)) {
            return false;
        } else if (this.dimension != null && this.dimension != serverLevel.getDimensionKey()) {
            return false;
        } else {
            BlockPosition blockPos = new BlockPosition(d, e, f);
            boolean bl = serverLevel.isLoaded(blockPos);
            Optional<ResourceKey<BiomeBase>> optional = serverLevel.registryAccess().registryOrThrow(IRegistry.BIOME_REGISTRY).getResourceKey(serverLevel.getBiome(blockPos));
            if (!optional.isPresent()) {
                return false;
            } else if (this.biome == null || bl && this.biome == optional.get()) {
                if (this.feature == null || bl && serverLevel.getStructureManager().getStructureAt(blockPos, true, this.feature).isValid()) {
                    if (this.smokey == null || bl && this.smokey == BlockCampfire.isSmokeyPos(serverLevel, blockPos)) {
                        if (!this.light.matches(serverLevel, blockPos)) {
                            return false;
                        } else if (!this.block.matches(serverLevel, blockPos)) {
                            return false;
                        } else {
                            return this.fluid.matches(serverLevel, blockPos);
                        }
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
    }

    public JsonElement serializeToJson() {
        if (this == ANY) {
            return JsonNull.INSTANCE;
        } else {
            JsonObject jsonObject = new JsonObject();
            if (!this.x.isAny() || !this.y.isAny() || !this.z.isAny()) {
                JsonObject jsonObject2 = new JsonObject();
                jsonObject2.add("x", this.x.serializeToJson());
                jsonObject2.add("y", this.y.serializeToJson());
                jsonObject2.add("z", this.z.serializeToJson());
                jsonObject.add("position", jsonObject2);
            }

            if (this.dimension != null) {
                World.RESOURCE_KEY_CODEC.encodeStart(JsonOps.INSTANCE, this.dimension).resultOrPartial(LOGGER::error).ifPresent((jsonElement) -> {
                    jsonObject.add("dimension", jsonElement);
                });
            }

            if (this.feature != null) {
                jsonObject.addProperty("feature", this.feature.getFeatureName());
            }

            if (this.biome != null) {
                jsonObject.addProperty("biome", this.biome.location().toString());
            }

            if (this.smokey != null) {
                jsonObject.addProperty("smokey", this.smokey);
            }

            jsonObject.add("light", this.light.serializeToJson());
            jsonObject.add("block", this.block.serializeToJson());
            jsonObject.add("fluid", this.fluid.serializeToJson());
            return jsonObject;
        }
    }

    public static CriterionConditionLocation fromJson(@Nullable JsonElement json) {
        if (json != null && !json.isJsonNull()) {
            JsonObject jsonObject = ChatDeserializer.convertToJsonObject(json, "location");
            JsonObject jsonObject2 = ChatDeserializer.getAsJsonObject(jsonObject, "position", new JsonObject());
            CriterionConditionValue.DoubleRange doubles = CriterionConditionValue.DoubleRange.fromJson(jsonObject2.get("x"));
            CriterionConditionValue.DoubleRange doubles2 = CriterionConditionValue.DoubleRange.fromJson(jsonObject2.get("y"));
            CriterionConditionValue.DoubleRange doubles3 = CriterionConditionValue.DoubleRange.fromJson(jsonObject2.get("z"));
            ResourceKey<World> resourceKey = jsonObject.has("dimension") ? MinecraftKey.CODEC.parse(JsonOps.INSTANCE, jsonObject.get("dimension")).resultOrPartial(LOGGER::error).map((resourceLocation) -> {
                return ResourceKey.create(IRegistry.DIMENSION_REGISTRY, resourceLocation);
            }).orElse((ResourceKey<World>)null) : null;
            StructureGenerator<?> structureFeature = jsonObject.has("feature") ? StructureGenerator.STRUCTURES_REGISTRY.get(ChatDeserializer.getAsString(jsonObject, "feature")) : null;
            ResourceKey<BiomeBase> resourceKey2 = null;
            if (jsonObject.has("biome")) {
                MinecraftKey resourceLocation = new MinecraftKey(ChatDeserializer.getAsString(jsonObject, "biome"));
                resourceKey2 = ResourceKey.create(IRegistry.BIOME_REGISTRY, resourceLocation);
            }

            Boolean boolean_ = jsonObject.has("smokey") ? jsonObject.get("smokey").getAsBoolean() : null;
            CriterionConditionLight lightPredicate = CriterionConditionLight.fromJson(jsonObject.get("light"));
            CriterionConditionBlock blockPredicate = CriterionConditionBlock.fromJson(jsonObject.get("block"));
            CriterionConditionFluid fluidPredicate = CriterionConditionFluid.fromJson(jsonObject.get("fluid"));
            return new CriterionConditionLocation(doubles, doubles2, doubles3, resourceKey2, structureFeature, resourceKey, boolean_, lightPredicate, blockPredicate, fluidPredicate);
        } else {
            return ANY;
        }
    }

    public static class Builder {
        private CriterionConditionValue.DoubleRange x = CriterionConditionValue.DoubleRange.ANY;
        private CriterionConditionValue.DoubleRange y = CriterionConditionValue.DoubleRange.ANY;
        private CriterionConditionValue.DoubleRange z = CriterionConditionValue.DoubleRange.ANY;
        @Nullable
        private ResourceKey<BiomeBase> biome;
        @Nullable
        private StructureGenerator<?> feature;
        @Nullable
        private ResourceKey<World> dimension;
        @Nullable
        private Boolean smokey;
        private CriterionConditionLight light = CriterionConditionLight.ANY;
        private CriterionConditionBlock block = CriterionConditionBlock.ANY;
        private CriterionConditionFluid fluid = CriterionConditionFluid.ANY;

        public static CriterionConditionLocation.Builder location() {
            return new CriterionConditionLocation.Builder();
        }

        public CriterionConditionLocation.Builder setX(CriterionConditionValue.DoubleRange x) {
            this.x = x;
            return this;
        }

        public CriterionConditionLocation.Builder setY(CriterionConditionValue.DoubleRange y) {
            this.y = y;
            return this;
        }

        public CriterionConditionLocation.Builder setZ(CriterionConditionValue.DoubleRange z) {
            this.z = z;
            return this;
        }

        public CriterionConditionLocation.Builder setBiome(@Nullable ResourceKey<BiomeBase> biome) {
            this.biome = biome;
            return this;
        }

        public CriterionConditionLocation.Builder setFeature(@Nullable StructureGenerator<?> feature) {
            this.feature = feature;
            return this;
        }

        public CriterionConditionLocation.Builder setDimension(@Nullable ResourceKey<World> dimension) {
            this.dimension = dimension;
            return this;
        }

        public CriterionConditionLocation.Builder setLight(CriterionConditionLight light) {
            this.light = light;
            return this;
        }

        public CriterionConditionLocation.Builder setBlock(CriterionConditionBlock block) {
            this.block = block;
            return this;
        }

        public CriterionConditionLocation.Builder setFluid(CriterionConditionFluid fluid) {
            this.fluid = fluid;
            return this;
        }

        public CriterionConditionLocation.Builder setSmokey(Boolean smokey) {
            this.smokey = smokey;
            return this;
        }

        public CriterionConditionLocation build() {
            return new CriterionConditionLocation(this.x, this.y, this.z, this.biome, this.feature, this.dimension, this.smokey, this.light, this.block, this.fluid);
        }
    }
}
