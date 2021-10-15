package net.minecraft.server;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.util.Collection;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.advancements.Advancements;
import net.minecraft.advancements.critereon.LootDeserializationContext;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.packs.resources.IResourceManager;
import net.minecraft.server.packs.resources.ResourceDataJson;
import net.minecraft.util.ChatDeserializer;
import net.minecraft.util.profiling.GameProfilerFiller;
import net.minecraft.world.level.storage.loot.LootPredicateManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AdvancementDataWorld extends ResourceDataJson {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final Gson GSON = (new GsonBuilder()).create();
    public Advancements advancements = new Advancements();
    private final LootPredicateManager predicateManager;

    public AdvancementDataWorld(LootPredicateManager conditionManager) {
        super(GSON, "advancements");
        this.predicateManager = conditionManager;
    }

    @Override
    protected void apply(Map<MinecraftKey, JsonElement> prepared, IResourceManager manager, GameProfilerFiller profiler) {
        Map<MinecraftKey, Advancement.SerializedAdvancement> map = Maps.newHashMap();
        prepared.forEach((id, json) -> {
            try {
                JsonObject jsonObject = ChatDeserializer.convertToJsonObject(json, "advancement");
                Advancement.SerializedAdvancement builder = Advancement.SerializedAdvancement.fromJson(jsonObject, new LootDeserializationContext(id, this.predicateManager));
                map.put(id, builder);
            } catch (IllegalArgumentException | JsonParseException var6) {
                LOGGER.error("Parsing error loading custom advancement {}: {}", id, var6.getMessage());
            }

        });
        Advancements advancementList = new Advancements();
        advancementList.add(map);

        for(Advancement advancement : advancementList.getRoots()) {
            if (advancement.getDisplay() != null) {
                AdvancementTree.run(advancement);
            }
        }

        this.advancements = advancementList;
    }

    @Nullable
    public Advancement getAdvancement(MinecraftKey id) {
        return this.advancements.get(id);
    }

    public Collection<Advancement> getAdvancements() {
        return this.advancements.getAllAdvancements();
    }
}
