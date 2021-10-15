package net.minecraft.data.models.model;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.level.block.Block;

public class ModelTemplate {
    private final Optional<MinecraftKey> model;
    private final Set<TextureSlot> requiredSlots;
    private final Optional<String> suffix;

    public ModelTemplate(Optional<MinecraftKey> parent, Optional<String> variant, TextureSlot... requiredTextures) {
        this.model = parent;
        this.suffix = variant;
        this.requiredSlots = ImmutableSet.copyOf(requiredTextures);
    }

    public MinecraftKey create(Block block, TextureMapping texture, BiConsumer<MinecraftKey, Supplier<JsonElement>> modelCollector) {
        return this.create(ModelLocationUtils.getModelLocation(block, this.suffix.orElse("")), texture, modelCollector);
    }

    public MinecraftKey createWithSuffix(Block block, String suffix, TextureMapping texture, BiConsumer<MinecraftKey, Supplier<JsonElement>> modelCollector) {
        return this.create(ModelLocationUtils.getModelLocation(block, suffix + (String)this.suffix.orElse("")), texture, modelCollector);
    }

    public MinecraftKey createWithOverride(Block block, String suffix, TextureMapping texture, BiConsumer<MinecraftKey, Supplier<JsonElement>> modelCollector) {
        return this.create(ModelLocationUtils.getModelLocation(block, suffix), texture, modelCollector);
    }

    public MinecraftKey create(MinecraftKey id, TextureMapping texture, BiConsumer<MinecraftKey, Supplier<JsonElement>> modelCollector) {
        Map<TextureSlot, MinecraftKey> map = this.createMap(texture);
        modelCollector.accept(id, () -> {
            JsonObject jsonObject = new JsonObject();
            this.model.ifPresent((resourceLocation) -> {
                jsonObject.addProperty("parent", resourceLocation.toString());
            });
            if (!map.isEmpty()) {
                JsonObject jsonObject2 = new JsonObject();
                map.forEach((textureSlot, resourceLocation) -> {
                    jsonObject2.addProperty(textureSlot.getId(), resourceLocation.toString());
                });
                jsonObject.add("textures", jsonObject2);
            }

            return jsonObject;
        });
        return id;
    }

    private Map<TextureSlot, MinecraftKey> createMap(TextureMapping texture) {
        return Streams.concat(this.requiredSlots.stream(), texture.getForced()).collect(ImmutableMap.toImmutableMap(Function.identity(), texture::get));
    }
}
